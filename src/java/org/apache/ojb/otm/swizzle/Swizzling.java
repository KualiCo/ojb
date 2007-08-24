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

/**
 * @todo document
 */
public interface Swizzling
{

    /**
     *
     * Swizzle object references.
     *
     * @param newObj    the object being inserted into the EditingContext,
     * is null if the object is being invalidated
     * @param oldObj    the object present in the EditingContext,
     * is null if no object is present
     * @param pb        the PersistenceBroker that is used to get
     * persistent class info
     * @param cache     the "cache" of old objects, only lookup() method
     * can be used by the Swizzling implementation to seek for old objects
     * that should be set as a new value of relations.
     *
     * @return          the Swizzled Object
     *
     */
    public Object swizzle(Object newObj, Object oldObj, PersistenceBroker pb,
                          ObjectCache cache);

    /**
     *
     * Test if the given swizzled object is the same as the given object. By same object we mean,
     * that the System.identityHashCode() of the given object is the same as that of the object
     * represented by the swizzled object.
     *
     * @param swizzledObject        The swizzled object
     * @param object                The other object to be compared to
     * @return                      true, if they are the same. false, otherwise.
     *
     */
    public boolean isSameInstance(Object swizzledObject, Object object);

    /**
     *
     * Get the real object associated with the given swizzled object.
     *
     * @param   swizzledObject      the swizzled object
     * @return                      the real object
     *
     */
    public Object getRealTarget(Object swizzledObject);
}
