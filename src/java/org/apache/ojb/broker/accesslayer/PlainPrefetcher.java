package org.apache.ojb.broker.accesslayer;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.cache.ObjectCache;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.query.Query;

/**
 * Prefetcher for plain list of objects (no relations).
 *
 * @author <a href="mailto:olegnitz@apache.org">Oleg Nitz</a>
 * @version $Id: PlainPrefetcher.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class PlainPrefetcher extends BasePrefetcher
{

    public PlainPrefetcher(PersistenceBrokerImpl aBroker, Class anItemClass)
    {
        super(aBroker, anItemClass);
    }

    public void prepareRelationshipSettings()
    {
        // no op
    }

    public void restoreRelationshipSettings()
    {
        // no op
    }

    protected void associateBatched(Collection proxies, Collection realSubjects)
    {
        PersistenceBroker pb = getBroker();
        IndirectionHandler handler;
        Identity id;
        Object proxy;
        Object realSubject;
        HashMap realSubjectsMap = new HashMap(realSubjects.size());

        for (Iterator it = realSubjects.iterator(); it.hasNext(); )
        {
            realSubject = it.next();
            realSubjectsMap.put(pb.serviceIdentity().buildIdentity(realSubject), realSubject);
        }

        for (Iterator it = proxies.iterator(); it.hasNext(); )
        {
            proxy = it.next();
            handler = ProxyHelper.getIndirectionHandler(proxy);

            if (handler == null)
            {
                continue;
            }

            id = handler.getIdentity();
            realSubject = realSubjectsMap.get(id);
            if (realSubject != null)
            {
                handler.setRealSubject(realSubject);
            }
        }
    }

    /**
     * Build the multiple queries for one relationship because of limitation of IN(...)
     * @param proxies Collection containing all proxy objects to load
     * @param realSubjects Collection where real subjects found in the cache should be added.
     */
    protected Query[] buildPrefetchQueries(Collection proxies, Collection realSubjects)
    {
        Collection queries = new ArrayList();
        Collection idsSubset;
        Object proxy;
        IndirectionHandler handler;
        Identity id;
        Class realClass;
        HashMap classToIds = new HashMap();
        Class topLevelClass = getItemClassDescriptor().getClassOfObject();
        PersistenceBroker pb = getBroker();
        ObjectCache cache = pb.serviceObjectCache();

        for (Iterator it = proxies.iterator(); it.hasNext(); )
        {
            proxy = it.next();
            handler = ProxyHelper.getIndirectionHandler(proxy);

            if (handler == null)
            {
                continue;
            }
            
            id = handler.getIdentity();
            if (cache.lookup(id) != null)
            {
                realSubjects.add(pb.getObjectByIdentity(id));
                continue;
            }
            realClass = id.getObjectsRealClass();
            if (realClass == null)
            {
                realClass = Object.class; // to remember that the real class is unknown
            }
            idsSubset = (HashSet) classToIds.get(realClass);
            if (idsSubset == null)
            {
                idsSubset = new HashSet();
                classToIds.put(realClass, idsSubset);
            }
            idsSubset.add(id);
            if (idsSubset.size() == pkLimit)
            {
                Query query;
                if (realClass == Object.class)
                {
                    query = buildPrefetchQuery(topLevelClass, idsSubset, true);
                }
                else
                {
                    query = buildPrefetchQuery(realClass, idsSubset, false);
                }
                queries.add(query);
                idsSubset.clear();
            }
        }

        for (Iterator it = classToIds.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            realClass = (Class) entry.getKey();
            idsSubset = (HashSet) entry.getValue();
            if (idsSubset.size() > 0)
            {
                Query query;
                if (realClass == Object.class)
                {
                    query = buildPrefetchQuery(topLevelClass, idsSubset, true);
                }
                else
                {
                    query = buildPrefetchQuery(realClass, idsSubset, false);
                }
                queries.add(query);
            }
        }

        return (Query[]) queries.toArray(new Query[queries.size()]);
    }

    protected Query buildPrefetchQuery(Class clazz, Collection ids, boolean withExtents)
    {
        Query query = buildPrefetchQuery(clazz, ids,
                getDescriptorRepository().getDescriptorFor(clazz).getPkFields());
        query.setWithExtents(withExtents);
        return query;
    }
}
