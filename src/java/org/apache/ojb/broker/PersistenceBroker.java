package org.apache.ojb.broker;

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

import org.apache.ojb.broker.accesslayer.ConnectionManagerIF;
import org.apache.ojb.broker.accesslayer.JdbcAccess;
import org.apache.ojb.broker.accesslayer.StatementManagerIF;
import org.apache.ojb.broker.accesslayer.sql.SqlGenerator;
import org.apache.ojb.broker.cache.ObjectCache;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.ObjectModification;
import org.apache.ojb.broker.util.configuration.Configurable;
import org.apache.ojb.broker.util.sequence.SequenceManager;
import org.odbms.ObjectContainer;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

/**
 *
 * PersistenceBroker declares a protocol for persisting arbitrary objects.
 * A typical implementation might wrap an RDBMS access layer.
 *
 * @see org.apache.ojb.broker.core.PersistenceBrokerImpl
 * @see org.apache.ojb.broker.core.PersistenceBrokerBean
 *
 * @author Thomas Mahler
 * @version $Id: PersistenceBroker.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public interface PersistenceBroker extends Configurable, ObjectContainer
{
    // *************************************************************************
    // Services handled by the PersistenceBroker
    // *************************************************************************

    /**
     * Returns the {@link org.apache.ojb.broker.accesslayer.StatementManagerIF} instance associated with this broker.
     * 
     * @return The statement manager
     */
    public StatementManagerIF serviceStatementManager();

    /**
     * Returns the {@link org.apache.ojb.broker.accesslayer.ConnectionManagerIF} instance associated with this broker.
     * 
     * @return The connection manager
     */
    public ConnectionManagerIF serviceConnectionManager();

    /**
     * Returns the {@link org.apache.ojb.broker.accesslayer.sql.SqlGenerator} instance associated with this broker.
     * 
     * @return The SQL generator
     */
    public SqlGenerator serviceSqlGenerator();

    /**
     * Returns the {@link org.apache.ojb.broker.accesslayer.JdbcAccess} instance associated with this broker.
     * 
     * @return The JDBC access object
     */
    public JdbcAccess serviceJdbcAccess();

    /**
     * Returns the {@link org.apache.ojb.broker.util.sequence.SequenceManager} instance associated with this broker.
     * 
     * @return The sequence manager
     */
    public SequenceManager serviceSequenceManager();

    /**
     * Returns the {@link org.apache.ojb.broker.util.BrokerHelper} instance associated with this broker, which
     * makes some additional helper methods available.
     * 
     * @return The broker helper object
     */
    public BrokerHelper serviceBrokerHelper();

    /**
     * Returns the {@link org.apache.ojb.broker.cache.ObjectCache} instance associated
     * with this broker.
     * 
     * @return The object cache
     */
    public ObjectCache serviceObjectCache();

    /**
     * Return the {@link IdentityFactory} instance associated with this broker.
     * 
     * @return The identity factory
     */
    public IdentityFactory serviceIdentity();


    // *************************************************************************
    // PersistenceBroker listener methods
    // *************************************************************************

    /**
     * Fires a broker event to inform all registered {@link PBListener} instances.
     * 
     * @param event The event to fire
     */
    public void fireBrokerEvent(PersistenceBrokerEvent event);

    /**
     * Fires a life cycle event to inform all registered {@link PBListener} instances.
     * 
     * @param event The event to fire
     */
    public void fireBrokerEvent(PBLifeCycleEvent event);

    /**
     * Fires a state event to inform all registered {@link PBListener} instances.
     * 
     * @param event The event to fire
     */
    public void fireBrokerEvent(PBStateEvent event);

    /**
     * Removes all temporary listeners from this broker.
     * Use with care, because some internals rely on this mechanism.
     * 
     * @see #removeListener(PBListener)
     */
    public void removeAllListeners() throws PersistenceBrokerException;

    /**
     * Removes all temporary and, if desired, permanent listeners from this broker.
     * Use with care, because some internals rely on this mechanism.
     * 
     * @param permanent Whether the listener will stay registered after closing
     *                  the broker
     * @see #removeListener(PBListener)
     */
    public void removeAllListeners(boolean permanent) throws PersistenceBrokerException;


    /**
     * Adds a temporary {@link org.apache.ojb.broker.PBListener} to this broker.
     * Note that temporary listeners will be removed upon closing a broker (returning
     * it to the pool).
     *
     * @param listener The listener to add
     * @see #addListener(PBListener, boolean)
     */
    public void addListener(PBListener listener) throws PersistenceBrokerException;

    /**
     * Adds a temporary or permanent {@link org.apache.ojb.broker.PBListener} to this broker,
     * depending on the parameter value. Note that temporary listeners will be removed upon
     * closing a broker (returning it to the pool).
     * <br/>
     * <b>NOTE:</b> Handle carefully when using this method, keep in mind you don't
     * know which broker instance will be returned next time from the pool! To guarantee that
     * a listener is connect to every broker, the best way is to define your own implementation of
     * {@link org.apache.ojb.broker.core.PersistenceBrokerFactoryIF} or extend the default
     * one, {@link org.apache.ojb.broker.core.PersistenceBrokerFactoryDefaultImpl}. There you
     * can add the listener at creation of the {@link org.apache.ojb.broker.PersistenceBroker}
     * instances.
     * 
     * @param listener  The listener to add
     * @param permanent Whether the listener will stay registered after closing
     *                  the broker
     */
    public void addListener(PBListener listener, boolean permanent) throws PersistenceBrokerException;

    /**
     * Removes the specified listener from this broker.
     * 
     * @param listener The listener to remove
     */
    public void removeListener(PBListener listener) throws PersistenceBrokerException;


    // *************************************************************************
    // Transaction and instance handling stuff
    // *************************************************************************

    /**
     * Aborts and closes the current transaction. This abandons all persistent object modifications
     * and releases the associated locks.
     * 
     * @throws TransactionNotInProgressException If no transaction is currently in progress
     */
    public void abortTransaction() throws TransactionNotInProgressException;

    /**
     * Begins a transaction against the underlying RDBMS.
     * 
     * @throws TransactionInProgressException If there is already a transaction in progress
     */
    public void beginTransaction() throws TransactionInProgressException, TransactionAbortedException;

    /**
     * Commits and closes the current transaction. This commits all database-changing statements (e.g.
     * UPDATE, INSERT and DELETE) issued within the transaction since the last commit to the database,
     * and releases any locks held by the transaction.
     * 
     * @throws TransactionNotInProgressException If there is no transaction currently in progress
     * @throws TransactionAbortedException       If the transaction cannot be committed
     */
    public void commitTransaction() throws TransactionNotInProgressException, TransactionAbortedException;

    /**
     * Determines whether there is currently a transaction in progress.
     * 
     * @return <code>true</code> if there is a transaction in progress
     */
    public boolean isInTransaction() throws PersistenceBrokerException;

    /**
     * Closes this broker so that no further requests may be made on it. Closing a broker might release
     * it to the pool of available brokers, or might be garbage collected, at the option of the implementation.
     *
     * @return <code>true</code> if the broker was successfully closed
     */
    public boolean close();

    /**
     * Determines whether this broker is closed.
     * 
     * @return <tt>true</tt> if this instance is closed
     */
    public boolean isClosed();



    // *************************************************************************
    // Metadata service methods
    // *************************************************************************

    /**
     * Returns the metadata descriptor repository associated with this broker.
     * 
     * @return The descriptor repository
     */
    public DescriptorRepository getDescriptorRepository();

    /**
     * Get the {@link PBKey} for this broker.
     * 
     * @return The broker key
     */
    public PBKey getPBKey();

    /**
     * Returns the class descriptor for the given persistence capable class.
     * 
     * @param clazz The target class
     * @return The class descriptor
     * @throws PersistenceBrokerException If the class is not persistence capable, i.e.
     *         if no metadata was defined for this class and hence its class descriptor
     *         was not found
     */
    public ClassDescriptor getClassDescriptor(Class clazz) throws PersistenceBrokerException;

    /**
     * Determines whether the given class is persistence capable and thus has an associated
     * class descriptor in the metadata.
     * 
     * @param clazz The target class
     * @return <code>true</code> if a class descriptor was found
     */
    public boolean hasClassDescriptor(Class clazz);

    /**
     * Returns the top level class (most abstract class in terms of extents) from which the
     * given class extends. This may be a (abstract) base-class, an interface or the given
     * class itself, if no extent is defined.
     *
     * @param clazz The class to get the top level class for
     * @return The top level class for it
     * @throws PersistenceBrokerException If the class is not persistence capable,
     *         if no metadata was defined for this class
     */
    public Class getTopLevelClass(Class clazz) throws PersistenceBrokerException;

    // *************************************************************************
    // Object lifecycle
    // *************************************************************************

    /**
     * Clears the broker's internal cache.
     */
    public void clearCache() throws PersistenceBrokerException;

    /**
     * Removes the given object or, if it is an instance of {@link org.apache.ojb.broker.Identity},
     * the object identified by it, from the broker's internal cache. Note that the removal is
     * not recursive. This means, objects referenced by the removed object will not be
     * automatically removed from the cache by this operation.
     * 
     * @param objectOrIdentity The object to be removed from the cache or its identity 
     */
    public void removeFromCache(Object objectOrIdentity) throws PersistenceBrokerException;

    /**
     * Makes the given object persistent in the underlying persistence system.
     * This is usually done by issuing an INSERT ... or UPDATE ...  in an RDBMS.
     *
     * @param obj The object to store
     * @param modification Specifies what operation to perform (for generating optimized SQL)
     */
    public void store(Object obj,
                      ObjectModification modification) throws PersistenceBrokerException;

    /**
     * Make the given object persistent in the underlying persistence system.
     * This is usually done by issuing an INSERT ... or UPDATE ...  in an RDBMS.
     * 
     * @param obj The object to store
     */
    public void store(Object obj) throws PersistenceBrokerException;

    /**
     * Deletes the given object's persistent representation in the underlying persistence system.
     * This is usually done by issuing a DELETE ... in an RDBMS
     * 
     * @param obj The object to delete
     */
    public void delete(Object obj) throws PersistenceBrokerException;

    /**
     * Deletes an m:n implementor which defines the relationship between two persistent objects.
     * This is usually a row in an indirection table.<br/>
     * Note that OJB currently doesn't handle collection inheritance, so collections descriptors
     * are written per class. We try to match one of these collection descriptors, iterating from the left side
     * and looking for possible for classes on the right side using isAssignableFrom(rightClass).
     *
     * TODO: handle cache problems
     * TODO: delete more than one row if possible
     * 
     * @param m2nImpl The m:n implementor to delete
     */
    public void deleteMtoNImplementor(MtoNImplementor m2nImpl) throws PersistenceBrokerException;

    /**
     * Stores the given m:n implementor int the underlying persistence system. 
     * This is usually done by inserting a row in an indirection table.<br/>
     * Note that OJB currently doesn't handle collection inheritance, so collections descriptors
     * are written per class. We try to match one of these collection descriptors, iterating from the left side
     * and looking for possible for classes on the right side using isAssignableFrom(rightClass).
     * 
     * @param m2nImpl The m:n implementor to delete
     */
    public void addMtoNImplementor(MtoNImplementor m2nImpl) throws PersistenceBrokerException;

    /**
     * Deletes all objects matching the given query, from the underlying persistence system.
     * This is usually done via DELETE ... in an RDBMS.<br/>
     * <b>Note:</b> This method directly perform the delete statement ignoring any object
     * references and does not synchronize the cache - take care!
     * 
     * @param query The query determining the objects to delete
     */
    public void deleteByQuery(Query query) throws PersistenceBrokerException;

    // *************************************************************************
    // Query methods
    // *************************************************************************

    /**
     * Retrieve all references and collections of the given object irrespective of the
     * metadata settings defined for them.
     *  
     * @param obj The persistent object
     */
    public void retrieveAllReferences(Object obj) throws PersistenceBrokerException;

    /**
     * Retrieve the specified reference or collection attribute for the given persistent object.
     * 
     * @param obj      The persistent object
     * @param attrName The name of the attribute to retrieve
     */
    public void retrieveReference(Object obj, String attrName) throws PersistenceBrokerException;

    /**
     * Returns the number of elements that the given query will return.
     * 
     * @param query The query
     * @return The number of elements returned by the query
     */
    public int getCount(Query query) throws PersistenceBrokerException;

    /**
     * Retrieves the persistent objects matching the given query. Note that if the Query has
     * no criteria ALL persistent objects of the class targeted by the query will be returned.
     * 
     * @param query The query
     * @return The persistent objects matching the query
     */
    public Collection getCollectionByQuery(Query query) throws PersistenceBrokerException;

    /**
     * Retrieves the persistent objects matching the given query. The resulting collection will
     * be of the supplied collection type. Note that if the Query has no criteria ALL persistent
     * objects of the class targeted by the query will be returned.
     * 
     * @param collectionClass The collection type which needs to implement
     *                        {@link ManageableCollection}
     * @param query           The query
     * @return The persistent objects matching the query
     */
    public ManageableCollection getCollectionByQuery(Class collectionClass, Query query)
            throws PersistenceBrokerException;

    /**
     * Retrieves the persistent objects matching the given query and returns them as an iterator
     * which may, depending on the configured collection type, be reloading the objects from
     * the database upon calling {@link Iterator#next()}. Note that if the Query has no criteria
     * ALL persistent objects of the class targeted by the query will be returned.
     * 
     * @param query The query
     * @return The persistent objects matching the query
     */
    public Iterator getIteratorByQuery(Query query) throws PersistenceBrokerException;

    /**
     * Retrieves the rows (as <code>Object[]</code> instances) matching the given query and
     * returns them as an iterator which may, depending on the configured collection type, be reloading
     * the objects from the database upon calling {@link Iterator#next()}.
     * 
     * @param query The report query
     * @return The rows matching the query
     */
    public Iterator getReportQueryIteratorByQuery(Query query) throws PersistenceBrokerException;

    /**
     * Retrieve a persistent object from the underlying datastore by its identity. However, users
     * are encouraged to use {@link #getObjectByQuery(Query)} instead, as this method is mainly
     * intended to be used for internal handling of materialization by OID (e.g. in Proxies).
     * 
     * @param id The persistent object's id
     * @return The persistent object
     */
    public Object getObjectByIdentity(Identity id) throws PersistenceBrokerException;

    /**
     * Retrieve the (first) persistent object from the underlying datastore that matches the given
     * query.
     * 
     * @param query The query
     * @return The persistent object
     */
    public Object getObjectByQuery(Query query) throws PersistenceBrokerException;

    /**
     * Returns an enumeration of objects representing the primary keys for the objects that match
     * the given query. Mainly useful for EJB Finder Methods.<br/>
     * <b>Note:</b> This method is not yet aware of extents!
     * 
     * @param pkClass The class to use for the primary keys
     * @param query   The query
     * @return The pk enumeration
     */
    public Enumeration getPKEnumerationByQuery(Class pkClass, Query query)
            throws PersistenceBrokerException;
}
