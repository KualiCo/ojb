package org.apache.ojb.broker.platforms;

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

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.apache.ojb.broker.query.LikeCriteria;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: PlatformMsAccessImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PlatformMsAccessImpl extends PlatformDefaultImpl
{
    /**
     * @see Platform#setObjectForStatement(PreparedStatement, int, Object, int)
     */
    public void setObjectForStatement(PreparedStatement ps, int index, Object value, int sqlType)
        throws SQLException
    {
        if (sqlType == Types.DECIMAL)
        {
            ps.setBigDecimal(index, (BigDecimal) value);
        }
        else if (sqlType == Types.FLOAT)
        {
            // this is because in repository_junit.xml price field is a double in the Article class
            // but repository maps it to a sql type of FLOAT (any my ODBC/JDBC bridge/driver cannot
            // cope with this conversion...possibility of a truncation error heer as well...
            if (value instanceof Double)
            {
                ps.setDouble(index, ((Double) value).doubleValue());
            }
            else
            {
                super.setObjectForStatement(ps, index, value, sqlType);
            }
        }
        // patch by Ralph Brandes to allow writing to memo fields
        else if (sqlType == Types.LONGVARCHAR)
        {
            if (value instanceof String)
            {
                String s = (String) value;
                // ps.setCharacterStream(index, new StringReader(s), s.length());
                // for MSACCESS :                
                byte[] bytes = s.getBytes();
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);                
                ps.setAsciiStream(index, bais, bytes.length);
            }
            else
            {
                super.setObjectForStatement(ps, index, value, sqlType);
            }
        }
        // patch by Tino Schöllhorn 
        // Current ODBC-Implementation for Access (I use ODBC 4.0.xxxx) does not 
        // support the conversion of LONG values. 
        // Error is : "Optional feature not implemented"
        // So I try to pass the LONG-value as an Integer even though it might be possible
        // that the conversion fails - but I don't think that is an issues with Access anyway.
        else if (value instanceof Long)
        {
            ps.setInt(index,((Long)value).intValue());
        }
        else
        {
            super.setObjectForStatement(ps, index, value, sqlType);        
        }
    }        

    /**
     * @see Platform#beforeStatementClose(Statement stmt, ResultSet rs)
     */
    public void beforeStatementClose(Statement stmt, ResultSet rs) throws PlatformException
    {
        if (rs != null)
        {
            try
            {
                rs.close();
            }
            catch (SQLException e)
            {
                LoggerFactory.getDefaultLogger().warn(
                    "Resultset closing failed (can be ignored for MsAccess)");
            }
        }
    }
    
    /**
     * Answer the Character for Concatenation
     */
    protected String getConcatenationCharacter()
    {
        return "&";
    }    
    
    /**
     * @see org.apache.ojb.broker.platforms.Platform#getEscapeClause(org.apache.ojb.broker.query.LikeCriteria)
     */
    public String getEscapeClause(LikeCriteria aCriteria)
    {
        // TODO: implement ms-access escaping
        return "";  
    }
}
