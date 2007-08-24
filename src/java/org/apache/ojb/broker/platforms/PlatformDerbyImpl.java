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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * This class defines specific behavior for the Derby platform.
 */
public class PlatformDerbyImpl extends PlatformDefaultImpl
{
    /**
     * {@inheritDoc}
     */
    public byte getJoinSyntaxType()
    {
        return SQL92_NOPAREN_JOIN_SYNTAX;
    }

    /**
     * {@inheritDoc}
     */
    public boolean supportsMultiColumnCountDistinct()
    {
        // Currently Derby supports COUNT DISTINCT only for one column
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void setObjectForStatement(PreparedStatement ps, int index, Object value, int jdbcType) throws SQLException
    {
        if (((jdbcType == Types.CHAR) || (jdbcType == Types.VARCHAR)) &&
            (value instanceof Character))
        {
            // [tomdz]
            // Currently, Derby doesn't like Character objects in the PreparedStatement
            // when using PreparedStatement#setObject(index, value, jdbcType) method
            // (see issue DERBY-773)
            // So we make a String object out of the Character object and use that instead
            super.setObjectForStatement(ps, index, value.toString(), jdbcType);
        }
        else
        {
            super.setObjectForStatement(ps, index, value, jdbcType);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getLastInsertIdentityQuery(String tableName)
    {
        // matthias.roth@impart.ch
        // the function is used by the org.apache.ojb.broker.util.sequence.SequenceManagerNativeImpl
		// this call must be made before commit the insert command, so you
        // must turn off autocommit by seting the useAutoCommit="2"
        // or use useAutoCommit="1" or use a connection with autoCommit set false
        // by default (e.g. in managed environments)
        // transaction demarcation is mandatory
        return "values IDENTITY_VAL_LOCAL()";
    }
}
