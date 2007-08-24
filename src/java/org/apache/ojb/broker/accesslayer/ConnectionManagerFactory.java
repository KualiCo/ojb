package org.apache.ojb.broker.accesslayer;

/* Copyright 2003-2005 The Apache Software Foundation
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

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.util.factory.ConfigurableFactory;

/**
 * Factory for {@link org.apache.ojb.broker.accesslayer.ConnectionManagerIF}
 * implementations.
 * <br/>
 * See also {@link org.apache.ojb.broker.accesslayer.ConnectionFactoryFactory}
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ConnectionManagerFactory.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class ConnectionManagerFactory extends ConfigurableFactory
{
    private static ConnectionManagerFactory singleton;

    public static synchronized ConnectionManagerFactory getInstance()
    {
        if (singleton == null)
        {
            singleton = new ConnectionManagerFactory();
        }
        return singleton;
    }

    protected String getConfigurationKey()
    {
        return "ConnectionManagerClass";
    }

    public ConnectionManagerIF createConnectionManager(PersistenceBroker broker)
    {
        return (ConnectionManagerIF) createNewInstance(PersistenceBroker.class, broker);
    }
}
