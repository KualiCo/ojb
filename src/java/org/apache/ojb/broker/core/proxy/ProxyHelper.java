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

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.ref.Reference;

import org.apache.ojb.broker.PBFactoryException;

/**
 * ProxyHelper used to get the real thing behind a proxy
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: ProxyHelper.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public class ProxyHelper
{
    private static Reference proxyFactoryRef;

    public synchronized static ProxyFactory getProxyFactory()
    {
        if((proxyFactoryRef == null) || (proxyFactoryRef.get() == null))
        {
            try
            {
                /*
                TODO: Check usage of WeakReference
                arminw: Changed Soft- to WeakReference. If in AbstractProxyFactory the
                the ProxyFactory instance is freed this class will take care of that.
                */
                proxyFactoryRef = new WeakReference(AbstractProxyFactory.getProxyFactory());
            }
            catch(PBFactoryException ex)
            {
                // seems we cannot get a broker; in that case we're defaulting to the CGLib proxy factory
                // (which also works for older JDKs) ie. for broker-less mode (only metadata)
                return new ProxyFactoryCGLIBImpl();
            }
        }
        return (ProxyFactory) proxyFactoryRef.get();
    }

    /**
     * Get the real Object
     *
     * @param objectOrProxy
     * @return Object
     */
    public static final Object getRealObject(Object objectOrProxy)
    {
        return getProxyFactory().getRealObject(objectOrProxy);
    }

    /**
     * Get the real Object for already materialized Handler
     *
     * @param objectOrProxy
     * @return Object or null if the Handel is not materialized
     */
    public static final Object getRealObjectIfMaterialized(Object objectOrProxy)
    {
        return getProxyFactory().getRealObjectIfMaterialized(objectOrProxy);
    }

    /**
     * Get the real Class
     *
     * @param objectOrProxy
     * @return Class
     */
    public static final Class getRealClass(Object objectOrProxy)
    {
        return getProxyFactory().getRealClass(objectOrProxy);
    }

    /**
     * Determines whether the given object is an OJB proxy.
     *
     * @return <code>true</code> if the object is an OJB proxy
     */
    public static boolean isNormalOjbProxy(Object proxyOrObject)
    {
        return getProxyFactory().isNormalOjbProxy(proxyOrObject);
    }

    /**
     * Determines whether the given object is an OJB virtual proxy.
     *
     * @return <code>true</code> if the object is an OJB virtual proxy
     */
    public static boolean isVirtualOjbProxy(Object proxyOrObject)
    {
        return getProxyFactory().isVirtualOjbProxy(proxyOrObject);
    }

    /**
     * Returns <tt>true</tt> if the given object is a {@link java.lang.reflect.Proxy}
     * or a {@link VirtualProxy} instance.
     */
    public static boolean isProxy(Object proxyOrObject)
    {
        return getProxyFactory().isProxy(proxyOrObject);
    }

    /**
     * Returns the invocation handler object of the given proxy object.
     *
     * @param obj The object
     * @return The invocation handler if the object is an OJB proxy, or <code>null</code>
     *         otherwise
     */
    public static IndirectionHandler getIndirectionHandler(Object obj)
    {
        return getProxyFactory().getIndirectionHandler(obj);
    }

    /**
     * Determines whether the object is a materialized object, i.e. no proxy or a
     * proxy that has already been loaded from the database.
     *
     * @param object The object to test
     * @return <code>true</code> if the object is materialized
     */
    public static boolean isMaterialized(Object object)
    {
        return getProxyFactory().isMaterialized(object);
    }

    /**
     * Materialization-safe version of toString. If the object is a yet-unmaterialized proxy,
     * then only the text "unmaterialized proxy for ..." is returned and the proxy is NOT
     * materialized. Otherwise, the normal toString method is called. This useful e.g. for
     * logging etc.
     *
     * @param object The object for which a string representation shall be generated
     * @return The string representation
     */
    public static String toString(Object object)
    {
        return getProxyFactory().toString(object);
    }

    /** Return CollectionProxy for item is item is a CollectionProxy, otherwise return null */
    public static CollectionProxy getCollectionProxy(Object item)
    {
        return getProxyFactory().getCollectionProxy(item);
    }

    /** Reports if item is a CollectionProxy. */
    public static boolean isCollectionProxy(Object item)
    {
        // TODO: Provide handling for pluggable collection proxy implementations
        return getProxyFactory().isCollectionProxy(item);
    }
}
