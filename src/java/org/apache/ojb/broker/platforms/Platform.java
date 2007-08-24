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

import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.query.LikeCriteria;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * This interface provides callbacks that allow to perform
 * RDBMS Platform specific operations wherever neccessary.
 * The Platform implementation is selected by the platform attribute for
 * each jdbc-connection-descriptor entry in the repository file.
 *
 * @version $Id: Platform.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 * @author	 Thomas Mahler
 */
public interface Platform
{

    /**
     * Called after a statement has been created.
     */
    void afterStatementCreate(Statement stmt) throws PlatformException;

    /**
     * Called by {@link org.apache.ojb.broker.accesslayer.StatementManagerIF} implementation
     * before invoking <tt>stmt.close()</tt> method.
     */
    void beforeStatementClose(Statement stmt, ResultSet rs) throws PlatformException;

    /**
     * Called by {@link org.apache.ojb.broker.accesslayer.StatementManagerIF} implementation
     * after invoking <tt>stmt.close()</tt> method.
     */
    void afterStatementClose(Statement stmt, ResultSet rs) throws PlatformException;

    /**
     * Called before batching operations on a statement.
     * @param stmt the statement you want to batch on
     * @throws PlatformException
     */
    void beforeBatch(PreparedStatement stmt) throws PlatformException;

    /**
     * Called when adding statements to current batch.
     * @param stmt the statement you are adding to the batch
     * @throws PlatformException
     */
    void addBatch(PreparedStatement stmt) throws PlatformException;

    /**
     * Executes current batch.
     * @param stmt the statement you want to execute the batch on
     * @throws PlatformException
     */
    int[] executeBatch(PreparedStatement stmt) throws PlatformException;

    /**
     * Called immediately after a JDBC connection has been created by a
     * ConnectionFactory implementation (not used for DataSource connections).
     * @param conn the Connection to be initialized
     */
    void initializeJdbcConnection(JdbcConnectionDescriptor jcd, Connection conn) throws PlatformException;

    /**
     * Used to do a temporary change of the m_connection autoCommit state.
     * When using this method ensure to reset the original state before
     * m_connection was returned to pool or closed.
     * Only when
     * {@link org.apache.ojb.broker.metadata.JdbcConnectionDescriptor#getUseAutoCommit()} was set to
     * {@link org.apache.ojb.broker.metadata.JdbcConnectionDescriptor#AUTO_COMMIT_SET_TRUE_AND_TEMPORARY_FALSE}
     * the change of the autoCommit state take effect.
     */
    void changeAutoCommitState(JdbcConnectionDescriptor jcd, Connection con, boolean newState);

    /**
     * Called to let the Platform implementation perform any JDBC type-specific operations
     * needed by the driver when binding positional parameters for a PreparedStatement.
     */
    void setObjectForStatement(PreparedStatement ps, int index, Object value, int sqlType)
            throws SQLException;

    /**
     * Called to let the Platform implementation perform any JDBC type-specific operations
     * needed by the driver when binding null parameters for a PreparedStatement.
     */
    void setNullForStatement(PreparedStatement ps, int index, int sqlType)
            throws SQLException;

    /**
     * Get join syntax type for this RDBMS - one of the constants from JoinSyntaxTypes interface.
     * @see org.apache.ojb.broker.accesslayer.JoinSyntaxTypes#SQL92_JOIN_SYNTAX
     * @see org.apache.ojb.broker.accesslayer.JoinSyntaxTypes#SQL92_NOPAREN_JOIN_SYNTAX
     * @see org.apache.ojb.broker.accesslayer.JoinSyntaxTypes#ORACLE_JOIN_SYNTAX
     * @see org.apache.ojb.broker.accesslayer.JoinSyntaxTypes#SYBASE_JOIN_SYNTAX
     */
    byte getJoinSyntaxType();

    /**
     * Override default ResultSet size determination (rs.last();rs.getRow())
     * with select count(*) operation.
     */
    boolean useCountForResultsetSize();

