package org.apache.ojb.jdori.sql;
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

import java.util.HashMap;
import java.util.Properties;

import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.spi.PersistenceCapable;

import org.apache.ojb.broker.Identity;

import com.sun.jdori.StoreManager;
import com.sun.jdori.TranscriberFactory;
import com.sun.jdori.common.PersistenceManagerFactoryImpl;
import com.sun.jdori.common.PersistenceManagerImpl;

/**
 * PMF for OjbStore implementation
 * @author thma
 *
 */
public class OjbStorePMF extends PersistenceManagerFactoryImpl
{
    private final HashMap storeManagers = new HashMap();

    private static HashMap trackedClassesMap = getTrackedClasses();
    
    private static final OjbStorePMF INSTANCE = new OjbStorePMF();

	public static OjbStorePMF getInstance()
	{
		return INSTANCE;
	}

    /**
     * Constructor for PMF.
     */
    public OjbStorePMF()
    {
        super();
    }

    /**
     * Constructor for PMF.
     * @param URL
     * @param userName
     * @param password
     * @param driverName
     */
    public OjbStorePMF(String URL, String userName, String password, String driverName)
    {
        super(URL, userName, password, driverName);
    }

    private static HashMap getTrackedClasses()
    {
        HashMap classMap = new HashMap();
        // java.util.Date and java.sql classes:
        classMap.put(java.util.Date.class, com.sun.jdori.common.sco.Date.class);
        classMap.put(com.sun.jdori.common.sco.Date.class, com.sun.jdori.common.sco.Date.class);
        classMap.put(java.sql.Date.class, com.sun.jdori.common.sco.SqlDate.class);
        classMap.put(
            com.sun.jdori.common.sco.SqlDate.class,
            com.sun.jdori.common.sco.SqlDate.class);
        classMap.put(java.sql.Time.class, com.sun.jdori.common.sco.SqlTime.class);
        classMap.put(
            com.sun.jdori.common.sco.SqlTime.class,
            com.sun.jdori.common.sco.SqlTime.class);
        classMap.put(java.sql.Timestamp.class, com.sun.jdori.common.sco.SqlTimestamp.class);
        classMap.put(
            com.sun.jdori.common.sco.SqlTimestamp.class,
            com.sun.jdori.common.sco.SqlTimestamp.class);

        // java.util.Set
        classMap.put(java.util.HashSet.class, com.sun.jdori.common.sco.HashSet.class);
        classMap.put(java.util.AbstractSet.class, com.sun.jdori.common.sco.HashSet.class);
        classMap.put(java.util.Set.class, com.sun.jdori.common.sco.HashSet.class);
        classMap.put(
            com.sun.jdori.common.sco.HashSet.class,
            com.sun.jdori.common.sco.HashSet.class);

        // java.util.List
        classMap.put(java.util.ArrayList.class, com.sun.jdori.common.sco.ArrayList.class);
        classMap.put(java.util.AbstractList.class, com.sun.jdori.common.sco.ArrayList.class);
        classMap.put(java.util.List.class, com.sun.jdori.common.sco.ArrayList.class);
        classMap.put(java.util.AbstractCollection.class, com.sun.jdori.common.sco.ArrayList.class);
        classMap.put(java.util.Collection.class, com.sun.jdori.common.sco.ArrayList.class);
        classMap.put(
            com.sun.jdori.common.sco.ArrayList.class,
            com.sun.jdori.common.sco.ArrayList.class);

        // java.util.Vector
        classMap.put(java.util.Vector.class, com.sun.jdori.common.sco.Vector.class);
        classMap.put(com.sun.jdori.common.sco.Vector.class, com.sun.jdori.common.sco.Vector.class);

        // java.util.SortedSet
        classMap.put(java.util.TreeSet.class, com.sun.jdori.common.sco.TreeSet.class);
        classMap.put(java.util.SortedSet.class, com.sun.jdori.common.sco.TreeSet.class);
        classMap.put(
            com.sun.jdori.common.sco.TreeSet.class,
            com.sun.jdori.common.sco.TreeSet.class);

        // java.util.LinkedList
        classMap.put(java.util.LinkedList.class, com.sun.jdori.common.sco.LinkedList.class);
        classMap.put(
            java.util.AbstractSequentialList.class,
            com.sun.jdori.common.sco.LinkedList.class);
        classMap.put(
            com.sun.jdori.common.sco.LinkedList.class,
            com.sun.jdori.common.sco.LinkedList.class);

        // java.util.Map
        classMap.put(java.util.Map.class, com.sun.jdori.common.sco.HashMap.class);
        classMap.put(java.util.AbstractMap.class, com.sun.jdori.common.sco.HashMap.class);
        classMap.put(java.util.HashMap.class, com.sun.jdori.common.sco.HashMap.class);
        classMap.put(
            com.sun.jdori.common.sco.HashMap.class,
            com.sun.jdori.common.sco.HashMap.class);

        // java.util.Hashtable
        classMap.put(java.util.Hashtable.class, com.sun.jdori.common.sco.Hashtable.class);
        classMap.put(
            com.sun.jdori.common.sco.Hashtable.class,
            com.sun.jdori.common.sco.Hashtable.class);

        // java.util.SortedMap
        classMap.put(java.util.SortedMap.class, com.sun.jdori.common.sco.TreeMap.class);
        classMap.put(java.util.TreeMap.class, com.sun.jdori.common.sco.TreeMap.class);
        classMap.put(
            com.sun.jdori.common.sco.TreeMap.class,
            com.sun.jdori.common.sco.TreeMap.class);

        return classMap;
    }

