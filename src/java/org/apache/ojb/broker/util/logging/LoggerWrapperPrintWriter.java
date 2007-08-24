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

import java.io.PrintWriter;

import org.apache.commons.lang.BooleanUtils;

/**
 * Extremely simple piggyback for OJB Logger interface to provide PrintWriter dito.
 *
 * @author <a href="mailto:mkalen@apache.org">Martin Kal&eacute;n</a>
 * @version CVS $Id: LoggerWrapperPrintWriter.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 * @since OJB 1.0.4, 2005-apr-30
 */
public class LoggerWrapperPrintWriter extends PrintWriter
{

    private static final String LINESEP = System.getProperty("line.separator");
    private static final int DEFAULT_LEVEL = Logger.INFO;

    private final Logger logger;
    private final int level;
    private final boolean filterEverything;

    /**
     * Construct a new PrintWriter piggyback for the specified OJB logger.
     * @param logger the logger to which all PrintWriter events should be sent to
     * @param level the log level for PrintWriter events, can only be specified
     * as a single level since ther is no priority concept in PrintWriter API
     */
    public LoggerWrapperPrintWriter(Logger logger, int level)
    {
        super(System.out); // dummy, must initialize stream
        this.logger = logger;
        this.level = level;
        filterEverything = !logger.isEnabledFor(level);
    }

    public LoggerWrapperPrintWriter(Logger logger)
    {
        this(logger, DEFAULT_LEVEL);
    }

    private void log(String s)
    {
        switch (level)
        {
            case Logger.FATAL:
                logger.fatal(s);
                break;

            case Logger.ERROR:
                logger.error(s);
                break;

            case Logger.WARN:
                logger.warn(s);
                break;

            case Logger.INFO:
                logger.info(s);
                break;

            case Logger.DEBUG:
                logger.debug(s);
                break;

            default:
                throw new RuntimeException("Internal OJB fault. Logger API does not permit level "
                    + level);
        }
    }

    private void logLn(String s)
    {
        if (s != null)
        {
            log(s);
        }
        log(LINESEP);
    }


    public void println()
    {
        if (!filterEverything)
        {
            logLn(null);
        }
    }

    public void print(char c)
    {
        if (!filterEverything)
        {
            log(new String(new char[]{c}));
        }
    }

    public void println(char c)
    {
        if (!filterEverything)
        {
            logLn(new String(new char[]{c}));
        }
    }

    public void print(double v)
    {
        if (!filterEverything)
        {
            log(Double.toString(v));
        }
    }

    public void println(double v)
    {
        if (!filterEverything)
        {
            logLn(Double.toString(v));
        }
    }

    public void print(float v)
    {
        if (!filterEverything)
        {
            log(Float.toString(v));
        }
    }

    public void println(float v)
    {
        if (!filterEverything)
        {
            logLn(Float.toString(v));
        }
    }

    public void print(int i)
    {
        if (!filterEverything)
        {
            log(Integer.toString(i));
        }
    }

    public void println(int i)
    {
        if (!filterEverything)
        {
            logLn(Integer.toString(i));
        }
    }

    public void print(long l)
    {
        if (!filterEverything)
        {
            log(Long.toString(l));
        }
    }

    public void println(long l)
    {
        if (!filterEverything)
        {
            logLn(Long.toString(l));
        }
    }

    public void print(boolean b)
    {
        if (!filterEverything)
        {
            log(BooleanUtils.toStringTrueFalse(b));
        }
    }

    public void println(boolean b)
    {
        if (!filterEverything)
        {
            logLn(BooleanUtils.toStringTrueFalse(b));
        }
    }

    public void print(char[] chars)
    {
        if (!filterEverything)
        {
            log(new String(chars));
        }
    }

    public void println(char[] chars)
    {
        if (!filterEverything)
        {
            logLn(new String(chars));
        }
    }

    public void print(Object o)
    {
        if (!filterEverything && o != null)
        {
            log(o.toString());
        }
    }

    public void println(Object o)
    {
        if (!filterEverything && o != null)
        {
            logLn(o.toString());
        }
    }

    public void print(String s)
    {
        if (!filterEverything)
        {
            log(s);
        }
    }

    public void println(String s)
    {
        if (!filterEverything)
        {
            logLn(s);
        }
    }

    public void write(int i)
    {
        if (!filterEverything)
        {
            print(i);
        }
    }

    public void write(String s)
    {
        if (!filterEverything)
        {
            print(s);
        }
    }

    public void write(char[] chars)
    {
        if (!filterEverything)
        {
            print(chars);
        }
    }

    public void write(char[] chars, int i, int i1)
    {
        if (!filterEverything)
        {
            log(new String(chars, i, i1));
        }
    }

    public void write(String s, int i, int i1)
    {
        if (!filterEverything)
        {
            log(s.substring(i, i1));
        }
    }

}
