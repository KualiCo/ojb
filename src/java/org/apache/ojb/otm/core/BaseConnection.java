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

import org.apache.ojb.broker.*;
import org.apache.ojb.broker.cache.ObjectCache;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.accesslayer.OJBIterator;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.ReportQuery;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.configuration.Configurator;
import org.apache.ojb.odmg.oql.EnhancedOQLQuery;
import org.apache.ojb.odmg.oql.OQLQueryImpl;
import org.apache.ojb.otm.EditingContext;
import org.apache.ojb.otm.OTMConnection;
import org.apache.ojb.otm.copy.ObjectCopyStrategy;
import org.apache.ojb.otm.lock.LockType;
import org.apache.ojb.otm.lock.LockingException;
import org.odmg.ODMGRuntimeException;
import org.odmg.OQLQuery;

import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.ArrayList;

/**
 * 
 * <javadoc>
 * 
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird </a>
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah </a>
 * @version $Id: BaseConnection.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 *  
 */
public abstract class BaseConnection implements OTMConnection
{

    private PersistenceBroker _pb;
    private Transaction _tx;
    private ConcreteEditingContext _editingContext;
    private Configurator m_configurator;

    /**
     * Constructor for BaseConnection.
     *  
     */
    public BaseConnection(PBKey pbKey)
    {
        _pb = PersistenceBrokerFactory.createPersistenceBroker(pbKey);
        m_configurator = PersistenceBrokerFactory.getConfigurator();
    }

    public void close()
    {
        _pb.close();
        _pb = null;
    }

    public boolean isClosed()
    {
        if (_pb == null)
            return true;
        else
            return _pb.isClosed();
    }

    public PersistenceBroker getKernelBroker()
    {
        return _pb;
    }

    public void setTransaction(Transaction transaction)
    {
        if (transaction == null)
        {
            _editingContext = null;
        }
        else if (_tx != null)
        {
            throw new IllegalStateException("OTMConnection is already bound to the transacaction "
                    + _tx);
        }
        else
        {
            _editingContext = new ConcreteEditingContext(transaction, _pb);
        }
        _tx = transaction;
    }

    public Transaction getTransaction()
    {
        return _tx;
    }

    //////////////////////////////////////
    // OTMConnection protocol
    //////////////////////////////////////

    /**
     * @see org.apache.ojb.otm.OTMConnection#getObjectByIdentity(Identity, int)
     */
    public Object getObjectByIdentity(Identity oid, int lock) throws LockingException
    {
        checkTransaction("getObjectByIdentity");
        Object userObject;
        Object cacheObject;

        cacheObject = _pb.getObjectByIdentity(oid);
        if (cacheObject == null)
        {
            // Possibly the object was inserted in this transaction
            // and was not stored to database yet
            userObject = _editingContext.lookup(oid);
        }
        else
        {
            userObject = getUserObject(oid, cacheObject);
            // userObject from editing context may be proxy
            userObject = ProxyHelper.getRealObject(userObject);
            _editingContext.insert(oid, userObject, lock);
        }
        return userObject;
    }

    private void checkTransaction(String methodBeingCalled)
    {
        if (null == _tx)
        {
            throw new TransactionNotInProgressException(
                    methodBeingCalled
                            + " requires a valid transaction. Please make sure you have created a new transaction, and called begin() on it.");
        }
        if (!_tx.isInProgress())
        {
            throw new TransactionNotInProgressException(methodBeingCalled
                    + " cannot be called before transaction begin() is called");
        }
    }

    /**
     * @see org.apache.ojb.otm.OTMConnection#getObjectByIdentity(Identity)
     */
    public Object getObjectByIdentity(Identity oid) throws LockingException
    {
        return getObjectByIdentity(oid, LockType.READ_LOCK);
    }

    /**
     * @param query The query to execute
     * @return an Iterator that iterates Objects. The returned objects are locked for read.
     */
    public Iterator getIteratorByQuery(Query query)
    {
        return getIteratorByQuery(query, LockType.READ_LOCK);
    }

    /**
     * @param query The query to execute
     * @param lock the lock that need to be acquired on the object Possible values are:
     *            LockType.NO_LOCK (aka read only) - changes to the object will not be written to
     *            database; LockType.READ_LOCK (aka optimistic lock) - changes to the object will
     *            be written to the database, in this case the lock will be automatically upgraded
     *            to the write lock on transaction commit; LockType.WRITE_LOCK (aka pessimistic
     *            lock) - changes to the object will be written to the database.
     * @return an Iterator that iterates Objects of class c if calling the .next() method. The
     *         returned objects are locked with the given lock value.
     */
    public Iterator getIteratorByQuery(Query query, int lock)
    {
        checkTransaction("getIteratorByQuery");
        return new OTMIterator((OJBIterator) _pb.getIteratorByQuery(query), lock, null);
    }

