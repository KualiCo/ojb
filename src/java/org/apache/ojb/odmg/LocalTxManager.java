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

import java.util.Hashtable;

import org.apache.ojb.broker.util.configuration.Configuration;
import org.odmg.TransactionNotInProgressException;

/**
 * In a non-appserver environment, without a transaction manager, we can
 * safely associate the current ODMG transaction with the calling thread.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 * @version $Id: LocalTxManager.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class LocalTxManager implements OJBTxManager
{
    /**
     * Internal table which provides mapping between threads and transactions.
     * This is required because a Thread can join a Transaction already in
     * progress.
     * If the thread joins a transaction, then "getTransaction()" should return
     * the apropriate one.  The only way we can ensure that is by keeping hold
     * of the txTable.
     */
    private static TransactionTable tx_table = new TransactionTable();

    public LocalTxManager()
    {

    }

    /**
     * Returns the current transaction for the calling thread.
     *
     * @throws org.odmg.TransactionNotInProgressException
     *          {@link org.odmg.TransactionNotInProgressException} if no transaction was found.
     */
    public TransactionImpl getCurrentTransaction()
    {
        TransactionImpl tx = tx_table.get(Thread.currentThread());
        if(tx == null)
        {
            throw new TransactionNotInProgressException("Calling method needed transaction, but no transaction found for current thread :-(");
        }
        return tx;
    }

    /**
     * Returns the current transaction for the calling thread or <code>null</code>
     * if no transaction was found.
     */
    public TransactionImpl getTransaction()
    {
        return  tx_table.get(Thread.currentThread());
    }

    /**
     * add the current transaction to the map key'd by the calling thread.
     */
    public void registerTx(TransactionImpl tx)
    {
        tx_table.put(Thread.currentThread(), tx);
    }

    /**
     * remove the current transaction from the map key'd by the calling thread.
     */
    public void deregisterTx(Object token)
    {
        tx_table.remove(Thread.currentThread());
    }

    /**
     * included to keep interface contract consistent.
     */
    public void abortExternalTx(TransactionImpl odmgTrans)
    {
        /**
         * no op
         */
    }

    public void configure(Configuration config)
    {

    }


    //=======================================================
    // inner class
    //=======================================================
    /**
     * TransactionTable provides a mapping between the calling
     * thread and the Transaction it is currently using.
     * One thread can be joined with one transaction at
     * a certain point in time. But a thread can join with
     * different transactions subsequently.
     * This mapping from threads to Transactions is based on ODMG.
     *
     * @author Thomas Mahler & David Dixon-Peugh
     */
    static final class TransactionTable
    {
        /**
         * the internal Hashtable mapping Transactions to threads
         */
        private Hashtable m_table = new Hashtable();

        /**
         * Creates new TransactionTable
         */
        public TransactionTable()
        {
        }

        /**
         * Retreive a Transaction associated with a thread.
         *
         * @param key_thread The thread to lookup.
         * @return The transaction associated with the thread.
         */
        public TransactionImpl get(Thread key_thread)
        {
            return (TransactionImpl) m_table.get(key_thread);
        }

        /**
         * Store the Thread/Transaction pair in the TransactionTable
         *
         * @param key_thread Thread that the transaction will be associated to
         * @param value_tx   Transaction to be associated with the thread
         */
        public void put(Thread key_thread, TransactionImpl value_tx)
        {
            m_table.put(key_thread, value_tx);
        }

        /**
         * Remove the entry for the thread
         *
         * @param key_thread Thread to be removed.
         */
        public void remove(Thread key_thread)
        {
            m_table.remove(key_thread);
        }
    }
}
