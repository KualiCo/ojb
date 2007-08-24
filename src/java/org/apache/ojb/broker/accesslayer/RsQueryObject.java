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

import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryBySQL;
import org.apache.ojb.broker.metadata.ClassDescriptor;

/**
 * Helper class for {@link RsIterator} queries.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: RsQueryObject.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class RsQueryObject
{
    private Query query;
    private ClassDescriptor cld;
    private boolean isSQLBased;

    //*******************************************
    // static access methods
    //*******************************************

    /**
     * Returns a new instance of this class.
     */
    public static RsQueryObject get(ClassDescriptor cld, Query query)
    {
        return new RsQueryObject(cld, query);
    }


    //*******************************************
    // private constructors
    //*******************************************
    private RsQueryObject(ClassDescriptor cld, Query query)
    {
        this.query = query;
        this.cld = cld;
        if(query instanceof QueryBySQL)
        {
            isSQLBased = true;
        }
    }

    //*******************************************
    // public methods
    //*******************************************
    public ResultSetAndStatement performQuery(JdbcAccess jdbcAccess)
    {
        if (isSQLBased())
        {

            return jdbcAccess.executeSQL(((QueryBySQL) query).getSql(), cld, Query.SCROLLABLE);
        }
        else
        {
            return jdbcAccess.executeQuery(query, cld);
        }
    }

    public boolean usePaging()
    {
        return query.usePaging();
    }

    public int getStartIndex()
    {
        return query.getStartAtIndex();
    }

    public int getEndIndex()
    {
        return query.getEndAtIndex();
    }

    public ClassDescriptor getClassDescriptor()
    {
        return cld;
    }

    public Query getQuery()
    {
        return query;
    }

    public boolean isSQLBased()
    {
        return isSQLBased;
    }

    public String getSQLBasedQuery()
    {
        if(isSQLBased())
        {
            return ((QueryBySQL)query).getSql();
        }
        else
        {
            return null;
        }
    }

    public String toString()
    {
        return this.getClass().getName() +
                "[" + "query: " + query + ", class descriptor: " + cld.getClassNameOfObject() + "]";
    }
}
