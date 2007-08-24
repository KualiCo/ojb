package org.apache.ojb.broker.metadata;

/* Copyright 2002-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.accesslayer.ConnectionManagerIF;
import org.apache.ojb.broker.accesslayer.RowReader;
import org.apache.ojb.broker.accesslayer.RowReaderDefaultImpl;
import org.apache.ojb.broker.accesslayer.StatementsForClassFactory;
import org.apache.ojb.broker.accesslayer.StatementsForClassIF;
import org.apache.ojb.broker.core.ValueContainer;
import org.apache.ojb.broker.locking.IsolationLevels;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.SqlHelper;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.Configurator;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;
import org.apache.ojb.broker.util.logging.LoggerFactory;


/**
 * A ClassDescriptor contains all information for mapping objects of a
 * given class to database tables.
 * <br>
 * Note: Be careful when use ClassDescriptor variables or caching
 * ClassDescriptor instances, because instances could become invalid
 * during runtime (see {@link MetadataManager}).
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: ClassDescriptor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public final class ClassDescriptor extends DescriptorBase
    implements Serializable, XmlCapable, IsolationLevels
{
	private String persistentFieldClassName;

    private static final long serialVersionUID = -5212253607374173965L;

    public static final String DYNAMIC_STR = "dynamic";
    public static final String OJB_CONCRETE_CLASS = "ojbConcreteClass";
    private static final Class[] NO_PARAMS = {};

    //---------------------------------------------------------------
    /**
     * The descriptor for the insert procedure/function.
     */
    private InsertProcedureDescriptor insertProcedure;

    //---------------------------------------------------------------
    /**
     * The descriptor for the update procedure/function.
     */
    private UpdateProcedureDescriptor updateProcedure;

    //---------------------------------------------------------------
    /**
     * The descriptor for the delete procedure/function.
     */
    private DeleteProcedureDescriptor deleteProcedure;

    //---------------------------------------------------------------
    // transient fields, to make this class serializable we have to declare
    // some transient fields and some associated string fields to reinitialze
    // transient fields after serialization
    //---------------------------------------------------------------
    /**
     * optional method to be invoked after instance fields are initialized
     */
    private transient Method initializationMethod;
    private String initializationMethodName;

    private transient Method factoryMethod;
    private String factoryMethodName;
    /**
     * whether we have already tried to look up the zero
     * argument constructor. Transient declared, because
     * {@link Constructor} is transient and we need to
     * reinitialize constructor after serialization.
     */
    private transient boolean alreadyLookedupZeroArguments = false;
    /**
     * the zero argument constructor for this class
     */
    private transient Constructor zeroArgumentConstructor = null;

    /**
     * used to signal use of ojbConcreteClass field
     */
    private transient boolean ojbConcreteFieldCheckDone = false;
    private transient FieldDescriptor ojbConcreteClassField;
    /**
     * We have to bound {@link org.apache.ojb.broker.accesslayer.StatementsForClassIF}
     * instance to this class, because metadata may change.
     */
    private transient StatementsForClassIF statementsForClass;
    //---------------------------------------------------------------
    // end transient fields
    //---------------------------------------------------------------

    private DescriptorRepository m_repository;
    /**
     * optional class.method to be invoked to create object instance.  Both
     * of these must be present for this function to be successful.
     */
    private Class factoryClass;
    private int useIdentityColumn = 0;

    private String baseClass = null;
    /**
     * transaction isolation level specified for this class, used in the ODMG server
     */
    private int m_IsolationLevel;
    /**
     * the SQL SCHEMA of the underlying table of this class
     */
    private String schema = null;
    /**
     * the described class
     */
    private Class m_Class = null;
    /**
     * whether the described class is abstract
     */
    private boolean isAbstract = false;
    /**
     * the table name used to store the scalar attributes of this class
     */
    private String m_TableName = null;
//    private Vector superPersistentFieldDescriptors = null;
    /**
     * the RowReader for this class
     */
    private RowReader m_rowReader = null;
/*
arminw:
TODO: this feature doesn't work, so remove/reuse this in future
*/
    /**
     * the class that this class extends
     */
    private String superClass;
    /**
     * reference column for the superclass
     */
    private int superClassFieldRef;
    /**
     * does the described class represent an interface?
     */
    private boolean m_isInterface = false;
    /**
     * the proxy class for the described class, may be null
     */
    private Class proxyClass = null;
    /**
     * the proxy class name for the described class, may be null
     */
    private String proxyClassName = null;
    /**
     * if false do not accept implicit locks on this class
     */
    private boolean acceptLocks = true;
    /**
     * if true instances of this class are always refreshed
     * even if they are already in the cache.
     * false by default.
     */
    private boolean alwaysRefresh = false;
    private int m_ProxyPrefetchingLimit = 50;
    /**
     * optional, ObjectCacheDescriptor for representing class
     */
    private ObjectCacheDescriptor objectCacheDescriptor;
    /**
     * the vector of indices used in DDL generation.
     */
    private Vector indexes = new Vector();

    //-----------------------------------------------------------------
    //-----------------------------------------------------------------
    // !!! the following arrays and maps have take care of metadata changes!!!
    //-----------------------------------------------------------------
    //-----------------------------------------------------------------
    private FieldDescriptor m_autoIncrementField = null;
    /**
     * the FieldDescriptors for the primitive attributes
     */
    private FieldDescriptor[] m_FieldDescriptions = null;
    /**
     * the descriptors for collection attributes
     */
    private Vector m_CollectionDescriptors = new Vector();
    /**
     * the descriptor for 1-1 reference attributes
     */
    private Vector m_ObjectReferenceDescriptors = new Vector();
    /**
     * the non-primary key FieldDescriptors
     */
    private FieldDescriptor[] m_nonPkFieldDescriptors = null;
    /**
     * the primary key FieldDescriptors
     */
    private FieldDescriptor[] m_PkFieldDescriptors = null;
    /**
     * the read/write FieldDescriptors BRJ
     */
    private FieldDescriptor[] m_RwFieldDescriptors = null;
    private FieldDescriptor[] m_RwNonPkFieldDescriptors = null;
    /**
     * the optimistic lockingFieldDescriptors BRJ
     */
    private FieldDescriptor[] m_lockingFieldDescriptors = null;
    /**
     * the list of classes in the extent of this class. can be empty
     */
    private Vector extentClasses = new Vector();
    /**
     * the list of class names in the extent of this class. can be empty
     */
    private Vector extentClassNames = new Vector();
    private Map m_fieldDescriptorNameMap = null;
    private Map m_collectionDescriptorNameMap = null;
    private Map m_objectReferenceDescriptorsNameMap = null;

    // BRJ: ClassDescriptor referenced by 'super' ObjectReferenceDescriptor
    private ClassDescriptor m_superCld = null;
    private boolean m_superCldSet = false;

    //-----------------------------------------------------------------
    //-----------------------------------------------------------------
    // END of cached metadata information
    //-----------------------------------------------------------------
    //-----------------------------------------------------------------


    //---------------------------------------------------------------
    /**
     * Constructor declaration
     */
    public ClassDescriptor(DescriptorRepository pRepository)
    {
        m_repository = pRepository;
        m_IsolationLevel = pRepository.getDefaultIsolationLevel();
    }


    //---------------------------------------------------------------
    // method declarations
    //---------------------------------------------------------------
    public String getBaseClass()
    {
        return baseClass;
    }
    public void setBaseClass(String baseClass)
    {
        this.baseClass = baseClass;
        // first deregister
        getRepository().deregisterSuperClassMultipleJoinedTables(this);
        // register classes using mapping of classes to multiple joined tables
        getRepository().registerSuperClassMultipleJoinedTables(this);
    }

