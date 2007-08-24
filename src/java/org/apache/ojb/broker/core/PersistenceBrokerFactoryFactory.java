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

import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.Configurator;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 *
 * @author Thomas Mahler
 * @version $Id: PersistenceBrokerFactoryFactory.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PersistenceBrokerFactoryFactory
{
    private static Logger log = LoggerFactory.getBootLogger();

    private static final String PBF_KEY = "PersistenceBrokerFactoryClass";
    private static PersistenceBrokerFactoryIF singleton = init();

    /**
     * Returns an {@link PersistenceBrokerFactoryIF} instance.
     */
    public static PersistenceBrokerFactoryIF instance()
    {
        return singleton;
    }

    private static PersistenceBrokerFactoryIF init()
    {
        if (log.isDebugEnabled()) log.debug("Instantiate PersistenceBrokerFactory");
        Class pbfClass = null;
        try
        {
            Configurator configurator = OjbConfigurator.getInstance();
            Configuration config = configurator.getConfigurationFor(null);
            pbfClass = config.getClass(PBF_KEY, null);
            if(pbfClass == null)
            {
                log.error("Creation of PersistenceBrokerFactory (PBF) instance failed, can't get PBF class object");
                throw new OJBRuntimeException("Property for key '" + PBF_KEY + "' can not be found in properties file");
            }
            PersistenceBrokerFactoryIF result = (PersistenceBrokerFactoryIF) ClassHelper.newInstance(pbfClass);
            configurator.configure(result);
            log.info("PersistencebrokerFactory class instantiated: " + result);
            return result;
        }
        catch (Exception e)
        {
            if(e instanceof OJBRuntimeException)
            {
                throw (OJBRuntimeException) e;
            }
            else
            {
                throw new OJBRuntimeException("Error while instantiation of PersistenceBrokerFactory class", e);
            }
        }
    }
}
