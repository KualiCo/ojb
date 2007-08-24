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

import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.transaction.tm.TransactionManagerFactoryException;
import org.apache.ojb.broker.transaction.tm.TransactionManagerFactoryFactory;
import org.apache.commons.lang.SystemUtils;
import org.odmg.TransactionNotInProgressException;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.lang.ref.WeakReference;

/**
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 *
 * In an app server environment, where we have a transaction manager, we
 * use the transactionmanager and it's associated JTA transaction to associate
 * with the ODMG transaction. So the key is retrieved by calling getTransaction
 * on the transactionManager
 */
public class JTATxManager implements OJBTxManager
{
    private Logger log = LoggerFactory.getLogger(JTATxManager.class);
    private static ThreadLocal txRepository = new ThreadLocal();

    /**
     * Remove the ODMG transaction from the transaction buffer
     * ODMG transactions are associated with JTA transactions via a map
     */
    public void deregisterTx(Object transaction)
    {
//        TxBuffer buf = (TxBuffer) txRepository.get();
//        if (buf != null)
//        {
//            buf.setInternTx(null);
//        }
        txRepository.set(null);
    }

    public void registerTx(TransactionImpl odmgTrans)
    {
        if (log.isDebugEnabled()) log.debug("registerSynchronization was called");
        Transaction transaction = null;
        try
        {
            transaction = getJTATransaction();
        }
        catch (SystemException e)
        {
            log.error("Obtain current transaction from container failed", e);
        }
        if (transaction == null)
        {
            log.error("Cannot get the external transaction from the external TM");
            throw new TransactionNotInProgressException("No external transaction found");
        }
        if (log.isDebugEnabled())
        {
            log.debug("registerSynchronization was called with parameters"
                    + SystemUtils.LINE_SEPARATOR +"J2EETransactionImpl: " + odmgTrans
                    + SystemUtils.LINE_SEPARATOR + "Transaction: " + transaction);
        }
        registerSynchronization(odmgTrans, transaction);
    }

    /**
     * Do synchronization of the given J2EE ODMG Transaction
     */
    private void registerSynchronization(TransactionImpl odmgTrans, Transaction transaction)
    {
        // todo only need for development
        if (odmgTrans == null || transaction == null)
        {
            log.error("One of the given parameters was null --> cannot do synchronization!" +
                    " omdg transaction was null: " + (odmgTrans == null) +
                    ", external transaction was null: " + (transaction == null));
            return;
        }

        int status = -1; // default status.
        try
        {
            status = transaction.getStatus();
            if (status != Status.STATUS_ACTIVE)
            {
                throw new OJBRuntimeException(
                        "Transaction synchronization failed - wrong status of external container tx: " +
                        getStatusString(status));
            }
        }
        catch (SystemException e)
        {
            throw new OJBRuntimeException("Can't read status of external tx", e);
        }

        try
        {
            //Sequence of the following method calls is significant
            // 1. register the synchronization with the ODMG notion of a transaction.
            transaction.registerSynchronization((J2EETransactionImpl) odmgTrans);
            // 2. mark the ODMG transaction as being in a JTA Transaction
            // Associate external transaction with the odmg transaction.
            txRepository.set(new TxBuffer(odmgTrans, transaction));
        }
        catch (Exception e)
        {
            log.error("Cannot associate PersistenceBroker with running Transaction", e);
            throw new OJBRuntimeException(
                    "Transaction synchronization failed - wrong status of external container tx", e);
        }
    }

