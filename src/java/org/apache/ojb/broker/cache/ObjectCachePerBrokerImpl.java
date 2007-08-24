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

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PBStateEvent;
import org.apache.ojb.broker.PBStateListener;
import org.apache.ojb.broker.PersistenceBroker;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This local {@link ObjectCache} implementation allows to have dedicated caches per broker.
 * All calls are delegated to the cache associated with the currentBroker.
 * When the broker was closed (returned to pool) the cache was cleared.
 *
 * <p>
 * Implementation configuration properties:
 * </p>
 *
 * <table cellspacing="2" cellpadding="2" border="3" frame="box">
 * <tr>
 *     <td><strong>Property Key</strong></td>
 *     <td><strong>Property Values</strong></td>
 * </tr>
 * <tr>
 *     <td> - </td>
 *     <td>
 *          -
 *    </td>
 * </tr>
 * </table>
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: ObjectCachePerBrokerImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class ObjectCachePerBrokerImpl implements ObjectCache, PBStateListener
{
    /**
     * the hashtable holding all cached object
     */
    protected Map objectTable = null;

    /**
     * public Default Constructor
     */
    public ObjectCachePerBrokerImpl(PersistenceBroker broker, Properties prop)
    {
        objectTable = new HashMap();
        // add this cache as permanent listener
        broker.addListener(this, true);
    }

    /**
     * Clear ObjectCache. I.e. remove all entries for classes and objects.
     */
    public void clear()
    {
        objectTable.clear();
    }

    /**
     * Makes object persistent to the Objectcache.
     * I'm using soft-references to allow gc reclaim unused objects
     * even if they are still cached.
     */
    public void cache(Identity oid, Object obj)
    {
        if ((obj != null))
        {
            SoftReference ref = new SoftReference(obj);
            objectTable.put(oid, ref);
        }
    }

    public boolean cacheIfNew(Identity oid, Object obj)
    {
        if(objectTable.get(oid) == null)
        {
            objectTable.put(oid, obj);
            return true;
        }
        return false;
    }

    /**
     * Lookup object with Identity oid in objectTable.
     * Returns null if no matching id is found
     */
    public Object lookup(Identity oid)
    {
        Object obj = null;
        SoftReference ref = (SoftReference) objectTable.get(oid);
        if (ref != null)
        {
            obj = ref.get();
            if (obj == null)
            {
                objectTable.remove(oid);    // Soft-referenced Object reclaimed by GC
            }
        }
        return obj;
    }

    /**
     * Removes an Object from the cache.
     */
    public void remove(Identity oid)
    {
        if (oid != null)
        {
            objectTable.remove(oid);
        }
    }

    /**
     * We clear the cache
     */
    public void beforeClose(PBStateEvent event)
    {
        clear();
    }

    public void afterOpen(PBStateEvent event)
    {
        //do nothing
    }

    public void beforeBegin(PBStateEvent event)
    {
        //do nothing
    }

    public void afterBegin(PBStateEvent event)
    {
        //do nothing
    }

    public void beforeCommit(PBStateEvent event)
    {
        //do nothing
    }

    public void afterCommit(PBStateEvent event)
    {
        //do nothing
    }

    public void beforeRollback(PBStateEvent event)
    {
        //do nothing
   }

    public void afterRollback(PBStateEvent event)
    {
        // clear to be in sync with DB
        clear();
    }
}
