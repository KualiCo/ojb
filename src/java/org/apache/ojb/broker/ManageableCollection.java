package org.apache.ojb.broker;

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

import java.util.Iterator;

/**
 * OJB can handle java.util.Collection as well as user defined collection classes as collection attributes
 * in persistent classes. In order to collaborate with the OJB mechanisms these collection must provide a minimum
 * protocol as defined by this interface ManageableCollection.
 * The methods have a prefix "ojb" that indicates that these methods are "technical" methods, required
 * by OJB and not to be used in business code.
 *
 * @author Thomas Mahler
 * @version $Id: ManageableCollection.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public interface ManageableCollection extends java.io.Serializable
{
    /**
     * Adds a single object to the Collection. This method is used during reading collection elements
     * from the database. Thus it is safe to cast the object to the underlying element type of the
     * collection.
     * 
     * @param anObject The object to add
     */
    void ojbAdd(Object anObject);

    /**
     * Adds another collection to this collection. Used in reading extents from the database.
     * Thus it is safe to cast the given collection to this class.
     * 
     * @param otherCollection The added collection
     */
    void ojbAddAll(ManageableCollection otherCollection);

    /**
     * Returns an iterator over all elements in the collection. Used during store and delete
     * operations. If the implementor does not return an iterator over ALL elements, OJB cannot
     * store and delete all elements properly.
     *
     * @return The iterator
     */
    Iterator ojbIterator();

    /**
     * A callback method to implement 'removal-aware' (track removed objects and delete
     * them by its own) collection implementations.
     * 
     * @param broker The persistence broker
     */
    public void afterStore(PersistenceBroker broker) throws PersistenceBrokerException;
}
