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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.metadata.MetadataException;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Abstract implementation for the ProxyFactory
 *
 * @author andrew.clute
 * @version $Id:
 */
public abstract class AbstractProxyFactory implements ProxyFactory
{
    /*
    arminw:
    Do we need a serializable ProxyFactory implementation? If yes, we have to fix this class.
    TODO: ProxyFactory is declared as Serializable but Constructor class is not, but used without transient keyword.
    */

    private static Logger log = LoggerFactory.getLogger(AbstractProxyFactory.class);
    private static transient ProxyFactory singleton;

    /** The indirection handler class */
    private Class _indirectionHandlerClass;
    /** The constructor used for creating indirection handler instances (shortcut) */
    private transient Constructor _indirectionHandlerConstructor;
    /** The constructor used for creating list proxies */
    private Constructor _listProxyConstructor;
    /** The constructor used for creating set proxies */
    private Constructor _setProxyConstructor;
    /** The constructor used for creating collection proxies */
    private Constructor _collectionProxyConstructor;

    private static ProxyConfiguration getProxyConfiguration()
    {
        return (ProxyConfiguration) OjbConfigurator.getInstance().getConfigurationFor(null);
    }

    /**
     * Returns the constructor of the indirection handler class.
     *
     * @return The constructor for indirection handlers
     */
    private synchronized Constructor getIndirectionHandlerConstructor()
    {
        if(_indirectionHandlerConstructor == null)
        {
            Class[] paramType = {PBKey.class, Identity.class};

            try
            {
                _indirectionHandlerConstructor = getIndirectionHandlerClass().getConstructor(paramType);
            }
            catch(NoSuchMethodException ex)
            {
                throw new MetadataException("The class "
                        + _indirectionHandlerClass.getName()
                        + " specified for IndirectionHandlerClass"
                        + " is required to have a public constructor with signature ("
                        + PBKey.class.getName()
                        + ", "
                        + Identity.class.getName()
                        + ").");
            }
        }
        return _indirectionHandlerConstructor;
    }

    /**
     * Returns the indirection handler class.
     *
     * @return The class for indirection handlers
     */
    public Class getIndirectionHandlerClass()
    {
        if(_indirectionHandlerClass == null)
        {
            setIndirectionHandlerClass(getProxyConfiguration().getIndirectionHandlerClass());
        }

        return _indirectionHandlerClass;
    }

    /**
     * Sets the indirection handler class.
     *
     * @param indirectionHandlerClass The class for indirection handlers
     */
    public void setIndirectionHandlerClass(Class indirectionHandlerClass)
    {
        if(indirectionHandlerClass == null)
        {
            //throw new MetadataException("No IndirectionHandlerClass specified.");
            /**
             * andrew.clute
             * Allow the default IndirectionHandler for the given ProxyFactory implementation
             * when the parameter is not given
             */
            indirectionHandlerClass = getDefaultIndirectionHandlerClass();
        }
        if(indirectionHandlerClass.isInterface()
                || Modifier.isAbstract(indirectionHandlerClass.getModifiers())
                || !getIndirectionHandlerBaseClass().isAssignableFrom(indirectionHandlerClass))
        {
            throw new MetadataException("Illegal class "
                    + indirectionHandlerClass.getName()
                    + " specified for IndirectionHandlerClass. Must be a concrete subclass of "
                    + getIndirectionHandlerBaseClass().getName());
        }
        _indirectionHandlerClass = indirectionHandlerClass;
    }

    /**
     * Creates a new indirection handler instance.
     *
     * @param brokerKey The associated {@link PBKey}.
     * @param id The subject's ids
     * @return The new instance
     */
    public IndirectionHandler createIndirectionHandler(PBKey brokerKey, Identity id)
    {
        Object args[] = {brokerKey, id};

        try
        {
            return (IndirectionHandler) getIndirectionHandlerConstructor().newInstance(args);
        }
        catch(InvocationTargetException ex)
        {
            throw new PersistenceBrokerException("Exception while creating a new indirection handler instance", ex);
        }
        catch(InstantiationException ex)
        {
            throw new PersistenceBrokerException("Exception while creating a new indirection handler instance", ex);
        }
        catch(IllegalAccessException ex)
        {
            throw new PersistenceBrokerException("Exception while creating a new indirection handler instance", ex);
        }
    }

