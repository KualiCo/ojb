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

import java.util.HashMap;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.transaction.tm.TransactionManagerFactoryFactory;
import org.apache.ojb.broker.transaction.tm.TransactionManagerFactoryException;
import org.apache.ojb.otm.OTMConnection;
import org.apache.ojb.otm.core.BaseConnection;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.core.TransactionException;

/**
 * Factory for OTM Transactions within a managed environment (JTA).
 *
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 *
 */
public abstract class ManagedTransactionFactory implements TransactionFactory
{

    private HashMap _transactionMap;
    private TransactionManager tm;

	/**
	 * Constructor for ManagedTransactionFactory.
	 */
	public ManagedTransactionFactory()
	{
		_transactionMap = new HashMap();
	}


    //////////////////////////////////////////////////////
    // TransactionFactory protocol
    //////////////////////////////////////////////////////

	/**
	 * @see org.apache.ojb.otm.transaction.TransactionFactory#getTransactionForConnection(OTMConnection)
	 */
	public Transaction getTransactionForConnection(OTMConnection connection)
	{
        if (!(connection instanceof BaseConnection))
        {
            throw new TransactionFactoryException("Unknown connection type");
        }
        BaseConnection baseConnection = (BaseConnection) connection;

		javax.transaction.Transaction jtaTx = getJTATransaction();
        if (jtaTx == null)
        {
            throw new TransactionFactoryException("Unable to get the JTA Transaction");
        }

        Transaction tx = (Transaction) _transactionMap.get(jtaTx);
        if (tx == null)
        {
            tx = new Transaction();
            _transactionMap.put(jtaTx, tx);
        }

        // ensure that this connection is registered into this transaction
        tx.registerConnection(baseConnection);
        return tx;
	}

    /**
     * @see org.apache.ojb.otm.transaction.TransactionFactory#acquireConnection
     */
    public OTMConnection acquireConnection(PBKey pbKey)
    {
        return new ManagedConnection(pbKey);
    }


    //////////////////////////////////////////
    // Other operations
    //////////////////////////////////////////

    public javax.transaction.Transaction getJTATransaction()
    {
        if(tm == null)
        {
            try
            {
                tm = TransactionManagerFactoryFactory.instance().getTransactionManager();
            }
            catch (TransactionManagerFactoryException e)
            {
                throw new TransactionFactoryException("Can't instantiate TransactionManagerFactory", e);
            }
        }
        try
        {
            return tm.getTransaction();
        }
        catch(SystemException e)
        {
            throw new TransactionFactoryException("Error acquiring JTA Transaction", e);
        }
    }

    private static class ManagedConnection extends BaseConnection
    {

        public ManagedConnection (PBKey pbKey)
        {
            super(pbKey);
        }

        /**
         * @see org.apache.ojb.otm.core.BaseConnection#transactionBegin()
         */
        public void transactionBegin() throws TransactionException
        {
            // Nothing to do!
        }

        /**
         * @see org.apache.ojb.otm.core.BaseConnection#transactionPrepare()
         */
        public void transactionPrepare() throws TransactionException
        {
            // Nothing to do, since all resources are managed by JTS and will be committed
            // directly.
        }

        /**
         * @see org.apache.ojb.otm.core.BaseConnection#transactionCommit()
         */
        public void transactionCommit() throws TransactionException
        {
            // Nothing to do, since all resources are managed by JTS and will be committed
            // directly.
        }

        /**
         * @see org.apache.ojb.otm.core.BaseConnection#transactionRollback()
         */
        public void transactionRollback() throws TransactionException
        {
            // Nothing to do, since all resources are managed by JTS and will be rolled back
            // directly.
        }

    }
}
