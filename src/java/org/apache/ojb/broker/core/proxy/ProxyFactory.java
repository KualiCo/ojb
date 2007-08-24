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
import java.util.Set;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.query.Query;

/**
 * Factory class for creating instances of the indirection handler used by OJB's proxies, and
 * for the collection proxies.
 *
 * @author <a href="mailto:tomdz@apache.org">Thomas Dudziak<a>
 * @version $Id: ProxyFactory.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public interface ProxyFactory extends Serializable
{
    /**
     * Returns the indirection handler class.
     * 
     * @return The class for indirection handlers
     */
    public abstract Class getIndirectionHandlerClass();

    /**
     * Sets the indirection handler class.
     * 
     * @param indirectionHandlerClass The class for indirection handlers
     */
    public abstract void setIndirectionHandlerClass(Class indirectionHandlerClass);
    
    /**
     * Returns the class of a default IndirectionHandler that can be used for this implementaiton
     * if now IndirectionHandlerClass implementation is given.
     * 
     */
    public abstract Class getDefaultIndirectionHandlerClass();
    
    
    /**
     * Returns the class of the base class that the given IndirectionHandler must extend/implement
     * 
     */
    public abstract Class getIndirectionHandlerBaseClass();
    

    /**
     * Creates a new indirection handler instance.
     * 
     * @param persistenceConf The persistence configuration
     * @param id              The subject's ids
     * @return The new instance
     */
    public abstract IndirectionHandler createIndirectionHandler(PBKey pbKey, Identity id);

    /**
     * Returns the list proxy class.
     * 
     * @return The class used for list proxies
     */
    public abstract Class getListProxyClass();

    /**
     * Dets the proxy class to use for collection classes that implement the {@link java.util.List} interface.
     * Notes that the proxy class must implement the {@link java.util.List} interface, and have a constructor
     * of the signature ({@link org.apache.ojb.broker.PBKey}, {@link java.lang.Class}, {@link org.apache.ojb.broker.query.Query}).
     *
     * @param listProxyClass The proxy class
     */
    public abstract void setListProxyClass(Class listProxyClass);

    /**
     * Returns the set proxy class.
     * 
     * @return The class used for set proxies
     */
    public abstract Class getSetProxyClass();

    /**
     * Dets the proxy class to use for collection classes that implement the {@link Set} interface.
     *
     * @param setProxyClass The proxy class
     */
    public abstract void setSetProxyClass(Class setProxyClass);

    /**
     * Returns the collection proxy class.
     * 
     * @return The class used for collection proxies
     */
    public abstract Class getCollectionProxyClass();

    /**
     * Dets the proxy class to use for generic collection classes implementing the {@link java.util.Collection} interface.
     *
     * @param collectionProxyClass The proxy class
     */
    public abstract void setCollectionProxyClass(Class collectionProxyClass);

    /**
     * Create a Collection Proxy for a given context.
     * 
     * @param persistenceConf The persistence configuration that the proxy will be bound to
     * @param context         The creation context
     * @return The collection proxy
     */
    public abstract ManageableCollection createCollectionProxy(PBKey brokerKey, Query query, Class collectionClass);
    
    
    public OJBProxy createProxy(Class baseClass, IndirectionHandler handler) throws Exception;
    
    
    /**
     * Get the real Object
     *
     * @param objectOrProxy
     * @return Object
     */
    public Object getRealObject(Object objectOrProxy);

    /**
     * Get the real Object for already materialized Handler
     *
     * @param objectOrProxy
     * @return Object or null if the Handel is not materialized
     */
    public Object getRealObjectIfMaterialized(Object objectOrProxy);

    /**
     * Get the real Class
     *
     * @param objectOrProxy
     * @return Class
     */
    public Class getRealClass(Object objectOrProxy);
    /**
     * Determines whether the given object is an OJB proxy.
     * 
     * @return <code>true</code> if the object is an OJB proxy
     */
    public boolean isNormalOjbProxy(Object proxyOrObject);

    /**
     * Determines whether the given object is an OJB virtual proxy.
     * 
     * @return <code>true</code> if the object is an OJB virtual proxy
     */
    public boolean isVirtualOjbProxy(Object proxyOrObject);

    /**
     * Returns <tt>true</tt> if the given object is a {@link java.lang.reflect.Proxy}
     * or a {@link VirtualProxy} instance.
     */
    public boolean isProxy(Object proxyOrObject);

    /**
     * Returns the invocation handler object of the given proxy object.
     * 
     * @param obj The object
     * @return The invocation handler if the object is an OJB proxy, or <code>null</code>
     *         otherwise
     */
    public IndirectionHandler getIndirectionHandler(Object obj);

    /**
     * Determines whether the object is a materialized object, i.e. no proxy or a
     * proxy that has already been loaded from the database.
     *   
     * @param object The object to test
     * @return <code>true</code> if the object is materialized
     */
    public boolean isMaterialized(Object object);

    
    /**
     * Return CollectionProxy for item is item is a CollectionProxy, otherwise return null
     */
    public CollectionProxy getCollectionProxy(Object item);

    /**
     * Reports if item is a CollectionProxy.
     *
     * TODO: Provide handling for pluggable collection proxy implementations
     */
    public boolean isCollectionProxy(Object item);
    
    /**
     * Materialization-safe version of toString. If the object is a yet-unmaterialized proxy,
     * then only the text "unmaterialized proxy for ..." is returned and the proxy is NOT
     * materialized. Otherwise, the normal toString method is called. This useful e.g. for
     * logging etc.
     * 
     * @param object The object for which a string representation shall be generated
     * @return The string representation
     */
    public String toString(Object proxy);
    
    /**
     * Method that returns whether or not this ProxyFactory can generate reference Proxies
     * for classes regardless if they extend an interface or not.
     * 
     */
    boolean interfaceRequiredForProxyGeneration();
}
