package org.apache.ojb.odmg.collections;

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

import java.util.ListIterator;

import org.apache.ojb.odmg.TransactionImpl;
import org.apache.ojb.odmg.RuntimeObject;
import org.odmg.Transaction;

/**
 * Iterator implementation for {@link org.odmg.DList} implementation.
 * 
 * @version $Id: DListIterator.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
class DListIterator implements ListIterator
{
    protected ListIterator iter;
    private DListImpl dlist;
    private DListEntry currentEntry = null;

    /**
     * DListIterator constructor comment.
     */
    public DListIterator(DListImpl list)
    {
        this.dlist = list;
        this.iter = list.getElements().listIterator();
    }

    /**
     * DListIterator constructor comment.
     */
    public DListIterator(DListImpl list, int index)
    {
        this.dlist = list;
        this.iter = list.getElements().listIterator(index);
    }

    /**
     * @see ListIterator#add(Object)
     */
    public void add(Object obj)
    {
        DListEntry entry = new DListEntry(this.dlist, obj);
        entry.setPosition(this.nextIndex() - 1);
        iter.add(entry);

        TransactionImpl tx = dlist.getTransaction();
        if (tx != null)
        {
            RuntimeObject rt = new RuntimeObject(entry, tx, true);
            tx.lockAndRegister(rt, Transaction.WRITE, false, tx.getRegistrationList());
        }
    }

    /**
     * @see java.util.ListIterator#hasNext()
     */
    public boolean hasNext()
    {
        return iter.hasNext();
    }

    /**
     * @see java.util.ListIterator#hasPrevious()
     */
    public boolean hasPrevious()
    {
        return iter.hasPrevious();
    }

    /**
     * @see java.util.ListIterator#next()
     */
    public Object next()
    {
        currentEntry = ((DListEntry) iter.next());
        return currentEntry.getRealSubject();
    }

    /**
     * @see java.util.ListIterator#nextIndex()
     */
    public int nextIndex()
    {
        return iter.nextIndex();
    }

    /**
     * @see java.util.ListIterator#previous()
     */
    public Object previous()
    {
        currentEntry = ((DListEntry) iter.previous());
        return currentEntry.getRealSubject();
    }

    /**
     * @see java.util.ListIterator#previousIndex()
     */
    public int previousIndex()
    {
        return iter.previousIndex();
    }

    /**
     * @see java.util.ListIterator#remove()
     */
    public void remove()
    {
        iter.remove();
        TransactionImpl tx = dlist.getTransaction();
        if (tx != null)
        {
            tx.markDelete(currentEntry);
        }
        currentEntry = null;
    }

    /**
     * @see ListIterator#set(Object)
     */
    public void set(Object o)
    {
        throw new UnsupportedOperationException("Method is not supported");
        // currentEntry.setRealSubject(o);
    }

}
