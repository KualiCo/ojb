package org.apache.ojb.broker.query;

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

import java.util.List;

/**
 * represents Queries that can be used by the OJB PersistenceBroker
 * to retrieve Objects from the underlying DB.
 * Until now there are two implementations:
 * 1. QueryByCriteria, represents SELECT * FROM ... WHERE ... queries
 * 2. QueryByIdentity, uses Example objects or OIDs
 * as templates for the db lookup
 * there could additional implementations, e.g for user defined SQL
 *
 * For the Criteria API I reused code from the COBRA project,
 * as you will see by their class comments.
 *
 * I removed all stuff that relies on knowlegde of the DataDictionary
 * or MetaData layer. The Query and Criteria classes thus don't know
 * how to build SQL statements, as in the COBRA original sources.
 * I use the this classes as mere data-structures, that are
 * processed by the OJB Accesslayer (SqlGenerator, JdbcAccess).
 *
 * This design will allow to reuse the org.apache.ojb.broker.query package in other
 * projects without breaking any references. I hope this will be
 * useful for someone.
 *
 * @author Thomas Mahler
 * @version $Id: Query.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public interface Query extends java.io.Serializable
{
	static final long serialVersionUID = 7616997212439931319L;
    
    public static final int NO_START_AT_INDEX = 0;
    public static final int NO_END_AT_INDEX = 0;
    public static final boolean SCROLLABLE = true;
    public static final boolean NOT_SCROLLABLE = false;

    /**
     * return the criteria of the query if present or null.
     */
    public abstract Criteria getCriteria();

    /**
     * return the criteria of the query if present or null.
     */
    public abstract Criteria getHavingCriteria();

    /**
     * return the template Object if present or null
     */
    public abstract Object getExampleObject();

    /**
     * return the target class, representing the extend to be searched
     */
    public abstract Class getSearchClass();

    /**
     * return the base class, with respect to which all paths are done
     */
    public abstract Class getBaseClass();

    /**
     *  return true if select DISTINCT should be used
     */
    public boolean isDistinct();

    /**
     * Answer the orderBy of all Criteria and Sub Criteria the elements are of
     * class FieldHelper
     * @return List of FieldHelper
     */
    public List getOrderBy();

    /**
     * Gets the groupby for ReportQueries of all Criteria and Sub Criteria
     * the elements are of class FieldHelper
     * @return List of FieldHelper
     */
    public List getGroupBy();

    /**
     *
     * @return the row at which the query should start retrieving results. 
     * If the start at index is 0, then ignore all cursor control.
     */
    int getStartAtIndex();

    /**
     * Set the row at which the query should start retrieving results, inclusive
     * first row is 1
     * @param startAtIndex starting index, inclusive.
     */
    void setStartAtIndex(int startAtIndex);

    /**
     *
     * @return the row at which the query should stop retrieving results. 
     * If the end at index is 0, ignore all cursor control
     */
    int getEndAtIndex();

    /**
     * Set the row at which the query should stop retrieving results, inclusive.
     * first row is 1
     * @param endAtIndex ending index, inclusive
     */
    void setEndAtIndex(int endAtIndex);

    /**
     * Returns the names of Relationships to be prefetched
     * @return List of Strings
     */
    public List getPrefetchedRelationships();

    /**
     * @deprecated 
     * @param size
     */
    void fullSize(int size);
    /**
     * @deprecated use OJBIterator.fullSize()
     * @return
     */
    int fullSize();

    void setWithExtents(boolean withExtents);
    boolean getWithExtents();
    
    /**
     * Answer true if start- and endIndex is set
     * @return
     */
    public boolean usePaging();

    /**
     * Set fetchSize hint for this Query. Passed to the JDBC driver on the
     * Statement level. It is JDBC driver-dependant if this function has
     * any effect at all, since fetchSize is only a hint.
     * @param fetchSize the fetch size specific to this query
     */
    void setFetchSize(int fetchSize);

    /**
     * Returns the fetchSize hint for this Query
     * @return the fetch size hint specific to this query
     * (or 0 if not set / using driver default)
     */
    int getFetchSize();

}
