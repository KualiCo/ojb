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

import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;

/**
 * this is a most simple Logger implementation.
 * All output is directed to System.out.
 *
 * @author Thomas Mahler
 * @version $Id: PoorMansLoggerImpl.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public class PoorMansLoggerImpl implements Logger
{
    protected static final String STR_DEBUG = "DEBUG";
    protected static final String STR_INFO = "INFO";
    protected static final String STR_WARN = "WARN";
    protected static final String STR_ERROR = "ERROR";
    protected static final String STR_FATAL = "FATAL";

    protected static final String STR_DEBUG_MSG = "DEBUG: ";
    protected static final String STR_INFO_MSG = "INFO: ";
    protected static final String STR_WARN_MSG = "WARN: ";
    protected static final String STR_ERROR_MSG = "ERROR: ";
    protected static final String STR_FATAL_MSG = "FATAL: ";

    protected static final String BRAKE_OPEN = "[";
    protected static final String BRAKE_CLOSE = "] ";

    private String name ;

    private int level = 0;

    public PoorMansLoggerImpl( String name)
    {
        this.name = name ;
    }

    protected int getLevel()
    {
        return level;
    }

    void setLevel(int pLevel)
    {
        level = pLevel;
    }

    public String getName()
    {
        return name;
    }

    /**
     * generate a message for loglevel DEBUG
     * @param pObject the message Object
     */
    public void debug(Object pObject)
    {
        debug(pObject, null);
    }

    public void debug(Object message, Throwable t)
    {
        if (DEBUG >= getLevel())
        {
            log(STR_DEBUG_MSG, message, t);
        }
    }

    public void safeDebug(String message,Object obj)
    {
        safeDebug(message,obj,null);
    }

    public void safeDebug(String message,Object obj,Throwable t)
    {
        if(DEBUG >= getLevel())
        {
            String toString = null;
            if(obj != null)
            {
                try
                {
                    toString = obj.toString();
                }
                catch(Throwable throwable)
                {
                    toString = "BAD toString() impl for "+obj.getClass().getName();
                }
            }
            log(STR_DEBUG_MSG,message + " : " + toString,t);
        }
    }


    /**
     * generate a message for loglevel INFO
     * @param pObject the message Object
     */
    public void info(Object pObject)
    {
        info(pObject, null);
    }

    public void info(Object message, Throwable t)
    {
        if (INFO >= getLevel())
        {
            log(STR_INFO_MSG, message, t);
        }
    }

    public void safeInfo(String message,Object obj)
    {
        safeInfo(message,obj,null);
    }

    public void safeInfo(String message,Object obj,Throwable t)
    {
        if(INFO >= getLevel())
        {
            String toString = null;
            if(obj != null)
            {
                try
                {
                    toString = obj.toString();
                }
                catch(Throwable throwable)
                {
                    toString = "BAD toString() impl for "+obj.getClass().getName();
                }
            }
            log(STR_INFO_MSG, message + " : " + toString,t);
        }
    }

    /**
     * generate a message for loglevel WARN
     * @param pObject the message Object
     */
    public void warn(Object pObject)
    {
        warn(pObject, null);
    }

    public void warn(Object message, Throwable t)
    {
        if (WARN >= getLevel())
        {
            log(STR_WARN_MSG, message, t);
        }
    }

    public void safeWarn(String message,Object obj)
    {
        safeWarn(message,obj,null);
    }

    public void safeWarn(String message,Object obj,Throwable t)
    {
        if(WARN >= getLevel())
        {
            String toString = null;
            if(obj != null)
            {
                try
                {
                    toString = obj.toString();
                }
                catch(Throwable throwable)
                {
                    toString = "BAD toString() impl for "+obj.getClass().getName();
                }
            }
            log(STR_WARN_MSG,message + " : " + toString,t);
        }
    }

    /**
     * generate a message for loglevel ERROR
     * @param pObject the message Object
     */
    public void error(Object pObject)
    {
        error(pObject, null);
    }

    public void error(Object message, Throwable t)
    {
        if (ERROR >= getLevel())
        {
            log(STR_ERROR_MSG, message, t);
        }
    }

    public void safeError(String message,Object obj)
    {
        safeError(message,obj,null);
    }

    public void safeError(String message,Object obj,Throwable t)
    {
        if(ERROR >= getLevel())
        {
            String toString = null;
            if(obj != null)
            {
                try
                {
                    toString = obj.toString();
                }
                catch(Throwable throwable)
                {
                    toString = "BAD toString() impl for "+obj.getClass().getName();
                }
            }
            log(STR_ERROR_MSG,message + " : " + toString,t);
        }
    }

    /**
     * generate a message for loglevel FATAL
     * @param pObject the message Object
     */
    public void fatal(Object pObject)
    {
        fatal(pObject, null);
    }

    public void fatal(Object message, Throwable t)
    {
        if (FATAL >= getLevel())
        {
            log(STR_FATAL_MSG, message, t);
        }
    }

    public void safeFatal(String message,Object obj)
    {
        safeFatal(message,obj,null);
    }

    public void safeFatal(String message,Object obj,Throwable t)
    {
        if(FATAL >= getLevel())
        {
            String toString = null;
            if(obj != null)
            {
                try
                {
                    toString = obj.toString();
                }
                catch(Throwable throwable)
                {
                    toString = "BAD toString() impl for "+obj.getClass().getName();
                }
            }
            log(STR_FATAL_MSG,message + " : " + toString,t);
        }
    }


    public boolean isDebugEnabled()
    {
        return isEnabledFor(DEBUG);
    }

    public boolean isEnabledFor(int priority)
    {
        return priority >= getLevel();
    }

    protected void log(String aLevel, Object obj, Throwable t)
    {
        System.out.print(BRAKE_OPEN + name + BRAKE_CLOSE + aLevel);
        if (obj != null && obj instanceof Throwable)
        {
            try
            {
                System.out.println(((Throwable) obj).getMessage());
                ((Throwable) obj).printStackTrace();
            }
            catch (Throwable ignored)
            {
                /*logging should be failsafe*/
            }
        }
        else
        {
            System.out.println(obj);
        }

        if (t != null)
        {
            try
            {
                System.out.println(t.getMessage());
                t.printStackTrace();
            }
            catch (Throwable ignored)
            {
                /*logging should be failsafe*/
            }
        }
    }

    /*
     * @see org.apache.ojb.broker.util.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration config) throws ConfigurationException
    {
        LoggingConfiguration lc = (LoggingConfiguration) config;
        String levelName = lc.getLogLevel(name);
        setLevel(levelName);
    }

	public void setLevel(String levelName)
	{
		if (levelName.equalsIgnoreCase(STR_DEBUG))
		{
		    level = DEBUG;
		}
		else if (levelName.equalsIgnoreCase(STR_INFO))
		{
		    level = INFO;
		}
		else if (levelName.equalsIgnoreCase(STR_WARN))
		{
		    level = WARN;
		}
		else if (levelName.equalsIgnoreCase(STR_ERROR))
		{
		    level = ERROR;
		}
		else if (levelName.equalsIgnoreCase(STR_FATAL))
		{
		    level = FATAL;
		}
		else
		{
		    level = WARN;
		}
	}
}
