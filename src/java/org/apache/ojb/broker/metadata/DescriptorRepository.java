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
import java.util.*;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.locking.IsolationLevels;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.logging.Logger;

/**
 * The repository containing all object mapping and manipulation information of
 * all used persistent objects.
 * <br>
 * Note: Be careful when use references of this class or caching instances of this class,
 * because instances could become invalid (see {@link MetadataManager}).
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @author <a href="mailto:leandro@ibnetwork.com.br">Leandro Rodrigo Saad Cruz<a>
 * @version $Id: DescriptorRepository.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
@SuppressWarnings("unchecked")
public final class DescriptorRepository extends DescriptorBase
        implements Serializable, XmlCapable, IsolationLevels
{
    static final long serialVersionUID = -1556339982311359524L;
    private Logger log = LoggerFactory.getLogger(DescriptorRepository.class);

    /**
     * The version identifier of the Repository.
     * Used to validate repository.xml against the dtd.
     */
    private static final String VERSION = "1.0";
    /**
     * the default isolation level used for this repository
     */
    private int defaultIsolationLevel = IsolationLevels.IL_DEFAULT;
    /**
     * This table holds all known Mapping descriptions.
     * Key values are the respective Class objects
     */
	private final HashMap descriptorTable;
    /**
     * We need a lot the extent, to which a class belongs
     * (@see DescriptorRepository#getExtentClass). To speed up the costy
     * evaluation, we use this tiny hash map.
     */
    private Map extentTable;

    private Map superClassMultipleJoinedTablesMap;

    private transient Map m_multiMappedTableMap;
    private transient Map m_topLevelClassTable;
    private transient Map m_firstConcreteClassMap;
    private transient Map m_allConcreteSubClass;

    /**
     * Constructor declaration
     */
    public DescriptorRepository() throws PersistenceBrokerException
    {
        descriptorTable = new HashMap();
        extentTable = new HashMap();
        superClassMultipleJoinedTablesMap = new HashMap();
    }

    public static String getVersion()
    {
        return VERSION;
    }

    /**
     * Add a pair of extent/classdescriptor to the extentTable to gain speed
     * while retrieval of extents.
     * @param classname the name of the extent itself
     * @param cld the class descriptor, where it belongs to
     */
    void addExtent(String classname, ClassDescriptor cld)
    {
        synchronized (extentTable)
        {
            extentTable.put(classname, cld);
        }
    }

    /**
     * Remove a pair of extent/classdescriptor from the extentTable.
     * @param classname the name of the extent itself
     */
    void removeExtent(String classname)
    {
        synchronized (extentTable)
        {
            // returns the super class for given extent class name
            ClassDescriptor cld = (ClassDescriptor) extentTable.remove(classname);
            if(cld != null && m_topLevelClassTable != null)
            {
                Class extClass = null;
                try
                {
                    extClass = ClassHelper.getClass(classname);
                }
                catch (ClassNotFoundException e)
                {
                    // Should not happen
                    throw new MetadataException("Can't instantiate class object for needed extent remove", e);
                }
                // remove extent from super class descriptor
                cld.removeExtentClass(classname);
                m_topLevelClassTable.remove(extClass);
                // clear map with first concrete classes, because the removed
                // extent could be such a first found concrete class
                m_firstConcreteClassMap = null;
            }
        }
    }

    /**
     * Returns the top level (extent) class to which the given class belongs.
     * This may be a (abstract) base-class, an interface or the given class
     * itself if given class is not defined as an extent in other class
     * descriptors.
     *
     * @throws ClassNotPersistenceCapableException if clazz is not persistence capable,
     * i.e. if clazz is not defined in the DescriptorRepository.
     */
    public Class getTopLevelClass(Class clazz) throws ClassNotPersistenceCapableException
    {
        if(m_topLevelClassTable == null)
        {
            m_topLevelClassTable = new HashMap();
        }
        // try to find an extent that contains clazz
        Class retval = (Class) m_topLevelClassTable.get(clazz);
        if (retval == null)
        {
            synchronized (extentTable)
            {
                ClassDescriptor cld = (ClassDescriptor) extentTable.get(clazz.getName());
                if (cld == null)
                {
                    // walk the super-references
                    cld = getDescriptorFor(clazz).getSuperClassDescriptor();
                }
                
                if (cld != null)
                {
                    // fix by Mark Rowell
                    // Changed to call getExtentClass recursively
                    retval = getTopLevelClass(cld.getClassOfObject());
                    // if such an extent could not be found just return clazz itself.
                    if (retval == null)
                    {
                        retval = clazz;
                    }
                }
                else
                {
                    // check if class is persistence capable
                    // +
                    // Adam Jenkins: use the class that is associated
                    // with the descriptor instead of the actual class
                    ClassDescriptor temp = getDescriptorFor(clazz);
                    retval = temp.getClassOfObject();
                }
                m_topLevelClassTable.put(clazz, retval);
            }
        }
        return retval;
    }

    /**
     * @return all field descriptors for a class that belongs to a set of classes mapped
     * to the same table, otherwise the select queries produced won't contain the necessary
     * information to materialize extents mapped to the same class.
     */
    public synchronized FieldDescriptor[] getFieldDescriptorsForMultiMappedTable(ClassDescriptor targetCld)
    {
        if (m_multiMappedTableMap == null)
        {
            m_multiMappedTableMap = new HashMap();
        }

        FieldDescriptor[] retval = (FieldDescriptor[]) m_multiMappedTableMap.get(targetCld.getClassNameOfObject());
        if (retval == null)
        {
            retval = getAllMappedColumns(getClassesMappedToSameTable(targetCld));
            m_multiMappedTableMap.put(targetCld.getClassNameOfObject(), retval);
        }
        return retval;
    }

    private FieldDescriptor[] getAllMappedColumns(List classDescriptors)
    {
        /* mkalen: Use an ordered implementation not to loose individual field ordering.
            This is especially important for eg Oracle9i platform and LONGVARBINARY columns,
            see http://download-west.oracle.com/docs/cd/B10501_01/java.920/a96654/basic.htm#1021777
            "If you do not use the SELECT-list order to access data,
             then you can lose the stream data."
        */
        List allFieldDescriptors = new Vector();

        Set visitedColumns = new HashSet();
        Iterator it = classDescriptors.iterator();
        ClassDescriptor temp = null;
        FieldDescriptor[] fields;
        while (it.hasNext())
        {
            temp = (ClassDescriptor) it.next();
            fields = temp.getFieldDescriptions();
            if (fields != null)
            {
                for (int i = 0; i < fields.length; i++)
                {
                    /*
                    MBAIRD
                    hashmap will only allow one entry per unique key,
                    so no need to check contains(fields[i].getColumnName()).
                    arminw:
                    use contains to avoid overriding of target class fields by the same
                    field-descriptor of other classes mapped to the same DB table, because
                    the other class can use e.g. different FieldConversion.
                    In #getClassesMappedToSameTable(...) we make sure that target
                    class has first position in list.
                     */
                    final String columnName = fields[i].getColumnName();
                    if (!visitedColumns.contains(columnName))
                    {
                        visitedColumns.add(columnName);
                        allFieldDescriptors.add(fields[i]);
                    }
                }
            }
        }
        FieldDescriptor[] retval = new FieldDescriptor[allFieldDescriptors.size()];
        allFieldDescriptors.toArray(retval);
        return retval;
    }

    private List getClassesMappedToSameTable(ClassDescriptor targetCld)
    {
        /*
        try to find an extent that contains clazz
        clone map to avoid synchronization problems, because another thread
        can do a put(..) operation on descriptor table
        */
        Iterator iter = ((HashMap)descriptorTable.clone()).values().iterator();
        List retval = new ArrayList();
        // make sure that target class is at first position
        retval.add(targetCld);
        while (iter.hasNext())
        {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            if (cld.getFullTableName() != null)
            {
                if (cld.getFullTableName().equals(targetCld.getFullTableName())
                        && !targetCld.getClassOfObject().equals(cld.getClassOfObject()))
                {
                    retval.add(cld);
                }
            }
        }
        return retval;
    }

    public Map getDescriptorTable()
    {
        return descriptorTable;
    }

    /**
     * Return the first found concrete class {@link ClassDescriptor}.
     * This means a class which is not an interface or an abstract class.
     * If given class descriptor is a concrete class, given class descriptor
     * was returned. If no concrete class can be found <code>null</code> will be
     * returned.
     */
    public ClassDescriptor findFirstConcreteClass(ClassDescriptor cld)
    {
        if(m_firstConcreteClassMap == null)
        {
            m_firstConcreteClassMap = new HashMap();
        }
        ClassDescriptor result = (ClassDescriptor) m_firstConcreteClassMap.get(cld.getClassNameOfObject());
        if (result == null)
        {
            if(cld.isInterface() || cld.isAbstract())
            {
                if(cld.isExtent())
                {
                    List extents = cld.getExtentClasses();
                    for (int i = 0; i < extents.size(); i++)
                    {
                        Class ext = (Class) extents.get(i);
                        result = findFirstConcreteClass(getDescriptorFor(ext));
                        if(result != null) break;
                    }
                }
                else
                {
                    LoggerFactory.getDefaultLogger().error("["+this.getClass().getName()+"] Found interface/abstract class" +
                            " in metadata declarations without concrete class: "+cld.getClassNameOfObject());
                }
                m_firstConcreteClassMap.put(cld.getClassNameOfObject(), result);
            }
            else
            {
                result = cld;
            }
        }
        return result;
    }

    /**
     *
     * Utility method to discover all concrete subclasses of a given super class. <br>
     * This method was introduced in order to get Extent Aware Iterators.
     *
     * @return a Collection of ClassDescriptor objects
     */
    public Collection getAllConcreteSubclassDescriptors(ClassDescriptor aCld)
    {
        if(m_allConcreteSubClass == null)
        {
            m_allConcreteSubClass = new HashMap();
        }
        Collection concreteSubclassClds = (Collection) m_allConcreteSubClass.get(aCld.getClassOfObject());

        if (concreteSubclassClds == null)
        {
            // BRJ: As long as we do not have an ordered Set
            // duplicates have to be prevented manually.
            // a HashSet should not be used because the order is unpredictable
            concreteSubclassClds = new ArrayList();
            Iterator iter = aCld.getExtentClasses().iterator();

            while (iter.hasNext())
            {
                Class extentClass = (Class) iter.next();
                ClassDescriptor extCld = getDescriptorFor(extentClass);
                if (aCld.equals(extCld))
                {
                    // prevent infinite recursion caused by cyclic references
                    continue;
                }
                if (!extCld.isInterface() && !extCld.isAbstract())
                {
                    if (!concreteSubclassClds.contains(extCld))
                    {
                        concreteSubclassClds.add(extCld);
                    }
                }

                // recurse
                Iterator subIter = getAllConcreteSubclassDescriptors(extCld).iterator();
                while (subIter.hasNext())
                {
                    ClassDescriptor subCld = (ClassDescriptor)subIter.next();
                    if (!concreteSubclassClds.contains(subCld))
                    {
                        concreteSubclassClds.add(subCld);
                    }
                }
            }
            m_allConcreteSubClass.put(aCld.getClassOfObject(), concreteSubclassClds);
        }

        return concreteSubclassClds;
    }


    /**
     * Checks if repository contains given class.
     */
    public boolean hasDescriptorFor(Class c)
    {
        return descriptorTable.containsKey(c.getName());
    }

    /**
     * lookup a ClassDescriptor in the internal Hashtable
     * @param strClassName a fully qualified class name as it is returned by Class.getName().
     */
    public ClassDescriptor getDescriptorFor(String strClassName) throws ClassNotPersistenceCapableException
    {
        ClassDescriptor result = discoverDescriptor(strClassName);
        if (result == null)
        {
            throw new ClassNotPersistenceCapableException(strClassName + " not found in OJB Repository");
        }
        else
        {
            return result;
        }
    }

    /**
     * lookup a ClassDescriptor in the internal Hashtable
     */
    public ClassDescriptor getDescriptorFor(Class c) throws ClassNotPersistenceCapableException
    {
        return this.getDescriptorFor(c.getName());
    }

    /**
     * Convenience for {@link #put(Class c, ClassDescriptor cld)}
     */
    public void setClassDescriptor(ClassDescriptor cld)
    {
        this.put(cld.getClassNameOfObject(), cld);
    }

    /**
     * Add a ClassDescriptor to the internal Hashtable<br>
     * Set the Repository for ClassDescriptor
     */
    public void put(Class c, ClassDescriptor cld)
    {
        this.put(c.getName(), cld);
    }

    /**
     * Add a ClassDescriptor to the internal Hashtable<br>
     * Set the Repository for ClassDescriptor
     */
    public void put(String classname, ClassDescriptor cld)
    {
        cld.setRepository(this); // BRJ
        synchronized (descriptorTable)
        {
            descriptorTable.put(classname, cld);
            List extentClasses = cld.getExtentClasses();
            for (int i = 0; i < extentClasses.size(); ++i)
            {
                addExtent(((Class) extentClasses.get(i)).getName(), cld);
            }
            changeDescriptorEvent();
        }
    }

    public void remove(String className)
    {
        synchronized (descriptorTable)
        {
            ClassDescriptor cld = (ClassDescriptor) descriptorTable.remove(className);
            if(cld != null)
            {
                // class itself could no longer be a extent
                Iterator it = descriptorTable.values().iterator();
                while (it.hasNext())
                {
                    ((ClassDescriptor) it.next()).removeExtentClass(className);
                }
                removeExtent(className);
                List extentClasses = cld.getExtentClasses();
                for (int i = 0; i < extentClasses.size(); ++i)
                {
                    removeExtent(((Class) extentClasses.get(i)).getName());
                }
                changeDescriptorEvent();
                // deregister classes using mapping of classes to multiple joined tables
                // the registration is done by the class-descriptor itself
                deregisterSuperClassMultipleJoinedTables(cld);
            }
        }
    }

    public void remove(Class clazz)
    {
        remove(clazz.getName());
    }

    private synchronized void changeDescriptorEvent()
    {
        m_multiMappedTableMap = null;
        m_topLevelClassTable = null;
        m_firstConcreteClassMap = null;
        m_allConcreteSubClass = null;
    }

    /**
     * Returns an iterator over all managed {@link ClassDescriptor}.
     */
    public Iterator iterator()
    {
        /*
        clone map to avoid synchronization problems
        */
        return ((HashMap)descriptorTable.clone()).values().iterator();
    }

    /**
     * Returns the defaultIsolationLevel.
     * @return int
     */
    public int getDefaultIsolationLevel()
    {
        return defaultIsolationLevel;
    }

    /**
     * Sets the defaultIsolationLevel.
     * @param defaultIsolationLevel The defaultIsolationLevel to set
     */
    public void setDefaultIsolationLevel(int defaultIsolationLevel)
    {
        this.defaultIsolationLevel = defaultIsolationLevel;
    }

    /**
     * returns a string representation
     */
    public String toString()
    {
        /**
         * Kuali Foundation modification -- 6/19/2009
         */
    	synchronized (descriptorTable) {			
        /**
         * End of Kuali Foundation modification
         */
	        Iterator it = descriptorTable.entrySet().iterator();
	        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
	        String className = "class name: ";
	        String tableName = "> table name: ";
	        while (it.hasNext())
	        {
	            Map.Entry me = (Map.Entry) it.next();
	            ClassDescriptor descriptor = (ClassDescriptor) me.getValue();
	            buf.append(className + me.getKey() + " =", tableName + descriptor.getFullTableName());
	        }
	        return buf.toString();
        /**
         * Kuali Foundation modification -- 6/19/2009
         */
		}
        /**
         * End of Kuali Foundation modification
         */
    }

    /*
     * @see XmlCapable#toXML()
     */
    public String toXML()
    {
        String eol = SystemUtils.LINE_SEPARATOR;
        StringBuffer buf = new StringBuffer();

        // write all ClassDescriptors
        Iterator i = this.iterator();
        while (i.hasNext())
        {
            buf.append(((XmlCapable) i.next()).toXML() + eol);
        }
        return buf.toString();
    }

    /**
     * returns IsolationLevel literal as matching
     * to the corresponding id
     * @return the IsolationLevel literal
     */
    protected String getIsolationLevelAsString()
    {
        if (defaultIsolationLevel == IL_READ_UNCOMMITTED)
        {
            return LITERAL_IL_READ_UNCOMMITTED;
        }
        else if (defaultIsolationLevel == IL_READ_COMMITTED)
        {
            return LITERAL_IL_READ_COMMITTED;
        }
        else if (defaultIsolationLevel == IL_REPEATABLE_READ)
        {
            return LITERAL_IL_REPEATABLE_READ;
        }
        else if (defaultIsolationLevel == IL_SERIALIZABLE)
        {
            return LITERAL_IL_SERIALIZABLE;
        }
        else if (defaultIsolationLevel == IL_OPTIMISTIC)
        {
            return LITERAL_IL_OPTIMISTIC;
        }
        return LITERAL_IL_READ_UNCOMMITTED;
    }

    /**
     * Starts by looking to see if the <code>className</code> is
     * already mapped specifically to the descritpor repository.
     * If the <code>className</code> is not specifically mapped we
     * look at the <code>className</code>'s parent class for a mapping.
     * We do this until the parent class is of the type
     * <code>java.lang.Object</code>.  If no mapping was found,
     * <code>null</code> is returned.  Mappings successfuly discovered
     * through inheritence are added to the internal table of
     * class descriptors to improve performance on subsequent requests
     * for those classes.
     *
     * <br/>
     * author <a href="mailto:weaver@apache.org">Scott T. Weaver</a>
     * @param className name of class whose descriptor we need to find.
     * @return ClassDescriptor for <code>className</code> or <code>null</code>
     * if no ClassDescriptor could be located.
     */
    protected ClassDescriptor discoverDescriptor(String className)
    {
        ClassDescriptor result = (ClassDescriptor) descriptorTable.get(className);
        if (result == null)
        {
            Class clazz;
            try
            {
                clazz = ClassHelper.getClass(className, true);
            }
            catch (ClassNotFoundException e)
            {
                throw new OJBRuntimeException("Class, " + className + ", could not be found.", e);
            }
            result = discoverDescriptor(clazz);
         }
        return result;
    }

    /**
     * Internal method for recursivly searching for a class descriptor that avoids
     * class loading when we already have a class object.
     *
     * @param clazz The class whose descriptor we need to find
     * @return ClassDescriptor for <code>clazz</code> or <code>null</code>
     *         if no ClassDescriptor could be located.
     */
    private ClassDescriptor discoverDescriptor(Class clazz)
    {
        ClassDescriptor result = (ClassDescriptor) descriptorTable.get(clazz.getName());

        if (result == null)
        {
            Class superClass = clazz.getSuperclass();
            // only recurse if the superClass is not java.lang.Object
            if (superClass != null)
            {
                result = discoverDescriptor(superClass);
            }
            if (result == null)
            {
                // we're also checking the interfaces as there could be normal
                // mappings for them in the repository (using factory-class,
                // factory-method, and the property field accessor)
                Class[] interfaces = clazz.getInterfaces();

                if ((interfaces != null) && (interfaces.length > 0))
                {
                    for (int idx = 0; (idx < interfaces.length) && (result == null); idx++)
                    {
                        result = discoverDescriptor(interfaces[idx]);
                    }
                }
            }

            if (result != null)
            {
                /**
                 * Kuali Foundation modification -- 6/19/2009
                 */
            	synchronized (descriptorTable) {
                /**
                 * End of Kuali Foundation modification
                 */
            		descriptorTable.put(clazz.getName(), result);
                /**
                 * Kuali Foundation modification -- 6/19/2009
                 */
            	}
                /**
                 * End of Kuali Foundation modification
                 */
            }
        }
        return result;
    }

    /**
     * Internal used! Register sub-classes of specified class when mapping class to
     * multiple joined tables is used. Normally this method is called by the {@link ClassDescriptor}
     * itself.
     *
     * @param cld The {@link ClassDescriptor} of the class to register.
     */
    protected void registerSuperClassMultipleJoinedTables(ClassDescriptor cld)
    {
        /*
        arminw: Sadly, we can't map to sub class-descriptor, because it's not guaranteed
        that they exist when this method is called. Thus we map the class instance instead
        of the class-descriptor.
        */
        if(cld.getBaseClass() != null)
        {
            try
            {
                Class superClass = ClassHelper.getClass(cld.getBaseClass());
                Class currentClass = cld.getClassOfObject();
                synchronized(descriptorTable)
                {
                    List subClasses = (List) superClassMultipleJoinedTablesMap.get(superClass);
                    if(subClasses == null)
                    {
                        subClasses = new ArrayList();
                        superClassMultipleJoinedTablesMap.put(superClass,  subClasses);
                    }
                    if(!subClasses.contains(currentClass))
                    {
                        if(log.isDebugEnabled())
                        {
                            log.debug("(MultipleJoinedTables): Register sub-class '" + currentClass
                                    + "' for class '" + superClass);
                        }
                        subClasses.add(currentClass);
                    }
                }
            }
            catch(Exception e)
            {
                throw new MetadataException("Can't register super class '" + cld.getBaseClass()
                        + "' for class-descriptor: " + cld, e);
            }
        }
    }

    /**
     * Internal used! Deregister sub-classes of specified class when mapping to multiple joined tables
     * is used. Normally this method is called when {@link #remove(Class)} a class.
     *
     * @param cld The {@link ClassDescriptor} of the class to register.
     */
    protected void deregisterSuperClassMultipleJoinedTables(ClassDescriptor cld)
    {
        try
        {
            Class currentClass = cld.getClassOfObject();
            synchronized(descriptorTable)
            {
                // first remove registered sub-classes for current class
                List subClasses = (List) superClassMultipleJoinedTablesMap.remove(currentClass);
                if(subClasses != null && log.isDebugEnabled())
                {
                    log.debug("(MultipleJoinedTables): Deregister class " + currentClass
                            + " with sub classes " + subClasses);
                }
                if(cld.getBaseClass() != null)
                {
                    // then remove sub-class entry of current class for super-class
                    Class superClass = ClassHelper.getClass(cld.getBaseClass());
                    subClasses = (List) superClassMultipleJoinedTablesMap.get(superClass);
                    if(subClasses != null)
                    {
                        boolean removed = subClasses.remove(currentClass);
                        if(removed && log.isDebugEnabled())
                        {
                            log.debug("(MultipleJoinedTables): Remove sub-class entry '" + currentClass
                            + "' in mapping for class '" + superClass + "'");
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
            throw new MetadataException("Can't deregister super class '" + cld.getBaseClass()
                    + "' for class-descriptor: " + cld, e);
        }
    }

    /**
     * Return <em>sub-classes</em> of the specified class using the
     * <em>"super"-Reference</em> concept.
     * @param cld The {@link ClassDescriptor} of the class to search for sub-classes.
     * @param wholeTree If set <em>true</em>, the whole sub-class tree of the specified
     * class will be returned. If <em>false</em> only the direct sub-classes of the specified class
     * will be returned.
     * @return An array of <em>sub-classes</em> for the specified class.
     */
    public Class[] getSubClassesMultipleJoinedTables(ClassDescriptor cld, boolean wholeTree)
    {
        ArrayList result = new ArrayList();
        createResultSubClassesMultipleJoinedTables(result, cld, wholeTree);
        return (Class[]) result.toArray(new Class[result.size()]);
    }

    /**
     * Add all sub-classes using multiple joined tables feature for specified class.
     * @param result The list to add results.
     * @param cld The {@link ClassDescriptor} of the class to search for sub-classes.
     * @param wholeTree If set <em>true</em>, the whole sub-class tree of the specified
     * class will be returned. If <em>false</em> only the direct sub-classes of the specified class
     * will be returned.
     */
    private void createResultSubClassesMultipleJoinedTables(List result, ClassDescriptor cld, boolean wholeTree)
    {
        List tmp = (List) superClassMultipleJoinedTablesMap.get(cld.getClassOfObject());
        if(tmp != null)
        {
            result.addAll(tmp);
            if(wholeTree)
            {
                for(int i = 0; i < tmp.size(); i++)
                {
                    Class subClass = (Class) tmp.get(i);
                    ClassDescriptor subCld = getDescriptorFor(subClass);
                    createResultSubClassesMultipleJoinedTables(result, subCld, wholeTree);
                }
            }
        }
    }

    protected void finalize() throws Throwable
    {
        log.info("# finalize DescriptorRepository instance #");
        super.finalize();
    }
}
