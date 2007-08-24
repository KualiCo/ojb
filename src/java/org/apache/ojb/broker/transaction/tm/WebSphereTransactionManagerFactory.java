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


import org.apache.ojb.broker.transaction.tm.AbstractTransactionManagerFactory;

/**
 * Websphere (4 and above) {@link javax.transaction.TransactionManager} lookup.
 *
 * @author matthew.baird
 * @version $Id: WebSphereTransactionManagerFactory.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class WebSphereTransactionManagerFactory extends AbstractTransactionManagerFactory
{
    /**
     * Support versions (Websphere 4, 5 and >5)
     */
    private static final String[][] config = {
        {"Websphere 4", TM_DEFAULT_METHOD_NAME, "com.ibm.ejs.jts.jta.JTSXA"},
        {"Websphere 5", TM_DEFAULT_METHOD_NAME, "com.ibm.ejs.jts.jta.TransactionManagerFactory"},
        {"Websphere >5", TM_DEFAULT_METHOD_NAME, "com.ibm.ws.Transaction.TransactionManagerFactory"}};

    /**
     * @see org.apache.ojb.broker.transaction.tm.AbstractTransactionManagerFactory#getLookupInfo
     */
    public String[][] getLookupInfo()
    {
        return config;
    }
}
