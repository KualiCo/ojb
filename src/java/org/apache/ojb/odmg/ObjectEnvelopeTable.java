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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.OptimisticLockException;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.accesslayer.ConnectionManagerIF;
import org.apache.ojb.broker.core.proxy.CollectionProxy;
import org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.odmg.link.LinkEntry;
import org.apache.ojb.odmg.link.LinkEntryMtoN;
import org.apache.ojb.odmg.states.StateOldClean;
import org.odmg.LockNotGrantedException;
import org.odmg.ODMGRuntimeException;
import org.odmg.Transaction;
import org.odmg.TransactionAbortedException;

/**
 * manages all ObjectEnvelopes included by a transaction.
 * Performs commit, and rollack operations on all included Envelopes.
 *
 * @author Thomas Mahler
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 *
 *         MBAIRD: added explicit closing and de-referencing to prevent any
 *         GC issues.
 */
public class ObjectEnvelopeTable
{
    private Logger log = LoggerFactory.getLogger(ObjectEnvelopeTable.class);
    private TransactionImpl transaction;

    /**
     * A list of {@link org.apache.ojb.broker.Identity} objects which are
     * new associated with an object and should be protected from being marked
     * as "delete". E.g. if a collection reference C is moved from object A1 to A2,
     * then A1 wants to "delete" C and A2 wants to mark the new C object as "new".
     */
    private List newAssociatedIdentites = new ArrayList();
    private List m2nLinkList = new ArrayList();
    private List m2nUnlinkList = new ArrayList();
    private List markedForDeletionList = new ArrayList();
    private List markedForInsertList = new ArrayList();

    /** the internal table mapping Objects to their ObjectTransactionWrappers */
    private Map mhtObjectEnvelopes = new HashMap();

    /**
     * a vector containing the ObjectEnvelope objects representing modifications
     * in the order they were added. If an ObjectEnvelope is added twice, only
     * the the second addition is ignored.
     */
    private ArrayList mvOrderOfIds = new ArrayList();

    /** marker used to avoid superfluous reordering and commiting */
    private boolean needsCommit = false;

    /** Creates new ObjectEnvelopeTable */
    public ObjectEnvelopeTable(TransactionImpl myTransaction)
    {
        transaction = myTransaction;
    }

    TransactionImpl getTransaction()
    {
        return transaction;
    }

    /** prepare this instance for reuse */
    public void refresh()
    {
        needsCommit = false;
        mhtObjectEnvelopes = new HashMap();
        mvOrderOfIds = new ArrayList();
        afterWriteCleanup();
    }

    void afterWriteCleanup()
    {
        m2nLinkList.clear();
        m2nUnlinkList.clear();
        newAssociatedIdentites.clear();
        markedForDeletionList.clear();
        markedForInsertList.clear();
    }

