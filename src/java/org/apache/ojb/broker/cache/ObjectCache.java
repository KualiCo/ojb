package org.apache.ojb.broker.cache;

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

import org.apache.ojb.broker.Identity;

/**
 * The <code>ObjectCache</code> stores all Objects loaded by the
 * {@link org.apache.ojb.broker.PersistenceBroker} from a DB.
 * When the PersistenceBroker tries to get an Object by its Primary key values
 * it first lookups the cache if the object has been already loaded and cached.
 * <br/><br/>
 * Using an ObjectCache has several advantages:
 * - it increases performance as it reduces DB lookups.
 * - it allows to perform circular lookups (as by crossreferenced objects)
 * that would result in non-terminating loops without such a cache. This will be internally handled by OJB, no
 * need to take care of this.
 * - it maintains the uniqueness of objects as any Db row will be mapped to
 * exactly one object.
 * <br/><br/>
 * This interface allows to have userdefined Cache implementations.
 * The ObjectCacheFactory is responsible for generating cache instances.
 * by default it uses the OJB {@link ObjectCacheDefaultImpl}.
 * <br/><br/>
 * <b>Note:</b> Each {@link org.apache.ojb.broker.PersistenceBroker} was
 * associated with its own <code>ObjectCache</code> instance at creation
 * time.
 * <br/>
 * {@link ObjectCacheFactory} is responsible for creating <code>ObjectCache</code>
 * instances. To make the <code>ObjectCache</code> implementation work, a
 * constructor with {@link org.apache.ojb.broker.PersistenceBroker} and
 * {@link java.util.Properties} as arguments or only <code>PersistenceBroker</code>
 * argument is needed.
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @author <a href="mailto:armin@codeaulait.de">Armin Waibel<a>
 *
 * @version $Id: ObjectCache.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public interface ObjectCache
{
    /**
     * Used to cache objects by it's {@link org.apache.ojb.broker.Identity}.
     *
     * @param oid Identity of the object to cache.
     * @param obj The object to cache.
     */
    public void cache(Identity oid, Object obj);

    /**
     * Lookup object with Identity 'oid' in cache.
     *
     * @param oid Identity of the object to search for.
     * @return The cached object or <em>null</em> if no matching object for
     * specified {@link org.apache.ojb.broker.Identity} is found.
     */
    public Object lookup(Identity oid);

    /**
     * Removes an Object from the cache.
     *
     * @param oid Identity of the object to be removed.
     */
    public void remove(Identity oid);

    /**
     * Clear the cache.
     */
    public void clear();
}