    /**
     * @param query The OQL query to execute. Use this method if you don't want to load all the
     *            collection at once as OQLQuery.execute() does.
     * @return an Iterator that iterates Objects. The returned objects are locked for read.
     */
    public Iterator getIteratorByOQLQuery(OQLQuery query)
    {
        return getIteratorByOQLQuery(query, LockType.READ_LOCK);
    }

    /**
     * @param query The OQL query to execute. Use this method if you don't want to load all the
     *            collection at once as OQLQuery.execute() does.
     * @return an Iterator that iterates Objects. The returned objects are locked for read.
     */
    public Iterator getIteratorByOQLQuery(OQLQuery query, int lock)
    {
        checkTransaction("getIteratorByOQLQuery");
        if (query instanceof OTMOQLQueryImpl)
        {
            OTMOQLQueryImpl q = (OTMOQLQueryImpl) query;
            return new OTMIterator((OJBIterator) _pb.getIteratorByQuery(q.getQuery()), lock, q);
        }
        else
        {
            throw new IllegalArgumentException("The OQLQuery where created not via OTM API");
        }
    }

    /**
     * @param query The query to execute
     * @param lock the lock that need to be acquired on the object Possible values are:
     *            LockType.NO_LOCK (aka read only) - changes to the object will not be written to
     *            database; LockType.READ_LOCK (aka optimistic lock) - changes to the object will
     *            be written to the database, in this case the lock will be automatically upgraded
     *            to the write lock on transaction commit; LockType.WRITE_LOCK (aka pessimistic
     *            lock) - changes to the object will be written to the database.
     * @return an Iterator that iterates Objects of class c if calling the .next() method. The
     *         returned objects are locked with the given lock value.
     */
    public Collection getCollectionByQuery(Query query, int lock)
    {
        checkTransaction("getCollectionByQuery");
        Collection col = _pb.getCollectionByQuery(query);
        Collection result = createCollectionOfTheSameClass(col);
        for (Iterator it = col.iterator(); it.hasNext();)
        {
            result.add(insertObject(it.next(), lock));
        }
        return result;
    }

    /**
     * @param query The query to execute
     * @return an Iterator that iterates Objects of class c if calling the .next() method. The
     *         returned objects are locked for read.
     */
    public Collection getCollectionByQuery(Query query)
    {
        return getCollectionByQuery(query, LockType.READ_LOCK);
    }

    /**
     * Get the identity of the object
     *
     * @param object The object
     * @return the identity of the object
     */
    public Identity getIdentity(Object object)
    {
        return new Identity(object, _pb);
    }

    /**
     * Get the class descriptor
     *
     * @param clazz The class
     * @return the descriptor of the class
     */
    public ClassDescriptor getDescriptorFor(Class clazz)
    {
        return _pb.getClassDescriptor(clazz);
    }

    /**
     * @see org.apache.ojb.otm.OTMConnection#invalidate(Identity)
     */
    public void invalidate(Identity oid) throws LockingException
    {
        if (null == _tx)
        {
            throw new TransactionNotInProgressException(
                    "invalidate requires a valid transaction. Please make sure you have created a new transaction, and called begin() on it.");
        }
        // mark as invalidated in the editing context, if it's found there
        _editingContext.insert(oid, null, LockType.READ_LOCK);

        // remove from the cache
        _pb.serviceObjectCache().remove(oid);

    }

    /**
     * @see org.apache.ojb.otm.OTMConnection#serviceObjectCache()
     */
    public ObjectCache serviceObjectCache()
    {
        return _pb.serviceObjectCache();
    }

    /**
     * TODO remove all from editing context.
     *
     * @throws LockingException
     */
    public void invalidateAll() throws LockingException
    {
        _pb.serviceObjectCache().clear();
    }

    /**
     * @see org.apache.ojb.otm.OTMConnection#lockForWrite(Object)
     */
    public void lockForWrite(Object object) throws LockingException
    {
        checkTransaction("lockForWrite");
        makePersistent(object);
    }

