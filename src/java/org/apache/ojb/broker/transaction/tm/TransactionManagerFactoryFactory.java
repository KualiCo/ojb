package org.apache.ojb.broker.transaction.tm;

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

import org.apache.ojb.broker.util.factory.ConfigurableFactory;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.transaction.tm.TransactionManagerFactory;

public class TransactionManagerFactoryFactory
{
    private static Logger log = LoggerFactory.getLogger(TransactionManagerFactoryFactory.class);
    private static TransactionManagerFactory tmInstance;
    static
    {
        try
        {
            tmInstance = new TMFactoryFactory().createTransactionManagerFactory();
        }
        catch (Exception e)
        {
            log.error("Instantiation of TransactionManagerFactory failed", e);
        }
    }

    public synchronized static TransactionManagerFactory instance()
    {
        return tmInstance;
    }

    public static class TMFactoryFactory extends ConfigurableFactory
    {
        protected String getConfigurationKey()
        {
            return "JTATransactionManagerClass";
        }

        protected TransactionManagerFactory createTransactionManagerFactory()
        {
            return (TransactionManagerFactory) this.createNewInstance();
        }
    }
}