    private static String getStatusString(int status)
    {
        switch (status)
        {
            case Status.STATUS_ACTIVE:
                return "STATUS_ACTIVE";
            case Status.STATUS_COMMITTED:
                return "STATUS_COMMITTED";
            case Status.STATUS_COMMITTING:
                return "STATUS_COMMITTING";
            case Status.STATUS_MARKED_ROLLBACK:
                return "STATUS_MARKED_ROLLBACK";
            case Status.STATUS_NO_TRANSACTION:
                return "STATUS_NO_TRANSACTION";
            case Status.STATUS_PREPARED:
                return "STATUS_PREPARED";
            case Status.STATUS_PREPARING:
                return "STATUS_PREPARING";
            case Status.STATUS_ROLLEDBACK:
                return "STATUS_ROLLEDBACK";
            case Status.STATUS_ROLLING_BACK:
                return "STATUS_ROLLING_BACK";
            case Status.STATUS_UNKNOWN:
                return "STATUS_UNKNOWN";
            default:
                return "NO STATUS FOUND";
        }
    }

    /**
     * Return the TransactionManager of the external app
     */
    private TransactionManager getTransactionManager()
    {
        TransactionManager retval = null;
        try
        {
            if (log.isDebugEnabled()) log.debug("getTransactionManager called");
            retval = TransactionManagerFactoryFactory.instance().getTransactionManager();
        }
        catch (TransactionManagerFactoryException e)
        {
            log.warn("Exception trying to obtain TransactionManager from Factory", e);
            e.printStackTrace();
        }
        return retval;
    }

    public Transaction getJTATransaction() throws SystemException
    {
        if (log.isDebugEnabled()) log.debug("getTransaction called");
        if (getTransactionManager() == null)
        {
            log.warn("TransactionManager was null");
            return null;
        }
        return getTransactionManager().getTransaction();
    }

    /**
     * Returns the current transaction based on the JTA Transaction.
     * @throws org.odmg.TransactionNotInProgressException if no transaction was found.
     */
    public TransactionImpl getCurrentTransaction()
    {
        TransactionImpl retval = getTransaction();
        if (null == retval)
        {
            throw new TransactionNotInProgressException(
                    "Calling method needed transaction, but no transaction found via TransactionManager");
        }
        return retval;
    }

    /**
     * Returns the current transaction based on the JTA Transaction or <code>null</code>
     * if no transaction was found.
     */
    public TransactionImpl getTransaction()
    {
        TxBuffer buf = (TxBuffer) txRepository.get();
        return buf != null ? buf.getInternTx() : null;
    }

    /**
     * Abort an active extern transaction associated with the given PB.
     */
    public void abortExternalTx(TransactionImpl odmgTrans)
    {
        if (log.isDebugEnabled()) log.debug("abortExternTransaction was called");
        if (odmgTrans == null) return;
        TxBuffer buf = (TxBuffer) txRepository.get();
        Transaction extTx = buf != null ? buf.getExternTx() : null;
        try
        {
            if (extTx != null && extTx.getStatus() == Status.STATUS_ACTIVE)
            {
                if(log.isDebugEnabled())
                {
                    log.debug("Set extern transaction to rollback");
                }
                extTx.setRollbackOnly();
            }
        }
        catch (Exception ignore)
        {
        }
        txRepository.set(null);
    }

    public void configure(Configuration config)
    {
        /**
         * no-op
         */
    }


    //************************************************************************
    // inner class
    //************************************************************************
    private static final class TxBuffer
    {
        private WeakReference externTx = null;
        private WeakReference internTx = null;

        public TxBuffer()
        {
        }

        /*
        arminw:
        use WeakReference to make sure that closed Transaction objects can be
        immediately reclaimed by the garbage collector.
        */

        public TxBuffer(TransactionImpl internTx, Transaction externTx)
        {
            this.internTx = new WeakReference(internTx);
            this.externTx = new WeakReference(externTx);
        }

        public Transaction getExternTx()
        {
            return (Transaction) externTx.get();
        }

        public void setExternTx(Transaction externTx)
        {
            this.externTx = new WeakReference(externTx);
        }

        public TransactionImpl getInternTx()
        {
            return (TransactionImpl) internTx.get();
        }

        public void setInternTx(TransactionImpl internTx)
        {
            this.internTx = new WeakReference(internTx);
        }
    }
}