    /**
     * @see com.sun.jdori.common.PersistenceManagerFactoryImpl#getOptionArray()
     */
    protected String[] getOptionArray()
    {
    	String[] optionsA = {""};
        return optionsA;
    }

    /**
     * @see com.sun.jdori.common.PersistenceManagerFactoryImpl#createPersistenceManager(String, String)
     */
    protected PersistenceManager createPersistenceManager(String aUserid, String aPassword)
    {

        PersistenceManager result = null;
        try
        {
            result = new PersistenceManagerImpl(this, aUserid, aPassword);
        }
        catch (JDOException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new OjbStoreFatalInternalException(
                getClass(),
                "createPersistenceManager(userid, password)",
                ex);
        }
        return result;
    }

    /**
     * @see com.sun.jdori.common.PersistenceManagerFactoryImpl#setPMFClassProperty(Properties)
     */
    protected void setPMFClassProperty(Properties props)
    {
        props.setProperty("javax.jdo.PersistenceManagerFactoryClass", this.getClass().getName());
    }

    /**
     * @see com.sun.jdori.common.PersistenceManagerFactoryImpl#encrypt(String)
     */
    protected String encrypt(String s)
    {
        return s;
    }

    /**
     * @see com.sun.jdori.common.PersistenceManagerFactoryImpl#decrypt(String)
     */
    protected String decrypt(String s)
    {
        return s;
    }

    /**
     * @see com.sun.jdori.common.PersistenceManagerFactoryImpl#setCFProperties(Properties)
     */
    protected void setCFProperties(Properties p)
    {
    }

    /**
     * @see com.sun.jdori.common.PersistenceManagerFactoryImpl#getCFFromProperties(Properties)
     */
    protected void getCFFromProperties(Properties p)
    {
    }

    /**
     * @see com.sun.jdori.common.PersistenceManagerFactoryImpl#isConnectionFactoryConfigured()
     */
    protected boolean isConnectionFactoryConfigured()
    {
        return true;
    }

    /**
     * @see com.sun.jdori.PersistenceManagerFactoryInternal#getTranscriberFactory()
     */
    public TranscriberFactory getTranscriberFactory()
    {
        return null;
    }

    /**
     * @see com.sun.jdori.PersistenceManagerFactoryInternal#getObjectIdClass(Class)
     */
    public Class getObjectIdClass(Class cls)
    {
        Class result = null;
        if (null != cls && PersistenceCapable.class.isAssignableFrom(cls))
        {
            result = Identity.class;
        }
        return result;
    }

    /**
     * @see com.sun.jdori.PersistenceManagerFactoryInternal#getStoreManager(PersistenceManager)
     */
    public StoreManager getStoreManager(PersistenceManager pm)
    {
        OjbStoreManager result = null;
        try
        {
            result = (OjbStoreManager) storeManagers.get(pm);
            if (null == result)
            {
                result = new OjbStoreManager(this);
                storeManagers.put(pm, result);
            }
        }
        catch (JDOException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new OjbStoreFatalInternalException(getClass(), "getStoreManager", ex);
        }
        return result;
    }

    /**
     * @see com.sun.jdori.PersistenceManagerFactoryInternal#releaseStoreManager(PersistenceManager)
     */
    public void releaseStoreManager(PersistenceManager pm)
    {
        try
        {
            storeManagers.remove(pm);
        }
        catch (JDOException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new OjbStoreFatalInternalException(getClass(), "releaseStoreManager", ex);
        }
    }

    /**
     * @see com.sun.jdori.PersistenceManagerFactoryInternal#getTrackedClass(Class)
     */
    public Class getTrackedClass(Class type)
    {
        return (Class) trackedClassesMap.get(type);
    }

}
