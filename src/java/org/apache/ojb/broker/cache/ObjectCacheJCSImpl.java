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

import java.util.Properties;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;
import org.apache.jcs.access.exception.ObjectExistsException;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;

/**
 * This local {@link ObjectCache} implementation using
 * <a href="http://jakarta.apache.org/turbine/jcs/index.html">
 * turbine-JCS</a> to cache objects is primarily for intern use in
 * conjunction with {@link ObjectCacheJCSPerClassImpl} implementation. If
 * used as main <code>ObjectCache</code> all cached objects will be cached
 * under the same JCS region name (see {@link #DEFAULT_REGION}).
 * <p/>
 * <p/>
 * Implementation configuration properties:
 * </p>
 * <p/>
 * <table cellspacing="2" cellpadding="2" border="3" frame="box">
 * <tr>
 * <td><strong>Property Key</strong></td>
 * <td><strong>Property Values</strong></td>
 * </tr>
 * <tr>
 * <td> - </td>
 * <td>
 * -
 * </td>
 * </tr>
 * </table>
 *
 * @author Matthew Baird (mattbaird@yahoo.com);
 * @version $Id: ObjectCacheJCSImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class ObjectCacheJCSImpl implements ObjectCache
{
    /**
     * The used default region name.
     */
    public static final String DEFAULT_REGION = "ojbDefaultJCSRegion";

    private JCS jcsCache;
    /**
     * if no regionname is passed in, we use the default region.
     */
    private String regionName = DEFAULT_REGION;

    public ObjectCacheJCSImpl(PersistenceBroker broker, Properties prop)
    {
        this(null);
    }

    /**
     * Constructor used by the {@link ObjectCacheJCSPerClassImpl}
     */
    public ObjectCacheJCSImpl(String name)
    {
        regionName = (name != null ? name : DEFAULT_REGION);
        try
        {
            jcsCache = JCS.getInstance(regionName);
        }
        catch(Exception e)
        {
            throw new RuntimeCacheException("Can't instantiate JCS ObjectCacheImplementation", e);
        }
    }

    public String getRegionName()
    {
        return regionName;
    }

    /**
     * makes object obj persistent to the Objectcache under the key oid.
     */
    public void cache(Identity oid, Object obj)
    {
        try
        {
            jcsCache.put(oid.toString(), obj);
        }
        catch (CacheException e)
        {
            throw new RuntimeCacheException(e);
        }
    }

    public boolean cacheIfNew(Identity oid, Object obj)
    {
        boolean result = false;
        try
        {
            jcsCache.putSafe(oid.toString(), obj);
            result = true;
        }
        catch(ObjectExistsException e)
        {
            // do nothing, object already in cache
        }
        catch(CacheException e)
        {
            throw new RuntimeCacheException(e);
        }
        return result;
    }

    /**
     * Lookup object with Identity oid in objectTable.
     * returns null if no matching id is found
     */
    public Object lookup(Identity oid)
    {
        return jcsCache.get(oid.toString());
    }

    /**
     * removes an Object from the cache.
     *
     * @param oid the Identity of the object to be removed.
     */
    public void remove(Identity oid)
    {
        try
        {
            jcsCache.remove(oid.toString());
        }
        catch (CacheException e)
        {
            throw new RuntimeCacheException(e.getMessage());
        }
    }

    /**
     * clear the ObjectCache.
     */
    public void clear()
    {
        if (jcsCache != null)
        {
            try
            {
                jcsCache.remove();
            }
            catch (CacheException e)
            {
                throw new RuntimeCacheException(e);
            }
        }
    }

    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE);
        buf.append("JCS region name", regionName);
        buf.append("JCS region", jcsCache);
        return buf.toString();
    }
}

