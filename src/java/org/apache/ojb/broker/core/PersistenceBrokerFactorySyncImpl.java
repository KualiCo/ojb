package org.apache.ojb.broker.core;

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

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.ojb.broker.PBFactoryException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBrokerInternal;
import org.apache.ojb.broker.TransactionAbortedException;
import org.apache.ojb.broker.TransactionInProgressException;
import org.apache.ojb.broker.TransactionNotInProgressException;
import org.apache.ojb.broker.accesslayer.ConnectionManagerIF;
import org.apache.ojb.broker.transaction.tm.TransactionManagerFactoryException;
import org.apache.ojb.broker.transaction.tm.TransactionManagerFactoryFactory;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Workaround for participate the PB-api in JTA {@link javax.transaction.Transaction transaction} by
 * implementing the {@link javax.transaction.Synchronization} interface.
 * <br/>
 * This may will be deprecated when we implemented a full JCA compliant connector.
 * <br/>
 * When a new {@link org.apache.ojb.broker.PersistenceBroker} instance is created in method
 * {@link #wrapBrokerWithPoolingHandle}
 * the given PB instance is wrapped with {@link PersistenceBrokerSyncImpl} before it was put to the PB-pool.
 * When a PB instance was requested class try to lookup the current JTA transaction in
 * {@link #wrapRequestedBrokerInstance} before the pooled PB instance was wrapped with the PB handle.
 * If a running tx was found the PB instance was registered with the transaction using the
 * {@link Synchronization} interface.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PersistenceBrokerFactorySyncImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PersistenceBrokerFactorySyncImpl extends PersistenceBrokerFactoryDefaultImpl
{
    private Logger log = LoggerFactory.getLogger(PersistenceBrokerFactorySyncImpl.class);
    private TransactionManager txMan;
    private TxRegistry txRegistry;

    public PersistenceBrokerFactorySyncImpl()
    {
        super();
        try
        {
            txMan = TransactionManagerFactoryFactory.instance().getTransactionManager();
        }
        catch (TransactionManagerFactoryException e)
        {
            throw new PBFactoryException("Can't instantiate TransactionManager of managed environment", e);
        }
        txRegistry = new TxRegistry();
    }

    public PersistenceBrokerInternal createPersistenceBroker(PBKey pbKey) throws PBFactoryException
    {
        /*
        try to find a valid PBKey, if given key does not full match
        */
        pbKey = BrokerHelper.crossCheckPBKey(pbKey);
        /*
        arminw:
        First try to find a running JTA-tx. If a tx was found we try to find
        an associated PB instance. This ensures that in a running tx
        always the same PB instance was used.
        If no tx was found we lookup a instance from pool.
        All used PB instances always be wrapped with a "PBHandle"
        */
        Transaction tx;
        try
        {
            // search for an active tx
            tx = searchForValidTx();
        }
        catch (SystemException e)
        {
            throw new PBFactoryException("Can't create PB instance, failure while lookup" +
                    " running JTA transaction",e);
        }
        PersistenceBrokerSyncImpl obtainedBroker = null;
        PersistenceBrokerSyncHandle result;
        if (tx != null)
        {
            // try to find a broker instance already used by current tx
            obtainedBroker = txRegistry.findBroker(tx, pbKey);
        }

        if(obtainedBroker == null || obtainedBroker.isClosed())
        {
            // we have to lookup new PB instance with wrapped with handle
            // method #wrapRequestedBrokerInstance wraps the new instance
            // with a handle
            result = (PersistenceBrokerSyncHandle) super.createPersistenceBroker(pbKey);
        }
        else
        {
            // we found a PB instance that was already in use within the same JTA-tx
            // so we only return a new handle
            result = new PersistenceBrokerSyncHandle(obtainedBroker);
        }
        return result;
    }

    protected PersistenceBrokerInternal wrapBrokerWithPoolingHandle(PersistenceBrokerInternal broker, KeyedObjectPool pool)
    {
        // wrap real PB instance with an extended version of pooling PB
        return new PersistenceBrokerSyncImpl(broker, pool);
    }

    protected PersistenceBrokerInternal wrapRequestedBrokerInstance(PersistenceBrokerInternal broker)
    {
        // all PB instance should be of this type
        if (!(broker instanceof PersistenceBrokerSyncImpl))
        {
            throw new PBFactoryException("Expect instance of " + PersistenceBrokerSyncImpl.class
                    + ", found " + broker.getClass());
        }
        /*
        Before we return the PB handle, we jump into the running JTA tx
        */
        PersistenceBrokerSyncImpl pb = (PersistenceBrokerSyncImpl) broker;
        try
        {
            // search for an active tx
            Transaction tx = searchForValidTx();
            if (tx != null)
            {
                txRegistry.register(tx, pb);
                try
                {
                    pb.internBegin();
                }
                catch (Exception e)
                {
                    /*
                    if something going wrong with pb-tx, we rollback the
                    whole JTA tx
                    */
                    log.error("Unexpected exception when start intern pb-tx", e);
                    try
                    {
                        tx.setRollbackOnly();
                    }
                    catch (Throwable ignore)
                    {
                    }
                    throw new PBFactoryException("Unexpected exception when start intern pb-tx", e);
                }
            }
        }
        catch (Exception e)
        {
            if(e instanceof PBFactoryException)
            {
                throw (PBFactoryException) e;
            }
            else
            {
                throw new PBFactoryException("Error while try to participate in JTA transaction", e);
            }
        }
        return new PersistenceBrokerSyncHandle(pb);
    }

    private Transaction searchForValidTx() throws SystemException
    {
        Transaction tx = txMan.getTransaction();
        if (tx != null)
        {
            int status = tx.getStatus();
            if (status != Status.STATUS_ACTIVE && status != Status.STATUS_NO_TRANSACTION)
            {
                throw new PBFactoryException("Transaction synchronization failed - wrong" +
                        " status of external JTA tx. Expected was an 'active' or 'no transaction'"
                        + ", found status is '" + getStatusFlagAsString(status) + "'");
            }
        }
        return tx;
    }

    /**
     * Returns a string representation of the given
     * {@link javax.transaction.Status} flag.
     */
    private static String getStatusFlagAsString(int status)
    {
        String statusName = "no match, unknown status!";
        try
        {
            Field[] fields = Status.class.getDeclaredFields();
            for (int i = 0; i < fields.length; i++)
            {
                if (fields[i].getInt(null) == status)
                {
                    statusName = fields[i].getName();
                    break;
                }
            }
        }
        catch (Exception e)
        {
            statusName = "no match, unknown status!";
        }
        return statusName;
    }

    //****************************************************
    // inner class
    //****************************************************
    public static class PersistenceBrokerSyncImpl extends PoolablePersistenceBroker implements Synchronization
    {
        private Logger log = LoggerFactory.getLogger(PersistenceBrokerSyncImpl.class);
        /**
         * Used to register all handles using this PB instance
         */
        private List handleList = new ArrayList();

        public PersistenceBrokerSyncImpl(PersistenceBrokerInternal broker, KeyedObjectPool pool)
        {
            super(broker, pool);
        }

        public void beforeCompletion()
        {
            if (log.isDebugEnabled()) log.debug("beforeCompletion was called, nothing to do");
            if(handleList.size() > 0)
            {
                for(int i = 0; i < handleList.size(); i++)
                {
                    log.warn("Found unclosed PersistenceBroker handle, will do automatic close. Please make" +
                            " sure that all used PB instances will be closed.");
                    PersistenceBrokerHandle pbh = (PersistenceBrokerHandle) handleList.get(i);
                    pbh.close();
                }
                handleList.clear();
            }
            ConnectionManagerIF cm = serviceConnectionManager();
            if(cm.isBatchMode()) cm.executeBatch();
            // close connection immediately when in JTA-tx to avoid bad reports from server con-pool
            if(cm.isInLocalTransaction())
            {
                // we should not be in a local tx when performing tx completion
                log.warn("Seems the used PersistenceBroker handle wasn't closed, close the used" +
                        " handle before the transaction completes.");
                // in managed environments this call will be ignored by
                // the wrapped connection
                cm.localCommit();
            }
            cm.releaseConnection();
        }

        public void afterCompletion(int status)
        {
            if (log.isDebugEnabled()) log.debug("afterCompletion was called");
            /*
            we only commit if tx was successfully committed
            */
            try
            {
                if (status != Status.STATUS_COMMITTED)
                {
                    if (status == Status.STATUS_ROLLEDBACK || status == Status.STATUS_ROLLING_BACK)
                    {
                        if (log.isDebugEnabled()) log.debug("Aborting PB-tx due to JTA initiated Rollback: "
                                + getStatusFlagAsString(status));
                    }
                    else
                    {
                        log.error("Aborting PB-tx due to inconsistent, and unexpected, status of JTA tx: "
                                + getStatusFlagAsString(status));
                    }
                    internAbort();
                }
                else
                {
                    if (log.isDebugEnabled()) log.debug("Commit PB-tx");
                    internCommit();
                }
            }
            finally
            {
                // returns the underlying PB instance to pool
                doRealClose();
            }
        }

        private void internBegin()
        {
            setManaged(true);
            super.beginTransaction();
        }

        private void internCommit()
        {
            super.commitTransaction();
        }

        private void internAbort()
        {
            super.abortTransaction();
        }

        private void doRealClose()
        {
            if (log.isDebugEnabled()) log.debug("Now do real close of PB instance");
            super.close();
        }

        public boolean close()
        {
            if(!isInTransaction())
            {
                if (log.isDebugEnabled())
                    log.debug("PB close was called, pass the close call to underlying PB instance");
                /*
                if we not in JTA-tx, we close PB instance in a "normal" way. The PB.close()
                should also release the used connection.
                */
                doRealClose();
            }
            else
            {
                // if we in tx and other handles operate on the same PB instance, do
                // nothing, till all handles are closed.
                if(handleList.size() > 0)
                {
                    if(log.isEnabledFor(Logger.INFO)) log.info("PB.close(): Active used by " + handleList.size()
                            + " handle objects, will skip close call");
                }
                else
                {
                    /*
                    arminw:
                    if in JTA-tx, we don't really close the underlying PB instance (return PB
                    instance to pool, release used connection). As recently as the JTA was
                    completed we can return PB instance to pool. Thus after tx completion method
                    doRealClose() was called to close (return to pool) underlying PB instance.

                    But to free used resources as soon as possible, we release the used connection
                    immediately. The JTA-tx will handle the connection status in a proper way.
                    */
                    if (log.isDebugEnabled())
                        log.debug("PB close was called, only close the PB handle when in JTA-tx");

                    /*
                    TODO: workaround, in 1.1 use special method do handle this stuff
                    arminw:
                    needed to prevent unclosed connection Statement instances when RsIterator
                    wasn't fully materialized in managed environment, because RsIterator is
                    a PBStateListener and below we close the connection.
                    */
                    PersistenceBrokerImpl pb = ((PersistenceBrokerImpl) getInnermostDelegate());
                    pb.fireBrokerEvent(pb.BEFORE_CLOSE_EVENT);

                    ConnectionManagerIF cm = serviceConnectionManager();
                    if(cm.isInLocalTransaction())
                    {
                        /*
                        arminw:
                        in managed environment con.commit calls will be ignored because, the JTA
                        transaction manager control the connection status. But to make
                        connectionManager happy we have to complete the "local tx" of the
                        connectionManager before release the connection
                        */
                        cm.localCommit();
                    }
                    cm.releaseConnection();
                }
            }
            return true;
        }

        void registerHandle(PersistenceBrokerHandle handle)
        {
            handleList.add(handle);
        }

        void deregisterHandle(PersistenceBrokerHandle handle)
        {
            handleList.remove(handle);
        }

        public void beginTransaction() throws TransactionInProgressException, TransactionAbortedException
        {
            throw new UnsupportedOperationException("In managed environments only JTA transaction demarcation allowed");
        }

        public void commitTransaction() throws TransactionNotInProgressException, TransactionAbortedException
        {
            throw new UnsupportedOperationException("In managed environments only JTA transaction demarcation allowed");
        }

        public void abortTransaction() throws TransactionNotInProgressException
        {
            throw new UnsupportedOperationException("In managed environments only JTA transaction demarcation allowed");
        }
    }

    //****************************************************
    // inner class
    //****************************************************
    /**
     * This class collects all PB instances requested in the scope of one transaction
     */
    class TransactionBox implements Synchronization
    {
        Transaction jtaTx;
        Map syncMap = new HashMap();
        boolean isLocked = false;
        boolean isClosed = false;

        public TransactionBox(Transaction tx)
        {
            this.jtaTx = tx;
        }

        PersistenceBrokerSyncImpl find(PBKey key)
        {
            return (PersistenceBrokerSyncImpl) syncMap.get(key);
        }

        void add(PersistenceBrokerSyncImpl syncObj)
        {
            if (isLocked)
            {
                throw new PBFactoryException("Can't associate object with JTA transaction, because tx-completion started");
            }
            syncMap.put(syncObj.getPBKey(), syncObj);
        }

        public void afterCompletion(int status)
        {
            boolean failures = false;
            Synchronization synchronization = null;
            for (Iterator iterator = syncMap.values().iterator(); iterator.hasNext();)
            {
                try
                {
                    synchronization = (Synchronization) iterator.next();
                    synchronization.afterCompletion(status);
                }
                catch (Exception e)
                {
                    failures = true;
                    log.error("Unexpected error when perform Synchronization#afterCompletion method" +
                            " call on object " + synchronization, e);
                }
            }
            isClosed = true;
            // discard association of PB instances and jta-tx
            txRegistry.removeTxBox(jtaTx);
            if (failures)
            {
                throw new PBFactoryException("Unexpected error occured while performing" +
                        " Synchronization#afterCompletion method");
            }
        }

        public void beforeCompletion()
        {
            boolean failures = false;
            Synchronization synchronization = null;
            for (Iterator iterator = syncMap.values().iterator(); iterator.hasNext();)
            {
                try
                {
                    synchronization = (Synchronization) iterator.next();
                    synchronization.beforeCompletion();
                }
                catch (Exception e)
                {
                    failures = true;
                    log.error("Unexpected error when perform Synchronization#beforeCompletion method" +
                            " call on object " + synchronization, e);
                }
            }
            isLocked = true;
            if (failures)
            {
                throw new PBFactoryException("Unexpected error occured while performing" +
                        " Synchronization#beforeCompletion method");
            }
        }
    }

    //****************************************************
    // inner class
    //****************************************************
    /**
     * Maps all {@link TransactionBox} instances based on {@link Transaction} object identity.
     *
     * TODO: Not sure if we should held TransactionBox instances per thread or per transaction object identity.
     * As far as I know it is possible in JTA that thread A starts a tx and thread B commits the tx, thus I
     * start with tx identity as key in registry
     */
    class TxRegistry
    {
        Map txBoxMap;

        public TxRegistry()
        {
            txBoxMap = Collections.synchronizedMap(new WeakHashMap());
        }

        void register(Transaction tx, PersistenceBrokerSyncImpl syncObject) throws RollbackException, SystemException
        {
            TransactionBox txBox = (TransactionBox) txBoxMap.get(tx);
            if (txBox == null || txBox.isClosed)
            {
                // if environment reuse tx instances we can find closed TransactionBox instances
                if (txBox != null) txBoxMap.remove(tx);
                txBox = new TransactionBox(tx);
                tx.registerSynchronization(txBox);
                txBoxMap.put(tx, txBox);
            }
            txBox.add(syncObject);
        }

        PersistenceBrokerSyncImpl findBroker(Transaction tx, PBKey pbKey)
        {
            PersistenceBrokerSyncImpl result = null;
            TransactionBox txBox = (TransactionBox) txBoxMap.get(tx);
            if(txBox != null)
            {
                result = txBox.find(pbKey);
            }
            return result;
        }

        TransactionBox findTxBox(Transaction tx)
        {
            return (TransactionBox) txBoxMap.get(tx);
        }

        void removeTxBox(Transaction tx)
        {
            txBoxMap.remove(tx);
        }
    }

    //****************************************************
    // inner class
    //****************************************************
    /**
     * This wrapper was used when a PB instance which was already in use by a
     * transaction was found.
     */
    class PersistenceBrokerSyncHandle extends PersistenceBrokerHandle
    {
        /**
         * Constructor for the handle, set itself in
         * {@link PersistenceBrokerThreadMapping#setCurrentPersistenceBroker}
         */
        public PersistenceBrokerSyncHandle(PersistenceBrokerSyncImpl broker)
        {
            super(broker);
            // we register handle at underlying PB instance
            broker.registerHandle(this);
        }

        public boolean isClosed()
        {
            return super.isClosed();
        }

        public boolean close()
        {
            if(getDelegate() != null)
            {
                // deregister from underlying PB instance
                ((PersistenceBrokerSyncImpl) getDelegate()).deregisterHandle(this);
            }
            return super.close();
        }
    }
}
