package org.apache.ojb.otm.copy;

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

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.metadata.*;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.util.ConstructorHelper;
import org.apache.ojb.broker.util.IdentityMapFactory;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;

/**
 * recursively copies an object based on the ClassDescriptor
 * User: matthew.baird
 * Date: Jul 7, 2003
 * Time: 1:41:58 PM
 */
public final class MetadataObjectCopyStrategy implements ObjectCopyStrategy
{
    private static final ReflectiveObjectCopyStrategy _reflective = new ReflectiveObjectCopyStrategy();
    private static final SerializeObjectCopyStrategy _serialize = new SerializeObjectCopyStrategy();

    /**
     * Uses an IdentityMap to make sure we don't recurse infinitely on the same object in a cyclic object model.
     * Proxies
     * @param obj
     * @return
     */
    public Object copy(final Object obj, final PersistenceBroker broker)
    {
        return clone(obj, IdentityMapFactory.getIdentityMap(), broker);
    }

    private static Object clone(final Object toCopy, final Map objMap, final PersistenceBroker broker)
    {
        /**
         * first, check to make sure we aren't recursing to some object that we've already copied.
         * if the toCopy is in the objMap, just return it.
         */
        if (objMap.containsKey(toCopy)) return objMap.get(toCopy);
        /**
         * if null, return null, duh
         */
        if (toCopy == null)
            return null;

        /**
         * if this is a proxy, just copy the proxy, don't materialize it, and stop recursing
         */
        if (ProxyHelper.isVirtualOjbProxy(toCopy))
        {
            return _reflective.copy(toCopy, null);
        }
        else if (ProxyHelper.isNormalOjbProxy(toCopy))
        {
            return _serialize.copy(toCopy, null);
        }

        /**
         * if no classdescriptor exists for this object, just return this object, we
         * can't copy it.
         */
        final ClassDescriptor cld = broker.getClassDescriptor(toCopy.getClass());
        if (cld == null)
        {
            return _reflective.copy(toCopy, null);
        }

        final Object retval;
        try
        {
            final Constructor con = cld.getZeroArgumentConstructor();
            retval = ConstructorHelper.instantiate(con);
            objMap.put(toCopy,retval);
        }
        catch (InstantiationException e)
        {
            throw new ObjectCopyException("InstantiationException", e);
        }

        /**
         * first copy all the fields
         * fields are not mapped objects (ie ObjectReferenceDescriptors)
         */
        final FieldDescriptor[] fieldDescs = cld.getFieldDescriptions();
//        final BrokerHelper brokerHelper = broker.serviceBrokerHelper();
        for (int i = 0; i < fieldDescs.length; i++)
        {
            final FieldDescriptor fd = fieldDescs[i];
            final PersistentField f = fd.getPersistentField();
            Object fieldValue = f.get(toCopy);
/*
arminw:
TODO: ensure that the autoincrement values be assigned before the copy was done
If possible we should avoid to declare BrokerHelper#getAutoIncrementValue public and
if we copy an object user don't expect the change of fields.
*/
//            // If the field is auto increment, assign its value before copying!
//            if (fd.isAutoIncrement())
//            {
//                fieldValue = brokerHelper.getAutoIncrementValue(fd, toCopy, fieldValue);
//            }

            f.set(retval, fieldValue);
        }

        /**
         * then copy all the 1:1 references
         */
        final Collection refDescsCol = cld.getObjectReferenceDescriptors();
        final ObjectReferenceDescriptor[] rds = (ObjectReferenceDescriptor[]) refDescsCol.toArray(new ObjectReferenceDescriptor[refDescsCol.size()]);
        for (int i = 0; i < rds.length; i++)
        {
            final ObjectReferenceDescriptor rd = rds[i];
            final PersistentField f = rd.getPersistentField();
            /**
             * recursively copy the referenced objects
             * register in the objMap first
             */
            final Object object = f.get(toCopy);
            final Object clone = clone(object, objMap, broker);
            objMap.put(object, clone);
            f.set(retval, clone);
        }
        /**
         * then copy all the 1:M and M:N references
         */
        final Collection colDescsCol = cld.getCollectionDescriptors();
        final Iterator it = colDescsCol.iterator();
        while (it.hasNext())
        {
            final CollectionDescriptor cd = (CollectionDescriptor) it.next();
            final PersistentField f = cd.getPersistentField();
            final Object collection = f.get(toCopy);
            /**
             * handle collection proxies where the entire Collection is a big proxy
             * (vs all the elements in the collection are proxies
             */
            if (collection == null)
            {
                f.set(retval, null);
            }
            else if (collection instanceof CollectionProxyDefaultImpl)
            {
                f.set(retval, _reflective.copy(collection, null));
            }
            else if (collection instanceof Collection)
            {
                try
                {
                    final Collection newCollection = (Collection) collection.getClass().newInstance();
                    final Iterator tempIter = ((Collection) collection).iterator();
                    Object obj;
                    while (tempIter.hasNext())
                    {
                        obj = tempIter.next();
                        /**
                        * if this is a proxy, just copy the proxy, don't materialize it, and stop recursing
                        */
                        if (ProxyHelper.isNormalOjbProxy(obj))  // tomdz: what about VirtualProxy ?
                        {
                            newCollection.add(obj);
                        }
                        else
                        {
                            final Object clone = clone(obj, objMap, broker);
                            objMap.put(obj, clone);
                            newCollection.add(clone);
                        }
                    }
                    f.set(retval, newCollection);
                }
                catch (InstantiationException e)
                {
                    throw new ObjectCopyException("InstantiationException", e);
                }
                catch (IllegalAccessException e)
                {
                    throw new ObjectCopyException("IllegalAccessException", e);
                }
            }
            else
            {
                throw new java.lang.UnsupportedOperationException("MetadataObjectCopyStrategy cannot handle Collection of type: " + collection.getClass().getName());
            }
        }
        return retval;
    }
}
