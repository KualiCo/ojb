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

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.MtoNImplementor;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PBListener;
import org.apache.ojb.broker.PBState;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerEvent;
import org.apache.ojb.broker.PBLifeCycleEvent;
import org.apache.ojb.broker.PBStateEvent;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.TransactionAbortedException;
import org.apache.ojb.broker.TransactionInProgressException;
import org.apache.ojb.broker.TransactionNotInProgressException;
import org.apache.ojb.broker.IdentityFactory;
import org.apache.ojb.broker.PersistenceBrokerInternal;
import org.apache.ojb.broker.accesslayer.ConnectionManagerIF;
import org.apache.ojb.broker.accesslayer.JdbcAccess;
import org.apache.ojb.broker.accesslayer.StatementManagerIF;
import org.apache.ojb.broker.accesslayer.RelationshipPrefetcherFactory;
import org.apache.ojb.broker.accesslayer.sql.SqlGenerator;
import org.apache.ojb.broker.cache.ObjectCache;
import org.apache.ojb.broker.core.proxy.ProxyFactory;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.ObjectModification;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.sequence.SequenceManager;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Delegating implementation of a PersistenceBroker.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: DelegatingPersistenceBroker.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class DelegatingPersistenceBroker implements PersistenceBrokerInternal, PBState
{
	protected PersistenceBrokerInternal m_broker;

	public DelegatingPersistenceBroker(PersistenceBrokerInternal broker)
	{
		this.m_broker = broker;
	}

	/**
	 * All delegated method use this method to
	 * get the wrapped broker.
	 */
	protected PersistenceBrokerInternal getBroker()
	{
		if (m_broker != null)
        {
			return m_broker;
        }
		else
        {
            throw new IllegalStateException(
                    "This PersistenceBroker instance is already closed and no longer useable." +
                    " It's not possible to re-use a closed instance.");
        }

	}

	/**
	 * Returns only the wrapped
	 * {@link org.apache.ojb.broker.PersistenceBroker} instance
	 */
	public PersistenceBrokerInternal getDelegate()
	{
		return this.m_broker;
	}

	public void setDelegate(PersistenceBrokerInternal broker)
	{
		this.m_broker = broker;
	}

	/**
	 * If my underlying {@link org.apache.ojb.broker.PersistenceBroker}
	 * is not a {@link DelegatingPersistenceBroker}, returns it,
	 * otherwise recursively invokes this method on my delegate.
	 * <p>
	 * Hence this method will return the first
	 * delegate that is not a {@link DelegatingPersistenceBroker},
	 * or <tt>null</tt> when no non-{@link DelegatingPersistenceBroker}
	 * delegate can be found by transversing this chain.
	 * <p>
	 * This method is useful when you may have nested
	 * {@link DelegatingPersistenceBroker}s, and you want to make
	 * sure to obtain a "genuine" {@link org.apache.ojb.broker.PersistenceBroker}
	 * implementaion instance.
	 */
	public PersistenceBroker getInnermostDelegate()
	{
		PersistenceBroker broker = this.m_broker;
		while (broker != null && broker instanceof DelegatingPersistenceBroker)
		{
			broker = ((DelegatingPersistenceBroker) broker).getDelegate();
			if (this == broker)
			{
				return null;
			}
		}
		return broker;
	}

    public boolean isManaged()
    {
        return m_broker.isManaged();
    }

    public void setManaged(boolean managed)
    {
        m_broker.setManaged(managed);
    }

    public QueryReferenceBroker getReferenceBroker()
    {
        return m_broker.getReferenceBroker();
    }

    public void checkRefreshRelationships(Object obj, Identity oid, ClassDescriptor cld)
    {
        m_broker.checkRefreshRelationships(obj, oid, cld);
    }

    public RelationshipPrefetcherFactory getRelationshipPrefetcherFactory()
    {
        return m_broker.getRelationshipPrefetcherFactory();
    }

    public void store(Object obj, Identity oid, ClassDescriptor cld, boolean insert, boolean ignoreReferences)
    {
        m_broker.store(obj, oid, cld, insert, ignoreReferences);
    }

    public void delete(Object obj, boolean ignoreReferences) throws PersistenceBrokerException
    {
        m_broker.delete(obj, ignoreReferences);
    }

    public boolean isInTransaction() throws PersistenceBrokerException
    {
        return m_broker != null && getBroker().isInTransaction();
    }

	public boolean isClosed()
	{
		return m_broker == null || getBroker().isClosed();
	}

	public void setClosed(boolean closed)
	{
		((PBState) getBroker()).setClosed(closed);
	}

	public void beginTransaction()
			throws TransactionInProgressException, TransactionAbortedException
	{
		getBroker().beginTransaction();
	}

	public void commitTransaction()
			throws TransactionNotInProgressException, TransactionAbortedException
	{
		getBroker().commitTransaction();
	}

	public void abortTransaction() throws TransactionNotInProgressException
	{
		getBroker().abortTransaction();
	}

	public boolean close()
	{
		return getBroker().close();
	}

	public SqlGenerator serviceSqlGenerator()
	{
		return getBroker().serviceSqlGenerator();
	}

	public JdbcAccess serviceJdbcAccess()
	{
		return getBroker().serviceJdbcAccess();
	}

	public void delete(Object obj) throws PersistenceBrokerException
	{
		getBroker().delete(obj);
	}

	public void store(Object obj) throws PersistenceBrokerException
	{
		getBroker().store(obj);
	}

	public void store(Object obj,
					  ObjectModification modification) throws PersistenceBrokerException
	{
		getBroker().store(obj, modification);
	}

	public PBKey getPBKey()
	{
		return getBroker().getPBKey();
	}

	public void removeFromCache(Object obj) throws PersistenceBrokerException
	{
		getBroker().removeFromCache(obj);
	}

	public void clearCache() throws PersistenceBrokerException
	{
		getBroker().clearCache();
	}

	public DescriptorRepository getDescriptorRepository()
	{
		return getBroker().getDescriptorRepository();
	}

	public void removeAllListeners() throws PersistenceBrokerException
	{
		getBroker().removeAllListeners();
	}

	public void removeAllListeners(boolean permanent) throws PersistenceBrokerException
	{
		getBroker().removeAllListeners(permanent);
	}

	public void retrieveReference(Object pInstance, String pAttributeName) throws PersistenceBrokerException
	{
		getBroker().retrieveReference(pInstance, pAttributeName);
	}

	public void retrieveAllReferences(Object pInstance) throws PersistenceBrokerException
	{
		getBroker().retrieveAllReferences(pInstance);
	}

	public ConnectionManagerIF serviceConnectionManager()
	{
		return getBroker().serviceConnectionManager();
	}

	public StatementManagerIF serviceStatementManager()
	{
		return getBroker().serviceStatementManager();
	}

	public SequenceManager serviceSequenceManager()
	{
		return getBroker().serviceSequenceManager();
	}

	public BrokerHelper serviceBrokerHelper()
	{
		return getBroker().serviceBrokerHelper();
	}

	public ObjectCache serviceObjectCache()
	{
		return getBroker().serviceObjectCache();
	}

    public IdentityFactory serviceIdentity()
    {
        return getBroker().serviceIdentity();
    }

	public void fireBrokerEvent(PersistenceBrokerEvent event)
	{
		getBroker().fireBrokerEvent(event);
	}

	public void fireBrokerEvent(PBLifeCycleEvent event)
	{
		getBroker().fireBrokerEvent(event);
	}

	public void fireBrokerEvent(PBStateEvent event)
	{
		getBroker().fireBrokerEvent(event);
	}

	public void addListener(PBListener listener) throws PersistenceBrokerException
	{
		getBroker().addListener(listener);
	}

	public void addListener(PBListener listener, boolean permanent) throws PersistenceBrokerException
	{
		getBroker().addListener(listener, permanent);
	}

	public void removeListener(PBListener listener) throws PersistenceBrokerException
	{
        //do nothing	    
	}

	public Class getTopLevelClass(Class clazz) throws PersistenceBrokerException
	{
		return getBroker().getTopLevelClass(clazz);
	}

	public boolean hasClassDescriptor(Class clazz)
	{
		return getBroker().hasClassDescriptor(clazz);
	}

	public ClassDescriptor getClassDescriptor(Class clazz) throws PersistenceBrokerException
	{
		return getBroker().getClassDescriptor(clazz);
	}

	public Enumeration getPKEnumerationByQuery(Class primaryKeyClass,
											   Query query) throws PersistenceBrokerException
	{
		return getBroker().getPKEnumerationByQuery(primaryKeyClass, query);
	}

	public Object getObjectByQuery(Query query) throws PersistenceBrokerException
	{
		return getBroker().getObjectByQuery(query);
	}

	public Object getObjectByIdentity(Identity id) throws PersistenceBrokerException
	{
		return getBroker().getObjectByIdentity(id);
	}

	public Iterator getReportQueryIteratorByQuery(Query query) throws PersistenceBrokerException
	{
		return getBroker().getReportQueryIteratorByQuery(query);
	}

	public Iterator getIteratorByQuery(Query query) throws PersistenceBrokerException
	{
		return getBroker().getIteratorByQuery(query);
	}

	public ManageableCollection getCollectionByQuery(Class collectionClass, Query query)
			throws PersistenceBrokerException
	{
		return getBroker().getCollectionByQuery(collectionClass, query);
	}

	public int getCount(Query query) throws PersistenceBrokerException
	{
		return getBroker().getCount(query);
	}

	public Collection getCollectionByQuery(Query query) throws PersistenceBrokerException
	{
		return getBroker().getCollectionByQuery(query);
	}

	public void configure(Configuration pConfig) throws ConfigurationException
	{
		getBroker().configure(pConfig);
	}

	public org.odbms.Query query()
	{
		return getBroker().query();
	}

	public void deleteByQuery(Query query) throws PersistenceBrokerException
	{
		getBroker().deleteByQuery(query);
	}

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#deleteMtoNImplementor
     */
    public void deleteMtoNImplementor(MtoNImplementor m2nImpl) throws PersistenceBrokerException
    {
        getBroker().deleteMtoNImplementor(m2nImpl);
    }

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#addMtoNImplementor
     */
    public void addMtoNImplementor(MtoNImplementor m2nImpl) throws PersistenceBrokerException
    {
        getBroker().addMtoNImplementor(m2nImpl);
    }

    public ProxyFactory getProxyFactory() 
    {
       return getBroker().getProxyFactory();
    }

    public Object createProxy(Class proxyClass, Identity realSubjectsIdentity) {
        return getBroker().createProxy(proxyClass, realSubjectsIdentity);
    }
    
    
    

}
