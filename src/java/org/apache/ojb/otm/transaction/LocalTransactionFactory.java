package org.apache.ojb.otm.transaction;

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

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.otm.OTMConnection;
import org.apache.ojb.otm.core.BaseConnection;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.core.TransactionException;

import java.util.HashMap;

/**
 *
 * Factory for local transactions. Each OTMConnection is associated with exactly one transaction.
 *
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 *
 */
public class LocalTransactionFactory implements TransactionFactory
{

    private HashMap _transactionMap;

    public LocalTransactionFactory ()
    {
        _transactionMap = new HashMap();
    }

    /**
     * @see org.apache.ojb.otm.transaction.TransactionFactory#getTransactionForConnection(OTMConnection)
     */
    public Transaction getTransactionForConnection (OTMConnection connection)
    {
        if (!(connection instanceof BaseConnection))
        {
			StringBuffer msg = new StringBuffer();
			msg.append("Unknown connection type: ");
			if (connection != null)
				msg.append(connection.getClass().getName());
			else
				msg.append(" null. Make sure you pass a non-null OTMConnection to this method. An OTMConnection can be acquired by calling acquireConnection (PBKey pbKey)");
            throw new TransactionFactoryException(msg.toString());
        }

        Transaction tx = (Transaction) _transactionMap.get(connection);
        if (tx == null)
        {
            tx = new Transaction();
            _transactionMap.put(connection, tx);
        }
        // ensure that this connection is registered into this transaction
        tx.registerConnection(connection);
        return tx;
    }


    /**
     * @see org.apache.ojb.otm.transaction.TransactionFactory#acquireConnection(PBKey)
     */
    public OTMConnection acquireConnection (PBKey pbKey)
    {
        OTMConnection newConnection = new LocalConnection(pbKey);
        // Ensure the transaction is established for this connection.
        getTransactionForConnection(newConnection);
        return newConnection;
    }

    /**
     *
     * Represents a local connection. This is a private static inner class to restrict visibility
     * to others.
     *
     * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
     *
     */
    private static class LocalConnection extends BaseConnection
    {
        public LocalConnection (PBKey pbKey)
        {
            super(pbKey);
        }


        /**
         * @see org.apache.ojb.otm.core.BaseConnection#transactionBegin()
         */
        public void transactionBegin() throws TransactionException
        {
            getKernelBroker().beginTransaction();
        }
        
        /**
         * @see org.apache.ojb.otm.core.BaseConnection#transactionPrepare()
         */
        public void transactionPrepare() throws TransactionException
        {
            // Nothing to do!
        }

        /**
         * @see org.apache.ojb.otm.core.BaseConnection#transactionCommit()
         */
        public void transactionCommit() throws TransactionException
        {
            getKernelBroker().commitTransaction();
        }

        /**
         * @see org.apache.ojb.otm.core.BaseConnection#transactionRollback()
         */
        public void transactionRollback() throws TransactionException
        {
            getKernelBroker().abortTransaction();
        }

    }
}
