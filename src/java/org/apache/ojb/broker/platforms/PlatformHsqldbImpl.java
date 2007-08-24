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


/**
 * This class extends <code>PlatformDefaultImpl</code> and defines specific
 * behavior for the Hsqldb platform.
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: PlatformHsqldbImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PlatformHsqldbImpl extends PlatformDefaultImpl
{
    private static final String LAST_INSERT = "CALL IDENTITY()";

    /**
     * Get join syntax type for this RDBMS - one on of the constants from JoinSyntaxType interface
     */
    public byte getJoinSyntaxType()
    {
        return SQL92_NOPAREN_JOIN_SYNTAX;
    }

    public String getLastInsertIdentityQuery(String tableName)
    {
        return LAST_INSERT;
    }
    
    /* (non-Javadoc)
     * @see org.apache.ojb.broker.platforms.Platform#addPagingSql(java.lang.StringBuffer)
     */
    public void addPagingSql(StringBuffer anSqlString)
    {
        anSqlString.insert(6, " LIMIT ? ? ");
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.platforms.Platform#bindPagingParametersFirst()
     */
    public boolean bindPagingParametersFirst()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.platforms.Platform#supportsPaging()
     */
    public boolean supportsPaging()
    {
        return true;
    }

// arminw: Check is not necessary any longer
//    /**
//     * HSQLDB does not implement CallableStatement.
//     *
//     * @see org.apache.ojb.broker.platforms.Platform#isCallableStatement(java.sql.PreparedStatement)
//     */
//    public boolean isCallableStatement(PreparedStatement stmt)
//    {
//        return false;
//    }

}