    /**
     * @see org.apache.ojb.otm.OTMConnection#makePersistent(Object)
     */
    public void makePersistent(Object userObject) throws LockingException
    {
        checkTransaction("makePersistent");
        Identity oid = new Identity(userObject, _pb);
        Object cacheObject = _pb.getObjectByIdentity(oid);

        if ((cacheObject != null) && (_editingContext.lookup(oid) == null))
        {
            // The object exists in the database, but is not yet in the editing
            // context, so we need to put it to the editing context in its
            // old state, then we will put the modified userObject.
            // This will allow the editing context to find changes
            ObjectCopyStrategy copyStrategy = _tx.getKit().getCopyStrategy(oid);
            Object origUserObject = copyStrategy.copy(cacheObject, _pb);
            _editingContext.insert(oid, origUserObject, LockType.WRITE_LOCK);
        }
        _editingContext.insert(oid, userObject, LockType.WRITE_LOCK);
    }

    /**
     * @see org.apache.ojb.otm.OTMConnection#deletePersistent(Object)
     */
    public void deletePersistent(Object userObject) throws LockingException
    {
        checkTransaction("deletePersistent");
        Identity oid = new Identity(userObject, _pb);
        Object cacheObject = _pb.getObjectByIdentity(oid);
        if (cacheObject == null)
        {
            // Possibly the object was inserted in this transaction
            // and was not stored to database yet, so we simply remove it
            // from editing context.
            _editingContext.remove(oid);
        }
        else
        {
            if (_editingContext.lookup(oid) == null)
            {
                // The object exists in the database, but is not yet in the editing
                // context, so we need to put it to the editing context
                ObjectCopyStrategy copyStrategy = _tx.getKit().getCopyStrategy(oid);
                Object origUserObject = copyStrategy.copy(cacheObject, _pb);
                _editingContext.insert(oid, origUserObject, LockType.WRITE_LOCK);
            }
            _editingContext.deletePersistent(oid, userObject);
        }
    }

    /**
     * @see org.apache.ojb.otm.OTMConnection#refresh(Object)
     */
    public void refresh(Object userObject)
    {
        checkTransaction("refresh");
        Identity oid = new Identity(userObject, _pb);
        _editingContext.refresh(oid, userObject);
    }

    public EditingContext getEditingContext()
    {
        return _editingContext;
    }

    public EnhancedOQLQuery newOQLQuery()
    {
        return newOQLQuery(LockType.READ_LOCK);
    }

    public EnhancedOQLQuery newOQLQuery(int lock)
    {
        checkTransaction("newOQLQuery");
        OQLQueryImpl query = new OTMOQLQueryImpl(_pb.getPBKey(), lock);
        try
        {
            m_configurator.configure(query);
        }
        catch (ConfigurationException e)
        {
            throw new ODMGRuntimeException("Error in configuration of OQLQueryImpl instance: "
                    + e.getMessage());
        }
        return query;
    }

    public int getCount(Query query)
    {
        checkTransaction("getCount");
        return _pb.getCount(query);
    }

    private Object insertObject(Object cacheObject, int lock)
    {
        Object ctxObject;
        Identity oid;
        Object userObject;


        oid = getIdentity(cacheObject);
        userObject = getUserObject(oid, cacheObject);
        try
        {
            _editingContext.insert(oid, userObject, lock);
        }
        catch (LockingException ex)
        {
            throw new LockingPassthruException(ex);
        }

        return userObject;
    }

    /**
     * Get user object (from the editing context) with the given oid.
     * If not found, then create it as a copy of cacheObject.
     * User object and cache object must be separate.
     * @param oid The identity
     * @param cacheObject the object for user
     */
    private Object getUserObject(Identity oid, Object cacheObject)
    {
        Object userObject = _editingContext.lookup(oid);

        if (userObject == null)
        {
            ObjectCopyStrategy copyStrategy = _tx.getKit().getCopyStrategy(oid);
            userObject = copyStrategy.copy(cacheObject, _pb);
        }
        return userObject;
    }

    private Collection createCollectionOfTheSameClass(Collection col)
    {
        try
        {
            return (Collection) col.getClass().newInstance();
        }
        catch (Throwable ex)
        {
            return new ArrayList();
        }
    }

    ///////////////////////////////////////
    // Transaction Notifications
    ///////////////////////////////////////

    /**
     *
     * Notification issued by the driving transaction to begin this transaction
     *
     */
    public abstract void transactionBegin() throws TransactionException;

    /**
     *
     * Prepare for a commit. As part of a two phase commit protocol of the transaction.
     *
     */
    public abstract void transactionPrepare() throws TransactionException;

