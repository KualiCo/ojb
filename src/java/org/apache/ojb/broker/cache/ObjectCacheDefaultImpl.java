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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PBStateEvent;
import org.apache.ojb.broker.PBStateListener;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * This global ObjectCache stores all Objects loaded by the <code>PersistenceBroker</code>
 * from a DB using a static {@link java.util.Map}. This means each {@link ObjectCache}
 * instance associated with all {@link PersistenceBroker} instances use the same
 * <code>Map</code> to cache objects. This could lead in "dirty-reads" (similar to read-uncommitted
 * mode in DB) when a concurrent thread look up same object modified by another thread.
 * <br/>
 * When the PersistenceBroker tries to get an Object by its {@link Identity}.
 * It first lookups the cache if the object has been already loaded and cached.
 * <p/>
 * NOTE: By default objects cached via {@link SoftReference} which allows
 * objects (softly) referenced by the cache to be reclaimed by the Java Garbage Collector when
 * they are not longer referenced elsewhere, so lifetime of cached object is limited by
 * <br/> - the lifetime of the cache object - see property <code>timeout</code>.
 * <br/> - the garabage collector used memory settings - see property <code>useSoftReferences</code>.
 * <br/> - the maximum capacity of the cache - see property <code>maxEntry</code>.
 * </p>
 * <p/>
 * Implementation configuration properties:
 * </p>
 * <p/>
 * <p/>
 * <table cellspacing="2" cellpadding="2" border="3" frame="box">
 * <tr>
 * <td><strong>Property Key</strong></td>
 * <td><strong>Property Values</strong></td>
 * </tr>
 * <p/>
 * <tr>
 * <td>timeout</td>
 * <td>
 * Lifetime of the cached objects in seconds.
 * If expired the cached object was not returned
 * on lookup call (and removed from cache). Default timeout
 * value is 900 seconds. When set to <tt>-1</tt> the lifetime of
 * the cached object depends only on GC and do never get timed out.
 * </td>
 * </tr>
 * <p/>
 * <tr>
 * <td>autoSync</td>
 * <td>
 * If set <tt>true</tt> all cached/looked up objects within a PB-transaction are traced.
 * If the the PB-transaction was aborted all traced objects will be removed from
 * cache. Default is <tt>false</tt>.
 * <p/>
 * NOTE: This does not prevent "dirty-reads" (more info see above).
 * </p>
 * <p/>
 * It's not a smart solution for keeping cache in sync with DB but should do the job
 * in most cases.
 * <br/>
 * E.g. if you lookup 1000 objects within a transaction and modify one object and then abort the
 * transaction, 1000 objects will be passed to cache, 1000 objects will be traced and
 * all 1000 objects will be removed from cache. If you read these objects without tx or
 * in a former tx and then modify one object in a tx and abort the tx, only one object was
 * traced/removed.
 * </p>
 * </td>
 * </tr>
 * <p/>
 * <tr>
 * <td>cachingKeyType</td>
 * <td>
 * Determines how the key was build for the cached objects:
 * <br/>
 * 0 - Identity object was used as key, this was the <em>default</em> setting.
 * <br/>
 * 1 - Idenity + jcdAlias name was used as key. Useful when the same object metadata model
 * (DescriptorRepository instance) are used for different databases (JdbcConnectionDescriptor)
 * <br/>
 * 2 - Identity + model (DescriptorRepository) was used as key. Useful when different metadata
 * model (DescriptorRepository instance) are used for the same database. Keep in mind that there
 * was no synchronization between cached objects with same Identity but different metadata model.
 * <br/>
 * 3 - all together (1+2)
 * </td>
 * </tr>
 * <p/>
 * <tr>
 * <td>useSoftReferences</td>
 * <td>
 * If set <em>true</em> this class use {@link java.lang.ref.SoftReference} to cache
 * objects. Default value is <em>true</em>.
 * </td>
 * </tr>
 * </table>
 * <p/>
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: ObjectCacheDefaultImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class ObjectCacheDefaultImpl implements ObjectCacheInternal, PBStateListener
{
    private Logger log = LoggerFactory.getLogger(ObjectCacheDefaultImpl.class);

    public static final String TIMEOUT_PROP = "timeout";
    public static final String AUTOSYNC_PROP = "autoSync";
    public static final String CACHING_KEY_TYPE_PROP = "cachingKeyType";
    public static final String SOFT_REFERENCES_PROP = "useSoftReferences";
    /**
     * static Map held all cached objects
     */
    protected static final Map objectTable = new Hashtable();
    private static final ReferenceQueue queue = new ReferenceQueue();

    private static long hitCount = 0;
    private static long failCount = 0;
    private static long gcCount = 0;

    protected PersistenceBroker broker;
    private List identitiesInWork;
    /**
     * Timeout of the cached objects. Default was 900 seconds.
     */
    private long timeout = 1000 * 60 * 15;
    private boolean useAutoSync = false;
    /**
     * Determines how the key was build for the cached objects:
     * <br/>
     * 0 - Identity object was used as key
     * 1 - Idenity + jcdAlias name was used as key
     * 2 - Identity + model (DescriptorRepository) was used as key
     * 3 - all together (1+2)
     */
    private int cachingKeyType;
    private boolean useSoftReferences = true;

    public ObjectCacheDefaultImpl(PersistenceBroker broker, Properties prop)
    {
        this.broker = broker;
        timeout = prop == null ? timeout : (Long.parseLong(prop.getProperty(TIMEOUT_PROP, "" + (60 * 15))) * 1000);
        useSoftReferences = prop != null && (Boolean.valueOf((prop.getProperty(SOFT_REFERENCES_PROP, "true")).trim())).booleanValue();
        cachingKeyType = prop == null ? 0 : (Integer.parseInt(prop.getProperty(CACHING_KEY_TYPE_PROP, "0")));
        useAutoSync = prop != null && (Boolean.valueOf((prop.getProperty(AUTOSYNC_PROP, "false")).trim())).booleanValue();
        if(useAutoSync)
        {
            if(broker != null)
            {
                // we add this instance as a permanent PBStateListener
                broker.addListener(this, true);
            }
            else
            {
                log.info("Can't enable property '" + AUTOSYNC_PROP + "', because given PB instance is null");
            }
        }
        identitiesInWork = new ArrayList();
        if(log.isEnabledFor(Logger.INFO))
        {
            ToStringBuilder buf = new ToStringBuilder(this);
            buf.append("timeout", timeout)
                    .append("useSoftReferences", useSoftReferences)
                    .append("cachingKeyType", cachingKeyType)
                    .append("useAutoSync", useAutoSync);
            log.info("Setup cache: " + buf.toString());
        }
    }

    /**
     * Clear ObjectCache. I.e. remove all entries for classes and objects.
     */
    public void clear()
    {
        //processQueue();
        objectTable.clear();
        identitiesInWork.clear();
    }

    public void doInternalCache(Identity oid, Object obj, int type)
    {
        //processQueue();
        if((obj != null))
        {
            traceIdentity(oid);
            synchronized(objectTable)
            {
                if(log.isDebugEnabled()) log.debug("Cache object " + oid);
                objectTable.put(buildKey(oid), buildEntry(obj, oid));
            }
        }
    }

    /**
     * Makes object persistent to the Objectcache.
     * I'm using soft-references to allow gc reclaim unused objects
     * even if they are still cached.
     */
    public void cache(Identity oid, Object obj)
    {
        doInternalCache(oid, obj, ObjectCacheInternal.TYPE_UNKNOWN);
    }

    public boolean cacheIfNew(Identity oid, Object obj)
    {
        //processQueue();
        boolean result = false;
        Object key = buildKey(oid);
        if((obj != null))
        {
            synchronized(objectTable)
            {
                if(!objectTable.containsKey(key))
                {
                    objectTable.put(key, buildEntry(obj, oid));
                    result = true;
                }
            }
            if(result) traceIdentity(oid);
        }
        return result;
    }

    /**
     * Lookup object with Identity oid in objectTable.
     * Returns null if no matching id is found
     */
    public Object lookup(Identity oid)
    {
        processQueue();
        hitCount++;
        Object result = null;

        CacheEntry entry = (CacheEntry) objectTable.get(buildKey(oid));
        if(entry != null)
        {
            result = entry.get();
            if(result == null || entry.getLifetime() < System.currentTimeMillis())
            {
                /*
                cached object was removed by gc or lifetime was exhausted
                remove CacheEntry from map
                */
                gcCount++;
                remove(oid);
                // make sure that we return null
                result = null;
            }
            else
            {
                /*
                TODO: Not sure if this makes sense, could help to avoid corrupted objects
                when changed in tx but not stored.
                */
                traceIdentity(oid);
                if(log.isDebugEnabled()) log.debug("Object match " + oid);
            }
        }
        else
        {
            failCount++;
        }
        return result;
    }

    /**
     * Removes an Object from the cache.
     */
    public void remove(Identity oid)
    {
        //processQueue();
        if(oid != null)
        {
            removeTracedIdentity(oid);
            objectTable.remove(buildKey(oid));
            if(log.isDebugEnabled()) log.debug("Remove object " + oid);
        }
    }

    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE);
        buf.append("Count of cached objects", objectTable.keySet().size());
        buf.append("Lookup hits", hitCount);
        buf.append("Failures", failCount);
        buf.append("Reclaimed", gcCount);
        return buf.toString();
    }

    private void traceIdentity(Identity oid)
    {
        if(useAutoSync && (broker != null) && broker.isInTransaction())
        {
            identitiesInWork.add(oid);
        }
    }

    private void removeTracedIdentity(Identity oid)
    {
        identitiesInWork.remove(oid);
    }

    private void synchronizeWithTracedObjects()
    {
        Identity oid;
        log.info("tx was aborted," +
                " remove " + identitiesInWork.size() + " traced (potentially modified) objects from cache");
        for(Iterator iterator = identitiesInWork.iterator(); iterator.hasNext();)
        {
            oid = (Identity) iterator.next();
            objectTable.remove(buildKey(oid));
        }
    }

    public void beforeRollback(PBStateEvent event)
    {
        synchronizeWithTracedObjects();
        identitiesInWork.clear();
    }

    public void beforeCommit(PBStateEvent event)
    {
        // identitiesInWork.clear();
    }

    public void beforeClose(PBStateEvent event)
    {
        /*
        arminw: In managed environments listener method "beforeClose" is called twice
        (when the PB handle is closed and when the real PB instance is closed/returned to pool).
        We are only interested in the real close call when all work is done.
        */
        if(!broker.isInTransaction())
        {
            identitiesInWork.clear();
        }
    }

    public void afterRollback(PBStateEvent event)
    {
    }

    public void afterCommit(PBStateEvent event)
    {
        identitiesInWork.clear();
    }

    public void afterBegin(PBStateEvent event)
    {
    }

    public void beforeBegin(PBStateEvent event)
    {
    }

    public void afterOpen(PBStateEvent event)
    {
    }

    private CacheEntry buildEntry(Object obj, Identity oid)
    {
        if(useSoftReferences)
        {
            return new CacheEntrySoft(obj, oid, queue, timeout);
        }
        else
        {
            return new CacheEntryHard(obj, oid, timeout);
        }
    }

    private void processQueue()
    {
        CacheEntry sv;
        while((sv = (CacheEntry) queue.poll()) != null)
        {
            removeTracedIdentity(sv.getOid());
            objectTable.remove(buildKey(sv.getOid()));
        }
    }

    private Object buildKey(Identity oid)
    {
        Object key;
        switch(cachingKeyType)
        {
            case 0:
                key = oid;
                break;
            case 1:
                key = new OrderedTuple(oid, broker.getPBKey().getAlias());
                break;
            case 2:
                /*
                this ObjectCache implementation only works in single JVM, so the hashCode
                of the DescriptorRepository class is unique
                TODO: problem when different versions of same DR are used
                */
                key = new OrderedTuple(oid,
                        new Integer(broker.getDescriptorRepository().hashCode()));
                break;
            case 3:
                key = new OrderedTuple(oid, broker.getPBKey().getAlias(),
                        new Integer(broker.getDescriptorRepository().hashCode()));
                break;
            default:
                throw new OJBRuntimeException("Unexpected error, 'cacheType =" + cachingKeyType + "' was not supported");
        }
        return key;
    }


    //-----------------------------------------------------------
    // inner class to build unique key for cached objects
    //-----------------------------------------------------------
    /**
     * Implements equals() and hashCode() for an ordered tuple of constant(!)
     * objects
     *
     * @author Gerhard Grosse
     * @since Oct 12, 2004
     */
    static final class OrderedTuple
    {
        private static int[] multipliers =
                new int[]{13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 51};

        private Object[] elements;
        private int hashCode;

        public OrderedTuple(Object element)
        {
            elements = new Object[1];
            elements[0] = element;
            hashCode = calcHashCode();
        }

        public OrderedTuple(Object element1, Object element2)
        {
            elements = new Object[2];
            elements[0] = element1;
            elements[1] = element2;
            hashCode = calcHashCode();
        }

        public OrderedTuple(Object element1, Object element2, Object element3)
        {
            elements = new Object[3];
            elements[0] = element1;
            elements[1] = element2;
            elements[2] = element3;
            hashCode = calcHashCode();
        }

        public OrderedTuple(Object[] elements)
        {
            this.elements = elements;
            this.hashCode = calcHashCode();
        }

        private int calcHashCode()
        {
            int code = 7;
            for(int i = 0; i < elements.length; i++)
            {
                int m = i % multipliers.length;
                code += elements[i].hashCode() * multipliers[m];
            }
            return code;
        }

        public boolean equals(Object obj)
        {
            if(!(obj instanceof OrderedTuple))
            {
                return false;
            }
            else
            {
                OrderedTuple other = (OrderedTuple) obj;
                if(this.hashCode != other.hashCode)
                {
                    return false;
                }
                else if(this.elements.length != other.elements.length)
                {
                    return false;
                }
                else
                {
                    for(int i = 0; i < elements.length; i++)
                    {
                        if(!this.elements[i].equals(other.elements[i]))
                        {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }

        public int hashCode()
        {
            return hashCode;
        }

        public String toString()
        {
            StringBuffer s = new StringBuffer();
            s.append('{');
            for(int i = 0; i < elements.length; i++)
            {
                s.append(elements[i]).append('#').append(elements[i].hashCode()).append(',');
            }
            s.setCharAt(s.length() - 1, '}');
            s.append("#").append(hashCode);
            return s.toString();
        }
    }

    //-----------------------------------------------------------
    // inner classes to wrap cached objects
    //-----------------------------------------------------------
    interface CacheEntry
    {
        Object get();
        Identity getOid();
        long getLifetime();
    }

    final static class CacheEntrySoft extends SoftReference implements CacheEntry
    {
        private final long lifetime;
        private final Identity oid;

        CacheEntrySoft(Object object, final Identity k, final ReferenceQueue q, long timeout)
        {
            super(object, q);
            oid = k;
            // if timeout is negative, lifetime of object never expire
            if(timeout < 0)
            {
                lifetime = Long.MAX_VALUE;
            }
            else
            {
                lifetime = System.currentTimeMillis() + timeout;
            }
        }

        public Identity getOid()
        {
            return oid;
        }

        public long getLifetime()
        {
            return lifetime;
        }
    }

    final static class CacheEntryHard implements CacheEntry
    {
        private final long lifetime;
        private final Identity oid;
        private Object obj;

        CacheEntryHard(Object object, final Identity k, long timeout)
        {
            obj = object;
            oid = k;
            // if timeout is negative, lifetime of object never expire
            if(timeout < 0)
            {
                lifetime = Long.MAX_VALUE;
            }
            else
            {
                lifetime = System.currentTimeMillis() + timeout;
            }
        }

        public Object get()
        {
            return obj;
        }

        public Identity getOid()
        {
            return oid;
        }

        public long getLifetime()
        {
            return lifetime;
        }
    }
}
