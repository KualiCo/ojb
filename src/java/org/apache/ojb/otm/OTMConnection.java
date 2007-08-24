package org.apache.ojb.otm;

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

import java.util.Collection;
import java.util.Iterator;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.cache.ObjectCache;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.otm.lock.LockingException;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.odmg.oql.EnhancedOQLQuery;
import org.odmg.OQLQuery;

/**
 *
 *  A OTMConnection within the given Environment
 *
 *  @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 */
public interface OTMConnection
{

    /**
     *
     * Make the given object persistent by inserting it into the database.
     * Also read locks the object (OTM will automatically lock
     * it for write on transaction commit if the object will appear
     * to be modified).
     *
     * @param object       the object to be made persistent
     *
     */
    public void makePersistent(Object object)
            throws LockingException;

    /**
     * Obtain the Transaction this connection is associated with
     */
    public Transaction getTransaction();

    /**
     * Associate this connection with a given transaction.
     */
    public void setTransaction(Transaction tx);

    /**
     *
     *  Mark the given object for deletion from the persistent store. The object would then become
     *  a transient object, rather than a persistent one.
     *
     *  @param obj      the object to delete
     *
     */
    public void deletePersistent(Object obj)
            throws LockingException;

    /**
     *
     *  Lock the given object for Write. Only write locked objects are persisted back to the
     *  database. Changes to read objects are not inserted back into the database.
     *
     *  @param object       the object to be locked for write.
     *
     */
    public void lockForWrite(Object object)
            throws LockingException;

    /**
     *
     *  Get the object with the given Identity from the persistent store. By default, the fetch is
     *  for read. (OTM will automatically lock it for write on transaction commit
     * if the object will appear to be modified).
     *
     *  @param oid                  the Identity of the object to fetch
     *  @return                     the object from the persistent store.
     *  @throws LockingException    thrown by the LockManager to avoid deadlocks. The fetch could be
     *                              re-submitted.
     *
     */
    public Object getObjectByIdentity(Identity oid)
            throws LockingException;

    /**
     *
     * Get the object with the given Identity from the persistent store with the given lock value.
     *
     * @param oid                   the Identity of the object to fetch
     * @param lock                  the lock that need to be acquired on the object
     * Possible values are:
     * LockType.NO_LOCK (aka read only) - changes to the object will not be written to database;
     * LockType.READ_LOCK (aka optimistic lock) - changes to the object will be written to the database,
     * in this case the lock will be automatically upgraded to the write lock on transaction commit;
     * LockType.WRITE_LOCK (aka pessimistic lock) - changes to the object will be written to the database.
     *
     * @return                     the object from the persistent store.
     * @throws LockingException     thrown by the LockManager to avoid a deadlock.
     *
     */
    public Object getObjectByIdentity(Identity oid, int lock)
            throws LockingException;

    /**
     * @param query The query to execute
     * @return an Iterator that iterates Objects of class c if calling the .next()
     * method. The returned objects are locked for read.
     */
    public Iterator getIteratorByQuery(Query query);

    /**
     * @param query The query to execute
     * @param lock the lock that need to be acquired on the object
     * Possible values are:
     * LockType.NO_LOCK (aka read only) - changes to the object will not be written to database;
     * LockType.READ_LOCK (aka optimistic lock) - changes to the object will be written to the database,
     * in this case the lock will be automatically upgraded to the write lock on transaction commit;
     * LockType.WRITE_LOCK (aka pessimistic lock) - changes to the object will be written to the database.
     * @return an Iterator that iterates Objects of class c if calling the .next()
     * method. The returned objects are locked with the given lock value.
     */
    public Iterator getIteratorByQuery(Query query, int lock);

    /**
     * @param query The OQL query to execute
     * @return an Iterator that iterates Objects of class c if calling the .next()
     * method. The returned objects are locked for read.
     */
    public Iterator getIteratorByOQLQuery(OQLQuery query);

