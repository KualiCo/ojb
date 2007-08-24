package org.apache.ojb.broker.metadata.fieldaccess;

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

import java.util.Map;

import org.apache.commons.collections.map.ReferenceIdentityMap;
import org.apache.ojb.broker.metadata.MetadataException;

/**
 * This class handle an anonymous persistent fiels for 1-1 association,
 * and ojbConcreteClass
 * @author Houar TINE
 * @version $Id: AnonymousPersistentField.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class AnonymousPersistentField implements PersistentField
{
    private static final long serialVersionUID = 3989863424358352941L;

    private transient Map fkCache;
    private String fieldname;

    public AnonymousPersistentField(String fieldname)
    {
        this.fieldname = fieldname;
    }

    public synchronized void set(Object obj, Object value) throws MetadataException
    {
        putToFieldCache(obj, value);
    }

    public synchronized Object get(Object anObject) throws MetadataException
    {
        return getFromFieldCache(anObject);
    }

/*
    Use ReferenceIdentityMap (with weak key and hard value setting) instead of
    WeakHashMap to hold anonymous field values. Here is an snip of the mail from Andy Malakov:
    <snip>
        I found that usage of database identity in Java produces quite interesting problem in OJB:
        In my application all persistent Java objects use database identity instead of Java reference identity
        (i.e. Persistable.equals() is redefined so that two persistent objects are the same if they have the same
        primary key and top-level class).

        In OJB, for each field declared in repository there is dedicated instance of AnonymousPersistentField that stores
        object-to-field-value mapping in WeakHashMap (in fkCache attribute). Despite usage of cache
        (ObjectCachePerBrokerImpl in my case) it is possible that identical DB objects will end up as different
        Java objects during retrieval of complex objects.

        Now imagine what happens when two identical instances are retrieved:
        1)
        When first instance is retrieved it stores its foreign keys in AnonymousPersistentField.fkCache under instance's
        identity. (happens in RowReaderDefaultImpl.buildWithReflection())
        2)
        When second object is retrieved and stored in fkCache, first instance is probably still cached
        [WeakHashMap entries are cleaned up only during GC]. Since keys are identical WeakHashMap only updates entry
        value and DOES NOT update entry key.
        3)
        If Full GC happens after that moment it will dispose fcCache entry if the FIRST reference becomes
        soft-referenced only.
    </snip>
*/
    protected void putToFieldCache(Object key, Object value)
    {
        if (key != null)
        {
            if (fkCache == null)
            {
                fkCache = new ReferenceIdentityMap (ReferenceIdentityMap.WEAK, ReferenceIdentityMap.HARD, true);
            }
            if (value != null)
                 fkCache.put(key, value);
             else
                 fkCache.remove (key);
        }
    }

    protected Object getFromFieldCache(Object key)
    {
        return (key != null && fkCache != null) ? fkCache.get(key) : null;
    }

    /**
     * Always returns <tt>null</tt>.
     * @see PersistentField#getDeclaringClass()
     */
    public Class getDeclaringClass()
    {
        return null;
    }

    /**
     * @see PersistentField#getName()
     */
    public String getName()
    {
        return fieldname;
    }

    /**
     * Always returns <tt>null</tt>.
     * @see PersistentField#getType()
     */
    public Class getType()
    {
        return null;
    }

    /**
     * Returns <tt>false</tt>.
     * @see PersistentField#usesAccessorsAndMutators()
     */
    public boolean usesAccessorsAndMutators()
    {
        return false;
    }
}
