package org.apache.ojb.broker.accesslayer;

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

import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.query.Query;

/**
 * PagingIterator is wrapper around an OJBIterator to support startAt endAt
 * positions. The PagingIterator returns rows <b>including</b> startAt 
 * and <b>including</b> endAt.
 * 
 * startAt = 1, endAt = 11 returns rows 1 to 11 if available
 * if endAt == Query.NO_END_AT_INDEX endAt is set to the last available row
 * 
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: PagingIterator.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class PagingIterator implements OJBIterator
{
    private final OJBIterator m_iterator;
    private int m_startAt;
    private int m_endAt;
    private int m_rowLimit;
    private int m_fullSize;
    private int m_currentCursorPosition; // position of the wrapped iterator

    /**
     * Constructor 
     * @param anIterator wrapped Iterator
     * @param startAt (first row is 1)
     * @param endAt (Query.NO_END_AT_INDEX stands for all available rows) 
     */
    public PagingIterator(OJBIterator anIterator, int startAt, int endAt)
    {
        super();

        if (endAt != Query.NO_START_AT_INDEX && startAt > endAt)
        {
            throw new PersistenceBrokerException("startAt must be less than endAt.");
        }

        m_iterator = anIterator;
        m_fullSize = m_iterator.size();

        if (startAt == Query.NO_START_AT_INDEX)
        {
            m_startAt = 1;
        }
        else
        {
            m_startAt = startAt;
        }

        if (endAt == Query.NO_END_AT_INDEX)
        {
            m_endAt = m_fullSize;
        }
        else
        {
            m_endAt = Math.min(endAt, m_fullSize);
        }
        
        m_rowLimit = Math.max(0, m_endAt - m_startAt + 1);
        m_currentCursorPosition = m_startAt - 1;

        m_iterator.absolute(m_currentCursorPosition);
    }

    /**
	 * @see org.apache.ojb.broker.accesslayer.OJBIterator#size()
	 */
    public int size() throws PersistenceBrokerException
    {
        if (m_fullSize < m_rowLimit)
        {
            return m_fullSize;
        }
        else
        {
            return m_rowLimit;
        }
    }

    /**
     * @see org.apache.ojb.broker.accesslayer.OJBIterator#fullSize()
     */
    public int fullSize() throws PersistenceBrokerException
    {
        return m_fullSize;
    }
    
    /**
	 * @see org.apache.ojb.broker.accesslayer.OJBIterator#absolute(int)
	 */
    public boolean absolute(int row) throws PersistenceBrokerException
    {
        int newPosition = (m_startAt - 1) + row;
        
        if (newPosition < m_startAt)
        {
            newPosition = Math.max(m_endAt + row, m_startAt - 1);
        }
        
        if (newPosition > m_endAt)
        {
            newPosition = m_endAt;
        }
        
        m_currentCursorPosition = newPosition;
        return m_iterator.absolute(newPosition);
    }

    /**
	 * @see org.apache.ojb.broker.accesslayer.OJBIterator#relative(int)
	 */
    public boolean relative(int row) throws PersistenceBrokerException
    {
        return absolute(m_currentCursorPosition - (m_startAt - 1) + row);
    }

    /**
	 * @see org.apache.ojb.broker.accesslayer.OJBIterator#releaseDbResources()
	 */
    public void releaseDbResources()
    {
        m_iterator.releaseDbResources();
    }

    /**
	 * remove is not supported
	 */
    public void remove()
    {
        throw new UnsupportedOperationException("remove not supported by PagingIterator");
    }

    /**
	 * @see java.util.Iterator#hasNext()
	 */
    public boolean hasNext()
    {
        if (m_currentCursorPosition < m_endAt)
        {
            return true;
        }
        else
        {
            releaseDbResources();
            return false;
        }

    }

    /**
	 * @see java.util.Iterator#next()
	 */
    public Object next()
    {
        m_currentCursorPosition++;
        return m_iterator.next();
    }

    /**
     * @see org.apache.ojb.broker.accesslayer.OJBIterator#disableLifeCycleEvents()
     */
    public void disableLifeCycleEvents()
    {
        m_iterator.disableLifeCycleEvents();       
    }
}
