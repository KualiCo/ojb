package org.apache.ojb.broker.prevayler;

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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.MtoNImplementor;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.TransactionAbortedException;
import org.apache.ojb.broker.TransactionInProgressException;
import org.apache.ojb.broker.TransactionNotInProgressException;
import org.apache.ojb.broker.core.PersistenceBrokerFactoryIF;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByIdentity;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.ObjectModification;
import org.prevayler.Command;
import org.prevayler.Prevayler;
import org.prevayler.implementation.SnapshotPrevayler;


/**
 * An OJB PersistenBroker implementation working against a datastore
 * that is persisted by Prevayler.
 * So in other word, this is an OODBMS with a PB API.
 * Of course you can use OJB/ODMG or OJB/JDO on top of it.
 * 
 * important note: this implementation is not finished. 
 * Do not try to use it in production environments.
 * 
 * @author Thomas Mahler
 */
public class PBPrevaylerImpl extends PersistenceBrokerImpl
{

	private transient Database db;
	
	private Prevayler prevayler;

	private ArrayList commandLog = new ArrayList(100);
	
	private boolean inTransaction = false;


    /**
     * Constructor for PBPrevaylerImpl.
     * @param key
     * @param pbf
     */
    public PBPrevaylerImpl(PBKey key, PersistenceBrokerFactoryIF pbf)
    {
    	super(key, pbf);
        refresh();
        if(key == null) throw new PersistenceBrokerException("Could not instantiate broker with PBKey 'null'");
        this.pbf = pbf;
        this.setPBKey(key);

        brokerHelper = new BrokerHelper(this);
        //connectionManager = ConnectionManagerFactory.getInstance().createConnectionManager(this);
        //objectCache = ObjectCacheFactory.getInstance().createObjectCache(this);
        //sequenceManager = SequenceManagerFactory.getSequenceManager(this);
        //dbAccess = JdbcAccessFactory.getInstance().createJdbcAccess(this);
        //statementManager = StatementManagerFactory.getInstance().createStatementManager(this);
        //sqlGenerator = SqlGeneratorFactory.getInstance().createSqlGenerator(connectionManager.getSupportedPlatform());

        //markedForDelete = new ArrayList();        
        try
        {
             prevayler = new SnapshotPrevayler(new Database(), "PrevalenceBase" + File.separator + "Database");
             db = (Database) prevayler.system();	
             db.setBroker(this);
        }
        catch (Exception e)
        {
        }
    }


	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#abortTransaction()
	 */
	public void abortTransaction() throws TransactionNotInProgressException
	{
		if (! isInTransaction())
		{
			throw new TransactionNotInProgressException();
		}
		inTransaction = false;
		commandLog.clear();		
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#beginTransaction()
	 */
	public void beginTransaction()
		throws TransactionInProgressException, TransactionAbortedException
	{
		if (this.isInTransaction())
		{
			throw new TransactionInProgressException();
		}
		inTransaction = true;
		commandLog.clear();
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#commitTransaction()
	 */
    public void commitTransaction()
        throws TransactionNotInProgressException, TransactionAbortedException
    {
        if (!isInTransaction())
        {
            throw new TransactionNotInProgressException();
        }

        Iterator iter = commandLog.iterator();
        try
        {
            while (iter.hasNext())
            {
                Command cmd = (Command) iter.next();
                prevayler.executeCommand(cmd);
            }
        }
        catch (Exception e)
        {
            this.abortTransaction();
        }
        inTransaction = false;
        commandLog.clear();
    }

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#isInTransaction()
	 */
	public boolean isInTransaction() throws PersistenceBrokerException
	{
		return inTransaction;
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#close()
	 */
	public boolean close()
	{
        if (isInTransaction())
        {
            abortTransaction();
        }
		try
        {
            ((SnapshotPrevayler)prevayler).takeSnapshot();
        }
        catch (IOException e)
        {
        }
        setClosed(true);
		return true;
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#clearCache()
	 */
	public void clearCache() throws PersistenceBrokerException
	{
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#removeFromCache(Object)
	 */
	public void removeFromCache(Object obj) throws PersistenceBrokerException
	{
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#store(Object, ObjectModification)
	 */
	public void store(Object obj, ObjectModification modification)
		throws PersistenceBrokerException
	{
		this.store(obj);
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#store(Object)
	 */
	public void store(Object obj) throws PersistenceBrokerException
	{
		if (! (obj instanceof Serializable))
		{
			throw new PersistenceBrokerException(obj.getClass().getName() + "does not implement java.io.Serializable.");
		}
		
		CommandStore cmd = new CommandStore(obj);
		commandLog.add(cmd);		
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#delete(Object)
	 */
	public void delete(Object obj) throws PersistenceBrokerException
	{
		Command cmd = new CommandDelete(obj);
		commandLog.add(cmd);
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#deleteMtoNImplementor(MtoNImplementor)
	 */
	public void deleteMtoNImplementor(MtoNImplementor m2nImpl)
		throws PersistenceBrokerException
	{
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#addMtoNImplementor(MtoNImplementor)
	 */
	public void addMtoNImplementor(MtoNImplementor m2nImpl)
		throws PersistenceBrokerException
	{
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#deleteByQuery(Query)
	 */
	public void deleteByQuery(Query query) throws PersistenceBrokerException
	{
		throw new PersistenceBrokerException("not yet implemented");
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#retrieveAllReferences(Object)
	 */
	public void retrieveAllReferences(Object pInstance)
		throws PersistenceBrokerException
	{
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#retrieveReference(Object, String)
	 */
	public void retrieveReference(Object pInstance, String pAttributeName)
		throws PersistenceBrokerException
	{
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#getCount(Query)
	 */
	public int getCount(Query query) throws PersistenceBrokerException
	{
		throw new PersistenceBrokerException("not yet implemented");
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#getCollectionByQuery(Query)
	 */
	public Collection getCollectionByQuery(Query query)
		throws PersistenceBrokerException
	{
		// needs some more work ;-)
		return db.getTable().values();
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#getCollectionByQuery(Class, Query)
	 */
	public ManageableCollection getCollectionByQuery(
		Class collectionClass,
		Query query)
		throws PersistenceBrokerException
	{
		throw new PersistenceBrokerException("not yet implemented");
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#getIteratorByQuery(Query)
	 */
	public Iterator getIteratorByQuery(Query query)
		throws PersistenceBrokerException
	{
		throw new PersistenceBrokerException("not yet implemented");
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#getReportQueryIteratorByQuery(Query)
	 */
	public Iterator getReportQueryIteratorByQuery(Query query)
		throws PersistenceBrokerException
	{
		throw new PersistenceBrokerException("not yet implemented");
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#getObjectByIdentity(Identity)
	 */
	public Object getObjectByIdentity(Identity id)
		throws PersistenceBrokerException
	{
		return db.lookupObjectByIdentity(id);
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#getObjectByQuery(Query)
	 */
	public Object getObjectByQuery(Query query)
		throws PersistenceBrokerException
	{
		if (query instanceof QueryByIdentity)
		{
			Object id = ((QueryByIdentity) query).getExampleObject();
			if (! (id instanceof Identity))
			{
				id = new Identity(id,PersistenceBrokerFactory.defaultPersistenceBroker());	
			}			
			Identity oid = (Identity) id;
			return db.lookupObjectByIdentity(oid);
		}
		else
		{
			throw new PersistenceBrokerException("not yet implemented");	
		}
	}

	/**
	 * @see org.apache.ojb.broker.PersistenceBroker#getPKEnumerationByQuery(Class, Query)
	 */
	public Enumeration getPKEnumerationByQuery(
		Class PrimaryKeyClass,
		Query query)
		throws PersistenceBrokerException
	{
		throw new PersistenceBrokerException("not yet implemented");
	}


}