    /**
     * Perform write to DB on all registered object wrapper ({@link ObjectEnvelope})
     *
     * @param reuse When all registered objects be re-used after writing to
     * DB set <em>true</em>, else set <em>false</em> to improve performance.
     */
    public void writeObjects(boolean reuse) throws TransactionAbortedException, LockNotGrantedException
    {
        PersistenceBroker broker = transaction.getBroker();
        ConnectionManagerIF connMan = broker.serviceConnectionManager();
        boolean saveBatchMode = connMan.isBatchMode();

        try
        {
            if(log.isDebugEnabled())
            {
                log.debug(
                        "PB is in internal tx: "
                                + broker.isInTransaction()
                                + "  broker was: "
                                + broker);
            }
            // all neccessary db operations are executed within a PersistenceBroker transaction:
            if(!broker.isInTransaction())
            {
                log.error("PB associated with current odmg-tx is not in tx");
                throw new TransactionAbortedException("Underlying PB is not in tx, was begin call done before commit?");
            }

            // Committing has to be done in two phases. First implicitly upgrade to lock on all related
            // objects of objects in this transaction. Then the list of locked objects has to be
            // reordered to solve referential integrity dependencies, then the objects are
            // written into the database.

            // 0. turn on the batch mode
            connMan.setBatchMode(true);

            // 1. mark objects no longer available in collection
            // for delete and add new found objects
            checkAllEnvelopes(broker);

            // 2. mark all dependend objects for cascading insert/delete
            cascadingDependents();

            // 3. upgrade implicit locks.
            //upgradeImplicitLocksAndCheckIfCommitIsNeeded();
            upgradeLockIfNeeded();

            // 4. Reorder objects
            reorder();
//            System.out.println("## ordering: ");
//            for(int i = 0; i < mvOrderOfIds.size(); i++)
//            {
//                System.out.println("" + mvOrderOfIds.get(i));
//            }
//            System.out.println("## ordering end");

            // 5. write objects.
            writeAllEnvelopes(reuse);

            // 6. execute batch
            connMan.executeBatch();

            // 7. Update all Envelopes to new CleanState
            prepareForReuse(reuse);

            // 6. commit cleanup
            afterWriteCleanup();

        }
        catch(Exception e)
        {
            connMan.clearBatch();
            /*
            arminw:
            log only a warn message, because in top-level methods
            a error log will be done ditto
            */
            if(e instanceof OptimisticLockException)
            {
                // make error log to show the full stack trace one time
                log.error("Optimistic lock exception while write objects", e);
                // PB OptimisticLockException should be clearly signalled to the user
                Object sourceObject = ((OptimisticLockException) e).getSourceObject();
                throw new LockNotGrantedException("Optimistic lock exception occur, source object was (" + sourceObject + ")," +
                        " message was (" + e.getMessage() + ")");
            }
            else if(!(e instanceof RuntimeException))
            {
                log.warn("Error while write objects for tx " + transaction, e);
                throw new ODMGRuntimeException("Unexpected error while write objects: " + e.getMessage());
            }
            else
            {
                log.warn("Error while write objects for tx " + transaction, e);
                throw (RuntimeException) e;
            }
        }
        finally
        {
            needsCommit = false;
            connMan.setBatchMode(saveBatchMode);
        }
    }

    /** commit all envelopes against the current broker */
    private void writeAllEnvelopes(boolean reuse)
    {
        // perform remove of m:n indirection table entries first
        performM2NUnlinkEntries();

        Iterator iter;
        // using clone to avoid ConcurentModificationException
        iter = ((List) mvOrderOfIds.clone()).iterator();
        while(iter.hasNext())
        {
            ObjectEnvelope mod = (ObjectEnvelope) mhtObjectEnvelopes.get(iter.next());
            boolean insert = false;
            if(needsCommit)
            {
                insert = mod.needsInsert();
                mod.getModificationState().commit(mod);
                if(reuse && insert)
                {
                    getTransaction().doSingleLock(mod.getClassDescriptor(), mod.getObject(), mod.getIdentity(), Transaction.WRITE);
                }
            }
            /*
            arminw: important to call this cleanup method for each registered
            ObjectEnvelope, because this method will e.g. remove proxy listener
            objects for registered objects.
            */
            mod.cleanup(reuse, insert);
        }
        // add m:n indirection table entries
        performM2NLinkEntries();
    }

    /**
     * Mark objects no longer available in collection for delete and new objects for insert.
     *
     * @param broker the PB to persist all objects
     */
    private void checkAllEnvelopes(PersistenceBroker broker)
    {
        Iterator iter = ((List) mvOrderOfIds.clone()).iterator();
        while(iter.hasNext())
        {
            ObjectEnvelope mod = (ObjectEnvelope) mhtObjectEnvelopes.get(iter.next());
            // only non transient objects should be performed
            if(!mod.getModificationState().isTransient())
            {
                mod.markReferenceElements(broker);
            }
        }
    }

