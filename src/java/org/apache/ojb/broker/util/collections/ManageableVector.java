package org.apache.ojb.broker.util.collections;

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

import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;

import java.util.Iterator;
import java.util.Vector;

/**
 * is a utility class. provides a Vector that addionally implements
 * the ManageableCollection interface. This class may be used
 * as a type for collection attributes.
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: ManageableVector.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class ManageableVector extends Vector implements ManageableCollection
{
    /**
     * add a single Object to the Collection. This method is used during reading Collection elements
     * from the database. Thus it is is save to cast anObject to the underlying element type of the
     * collection.
     */
    public void ojbAdd(Object anObject)
    {
        this.add(anObject);
    }

    /**
     * adds a Collection to this collection. Used in reading Extents from the Database.
     * Thus it is save to cast otherCollection to this.getClass().
     */
    public void ojbAddAll(ManageableCollection otherCollection)
    {
        this.addAll((ManageableVector) otherCollection);
    }

    public void afterStore(PersistenceBroker broker) throws PersistenceBrokerException
    {
        //do nothing
    }

    /**
     * returns an Iterator over all elements in the collection. Used during store and delete Operations.
     * If the implementor does not return an iterator over ALL elements, OJB cannot store and delete all elements properly.
     *
     */
    public Iterator ojbIterator()
    {
        return this.iterator();
    }
}
