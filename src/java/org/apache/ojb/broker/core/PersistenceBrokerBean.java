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

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.IdentityFactory;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.MtoNImplementor;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PBLifeCycleEvent;
import org.apache.ojb.broker.PBListener;
import org.apache.ojb.broker.PBStateEvent;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerEvent;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerInternal;
import org.apache.ojb.broker.TransactionAbortedException;
import org.apache.ojb.broker.TransactionInProgressException;
import org.apache.ojb.broker.TransactionNotInProgressException;
import org.apache.ojb.broker.accesslayer.ConnectionManagerIF;
import org.apache.ojb.broker.accesslayer.JdbcAccess;
import org.apache.ojb.broker.accesslayer.StatementManagerIF;
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
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.sequence.SequenceManager;

/**
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 *
 * The PersistenceBrokerBean wraps a persistenceBroker implementation and allows
 * PersistenceBroker server operations with communication happening over RMI.
 * Useful if you are going to use OJB in a J2EE environment.
 *
 * Allows for OJB objects with proxies to be taken outside of the VM, say to an
 * instance of a servlet container, and the proxies will call back on the bean
 * to materialize proxies via standard RMI bean calls, instead of the custom
 * protocol.
 *
 * Container will be responsible for pooling of bean instances.
 *
 * Can be used by normal EJB clients, not just the PersistenceBrokerClient
 *
 * @ejb:bean
 *             type="Stateless"
 *             name="PersistenceBrokerBean"
 *             jndi-name="org.apache.ojb.broker.core.PersistenceBrokerBean"
 *             local-jndi-name="ojb.PersistenceBrokerBean"
 *             view-type="both"
 *             transaction-type="Container"
 *
 * @ejb:interface
 *         remote-class="org.apache.ojb.broker.server.PersistenceBrokerRemote"
 *         local-class="org.apache.ojb.broker.server.PersistenceBrokerLocal"
 *         extends="javax.ejb.EJBObject, org.apache.ojb.broker.PersistenceBroker"
 *
 * @ejb:home
 *         remote-class="org.apache.ojb.broker.server.PersistenceBrokerHome"
 *         local-class="org.apache.ojb.broker.server.PersistenceBrokerLocalHome"
 *         extends="javax.ejb.EJBHome"
 *
 * @ejb:transaction
 *         type="Required"
 *
 * @ejb:env-entry
 *         name="ojb.repository"
 *         type="java.lang.String"
 *         value="repository.xml"
 *
 */