//    /**
//     * @deprecated no longer needed map class on multi joined table
//     */
//    public void setSuperPersistentFieldDescriptors(Vector superPersistentFieldDescriptors)
//    {
//        this.superPersistentFieldDescriptors = superPersistentFieldDescriptors;
//    }
//
//    /**
//     * @deprecated no longer needed map class on multi joined table
//     */
//    public Vector getSuperPersistentFieldDescriptors()
//    {
//        return superPersistentFieldDescriptors;
//    }

    /**
     * Returns the appropriate {@link ObjectCacheDescriptor}
     * or <code>null</code> if not specified.
     */
    public ObjectCacheDescriptor getObjectCacheDescriptor()
    {
        return objectCacheDescriptor;
    }

    /**
     * Sets the {@link ObjectCacheDescriptor} for representing class.
     */
    public void setObjectCacheDescriptor(ObjectCacheDescriptor objectCacheDescriptor)
    {
        this.objectCacheDescriptor = objectCacheDescriptor;
    }


    /**
     * sets the row reader class for this descriptor
     */
    public void setRowReader(RowReader newReader)
    {
        m_rowReader = newReader;
    }

    /**
     * Returns the {@link org.apache.ojb.broker.accesslayer.RowReader}
     * for this descriptor.
     */
    public synchronized RowReader getRowReader()
    {
        if (m_rowReader == null)
        {
            Configurator configurator = OjbConfigurator.getInstance();
            Configuration config = configurator.getConfigurationFor(null);
            Class rrClass = config.getClass("RowReaderDefaultClass", RowReaderDefaultImpl.class);

            setRowReader(rrClass.getName());
        }
        return m_rowReader;
    }

    /**
     * sets the row reader class name for thie class descriptor
     */
    public void setRowReader(String newReaderClassName)
    {
        try
        {
            m_rowReader =
                (RowReader) ClassHelper.newInstance(
                    newReaderClassName,
                    ClassDescriptor.class,
                    this);
        }
        catch (Exception e)
        {
            throw new MetadataException("Instantiating of current set RowReader failed", e);
        }
    }

    public String getRowReaderClassName()
    {
        return m_rowReader != null ? m_rowReader.getClass().getName() : null;
    }

    /**
     * returns the name of the described class
     * @return String name of the described class
     */
    public String getClassNameOfObject()
    {
        return m_Class != null ? m_Class.getName() : null;
    }

    /**
     * returns the class object of the described class
     * @return Class the described class
     */
    public Class getClassOfObject()
    {
        return m_Class;
    }

    /**
     * sets the class object described by this descriptor.
     * @param c the class to describe
     */
    public void setClassOfObject(Class c)
    {
        m_Class = c;
        isAbstract = Modifier.isAbstract(m_Class.getModifiers());
        // TODO : Shouldn't the HashMap in DescriptorRepository be updated as well?
    }

    /**
     * adds a FIELDDESCRIPTOR to this ClassDescriptor.
     * @param fld
     */
    public void addFieldDescriptor(FieldDescriptor fld)
    {
        fld.setClassDescriptor(this); // BRJ
        if (m_FieldDescriptions == null)
        {
            m_FieldDescriptions = new FieldDescriptor[1];
            m_FieldDescriptions[0] = fld;
        }
        else
        {
            int size = m_FieldDescriptions.length;
            FieldDescriptor[] tmpArray = new FieldDescriptor[size + 1];
            System.arraycopy(m_FieldDescriptions, 0, tmpArray, 0, size);
            tmpArray[size] = fld;
            m_FieldDescriptions = tmpArray;
            // 2. Sort fields according to their getOrder() Property
            Arrays.sort(m_FieldDescriptions, FieldDescriptor.getComparator());
        }

        m_fieldDescriptorNameMap = null;
        m_PkFieldDescriptors = null;
        m_nonPkFieldDescriptors = null;
        m_lockingFieldDescriptors = null;
        m_RwFieldDescriptors = null;
        m_RwNonPkFieldDescriptors = null;
    }

    public boolean removeFieldDescriptor(FieldDescriptor fld)
    {
        boolean result = false;
        if(m_FieldDescriptions == null) return result;

        List list = new ArrayList(Arrays.asList(m_FieldDescriptions));
        result = list.remove(fld);
        m_FieldDescriptions = (FieldDescriptor[]) list.toArray(new FieldDescriptor[list.size()]);

        m_fieldDescriptorNameMap = null;
        m_PkFieldDescriptors = null;
        m_nonPkFieldDescriptors = null;
        m_lockingFieldDescriptors = null;
        m_RwFieldDescriptors = null;
        m_RwNonPkFieldDescriptors = null;
        return result;
    }

    /**
     * Returns all defined {@link CollectionDescriptor} for
     * this class descriptor.
     */
    public Vector getCollectionDescriptors()
    {
        return m_CollectionDescriptors;
    }

    /**
     * Add a {@link CollectionDescriptor}.
     */
    public void addCollectionDescriptor(CollectionDescriptor cod)
    {
        m_CollectionDescriptors.add(cod);
        cod.setClassDescriptor(this); // BRJ

        m_collectionDescriptorNameMap = null;
    }

    public void removeCollectionDescriptor(CollectionDescriptor cod)
    {
        m_CollectionDescriptors.remove(cod);
        m_collectionDescriptorNameMap = null;
    }

    /**
     * Returns all defined {@link ObjectReferenceDescriptor}.
     */
    public Vector getObjectReferenceDescriptors()
    {
        return m_ObjectReferenceDescriptors;
    }

    /**
     * Add a {@link ObjectReferenceDescriptor}.
     */
    public void addObjectReferenceDescriptor(ObjectReferenceDescriptor ord)
    {
        m_ObjectReferenceDescriptors.add(ord);
        ord.setClassDescriptor(this); // BRJ

        m_objectReferenceDescriptorsNameMap = null;
    }

    public void removeObjectReferenceDescriptor(ObjectReferenceDescriptor ord)
    {
        m_ObjectReferenceDescriptors.remove(ord);
        m_objectReferenceDescriptorsNameMap = null;
    }

    /**
     * Get an ObjectReferenceDescriptor by name BRJ
     * @param name
     * @return ObjectReferenceDescriptor or null
     */
    public ObjectReferenceDescriptor getObjectReferenceDescriptorByName(String name)
    {
        ObjectReferenceDescriptor ord = (ObjectReferenceDescriptor)
            getObjectReferenceDescriptorsNameMap().get(name);

        //
        // BRJ: if the ReferenceDescriptor is not found
        // look in the ClassDescriptor referenced by 'super' for it
        //
        if (ord == null)
        {
            ClassDescriptor superCld = getSuperClassDescriptor();
            if (superCld != null)
            {
                ord = superCld.getObjectReferenceDescriptorByName(name);
            }
        }
        return ord;
    }

    private Map getObjectReferenceDescriptorsNameMap()
    {
        if (m_objectReferenceDescriptorsNameMap == null)
        {
            HashMap nameMap = new HashMap();

            Vector descriptors = getObjectReferenceDescriptors();
            for (int i = descriptors.size() - 1; i >= 0; i--)
            {
                ObjectReferenceDescriptor ord = (ObjectReferenceDescriptor) descriptors.get(i);
                nameMap.put(ord.getAttributeName(), ord);
            }
            m_objectReferenceDescriptorsNameMap = nameMap;
        }

        return m_objectReferenceDescriptorsNameMap;
    }

    /**
     * Get an CollectionDescriptor by name  BRJ
     * @param name
     * @return CollectionDescriptor or null
     */
    public CollectionDescriptor getCollectionDescriptorByName(String name)
    {
        if (name == null)
        {
            return null;
        }

        CollectionDescriptor cod  = (CollectionDescriptor) getCollectionDescriptorNameMap().get(name);

        //
        // BRJ: if the CollectionDescriptor is not found
        // look in the ClassDescriptor referenced by 'super' for it
        //
        if (cod == null)
        {
            ClassDescriptor superCld = getSuperClassDescriptor();
            if (superCld != null)
            {
                cod = superCld.getCollectionDescriptorByName(name);
            }
        }

        return cod;
    }

    private Map getCollectionDescriptorNameMap()
    {
        if (m_collectionDescriptorNameMap == null)
        {
            HashMap nameMap = new HashMap();

            Vector descriptors = getCollectionDescriptors();
            for (int i = descriptors.size() - 1; i >= 0; i--)
            {
                CollectionDescriptor cod = (CollectionDescriptor) descriptors.get(i);
                nameMap.put(cod.getAttributeName(), cod);
            }
            m_collectionDescriptorNameMap = nameMap;
        }

        return m_collectionDescriptorNameMap;
    }

    /**
     * Answers the ClassDescriptor referenced by 'super' ReferenceDescriptor.
     * @return ClassDescriptor or null
     */
    public ClassDescriptor getSuperClassDescriptor()
    {
        if (!m_superCldSet)
        {
            if(getBaseClass() != null)
            {
                m_superCld = getRepository().getDescriptorFor(getBaseClass());
                if(m_superCld.isAbstract() || m_superCld.isInterface())
                {
                    throw new MetadataException("Super class mapping only work for real class, but declared super class" +
                            " is an interface or is abstract. Declared class: " + m_superCld.getClassNameOfObject());
                }
            }
            m_superCldSet = true;
        }

        return m_superCld;
    }

    /**
     * add an Extent class to the current descriptor
     * @param newExtendClass
     * @deprecated use {@link #addExtentClass(String newExtentClass)} instead
     */
    public void addExtentClassName(Class newExtendClass)
    {
        addExtentClass(newExtendClass);
    }

    /**
     * add an Extent class to the current descriptor
     * @param newExtendClass
     */
    public void addExtentClass(Class newExtendClass)
    {
        extentClasses.add(newExtendClass);
        this.addExtentClass(newExtendClass.getName());
    }

    /**
     * add an Extent class to the current descriptor
     * @param newExtentClassName name of the class to add
     */
    public void addExtentClass(String newExtentClassName)
    {
        extentClassNames.add(newExtentClassName);
        if(m_repository != null) m_repository.addExtent(newExtentClassName, this);
    }

    public void removeExtentClass(String extentClassName)
    {
        extentClassNames.remove(extentClassName);
        if(m_repository != null) m_repository.removeExtent(extentClassName);
    }

    /**
     * return all classes in this extent.
     * Creation date: (02.02.2001 17:49:11)
     * @return java.util.Vector
     */
    public synchronized Vector getExtentClasses()
    {
        if (extentClassNames.size() != extentClasses.size())
        {
            extentClasses.clear();
            for (Iterator iter = extentClassNames.iterator(); iter.hasNext();)
            {
                String classname = (String) iter.next();
                Class extentClass;
                try
                {
                    extentClass = ClassHelper.getClass(classname);
                }
                catch (ClassNotFoundException e)
                {
                    throw new MetadataException(
                        "Unable to load class ["
                            + classname
                            + "]. Make sure it is available on the classpath.",
                        e);
                }
                extentClasses.add(extentClass);
            }
        }
        return extentClasses;
    }


    /**
     * Return the names of all classes in this extent
     * @return java.util.Vector a Vector containing the fully qualified names
     * of all classes in this extent
     */
    public synchronized Vector getExtentClassNames()
    {
        return this.extentClassNames;
    }

    /**
     * Insert the method's description here.
     * Creation date: (02.02.2001 17:49:11)
     * @return boolean
     */
    public boolean isExtent()
    {
        return (getExtentClassNames().size() > 0);
    }

    /**
     * Insert the method's description here.
     * Creation date: (26.01.2001 09:20:09)
     * @return java.lang.Class
     */
    public synchronized Class getProxyClass()
    {
        if ((proxyClass == null) && (proxyClassName != null))
        {
            if (isDynamicProxy())
            {
                /**
                 * AClute: Return the same class back if it is dynamic. This signifies
                 * that this class become the base to a generated sub-class, regadless
                 * of which Proxy implementation is used
                 */
                return getClassOfObject();
            }
            else
            {
                try
                {
                    proxyClass = ClassHelper.getClass(proxyClassName);
                }
                catch (ClassNotFoundException e)
                {
                    throw new MetadataException(e);
                }
            }
        }
        return proxyClass;
    }

    public boolean isDynamicProxy()
    {
        return DYNAMIC_STR.equalsIgnoreCase(proxyClassName);
    }

    /**
     * Sets the proxy class to be used.
     * @param newProxyClass java.lang.Class
     */
    public void setProxyClass(Class newProxyClass)
    {
        proxyClass = newProxyClass;
        if (proxyClass == null)
        {
            setProxyClassName(null);
        }
        else
        {
            proxyClassName = proxyClass.getName();
        }
    }

    /**
     * Sets the name of the proxy class to be used.
     * using "dynamic" instead of a real classname
     * will result in usage of dynamic proxies.
     * @param newProxyClassName the classname or "dynamic"
     */
    public void setProxyClassName(String newProxyClassName)
    {
        proxyClassName = newProxyClassName;
    }

    /**
     * Get the name of the proxy class. This method doesn't try to access
     * the real class, so it can be called even if the class doesn't exist.
     */
    public String getProxyClassName()
    {
        return proxyClassName;
    }

    /**
     * Returns array of all FieldDescriptors.
     */
    public FieldDescriptor[] getFieldDescriptions()
    {
        return m_FieldDescriptions;
    }

    /**
     * Returns the matching {@link FieldDescriptor}.
     */
    public FieldDescriptor getFieldDescriptorByIndex(int index)
    {
        return m_FieldDescriptions[index - 1];
    }

    /**
     * Returns the matching {@link FieldDescriptor} - only fields
     * of the current class will be scanned, to include fields defined
     * the the super-classes too, use method {@link #getFieldDescriptor(boolean)}.
     */
    public FieldDescriptor getFieldDescriptorByName(String name)
    {
        if (name == null || m_FieldDescriptions == null)
        {
            return null;
        }

        if (m_fieldDescriptorNameMap == null)
        {
            HashMap nameMap = new HashMap();

            FieldDescriptor[] descriptors = getFieldDescriptions();
            for (int i = descriptors.length - 1; i >= 0; i--)
            {
                FieldDescriptor fld = descriptors[i];
                nameMap.put(fld.getPersistentField().getName(), fld);
            }

            m_fieldDescriptorNameMap = nameMap;
        }

        return (FieldDescriptor) m_fieldDescriptorNameMap.get(name);
    }

    /**
     * return the FieldDescriptor for the Attribute referenced in the path<br>
     * the path may contain simple attribut names, functions and path expressions
     * using relationships <br>
     * ie: name, avg(price), adress.street
     * @param aPath the path to the attribute
     * @param pathHints a Map containing the class to be used for a segment or <em>null</em>
     * if no segment was used.
     * @return the FieldDescriptor or null (ie: for m:n queries)
     */
    public FieldDescriptor getFieldDescriptorForPath(String aPath, Map pathHints)
    {
        ArrayList desc = getAttributeDescriptorsForPath(aPath, pathHints);
        FieldDescriptor fld = null;
        Object temp;

        if (!desc.isEmpty())
        {
            temp = desc.get(desc.size() - 1);
            if (temp instanceof FieldDescriptor)
            {
                fld = (FieldDescriptor) temp;
            }
        }
        return fld;
    }

    /**
     * return the FieldDescriptor for the Attribute referenced in the path<br>
     * the path may contain simple attribut names, functions and path expressions
     * using relationships <br>
     * ie: name, avg(price), adress.street
     * @param aPath the path to the attribute
     * @return the FieldDescriptor or null (ie: for m:n queries)
     */
    public FieldDescriptor getFieldDescriptorForPath(String aPath)
    {
        return getFieldDescriptorForPath(aPath, null);
    }
