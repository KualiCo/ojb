package org.apache.ojb.broker.transaction;

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

import javax.transaction.Transaction;

/**
 * Class give support in transaction handling.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: OJBTxManager.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public interface OJBTxManager
{
    void registerTx(OJBTxObject obj);

    void deregisterTx(OJBTxObject obj);

    /**
     * Returns the current transaction for the calling thread or <code>null</code>
     * if no transaction was found.
     *
     * @see #getCurrentJTATransaction
     */
    Transaction getJTATransaction();

    /**
     * Returns the current transaction for the calling thread.
     *
     * @see #getJTATransaction
     * @throws TransactionNotInProgressException if no transaction was found.
     */
    Transaction getCurrentJTATransaction();
}
