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

import org.apache.ojb.broker.PersistenceBrokerException;

/**
 * This exception is used to signal Problems that occur
 * during configuration operations.
 *
 * @author Thomas Mahler
 * @version $Id: ConfigurationException.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class ConfigurationException extends PersistenceBrokerException
{
	/**
	 * protected Constructor
	 */
    protected ConfigurationException(String key, String message)
    {
        super("Configuration problem on key: " + key + "\nReason is: " + message);
    }

	/**
	 * public Constructor
	 */
    public ConfigurationException(String message)
    {
        super("Configuration problem: " + message);
    }

	/**
	 * public Constructor
	 */
    public ConfigurationException(String message, Throwable exc)
    {
        super("Configuration problem: " + message + " because of: " + exc);
    }

}
