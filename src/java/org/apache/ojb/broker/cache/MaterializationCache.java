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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * A wrapper class for {@link ObjectCache} implementations used to materialize object graphs and
 * push the fully materialized object to the real object cache.
 * To avoid passing of partial materialized objects to cache this class act as a temporary storage
 * for unmaterialized (new read or refreshed) objects.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: MaterializationCache.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class MaterializationCache implements ObjectCacheInternal
{
    private static Logger log = LoggerFactory.getLogger(MaterializationCache.class);

    private CacheDistributor cacheDistributor;
    private HashMap objectBuffer;
    private boolean enabledReadCache;
    private int invokeCounter;

    MaterializationCache(CacheDistributor cache)
    {
        this.cacheDistributor = cache;
        this.objectBuffer = new HashMap();
        enabledReadCache = false;
    }

    /**
     * Returns <em>true</em> if the materialisation cache is enabled, otherwise <em>false</em>.
     */
    public boolean isEnabledMaterialisationCache()
    {
        return enabledReadCache;
    }

    /**
     * For internal use only! Helper method to guarantee that only full materialized objects
     * will be pushed to the application cache regardless if an local PB transaction
     * is running or not. When a complex object is materialized there will be
     * nested calls to the same PB instance methods, e.g. materialization of a referenced
     * object which itself have several references, ...
     * <br/>
     * This method and {@link #disableMaterializationCache()} are used to delimit nested calls
     * and to detect the end of an object materialization and avoid endless loops on circular
     * references.
     * <br/>
     * If an code block with 'enabledMaterializationCache' throws an exception, in catch-block
     * method {@link #doLocalClear()} have to be called.
     */
    public void enableMaterializationCache()
    {
        ++invokeCounter;
        enabledReadCache = true;
    }

    /**
     * @see #enableMaterializationCache()
     */
    public void disableMaterializationCache()
    {
        if(!enabledReadCache) return;

        --invokeCounter;
        /*
        if materialization of the requested object was completed, the
        counter represents '0' and we push the object
        to the application cache
        */
        if(invokeCounter == 0)
        {
            try
            {
                if(log.isDebugEnabled())
                {
                    log.debug("Materialisation of object is finished, push "
                            + objectBuffer.size() + "objects to cache");
                }
                pushObjects();
            }
            finally
            {
                doLocalClear();
            }
        }
    }

    public void doInternalCache(Identity oid, Object obj, int type)
    {
        // if OJB try to build an object graph put objects in local cache
        // else use the application cache
        if(enabledReadCache)
        {
            doLocalCache(oid, obj, type);
        }
        else
        {
            cacheDistributor.doInternalCache(oid, obj, type);
        }
    }

    public void cache(Identity oid, Object obj)
    {
        doInternalCache(oid, obj, TYPE_UNKNOWN);
    }

    /**
     * @see ObjectCacheInternal#cacheIfNew(org.apache.ojb.broker.Identity, Object)
     */ 
    public boolean cacheIfNew(Identity oid, Object obj)
    {
        boolean result = cacheDistributor.cacheIfNew(oid, obj);
        if(enabledReadCache)
        {
            doLocalCache(oid, obj, TYPE_CACHED_READ);
        }
        return result;
    }

    public Object lookup(Identity oid)
    {
        Object result = null;
        if(enabledReadCache)
        {
            result = doLocalLookup(oid);
        }
        if(result == null)
        {
            result = cacheDistributor.lookup(oid);
        }
        return result;
    }

    public Object doLocalLookup(Identity oid)
    {
        ObjectEntry entry = (ObjectEntry) objectBuffer.get(oid);
        return entry != null ? entry.obj : null;
    }

    public void remove(Identity oid)
    {
        doLocalRemove(oid);
        cacheDistributor.remove(oid);
    }

    public void doLocalRemove(Identity oid)
    {
        objectBuffer.remove(oid);
    }

    /**
     * Clears the internal used cache for object materialization.
     */
    public void doLocalClear()
    {
        if(log.isDebugEnabled()) log.debug("Clear materialization cache");
        invokeCounter = 0;
        enabledReadCache = false;
        objectBuffer.clear();
    }

    public void clear()
    {
        if(log.isDebugEnabled()) log.debug("Clear used caches");
        doLocalClear();
        cacheDistributor.clear();
    }

    private void doLocalCache(Identity oid, Object obj, int type)
    {
        objectBuffer.put(oid, new ObjectEntry(obj, type));
    }

    private void pushObjects()
    {
        Iterator it = objectBuffer.entrySet().iterator();
        Map.Entry entry;
        ObjectEntry oe;
        while(it.hasNext())
        {
            entry = (Map.Entry) it.next();
            oe = (ObjectEntry) entry.getValue();
            /*
            never push temporary object to a higher level cache
            */
            if(oe.type != TYPE_TEMP)
            {
                if(log.isDebugEnabled()) log.debug("Push to cache: " + entry.getKey());
                cacheDistributor.doInternalCache((Identity) entry.getKey(), oe.obj, oe.type);
            }
        }
    }

    //===========================================================
    // inner class
    //===========================================================

    static final class ObjectEntry
    {
        Object obj;
        int type;

        public ObjectEntry(Object obj, int type)
        {
            this.obj = obj;
            this.type = type;
        }
    }
}
