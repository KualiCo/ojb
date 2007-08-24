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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.metadata.ObjectCacheDescriptor;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * A intern used {@link AbstractMetaCache} implementation acting
 * as distributor of <code>ObjectCache</code> implementations declared
 * in configuration metadata.
 * <p/>
 * Reads the name of the used ObjectCache implementation
 * <br/>
 * a) from class-descriptor, or if not found
 * <br/>
 * b) from jdbc-connection-descriptor, or if not found
 * <br/>
 * use a given standard ObjectCache implementation (given by
 * constructor argument).
 * </p>
 *
 * @author Matthew Baird  (mattbaird@yahoo.com)
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: CacheDistributor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
class CacheDistributor implements ObjectCacheInternal
{
    private static Logger log = LoggerFactory.getLogger(CacheDistributor.class);
    private static final String DESCRIPTOR_BASED_CACHES = "descriptorBasedCaches";
    public static final String CACHE_EXCLUDES_STRING = "cacheExcludes";
    private static final String DELIMITER_FOR_EXCLUDE = ",";
    private static final ObjectCacheInternal DUMMY_CACHE =
            new ObjectCacheInternalWrapper(new ObjectCacheEmptyImpl(null, null));

    /**
     * map, represents used cache implementations
     */
    private Map caches = new HashMap();
    private List excludedPackages;

    private final PersistenceBroker broker;
    /**
     * If <code>true</code> the class name of the object is used
     * to find a per class {@link ObjectCache} implementation.
     * If set <code>false</code> the {@link ObjectCacheDescriptor}
     * instance is used as key to find a per class ObjectCache.
     */
    private boolean descriptorBasedCaches;

    /**
     * public Default Constructor
     */
    public CacheDistributor(final PersistenceBroker broker)
    {
        this.broker = broker;
        this.descriptorBasedCaches = OjbConfigurator.getInstance().getConfigurationFor(null)
                .getBoolean(DESCRIPTOR_BASED_CACHES, false);
        String exclude = broker.serviceConnectionManager().getConnectionDescriptor().getAttribute(CACHE_EXCLUDES_STRING);
        if(exclude != null)
        {
            exclude = exclude.trim();
            if(exclude.length() > 0)
            {
                excludedPackages = createExcludedPackagesList(exclude);
                log.info("Packages to exclude from caching: " + excludedPackages);
            }
        }
    }

    public void cache(Identity oid, Object obj)
    {
        getCache(oid.getObjectsTopLevelClass()).cache(oid, obj);
    }

    /**
     * @see ObjectCacheInternal#cacheIfNew(org.apache.ojb.broker.Identity, Object)
     */
    public boolean cacheIfNew(Identity oid, Object obj)
    {
        return getCache(oid.getObjectsTopLevelClass()).cacheIfNew(oid, obj);
    }

    public Object lookup(Identity oid)
    {
        return getCache(oid.getObjectsTopLevelClass()).lookup(oid);
    }

    public void remove(Identity oid)
    {
        getCache(oid.getObjectsTopLevelClass()).remove(oid);
    }

    public void clear()
    {
        synchronized(caches)
        {
            Iterator it = caches.values().iterator();
            ObjectCache oc = null;
            while(it.hasNext())
            {
                oc = (ObjectCache) it.next();
                try
                {
                    oc.clear();
                }
                catch(Exception e)
                {
                    log.error("Error while call method 'clear()' on '" + oc + "'", e);
                }
            }
        }
    }

    public void doInternalCache(Identity oid, Object obj, int type)
    {
        getCache(oid.getObjectsTopLevelClass()).doInternalCache(oid, obj, type);
    }

