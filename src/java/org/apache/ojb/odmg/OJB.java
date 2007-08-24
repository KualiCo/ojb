package org.apache.ojb.odmg;

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

import org.apache.ojb.broker.util.factory.ConfigurableFactory;
import org.odmg.Implementation;


/**
 * Facade to the persistence ObjectServer system.
 * Implements the factory interface for a particular ODMG implementation.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: OJB.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class OJB extends ConfigurableFactory
{
    private static OJB instance;

    static
    {
        instance = new OJB();
    }

    /**
     * protected Constructor: use static factory method
     * getInstance() to obtain an instance of {@link Implementation}
     */
    protected OJB()
    {
    }

    /**
     * Return new instance of the {@link org.odmg.Implementation} class.
     * The used implementation class can be specified in OJB properties file.
     */
    public static ImplementationExt getInstance()
    {
        return (ImplementationExt) instance.createNewInstance();
    }

    protected String getConfigurationKey()
    {
        return "ImplementationClass";
    }
}
