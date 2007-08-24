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

import java.util.Iterator;

import org.apache.ojb.broker.PersistenceBrokerException;

/**
 * A {@link Iterator} extension internaly used by OJB to handle query results.
 *
 * <p>
 * NOTE: OJB is very strict in handling <tt>OJBIterator</tt> instances. <tt>OJBIterator</tt> is
 * bound very closely to the used {@link org.apache.ojb.broker.PersistenceBroker} instance.
 * Thus if you do a
 * <br/> - {@link org.apache.ojb.broker.PersistenceBroker#close}
 * <br/> - {@link org.apache.ojb.broker.PersistenceBroker#commitTransaction}
 * <br/> - {@link org.apache.ojb.broker.PersistenceBroker#abortTransaction}
 * <br/>
 * call, the current <tt>OJBIterator</tt> instance resources will be cleaned up automatic
 * and invalidate current instance.
 * </p>
 *
 * @version $Id: OJBIterator.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public interface OJBIterator extends Iterator
{
    /**
     * @return the size of the iterator, aka the number of rows in this iterator.
     */
    int size() throws PersistenceBrokerException;

    /**
     * @return the unlimited size of the iterator,
     * fullSize() may differ from size() for PagingIterator
     */
    int fullSize() throws PersistenceBrokerException;

    /**
     * Moves the cursor to the given row number in the iterator.
     * If the row number is positive, the cursor moves to the given row number with
     * respect to the beginning of the iterator. The first row is row 1, the second is row 2, and so on.
     * @param row the row to move to in this iterator, by absolute number
     */
    boolean absolute(int row) throws PersistenceBrokerException;

    /**
     * Moves the cursor a relative number of rows, either positive or negative. Attempting to move beyond the first/last
     * row in the iterator positions the cursor before/after the the first/last row. Calling relative(0) is valid,
     * but does not change the cursor position.
     * @param row the row to move to in this iterator, by relative number
     */
    boolean relative(int row) throws PersistenceBrokerException;

    /**
     * Release all internally used Database resources of the iterator.
     * Clients must call this methods explicitely if the iterator is not
     * exhausted by the client application. If the Iterator is exhauseted
     * this method will be called implicitely.
     */
    public void releaseDbResources();
    
    /**
     * Do not fire any PBLifeCycleEvent when reading next item. 
     */
    public void disableLifeCycleEvents();
}
