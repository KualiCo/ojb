package org.apache.ojb.broker.cache;

/* Copyright 2004-2005 The Apache Software Foundation
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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.collections.LRUMap;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.util.configuration.Configurable;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;

/**
 * A global {@link ObjectCache} implementation.
 *
 * @author matthew.baird
 * @version $Id: ObjectCacheSoftImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class ObjectCacheSoftImpl implements ObjectCache, Configurable
{
    /**
     * The static the cache map
     */
    private static SoftHashMap cache = null;

    /**
     * The size of the cache
     */
    private static int size = 10000;

    /**
     * Constructor called by ojb
     *
     * @param broker     ignored parameter
     * @param properties ignored parameter
     */
    public ObjectCacheSoftImpl(PersistenceBroker broker, Properties properties)
    {
        if (cache == null)
        {
            OjbConfigurator.getInstance().configure(this);
            cache = new SoftHashMap(size);
        }
    }

    /**
     * @see org.apache.ojb.broker.util.configuration.Configurable#configure(org.apache.ojb.broker.util.configuration.Configuration)
     */
    public void configure(Configuration configuration) throws ConfigurationException
    {
        size = configuration.getInteger("ObjectCacheSoftImpl", size);
    }

    /**
     * @see org.apache.ojb.broker.cache.ObjectCache#cache(org.apache.ojb.broker.Identity, java.lang.Object)
     */
    public void cache(Identity oid, Object obj)
    {
        synchronized(cache)
        {
            cache.put(oid, obj);
        }
    }

    public boolean cacheIfNew(Identity oid, Object obj)
    {
        synchronized(cache)
        {
            if(cache.get(oid) == null)
            {
                cache.put(oid, obj);
                return true;
            }
            return false;
        }
    }

    /**
     * @see org.apache.ojb.broker.cache.ObjectCache#lookup(org.apache.ojb.broker.Identity)
     */
    public Object lookup(Identity oid)
    {
        return cache.get(oid);
    }

    /**
     * @see org.apache.ojb.broker.cache.ObjectCache#remove(org.apache.ojb.broker.Identity)
     */
    public void remove(Identity oid)
    {
        synchronized(cache)
        {
            cache.remove(oid);
        }
    }

    /**
     * @see org.apache.ojb.broker.cache.ObjectCache#clear()
     */
    public void clear()
    {
        cache.clear();
    }

    /**
     * Kind of map using SoftReference to store values
     */
    public static final class SoftHashMap
    {
        /**
         * The internal HashMap that will hold the SoftReference.
         */
        private HashMap hash;
        /**
         * The FIFO list of hard references, order of last access.
         */
        private LRUMap hardCacheMap;
        /**
         * Reference queue for cleared SoftReference objects.
         */
        private ReferenceQueue queue;

        /**
         * Construct a new hash map with the specified size
         *
         * @param hardSize the maximum capacity of this map
         */
        public SoftHashMap(final int hardSize)
        {
            hash = new HashMap();
            hardCacheMap = new LRUMap(hardSize);
            queue = new ReferenceQueue();
        }

        /**
         * Put the key, value pair into the HashMap using a SoftValue object
         *
         * @param key   the key
         * @param value the value
         * @return the old value
         */
        public Object put(Object key, Object value)
        {
            //check null since hashtable doesn't support null key or null value
            if (key == null || value == null)
            {
                return null;
            }
            processQueue(); // throw out garbage collected values first
            hardCacheMap.put(key, value);
            return hash.put(key, new SoftValue(value, key, queue));
        }

        /**
         * Retrieve the value associated to a given key
         *
         * @param key the key
         * @return the value associated to this key
         */
        public Object get(Object key)
        {
            // Check null since Hashtable doesn't support null key or null value
            if (key == null)
            {
                return null;
            }
            Object result = null;
            // We get the SoftReference represented by that key
            SoftReference softRef = (SoftReference) hash.get(key);
            if (softRef != null)
            {
                result = softRef.get();
                if (result == null)
                {
                    // If the value has been garbage collected, remove the
                    // entry from the HashMap.
                    hash.remove(key);
                }
                else
                {
                    if (!hardCacheMap.containsKey(key))
                    {
                        hardCacheMap.put(key, result);
                    }
                    else
                    {
                        hardCacheMap.get(key);
                    }
                }
            }
            return result;
        }

        /**
         * Remove the entry for this key
         *
         * @param key the key
         * @return the old value
         */
        public Object remove(Object key)
        {
            processQueue(); // throw out garbage collected values first
            Object retval = null;
            Object value = hash.remove(key);
            if (value != null)
            {
                if (value instanceof SoftValue)
                {
                    retval = ((SoftValue) value).get();
                }
            }
            return retval;
        }

        /**
         * Clear the map
         */
        public void clear()
        {
            processQueue();
            hash.clear();
            hardCacheMap.clear();
        }

        /**
         * Class derived from SoftReference, used to
         * store the key of the map.
         */
        private class SoftValue extends SoftReference
        {
            /**
             * the key
             */
            private final Object key; // always make data member final

            /**
             * Create a SoftValue given the object, key and queue
             *
             * @param k   the object
             * @param key the key
             * @param q   the reference queue
             */
            private SoftValue(final Object k, final Object key, final ReferenceQueue q)
            {
                super(k, q);
                this.key = key;
            }
        }

        /**
         * Removes keys and objects that have been garbaged
         */
        private void processQueue()
        {
            SoftValue sv;
            while ((sv = (SoftValue) queue.poll()) != null)
            {
                hash.remove(sv.key); // we can access private data!
            }
        }

    }
}
