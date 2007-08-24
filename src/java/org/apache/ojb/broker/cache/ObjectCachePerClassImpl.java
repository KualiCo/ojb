package org.apache.ojb.broker.cache;

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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;

/**
 * Global {@link ObjectCache} implementation.
 *
 * @author matthew.baird
 * @version $Id: ObjectCachePerClassImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class ObjectCachePerClassImpl extends AbstractMetaCache
{
    private static Map cachesByClass = Collections.synchronizedMap(new HashMap());

    /**
     * Constructor for the ObjectCachePerClassImpl object
     */
    public ObjectCachePerClassImpl(PersistenceBroker broker, Properties prop)
    {
        setClassCache(Object.class, new ObjectCacheDefaultImpl(broker, null));
    }

    public ObjectCache getCache(Identity oid, Object obj, int methodCall)
    {
        if(oid.getObjectsRealClass() == null)
        {
            return null;
        }
        else
        {
            return getCachePerClass(oid.getObjectsRealClass(), methodCall);
        }
    }

    /**
     * Clears the cache
     */
    public void clear()
    {
        Iterator it = cachesByClass.values().iterator();
        while (it.hasNext())
        {
            ObjectCache cache = (ObjectCache) it.next();
            if (cache != null)
            {
                cache.clear();
            }
        }
    }

    /**
     * Sets the ObjectCache implementation to use for objects with the given
     * type and subclasses
     *
     * @param objectClass The object's class, use java.lang.Object to alter
     *                    default caching for all objects which have no special
     *                    caching defined
     * @param cache       The new ObjectCache implementation to use for this
     *                    class and subclasses, null to switch off caching
     *                    for the given class
     */
    public void setClassCache(Class objectClass, ObjectCache cache)

    {
        setClassCache(objectClass.getName(), cache);
    }

    /**
     * Sets the ObjectCache implementation for the given class name
     *
     * @param className The name of the class to cache
     * @param cache     The ObjectCache to use for this class and subclasses
     */
    private void setClassCache(String className, ObjectCache cache)
    {
        cachesByClass.put(className, cache);
    }

    /**
     * Gets the cache for the given class
     *
     * @param objectClass The class to look up the cache for
     * @return The cache
     */
    private ObjectCache getCachePerClass(Class objectClass, int methodCall)
    {
        ObjectCache cache = (ObjectCache) cachesByClass.get(objectClass.getName());
        if (cache == null && AbstractMetaCache.METHOD_CACHE == methodCall
                && !cachesByClass.containsKey(objectClass.getName()))
        {
            cache = new ObjectCacheDefaultImpl(null, null);
            setClassCache(objectClass.getName(), cache);
        }
        return cache;
    }
}
