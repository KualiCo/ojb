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

import java.io.Serializable;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBrokerException;

/**
 * Proxy base class. can be used to implement lazy materialization techniques.
 * 
 * @author <a href="mailto:thomas.mahler@itellium.com">Thomas Mahler<a>
 * @version $Id: VirtualProxy.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public abstract class VirtualProxy implements OJBProxy,Serializable
{
	static final long serialVersionUID = -3999451313262635171L;

    /**
     * reference to the IndirectionHandler that encapsulates the delegation mechanism
     * */
    private IndirectionHandler indirectionHandler = null;

    /**
     * Creates a new, uninitialized proxy.
     */
    public VirtualProxy()
    {}

    /**
     * Creates a VirtualProxy for the subject with the given identity.
     * 
     * @param key The key of the PersistenceBroker
     * @param oid The identity of the subject
     */
    public VirtualProxy(PBKey key, Identity oid)
    {
        indirectionHandler = AbstractProxyFactory.getProxyFactory().createIndirectionHandler(key, oid);
    }

    /**
     * Create a VirtualProxy that uses the given invocation handler.
     * [tomdz] Why here the use of InvocationHandler ?
     * 
     * @param handler The indirection handler of the proxy
     * 
     */
    
    public VirtualProxy(IndirectionHandler handler)
    {
        indirectionHandler = handler;
    }

    /**
     * Returns the indirection handler of the given proxy.
     * 
     * @param proxy The proxy
     * @return The indirection handler
     */
    public static IndirectionHandler getIndirectionHandler(VirtualProxy proxy)
    {
        return proxy.indirectionHandler;
    }

    /**
     * Determines whether this proxy already has been materialized.
     * 
     * @return <code>true</code> if the real subject already been loaded
     */
    public boolean alreadyMaterialized()
    {
        return indirectionHandler.alreadyMaterialized();
    }


    /**
     * Returns the proxies real subject. The subject will be materialized if necessary.
     * 
     * @return The subject
     */
    public Object getRealSubject() throws PersistenceBrokerException
    {
        return indirectionHandler.getRealSubject();
    }
    
    /**
     * Returns the IndirectionHandler
     * 
     * @return The indirectionHandler
     */
    public IndirectionHandler getIndirectionHandler()
    {
        return indirectionHandler; 
    }

    /**
     * Returns the identity of the subject.
     * 
     * @return The identity
     */
    Identity getIdentity()
    {
        return indirectionHandler.getIdentity();
    }
    
	public Object writeReplace()
	{
		return this; 
	}
    

}
