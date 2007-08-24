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
import org.apache.ojb.broker.util.configuration.Configurable;

/**
 * Factory for {@link PersistenceBroker} instances.
 * Each implementation have to provide a default constructor.
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: PersistenceBrokerFactoryIF.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public interface PersistenceBrokerFactoryIF extends Configurable
{
    /**
     * Set the {@link PBKey} used for convinience {@link PersistenceBroker}
     * lookup method {@link #defaultPersistenceBroker}.
     * <br/>
     * Note: It's only allowed to set the default {@link PBKey} once.
     * All further calls will cause an exception.
     * If a default {@link org.apache.ojb.broker.metadata.JdbcConnectionDescriptor}
     * was declared in configuration file, OJB will set the declared PBKey as default.
     * <br/>
     * This method is convenience for
     * {@link org.apache.ojb.broker.metadata.MetadataManager#setDefaultPBKey}.
     */
    public void setDefaultKey(PBKey key);

    /**
     * Get the default {@link PBKey}.
     * This method is convenience for
     * {@link org.apache.ojb.broker.metadata.MetadataManager#getDefaultPBKey}.
     *
     * @see #setDefaultKey
     */
    public PBKey getDefaultKey();

    /**
     * Return {@link org.apache.ojb.broker.PersistenceBroker} instance for the given
     * {@link org.apache.ojb.broker.PBKey}.
     *
     * @param key
     */
    public PersistenceBrokerInternal createPersistenceBroker(PBKey key) throws PBFactoryException;

    /**
     * Return a ready for action {@link org.apache.ojb.broker.PersistenceBroker} instance.
     *
     * @param jcdAlias An jcdAlias name specified in a <tt>jdbc-connection-descriptor</tt>
     * @param user user name specified in a <tt>jdbc-connection-descriptor</tt>
     * @param password valid password specified in a <tt>jdbc-connection-descriptor</tt>
     */
    public PersistenceBrokerInternal createPersistenceBroker(String jcdAlias, String user, String password)
            throws PBFactoryException;

    /**
     * Return a default broker instance, specified in configuration
     * or set using {@link #setDefaultKey}.
     */
    public PersistenceBrokerInternal defaultPersistenceBroker() throws PBFactoryException;

    /**
     * release all broker instances pooled by the factory.
     * each broker instance is closed before release.
     */
    public void releaseAllInstances();

    /**
     * Returns the total number of
     * active {@link org.apache.ojb.broker.PersistenceBroker}
     * instances.
     */
    public int activePersistenceBroker();

    /**
     * Shutdown method for OJB, kills all running processes within OJB - after
     * shutdown OJB can no longer be used.
     * <br/>
     * This method is introduced to solve hot/redeployment problems (memory leaks) caused by
     * the usage of {@link ThreadLocal} instances in OJB source and the reuse of threads
     * by the container (e.g. servlet- or ejb-container).
     */
    public void shutdown();
}
