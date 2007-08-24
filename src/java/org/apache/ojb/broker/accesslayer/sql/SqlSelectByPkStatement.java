package org.apache.ojb.broker.accesslayer.sql;

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

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.platforms.Platform;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.util.logging.Logger;

/**
 * Model a SELECT Statement by Primary Key
 *
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: SqlSelectByPkStatement.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */

public class SqlSelectByPkStatement extends SqlSelectStatement
{
    /**
     * Constructor for SqlSelectByPkStatement.
     *
     * @param cld
     * @param logger
     */
    public SqlSelectByPkStatement(Platform pf, ClassDescriptor cld, Logger logger)
    {
        super(pf, cld, buildQuery(cld), logger);
    }

    /**
     * Build a Pk-Query base on the ClassDescriptor.
     *
     * @param cld
     * @return a select by PK query
     */
    private static Query buildQuery(ClassDescriptor cld)
    {
        FieldDescriptor[] pkFields = cld.getPkFields();
        Criteria crit = new Criteria();

        for(int i = 0; i < pkFields.length; i++)
        {
            crit.addEqualTo(pkFields[i].getAttributeName(), null);
        }
        return new QueryByCriteria(cld.getClassOfObject(), crit);
    }
}