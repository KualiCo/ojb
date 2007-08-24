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

import java.util.ArrayList;
import java.util.List;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.util.logging.Logger;

/**
 * Model an INSERT Statement
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: SqlInsertStatement.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class SqlInsertStatement extends SqlPkStatement
{
    private String sql;

    /**
     * Constructor for SqlInsertStatement.
     *
     * @param cld
     * @param logger
     */
    public SqlInsertStatement(ClassDescriptor cld, Logger logger)
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

            stmt.append("INSERT INTO ");
            appendTable(cld, stmt);
            stmt.append(" (");
            appendListOfColumns(cld, stmt);
            stmt.append(")");
            appendListOfValues(cld, stmt);

            sql = stmt.toString();
        }
        return sql;
    }

    private List appendListOfColumns(ClassDescriptor cld, StringBuffer buf)
    {
        FieldDescriptor[] fields = cld.getAllRwFields();

        ArrayList columnList = new ArrayList();

        for(int i = 0; i < fields.length; i++)
        {
            if(i > 0)
            {
                buf.append(",");
            }
            buf.append(fields[i].getColumnName());
            columnList.add(fields[i].getAttributeName());
        }
        return columnList;
    }

    /**
     * generates a values(?,) for a prepared insert statement.
     * returns null if there are no fields
     * @param stmt the StringBuffer
     */
    private void appendListOfValues(ClassDescriptor cld, StringBuffer stmt)
    {
        FieldDescriptor[] fields = cld.getAllRwFields();

        if(fields.length == 0)
        {
            return;
        }

        stmt.append(" VALUES (");
        for(int i = 0; i < fields.length; i++)
        {
            stmt.append("?");
            if(i < fields.length - 1)
            {
                stmt.append(",");
            }
        }
        stmt.append(") ");
    }

}

