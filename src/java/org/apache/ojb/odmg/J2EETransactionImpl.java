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

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.accesslayer.ConnectionManagerIF;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.odmg.LockNotGrantedException;
import org.odmg.ODMGRuntimeException;
import org.odmg.TransactionAbortedException;

/**
 * Implementation for use in managed environments.
 *
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 * @version $Id: J2EETransactionImpl.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class J2EETransactionImpl extends TransactionImpl implements Synchronization
{
    private Logger log = LoggerFactory.getLogger(J2EETransactionImpl.class);
    private boolean isInExternTransaction;

    /**
     * beforeCompletion is being called twice in JBoss, so this
     * isPrepared flag prevents code from executing twice.
     * todo: find out why it's being called twice and fix it.
     */
    private boolean beforeCompletionCall = false;
    private boolean afterCompletionCall = false;

    public J2EETransactionImpl(ImplementationImpl implementation)
    {
        super(implementation);
        isInExternTransaction = false;
    }

    public void setInExternTransaction(boolean mode)
    {
        isInExternTransaction = mode;
        }

    public boolean isInExternTransaction()
    {
        return isInExternTransaction;
    }

    public void join()
    {
            throw new UnsupportedOperationException("Not supported in managed enviroment");
        }

    public void leave()
    {
            throw new UnsupportedOperationException("Not supported in managed enviroment");
        }

    public void checkpoint()
    {
            throw new UnsupportedOperationException("Not supported in managed enviroment");
        }

    /**
     * FOR internal use. This method was called after the external transaction was completed.
     *
     * @see javax.transaction.Synchronization
     */
    public void afterCompletion(int status)
    {
        if(afterCompletionCall) return;

        log.info("Method afterCompletion was called");
        try
        {
            switch(status)
            {
                case Status.STATUS_COMMITTED:
                    if(log.isDebugEnabled())
                    {
                        log.debug("Method afterCompletion: Do commit internal odmg-tx, status of JTA-tx is " + TxUtil.getStatusString(status));
                    }
                    commit();
                    break;
                default:
                    log.error("Method afterCompletion: Do abort call on internal odmg-tx, status of JTA-tx is " + TxUtil.getStatusString(status));
                    abort();
            }
        }
        finally
        {
            afterCompletionCall = true;
            log.info("Method afterCompletion finished");
        }
    }

    /**
     * FOR internal use. This method was called before the external transaction was completed.
     *
     * This method was called by the JTA-TxManager before the JTA-tx prepare call. Within this method
     * we prepare odmg for commit and pass all modified persistent objects to DB and release/close the used
     * connection. We have to close the connection in this method, because the TxManager does prepare for commit
     * after this method and all used DataSource-connections have to be closed before.
     *
     * @see javax.transaction.Synchronization
     */
    public void beforeCompletion()
    {
        // avoid redundant calls
        if(beforeCompletionCall) return;

        log.info("Method beforeCompletion was called");
        int status = Status.STATUS_UNKNOWN;
        try
        {
            JTATxManager mgr = (JTATxManager) getImplementation().getTxManager();
            status = mgr.getJTATransaction().getStatus();
            // ensure proper work, check all possible status
            // normally only check for 'STATUS_MARKED_ROLLBACK' is necessary
            if(status == Status.STATUS_MARKED_ROLLBACK
                    || status == Status.STATUS_ROLLEDBACK
                    || status == Status.STATUS_ROLLING_BACK
                    || status == Status.STATUS_UNKNOWN
                    || status == Status.STATUS_NO_TRANSACTION)
            {
                log.error("Synchronization#beforeCompletion: Can't prepare for commit, because tx status was "
                        + TxUtil.getStatusString(status) + ". Do internal cleanup only.");
            }
            else
            {
                if(log.isDebugEnabled())
                {
                    log.debug("Synchronization#beforeCompletion: Prepare for commit");
                }
                // write objects to database
                prepareCommit();
            }
        }
        catch(Exception e)
        {
            log.error("Synchronization#beforeCompletion: Error while prepare for commit", e);
            if(e instanceof LockNotGrantedException)
            {
                throw (LockNotGrantedException) e;
            }
            else if(e instanceof TransactionAbortedException)
            {
                throw (TransactionAbortedException) e;
            }
            else if(e instanceof ODMGRuntimeException)
            {
                throw (ODMGRuntimeException) e;
            }
            else
            { 
                throw new ODMGRuntimeException("Method beforeCompletion() fails, status of JTA-tx was "
                        + TxUtil.getStatusString(status) + ", message: " + e.getMessage());
            }

        }
        finally
        {
            beforeCompletionCall = true;
            setInExternTransaction(false);
            internalCleanup();
        }
    }

    /**
     * In managed environment do internal close the used connection
     */
    private void internalCleanup()
    {
        if(hasBroker())
        {
            PersistenceBroker broker = getBroker();
            if(log.isDebugEnabled())
            {
                log.debug("Do internal cleanup and close the internal used connection without" +
                        " closing the used broker");
            }
            ConnectionManagerIF cm = broker.serviceConnectionManager();
            if(cm.isInLocalTransaction())
            {
                /*
                arminw:
                in managed environment this call will be ignored because, the JTA transaction
                manager control the connection status. But to make connectionManager happy we
                have to complete the "local tx" of the connectionManager before release the
                connection
                */
                cm.localCommit();
            }
            cm.releaseConnection();
        }
    }

    public void commit()
    {
        try
        {
            // prepare for commit was done before on 'beforeCompleation' call
            if(log.isDebugEnabled()) log.debug("Commit transaction " + this + ", commit on broker " + broker);
            if(hasBroker())
            {
                getBroker().commitTransaction();
                doClose();
            }
            setStatus(Status.STATUS_COMMITTED);
            // Now, we notify everything the commit is done.
            performTransactionAwareAfterCommit();
        }
        catch(Exception ex)
        {
            // We should not reach this block
            log.error("Unexpected error while do commit on used PB-Instance and close resources", ex);
            abort();
        }
    }

    public void abort()
    {
        if(getStatus() == Status.STATUS_ROLLEDBACK) return;

        try
        {
            try
            {
                doAbort();
            }
            catch(Exception ignore)
            {
                log.error("Failure while do abort call", ignore);
            }

            getImplementation().getTxManager().abortExternalTx(this);

            try
            {
                doClose();
            }
            catch(Exception e)
            {
                log.error("Failure while do abort call", e);
            }
            setStatus(Status.STATUS_ROLLEDBACK);
        }
        finally
        {
            setInExternTransaction(false);
        }
    }
}
