package org.apache.ojb.broker.util.configuration;

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

import org.apache.ojb.broker.util.logging.Logger;

/**
 * The <code>Configurator</code> interface defines methods for looking up 
 * Configurations and for configuring <code>Configurable</code> instances.
 * 
 * call sequence:
 * 1. The application obtains a <code>Configurator</code> instance (typically from a
 * Factory).
 * 
 * 2. The application uses the Configurator to configure <code>Configurable</code> 
 * instances. 
 * The Configurator must lookup the proper <code>Configuration</code> and invoke
 * the <code>configure</code> method on the <code>Configurable</code> instance.
 *
 * <pre>
 *      // 1. obtain Configurator
 *      Configurator configurator = OjbConfigurator.getInstance();
 * 
 *      // 2. ask Configurator to configure the Configurable instance
 *      Configurable obj = ...
 *      configurator.configure(obj);
 * </pre>
 *
 * @author Thomas Mahler
 * @version $Id: Configurator.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public interface Configurator
{

	/**
	 * this method allows to set a logger that tracks configuration events.
	 * @param loggerInstance the logger to set
	 */
    public void setLogger(Logger loggerInstance);

    /**
     * configures the <code>Configurable</code> instance target.
     * @param target the <code>Configurable</code> instance.
     * @throws ConfigurationException
     */
    public void configure(Configurable target) throws ConfigurationException;

    /**
     * looks up the proper <code>Configuration</code> for 
     * the <code>Configurable</code> instance target.
     * @param target the <code>Configurable</code> instance.
     * @return the resulting<code>Configuration</code>.
     * @throws  ConfigurationException
     */
    public Configuration getConfigurationFor(Configurable target) throws ConfigurationException;
}
