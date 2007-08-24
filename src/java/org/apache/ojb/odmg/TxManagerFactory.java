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

/**
 * The TxManagerFactory is responsible for creating instances of
 * TransactionManagers. Set the "OJBTxManagerClass" property in
 * OJB.properties in order to install a new TxManagerFactory
 * 
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 * @version $Id: TxManagerFactory.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class TxManagerFactory extends ConfigurableFactory
{
    private static TxManagerFactory singleton;
    private OJBTxManager manager;

    static
    {
        singleton = new TxManagerFactory();
    }

    private TxManagerFactory()
    {
        manager = (OJBTxManager) this.createNewInstance();
    }

    private OJBTxManager getManager()
    {
        return manager;
    }

    public synchronized static OJBTxManager instance()
    {
        return singleton.getManager();
    }

    protected String getConfigurationKey()
    {
        return "OJBTxManagerClass";
    }
}
