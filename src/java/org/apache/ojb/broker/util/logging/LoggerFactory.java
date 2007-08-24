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


/**
 * This is a wrapper thta provides static accessors to LoggerFactoryImpl methods
 *
 * @author Thomas Mahler
 * @author <a href="leandro@ibnetwork.com.br">Leandro Rodrigo Saad Cruz</a>
 * @version $Id: LoggerFactory.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public class LoggerFactory
{

    private static LoggerFactoryImpl getImpl()
    {
        return LoggerFactoryImpl.getInstance();
    }

    /**
     * returns a minimal logger that needs no configuration
     * and can thus be safely used during OJB boot phase
     * (i.e. when OJB.properties have not been loaded).
     * @return Logger the OJB BootLogger
     */
    public static Logger getBootLogger()
    {
        return getImpl().getBootLogger();
    }

    /**
     * returns the default logger. This Logger can
     * be used when it is not appropriate to use a
     * dedicated fresh Logger instance.
     * @return default Logger
     */
    public static Logger getDefaultLogger()
    {
        return getImpl().getDefaultLogger();
    }

    /**
     * returns a Logger. The Logger is named
     * after the full qualified name of input parameter clazz
     * @param clazz the Class which name is to be used as name
     * @return Logger the returned Logger
     */
    public static Logger getLogger(Class clazz)
    {
        return getImpl().getLogger(clazz.getName());
    }

    /**
     * returns a Logger.
     * @param name the name of the Logger
     * @return Logger the returned Logger
     */
    public static Logger getLogger(String name)
    {
        return getImpl().getLogger(name);
    } 

}
