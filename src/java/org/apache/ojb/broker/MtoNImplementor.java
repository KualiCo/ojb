package org.apache.ojb.broker;

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

import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.CollectionDescriptor;

/**
 * Helper class to handle single m:n relation entries (m:n indirection table entries).
 * <br/>
 * The "left / right" notation is only used to differ both sides of the relation.
 *
 * @author Leandro Rodrigo Saad Cruz
 * @version $Id: MtoNImplementor.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class MtoNImplementor
{
    private Object leftObject;
    private Object rightObject;
    private Class leftClass;
    private Class rightClass;
    private CollectionDescriptor leftDescriptor;

    /**
     * Creates a new instance.
     * 
     * @param pb             The currently used {@link PersistenceBroker} instance
     * @param leftDescriptor The collection descriptor for the left side
     * @param left           The left side object
     * @param right          The right side object
     * @deprecated
     */
    public MtoNImplementor(PersistenceBroker pb, CollectionDescriptor leftDescriptor, Object left, Object right)
    {
        init(leftDescriptor, left, right);
    }

    /**
     * Creates a new instance.
     *
     * @param pb            The currently used {@link PersistenceBroker} instance
     * @param leftFieldName Field name of the left m:n reference
     * @param left          The left side object
     * @param right         The right side object
     */
    public MtoNImplementor(PersistenceBroker pb, String leftFieldName, Object left, Object right)
    {
        if(left == null || right == null)
        {
            throw new IllegalArgumentException("both objects must exist");
        }
        CollectionDescriptor cod = pb.getClassDescriptor(ProxyHelper.getRealClass(left)).getCollectionDescriptorByName(leftFieldName);
        init(cod, left, right);
    }

    /**
     * Creates a new instance.
     * 
     * @param leftDescriptor The collection descriptor for the left side
     * @param left           The left side object
     * @param right          The right side object
     * @deprecated
     */
    public MtoNImplementor(CollectionDescriptor leftDescriptor, Object left, Object right)
    {
        init(leftDescriptor, left, right);
    }

    private void init(CollectionDescriptor leftDescriptor, Object left, Object right)
    {
        if(left == null || right == null)
        {
            throw new IllegalArgumentException("both objects must exist");
        }
        this.leftDescriptor = leftDescriptor;
        leftObject = left;
        rightObject = right;
        leftClass = ProxyHelper.getRealClass(leftObject);
        rightClass = ProxyHelper.getRealClass(rightObject);
    }

    /**
     * Returns the collection descriptor for the left side of the m:n collection.
     * 
     * @return The collection descriptor
     */
    public CollectionDescriptor getLeftDescriptor()
    {
        return leftDescriptor;
    }

    /**
     * Returns the class of the left side of the m:n collection.
     * 
     * @return The class of the left side
     */
    public Class getLeftClass()
    {
        return leftClass;
    }

    /**
     * Returns the class of the right side of the m:n collection.
     * 
     * @return The class of the right side
     */
    public Class getRightClass()
    {
        return rightClass;
    }

    /**
     * Returns the object for the left side of the m:n collection.
     * 
     * @return The object for the left side
     */
    public Object getLeftObject()
    {
        return leftObject;
    }

    /**
     * Returns the object for the right side of the m:n collection.
     * 
     * @return The object for the right side
     */
    public Object getRightObject()
    {
        return rightObject;
    }
}