    public ObjectCacheInternal getCache(Class targetClass)
    {
        /*
        the priorities to find an ObjectCache for a specific object are:
        1. try to find a cache defined per class
        2. try to find a cache defined per jdbc-connection-descriptor
        */
        boolean useConnectionLevelCache = false;
        ObjectCacheInternal retval = null;
        /*
        first search in class-descriptor, then in jdbc-connection-descriptor
        for ObjectCacheDescriptor.
        */
        ObjectCacheDescriptor ocd = searchInClassDescriptor(targetClass);
        if(ocd == null)
        {
            ocd = searchInJdbcConnectionDescriptor();
            useConnectionLevelCache = true;
        }
        if(ocd == null)
        {
            throw new OJBRuntimeException("No object cache descriptor found for " + targetClass + ", using PBKey " + broker.getPBKey()
                    + ". Please set a cache descriptor in jdbc-connection-descriptor or in class-descriptor");
        }
        else
        {
            // use a class-descriptor level cache
            if(!useConnectionLevelCache)
            {
                if(!descriptorBasedCaches)
                {
                    synchronized(caches)
                    {
                        retval = lookupCache(targetClass);

                        if(retval == null)
                        {
                            if(log.isEnabledFor(Logger.INFO))
                            {
                                String eol = SystemUtils.LINE_SEPARATOR;
                                log.info(eol + "<====" + eol + "Setup new object cache instance on CLASS LEVEL for" + eol
                                        + "PersistenceBroker: " + broker + eol
                                        + "descriptorBasedCache: " + descriptorBasedCaches + eol
                                        + "Class: " + targetClass + eol
                                        + "ObjectCache: " + ocd + eol + "====>");
                            }
                            retval = prepareAndAddCache(targetClass, ocd);
                        }
                    }
                }
                else
                {
                    synchronized(caches)
                    {
                        retval = lookupCache(ocd);

                        if(retval == null)
                        {
                            if(log.isEnabledFor(Logger.INFO))
                            {
                                String eol = SystemUtils.LINE_SEPARATOR;
                                log.info(eol + "<====" + eol + "Setup new object cache instance on CLASS LEVEL for" + eol
                                        + "PersistenceBroker: " + broker + eol
                                        + "descriptorBasedCache: " + descriptorBasedCaches + eol
                                        + "class: " + targetClass + eol
                                        + "ObjectCache: " + ocd + eol + "====>");
                            }
                            retval = prepareAndAddCache(ocd, ocd);
                        }
                    }
                }
            }
            // use a jdbc-connection-descriptor level cache
            else
            {
                if(isExcluded(targetClass))
                {
                    if(log.isDebugEnabled()) log.debug("Class '" + targetClass.getName() + "' is excluded from being cached");
                    retval = DUMMY_CACHE;
                }
                else
                {
                    String jcdAlias = broker.serviceConnectionManager().getConnectionDescriptor().getJcdAlias();
                    synchronized(caches)
                    {
                        retval = lookupCache(jcdAlias);

                        if(retval == null)
                        {
                            if(log.isEnabledFor(Logger.INFO))
                            {
                                String eol = SystemUtils.LINE_SEPARATOR;
                                log.info(eol + "<====" + eol + "Setup new object cache instance on CONNECTION LEVEL for" + eol
                                        + "PersistenceBroker: " + broker + eol
                                        + "descriptorBasedCache: " + descriptorBasedCaches + eol
                                        + "Connection jcdAlias: " + jcdAlias + eol
                                        + "Calling class: " + targetClass
                                        + "ObjectCache: " + ocd + eol + "====>");
                            }
                            retval = prepareAndAddCache(jcdAlias, ocd);
                        }
                    }
                }
            }
        }
        return retval;
    }

