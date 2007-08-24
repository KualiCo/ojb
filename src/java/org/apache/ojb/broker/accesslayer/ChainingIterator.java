package org.apache.ojb.broker.accesslayer;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.PersistenceBrokerException;

/**
 * @author matthew.baird (mattbaird@yahoo.com)
 *
 * The ChainingIterator is an extent aware Iterator.
 *
 * How the ChainingIterator works:
 * The ChainedIterator holds a collection of RsIterators for each queried 
 * Interface-based extent.
 *
 * The RsIterator is able to load objects that are non-interface extents, 
 * mapped to the same table.
 *
 * The ChainingIterator cannot return sorted results as the iterator is a 
 * collection of query results across different tables.
 *
 * @version $Id: ChainingIterator.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class ChainingIterator implements OJBIterator
{
    private List m_rsIterators = new ArrayList();
    private OJBIterator m_activeIterator = null;

    /**
     * The following are used to maintain an index of where
     * the cursor is in the array of rsiterators. We do this
     * because we can't find the position through the interface,
     * and we need the position in order to support the relative(x)
     * calls
     */
    private int m_activeIteratorIndex = 0;
    private int m_fullSize = -1;
    private int m_currentCursorPosition = 0;
    /** if true do not fire PBLifeCycleEvent. */
    private boolean disableLifeCycleEvents = false;

    /**
     * Constructor for ChainingIterator.
     */
    public ChainingIterator()
    {
        super();
    }

    /**
     * Constructor for ChainingIterator.
     */
    public ChainingIterator(List iterators)
    {
        Iterator checkIterator = iterators.iterator();
        OJBIterator temp;

        /**
         * validate that all items in List are iterators and
         * they are not empty.
         */
        while (checkIterator.hasNext())
        {
            temp = (OJBIterator) checkIterator.next();
            addIterator(temp);
        }
    }

    /**
     * use this method to construct the ChainingIterator
     * iterator by iterator.
     */
    public void addIterator(OJBIterator iterator)
    {
        /**
         * only add iterators that are not null and non-empty.
         */
        if (iterator != null)
        {
            if (iterator.hasNext())
            {
                setNextIterator();
                m_rsIterators.add(iterator);
            }
        }
    }

    /**
     * Calculates the size of all the iterators. Caches it for fast
     * lookups in the future. iterators shouldn't change size after the
     * queries have been executed so caching is safe (assumption, should check).
     * @return the combined size of all the iterators for all extents.
     */
    public int size() throws PersistenceBrokerException
    {
        if (m_fullSize == -1)
        {
            int size = 0;
            Iterator it = m_rsIterators.iterator();
            while (it.hasNext())
            {
                size += ((OJBIterator) it.next()).size();
            }
            m_fullSize = size;
        }
        return m_fullSize;
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.accesslayer.OJBIterator#fullSize()
     */
    public int fullSize() throws PersistenceBrokerException
    {
        return size();
    }

    /**
     * the absolute and relative calls are the trickiest parts. We have to
     * move across cursor boundaries potentially.
     * 
     * a + row value indexes from beginning of resultset
     * a - row value indexes from the end of th resulset.
     * 
     * Calling absolute(1) is the same as calling first(). 
     * Calling absolute(-1) is the same as calling last().
     */
    public boolean absolute(int row) throws PersistenceBrokerException
    {
        // 1. handle the special cases first.
        if (row == 0)
        {
            return true;
        }

        if (row == 1)
        {
            m_activeIteratorIndex = 0;
            m_activeIterator = (OJBIterator) m_rsIterators.get(m_activeIteratorIndex);
            m_activeIterator.absolute(1);
            return true;
        }
        if (row == -1)
        {
            m_activeIteratorIndex = m_rsIterators.size();
            m_activeIterator = (OJBIterator) m_rsIterators.get(m_activeIteratorIndex);
            m_activeIterator.absolute(-1);
            return true;
        }

        // now do the real work.
        boolean movedToAbsolute = false;
        boolean retval = false;
        setNextIterator();

        // row is positive, so index from beginning.
        if (row > 0)
        {
            int sizeCount = 0;
            Iterator it = m_rsIterators.iterator();
            OJBIterator temp = null;
            while (it.hasNext() && !movedToAbsolute)
            {
                temp = (OJBIterator) it.next();
                if (temp.size() < row)
                {
                    sizeCount += temp.size();
                }
                else
                {
                    // move to the offset - sizecount
                    m_currentCursorPosition = row - sizeCount;
                    retval = temp.absolute(m_currentCursorPosition);
                    movedToAbsolute = true;
                }
            }

        }

        // row is negative, so index from end
        else if (row < 0)
        {
            int sizeCount = 0;
            OJBIterator temp = null;
            for (int i = m_rsIterators.size(); ((i >= 0) && !movedToAbsolute); i--)
            {
                temp = (OJBIterator) m_rsIterators.get(i);
                if (temp.size() < row)
                {
                    sizeCount += temp.size();
                }
                else
                {
                    // move to the offset - sizecount
                    m_currentCursorPosition = row + sizeCount;
                    retval = temp.absolute(m_currentCursorPosition);
                    movedToAbsolute = true;
                }
            }
        }

        return retval;
    }

    /**
     * Moves the cursor a relative number of rows.
     * Movement can go in forward (positive) or reverse (negative).
     * 
     * Calling relative does not "wrap" meaning if you move before first or 
     * after last you get positioned at the first or last row.
     * 
     * Calling relative(0) does not change the cursor position.
     * 
     * Note: Calling the method relative(1) is different from calling 
     * the method next() because is makes sense to call next() when 
     * there is no current row, for example, when the cursor is 
     * positioned before the first row or after the last row of 
     * the result set.
     */
    public boolean relative(int row) throws PersistenceBrokerException
    {
        if (row == 0)
        {
            return true;
        }

        boolean movedToRelative = false;
        boolean retval = false;
        setNextIterator();

        if (row > 0)
        {
            // special case checking for the iterator we're currently in
            // (since it isn't positioned on the boundary potentially)
            if (row > (m_activeIterator.size() - m_currentCursorPosition))
            {
                // the relative position lies over the border of the
                // current iterator.

                // starting position counter should be set to whatever we have left in
                // active iterator.
                int positionCounter = m_activeIterator.size() - m_currentCursorPosition;
                for (int i = m_activeIteratorIndex + 1; ((i < m_rsIterators.size()) && !movedToRelative); i++)
                {
                    m_activeIteratorIndex = i;
                    m_currentCursorPosition = 0;
                    m_activeIterator = (OJBIterator) m_rsIterators.get(m_activeIteratorIndex);
                    if (!((row - positionCounter) > m_activeIterator.size()))
                    {
                        // the relative position requested is within this iterator.
                        m_currentCursorPosition = row - positionCounter;
                        retval = m_activeIterator.relative(m_currentCursorPosition);
                        movedToRelative = true;
                    }
                }
            }
            else
            {
                // the relative position lays within the current iterator.
                retval = m_activeIterator.relative(row);
                movedToRelative = true;
            }
        }

        return retval;
    }

    /**
     * delegate to each contained OJBIterator and release
     * its resources.
     */
    public void releaseDbResources()
    {
        Iterator it = m_rsIterators.iterator();
        while (it.hasNext())
        {
            ((OJBIterator) it.next()).releaseDbResources();
        }
    }

    /**
     * check the list of iterators to see if we have a next element.
     * @return true if one of the contained iterators past the current
     * position has a next.
     */
    public boolean hasNext()
    {
        setNextIterator();
        if (m_activeIterator == null)
        {
            return false;
        }
        else
        {
            return m_activeIterator.hasNext();
        }
    }

    /**
     * first checks to make sure we aren't at the end of the list of
     * iterators, positions the cursor appropriately, then retrieves
     * next object in active iterator.
     * @return the next object in the iterator.
     */
    public Object next()
    {
        setNextIterator();
        m_currentCursorPosition++;
        return m_activeIterator.next();
    }

    public void remove()
    {
        setNextIterator();
        m_activeIterator.remove();
    }

    /**
     * Convenience routine to move to the next iterator if needed.
     * @return true if the iterator is changed, false if no changes.
     */
    private boolean setNextIterator()
    {
        boolean retval = false;
        // first, check if the activeIterator is null, and set it.
        if (m_activeIterator == null)
        {
            if (m_rsIterators.size() > 0)
            {
                m_activeIteratorIndex = 0;
                m_currentCursorPosition = 0;
                m_activeIterator = (OJBIterator) m_rsIterators.get(m_activeIteratorIndex);
            }
        }
        else if (!m_activeIterator.hasNext())
        {
            if (m_rsIterators.size() > (m_activeIteratorIndex + 1))
            {
                // we still have iterators in the collection, move to the
                // next one, increment the counter, and set the active
                // iterator.
                m_activeIteratorIndex++;
                m_currentCursorPosition = 0;
                m_activeIterator = (OJBIterator) m_rsIterators.get(m_activeIteratorIndex);
                retval = true;
            }
        }

        return retval;
    }

    /**
     * Answer true if an Iterator for a Table is already available
     * @param aTable
     * @return
     */
    public boolean containsIteratorForTable(String aTable)
    {
        boolean result = false;

        if (m_rsIterators != null)
        {
            for (int i = 0; i < m_rsIterators.size(); i++)
            {
                OJBIterator it = (OJBIterator) m_rsIterators.get(i);
                if (it instanceof RsIterator)
                {
                    if (((RsIterator) it).getClassDescriptor().getFullTableName().equals(aTable))
                    {
                        result = true;
                        break;
                    }
                }
                else if (it instanceof ChainingIterator)
                {
                    result = ((ChainingIterator) it).containsIteratorForTable(aTable);
                }
            }
        }

        return result;
    }

    /**
     * @see org.apache.ojb.broker.accesslayer.OJBIterator#disableLifeCycleEvents()
     */
    public void disableLifeCycleEvents()
    {
        Iterator iterators = m_rsIterators.iterator();
        while (iterators.hasNext())
        {
            OJBIterator iter = (OJBIterator) iterators.next();
            iter.disableLifeCycleEvents();
        }        
    }

}
