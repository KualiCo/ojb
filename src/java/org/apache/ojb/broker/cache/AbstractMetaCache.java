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

/**
 * An abstract 'meta' implementation of the {@link ObjectCache}
 * interace.
 * <br/>
 * Implement the abstract {@link #getCache} method in sub-classes.
 * All base Object/Identity validation is done by this class.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: AbstractMetaCache.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public abstract class AbstractMetaCache implements ObjectCache
{
    public static final int METHOD_CACHE = 1;
    public static final int METHOD_LOOKUP = 2;
    public static final int METHOD_REMOVE = 3;

    /**
     * This method handle all calls against the {@link ObjectCache} interface.
     * Note: The parameter <code>obj</code> can be <code>null</code> - e.g. when
     * lookup or remove method was called.
     *
     * @param oid Identity of the target object.
     * @param obj The target object itself or <code>null</code> if not available.
     * @param callingMethod Specifies the type of method call against the {@link ObjectCache}
     * interface. {@link #METHOD_CACHE}, {@link #METHOD_LOOKUP}, {@link #METHOD_REMOVE}.
     * @return The {@link ObjectCache} implementation.
     */
    public abstract ObjectCache getCache(Identity oid, Object obj, int callingMethod);

    /**
     * Caches the given object using the given Identity as key
     *
     * @param oid  The Identity key
     * @param obj  The object o cache
     */
    public void cache(Identity oid, Object obj)
    {
        if (oid != null && obj != null)
        {
            ObjectCache cache = getCache(oid, obj, METHOD_CACHE);
            if (cache != null)
            {
                cache.cache(oid, obj);
            }
        }
    }

    /**
     * We delegate this method to the standard cache method.
     * <br/>
     * ++ Override if needed ++
     */
    public boolean cacheIfNew(Identity oid, Object obj)
    {
        cache(oid, obj);
        return true;
    }

    /**
     * Looks up the object from the cache
     *
     * @param oid  The Identity to look up the object for
     * @return     The object if found, otherwise null
     */
    public Object lookup(Identity oid)
    {
        Object ret = null;
        if (oid != null)
        {
            ObjectCache cache = getCache(oid, null, METHOD_LOOKUP);
            if (cache != null)
            {
                ret = cache.lookup(oid);
            }
        }
        return ret;
    }

    /**
     * Removes the given object from the cache
     *
     * @param oid  oid of the object to remove
     */
    public void remove(Identity oid)
    {
        if (oid == null) return;

        ObjectCache cache = getCache(oid, null, METHOD_REMOVE);
        if (cache != null)
        {
            cache.remove(oid);
        }
    }
}
