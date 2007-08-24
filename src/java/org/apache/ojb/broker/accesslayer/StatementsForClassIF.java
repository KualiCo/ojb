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

import org.apache.ojb.broker.PersistenceBrokerSQLException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;

/**
  * A class that implements this interface serves as a cache for 
  * <code>java.sql.Statements<code> used for persistence operations
  * on a given class. 
  * @author brj
  * @author <a href="mailto:rburt3@mchsi.com">Randall Burt</a>
  * @version $Id: StatementsForClassIF.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
  */

public interface StatementsForClassIF
{
	/**
     * Returns the DELETE Statement used for clazz.
     * @return java.sql.PreparedStatement
     */
	PreparedStatement getDeleteStmt(Connection con) throws SQLException;

	/**
     * Returns a generic unprepared Statement used for clazz.
     * Never use this method for UPDATE/INSERT/DELETE if you want to use the batch mode.
     * @return java.sql.Statement
     */
	Statement getGenericStmt(Connection con, boolean scrollable) throws PersistenceBrokerSQLException;

	/**
     * Returns the INSERT Statement used for clazz.
     * @return java.sql.PreparedStatement
     */

	PreparedStatement getInsertStmt(Connection con) throws SQLException;

	/**
     * Returns a prepared Statement used for clazz.
     * @return java.sql.Statement
     */
	PreparedStatement getPreparedStmt(Connection con, String sql,
                                      boolean scrollable, int explicitFetchSizeHint, boolean callableStmt)
            throws PersistenceBrokerSQLException;

	/**
     * Returns the SELECT Statement used for clazz.
     * @return java.sql.PreparedStatement
     */
	PreparedStatement getSelectByPKStmt(Connection con) throws SQLException;

	/**
     * Returns the UPDATE Statement used for clazz.
     * @return java.sql.PreparedStatement
     */
	PreparedStatement getUpdateStmt(Connection con) throws SQLException;
}
