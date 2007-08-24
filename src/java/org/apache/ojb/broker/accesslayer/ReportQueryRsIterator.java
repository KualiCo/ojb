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
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.JdbcTypesHelper;
import org.apache.ojb.broker.query.ReportQuery;

/**
 * RsIterator for ReportQueries
 *
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: ReportQueryRsIterator.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class ReportQueryRsIterator extends RsIterator
{

    private int m_attributeCount;
    private int[] m_jdbcTypes;
    
    /**
     * Constructor for ReportQueryRsIterator.
     */
    public ReportQueryRsIterator(RsQueryObject queryObject, PersistenceBrokerImpl broker)
    {
        super(queryObject, broker);
        try
        {
            // BRJ: use only explicit attributes (columns) ! 
            // ignore those automatically added for ordering or grouping 
            ReportQuery q = (ReportQuery)queryObject.getQuery();
            m_attributeCount = q.getAttributes().length;
            
            init_jdbcTypes();
        }
        catch (SQLException e)
        {
            releaseDbResources();
            throw new PersistenceBrokerException(e);
        }
    }

    /**
     * get the jdbcTypes from the Query or the ResultSet if not available from the Query
     * @throws SQLException
     */
    private void init_jdbcTypes() throws SQLException
    {
        ReportQuery q = (ReportQuery) getQueryObject().getQuery();
        m_jdbcTypes = new int[m_attributeCount];
        
        // try to get jdbcTypes from Query
        if (q.getJdbcTypes() != null)
        {
            m_jdbcTypes = q.getJdbcTypes();
        }
        else
        {
            ResultSetMetaData rsMetaData = getRsAndStmt().m_rs.getMetaData();
            for (int i = 0; i < m_attributeCount; i++)
            {
                m_jdbcTypes[i] = rsMetaData.getColumnType(i + 1);
            }
            
        }
    }
    
    
    /**
     * returns an Object[] representing the columns of the current ResultSet
     * row. There is no OJB object materialization, Proxy generation etc.
     * involved to maximize performance.
     */
    protected Object getObjectFromResultSet() throws PersistenceBrokerException
    {
        Object[] result = new Object[m_attributeCount];
        ReportQuery q =(ReportQuery) getQueryObject().getQuery();

        for (int i = 0; i < m_attributeCount; i++)
        {
            try
            {
                int jdbcType = m_jdbcTypes[i];
                String attr = q.getAttributes()[i];
                FieldDescriptor fld = (FieldDescriptor) q.getAttributeFieldDescriptors().get(attr);
                Object val =JdbcTypesHelper.getObjectFromColumn(getRsAndStmt().m_rs, new Integer(jdbcType), i + 1);
                
                if (fld != null && fld.getFieldConversion() != null)
                {
                    val = fld.getFieldConversion().sqlToJava(val);
                }
                result[i] = val;
            }
            catch (SQLException e)
            {
                throw new PersistenceBrokerException(e);
            }
        }
        return result;
    }
 

}
