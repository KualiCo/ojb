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

import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PBStateEvent;
import org.apache.ojb.broker.PBStateListener;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.core.DelegatingPersistenceBroker;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.MetadataException;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * A two-level {@link ObjectCache} implementation with a session- and an application cache. The application
 * cache could be specified by the property <code>applicationCache</code>.
 * <p/>
 * The first level is a transactional session
 * cache which cache objects till {@link org.apache.ojb.broker.PersistenceBroker#close()} or if
 * a PB-tx is running till {@link org.apache.ojb.broker.PersistenceBroker#abortTransaction()} or
 * {@link org.apache.ojb.broker.PersistenceBroker#commitTransaction()}. On commit all objects written to
 * database will be pushed to the application cache.
 * </p>
 * <p/>
 * The session cache act as a temporary storage for all read/store operations of persistent objects
 * and only on commit or close of the used PB instance the buffered objects of type
 * {@link #TYPE_WRITE} will be written to the application cache. Except objects of type
 * {@link #TYPE_NEW_MATERIALIZED} these objects will be immediatly pushed to application cache.
 * </p>
 * <p/>
 * <p/>
 * </p>
 * <p/>
 * The application cache
 * </p>
 * <p/>
 * <table cellspacing="2" cellpadding="2" border="3" frame="box">
 * <tr>
 * <td><strong>Property Key</strong></td>
 * <td><strong>Property Values</strong></td>
 * </tr>
 * <p/>
 * <tr>
 * <td>applicationCache</td>
 * <td>
 * Specifies the {@link ObjectCache} implementation used as application cache (second level cache).
 * By default {@link ObjectCacheDefaultImpl} was used. It's recommended to use a shared cache implementation
 * (all used PB instances should access the same pool of objects - e.g. by using a static Map in cache
 * implementation).
 * </td>
 * </tr>
 * <p/>
 * <tr>
 * <td>copyStrategy</td>
 * <td>
 * Specifies the implementation class of the {@link ObjectCacheTwoLevelImpl.CopyStrategy}
 * interface, which was used to copy objects on read and write to application cache. If not
 * specified a default implementation based was used ({@link ObjectCacheTwoLevelImpl.CopyStrategyImpl}
 * make field-descriptor based copies of the cached objects).
 * </td>
 * </tr>
 * <p/>
 * <tr>
 * <td>forceProxies</td>
 * <td>
 * If <em>true</em> on materialization of cached objects, all referenced objects will
 * be represented by proxy objects (independent from the proxy settings in reference- or
 * collection-descriptor).
 * <br/>
 * <strong>Note:</strong> To use this feature all persistence capable objects have to be
 * interface based <strong>or</strong> the <em>ProxyFactory</em> and
 * <em>IndirectionHandler</em> implementation classes supporting dynamic proxy enhancement
 * for all classes (see OJB.properties file).
 * </td>
 * </tr>
 * </table>
 * <p/>
 *
 * @version $Id: ObjectCacheTwoLevelImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class ObjectCacheTwoLevelImpl implements ObjectCacheInternal, PBStateListener
{
    private Logger log = LoggerFactory.getLogger(ObjectCacheTwoLevelImpl.class);

    public static final String APPLICATION_CACHE_PROP = "applicationCache";
    public static final String COPY_STRATEGY_PROP = "copyStrategy";
    public static final String FORCE_PROXIES = "forceProxies";
    private static final String DEF_COPY_STRATEGY = ObjectCacheTwoLevelImpl.CopyStrategyImpl.class.getName();
    private static final String DEF_APP_CACHE = ObjectCacheDefaultImpl.class.getName();

    private HashMap sessionCache;
    // private boolean enabledReadCache;
    private int invokeCounter;
    private ReferenceQueue queue = new ReferenceQueue();
    private ObjectCacheInternal applicationCache;
    private CopyStrategy copyStrategy;
    private PersistenceBrokerImpl broker;
    private boolean forceProxies = false;

    public ObjectCacheTwoLevelImpl(final PersistenceBroker broker, Properties prop)
    {
        // TODO: Fix cast. Cast is needed to get access to ReferenceBroker class in PBImpl, see method #lookup
        if(broker instanceof PersistenceBrokerImpl)
        {
            this.broker = (PersistenceBrokerImpl) broker;
        }
        else if(broker instanceof DelegatingPersistenceBroker)
        {
            this.broker = (PersistenceBrokerImpl) ((DelegatingPersistenceBroker) broker).getInnermostDelegate();
        }
        else
        {
            throw new RuntimeCacheException("Can't initialize two level cache, expect instance of"
                    + PersistenceBrokerImpl.class + " or of " + DelegatingPersistenceBroker.class
                    + " to setup application cache, but was " + broker);
        }
        this.sessionCache = new HashMap(100);
        // this.enabledReadCache = false;
        setupApplicationCache(this.broker, prop);
        // we add this instance as a permanent PBStateListener
        broker.addListener(this, true);
    }

    /**
     * Returns the {@link org.apache.ojb.broker.PersistenceBroker} instance associated with
     * this cache instance.
     */
    public PersistenceBrokerImpl getBroker()
    {
        return broker;
    }

    private void setupApplicationCache(PersistenceBrokerImpl broker, Properties prop)
    {
        if(log.isDebugEnabled()) log.debug("Start setup application cache for broker " + broker);
        if(prop == null)
        {
            prop = new Properties();
        }
        String copyStrategyName = prop.getProperty(COPY_STRATEGY_PROP, DEF_COPY_STRATEGY).trim();
        if(copyStrategyName.length() == 0)
        {
            copyStrategyName = DEF_COPY_STRATEGY;
        }
        String applicationCacheName = prop.getProperty(APPLICATION_CACHE_PROP, DEF_APP_CACHE).trim();
        if(applicationCacheName.length() == 0)
        {
            applicationCacheName = DEF_APP_CACHE;
        }
        
        String forceProxyValue = prop.getProperty(FORCE_PROXIES, "false").trim();
        forceProxies = Boolean.valueOf(forceProxyValue).booleanValue();
        
        if (forceProxies && broker.getProxyFactory().interfaceRequiredForProxyGeneration()){
            log.warn("'" + FORCE_PROXIES + "' is set to true, however a ProxyFactory implementation " +
                    "[" + broker.getProxyFactory().getClass().getName() +"] " +
                    " that requires persistent objects to implement an inteface is being used. Please ensure " +
                    "that all persistent objects implement an interface, or change the ProxyFactory setting to a dynamic " +
                    "proxy generator (like ProxyFactoryCGLIBImpl).");
        }
        
        Class[] type = new Class[]{PersistenceBroker.class, Properties.class};
        Object[] objects = new Object[]{broker, prop};
        try
        {
            this.copyStrategy = (CopyStrategy) ClassHelper.newInstance(copyStrategyName);
            Class target = ClassHelper.getClass(applicationCacheName);
            if(target.equals(ObjectCacheDefaultImpl.class))
            {
                // this property doesn't make sense in context of two-level cache
                prop.setProperty(ObjectCacheDefaultImpl.AUTOSYNC_PROP, "false");
            }
            ObjectCache temp = (ObjectCache) ClassHelper.newInstance(target, type, objects);
            if(!(temp instanceof ObjectCacheInternal))
            {
                log.warn("Specified application cache class doesn't implement '" + ObjectCacheInternal.class.getName()
                    + "'. For best interaction only specify caches implementing the internal object cache interface.");
                temp = new CacheDistributor.ObjectCacheInternalWrapper(temp);
            }
            this.applicationCache = (ObjectCacheInternal) temp;
        }
        catch(Exception e)
        {
            throw new MetadataException("Can't setup application cache. Specified application cache was '"
                    + applicationCacheName + "', copy strategy was '" + copyStrategyName + "'", e);
        }
        if(log.isEnabledFor(Logger.INFO))
        {
            ToStringBuilder buf = new ToStringBuilder(this);
            buf.append("copyStrategy", copyStrategyName)
                    .append("applicationCache", applicationCacheName);
            log.info("Setup cache: " + buf.toString());
        }
    }

    /**
     * Returns the application cache that this 2-level cache uses.
     * 
     * @return The application cache
     */
    public ObjectCacheInternal getApplicationCache()
    {
        return applicationCache;
    }

    private Object lookupFromApplicationCache(Identity oid)
    {
        Object result = null;
        Object obj = getApplicationCache().lookup(oid);
        if(obj != null)
        {
            result = copyStrategy.read(broker, obj);
        }
        return result;
    }

    private boolean putToApplicationCache(Identity oid, Object obj, boolean cacheIfNew)
    {
        /*
        we allow to reuse cached objects, so lookup the old cache object
        and forward it to the CopyStrategy
        */
        Object oldTarget = null;
        if(!cacheIfNew)
        {
            oldTarget = getApplicationCache().lookup(oid);
        }
        Object target = copyStrategy.write(broker, obj, oldTarget);
        if(cacheIfNew)
        {
            return getApplicationCache().cacheIfNew(oid, target);
        }
        else
        {
            getApplicationCache().cache(oid, target);
            return false;
        }
    }

    /**
     * Discard all session cached objects and reset the state of
     * this class for further usage.
     */
    public void resetSessionCache()
    {
        sessionCache.clear();
        invokeCounter = 0;
    }

    /**
     * Push all cached objects of the specified type, e.g. like {@link #TYPE_WRITE} to
     * the application cache and reset type to the specified one.
     */
    private void pushToApplicationCache(int typeToProcess, int typeAfterProcess)
    {
        for(Iterator iter = sessionCache.values().iterator(); iter.hasNext();)
        {
            CacheEntry entry = (CacheEntry) iter.next();
            // if the cached object was garbage collected, nothing to do
            Object result = entry.get();
            if(result == null)
            {
                if(log.isDebugEnabled())
                    log.debug("Object in session cache was gc, nothing to push to application cache");
            }
            else
            {
                // push all objects of the specified type to application cache
                if(entry.type == typeToProcess)
                {
                    if(log.isDebugEnabled())
                    {
                        log.debug("Move obj from session cache --> application cache : " + entry.oid);
                    }
                    /*
                    arminw:
                    only cache non-proxy or real subject of materialized proxy objects
                    */
                    if(ProxyHelper.isMaterialized(result))
                    {
                        putToApplicationCache(entry.oid, ProxyHelper.getRealObject(result), false);
                        // set the new type after the object was pushed to application cache
                        entry.type = typeAfterProcess;
                    }
                }
            }
        }
    }

    /**
     * Cache the given object. Creates a
     * {@link org.apache.ojb.broker.cache.ObjectCacheTwoLevelImpl.CacheEntry} and put it
     * to session cache. If the specified object to cache is of type {@link #TYPE_NEW_MATERIALIZED}
     * it will be immediately pushed to the application cache.
     */
    public void doInternalCache(Identity oid, Object obj, int type)
    {
        processQueue();
        // pass new materialized objects immediately to application cache
        if(type == TYPE_NEW_MATERIALIZED)
        {
            boolean result = putToApplicationCache(oid, obj, true);
            CacheEntry entry = new CacheEntry(oid, obj, TYPE_CACHED_READ, queue);
            if(result)
            {
                // as current session says this object is new, put it
                // in session cache
                putToSessionCache(oid, entry, false);
            }
            else
            {
                // object is not new, but if not in session cache
                // put it in
                putToSessionCache(oid, entry, true);
                if(log.isDebugEnabled())
                {
                    log.debug("The 'new' materialized object was already in cache," +
                            " will not push it to application cache: " + oid);
                }
            }
        }
        else
        {
            // other types of cached objects will only be put to the session
            // cache.
            CacheEntry entry = new CacheEntry(oid, obj, type, queue);
            putToSessionCache(oid, entry, false);
        }
    }

    /**
     * Lookup corresponding object from session cache or if not found from
     * the underlying real {@link ObjectCache} - Return <em>null</em> if no
     * object was found.
     */
    public Object lookup(Identity oid)
    {
        Object result = null;
        // 1. lookup an instance in session cache
        CacheEntry entry = (CacheEntry) sessionCache.get(oid);
        if(entry != null)
        {
            result = entry.get();
        }
        if(result == null)
        {
            result = lookupFromApplicationCache(oid);
            // 4. if we have a match
            // put object in session cache
            if(result != null)
            {
                doInternalCache(oid, result, TYPE_CACHED_READ);
                materializeFullObject(result);
                if(log.isDebugEnabled()) log.debug("Materialized object from second level cache: " + oid);
            }
        }
        if(result != null && log.isDebugEnabled())
        {
            log.debug("Match for: " + oid);
        }
        return result;
    }

    /**
     * This cache implementation cache only "flat" objects (persistent objects without any
     * references), so when {@link #lookup(org.apache.ojb.broker.Identity)} a cache object
     * it needs full materialization (assign all referenced objects) before the cache returns
     * the object. The materialization of the referenced objects based on the auto-XXX settings
     * specified in the metadata mapping.
     * <br/>
     * Override this method if needed in conjunction with a user-defined
     * {@link org.apache.ojb.broker.cache.ObjectCacheTwoLevelImpl.CopyStrategy}.
     *
     * @param target The "flat" object for full materialization
     */
    public void materializeFullObject(Object target)
    {
        ClassDescriptor cld = broker.getClassDescriptor(target.getClass());
        // don't force, let OJB use the user settings
        final boolean forced = false;
        if (forceProxies){
            broker.getReferenceBroker().retrieveProxyReferences(target, cld, forced);
            broker.getReferenceBroker().retrieveProxyCollections(target, cld, forced);
        }else{
            broker.getReferenceBroker().retrieveReferences(target, cld, forced);
            broker.getReferenceBroker().retrieveCollections(target, cld, forced);
        }    
    }

    /**
     * Remove the corresponding object from session AND application cache.
     */
    public void remove(Identity oid)
    {
        if(log.isDebugEnabled()) log.debug("Remove object " + oid);
        sessionCache.remove(oid);
        getApplicationCache().remove(oid);
    }

    /**
     * Clear session cache and application cache.
     */
    public void clear()
    {
        sessionCache.clear();
        getApplicationCache().clear();
    }

    /**
     * Put the specified object to session cache.
     */
    public void cache(Identity oid, Object obj)
    {
        doInternalCache(oid, obj, TYPE_UNKNOWN);
    }

    public boolean cacheIfNew(Identity oid, Object obj)
    {
        boolean result = putToApplicationCache(oid, obj, true);
        if(result)
        {
            CacheEntry entry = new CacheEntry(oid, obj, TYPE_CACHED_READ, queue);
            putToSessionCache(oid, entry, true);
        }
        return result;
    }

    /**
     * Put object to session cache.
     *
     * @param oid The {@link org.apache.ojb.broker.Identity} of the object to cache
     * @param entry The {@link org.apache.ojb.broker.cache.ObjectCacheTwoLevelImpl.CacheEntry} of the object
     * @param onlyIfNew Flag, if set <em>true</em> only new objects (not already in session cache) be cached.
     */
    private void putToSessionCache(Identity oid, CacheEntry entry, boolean onlyIfNew)
    {
        if(onlyIfNew)
        {
            // no synchronization needed, because session cache was used per broker instance
            if(!sessionCache.containsKey(oid)) sessionCache.put(oid, entry);
        }
        else
        {
            sessionCache.put(oid, entry);
        }
    }

    /**
     * Make sure that the Identity objects of garbage collected cached
     * objects are removed too.
     */
    private void processQueue()
    {
        CacheEntry sv;
        while((sv = (CacheEntry) queue.poll()) != null)
        {
            sessionCache.remove(sv.oid);
        }
    }

    //------------------------------------------------------------
    // PBStateListener methods
    //------------------------------------------------------------
    /**
     * After committing the transaction push the object
     * from session cache ( 1st level cache) to the application cache
     * (2d level cache). Finally, clear the session cache.
     */
    public void afterCommit(PBStateEvent event)
    {
        if(log.isDebugEnabled()) log.debug("afterCommit() call, push objects to application cache");
        if(invokeCounter != 0)
        {
            log.error("** Please check method calls of ObjectCacheTwoLevelImpl#enableMaterialization and" +
                    " ObjectCacheTwoLevelImpl#disableMaterialization, number of calls have to be equals **");
        }
        try
        {
            // we only push "really modified objects" to the application cache
            pushToApplicationCache(TYPE_WRITE, TYPE_CACHED_READ);
        }
        finally
        {
            resetSessionCache();
        }
    }

    /**
     * Before closing the PersistenceBroker ensure that the session
     * cache is cleared
     */
    public void beforeClose(PBStateEvent event)
    {
        /*
        arminw:
        this is a workaround for use in managed environments. When a PB instance is used
        within a container a PB.close call is done when leave the container method. This close
        the PB handle (but the real instance is still in use) and the PB listener are notified.
        But the JTA tx was not committed at
        this point in time and the session cache should not be cleared, because the updated/new
        objects will be pushed to the real cache on commit call (if we clear, nothing to push).
        So we check if the real broker is in a local tx (in this case we are in a JTA tx and the handle
        is closed), if true we don't reset the session cache.
        */
        if(!broker.isInTransaction())
        {
            if(log.isDebugEnabled()) log.debug("Clearing the session cache");
            resetSessionCache();
        }
    }

    /**
     * Before rollbacking clear the session cache (first level cache)
     */
    public void beforeRollback(PBStateEvent event)
    {
        if(log.isDebugEnabled()) log.debug("beforeRollback()");
        resetSessionCache();
    }

    public void afterOpen(PBStateEvent event)
    {
    }

    public void beforeBegin(PBStateEvent event)
    {
    }

    public void afterBegin(PBStateEvent event)
    {
    }

    public void beforeCommit(PBStateEvent event)
    {
    }

    public void afterRollback(PBStateEvent event)
    {
    }
    //------------------------------------------------------------

    //-----------------------------------------------------------
    // inner class
    //-----------------------------------------------------------

    /**
     * Helper class to wrap cached objects using {@link java.lang.ref.SoftReference}, which
     * allows to release objects when they no longer referenced within the PB session.
     */
    static final class CacheEntry extends SoftReference implements Serializable
    {
        private int type;
        private Identity oid;

        public CacheEntry(Identity oid, Object obj, int type, final ReferenceQueue q)
        {
            super(obj, q);
            this.oid = oid;
            this.type = type;
        }
    }


    public interface CopyStrategy
    {
        /**
         * Called when an object is read from the application cache (second level cache)
         * before the object is full materialized, see {@link ObjectCacheTwoLevelImpl#materializeFullObject(Object)}.
         *
         * @param broker The current used {@link org.apache.ojb.broker.PersistenceBroker} instance.
         * @param obj The object read from the application cache.
         * @return A copy of the object.
         */
        public Object read(PersistenceBroker broker, Object obj);

        /**
         * Called before an object is written to the application cache (second level cache).
         *
         * @param broker The current used {@link org.apache.ojb.broker.PersistenceBroker} instance.
         * @param obj The object to cache in application cache.
         * @param oldObject The old cache object or <em>null</em>
         * @return A copy of the object to write to application cache.
         */
        public Object write(PersistenceBroker broker, Object obj, Object oldObject);
    }

    public static class CopyStrategyImpl implements CopyStrategy
    {
        static final String CLASS_NAME_STR = "ojbClassName11";

        public CopyStrategyImpl()
        {
        }

        public Object read(PersistenceBroker broker, Object obj)
        {
            HashMap source = (HashMap) obj;
            String className = (String) source.get(CLASS_NAME_STR);
            ClassDescriptor cld = broker.getDescriptorRepository().getDescriptorFor(className);
            Object target = ClassHelper.buildNewObjectInstance(cld);
            // perform main object values
            FieldDescriptor[] flds = cld.getFieldDescriptor(true);
            FieldDescriptor fld;
            int length = flds.length;
            for(int i = 0; i < length; i++)
            {
                fld = flds[i];
                // read the field value
                Object value = source.get(fld.getPersistentField().getName());
                // copy the field value
                if(value != null) value = fld.getJdbcType().getFieldType().copy(value);
                // now make a field-conversion to java-type, because we only
                // the sql type of the field
                value = fld.getFieldConversion().sqlToJava(value);
                // set the copied field value in new object
                fld.getPersistentField().set(target, value);
            }
            return target;
        }

        public Object write(PersistenceBroker broker, Object obj, Object oldObject)
        {
            ClassDescriptor cld = broker.getClassDescriptor(obj.getClass());
            // we store field values by name in a Map
            HashMap target = oldObject != null ? (HashMap) oldObject : new HashMap();
            // perform main object values
            FieldDescriptor[] flds = cld.getFieldDescriptor(true);
            FieldDescriptor fld;
            int length = flds.length;
            for(int i = 0; i < length; i++)
            {
                fld = flds[i];
                // get the value
                Object value = fld.getPersistentField().get(obj);
                // convert value to a supported sql type
                value = fld.getFieldConversion().javaToSql(value);
                // copy the sql type
                value = fld.getJdbcType().getFieldType().copy(value);
                target.put(fld.getPersistentField().getName(), value);
            }
            target.put(CLASS_NAME_STR, obj.getClass().getName());
            return target;
        }
    }
}
