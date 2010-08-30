package org.apache.ojb.broker.core;

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

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.IdentityFactory;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.MtoNImplementor;
import org.apache.ojb.broker.OptimisticLockException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PBState;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.TransactionAbortedException;
import org.apache.ojb.broker.TransactionInProgressException;
import org.apache.ojb.broker.TransactionNotInProgressException;
import org.apache.ojb.broker.accesslayer.ChainingIterator;
import org.apache.ojb.broker.accesslayer.ConnectionManagerFactory;
import org.apache.ojb.broker.accesslayer.ConnectionManagerIF;
import org.apache.ojb.broker.accesslayer.JdbcAccess;
import org.apache.ojb.broker.accesslayer.JdbcAccessFactory;
import org.apache.ojb.broker.accesslayer.OJBIterator;
import org.apache.ojb.broker.accesslayer.PagingIterator;
import org.apache.ojb.broker.accesslayer.PkEnumeration;
import org.apache.ojb.broker.accesslayer.RelationshipPrefetcherFactory;
import org.apache.ojb.broker.accesslayer.StatementManagerFactory;
import org.apache.ojb.broker.accesslayer.StatementManagerIF;
import org.apache.ojb.broker.accesslayer.sql.SqlGenerator;
import org.apache.ojb.broker.accesslayer.sql.SqlGeneratorFactory;
import org.apache.ojb.broker.cache.MaterializationCache;
import org.apache.ojb.broker.cache.ObjectCache;
import org.apache.ojb.broker.cache.ObjectCacheFactory;
import org.apache.ojb.broker.cache.ObjectCacheInternal;
import org.apache.ojb.broker.core.proxy.AbstractProxyFactory;
import org.apache.ojb.broker.core.proxy.CollectionProxy;
import org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.ProxyFactory;
import org.apache.ojb.broker.core.proxy.VirtualProxy;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ClassNotPersistenceCapableException;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByIdentity;
import org.apache.ojb.broker.query.QueryBySQL;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.IdentityArrayList;
import org.apache.ojb.broker.util.ObjectModification;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.sequence.SequenceManager;
import org.apache.ojb.broker.util.sequence.SequenceManagerFactory;

/**
 * The PersistenceBrokerImpl is an implementation of the PersistenceBroker
 * Interface that specifies a persistence mechanism for Java objects.
 * This Concrete implementation provides an object relational mapping
 * and allows to store and retrieve arbitrary objects in/from relational
 * databases accessed by JDBC.
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @author <a href="mailto:leandro@ibnetwork.com.br">Leandro Rodrigo Saad Cruz<a>
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird<a>
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 *
 * @version $Id: PersistenceBrokerImpl.java,v 1.4 2008-07-08 15:53:48 sfheise Exp $
 */
public class PersistenceBrokerImpl extends PersistenceBrokerAbstractImpl implements PBState
{
    private Logger logger = LoggerFactory.getLogger(PersistenceBrokerImpl.class);

    protected PersistenceBrokerFactoryIF pbf;
    protected BrokerHelper brokerHelper;
    protected MtoNBroker mtoNBroker;
    protected QueryReferenceBroker referencesBroker;

    /**
     * signs if this broker was closed
     */
    private boolean isClosed;
    /**
     * Reflects the transaction status of this instance.
     */
    private boolean inTransaction;
    /**
     * Flag indicate that this PB instance is handled
     * in a managed environment.
     */
    private boolean managed;
    private MaterializationCache objectCache;
    /**
     * m_DbAccess is used to do all Jdbc related work: connecting, executing...
     */
    private JdbcAccess dbAccess;
    /**
     * holds mapping information for all classes to be treated by PersistenceBroker
     */
    private DescriptorRepository descriptorRepository = null;
    private ConnectionManagerIF connectionManager = null;
    private SequenceManager sequenceManager = null;
    private StatementManagerIF statementManager = null;
    private SqlGenerator sqlGenerator;
    private IdentityFactory identityFactory;
    private RelationshipPrefetcherFactory relationshipPrefetcherFactory;
    private ProxyFactory proxyFactory;
    private PBKey pbKey;

    /**
     * List of objects being stored now, allows to avoid infinite
     * recursion storeCollections -> storeReferences -> storeCollections...
     *
     */
    /*
    we use an object identity based List to compare objects to prevent problems
    with user implemented equals/hashCode methods of persistence capable objects
    (e.g. objects are equals but PK fields not)
    */
    private List nowStoring = new IdentityArrayList();

    /**
     * Lists for object registration during delete operations.
     * We reuse these list to avoid excessive object creation.
     * @see #clearRegistrationLists
     */
    /*
    arminw: list was cleared before delete method end. Internal we only
    call doDelete(...) method. Same procedure as 'nowStoring'

    we use an object identity based List to compare objects to prevent problems
    with user implemented equals/hashCode methods of persistence capable objects
    (e.g. objects are equals but PK fields not)
    */
    private List markedForDelete = new IdentityArrayList();

    /**
     * The set of identities of all deleted objects during current transaction
     */
    /*
    olegnitz: this is the only way I know that solves the following problem
    of batch mode: if one does store() after delete() for the same OID,
    the broker checks whether the given OID exists in database to decide
    which action to do: INSERT or UPDATE. If the preceding DELETE statement is
    still in batch (not executed yet), then the OID exists in database so
    the broker does UPDATE. Due the the following set of deleted OIDs
    the broker will know that it should do INSERT.
    */
    private Set deletedDuringTransaction = new HashSet();

    /**
     * Constructor used by {@link PersistenceBrokerFactoryIF} implementation.
     */
    public PersistenceBrokerImpl(PBKey key, PersistenceBrokerFactoryIF pbf)
    {
        refresh();
        if(key == null) throw new PersistenceBrokerException("Could not instantiate broker with PBKey 'null'");
        this.pbf = pbf;
        this.pbKey = key;
        /*
        be careful when changing initializing order
        */
        brokerHelper = new BrokerHelper(this);
        connectionManager = ConnectionManagerFactory.getInstance().createConnectionManager(this);
        /*
        TODO: find better solution
        MaterializationCache is a interim solution help to solve the problem of not full
        materialized object reads by concurrent threads and will be replaced when
        the new real two-level cache was introduced
        */
        objectCache = ObjectCacheFactory.getInstance().createObjectCache(this);
        sequenceManager = SequenceManagerFactory.getSequenceManager(this);
        dbAccess = JdbcAccessFactory.getInstance().createJdbcAccess(this);
        statementManager = StatementManagerFactory.getInstance().createStatementManager(this);
        sqlGenerator = SqlGeneratorFactory.getInstance().createSqlGenerator(
                        connectionManager.getSupportedPlatform());
        mtoNBroker = new MtoNBroker(this);
        referencesBroker = new QueryReferenceBroker(this);
        identityFactory = new IdentityFactoryImpl(this);
        relationshipPrefetcherFactory = new RelationshipPrefetcherFactory(this);
        proxyFactory = AbstractProxyFactory.getProxyFactory();
    }

    public MaterializationCache getInternalCache()
    {
        return objectCache;
    }

    public IdentityFactory serviceIdentity()
    {
        return this.identityFactory;
    }

    public SqlGenerator serviceSqlGenerator()
    {
        return this.sqlGenerator;
    }

    public StatementManagerIF serviceStatementManager()
    {
        return statementManager;
    }

    public JdbcAccess serviceJdbcAccess()
    {
        return dbAccess;
    }

    public ConnectionManagerIF serviceConnectionManager()
    {
        return connectionManager;
    }

    public SequenceManager serviceSequenceManager()
    {
        return this.sequenceManager;
    }

    public BrokerHelper serviceBrokerHelper()
    {
        return this.brokerHelper;
    }

    public ObjectCache serviceObjectCache()
    {
        return this.objectCache;
    }

    public QueryReferenceBroker getReferenceBroker()
    {
        return this.referencesBroker;
    }

    public RelationshipPrefetcherFactory getRelationshipPrefetcherFactory()
    {
        return relationshipPrefetcherFactory;
    }

    public boolean isClosed()
    {
        return this.isClosed;
    }

    public void setClosed(boolean closed)
    {
        // When lookup the PB instance from pool method setClosed(false)
        // was called before returning instance from pool, in this case
        // OJB have to refresh the instance.
        if(!closed)
        {
            refresh();
        }
        this.isClosed = closed;
    }

    /**
     * If <em>true</em> this instance is handled by a managed
     * environment - registered within a JTA transaction.
     */
    public boolean isManaged()
    {
        return managed;
    }

    /**
     * Set <em>true</em> if this instance is registered within a
     * JTA transaction. On {@link #close()} call this flag was reset
     * to <em>false</em> automatic.
     */
    public void setManaged(boolean managed)
    {
        this.managed = managed;
    }

    /**
     * Lookup the current {@link DescriptorRepository} for
     * this class. This method is responsible to keep this
     * PB instance in sync with {@link MetadataManager}.
     */
    public void refresh()
    {
        // guarantee that refreshed use initial status
        setInTransaction(false);
        this.descriptorRepository = MetadataManager.getInstance().getRepository();
    }

