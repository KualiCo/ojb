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

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.accesslayer.ConnectionManagerIF;
import org.apache.ojb.broker.cache.ObjectCache;
import org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.ListProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.SetProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.CollectionProxyListener;
import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.MaterializationListener;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;

import org.apache.ojb.otm.EditingContext;
import org.apache.ojb.otm.OTMKit;
import org.apache.ojb.otm.copy.ObjectCopyStrategy;
import org.apache.ojb.otm.lock.LockManager;
import org.apache.ojb.otm.lock.LockType;
import org.apache.ojb.otm.lock.LockingException;
import org.apache.ojb.otm.states.State;
import org.apache.ojb.otm.swizzle.Swizzling;

/**
 *
 * Concrete implementation of EditingContext.
 *
 * @author  <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 * @see     org.apache.ojb.otm.EditingContext
 *
 */
public class ConcreteEditingContext
        implements EditingContext, MaterializationListener, ObjectCache
{
    // for hasBidirectionalAssociation method
    // Maps PBKeys to the sets of classes with/without
    // bidirectional associations
    private static HashMap _withBidirAsscMap = new HashMap();
    private static HashMap _withoutBidirAsscMap = new HashMap();

    private HashSet _withBidirAssc;
    private HashSet _withoutBidirAssc;

    private HashMap _objects;
    private ArrayList _order;
    private Transaction _tx;
    private PersistenceBroker _pb;
    private HashMap _original;
    private HashMap _checkpointed;
    private HashMap _colProxyListeners;

    //////////////////////////////////////////
    // Constructor
    //////////////////////////////////////////

    public ConcreteEditingContext(Transaction tx, PersistenceBroker pb)
    {
        PBKey pbkey;

        _tx = tx;
        _pb = pb;
        _objects = new HashMap();
        _order = new ArrayList();
        _original = new HashMap();
        _checkpointed = _original;
        pbkey = _pb.getPBKey();
        _withoutBidirAssc = (HashSet) _withoutBidirAsscMap.get(pbkey);
        if (_withoutBidirAssc != null)
        {
            _withBidirAssc = (HashSet) _withBidirAsscMap.get(pbkey);
        }
        else
        {
            _withoutBidirAssc = new HashSet();
            _withoutBidirAsscMap.put(pbkey, _withoutBidirAssc);
            _withBidirAssc = new HashSet();
            _withBidirAsscMap.put(pbkey, _withBidirAssc);
        }
    }

    //////////////////////////////////////////
    // EditingContext operations
    //////////////////////////////////////////

    /**
     * @see org.apache.ojb.otm.EditingContext#insert(Identity, Object, int)
     */
    public void insert(Identity oid, Object userObject, int lock)
            throws LockingException
    {
        ContextEntry entry;

        entry = insertInternal(oid, userObject, lock, true, null, new Stack());
        if ((entry != null) && entry.state.needsDelete()) {
            // Undelete it
            entry.state = State.PERSISTENT_CLEAN;
        }
    }

    private ContextEntry insertInternal(Identity oid, Object userObject,
            int lock, boolean canCreate, Identity insertBeforeThis, Stack stack)
            throws LockingException
    {
        ContextEntry entry;
        LockManager lockManager;
        Swizzling swizzlingStrategy;
        IndirectionHandler handler = null;
        OTMKit kit = _tx.getKit();
        // Are we building object's relations for the userObject in the transaction?
        // Otherwise we just get data from the "userObject" and put it into
        // the previously loaded/created object in the transaction
        boolean buildingObject = false;
        boolean lazySwizzle = false;

        if (lock == LockType.NO_LOCK)
        {
            return null;
        }

        entry = (ContextEntry) _objects.get(oid);

        if (userObject == null)
        {
            // invalidating object...
            _original.remove(oid);
            _checkpointed.remove(oid);
            if (entry != null)
            {
                entry.userObject = null;
                entry.cacheObject = null;
            }
            return entry;
        }

        lockManager = LockManager.getInstance();
        swizzlingStrategy = kit.getSwizzlingStrategy();

        handler = ProxyHelper.getIndirectionHandler(userObject);
        if ((handler != null) && handler.alreadyMaterialized())
        {
            userObject = handler.getRealSubject();
            handler = null;
        }

        if ((entry == null) || (entry.userObject == null))
        {
            // first insertion of the userObject into editing context
            Object swizzledObject = swizzlingStrategy.swizzle(userObject, null, _pb, this);
            entry = new ContextEntry(swizzledObject);
            if (entry.handler != null)
            {
                ObjectCopyStrategy copyStrategy = _tx.getKit().getCopyStrategy(oid);
                entry.cacheObject = copyStrategy.copy(userObject, _pb);
                // Assume that object exists, otherwise were the proxy came from?
                _objects.put(oid, entry);
                lockManager.ensureLock(oid, _tx, lock, _pb); // lock after _objects.put to avoid hanged locks
                entry.handler.addListener(this);
            }
            else
            {
                Object origCacheObj = _pb.getObjectByIdentity(oid);

                if ((origCacheObj == null) && !canCreate)
                {
                    // we don't create the objects by reachability
                    throw new IllegalStateException("Related object is neither persistent, nor otm-depentent: " + oid);
                }
                if (origCacheObj != null)
                {
                    entry.cacheObject = origCacheObj;
                }
                buildingObject = true;
                _objects.put(oid, entry);
                lockManager.ensureLock(oid, _tx, lock, _pb); // lock after _objects.put to avoid hanged locks

                if (userObject != null)
                {
                    if ((origCacheObj == null) && canCreate)
                    {
                        ObjectCopyStrategy copyStrategy = _tx.getKit().getCopyStrategy(oid);
                        entry.cacheObject = copyStrategy.copy(userObject, _pb);
                        entry.state = State.PERSISTENT_NEW;
                        if (kit.isEagerInsert(userObject)
                                || hasBidirectionalAssociation(userObject.getClass()))
                        {
                            _pb.store(entry.cacheObject, entry.state);
                            entry.state = State.PERSISTENT_CLEAN;
                            origCacheObj = entry.cacheObject;
                        }
                    }

                    if (origCacheObj != null)
                    {
                        _original.put(oid, getFields(userObject, false, true));
                    }
                }
            }
            if (insertBeforeThis != null)
            {
                int insertIndex = _order.indexOf(insertBeforeThis);
                _order.add(insertIndex, oid);
            }
            else
            {
                _order.add(oid);
            }
        }
        else
        {
            // The object in context is the same object attempted an insert on
            // Ensure we have the correct lock level
            lockManager.ensureLock(oid, _tx, lock, _pb);

            if (handler == null)
            {
                if (!swizzlingStrategy.isSameInstance(entry.userObject, userObject))
                {
                    // the new object contains data to deal with
                    if (entry.handler != null)
                    {
                        // materialize old object even if it is not
                        // materialized yet, because we need a place
                        // to copy the data from the new object
                        entry.userObject = entry.handler.getRealSubject();
                        entry.handler = null;
                    }
                    // swizzle after lockReachableObjects(), when all related objects
                    // will be in the editing context
                    lazySwizzle = true;
                }
            }
        }

        // perform automatic read lock for all reachable objects
        // if the inserted object is materialized
        if ((handler == null) && !stack.contains(userObject))
        {
            stack.push(userObject);
            lockReachableObjects(oid, userObject, entry.cacheObject, lock, stack, buildingObject);
            stack.pop();
            if (lazySwizzle)
            {
                entry.userObject = swizzlingStrategy.swizzle(userObject, entry.userObject, _pb, this);
            }
        }

        return entry;
    }

    /**
     * Lock all objects reachable via 1:N and N:1 relations,
     * @param lock The lock type to use
     */
    private void lockReachableObjects(Identity oid, Object userObject,
            Object cacheObject, int lock, Stack stack, boolean buildingObject)
            throws LockingException
    {
        ContextEntry entry;
        boolean onlyDependants = !_tx.getKit().isImplicitLockingUsed();
        ClassDescriptor mif = _pb.getClassDescriptor(userObject.getClass());

        // N:1 relations
        Iterator iter = mif.getObjectReferenceDescriptors().iterator();
        ObjectReferenceDescriptor rds = null;
        PersistentField f;
        Object relUserObj;
        Identity relOid;
        boolean isDependent;

        while (iter.hasNext())
        {
            rds = (ObjectReferenceDescriptor) iter.next();
            isDependent = rds.getOtmDependent();
            if (onlyDependants && !isDependent)
            {
                continue;
            }
            f = rds.getPersistentField();
            relUserObj = f.get(userObject);
            if (relUserObj != null)
            {
                relOid = new Identity(relUserObj, _pb);
                entry = (ContextEntry) _objects.get(relOid);
                if ((entry == null) || (entry.userObject != relUserObj))
                {
                    entry = insertInternal(relOid, relUserObj, lock, isDependent,
                                           oid, stack);
                    if (buildingObject && (entry != null))
                    {
                        f.set(userObject, entry.userObject);
                        f.set(cacheObject, entry.cacheObject);
                    }
                }
            }
        }

        // 1:N relations
        Iterator collections = mif.getCollectionDescriptors().iterator();
        CollectionDescriptor cds;
        Object userCol;
        Iterator userColIterator;
        Class type;
        ArrayList newUserCol = null;
        ArrayList newCacheCol = null;

        while (collections.hasNext())
        {
            cds = (CollectionDescriptor) collections.next();
            f = cds.getPersistentField();
            type = f.getType();
            isDependent = cds.getOtmDependent();
            if (onlyDependants && !isDependent)
            {
                continue;
            }
            userCol = f.get(userObject);
            if (userCol != null)
            {
                if ((userCol instanceof CollectionProxyDefaultImpl)
                        && !((CollectionProxyDefaultImpl) userCol).isLoaded())
                {
                    continue;
                }

                if (buildingObject)
                {
                    newUserCol = new ArrayList();
                    newCacheCol = new ArrayList();
                }

                if (Collection.class.isAssignableFrom(type))
                {
                    userColIterator = ((Collection) userCol).iterator();
                }
                else if (type.isArray())
                {
                    userColIterator = new ArrayIterator(userCol);
                }
                else
                {
                    throw new OJBRuntimeException(
                        userCol.getClass()
                            + " can not be managed by OJB OTM, use Array or Collection instead !");
                }

                while (userColIterator.hasNext())
                {
                    relUserObj = userColIterator.next();
                    relOid = new Identity(relUserObj, _pb);
                    entry = (ContextEntry) _objects.get(relOid);
                    if ((entry == null) || (entry.userObject != relUserObj))
                    {
                        entry = insertInternal(relOid, relUserObj, lock,
                                               isDependent, null, stack);
                    }
                    if (buildingObject && (entry != null))
                    {
                        newUserCol.add(entry.userObject);
                        newCacheCol.add(entry.cacheObject);
                    }
                }
                if (buildingObject)
                {
                    setCollectionField(userObject, f, newUserCol);
                    setCollectionField(cacheObject, f, newCacheCol);
                }
            }
        }
    }

    /**
     * @see org.apache.ojb.otm.EditingContext#remove(Identity)
     */
    public void remove(Identity oid)
    {
        _objects.remove(oid);
        _order.remove(oid);
        LockManager.getInstance().releaseLock(oid, _tx);
    }


    public void deletePersistent(Identity oid, Object userObject)
            throws LockingException
    {
        ContextEntry entry;

        entry = insertInternal(oid, userObject, LockType.WRITE_LOCK, true, null,
                               new Stack());
        if (entry != null)
        {
            entry.state = entry.state.deletePersistent();
        }
        _order.remove(oid);
        _order.add(oid);
    }

    /**
     * @see org.apache.ojb.otm.EditingContext#lookup(Identity)
     */
    public Object lookup(Identity oid)
    {
        ContextEntry entry = (ContextEntry) _objects.get(oid);
        return (entry == null ? null : entry.userObject);
    }

    public boolean contains(Identity oid)
    {
        return lookup(oid) != null;
    }

    /**
     * @see org.apache.ojb.otm.EditingContext#lookupState(Identity)
     */
    public State lookupState(Identity oid)
            throws LockingException
    {
        State retval = null;
        ContextEntry entry = (ContextEntry) _objects.get(oid);
        if (entry != null)
        {
            /**
             * possibly return a clone so we don't allow people to tweak states.
             */
            retval = entry.state;
        }
        return retval;
    }

    /**
     * @see org.apache.ojb.otm.EditingContext#setState(Identity, State)
     */
    public void setState(Identity oid, State state)
    {
        ContextEntry entry = (ContextEntry) _objects.get(oid);
        entry.state = state;
    }

    public Collection getAllObjectsInContext()
    {
        return _objects.values();
    }

    //////////////////////////////////////////
    // MaterializationListener interface
    //////////////////////////////////////////

    public void beforeMaterialization(IndirectionHandler handler, Identity oid)
    {
        //noop
    }

    public void afterMaterialization(IndirectionHandler handler, Object cacheObject)
    {
        Identity oid = handler.getIdentity();
        ContextEntry entry = (ContextEntry) _objects.get(oid);

        if (entry == null)
        {
            return;
        }

        int lock = LockManager.getInstance().getLockHeld(oid, _tx);
        ObjectCopyStrategy copyStrategy = _tx.getKit().getCopyStrategy(oid);
        Object userObject = copyStrategy.copy(cacheObject, _pb);
        handler.setRealSubject(userObject);
        _original.put(oid, getFields(userObject, false, true));

        // replace the proxy object with the real one
        entry.userObject = userObject;
        entry.cacheObject = cacheObject;
        entry.handler.removeListener(this);
        entry.handler = null;

        // perform automatic lock for all reachable objects
        // if the inserted object is materialized
        try
        {
            lockReachableObjects(oid, userObject, cacheObject, lock, new Stack(), true);
        }
        catch (LockingException ex)
        {
            throw new LockingPassthruException(ex);
        }
    }



    //////////////////////////////////////////
    // Other operations
    //////////////////////////////////////////

    /**
     *
     *  Commit this context into the persistent store.
     *  The EditingContext is not usable after a commit.
     *
     */
    public void commit() throws TransactionAbortedException
    {
        checkpointInternal(true);
        releaseLocksAndClear();
    }

    private void releaseLocksAndClear()
    {
        releaseLocks();
        removeMaterializationListener();
        _objects.clear();
        _order.clear();
        _original.clear();
        if (_checkpointed != _original)
        {
            _checkpointed.clear();
        }
    }

    /**
     *
     *  Writes all changes in this context into the persistent store.
     *
     */
    public void checkpoint() throws TransactionAbortedException
    {
        checkpointInternal(false);
        _checkpointed = new HashMap();
        for (Iterator iterator = _order.iterator(); iterator.hasNext();)
        {
            Identity oid = (Identity) iterator.next();
            ContextEntry entry = (ContextEntry) _objects.get(oid);
            if (entry.handler == null)
            {
                _checkpointed.put(oid, getFields(entry.userObject, false, true));
            }
        }
    }

    /**
     *
     *  Writes all changes in this context into the persistent store.
     *
     */
    private void checkpointInternal(boolean isCommit)
            throws TransactionAbortedException
    {
        if (_order.size() == 0)
        {
            return;
        }

        removeCollectionProxyListeners();

        ConnectionManagerIF connMan = _pb.serviceConnectionManager();
        boolean saveBatchMode = connMan.isBatchMode();
        Swizzling swizzlingStrategy = _tx.getKit().getSwizzlingStrategy();
        LockManager lockManager = LockManager.getInstance();
        Identity[] lockOrder = (Identity[]) _order.toArray(new Identity[_order.size()]);
        ObjectCache cache = _pb.serviceObjectCache();
        boolean isInsertVerified = _tx.getKit().isInsertVerified();
        ArrayList changedCollections = new ArrayList();

        // sort objects in the order of oid.hashCode to avoid deadlocks
        Arrays.sort(lockOrder, new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                return o1.hashCode() - o2.hashCode();
            }

            public boolean equals(Object obj)
            {
                return false;
            }
        });

        try {
            // mark dirty objects and lock them for write
            // also handle dependent objects and if there were inserted once,
            // repeat this process for their dependants ("cascade create")
            ArrayList newObjects = new ArrayList();
            int countNewObjects;
            do
            {
                newObjects.clear();
                countNewObjects = 0;
                for (int i = 0; i < lockOrder.length; i++)
                {
                    Identity oid = lockOrder[i];
                    ContextEntry entry = (ContextEntry) _objects.get(oid);
                    State state = entry.state;

                    if (entry.userObject == null) // invalidated
                    {
                        continue;
                    }

                    if (entry.handler == null) // materialized
                    {
                        if (!state.isDeleted())
                        {
                            Object[][] origFields = (Object[][]) _checkpointed.get(oid);
                            Object[][] newFields = getFields(entry.userObject, true, !isCommit);

                            if (origFields == null)
                            {
                                entry.needsCacheSwizzle = true;
                                newObjects.addAll(
                                        handleDependentReferences(oid, entry.userObject,
                                        null, newFields[0], newFields[2]));
                                newObjects.addAll(
                                        handleDependentCollections(oid, entry.userObject,
                                        null, newFields[1], newFields[3]));
                            }
                            else
                            {
                                if (isModified(origFields[0], newFields[0]))
                                {
                                    entry.state = state.markDirty();
                                    entry.needsCacheSwizzle = true;
                                    lockManager.ensureLock(oid, _tx, LockType.WRITE_LOCK, _pb);
                                    newObjects.addAll(
                                            handleDependentReferences(oid, entry.userObject,
                                            origFields[0], newFields[0], newFields[2]));
                                }

                                if (isModified(origFields[1], newFields[1]))
                                {
                                    // there are modified collections,
                                    // so we need to lock the object and to swizzle it to cache
                                    entry.needsCacheSwizzle = true;
                                    lockManager.ensureLock(oid, _tx, LockType.WRITE_LOCK, _pb);
                                    newObjects.addAll(
                                            handleDependentCollections(oid, entry.userObject,
                                            origFields[1], newFields[1], newFields[3]));
                                    changedCollections.add(oid);
                                }
                            }
                        }
                    }
                }
                countNewObjects = newObjects.size();
                if (countNewObjects > 0)
                {
                    // new objects are not locked, so we don't need to ensure the order
                    lockOrder = (Identity[]) newObjects.toArray(
                            new Identity[countNewObjects]);
                }
            }
            while (countNewObjects > 0);

            // Swizzle the context objects and the cache objects
            for (Iterator it = _order.iterator(); it.hasNext(); )
            {
                Identity oid = (Identity) it.next();
                ContextEntry entry = (ContextEntry) _objects.get(oid);

                if (entry.needsCacheSwizzle)
                {
                    entry.userObject = swizzlingStrategy.getRealTarget(entry.userObject);
                    entry.cacheObject = swizzlingStrategy.swizzle(
                    // we create the special ObjectCache implememntation
                            // that returns cacheObject, not userObject
                            entry.userObject, entry.cacheObject, _pb, new ObjectCache()
                            {
                                public Object lookup(Identity anOid)
                                {
                                    ContextEntry ent = (ContextEntry) _objects.get(anOid);
                                    return (ent == null ? null : ent.cacheObject);
                                }

                                public boolean contains(Identity oid)
                                {
                                    return lookup(oid) != null;
                                }

                                public void cache(Identity anOid, Object obj)
                                {
                                    // do nothing
                                }

                                public boolean cacheIfNew(Identity oid, Object obj)
                                {
                                    return false;
                                }

                                public void clear()
                                {
                                    // do nothing
                                }

                                public void remove(Identity anOid)
                                {
                                    // do nothing
                                }
                            });
                }
            }

            // Cascade delete for dependent objects
            int countCascadeDeleted;
            do
            {
                countCascadeDeleted = 0;
                // Use intermediate new ArrayList(_order) because _order
                // may be changed during cascade delete
                for (Iterator it = (new ArrayList(_order)).iterator(); it.hasNext(); )
                {
                    Identity oid = (Identity) it.next();
                    ContextEntry entry = (ContextEntry) _objects.get(oid);

                    if (entry.state.isDeleted())
                    {
                        countCascadeDeleted += doCascadeDelete(oid, entry.userObject);
                    }
                }
            }
            while (countCascadeDeleted > 0);

            // perform database operations
            connMan.setBatchMode(true);
            try
            {
                for (Iterator it = _order.iterator(); it.hasNext(); )
                {
                    Identity oid = (Identity) it.next();
                    ContextEntry entry = (ContextEntry) _objects.get(oid);
                    State state = entry.state;

                    if (!state.needsInsert() && !state.needsUpdate()
                            && !state.needsDelete())
                    {
                        if (changedCollections.contains(oid)) {
                            _pb.store(entry.cacheObject, state);
                        }
                        continue;
                    }

                    if (state.needsInsert())
                    {
                        if (isInsertVerified)
                        {
                            // PB verifies object existence by default
                            _pb.store(entry.cacheObject);
                        }
                        else
                        {
                            // PB migth already created the object by auto-update
                            if (cache.lookup(oid) == null) {
                                _pb.store(entry.cacheObject, state);
                            }
                        }

                    }
                    else if (state.needsUpdate())
                    {
                        _pb.store(entry.cacheObject, state);
                    }
                    else if (state.needsDelete())
                    {
                        _pb.delete(entry.cacheObject);
                    }
                    entry.state = state.commit();
                }
                connMan.executeBatch();
            }
            finally
            {
                connMan.setBatchMode(saveBatchMode);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw new TransactionAbortedException(ex);
        }
    }

    /**
     *
     *  Rollback all changes made during this transaction. The EditingContext is not usable after
     *  a rollback.
     *
     */
    public void rollback()
    {
        for (Iterator iterator = _order.iterator(); iterator.hasNext();)
        {
            Identity oid = (Identity) iterator.next();
            ContextEntry entry = (ContextEntry) _objects.get(oid);
            entry.state = entry.state.rollback();
            Object[][] origFields = (Object[][]) _original.get(oid);
            if (origFields != null)
            {
                setFields(entry.userObject, origFields);
                setFields(entry.cacheObject, origFields);
            }
        }
        releaseLocksAndClear();
    }

    /**
     *
     *  Rollback all changes made during this transaction to the given object.
     *
     */
    public void refresh(Identity oid, Object object)
    {
        ContextEntry entry = (ContextEntry) _objects.get(oid);
        Object[][] origFields = (Object[][]) _original.get(oid);
        if (origFields != null)
        {
            setFields(entry.userObject, origFields);
            if (object != entry.userObject)
            {
                setFields(object, origFields);
            }
        }
        entry.state = entry.state.refresh();
    }

    private void removeMaterializationListener()
    {
        for (Iterator it = _order.iterator(); it.hasNext();)
        {
            Identity oid = (Identity) it.next();
            ContextEntry entry = (ContextEntry) _objects.get(oid);
            if (entry.handler != null)
            {
                entry.handler.removeListener(this);
            }
        }
    }

    private void removeCollectionProxyListeners()
    {
        if (_colProxyListeners != null)
        {
            for (Iterator it = _colProxyListeners.keySet().iterator(); it.hasNext();)
            {
                CollectionProxyListener listener = (CollectionProxyListener) it.next();
                CollectionProxyDefaultImpl colProxy = (CollectionProxyDefaultImpl) _colProxyListeners.get(listener);
                colProxy.removeListener(listener);
            }
            _colProxyListeners.clear();
        }
    }

    private void releaseLocks()
    {
        LockManager lockManager = LockManager.getInstance();

        for (Iterator it = _objects.keySet().iterator(); it.hasNext(); )
        {
            Identity oid = (Identity) it.next();
            lockManager.releaseLock(oid, _tx);
        }
        _tx.getKit().getLockMap().gc();
    }

    /**
     * This method compared simple field values:
     * there are some tricks...
     */
    private boolean isEqual(Object fld1, Object fld2)
    {
        if (fld1 == null || fld2 == null)
        {
            return (fld1 == fld2);
        }
        else if ((fld1 instanceof BigDecimal) && (fld2 instanceof BigDecimal))
        {
            return (((BigDecimal) fld1).compareTo((BigDecimal) fld2) == 0);
        }
        else if ((fld1 instanceof Date) && (fld2 instanceof Date))
        {
            return (((Date) fld1).getTime() == ((Date) fld2).getTime());
        }
        else
        {
            return fld1.equals(fld2);
        }
    }

    private boolean isModified(Object[] newFields, Object[] oldFields)
    {
        if (newFields.length != oldFields.length)
        {
            return true;
        }

        for (int i = 0; i < newFields.length; i++)
        {
            if (!isEqual(newFields[i], oldFields[i]))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * @param addListeners Whether to add CollectionProxy listeners
     * @return four arrays of field values:
     * 1) The class, simple fields, identities of references
     * 2) collections of identities
     * 3) references (parallel to identities of references in 1)
     * 4) collections of objects (parallel to collections of identities in 2)
     * if "withObjects" parameter is "false", then returns nulls
     * in places of 3) and 4)
     */
    private Object[][] getFields(Object obj, boolean withObjects, boolean addListeners)
    {
        ClassDescriptor mif = _pb.getClassDescriptor(obj.getClass());
        FieldDescriptor[] fieldDescs = mif.getFieldDescriptions();
        Collection refDescs = mif.getObjectReferenceDescriptors();
        Collection colDescs = mif.getCollectionDescriptors();
        int count = 0;
        Object[] fields = new Object[1 + fieldDescs.length + refDescs.size()];
        ArrayList[] collections = new ArrayList[colDescs.size()];
        Object[] references = null;
        ArrayList[] collectionsOfObjects = null;
        int lockForListeners = LockType.NO_LOCK;

        if (withObjects)
        {
            references = new Object[refDescs.size()];
            collectionsOfObjects = new ArrayList[colDescs.size()];
        }

        if (addListeners)
        {
            lockForListeners = LockManager.getInstance().getLockHeld(
                    new Identity(obj, _pb), _tx);
        }

        fields[0] = obj.getClass(); // we must notice if the object class changes
        count++;

        for (int i = 0; i < fieldDescs.length; i++)
        {
            FieldDescriptor fd = fieldDescs[i];
            PersistentField f = fd.getPersistentField();
            fields[count] = f.get(obj);
            count++;
        }

        int countRefs = 0;
        for (Iterator it = refDescs.iterator(); it.hasNext(); count++, countRefs++)
        {
            ObjectReferenceDescriptor rds = (ObjectReferenceDescriptor) it.next();
            PersistentField f = rds.getPersistentField();
            Object relObj = f.get(obj);
            if (relObj != null)
            {
                fields[count] = new Identity(relObj, _pb);
                if (withObjects)
                {
                    references[countRefs] = relObj;
                }
            }
        }

        count = 0;
        for (Iterator it = colDescs.iterator(); it.hasNext(); count++)
        {
            CollectionDescriptor cds = (CollectionDescriptor) it.next();
            PersistentField f = cds.getPersistentField();
            Class type = f.getType();
            Object col = f.get(obj);

            if ((col != null) && (col instanceof CollectionProxyDefaultImpl)
                    && !((CollectionProxyDefaultImpl) col).isLoaded())
            {
                if (addListeners)
                {
                    OTMCollectionProxyListener listener =
                            new OTMCollectionProxyListener(cds, collections,
                                                           count, lockForListeners);

                    ((CollectionProxyDefaultImpl) col).addListener(listener);
                    if (_colProxyListeners == null)
                    {
                        _colProxyListeners = new HashMap();
                    }
                    _colProxyListeners.put(listener, col);
                }
                continue;
            }

            if (col != null)
            {
                ArrayList list = new ArrayList();
                ArrayList listOfObjects = null;
                Iterator colIterator;

                collections[count] = list;
                if (withObjects)
                {
                    listOfObjects = new ArrayList();
                    collectionsOfObjects[count] = listOfObjects;
                }

                if (Collection.class.isAssignableFrom(type))
                {
                    colIterator = ((Collection) col).iterator();
                }
                else if (type.isArray())
                {
                    colIterator = new ArrayIterator(col);
                }
                else
                {
                    continue;
                }

                while (colIterator.hasNext())
                {
                    Object relObj = colIterator.next();
                    list.add(new Identity(relObj, _pb));
                    if (withObjects)
                    {
                        listOfObjects.add(relObj);
                    }
                }
            }
        }

        return new Object[][] {fields, collections, references, collectionsOfObjects};
    }

    private void setFields(Object obj, Object[][] fieldsAndCollections)
    {
        ClassDescriptor mif = _pb.getClassDescriptor(obj.getClass());
        FieldDescriptor[] fieldDescs = mif.getFieldDescriptions();
        Collection refDescs = mif.getObjectReferenceDescriptors();
        Collection colDescs = mif.getCollectionDescriptors();
        Object[] fields = fieldsAndCollections[0];
        ArrayList[] collections = (ArrayList[]) fieldsAndCollections[1];
        int count = 0;

        if (!fields[0].equals(obj.getClass()))
        {
            System.err.println("Can't restore the object fields "
                    + "since its class changed during transaction from "
                    + fields[0] + " to " + obj.getClass());
            return;
        }
        count++;

        for (int i = 0; i < fieldDescs.length; i++)
        {
            FieldDescriptor fd = fieldDescs[i];
            PersistentField f = fd.getPersistentField();
            f.set(obj, fields[count]);
            count++;
        }

        for (Iterator it = refDescs.iterator(); it.hasNext(); count++)
        {
            ObjectReferenceDescriptor rds = (ObjectReferenceDescriptor) it.next();
            PersistentField f = rds.getPersistentField();
            Identity oid = (Identity) fields[count];
            Object relObj;
            if (oid == null)
            {
                relObj = null;
            }
            else
            {
                relObj = _pb.getObjectByIdentity(oid);
            }
            f.set(obj, relObj);
        }

        count = 0;
        for (Iterator it = colDescs.iterator(); it.hasNext(); count++)
        {
            CollectionDescriptor cds = (CollectionDescriptor) it.next();
            PersistentField f = cds.getPersistentField();
            ArrayList list = collections[count];
            ArrayList newCol;

            if (list == null)
            {
                f.set(obj, null);
            }
            else
            {
                newCol = new ArrayList();
                for (Iterator it2 = list.iterator(); it2.hasNext(); )
                {
                    Identity relOid = (Identity) it2.next();
                    Object relObj = _pb.getObjectByIdentity(relOid);

                    if (relObj != null)
                    {
                        newCol.add(relObj);
                    }
                }
                setCollectionField(obj, f, newCol);
            }
        }
    }

    private void setCollectionField(Object obj, PersistentField f, List newCol)
    {
        Class type = f.getType();

        if (Collection.class.isAssignableFrom(type))
        {
            Collection col = (Collection) f.get(obj);

            if (col == null)
            {
                if (type == List.class || type == Collection.class)
                {
                    col = new ArrayList();
                }
                else if (type == Set.class)
                {
                    col = new HashSet();
                }
                else
                {
                    try
                    {
                        col = (Collection) type.newInstance();
                    }
                    catch (Throwable ex)
                    {
                        System.err.println("Cannot instantiate collection field: " + f);
                        ex.printStackTrace();
                        return;
                    }
                }
            }
            else
            {
                if (col instanceof CollectionProxyDefaultImpl)
                {
                    CollectionProxyDefaultImpl cp = (CollectionProxyDefaultImpl) col;
                    if (col instanceof List)
                    {
                        col = new ListProxyDefaultImpl(_pb.getPBKey(), cp.getData().getClass(), null);
                    }
                    else if (col instanceof Set)
                    {
                        col = new SetProxyDefaultImpl(_pb.getPBKey(), cp.getData().getClass(), null);
                    }
                    else
                    {
                        col = new CollectionProxyDefaultImpl(_pb.getPBKey(), cp.getData().getClass(), null);
                    }
                    col.clear();
                }
                else
                {
                    try
                    {
                        col = (Collection) col.getClass().newInstance();
                    }
                    catch (Exception ex)
                    {
                        System.err.println("Cannot instantiate collection field: " + f);
                        ex.printStackTrace();
                        return;
                    }
                }
            }
            col.addAll(newCol);
            f.set(obj, col);
        }
        else if (type.isArray())
        {
            int length = newCol.size();
            Object array = Array.newInstance(type.getComponentType(), length);

            for (int i = 0; i < length; i++)
            {
                Array.set(array, i, newCol.get(i));
            }
            f.set(obj, array);
        }
    }

    /**
     * Does the given class has bidirectional assiciation
     * with some other class?
     */
    private boolean hasBidirectionalAssociation(Class clazz)
    {
        ClassDescriptor cdesc;
        Collection refs;
        boolean hasBidirAssc;

        if (_withoutBidirAssc.contains(clazz))
        {
            return false;
        }

        if (_withBidirAssc.contains(clazz))
        {
            return true;
        }

        // first time we meet this class, let's look at metadata
        cdesc = _pb.getClassDescriptor(clazz);
        refs = cdesc.getObjectReferenceDescriptors();
        hasBidirAssc = false;
        REFS_CYCLE:
        for (Iterator it = refs.iterator(); it.hasNext(); )
        {
            ObjectReferenceDescriptor ord;
            ClassDescriptor relCDesc;
            Collection relRefs;

            ord = (ObjectReferenceDescriptor) it.next();
            relCDesc = _pb.getClassDescriptor(ord.getItemClass());
            relRefs = relCDesc.getObjectReferenceDescriptors();
            for (Iterator relIt = relRefs.iterator(); relIt.hasNext(); )
            {
                ObjectReferenceDescriptor relOrd;

                relOrd = (ObjectReferenceDescriptor) relIt.next();
                if (relOrd.getItemClass().equals(clazz))
                {
                    hasBidirAssc = true;
                    break REFS_CYCLE;
                }
            }
        }
        if (hasBidirAssc)
        {
            _withBidirAssc.add(clazz);
        }
        else
        {
            _withoutBidirAssc.add(clazz);
        }

        return hasBidirAssc;
    }

    /**
     * @return number of deleted objects: 1 or 0 (if the object is already deleted)
     */
    private int markDelete(Identity oid, Identity mainOid, boolean isCollection)
    {
        ContextEntry entry = (ContextEntry) _objects.get(oid);

        if (entry == null)
        {
            throw new IllegalStateException("markDelete failed: the dependent object "
                    + oid + " is not in the editing context");
        }

        if (entry.state.isDeleted())
        {
            return 0;
        }
        else
        {
            entry.state = entry.state.deletePersistent();
            if (mainOid != null)
            {
                int dependentIndex = _order.indexOf(oid);
                int mainIndex = _order.indexOf(mainOid);

                if (isCollection) // remove collection item before main obj
                {
                    if (dependentIndex > mainIndex)
                    {
                        _order.remove(dependentIndex);
                        _order.add(mainIndex, oid);
                    }
                }
                else // remove reference after main obj
                {
                    if (dependentIndex < mainIndex)
                    {
                        _order.remove(dependentIndex); // this causes mainIndex--
                        _order.add(mainIndex, oid);
                    }
                }

            }
            return 1;
        }
    }

    /**
     * Mark for creation all newly introduced dependent references.
     * Mark for deletion all nullified dependent references.
     * @return the list of created objects
     */
    private ArrayList handleDependentReferences(Identity oid, Object userObject,
            Object[] origFields, Object[] newFields, Object[] newRefs)
            throws LockingException
    {
        ClassDescriptor mif = _pb.getClassDescriptor(userObject.getClass());
        FieldDescriptor[] fieldDescs = mif.getFieldDescriptions();
        Collection refDescs = mif.getObjectReferenceDescriptors();
        int count = 1 + fieldDescs.length;
        ArrayList newObjects = new ArrayList();
        int countRefs = 0;

        for (Iterator it = refDescs.iterator(); it.hasNext(); count++, countRefs++)
        {
            ObjectReferenceDescriptor rds = (ObjectReferenceDescriptor) it.next();
            Identity origOid = (origFields == null ? null : (Identity) origFields[count]);
            Identity newOid = (Identity) newFields[count];

            if (rds.getOtmDependent())
            {
                if ((origOid == null) && (newOid != null))
                {
                    ContextEntry entry = (ContextEntry) _objects.get(newOid);

                    if (entry == null)
                    {
                        Object relObj = newRefs[countRefs];
                        insertInternal(newOid, relObj, LockType.WRITE_LOCK,
                                       true, oid, new Stack());
                        newObjects.add(newOid);
                    }
                }
                else if ((origOid != null) &&
                         ((newOid == null) || !newOid.equals(origOid)))
                {
                    markDelete(origOid, oid, false);
                }
            }
        }

        return newObjects;
    }

    /**
     * Mark for creation all objects that were included into dependent collections.
     * Mark for deletion all objects that were excluded from dependent collections.
     */
    private ArrayList handleDependentCollections(Identity oid, Object obj,
            Object[] origCollections, Object[] newCollections,
            Object[] newCollectionsOfObjects)
            throws LockingException
    {
        ClassDescriptor mif = _pb.getClassDescriptor(obj.getClass());
        Collection colDescs = mif.getCollectionDescriptors();
        ArrayList newObjects = new ArrayList();
        int count = 0;

        for (Iterator it = colDescs.iterator(); it.hasNext(); count++)
        {
            CollectionDescriptor cds = (CollectionDescriptor) it.next();

            if (cds.getOtmDependent())
            {
                ArrayList origList = (origCollections == null ? null
                                        : (ArrayList) origCollections[count]);
                ArrayList newList = (ArrayList) newCollections[count];

                if (origList != null)
                {
                    for (Iterator it2 = origList.iterator(); it2.hasNext(); )
                    {
                        Identity origOid = (Identity) it2.next();

                        if ((newList == null) || !newList.contains(origOid))
                        {
                            markDelete(origOid, oid, true);
                        }
                    }
                }

                if (newList != null)
                {
                    int countElem = 0;
                    for (Iterator it2 = newList.iterator(); it2.hasNext(); countElem++)
                    {
                        Identity newOid = (Identity) it2.next();

                        if ((origList == null) || !origList.contains(newOid))
                        {
                            ContextEntry entry = (ContextEntry) _objects.get(newOid);

                            if (entry == null)
                            {
                                ArrayList relCol = (ArrayList)
                                        newCollectionsOfObjects[count];
                                Object relObj = relCol.get(countElem);
                                insertInternal(newOid, relObj, LockType.WRITE_LOCK,
                                               true, null, new Stack());
                                newObjects.add(newOid);
                            }
                        }
                    }
                }
            }
        }

        return newObjects;
    }

    /**
     * Mark for deletion all dependent objects (via references and collections).
     * @return the number of deleted objects
     */
    private int doCascadeDelete(Identity oid, Object obj)
    {
        ClassDescriptor mif = _pb.getClassDescriptor(ProxyHelper.getRealClass(obj));
        Collection refDescs = mif.getObjectReferenceDescriptors();
        Collection colDescs = mif.getCollectionDescriptors();
        int countCascadeDeleted = 0;

        for (Iterator it = refDescs.iterator(); it.hasNext(); )
        {
            ObjectReferenceDescriptor rds = (ObjectReferenceDescriptor) it.next();

            if (rds.getOtmDependent())
            {
                PersistentField f = rds.getPersistentField();
                Object relObj = f.get(obj);

                if (relObj != null)
                {
                    countCascadeDeleted +=
                            markDelete(new Identity(relObj, _pb), oid, false);
                }
            }
        }

        for (Iterator it = colDescs.iterator(); it.hasNext(); )
        {
            CollectionDescriptor cds = (CollectionDescriptor) it.next();

            if (cds.getOtmDependent())
            {
                PersistentField f = cds.getPersistentField();
                Class type = f.getType();
                Object col = f.get(obj);

                if (col != null)
                {
                    Iterator colIterator;

                    if (Collection.class.isAssignableFrom(type))
                    {
                        colIterator = ((Collection) col).iterator();
                    }
                    else if (type.isArray())
                    {
                        colIterator = new ArrayIterator(col);
                    }
                    else
                    {
                        continue;
                    }

                    while (colIterator.hasNext())
                    {

                        countCascadeDeleted +=
                                markDelete(new Identity(colIterator.next(), _pb), oid, true);
                    }
                }
            }
        }

        return countCascadeDeleted;
    }

    /*
     * The rest of ObjectCache implementation for swizling
     * All methods except lookup() are never used by swizzling,
     * remove() appeared to already exist in this class
     * with the same signature as in ObjectCache interface,
     * other methods are unsupported.
     */
    public void cache(Identity oid, Object obj)
    {
        throw new UnsupportedOperationException();
    }

    public boolean cacheIfNew(Identity oid, Object obj)
    {
        // not implemented
        throw new UnsupportedOperationException("Not implemented");
    }

    public void clear()
    {
        throw new UnsupportedOperationException();
    }


    //////////////////////////////////////////
    // Inner classes
    //////////////////////////////////////////

    private static class ContextEntry
    {
        Object userObject;
        Object cacheObject;
        State state = State.PERSISTENT_CLEAN;

        /**
         * Handler the proxy object, null if the object is real
         */
        IndirectionHandler handler;

        /**
         * This flag is used during commit/checkpoint
         */
        boolean needsCacheSwizzle;

        ContextEntry(Object theUserObject)
        {
            userObject = theUserObject;
            if (userObject != null)
            {
                handler = ProxyHelper.getIndirectionHandler(userObject);
                if ((handler != null) && handler.alreadyMaterialized())
                {
                    userObject = handler.getRealSubject();
                    handler = null;
                }
            }
        }
    }

    private class OTMCollectionProxyListener implements CollectionProxyListener
    {
        private final CollectionDescriptor _cds;
        private final ArrayList[] _collections;
        private final int _index;
        private final int _lock;

        OTMCollectionProxyListener(CollectionDescriptor cds,
                ArrayList[] collections, int index, int lock)
        {
            _cds = cds;
            _collections = collections;
            _index = index;
            _lock = lock;
        }

        public void beforeLoading(CollectionProxyDefaultImpl colProxy)
        {
            // do nothing
        }

        /**
         * The collection proxy contains PB cache objects. We have to replace it
         * with a collection of user objects.
         */
        public void afterLoading(CollectionProxyDefaultImpl colProxy)
        {
            ArrayList list = new ArrayList();
            ArrayList newUserCol = new ArrayList();
            LockManager lockManager = LockManager.getInstance();
            _collections[_index] = list;

            for (Iterator it = colProxy.iterator(); it.hasNext(); )
            {
                Object relUserObj;
                Object relCacheObj = it.next();
                Identity relOid = new Identity(relCacheObj, _pb);
                ContextEntry entry;

                list.add(relOid);
                entry = (ContextEntry) _objects.get(relOid);
                if (entry != null)
                {
                    relUserObj = entry.userObject;
                }
                else
                {
                    ObjectCopyStrategy copyStrategy;

                    copyStrategy = _tx.getKit().getCopyStrategy(relOid);
                    relUserObj = copyStrategy.copy(relCacheObj, _pb);
                    try
                    {
                        entry = insertInternal(relOid, relUserObj, _lock,
                                _cds.getOtmDependent(), null, new Stack());
                        if (entry != null)
                        {
                            relUserObj = entry.userObject;
                        }
                    }
                    catch (LockingException ex)
                    {
                        throw new LockingPassthruException(ex);
                    }
                }
                newUserCol.add(relUserObj);
            }
            colProxy.clear();
            colProxy.addAll(newUserCol);
        }
    }
}
