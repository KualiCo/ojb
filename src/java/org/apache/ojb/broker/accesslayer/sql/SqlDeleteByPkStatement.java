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
import org.apache.ojb.broker.util.logging.Logger;

/**
 * Model a DELETE Statement
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: SqlDeleteByPkStatement.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class SqlDeleteByPkStatement extends SqlPkStatement
{
    private String sql;

    /**
     * Constructor for SqlDeleteByPkStatement.
     *
     * @param cld
     * @param logger
     */
    public SqlDeleteByPkStatement(ClassDescriptor cld, Logger logger)
    {
        super(cld, logger);
    }

    /** @see SqlStatement#getStatement() */
    public String getStatement()
    {
        if(sql == null)
        {
            StringBuffer stmt = new StringBuffer(1024);
            ClassDescriptor cld = getClassDescriptor();

            stmt.append("DELETE FROM ");
            appendTable(cld, stmt);
            appendWhereClause(cld, true, stmt); //use Locking

            sql = stmt.toString();
        }
        return sql;
    }

}

