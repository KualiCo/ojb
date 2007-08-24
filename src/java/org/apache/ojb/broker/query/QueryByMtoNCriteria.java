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


/**
 * represents a search by criteria.
 * "find all articles where article.price > 100"
 * could be represented as:
 *
 * Criteria crit = new Criteria();
 * crit.addGreaterThan("price", new Double(100));
 * Query qry = new QueryByCriteria(Article.class, crit);
 *
 * The PersistenceBroker can retrieve Objects by Queries as follows:
 *
 * PersistenceBroker broker = PersistenceBrokerFactory.createPersistenceBroker();
 * Collection col = broker.getCollectionByQuery(qry);
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: QueryByMtoNCriteria.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */

public class QueryByMtoNCriteria extends QueryByCriteria implements MtoNQuery
{
    private String indirectionTable;

	/**
	 * return indirectionTable
	 */ 
    public String getIndirectionTable()
    {
    	return  indirectionTable;
    }

    /**
     * Build a Query for class targetClass with criteria.
     * Criteriy may be null (will result in a query returning ALL objects from a table)
     */
    public QueryByMtoNCriteria(Class targetClass, String indirectionTable, Criteria criteria)
    {
        this(targetClass, indirectionTable, criteria, false);
    }

    /**
     * Build a Query for class targetClass with criteria.
     * Criteriy may be null (will result in a query returning ALL objects from a table)
     */
    public QueryByMtoNCriteria(Class targetClass, String indirectionTable, Criteria criteria, boolean distinct)
    {
        super(targetClass , criteria, distinct);
        this.indirectionTable = indirectionTable;
    }

    /**
     * Insert the method's description here.
     * Creation date: (07.02.2001 22:01:55)
     * @return java.lang.String
     */
    public String toString()
    {
        return "Query from " + indirectionTable + " where " + getCriteria();
    }

}