/*
arminw:
TODO: this feature doesn't work, so remove this in future
*/
    /**
     *
     * @return this classes FieldDescriptor's as well as it's parents and so on and so on
     */
    public FieldDescriptor[] getFieldDescriptorsInHeirarchy()
    {
        if (superClass == null)
        {
            return getFieldDescriptions();
        }
        ClassDescriptor cldSuper = getRepository().getDescriptorFor(superClass);
        return appendFieldDescriptorArrays(
            getFieldDescriptions(),
            cldSuper.getFieldDescriptorsInHeirarchy());
    }

    private FieldDescriptor[] appendFieldDescriptorArrays(
        FieldDescriptor[] fieldDescriptions,
        FieldDescriptor[] fieldDescriptorsInHeirarchy)
    {
        // take the 2 arrays and add them into one
        int size = fieldDescriptions.length + fieldDescriptorsInHeirarchy.length;
        FieldDescriptor[] newArray = new FieldDescriptor[size];
        System.arraycopy(fieldDescriptions, 0, newArray, 0, fieldDescriptions.length);
        System.arraycopy(fieldDescriptorsInHeirarchy, 0, newArray, fieldDescriptions.length, fieldDescriptorsInHeirarchy.length);
        return newArray;
    }

    /**
     * Returns the first found autoincrement field
     * defined in this class descriptor. Use carefully
     * when multiple autoincrement field were defined.
     * @deprecated does not make sense because it's possible to
     * define more than one autoincrement field. Alternative
     * see {@link #getAutoIncrementFields}
     */
    public FieldDescriptor getAutoIncrementField()
    {
        if (m_autoIncrementField == null)
        {
            FieldDescriptor[] fds = getPkFields();

            for (int i = 0; i < fds.length; i++)
            {
                FieldDescriptor fd = fds[i];
                if (fd.isAutoIncrement())
                {
                    m_autoIncrementField = fd;
                    break;
                }
            }
        }
        if (m_autoIncrementField == null)
        {
            LoggerFactory.getDefaultLogger().warn(
                this.getClass().getName()
                    + ": "
                    + "Could not find autoincrement attribute for class: "
                    + this.getClassNameOfObject());
        }
        return m_autoIncrementField;
    }

    public FieldDescriptor[] getAutoIncrementFields()
    {
        ArrayList result = new ArrayList();
        for (int i = 0; i < m_FieldDescriptions.length; i++)
        {
            FieldDescriptor field = m_FieldDescriptions[i];
            if(field.isAutoIncrement()) result.add(field);
        }
        return (FieldDescriptor[]) result.toArray(new FieldDescriptor[result.size()]);
    }

    /**
     * returns an Array with an Objects CURRENT locking VALUES , BRJ
     * @throws PersistenceBrokerException if there is an erros accessing o field values
     */
    public ValueContainer[] getCurrentLockingValues(Object o) throws PersistenceBrokerException
    {
        FieldDescriptor[] fields = getLockingFields();
        ValueContainer[] result = new ValueContainer[fields.length];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = new ValueContainer(fields[i].getPersistentField().get(o), fields[i].getJdbcType());
        }
        return result;
    }

    /**
     * updates the values for locking fields , BRJ
     * handles int, long, Timestamp
     * respects updateLock so locking field are only updated when updateLock is true
     * @throws PersistenceBrokerException if there is an erros accessing obj field values
     */
    public void updateLockingValues(Object obj) throws PersistenceBrokerException
    {
        FieldDescriptor[] fields = getLockingFields();
        for (int i = 0; i < fields.length; i++)
        {
            FieldDescriptor fmd = fields[i];
            if (fmd.isUpdateLock())
            {
                PersistentField f = fmd.getPersistentField();
                Object cv = f.get(obj);
                // int
                if ((f.getType() == int.class) || (f.getType() == Integer.class))
                {
                    int newCv = 0;
                    if (cv != null)
                    {
                        newCv = ((Number) cv).intValue();
                    }
                    newCv++;
                    f.set(obj, new Integer(newCv));
                }
                // long
                else if ((f.getType() == long.class) || (f.getType() == Long.class))
                {
                    long newCv = 0;
                    if (cv != null)
                    {
                        newCv = ((Number) cv).longValue();
                    }
                    newCv++;
                    f.set(obj, new Long(newCv));
                }
                // Timestamp
                else if (f.getType() == Timestamp.class)
                {
                    long newCv = System.currentTimeMillis();
                    f.set(obj, new Timestamp(newCv));
                }
            }
        }
    }

    /**
     * return an array of NONPK-FieldDescription sorted ascending
     * according to the field-descriptions getOrder() property
     */
    public FieldDescriptor[] getNonPkFields()
    {
        if (m_nonPkFieldDescriptors == null)
        {
            // 1. collect all Primary Key fields from Field list
            Vector vec = new Vector();
            for (int i = 0; i < m_FieldDescriptions.length; i++)
            {
                FieldDescriptor fd = m_FieldDescriptions[i];
                if (!fd.isPrimaryKey())
                {
                    vec.add(fd);
                }
            }
            // 2. Sort fields according to their getOrder() Property
            Collections.sort(vec, FieldDescriptor.getComparator());
            m_nonPkFieldDescriptors =
                (FieldDescriptor[]) vec.toArray(new FieldDescriptor[vec.size()]);
        }
        return m_nonPkFieldDescriptors;
    }

    /**
     * Return an array of PK FieldDescription sorted ascending
     * according to the field-descriptions getOrder() property
     */
    public FieldDescriptor[] getPkFields()
    {
        if (m_PkFieldDescriptors == null)
        {
            // 1. collect all Primary Key fields from Field list
            Vector vec = new Vector();
            // 1.a if descriptor describes an interface: take PK fields from an implementors ClassDescriptor
            if (m_isInterface)
            {
                if (getExtentClasses().size() == 0)
                {
                    throw new PersistenceBrokerException(
                        "No Implementors declared for interface "
                            + this.getClassOfObject().getName());
                }
                Class implementor = (Class) getExtentClasses().get(0);
                ClassDescriptor implCld = this.getRepository().getDescriptorFor(implementor);
                m_PkFieldDescriptors = implCld.getPkFields();
            }
            else
            {
                FieldDescriptor[] fields;
                // 1.b if not an interface The classdescriptor must have FieldDescriptors
                fields = getFieldDescriptions();
                // now collect all PK fields
                for (int i = 0; i < fields.length; i++)
                {
                    FieldDescriptor fd = fields[i];
                    if (fd.isPrimaryKey())
                    {
                        vec.add(fd);
                    }
                }
                // 2. Sort fields according to their getOrder() Property
                Collections.sort(vec, FieldDescriptor.getComparator());
                m_PkFieldDescriptors = (FieldDescriptor[]) vec.toArray(new FieldDescriptor[vec.size()]);
            }
        }
        return m_PkFieldDescriptors;
    }

    /**
     * Returns array of read/write non pk FieldDescriptors.
     */
    public FieldDescriptor[] getNonPkRwFields()
    {
        if (m_RwNonPkFieldDescriptors == null)
        {
            FieldDescriptor[] fields = getNonPkFields();
            Collection rwFields = new ArrayList();

            for (int i = 0; i < fields.length; i++)
            {
                FieldDescriptor fd = fields[i];
                if (!fd.isAccessReadOnly())
                {
                    rwFields.add(fd);
                }
            }
            m_RwNonPkFieldDescriptors =
                (FieldDescriptor[]) rwFields.toArray(new FieldDescriptor[rwFields.size()]);
        }
        return m_RwNonPkFieldDescriptors;
    }

    /**
     * Returns array of read/write FieldDescriptors.
     */
    public FieldDescriptor[] getAllRwFields()
    {
        if (m_RwFieldDescriptors == null)
        {
            FieldDescriptor[] fields = getFieldDescriptions();
            Collection rwFields = new ArrayList();

            for (int i = 0; i < fields.length; i++)
            {
                FieldDescriptor fd = fields[i];
                /*
                arminw: if locking is enabled and the increment of locking
                values is done by the database, the field is read-only
                */
                if(fd.isAccessReadOnly() || (fd.isLocking() && !fd.isUpdateLock()))
                {
                    continue;
                }
                rwFields.add(fd);
            }
            m_RwFieldDescriptors =
                (FieldDescriptor[]) rwFields.toArray(new FieldDescriptor[rwFields.size()]);
        }

        return m_RwFieldDescriptors;
    }

    /**
     * return an array of FieldDescription for optimistic locking sorted ascending
     * according to the field-descriptions getOrder() property
     */
    public FieldDescriptor[] getLockingFields()
    {
        if (m_lockingFieldDescriptors == null)
        {
            // 1. collect all Primary Key fields from Field list
            Vector vec = new Vector();
            for (int i = 0; i < m_FieldDescriptions.length; i++)
            {
                FieldDescriptor fd = m_FieldDescriptions[i];
                if (fd.isLocking())
                {
                    vec.add(fd);
                }
            }
            // 2. Sort fields according to their getOrder() Property
            Collections.sort(vec, FieldDescriptor.getComparator());
            m_lockingFieldDescriptors =
                (FieldDescriptor[]) vec.toArray(new FieldDescriptor[vec.size()]);
        }
        return m_lockingFieldDescriptors;
    }

    /**
     * return true if optimistic locking is used
     */
    public boolean isLocking()
    {
        return getLockingFields().length > 0;
    }

    /**
     * return all AttributeDescriptors for the path<br>
     * ie: partner.addresses.street returns a Collection of 3 AttributeDescriptors
     * (ObjectReferenceDescriptor, CollectionDescriptor, FieldDescriptor)<br>
     * ie: partner.addresses returns a Collection of 2 AttributeDescriptors
     * (ObjectReferenceDescriptor, CollectionDescriptor)
     * @param aPath the cleaned path to the attribute
     * @return ArrayList of AttributeDescriptors
     */
    public ArrayList getAttributeDescriptorsForPath(String aPath)
    {
        return getAttributeDescriptorsForPath(aPath, new HashMap());
    }

    /**
     * return all AttributeDescriptors for the path<br>
     * ie: partner.addresses.street returns a Collection of 3 AttributeDescriptors
     * (ObjectReferenceDescriptor, CollectionDescriptor, FieldDescriptor)<br>
     * ie: partner.addresses returns a Collection of 2 AttributeDescriptors
     * (ObjectReferenceDescriptor, CollectionDescriptor)
     * @param aPath the cleaned path to the attribute
     * @param pathHints a Map containing the class to be used for a segment or <em>null</em>
     * if no segment was used.
     * @return ArrayList of AttributeDescriptors
     */
    public ArrayList getAttributeDescriptorsForPath(String aPath, Map pathHints)
    {
        return getAttributeDescriptorsForCleanPath(SqlHelper.cleanPath(aPath), pathHints);
    }

    /**
     * return all AttributeDescriptors for the path<br>
     * ie: partner.addresses.street returns a Collection of 3 AttributeDescriptors
     * (ObjectReferenceDescriptor, CollectionDescriptor, FieldDescriptor)<br>
     * ie: partner.addresses returns a Collection of 2 AttributeDescriptors
     * (ObjectReferenceDescriptor, CollectionDescriptor)
     * @param aPath the cleaned path to the attribute
     * @param pathHints a Map containing the class to be used for a segment or <em>null</em>
     * if no segment is used.
     * @return ArrayList of AttributeDescriptors
     */
    private ArrayList getAttributeDescriptorsForCleanPath(String aPath, Map pathHints)
    {
        ArrayList result = new ArrayList();
        ClassDescriptor cld = this;
        ObjectReferenceDescriptor ord;
        FieldDescriptor fld;
        String currPath = aPath;
        String segment;
        StringBuffer processedSegment = new StringBuffer();
        int sepPos;
        Class itemClass;

        while (currPath.length() > 0)
        {
            sepPos = currPath.indexOf(".");
            if (sepPos >= 0)
            {
                segment = currPath.substring(0, sepPos);
                currPath = currPath.substring(sepPos + 1);
            }
            else
            {
                segment = currPath;
                currPath = "";
            }

            if (processedSegment.length() > 0)
            {
                processedSegment.append(".");
            }
            processedSegment.append(segment);

            // look for 1:1 or n:1 Relationship
            ord = cld.getObjectReferenceDescriptorByName(segment);
            if (ord == null)
            {
                // look for 1:n or m:n Relationship
                ord = cld.getCollectionDescriptorByName(segment);
            }

            if (ord != null)
            {
                // BRJ : look for hints for the processed segment
                // ie: ref pointng to ClassA and ref.ref pointing to ClassC
                List hintClasses = pathHints != null ? (List) pathHints.get(processedSegment.toString()) : null;
                if (hintClasses != null && hintClasses.get(0) != null)
                {
                    itemClass = (Class) hintClasses.get(0);
                }
                else
                {
                    itemClass = ord.getItemClass();
                }

                cld = cld.getRepository().getDescriptorFor(itemClass);
                result.add(ord);
            }
            else
            {
                // look for Field
                fld = cld.getFieldDescriptorByName(segment);
                if (fld != null)
                {
                    result.add(fld);
                }
            }
        }

        return result;
    }

    /**
     * returns the zero argument constructor for the class represented by this class descriptor
     * or null if a zero argument constructor does not exist.  If the zero argument constructor
     * for this class is not public it is made accessible before being returned.
     */
    public Constructor getZeroArgumentConstructor()
    {
        if (zeroArgumentConstructor == null && !alreadyLookedupZeroArguments)
        {
            try
            {
                zeroArgumentConstructor = getClassOfObject().getConstructor(NO_PARAMS);
            }
            catch (NoSuchMethodException e)
            {
                //no public zero argument constructor available let's try for a private/protected one
                try
                {
                    zeroArgumentConstructor = getClassOfObject().getDeclaredConstructor(NO_PARAMS);

                    //we found one, now let's make it accessible
                    zeroArgumentConstructor.setAccessible(true);
                }
                catch (NoSuchMethodException e2)
                {
                    //out of options, log the fact and let the method return null
                    LoggerFactory.getDefaultLogger().warn(
                        this.getClass().getName()
                            + ": "
                            + "No zero argument constructor defined for "
                            + this.getClassOfObject());
                }
            }

            alreadyLookedupZeroArguments = true;
        }

        return zeroArgumentConstructor;
    }

    /*
     * @see XmlCapable#toXML()
     */
    public String toXML()
    {
        RepositoryTags tags = RepositoryTags.getInstance();
        String eol = System.getProperty("line.separator");

        // comment on class
        StringBuffer result = new StringBuffer(1024);
        result.append( eol);
        result.append( "  <!-- Mapping for Class ");
        result.append( this.getClassNameOfObject());
        result.append( " -->");
        result.append( eol );

        // opening tag and attributes
        result.append( "  ");
        result.append( tags.getOpeningTagNonClosingById(CLASS_DESCRIPTOR));
        result.append( eol );

        // class
        result.append( "    ");
        result.append( tags.getAttribute(CLASS_NAME, this.getClassNameOfObject()));
        result.append( eol );

        // isolation level is optional
        if (null != getRepository())
        {
            if (getIsolationLevel() != this.getRepository().getDefaultIsolationLevel())
            {
                result.append( "    ");
                result.append( tags.getAttribute(ISOLATION_LEVEL, this.isolationLevelXml()) );
                result.append( eol );
            }
        }

        Class theProxyClass = null;
        try
        {
            theProxyClass = this.getProxyClass();
        }
        catch (Throwable t)
        {
            // Ignore this exception, just try to get the Class object of the
            // proxy class in order to be able to decide, whether the class
            // is a dynamic proxy or not.
        }

        // proxy is optional
        if (theProxyClass != null)
	{
	    if (isDynamicProxy())   // tomdz: What about VirtualProxy ?
            {
        	result.append( "    ");
                result.append( tags.getAttribute(CLASS_PROXY, DYNAMIC_STR));
                result.append( eol );
    	    }
            else
	    {
    	        result.append( "    ");
            result.append( tags.getAttribute(CLASS_PROXY, this.getProxyClassName()));
            result.append( eol );
            }
            result.append( "        ");
        result.append( tags.getAttribute(PROXY_PREFETCHING_LIMIT, "" + this.getProxyPrefetchingLimit()));
        result.append( eol );
	}

        // schema is optional
        if (this.getSchema() != null)
        {
            result.append( "    ");
            result.append( tags.getAttribute(SCHEMA_NAME, this.getSchema()));
            result.append( eol );
        }

        // table name
        if (this.getTableName() != null)
        {
            result.append("    ");
            result.append( tags.getAttribute(TABLE_NAME, this.getTableName()));
            result.append( eol );
        }

        // rowreader is optional
        if (this.getRowReaderClassName() != null)
        {
            result.append( "    ");
            result.append( tags.getAttribute(ROW_READER, this.getRowReaderClassName()));
            result.append( eol );
        }

        //accept-locks is optional, enabled by default
        if (!this.acceptLocks)
        {
            result.append( "        ");
            result.append( tags.getAttribute(ACCEPT_LOCKS, "false"));
            result.append( eol );
        }
        // sequence manager attribute not yet implemented

        // initialization method is optional
        if (this.getInitializationMethod() != null)
        {
            result.append( "    ");
            result.append( tags.getAttribute(INITIALIZATION_METHOD, this.getInitializationMethod().getName()));
            result.append( eol );
        }

        // factory class is optional
        if (this.getFactoryClass() != null)
        {
            result.append( "    ");
            result.append( tags.getAttribute(FACTORY_CLASS, this.getFactoryClass().getName()) );
            result.append( eol );
        }

        //	factory method is optional
        if (this.getFactoryMethod() != null)
        {
            result.append( "    ");
            result.append( tags.getAttribute(FACTORY_METHOD, this.getFactoryMethod().getName()) );
            result.append( eol );
        }

        //reference refresh is optional, disabled by default
        if (isAlwaysRefresh())
        {
            result.append( "    ");
            result.append( tags.getAttribute(REFRESH, "true"));
            result.append( eol );
        }

        result.append( "  >");
        result.append( eol );

        // end of attributes

        // begin of elements
        if (isInterface())
        {
            // extent-class
            for (int i = 0; i < getExtentClassNames().size(); i++)
            {
                result.append( "      ");
                result.append( tags.getOpeningTagNonClosingById(CLASS_EXTENT));
                result.append( " " );
                result.append( tags.getAttribute(CLASS_REF, getExtentClassNames().get(i).toString()) );
                result.append( " />");
                result.append( eol );
            }
        }
        else
        {
            // class extent is optional
            if (isExtent())
            {
                for (int i = 0; i < getExtentClassNames().size(); i++)
                {
                    result.append( "      ");
                    result.append( tags.getOpeningTagNonClosingById(CLASS_EXTENT));
                    result.append( " " );
                    result.append( tags.getAttribute(CLASS_REF, getExtentClassNames().get(i).toString()) );
                    result.append( " />");
                    result.append( eol );
                }
            }

            // write all FieldDescriptors
            FieldDescriptor[] fields = getFieldDescriptions();
            for (int i = 0; i < fields.length; i++)
            {
                result.append( fields[i].toXML() );
            }

            // write optional ReferenceDescriptors
            Vector refs = getObjectReferenceDescriptors();
            for (int i = 0; i < refs.size(); i++)
            {
                result.append( ((ObjectReferenceDescriptor) refs.get(i)).toXML() );
            }

            // write optional CollectionDescriptors
            Vector cols = getCollectionDescriptors();
            for (int i = 0; i < cols.size(); i++)
            {
                result.append( ((CollectionDescriptor) cols.get(i)).toXML() );
            }

            // write optional IndexDescriptors
            for (int i = 0; i < indexes.size(); i++)
            {
                IndexDescriptor indexDescriptor = (IndexDescriptor) indexes.elementAt(i);
                result.append( indexDescriptor.toXML() );
            }

            // Write out the procedures
            if (this.getInsertProcedure() != null)
            {
                result.append( this.getInsertProcedure().toXML() );
            }
            if (this.getUpdateProcedure() != null)
            {
                result.append( this.getUpdateProcedure().toXML() );
            }
            if (this.getDeleteProcedure() != null)
            {
                result.append( this.getDeleteProcedure().toXML() );
            }
        }
        result.append( "  ");
        result.append( tags.getClosingTagById(CLASS_DESCRIPTOR) );
        return result.toString();
    }

    private String isolationLevelXml()
    {
        switch (this.getIsolationLevel())
        {
            case (IL_OPTIMISTIC) :
                {
                    return LITERAL_IL_OPTIMISTIC;
                }
            case (IL_READ_COMMITTED) :
                {
                    return LITERAL_IL_READ_COMMITTED;
                }
            case (IL_READ_UNCOMMITTED) :
                {
                    return LITERAL_IL_READ_UNCOMMITTED;
                }
            case (IL_REPEATABLE_READ) :
                {
                    return LITERAL_IL_REPEATABLE_READ;
                }
            case (IL_SERIALIZABLE) :
                {
                    return LITERAL_IL_SERIALIZABLE;
                }
            default :
                {
                    return LITERAL_IL_READ_UNCOMMITTED;
                }
        }
    }
