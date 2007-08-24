package org.apache.ojb.broker.core.proxy;

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

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * @author andrew.clute
 * Simple class that is serializable, and containes references to an IndirectionHandler
 * and the base class. When the object is unserialized, it then generates a new Proxy 
 * back in it's place.
 *
 */
public class OJBSerializableProxy implements Serializable
{

	private static final long serialVersionUID = 568312334450175549L;
	private Logger logger = LoggerFactory.getLogger(OJBSerializableProxy.class);

	private Class classObject;

	private IndirectionHandler indirectionHandler;

	public OJBSerializableProxy(Class proxyClass, IndirectionHandler indirectionHandler)
	{
		this.classObject = proxyClass;
		this.indirectionHandler = indirectionHandler;
	}

	private Object readResolve() throws ObjectStreamException
	{
		try
		{
			return ProxyHelper.getProxyFactory().createProxy(classObject, indirectionHandler);
		} catch (Throwable e)
		{
			//Fail gracefully -- there a bunch of reasons why we cannot create the Proxy, and
			//so we just want to put a null into the reference.
			logger.warn("Unable to create a new Proxy of type '" + classObject.getName() + "' due to a '" + e.getClass().getName() + "'.");
			return null;
		}
	}

}
