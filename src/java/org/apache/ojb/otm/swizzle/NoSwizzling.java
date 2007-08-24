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

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.cache.ObjectCache;

public class NoSwizzling implements Swizzling
{

    /**
     * @see org.apache.ojb.otm.swizzle.Swizzling#swizzle(Object, Object, PersistenceBroker)
     */
    public Object swizzle(Object newObj, Object oldObj,
                          PersistenceBroker pb, ObjectCache cache)
    {
        return newObj;
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