    /**
     * This method have to be called to reuse all registered {@link ObjectEnvelope}
     * objects after transaction commit/flush/checkpoint call.
     */
    private void prepareForReuse(boolean reuse)
    {
        if(reuse)
        {
            // using clone to avoid ConcurentModificationException
            Iterator iter = ((List) mvOrderOfIds.clone()).iterator();
            while(iter.hasNext())
            {
                ObjectEnvelope mod = (ObjectEnvelope) mhtObjectEnvelopes.get(iter.next());
                if(!needsCommit || (mod.getModificationState() == StateOldClean.getInstance()
                        || mod.getModificationState().isTransient()))
                {
                    // nothing to do
                }
                else
                {
                    mod.setModificationState(mod.getModificationState().markClean());
                }
            }
        }
    }

    /**
     * Checks the status of all modified objects and
     * upgrade the lock if needed, cleanup the {@link ObjectEnvelope}
     * objects.
     */
    private void upgradeLockIfNeeded()
    {
        // using clone to avoid ConcurentModificationException
        Iterator iter = ((List) mvOrderOfIds.clone()).iterator();
        TransactionImpl tx = getTransaction();
        ObjectEnvelope mod;
        while(iter.hasNext())
        {
            mod = (ObjectEnvelope) mhtObjectEnvelopes.get(iter.next());
            // ignore transient objects
            if(!mod.getModificationState().isTransient())
            {
                /*
                now we check if all modified objects has a write lock. On insert of new
                objects we don't need a write lock.
                */
                if(!mod.needsInsert())
                {
                    if((mod.needsDelete() || mod.needsUpdate()
                            || mod.hasChanged(tx.getBroker())))
                    {
                        needsCommit = true;
                        // mark object dirty
                        mod.setModificationState(mod.getModificationState().markDirty());
                        ClassDescriptor cld = mod.getClassDescriptor();
                        // if the object isn't already locked, we will do it now
                        if(!mod.isWriteLocked())
                        {
                            tx.doSingleLock(cld, mod.getObject(), mod.getIdentity(), Transaction.WRITE);
                        }
                    }
                }
                else
                {
                    needsCommit = true;
                }
            }
        }
    }

    /** perform rollback on all tx-states */
    public void rollback()
    {
        try
        {
            Iterator iter = mvOrderOfIds.iterator();
            while(iter.hasNext())
            {
                ObjectEnvelope mod = (ObjectEnvelope) mhtObjectEnvelopes.get(iter.next());
                if(log.isDebugEnabled())
                    log.debug("rollback: " + mod);
                // if the Object has been modified by transaction, mark object as dirty
                if(mod.hasChanged(transaction.getBroker()))
                {
                    mod.setModificationState(mod.getModificationState().markDirty());
                }
                mod.getModificationState().rollback(mod);
            }
        }
        finally
        {
            needsCommit = false;
        }
        afterWriteCleanup();
    }

    /** remove an objects entry from the object registry */
    public void remove(Object pKey)
    {
        Identity id;
        if(pKey instanceof Identity)
        {
            id = (Identity) pKey;
        }
        else
        {
            id = transaction.getBroker().serviceIdentity().buildIdentity(pKey);
        }
        mhtObjectEnvelopes.remove(id);
        mvOrderOfIds.remove(id);
    }

    /**
     * Get an enumeration of all the elements in this ObjectEnvelopeTable
     * in random order.
     *
     * @return Enumeration an enumeration of all elements managed by this ObjectEnvelopeTable
     */
    public Enumeration elements()
    {
        return java.util.Collections.enumeration(mhtObjectEnvelopes.values());
    }

    /** retrieve an objects ObjectModification state from the hashtable */
    public ObjectEnvelope getByIdentity(Identity id)
    {
        return (ObjectEnvelope) mhtObjectEnvelopes.get(id);
    }

