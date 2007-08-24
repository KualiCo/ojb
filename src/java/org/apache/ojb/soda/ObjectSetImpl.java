package org.apache.ojb.soda;

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

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.accesslayer.RsIterator;
import org.apache.ojb.broker.query.Query;
import org.odbms.ObjectSet;

/**
 * @version $Id: ObjectSetImpl.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class ObjectSetImpl implements ObjectSet
{
    protected Iterator ojbIterator;
    protected int length;
    protected Vector elements;
    protected int position;
    protected int scrolled;
    protected boolean resultSetClosed;

    /**
     * Constructor for ObjectSetImpl. Builds up an ObjectSet from an OJB Query object
     */
    public ObjectSetImpl(PersistenceBroker broker, Query query, int limit)
    {
        super();
        position = 0;
        scrolled = 0;

        // avoid double query
//        length = broker.getCount(query);
//        if (limit >= 0)
//        {
//        	length = Math.min(length,limit);
//        }
//        elements = new Vector(length);

        // thma:
        // unfortunately Iterators are currently not extent-ware
        // we have to use getCollectionBy Query () thus!
        //ojbIterator = ojbBroker.getIteratorByQuery(query);
        Collection col = broker.getCollectionByQuery(query);
        ojbIterator = col.iterator();

        length = col.size();
        if (limit >= 0)
        {
        	length = Math.min(length,limit);
        }
        elements = new Vector(length);

        setResultSetClosed(false);
    }

    /*
     * @see ObjectSet#hasNext()
     */
    public synchronized boolean hasNext()
    {
        if (position < length)
        {
            if (position < scrolled)
            {
             	return true;
            }
            else
            {
                boolean result = ojbIterator.hasNext();
             	return result;
            }
        }
        else
        {
			releaseJdbcResources();
            return false;
        }

    }

    protected void releaseJdbcResources()
    {
        if (!isResultSetClosed())
        {
            if (ojbIterator instanceof RsIterator)
            {
                ((RsIterator) ojbIterator).releaseDbResources();
            }
            setResultSetClosed(true);
        }
    }

    /*
     * @see ObjectSet#next()
     */
    public synchronized Object next()
    {
        if (position < scrolled)
        {
            position++;
            return elements.get(position - 1);
        }
        else
        {
            Object next = ojbIterator.next();
            elements.add(next);
            position++;
            scrolled++;
            return next;
        }
    }

    /*
     * @see ObjectSet#reset()
     */
    public synchronized void reset()
    {
        position = 0;
    }

    /*
     * @see ObjectSet#size()
     */
    public int size()
    {
        return length;
    }

    /**
     * Gets the resultSetClosed.
     * @return Returns a boolean
     */
    public boolean isResultSetClosed()
    {
        return resultSetClosed;
    }

    /**
     * Sets the resultSetClosed.
     * @param resultSetClosed The resultSetClosed to set
     */
    public void setResultSetClosed(boolean resultSetClosed)
    {
        this.resultSetClosed = resultSetClosed;
    }
}
