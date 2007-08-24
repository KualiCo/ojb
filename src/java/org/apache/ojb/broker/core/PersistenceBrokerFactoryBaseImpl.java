package org.apache.ojb.broker.core;

/* Copyright 2003-2005 The Apache Software Foundation
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

import org.apache.ojb.broker.PBFactoryException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerInternal;
import org.apache.ojb.broker.accesslayer.ConnectionFactoryFactory;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;
import org.apache.ojb.broker.util.interceptor.InterceptorFactory;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * This is an base implementation of the {@link PersistenceBrokerFactoryIF}
 * interface. Each request ({@link PersistenceBrokerFactoryIF#createPersistenceBroker} or
 * {@link PersistenceBrokerFactoryIF#defaultPersistenceBroker} call) creates a new
 * {@link PersistenceBroker} instance. No pooling of broker instances is used.
 *
 * @see PersistenceBrokerFactoryDefaultImpl
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PersistenceBrokerFactoryBaseImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PersistenceBrokerFactoryBaseImpl implements PersistenceBrokerFactoryIF
{
    private static Logger log = LoggerFactory.getLogger(PersistenceBrokerFactoryBaseImpl.class);

    private Class implementationClass;
    private long instanceCount;

    public PersistenceBrokerFactoryBaseImpl()
    {
        configure(OjbConfigurator.getInstance().getConfigurationFor(null));
    }

    /**
     * @see PersistenceBrokerFactoryIF#setDefaultKey
     */
    public void setDefaultKey(PBKey key)
    {
        try
        {
            MetadataManager.getInstance().setDefaultPBKey(key);
        }
        catch (Exception e)
        {
            throw new PBFactoryException(e);
        }
    }

    /**
     * @see PersistenceBrokerFactoryIF#getDefaultKey()
     */
    public PBKey getDefaultKey()
    {
        return MetadataManager.getInstance().getDefaultPBKey();
    }

    /**
     * For internal use! This method creates real new PB instances
     */
    protected PersistenceBrokerInternal createNewBrokerInstance(PBKey key) throws PBFactoryException
    {
        if (key == null) throw new PBFactoryException("Could not create new broker with PBkey argument 'null'");
        // check if the given key really exists
        if (MetadataManager.getInstance().connectionRepository().getDescriptor(key) == null)
        {
            throw new PBFactoryException("Given PBKey " + key + " does not match in metadata configuration");
        }
        if (log.isEnabledFor(Logger.INFO))
        {
            // only count created instances when INFO-Log-Level
            log.info("Create new PB instance for PBKey " + key +
                    ", already created persistence broker instances: " + instanceCount);
            // useful for testing
            ++this.instanceCount;
        }

        PersistenceBrokerInternal instance = null;
        Class[] types = {PBKey.class, PersistenceBrokerFactoryIF.class};
        Object[] args = {key, this};
        try
        {
            instance = (PersistenceBrokerInternal) ClassHelper.newInstance(implementationClass, types, args);
            OjbConfigurator.getInstance().configure(instance);
            instance = (PersistenceBrokerInternal) InterceptorFactory.getInstance().createInterceptorFor(instance);
        }
        catch (Exception e)
        {
            log.error("Creation of a new PB instance failed", e);
            throw new PBFactoryException("Creation of a new PB instance failed", e);
        }
        return instance;
    }

    /**
     * Always return a new created {@link PersistenceBroker} instance
     *
     * @param pbKey
     * @return
     * @throws PBFactoryException
     */
    public PersistenceBrokerInternal createPersistenceBroker(PBKey pbKey) throws PBFactoryException
    {
        if (log.isDebugEnabled()) log.debug("Obtain broker from pool, used PBKey is " + pbKey);

        /*
        try to find a valid PBKey, if given key does not full match
        */
        pbKey = BrokerHelper.crossCheckPBKey(pbKey);

        try
        {
            return createNewBrokerInstance(pbKey);

        }
        catch (Exception e)
        {
            throw new PBFactoryException("Borrow broker from pool failed, using PBKey " + pbKey, e);
        }
    }

    /**
     * @see PersistenceBrokerFactoryIF#createPersistenceBroker(
            * String jcdAlias, String user, String password)
     */
    public PersistenceBrokerInternal createPersistenceBroker(String jcdAlias, String user, String password)
            throws PBFactoryException
    {
        return this.createPersistenceBroker(new PBKey(jcdAlias, user, password));
    }

    /**
     * @see PersistenceBrokerFactoryIF#createPersistenceBroker(PBKey key)
     */
    public PersistenceBrokerInternal defaultPersistenceBroker() throws PBFactoryException
    {
        if (getDefaultKey() == null) throw new PBFactoryException("There was no 'default-connection' attribute" +
                " enabled in the jdbc connection descriptor");
        return this.createPersistenceBroker(getDefaultKey());
    }

    /*
     * @see org.apache.ojb.broker.util.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration config) throws ConfigurationException
    {
        implementationClass = ((PersistenceBrokerConfiguration) config).getPersistenceBrokerClass();
    }

    /**
     * @see PersistenceBrokerFactoryIF#releaseAllInstances()
     */
    public synchronized void releaseAllInstances()
    {
        instanceCount = 0;
    }

    /**
     * Not implemented!
     *
     * @return always 0
     */
    public int activePersistenceBroker()
    {
        return 0;
    }

    public void shutdown()
    {
        try
        {
            ConnectionFactoryFactory.getInstance().createConnectionFactory().releaseAllResources();
            PersistenceBrokerThreadMapping.shutdown();
            MetadataManager.getInstance().shutdown();
        }
        catch(RuntimeException e)
        {
            log.error("Error while shutdown of OJB", e);
            throw e;
        }
    }
}
