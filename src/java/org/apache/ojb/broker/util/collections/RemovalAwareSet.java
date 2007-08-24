package org.apache.ojb.broker.util.collections;

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

import java.util.Iterator;
import java.util.Vector;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;

/**
 * This is a set implementation that tracks removal and addition of elements.
 * This tracking allow the PersistenceBroker to delete elements from
 * the database that have been removed from the collection before a
 * PB.store() orperation occurs.
 * This will allow to use the PB api in way pretty close to ODMG persistent
 * collections!
 * 
 * @author Thomas Dudziak
 * @version $Id: RemovalAwareSet.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class RemovalAwareSet extends ManageableHashSet implements IRemovalAwareCollection
{
    private Vector allObjectsToBeRemoved = new Vector();

    /**
     * @see org.apache.ojb.broker.ManageableCollection#afterStore(PersistenceBroker broker)
     */
    public void afterStore(PersistenceBroker broker) throws PersistenceBrokerException
    {
        // make sure allObjectsToBeRemoved does not contain
        // any instances that got re-added to the list
        allObjectsToBeRemoved.removeAll(this);

        Iterator iter = allObjectsToBeRemoved.iterator();
        while (iter.hasNext())
        {
            broker.delete(iter.next());
        }
        allObjectsToBeRemoved.clear();
    }

    protected void registerForDeletion(Object toBeRemoved)
    {
        //only add objects once to avoid double deletions
        if (!allObjectsToBeRemoved.contains(toBeRemoved))
        {
            this.allObjectsToBeRemoved.add(toBeRemoved);
        }
    }

    /**
     * @see java.util.Collection#remove(Object)
     */
    public boolean remove(Object o)
    {
        boolean result = super.remove(o);
        registerForDeletion(o);
        return result;
    }

    /**
     * @see java.util.Vector#removeAllElements()
     */
    public synchronized void removeAllElements()
    {
        for (Iterator it = iterator(); it.hasNext();)
        {
            registerForDeletion(it.next());
        }
        super.clear();
    }

    public synchronized void clear()
    {
      removeAllElements();
    }

    public void resetDeleted()
    {
      this.allObjectsToBeRemoved.clear();
    }
    
}
