package org.apache.ojb.otm.core;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.otm.OTMKit;
import org.apache.ojb.otm.OTMConnection;

/**
 *  Transaction delivers the core function of OTMKit - to manage objects within the
 *  context of a transaction.
 *
 *  @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 */
public class Transaction
{
    private boolean _isInProgress;
    private List _listeners;
    private List _connections;
    private OTMKit _kit;

    public Transaction()
    {
        _listeners = new ArrayList();
        _connections = new ArrayList();
    }

    public OTMKit getKit()
    {
        return _kit;
    }

    public void setKit(OTMKit kit)
    {
        _kit = kit;
    }

    public void begin()
            throws TransactionException
    {
        if (_isInProgress)
        {
            throw new TransactionInProgressException(
                "Transaction already in progress, cannot restart");
        }

        _isInProgress = true;

        for (Iterator iterator = _connections.iterator(); iterator.hasNext();)
        {
            BaseConnection connection = (BaseConnection) iterator.next();
            connection.transactionBegin();
        }

        for (Iterator iterator = _listeners.iterator(); iterator.hasNext();)
        {
            TransactionListener listener = (TransactionListener) iterator.next();
            listener.transactionBegan(this);
        }
    }

    /**
     *
     *  Commit this transaction. A commit notifies all listeners of this transaction. It then
     *  initiates a two phase commit on connections. Since, connections cannot be associated to
     *  more than one transaction at any given point in time, there is no neccessity to identify
     *  transaction.
     *
     */
    public void commit()
            throws TransactionException
    {
        if (!_isInProgress)
        {
            throw new TransactionNotInProgressException(
                "Transaction not in progress, nothing to commit");
        }

        for (Iterator iterator = _listeners.iterator(); iterator.hasNext();)
        {
            TransactionListener listener = (TransactionListener) iterator.next();

            listener.transactionCommitting(this);
        }

        for (Iterator iterator = _connections.iterator(); iterator.hasNext();)
        {
            BaseConnection connection = (BaseConnection) iterator.next();
            ConcreteEditingContext context =
                    (ConcreteEditingContext) connection.getEditingContext();

            context.commit();
            connection.transactionPrepare();
        }

        for (Iterator iterator = _connections.iterator(); iterator.hasNext();)
        {
            BaseConnection connection = (BaseConnection) iterator.next();

            connection.transactionCommit();
            connection.setTransaction(null);
        }

        _connections.clear();
        _isInProgress = false;
    }

    /**
     *
     *  Checkpoint this transaction.
     *
     */
    public void checkpoint()
            throws TransactionException
    {
        if (!_isInProgress)
        {
            throw new TransactionNotInProgressException(
                "Transaction not in progress, cannot checkpoint");
        }

        for (Iterator iterator = _connections.iterator(); iterator.hasNext();)
        {
            BaseConnection connection = (BaseConnection) iterator.next();
            ConcreteEditingContext context =
                    (ConcreteEditingContext) connection.getEditingContext();

            context.checkpoint();
        }
    }

    /**
     *
     * Rollback this transaction. A rollback on the transaction, notifies all its listeners. It,
     * then initiates a rollback on all associated connections.
     *
     */
    public void rollback()
            throws TransactionException
    {
        if (!_isInProgress)
        {
            throw new TransactionNotInProgressException(
                "Transaction not in progress, nothing to commit");
        }

        for (Iterator iterator = _listeners.iterator(); iterator.hasNext();)
        {
            TransactionListener listener = (TransactionListener) iterator.next();
            listener.transactionRollingBack(this);
        }

        for (Iterator iterator = _connections.iterator(); iterator.hasNext();)
        {
            BaseConnection connection = (BaseConnection) iterator.next();
            ConcreteEditingContext context =
                    (ConcreteEditingContext) connection.getEditingContext();

            context.rollback();
            connection.transactionRollback();
            connection.setTransaction(null);
        }

        _connections.clear();
        _isInProgress = false;
    }

    public boolean isInProgress()
    {
        return _isInProgress;
    }
    
    /**
     *
     *  Associate a connection to this transaction. A OTMConnection can be registered to atmost one
     *  transaction, while a transaction can manage multiple connections.
     *
     *  @param connection       the connection to register
     *
     */
    public void registerConnection(OTMConnection connection)
    {
        Transaction connectionTx = connection.getTransaction();
        if ((connectionTx != null) && (connectionTx != this))
        {
            throw new TransactionException(
                "Attempt to re-assign a different transaction to a open connection");
        }

        if (!_connections.contains(connection))
        {
            _connections.add(connection);

            connection.setTransaction(this);

        }
    }

    /**
     *
     *  Adds a listener to this transaction. Listeners get boundary notifications of this
     *  transaction.
     *
     *  @param listener         the listener of this transaction
     *
     */
    public void registerListener(TransactionListener listener)
    {
        if (_listeners.indexOf(listener) < 0)
        {
            _listeners.add(listener);
        }
    }
}
