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

import java.util.Iterator;

import org.apache.ojb.odmg.TransactionImpl;

/**
 * Iterator implementation for {@link org.odmg.DSet} implementation.
 *
 * @version $Id: DSetIterator.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
class DSetIterator implements Iterator
{
    private Iterator iter;
    private DSetImpl dSet;
    private DSetEntry currentEntry = null;

    /**
     * DListIterator constructor comment.
     */
    public DSetIterator(DSetImpl set)
    {
        this.dSet = set;
        this.iter = set.getElements().iterator();
    }

    /**
     * @see java.util.Iterator#hasNext() 
     */
    public boolean hasNext()
    {
        return iter.hasNext();
    }

    /**
     * @see java.util.Iterator#next()
     */
    public Object next()
    {
        currentEntry = ((DSetEntry) iter.next());
        return currentEntry.getRealSubject();
    }

    /**
     * @see java.util.Iterator#remove()
     */
    public void remove()
    {
        iter.remove();
        TransactionImpl tx = dSet.getTransaction();
        if (tx != null)
        {
            tx.markDelete(currentEntry);
        }
        currentEntry = null;
    }
}