    /**
     * Retrieves the constructor that is used by OJB to create instances of the given collection proxy
     * class.
     *
     * @param proxyClass The proxy class
     * @param baseType The required base type of the proxy class
     * @param typeDesc The type of collection proxy
     * @return The constructor
     */
    private static Constructor retrieveCollectionProxyConstructor(Class proxyClass, Class baseType, String typeDesc)
    {
        if(proxyClass == null)
        {
            throw new MetadataException("No " + typeDesc + " specified.");
        }
        if(proxyClass.isInterface() || Modifier.isAbstract(proxyClass.getModifiers()) || !baseType.isAssignableFrom(proxyClass))
        {
            throw new MetadataException("Illegal class "
                    + proxyClass.getName()
                    + " specified for "
                    + typeDesc
                    + ". Must be a concrete subclass of "
                    + baseType.getName());
        }

        Class[] paramType = {PBKey.class, Class.class, Query.class};

        try
        {
            return proxyClass.getConstructor(paramType);
        }
        catch(NoSuchMethodException ex)
        {
            throw new MetadataException("The class "
                    + proxyClass.getName()
                    + " specified for "
                    + typeDesc
                    + " is required to have a public constructor with signature ("
                    + PBKey.class.getName()
                    + ", "
                    + Class.class.getName()
                    + ", "
                    + Query.class.getName()
                    + ").");
        }
    }

    /**
     * Returns the list proxy class.
     *
     * @return The class used for list proxies
     */
    public Class getListProxyClass()
    {
        return getListProxyConstructor().getDeclaringClass();
    }

    /**
     * Returns the constructor of the list proxy class.
     *
     * @return The constructor for list proxies
     */
    private Constructor getListProxyConstructor()
    {
        if(_listProxyConstructor == null)
        {
            setListProxyClass(getProxyConfiguration().getListProxyClass());
        }
        return _listProxyConstructor;
    }

    /**
     * Dets the proxy class to use for collection classes that implement the {@link java.util.List} interface.
     * Notes that the proxy class must implement the {@link java.util.List} interface, and have a constructor
     * of the signature ({@link org.apache.ojb.broker.PBKey}, {@link java.lang.Class}, {@link org.apache.ojb.broker.query.Query}).
     *
     * @param listProxyClass The proxy class
     */
    public void setListProxyClass(Class listProxyClass)
    {
        _listProxyConstructor = retrieveCollectionProxyConstructor(listProxyClass, List.class, "ListProxyClass");
    }

    /**
     * Returns the set proxy class.
     *
     * @return The class used for set proxies
     */
    public Class getSetProxyClass()
    {
        return getSetProxyConstructor().getDeclaringClass();
    }

    /**
     * Returns the constructor of the set proxy class.
     *
     * @return The constructor for set proxies
     */
    private Constructor getSetProxyConstructor()
    {
        if(_setProxyConstructor == null)
        {
            setSetProxyClass(getProxyConfiguration().getSetProxyClass());
        }
        return _setProxyConstructor;
    }

    /**
     * Dets the proxy class to use for collection classes that implement the {@link Set} interface.
     *
     * @param setProxyClass The proxy class
     */
    public void setSetProxyClass(Class setProxyClass)
    {
        _setProxyConstructor = retrieveCollectionProxyConstructor(setProxyClass, Set.class, "SetProxyClass");
    }

    /**
     * Returns the collection proxy class.
     *
     * @return The class used for collection proxies
     */
    public Class getCollectionProxyClass()
    {
        return getCollectionProxyConstructor().getDeclaringClass();
    }

    /**
     * Returns the constructor of the generic collection proxy class.
     *
     * @return The constructor for collection proxies
     */
    private Constructor getCollectionProxyConstructor()
    {
        if(_collectionProxyConstructor == null)
        {
            setCollectionProxyClass(getProxyConfiguration().getCollectionProxyClass());
        }
        return _collectionProxyConstructor;
    }

    /**
     * Dets the proxy class to use for generic collection classes implementing the {@link java.util.Collection} interface.
     *
     * @param collectionProxyClass The proxy class
     */
    public void setCollectionProxyClass(Class collectionProxyClass)
    {
        _collectionProxyConstructor = retrieveCollectionProxyConstructor(collectionProxyClass, Collection.class, "CollectionProxyClass");
        // we also require the class to be a subclass of ManageableCollection
        if(!ManageableCollection.class.isAssignableFrom(collectionProxyClass))
        {
            throw new MetadataException("Illegal class "
                    + collectionProxyClass.getName()
                    + " specified for CollectionProxyClass. Must be a concrete subclass of "
                    + ManageableCollection.class.getName());
        }
    }

    /**
     * Determines which proxy to use for the given collection class (list, set or generic collection proxy).
     *
     * @param collectionClass The collection class
     * @return The constructor of the proxy class
     */
    private Constructor getCollectionProxyConstructor(Class collectionClass)
    {
        if(List.class.isAssignableFrom(collectionClass))
        {
            return getListProxyConstructor();
        }
        else if(Set.class.isAssignableFrom(collectionClass))
        {
            return getSetProxyConstructor();
        }
        else
        {
            return getCollectionProxyConstructor();
        }
    }

