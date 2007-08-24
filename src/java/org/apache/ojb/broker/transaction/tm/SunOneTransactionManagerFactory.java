package org.apache.ojb.broker.transaction.tm;

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

import org.apache.ojb.broker.transaction.tm.AbstractTransactionManagerFactory;

/**
 * SunOne {@link javax.transaction.TransactionManager} lookup.
 *
 * @author Rice Yeh
 * @version $Id: SunOneTransactionManagerFactory.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class SunOneTransactionManagerFactory extends AbstractTransactionManagerFactory
{
    private static final String[][] config = {
        {"SunOne", "getTransactionManagerImpl", "com.sun.jts.jta.TransactionManagerImpl"}};

    /**
     * @see org.apache.ojb.broker.transaction.tm.AbstractTransactionManagerFactory#getLookupInfo
     */
    public String[][] getLookupInfo()
    {
        return config;
    }
}
