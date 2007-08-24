package org.apache.ojb.broker;

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

import org.apache.ojb.broker.accesslayer.RelationshipPrefetcherFactory;
import org.apache.ojb.broker.core.QueryReferenceBroker;
import org.apache.ojb.broker.core.proxy.ProxyFactory;
import org.apache.ojb.broker.metadata.ClassDescriptor;

/**
 * Extended version of the {@link PersistenceBroker} specifying additional functionality
 * that is only relevant internally. 
 *
 * @author Armin Waibel
 * @version $Id: PersistenceBrokerInternal.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public interface PersistenceBrokerInternal extends PersistenceBroker
{
    /**
     * Determines whether this instance is handled by a managed
     * environment, i.e. whether it is registered within a JTA transaction.
     * 
     * @return <code>true</code> if this broker is managed
     */
    public boolean isManaged();

    /**
     * Specifies whether this instance is handled by a managed
     * environment, i.e. whether it is registered within a JTA transaction.
     * Note that on {@link #close()} this will automatically be reset
     * to <em>false</em>.
     * 
     * @param managed <code>true</code> if this broker is managed
     */
    public void setManaged(boolean managed);

    /**
     * Performs the real store work (insert or update) and is intended for use by
     * top-level apis internally.
     *
     * @param obj              The object to store
     * @param oid              The identity of the object to store
     * @param cld              The class descriptor of the object
     * @param insert           If <em>true</em> an insert operation will be performed, else update
     *                         operation
     * @param ignoreReferences Whether automatic storing of contained references/collections (except
     *                         super-references) shall be suppressed (independent of the auto-update
     *                         setting in the metadata)
     */
    public void store(Object obj, Identity oid, ClassDescriptor cld, boolean insert, boolean ignoreReferences);

    /**
     * Deletes the persistence representation of the given object in the underlying
     * persistence system. This method is intended for use in top-level apis internally.
     *
     * @param obj              The object to delete
     * @param ignoreReferences Whether automatic deletion of contained references/collections (except
     *                         super-references) shall be suppressed (independent of the auto-delete
     *                         setting in the metadata)
     */
    public void delete(Object obj, boolean ignoreReferences) throws PersistenceBrokerException;

    /**
     * Returns the broker specifically for retrieving references via query.
     * 
     * @return The query reference broker
     */
    public QueryReferenceBroker getReferenceBroker();
    
    /**
     * Refreshes the references of the given object whose <code>refresh</code>
     * is set to <code>true</code>.
     *
     * @param obj The object to check
     * @param oid The identity of the object
     * @param cld The class descriptor for the object
     */
    public void checkRefreshRelationships(Object obj, Identity oid, ClassDescriptor cld);

    /**
     * Return the factory for creating relationship prefetcher objects.
     * 
     * @return The factory
     */
    public RelationshipPrefetcherFactory getRelationshipPrefetcherFactory();

    /**
     * Return the factory for creating proxies.
     * 
     * @return The factory
     */
    public ProxyFactory getProxyFactory();
    
    /**
     * Shortcut method for creating a proxy of the given type.
     * 
     * @param proxyClass           The proxy type
     * @param realSubjectsIdentity The identity of the real subject
     * @return The proxy
     */
    public Object createProxy(Class proxyClass, Identity realSubjectsIdentity);
}
