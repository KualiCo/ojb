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

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import java.io.Serializable;

/**
 * @author matthew.baird
 *
 */
public class OTMConnectionManager
		implements ConnectionManager, Serializable
{

	public OTMConnectionManager()
	{
		Util.log("In OTMConnectionManager");
	}

	public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo info)
			throws ResourceException
	{
		Util.log("In OTMConnectionManager.allocateConnection");
		ManagedConnection mc = mcf.createManagedConnection(null, info);
		return mc.getConnection(null, info);
	}
}
