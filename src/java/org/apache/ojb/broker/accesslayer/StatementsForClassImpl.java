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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.ojb.broker.PersistenceBrokerSQLException;
import org.apache.ojb.broker.accesslayer.sql.SqlGenerator;
import org.apache.ojb.broker.accesslayer.sql.SqlGeneratorFactory;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.ConnectionPoolDescriptor;
import org.apache.ojb.broker.platforms.Platform;
import org.apache.ojb.broker.platforms.PlatformException;
import org.apache.ojb.broker.platforms.PlatformFactory;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.ExceptionHelper;

/**
 * This class serves as a cache for Statements that are
 * used for persistence operations on a given class.
 * @author Thomas Mahler
 * @version $Id: StatementsForClassImpl.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class StatementsForClassImpl implements StatementsForClassIF
{
    private Logger log = LoggerFactory.getLogger(StatementsForClassImpl.class);

    /**
     * sets the escape processing mode
     */
    // protected static boolean ESCAPEPROCESSING = false;

    protected final ClassDescriptor classDescriptor;
    protected final SqlGenerator sqlGenerator;
    protected final Platform platform;
    protected final Class clazz;
    protected final int fetchSize;
    private String deleteSql;
    private String insertSql;
    private String updateSql;
    private String selectByPKSql;

    /**
     * force use of JDBC 1.0 statement creation
     */
    protected boolean FORCEJDBC1_0 = false;

    public StatementsForClassImpl(
        final JdbcConnectionDescriptor jcd,
        final ClassDescriptor classDescriptor)
    {
        this.classDescriptor = classDescriptor;
        clazz = classDescriptor.getClassOfObject();
        platform = PlatformFactory.getPlatformFor(jcd);
        sqlGenerator = SqlGeneratorFactory.getInstance().createSqlGenerator(platform);

        final ConnectionPoolDescriptor cpd = jcd.getConnectionPoolDescriptor();
        fetchSize = cpd.getFetchSize();

        // detect JDBC level
        double level = jcd.getJdbcLevel();
        FORCEJDBC1_0 = level == 1.0;
    }

    /**
     * Answer true if a PreparedStatement has to be used
     * <br>false for a CallableStatement
     */
    protected boolean usePreparedDeleteStatement()
    {
        return !(classDescriptor.getDeleteProcedure() != null &&
                classDescriptor.getDeleteProcedure().hasReturnValues());
    }

    public PreparedStatement getDeleteStmt(Connection con) throws SQLException
    {
        if (deleteSql == null)
        {
            deleteSql = sqlGenerator.getPreparedDeleteStatement(classDescriptor).getStatement();
        }
        try
        {
            return prepareStatement(con,
                    deleteSql,
                    Query.NOT_SCROLLABLE,
                    usePreparedDeleteStatement(),
                    StatementManagerIF.FETCH_SIZE_NOT_APPLICABLE);
        }
        catch (SQLException ex)
        {
            log.error("Can't prepare delete statement: " + deleteSql, ex);
            throw ex;
        }
    }

    public Statement getGenericStmt(Connection con, boolean scrollable)
        throws PersistenceBrokerSQLException
    {
        Statement stmt;
        try
        {
            stmt = createStatement(con, scrollable,
                    StatementManagerIF.FETCH_SIZE_NOT_EXPLICITLY_SET);
        }
        catch (SQLException ex)
        {
            throw ExceptionHelper.generateException("Can't prepare statement:", ex, null, log);
        }
        return stmt;
    }

    /**
     * Answer true if a PreparedStatement has to be used
     * <br>false for a CallableStatement
     */
    protected boolean usePreparedInsertStatement()
    {
        return !(classDescriptor.getInsertProcedure() != null &&
                classDescriptor.getInsertProcedure().hasReturnValues());
    }

    public PreparedStatement getInsertStmt(Connection con) throws SQLException
    {
        if (insertSql == null)
        {
            insertSql = sqlGenerator.getPreparedInsertStatement(classDescriptor).getStatement();
        }
        try
        {
            return prepareStatement(con,
                    insertSql,
                    Query.NOT_SCROLLABLE,
                    usePreparedInsertStatement(),
                    StatementManagerIF.FETCH_SIZE_NOT_APPLICABLE);
        }
        catch (SQLException ex)
        {
            log.error("Can't prepare insert statement: " + insertSql, ex);
            throw ex;
        }
    }

    public PreparedStatement getPreparedStmt(Connection con, String sql,
                                             boolean scrollable, int explicitFetchSizeHint, boolean callableStmt)
        throws PersistenceBrokerSQLException
    {
        PreparedStatement stmt;
        try
        {
            stmt = prepareStatement(con, sql, scrollable,
                    !callableStmt, explicitFetchSizeHint);
        }
        catch (SQLException ex)
        {
            throw ExceptionHelper.generateException("Can't prepare statement:", ex, sql, log);
        }
        return stmt;
    }

    public PreparedStatement getSelectByPKStmt(Connection con) throws SQLException
    {
        if (selectByPKSql == null)
        {
            selectByPKSql = sqlGenerator.getPreparedSelectByPkStatement(classDescriptor).getStatement();
        }
        try
        {
            return prepareStatement(con, selectByPKSql, Query.NOT_SCROLLABLE, true, 1);
        }
        catch (SQLException ex)
        {
            log.error(ex);
            throw ex;
        }
    }

    /**
     * Answer true if a PreparedStatement has to be used
     * <br>false for a CallableStatement
     */
    protected boolean usePreparedUpdateStatement()
    {
        return !(classDescriptor.getUpdateProcedure() != null &&
                classDescriptor.getUpdateProcedure().hasReturnValues());
    }

    public PreparedStatement getUpdateStmt(Connection con) throws SQLException
    {
        if (updateSql == null)
        {
            updateSql = sqlGenerator.getPreparedUpdateStatement(classDescriptor).getStatement();
        }
        try
        {
            return prepareStatement(con,
                    updateSql,
                    Query.NOT_SCROLLABLE,
                    usePreparedUpdateStatement(),
                    StatementManagerIF.FETCH_SIZE_NOT_APPLICABLE);
        }
        catch (SQLException ex)
        {
            log.error("Can't prepare update statement: " + updateSql, ex);
            throw ex;
        }
    }

    /**
     * Prepares a statement with parameters that should work with most RDBMS.
     *
     * @param con the connection to utilize
     * @param sql the sql syntax to use when creating the statement.
     * @param scrollable determines if the statement will be scrollable.
     * @param createPreparedStatement if <code>true</code>, then a
     * {@link PreparedStatement} will be created. If <code>false</code>, then
     * a {@link java.sql.CallableStatement} will be created.
     * @param explicitFetchSizeHint will be used as fetchSize hint
     * (if applicable) if > 0
     *
     * @return a statement that can be used to execute the syntax contained in
     * the <code>sql</code> argument.
     */
    protected PreparedStatement prepareStatement(Connection con,
                                                 String sql,
                                                 boolean scrollable,
                                                 boolean createPreparedStatement,
                                                 int explicitFetchSizeHint)
        throws SQLException
    {
        PreparedStatement result;

        // if a JDBC1.0 driver is used the signature
        // prepareStatement(String, int, int) is  not defined.
        // we then call the JDBC1.0 variant prepareStatement(String)
        try
        {
            // if necessary use JDB1.0 methods
            if (!FORCEJDBC1_0)
            {
                if (createPreparedStatement)
                {
                    result =
                        con.prepareStatement(
                            sql,
                            scrollable
                                ? ResultSet.TYPE_SCROLL_INSENSITIVE
                                : ResultSet.TYPE_FORWARD_ONLY,
                            ResultSet.CONCUR_READ_ONLY);
                    afterJdbc2CapableStatementCreate(result, explicitFetchSizeHint);
                }
                else
                {
                    result =
                        con.prepareCall(
                            sql,
                            scrollable
                                ? ResultSet.TYPE_SCROLL_INSENSITIVE
                                : ResultSet.TYPE_FORWARD_ONLY,
                            ResultSet.CONCUR_READ_ONLY);
                }
            }
            else
            {
                if (createPreparedStatement)
                {
                    result = con.prepareStatement(sql);
                }
                else
                {
                    result = con.prepareCall(sql);
                }
            }
        }
        catch (AbstractMethodError err)
        {
            // this exception is raised if Driver is not JDBC 2.0 compliant
            log.warn("Used driver seems not JDBC 2.0 compatible, use the JDBC 1.0 mode", err);
            if (createPreparedStatement)
            {
                result = con.prepareStatement(sql);
            }
            else
            {
                result = con.prepareCall(sql);
            }
            FORCEJDBC1_0 = true;
        }
        catch (SQLException eSql)
        {
            // there are JDBC Driver that nominally implement JDBC 2.0, but
            // throw DriverNotCapableExceptions. If we catch one of these
            // we force usage of JDBC 1.0
            if (eSql
                .getClass()
                .getName()
                .equals("interbase.interclient.DriverNotCapableException"))
            {
                log.warn("JDBC 2.0 problems with this interbase driver, we use the JDBC 1.0 mode");
                if (createPreparedStatement)
                {
                    result = con.prepareStatement(sql);
                }
                else
                {
                    result = con.prepareCall(sql);
                }
                FORCEJDBC1_0 = true;
            }
            else
            {
                throw eSql;
            }
        }
        try
        {
            if (!ProxyHelper.isNormalOjbProxy(result))  // tomdz: What about VirtualProxy
            {
                platform.afterStatementCreate(result);
            }
        }
        catch (PlatformException e)
        {
            log.error("Platform dependend failure", e);
        }
        return result;
    }

    /**
     * Creates a statement with parameters that should work with most RDBMS.
     */
    private Statement createStatement(Connection con, boolean scrollable, int explicitFetchSizeHint)
        throws java.sql.SQLException
    {
        Statement result;
        try
        {
            // if necessary use JDBC1.0 methods
            if (!FORCEJDBC1_0)
            {
                result =
                    con.createStatement(
                        scrollable
                            ? ResultSet.TYPE_SCROLL_INSENSITIVE
                            : ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY);
                afterJdbc2CapableStatementCreate(result, explicitFetchSizeHint);
            }
            else
            {
                result = con.createStatement();
            }
        }
        catch (AbstractMethodError err)
        {
            // if a JDBC1.0 driver is used, the signature
            // createStatement(int, int) is  not defined.
            // we then call the JDBC1.0 variant createStatement()
            log.warn("Used driver seems not JDBC 2.0 compatible, use the JDBC 1.0 mode", err);
            result = con.createStatement();
            FORCEJDBC1_0 = true;
        }
        catch (SQLException eSql)
        {
            // there are JDBC Driver that nominally implement JDBC 2.0, but
            // throw DriverNotCapableExceptions. If we catch one of these
            // we force usage of JDBC 1.0
            if (eSql.getClass().getName()
                .equals("interbase.interclient.DriverNotCapableException"))
            {
                log.warn("JDBC 2.0 problems with this interbase driver, we use the JDBC 1.0 mode");
                FORCEJDBC1_0 = true;
                result = con.createStatement();
            }
            else
            {
                throw eSql;
            }
        }
        try
        {
            platform.afterStatementCreate(result);
        }
        catch (PlatformException e)
        {
            log.error("Platform dependend failure", e);
        }
        return result;
    }

    private void afterJdbc2CapableStatementCreate(Statement stmt, int explicitFetchSizeHint)
            throws SQLException
    {
        if (stmt != null)
        {
            final int fetchSizeHint;
            if (explicitFetchSizeHint == StatementManagerIF.FETCH_SIZE_NOT_APPLICABLE)
            {
                fetchSizeHint = StatementManagerIF.FETCH_SIZE_NOT_APPLICABLE;
            }
            else if (explicitFetchSizeHint != StatementManagerIF.FETCH_SIZE_NOT_EXPLICITLY_SET)
            {
                fetchSizeHint = explicitFetchSizeHint; // specific for this Statement
            }
            else
            {
                fetchSizeHint = fetchSize; // connection pool default
            }
            if (fetchSizeHint > 0)
            {
                stmt.setFetchSize(fetchSize);
            }
        }
    }

}
