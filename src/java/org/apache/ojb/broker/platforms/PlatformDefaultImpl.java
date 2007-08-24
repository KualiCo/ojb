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

import java.io.StringReader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Properties;

import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.accesslayer.JoinSyntaxTypes;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.query.LikeCriteria;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * This class is a concrete implementation of <code>Platform</code>. Provides default implementations for all
 * methods declared in <code>Platform</code>.
 * It is intended as a vanilla implementation and as baseclass for
 * platform specific implementations.
 *
 * @version $Id: PlatformDefaultImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 * @author		Thomas Mahler
 */
public class PlatformDefaultImpl implements Platform, JoinSyntaxTypes
{
    protected Logger log = LoggerFactory.getLogger(PlatformDefaultImpl.class);
    private static final String INITIALIZATION_CHECK_AUTOCOMMIT = "initializationCheck";
    private static final String FALSE_STR = "false";

    protected boolean m_batchUpdatesChecked = false;
    protected boolean m_supportsBatchUpdates = false;

    public boolean supportsBatchOperations()
    {
        return m_supportsBatchUpdates;
    }

    /**
     * Sets platform information for if the jdbc driver/db combo support
     * batch operations. Will only be checked once, then have same batch
     * support setting for the entire session.
     *
     * @param conn
     */
    protected void checkForBatchSupport(Connection conn)
    {
        if (!m_batchUpdatesChecked)
        {
            DatabaseMetaData meta;
            try
            {
                meta = conn.getMetaData();
                m_supportsBatchUpdates = meta.supportsBatchUpdates();
            }
            catch (Throwable th)
            {
                log.info("Batch support check failed", th);
                m_supportsBatchUpdates = false;
            }
            finally
            {
                m_batchUpdatesChecked = true;
            }
        }
    }

