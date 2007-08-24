package org.apache.ojb.broker;

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

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ClassNotPersistenceCapableException;
import org.apache.ojb.broker.core.ValueContainer;
import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.ArrayUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Represents the identity of an object.
 * <br/>
 * It's composed of:
 * <ul>
 * <li>
 * class of the real object
 * </li>
 * <li>
 * top-level class of the real object (could be an abstract class or interface or the
 * class of the object itself), used to make an object unique across extent classes
 * </li>
 * <li>
 * an array of all primary key value objects
 * </li>
 * <li>
 * a flag which indicates whether this is a <em>transient Identity</em>
 * (identity of a non-persistent, "new" object) or a <em>persistent Identity</em> (identity object
 * of a persistent, "already written to datastore" object).
 * </li>
 * </ul>
 * <p>
 * To create <code>Identity</code> objects it's strongly recommended to use the {@link IdentityFactory}, because
 * in future releases of OJB the <code>Identity</code> constructors will be no longer reachable or forbidden to use.
 * </p>
 * <p>
 * NOTE: An <em>Identity</em> object must be unique
 * accross extents. Means all objects with the same top-level class need unique
 * PK values.
 * </p>
 * @see org.apache.ojb.broker.IdentityFactory

 * @author Thomas Mahler
 * @version $Id: Identity.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class Identity implements Serializable
{
    /** Unique id for serialization purposes. */ 
    private static final long serialVersionUID = 3182285550574178710L;

    private static final int IS_TRANSIENT = 3;
    private static final int IS_PERMANENT = 17;
    /**
     * Used for hashCode calculation.
     */
    private static final int iConstant = 37;

    /**
     * The top-level Class of the identified object, ie. an interface.
     */
    private Class m_objectsTopLevelClass;

    /**
     * The real Class of the identified object, ie. the implementing class.
     */
    private Class m_objectsRealClass = null;

    /**
     * The ordered list of primary key values maintaining the objects identity in the underlying RDBMS.
     */
    private Object[] m_pkValues;

    private final int isTransient;

    /*
    the hashcode of different objects has to be unique across different
    JVM and have to be the same for the same object in different JVM.

    In distributed enviroments the Identity object have to recalculate the
    hashCode and toString values, because the hash code of the Class object
    differs in different JVM
    */
    private transient String m_stringRepresentation = null;
    private transient Integer m_hashCode;

    /**
     * For internal use only!
     */
    protected Identity()
    {
        isTransient = IS_TRANSIENT;
    }

    /**
     * For internal use only!. Creates an em from a class and the objects primary key values.
     * used for the definition of proxies.
     * <br/>
     * OJB user have to use {@link IdentityFactory} to create object identity.
     *
     *
     * @param realClass the concrete class of the object, or null if not known.
     * @param topLevel the highest persistence-capable class or
     * interface (in the inheritance hierarchy) that the identified object is an instance of
     * @param pkValues (unique across the extents !)
     * @param isTransient If <em>true</em>
     */
    public Identity(final Class realClass, final Class topLevel, final Object[] pkValues, final boolean isTransient)
    {
        m_objectsTopLevelClass = topLevel;
        m_objectsRealClass = realClass;
        m_pkValues = pkValues;
        this.isTransient = isTransient ? IS_TRANSIENT : IS_PERMANENT;
        checkForPrimaryKeys(null);
    }

    /**
     * For internal use only! Creates an Identity from a class and the objects primary key values.
     * used for the definition of proxies.
     * <br/>
     * OJB user have to use {@link IdentityFactory} to create object identity.
     *
     * @param realClass the concrete class of the object, or null if not known.
     * @param topLevel the highest persistence-capable class or
     * interface (in the inheritance hierarchy) that the identified object is an instance of
     * @param pkValues (unique across the extents !)
     */
    public Identity(final Class realClass, final Class topLevel, final Object[] pkValues)
    {
        m_objectsTopLevelClass = topLevel;
        m_objectsRealClass = realClass;
        m_pkValues = pkValues;
        this.isTransient = IS_PERMANENT;
        checkForPrimaryKeys(null);
    }

    /**
     * Constructor for internal use. Use {@link IdentityFactory} to create an object identity.
     * 
     * @param objectToIdentitify The object for which to create the identity
     * @param targetBroker       The persistence broker
     */
    public Identity(final Object objectToIdentitify, final PersistenceBroker targetBroker)
    {
        this.isTransient = IS_PERMANENT;
        init(objectToIdentitify, targetBroker, null);
    }

    /**
     * Constructor for internal use. Use {@link IdentityFactory} to create an object identity.
     * 
     * @param objectToIdentitify The object for which to create the identity
     * @param targetBroker       The persistence broker
     * @param cld                The class descriptor
     */
    public Identity(final Object objectToIdentitify, final PersistenceBroker targetBroker, final ClassDescriptor cld)
    {
        this.isTransient = IS_PERMANENT;
        init(objectToIdentitify, targetBroker, cld);
    }

    private void init(final Object objectToIdentify, final PersistenceBroker targetBroker, ClassDescriptor cld)
    {
        if(objectToIdentify == null) throw new OJBRuntimeException("Can't create Identity for 'null'-object");
        try
        {
            final IndirectionHandler handler = ProxyHelper.getIndirectionHandler(objectToIdentify);

            synchronized(objectToIdentify)
            {
                if (handler != null)
                {
                    final Identity sourceOID = handler.getIdentity();
                    m_objectsTopLevelClass = sourceOID.m_objectsTopLevelClass;
                    m_objectsRealClass = sourceOID.m_objectsRealClass;
                    m_pkValues = sourceOID.m_pkValues;
                }
                else
                {
                    if (cld == null)
                    {
                        cld = targetBroker.getClassDescriptor(objectToIdentify.getClass());
                    }

                    // identities must be unique accross extents !
                    m_objectsTopLevelClass = targetBroker.getTopLevelClass(objectToIdentify.getClass());
                    m_objectsRealClass = objectToIdentify.getClass();

                    // BRJ: definitely do NOT convertToSql
                    // conversion is done when binding the sql-statement
                    final BrokerHelper helper = targetBroker.serviceBrokerHelper();
                    final ValueContainer[] pkValues = helper.getValuesForObject(cld.getPkFields(), objectToIdentify, false, true);
                    if (pkValues == null || pkValues.length == 0)
                    {
                        throw createException("Can't extract PK value fields", objectToIdentify, null);
                    }
                    m_pkValues = helper.extractValueArray(pkValues);
                }
            }

            checkForPrimaryKeys(objectToIdentify);
        }
        catch (ClassNotPersistenceCapableException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw createException("Can not init Identity for given object.", objectToIdentify, e);
        }
    }

    /**
     * Factory method that returns an Identity object created from a serializated representation.
     * 
     * @param anArray The serialized representation
     * @return The identity
     * @see {@link #serialize}.
     * @deprecated
     */
    public static Identity fromByteArray(final byte[] anArray) throws PersistenceBrokerException
    {
        // reverse of the serialize() algorithm:
        // read from byte[] with a ByteArrayInputStream, decompress with
        // a GZIPInputStream and then deserialize by reading from the ObjectInputStream
        try
        {
            final ByteArrayInputStream bais = new ByteArrayInputStream(anArray);
            final GZIPInputStream gis = new GZIPInputStream(bais);
            final ObjectInputStream ois = new ObjectInputStream(gis);
            final Identity result = (Identity) ois.readObject();
            ois.close();
            gis.close();
            bais.close();
            return result;
        }
        catch (Exception ex)
        {
            throw new PersistenceBrokerException(ex);
        }
    }

    /**
     * Determines whether the identity is transient.
     * 
     * @return <code>true</code> if the identity is transient
     */
    public boolean isTransient()
    {
        return isTransient == IS_TRANSIENT;
    }

    /**
     * Returns the top-level class of the real subject (base class,
     * base interface denoted in the repository or
     * objects real class if no top-level was found).
     *
     * @return The top level class
     */
    public Class getObjectsTopLevelClass()
    {
        return m_objectsTopLevelClass;
    }

    /**
     * Return the "real" class of the real subject.
     * 
     * @return The real class
     */
    public Class getObjectsRealClass()
    {
        return m_objectsRealClass;
    }

    /**
     * Set the real class of the subject.
     * 
     * @param objectsRealClass The real class
     */
    public void setObjectsRealClass(final Class objectsRealClass)
    {
        this.m_objectsRealClass = objectsRealClass;
    }

    /**
     * Return the serialized form of this Identity.
     * 
     * @return The serialized representation
     * @see #fromByteArray
     * @deprecated
     */
    public byte[] serialize() throws PersistenceBrokerException
    {
        // Identity is serialized and written to an ObjectOutputStream
        // This ObjectOutputstream is compressed by a GZIPOutputStream
        // and finally written to a ByteArrayOutputStream.
        // the resulting byte[] is returned
        try
        {
            final ByteArrayOutputStream bao = new ByteArrayOutputStream();
            final GZIPOutputStream gos = new GZIPOutputStream(bao);
            final ObjectOutputStream oos = new ObjectOutputStream(gos);
            oos.writeObject(this);
            oos.close();
            gos.close();
            bao.close();
            return bao.toByteArray();
        }
        catch (Exception ignored)
        {
            throw new PersistenceBrokerException(ignored);
        }
    }

    /**
     * return a String representation.
     * @return java.lang.String
     */
    public String toString()
    {
        if (m_stringRepresentation == null)
        {
            final StringBuffer buf = new StringBuffer();
            buf.append(m_objectsTopLevelClass.getName());
            for (int i = 0; i < m_pkValues.length; i++)
            {
                buf.append((i == 0) ? "{" : ",");
                buf.append(m_pkValues[i]);
            }
            buf.append("}");
            if(isTransient == IS_TRANSIENT) buf.append("-transient");
            m_stringRepresentation = buf.toString();
        }
        return m_stringRepresentation;
    }


    /**
     * OJB can handle only classes that declare at least one primary key attribute,
     * this method checks this condition.
     * 
     * @param realObject The real object to check
     * @throws ClassNotPersistenceCapableException thrown if no primary key is specified for the objects class
     */
    protected void checkForPrimaryKeys(final Object realObject) throws ClassNotPersistenceCapableException
    {
        // if no PKs are specified OJB can't handle this class !
        if (m_pkValues == null || m_pkValues.length == 0)
        {
            throw createException("OJB needs at least one primary key attribute for class: ", realObject, null);
        }
// arminw: should never happen
//        if(m_pkValues[0] instanceof ValueContainer)
//            throw new OJBRuntimeException("Can't handle pk values of type "+ValueContainer.class.getName());
    }

    /**
     * Returns the primary key values of the real subject.
     * 
     * @return The pk values
     */
    public Object[] getPrimaryKeyValues()
    {
        return m_pkValues;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object obj)
    {
        if(this == obj) return true;

        boolean result = false;
        if (obj instanceof Identity)
        {
            final Identity id = (Identity) obj;
            result = m_objectsTopLevelClass.equals(id.m_objectsTopLevelClass) && isTransient == id.isTransient;
            if(result)
            {
                final Object[] otherPkValues = id.m_pkValues;
                result = m_pkValues.length == otherPkValues.length;
                if(result)
                {
                    for (int i = 0; result && i < m_pkValues.length; i++)
                    {
                        result = (m_pkValues[i] == null) ? (otherPkValues[i] == null)
                                : m_pkValues[i].equals(otherPkValues[i]);

                        // special treatment for byte[]
                        if (!result && m_pkValues[i] instanceof byte[] && otherPkValues[i] instanceof byte[])
                        {
                            result = Arrays.equals((byte[]) m_pkValues[i], (byte[]) otherPkValues[i]);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        /*
        arminw:
        identity is quasi immutable (toplevel class and PK fields
        never change), thus we can note hashCode
        */
        if(m_hashCode == null)
        {
            int iTotal = isTransient;
            Object obj;
            for (int i = 0; i < m_pkValues.length; i++)
            {
                obj = m_pkValues[i];
                if(obj instanceof byte[])
                {
                    iTotal = iTotal * iConstant + ((byte[]) obj).length;
            }
                else
                {
                    iTotal = iTotal * iConstant + (obj != null ? obj.hashCode() : 0);
        }
            }
            iTotal = iTotal * iConstant + m_objectsTopLevelClass.hashCode();
            m_hashCode = new Integer(iTotal);
        }
        return m_hashCode.intValue();
    }

    private ClassNotPersistenceCapableException createException(String msg, final Object objectToIdentify, final Exception e)
    {
        final String eol = SystemUtils.LINE_SEPARATOR;
        if(msg == null)
        {
            msg = "Unexpected error:";
        }
        if(e != null)
        {
            return new ClassNotPersistenceCapableException(msg + eol +
                        "objectTopLevelClass=" + (m_objectsTopLevelClass != null ? m_objectsTopLevelClass.getName() : null) + eol +
                        "objectRealClass=" + (m_objectsRealClass != null ? m_objectsRealClass.getName() : null) + eol +
                        "pkValues=" + (m_pkValues != null ? ArrayUtils.toString(m_pkValues) : null) +
                        (objectToIdentify != null ? (eol + "object to identify: " + objectToIdentify) : ""), e);
        }
        else
        {
            return new ClassNotPersistenceCapableException(msg + eol +
                        "objectTopLevelClass=" + (m_objectsTopLevelClass != null ? m_objectsTopLevelClass.getName() : null) + eol +
                        "objectRealClass=" + (m_objectsRealClass != null ? m_objectsRealClass.getName() : null) + eol +
                        "pkValues=" + (m_pkValues != null ? ArrayUtils.toString(m_pkValues) : null) +
                        eol + "object to identify: " + objectToIdentify);
        }
    }
}