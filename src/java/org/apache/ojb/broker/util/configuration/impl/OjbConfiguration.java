package org.apache.ojb.broker.util.configuration.impl;

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

import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.cache.ObjectCache;
import org.apache.ojb.broker.cache.ObjectCacheDefaultImpl;
import org.apache.ojb.broker.core.PBPoolConfiguration;
import org.apache.ojb.broker.core.PersistenceBrokerConfiguration;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.IndirectionHandlerJDKImpl;
import org.apache.ojb.broker.core.proxy.ListProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.ProxyConfiguration;
import org.apache.ojb.broker.core.proxy.ProxyFactory;
import org.apache.ojb.broker.core.proxy.ProxyFactoryJDKImpl;
import org.apache.ojb.broker.core.proxy.SetProxyDefaultImpl;
import org.apache.ojb.broker.metadata.MetadataConfiguration;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldDirectImpl;
import org.apache.ojb.broker.util.pooling.PoolConfiguration;
import org.apache.ojb.odmg.OdmgConfiguration;
import org.apache.ojb.odmg.collections.DListImpl;

/**
 * This class contains the runtime configuration of the OJB
 * system. This Configuration is read in only once at application startup.
 * Changes to the OJB.properties file during execution are <b>not</b>
 * reflected back into the application!
 *
 * @author Thomas Mahler
 * @version $Id: OjbConfiguration.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class OjbConfiguration extends    ConfigurationAbstractImpl
                              implements OdmgConfiguration,
                                         PersistenceBrokerConfiguration,
                                         ProxyConfiguration,
                                         PBPoolConfiguration,
                                         MetadataConfiguration
{
    /** Default filename of the OJB properties file */
    public static final String OJB_PROPERTIES_FILE = "OJB.properties";
    /** Default filename of the OJB repository metadata file */
    public static final String OJB_METADATA_FILE   = "repository.xml";

    /** the repository file keeping the O/R Metadata*/
    private String repositoryFilename;
    private Class objectCacheClass;
    private Class persistentFieldClass;
    private Class persistenceBrokerClass;

    // proxy related classes
    private Class listProxyClass;
    private Class setProxyClass;
    private Class collectionProxyClass;
    private Class indirectionHandlerClass;
    private Class proxyFactoryClass;

    // limit for number of values in SQL IN Statement
    private int sqlInLimit;

    // PB pooling configuration
    private int maxActive;
    private int maxIdle;
    private long maxWait;
    private long timeBetweenEvictionRunsMillis;
    private long minEvictableIdleTimeMillis;
    private byte whenExhaustedAction;

    // ODMG configuration
    private boolean useImplicitLocking;
    private boolean lockAssociationAsWrites;
    private Class oqlCollectionClass;

    // Metadata configuration
    private boolean useSerializedRepository;



    public OjbConfiguration()
    {
        super();
    }

    public boolean useSerializedRepository()
    {
        return useSerializedRepository;
    }

    public boolean lockAssociationAsWrites()
    {
        return lockAssociationAsWrites;
    }

    public String getRepositoryFilename()
    {
        return repositoryFilename;
    }

    //*************************************************************
    //PBPoolConfiguration methods
    public int getMaxActive()
    {
        return maxActive;
    }

    public int getMaxIdle()
    {
        return maxIdle;
    }

    public long getMaxWaitMillis()
    {
        return maxWait;
    }

    public long getTimeBetweenEvictionRunsMilli()
    {
        return timeBetweenEvictionRunsMillis;
    }

    public long getMinEvictableIdleTimeMillis()
    {
        return minEvictableIdleTimeMillis;
    }

    public byte getWhenExhaustedAction()
    {
        return whenExhaustedAction;
    }
    //*************************************************************


    //*************************************************************

    public Class getObjectCacheClass()
    {
        return objectCacheClass;
    }

    public Class getOqlCollectionClass()
    {
        return oqlCollectionClass;
    }

    public Class getPersistentFieldClass()
    {
        return persistentFieldClass;
    }

    public Class getPersistenceBrokerClass()
    {
        return persistenceBrokerClass;
    }

    /**
     * Returns the indirection handler implementation class.
     * 
     * @return The indirection handler class
     * @see org.apache.ojb.broker.core.proxy.IndirectionHandler
     */
    public Class getIndirectionHandlerClass()
    {
        return indirectionHandlerClass;
    }
    
    /**
     * Returns the proxy class used for that implement the {@link java.util.List} interface.
     * 
     * @return The proxy class 
     * @see org.apache.ojb.broker.core.proxy.ProxyFactory#setListProxyClass(Class)
     */
    public Class getListProxyClass()
    {
        return listProxyClass;
    }

    /**
     * Returns the proxy class used for that implement the {@link java.util.Set} interface.
     * 
     * @return The proxy class 
     * @see org.apache.ojb.broker.core.proxy.ProxyFactory#setSetProxyClass(Class)
     */
    public Class getSetProxyClass()
    {
        return setProxyClass;
    }

    /**
     * Returns the proxy class used for that implement the {@link java.util.Collection} interface.
     * 
     * @return The proxy class 
     * @see org.apache.ojb.broker.core.proxy.ProxyFactory#setCollectionProxyClass(Class)
     */
    public Class getCollectionProxyClass()
    {
        return collectionProxyClass;
    }
    
    
	/**
     * Returns the class that will be used as the proxy factory.
     * 
     * @return The proxy factory class 
     */
    public Class getProxyFactoryClass() {
        return proxyFactoryClass;
    }

    /**
     * Loads the configuration from file "OBJ.properties". If the system
     * property "OJB.properties" is set, then the configuration in that file is
     * loaded. Otherwise, the file "OJB.properties" is tried. If that is also
     * unsuccessful, then the configuration is filled with default values.
     */
    protected void load()
    {
        // properties file may be set as a System property.
        // if no property is set take default name.
        String fn = System.getProperty(OJB_PROPERTIES_FILE, OJB_PROPERTIES_FILE);
        setFilename(fn);
        super.load();

        // default repository & connection descriptor file
        repositoryFilename = getString("repositoryFile", OJB_METADATA_FILE);

        // object cache class
        objectCacheClass = getClass("ObjectCacheClass", ObjectCacheDefaultImpl.class, ObjectCache.class);

        // load PersistentField Class
        persistentFieldClass =
                getClass("PersistentFieldClass", PersistentFieldDirectImpl.class, PersistentField.class);

        // load PersistenceBroker Class
        persistenceBrokerClass =
                getClass("PersistenceBrokerClass", PersistenceBrokerImpl.class, PersistenceBroker.class);

        // load ListProxy Class
        listProxyClass = getClass("ListProxyClass", ListProxyDefaultImpl.class);

        // load SetProxy Class
        setProxyClass = getClass("SetProxyClass", SetProxyDefaultImpl.class);

        // load CollectionProxy Class
        collectionProxyClass = getClass("CollectionProxyClass", CollectionProxyDefaultImpl.class);

        // load IndirectionHandler Class
        indirectionHandlerClass =
            getClass("IndirectionHandlerClass", IndirectionHandlerJDKImpl.class, IndirectionHandler.class);
        
        // load ProxyFactory Class
        proxyFactoryClass =
            getClass("ProxyFactoryClass", ProxyFactoryJDKImpl.class, ProxyFactory.class);

        // load configuration for ImplicitLocking parameter:
        useImplicitLocking = getBoolean("ImplicitLocking", false);

        // load configuration for LockAssociations parameter:
        lockAssociationAsWrites = (getString("LockAssociations", "WRITE").equalsIgnoreCase("WRITE"));

        // load OQL Collection Class
        oqlCollectionClass = getClass("OqlCollectionClass", DListImpl.class, ManageableCollection.class);

        // set the limit for IN-sql , -1 for no limits
        sqlInLimit = getInteger("SqlInLimit", -1);

        //load configuration for PB pool
        maxActive = getInteger(PoolConfiguration.MAX_ACTIVE,
                PoolConfiguration.DEFAULT_MAX_ACTIVE);
        maxIdle = getInteger(PoolConfiguration.MAX_IDLE,
                PoolConfiguration.DEFAULT_MAX_IDLE);
        maxWait = getLong(PoolConfiguration.MAX_WAIT,
                PoolConfiguration.DEFAULT_MAX_WAIT);
        timeBetweenEvictionRunsMillis = getLong(PoolConfiguration.TIME_BETWEEN_EVICTION_RUNS_MILLIS,
                PoolConfiguration.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS);
        minEvictableIdleTimeMillis = getLong(PoolConfiguration.MIN_EVICTABLE_IDLE_TIME_MILLIS,
                PoolConfiguration.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS);
        whenExhaustedAction = getByte(PoolConfiguration.WHEN_EXHAUSTED_ACTION,
                PoolConfiguration.DEFAULT_WHEN_EXHAUSTED_ACTION);

        useSerializedRepository = getBoolean("useSerializedRepository", false);
    }

    /**
     * Returns the SQLInLimit.
     * @return int
     */
    public int getSqlInLimit()
    {
        return sqlInLimit;
    }

    /**
     * Sets the persistentFieldClass.
     * @param persistentFieldClass The persistentFieldClass to set
     */
    public void setPersistentFieldClass(Class persistentFieldClass)
    {
        this.persistentFieldClass = persistentFieldClass;
    }

    /**
     * @see org.apache.ojb.odmg.OdmgConfiguration#useImplicitLocking()
     */
    public boolean useImplicitLocking()
    {
        return useImplicitLocking;
    }

    public void setUseImplicitLocking(boolean implicitLocking)
    {
        this.useImplicitLocking = implicitLocking;
    }

}
