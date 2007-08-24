package org.apache.ojb.broker.platforms;

/* Copyright 2004-2005 The Apache Software Foundation
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
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.ojb.broker.metadata.JdbcType;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.metadata.JdbcTypesHelper;

/**
 * This class is a concrete implementation of <code>Platform</code>. Provides
 * an implementation that works around some issues with Oracle running within WebLogic. As
 * WebLogic wraps the Oracle physical connection with its own logical connection it is necessary to
 * retrieve the underlying physical connection before creating a CLOB or BLOB.
 *
 * NOTE : When you use the physical connection WebLogic by default marks it as "infected" and discards it when
 * the logicical connection is closed.  You can change this behavior by setting the
 * RemoveInfectedConnectionsEnabled attribute on a connection pool.
 * see http://e-docs.bea.com/wls/docs81/jdbc/thirdparty.html#1043646
 *
 * Optimization: Oracle Batching (not standard JDBC batching)
 * see http://technet.oracle.com/products/oracle9i/daily/jun07.html
 *
 * Optimization: Oracle Prefetching
 * see http://otn.oracle.com/sample_code/tech/java/sqlj_jdbc/files/advanced/RowPrefetchSample/Readme.html
 *
 * TODO: Optimization: use ROWNUM to minimize the effects of not having server side cursors
 * see http://asktom.oracle.com/pls/ask/f?p=4950:8:::::F4950_P8_DISPLAYID:127412348064
 *
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 * @author <a href="mailto:erik@cj.com">Erik Forkalsrud</a>
 * @author <a href="mailto:martin.kalen@curalia.se">Martin Kal&eacute;n</a>
 * @author <a href="mailto:d441-6iq2@spamex.com">Dougall Squair</a>
 * @version CVS $Id: PlatformWLOracle9iImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 * @see Platform
 * @see PlatformDefaultImpl
 * @see PlatformOracleImpl
 * @see PlatformOracle9iImpl
 *
 * @deprecated since OJB 1.0.2 the default {@link PlatformOracle9iImpl} should be usable in WebLogic
 */
public class PlatformWLOracle9iImpl extends PlatformOracleImpl
{
    protected static final int ROW_PREFETCH_SIZE = 100;

    // From Oracle9i JDBC Developer's Guide and Reference:
    // "Batch values between 5 and 30 tend to be the most effective."
    protected static final int STATEMENTS_PER_BATCH = 20;
    protected static Map m_batchStatementsInProgress = Collections.synchronizedMap(new WeakHashMap(STATEMENTS_PER_BATCH));

    protected static final Class[] PARAM_TYPE_INTEGER = {Integer.TYPE};
    protected static final Class[] PARAM_TYPE_BOOLEAN = {Boolean.TYPE};
    protected static final Class[] PARAM_TYPE_STRING = {String.class};

    protected static final Object[] PARAM_ROW_PREFETCH_SIZE = new Object[]{new Integer(ROW_PREFETCH_SIZE)};
    protected static final Object[] PARAM_STATEMENT_BATCH_SIZE = new Object[]{new Integer(STATEMENTS_PER_BATCH)};
    protected static final Object[] PARAM_BOOLEAN_TRUE = new Object[]{Boolean.TRUE};

    protected static final JdbcType BASE_CLOB = JdbcTypesHelper.getJdbcTypeByName("clob");
    protected static final JdbcType BASE_BLOB = JdbcTypesHelper.getJdbcTypeByName("blob");



    /**
     * Enables Oracle row prefetching if supported.
     * See http://otn.oracle.com/sample_code/tech/java/sqlj_jdbc/files/advanced/RowPrefetchSample/Readme.html.
     * This is RDBMS server-to-client prefetching and thus one layer below
     * the OJB-internal prefetching-to-cache introduced in version 1.0rc5.
     * @param stmt the statement just created
     * @throws PlatformException upon JDBC failure
     */
    public void afterStatementCreate(java.sql.Statement stmt) throws PlatformException
    {
        super.afterStatementCreate(stmt);

        // Check for OracleStatement-specific row prefetching support
        final Method methodSetRowPrefetch;
        methodSetRowPrefetch = ClassHelper.getMethod(stmt, "setRowPrefetch", PARAM_TYPE_INTEGER);

        final boolean rowPrefetchingSupported = methodSetRowPrefetch != null;
        if (rowPrefetchingSupported)
        {
            try
            {
                // Set number of prefetched rows
                methodSetRowPrefetch.invoke(stmt, PARAM_ROW_PREFETCH_SIZE);
            }
            catch (Exception e)
            {
                throw new PlatformException(e.getLocalizedMessage(), e);
            }
        }
    }

    /**
     * Try Oracle update batching and call setExecuteBatch or revert to
     * JDBC update batching. See 12-2 Update Batching in the Oracle9i
     * JDBC Developer's Guide and Reference.
     * @param stmt the prepared statement to be used for batching
     * @throws PlatformException upon JDBC failure
     */
    public void beforeBatch(PreparedStatement stmt) throws PlatformException
    {
        // Check for Oracle batching support
        final Method methodSetExecuteBatch;
        final Method methodSendBatch;
        methodSetExecuteBatch = ClassHelper.getMethod(stmt, "setExecuteBatch", PARAM_TYPE_INTEGER);
        methodSendBatch = ClassHelper.getMethod(stmt, "sendBatch", null);

        final boolean statementBatchingSupported = methodSetExecuteBatch != null && methodSendBatch != null;
        if (statementBatchingSupported)
        {
            try
            {
                // Set number of statements per batch
                methodSetExecuteBatch.invoke(stmt, PARAM_STATEMENT_BATCH_SIZE);
                m_batchStatementsInProgress.put(stmt, methodSendBatch);
            }
            catch (Exception e)
            {
                throw new PlatformException(e.getLocalizedMessage(), e);
            }
        }
        else
        {
            super.beforeBatch(stmt);
        }
    }

