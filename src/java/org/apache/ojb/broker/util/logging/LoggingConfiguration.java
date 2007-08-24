package org.apache.ojb.broker.util.logging;

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

import java.io.InputStream;
import java.io.File;
import java.util.Properties;
import java.net.URL;

import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.configuration.impl.ConfigurationAbstractImpl;
import org.apache.commons.lang.SystemUtils;

/**
 * Provides the configuration for the logging. Note that this is separated from the OJB
 * configuration.
 * 
 * @version $Id: LoggingConfiguration.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public class LoggingConfiguration extends ConfigurationAbstractImpl
{
    /** The commons-logging property denoting which log to use. This property
     *  is repeated here to avoid making this class dependent upon commons-logging */
    public static final String PROPERTY_COMMONS_LOGGING_LOG = "org.apache.commons.logging.Log";
    /** The commons-logging property denoting which log factory to use. This property
     *  is repeated here to avoid making this class dependent upon commons-logging */
    public static final String PROPERTY_COMMONS_LOGGING_LOGFACTORY = "org.apache.commons.logging.LogFactory";
    /** The property denoting the OJB logger class */
    public static final String PROPERTY_OJB_LOGGERCLASS      = "org.apache.ojb.broker.util.logging.Logger.class";
    /** The property denoting the config file for the OJB logger class */
    public static final String PROPERTY_OJB_LOGGERCONFIGFILE = "org.apache.ojb.broker.util.logging.Logger.configFile";
    /** Default filename of the OJB logging properties file */
    public static final String OJB_LOGGING_PROPERTIES_FILE = "OJB-logging.properties";
    /** Default log level */
    public static final String OJB_DEFAULT_LOG_LEVEL = "WARN";
    /** Default boot log level */
    public static final String OJB_DEFAULT_BOOT_LOG_LEVEL = "INFO";

    /** The logger class */
    private Class  _loggerClass;
    /** The config file for the logger */
    private String _loggerConfigFile;

    /**
     * Creates a new logging configuration object which automatically initializes itself.
     */
    public LoggingConfiguration()
    {
        super();
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.util.configuration.impl.ConfigurationAbstractImpl#load()
     */
    protected void load()
    {
        Logger bootLogger = LoggerFactory.getBootLogger();

        // first we check whether the system property
        //   org.apache.ojb.broker.util.logging.Logger
        // is set (or its alias LoggerClass which is deprecated)
        ClassLoader contextLoader   = ClassHelper.getClassLoader();
        String      loggerClassName;

        _loggerClass      = null;
        properties        = new Properties();
        loggerClassName   = getLoggerClass(System.getProperties());
        _loggerConfigFile = getLoggerConfigFile(System.getProperties());

        InputStream ojbLogPropFile;
        if (loggerClassName == null)
        {
            // now we're trying to load the OJB-logging.properties file
            String ojbLogPropFilePath = System.getProperty(OJB_LOGGING_PROPERTIES_FILE, OJB_LOGGING_PROPERTIES_FILE);
            try
            {
                URL ojbLoggingURL = ClassHelper.getResource(ojbLogPropFilePath);
                if (ojbLoggingURL == null)
                {
                    ojbLoggingURL = (new File(ojbLogPropFilePath)).toURL();
                }
                ojbLogPropFile = ojbLoggingURL.openStream();
                try
                {
                    bootLogger.info("Found logging properties file: " + ojbLogPropFilePath);
                    properties.load(ojbLogPropFile);
                    _loggerConfigFile = getLoggerConfigFile(properties);
                    loggerClassName = getLoggerClass(properties);
                }
                finally
                {
                    ojbLogPropFile.close();
                }
            }
            catch (Exception ex)
            {
                if(loggerClassName == null)
                {
                    bootLogger.warn("Can't read logging properties file using path '" + ojbLogPropFilePath
                            + "', message is: " + SystemUtils.LINE_SEPARATOR + ex.getMessage()
                            + SystemUtils.LINE_SEPARATOR + "Will try to load logging properties from OJB.properties file");
                }
                else
                {
                    bootLogger.info("Problems while closing resources for path '" + ojbLogPropFilePath
                            + "', message is: " + SystemUtils.LINE_SEPARATOR + ex.getMessage(), ex);
                }
            }
        }
        if (loggerClassName == null)
        {
            // deprecated: load the OJB.properties file
            // this is not good because we have all OJB properties in this config
            String ojbPropFile = System.getProperty("OJB.properties", "OJB.properties");

            try
            {
                ojbLogPropFile = contextLoader.getResourceAsStream(ojbPropFile);
                if (ojbLogPropFile != null)
                {
                    try
                    {
                        properties.load(ojbLogPropFile);
                        loggerClassName   = getLoggerClass(properties);
                        _loggerConfigFile = getLoggerConfigFile(properties);
                        if (loggerClassName != null)
                        {
                            // deprecation warning for after 1.0
                            bootLogger.warn("Please use a separate '"+OJB_LOGGING_PROPERTIES_FILE+"' file to specify your logging settings");
                        }
                    }
                    finally
                    {
                        ojbLogPropFile.close();
                    }
                }
            }
            catch (Exception ex)
            {}
        }
        if (loggerClassName != null)
        {
            try
            {
                _loggerClass = ClassHelper.getClass(loggerClassName);
                bootLogger.info("Logging: Found logger class '" + loggerClassName);
            }
            catch (ClassNotFoundException ex)
            {
                _loggerClass = PoorMansLoggerImpl.class;
                bootLogger.warn("Could not load logger class "+loggerClassName+", defaulting to "+_loggerClass.getName(), ex);
            }
        }
        else
        {
            // still no logger configured - lets check whether commons-logging is configured
            if ((System.getProperty(PROPERTY_COMMONS_LOGGING_LOG) != null) ||
                (System.getProperty(PROPERTY_COMMONS_LOGGING_LOGFACTORY) != null))
            {
                // yep, so use commons-logging
                _loggerClass = CommonsLoggerImpl.class;
                bootLogger.info("Logging: Found commons logging properties, use " + _loggerClass);
            }
            else
            {
                // but perhaps there is a log4j.properties file ?
                try
                {
                    ojbLogPropFile = contextLoader.getResourceAsStream("log4j.properties");
                    if (ojbLogPropFile != null)
                    {
                        // yep, so use log4j
                        _loggerClass      = Log4jLoggerImpl.class;
                        _loggerConfigFile = "log4j.properties";
                        bootLogger.info("Logging: Found 'log4j.properties' file, use " + _loggerClass);
                        ojbLogPropFile.close();
                    }
                }
                catch (Exception ex)
                {}
                if (_loggerClass == null)
                {
                    // or a commons-logging.properties file ?
                    try
                    {
                        ojbLogPropFile = contextLoader.getResourceAsStream("commons-logging.properties");
                        if (ojbLogPropFile != null)
                        {
                            // yep, so use commons-logging
                            _loggerClass      = CommonsLoggerImpl.class;
                            _loggerConfigFile = "commons-logging.properties";
                            bootLogger.info("Logging: Found 'commons-logging.properties' file, use " + _loggerClass);
                            ojbLogPropFile.close();
                        }
                    }
                    catch (Exception ex)
                    {}
                    if (_loggerClass == null)
                    {
                        // no, so default to poor man's logging
                        bootLogger.info("** Can't find logging configuration file, use default logger **");
                        _loggerClass = PoorMansLoggerImpl.class;
                    }
                }
            }
        }
    }

    private String getLoggerClass(Properties props)
    {
        String loggerClassName = props.getProperty(PROPERTY_OJB_LOGGERCLASS);

        if (loggerClassName == null)
        {
            loggerClassName = props.getProperty("LoggerClass");
        }
        return loggerClassName;
    }

    private String getLoggerConfigFile(Properties props)
    {
        String loggerConfigFile = props.getProperty(PROPERTY_OJB_LOGGERCONFIGFILE);

        if (loggerConfigFile == null)
        {
            loggerConfigFile = props.getProperty("LoggerConfigFile");
        }
        return loggerConfigFile;
    }

    public String getLogLevel(String loggerName)
    {
        /*
        arminw:
        use ROOT.LogLevel property to define global
        default log level
        */
        return getString(loggerName + ".LogLevel", getString("ROOT.LogLevel", OJB_DEFAULT_LOG_LEVEL));
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.util.configuration.Configuration#setLogger(org.apache.ojb.broker.util.logging.Logger)
     */
    public void setLogger(Logger loggerInstance)
    {
        // ignored - only logging via the boot logger
    }

    /**
     * Returns the logger class.
     * 
     * @return The logger class
     */
    public Class getLoggerClass()
    {
        return _loggerClass;
    }

    /**
     * Returns the name of the config file for the logger.
     * 
     * @return The config file if it was configured
     */
    public String getLoggerConfigFile()
    {
        return _loggerConfigFile;
    }
}
