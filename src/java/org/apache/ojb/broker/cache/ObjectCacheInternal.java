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
 * Internal used extension of the {@link ObjectCache}.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: ObjectCacheInternal.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public interface ObjectCacheInternal extends ObjectCache
{
    /**
     * Object was update or insert.
     */
    public static final int TYPE_WRITE = 5;
    /**
     * Object was read from a cache entity (e.g. from a second-level cache).
     */
    public static final int TYPE_CACHED_READ = 7;
    /**
     * Object was new materialized from persistence storage.
     */
    public static final int TYPE_NEW_MATERIALIZED = 11;
    /**
     * Object caching type was unkown.
     */
    public static final int TYPE_UNKNOWN = 0;
    /**
     * Object caching type used for temporary storage of objects,
     * these objects will never be pushed to a higher level cache.
     */
    public static final int TYPE_TEMP = -1;


    /**
     * For internal use.
     * This method have to be used by all OJB classes to cache objects.
     * It allows to decide if an object should be cached or not. Useful
     * for two level caches to reduce object copy costs.
     */
    public void doInternalCache(Identity oid, Object obj, int type);

    /**
     * For internal use within <em>ObjectCache</em> implementations or to
     * build two-level caches. Handle with care.
     * <p>
     * Used to cache new objects (not already cached) by it's
     * {@link org.apache.ojb.broker.Identity}. This method was used to
     * cache new materialized objects and should work as a "atomic" method
     * (the check and the put of the object should be atomic) to avoid
     * concurrency problems.
     * </p>
     * <p>
     * Currently it's not mandatory that all <em>ObjectCache</em> implementations
     * support this method, so in some cases it's allowed to delegate this
     * method call to the standard {@link #cache(org.apache.ojb.broker.Identity, Object) cache}.
     * </p>
     *
     * @param oid Identity of the object to cache.
     * @param obj The object to cache.
     * @return If object was added <em>true</em>, else <em>false</em>.
     */
    public boolean cacheIfNew(Identity oid, Object obj);
}