    /**
     * Create a Collection Proxy for a given query.
     *
     * @param brokerKey The key of the persistence broker
     * @param query The query
     * @param collectionClass The class to build the proxy for
     * @return The collection proxy
     */
    public ManageableCollection createCollectionProxy(PBKey brokerKey, Query query, Class collectionClass)
    {
        Object args[] = {brokerKey, collectionClass, query};

        try
        {
            return (ManageableCollection) getCollectionProxyConstructor(collectionClass).newInstance(args);
        }
        catch(InstantiationException ex)
        {
            throw new PersistenceBrokerException("Exception while creating a new collection proxy instance", ex);
        }
        catch(InvocationTargetException ex)
        {
            throw new PersistenceBrokerException("Exception while creating a new collection proxy instance", ex);
        }
        catch(IllegalAccessException ex)
        {
            throw new PersistenceBrokerException("Exception while creating a new collection proxy instance", ex);
        }
    }

    /**
     * Get the real Object
     *
     * @param objectOrProxy
     * @return Object
     */
    public final Object getRealObject(Object objectOrProxy)
    {
        if(isNormalOjbProxy(objectOrProxy))
        {
            String msg;

            try
            {
                return getIndirectionHandler(objectOrProxy).getRealSubject();
            }
            catch(ClassCastException e)
            {
                // shouldn't happen but still ...
                msg = "The InvocationHandler for the provided Proxy was not an instance of " + IndirectionHandler.class.getName();
                log.error(msg);
                throw new PersistenceBrokerException(msg, e);
            }
            catch(IllegalArgumentException e)
            {
                msg = "Could not retrieve real object for given Proxy: " + objectOrProxy;
                log.error(msg);
                throw new PersistenceBrokerException(msg, e);
            }
            catch(PersistenceBrokerException e)
            {
                log.error("Could not retrieve real object for given Proxy: " + objectOrProxy);
                throw e;
            }
        }
        else if(isVirtualOjbProxy(objectOrProxy))
        {
            try
            {
                return ((VirtualProxy) objectOrProxy).getRealSubject();
            }
            catch(PersistenceBrokerException e)
            {
                log.error("Could not retrieve real object for VirtualProxy: " + objectOrProxy);
                throw e;
            }
        }
        else
        {
            return objectOrProxy;
        }
    }

    /**
     * Get the real Object for already materialized Handler
     *
     * @param objectOrProxy
     * @return Object or null if the Handel is not materialized
     */
    public Object getRealObjectIfMaterialized(Object objectOrProxy)
    {
        if(isNormalOjbProxy(objectOrProxy))
        {
            String msg;

            try
            {
                IndirectionHandler handler = getIndirectionHandler(objectOrProxy);

                return handler.alreadyMaterialized() ? handler.getRealSubject() : null;
            }
            catch(ClassCastException e)
            {
                // shouldn't happen but still ...
                msg = "The InvocationHandler for the provided Proxy was not an instance of " + IndirectionHandler.class.getName();
                log.error(msg);
                throw new PersistenceBrokerException(msg, e);
            }
            catch(IllegalArgumentException e)
            {
                msg = "Could not retrieve real object for given Proxy: " + objectOrProxy;
                log.error(msg);
                throw new PersistenceBrokerException(msg, e);
            }
            catch(PersistenceBrokerException e)
            {
                log.error("Could not retrieve real object for given Proxy: " + objectOrProxy);
                throw e;
            }
        }
        else if(isVirtualOjbProxy(objectOrProxy))
        {
            try
            {
                VirtualProxy proxy = (VirtualProxy) objectOrProxy;

                return proxy.alreadyMaterialized() ? proxy.getRealSubject() : null;
            }
            catch(PersistenceBrokerException e)
            {
                log.error("Could not retrieve real object for VirtualProxy: " + objectOrProxy);
                throw e;
            }
        }
        else
        {
            return objectOrProxy;
        }
    }

    /**
     * Get the real Class
     *
     * @param objectOrProxy
     * @return Class
     */
    public Class getRealClass(Object objectOrProxy)
    {
        IndirectionHandler handler;

        if(isNormalOjbProxy(objectOrProxy))
        {
            String msg;

            try
            {
                handler = getIndirectionHandler(objectOrProxy);
                /*
                 arminw:
                 think we should return the real class
                 */
                // return handler.getIdentity().getObjectsTopLevelClass();
                return handler.getIdentity().getObjectsRealClass();
            }
            catch(ClassCastException e)
            {
                // shouldn't happen but still ...
                msg = "The InvocationHandler for the provided Proxy was not an instance of " + IndirectionHandler.class.getName();
                log.error(msg);
                throw new PersistenceBrokerException(msg, e);
            }
            catch(IllegalArgumentException e)
            {
                msg = "Could not retrieve real object for given Proxy: " + objectOrProxy;
                log.error(msg);
                throw new PersistenceBrokerException(msg, e);
            }
        }
        else if(isVirtualOjbProxy(objectOrProxy))
        {
            handler = VirtualProxy.getIndirectionHandler((VirtualProxy) objectOrProxy);
            /*
             arminw:
             think we should return the real class
             */
            // return handler.getIdentity().getObjectsTopLevelClass();
            return handler.getIdentity().getObjectsRealClass();
        }
        else
        {
            return objectOrProxy.getClass();
        }
    }

