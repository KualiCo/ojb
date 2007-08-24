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
import java.sql.Types;
import java.sql.SQLException;

/**
 * This class extends <code>PlatformDefaultImpl</code> and defines specific behavior for the
 * Microsoft SQL Server platform.
 * 
 * @version $Id: PlatformMsSQLServerImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PlatformMsSQLServerImpl extends PlatformDefaultImpl
{
    /**
     * Get join syntax type for this RDBMS - one on of the constants from JoinSyntaxType interface
     * MBAIRD: MS SQL Server 2000 actually supports both types, but due to a problem with the sql
     * generator, we opt to have no parens.
     */
    public byte getJoinSyntaxType()
    {
        return SQL92_NOPAREN_JOIN_SYNTAX;
    }

    public CallableStatement prepareNextValProcedureStatement(Connection con, String procedureName,
            String sequenceName) throws PlatformException
    {
        try
        {
            String sp = "{?= call " + procedureName + " (?)}";
            CallableStatement cs = con.prepareCall(sp);
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2, sequenceName);
            return cs;
        }
        catch (SQLException e)
        {
            throw new PlatformException(e);
        }
    }

    public String getLastInsertIdentityQuery(String tableName)
    {
        /*
        More info about the used identity-query see JIRA OJB-77
        http://issues.apache.org/jira/browse/OJB-77

        As suggested in OJB-77 the latest recommendation from MS was to
        use function "SELECT SCOPE_IDENTITY()" to get the latest generated identity
        for the current session and scope:
        "SCOPE_IDENTITY and @@IDENTITY will return last identity values generated in
        any table in the current session. However, SCOPE_IDENTITY returns values
        inserted only within the current scope; @@IDENTITY is not limited to a
        specific scope."
        */
        return "SELECT SCOPE_IDENTITY()";
    }

    /**
     * Answer the Character for Concatenation
     */
    protected String getConcatenationCharacter()
    {
        return "+";
    }

}