    private ObjectCacheInternal prepareAndAddCache(Object key, ObjectCacheDescriptor ocd)
    {
        ObjectCacheInternal cache;
        // before the synchronize method lock this,
        // another thread maybe added same key
        if((cache = lookupCache(key)) != null)
        {
            log.info("Key '" + key + "' was already in use no need to create the ObjectCache instance again");
        }
        else
        {
            if(log.isDebugEnabled()) log.debug("Create new ObjectCache implementation for " + key);
            try
            {
                ObjectCache temp = (ObjectCache) ClassHelper.newInstance(ocd.getObjectCache(),
                        new Class[]{PersistenceBroker.class, Properties.class},
                        new Object[]{broker, ocd.getConfigurationProperties()});
                if(temp instanceof ObjectCacheInternal)
                {
                    cache = (ObjectCacheInternal) temp;
                }
                else
                {
                    log.info("Specified cache " + ocd.getObjectCache() + " does not implement "
                            + ObjectCacheInternal.class + " and will be wrapped by a helper class");
                    cache = new ObjectCacheInternalWrapper(temp);
                }
            }
            catch(Exception e)
            {
                log.error("Can not create ObjectCache instance using class " + ocd.getObjectCache(), e);
                throw new OJBRuntimeException(e);
            }
            caches.put(key, cache);
        }
        return cache;
    }

    private ObjectCacheInternal lookupCache(Object key)
    {
        return (ObjectCacheInternal) caches.get(key);
    }

    private List createExcludedPackagesList(String theList)
    {
        StringTokenizer tok = new StringTokenizer(theList, DELIMITER_FOR_EXCLUDE);
        String token = null;
        ArrayList result = new ArrayList();
        while(tok.hasMoreTokens())
        {
            token = tok.nextToken().trim();
            if(token.length() > 0) result.add(token);
        }
        return result;
    }

    private boolean isExcluded(Class targetClass)
    {
        if(excludedPackages != null)
        {
            String name = targetClass.getName();
            for(int i = 0; i < excludedPackages.size(); i++)
            {
                String exclude = (String) excludedPackages.get(i);
                if(name.startsWith(exclude))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Try to lookup {@link ObjectCacheDescriptor} in
     * {@link org.apache.ojb.broker.metadata.ClassDescriptor}.
     *
     * @param targetClass
     * @return Returns the found {@link ObjectCacheDescriptor} or <code>null</code>
     *         if none was found.
     */
    protected ObjectCacheDescriptor searchInClassDescriptor(Class targetClass)
    {
        return targetClass != null ? broker.getClassDescriptor(targetClass).getObjectCacheDescriptor() : null;
    }

    /**
     * Lookup {@link ObjectCacheDescriptor} in
     * {@link org.apache.ojb.broker.metadata.JdbcConnectionDescriptor}.
     *
     * @return Returns the found {@link ObjectCacheDescriptor} or <code>null</code>
     *         if none was found.
     */
    protected ObjectCacheDescriptor searchInJdbcConnectionDescriptor()
    {
        return broker.serviceConnectionManager().getConnectionDescriptor().getObjectCacheDescriptor();
    }

    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE);
        return buf.append("Associated PB", broker)
                .append("Mapped caches", caches).toString();
    }

    //=================================================
    // inner class
    //=================================================
    /**
     * Wrapper class used to make existing {@link ObjectCache} implementations work
     * with {@link ObjectCacheInternal}.
     */
    static final class ObjectCacheInternalWrapper implements ObjectCacheInternal
    {
        ObjectCache cache = null;

        public ObjectCacheInternalWrapper(ObjectCache cache)
        {
            this.cache = cache;
        }

        public void doInternalCache(Identity oid, Object obj, int type)
        {
            cache(oid, obj);
        }

        public void doInternalClear()
        {
            // noop
        }

        public boolean contains(Identity oid)
        {
            return cache.lookup(oid) != null;
        }

        public void cache(Identity oid, Object obj)
        {
            cache.cache(oid, obj);
        }

        public boolean cacheIfNew(Identity oid, Object obj)
        {
            cache.cache(oid, obj);
            return true;
        }

        public Object lookup(Identity oid)
        {
            return cache.lookup(oid);
        }

        public void remove(Identity oid)
        {
            cache.remove(oid);
        }

        public void clear()
        {
            cache.clear();
        }
    }

}