    /**
     * Release all resources used by this
     * class - CAUTION: No further operations can be
     * done with this instance after calling this method.
     */
    public void destroy()
    {
        removeAllListeners();
        if (connectionManager != null)
        {
            if(connectionManager.isInLocalTransaction())
            {
                connectionManager.localRollback();
            }
            connectionManager.releaseConnection();
        }
        this.setClosed(true);

        this.descriptorRepository = null;
        this.pbKey = null;
        this.pbf = null;
        this.connectionManager = null;
        this.dbAccess = null;
        this.objectCache = null;
        this.sequenceManager = null;
        this.sqlGenerator = null;
        this.statementManager = null;
    }

    public PBKey getPBKey()
    {
        return pbKey;
    }

    public void setPBKey(PBKey key)
    {
        this.pbKey = key;
    }

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#close()
     */
    public boolean close()
    {
        /**
         * MBAIRD: if we call close on a broker that is in a transaction,
         * we should just abort whatever it's doing.
         */
        if (isInTransaction())
        {
            logger.error("Broker is still in PB-transaction, do automatic abort before close!");
            abortTransaction();
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("PB.close was called: " + this);
        }
        try
        {
            fireBrokerEvent(BEFORE_CLOSE_EVENT);
            clearRegistrationLists();
            referencesBroker.removePrefetchingListeners();
            if (connectionManager != null)
            {
                connectionManager.releaseConnection();
                /*
                arminw:
                set batch mode explicit to 'false'. Using

                connectionManager.setBatchMode(
                        connectionManager.getConnectionDescriptor().getBatchMode());

                cause many unexpected junit failures/errors when running
                test suite with batch-mode 'true' setting.
                */
                connectionManager.setBatchMode(false);
            }
        }
        finally
        {
            // reset flag indicating use in managed environment
            setManaged(false);
            // free current used DescriptorRepository reference
            descriptorRepository = null;
            removeAllListeners();
            this.setClosed(true);
        }
        return true;
    }

    /**
     * Abort and close the transaction.
     * Calling abort abandons all persistent object modifications and releases the
     * associated locks.
     * If transaction is not in progress a TransactionNotInProgressException is thrown
     */
    public synchronized void abortTransaction() throws TransactionNotInProgressException
    {
        if(isInTransaction())
        {
            fireBrokerEvent(BEFORE_ROLLBACK_EVENT);
            setInTransaction(false);
            clearRegistrationLists();
            referencesBroker.removePrefetchingListeners();
            /*
            arminw:
            check if we in local tx, before do local rollback
            Necessary, because ConnectionManager may do a rollback by itself
            or in managed environments the used connection is already be closed
            */
            if(connectionManager.isInLocalTransaction()) this.connectionManager.localRollback();
            fireBrokerEvent(AFTER_ROLLBACK_EVENT);
        }
    }

    /**
     * begin a transaction against the underlying RDBMS.
     * Calling <code>beginTransaction</code> multiple times,
     * without an intervening call to <code>commitTransaction</code> or <code>abortTransaction</code>,
     * causes the exception <code>TransactionInProgressException</code> to be thrown
     * on the second and subsequent calls.
     */
    public synchronized void beginTransaction() throws TransactionInProgressException, TransactionAbortedException
    {
        if (isInTransaction())
        {
            throw new TransactionInProgressException("PersistenceBroker is already in transaction");
        }
        fireBrokerEvent(BEFORE_BEGIN_EVENT);
        setInTransaction(true);
        this.connectionManager.localBegin();
        fireBrokerEvent(AFTER_BEGIN_EVENT);
    }

    /**
     * Commit and close the transaction.
     * Calling <code>commit</code> commits to the database all
     * UPDATE, INSERT and DELETE statements called within the transaction and
     * releases any locks held by the transaction.
     * If beginTransaction() has not been called before a
     * TransactionNotInProgressException exception is thrown.
     * If the transaction cannot be commited a TransactionAbortedException exception is thrown.
     */
    public synchronized void commitTransaction() throws TransactionNotInProgressException, TransactionAbortedException
    {
        if (!isInTransaction())
        {
            throw new TransactionNotInProgressException("PersistenceBroker is NOT in transaction, can't commit");
        }
        fireBrokerEvent(BEFORE_COMMIT_EVENT);
        setInTransaction(false);
        clearRegistrationLists();
        referencesBroker.removePrefetchingListeners();
        /*
        arminw:
        In managed environments it should be possible to close a used connection before
        the tx was commited, thus it will be possible that the PB instance is in PB-tx, but
        the connection is already closed. To avoid problems check if CM is in local tx before
        do the CM.commit call
        */
        if(connectionManager.isInLocalTransaction())
        {
            this.connectionManager.localCommit();
        }
        fireBrokerEvent(AFTER_COMMIT_EVENT);
    }

    /**
     * Deletes the concrete representation of the specified object in the underlying
     * persistence system. This method is intended for use in top-level api or
     * by internal calls.
     *
     * @param obj The object to delete.
     * @param ignoreReferences With this flag the automatic deletion/unlinking
     * of references can be suppressed (independent of the used auto-delete setting in metadata),
     * except {@link org.apache.ojb.broker.metadata.SuperReferenceDescriptor}
     * these kind of reference (descriptor) will always be performed. If <em>true</em>
     * all "normal" referenced objects will be ignored, only the specified object is handled.
     * @throws PersistenceBrokerException
     */
    public void delete(Object obj, boolean ignoreReferences) throws PersistenceBrokerException
    {
        if(isTxCheck() && !isInTransaction())
        {
            if(logger.isEnabledFor(Logger.ERROR))
            {
                String msg = "No running PB-tx found. Please, only delete objects in context of a PB-transaction" +
                    " to avoid side-effects - e.g. when rollback of complex objects.";
                try
                {
                    throw new Exception("** Delete object without active PersistenceBroker transaction **");
                }
                catch(Exception e)
                {
                    logger.error(msg, e);
                }
            }
        }
        try
        {
            doDelete(obj, ignoreReferences);
        }
        finally
        {
            markedForDelete.clear();
        }
    }

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#delete
     */
    public void delete(Object obj) throws PersistenceBrokerException
    {
        delete(obj, false);
    }

    /**
     * do delete given object. Should be used by all intern classes to delete
     * objects.
     */
    private void doDelete(Object obj, boolean ignoreReferences) throws PersistenceBrokerException
    {
        //logger.info("DELETING " + obj);
        // object is not null
        if (obj != null)
        {
            obj = getProxyFactory().getRealObject(obj);
            /**
             * Kuali Foundation modification -- 8/24/2007
             */
            if ( obj == null ) return;
            /**
             * End of Kuali Foundation modification
             */
            /**
             * MBAIRD
             * 1. if we are marked for delete already, avoid recursing on this object
             *
             * arminw:
             * use object instead Identity object in markedForDelete List,
             * because using objects we get a better performance. I can't find
             * side-effects in doing so.
             */
            if (markedForDelete.contains(obj))
            {
                return;
            }
            
            ClassDescriptor cld = getClassDescriptor(obj.getClass());
            //BRJ: check for valid pk
            if (!serviceBrokerHelper().assertValidPkForDelete(cld, obj))
            {
                String msg = "Cannot delete object without valid PKs. " + obj;
                logger.error(msg);
                return;
            }
            
            /**
             * MBAIRD
             * 2. register object in markedForDelete map.
             */
            markedForDelete.add(obj);
            Identity oid = serviceIdentity().buildIdentity(cld, obj);

            // Invoke events on PersistenceBrokerAware instances and listeners
            BEFORE_DELETE_EVENT.setTarget(obj);
            fireBrokerEvent(BEFORE_DELETE_EVENT);
            BEFORE_DELETE_EVENT.setTarget(null);

            // now perform deletion
            performDeletion(cld, obj, oid, ignoreReferences);
 	  	 
            // Invoke events on PersistenceBrokerAware instances and listeners
            AFTER_DELETE_EVENT.setTarget(obj);
            fireBrokerEvent(AFTER_DELETE_EVENT);
            AFTER_DELETE_EVENT.setTarget(null);
 	  	 	
            // let the connection manager to execute batch
            connectionManager.executeBatchIfNecessary();
        }
    }
 	  	 
    /**
     * This method perform the delete of the specified object
     * based on the {@link org.apache.ojb.broker.metadata.ClassDescriptor}.
     */
    private void performDeletion(final ClassDescriptor cld, final Object obj, final Identity oid, final boolean ignoreReferences) throws PersistenceBrokerException
    {
            // 1. delete dependend collections
            if (!ignoreReferences  && cld.getCollectionDescriptors().size() > 0)
            {
                deleteCollections(obj, cld.getCollectionDescriptors());
            }
            // 2. delete object from directly mapped table
            try
            {
                dbAccess.executeDelete(cld, obj); // use obj not oid to delete, BRJ
            }
            catch(OptimisticLockException e)
            {
                // ensure that the outdated object be removed from cache
                objectCache.remove(oid);
                throw e;
            }

            // 3. Add OID to the set of deleted objects
            deletedDuringTransaction.add(oid);

            // 4. delete dependend upon objects last to avoid FK violations
            if (cld.getObjectReferenceDescriptors().size() > 0)
            {
                deleteReferences(cld, obj, oid, ignoreReferences);
            }
            // remove obj from the object cache:
            objectCache.remove(oid);
    }

