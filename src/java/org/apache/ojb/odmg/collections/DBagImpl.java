package org.apache.ojb.odmg.collections;

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

import org.apache.ojb.broker.PBKey;
import org.odmg.DBag;

import java.util.Iterator;

/**
 * The {@link org.odmg.DBag} implementation class.
 */
public class DBagImpl extends DListImpl implements org.odmg.DBag
{
    private static final long serialVersionUID = -4937635522392824190L;
    
    public DBagImpl()
    {
        super();
    }

    /**
     * DBagImpl constructor comment.
     */
    public DBagImpl(PBKey key)
    {
        super(key);
    }

    /**
     * A new <code>DBag</code> instance is created that contains the difference of
     * this object and the <code>DBag</code> instance referenced by <code>otherBag</code>.
     * This method is similar to the <code>removeAll</code> method in <code>Collection</code>,
     * except that this method creates a new collection and <code>removeAll</code>
     * modifies the object to contain the result.
     * @param	otherBag The other bag to use in creating the difference.
     * @return A <code>DBag</code> instance that contains the elements of this object
     * minus the elements in <code>otherBag</code>.
     */
    public DBag difference(DBag otherBag)
    {
        DBagImpl result = new DBagImpl(getPBKey());
        Iterator iter = this.iterator();
        while (iter.hasNext())
        {
            Object candidate = iter.next();
            if (!otherBag.contains(candidate))
            {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * A new <code>DBag</code> instance is created that contains the intersection of
     * this object and the <code>DBag</code> referenced by <code>otherBag</code>.
     * This method is similar to the <code>retainAll</code> method in <code>Collection</code>,
     * except that this method creates a new collection and <code>retainAll</code>
     * modifies the object to contain the result.
     * @param	otherBag The other bag to use in creating the intersection.
     * @return A <code>DBag</code> instance that contains the intersection of this
     * object and <code>otherBag</code>.
     */
    public DBag intersection(DBag otherBag)
    {
        DBagImpl result = new DBagImpl(getPBKey());
        Iterator iter = otherBag.iterator();
        while (iter.hasNext())
        {
            Object candidate = iter.next();
            if (this.contains(candidate))
            {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * This method returns the number of occurrences of the object <code>obj</code>
     * in the <code>DBag</code> collection.
     * @param obj The value that may have elements in the collection.
     * @return The number of occurrences of <code>obj</code> in this collection.
     */
    public int occurrences(Object obj)
    {
        int count = 0;
        for (int i = 0; i < this.size(); i++)
        {
            if ((obj == null) ? this.get(i) == null : this.get(i).equals(obj))
            {
                count++;
            }
        }
        return count;
    }

    /**
     * A new <code>DBag</code> instance is created that is the union of this object
     * and <code>otherBag</code>.
     * This method is similar to the <code>addAll</code> method in <code>Collection</code>,
     * except that this method creates a new collection and <code>addAll</code>
     * modifies the object to contain the result.
     * @param	otherBag	The other bag to use in the union operation.
     * @return A <code>DBag</code> instance that contains the union of this object
     * and <code>otherBag</code>.
     */
    public DBag union(DBag otherBag)
    {
        return (DBagImpl) concat((DBagImpl) otherBag);
    }
}
