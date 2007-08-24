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

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;

/**
 * RsIterator based on SQL-Statement
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: SqlBasedRsIterator.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class SqlBasedRsIterator extends RsIterator
{
    /**
     * SqlBasedRsIterator constructor.
     */
    public SqlBasedRsIterator(RsQueryObject queryObject, PersistenceBrokerImpl broker)
            throws PersistenceBrokerException
    {
        super(queryObject, broker);
        if(!queryObject.isSQLBased())
        {
            throw new PersistenceBrokerException("Given query is not a QueryBySQL object");
        }
    }

    /**
     * returns a proxy or a fully materialized Object from the current row of the
     * underlying resultset.
     */
    protected Object getObjectFromResultSet() throws PersistenceBrokerException
    {

        try
        {
            // if all primitive attributes of the object are contained in the ResultSet
            // the fast direct mapping can be used
            return super.getObjectFromResultSet();
        }
                // if the full loading failed we assume that at least PK attributes are contained
                // in the ResultSet and perform a slower Identity based loading...
                // This may of course also fail and can throw another PersistenceBrokerException
        catch (PersistenceBrokerException e)
        {
            Identity oid = getIdentityFromResultSet();
            return getBroker().getObjectByIdentity(oid);
        }

    }
}