    /**
     * Extent aware Delete by Query
     * @param query
     * @param cld
     * @throws PersistenceBrokerException
     */
    private void deleteByQuery(Query query, ClassDescriptor cld) throws PersistenceBrokerException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("deleteByQuery " + cld.getClassNameOfObject() + ", " + query);
        }

        if (query instanceof QueryBySQL)
        {
            String sql = ((QueryBySQL) query).getSql();
            this.dbAccess.executeUpdateSQL(sql, cld);
        }
        else
        {
            // if query is Identity based transform it to a criteria based query first
            if (query instanceof QueryByIdentity)
            {
                QueryByIdentity qbi = (QueryByIdentity) query;
                Object oid = qbi.getExampleObject();
                // make sure it's an Identity
                if (!(oid instanceof Identity))
                {
                    oid = serviceIdentity().buildIdentity(oid);
                }
                query = referencesBroker.getPKQuery((Identity) oid);
            }

            if (!cld.isInterface())
            {
                this.dbAccess.executeDelete(query, cld);
            }

            // if class is an extent, we have to delete all extent classes too
            String lastUsedTable = cld.getFullTableName();
            if (cld.isExtent())
            {
                Iterator extents = getDescriptorRepository().getAllConcreteSubclassDescriptors(cld).iterator();

                while (extents.hasNext())
                {
                    ClassDescriptor extCld = (ClassDescriptor) extents.next();

                    // read same table only once
                    if (!extCld.getFullTableName().equals(lastUsedTable))
                    {
                        lastUsedTable = extCld.getFullTableName();
                        this.dbAccess.executeDelete(query, extCld);
                    }
                }
            }

        }
    }

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#deleteByQuery(Query)
     */
    public void deleteByQuery(Query query) throws PersistenceBrokerException
    {
        ClassDescriptor cld = getClassDescriptor(query.getSearchClass());
        deleteByQuery(query, cld);
    }

    /**
     * Deletes references that <b>obj</b> points to.
     * All objects which we have a FK poiting to (Via ReferenceDescriptors)
     * will be deleted if auto-delete is true <b>AND</b>
     * the member field containing the object reference is NOT null.
     *
     * @param obj Object which we will delete references for
     * @param listRds list of ObjectRederenceDescriptors
     * @param ignoreReferences With this flag the automatic deletion/unlinking
     * of references can be suppressed (independent of the used auto-delete setting in metadata),
     * except {@link org.apache.ojb.broker.metadata.SuperReferenceDescriptor}
     * these kind of reference (descriptor) will always be performed.
     * @throws PersistenceBrokerException if some goes wrong - please see the error message for details
     */
    private void deleteReferences(ClassDescriptor cld, Object obj, Identity oid, boolean ignoreReferences) throws PersistenceBrokerException
    {
    	List listRds = cld.getObjectReferenceDescriptors();
        // get all members of obj that are references and delete them
        Iterator i = listRds.iterator();
        while (i.hasNext())
        {
            ObjectReferenceDescriptor rds = (ObjectReferenceDescriptor) i.next();
            if ((!ignoreReferences && rds.getCascadingDelete() == ObjectReferenceDescriptor.CASCADE_OBJECT)
                    || rds.isSuperReferenceDescriptor())
            {
                Object referencedObject = rds.getPersistentField().get(obj);
                if (referencedObject != null)
                {
                	if(rds.isSuperReferenceDescriptor())
                	{
                		ClassDescriptor base = cld.getSuperClassDescriptor();
                		/*
 	  	                 arminw: If "table-per-subclass" inheritance is used we have to
 	  	                 guarantee that all super-class table entries are deleted too.
 	  	                 Thus we have to perform the recursive deletion of all super-class
 	  	                 table entries.
                		 */
                		performDeletion(base, referencedObject, oid, ignoreReferences);
                	}
                	else
                	{
                		doDelete(referencedObject, ignoreReferences);
                	}
                }
            }
        }
    }

    /**
     * Deletes collections of objects poiting to <b>obj</b>.
     * All object which have a FK poiting to this object (Via CollectionDescriptors)
     * will be deleted if auto-delete is true <b>AND</b>
     * the member field containing the object reference if NOT null.
     *
     * @param obj Object which we will delete collections for
     * @param listCds list of ObjectReferenceDescriptors
     * @throws PersistenceBrokerException if some goes wrong - please see the error message for details
     */
    private void deleteCollections(Object obj, List listCds) throws PersistenceBrokerException
    {
        // get all members of obj that are collections and delete all their elements
        Iterator i = listCds.iterator();

        while (i.hasNext())
        {
            CollectionDescriptor cds = (CollectionDescriptor) i.next();
            if(cds.getCascadingDelete() != ObjectReferenceDescriptor.CASCADE_NONE)
            {
                if(cds.isMtoNRelation())
                {
                    // if this is a m:n mapped table, remove entries from indirection table
                    mtoNBroker.deleteMtoNImplementor(cds, obj);
                }
                /*
                if cascading delete is on, delete all referenced objects.
                NOTE: User has to take care to populate all referenced objects before delete
                the main object to avoid referential constraint violation
                 */
                if (cds.getCascadingDelete() == ObjectReferenceDescriptor.CASCADE_OBJECT)
                {
                    Object col = cds.getPersistentField().get(obj);
                    if (col != null)
                    {
                        Iterator colIterator = BrokerHelper.getCollectionIterator(col);
                        while (colIterator.hasNext())
                        {
                            doDelete(colIterator.next(), false);
                        }
                    }
                }
            }
        }
    }

    /**
     * Store an Object.
     * @see org.apache.ojb.broker.PersistenceBroker#store(Object)
     */
    public void store(Object obj) throws PersistenceBrokerException
    {
        obj = extractObjectToStore(obj);
        // only do something if obj != null
        if(obj == null) return;

        ClassDescriptor cld = getClassDescriptor(obj.getClass());
        /*
        if one of the PK fields was null, we assume the objects
        was new and needs insert
        */
        boolean insert = serviceBrokerHelper().hasNullPKField(cld, obj);
        Identity oid = serviceIdentity().buildIdentity(cld, obj);
        /*
        if PK values are set, lookup cache or db to see whether object
        needs insert or update
        */
        if (!insert)
        {
            insert = objectCache.lookup(oid) == null
                && !serviceBrokerHelper().doesExist(cld, oid, obj);
        }
        store(obj, oid, cld, insert);
    }

    /**
     * Check if the given object is <code>null</code> or an unmaterialized proxy object - in
     * both cases <code>null</code> will be returned, else the given object itself or the
     * materialized proxy object will be returned.
     */
    private Object extractObjectToStore(Object obj)
    {
        Object result = obj;
        // only do something if obj != null
        if(result != null)
        {
            // ProxyObjects only have to be updated if their real
            // subjects have been loaded
            result = getProxyFactory().getRealObjectIfMaterialized(obj);
            // null for unmaterialized Proxy
            if (result == null)
            {
                if(logger.isDebugEnabled())
                    logger.debug("No materialized object could be found -> nothing to store," +
                            " object was " + ObjectUtils.identityToString(obj));
            }
        }
        return result;
    }

    /**
     * Method which start the real store work (insert or update)
     * and is intended for use by top-level api or internal calls.
     *
     * @param obj The object to store.
     * @param oid The {@link Identity} of the object to store.
     * @param cld The {@link org.apache.ojb.broker.metadata.ClassDescriptor} of the object.
     * @param insert If <em>true</em> an insert operation will be performed, else update operation.
     * @param ignoreReferences With this flag the automatic storing/linking
     * of references can be suppressed (independent of the used auto-update setting in metadata),
     * except {@link org.apache.ojb.broker.metadata.SuperReferenceDescriptor}
     * these kind of reference (descriptor) will always be performed. If <em>true</em>
     * all "normal" referenced objects will be ignored, only the specified object is handled.
     */
    public void store(Object obj, Identity oid, ClassDescriptor cld,  boolean insert, boolean ignoreReferences)
    {
        if(obj == null || nowStoring.contains(obj))
        {
            return;
        }

        /*
        if the object has been deleted during this transaction,
        then we must insert it
        */
        //System.out.println("## insert: " +insert + " / deleted: " + deletedDuringTransaction);
        if (!insert)
        {
            insert = deletedDuringTransaction.contains(oid);
        }

        //************************************************
        // now store it:
        if(isTxCheck() && !isInTransaction())
        {
            if(logger.isEnabledFor(Logger.ERROR))
            {
                try
                {
                    throw new Exception("** Try to store object without active PersistenceBroker transaction **");
                }
                catch(Exception e)
                {
                    logger.error("No running tx found, please only store in context of an PB-transaction" +
                    ", to avoid side-effects - e.g. when rollback of complex objects", e);
                }
            }
        }
        // Invoke events on PersistenceBrokerAware instances and listeners
        if (insert)
        {
            BEFORE_STORE_EVENT.setTarget(obj);
            fireBrokerEvent(BEFORE_STORE_EVENT);
            BEFORE_STORE_EVENT.setTarget(null);
        }
        else
        {
            BEFORE_UPDATE_EVENT.setTarget(obj);
            fireBrokerEvent(BEFORE_UPDATE_EVENT);
            BEFORE_UPDATE_EVENT.setTarget(null);
        }

        try
        {
            nowStoring.add(obj);
            storeToDb(obj, cld, oid, insert, ignoreReferences);
        }
        finally
        {
            // to optimize calls to DB don't remove already stored objects
            nowStoring.remove(obj);
        }


        // Invoke events on PersistenceBrokerAware instances and listeners
        if (insert)
        {
            AFTER_STORE_EVENT.setTarget(obj);
            fireBrokerEvent(AFTER_STORE_EVENT);
            AFTER_STORE_EVENT.setTarget(null);
        }
        else
        {
            AFTER_UPDATE_EVENT.setTarget(obj);
            fireBrokerEvent(AFTER_UPDATE_EVENT);
            AFTER_UPDATE_EVENT.setTarget(null);
        }
        // end of store operation
        //************************************************

        // if the object was stored, remove it from deleted set
        if(deletedDuringTransaction.size() > 0) deletedDuringTransaction.remove(oid);

        // let the connection manager to execute batch
        connectionManager.executeBatchIfNecessary();
    }

    /**
     * Internal used method which start the real store work.
     */
    protected void store(Object obj, Identity oid, ClassDescriptor cld,  boolean insert)
    {
        store(obj, oid, cld, insert, false);
    }

    /**
     * Store all object references that <b>obj</b> points to.
     * All objects which we have a FK pointing to (Via ReferenceDescriptors) will be
     * stored if auto-update is true <b>AND</b> the member field containing the object
     * reference is NOT null.
     * With flag <em>ignoreReferences</em> the storing/linking
     * of references can be suppressed (independent of the used auto-update setting),
     * except {@link org.apache.ojb.broker.metadata.SuperReferenceDescriptor}
     * these kind of reference (descriptor) will always be performed.
     *
     * @param obj Object which we will store references for
     */
    private void storeReferences(Object obj, ClassDescriptor cld, boolean insert, boolean ignoreReferences)
    {
        // get all members of obj that are references and store them
        Collection listRds = cld.getObjectReferenceDescriptors();
        // return if nothing to do
        if(listRds == null || listRds.size() == 0)
        {
            return;
        }
        Iterator i = listRds.iterator();
        while (i.hasNext())
        {
            ObjectReferenceDescriptor rds = (ObjectReferenceDescriptor) i.next();
            /*
            arminw: the super-references (used for table per subclass inheritance) must
            be performed in any case. The "normal" 1:1 references can be ignored when
            flag "ignoreReferences" is set
            */
            if((!ignoreReferences && rds.getCascadingStore() != ObjectReferenceDescriptor.CASCADE_NONE)
                    || rds.isSuperReferenceDescriptor())
            {
                storeAndLinkOneToOne(false, obj, cld, rds, insert);
            }
        }
    }

    /**
     * Store/Link 1:1 reference.
     *
     * @param obj real object the reference starts
     * @param rds {@link ObjectReferenceDescriptor} of the real object
     * @param insert flag for insert operation
     */
    private void storeAndLinkOneToOne(boolean onlyLink, Object obj, ClassDescriptor cld,
                                      ObjectReferenceDescriptor rds, boolean insert)
    {
        Object ref = rds.getPersistentField().get(obj);
        if (!onlyLink && rds.getCascadingStore() == ObjectReferenceDescriptor.CASCADE_OBJECT)
        {
            if(rds.isSuperReferenceDescriptor())
            {
                ClassDescriptor superCld = rds.getClassDescriptor().getSuperClassDescriptor();
                Identity oid = serviceIdentity().buildIdentity(superCld, ref);
                storeToDb(ref, superCld, oid, insert);
            }
            else store(ref);
        }
    
        /**
         * Kuali Foundation modification -- 1/10/2008
         */
        ref = getProxyFactory().getRealObject(ref);
        /**
         * End of Kuali Foundation modification
         */
        link(obj, cld, rds, ref, insert);
    }

    /**
     * Store/Link collections of objects poiting to <b>obj</b>.
     * More info please see comments in source.
     *
     * @param obj real object which we will store collections for
     * @throws PersistenceBrokerException if some goes wrong - please see the error message for details
     */
    private void storeCollections(Object obj, ClassDescriptor cld, boolean insert) throws PersistenceBrokerException
    {
        // get all members of obj that are collections and store all their elements
        Collection listCods = cld.getCollectionDescriptors();
        // return if nothing to do
        if (listCods.size() == 0)
        {
            return;
        }
        Iterator i = listCods.iterator();
        while (i.hasNext())
        {
            CollectionDescriptor cod = (CollectionDescriptor) i.next();

            // if CASCADE_NONE was set, do nothing with referenced objects
            if (cod.getCascadingStore() != ObjectReferenceDescriptor.CASCADE_NONE)
            {
                Object referencedObjects = cod.getPersistentField().get(obj);
                if (cod.isMtoNRelation())
                {
                    storeAndLinkMtoN(false, obj, cod, referencedObjects, insert);
                }
                else
                {
                    storeAndLinkOneToMany(false, obj, cod, referencedObjects, insert);
                }

                // BRJ: only when auto-update = object (CASCADE_OBJECT)
                //
                if ((cod.getCascadingStore() == ObjectReferenceDescriptor.CASCADE_OBJECT)
                        && (referencedObjects instanceof ManageableCollection))
                {
                    ((ManageableCollection) referencedObjects).afterStore(this);
                }
            }
        }
    }

    /**
     * Store/Link m:n collection references.
     *
     * @param obj real object the reference starts
     * @param cod {@link CollectionDescriptor} of the real object
     * @param referencedObjects the referenced objects ({@link ManageableCollection} or Collection or Array) or null
     * @param insert flag for insert operation
     */
    private void storeAndLinkMtoN(boolean onlyLink, Object obj, CollectionDescriptor cod,
                                  Object referencedObjects, boolean insert)
    {
        /*
        - if the collection is a collectionproxy and it's not already loaded
        no need to perform an update on the referenced objects
        - on insert we link and insert the referenced objects, because the proxy
        collection maybe "inherited" from the object before the PK was replaced
        */
        if(insert || !(referencedObjects instanceof CollectionProxy
                        && !((CollectionProxy) referencedObjects).isLoaded()))
        {
            // if referenced objects are null, assign empty list
            if(referencedObjects == null)
            {
                referencedObjects = Collections.EMPTY_LIST;
            }
            /*
            NOTE: Take care of referenced objects, they could be of type Collection or
            an Array or of type ManageableCollection, thus it is not guaranteed that we
            can cast to Collection!!!

            if we store an object with m:n reference and no references could be
            found, we remove all entires of given object in indirection table
            */
            Iterator referencedObjectsIterator;

            if(!onlyLink && cod.getCascadingStore() == ObjectReferenceDescriptor.CASCADE_OBJECT)
            {
                referencedObjectsIterator = BrokerHelper.getCollectionIterator(referencedObjects);
                while (referencedObjectsIterator.hasNext())
                {
                    store(referencedObjectsIterator.next());
                }
            }

            Collection existingMtoNKeys;
            if(!insert)
            {
                existingMtoNKeys = mtoNBroker.getMtoNImplementor(cod, obj);
                // we can't reuse iterator
                referencedObjectsIterator = BrokerHelper.getCollectionIterator(referencedObjects);
                // remove all entries in indirection table which not be part of referenced objects
                mtoNBroker.deleteMtoNImplementor(cod, obj, referencedObjectsIterator, existingMtoNKeys);
            }
            else
            {
                existingMtoNKeys = Collections.EMPTY_LIST;
            }
            // we can't reuse iterator
            referencedObjectsIterator = BrokerHelper.getCollectionIterator(referencedObjects);
            while (referencedObjectsIterator.hasNext())
            {
                Object refObj = referencedObjectsIterator.next();
                // Now store indirection record
                // BRJ: this could cause integrity problems because
                // obj may not be stored depending on auto-update
                mtoNBroker.storeMtoNImplementor(cod, obj, refObj, existingMtoNKeys);
            }
        }
    }

    /**
     * Store/Link 1:n collection references.
     *
     * @param obj real object the reference starts
     * @param linkOnly if true the referenced objects will only be linked (FK set, no reference store).
     * Reference store setting in descriptor will be ignored in this case
     * @param cod {@link CollectionDescriptor} of the real object
     * @param referencedObjects the referenced objects ({@link ManageableCollection} or Collection or Array) or null
     * @param insert flag for insert operation
     */
    private void storeAndLinkOneToMany(boolean linkOnly, Object obj, CollectionDescriptor cod,
                                       Object referencedObjects, boolean insert)
    {
        if(referencedObjects == null)
        {
            return;
        }
        /*
        Only make sense to perform (link or/and store) real referenced objects
        or materialized collection proxy objects, because on unmaterialized collection
        nothing has changed.

        - if the collection is a collectionproxy and it's not already loaded
        no need to perform an update on the referenced objects
        - on insert we link and insert the referenced objects, because the proxy
        collection maybe "inherited" from the object before the PK was replaced
        */
        if(insert || !(referencedObjects instanceof CollectionProxyDefaultImpl
                        && !((CollectionProxyDefaultImpl) referencedObjects).isLoaded()))
        {
            Iterator it = BrokerHelper.getCollectionIterator(referencedObjects);
            Object refObj;
            while(it.hasNext())
            {
                refObj = it.next();
                /*
                TODO: Check this!
                arminw:
                When it's necessary to 'link' (set the FK) the 1:n reference objects?
                1. set FK in refObj if it is materialized
                2. if the referenced object is a proxy AND the main object needs insert
                we have to materialize the real object, because the user may move a collection
                of proxy objects from object A to new object B. In this case we have to replace the
                FK in the proxy object with new key of object B.
                */
                if(insert || getProxyFactory().isMaterialized(refObj))
                {
                    ClassDescriptor refCld = getClassDescriptor(getProxyFactory().getRealClass(refObj));
                    // get the real object before linking
                    refObj = getProxyFactory().getRealObject(refObj);
                    link(refObj, refCld, cod, obj, insert);
                    // if enabled cascade store and not only link, store the refObj
                    if(!linkOnly && cod.getCascadingStore() == ObjectReferenceDescriptor.CASCADE_OBJECT)
                    {
                        store(refObj);
                    }
                }
            }
        }
    }

    /**
     * Assign FK value to target object by reading PK values of referenced object.
     *
     * @param targetObject real (non-proxy) target object
     * @param cld {@link ClassDescriptor} of the real target object
     * @param rds An {@link ObjectReferenceDescriptor} or {@link CollectionDescriptor}
     * associated with the real object.
     * @param referencedObject referenced object or proxy
     * @param insert Show if "linking" is done while insert or update.
     */
    public void link(Object targetObject, ClassDescriptor cld, ObjectReferenceDescriptor rds, Object referencedObject, boolean insert)
    {
        // MBAIRD: we have 'disassociated' this object from the referenced object,
        // the object represented by the reference descriptor is now null, so set
        // the fk in the target object to null.
        // arminw: if an insert was done and ref object was null, we should allow
        // to pass FK fields of main object (maybe only the FK fields are set)
        if (referencedObject == null)
        {
            /*
            arminw:
            if update we set FK fields to 'null', because reference was disassociated
            We do nothing on insert, maybe only the FK fields of main object (without
            materialization of the reference object) are set by the user
            */
            if(!insert)
            {
                unlinkFK(targetObject, cld, rds);
            }
        }
        else
        {
            setFKField(targetObject, cld, rds, referencedObject);
        }
    }

    /**
     * Unkink FK fields of target object.
     *
     * @param targetObject real (non-proxy) target object
     * @param cld {@link ClassDescriptor} of the real target object
     * @param rds An {@link ObjectReferenceDescriptor} or {@link CollectionDescriptor}
     * associated with the real object.
     */
    public void unlinkFK(Object targetObject, ClassDescriptor cld, ObjectReferenceDescriptor rds)
    {
        setFKField(targetObject, cld, rds, null);
    }

    /**
     * Set the FK value on the target object, extracted from the referenced object. If the referenced object was
     * <i>null</i> the FK values were set to <i>null</i>, expect when the FK field was declared as PK.
     *
     * @param targetObject real (non-proxy) target object
     * @param cld {@link ClassDescriptor} of the real target object
     * @param rds An {@link ObjectReferenceDescriptor} or {@link CollectionDescriptor}
     * @param referencedObject The referenced object or <i>null</i>
     */
    private void setFKField(Object targetObject, ClassDescriptor cld, ObjectReferenceDescriptor rds, Object referencedObject)
    {
        ValueContainer[] refPkValues;
        FieldDescriptor fld;
        FieldDescriptor[] objFkFields = rds.getForeignKeyFieldDescriptors(cld);
        if (objFkFields == null)
        {
            throw new PersistenceBrokerException("No foreign key fields defined for class '"+cld.getClassNameOfObject()+"'");
        }
        if(referencedObject == null)
        {
            refPkValues = null;
        }
        else
        {
            Class refClass = proxyFactory.getRealClass(referencedObject);
            ClassDescriptor refCld = getClassDescriptor(refClass);
            refPkValues = brokerHelper.getKeyValues(refCld, referencedObject, false);
        }
        for (int i = 0; i < objFkFields.length; i++)
        {
            fld = objFkFields[i];
            /*
            arminw:
            we set the FK value when the extracted PK fields from the referenced object are not null at all
            or if null, the FK field was not a PK field of target object too.
            Should be ok, because the values of the extracted PK field values should never be null and never
            change, so it doesn't matter if the target field is a PK too.
            */
            if(refPkValues != null || !fld.isPrimaryKey())
            {
                fld.getPersistentField().set(targetObject, refPkValues != null ? refPkValues[i].getValue(): null);
            }
        }
    }

    /**
     * Assign FK value of main object with PK values of the reference object.
     *
     * @param obj real object with reference (proxy) object (or real object with set FK values on insert)
     * @param cld {@link ClassDescriptor} of the real object
     * @param rds An {@link ObjectReferenceDescriptor} of real object.
     * @param insert Show if "linking" is done while insert or update.
     */
    public void linkOneToOne(Object obj, ClassDescriptor cld, ObjectReferenceDescriptor rds, boolean insert)
    {
        storeAndLinkOneToOne(true, obj, cld, rds, true);
    }

    /**
     * Assign FK value to all n-side objects referenced by given object.
     *
     * @param obj real object with 1:n reference
     * @param cod {@link CollectionDescriptor} of referenced 1:n objects
     * @param insert flag signal insert operation, false signals update operation
     */
    public void linkOneToMany(Object obj, CollectionDescriptor cod, boolean insert)
    {
        Object referencedObjects = cod.getPersistentField().get(obj);
        storeAndLinkOneToMany(true, obj, cod,referencedObjects, insert);
    }

    /**
     * Assign FK values and store entries in indirection table
     * for all objects referenced by given object.
     *
     * @param obj real object with 1:n reference
     * @param cod {@link CollectionDescriptor} of referenced 1:n objects
     * @param insert flag signal insert operation, false signals update operation
     */
    public void linkMtoN(Object obj, CollectionDescriptor cod, boolean insert)
    {
        Object referencedObjects = cod.getPersistentField().get(obj);
        storeAndLinkMtoN(true, obj, cod, referencedObjects, insert);
    }

    public void unlinkXtoN(Object obj, CollectionDescriptor col)
    {
        if(col.isMtoNRelation())
        {
            // if this is a m:n mapped table, remove entries from indirection table
            mtoNBroker.deleteMtoNImplementor(col, obj);
        }
        else
        {
            Object collectionObject = col.getPersistentField().get(obj);
            if (collectionObject != null)
            {
                Iterator colIterator = BrokerHelper.getCollectionIterator(collectionObject);
                ClassDescriptor cld = null;
                while (colIterator.hasNext())
                {
                    Object target = colIterator.next();
                    if(cld == null) cld = getClassDescriptor(getProxyFactory().getRealClass(target));
                    unlinkFK(target, cld, col);
                }
            }
        }
    }

    /**
     * Retrieve all References (also Collection-attributes) of a given instance.
     * Loading is forced, even if the collection- and reference-descriptors differ.
     * @param pInstance the persistent instance to work with
     */
    public void retrieveAllReferences(Object pInstance) throws PersistenceBrokerException
    {
        if (logger.isDebugEnabled())
        {
        	logger.debug("Manually retrieving all references for object " + serviceIdentity().buildIdentity(pInstance));
        }
        ClassDescriptor cld = getClassDescriptor(pInstance.getClass());
        getInternalCache().enableMaterializationCache();
        // to avoid problems with circular references, locally cache the current object instance
        Identity oid = serviceIdentity().buildIdentity(pInstance);
//        boolean needLocalRemove = false;
        if(getInternalCache().doLocalLookup(oid) == null)
        {
            getInternalCache().doInternalCache(oid, pInstance, MaterializationCache.TYPE_TEMP);
//            needLocalRemove = true;
        }
        try
        {
            referencesBroker.retrieveReferences(pInstance, cld, true);
            referencesBroker.retrieveCollections(pInstance, cld, true);
// arminw: should no longer needed since we use TYPE_TEMP for this kind of objects
//            // do locally remove the object to avoid problems with object state detection (insert/update),
//            // because objects found in the cache detected as 'old' means 'update'
//            if(needLocalRemove) getInternalCache().doLocalRemove(oid);
            getInternalCache().disableMaterializationCache();
        }
        catch(RuntimeException e)
        {
            getInternalCache().doLocalClear();
            throw e;
        }
    }

    /**
     * retrieve a single reference- or collection attribute
     * of a persistent instance.
     * @param pInstance the persistent instance
     * @param pAttributeName the name of the Attribute to load
     */
    public void retrieveReference(Object pInstance, String pAttributeName) throws PersistenceBrokerException
    {
        if (logger.isDebugEnabled())
        {
        	logger.debug("Retrieving reference named ["+pAttributeName+"] on object of type ["+
        	            pInstance.getClass().getName()+"]");
        }
        ClassDescriptor cld = getClassDescriptor(pInstance.getClass());
        CollectionDescriptor cod = cld.getCollectionDescriptorByName(pAttributeName);
        getInternalCache().enableMaterializationCache();
        // to avoid problems with circular references, locally cache the current object instance
        Identity oid = serviceIdentity().buildIdentity(pInstance);
        boolean needLocalRemove = false;
        if(getInternalCache().doLocalLookup(oid) == null)
        {
            getInternalCache().doInternalCache(oid, pInstance, MaterializationCache.TYPE_TEMP);
            needLocalRemove = true;
        }
        try
        {
            if (cod != null)
            {
                referencesBroker.retrieveCollection(pInstance, cld, cod, true);
            }
            else
            {
                ObjectReferenceDescriptor ord = cld.getObjectReferenceDescriptorByName(pAttributeName);
                if (ord != null)
                {
                    referencesBroker.retrieveReference(pInstance, cld, ord, true);
                }
                else
                {
                    throw new PersistenceBrokerException("did not find attribute " + pAttributeName +
                            " for class " + pInstance.getClass().getName());
                }
            }
            // do locally remove the object to avoid problems with object state detection (insert/update),
            // because objects found in the cache detected as 'old' means 'update'
            if(needLocalRemove) getInternalCache().doLocalRemove(oid);
            getInternalCache().disableMaterializationCache();
        }
        catch(RuntimeException e)
        {
            getInternalCache().doLocalClear();
            throw e;
        }
    }

    /**
     * Check if the references of the specified object have enabled
     * the <em>refresh</em> attribute and refresh the reference if set <em>true</em>.
     *
     * @throws PersistenceBrokerException if there is a error refreshing collections or references
     * @param obj The object to check.
     * @param oid The {@link Identity} of the object.
     * @param cld The {@link org.apache.ojb.broker.metadata.ClassDescriptor} of the object.
     */
    public void checkRefreshRelationships(Object obj, Identity oid, ClassDescriptor cld)
    {
        Iterator iter;
        CollectionDescriptor cds;
        ObjectReferenceDescriptor rds;
        // to avoid problems with circular references, locally cache the current object instance
        Object tmp = getInternalCache().doLocalLookup(oid);
        if(tmp != null && getInternalCache().isEnabledMaterialisationCache())
        {
            /*
            arminw: This should fix OJB-29, infinite loops on bidirectional 1:1 relations with
            refresh attribute 'true' for both references. OJB now assume that the object is already
            refreshed when it's cached in the materialisation cache
            */
            return;
        }
        try
        {
            getInternalCache().enableMaterializationCache();
            if(tmp == null)
            {
                getInternalCache().doInternalCache(oid, obj, MaterializationCache.TYPE_TEMP);
            }
            if(logger.isDebugEnabled()) logger.debug("Refresh relationships for " + oid);
            iter = cld.getCollectionDescriptors().iterator();
            while (iter.hasNext())
            {
                cds = (CollectionDescriptor) iter.next();
                if (cds.isRefresh())
                {
                    referencesBroker.retrieveCollection(obj, cld, cds, false);
                }
            }
            iter = cld.getObjectReferenceDescriptors().iterator();
            while (iter.hasNext())
            {
                rds = (ObjectReferenceDescriptor) iter.next();
                if (rds.isRefresh())
                {
                    referencesBroker.retrieveReference(obj, cld, rds, false);
                }
            }
            getInternalCache().disableMaterializationCache();
        }
        catch(RuntimeException e)
        {
            getInternalCache().doLocalClear();
            throw e;
        }
    }

    /**
     * retrieve a collection of type collectionClass matching the Query query
     *
     * @see org.apache.ojb.broker.PersistenceBroker#getCollectionByQuery(Class, Query)
     */
    public ManageableCollection getCollectionByQuery(Class collectionClass, Query query)
            throws PersistenceBrokerException
    {
        return referencesBroker.getCollectionByQuery(collectionClass, query, false);
    }

    /**
     * retrieve a collection of itemClass Objects matching the Query query
     */
    public Collection getCollectionByQuery(Query query) throws PersistenceBrokerException
    {
        return referencesBroker.getCollectionByQuery(query, false);
    }

    /**
     * Retrieve an plain object (without populated references) by it's identity
     * from the database
     *
     * @param cld the real {@link org.apache.ojb.broker.metadata.ClassDescriptor} of the object to refresh
     * @param oid the {@link org.apache.ojb.broker.Identity} of the object
     * @return A new plain object read from the database or <em>null</em> if not found
     * @throws ClassNotPersistenceCapableException
     */
    private Object getPlainDBObject(ClassDescriptor cld, Identity oid) throws ClassNotPersistenceCapableException
    {
        Object newObj = null;

        // Class is NOT an Interface: it has a directly mapped table and we lookup this table first:
        if (!cld.isInterface())
        {
            // 1. try to retrieve skalar fields from directly mapped table columns
            newObj = dbAccess.materializeObject(cld, oid);
        }

        // if we did not find the object yet AND if the cld represents an Extent,
        // we can lookup all tables of the extent classes:
        if (newObj == null && cld.isExtent())
        {
            Iterator extents = getDescriptorRepository().getAllConcreteSubclassDescriptors(cld).iterator();

            while (extents.hasNext())
            {
                ClassDescriptor extCld = (ClassDescriptor) extents.next();

                newObj = dbAccess.materializeObject(extCld, oid);
                if (newObj != null)
                {
                    break;
                }
            }
        }
        return newObj;
    }
    
    
    /**
     * Retrieve an full materialized (dependent on the metadata settings)
     * object by it's identity from the database, as well as caching the
     * object
     *
     * @param oid The {@link org.apache.ojb.broker.Identity} of the object to for
     * @return A new object read from the database or <em>null</em> if not found
     * @throws ClassNotPersistenceCapableException
     */
    private Object getDBObject(Identity oid) throws ClassNotPersistenceCapableException
    {
        Class c = oid.getObjectsRealClass();

        if (c == null)
        {
            logger.info("Real class for used Identity object is 'null', use top-level class instead");
            c = oid.getObjectsTopLevelClass();
        }

        ClassDescriptor cld = getClassDescriptor(c);
        Object newObj = getPlainDBObject(cld, oid);

        // loading references is useful only when the Object could be found in db:
        if (newObj != null)
        {
            if (oid.getObjectsRealClass() == null)
            {
                oid.setObjectsRealClass(newObj.getClass());
            }

            /*
             * synchronize on newObj so the ODMG-layer can take a snapshot only of
             * fully cached (i.e. with all references + collections) objects
             */
            synchronized (newObj)
            {
                objectCache.enableMaterializationCache();
                try
                {
                    // cache object immediately , so that references
                    // can be established from referenced Objects back to this Object
                    objectCache.doInternalCache(oid, newObj, ObjectCacheInternal.TYPE_NEW_MATERIALIZED);

                    /*
                     * Chris Lewington: can cause problems with multiple objects
                     * mapped to one table, as follows:
                     *
                     * if the class searched on does not match the retrieved
                     * class, eg a search on an OID retrieves a row but it could
                     * be a different class (OJB gets all column values),
                     * then trying to resolve references will fail as the object
                     * will not match the Class Descriptor.
                     *
                     * To be safe, get the descriptor of the retrieved object
                     * BEFORE resolving refs
                     */
                    ClassDescriptor newObjCld = getClassDescriptor(newObj.getClass());
                    // don't force loading of references:
                    final boolean unforced = false;

                    // 2. retrieve non-skalar fields that contain objects retrievable from other tables
                    referencesBroker.retrieveReferences(newObj, newObjCld, unforced);
                    // 3. retrieve collection fields from foreign-key related tables:
                    referencesBroker.retrieveCollections(newObj, newObjCld, unforced);
                    objectCache.disableMaterializationCache();
                }
                catch(RuntimeException e)
                {
                    objectCache.doLocalClear();
                    throw e;
                }
            }
        }

        return newObj;
    }

    /**
     * returns an Iterator that iterates Objects of class c if calling the .next()
     * method. The Elements returned come from a SELECT ... WHERE Statement
     * that is defined by the Query query.
     * If itemProxy is null, no proxies are used.
     */
    public Iterator getIteratorByQuery(Query query) throws PersistenceBrokerException
    {
        Class itemClass = query.getSearchClass();
        ClassDescriptor cld = getClassDescriptor(itemClass);
        return getIteratorFromQuery(query, cld);
    }

    /**
     * Get an extent aware Iterator based on the Query
     *
     * @param query
     * @param cld the ClassDescriptor
     * @return OJBIterator
     */
    protected OJBIterator getIteratorFromQuery(Query query, ClassDescriptor cld) throws PersistenceBrokerException
    {
        RsIteratorFactory factory = RsIteratorFactoryImpl.getInstance();
        OJBIterator result = getRsIteratorFromQuery(query, cld, factory);

        if (query.usePaging())
        {
            result = new PagingIterator(result, query.getStartAtIndex(), query.getEndAtIndex());
        }
        return result;
    }

    public Object getObjectByIdentity(Identity id) throws PersistenceBrokerException
    {
        objectCache.enableMaterializationCache();
        Object result = null;
        try
        {
            result = doGetObjectByIdentity(id);
            objectCache.disableMaterializationCache();
        }
        catch(RuntimeException e)
        {
            // catch runtime exc. to guarantee clearing of internal buffer on failure
            objectCache.doLocalClear();
            throw e;
        }
        return result;
    }

    /**
     * Internal used method to retrieve object based on Identity.
     *
     * @param id
     * @return
     * @throws PersistenceBrokerException
     */
    public Object doGetObjectByIdentity(Identity id) throws PersistenceBrokerException
    {
        if (logger.isDebugEnabled()) logger.debug("getObjectByIdentity " + id);

        // check if object is present in ObjectCache:
        Object obj = objectCache.lookup(id);
        // only perform a db lookup if necessary (object not cached yet)
        if (obj == null)
        {
            obj = getDBObject(id);
        }
        else
        {
            ClassDescriptor cld = getClassDescriptor(obj.getClass());
            // if specified in the ClassDescriptor the instance must be refreshed
            if (cld.isAlwaysRefresh())
            {
                refreshInstance(obj, id, cld);
            }
            // now refresh all references
            checkRefreshRelationships(obj, id, cld);
        }

        // Invoke events on PersistenceBrokerAware instances and listeners
        AFTER_LOOKUP_EVENT.setTarget(obj);
        fireBrokerEvent(AFTER_LOOKUP_EVENT);
        AFTER_LOOKUP_EVENT.setTarget(null);

        //logger.info("RETRIEVING object " + obj);
        return obj;
    }

    /**
     * refresh all primitive typed attributes of a cached instance
     * with the current values from the database.
     * refreshing of reference and collection attributes is not done
     * here.
     * @param cachedInstance the cached instance to be refreshed
     * @param oid the Identity of the cached instance
     * @param cld the ClassDescriptor of cachedInstance
     */
    private void refreshInstance(Object cachedInstance, Identity oid, ClassDescriptor cld)
    {
        // read in fresh copy from the db, but do not cache it
        Object freshInstance = getPlainDBObject(cld, oid);

        // update all primitive typed attributes
        FieldDescriptor[] fields = cld.getFieldDescriptions();
        FieldDescriptor fmd;
        PersistentField fld;
        for (int i = 0; i < fields.length; i++)
        {
            fmd = fields[i];
            fld = fmd.getPersistentField();
            fld.set(cachedInstance, fld.get(freshInstance));
        }
    }

    /**
     * retrieve an Object by query
     * I.e perform a SELECT ... FROM ... WHERE ...  in an RDBMS
     */
    public Object getObjectByQuery(Query query) throws PersistenceBrokerException
    {
        Object result = null;
        if (query instanceof QueryByIdentity)
        {
            // example obj may be an entity or an Identity
            Object obj = query.getExampleObject();
            if (obj instanceof Identity)
            {
                Identity oid = (Identity) obj;
                result = getObjectByIdentity(oid);
            }
            else
            {
                // TODO: This workaround doesn't allow 'null' for PK fields
                if (!serviceBrokerHelper().hasNullPKField(getClassDescriptor(obj.getClass()), obj))
                {
                    Identity oid = serviceIdentity().buildIdentity(obj);
                    result = getObjectByIdentity(oid);
                }
            }
        }
        else
        {
            Class itemClass = query.getSearchClass();
            ClassDescriptor cld = getClassDescriptor(itemClass);
            /*
            use OJB intern Iterator, thus we are able to close used
            resources instantly
            */
            OJBIterator it = getIteratorFromQuery(query, cld);
            /*
            arminw:
            patch by Andre Clute, instead of taking the first found result
            try to get the first found none null result.
            He wrote:
            I have a situation where an item with a certain criteria is in my
            database twice -- once deleted, and then a non-deleted version of it.
            When I do a PB.getObjectByQuery(), the RsIterator get's both results
            from the database, but the first row is the deleted row, so my RowReader
            filters it out, and do not get the right result.
            */
            try
            {
                while (result==null && it.hasNext())
                {
                    result = it.next();
                }
            } // make sure that we close the used resources
            finally
            {
                if(it != null) it.releaseDbResources();
            }
        }
        return result;
    }

    /**
     * returns an Enumeration of PrimaryKey Objects for objects of class DataClass.
     * The Elements returned come from a SELECT ... WHERE Statement
     * that is defined by the fields and their coresponding values of listFields
     * and listValues.
     * Useful for EJB Finder Methods...
     * @param primaryKeyClass the pk class for the searched objects
     * @param query the query
     */
    public Enumeration getPKEnumerationByQuery(Class primaryKeyClass, Query query) throws PersistenceBrokerException
    {
        if (logger.isDebugEnabled()) logger.debug("getPKEnumerationByQuery " + query);

        query.setFetchSize(1);
        ClassDescriptor cld = getClassDescriptor(query.getSearchClass());
        return new PkEnumeration(query, cld, primaryKeyClass, this);
    }

    /**
     * Makes object obj persistent in the underlying persistence system.
     * E.G. by INSERT INTO ... or UPDATE ...  in an RDBMS.
     * The ObjectModification parameter can be used to determine whether INSERT or update is to be used.
     * This functionality is typically called from transaction managers, that
     * track which objects have to be stored. If the object is an unmaterialized
     * proxy the method return immediately.
     */
    public void store(Object obj, ObjectModification mod) throws PersistenceBrokerException
    {
        obj = extractObjectToStore(obj);
        // null for unmaterialized Proxy
        if (obj == null)
        {
            return;
        }

        ClassDescriptor cld = getClassDescriptor(obj.getClass());
        // this call ensures that all autoincremented primary key attributes are filled
        Identity oid = serviceIdentity().buildIdentity(cld, obj);
        // select flag for insert / update selection by checking the ObjectModification
        if (mod.needsInsert())
        {
            store(obj, oid, cld, true);
        }
        else if (mod.needsUpdate())
        {
            store(obj, oid, cld, false);
        }
        /*
        arminw
        TODO: Why we need this behaviour? What about 1:1 relations?
        */
        else
        {
            // just store 1:n and m:n associations
            storeCollections(obj, cld, mod.needsInsert());
        }
    }

    /**
     * I pulled this out of internal store so that when doing multiple table
     * inheritance, i can recurse this function.
     *
     * @param obj
     * @param cld
     * @param oid   BRJ: what is it good for ???
     * @param insert
     * @param ignoreReferences
     */
    private void storeToDb(Object obj, ClassDescriptor cld, Identity oid, boolean insert, boolean ignoreReferences)
    {
        // 1. link and store 1:1 references
        storeReferences(obj, cld, insert, ignoreReferences);

        Object[] pkValues = oid.getPrimaryKeyValues();
        if (!serviceBrokerHelper().assertValidPksForStore(cld.getPkFields(), pkValues))
        {
            // BRJ: fk values may be part of pk, but the are not known during
            // creation of Identity. so we have to get them here
            pkValues = serviceBrokerHelper().getKeyValues(cld, obj);
            if (!serviceBrokerHelper().assertValidPksForStore(cld.getPkFields(), pkValues))
            {
                String append = insert ? " on insert" : " on update" ;
                throw new PersistenceBrokerException("assertValidPkFields failed for Object of type: " + cld.getClassNameOfObject() + append);
            }
        }

        // get super class cld then store it with the object
        /*
        now for multiple table inheritance
        1. store super classes, topmost parent first
        2. go down through heirarchy until current class
        3. todo: store to full extent?

// arminw: TODO: The extend-attribute feature dosn't work, should we remove this stuff?
        This if-clause will go up the inheritance heirarchy to store all the super classes.
        The id for the top most super class will be the id for all the subclasses too
         */
        if(cld.getSuperClass() != null)
        {

            ClassDescriptor superCld = getDescriptorRepository().getDescriptorFor(cld.getSuperClass());
            storeToDb(obj, superCld, oid, insert);
            // arminw: why this?? I comment out this section
            // storeCollections(obj, cld.getCollectionDescriptors(), insert);
        }

        // 2. store primitive typed attributes (Or is THIS step 3 ?)
        // if obj not present in db use INSERT
        if (insert)
        {
            dbAccess.executeInsert(cld, obj);
            if(oid.isTransient())
            {
                // Create a new Identity based on the current set of primary key values.
                oid = serviceIdentity().buildIdentity(cld, obj);
            }
        }
        // else use UPDATE
        else
        {
            try
            {
                dbAccess.executeUpdate(cld, obj);
            }
            catch(OptimisticLockException e)
            {
                // ensure that the outdated object be removed from cache
                objectCache.remove(oid);
                throw e;
            }
        }
        // cache object for symmetry with getObjectByXXX()
        // Add the object to the cache.
        objectCache.doInternalCache(oid, obj, ObjectCacheInternal.TYPE_WRITE);
        // 3. store 1:n and m:n associations
        if(!ignoreReferences) storeCollections(obj, cld, insert);
    }

    /**
     * I pulled this out of internal store so that when doing multiple table
     * inheritance, i can recurse this function.
     *
     * @param obj
     * @param cld
     * @param oid   BRJ: what is it good for ???
     * @param insert
     */
    private void storeToDb(Object obj, ClassDescriptor cld, Identity oid, boolean insert)
    {
        storeToDb(obj, cld, oid, insert, false);
    }

    /**
     * returns true if the broker is currently running a transaction.
     * @return boolean
     */
    public boolean isInTransaction()
    {
        // return this.connectionManager.isInLocalTransaction();
        return inTransaction;
    }

    public void setInTransaction(boolean inTransaction)
    {
        this.inTransaction = inTransaction;
    }

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#removeFromCache
     */
    public void removeFromCache(Object objectOrIdentity) throws PersistenceBrokerException
    {
        Identity identity;
        if (objectOrIdentity instanceof Identity)
        {
            identity = (Identity)objectOrIdentity;
        }
        else
        {
            identity = serviceIdentity().buildIdentity(objectOrIdentity);
        }
        objectCache.remove(identity);
    }

    /**
     * returns a ClassDescriptor for the persistence capable class clazz.
     * throws a PersistenceBrokerException if clazz is not persistence capable,
     * i.e. if clazz is not defined in the DescriptorRepository.
     */
    public ClassDescriptor getClassDescriptor(Class clazz) throws PersistenceBrokerException
    {
        return descriptorRepository.getDescriptorFor(clazz);
    }

    public boolean hasClassDescriptor(Class clazz)
    {
        return descriptorRepository.hasDescriptorFor(clazz);
    }

    /**
     * clears the brokers internal cache.
     * removing is recursive. That is referenced Objects are also
     * removed from the cache, if the auto-retrieve flag is set
     * for obj.getClass() in the metadata repository.
     *
     */
    public void clearCache() throws PersistenceBrokerException
    {
        objectCache.clear();
    }

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#getTopLevelClass
     */
    public Class getTopLevelClass(Class clazz) throws PersistenceBrokerException
    {
        try
        {
            return descriptorRepository.getTopLevelClass(clazz);
        }
        catch (ClassNotPersistenceCapableException e)
        {
            throw new PersistenceBrokerException(e);
        }
    }

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#getCount(Query)
     */
    public int getCount(Query query) throws PersistenceBrokerException
    {
        Query countQuery = serviceBrokerHelper().getCountQuery(query);
        Iterator iter;
        int result = 0;

        if (logger.isDebugEnabled()) logger.debug("getCount " + countQuery.getSearchClass() + ", " + countQuery);

        iter = getReportQueryIteratorByQuery(countQuery);
        try
        {
            while (iter.hasNext())
            {
                Object[] row = (Object[]) iter.next();
                result += ((Number) row[0]).intValue();
            }
        }
        finally
        {
            if (iter instanceof OJBIterator)
            {
                ((OJBIterator) iter).releaseDbResources();
            }
        }

        return result;
    }

    /**
     * Get an Iterator based on the ReportQuery
     *
     * @param query
     * @return Iterator
     */
    public Iterator getReportQueryIteratorByQuery(Query query) throws PersistenceBrokerException
    {
        ClassDescriptor cld = getClassDescriptor(query.getSearchClass());
        return getReportQueryIteratorFromQuery(query, cld);
    }

    /**
     * Get an extent aware RsIterator based on the Query
     *
     * @param query
     * @param cld
     * @param factory the Factory for the RsIterator
     * @return OJBIterator
     */
    private OJBIterator getRsIteratorFromQuery(Query query, ClassDescriptor cld, RsIteratorFactory factory)
        throws PersistenceBrokerException
    {
        query.setFetchSize(1);
        if (query instanceof QueryBySQL)
        {
            if(logger.isDebugEnabled()) logger.debug("Creating SQL-RsIterator for class ["+cld.getClassNameOfObject()+"]");
            return factory.createRsIterator((QueryBySQL) query, cld, this);
        }

        if (!cld.isExtent() || !query.getWithExtents())
        {
            // no extents just use the plain vanilla RsIterator
            if(logger.isDebugEnabled()) logger.debug("Creating RsIterator for class ["+cld.getClassNameOfObject()+"]");

            return factory.createRsIterator(query, cld, this);
        }

        if(logger.isDebugEnabled()) logger.debug("Creating ChainingIterator for class ["+cld.getClassNameOfObject()+"]");

        ChainingIterator chainingIter = new ChainingIterator();

        // BRJ: add base class iterator
        if (!cld.isInterface())
        {
            if(logger.isDebugEnabled()) logger.debug("Adding RsIterator for class ["+cld.getClassNameOfObject()+"] to ChainingIterator");

            chainingIter.addIterator(factory.createRsIterator(query, cld, this));
        }

        Iterator extents = getDescriptorRepository().getAllConcreteSubclassDescriptors(cld).iterator();
        while (extents.hasNext())
        {
            ClassDescriptor extCld = (ClassDescriptor) extents.next();

            // read same table only once
            if (chainingIter.containsIteratorForTable(extCld.getFullTableName()))
            {
                if(logger.isDebugEnabled()) logger.debug("Skipping class ["+extCld.getClassNameOfObject()+"]");
            }
            else
            {
                if(logger.isDebugEnabled()) logger.debug("Adding RsIterator of class ["+extCld.getClassNameOfObject()+"] to ChainingIterator");

                // add the iterator to the chaining iterator.
                chainingIter.addIterator(factory.createRsIterator(query, extCld, this));
            }
        }

        return chainingIter;
    }

    /**
     * Get an extent aware Iterator based on the ReportQuery
     *
     * @param query
     * @param cld
     * @return OJBIterator
     */
    private OJBIterator getReportQueryIteratorFromQuery(Query query, ClassDescriptor cld) throws PersistenceBrokerException
    {
        RsIteratorFactory factory = ReportRsIteratorFactoryImpl.getInstance();
        OJBIterator result = getRsIteratorFromQuery(query, cld, factory);

        if (query.usePaging())
        {
            result = new PagingIterator(result, query.getStartAtIndex(), query.getEndAtIndex());
        }

        return result;
    }

    /**
     * @see org.odbms.ObjectContainer#query()
     */
    public org.odbms.Query query()
    {
        return new org.apache.ojb.soda.QueryImpl(this);
    }

    /**
     * @return DescriptorRepository
     */
    public DescriptorRepository getDescriptorRepository()
    {
        return descriptorRepository;
    }

    protected void finalize()
    {
        if (!isClosed)
        {
            close();
        }
    }

    /**
     * clean up the maps for reuse by the next transaction.
     */
    private void clearRegistrationLists()
    {
        nowStoring.clear();
        objectCache.doLocalClear();
        deletedDuringTransaction.clear();
        /*
        arminw:
        for better performance I don't register MtoNBroker as listner,
        so use this method to reset on commit/rollback
        */
        mtoNBroker.reset();
    }
    


    /**
     * @see org.apache.ojb.broker.PersistenceBroker#deleteMtoNImplementor
     */
    public void deleteMtoNImplementor(MtoNImplementor m2nImpl) throws PersistenceBrokerException
    {
        mtoNBroker.deleteMtoNImplementor(m2nImpl);
    }

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#addMtoNImplementor
     */
    public void addMtoNImplementor(MtoNImplementor m2n) throws PersistenceBrokerException
    {
		mtoNBroker.storeMtoNImplementor(m2n);
    }

    public ProxyFactory getProxyFactory() {
        return proxyFactory;
    }
    
    /**
     * Creates a proxy instance.
     * 
     * @param baseClassForProxy  The base class that the Proxy should extend. For dynamic Proxies, the method of 
     *                           generation is dependent on the ProxyFactory implementation.
     * @param realSubjectsIdentity The identity of the subject
     * @return An instance of the proxy subclass
     * @throws PersistenceBrokerException If there is an error creating the proxy object
     */
    public Object createProxy(Class baseClassForProxy, Identity realSubjectsIdentity)
    {
        try
        {
            // the invocation handler manages all delegation stuff
            IndirectionHandler handler     = getProxyFactory().createIndirectionHandler(pbKey, realSubjectsIdentity);

            // the proxy simply provides the interface of the real subject
            if (VirtualProxy.class.isAssignableFrom(baseClassForProxy))
            {
                Constructor constructor = baseClassForProxy.getDeclaredConstructor(new Class[]{ IndirectionHandler.class });
                return constructor.newInstance(new Object[]{ handler });
            }
            else
            {                
                return getProxyFactory().createProxy(baseClassForProxy,handler);
            }

            
        }
        catch (Exception ex)
        {
            throw new PersistenceBrokerException("Unable to create proxy using class:"+baseClassForProxy.getName(), ex);
        }
    }
    
    
}
