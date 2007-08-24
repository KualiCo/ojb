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

import org.apache.ojb.broker.util.configuration.Configurable;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.configuration.Configurator;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * The <code>Configurator</code> for the OJB system.
 * Implemented as a singleton.
 * @author Thomas Mahler
 * @version $Id: OjbConfigurator.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class OjbConfigurator implements Configurator
{
	/**
     * the logger instance.
     */
    private static Logger log = LoggerFactory.getBootLogger();

    /**
     * the singleton instance of this class.
     */
    private static OjbConfigurator instance = new OjbConfigurator();

    /**
	 * the configuration to be used throught OJB
	 */
    private OjbConfiguration configuration;

	/**
	 * private Constructor. There is no public Constructors.
	 * Use the static method <code>getInstance()</code> to obtain an
	 * instance of this class.
	 */
    private OjbConfigurator()
    {
        configuration = new OjbConfiguration();
    }

	/**
	 * returns the singleton instance.
	 * @return the singleton instance.
	 */
    public static OjbConfigurator getInstance()
    {
        return instance;
    }

    /**
     * @see Configurator#setLogger(Logger)
     */
    public void setLogger(Logger logger)
    {
        log = logger;
    }

    /**
     * @see Configurator#configure(Configurable)
     */
    public void configure(Configurable target) throws ConfigurationException
    {
        target.configure(configuration);
    }

    /**
     * @see Configurator#getConfigurationFor(Configurable)
     */
    public Configuration getConfigurationFor(Configurable target)
            throws ConfigurationException
    {
        return configuration;
    }

}
