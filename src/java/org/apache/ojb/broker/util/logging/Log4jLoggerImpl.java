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

import java.net.URL;
import java.util.Enumeration;

import org.apache.log4j.LogManager;
import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;

/**
 * This is a Logger implementation based on Log4j.
 * It can be enabled by putting
 * LoggerClass=org.apache.ojb.broker.util.logging.Log4jLoggerImpl
 * in the OJB .properties file. <br>
 * If you want log4j to initialize from a property file you can add
 * LoggerConfigFile=log4j.properties to the org.apache.ojb.properties file.
 * the logger only initializes log4j if the application hasn't done it yet
 *
 * You can find sample log4j.properties file in the log4j web site
 * http://jakarta.apache.org/log4j
 * in the javadoc look for org.apache.log4j.examples
 *
 * @author Bertrand
 * @author Thomas Mahler
 * @version $Id: Log4jLoggerImpl.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public class Log4jLoggerImpl implements Logger
{
	static private final String FQCN = Log4jLoggerImpl.class.getName();
    /** flag about log4j configuration state */
    private static boolean log4jConfigured = false;

    private transient org.apache.log4j.Logger logger;
	private String name;

    /** Helper method to check if log4j is already configured */
    private static synchronized boolean isLog4JConfigured()
    {
        if(!log4jConfigured)
        {
            Enumeration en = org.apache.log4j.Logger.getRootLogger().getAllAppenders();

            if (!(en instanceof org.apache.log4j.helpers.NullEnumeration))
            {
                log4jConfigured = true;
            }
            else
            {
                Enumeration cats = LogManager.getCurrentLoggers();
                while (cats.hasMoreElements())
                {
                    org.apache.log4j.Logger c = (org.apache.log4j.Logger) cats.nextElement();
                    if (!(c.getAllAppenders() instanceof org.apache.log4j.helpers.NullEnumeration))
                    {
                        log4jConfigured = true;
                    }
                }
            }
            if(log4jConfigured)
            {
                String msg = "Log4J is already configured, will not search for log4j properties file";
                LoggerFactory.getBootLogger().info(msg);
            }
            else
            {
                LoggerFactory.getBootLogger().info("Log4J is not configured");
            }
        }
        return log4jConfigured;
    }

    /**
     * Initialization of log4j <br>
     * <b>NOTE</b>  - if log4j property file is called log4j.properties then
     * log4j will be configured already.
     */
    private static synchronized void initializeLog4JSubSystem(String configFile)
    {
        LoggerFactory.getBootLogger().info("Initializing Log4J using file: '" + configFile + "'");
        if(configFile == null || "".equals(configFile.trim()))
        {
            // no configuration available
            LoggerFactory.getBootLogger().warn("No log4j configuration file specified");
        }
        else
        {
            // try resource look in classpath
            URL url = ClassHelper.getResource(configFile);
            LoggerFactory.getBootLogger().info("Initializing Log4J : resource from config file:" + url);
            if (url != null)
            {
                PropertyConfigurator.configure(url);
            }
            // if file is not in classpath try ordinary filesystem lookup
            else
            {
                PropertyConfigurator.configure(configFile);
            }
        }
        log4jConfigured = true;
    }

	public Log4jLoggerImpl(String name)
	{
		this.name = name;
	}

    /**
	 * @see org.apache.ojb.broker.util.configuration.Configurable#configure(Configuration)
     * This method must be performed by LogFactory after creating a logger instance.
	 */
	public void configure(Configuration config) throws ConfigurationException
	{
        if (!isLog4JConfigured())
        {
            LoggingConfiguration lc = (LoggingConfiguration) config;
            initializeLog4JSubSystem(lc.getLoggerConfigFile());
        }
	}

    /**
	 * Gets the logger.
     *
	 * @return Returns a Category
	 */
	private org.apache.log4j.Logger getLogger()
	{
        /*
        Logger interface extends Serializable, thus Log field is
        declared 'transient' and we have to null-check
		*/
		if (logger == null)
		{
			logger = org.apache.log4j.Logger.getLogger(name);
		}
		return logger;
	}

	public String getName()
	{
		return name;
	}

	private Level getLevel()
	{
		return getLogger().getEffectiveLevel();
	}

	/**
	 * generate a message for loglevel DEBUG
     *
	 * @param pObject the message Object
	 */
	public final void debug(Object pObject)
	{
		getLogger().log(FQCN, Level.DEBUG, pObject, null);
	}

	/**
	 * generate a message for loglevel INFO
     *
	 * @param pObject the message Object
	 */
	public final void info(Object pObject)
	{
		getLogger().log(FQCN, Level.INFO, pObject, null);
	}

	/**
	 * generate a message for loglevel WARN
     *
	 * @param pObject the message Object
	 */
	public final void warn(Object pObject)
	{
		getLogger().log(FQCN, Level.WARN, pObject, null);
	}

	/**
	 * generate a message for loglevel ERROR
     *
	 * @param pObject the message Object
	 */
	public final void error(Object pObject)
	{
		getLogger().log(FQCN, Level.ERROR, pObject, null);
	}

	/**
	 * generate a message for loglevel FATAL
     *
	 * @param pObject the message Object
	 */
	public final void fatal(Object pObject)
	{
		getLogger().log(FQCN, Level.FATAL, pObject, null);
	}

	public void debug(Object message, Throwable obj)
	{
		getLogger().log(FQCN, Level.DEBUG, message, obj);
	}

	public void error(Object message, Throwable obj)
	{
		getLogger().log(FQCN, Level.ERROR, message, obj);
	}

	public void fatal(Object message, Throwable obj)
	{
		getLogger().log(FQCN, Level.FATAL, message, obj);
	}

	public void info(Object message, Throwable obj)
	{
		getLogger().log(FQCN, Level.INFO, message, obj);
	}

	public void warn(Object message, Throwable obj)
	{
		getLogger().log(FQCN, Level.WARN, message, obj);
	}

	public void safeDebug(String message, Object obj)
	{
		if (Level.DEBUG.isGreaterOrEqual(getLevel()))
		{
			String toString = null;
			if (obj != null)
			{
				try
				{
					toString = obj.toString();
				}
				catch (Throwable t)
				{
					toString = "BAD toString() impl for " + obj.getClass().getName();
				}
			}
			debug(message + " : " + toString);
		}
	}

	public void safeDebug(String message, Object obj, Throwable throwable)
	{
		if (Level.DEBUG.isGreaterOrEqual(getLevel()))
		{
			String toString = null;
			if (obj != null)
			{
				try
				{
					toString = obj.toString();
				}
				catch (Throwable t)
				{
					toString = "BAD toString() impl for " + obj.getClass().getName();
				}
			}
			debug(message + " : " + toString, throwable);
		}
	}

	public void safeInfo(String message, Object obj)
	{
		if (Level.INFO.isGreaterOrEqual(getLevel()))
		{
			String toString = null;
			if (obj != null)
			{
				try
				{
					toString = obj.toString();
				}
				catch (Throwable t)
				{
					toString = "BAD toString() impl for " + obj.getClass().getName();
				}
			}
			info(message + " : " + toString);
		}
	}

	public void safeInfo(String message, Object obj, Throwable throwable)
	{
		if (Level.INFO.isGreaterOrEqual(getLevel()))
		{
			String toString = null;
			if (obj != null)
			{
				try
				{
					toString = obj.toString();
				}
				catch (Throwable t)
				{
					toString = "BAD toString() impl for " + obj.getClass().getName();
				}
			}
			info(message + " : " + toString, throwable);
		}
	}

	public void safeWarn(String message, Object obj)
	{
		if (Level.WARN.isGreaterOrEqual(getLevel()))
		{
			String toString;
			try
			{
				toString = obj.toString();
			}
			catch (Throwable t)
			{
				toString = "BAD toString() impl for " + obj.getClass().getName();
			}
			 warn(message + " : " + toString);
		}
	}

	public void safeWarn(String message, Object obj, Throwable throwable)
	{
		if (Level.WARN.isGreaterOrEqual(getLevel()))
		{
			String toString;
			try
			{
				toString = obj.toString();
			}
			catch (Throwable t)
			{
				toString = "BAD toString() impl for " + obj.getClass().getName();
			}
			warn(message + " : " + toString, throwable);
		}
	}

	public void safeError(String message, Object obj)
	{
		if (Level.ERROR.isGreaterOrEqual(getLevel()))
		{
			String toString;
			try
			{
				toString = obj.toString();
			}
			catch (Throwable t)
			{
				toString = "BAD toString() impl for " + obj.getClass().getName();
			}
			error(message + " : " + toString);
		}
	}

	public void safeError(String message, Object obj, Throwable throwable)
	{
		if (Level.ERROR.isGreaterOrEqual(getLevel()))
		{
			String toString;
			try
			{
				toString = obj.toString();
			}
			catch (Throwable t)
			{
				toString = "BAD toString() impl for " + obj.getClass().getName();
			}
			error(message + " : " + toString, throwable);
		}
	}

	public void safeFatal(String message, Object obj)
	{
		if (Level.FATAL.isGreaterOrEqual(getLevel()))
		{
			String toString;
			try
			{
				toString = obj.toString();
			}
			catch (Throwable t)
			{
				toString = "BAD toString() impl for " + obj.getClass().getName();
			}
			fatal(message + " : " + toString);
		}
	}

	public void safeFatal(String message, Object obj, Throwable throwable)
	{
		if (Level.FATAL.isGreaterOrEqual(getLevel()))
		{
			String toString;
			try
			{
				toString = obj.toString();
			}
			catch (Throwable t)
			{
				toString = "BAD toString() impl for " + obj.getClass().getName();
			}
			fatal(message + " : " + toString, throwable);
		}
	}

	public boolean isDebugEnabled()
	{
		return getLogger().isDebugEnabled();
	}

	public boolean isEnabledFor(int priority)
	{
        org.apache.log4j.Logger log4j = getLogger();
        switch(priority)
        {
            case Logger.DEBUG: return log4j.isDebugEnabled();
            case Logger.INFO: return log4j.isInfoEnabled();
            case Logger.WARN: return log4j.isEnabledFor(org.apache.log4j.Priority.WARN);
            case Logger.ERROR: return log4j.isEnabledFor(org.apache.log4j.Priority.ERROR);
            case Logger.FATAL: return log4j.isEnabledFor(org.apache.log4j.Priority.FATAL);
        }
        return false;
    }
}
