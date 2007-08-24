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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PBFactoryException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.metadata.MetadataException;
import org.apache.ojb.broker.core.PersistenceBrokerThreadMapping;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.util.collections.IRemovalAwareCollection;
import org.apache.ojb.broker.util.collections.RemovalAwareCollection;

/**
 * A place holder for a whole collection to support deferred loading of relationships.
 * The complete collection is loaded on access to the data.
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: CollectionProxyDefaultImpl.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public class CollectionProxyDefaultImpl implements Collection, ManageableCollection, CollectionProxy
{
    /** The key for acquiring the above broker */
    private PBKey _brokerKey;
    /** Flag set when per-thread metadata profiles are in use. */
    private boolean _perThreadDescriptorsEnabled;
    /** Profile key used when lazy-loading with per-thread metadata profiles. */
    private Object _profileKey;
    /** The query that defines the values in the collection */
    private Query _query;
    /** The actual data (if already loaded) */
    private Collection _data;
    /** The collection type */
    private Class _collectionClass;
    /** The number of objects */
    private int _size = -1;
    /*
    arminw
    fix a bug, caused by closing PB instances
    obtained from PersistenceBrokerThreadMapping.
    TODO: Could we find a better solution for this?
    */
    private boolean _needsClose;
    /** Objects that listen on this proxy for loading events */
    private transient ArrayList _listeners;

    /**
     * Creates a new collection proxy (uses
     * {@link org.apache.ojb.broker.util.collections.RemovalAwareCollection}
     * as the collection class).
     * 
     * @param brokerKey The key of the persistence broker
     * @param query     The defining query
     */
    public CollectionProxyDefaultImpl(PBKey brokerKey, Query query)
    {
        this(brokerKey, RemovalAwareCollection.class, query);
    }

    /**
     * Creates a new collection proxy that uses the given collection type.
     * 
     * @param brokerKey The key of the persistence broker
     * @param collClass The collection type
     * @param query     The defining query
     */
    public CollectionProxyDefaultImpl(PBKey brokerKey, Class collClass, Query query)
    {
        MetadataManager mm = MetadataManager.getInstance();
        _perThreadDescriptorsEnabled = mm.isEnablePerThreadChanges();
        if (_perThreadDescriptorsEnabled)
        {
            // mkalen:  To minimize memory footprint we remember only the OJB profile key
            //          (instead of all active class-mappings).
            final Object key = mm.getCurrentProfileKey();
            if (key == null)
            {
                // mkalen:  Unsupported: using proxies with per-thread metadata changes without profile keys.
                throw new MetadataException("Trying to create a Collection proxy with per-thread metadata changes enabled, but no profile key.");
            }
            setProfileKey(key);
        }
        setBrokerKey(brokerKey);
        setCollectionClass(collClass);
        setQuery(query);
    }

    /**
     * Reactivates metadata profile used when creating proxy, if needed.
     * Calls to this method should be guarded by checking
     * {@link #_perThreadDescriptorsEnabled} since the profile never
     * needs to be reloaded if not using pre-thread metadata changes.
     */
    protected void loadProfileIfNeeded()
    {
        final Object key = getProfileKey();
        if (key != null)
        {
            final MetadataManager mm = MetadataManager.getInstance();
            if (!key.equals(mm.getCurrentProfileKey()))
            {
                mm.loadProfile(key);
            }
        }
    }

    /**
     * Determines whether the collection data already has been loaded from the database.
     *
     * @return <code>true</code> if the data is already loaded
     */
    public boolean isLoaded()
    {
        return _data != null;
    }

    /**
     * Determines the number of elements that the query would return. Override this
     * method if the size shall be determined in a specific way.
     * 
     * @return The number of elements
     */
    protected synchronized int loadSize() throws PersistenceBrokerException
    {
        PersistenceBroker broker = getBroker();
        try
        {
            return broker.getCount(getQuery());
        }
        catch (Exception ex)
        {
            throw new PersistenceBrokerException(ex);
        }
        finally
        {
            releaseBroker(broker);
        }
    }

    /**
     * Sets the size internally.
     * 
     * @param size The new size
     */
    protected synchronized void setSize(int size)
    {
        _size = size;
    }
    
    /**
     * Loads the data from the database. Override this method if the objects
     * shall be loaded in a specific way.
     * 
     * @return The loaded data
     */
    protected Collection loadData() throws PersistenceBrokerException
    {
        PersistenceBroker broker = getBroker();
        try
        {
            Collection result;

            if (_data != null) // could be set by listener
            {
                result = _data;
            }
            else if (_size != 0)
            {
                // TODO: returned ManageableCollection should extend Collection to avoid
                // this cast
                result = (Collection) broker.getCollectionByQuery(getCollectionClass(), getQuery());
            }
            else
            {
                result = (Collection)getCollectionClass().newInstance();
            }
            return result;
        }
        catch (Exception ex)
        {
            throw new PersistenceBrokerException(ex);
        }
        finally
        {
            releaseBroker(broker);
        }
    }

    /**
     * Notifies all listeners that the data is about to be loaded.
     */
    protected void beforeLoading()
    {
        if (_listeners != null)
        {
            CollectionProxyListener listener;

            if (_perThreadDescriptorsEnabled) {
                loadProfileIfNeeded();
            }
            for (int idx = _listeners.size() - 1; idx >= 0; idx--)
            {
                listener = (CollectionProxyListener)_listeners.get(idx);
                listener.beforeLoading(this);
            }
        }
    }

    /**
     * Notifies all listeners that the data has been loaded.
     */
    protected void afterLoading()
    {
        if (_listeners != null)
        {
            CollectionProxyListener listener;

            if (_perThreadDescriptorsEnabled) {
                loadProfileIfNeeded();
            }
            for (int idx = _listeners.size() - 1; idx >= 0; idx--)
            {
                listener = (CollectionProxyListener)_listeners.get(idx);
                listener.afterLoading(this);
            }
        }
    }

    /**
     * @see Collection#size()
     */
    public int size()
    {
        if (isLoaded())
        {
            return getData().size();
        }
        else
        {
            if (_size < 0)
            {
                _size = loadSize();
            }
            return _size;
        }
    }

    /**
     * @see Collection#isEmpty()
     */
    public boolean isEmpty()
    {
        return size() == 0;
    }

    /**
     * @see Collection#contains(Object)
     */
    public boolean contains(Object o)
    {
        return getData().contains(o);
    }

    /**
     * @see Collection#iterator()
     */
    public Iterator iterator()
    {
        return getData().iterator();
    }

    /**
     * @see Collection#toArray()
     */
    public Object[] toArray()
    {
        return getData().toArray();
    }

    /**
     * @see Collection#toArray(Object[])
     */
    public Object[] toArray(Object[] a)
    {
        return getData().toArray(a);
    }

    /**
     * @see Collection#add(Object)
     */
    public boolean add(Object o)
    {
        return getData().add(o);
    }

    /**
     * @see Collection#remove(Object)
     */
    public boolean remove(Object o)
    {
        return getData().remove(o);
    }

    /**
     * @see Collection#containsAll(Collection)
     */
    public boolean containsAll(Collection c)
    {
        return getData().containsAll(c);
    }

    /**
     * @see Collection#addAll(Collection)
     */
    public boolean addAll(Collection c)
    {
        return getData().addAll(c);
    }

    /**
     * @see Collection#removeAll(Collection)
     */
    public boolean removeAll(Collection c)
    {
        return getData().removeAll(c);
    }

    /**
     * @see Collection#retainAll(Collection)
     */
    public boolean retainAll(Collection c)
    {
        return getData().retainAll(c);
    }

    /**
     * Clears the proxy. A cleared proxy is defined as loaded
     *
     * @see Collection#clear()
     */
    public void clear()
    {
        Class collClass = getCollectionClass();

        // ECER: assure we notify all objects being removed, 
        // necessary for RemovalAwareCollections...
        if (IRemovalAwareCollection.class.isAssignableFrom(collClass))
        {
            getData().clear();
        }
        else
        {
            Collection coll;
            // BRJ: use an empty collection so isLoaded will return true
            // for non RemovalAwareCollections only !! 
            try
            {
                coll = (Collection) collClass.newInstance();
            }
            catch (Exception e)
            {
                coll = new ArrayList();
            }

            setData(coll);
        }
        _size = 0;
    }

    /**
     * Returns the defining query.
     * 
     * @return The query
     */
    public Query getQuery()
    {
        return _query;
    }

    /**
     * Sets the defining query.
     * 
     * @param query The query
     */
    protected void setQuery(Query query)
    {
        _query = query;
    }

    /**
     * Release the broker instance.
     */
    protected synchronized void releaseBroker(PersistenceBroker broker)
    {
        /*
        arminw:
        only close the broker instance if we get
        it from the PBF, do nothing if we obtain it from
        PBThreadMapping
        */
        if (broker != null && _needsClose)
        {
            _needsClose = false;
            broker.close();
        }
    }

    /**
     * Acquires a broker instance. If no PBKey is available a runtime exception will be thrown.
     * 
     * @return A broker instance
     */
    protected synchronized PersistenceBroker getBroker() throws PBFactoryException
    {
        /*
            mkalen:
            NB! The loadProfileIfNeeded must be called _before_ acquiring a broker below,
            since some methods in PersistenceBrokerImpl will keep a local reference to
            the descriptor repository that was active during broker construction/refresh
            (not checking the repository beeing used on method invocation).

            PersistenceBrokerImpl#getClassDescriptor(Class clazz) is such a method,
            that will throw ClassNotPersistenceCapableException on the following scenario:

            (All happens in one thread only):
            t0: activate per-thread metadata changes
            t1: load, register and activate profile A
            t2: load object O1 witch collection proxy C to objects {O2} (C stores profile key K(A))
            t3: close broker from t2
            t4: load, register and activate profile B
            t5: reference O1.getO2Collection, causing C loadData() to be invoked
            t6: C calls getBroker
                broker B is created and descriptorRepository is set to descriptors from profile B
            t7: C calls loadProfileIfNeeded, re-activating profile A
            t8: C calls B.getCollectionByQuery
            t9: B gets callback (via QueryReferenceBroker) to getClassDescriptor
                the local descriptorRepository from t6 is used!
                => We will now try to query for {O2} with profile B
                    (even though we re-activated profile A in t7)
                    => ClassNotPersistenceCapableException

            Keeping loadProfileIfNeeded() at the start of this method changes everything from t6:
            t6: C calls loadProfileIfNeeded, re-activating profile A
            t7: C calls getBroker,
                broker B is created and descriptorRepository is set to descriptors from profile A
            t8: C calls B.getCollectionByQuery
            t9: B gets callback to getClassDescriptor,
                the local descriptorRepository from t6 is used
                => We query for {O2} with profile A
                    => All good :-)
        */
        if (_perThreadDescriptorsEnabled)
        {
            loadProfileIfNeeded();
        }

        PersistenceBroker broker;
        if (getBrokerKey() == null)
        {
            /*
            arminw:
            if no PBKey is set we throw an exception, because we don't
            know which PB (connection) should be used.
            */
            throw new OJBRuntimeException("Can't find associated PBKey. Need PBKey to obtain a valid" +
                                          "PersistenceBroker instance from intern resources.");
        }
        // first try to use the current threaded broker to avoid blocking
        broker = PersistenceBrokerThreadMapping.currentPersistenceBroker(getBrokerKey());
        // current broker not found or was closed, create a intern new one
        if (broker == null || broker.isClosed())
        {
            broker = PersistenceBrokerFactory.createPersistenceBroker(getBrokerKey());
            // signal that we use a new internal obtained PB instance to read the
            // data and that this instance have to be closed after use
            _needsClose = true;
        }
        return broker;
    }

    /**
     * Returns the collection data, load it if not already done so.
     * 
     * @return The data
     */
    public synchronized Collection getData()
    {
        if (!isLoaded())
        {
            beforeLoading();
            setData(loadData());
            afterLoading();
        }
        return _data;
    }

    /**
     * Sets the collection data.
     * 
     * @param data The data
     */
    public void setData(Collection data)
    {
        _data = data;
    }

    /**
     * Returns the collection type.
     * 
     * @return The collection type
     */
    public Class getCollectionClass()
    {
        return _collectionClass;
    }

    /**
     * Sets the collection type.
     * 
     * @param collClass The collection type
     */
    protected void setCollectionClass(Class collClass)
    {
        _collectionClass = collClass;
    }

    /**
     * @see org.apache.ojb.broker.ManageableCollection#ojbAdd(Object)
     */
    public void ojbAdd(Object anObject)
    {
        add(anObject);
    }

    /**
     * @see org.apache.ojb.broker.ManageableCollection#ojbAddAll(ManageableCollection)
     */
    public void ojbAddAll(ManageableCollection otherCollection)
    {
        addAll((CollectionProxyDefaultImpl)otherCollection);
    }

    /**
     * @see org.apache.ojb.broker.ManageableCollection#ojbIterator()
     */
    public Iterator ojbIterator()
    {
        return iterator();
    }

    /**
     * @see org.apache.ojb.broker.ManageableCollection#afterStore(PersistenceBroker broker)
     */
    public void afterStore(PersistenceBroker broker) throws PersistenceBrokerException
    {
        // If the real subject is a ManageableCollection
        // the afterStore() callback must be invoked !
        Collection c = getData();

        if (c instanceof ManageableCollection)
        {
            ((ManageableCollection)c).afterStore(broker);
        }
    }

    /**
     * Returns the key of the persistence broker used by this collection.
     * 
     * @return The broker key
     */
    public PBKey getBrokerKey()
    {
        return _brokerKey;
    }

    /**
     * Sets the key of the persistence broker used by this collection.
     * 
     * @param brokerKey The key of the broker
     */
    protected void setBrokerKey(PBKey brokerKey)
    {
        _brokerKey = brokerKey;
    }

    /**
     * Returns the metadata profile key used when creating this proxy.
     *
     * @return brokerKey The key of the broker
     */
    protected Object getProfileKey()
    {
        return _profileKey;
    }

    /**
     * Sets the metadata profile key used when creating this proxy.
     *
     * @param profileKey the metadata profile key
     */
    public void setProfileKey(Object profileKey)
    {
        _profileKey = profileKey;
    }

    /**
     * Adds a listener to this collection.
     * 
     * @param listener The listener to add
     */
    public synchronized void addListener(CollectionProxyListener listener)
    {
        if (_listeners == null)
        {
            _listeners = new ArrayList();
        }
        // to avoid multi-add of same listener, do check
        if(!_listeners.contains(listener))
        {
            _listeners.add(listener);
        }
    }

    /**
     * Removes the given listener from this collecton.
     * 
     * @param listener The listener to remove
     */
    public synchronized void removeListener(CollectionProxyListener listener)
    {
        if (_listeners != null)
        {
            _listeners.remove(listener);
        }
    }

}
