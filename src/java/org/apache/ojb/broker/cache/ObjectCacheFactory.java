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

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Factory for {@link ObjectCache} implementation classes.
 * Each given {@link org.apache.ojb.broker.PersistenceBroker}
 * was associated with its own <code>ObjectCache</code> instance
 * and vice versa.
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: ObjectCacheFactory.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */

public class ObjectCacheFactory
{
    private Logger log = LoggerFactory.getLogger(ObjectCacheFactory.class);

    private static ObjectCacheFactory singleton = new ObjectCacheFactory();

    /**
     * Get the ObjectCacheFactory instance.
     */
    public static ObjectCacheFactory getInstance()
    {
        return singleton;
    }

    private ObjectCacheFactory()
    {
    }

    /**
     * Creates a new {@link ObjectCacheInternal} instance. Each <tt>ObjectCache</tt>
     * implementation was wrapped by a {@link CacheDistributor} and the distributor
     * was wrapped by {@link MaterializationCache}.
     *
     * @param broker The PB instance to associate with the cache instance
     */
    public MaterializationCache createObjectCache(PersistenceBroker broker)
    {
        CacheDistributor cache = null;
        try
        {
            log.info("Start creating new ObjectCache instance");
            /*
            1.
            if default cache was not found,
            create an new instance of the default cache specified in the
            configuration.
            2.
            Then instantiate AllocatorObjectCache to handle
            per connection/ per class caching instances.
            3.
            To support intern operations we wrap ObjectCache with an
            InternalObjectCache implementation
            */
            cache = new CacheDistributor(broker);
            log.info("Instantiate new " + cache.getClass().getName() + " for PB instance " + broker);
        }
        catch(Exception e)
        {
            log.error("Error while initiation, please check your configuration" +
                    " files and the used implementation class", e);
        }
        log.info("New ObjectCache instance was created");
        return new MaterializationCache(cache);
    }
}
