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
 * The <code>Configuration</code> interface defines lookup-methods to lookup
 * typed configuration-values.
 * For example <code>boolean getBoolean(String key, boolean defaultValue)</code> 
 * looks up a boolean value associated with <code>key</code>.
 * If no value is found for <code>key</code> the boolean <code>defaultValue</code>
 * is returned.
 * @author		Thomas Mahler
 * @version 	$Id: Configuration.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public interface Configuration
{

	/**
	 * this method allows to set a logger that tracks configuration events.
	 * @param loggerInstance the logger to set
	 */
    public void setLogger(Logger loggerInstance);

    /**
     * Returns the boolean value for the specified key. If no value for this key
     * is found in the configuration or the value is not an legal boolean
     * <code>defaultValue</code> is returned.
     *
     * @param key the key
     * @param defaultValue the default Value
     * @return the value for the key, or <code>defaultValue</code>
     */
    public boolean getBoolean(String key, boolean defaultValue);

    /**
     * Returns the class specified by the value for the specified key. If no
     * value for this key is found in the configuration, no class of this name
     * can be found or the specified class is not assignable
     * <code>assignable</code> <code>defaultValue</code> is returned.
     *
     * @param key the key
     * @param defaultValue the default Value
     * @param assignable a classe and/or interface the specified class must
     *          extend/implement.
     * @return the value for the key, or <code>defaultValue</code>
     */
    public Class getClass(String key, Class defaultValue, Class assignable);

    /**
     * Returns the class specified by the value for the specified key. If no
     * value for this key is found in the configuration, no class of this name
     * can be found or the specified class is not assignable to each
     * class/interface in <code>assignables</code> <code>defaultValue</code> is
     * returned.
     *
     * @param key the key
     * @param defaultValue the default Value
     * @param assignables classes and/or interfaces the specified class must
     *          extend/implement.
     * @return the value for the key, or <code>defaultValue</code>
     */
    public Class getClass(String key, Class defaultValue, Class[] assignables);

    /**
     * Returns the class specified by the value for the specified key. If no
     * value for this key is found in the configuration or no class of this name
     * can be found <code>defaultValue</code> is returned.
     *
     * @param key the key
     * @param defaultValue the default Value
     * @return the value for the key, or <code>defaultValue</code>
     */
    public Class getClass(String key, Class defaultValue);

    /**
     * Returns the integer value for the specified key. If no value for this key
     * is found in the configuration or the value is not an legal integer
     * <code>defaultValue</code> is returned.
     *
     * @param key the key
     * @param defaultValue the default Value
     * @return the value for the key, or <code>defaultValue</code>
     */
    public int getInteger(String key, int defaultValue);

    /**
     * Returns the string value for the specified key. If no value for this key
     * is found in the configuration <code>defaultValue</code> is returned.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value for the key, or <code>defaultValue</code>
     */
    public String getString(String key, String defaultValue);

    /**
     * Gets an array of Strings from the value of the specified key, seperated
     * by any key from <code>seperators</code>. If no value for this key
     * is found the array contained in <code>defaultValue</code> is returned.
     *
     * @param key the key
     * @param defaultValue the default Value
     * @param seperators the seprators to be used
     * @return the strings for the key, or the strings contained in
     *          <code>defaultValue</code>
     */
    public String[] getStrings(String key, String defaultValue, String seperators);

    /**
     * Gets an array of Strings from the value of the specified key, seperated
     * by ";". If no value for this key
     * is found the array contained in <code>defaultValue</code> is returned.
     *
     * @param key the key
     * @param defaultValue the default Value
     * @return the strings for the key, or the strings contained in
     *          <code>defaultValue</code>
     */
    public String[] getStrings(String key, String defaultValue);
}
