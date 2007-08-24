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

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * This class is a {@link Logger} implementation based on a {@link StringBuffer}. All
 * logging method calls are written in the buffer. With method {@link #flushLogBuffer()}
 * the buffer will be cleared and the buffered log statements returned as string.
 *
 * @version $Id: StringBufferLoggerImpl.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public class StringBufferLoggerImpl extends PoorMansLoggerImpl
{
    protected String EOL = SystemUtils.LINE_SEPARATOR;
    private StringBuffer buffer;
    private boolean errorLog;

    public StringBufferLoggerImpl(String name)
    {
        super(name);
        buffer = new StringBuffer(1000);
    }

    /**
     * Log all statements in a {@link StringBuffer}.
     */
    protected void log(String aLevel, Object obj, Throwable t)
    {
        buffer.append(BRAKE_OPEN).append(getName()).append(BRAKE_CLOSE).append(aLevel);
        if (obj != null && obj instanceof Throwable)
        {
            try
            {
                buffer.append(((Throwable) obj).getMessage()).append(EOL);
                ((Throwable) obj).printStackTrace();
            }
            catch (Throwable ignored)
            {
                /*logging should be failsafe*/
            }
        }
        else
        {
            try
            {
                buffer.append(obj).append(EOL);
            }
            catch(Exception e)
            {
                // ignore
            }
        }

        if (t != null)
        {
            try
            {
                buffer.append(t.getMessage()).append(EOL);
                buffer.append(ExceptionUtils.getFullStackTrace(t)).append(EOL);
            }
            catch (Throwable ignored)
            {
                /*logging should be failsafe*/
            }
        }
        if(!errorLog && (aLevel.equals(STR_ERROR) || aLevel.equals(STR_FATAL)))
        {
            errorLog = true;
        }
    }

    /**
     * Returns <em>true</em> if one or more error/fatal log method calls
     * have been logged in the buffer.
     */
    public boolean isErrorLog()
    {
        return errorLog;
    }

    /**
     * Returns all buffered log statements as string and clear the buffer.
     */
    public String flushLogBuffer()
    {
        String result = buffer.toString();
        buffer = new StringBuffer(1000);
        return result;
    }
}