/*
arminw:
TODO: this feature doesn't work, so remove this in future
*/
    /**
     * Set name of the super class.
     */
    public void setSuperClass(String classname)
    {
        this.superClass = classname;
    }

    /**
     * Return the super class or <code>null</code>
     * if not declared in repository file.
     */
    public String getSuperClass()
    {
        return superClass;
    }

    /**
     * TODO drop this method?
     */
    public void setSuperClassFieldRef(int fieldId)
    {
        this.superClassFieldRef = fieldId;
    }

    /**
     * TODO drop this method?
     */
    public int getSuperClassFieldRef()
    {
        return superClassFieldRef;
    }

    /**
     * Return true, if the described class is
     * an interface.
     */
    public boolean isInterface()
    {
        return m_isInterface;
    }

    /**
     * Set <code>true</code> if described class is
     * a interface.
     */
    public void setIsInterface(boolean newIsInterface)
    {
        m_isInterface = newIsInterface;
    }

    /**
     * @return boolean true if the mapped class is abstract
     */
    public boolean isAbstract()
    {
        return isAbstract;
    }

    /**
     * Returns acceptLocks.
     * @return boolean
     */
    public boolean isAcceptLocks()
    {
        return acceptLocks;
    }

    /**
     * Sets acceptLocks.
     * @param acceptLocks The m_acceptLocks to set
     */
    public void setAcceptLocks(boolean acceptLocks)
    {
        this.acceptLocks = acceptLocks;
    }

    /**
     * Gets the IndexDescriptors used for DDL generation.
     */
    public Vector getIndexes()
    {
        return indexes;
    }

    /**
     * Sets the IndexDescriptors used for DDL generation.
     */
    public void setIndexes(Vector indexes)
    {
        this.indexes = indexes;
    }

    /**
     * Gets the repository.
     * @return Returns a DescriptorRepository
     */
    public DescriptorRepository getRepository()
    {
        return m_repository;
    }

    /**
     * Sets the repository.
     * @param repository The repository to set
     */
    public void setRepository(DescriptorRepository repository)
    {
        m_repository = repository;
    }

    /**
     * returns the transaction isolation level to be used for this class. Used only in the ODMG server
     */
    public int getIsolationLevel()
    {
        return m_IsolationLevel;
    }

    /**
     * Method declaration
     * @param isoLevel
     */
    public void setIsolationLevel(int isoLevel)
    {
        m_IsolationLevel = isoLevel;
    }

    /**
     * Method declaration
     * @return table name
     */
    private String getTableName()
    {
        return m_TableName;
    }

    /**
     * Method declaration
     * @param str
     */
    public void setTableName(String str)
    {
        m_TableName = str;
    }

    /**
     * Answer Table name including schema    BRJ
     */
    public String getFullTableName()
    {
        if (getSchema() != null)
            return getSchema() + "." + getTableName();
        else
            return getTableName();
    }

    /**
     * Gets the schema.
     * @return Returns a String
     */
    public String getSchema()
    {
        return schema;
    }

    /**
     * Sets the schema.
     * @param schema The schema to set
     */
    public void setSchema(String schema)
    {
        this.schema = schema;
    }

    /**
     * Return a string representation of this class.
     */
    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        return buf
            .append("classNameOfObject", getClassNameOfObject())
            .append("tableName", getTableName())
            .append("schema", getSchema())
            .append("isInterface", isInterface())
            .append("extendClassNames", getExtentClassNames().toString())
            //.append("[fieldDescriptions:")
            .append(getFieldDescriptions())
            //.append("]")
            .toString();
    }

    /**
     * sets the initialization method for this descriptor
     */
    private synchronized void setInitializationMethod(Method newMethod)
    {
        if (newMethod != null)
        {
            // make sure it's a no argument method
            if (newMethod.getParameterTypes().length > 0)
            {
                throw new MetadataException(
                    "Initialization methods must be zero argument methods: "
                        + newMethod.getClass().getName()
                        + "."
                        + newMethod.getName());
            }

            // make it accessible if it's not already
            if (!newMethod.isAccessible())
            {
                newMethod.setAccessible(true);
            }
        }
        this.initializationMethod = newMethod;
    }

    /**
     * sets the initialization method for this descriptor by name
     */
    public synchronized void setInitializationMethod(String newMethodName)
    {
        Method newMethod = null;
        if (newMethodName != null)
        {
            initializationMethodName = newMethodName;
            try
            {
                // see if we have a publicly accessible method by the name
                newMethod = getClassOfObject().getMethod(newMethodName, NO_PARAMS);
            }
            catch (NoSuchMethodException e)
            {
                try
                {
                    // no publicly accessible method, see if there is a private/protected one
                    newMethod = getClassOfObject().getDeclaredMethod(newMethodName, NO_PARAMS);
                }
                catch (NoSuchMethodException e2)
                {
                    // there is no such method available
                    throw new MetadataException(
                        "Invalid initialization method, there is not"
                            + " a zero argument method named "
                            + newMethodName
                            + " on class "
                            + getClassOfObject().getName()
                            + ".");
                }
            }
        }
        setInitializationMethod(newMethod);
    }

    /**
     * Returns the initialization method for this descriptor or null if no
     * initialization method is defined.
     */
    public synchronized Method getInitializationMethod()
    {
        if(this.initializationMethod == null)
        {
            setInitializationMethod(initializationMethodName);
        }
        return initializationMethod;
    }

    /**
     * if true instances of this class are always refreshed
     * even if they are already in the cache.
     * @return boolean
     */
    public boolean isAlwaysRefresh()
    {
        return alwaysRefresh;
    }

    /**
     * Sets the alwaysRefresh parameter.
     * @param alwaysRefresh The value to set
     */
    public void setAlwaysRefresh(boolean alwaysRefresh)
    {
        this.alwaysRefresh = alwaysRefresh;
    }

    public int getProxyPrefetchingLimit()
    {
        return m_ProxyPrefetchingLimit;
    }

    public void setProxyPrefetchingLimit(int proxyPrefetchingLimit)
    {
        m_ProxyPrefetchingLimit = proxyPrefetchingLimit;
    }

    /**
     * Return factory class.
     */
    public synchronized Class getFactoryClass()
    {
        return this.factoryClass;
    }

    /**
     * Return factory method.
     */
    public synchronized Method getFactoryMethod()
    {
        if(factoryMethod == null && factoryMethodName != null)
        {
            setFactoryMethod(factoryMethodName);
        }
        return this.factoryMethod;
    }

    /**
     * Set the object factory for class described by this
     * descriptor.
     * @see #setFactoryMethod
     */
    public synchronized void setFactoryClass(Class newClass)
    {
        this.factoryClass = newClass;
    }

    /**
     * @see #setFactoryClass
     */
    public void setFactoryClass(String newClass)
    {
        if (null != newClass)
        {
            try
            {
                Class clazz = ClassHelper.getClass(newClass);
                setFactoryClass(clazz);
            }
            catch (Exception e)
            {
                // there is no such method available
                throw new MetadataException("Invalid factory class: " + newClass + ".");
            }
        }
        else
        {
            setFactoryClass((Class) null);
        }
    }

    /**
     * Specify the method to instantiate objects
     * represented by this descriptor.
     * @see #setFactoryClass 
     */
    private synchronized void setFactoryMethod(Method newMethod)
    {
        if (newMethod != null)
        {
            // make sure it's a no argument method
            if (newMethod.getParameterTypes().length > 0)
            {
                throw new MetadataException(
                    "Factory methods must be zero argument methods: "
                        + newMethod.getClass().getName()
                        + "."
                        + newMethod.getName());
            }

            // make it accessible if it's not already
            if (!newMethod.isAccessible())
            {
                newMethod.setAccessible(true);
            }
        }

        this.factoryMethod = newMethod;
    }

    /**
     * sets the initialization method for this descriptor by name
     */
    public synchronized void setFactoryMethod(String factoryMethodName)
    {
        Method newMethod = null;
        this.factoryMethodName = factoryMethodName;

        if (factoryMethodName != null)
        {
            try
            {
                // see if we have a publicly accessible method by the name
                newMethod = getFactoryClass().getMethod(factoryMethodName, NO_PARAMS);
            }
            catch (NoSuchMethodException e)
            {
                try
                {
                    // no publicly accessible method, see if there is a private/protected one
                    newMethod = getFactoryClass().getDeclaredMethod(factoryMethodName, NO_PARAMS);
                }
                catch (NoSuchMethodException e2)
                {
                    // there is no such method available
                    throw new MetadataException(
                        "Invalid factory method, there is not"
                            + " a zero argument method named "
                            + factoryMethodName
                            + " on class "
                            + getFactoryClass().getName()
                            + ".");
                }
            }
        }
        setFactoryMethod(newMethod);
    }

    //---------------------------------------------------------------
    /**
     * Change the descriptor for the insert procedure/function.
     *
     * @param newValue the new value.
     */
    public void setInsertProcedure(InsertProcedureDescriptor newValue)
    {
        this.insertProcedure = newValue;
    }

    //---------------------------------------------------------------
    /**
     * Retrieve the descriptor for the insert procedure/function.
     *
     * @return The current value
     */
    public InsertProcedureDescriptor getInsertProcedure()
    {
        return this.insertProcedure;
    }

    //---------------------------------------------------------------
    /**
     * Change the descriptor for the update procedure/function.
     *
     * @param newValue the new value.
     */
    public void setUpdateProcedure(UpdateProcedureDescriptor newValue)
    {
        this.updateProcedure = newValue;
    }

    //---------------------------------------------------------------
    /**
     * Retrieve the descriptor for the update procedure/function.
     *
     * @return The current value
     */
    public UpdateProcedureDescriptor getUpdateProcedure()
    {
        return this.updateProcedure;
    }

    //---------------------------------------------------------------
    /**
     * Change the descriptor for the delete procedure/function.
     *
     * @param newValue the new value.
     */
    public void setDeleteProcedure(DeleteProcedureDescriptor newValue)
    {
        this.deleteProcedure = newValue;
    }

    //---------------------------------------------------------------
    /**
     * Retrieve the descriptor for the delete procedure/function.
     *
     * @return The current value
     */
    public DeleteProcedureDescriptor getDeleteProcedure()
    {
        return this.deleteProcedure;
    }

    /**
     * Returns the ojbConcreteClass field or <code>null</code> if none defined.
     */
    public FieldDescriptor getOjbConcreteClassField()
    {
        // if not checked before
        if(!ojbConcreteFieldCheckDone)
        {
            ojbConcreteClassField = getFieldDescriptorByName(OJB_CONCRETE_CLASS);
            ojbConcreteFieldCheckDone = true;
        }
        return ojbConcreteClassField;
    }

    public StatementsForClassIF getStatementsForClass(ConnectionManagerIF conMan)
    {
        if(statementsForClass == null)
        {
           statementsForClass = StatementsForClassFactory.getInstance().
                   getStatementsForClass(conMan.getConnectionDescriptor(), this);
        }
        return statementsForClass;
    }


    /**
     * Optional! Set the {@link org.apache.ojb.broker.metadata.fieldaccess.PersistentField}
     * implementation class used by this class.
     * @param pfClassName The full qualified class name of the
     * {@link org.apache.ojb.broker.metadata.fieldaccess.PersistentField}.
     */
    public void setPersistentFieldClassName(String pfClassName)
    {
		this.persistentFieldClassName = pfClassName;
    }


    /**
     * Get the used {@link org.apache.ojb.broker.metadata.fieldaccess.PersistentField}
     * implementation name.
     */
    public String getPersistentFieldClassName()
    {
		return persistentFieldClassName;
    }

    /**
     * Returns <em>true</em> if an DB Identity column field based sequence
     * manager was used. In that cases we will find an autoincrement field with
     * read-only access and return true, otherwise false.
     */
     public boolean useIdentityColumnField()
     {
         if(useIdentityColumn == 0)
         {
             useIdentityColumn = -1;
             FieldDescriptor[] pkFields = getPkFields();
             for (int i = 0; i < pkFields.length; i++)
             {
                 // to find the identity column we search for a autoincrement
                 // read-only field
                if (pkFields[i].isAutoIncrement() && pkFields[i].isAccessReadOnly())
                {
                    useIdentityColumn = 1;
                    break;
                }
             }
         }
        return useIdentityColumn == 1;
     }

    /**
     * Returns all defined {@link ObjectReferenceDescriptor}.
     *
     * @param withInherited If <em>true</em> inherited super class references will be included.
     */
    public List getObjectReferenceDescriptors(boolean withInherited)
    {
        if(withInherited && getSuperClassDescriptor() != null)
        {
            List result = new ArrayList(m_ObjectReferenceDescriptors);
            result.addAll(getSuperClassDescriptor().getObjectReferenceDescriptors(true));
            return result;
        }
        else
        {
            return m_ObjectReferenceDescriptors;
        }
    }

    /**
     * Returns all defined {@link CollectionDescriptor} for
     * this class descriptor.
     *
     * @param withInherited If <em>true</em> inherited super class references will be included.
     */
    public List getCollectionDescriptors(boolean withInherited)
    {
        if(withInherited && getSuperClassDescriptor() != null)
        {
            List result = new ArrayList(m_CollectionDescriptors);
            result.addAll(getSuperClassDescriptor().getCollectionDescriptors(true));
            return result;
        }
        else
        {
            return m_CollectionDescriptors;
        }
    }

    /**
     * Return an array of all {@link FieldDescriptor} for this represented class, if
     * parameter <em>withInherited</em> is <em>true</em> all inherited descriptor
     * of declared super classes are included.
     *
     * @param withInherited If <em>true</em> inherited super class fields will be included.
     */
    public FieldDescriptor[] getFieldDescriptor(boolean withInherited)
    {
        if(withInherited && getSuperClassDescriptor() != null)
        {
            /*
            arminw: only return no-PK fields, because all PK fields are declared
            in sub-class too.
            */
            FieldDescriptor[] superFlds = getSuperClassDescriptor().getFieldDescriptorNonPk(true);
            if(m_FieldDescriptions == null)
            {
                m_FieldDescriptions = new FieldDescriptor[0];
            }
            FieldDescriptor[] result = new FieldDescriptor[m_FieldDescriptions.length + superFlds.length];
            System.arraycopy(m_FieldDescriptions, 0, result, 0, m_FieldDescriptions.length);
            System.arraycopy(superFlds, 0, result, m_FieldDescriptions.length, superFlds.length);
            // System.out.println("all fields: " + ArrayUtils.toString(result));
            return result;
        }
        else
        {
            return m_FieldDescriptions;
        }
    }

    /**
     * Return an array of NON-PK {@link FieldDescriptor}, if parameter <em>withInherited</em>
     * is <em>true</em> all inherited descriptor of declared super classes are included.
     *
     * @param withInherited If <em>true</em> inherited super class fields will be included.
     */
    public FieldDescriptor[] getFieldDescriptorNonPk(boolean withInherited)
    {
        if(withInherited && getSuperClassDescriptor() != null)
        {
            FieldDescriptor[] flds = getNonPkFields();
            FieldDescriptor[] superFlds = getSuperClassDescriptor().getFieldDescriptorNonPk(true);
            FieldDescriptor[] result = new FieldDescriptor[flds.length + superFlds.length];
            System.arraycopy(flds, 0, result, 0, flds.length);
            System.arraycopy(superFlds, 0, result, flds.length, superFlds.length);
            return result;
        }
        else
        {
            return getNonPkFields();
        }
    }

    /**
     * Returns the {@link SuperReferenceDescriptor} of this class or <em>null</em>
     * if none was used.
     *
     * @return The reference descriptor for the <em>super</em>-reference or <em>null</em>
     * if not exists.
     */
    public SuperReferenceDescriptor getSuperReference()
    {
        return (SuperReferenceDescriptor) getObjectReferenceDescriptorByName(SuperReferenceDescriptor.SUPER_FIELD_INTERNAL_NAME);
    }
}