    /**
     * Determines whether the given object is an OJB proxy.
     *
     * @return <code>true</code> if the object is an OJB proxy
     */
    public boolean isNormalOjbProxy(Object proxyOrObject)
    {
        return proxyOrObject instanceof OJBProxy;
    }

    /**
     * Determines whether the given object is an OJB virtual proxy.
     *
     * @return <code>true</code> if the object is an OJB virtual proxy
     */
    public boolean isVirtualOjbProxy(Object proxyOrObject)
    {
        return proxyOrObject instanceof VirtualProxy;
    }

    /**
     * Returns <tt>true</tt> if the given object is a {@link java.lang.reflect.Proxy}
     * or a {@link VirtualProxy} instance.
     */
    public boolean isProxy(Object proxyOrObject)
    {
        return isNormalOjbProxy(proxyOrObject) || isVirtualOjbProxy(proxyOrObject);
    }

    /**
     * Returns the IndirectionHandler associated with a dynamic proxy. Each
     * subclass is responsible for it's execution
     */
    protected abstract IndirectionHandler getDynamicIndirectionHandler(Object obj);

    /**
     * Returns the invocation handler object of the given proxy object.
     *
     * @param obj The object
     * @return The invocation handler if the object is an OJB proxy, or <code>null</code>
     *         otherwise
     */
    public IndirectionHandler getIndirectionHandler(Object obj)
    {
        if(obj == null)
        {
            return null;
        }
        else if(isNormalOjbProxy(obj))
        {
            return getDynamicIndirectionHandler(obj);
        }
        else if(isVirtualOjbProxy(obj))
        {
            return VirtualProxy.getIndirectionHandler((VirtualProxy) obj);
        }
        else
        {
            return null;
        }

    }

    /**
     * Determines whether the object is a materialized object, i.e. no proxy or a
     * proxy that has already been loaded from the database.
     *
     * @param object The object to test
     * @return <code>true</code> if the object is materialized
     */
    public boolean isMaterialized(Object object)
    {
        IndirectionHandler handler = getIndirectionHandler(object);

        return handler == null || handler.alreadyMaterialized();
    }

    /** Return CollectionProxy for item is item is a CollectionProxy, otherwise return null */
    public CollectionProxy getCollectionProxy(Object item)
    {
        if(isCollectionProxy(item))
        {
            return (CollectionProxy) item;
        }
        else
        {
            return null;
        }
    }

    /**
     * Reports if item is a CollectionProxy.
     *
     * TODO: Provide handling for pluggable collection proxy implementations
     */
    public boolean isCollectionProxy(Object item)
    {
        return (item instanceof CollectionProxy);
    }

    /**
     * Materialization-safe version of toString. If the object is a yet-unmaterialized proxy,
     * then only the text "unmaterialized proxy for ..." is returned and the proxy is NOT
     * materialized. Otherwise, the normal toString method is called. This useful e.g. for
     * logging etc.
     *
     * @param proxy The object for which a string representation shall be generated
     * @return The string representation
     */
    public String toString(Object proxy)
    {
        IndirectionHandler handler = getIndirectionHandler(proxy);
        if((handler != null) && handler.alreadyMaterialized())
        {
            return "unmaterialized proxy for " + handler.getIdentity();
        }
        else
        {
            return proxy.toString();
        }
    }

    public synchronized static ProxyFactory getProxyFactory()
    {
        /*
        TODO: Check usage of singleton.
        arminw: Think the ProxyFactory instance can be a singleton, because the proxy stuff
        will never change while runtime.
        */
        if(singleton == null)
        {
            Class proxyFactoryClass = null;
            try
            {
                proxyFactoryClass = getProxyConfiguration().getProxyFactoryClass();
                singleton = (ProxyFactory) proxyFactoryClass.newInstance();
            }
            catch(InstantiationException e)
            {
                throw new MetadataException("Illegal class " + proxyFactoryClass.getName() + " specified for ProxyFactoryClass.");
            }
            catch(IllegalAccessException e)
            {
                throw new MetadataException("Illegal class " + proxyFactoryClass.getName() + " specified for ProxyFactoryClass.");
            }
        }
        return singleton;
    }

}