    /**
     * @param query The OQL query to execute
     * @param lock the lock that need to be acquired on the object
     * Possible values are:
     * LockType.NO_LOCK (aka read only) - changes to the object will not be written to database;
     * LockType.READ_LOCK (aka optimistic lock) - changes to the object will be written to the database,
     * in this case the lock will be automatically upgraded to the write lock on transaction commit;
     * LockType.WRITE_LOCK (aka pessimistic lock) - changes to the object will be written to the database.
     * @return an Iterator that iterates Objects of class c if calling the .next()
     * method. The returned objects are locked for read.
     */
    public Iterator getIteratorByOQLQuery(OQLQuery query, int lock);

    /**
     * @param query The query to execute
     * @param lock the lock that need to be acquired on the object
     * Possible values are:
     * LockType.NO_LOCK (aka read only) - changes to the object will not be written to database;
     * LockType.READ_LOCK (aka optimistic lock) - changes to the object will be written to the database,
     * in this case the lock will be automatically upgraded to the write lock on transaction commit;
     * LockType.WRITE_LOCK (aka pessimistic lock) - changes to the object will be written to the database.
     * @return an Iterator that iterates Objects of class c if calling the .next()
     * method. The returned objects are locked with the given lock value.
     */
    public Collection getCollectionByQuery(Query query, int lock);

    /**
     * @param query The query to execute
     * @return an Iterator that iterates Objects of class c if calling the .next()
     * method. The returned objects are locked for read.
     */
    public Collection getCollectionByQuery(Query query);

    /**
     * Get the identity of the object
     * @param object The object
     * @return the identity of the object
     */
    public Identity getIdentity(Object object);

    public ClassDescriptor getDescriptorFor(Class clazz);

    /**
     *
     *  Get the EditingContext associated with the transaction to which this connection belongs.
     *  EditingContext contains and manages the set of objects read/edited within the current
     *  transaction.
     *
     *  @return                     EditingContext associated with current Transaction
     *
     */
    public EditingContext getEditingContext();

    /**
     * In the case if the program need to change the objects
     * via direct JDBC call, it should first call invalidate()
     * for the object, which will lock the object for write
     * and tell OJB OTM that it must be re-read from the database,
     * only after that you shold perform JDBC operation.
     * NOTE: it is not recommended to use read-uncommitted isolation
     * if you want this feature to work correctly.
     */
    public void invalidate(Identity oid)
            throws LockingException;

    /**
     * clear the underlying caches
     */
    public void invalidateAll()
            throws LockingException;

    /**
     * returns a new OQL Query. This OQL query is Enhanced, meaning it does
     * the ODMG functionality as well as some additional OJB specific, non
     * portable functionality.
     * @return the new OQLQuery
     */
    public EnhancedOQLQuery newOQLQuery();

    /**
     * returns a new OQL Query. This OQL query is Enhanced, meaning it does
     * the ODMG functionality as well as some additional OJB specific, non
     * portable functionality.
     * @param lock the lock that need to be acquired on the object
     * Possible values are:
     * LockType.NO_LOCK (aka read only) - changes to the object will not be written to database;
     * LockType.READ_LOCK (aka optimistic lock) - changes to the object will be written to the database,
     * in this case the lock will be automatically upgraded to the write lock on transaction commit;
     * LockType.WRITE_LOCK (aka pessimistic lock) - changes to the object will be written to the database.
     * @return the new OQLQuery
     */
    public EnhancedOQLQuery newOQLQuery(int lock);

    /**
     * return the number of objects that would be returned from this query
     * @param query
     * @return the number of objects that would be returned from this query
     */
    int getCount(Query query);

    /**
     * Close the OTMConnection
     */
    void close();

    /**
     * check if the OTMConnection is closed
     */

    boolean isClosed();

    /**
     * get the global cache
     * @return
     */
    ObjectCache serviceObjectCache();

    /**
     * Updates the values in the object from the data in data store.
     * The state of the object becomes "Persistent-clean".
     */
    void refresh(Object object);
}
