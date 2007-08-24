package org.apache.ojb.broker.accesslayer;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.cache.ObjectCache;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.query.Query;

/**
 * Relationship Prefetcher for References.
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: ReferencePrefetcher.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class ReferencePrefetcher extends RelationshipPrefetcherImpl
{

    /**
    * Constructor for ReferencePrefetcher.
    * @param aBroker
    * @param anOrd
    */
    public ReferencePrefetcher(PersistenceBrokerImpl aBroker, ObjectReferenceDescriptor anOrd)
    {
        super(aBroker, anOrd);
    }

    /**
     * Associate the batched Children with their owner object.
     * Loop over owners
     */
    protected void associateBatched(Collection owners, Collection children)
    {
        ObjectReferenceDescriptor ord = getObjectReferenceDescriptor();
        ClassDescriptor cld = getOwnerClassDescriptor();
        Object owner;
        Object relatedObject;
        Object fkValues[];
        Identity id;
        PersistenceBroker pb = getBroker();
        PersistentField field = ord.getPersistentField();
        Class topLevelClass = pb.getTopLevelClass(ord.getItemClass());
        HashMap childrenMap = new HashMap(children.size());


        for (Iterator it = children.iterator(); it.hasNext(); )
        {
            relatedObject = it.next();
            childrenMap.put(pb.serviceIdentity().buildIdentity(relatedObject), relatedObject);
        }

        for (Iterator it = owners.iterator(); it.hasNext(); )
        {
            owner = it.next();
            fkValues = ord.getForeignKeyValues(owner,cld);
            if (isNull(fkValues))
            {
                field.set(owner, null);
                continue;
            }
            id = pb.serviceIdentity().buildIdentity(null, topLevelClass, fkValues);
            relatedObject = childrenMap.get(id);
            field.set(owner, relatedObject);
        }
    }

    private boolean isNull(Object[] arr)
    {
        for (int i = 0; i < arr.length; i++)
        {
            if (arr[i] != null)
            {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Build the multiple queries for one relationship because of limitation of IN(...)
     * @param owners Collection containing all objects of the ONE side
     * @param children Collection where related objects found in the cache should be added.
     */
    protected Query[] buildPrefetchQueries(Collection owners, Collection children)
    {
        ClassDescriptor cld = getOwnerClassDescriptor();
        ObjectReferenceDescriptor ord = getObjectReferenceDescriptor();
        Collection queries = new ArrayList(owners.size());
        Collection idsSubset = new HashSet(owners.size());
        Iterator iter = owners.iterator();
        Class topLevelClass = getBroker().getTopLevelClass(ord.getItemClass());
        Object[] fkValues;
        Object owner;
        Identity id;
        PersistenceBroker pb = getBroker();
        ObjectCache cache = pb.serviceObjectCache();

        while (iter.hasNext())
        {
            owner = iter.next();
            fkValues = ord.getForeignKeyValues(owner,cld);
            if (isNull(fkValues))
            {
                continue;
            }
            id = pb.serviceIdentity().buildIdentity(null, topLevelClass, fkValues);
            if (cache.lookup(id) != null)
            {
                children.add(pb.getObjectByIdentity(id));
                continue;
            }
            idsSubset.add(id);
            if (idsSubset.size() == pkLimit)
            {
                queries.add(buildPrefetchQuery(idsSubset));
                idsSubset.clear();
            }
        }

        if (idsSubset.size() > 0)
        {
            queries.add(buildPrefetchQuery(idsSubset));
        }

        return (Query[]) queries.toArray(new Query[queries.size()]);
    }

    /**
     * @see org.apache.ojb.broker.accesslayer.RelationshipPrefetcherImpl#buildPrefetchCriteria(java.util.Collection, org.apache.ojb.broker.metadata.FieldDescriptor[])
     */
    protected Query buildPrefetchQuery(Collection ids)
    {
        return buildPrefetchQuery(ids, getItemClassDescriptor().getPkFields());
    }
}
