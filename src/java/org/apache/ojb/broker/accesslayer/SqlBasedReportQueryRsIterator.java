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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.metadata.JdbcTypesHelper;

/**
 * ReporQueryRsIterator based on SQL-Statement
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: SqlBasedReportQueryRsIterator.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class SqlBasedReportQueryRsIterator extends SqlBasedRsIterator
{
    private ResultSetMetaData rsMetaData;
    private int columnCount;

    /**
     * SqlBasedRsIterator constructor.
     */
    public SqlBasedReportQueryRsIterator(RsQueryObject queryObject, PersistenceBrokerImpl broker)
            throws PersistenceBrokerException
    {

        super(queryObject, broker);
        try
        {
            rsMetaData = getRsAndStmt().m_rs.getMetaData();
            columnCount = rsMetaData.getColumnCount();
        }
        catch (SQLException e)
        {
            throw new PersistenceBrokerException(e);
        }
    }

    /**
     * returns an Object[] representing the columns of the current ResultSet row.
     * There is no OJB object materialization, Proxy generation etc. involved
     * to maximize performance.
     */
    protected Object getObjectFromResultSet() throws PersistenceBrokerException
    {
        Object[] result = new Object[columnCount];
        for (int i = 0; i < columnCount; i++)
        {
            try
            {
                int jdbcType = rsMetaData.getColumnType(i + 1);
                Object item = JdbcTypesHelper.getObjectFromColumn(getRsAndStmt().m_rs, new Integer(jdbcType), i + 1);
                result[i] = item;
            }
            catch (SQLException e)
            {
                throw new PersistenceBrokerException(e);
            }
        }
        return result;
    }

}
