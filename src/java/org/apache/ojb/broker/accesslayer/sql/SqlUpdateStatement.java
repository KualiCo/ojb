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
import org.apache.ojb.broker.util.logging.Logger;

/**
 * Model an UPDATE Statement
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: SqlUpdateStatement.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class SqlUpdateStatement extends SqlPkStatement
{
    protected String sql;

    /**
     * Constructor for SqlUpdateStatement.
     *
     * @param cld
     * @param logger
     */
    public SqlUpdateStatement(ClassDescriptor cld, Logger logger)
    {
        super(cld, logger);
    }

    /**
     * generates a SET-phrase for a prepared update statement.
     *
     * @param stmt the StringBuffer
     */
    private void appendSetClause(ClassDescriptor cld, StringBuffer stmt)
    {
        FieldDescriptor[] fields = cld.getNonPkRwFields();

        if(fields.length == 0)
        {
            return;
        }

        stmt.append(" SET ");
        for(int i = 0; i < fields.length; i++)
        {
            stmt.append(fields[i].getColumnName());
            stmt.append("=?");
            if(i < fields.length - 1)
            {
                stmt.append(",");
            }
        }
    }

    /**
     * @see SqlStatement#getStatement()
     */
    public String getStatement()
    {
        if(sql == null)
        {
            StringBuffer stmt = new StringBuffer(1024);
            ClassDescriptor cld = getClassDescriptor();

            stmt.append("UPDATE ");
            appendTable(cld, stmt);
            appendSetClause(cld, stmt);
            appendWhereClause(cld, true, stmt); //use Locking

            sql = stmt.toString();
        }
        return sql;
    }

}
