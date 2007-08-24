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

import java.util.HashMap;
import java.util.Map;

import org.apache.ojb.broker.util.ClassHelper;
import org.apache.commons.lang.SystemUtils;

/**
 * The factory class <code>LoggerFactory</code> can be used
 * to create <code>Logger</code> instances.
 * The <code>Logger</code>-implementation class served
 * by the factory is configured by settings in the
 * OJB.properties file.
 *
 * @author Thomas Mahler
 * @author <a href="leandro@ibnetwork.com.br">Leandro Rodrigo Saad Cruz</a>
 * @version $Id: LoggerFactoryImpl.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 * @see <a href="http://jakarta.apache.org/log4j/docs/index.html">jakarta-log4j</a>
 */
public class LoggerFactoryImpl
{
    public static final String BOOT_LOG_LEVEL_STR = "OJB.bootLogLevel";
    protected static final String BOOT_STR = "BOOT";
    protected static final String DEFAULT_STR = "DEFAULT";
    protected static final LoggerFactoryImpl INSTANCE = new LoggerFactoryImpl();

    private Logger defaultLogger = null;

    private Logger bootLogger = null;

    private boolean bootLoggerIsReassigned = false;

    /** Used for caching logger instances */
    private Map cache = new HashMap();
    /** The configuration */
    private LoggingConfiguration conf;

    // yes. it's a singleton !
    private LoggerFactoryImpl()
    {
    }

    public static LoggerFactoryImpl getInstance()
    {
        return INSTANCE;
    }

    private LoggingConfiguration getConfiguration()
    {
        if(conf == null)
        {
            // this will load the configuration
            conf = new LoggingConfiguration();
        }
        return conf;
    }

    /**
     * returns a minimal logger that needs no configuration
     * and can thus be safely used during OJB boot phase
     * (i.e. when OJB.properties have not been loaded).
     *
     * @return Logger the OJB BootLogger
     */
    public Logger getBootLogger()
    {
        if(bootLogger == null)
        {
            // create a StringBuffer based Logger for boot log operations
            bootLogger = createStringBufferLogger_Boot();
        }
        return bootLogger;
    }


    /**
     * returns the default logger. This Logger can
     * be used when it is not appropriate to use a
     * dedicated fresh Logger instance.
     *
     * @return default Logger
     */
    public Logger getDefaultLogger()
    {
        if(defaultLogger == null)
        {
            defaultLogger = getLogger(DEFAULT_STR);
        }
        return defaultLogger;
    }


    /**
     * returns a Logger. The Logger is named
     * after the full qualified name of input parameter clazz
     *
     * @param clazz the Class which name is to be used as name
     * @return Logger the returned Logger
     */
    public Logger getLogger(Class clazz)
    {
        return getLogger(clazz.getName());
    }


    /**
     * returns a Logger.
     *
     * @param loggerName the name of the Logger
     * @return Logger the returned Logger
     */
    public Logger getLogger(String loggerName)
    {
        Logger logger;
        //lookup in the cache first
        logger = (Logger) cache.get(loggerName);

        if(logger == null)
        {
            try
            {
                // get the configuration (not from the configurator because this is independent)
                logger = createLoggerInstance(loggerName);
                if(getBootLogger().isDebugEnabled())
                {
                    getBootLogger().debug("Using logger class '"
                            + (getConfiguration() != null ? getConfiguration().getLoggerClass() : null)
                            + "' for " + loggerName);
                }
                // configure the logger
                getBootLogger().debug("Initializing logger instance " + loggerName);
                logger.configure(conf);
            }
            catch(Throwable t)
            {
                // do reassign check and signal logger creation failure
                reassignBootLogger(true);
                logger = getBootLogger();
                getBootLogger().error("[" + this.getClass().getName()
                            + "] Could not initialize logger " + (conf != null ? conf.getLoggerClass() : null), t);
            }
            //cache it so we can get it faster the next time
            cache.put(loggerName, logger);
            // do reassign check
            reassignBootLogger(false);
        }
        return logger;
    }

    /**
     * Creates a new Logger instance for the specified name.
     */
    private Logger createLoggerInstance(String loggerName) throws Exception
    {
        Class loggerClass = getConfiguration().getLoggerClass();
        Logger log = (Logger) ClassHelper.newInstance(loggerClass, String.class, loggerName);
        log.configure(getConfiguration());
        return log;
    }

    /**
     *
     * @param forceError
     */
    protected synchronized void reassignBootLogger(boolean forceError)
    {
        // if the boot logger was already reassigned do nothing
        if(!bootLoggerIsReassigned)
        {
            Logger newBootLogger = null;
            String name = getBootLogger().getName();
            try
            {
                // 1. try to use a Logger instance based on the configuration files
                newBootLogger = createLoggerInstance(name);
            }
            catch(Exception e) {/*ignore*/}
            if(newBootLogger == null)
            {
                // 2. if no logging library can be found, use OJB's console logger
                newBootLogger = createPoorMansLogger_Boot();
            }
            if(getBootLogger() instanceof StringBufferLoggerImpl)
            {
                /*
                if the StringBuffer based Logger was used for OJB bootstrap process
                get the logging statement string and log it on the "real" Logger instance
                */
                StringBufferLoggerImpl strLogger = (StringBufferLoggerImpl) getBootLogger();
                String bootMessage = strLogger.flushLogBuffer();
                String eol = SystemUtils.LINE_SEPARATOR;
                if(forceError || strLogger.isErrorLog())
                {
                    newBootLogger.error("-- boot log messages -->" + eol + bootMessage);
                }
                else
                {
                    newBootLogger.info("-- boot log messages -->" + eol + bootMessage);
                }
            }
            bootLogger = newBootLogger;
            bootLoggerIsReassigned = true;
        }
    }

    protected Logger createPoorMansLogger_Boot()
    {
        Logger bootLogger = new PoorMansLoggerImpl(BOOT_STR);
        // allow user to set boot log level via system property
        String level = System.getProperty(BOOT_LOG_LEVEL_STR, LoggingConfiguration.OJB_DEFAULT_BOOT_LOG_LEVEL);
        ((PoorMansLoggerImpl) bootLogger).setLevel(level);
        return bootLogger;
    }

    protected Logger createStringBufferLogger_Boot()
    {
        Logger bootLogger = new StringBufferLoggerImpl(BOOT_STR);
        // allow user to set boot log level via system property
        String level = System.getProperty(BOOT_LOG_LEVEL_STR, LoggingConfiguration.OJB_DEFAULT_BOOT_LOG_LEVEL);
        ((PoorMansLoggerImpl) bootLogger).setLevel(level);
        return bootLogger;
    }
}
