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

import org.apache.ojb.broker.util.logging.Logger;

/**
 * Model a MN-Statement based on Table, Columns and Values
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: SqlMNStatement.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public abstract class SqlMNStatement implements SqlStatement
{
    private String m_table;
    private String[] m_columns;
    private Logger m_logger;


    /**
     * Constructor for SqlMNStatement.
     */
    public SqlMNStatement(String table, String[] columns, Logger logger)
    {
        super();
        this.m_table = table;
        this.m_columns = columns;
        this.m_logger = logger;
    }

    /**
     * append table name
     */
    protected void appendTable(String table, StringBuffer stmt)
    {
        stmt.append(table);
    }

    /**
     * Returns the columns.
     * @return String[]
     */
    protected String[] getColumns()
    {
        return m_columns;
    }

    /**
     * Returns the table.
     * @return String
     */
    protected String getTable()
    {
        return m_table;
    }

    /**
     * Returns the logger.
     * @return Logger
     */
    protected Logger getLogger()
    {
        return m_logger;
    }

    /**
     * Generate a sql where-clause matching the contraints defined by the array of fields
     *
     * @param columns array containing all columns used in WHERE clause
     */
    protected void appendWhereClause(StringBuffer stmt, Object[] columns)
    {
        stmt.append(" WHERE ");

        for (int i = 0; i < columns.length; i++)
        {
            if (i > 0)
            {
                stmt.append(" AND ");
            }
            stmt.append(columns[i]);
            stmt.append("=?");
        }
    }

    /**
    * Appends to the statement a comma separated list of column names.
    *
    * @param columns defines the columns to be selected (for reports)
    * @return list of column names
    */
    protected List appendListOfColumns(String[] columns, StringBuffer stmt)
    {
        ArrayList columnList = new ArrayList();

        for (int i = 0; i < columns.length; i++)
        {
            if (i > 0)
            {
                stmt.append(",");
            }
            stmt.append(columns[i]);
            columnList.add(columns[i]);
        }
        return columnList;

    }


}
