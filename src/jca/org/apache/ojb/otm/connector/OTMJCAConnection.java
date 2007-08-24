package org.apache.ojb.otm.connector;

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
import org.apache.ojb.broker.cache.ObjectCache;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.odmg.oql.EnhancedOQLQuery;
import org.apache.ojb.otm.EditingContext;
import org.apache.ojb.otm.OTMConnection;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.lock.LockingException;
import org.odmg.OQLQuery;

import java.util.Collection;
import java.util.Iterator;

/**
 *
 * Wraps the OTMConnection and associates/disassociates the connection
 * handle.
 *
 * kudos to David Jencks for inspiration, and pointers.
 *
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird<a>
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 */

public class OTMJCAConnection implements OTMConnection
{
	private OTMJCAManagedConnection m_managedConnection;
	private boolean m_closed = false;

	public OTMJCAConnection(OTMJCAManagedConnection mc)
	{
		Util.log("In OTMJCAConnection");
		this.m_managedConnection = mc;
	}

	void setManagedConnection(OTMJCAManagedConnection managedConnection)
	{
		m_managedConnection = managedConnection;
	}

	public void makePersistent(Object o) throws LockingException
	{
		isValidUnderlyingConnection();
		m_managedConnection.getConnection().makePersistent(o);
	}

	public void deletePersistent(Object o) throws LockingException
	{
		isValidUnderlyingConnection();
		m_managedConnection.getConnection().deletePersistent(o);
	}

	public void lockForWrite(Object o) throws LockingException
	{
		isValidUnderlyingConnection();
		m_managedConnection.getConnection().lockForWrite(o);
	}

	public Object getObjectByIdentity(Identity identity) throws LockingException
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().getObjectByIdentity(identity);
	}

	public Object getObjectByIdentity(Identity identity, int i) throws LockingException
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().getObjectByIdentity(identity, i);
	}

	public Iterator getIteratorByQuery(Query query)
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().getIteratorByQuery(query);
	}

	public Iterator getIteratorByQuery(Query query, int i)
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().getIteratorByQuery(query, i);
	}

	public Iterator getIteratorByOQLQuery(OQLQuery query)
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().getIteratorByOQLQuery(query);
	}

	public Iterator getIteratorByOQLQuery(OQLQuery query, int lock)
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().getIteratorByOQLQuery(query, lock);
	}

    public Collection getCollectionByQuery(Query query, int lock)
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().getCollectionByQuery(query, lock);
	}

    public Collection getCollectionByQuery(Query query)
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().getCollectionByQuery(query);
	}

    public Identity getIdentity(Object o)
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().getIdentity(o);
	}

	public ClassDescriptor getDescriptorFor(Class aClass)
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().getDescriptorFor(aClass);
	}

	public EditingContext getEditingContext()
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().getEditingContext();
	}

	public void invalidate(Identity identity) throws LockingException
	{
		isValidUnderlyingConnection();
		m_managedConnection.getConnection().invalidate(identity);
	}

	public void invalidateAll() throws LockingException
	{
		isValidUnderlyingConnection();
		m_managedConnection.getConnection().invalidateAll();
	}

	public EnhancedOQLQuery newOQLQuery()
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().newOQLQuery();
	}

	public EnhancedOQLQuery newOQLQuery(int lock)
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().newOQLQuery(lock);
	}

	public int getCount(Query query)
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().getCount(query);
	}

    public void refresh(Object object)
	{
		isValidUnderlyingConnection();
		m_managedConnection.getConnection().refresh(object);
	}

    public void close()
	{
		m_closed = true;
		if (m_managedConnection != null)
		{
			m_managedConnection.closeHandle(this);
		}
		m_managedConnection = null;
	}

	public boolean isClosed()
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().isClosed();
	}

	public ObjectCache serviceObjectCache()
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection().serviceObjectCache();
	}

	private void isValidUnderlyingConnection() throws OTMConnectionRuntimeException
	{
		if (m_closed)
		{
			throw new OTMConnectionRuntimeException("OTMConnection handle is closed and unusable.");
		}
		if (m_managedConnection == null)
		{
			throw new OTMConnectionRuntimeException("Connection handle is not currently associated with a ManagedConnection");
		}
	}

	OTMConnection getConnection()
	{
		isValidUnderlyingConnection();
		return m_managedConnection.getConnection();
	}

    public Transaction getTransaction()
    {
        return this.m_managedConnection.getTransaction();
    }

    public void setTransaction(Transaction t)
    {
        this.m_managedConnection.setTransaction(t);
    }
}
