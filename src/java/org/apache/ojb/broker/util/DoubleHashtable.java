package org.apache.ojb.broker.util;

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

import java.util.Hashtable;

/**
 * this class can be used to build two-way lookup tables.
 * It provides lookup from keys to values and the inverse
 * lookup from values to keys.
 *
 * @author Thomas Mahler
 * @version $Id: DoubleHashtable.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class DoubleHashtable
{
    /**
     * the table for key lookups.
     */
    private Hashtable keyTable;

    /**
     * the table for value lookups.
     */
    private Hashtable valueTable;

    /**
     * public default constructor.
     */
    public DoubleHashtable()
    {
        keyTable = new Hashtable();
        valueTable = new Hashtable();
    }

    /**
     * put a (key, value) pair into the table.
     * @param key the key object.
     * @param value the value object.
     */
    public void put(Object key, Object value)
    {
        keyTable.put(key, value);
        valueTable.put(value, key);
    }

    /**
     * lookup a value from the table by its key.
     * @param key the key object
     * @return the associated value object
     */
    public Object getValueByKey(Object key)
    {
        return keyTable.get(key);
    }

    /**
     * lookup a key from the table by its value.
     * @param value the value object
     * @return the associated key object
     */
    public Object getKeyByValue(Object value)
    {
        return valueTable.get(value);
    }

    /**
     * remove a (key, value)-entry by its key
     * @param key the key object
     */
    public void removeByKey(Object key)
    {
        Object value = keyTable.remove(key);
        if (value != null)
        {
            valueTable.remove(value);
        }
    }

    /**
     * remove a (key, value)-entry by its value
     * @param value the value object
     */
    public void removeByValue(Object value)
    {
        Object key = valueTable.remove(value);
        if (key != null)
        {
            keyTable.remove(key);
        }
    }

}
