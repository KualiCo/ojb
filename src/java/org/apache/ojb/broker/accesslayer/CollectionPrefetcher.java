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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.IdentityFactory;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.FieldHelper;
import org.apache.ojb.broker.metadata.MetadataException;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.collections.RemovalAwareCollection;
import org.apache.ojb.broker.util.collections.RemovalAwareList;
import org.apache.ojb.broker.util.collections.RemovalAwareSet;

/**
 * Relationship Prefetcher for Collections.
 *
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: CollectionPrefetcher.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class CollectionPrefetcher extends RelationshipPrefetcherImpl
{

    /**
     * Constructor for CollectionPrefetcher.
     *
     * @param aBroker
     * @param anOrd
     */
    public CollectionPrefetcher(PersistenceBrokerImpl aBroker, ObjectReferenceDescriptor anOrd)
    {
        super(aBroker, anOrd);
    }

    /**
     * Build the multiple queries for one relationship because of limitation of IN(...)
     *
     * @param owners Collection containing all objects of the ONE side
     */
    protected Query[] buildPrefetchQueries(Collection owners, Collection children)
    {
        ClassDescriptor cld = getOwnerClassDescriptor();
        Class topLevelClass = getBroker().getTopLevelClass(cld.getClassOfObject());
        BrokerHelper helper = getBroker().serviceBrokerHelper();
        Collection queries = new ArrayList(owners.size());
        Collection idsSubset = new HashSet(owners.size());
        Object[] fkValues;
        Object owner;
        Identity id;

        Iterator iter = owners.iterator();
        while (iter.hasNext())
        {
            owner = iter.next();
            fkValues = helper.extractValueArray(helper.getKeyValues(cld, owner));
            id = getBroker().serviceIdentity().buildIdentity(null, topLevelClass, fkValues);
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
     * Build the query to perform a batched read get orderBy settings from CollectionDescriptor
     *
     * @param ids Collection containing all identities of objects of the ONE side
     */
    protected Query buildPrefetchQuery(Collection ids)
    {
        CollectionDescriptor cds = getCollectionDescriptor();
        QueryByCriteria query = buildPrefetchQuery(ids, cds.getForeignKeyFieldDescriptors(getItemClassDescriptor()));

        // check if collection must be ordered
        if (!cds.getOrderBy().isEmpty())
        {
            Iterator iter = cds.getOrderBy().iterator();
            while (iter.hasNext())
            {
                query.addOrderBy((FieldHelper) iter.next());
            }
        }

        return query;
    }

    /**
     * associate the batched Children with their owner object loop over children
     */
    protected void associateBatched(Collection owners, Collection children)
    {
        CollectionDescriptor cds = getCollectionDescriptor();
        PersistentField field = cds.getPersistentField();
        PersistenceBroker pb = getBroker();
        Class ownerTopLevelClass = pb.getTopLevelClass(getOwnerClassDescriptor().getClassOfObject());
        Class collectionClass = cds.getCollectionClass(); // this collection type will be used:
        HashMap ownerIdsToLists = new HashMap(owners.size());

        IdentityFactory identityFactory = pb.serviceIdentity();
        // initialize the owner list map
        for (Iterator it = owners.iterator(); it.hasNext();)
        {
            Object owner = it.next();
            ownerIdsToLists.put(identityFactory.buildIdentity(getOwnerClassDescriptor(), owner), new ArrayList());
        }

        // build the children lists for the owners
        for (Iterator it = children.iterator(); it.hasNext();)
        {
            Object child = it.next();
            // BRJ: use cld for real class, relatedObject could be Proxy
            ClassDescriptor cld = getDescriptorRepository().getDescriptorFor(ProxyHelper.getRealClass(child));

            Object[] fkValues = cds.getForeignKeyValues(child, cld);
            Identity ownerId = identityFactory.buildIdentity(null, ownerTopLevelClass, fkValues);
            List list = (List) ownerIdsToLists.get(ownerId);
            if (list != null)
            {
                list.add(child);
            }
        }

        // connect children list to owners
        for (Iterator it = owners.iterator(); it.hasNext();)
        {
            Object result;
            Object owner = it.next();
            Identity ownerId = identityFactory.buildIdentity(owner);
            List list = (List) ownerIdsToLists.get(ownerId);

            if ((collectionClass == null) && field.getType().isArray())
            {
                int length = list.size();
                Class itemtype = field.getType().getComponentType();
                result = Array.newInstance(itemtype, length);
                for (int j = 0; j < length; j++)
                {
                    Array.set(result, j, list.get(j));
                }
            }
            else
            {
                ManageableCollection col = createCollection(cds, collectionClass);
                for (Iterator it2 = list.iterator(); it2.hasNext();)
                {
                    col.ojbAdd(it2.next());
                }
                result = col;
            }

            Object value = field.get(owner);
            if ((value instanceof CollectionProxyDefaultImpl) && (result instanceof Collection))
            {
                ((CollectionProxyDefaultImpl) value).setData((Collection) result);
            }
            else
            {
                field.set(owner, result);
            }
        }
    }

    /**
     * Create a collection object of the given collection type. If none has been given,
     * OJB uses RemovalAwareList, RemovalAwareSet, or RemovalAwareCollection depending
     * on the field type. 
     * 
     * @param desc            The collection descriptor
     * @param collectionClass The collection class specified in the collection-descriptor
     * @return The collection object
     */
    protected ManageableCollection createCollection(CollectionDescriptor desc, Class collectionClass)
    {
        Class                fieldType = desc.getPersistentField().getType();
        ManageableCollection col;

        if (collectionClass == null)
        {
            if (ManageableCollection.class.isAssignableFrom(fieldType))
            {
                try
                {
                    col = (ManageableCollection)fieldType.newInstance();
                }
                catch (Exception e)
                {
                    throw new OJBRuntimeException("Cannot instantiate the default collection type "+fieldType.getName()+" of collection "+desc.getAttributeName()+" in type "+desc.getClassDescriptor().getClassNameOfObject());
                }
            }
            else if (fieldType.isAssignableFrom(RemovalAwareCollection.class))
            {
                col = new RemovalAwareCollection();
            }
            else if (fieldType.isAssignableFrom(RemovalAwareList.class))
            {
                col = new RemovalAwareList();
            }
            else if (fieldType.isAssignableFrom(RemovalAwareSet.class))
            {
                col = new RemovalAwareSet();
            }
            else
            {
                throw new MetadataException("Cannot determine a default collection type for collection "+desc.getAttributeName()+" in type "+desc.getClassDescriptor().getClassNameOfObject());
            }
        }
        else
        {
            try
            {
                col = (ManageableCollection)collectionClass.newInstance();
            }
            catch (Exception e)
            {
                throw new OJBRuntimeException("Cannot instantiate the collection class "+collectionClass.getName()+" of collection "+desc.getAttributeName()+" in type "+desc.getClassDescriptor().getClassNameOfObject());
            }
        }
        return col;
    }

    protected CollectionDescriptor getCollectionDescriptor()
    {
        return (CollectionDescriptor) getObjectReferenceDescriptor();
    }
}
