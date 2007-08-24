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
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.otm.Kit;
import org.apache.ojb.otm.OTMConnection;
import org.apache.ojb.otm.copy.ObjectCopyStrategy;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.lock.map.LockMap;
import org.apache.ojb.otm.lock.wait.LockWaitStrategy;
import org.apache.ojb.otm.swizzle.Swizzling;

import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;
import java.io.Serializable;

/**
 * represents the Kit used for JCA
 *
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird<a>
 */

public class JCAKit implements Kit, Serializable, Referenceable
{
	private OTMJCAManagedConnectionFactory m_managedConnectionFactory;
	private ConnectionManager m_connectionManager;
	private Reference m_reference;

	public JCAKit(ManagedConnectionFactory mcf, ConnectionManager cm)
	{
		Util.log("In JCAKit");
		m_managedConnectionFactory = (OTMJCAManagedConnectionFactory) mcf;

		if (cm == null)
			m_connectionManager = new OTMConnectionManager();
		else
			m_connectionManager = cm;
	}

	private Kit getKit()
	{
		try
		{
			return m_managedConnectionFactory.getKit();
		}
		catch (ResourceException e)
		{
			throw new OTMConnectionRuntimeException(e);
		}
	}

	/**
	 * Kit implementation
	 */
	public OTMConnection acquireConnection(PBKey pbkey)
	{
		Util.log("In JCAKit.getConnection,1");
		try
		{
			OTMConnectionRequestInfo info = new OTMConnectionRequestInfo(pbkey);
			return (OTMConnection) m_connectionManager.allocateConnection(m_managedConnectionFactory, info);
		}
		catch (ResourceException ex)
		{
			throw new OTMConnectionRuntimeException(ex);
		}
	}

	public Transaction getTransaction(OTMConnection otmConnection)
	{
		if (otmConnection instanceof OTMJCAConnection)
		{
			return getKit().getTransaction(((OTMJCAConnection)otmConnection).getConnection());
		}
		else
			return getKit().getTransaction(otmConnection);
	}

	public Swizzling getSwizzlingStrategy()
	{
		return getKit().getSwizzlingStrategy();
	}

	public LockWaitStrategy getLockWaitStrategy()
	{
		return getKit().getLockWaitStrategy();
	}

	public LockMap getLockMap()
	{
		return getKit().getLockMap();
	}

	public ObjectCopyStrategy getCopyStrategy(Identity identity)
	{
		return getKit().getCopyStrategy(identity);
	}


	public boolean isImplicitLockingUsed()
	{
		return getKit().isImplicitLockingUsed();
	}

	/**
	 * Referenceable implementation
	 */

	public void setReference(Reference reference)
	{
		this.m_reference = reference;
	}

	public Reference getReference()
	{
		return m_reference;
	}
}
