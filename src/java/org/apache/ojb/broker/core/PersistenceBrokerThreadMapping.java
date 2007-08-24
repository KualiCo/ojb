package org.apache.ojb.broker.core;

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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.WeakHashMap;

import org.apache.ojb.broker.PBFactoryException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerInternal;

/**
 * Helper class that tracks correspondence between PersistenceBroker instances
 * and threads. The main task that this class solves is: to get current
 * PersistenceBroker for the given thread. For internal use only.
 *
 * @author Oleg Nitz
 * @version $Id: PersistenceBrokerThreadMapping.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PersistenceBrokerThreadMapping
{
    /**
     * A Collection of all HashMaps added to the <CODE>ThreadLocal currentBrokerMap</CODE> that are still -alive-.
     * If all the PersistenceBrokers are always correctly closed, the Collection should remain empty.
     * The Collection is iterated trough when calling the <CODE>shutdown()</CODE> method and all the maps that are
     * still alive will be cleared.
     */
    private static Collection loadedHMs = new HashSet();

    /**
     * The hashmap that maps PBKeys to current brokers for the thread
     */
    private static ThreadLocal currentBrokerMap = new ThreadLocal();

    /**
     * Mark a PersistenceBroker as preferred choice for current Thread
     *
     * @param key    The PBKey the broker is associated to
     * @param broker The PersistenceBroker to mark as current
     */
    public static void setCurrentPersistenceBroker(PBKey key, PersistenceBrokerInternal broker)
            throws PBFactoryException
    {
        HashMap map = (HashMap) currentBrokerMap.get();
        WeakHashMap set = null;
        if(map == null)
        {
            map = new HashMap();
            currentBrokerMap.set(map);

            loadedHMs.add(map);
        }
        else
        {
            set = (WeakHashMap) map.get(key);
        }

        if(set == null)
        {
            // We emulate weak HashSet using WeakHashMap
            set = new WeakHashMap();
            map.put(key, set);
        }
        set.put(broker, null);
    }

    /**
     * Unmark a PersistenceBroker as preferred choice for current Thread
     *
     * @param key    The PBKey the broker is associated to
     * @param broker The PersistenceBroker to unmark
     */
    public static void unsetCurrentPersistenceBroker(PBKey key, PersistenceBrokerInternal broker)
            throws PBFactoryException
    {
        HashMap map = (HashMap) currentBrokerMap.get();
        WeakHashMap set = null;
        if(map != null)
        {
            set = (WeakHashMap) map.get(key);
            if(set != null)
            {
                set.remove(broker);
                if(set.isEmpty())
                {
                    map.remove(key);
                }
            }
            if(map.isEmpty())
            {
                currentBrokerMap.set(null);
                loadedHMs.remove(map);
            }
        }
    }

    /**
     * Return the current open {@link org.apache.ojb.broker.PersistenceBroker}
     * instance for the given {@link org.apache.ojb.broker.PBKey}, if any.
     *
     * @param key
     * @return null if no open {@link org.apache.ojb.broker.PersistenceBroker} found.
     */
    public static PersistenceBrokerInternal currentPersistenceBroker(PBKey key)
            throws PBFactoryException, PersistenceBrokerException
    {
        HashMap map = (HashMap) currentBrokerMap.get();
        WeakHashMap set;
        PersistenceBrokerInternal broker = null;

        if(map == null)
        {
            return null;
        }

        set = (WeakHashMap) map.get(key);
        if(set == null)
        {
            return null;
        }

        // seek for an open broker, preferably in transaction
        for(Iterator it = set.keySet().iterator(); it.hasNext();)
        {
            PersistenceBrokerInternal tmp = (PersistenceBrokerInternal) it.next();
            if(tmp == null || tmp.isClosed())
            {
                it.remove();
                continue;
            }
            broker = tmp;
            if(tmp.isInTransaction())
            {
                break; // the best choice found
            }
        }
        return broker;
    }

    /**
     * Clean up static fields and any registered ThreadLocal contents to grant a clean
     * shutdown/reload of OJB within re/hot-deployable applications.
     */
    public static void shutdown()
    {
        for(Iterator it = loadedHMs.iterator(); it.hasNext();)
        {
            ((HashMap) it.next()).clear();
        }
        loadedHMs.clear();
        loadedHMs = null;
        currentBrokerMap = null;
    }
}
