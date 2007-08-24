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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;

/**
 * This is a Logger implementation based on jakarta commons logging.
 * It can be enabled by putting
 * LoggerClass=org.apache.ojb.broker.util.logging.CommonsLoggerImpl
 * in the OJB .properties file. <br>
 * 
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: CommonsLoggerImpl.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public class CommonsLoggerImpl implements Logger
{
    private String name;
    private transient Log log;


	/**
	 * Constructor for CommonsLoggerImpl.
	 */
	public CommonsLoggerImpl(String aName)
	{
	    this.name = aName;
	}

    /**
	 * Returns the log.
	 * @return Log
	 */
	public Log getLog()
	{
		/*
        Logger interface extends Serializable, thus Log field is
        declared 'transient' and we have to null-check
		*/
        if(log == null)
        {
            log = LogFactory.getLog(name);
        }
        return log;
	}

    /**
	 * @see org.apache.ojb.broker.util.logging.Logger#isEnabledFor(int)
	 */
	public boolean isEnabledFor(int priority)
	{
		Log commonsLog = getLog();
        switch(priority)
        {
            case Logger.DEBUG: return commonsLog.isDebugEnabled();
            case Logger.INFO: return commonsLog.isInfoEnabled();
            case Logger.WARN: return commonsLog.isWarnEnabled();
            case Logger.ERROR: return commonsLog.isErrorEnabled();
            case Logger.FATAL: return commonsLog.isFatalEnabled();
        }
        return false;
    }

    /**
	 * @see org.apache.ojb.broker.util.logging.Logger#debug(Object)
	 */
	public void debug(Object pObject)
	{
		getLog().debug(pObject);
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#info(Object)
	 */
	public void info(Object pObject)
	{
        getLog().info(pObject);
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#warn(Object)
	 */
	public void warn(Object pObject)
	{
        getLog().warn(pObject);
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#error(Object)
	 */
	public void error(Object pObject)
	{
        getLog().error(pObject);
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#fatal(Object)
	 */
	public void fatal(Object pObject)
	{
        getLog().fatal(pObject);
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#debug(Object, Throwable)
	 */
	public void debug(Object message, Throwable obj)
	{
        getLog().debug(message, obj);
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#info(Object, Throwable)
	 */
	public void info(Object message, Throwable obj)
	{
        getLog().info(message, obj);
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#warn(Object, Throwable)
	 */
	public void warn(Object message, Throwable obj)
	{
        getLog().warn(message, obj);
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#error(Object, Throwable)
	 */
	public void error(Object message, Throwable obj)
	{
        getLog().error(message, obj);
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#fatal(Object, Throwable)
	 */
	public void fatal(Object message, Throwable obj)
	{
        getLog().fatal(message, obj);
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#isDebugEnabled()
	 */
	public boolean isDebugEnabled()
	{
		return getLog().isDebugEnabled();
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#getName()
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#safeDebug(String, Object)
	 */
	public void safeDebug(String message, Object obj)
	{
        if (getLog().isDebugEnabled())
        {
            String toString = safeToString(obj);
            getLog().debug(message + " : " + toString);
        }
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#safeDebug(String, Object, Throwable)
	 */
	public void safeDebug(String message, Object obj, Throwable t)
	{
        if (getLog().isDebugEnabled())
        {
            String toString = safeToString(obj);
            getLog().debug(message + " : " + toString, t);
        }
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#safeInfo(String, Object)
	 */
	public void safeInfo(String message, Object obj)
	{
        if (getLog().isInfoEnabled())
        {
            String toString = safeToString(obj);
            getLog().info(message + " : " + toString);
        }
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#safeInfo(String, Object, Throwable)
	 */
	public void safeInfo(String message, Object obj, Throwable t)
	{
        if (getLog().isInfoEnabled())
        {
            String toString = safeToString(obj);
            getLog().info(message + " : " + toString, t);
        }
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#safeWarn(String, Object)
	 */
	public void safeWarn(String message, Object obj)
	{
        if (getLog().isWarnEnabled())
        {
            String toString = safeToString(obj);
            getLog().warn(message + " : " + toString);
        }
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#safeWarn(String, Object, Throwable)
	 */
	public void safeWarn(String message, Object obj, Throwable t)
	{
        if (getLog().isWarnEnabled())
        {
            String toString = safeToString(obj);
            getLog().warn(message + " : " + toString, t);
        }
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#safeError(String, Object)
	 */
	public void safeError(String message, Object obj)
	{
        if (getLog().isErrorEnabled())
        {
            String toString = safeToString(obj);
            getLog().error(message + " : " + toString);
        }
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#safeError(String, Object, Throwable)
	 */
	public void safeError(String message, Object obj, Throwable t)
	{
        if (getLog().isErrorEnabled())
        {
            String toString = safeToString(obj);
            getLog().error(message + " : " + toString, t);
        }
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#safeFatal(String, Object)
	 */
	public void safeFatal(String message, Object obj)
	{
        if (getLog().isFatalEnabled())
        {
            String toString = safeToString(obj);
            getLog().fatal(message + " : " + toString);
        }
	}

	/**
	 * @see org.apache.ojb.broker.util.logging.Logger#safeFatal(String, Object, Throwable)
	 */
	public void safeFatal(String message, Object obj, Throwable t)
	{
        if (getLog().isFatalEnabled())
        {
			String toString = safeToString(obj);
            getLog().fatal(message + " : " + toString, t);
        }
	}

    /**
     * provides a safe toString
     */ 
	private String safeToString(Object obj)
	{
		String toString = null;
		if (obj != null)
		{
		    try
		    {
		        toString = obj.toString();
		    }
		    catch (Throwable ex)
		    {
		        toString = "BAD toString() impl for " + obj.getClass().getName();
		    }
		}
		return toString;
	}

	/**
	 * @see org.apache.ojb.broker.util.configuration.Configurable#configure(Configuration)
	 */
	public void configure(Configuration config) throws ConfigurationException
	{
	    // do nothing
	}
}