public class PersistenceBrokerBean implements PersistenceBroker, SessionBean
{
	private Logger m_log;
	private SessionContext m_ctx;
	private String m_ojbRepository;
	private PBKey m_pbKey;
	private PersistenceBrokerFactoryIF m_pbf;

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#getDescriptorRepository()
	 */
	public DescriptorRepository getDescriptorRepository()
	{
		return getBroker().getDescriptorRepository();
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#getPBKey()
	 */
	public PBKey getPBKey()
	{
		return getBroker().getPBKey();
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#delete(Object)
	 */
	public void delete(Object obj) throws PersistenceBrokerException
	{
		getBroker().delete(obj);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#deleteByQuery(Query)
	 */
	public void deleteByQuery(Query query) throws PersistenceBrokerException
	{
		getBroker().deleteByQuery(query);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#removeFromCache(Object)
	 */
	public void removeFromCache(Object obj) throws PersistenceBrokerException
	{
		getBroker().removeFromCache(obj);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#clearCache()
	 */
	public void clearCache() throws PersistenceBrokerException
	{
		getBroker().clearCache();
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#store(Object)
	 */
	public void store(Object obj) throws PersistenceBrokerException
	{
		getBroker().store(obj);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#abortTransaction()
	 */
	public void abortTransaction() throws TransactionNotInProgressException
	{
		getBroker().abortTransaction();
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#beginTransaction()
	 */
	public void beginTransaction()
			throws TransactionInProgressException, TransactionAbortedException
	{
		getBroker().beginTransaction();
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#commitTransaction()
	 */
	public void commitTransaction()
			throws TransactionNotInProgressException, TransactionAbortedException
	{
		getBroker().commitTransaction();
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#isInTransaction()
	 */
	public boolean isInTransaction() throws PersistenceBrokerException
	{
		return getBroker().isInTransaction();
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#close()
	 */
	public boolean close()
	{
		return getBroker().close();
	}

    public boolean isClosed()
    {
        return getBroker().isClosed();
    }

    /**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#getCollectionByQuery(Query)
	 */
	public Collection getCollectionByQuery(Query query)
			throws PersistenceBrokerException
	{
		return getBroker().getCollectionByQuery(query);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#getCount(Query)
	 */
	public int getCount(Query query) throws PersistenceBrokerException
	{
		return getBroker().getCount(query);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#getCollectionByQuery(Class, Query)
	 */
	public ManageableCollection getCollectionByQuery(
			Class collectionClass,
			Query query)
			throws PersistenceBrokerException
	{
		return getBroker().getCollectionByQuery(collectionClass, query);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#getIteratorByQuery(Query)
	 */
	public Iterator getIteratorByQuery(Query query)
			throws PersistenceBrokerException
	{
		return getBroker().getIteratorByQuery(query);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#getReportQueryIteratorByQuery(Query)
	 */
	public Iterator getReportQueryIteratorByQuery(Query query)
			throws PersistenceBrokerException
	{
		return getBroker().getReportQueryIteratorByQuery(query);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#getObjectByIdentity(Identity)
	 */
	public Object getObjectByIdentity(Identity id)
			throws PersistenceBrokerException
	{
		return getBroker().getObjectByIdentity(id);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#getObjectByQuery(Query)
	 */
	public Object getObjectByQuery(Query query)
			throws PersistenceBrokerException
	{
		return getBroker().getObjectByQuery(query);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#getPKEnumerationByQuery(Class, Query)
	 */
	public Enumeration getPKEnumerationByQuery(
			Class PrimaryKeyClass,
			Query query)
			throws PersistenceBrokerException
	{
		return getBroker().getPKEnumerationByQuery(PrimaryKeyClass, query);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#store(Object, ObjectModification)
	 */
	public void store(Object obj, ObjectModification modification)
			throws PersistenceBrokerException
	{
		getBroker().store(obj, modification);
	}


	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#getClassDescriptor(Class)
	 */
	public ClassDescriptor getClassDescriptor(Class clazz)
			throws PersistenceBrokerException
	{
		return getBroker().getClassDescriptor(clazz);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#hasClassDescriptor(Class)
	 */
	public boolean hasClassDescriptor(Class clazz)
	{
		return getBroker().hasClassDescriptor(clazz);
	}


	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#getTopLevelClass(Class)
	 */
	public Class getTopLevelClass(Class clazz) throws PersistenceBrokerException
	{
		return getBroker().getTopLevelClass(clazz);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#serviceStatementManager()
	 */
	public StatementManagerIF serviceStatementManager()
	{
		return getBroker().serviceStatementManager();
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#serviceConnectionManager()
	 */
	public ConnectionManagerIF serviceConnectionManager()
	{
		return getBroker().serviceConnectionManager();
	}

	public JdbcAccess serviceJdbcAccess()
	{
		return getBroker().serviceJdbcAccess();
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#serviceSqlGenerator()
	 */
	public SqlGenerator serviceSqlGenerator()
	{
		return getBroker().serviceSqlGenerator();
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
    
    

	public ProxyFactory getProxyFactory() {
        return getBroker().getProxyFactory();
    }
    
    public Object createProxy(Class proxyClass, Identity realSubjectsIdentity) {
        return getBroker().createProxy(proxyClass, realSubjectsIdentity);
    }

    /**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#addListener(PBListener)
	 */
	public void addListener(PBListener listener) throws PersistenceBrokerException
	{
		getBroker().addListener(listener);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#addListener(PBListener, boolean)
	 */
	public void addListener(PBListener listener, boolean permanent) throws PersistenceBrokerException
	{
		getBroker().addListener(listener, permanent);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#removeListener(PBListener)
	 */
	public void removeListener(PBListener listener) throws PersistenceBrokerException
	{
		getBroker().removeListener(listener);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#retrieveAllReferences(Object)
	 */
	public void retrieveAllReferences(Object pInstance)
			throws PersistenceBrokerException
	{
		getBroker().retrieveAllReferences(pInstance);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#retrieveReference(Object, String)
	 */
	public void retrieveReference(Object pInstance, String pAttributeName)
			throws PersistenceBrokerException
	{
		getBroker().retrieveReference(pInstance, pAttributeName);
	}

	public void removeAllListeners(boolean permanent) throws PersistenceBrokerException
	{
		getBroker().removeAllListeners(permanent);
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.PersistenceBroker#removeAllListeners()
	 */
	public void removeAllListeners() throws PersistenceBrokerException
	{
		getBroker().removeAllListeners();
	}

	/**
	 * @ejb:interface-method
	 * @see org.apache.ojb.broker.util.configuration.Configurable#configure(Configuration)
	 */
	public void configure(Configuration pConfig)
			throws ConfigurationException
	{
		getBroker().configure(pConfig);
	}

	/**
	 * @ejb:interface-method
	 * @see org.odbms.ObjectContainer#query()
	 */
	public org.odbms.Query query()
	{
		return getBroker().query();
	}

	private void ojbPrepare()
	{
		if (m_log.isDebugEnabled()) m_log.info("PersistenceBrokerBean: ejbActivate was called");
		Context context = null;
		// Lookup if a environment entry for repository exists
		String ojbRepository = null;
		try
		{
			context = new InitialContext();
			ojbRepository = (String) context.lookup("java:comp/env/ojb.repository");
		}
		catch (NamingException e)
		{
			m_log.error("Lookup for ojb repository failed", e);
		}
		// no repository found in environment, use default one
		if (ojbRepository == null || ojbRepository.equals(""))
		{
			m_log.info("No enviroment entry was found, use default repository");
			ojbRepository = "repository.xml";
		}
		m_log.info("Use OJB repository file: " + ojbRepository);
		m_pbKey = new PBKey(ojbRepository);

		// Lookup the PBF implementation
		try
		{
			context = new InitialContext();
			m_pbf = ((PBFactoryIF) context.lookup(PBFactoryIF.PBFACTORY_JNDI_NAME)).getInstance();
		}
		catch (NamingException e)
		{
			m_log.error("Lookup for PersistenceBrokerFactory failed", e);
			throw new PersistenceBrokerException(e);
		}
	}

	public void ejbActivate() throws EJBException, RemoteException
	{
		m_log = LoggerFactory.getLogger(PersistenceBrokerBean.class);
		ojbPrepare();
	}

	public void ejbPassivate() throws EJBException, RemoteException
	{
		m_log = null;
		m_pbf = null;
	}

	public void ejbRemove() throws EJBException, RemoteException
	{
		m_ctx = null;
	}

	public void setSessionContext(SessionContext sessionContext) throws EJBException, RemoteException
	{
		m_ctx = sessionContext;
	}

	private PersistenceBrokerInternal getBroker()
	{
		return m_pbf.createPersistenceBroker(m_pbKey);
	}

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#deleteMtoNImplementor()
     */
    public void deleteMtoNImplementor(MtoNImplementor m2nImpl) throws PersistenceBrokerException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#addMtoNImplementor()
     */
    public void addMtoNImplementor(MtoNImplementor m2nImpl) throws PersistenceBrokerException
    {
        throw new UnsupportedOperationException();
    }
    
    
}
