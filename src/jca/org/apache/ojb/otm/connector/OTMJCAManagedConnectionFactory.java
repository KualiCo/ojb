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

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.otm.Kit;
import org.apache.ojb.otm.OTMConnection;
import org.apache.ojb.otm.kit.SimpleKit;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.DriverManager;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird<a>
 */

public class OTMJCAManagedConnectionFactory
		implements ManagedConnectionFactory, Serializable
{
	private Kit m_kit;

	private synchronized void initialize()
	{
		if (m_kit == null)
		{
			m_kit = SimpleKit.getInstance();
		}
	}

	public Kit getKit() throws ResourceException
	{
		initialize();
		return m_kit;
	}

	public OTMJCAManagedConnectionFactory()
	{
		Util.log("In OTMJCAManagedConnectionFactory.constructor");
	}

	public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException
	{
		Util.log("In OTMJCAManagedConnectionFactory.createConnectionFactory,1");
		return new JCAKit(this, cxManager);
	}

	public Object createConnectionFactory() throws ResourceException
	{
		Util.log("In OTMJCAManagedConnectionFactory.createManagedFactory,2");
		return new JCAKit(this, null);
	}

	/**
	 * return a new managed connection. This connection is wrapped around the real connection and delegates to it
	 * to get work done.
	 * @param subject
	 * @param info
	 * @return
	 */
	public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo info)
	{
		Util.log("In OTMJCAManagedConnectionFactory.createManagedConnection");
		try
		{
			Kit kit = getKit();
			PBKey key = ((OTMConnectionRequestInfo) info).getPbKey();
			OTMConnection connection = kit.acquireConnection(key);
			return new OTMJCAManagedConnection(this, connection, key);
		}
		catch (ResourceException e)
		{
			throw new OTMConnectionRuntimeException(e.getMessage());
		}
	}

	public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)
			throws ResourceException
	{
		Util.log("OTMJCAManagedConnectionFactory::matchManagedConnections called with " + connectionSet.size() + " connections.");
		for (Iterator i = connectionSet.iterator(); i.hasNext();)
		{
			Object o = i.next();
			if (o instanceof OTMJCAManagedConnection)
			{
				// all idle connections are identical
				return (OTMJCAManagedConnection) o;
			}
		}
		Util.log("OTMJCAManagedConnectionFactory::No matched connections");
		return null;
	}

	public void setLogWriter(PrintWriter out) throws ResourceException
	{
		Util.log("In OTMJCAManagedConnectionFactory.setLogWriter");
	}

	public PrintWriter getLogWriter() throws ResourceException
	{
		Util.log("In OTMJCAManagedConnectionFactory.getLogWriter");
		return DriverManager.getLogWriter();
	}

	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		if (obj instanceof OTMJCAManagedConnectionFactory)
		{
			int hash1 = ((OTMJCAManagedConnectionFactory) obj).hashCode();
			int hash2 = hashCode();
			return hash1 == hash2;
		}
		else
		{
			return false;
		}
	}

	public int hashCode()
	{
		return 1;
	}
}