    /**
     * If this platform supports the batch operations jdbc 2.0 feature. This is
     * by driver, so we check the driver's metadata once and set something in
     * the platform.
     * @return true if the platform supports batch, false otherwise.
     */
    boolean supportsBatchOperations();

// arminw: think we can handle this internally
//    /**
//     * Sets platform information for if the jdbc driver/db combo support
//     * batch operations. Will only be checked once, then have same batch
//     * support setting for the entire session.
//     * @param conn
//     */
//    void checkForBatchSupport(Connection conn);

    /**
     * Returns a query to create a sequence entry.
     *
     * @param sequenceName The name of the sequence to create.
     * @param prop The database specific sequence properties.
     * @return a sql string to create a sequence
     */
    String createSequenceQuery(String sequenceName, Properties prop);

    /**
     * Returns a query to create a sequence entry.
     *
     * @param sequenceName The name of the sequence to create.
     * @return a sql string to create a sequence
     * @deprecated use {@link #createSequenceQuery(String)} instead.
     */
    String createSequenceQuery(String sequenceName);

    /**
     * Returns a query to obtain the next sequence key.
     * @return a sql string to get next sequence value
     */
    String nextSequenceQuery(String sequenceName);

    /**
     * Returns a query to drop a sequence entry.
     * @return a sql string to drop a sequence
     */
    String dropSequenceQuery(String sequenceName);

    /**
     * Create stored procedure call for a special sequence manager implementation
     * {@link org.apache.ojb.broker.util.sequence.SequenceManagerStoredProcedureImpl},
     * because it seems that jdbc-driver differ in handling of CallableStatement.
     * <br/>
     * Note: The out-parameter of the stored procedure must be registered at
     * first position, because lookup for new long id in the implementation:
     * <br/>
     * <pre>
     * Connection con = broker.serviceConnectionManager().getConnection();
     * cs = getPlatform().prepareNextValProcedureStatement(con, PROCEDURE_NAME, sequenceName);
     * cs.executeUpdate();
     * return cs.getLong(1);
     * </pre>
     */
    CallableStatement prepareNextValProcedureStatement(Connection con, String procedureName,
                                                       String sequenceName) throws PlatformException;

    /**
     * If database supports native key generation via identity column, this
     * method should return the sql-query to obtain the last generated id.
     */
    String getLastInsertIdentityQuery(String tableName);

    /**
     * Answer true if LIMIT or equivalent is supported
     * <b> SQL-Paging is not yet supported </b>
     */
    boolean supportsPaging();

    /**
     * Add the LIMIT or equivalent to the SQL 
     * <b> SQL-Paging is not yet supported </b>
     */
    void addPagingSql(StringBuffer anSqlString);

    /**
     * Answer true if the LIMIT parameters are bound before the query parameters
     * <b> SQL-Paging is not yet supported </b>
     */
    boolean bindPagingParametersFirst();

    /**
     * Bind the Paging Parameters
     * <b> SQL-Paging is not yet supported </b>
     * @param ps
     * @param index parameter index
     * @param startAt
     * @param endAt
     */
    int bindPagingParameters(PreparedStatement ps, int index, int startAt, int endAt) throws SQLException;

    /**
     * Whether the platform supports a COUNT DISTINCT across multiple columns.
     * 
     * @return <code>true</code> if it is supported
     */
    boolean supportsMultiColumnCountDistinct();
    
    /**
     * Concatenate the columns </br>
     * ie: col1 || col2 || col3 (ANSI)</br>
     * ie: col1 + col2 + col3 (MS SQL-Server)</br>
     * ie: concat(col1, col2, col3) (MySql)
     * 
     * @param theColumns
     * @return the concatenated String 
     */
    String concatenate(String[] theColumns);

    /**
     * Answer the Clause used Escape wildcards in LIKE 
     * @param aCriteria
     */
    String getEscapeClause(LikeCriteria aCriteria);

// arminw: Check is not necessary any longer
//    /**
//     * Determines whether statement is {@link CallableStatement} or not.
//     *
//     * @param stmt the statement
//     * @return true if statement is {@link CallableStatement}.
//     */
//    boolean isCallableStatement(PreparedStatement stmt);

    /**
     * Registers call argument at <code>position</code> as returning
     * a {@link ResultSet} value.
     *
     * @param stmt     the statement
     * @param position argument position
     */
    void registerOutResultSet(CallableStatement stmt, int position)
            throws SQLException;

}
