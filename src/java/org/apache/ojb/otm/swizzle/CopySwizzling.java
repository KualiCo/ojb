package org.apache.ojb.otm.swizzle;

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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.cache.ObjectCache;
import org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.ListProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.SetProxyDefaultImpl;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;

public class CopySwizzling implements Swizzling
{

    /**
     * @see org.apache.ojb.otm.swizzle.Swizzling#swizzle(Object, Object, PersistenceBroker, ObjectCache)
     */
    public Object swizzle(Object newObj, Object oldObj, PersistenceBroker pb,
                          ObjectCache cache)
    {
        if (newObj == null) // invalidating
        {
            return null;
        }

        if (oldObj == null)
        {
            return newObj;
        }

        if (!newObj.getClass().equals(oldObj.getClass()))
        {
            System.err.println("Cannot swizzle objects of different classes: "
                    + newObj.getClass() + " and " + oldObj.getClass());
            return newObj;
        }

        ClassDescriptor mif = pb.getClassDescriptor(newObj.getClass());
        FieldDescriptor[] fieldDescs = mif.getFieldDescriptions();

        for (int i = 0; i < fieldDescs.length; i++)
        {
            FieldDescriptor fd = fieldDescs[i];
            PersistentField f = fd.getPersistentField();
            f.set(oldObj, f.get(newObj));
        }

        // N:1 relations
        Iterator iter = mif.getObjectReferenceDescriptors().iterator();
        ObjectReferenceDescriptor rds;
        PersistentField field;
        Object newRelObj;
        Identity newRelOid;
        Object oldRelObj;

        while (iter.hasNext())
        {
            rds = (ObjectReferenceDescriptor) iter.next();
            field = rds.getPersistentField();
            newRelObj = field.get(newObj);
            oldRelObj = field.get(oldObj);
            if ((newRelObj == null) && (oldRelObj != null))
            {
                field.set(oldObj, null);
            }
            else if (newRelObj != null)
            {
                newRelOid = new Identity(newRelObj, pb);
                if ((oldRelObj == null) ||
                        !newRelOid.equals(new Identity(oldRelObj, pb)))
                {
                    // seek for existing old object with the new identity
                    oldRelObj = cache.lookup(newRelOid);
                    if (oldRelObj == null)
                    {
                        throw new IllegalStateException("Related object not found in the context: " + newRelOid);
                    }
                    field.set(oldObj, oldRelObj);
                }
            }
        }

        // 1:N relations
        Iterator collections = mif.getCollectionDescriptors().iterator();
        CollectionDescriptor collectionDescriptor;

        while (collections.hasNext())
        {
            collectionDescriptor = (CollectionDescriptor) collections.next();
            field = collectionDescriptor.getPersistentField();
            if (Collection.class.isAssignableFrom(field.getType()))
            {
                Collection newCol;
                Collection oldCol;

                newCol = (Collection) field.get(newObj);
                if (newCol == null)
                {
                    field.set(oldObj, null);
                    continue;
                }

                oldCol = (Collection) field.get(oldObj);
                if (newCol instanceof CollectionProxyDefaultImpl)
                {
                    CollectionProxyDefaultImpl cp = (CollectionProxyDefaultImpl) newCol;
                    if (newCol instanceof List)
                    {
                        oldCol = new ListProxyDefaultImpl(pb.getPBKey(), cp.getCollectionClass(), cp.getQuery());
                    }
                    else if (newCol instanceof Set)
                    {
                        oldCol = new SetProxyDefaultImpl(pb.getPBKey(), cp.getCollectionClass(), cp.getQuery());
                    }
                    else
                    {
                        oldCol = new CollectionProxyDefaultImpl(pb.getPBKey(), cp.getCollectionClass(), cp.getQuery());
                    }
                    if (!((CollectionProxyDefaultImpl) newCol).isLoaded())
                    {
                        field.set(oldObj, oldCol);
                        continue;
                    }
                    oldCol.clear();
                }
                else
                {
                    try
                    {
                        oldCol = (Collection) newCol.getClass().newInstance();
                    }
                    catch (Exception ex)
                    {
                        System.err.println("Cannot instantiate collection field which is neither Collection nor array: " + field);
                        ex.printStackTrace();
                        return newObj;
                    }
                }
                field.set(oldObj, oldCol);
                for (Iterator it = newCol.iterator(); it.hasNext(); )
                {
                    newRelObj = it.next();
                    newRelOid = new Identity(newRelObj, pb);
                    oldRelObj = cache.lookup(newRelOid);
                    if (oldRelObj == null)
                    {
                        oldRelObj = newRelObj;
                    }
                    oldCol.add(oldRelObj);
                }
            }
            else if (field.getType().isArray())
            {
                Object newArray = field.get(newObj);
                int length = Array.getLength(newArray);
                Object oldArray =
                        Array.newInstance(field.getType().getComponentType(), length);

                for (int i = 0; i < length; i++)
                {
                    newRelObj = Array.get(newArray, i);
                    newRelOid = new Identity(newRelObj, pb);
                    oldRelObj = cache.lookup(newRelOid);
                    if (oldRelObj == null)
                    {
                        throw new IllegalStateException("Related object not found for swizzle: " + newRelOid);
                    }
                    Array.set(oldArray, i, oldRelObj);
                }
                field.set(oldObj, oldArray);
            }
            else
            {
                throw new IllegalStateException("Cannot swizzle collection field: " + field);
            }
        }

        return oldObj;
    }

    /**
     * @see org.apache.ojb.otm.swizzle.Swizzling#isSameInstance(Object, Object)
     */
    public boolean isSameInstance(Object swizzledObject, Object object)
    {
        return (swizzledObject == object);
    }

    /**
     * @see org.apache.ojb.otm.swizzle.Swizzling#getRealTarget(Object)
     */
    public Object getRealTarget(Object swizzledObject)
    {
        return swizzledObject;
    }

}
