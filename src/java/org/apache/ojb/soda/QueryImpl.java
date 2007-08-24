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

import java.io.Serializable;

import org.odbms.Constraint;
import org.odbms.ObjectSet;
import org.odbms.Query;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PersistenceBroker;

/**
 * @author Thomas Mahler
 * @version $Id: QueryImpl.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class QueryImpl implements Query, Serializable
{
	static final long serialVersionUID = 7117766237756132776L;
	private org.apache.ojb.broker.query.Query ojbQuery = null;
	private int limitCount = -1;
    private PersistenceBroker broker;

    /**
     * Constructor for QueryImpl.
     */
    public QueryImpl(PersistenceBroker broker)
    {
        super();
        this.broker = broker;
    }

    /**
     * wrapping constructor. needed only as long we
     * don't support the soda constraint stuff.
     */
    public QueryImpl(org.apache.ojb.broker.query.Query query)
    {
        super();
        ojbQuery = query;
    }


    /*
     * @see Query#constrain(Object)
     */
    public Constraint constrain(Object example)
    {
        return null;
    }

    /*
     * @see Query#execute()
     */
    public ObjectSet execute()
    {
        // in future versions soda queries will be translated
        // into org.apache.ojb.broker.query.Query objects and placed
        // into the ojbQuery member variable.

        if (ojbQuery != null)
        {
            return new ObjectSetImpl(broker, ojbQuery, limitCount);
        }
        else throw new OJBRuntimeException("internal ojbQuery not filled. Can't execute this query yet!");
    }

    /*
     * @see Query#descendant(String)
     */
    public Query descendant(String path)
    {
        return this;
    }

    /*
     * @see Query#parent(String)
     */
    public Query parent(String path)
    {
        return this;
    }

    /*
     * @see Query#limitSize(int)
     */
    public Query limitSize(int count)
    {
        limitCount = count;
        return this;
    }

    /*
     * @see Query#orderAscending()
     */
    public Query orderAscending()
    {
        return this;
    }

    /*
     * @see Query#orderDescending()
     */
    public Query orderDescending()
    {
        return this;
    }


    /**
     * Sets the ojbQuery, needed only as long we
     * don't support the soda constraint stuff.
     * @param ojbQuery The ojbQuery to set
     */
    public void setOjbQuery(org.apache.ojb.broker.query.Query ojbQuery)
    {
        this.ojbQuery = ojbQuery;
    }

}
