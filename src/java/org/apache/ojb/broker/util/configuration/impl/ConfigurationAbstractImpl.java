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

import org.apache.ojb.broker.metadata.MetadataException;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.ClassHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Configuration Base Class that
 * keeps a Properties based configuration persistent in a file.
 * This class provides only basic infrastructure for loading etc.
 *
 * @author Thomas Mahler
 * @version $Id: ConfigurationAbstractImpl.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public abstract class ConfigurationAbstractImpl implements Configuration
{
    /**the logger used in this class.*/
    private Logger logger = LoggerFactory.getBootLogger();

    /**the name of the properties file*/
    protected String filename;

    /**the properties object holding the configuration data*/
    protected Properties properties;

    /**
     * Contains all legal values for boolean configuration entries that represent
     * <code>true</code>.
     */
    private String[] trueValues = {"true", "yes", "1"};
    /**
     * Contains all legal values for boolean configuration entries that represent
     * <code>false</code>.
     */
    private String[] falseValues = {"false", "no", "0"};

    /**
     * The constructor loads the configuration from file
     */
    public ConfigurationAbstractImpl()
    {
        load();
    }

    /**
     * Returns the string value for the specified key. If no value for this key
     * is found in the configuration <code>defaultValue</code> is returned.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value for the key, or <code>defaultValue</code>
     */
    public String getString(String key, String defaultValue)
    {
        String ret = properties.getProperty(key);
        if (ret == null)
        {
            if(defaultValue == null)
            {
                logger.info("No value for key '" + key + "'");
            }
            else
            {
                logger.debug("No value for key \"" + key + "\", using default \""
                        + defaultValue + "\".");
                properties.put(key, defaultValue);
            }

            ret = defaultValue;
        }

        return ret;
    }

    /**
     * Gets an array of Strings from the value of the specified key, seperated
     * by any key from <code>seperators</code>. If no value for this key
     * is found the array contained in <code>defaultValue</code> is returned.
     *
     * @param key the key
     * @param defaultValue the default Value
     * @param seperators the seprators to be used
     * @return the strings for the key, or the strings contained in <code>defaultValue</code>
     *
     * @see StringTokenizer
     */
    public String[] getStrings(String key, String defaultValue, String seperators)
    {
        StringTokenizer st = new StringTokenizer(getString(key, defaultValue), seperators);
        String[] ret = new String[st.countTokens()];
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = st.nextToken();
        }
        return ret;
    }

    /**
     * Gets an array of Strings from the value of the specified key, seperated
     * by ";". If no value for this key
     * is found the array contained in <code>defaultValue</code> is returned.
     *
     * @param key the key
     * @param defaultValue the default Value
     * @return the strings for the key, or the strings contained in <code>defaultValue</code>
     */
    public String[] getStrings(String key, String defaultValue)
    {
        return getStrings(key, defaultValue, ";");
    }

    /**
     * Returns the integer value for the specified key. If no value for this key
     * is found in the configuration or the value is not an legal integer
     * <code>defaultValue</code> is returned.
     *
     * @param key the key
     * @param defaultValue the default Value
     * @return the value for the key, or <code>defaultValue</code>
     */
    public int getInteger(String key, int defaultValue)
    {
        int ret;
        try
        {
            String tmp = properties.getProperty(key);
            if (tmp == null)
            {
                properties.put(key, String.valueOf(defaultValue));
                logger.debug("No value for key \"" + key + "\", using default "
                        + defaultValue + ".");
                return defaultValue;
            }
            ret = Integer.parseInt(tmp);
        }
        catch (NumberFormatException e)
        {
            Object wrongValue = properties.put(key, String.valueOf(defaultValue));
            logger.warn(
                    "Value \""
                    + wrongValue
                    + "\" is illegal for key \""
                    + key
                    + "\" (should be an integer, using default value "
                    + defaultValue
                    + ")");
            ret = defaultValue;
        }
        return ret;
    }

    public long getLong(String key, long defaultValue)
    {
        long ret;
        try
        {
            String tmp = properties.getProperty(key);
            if (tmp == null)
            {
                properties.put(key, String.valueOf(defaultValue));
                logger.debug("No value for key \"" + key + "\", using default "
                        + defaultValue + ".");
                return defaultValue;
            }
            ret = Long.parseLong(tmp);
        }
        catch (NumberFormatException e)
        {
            Object wrongValue = properties.put(key, String.valueOf(defaultValue));
            logger.warn(
                    "Value \""
                    + wrongValue
                    + "\" is illegal for key \""
                    + key
                    + "\" (should be an integer, using default value "
                    + defaultValue
                    + ")");
            ret = defaultValue;
        }
        return ret;
    }

    public byte getByte(String key, byte defaultValue)
    {
        byte ret;
        try
        {
            String tmp = properties.getProperty(key);
            if (tmp == null)
            {
                properties.put(key, String.valueOf(defaultValue));
                logger.debug("No value for key \"" + key + "\", using default "
                        + defaultValue + ".");
                return defaultValue;
            }
            ret = Byte.parseByte(tmp);
        }
        catch (NumberFormatException e)
        {
            Object wrongValue = properties.put(key, String.valueOf(defaultValue));
            logger.warn(
                    "Value \""
                    + wrongValue
                    + "\" is illegal for key \""
                    + key
                    + "\" (should be an integer, using default value "
                    + defaultValue
                    + ")");
            ret = defaultValue;
        }
        return ret;
    }

    /**
     * Returns the boolean value for the specified key. If no value for this key
     * is found in the configuration or the value is not an legal boolean
     * <code>defaultValue</code> is returned.
     *
     * @see #trueValues
     * @see #falseValues
     *
     * @param key the key
     * @param defaultValue the default Value
     * @return the value for the key, or <code>defaultValue</code>
     */
    public boolean getBoolean(String key, boolean defaultValue)
    {
        String tmp = properties.getProperty(key);

        if (tmp == null)
        {
            logger.debug("No value for key \"" + key + "\", using default "
                    + defaultValue + ".");
            properties.put(key, String.valueOf(defaultValue));
            return defaultValue;
        }

        for (int i = 0; i < trueValues.length; i++)
        {
            if (tmp.equalsIgnoreCase(trueValues[i]))
            {
                return true;
            }
        }

        for (int i = 0; i < falseValues.length; i++)
        {
            if (tmp.equalsIgnoreCase(falseValues[i]))
            {
                return false;
            }
        }

        logger.warn(
                "Value \""
                + tmp
                + "\" is illegal for key \""
                + key
                + "\" (should be a boolean, using default value "
                + defaultValue
                + ")");
        return defaultValue;
    }

    /**
     * Returns the class specified by the value for the specified key. If no
     * value for this key is found in the configuration, no class of this name
     * can be found or the specified class is not assignable to each
     * class/interface in <code>assignables defaultValue</code> is returned.
     *
     * @param key the key
     * @param defaultValue the default Value
     * @param assignables classes and/or interfaces the specified class must
     *          extend/implement.
     * @return the value for the key, or <code>defaultValue</code>
     */
    public Class getClass(String key, Class defaultValue, Class[] assignables)
    {
        String className = properties.getProperty(key);

        if (className == null)
        {
        	if (defaultValue == null)
        	{
        		logger.info("No value for key '" + key + "'");
                return null;
        	}
        	else
        	{
	            className = defaultValue.getName();
	            properties.put(key, className);
	            logger.debug("No value for key \"" + key + "\", using default "
	                    + className + ".");
	            return defaultValue;
        	}
        }

        Class clazz = null;
        try
        {
            clazz = ClassHelper.getClass(className);
        }
        catch (ClassNotFoundException e)
        {
            clazz = defaultValue;
            logger.warn(
                    "Value \""
                    + className
                    + "\" is illegal for key \""
                    + key
                    + "\" (should be a class, using default value "
                    + defaultValue
                    + ")", e);
        }

        for (int i = 0; i < assignables.length; i++)
        {
            Class assignable = assignables[i];
            if (!assignable.isAssignableFrom(clazz))
            {
                String extendsOrImplements;
                if (assignable.isInterface())
                {
                    extendsOrImplements = "implement the interface ";
                }
                else
                {
                    extendsOrImplements = "extend the class ";

                }
                logger.error(
                        "The specified class \""
                        + className
                        + "\" does not "
                        + extendsOrImplements
                        + assignables[i].getName()
                        + ", which is a requirement for the key \""
                        + key
                        + "\". Using default class "
                        + defaultValue);
                clazz = defaultValue;
            }

        }

        return clazz;
    }

    /**
     * Returns the class specified by the value for the specified key. If no
     * value for this key is found in the configuration, no class of this name
     * can be found or the specified class is not assignable <code>assignable
     * defaultValue</code> is returned.
     *
     * @param key the key
     * @param defaultValue the default Value
     * @param assignable a classe and/or interface the specified class must
     *          extend/implement.
     * @return the value for the key, or <code>defaultValue</code>
     */
    public Class getClass(String key, Class defaultValue, Class assignable)
    {
        return getClass(key, defaultValue, new Class[]{assignable});
    }

    /**
     * Returns the class specified by the value for the specified key. If no
     * value for this key is found in the configuration or no class of this name
     * can be found <code>defaultValue</code> is returned.
     *
     * @param key the key
     * @param defaultValue the default Value
     * @return the value for the key, or <code>defaultValue</code>
     */
    public Class getClass(String key, Class defaultValue)
    {
        return getClass(key, defaultValue, new Class[0]);
    }

    /**
     * Loads the Configuration from the properties file.
     *
     * Loads the properties file, or uses defaults on failure.
     *
     * @see org.apache.ojb.broker.util.configuration.impl.ConfigurationAbstractImpl#setFilename(java.lang.String)
     *
     */
    protected void load()
    {
        properties = new Properties();

        String filename = getFilename();
        
        try
        {
            URL url = ClassHelper.getResource(filename);

            if (url == null)
            {
                url = (new File(filename)).toURL();
            }

            logger.info("Loading OJB's properties: " + url);

            URLConnection conn = url.openConnection();
            conn.setUseCaches(false);
            conn.connect();
            InputStream strIn = conn.getInputStream();
            properties.load(strIn);
            strIn.close();
        }
        catch (FileNotFoundException ex)
        {
            // [tomdz] If the filename is explicitly reset (null or empty string) then we'll
            //         output an info message because the user did this on purpose
            //         Otherwise, we'll output a warning
            if ((filename == null) || (filename.length() == 0))
            {
                logger.info("Starting OJB without a properties file. OJB is using default settings instead.");
            }
            else
            {
                logger.warn("Could not load properties file '"+filename+"'. Using default settings!", ex);
            }
            // [tomdz] There seems to be no use of this setting ?
            //properties.put("valid", "false");
        }
        catch (Exception ex)
        {
            throw new MetadataException("An error happend while loading the properties file '"+filename+"'", ex);
        }
    }

    private String getFilename()
    {
        if (filename == null)
        {
            filename = this.getClass().getName() + ".properties";
        }
        return filename;
    }

    protected void setFilename(String name)
    {
        filename = name;
    }

    /**
     * @see Configuration#setLogger(Logger)
     */
    public void setLogger(Logger loggerInstance)
    {
        logger = loggerInstance;
    }

}
