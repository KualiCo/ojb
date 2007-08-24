package xdoclet.modules.ojb;

/* Copyright 2004-2005 The Apache Software Foundation
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

/**
 * Helper class for logging and message output.
 * 
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class LogHelper
{
    /**
     * Logs the given debug message to stdout (if verbose is on) and to the log for the given class
     * (if the log level has been set to debug or higher).
     * 
     * @param alsoStdout Whether to also put the message to stdout
     * @param clazz      The clazz
     * @param posInfo    The position info, e.g. method name
     * @param msg        The message
     */
    public static void debug(boolean alsoStdout, Class clazz, String posInfo, Object msg)
    {
        if (alsoStdout)
        {
            System.out.println(msg.toString());
        }

        String name = clazz.getName();

        if (posInfo != null)
        {    
            name += "." + posInfo;
        }

        Log log = LogFactory.getLog(name);

        if (log.isDebugEnabled())
        {
            log.debug(msg);
        }
    }

    /**
     * Logs the given warning to stdout and to the log for the given class
     * (if the log level has been set to warn or higher).
     * 
     * @param alsoStdout Whether to also put the message to stdout
     * @param clazz      The clazz
     * @param posInfo    The position info, e.g. method name
     * @param msg        The message
     */
    public static void warn(boolean alsoStdout, Class clazz, String posInfo, Object msg)
    {
        if (alsoStdout)
        {
            System.out.println("Warning: "+msg.toString());
        }

        String name = clazz.getName();

        if (posInfo != null)
        {    
            name += "." + posInfo;
        }

        Log log = LogFactory.getLog(name);

        if (log.isWarnEnabled())
        {
            log.warn(msg);
        }
    }
}