    /**
     * retrieve an objects ObjectEnvelope state from the hashtable.
     * If no ObjectEnvelope is found, a new one is created and returned.
     *
     * @return the resulting ObjectEnvelope
     */
    public ObjectEnvelope get(Object pKey, boolean isNew)
    {
        PersistenceBroker broker = transaction.getBroker();
        Identity oid = broker.serviceIdentity().buildIdentity(pKey);
        return get(oid, pKey, isNew);
    }

    /**
     * retrieve an objects ObjectEnvelope state from the hashtable.
     * If no ObjectEnvelope is found, a new one is created and returned.
     *
     * @return the resulting ObjectEnvelope
     */
    public ObjectEnvelope get(Identity oid, Object pKey, boolean isNew)
    {
        ObjectEnvelope result = getByIdentity(oid);
        if(result == null)
        {
            result = new ObjectEnvelope(this, oid, pKey, isNew);
            mhtObjectEnvelopes.put(oid, result);
            mvOrderOfIds.add(oid);
            if(log.isDebugEnabled())
                log.debug("register: " + result);
        }
        return result;
    }

    /** Returns a String representation of this object */
    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        String eol = SystemUtils.LINE_SEPARATOR;
        buf.append("# ObjectEnvelopeTable dump:" + eol + "start[");
        Enumeration en = elements();
        while(en.hasMoreElements())
        {
            ObjectEnvelope mod = (ObjectEnvelope) en.nextElement();
            buf.append(mod.toString() + eol);
        }
        buf.append("]end");
        return buf.toString();
    }

    /** retrieve an objects ObjectModification state from the hashtable */
    public boolean contains(Identity oid)
    {
        //Integer keyInteger = new Integer(System.identityHashCode(key));
        return mhtObjectEnvelopes.containsKey(oid);
    }

    /** Reorder the objects in the table to resolve referential integrity dependencies. */
    private void reorder()
    {
        if(getTransaction().isOrdering() && needsCommit && mhtObjectEnvelopes.size() > 1)
        {
            ObjectEnvelopeOrdering ordering = new ObjectEnvelopeOrdering(mvOrderOfIds, mhtObjectEnvelopes);
            ordering.reorder();
            Identity[] newOrder = ordering.getOrdering();

            mvOrderOfIds.clear();
            for(int i = 0; i < newOrder.length; i++)
            {
                mvOrderOfIds.add(newOrder[i]);
            }
        }
    }

    void cascadingDependents()
    {
        Iterator it = mhtObjectEnvelopes.values().iterator();
        ObjectEnvelope mod;
        // first we search for all deleted/insert objects
        while(it.hasNext())
        {
            mod = (ObjectEnvelope) it.next();
            if(mod.needsDelete())
            {
                addForDeletionDependent(mod);
            }
            else if(mod.needsInsert())
            {
                addForInsertDependent(mod);
            }
        }
        /*
        Now start cascade insert/delete work. The order of first delete
        then insert is mandatory, because the user could move unmaterialized
        collection proxy objects from one existing, which was deleted, to a new object. In this case
        the proxy was materialized on deletion of the main object, but on performing
        the cascading insert the collection objects will be found and assigned to the new object.
        */
        cascadeMarkedForDeletion();
        cascadeMarkedForInsert();
    }

    void addNewAssociatedIdentity(Identity oid)
    {
        newAssociatedIdentites.add(oid);
    }

    boolean isNewAssociatedObject(Identity oid)
    {
        return newAssociatedIdentites.contains(oid);
    }

    void addForInsertDependent(ObjectEnvelope mod)
    {
        markedForInsertList.add(mod);
    }

    /** Starts recursive insert on all insert objects object graph */
    private void cascadeMarkedForInsert()
    {
        // This list was used to avoid endless recursion on circular references
        List alreadyPrepared = new ArrayList();
        for(int i = 0; i < markedForInsertList.size(); i++)
        {
            ObjectEnvelope mod = (ObjectEnvelope) markedForInsertList.get(i);
            // only if a new object was found we cascade to register the dependent objects
            if(mod.needsInsert())
            {
                cascadeInsertFor(mod, alreadyPrepared);
                alreadyPrepared.clear();
            }
        }
        markedForInsertList.clear();
    }

    /**
     * Walk through the object graph of the specified insert object. Was used for
     * recursive object graph walk.
     */
    private void cascadeInsertFor(ObjectEnvelope mod, List alreadyPrepared)
    {
        // avoid endless recursion, so use List for registration
        if(alreadyPrepared.contains(mod.getIdentity())) return;
        alreadyPrepared.add(mod.getIdentity());

        ClassDescriptor cld = getTransaction().getBroker().getClassDescriptor(mod.getObject().getClass());

        List refs = cld.getObjectReferenceDescriptors(true);
        cascadeInsertSingleReferences(mod, refs, alreadyPrepared);

        List colls = cld.getCollectionDescriptors(true);
        cascadeInsertCollectionReferences(mod, colls, alreadyPrepared);
    }

    private void cascadeInsertSingleReferences(ObjectEnvelope source, List descriptor, List alreadyPrepared)
    {
        for(int i = 0; i < descriptor.size(); i++)
        {
            ObjectReferenceDescriptor ord = (ObjectReferenceDescriptor) descriptor.get(i);
            Object depObj = ord.getPersistentField().get(source.getObject());

            if(depObj != null)
            {
                // in any case we have to link the source object when the object needs insert
                source.addLinkOneToOne(ord, false);

                IndirectionHandler handler = ProxyHelper.getIndirectionHandler(depObj);
                // if the object is not materialized, nothing has changed
                if(handler == null || handler.alreadyMaterialized())
                {
                    RuntimeObject rt;
                    // if materialized
                    if(handler != null)
                    {
                        rt = new RuntimeObject(handler.getRealSubject(), getTransaction(), false);
                    }
                    else
                    {
                        rt = new RuntimeObject(depObj, getTransaction());
                    }
                    Identity oid = rt.getIdentity();
                    if(!alreadyPrepared.contains(oid))
                    {
                        ObjectEnvelope depMod = getByIdentity(oid);
                        // if the object isn't registered and is a new object, register it
                        // else we have nothing to do
                        if(depMod == null && rt.isNew())
                        {
                            getTransaction().lockAndRegister(rt, Transaction.WRITE, false, getTransaction().getRegistrationList());
                            depMod = getByIdentity(oid);
                            cascadeInsertFor(depMod, alreadyPrepared);
                        }
                    }
                }
            }
        }
    }

    private void cascadeInsertCollectionReferences(ObjectEnvelope source, List descriptor, List alreadyPrepared)
    {
        // PersistenceBroker pb = getTransaction().getBroker();
        for(int i = 0; i < descriptor.size(); i++)
        {
            CollectionDescriptor col = (CollectionDescriptor) descriptor.get(i);
            Object collOrArray = col.getPersistentField().get(source.getObject());
            CollectionProxy proxy = ProxyHelper.getCollectionProxy(collOrArray);
            /*
            on insert we perform only materialized collection objects. This should be
            sufficient, because in method #cascadingDependents() we make sure that on
            move of unmaterialized collection objects the proxy was materialized if needed.
            */
            if(proxy == null && collOrArray != null)
            {
                Iterator it = BrokerHelper.getCollectionIterator(collOrArray);
                while(it.hasNext())
                {
                    Object colObj = it.next();
                    if(colObj != null)
                    {
                        RuntimeObject rt = new RuntimeObject(colObj, getTransaction());
                        Identity oid = rt.getIdentity();
                        /*
                        arminw:
                        only when the main object need insert we start with FK assignment
                        of the 1:n and m:n relations. If the main objects need update (was already persisted)
                        it should be handled by the object state detection in ObjectEnvelope
                        */
                        if(source.needsInsert())
                        {
                            /*
                            arminw:
                            TODO: what is the valid way to go, when the collection object itself is
                            a unmaterialized proxy object? Think in this case we should materialize the
                            object when the main object needs insert, because we have to assign the FK values
                            to the main object
                            */
                            colObj = ProxyHelper.getRealObject(colObj);
                            ObjectEnvelope oe = getByIdentity(oid);
                            if(oe == null)
                            {
                                getTransaction().lockAndRegister(rt, Transaction.WRITE, false, getTransaction().getRegistrationList());
                                oe = getByIdentity(oid);
                            }
                            if(col.isMtoNRelation())
                            {
                                // the main objects needs insert, thus add new m:n link
                                addM2NLinkEntry(col, source.getObject(), colObj);
                            }
                            else
                            {
                                // we mark collection reference for linking
                                oe.addLinkOneToN(col, source.getObject(), false);
                                /*
                                arminw: The referenced object could be already persisted, so we have
                                to dirty it to guarantee the setting of the FK (linking)
                                */
                                oe.setModificationState(oe.getModificationState().markDirty());
                            }
                            cascadeInsertFor(oe, alreadyPrepared);
                        }
                    }
                }
            }
        }
    }

    void addForDeletionDependent(ObjectEnvelope mod)
    {
        markedForDeletionList.add(mod);
    }

    /** Starts recursive delete on all delete objects object graph */
    private void cascadeMarkedForDeletion()
    {
        List alreadyPrepared = new ArrayList();
        for(int i = 0; i < markedForDeletionList.size(); i++)
        {
            ObjectEnvelope mod = (ObjectEnvelope) markedForDeletionList.get(i);
            // if the object wasn't associated with another object, start cascade delete
            if(!isNewAssociatedObject(mod.getIdentity()))
            {
                cascadeDeleteFor(mod, alreadyPrepared);
                alreadyPrepared.clear();
            }
        }
        markedForDeletionList.clear();
    }

    /**
     * Walk through the object graph of the specified delete object. Was used for
     * recursive object graph walk.
     */
    private void cascadeDeleteFor(ObjectEnvelope mod, List alreadyPrepared)
    {
        // avoid endless recursion
        if(alreadyPrepared.contains(mod.getIdentity())) return;

        alreadyPrepared.add(mod.getIdentity());

        ClassDescriptor cld = getTransaction().getBroker().getClassDescriptor(mod.getObject().getClass());

        List refs = cld.getObjectReferenceDescriptors(true);
        cascadeDeleteSingleReferences(mod, refs, alreadyPrepared);

        List colls = cld.getCollectionDescriptors(true);
        cascadeDeleteCollectionReferences(mod, colls, alreadyPrepared);
    }

    private void cascadeDeleteSingleReferences(ObjectEnvelope source, List descriptor, List alreadyPrepared)
    {
        for(int i = 0; i < descriptor.size(); i++)
        {
            ObjectReferenceDescriptor ord = (ObjectReferenceDescriptor) descriptor.get(i);
            if(getTransaction().cascadeDeleteFor(ord))
            {
                Object depObj = ord.getPersistentField().get(source.getObject());
                if(depObj != null)
                {
                    Identity oid = getTransaction().getBroker().serviceIdentity().buildIdentity(depObj);
                    // if(!isNewAssociatedObject(oid) && !alreadyPrepared.contains(oid))
                    // if the object has a new association with a different object, don't delete it
                    if(!isNewAssociatedObject(oid))
                    {
                        ObjectEnvelope depMod = get(oid, depObj, false);
                        depMod.setModificationState(depMod.getModificationState().markDelete());
                        cascadeDeleteFor(depMod, alreadyPrepared);
                    }
                }
            }
        }
    }

    private void cascadeDeleteCollectionReferences(ObjectEnvelope source, List descriptor, List alreadyPrepared)
    {
        PersistenceBroker pb = getTransaction().getBroker();
        for(int i = 0; i < descriptor.size(); i++)
        {
            CollectionDescriptor col = (CollectionDescriptor) descriptor.get(i);
            boolean cascadeDelete = getTransaction().cascadeDeleteFor(col);
            Object collOrArray = col.getPersistentField().get(source.getObject());
            // TODO: remove cast
            CollectionProxyDefaultImpl proxy = (CollectionProxyDefaultImpl) ProxyHelper.getCollectionProxy(collOrArray);
            // on delete we have to materialize dependent objects
            if(proxy != null)
            {
                collOrArray = proxy.getData();
            }
            if(collOrArray != null)
            {
                Iterator it = BrokerHelper.getCollectionIterator(collOrArray);
                while(it.hasNext())
                {
                    Object colObj = ProxyHelper.getRealObject(it.next());
                    Identity oid = pb.serviceIdentity().buildIdentity(colObj);
                    ObjectEnvelope colMod = get(oid, colObj, false);
                    if(cascadeDelete)
                    {
                        colMod.setModificationState(colMod.getModificationState().markDelete());
                        cascadeDeleteFor(colMod, alreadyPrepared);
                    }
                    else
                    {
                        if(!col.isMtoNRelation())
                        {
                            colMod.addLinkOneToN(col, source.getObject(), true);
                            colMod.setModificationState(colMod.getModificationState().markDirty());
                        }
                    }
                    if(col.isMtoNRelation())
                    {
                        addM2NUnlinkEntry(col, source.getObject(), colObj);
                    }
                }
            }
        }
    }

    void addM2NLinkEntry(CollectionDescriptor cod, Object leftSource, Object rightSource)
    {
        if(!cod.isMtoNRelation()) throw new OJBRuntimeException("Expect a m:n releation, but specified a 1:n");
        m2nLinkList.add(new LinkEntryMtoN(leftSource, cod, rightSource, false));
    }

    void performM2NLinkEntries()
    {
        PersistenceBroker broker = getTransaction().getBroker();
        LinkEntry entry;
        for(int i = 0; i < m2nLinkList.size(); i++)
        {
            entry = (LinkEntry) m2nLinkList.get(i);
            entry.execute(broker);
        }
    }

    void addM2NUnlinkEntry(CollectionDescriptor cod, Object leftSource, Object rightSource)
    {
        if(!cod.isMtoNRelation()) throw new OJBRuntimeException("Expect a m:n releation, but specified a 1:n");
        m2nUnlinkList.add(new LinkEntryMtoN(leftSource, cod, rightSource, true));
    }

    void performM2NUnlinkEntries()
    {
        PersistenceBroker broker = getTransaction().getBroker();
        LinkEntry entry;
        for(int i = 0; i < m2nUnlinkList.size(); i++)
        {
            entry = (LinkEntry) m2nUnlinkList.get(i);
            entry.execute(broker);
        }
    }

    /**
     * Replace the {@link org.apache.ojb.broker.Identity}
     * of a registered {@link ObjectEnvelope} object.
     *
     * @param newOid
     * @param oldOid
     * @return Returns <em>true</em> if successful.
     */
    boolean replaceRegisteredIdentity(Identity newOid, Identity oldOid)
    {
        /*
        TODO: Find a better solution
        */
        boolean result = false;
        Object oe = mhtObjectEnvelopes.remove(oldOid);
        if(oe != null)
        {
            mhtObjectEnvelopes.put(newOid, oe);
            int index = mvOrderOfIds.indexOf(oldOid);
            mvOrderOfIds.remove(index);
            mvOrderOfIds.add(index, newOid);
            result = true;
            if(log.isDebugEnabled()) log.debug("Replace identity: " + oldOid + " --replaced-by--> " + newOid);
        }
        else
        {
            log.warn("Can't replace unregistered object identity (" + oldOid + ") with new identity (" + newOid + ")");
        }
        return result;
    }
}