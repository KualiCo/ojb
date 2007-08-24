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

/**
 * Declares a configuration for he indirection handler factory.
 *
 * @see org.apache.ojb.broker.core.proxy.ProxyFactory 
 * @author <a href="mailto:tomdz@apache.org">Thomas Dudziak<a>
 */
public interface ProxyConfiguration
{
    /**
     * Returns the indirection handler implementation class.
     * 
     * @return The indirection handler class
     * @see org.apache.ojb.broker.core.proxy.IndirectionHandler
     */
    Class getIndirectionHandlerClass();

    /**
     * Returns the collection proxy class for collection classes that implement the {@link java.util.List}
     * interface.
     * 
     * @return The proxy class
     * @see org.apache.ojb.broker.core.proxy.ProxyFactory#setListProxyClass(Class)
     */
    Class getListProxyClass();

    /**
     * Returns the collection proxy class for collection classes that implement the {@link java.util.Set}
     * interface.
     * 
     * @return The proxy class
     * @see org.apache.ojb.broker.core.proxy.ProxyFactory#setSetProxyClass(Class)
     */
    Class getSetProxyClass();

    /**
     * Returns the collection proxy class for generic collection classes that implement the
     * {@link java.util.Collection} interface.
     * 
     * @return The proxy class
     * @see org.apache.ojb.broker.core.proxy.ProxyFactory#setCollectionProxyClass(Class)
     */
    Class getCollectionProxyClass();
    
    /**
     * Returns the proxy factory class 
     * 
     * @return The proxy class
     */
    Class getProxyFactoryClass();
    
    
}