    public void afterStatementCreate(Statement stmt) throws PlatformException
    {
        //noop
    }

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
                throw new PlatformException("Resultset closing failed", e);
            }
        }
    }

    public void afterStatementClose(Statement stmt, ResultSet rs) throws PlatformException
    {
        //nothing
    }

    public void beforeBatch(PreparedStatement stmt) throws PlatformException
    {
        // nothing
    }

    public void addBatch(PreparedStatement stmt) throws PlatformException
    {
        try
        {
            stmt.addBatch();
        }
        catch (SQLException e)
        {
            throw new PlatformException("Failure while calling 'addBatch' on given Statement object", e);
        }
    }

    public int[] executeBatch(PreparedStatement stmt) throws PlatformException
    {
        try
        {
            return stmt.executeBatch();
        }
        catch (SQLException e)
        {
            throw new PlatformException("Failure while calling 'executeBatch' on given Statement object", e);
        }
    }


    /**
     * @see Platform#initializeJdbcConnection
     */
    public void initializeJdbcConnection(JdbcConnectionDescriptor jcd, Connection conn) throws PlatformException
    {
        if (jcd.getBatchMode()) checkForBatchSupport(conn);

        switch (jcd.getUseAutoCommit())
        {
            case JdbcConnectionDescriptor.AUTO_COMMIT_IGNORE_STATE:
                // nothing to do
                break;
            case JdbcConnectionDescriptor.AUTO_COMMIT_SET_TRUE_AND_TEMPORARY_FALSE:
                try
                {
                    /*
                    arminw:
                    workaround to be backward compatible. In future releases we shouldn't change the autocommit
                    state of a connection at initializing by the ConnectionFactory. The autocommit state should
                    only be changed by the ConnectionManager. We have to separate this stuff.
                    */
                    if (!jcd.getAttribute(INITIALIZATION_CHECK_AUTOCOMMIT, FALSE_STR).equalsIgnoreCase(FALSE_STR)
                            && !conn.getAutoCommit())
                    {
                        conn.setAutoCommit(true);
                    }
                }
                catch (SQLException e)
                {
                    if (!jcd.isIgnoreAutoCommitExceptions())
                    {
                        throw new PlatformException("Connection initializing: setAutoCommit(true) failed", e);
                    }
                    else
                    {
                        log.info("Connection initializing: setAutoCommit jdbc-driver problems. " + e.getMessage());
                    }
                }
                break;
            case JdbcConnectionDescriptor.AUTO_COMMIT_SET_FALSE:
                try
                {
                    if (conn.getAutoCommit()) conn.setAutoCommit(false);
                }
                catch (SQLException e)
                {
                    if (!jcd.isIgnoreAutoCommitExceptions())
                    {
                        throw new PlatformException("Connection initializing: setAutoCommit(false) failed", e);
                    }
                    else
                    {
                        log.info("Connection initializing: setAutoCommit jdbc-driver problems. " + e.getMessage());
                    }
                }
                break;
        }
    }

    public void changeAutoCommitState(JdbcConnectionDescriptor jcd, Connection con, boolean newState)
    {
        if (con == null)
        {
            log.error("Given m_connection was null, cannot prepare autoCommit state");
            return;
        }
        if (JdbcConnectionDescriptor.AUTO_COMMIT_SET_TRUE_AND_TEMPORARY_FALSE == jcd.getUseAutoCommit())
        {
            try
            {
                con.setAutoCommit(newState);
            }
            catch (SQLException e)
            {
                if (jcd.isIgnoreAutoCommitExceptions())
                {
                    log.info("Set autoCommit(" + newState + ") failed: " + e.getMessage());
                }
                else
                {
                    log.error("Set autoCommit(" + newState + ") failed", e);
                    throw new PersistenceBrokerException("Set autoCommit(false) failed", e);
                }
            }
        }
    }

    /*
     * @see Platform#setObject(PreparedStatement, int, Object, int)
     */
    public void setObjectForStatement(PreparedStatement ps, int index, Object value, int sqlType)
            throws SQLException
    {
        if ((sqlType == Types.LONGVARCHAR) && (value instanceof String))
        {
            String s = (String) value;
            ps.setCharacterStream(index, new StringReader(s), s.length());
        }
        /*
        PATCH for BigDecimal truncation problem. Seems that several databases (e.g. DB2, Sybase)
        has problem with BigDecimal fields if the sql-type was set. The problem was discussed here
        http://nagoya.apache.org/eyebrowse/ReadMsg?listName=ojb-user@db.apache.org&msgNo=14113
        A better option will be
        <snip>
        else if ((value instanceof BigDecimal) && (sqlType == Types.DECIMAL
                 || sqlType == Types.NUMERIC))
         {
             ps.setObject(index, value, sqlType,
                     ((BigDecimal) value).scale());
         }
         </snip>
        But this way maxDB/sapDB does not work correct, so we use the most flexible solution
        and let the jdbc-driver handle BigDecimal objects by itself.
        */
        else if(sqlType == Types.DECIMAL || sqlType == Types.NUMERIC)
        {
            ps.setObject(index, value);
        }
        else
        {
// arminw: this method call is done very, very often, so we can improve performance
// by comment out this section
//            if (log.isDebugEnabled()) {
//                log.debug("Default setObjectForStatement, sqlType=" + sqlType +
//                          ", value class=" + (value == null ? "NULL!" : value.getClass().getName())
//                            + ", value=" + value);
//            }
            ps.setObject(index, value, sqlType);
        }
    }

    /*
     * @see Platform#setNullForStatement(PreparedStatement, int, int)
     */
    public void setNullForStatement(PreparedStatement ps, int index, int sqlType) throws SQLException
    {
        ps.setNull(index, sqlType);
    }

    /**
     * Get join syntax type for this RDBMS - one on of the constants from JoinSyntaxType interface
     *
     * @see Platform#getJoinSyntaxType
     */
    public byte getJoinSyntaxType()
    {
        return SQL92_JOIN_SYNTAX;
    }

    /**
     * Override default ResultSet size determination (rs.last();rs.getRow())
     * with select count(*) operation
     *
     * @see Platform#useCountForResultsetSize()
     */
    public boolean useCountForResultsetSize()
    {
        return false;
    }

    public String createSequenceQuery(String sequenceName, Properties prop)
    {
        return createSequenceQuery(sequenceName);
    }

    /**
     * Override this method to enable database based sequence generation
     */
    public String createSequenceQuery(String sequenceName)
    {
        /*default implementation does not support this*/
        throw new UnsupportedOperationException("This feature is not supported by this implementation");
    }

    /**
     * Override this method to enable database based sequence generation
     */
    public String nextSequenceQuery(String sequenceName)
    {
        /*default implementation does not support this*/
        throw new UnsupportedOperationException("This feature is not supported by this implementation");
    }

    /**
     * Override this method to enable database based sequence generation
     */
    public String dropSequenceQuery(String sequenceName)
    {
        /*default implementation does not support this*/
        throw new UnsupportedOperationException("This feature is not supported by this implementation");
    }

    public CallableStatement prepareNextValProcedureStatement(Connection con, String procedureName,
                                                              String sequenceName) throws PlatformException
    {
        /*@todo implementation*/
        throw new UnsupportedOperationException("Not supported by this implementation");
    }

    public String getLastInsertIdentityQuery(String tableName)
    {
        /*@todo implementation*/
        throw new UnsupportedOperationException("This feature is not supported by this implementation");
    }

    /**
     * @see org.apache.ojb.broker.platforms.Platform#addPagingSql(java.lang.StringBuffer)
     */
    public void addPagingSql(StringBuffer anSqlString)
    {
        // do nothing
    }

    /**
     * @see org.apache.ojb.broker.platforms.Platform#bindPagingParametersFirst()
     */
    public boolean bindPagingParametersFirst()
    {
        return false;
    }

    /**
     * @see org.apache.ojb.broker.platforms.Platform#supportsPaging()
     */
    public boolean supportsPaging()
    {
        return false;
    }

    /**
     * @see org.apache.ojb.broker.platforms.Platform#bindPagingParameters(java.sql.PreparedStatement, int, int, int)
     */
    public int bindPagingParameters(PreparedStatement ps, int index, int startAt, int endAt) throws SQLException
    {
        ps.setInt(index, startAt - 1);              // zero based start
        index++;
        ps.setInt(index, endAt - (startAt - 1));    // number of rows to fetch
        index++;
        return index;
    }

    /**
     * Answer the Character for Concatenation
     */
    protected String getConcatenationCharacter()
    {
        return "||";
    }

    /**
     * {@inheritDoc}
     */
    public boolean supportsMultiColumnCountDistinct()
    {
        return true;
    }

    /**
     * @see org.apache.ojb.broker.platforms.Platform#concatenate(java.lang.String[])
     */
    public String concatenate(String[] theColumns)
    {
        if (theColumns.length == 1)
        {
            return theColumns[0];
        }

        StringBuffer buf = new StringBuffer();
        String concatChar = getConcatenationCharacter();

        for (int i = 0; i < theColumns.length; i++)
        {
            if (i > 0)
            {
                buf.append(" ").append(concatChar).append(" ");
            }
            buf.append(theColumns[i]);
        }

        return buf.toString();
    }

    /**
     * @see org.apache.ojb.broker.platforms.Platform#getEscapeClause(org.apache.ojb.broker.query.LikeCriteria)
     */
    public String getEscapeClause(LikeCriteria aCriteria)
    {
        String value = (String) aCriteria.getValue();
        char escapeChar = LikeCriteria.getEscapeCharacter();

        if (value.indexOf(escapeChar) >= 0)
        {
            return " ESCAPE '" + escapeChar + "'";
        }
        else
        {
            return "";
        }
    }

    /**
     * @see org.apache.ojb.broker.platforms.Platform#registerOutResultSet(java.sql.CallableStatement, int)
     */
    public void registerOutResultSet(CallableStatement stmt, int position)
            throws SQLException
    {
        stmt.registerOutParameter(position, Types.OTHER);
    }
}
