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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Simple, flexible local {@link ObjectCache} implementation using a
 * {@link java.util.HashMap} to cache given objects.
 * <p>
 * The cache uses soft-references which allows objects (softly) referenced by
 * the cache to be reclaimed by the Java Garbage Collector when they are not
 * longer referenced elsewhere.
 * </p>
 * <p>
 * NOTE: Handle with care! If multiple PB instances are used (OJB standard behavior) you
 * will run into synchronization problems.
 * </p>
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
 *     <td>timeout</td>
 *     <td>
 *          Lifetime of the cached objects in seconds.
 *          If expired the cached object was not returned
 *          on lookup call (and removed from cache).
 *    </td>
 * </tr>
 * </table>
 *
 * <br/>
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ObjectCacheLocalDefaultImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class ObjectCacheLocalDefaultImpl implements ObjectCache
{
    private static final String TIMEOUT = "timeout";

    /**
     * the hashtable holding all cached object
     */
    protected Map objectTable = new HashMap();

    /**
     * Timeout of the cached objects.
     */
    private long timeout = 1000 * 60 * 15;

    /**
     *
     */
    public ObjectCacheLocalDefaultImpl(PersistenceBroker broker, Properties prop)
    {
        timeout = prop == null ? timeout : ( Long.parseLong( prop.getProperty( TIMEOUT, "" + timeout ) ) )*1000;
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
            SoftReference ref = new SoftReference(new CacheEntry(obj));
            objectTable.put(oid, ref);
        }
    }

    public boolean cacheIfNew(Identity oid, Object obj)
    {
        if(objectTable.get(oid) == null)
        {
            cache(oid, obj);
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
        CacheEntry entry = null;
        SoftReference ref = (SoftReference) objectTable.get(oid);
        if (ref != null)
        {
            entry = (CacheEntry) ref.get();
            if (entry == null || entry.lifetime < System.currentTimeMillis())
            {
                objectTable.remove(oid);    // Soft-referenced Object reclaimed by GC
                // timeout, so set null
                entry = null;
            }
        }
        return entry != null ? entry.object : null;
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

    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE);
        buf.append("Count of cached objects", objectTable.keySet().size());
        return buf.toString();
    }

    //-----------------------------------------------------------
    // inner class
    //-----------------------------------------------------------
    class CacheEntry
    {
        long lifetime;
        Object object;

        public CacheEntry(Object object)
        {
            this.object = object;
            lifetime = System.currentTimeMillis() + timeout;
        }
    }

}
