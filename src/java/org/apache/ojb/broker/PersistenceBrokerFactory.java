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

import org.apache.ojb.broker.core.PersistenceBrokerFactoryFactory;
import org.apache.ojb.broker.util.configuration.Configurator;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;
import org.apache.ojb.broker.metadata.MetadataManager;

/**
 * Convenience factory class that produces {@link PersistenceBroker} instances.
 *
 * @see org.apache.ojb.broker.core.PersistenceBrokerFactoryFactory
 * @see org.apache.ojb.broker.core.PersistenceBrokerFactoryIF
 * @author Thomas Mahler
 * @version $Id: PersistenceBrokerFactory.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class PersistenceBrokerFactory
{
    /**
     * Returns the {@link org.apache.ojb.broker.util.configuration.Configurator}
     * object.
     *
     * @return The configurator
     */
    public static Configurator getConfigurator()
    {
        return OjbConfigurator.getInstance();
    }

    /**
     * Sets the key that specifies the default persistence manager.
     * 
     * @param key The default broker key
     * @see org.apache.ojb.broker.core.PersistenceBrokerFactoryIF#setDefaultKey
     */
    public static void setDefaultKey(PBKey key)
    {
        MetadataManager.getInstance().setDefaultPBKey(key);
    }

    /**
     * Returns the key that specifies the default persistence manager.
     * 
     * @return The default broker key
     * @see org.apache.ojb.broker.core.PersistenceBrokerFactoryIF#getDefaultKey
     */
    public static PBKey getDefaultKey()
    {
        return MetadataManager.getInstance().getDefaultPBKey();
    }

    /**
     * Creates a default broker instance for the default broker key.
     * 
     * @return The persistence broker
     * @see org.apache.ojb.broker.core.PersistenceBrokerFactoryIF#defaultPersistenceBroker
     */
    public static PersistenceBroker defaultPersistenceBroker() throws PBFactoryException
    {
        return PersistenceBrokerFactoryFactory.instance().
                defaultPersistenceBroker();
    }

    /**
     * Creates a new broker instance.
     * 
     * @param jcdAlias The jdbc connection descriptor name as defined in the repository
     * @param user     The user name to be used for connecting to the database
     * @param password The password to be used for connecting to the database
     * @return The persistence broker
     * @see org.apache.ojb.broker.core.PersistenceBrokerFactoryIF#createPersistenceBroker(java.lang.String, java.lang.String, java.lang.String)
     */
    public static PersistenceBroker createPersistenceBroker(String jcdAlias,
                                                            String user,
                                                            String password) throws PBFactoryException
    {
        return PersistenceBrokerFactoryFactory.instance().
                   createPersistenceBroker(jcdAlias, user, password);
    }

    /**
     * Creates a new broker instance for the given key.
     * 
     * @param key The broker key
     * @return The persistence broker
     * @see org.apache.ojb.broker.core.PersistenceBrokerFactoryIF#createPersistenceBroker(org.apache.ojb.broker.PBKey)
     */
    public static PersistenceBroker createPersistenceBroker(PBKey key) throws PBFactoryException
    {
        return PersistenceBrokerFactoryFactory.instance().createPersistenceBroker(key);
    }

    /**
     * Releases all broker instances pooled by this factory (if any). Note that the broker are
     * closed prior to releasing them.
     * 
     * @see org.apache.ojb.broker.core.PersistenceBrokerFactoryIF#releaseAllInstances
     */
    public static void releaseAllInstances()
    {
        PersistenceBrokerFactoryFactory.instance().releaseAllInstances();
    }

    /**
     * Shuts OJB down, i.e. releases all resources. You should not use any OJB functionality 
     * after calling this method. 
     * 
     * @see org.apache.ojb.broker.core.PersistenceBrokerFactoryIF#shutdown()
     */
    public static void shutdown()
    {
        PersistenceBrokerFactoryFactory.instance().shutdown();
    }
}
