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
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.util.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * A global {@link ObjectCache} implementation using a JCS region for
 * each class. Each class name was associated with a dedicated
 * {@link ObjectCacheJCSImpl} instance to cache given objects.
 * This allows to define JCS cache region configuration properties
 * for each used class in JCS configuration files.
 *
 * <br/>
 * More info see <a href="http://jakarta.apache.org/turbine/jcs/index.html">
 * turbine-JCS</a>.
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
 * @author  Matthew Baird  (mattbaird@yahoo.com)
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ObjectCacheJCSPerClassImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */

public class ObjectCacheJCSPerClassImpl extends AbstractMetaCache
{
    private static Map cachesByClass = new HashMap();

    /**
     * Constructor for the MetaObjectCachePerClassImpl object
     */
    public ObjectCacheJCSPerClassImpl(PersistenceBroker broker, Properties prop)
    {
    }

    public ObjectCache getCache(Identity oid, Object obj, int methodCall)
    {
        if (oid.getObjectsRealClass() == null)
        {
            LoggerFactory.getDefaultLogger().info("[" + this.getClass()
                    + "] Can't get JCS cache, real class was 'null' for Identity: " + oid);
            return null;
        }
        return getCachePerClass(oid.getObjectsRealClass(), methodCall);
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
            else
            {
                it.remove();
            }
        }
    }

    /**
     * Gets the cache for the given class
     *
     * @param objectClass  The class to look up the cache for
     * @return             The cache
     */
    private ObjectCache getCachePerClass(Class objectClass, int methodCall)
    {
        ObjectCache cache = (ObjectCache) cachesByClass.get(objectClass.getName());
        if (cache == null && methodCall == AbstractMetaCache.METHOD_CACHE)
        {
            /**
             * the cache wasn't found, and the cachesByClass didn't contain the key with a
             * null value, so create a new cache for this classtype
             */
            cache = new ObjectCacheJCSImpl(objectClass.getName());
            cachesByClass.put(objectClass.getName(), cache);
        }
        return cache;
    }
}
