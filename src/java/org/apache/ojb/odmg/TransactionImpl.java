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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PBFactoryException;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerInternal;
import org.apache.ojb.broker.core.PersistenceBrokerFactoryFactory;
import org.apache.ojb.broker.core.proxy.CollectionProxy;
import org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.CollectionProxyListener;
import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.MaterializationListener;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.GUID;
import org.apache.ojb.broker.util.configuration.Configurable;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.odmg.locking.LockManager;
import org.odmg.DatabaseClosedException;
import org.odmg.LockNotGrantedException;
import org.odmg.ODMGRuntimeException;
import org.odmg.Transaction;
import org.odmg.TransactionAbortedException;
import org.odmg.TransactionNotInProgressException;

/**
 *
 * Implementation of Transaction for org.odmg.Transaction.
 *
 * @author     Thomas Mahler & David Dixon-Peugh
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @author <a href="mailto:brianm@apache.org">Brian McCallister</a>
 * @version $Id: TransactionImpl.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 *
 */
public class TransactionImpl
        implements Transaction, MaterializationListener, Configurable, CollectionProxyListener, TransactionExt
{
    private Logger log = LoggerFactory.getLogger(TransactionImpl.class);
    private boolean impliciteWriteLocks;
    private boolean implicitLocking;
    private boolean ordering;

    private String txGUID;
    protected PersistenceBrokerInternal broker = null;
    private ArrayList registrationList = new ArrayList();
    private ImplementationImpl implementation;
    private NamedRootsMap namedRootsMap;

    /**
     * The status of the current transaction, as specified by the
     * javax.transaction package.
     * See {@link javax.transaction.Status} for list of valid values.
     */
    private int txStatus = Status.STATUS_NO_TRANSACTION;

    /**
     * the internal table containing all Objects "touched" by this tx and their
     * respective transactional state
     */
    protected ObjectEnvelopeTable objectEnvelopeTable = null;

    /**
     * reference to the currently opened database
     */
    private DatabaseImpl curDB;
    /**
     * The tx may me listening to a number of IndirectionHandlers.
     * on abort or commit these Handlers must be informed to remove
     * tx from their List of Listeners.
     */
    private ArrayList registeredIndirectionHandlers = new ArrayList();

    /**
     * Unloaded collection proxies which will be registered with tx when
     * collection is loaded
     */
    private ArrayList registeredCollectionProxies = new ArrayList();

    /**
     * list of proxy objects that were locked, but haven't been materialized yet.
     * This is necessary so the locks can be released on closing the transaction
     */
    private ArrayList unmaterializedLocks = new ArrayList();

    /**
     * Creates new Transaction
     * @param implementation The odmg Implementation class
     */
    public TransactionImpl(ImplementationImpl implementation)
    {
        this.implementation = implementation;
        this.impliciteWriteLocks = implementation.isImpliciteWriteLocks();
        this.implicitLocking = implementation.isImplicitLocking();
        this.ordering = implementation.isOrdering();
        //this.noteUserOrdering = implementation.isNoteUserOrder();

        // assign a globally uniqe id to this tx
        txGUID = new GUID().toString();
        curDB = implementation.getCurrentDatabase();
        namedRootsMap = new NamedRootsMap(this);
    }

    public ImplementationImpl getImplementation()
    {
        return implementation;
    }

    public NamedRootsMap getNamedRootsMap()
    {
        return namedRootsMap;
    }

    /**
     * Returns the associated database
     */
    public DatabaseImpl getAssociatedDatabase()
    {
        return this.curDB;
    }

    protected int getStatus()
    {
        return txStatus;
    }

    protected void setStatus(int status)
    {
        this.txStatus = status;
    }

    private void checkForDB()
    {
        if (curDB == null || !curDB.isOpen())
        {
            log.error("Transaction without a associated open Database.");
            throw new TransactionAbortedExceptionOJB(
                    "No open database found. Open the database before handling transactions");
        }
    }

    /**
     * Determine whether the transaction is open or not. A transaction is open if
     * a call has been made to <code>begin</code> , but a subsequent call to
     * either <code>commit</code> or <code>abort</code> has not been made.
     * @return    True if the transaction is open, otherwise false.
     */
    public boolean isOpen()
    {
        return (getStatus() == Status.STATUS_ACTIVE ||
                getStatus() == Status.STATUS_MARKED_ROLLBACK ||
                getStatus() == Status.STATUS_PREPARED ||
                getStatus() == Status.STATUS_PREPARING ||
                getStatus() == Status.STATUS_COMMITTING);
    }

    private void checkOpen()
    {
        if (!isOpen())
        {
            throw new TransactionNotInProgressException(
                    "Transaction was not open, call tx.begin() before perform action, current status is: " +
                    TxUtil.getStatusString(getStatus()));
        }
    }


    /**
     * Attach the caller's thread to this <code>Transaction</code> and detach the
     * thread from any former <code>Transaction</code> the thread may have been
     * associated with.
     */
    public void join()
    {
        checkOpen();
        implementation.getTxManager().deregisterTx(this);
        implementation.getTxManager().registerTx(this);
    }

    /**
     * Upgrade the lock on the given object to the given lock mode. The call has
     * no effect if the object's current lock is already at or above that level of
     * lock mode.
     *
     * @param  obj       object to acquire a lock on.
     * @param  lockMode  lock mode to acquire. The lock modes
     * are <code>READ</code> , <code>UPGRADE</code> , and <code>WRITE</code> .
     *
     * @exception  LockNotGrantedException    Description of Exception
     */
    public void lock(Object obj, int lockMode) throws LockNotGrantedException
    {
        if (log.isDebugEnabled()) log.debug("lock object was called on tx " + this + ", object is " + obj.toString());
        checkOpen();
        RuntimeObject rtObject = new RuntimeObject(obj, this);
        lockAndRegister(rtObject, lockMode, isImplicitLocking(), getRegistrationList());
//        if(isImplicitLocking()) moveToLastInOrderList(rtObject.getIdentity());
    }

    /**
     * Returns an empty List for registration of processed object Identity.
     */
    public ArrayList getRegistrationList()
    {
        clearRegistrationList();
        return registrationList;
    }

    /**
     * Clears the list of processed object Identity.
     */
    public void clearRegistrationList()
    {
        registrationList.clear();
    }

    /**
     * Lock and register the specified object, make sure that when cascading locking and register
     * is enabled to specify a List to register the already processed object Identiy.
     */
    public void lockAndRegister(RuntimeObject rtObject, int lockMode, List registeredObjects)
    {
        lockAndRegister(rtObject, lockMode, isImplicitLocking(), registeredObjects);
    }

    /**
     * Lock and register the specified object, make sure that when cascading locking and register
     * is enabled to specify a List to register the already processed object Identiy.
     */
    public synchronized void lockAndRegister(RuntimeObject rtObject, int lockMode, boolean cascade, List registeredObjects)
    {
        if(log.isDebugEnabled()) log.debug("Lock and register called for " + rtObject.getIdentity());
        // if current object was already locked, do nothing
        // avoid endless loops when circular object references are used
        if(!registeredObjects.contains(rtObject.getIdentity()))
        {
            if(cascade)
            {
                // if implicite locking is enabled, first add the current object to
                // list of registered objects to avoid endless loops on circular objects
                registeredObjects.add(rtObject.getIdentity());
                // lock and register 1:1 references first
                //
                // If implicit locking is used, we have materialize the main object
                // to lock the referenced objects too
                lockAndRegisterReferences(rtObject.getCld(), rtObject.getObjMaterialized(), lockMode, registeredObjects);
            }
            try
            {
                // perform the lock on the object
                // we don't need to lock new objects
                if(!rtObject.isNew())
                {
                    doSingleLock(rtObject.getCld(), rtObject.getObj(), rtObject.getIdentity(), lockMode);
                }
                // after we locked the object, register it to detect status and changes while tx
                doSingleRegister(rtObject, lockMode);
            }
            catch (Throwable t)
            {
                //log.error("Locking of obj " + rtObject.getIdentity() + " failed", t);
                // if registering of object fails release lock on object, because later we don't
                // get a change to do this.
                implementation.getLockManager().releaseLock(this, rtObject.getIdentity(), rtObject.getObj());
                if(t instanceof LockNotGrantedException)
                {
                    throw (LockNotGrantedException) t;
                }
                else
                {
                    log.error("Unexpected failure while locking", t);
                    throw new LockNotGrantedException("Locking failed for "
                            + rtObject.getIdentity()+ ", nested exception is: [" + t.getClass().getName()
                            + ": " + t.getMessage() + "]");
                }
            }
            if(cascade)
            {
                // perform locks and register 1:n and m:n references
                // If implicit locking is used, we have materialize the main object
                // to lock the referenced objects too
                lockAndRegisterCollections(rtObject.getCld(), rtObject.getObjMaterialized(), lockMode, registeredObjects);
            }
        }
    }

    /**
     * Only lock the specified object, represented by
     * the {@link RuntimeObject} instance.
     *
     * @param  cld       The {@link org.apache.ojb.broker.metadata.ClassDescriptor}
     * of the object to acquire a lock on.
     * @param  oid The {@link org.apache.ojb.broker.Identity} of the object to lock.
     * @param  lockMode  lock mode to acquire. The lock modes
     * are <code>READ</code> , <code>UPGRADE</code> , and <code>WRITE</code>.
     *
     * @exception  LockNotGrantedException    Description of Exception
     */
    void doSingleLock(ClassDescriptor cld, Object obj, Identity oid, int lockMode) throws LockNotGrantedException
    {
        LockManager lm = implementation.getLockManager();
        if (cld.isAcceptLocks())
        {
            if (lockMode == Transaction.READ)
            {
                if (log.isDebugEnabled()) log.debug("Do READ lock on object: " + oid);
                if(!lm.readLock(this, oid, obj))
                {
                    throw new LockNotGrantedException("Can not lock for READ: " + oid);
                }
            }
            else if (lockMode == Transaction.WRITE)
            {
                if (log.isDebugEnabled()) log.debug("Do WRITE lock on object: " + oid);
                if(!lm.writeLock(this, oid, obj))
                {
                    throw new LockNotGrantedException("Can not lock for WRITE: " + oid);
                }
            }
            else if (lockMode == Transaction.UPGRADE)
            {
                if (log.isDebugEnabled()) log.debug("Do UPGRADE lock on object: " + oid);
                if(!lm.upgradeLock(this, oid, obj))
                {
                    throw new LockNotGrantedException("Can not lock for UPGRADE: " + oid);
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Class '" + cld.getClassNameOfObject() + "' doesn't accept locks" +
                        " (accept-locks=false) when implicite locked, so OJB skip this object: " + oid);
            }
        }
    }

    /**
     * Detach the caller's thread from this <code>Transaction</code> , but do not
     * attach the thread to another <code>Transaction</code> .
     */
    public void leave()
    {
        checkOpen();
        implementation.getTxManager().deregisterTx(this);
    }

    /**
     * Write objects to data store, but don't release the locks.
     * I don't know what we should do if we are in a checkpoint and
     * we need to abort.
     */
    protected synchronized void doWriteObjects(boolean isFlush) throws TransactionAbortedException, LockNotGrantedException
    {
        /*
        arminw:
        if broker isn't in PB-tx, start tx
        */
        if (!getBroker().isInTransaction())
        {
            if (log.isDebugEnabled()) log.debug("call beginTransaction() on PB instance");
            broker.beginTransaction();
        }

        // Notify objects of impending commits.
        performTransactionAwareBeforeCommit();

        // Now perfom the real work
        objectEnvelopeTable.writeObjects(isFlush);
        // now we have to perform the named objects
        namedRootsMap.performDeletion();
        namedRootsMap.performInsert();
        namedRootsMap.afterWriteCleanup();
    }

    /**
     * Do the Aborts, but don't release the locks.
     * Do the aborts on the NamedRootsMap first, then
     * abort the other stuff.
     */
    protected synchronized void doAbort()
    {
        // Notify objects of impending aborts.
        performTransactionAwareBeforeRollback();

        // Now, we abort everything. . .
        objectEnvelopeTable.rollback();

        // Now, we notify everything the abort is done.
        performTransactionAwareAfterRollback();
    }

    /**
     * Close a transaction and do all the cleanup associated with it.
     */
    protected synchronized void doClose()
    {
        try
        {
            LockManager lm = getImplementation().getLockManager();
            Enumeration en = objectEnvelopeTable.elements();
            while (en.hasMoreElements())
            {
                ObjectEnvelope oe = (ObjectEnvelope) en.nextElement();
                lm.releaseLock(this, oe.getIdentity(), oe.getObject());
            }

            //remove locks for objects which haven't been materialized yet
            for (Iterator it = unmaterializedLocks.iterator(); it.hasNext();)
            {
                lm.releaseLock(this, it.next());
            }

            // this tx is no longer interested in materialization callbacks
            unRegisterFromAllIndirectionHandlers();
            unRegisterFromAllCollectionProxies();
        }
        finally
        {
            /**
             * MBAIRD: Be nice and close the table to release all refs
             */
            if (log.isDebugEnabled())
                log.debug("Close Transaction and release current PB " + broker + " on tx " + this);
            // remove current thread from LocalTxManager
            // to avoid problems for succeeding calls of the same thread
            implementation.getTxManager().deregisterTx(this);
            // now cleanup and prepare for reuse
            refresh();
        }
    }

    /**
     * cleanup tx and prepare for reuse
     */
    protected void refresh()
    {
        if (log.isDebugEnabled())
                log.debug("Refresh this transaction for reuse: " + this);
        try
        {
            // we reuse ObjectEnvelopeTable instance
            objectEnvelopeTable.refresh();
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("error closing object envelope table : " + e.getMessage());
                e.printStackTrace();
            }
        }
        cleanupBroker();
        // clear the temporary used named roots map
        // we should do that, because same tx instance
        // could be used several times
        broker = null;
        clearRegistrationList();
        unmaterializedLocks.clear();
        txStatus = Status.STATUS_NO_TRANSACTION;
    }

    /**
     * Commit the transaction, but reopen the transaction, retaining all locks.
     * Calling <code>checkpoint</code> commits persistent object modifications
     * made within the transaction since the last checkpoint to the database. The
     * transaction retains all locks it held on those objects at the time the
     * checkpoint was invoked.
     */
    public void checkpoint()
    {
        if (log.isDebugEnabled()) log.debug("Checkpoint was called, commit changes hold locks on tx " + this);
        try
        {
            checkOpen();
            doWriteObjects(true);
            // do commit on PB
            if (hasBroker() && broker.isInTransaction()) broker.commitTransaction();
        }
        catch (Throwable t)
        {
            log.error("Checkpoint call failed, do abort transaction", t);
            txStatus = Status.STATUS_MARKED_ROLLBACK;
            abort();
            if(!(t instanceof ODMGRuntimeException))
            {
                throw new TransactionAbortedExceptionOJB("Can't tx.checkpoint() objects: " + t.getMessage(), t);
            }
            else
            {
                throw (ODMGRuntimeException) t;
            }
        }
    }

    /**
     * @see org.apache.ojb.odmg.TransactionExt#flush
     */
    public void flush()
    {
        if (log.isDebugEnabled())
        {
            log.debug("Flush was called - write changes to database, do not commit, hold locks on tx " + this);
        }

        try
        {
            checkOpen();
            doWriteObjects(true);
        }
        catch (Throwable t)
        {
            log.error("Calling method 'tx.flush()' failed", t);
            txStatus = Status.STATUS_MARKED_ROLLBACK;
            abort();
            if(!(t instanceof ODMGRuntimeException))
            {
                throw new TransactionAbortedExceptionOJB("Can't tx.flush() objects: " + t.getMessage(), t);
            }
            else
            {
                throw (ODMGRuntimeException) t;
            }
        }
    }

    /**
     * @see org.apache.ojb.odmg.TransactionExt#markDelete
     */
    public void markDelete(Object anObject)
    {
        ObjectEnvelope otw = objectEnvelopeTable.get(anObject, false);
        // not needed on delete - or?
        //otw.refreshObjectIfNeeded(anObject);
        otw.setModificationState(otw.getModificationState().markDelete());
    }

    public void deletePersistent(RuntimeObject rt)
    {
//        if(rt.isNew())
//        {
//            throw new ObjectNotPersistentException("Object " + rt.getIdentity() + " is not yet persistent");
//        }
        if(rt.isProxy())
        {
            Object realObj = rt.getHandler().getRealSubject();
            rt = new RuntimeObject(realObj, rt.getIdentity(), this, false);
        }
        lockAndRegister(rt, Transaction.WRITE, getRegistrationList());
        ObjectEnvelope oe = objectEnvelopeTable.getByIdentity(rt.getIdentity());
        // TODO: not needed on delete - or? When optimistic locking is used we should always use the
        // specified object instance to use the last version of the object
        oe.refreshObjectIfNeeded(rt.getObj());
        oe.setModificationState(oe.getModificationState().markDelete());
    }

    /**
     * @see org.apache.ojb.odmg.TransactionExt#markDirty
     */
    public void markDirty(Object anObject)
    {
        ObjectEnvelope otw = objectEnvelopeTable.get(anObject, false);
        otw.refreshObjectIfNeeded(anObject);
        otw.setModificationState(otw.getModificationState().markDirty());
    }

    void markDirty(RuntimeObject rt)
    {
        ObjectEnvelope otw = objectEnvelopeTable.get(rt.getIdentity(), rt.getObj(), rt.isNew());
        otw.refreshObjectIfNeeded(rt.getObj());
        otw.setModificationState(otw.getModificationState().markDirty());
    }

    void markPersistent(RuntimeObject rtObj)
    {
        ObjectEnvelope oe = objectEnvelopeTable.getByIdentity(rtObj.getIdentity());
        if(oe == null)
        {
            oe = objectEnvelopeTable.get(rtObj.getIdentity(), rtObj.getObj(), rtObj.isNew());
        }
        if(oe.needsDelete())
        {
            oe.setModificationState(oe.getModificationState().markNew());
        }
        else
        {
            oe.setModificationState(oe.getModificationState().markDirty());
        }
        oe.refreshObjectIfNeeded(rtObj.getObj());
    }

    void makePersistent(RuntimeObject rt)
    {
        try
        {
            lockAndRegister(rt, Transaction.WRITE, getRegistrationList());
            markPersistent(rt);
        }
        catch (org.apache.ojb.broker.metadata.ClassNotPersistenceCapableException ex)
        {
            log.error("Can't persist object: " + rt.getIdentity(), ex);
            throw new org.odmg.ClassNotPersistenceCapableException(ex.getMessage());
        }
    }

    /**
     * @see org.apache.ojb.odmg.TransactionExt#isDeleted(org.apache.ojb.broker.Identity)
     */
    public boolean isDeleted(Identity id)
    {
        ObjectEnvelope envelope = objectEnvelopeTable.getByIdentity(id);
        return (envelope != null && envelope.needsDelete());
    }

    /**
     * Upgrade the lock on the given object to the given lock mode. Method <code>
     * tryLock</code> is the same as <code>lock</code> except it returns a boolean
     * indicating whether the lock was granted instead of generating an exception.
     * @param  obj          Description of Parameter
     * @param  lockMode     Description of Parameter
     * @return              Description of the Returned Value
     * </code>, <code>UPGRADE</code> , and <code>WRITE</code> .
     * @return true          if the lock has been acquired, otherwise false.
     */
    public boolean tryLock(Object obj, int lockMode)
    {
        if (log.isDebugEnabled()) log.debug("Try to lock object was called on tx " + this);
        checkOpen();
        try
        {
            lock(obj, lockMode);
            return true;
        }
        catch (LockNotGrantedException ex)
        {
            return false;
        }
    }

    /**
     * Commit and close the transaction. Calling <code>commit</code> commits to
     * the database all persistent object modifications within the transaction and
     * releases any locks held by the transaction. A persistent object
     * modification is an update of any field of an existing persistent object, or
     * an update or creation of a new named object in the database. If a
     * persistent object modification results in a reference from an existing
     * persistent object to a transient object, the transient object is moved to
     * the database, and all references to it updated accordingly. Note that the
     * act of moving a transient object to the database may create still more
     * persistent references to transient objects, so its referents must be
     * examined and moved as well. This process continues until the database
     * contains no references to transient objects, a condition that is guaranteed
     * as part of transaction commit. Committing a transaction does not remove
     * from memory transient objects created during the transaction.
     *
     * The updateObjectList contains a list of all objects for which this transaction
     * has write privledge to.  We need to update these objects.
     */
    public void commit()
    {
        checkOpen();
        try
        {
            prepareCommit();
            checkForCommit();

            txStatus = Status.STATUS_COMMITTING;
            if (log.isDebugEnabled()) log.debug("Commit transaction " + this);
            // now do real commit on broker
            if(hasBroker()) getBroker().commitTransaction();

            // Now, we notify everything the commit is done.
            performTransactionAwareAfterCommit();

            doClose();
            txStatus = Status.STATUS_COMMITTED;
        }
        catch(Exception ex)
        {
            log.error("Error while commit objects, do abort tx " + this + ", " + ex.getMessage(), ex);
            txStatus = Status.STATUS_MARKED_ROLLBACK;
            abort();
            if(!(ex instanceof ODMGRuntimeException))
            {
                throw new TransactionAbortedExceptionOJB("Can't commit objects: " + ex.getMessage(), ex);
            }
            else
            {
                throw (ODMGRuntimeException) ex;
            }
        }
    }

    protected void checkForCommit()
    {
        // Never commit transaction that has been marked for rollback
        if (txStatus == Status.STATUS_MARKED_ROLLBACK)
            throw new TransactionAbortedExceptionOJB("Illegal tx-status: tx is already markedRollback");
        // Don't commit if not prepared
        if (txStatus != Status.STATUS_PREPARED)
            throw new IllegalStateException("Illegal tx-status: Do prepare commit before commit");
    }

    /**
     * Prepare does the actual work of moving the changes at the object level
     * into storage (the underlying rdbms for instance). prepare Can be called multiple times, and
     * does not release locks.
     *
     * @throws TransactionAbortedException if the transaction has been aborted
     * for any reason.
     * @throws  IllegalStateException Method called if transaction is
     *  not in the proper state to perform this operation
     * @throws TransactionNotInProgressException if the transaction is closed.
     */
    protected void prepareCommit() throws TransactionAbortedException, LockNotGrantedException
    {
        if (txStatus == Status.STATUS_MARKED_ROLLBACK)
        {
            throw new TransactionAbortedExceptionOJB("Prepare Transaction: tx already marked for rollback");
        }
        if (txStatus != Status.STATUS_ACTIVE)
        {
            throw new IllegalStateException("Prepare Transaction: tx status is not 'active', status is " + TxUtil.getStatusString(txStatus));
        }
        try
        {
            txStatus = Status.STATUS_PREPARING;
            doWriteObjects(false);
            txStatus = Status.STATUS_PREPARED;
        }
        catch (RuntimeException e)
        {
            log.error("Could not prepare for commit", e);
            txStatus = Status.STATUS_MARKED_ROLLBACK;
            throw e;
        }
    }

    /**
     * Abort and close the transaction. Calling abort abandons all persistent
     * object modifications and releases the associated locks. Aborting a
     * transaction does not restore the state of modified transient objects
     */
    public void abort()
    {
        /*
        do nothing if already rolledback
        */
        if (txStatus == Status.STATUS_NO_TRANSACTION
                || txStatus == Status.STATUS_UNKNOWN
                || txStatus == Status.STATUS_ROLLEDBACK)
        {
            log.info("Nothing to abort, tx is not active - status is " + TxUtil.getStatusString(txStatus));
            return;
        }
        // check status of tx
        if (txStatus != Status.STATUS_ACTIVE && txStatus != Status.STATUS_PREPARED &&
                txStatus != Status.STATUS_MARKED_ROLLBACK)
        {
            throw new IllegalStateException("Illegal state for abort call, state was '" + TxUtil.getStatusString(txStatus) + "'");
        }
        if(log.isEnabledFor(Logger.INFO))
        {
            log.info("Abort transaction was called on tx " + this);
        }
        try
        {
            try
            {
                doAbort();
            }
            catch(Exception e)
            {
                log.error("Error while abort transaction, will be skipped", e);
            }

            // used in managed environments, ignored in non-managed
            this.implementation.getTxManager().abortExternalTx(this);

            try
            {
                if(hasBroker() && getBroker().isInTransaction())
                {
                    getBroker().abortTransaction();
                }
            }
            catch(Exception e)
            {
                log.error("Error while do abort used broker instance, will be skipped", e);
            }
        }
        finally
        {
            txStatus = Status.STATUS_ROLLEDBACK;
            // cleanup things, e.g. release all locks
            doClose();
        }
    }

    /**
     * Start a transaction. Calling <code>begin</code> multiple times on the same
     * transaction object, without an intervening call to <code>commit</code> or
     * <code>abort</code> , causes the exception <code>
     * TransactionInProgressException</code> to be thrown on the second and
     * subsequent calls. Operations executed before a transaction has been opened,
     * or before reopening after a transaction is aborted or committed, have
     * undefined results; these may throw a <code>
     * TransactionNotInProgressException</code> exception.
     */
    public synchronized void begin()
    {
        checkForBegin();
        if (log.isDebugEnabled()) log.debug("Begin transaction was called on tx " + this);
        // initialize the ObjectEnvelope table
        objectEnvelopeTable = new ObjectEnvelopeTable(this);
        // register transaction
        implementation.getTxManager().registerTx(this);
        // mark tx as active (open)
        txStatus = Status.STATUS_ACTIVE;
    }

    protected void checkForBegin()
    {
        /**
         * Is the associated database non-null and open? ODMG 3.0 says it must be.
         */
        if ((curDB == null) || !curDB.isOpen())
        {
            throw new DatabaseClosedException("Database is not open. Must have an open DB to begin the Tx.");
        }
        if (isOpen())
        {
            log.error("Transaction is already open");
            throw new org.odmg.TransactionInProgressException("Impossible to call begin on already opened tx");
        }
    }

    public String getGUID()
    {
        return txGUID;
    }

    /**
     * Get object by identity. First lookup among objects registered in the
     * transaction, then in persistent storage.
     * @param id The identity
     * @return The object
     * @throws PersistenceBrokerException
     */
    public Object getObjectByIdentity(Identity id)
            throws PersistenceBrokerException
    {
        checkOpen();
        ObjectEnvelope envelope = objectEnvelopeTable.getByIdentity(id);
        if (envelope != null)
        {
            return (envelope.needsDelete() ? null : envelope.getObject());
        }
        else
        {
            return getBroker().getObjectByIdentity(id);
        }
    }

    /**
     * Registers the object (without locking) with this transaction. This method
     * expects that the object was already locked, no check is done!!!
     */
    void doSingleRegister(RuntimeObject rtObject, int lockMode)
            throws LockNotGrantedException, PersistenceBrokerException
    {
        if(log.isDebugEnabled()) log.debug("Register object " + rtObject.getIdentity());
        Object objectToRegister = rtObject.getObj();
        /*
        if the object is a Proxy there are two options:
        1. The proxies real subject has already been materialized:
           we take this real subject as the object to register and proceed
           as if it were a ordinary object.
        2. The real subject has not been materialized: Then there is nothing
           to be registered now!
           Of course we might just materialize the real subject to have something
           to register. But this would make proxies useless for ODMG as proxies would
           get materialized even if their real subjects were not used by the
           client app.
           Thus we register the current transaction as a Listener to the IndirectionHandler
           of the Proxy.
           Only when the IndirectionHandler performs the materialization of the real subject
           at some later point in time it invokes callbacks on all it's listeners.
           Using this callback we can defer the registering until it's really needed.
        */
        if(rtObject.isProxy())
        {
            IndirectionHandler handler = rtObject.getHandler();
            if(handler == null)
            {
                throw new OJBRuntimeException("Unexpected error, expect an proxy object as indicated: " + rtObject);
            }
            if (handler.alreadyMaterialized())
            {
                objectToRegister = handler.getRealSubject();
            }
            else
            {
                registerToIndirectionHandler(handler);
                registerUnmaterializedLocks(rtObject.getObj());
                // all work is done, so set to null
                objectToRegister = null;
            }
        }
        // no Proxy and is not null, register real object
        if (objectToRegister != null)
        {
            ObjectEnvelope envelope = objectEnvelopeTable.getByIdentity(rtObject.getIdentity());
            // if we found an envelope, object is already registered --> we do nothing
            // than refreshing the object!
            if ((envelope == null))
            {
                // register object itself
                envelope = objectEnvelopeTable.get(rtObject.getIdentity(), objectToRegister, rtObject.isNew());
            }
            else
            {
                /*
                arminw:
                if an different instance of the same object was locked again
                we should replace the old instance with new one to make
                accessible the changed fields
                */
                envelope.refreshObjectIfNeeded(objectToRegister);
            }
            /*
            arminw:
            For better performance we check if this object has already a write lock
            in this case we don't need to acquire a write lock on commit
            */
            if(lockMode == Transaction.WRITE)
            {
                // signal ObjectEnvelope that a WRITE lock is already acquired
                envelope.setWriteLocked(true);
            }
        }
    }

    /**
     * we only use the registrationList map if the object is not a proxy. During the
     * reference locking, we will materialize objects and they will enter the registered for
     * lock map.
     */
    private void lockAndRegisterReferences(ClassDescriptor cld, Object sourceObject, int lockMode, List registeredObjects) throws LockNotGrantedException
    {
        if (implicitLocking)
        {
            Iterator i = cld.getObjectReferenceDescriptors(true).iterator();
            while (i.hasNext())
            {
                ObjectReferenceDescriptor rds = (ObjectReferenceDescriptor) i.next();
                Object refObj = rds.getPersistentField().get(sourceObject);
                if (refObj != null)
                {
                    boolean isProxy = ProxyHelper.isProxy(refObj);
                    RuntimeObject rt = isProxy ? new RuntimeObject(refObj, this, false) : new RuntimeObject(refObj, this);
                    if (!registrationList.contains(rt.getIdentity()))
                    {
                        lockAndRegister(rt, lockMode, registeredObjects);
                    }
                }
            }
        }
    }

    private void lockAndRegisterCollections(ClassDescriptor cld, Object sourceObject, int lockMode, List registeredObjects) throws LockNotGrantedException
    {
        if (implicitLocking)
        {
            Iterator i = cld.getCollectionDescriptors(true).iterator();
            while (i.hasNext())
            {
                CollectionDescriptor cds = (CollectionDescriptor) i.next();
                Object col = cds.getPersistentField().get(sourceObject);
                if (col != null)
                {
                    CollectionProxy proxy = ProxyHelper.getCollectionProxy(col);
                    if (proxy != null)
                    {
                        if (!proxy.isLoaded())
                        {
                            if (log.isDebugEnabled()) log.debug("adding self as listener to collection proxy");
                            proxy.addListener(this);
                            registeredCollectionProxies.add(proxy);
                            continue;
                        }
                    }
                    Iterator colIterator = BrokerHelper.getCollectionIterator(col);
                    Object item = null;
                    try
                    {
                        while (colIterator.hasNext())
                        {
                            item = colIterator.next();
                            RuntimeObject rt = new RuntimeObject(item, this);
                            if (rt.isProxy())
                            {
                                IndirectionHandler handler = ProxyHelper.getIndirectionHandler(item);
                                if (!handler.alreadyMaterialized())
                                {
                                    registerToIndirectionHandler(handler);
                                    continue;
                                }
                                else
                                {
                                    // @todo consider registering to hear when this is
                                    // derefernced instead of just loading here -bmc
                                    item = handler.getRealSubject();
                                }
                            }
                            if (!registrationList.contains(rt.getIdentity()))
                            {
                                lockAndRegister(rt, lockMode, registeredObjects);
                            }
                        }
                    }
                    catch (LockNotGrantedException e)
                    {
                        String eol = SystemUtils.LINE_SEPARATOR;
                        log.error("Lock not granted, while lock collection references[" +
                                eol + "current reference descriptor:" +
                                eol + cds.toXML() +
                                eol + "object to lock: " + item +
                                eol + "main object class: " + sourceObject.getClass().getName() +
                                eol + "]", e);
                        throw e;
                    }
                }
            }
        }
    }

    /**
     *  this callback is invoked before an Object is materialized
     *  within an IndirectionHandler.
     *  @param handler the invoking handler
     *  @param oid the identity of the object to be materialized
     */
    public void beforeMaterialization(IndirectionHandler handler, Identity oid)
    {
        //noop
    }

    /**
     *  this callback is invoked after an Object is materialized
     *  within an IndirectionHandler.
     *  this callback allows to defer registration of objects until
     *  it's really neccessary.
     *  @param handler the invoking handler
     *  @param materializedObject the materialized Object
     */
    public void afterMaterialization(IndirectionHandler handler, Object materializedObject)
    {
        try
        {
            Identity oid = handler.getIdentity();
            if (log.isDebugEnabled())
            log.debug("deferred registration: " + oid);
            if(!isOpen())
            {
                log.error("Proxy object materialization outside of a running tx, obj=" + oid);
                try{throw new Exception("Proxy object materialization outside of a running tx, obj=" + oid);}catch(Exception e)
                {
                e.printStackTrace();
                }
            }
            ClassDescriptor cld = getBroker().getClassDescriptor(materializedObject.getClass());
            RuntimeObject rt = new RuntimeObject(materializedObject, oid, cld, false, false);
            lockAndRegister(rt, Transaction.READ, isImplicitLocking(), getRegistrationList());
        }
        catch (Throwable t)
        {
            log.error("Register materialized object with this tx failed", t);
            throw new LockNotGrantedException(t.getMessage());
        }
        unregisterFromIndirectionHandler(handler);
    }

    protected synchronized void unRegisterFromAllIndirectionHandlers()
    {
        // unregistering manipulates the registeredIndirectionHandlers vector
        // we have to loop through this vector to avoid index proplems.
        for (int i = registeredIndirectionHandlers.size() - 1; i >= 0; i--)
        {
            unregisterFromIndirectionHandler((IndirectionHandler) registeredIndirectionHandlers.get(i));
        }
    }

    protected synchronized void unRegisterFromAllCollectionProxies()
    {
        for (int i = registeredCollectionProxies.size() - 1; i >= 0; i--)
        {
            unregisterFromCollectionProxy((CollectionProxy) registeredCollectionProxies.get(i));
        }
    }

    protected synchronized void unregisterFromCollectionProxy(CollectionProxy handler)
    {
        handler.removeListener(this);
        registeredCollectionProxies.remove(handler);
    }

    protected synchronized void unregisterFromIndirectionHandler(IndirectionHandler handler)
    {
        handler.removeListener(this);
        registeredIndirectionHandlers.remove(handler);
    }

    protected synchronized void registerToIndirectionHandler(IndirectionHandler handler)
    {
        handler.addListener(this);
        registeredIndirectionHandlers.add(handler);
    }

    /**
     * register proxy objects that were locked but haven't been materialized yet
     * so they can be unlocked when closing the transaction
     */
    protected void registerUnmaterializedLocks(Object obj)
    {
        unmaterializedLocks.add(obj);
    }

    /**
     * Gets the broker associated with the transaction.
     * MBAIRD: only return the associated broker if the transaction is open,
     * if it's closed, throw a TransactionNotInProgressException. If we allow
     * brokers to be reaquired by an already closed transaction, there is a
     * very good chance the broker will be leaked as the doClose() method of
     * transactionImpl will never be called and thus the broker will never
     * be closed and returned to the pool.
     * @return Returns a PersistenceBroker
     * @throws TransactionNotInProgressException is the transaction is not open;
     */
    public PersistenceBrokerInternal getBrokerInternal()
    {
        if (broker == null || broker.isClosed())
        {
            checkOpen();
            try
            {
                checkForDB();
                broker = PersistenceBrokerFactoryFactory.instance().createPersistenceBroker(curDB.getPBKey());
            }
            catch (PBFactoryException e)
            {
                log.error("Cannot obtain PersistenceBroker from PersistenceBrokerFactory, " +
                        "found PBKey was " + curDB.getPBKey(), e);
                throw new PersistenceBrokerException(e);
            }
        }
        return broker;
    }

    public PersistenceBroker getBroker()
    {
        return getBrokerInternal();
    }

    /**
     * Returns true if an {@link org.apache.ojb.broker.PersistenceBroker} was associated with this
     * tx instance.
     */
    protected boolean hasBroker()
    {
        return broker != null && !broker.isClosed();
    }

    protected void cleanupBroker()
    {
        if(hasBroker())
        {
            try
            {
                if(broker.isInTransaction())
                {
                    broker.abortTransaction();
                }
            }
            finally
            {
                broker.close();
                broker = null;
            }
        }
    }

    /*
     * @see Configurable#configure(Configuration)
     */
    public void configure(Configuration config) throws ConfigurationException
    {
    }

    /**
     * @see org.apache.ojb.odmg.TransactionExt#setImplicitLocking(boolean)
     */
    public synchronized void setImplicitLocking(boolean value)
    {
        implicitLocking = value;
    }

    public boolean isImplicitLocking()
    {
        return implicitLocking;
    }

    /**
     * noop -- here for interface
     */
    public void beforeLoading(CollectionProxyDefaultImpl colProxy)
    {
        // noop
    }

    /**
     * Remove colProxy from list of pending collections and
     * register its contents with the transaction.
     */
    public void afterLoading(CollectionProxyDefaultImpl colProxy)
    {
        if (log.isDebugEnabled()) log.debug("loading a proxied collection a collection: " + colProxy);
        Collection data = colProxy.getData();
        for (Iterator iterator = data.iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            if(!isOpen())
            {
                log.error("Collection proxy materialization outside of a running tx, obj=" + o);
                try{throw new Exception("Collection proxy materialization outside of a running tx, obj=" + o);}
                catch(Exception e)
                {e.printStackTrace();}
            }
            else
            {
                Identity oid = getBroker().serviceIdentity().buildIdentity(o);
                ClassDescriptor cld = getBroker().getClassDescriptor(ProxyHelper.getRealClass(o));
                RuntimeObject rt = new RuntimeObject(o, oid, cld, false, ProxyHelper.isProxy(o));
                lockAndRegister(rt, Transaction.READ, isImplicitLocking(), getRegistrationList());
            }
        }
        unregisterFromCollectionProxy(colProxy);
    }

    protected void performTransactionAwareBeforeCommit()
    {
        Enumeration en = objectEnvelopeTable.elements();
        while (en.hasMoreElements())
        {
            ((ObjectEnvelope) en.nextElement()).beforeCommit();
        }
    }
    protected void performTransactionAwareAfterCommit()
    {
        Enumeration en = objectEnvelopeTable.elements();
        try
        {
            while (en.hasMoreElements())
            {
                ((ObjectEnvelope) en.nextElement()).afterCommit();
            }
        }
        catch(Exception e)
        {
            log.error("Unexpected error while perform 'TransactionAware#afterCommit()' listener after commit of objects," +
                    " after commit you can't rollback - exception will be skipped.", e);
        }
    }
    protected void performTransactionAwareBeforeRollback()
    {
        Enumeration en = objectEnvelopeTable.elements();
        while (en.hasMoreElements())
        {
            try
            {
                ((ObjectEnvelope) en.nextElement()).beforeAbort();
            }
            catch(Exception e)
            {
                log.error("Unexpected error while perform 'TransactionAware#beforeAbort()' listener before rollback of objects" +
                    " - exception will be skipped to complete rollback.", e);
            }
        }
    }
    protected void performTransactionAwareAfterRollback()
    {
        Enumeration en = objectEnvelopeTable.elements();
        try
        {
            while (en.hasMoreElements())
            {
                ((ObjectEnvelope) en.nextElement()).afterAbort();
            }
        }
        catch(Exception e)
        {
            log.error("Unexpected error while perform 'TransactionAware#afterAbort()' listener after rollback of objects" +
                    " - exception will be skipped.", e);
        }
    }

    /**
     * Detect new objects.
     */
    protected boolean isTransient(ClassDescriptor cld, Object obj, Identity oid)
    {
        // if the Identity is transient we assume a non-persistent object
        boolean isNew = oid != null && oid.isTransient();
        /*
        detection of new objects is costly (select of ID in DB to check if object
        already exists) we do:
        a. check if the object has nullified PK field
        b. check if the object is already registered
        c. lookup from cache and if not found, last option select on DB
        */
        if(!isNew)
        {
            final PersistenceBroker pb = getBroker();
            if(cld == null)
            {
                cld = pb.getClassDescriptor(obj.getClass());
            }
            isNew = pb.serviceBrokerHelper().hasNullPKField(cld, obj);
            if(!isNew)
            {
                if(oid == null)
                {
                    oid = pb.serviceIdentity().buildIdentity(cld, obj);
                }
                final ObjectEnvelope mod = objectEnvelopeTable.getByIdentity(oid);
                if(mod != null)
                {
                    // already registered object, use current state
                    isNew = mod.needsInsert();
                }
                else
                {
                    // if object was found cache, assume it's old
                    // else make costly check against the DB
                    isNew = pb.serviceObjectCache().lookup(oid) == null
                            && !pb.serviceBrokerHelper().doesExist(cld, oid, obj);
                }
            }
        }
        return isNew;
    }

    /**
     * Allows to change the <em>cascading delete</em> behavior of the specified reference
     * of the target class while this transaction is in use.
     *
     * @param target The class to change cascading delete behavior of the references.
     * @param referenceField The field name of the 1:1, 1:n or 1:n reference.
     * @param doCascade If <em>true</em> cascading delete is enabled, <em>false</em> disabled.
     */
    public void setCascadingDelete(Class target, String referenceField, boolean doCascade)
    {
        ClassDescriptor cld = getBroker().getClassDescriptor(target);
        ObjectReferenceDescriptor ord = cld.getObjectReferenceDescriptorByName(referenceField);
        if(ord == null)
        {
            ord = cld.getCollectionDescriptorByName(referenceField);
        }
        if(ord == null)
        {
            throw new CascadeSettingException("Invalid reference field name '" + referenceField
                    + "', can't find 1:1, 1:n or m:n relation with that name in " + target);
        }
        runtimeCascadeDeleteMap.put(ord, (doCascade ? Boolean.TRUE : Boolean.FALSE));
    }

    /**
     * Allows to change the <em>cascading delete</em> behavior of all references of the
     * specified class while this transaction is in use - if the specified class is an
     * interface, abstract class or class with "extent" classes the cascading flag will
     * be propagated.
     *
     * @param target The class to change cascading delete behavior of all references.
     * @param doCascade If <em>true</em> cascading delete is enabled, <em>false</em> disabled.
     */
    public void setCascadingDelete(Class target, boolean doCascade)
    {
        ClassDescriptor cld = getBroker().getClassDescriptor(target);
        List extents = cld.getExtentClasses();
        Boolean result = doCascade ? Boolean.TRUE : Boolean.FALSE;
        setCascadingDelete(cld, result);
        if(extents != null && extents.size() > 0)
        {
            for(int i = 0; i < extents.size(); i++)
            {
                Class extent =  (Class) extents.get(i);
                ClassDescriptor tmp = getBroker().getClassDescriptor(extent);
                setCascadingDelete(tmp, result);
            }
        }
    }

    private void setCascadingDelete(ClassDescriptor cld, Boolean cascade)
    {
        List singleRefs = cld.getObjectReferenceDescriptors(true);
        for(int i = 0; i < singleRefs.size(); i++)
        {
            Object o = singleRefs.get(i);
            runtimeCascadeDeleteMap.put(o, cascade);
        }
        List collectionRefs = cld.getCollectionDescriptors(true);
        for(int i = 0; i < collectionRefs.size(); i++)
        {
            Object o =  collectionRefs.get(i);
            runtimeCascadeDeleteMap.put(o, cascade);
        }
    }

    private HashMap runtimeCascadeDeleteMap = new HashMap();
    /**
     * Returns <em>true</em> if cascading delete is enabled for the specified
     * single or collection descriptor.
     */
    protected boolean cascadeDeleteFor(ObjectReferenceDescriptor ord)
    {
        boolean result;
        Boolean runtimeSetting = (Boolean) runtimeCascadeDeleteMap.get(ord);
        if(runtimeSetting == null)
        {
            /*
            arminw: Here we use the auto-delete flag defined in metadata
            */
            result = ord.getCascadingDelete() == ObjectReferenceDescriptor.CASCADE_OBJECT;
        }
        else
        {
            result = runtimeSetting.booleanValue();
        }
        return result;
    }

    int getImpliciteLockType(int parentLockMode)
    {
        return (parentLockMode == Transaction.WRITE && impliciteWriteLocks) ? Transaction.WRITE : Transaction.READ;
    }

    /**
     * Return <em>true</em> if the OJB ordering algorithm is enabled.
     * @see #setOrdering(boolean)
     */
    public boolean isOrdering()
    {
        return ordering;
    }

    /**
     * Allows to enable/disable the OJB persistent object ordering algorithm. If
     * <em>true</em> OJB try to order the modified/new/deleted objects in a correct order
     * (based on a algorithm) before the objects are written to the persistent storage.
     * <br/>
     * If <em>false</em> the order of the objects rely on the order specified by
     * the user and on settings like {@link #setImplicitLocking(boolean)}.
     *
     * @param ordering Set <em>true</em> to enable object ordering on commit.
     */
    public void setOrdering(boolean ordering)
    {
        this.ordering = ordering;
    }


    //============================================================
    // inner class
    //============================================================
    /**
     * This was thrown when something wrong with the cascading delete setting.
     */
    static class CascadeSettingException extends OJBRuntimeException
    {
        public CascadeSettingException()
        {
        }

        public CascadeSettingException(String msg)
        {
            super(msg);
        }

        public CascadeSettingException(Throwable cause)
        {
            super(cause);
        }

        public CascadeSettingException(String msg, Throwable cause)
        {
            super(msg, cause);
        }
    }
}
