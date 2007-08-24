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

import org.apache.ojb.broker.util.logging.Logger;

/**
 * Model an INSERT Statement for M:N indirection table
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: SqlInsertMNStatement.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class SqlInsertMNStatement extends SqlMNStatement
{

    /**
     * Constructor for SqlInsertMNStatement.
     * @param table
     * @param columns
     */
    public SqlInsertMNStatement(String table, String[] columns, Logger logger)
    {
        super (table, columns, logger);
    }

    /**
     * generates a values(?,) for a prepared insert statement.
     * @param stmt the StringBuffer
     */
    private void appendListOfValues(StringBuffer stmt)
    {
        int cnt = getColumns().length;

        stmt.append(" VALUES (");

        for (int i = 0; i < cnt; i++)
        {
            if (i > 0)
            {
                stmt.append(',');
            }
            stmt.append('?');
        }
        stmt.append(')');
    }

    /**
     * @see org.apache.ojb.broker.accesslayer.SqlStatement#getStatement()
     */
    public String getStatement()
    {
        StringBuffer stmt = new StringBuffer(1024);

        stmt.append("INSERT INTO ");
        appendTable(getTable(),stmt);
        stmt.append(" (");
        appendListOfColumns(getColumns(),stmt);
        stmt.append(")");
        appendListOfValues(stmt);
        return stmt.toString();
    }

}

