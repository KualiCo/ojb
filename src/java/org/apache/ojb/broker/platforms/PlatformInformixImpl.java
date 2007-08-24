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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;

/**
 * This class extends <code>PlatformDefaultImpl</code> and defines specific
 * behavior for the Informix platform.
 *
 * @version 1.0
 * @author Thomas Mahler
 */
public class PlatformInformixImpl extends PlatformDefaultImpl
{

    /** @see Platform#initializeJdbcConnection */
    public void initializeJdbcConnection(JdbcConnectionDescriptor jcd, Connection conn) throws PlatformException
    {
        super.initializeJdbcConnection(jcd, conn);
        Statement stmt = null;
        try
        {
            stmt = conn.createStatement();
            stmt.execute("SET LOCK MODE TO WAIT");
        }
        catch (SQLException e)
        {
            // ignore it
        }
        finally
        {
            if(stmt != null)
            {
                try
                {
                    stmt.close();
                }
                catch(SQLException e)
                {
                    // ignore
                }
            }
        }
    }

    /**
     * @see org.apache.ojb.broker.platforms.PlatformDefaultImpl#prepareNextValProcedureStatement(java.sql.Connection,
     *      java.lang.String, java.lang.String)
     */
    public CallableStatement prepareNextValProcedureStatement(Connection con, String procedureName,
                                                              String sequenceName) throws PlatformException
    {
        try
        {
            /*
             * Following works for Informix Dynamik Server 9.4 and the Informix
             * JDBC.3.00.JC1 driver. It is important to call the executeQuery()
             * method here because the executeUpdate() method doesn't work
             * correctly and returns an error if it is called alone.
             */
            String sp = "{? = call " + procedureName + "(?,?)}";
            CallableStatement cs = con.prepareCall(sp);
            cs.registerOutParameter(1, Types.BIGINT);
            cs.setString(2, sequenceName);
            cs.executeQuery();
            return cs;
        }
        catch(SQLException e)
        {
            throw new PlatformException(e);
        }
    }
}
