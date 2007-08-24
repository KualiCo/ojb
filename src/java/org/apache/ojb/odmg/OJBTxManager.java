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

import org.apache.ojb.broker.util.configuration.Configurable;

/**
 *  The OJBTxManager defines the contract for associating the caller with the
 *  current or new transaction in ODMG.
 *
 * @author Matthew Baird
 * @version $Id: OJBTxManager.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public interface OJBTxManager extends Configurable
{
	/**
     * Returns the current transaction for the calling thread.
     * @throws org.odmg.TransactionNotInProgressException if no transaction was found.
     */
    TransactionImpl getCurrentTransaction();

    /**
     * Returns the current transaction for the calling thread or <code>null</code>
     * if no transaction was found.
     */
    TransactionImpl getTransaction();

    void registerTx(TransactionImpl tx);
    void deregisterTx(Object token);
    void abortExternalTx(TransactionImpl odmgTrans);
}