    /**
     *
     * Notification issued by the driving transaction to commit resources held by this connection.
     *
     */
    public abstract void transactionCommit() throws TransactionException;

    /**
     * 
     * Notification issued by the driving transaction to rollback resources held by this
     * connection.
     *  
     */
    public abstract void transactionRollback() throws TransactionException;

    ///////////////////////////////////////
    // Inner classes
    ///////////////////////////////////////

    private class OTMIterator implements OJBIterator
    {
        private final OJBIterator _it;
        private final int _lock;
        private final OTMOQLQueryImpl _oqlQuery;

        OTMIterator(OJBIterator it, int lock, OTMOQLQueryImpl oqlQuery)
        {
            _it = it;
            _lock = lock;
            _oqlQuery = oqlQuery;
        }

        public boolean hasNext()
        {
            boolean res = _it.hasNext();

            // once the result set is finished, close it
            if (!res)
            {
                done();
            }

            return res;
        }

        public Object next()
        {
            Object object = _it.next();
            object = insertObject(object, _lock);
            return object;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        public void done()
        {
            releaseDbResources();
            if (_oqlQuery != null)
            {
                _oqlQuery.resetBindIterator();
            }
        }

        protected void finalize()
        {
            done();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.ojb.broker.accesslayer.OJBIterator#absolute(int)
         */
        public boolean absolute(int row) throws PersistenceBrokerException
        {
            return _it.absolute(row);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.ojb.broker.accesslayer.OJBIterator#fullSize()
         */
        public int fullSize() throws PersistenceBrokerException
        {
            return _it.fullSize();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.ojb.broker.accesslayer.OJBIterator#relative(int)
         */
        public boolean relative(int row) throws PersistenceBrokerException
        {
            return _it.relative(row);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.ojb.broker.accesslayer.OJBIterator#releaseDbResources()
         */
        public void releaseDbResources()
        {
            _it.releaseDbResources();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.ojb.broker.accesslayer.OJBIterator#size()
         */
        public int size() throws PersistenceBrokerException
        {
            return _it.size();
        }

        /**
         * @see org.apache.ojb.broker.accesslayer.OJBIterator#disableLifeCycleEvents()
         */
        public void disableLifeCycleEvents()
        {
            _it.disableLifeCycleEvents();
        }
    }

    private class OTMOQLQueryImpl extends OQLQueryImpl
    {
        int _lock;

        public OTMOQLQueryImpl(PBKey key, int lock)
        {
            super(key);
            _lock = lock;
        }

        /**
         * Execute the query. After executing a query, the parameter list is reset.
         *
         * @return The object that represents the result of the query. The returned data, whatever
         *         its OQL type, is encapsulated into an object. For instance, when OQL returns an
         *         integer, the result is put into an <code>Integer</code> object. When OQL
         *         returns a collection (literal or object), the result is always a Java collection
         *         object of the same kind (for instance, a <code>DList</code>).
         * @exception org.odmg.QueryException An exception has occurred while executing the query.
         */
        public Object execute() throws org.odmg.QueryException
        {
            Collection result;
            Iterator iter = null;
            Query query = getQuery();

            try
            {
                if (!(query instanceof ReportQuery))
                {
                    Collection res0 = _pb.getCollectionByQuery(query);
                    result = createCollectionOfTheSameClass(res0);
                    for (iter = res0.iterator(); iter.hasNext();)
                    {
                        result.add(insertObject(iter.next(), _lock));
                    }
                }
                else
                {
                    result = new ArrayList();
                    iter = _pb.getReportQueryIteratorByQuery(query);
                    while (iter.hasNext())
                    {
                        Object[] res = (Object[]) iter.next();

                        if (res.length == 1)
                        {
                            if (res[0] != null) // skip null values
                            {
                                result.add(res[0]);
                            }
                        }
                        else
                        {
                            // skip null tuples
                            for (int i = 0; i < res.length; i++)
                            {
                                if (res[i] != null)
                                {
                                    result.add(res);
                                    break;
                                }
                            }
                        }
                    }
                }
                resetBindIterator();
            }
            finally
            {
                if ((iter != null) && (iter instanceof OJBIterator))
                {
                    ((OJBIterator) iter).releaseDbResources();
                }
            }
            return result;
        }

        void resetBindIterator()
        {
            // reset iterator to start of list so we can reuse this query
            ListIterator it = getBindIterator();
            while (it.hasPrevious())
            {
                it.previous();
            }
        }
    }

}