    /**
     * Try Oracle update batching and call executeUpdate or revert to
     * JDBC update batching.
     * @param stmt the statement beeing added to the batch
     * @throws PlatformException upon JDBC failure
     */
    public void addBatch(PreparedStatement stmt) throws PlatformException
    {
        // Check for Oracle batching support
        final boolean statementBatchingSupported = m_batchStatementsInProgress.containsKey(stmt);
        if (statementBatchingSupported)
        {
            try
            {
                stmt.executeUpdate();
            }
            catch (SQLException e)
            {
                throw new PlatformException(e.getLocalizedMessage(), e);
            }
        }
        else
        {
            super.addBatch(stmt);
        }
    }

    /**
     * Try Oracle update batching and call sendBatch or revert to
     * JDBC update batching.
     * @param stmt the batched prepared statement about to be executed
     * @return always <code>null</code> if Oracle update batching is used,
     * since it is impossible to dissolve total row count into distinct
     * statement counts. If JDBC update batching is used, an int array is
     * returned containing number of updated rows for each batched statement.
     * @throws PlatformException upon JDBC failure
     */
    public int[] executeBatch(PreparedStatement stmt) throws PlatformException
    {
        // Check for Oracle batching support
        final Method methodSendBatch = (Method) m_batchStatementsInProgress.remove(stmt);
        final boolean statementBatchingSupported = methodSendBatch != null;

        int[] retval = null;
        if (statementBatchingSupported)
        {
            try
            {
                // sendBatch() returns total row count as an Integer
                methodSendBatch.invoke(stmt, null);
            }
            catch (Exception e)
            {
                throw new PlatformException(e.getLocalizedMessage(), e);
            }
        }
        else
        {
            retval = super.executeBatch(stmt);
        }
        return retval;
    }

    /** @see Platform#setObjectForStatement */
    public void setObjectForStatement(PreparedStatement ps, int index, Object value, int sqlType) throws SQLException
    {
        boolean blobHandlingSupported = false;
        boolean clobHandlingSupported = false;
        Method methodSetBlob = null;
        Method methodSetClob = null;
        Method methodGetVendorConnection = null;

        // Check for Oracle JDBC-driver LOB-support
        if (sqlType == Types.CLOB)
        {
            try
            {
                Class clobClass = ClassHelper.getClass("oracle.sql.CLOB", false);
                methodSetClob = ClassHelper.getMethod(ps, "setCLOB", new Class[]{Integer.TYPE, clobClass});
                methodGetVendorConnection = ClassHelper.getMethod(ps.getConnection(), "getVendorConnection",
                        new Class[]{});
                clobHandlingSupported = methodSetClob != null && methodGetVendorConnection != null;
            }
            catch (Exception ignore)
            {
                // ignore it
            }
        }
        else if (sqlType == Types.BLOB)
        {
            try
            {
                Class blobClass = ClassHelper.getClass("oracle.sql.BLOB", false);
                methodSetBlob = ClassHelper.getMethod(ps, "setBLOB", new Class[]{Integer.TYPE, blobClass});
                methodGetVendorConnection = ClassHelper.getMethod(ps.getConnection(), "getVendorConnection",
                        new Class[]{});
                blobHandlingSupported = methodSetBlob != null && methodGetVendorConnection != null;
            }
            catch (Exception ignore)
            {
                // ignore it
            }
        }

        // Type-specific Oracle conversions
        if (((sqlType == Types.VARBINARY) || (sqlType == Types.LONGVARBINARY)) && (value instanceof byte[]))
        {
            byte buf[] = (byte[]) value;
            ByteArrayInputStream inputStream = new ByteArrayInputStream(buf);
            super.changePreparedStatementResultSetType(ps);
            ps.setBinaryStream(index, inputStream, buf.length);
        }
        else if (value instanceof Double)
        {
            // workaround for the bug in Oracle thin driver
            ps.setDouble(index, ((Double) value).doubleValue());
        }
        else if (sqlType == Types.BIGINT && value instanceof Integer)
        {
            // workaround: Oracle thin driver problem when expecting long
            ps.setLong(index, ((Integer) value).intValue());
        }
        else if (sqlType == Types.INTEGER && value instanceof Long)
        {
            ps.setLong(index, ((Long) value).longValue());
        }
        else if (sqlType == Types.CLOB && clobHandlingSupported && value instanceof String)
        {
            // TODO: If using Oracle update batching with the thin driver, throw exception on 4k limit
            try
            {
                Connection vendorConnection = (Connection) methodGetVendorConnection.invoke(ps.getConnection(),
                        new Object[]{});
                Object clob = Oracle9iLobHandler.createCLOBFromString(vendorConnection, (String) value);
                methodSetClob.invoke(ps, new Object[]{new Integer(index), clob});
            }
            catch (Exception e)
            {
                throw new SQLException(e.getLocalizedMessage());
            }
        }
        else if (sqlType == Types.BLOB && blobHandlingSupported && value instanceof byte[])
        {
            // TODO: If using Oracle update batching with the thin driver, throw exception on 2k limit
            try
            {
                Connection vendorConnection = (Connection) methodGetVendorConnection.invoke(ps.getConnection(),
                        new Object[]{});
                Object blob = Oracle9iLobHandler.createBLOBFromByteArray(vendorConnection, (byte[]) value);
                methodSetBlob.invoke(ps, new Object[]{new Integer(index), blob});
            }
            catch (Exception e)
            {
                throw new SQLException(e.getLocalizedMessage());
            }
        }
        else
        {
            // Fall-through to superclass
            super.setObjectForStatement(ps, index, value, sqlType);
        }
    }

}
