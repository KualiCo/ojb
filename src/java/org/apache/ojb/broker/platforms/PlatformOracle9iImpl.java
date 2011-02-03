package org.apache.ojb.broker.platforms;

/* Copyright 2003-2005 The Apache Software Foundation
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
import org.apache.ojb.broker.metadata.ConnectionPoolDescriptor;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This class is a concrete implementation of <code>Platform</code>. Provides
 * an implementation that works around some issues with Oracle in general and
 * Oracle 9i's Thin driver in particular.
 *
 * NOTE: When using BEA WebLogic and BLOB/CLOB datatypes, the physical connection will be
 * used causing WebLogic to mark it as "infected" and discard it when
 * the logicical connection is closed. You can change this behavior by setting the
 * RemoveInfectedConnectionsEnabled attribute on a connection pool.
 * see <a href="http://e-docs.bea.com/wls/docs81/jdbc/thirdparty.html#1043646">WebLogic docs</a>.
 *
 * Optimization: Oracle Batching (not standard JDBC batching)
 * see http://technet.oracle.com/products/oracle9i/daily/jun07.html
 *
 * Optimization: Oracle Prefetching
 * see http://otn.oracle.com/sample_code/tech/java/sqlj_jdbc/files/advanced/RowPrefetchSample/Readme.html
 *
 * Optimization: Oracle Statement Caching
 * see http://otn.oracle.com/sample_code/tech/java/sqlj_jdbc/files/jdbc30/StmtCacheSample/Readme.html
 *
 * TODO: Optimization: use ROWNUM to minimize the effects of not having server side cursors
 * see http://asktom.oracle.com/pls/ask/f?p=4950:8:::::F4950_P8_DISPLAYID:127412348064
 *
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 * @author <a href="mailto:mkalen@apache.org">Martin Kal&eacute;n</a>
 * @author Contributions from: Erik Forkalsrud, Danilo Tommasina, Thierry Hanot, Don Lyon
 * @version CVS $Id: PlatformOracle9iImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 * @see Platform
 * @see PlatformDefaultImpl
 * @see PlatformOracleImpl
 */
public class PlatformOracle9iImpl extends PlatformOracleImpl
{
    private Logger logger = LoggerFactory.getLogger(PlatformOracle9iImpl.class);

    /**
     * Number of cached statements per connection,
     * when using implicit caching with OracleConnections.
     * Set in {@link #initializeJdbcConnection}.
     * @see <a href="http://www.apache.org/~mkalen/ojb/broker-tests.html">Profiling page</a>
     * for a discussion re sizing
     */
    protected static final int STATEMENT_CACHE_SIZE = 10;
    /**
     * Number of rows pre-fetched by the JDBC-driver for each executed query,
     * when using Oracle row pre-fetching with OracleConnections.
     * Set in {@link #initializeJdbcConnection}.
     * <p>
     * <em>Note</em>: this setting can be overridden by specifying a
     * connection-pool attribute with name="jdbc.defaultRowPrefetch".
     * Oracle JDBC-driver default value=10.
     */
    protected static final int ROW_PREFETCH_SIZE = 20;

    // From Oracle9i JDBC Developer's Guide and Reference:
    // "Batch values between 5 and 30 tend to be the most effective."
    protected static final int STATEMENTS_PER_BATCH = 20;
    protected static Map m_batchStatementsInProgress = Collections.synchronizedMap(new WeakHashMap(STATEMENTS_PER_BATCH));

    protected static final Class[] PARAM_TYPE_EMPTY = {};
    protected static final Class[] PARAM_TYPE_INTEGER = {Integer.TYPE};
    protected static final Class[] PARAM_TYPE_BOOLEAN = {Boolean.TYPE};
    protected static final Class[] PARAM_TYPE_STRING = {String.class};

    protected static final Object[] PARAM_EMPTY = new Object[]{};
    protected static final Object[] PARAM_STATEMENT_CACHE_SIZE = new Object[]{new Integer(STATEMENT_CACHE_SIZE)};
    protected static final Object[] PARAM_ROW_PREFETCH_SIZE = new Object[]{new Integer(ROW_PREFETCH_SIZE)};
    protected static final Object[] PARAM_STATEMENT_BATCH_SIZE = new Object[]{new Integer(STATEMENTS_PER_BATCH)};
    protected static final Object[] PARAM_BOOLEAN_TRUE = new Object[]{Boolean.TRUE};

    protected static final String JBOSS_CONN_NAME =
            "org.jboss.resource.adapter.jdbc.WrappedConnection";
    protected static Class JBOSS_CONN_CLASS = null;

    protected static Class ORA_CONN_CLASS;
    protected static Class ORA_PS_CLASS;
    protected static Class ORA_CLOB_CLASS;
    protected static Class ORA_BLOB_CLASS;
    protected static Class[] PARAM_TYPE_INT_ORACLOB;
    protected static Class[] PARAM_TYPE_INT_ORABLOB;
    protected static Method METHOD_SET_STATEMENT_CACHE_SIZE;
    protected static Method METHOD_SET_IMPLICIT_CACHING_ENABLED;
    protected static Method METHOD_SET_ROW_PREFETCH;
    protected static Method METHOD_SET_BLOB = null;
    protected static Method METHOD_SET_CLOB = null;
    protected static boolean ORA_STATEMENT_CACHING_AVAILABLE;
    protected static boolean ORA_ROW_PREFETCH_AVAILABLE;
    protected static boolean ORA_CLOB_HANDLING_AVAILABLE;
    protected static boolean ORA_BLOB_HANDLING_AVAILABLE;

    /** Method names used by {@link #unwrapConnection}. */
    protected static final String UNWRAP_CONN_METHOD_NAMES[] =
            {
                "unwrapCompletely"          /* Oracle 10g */,
                "getInnermostDelegate"      /* Commons DBCP */,
                "getUnderlyingConnection"   /* JBoss */,
                "getVendorConnection"       /* BEA WebLogic */,
                "getJDBC"                   /* P6Spy */
            };
    /**
     * Method parameter signature used by {@link #unwrapConnection} for corresponding
     * {@link #UNWRAP_CONN_METHOD_NAMES}-index.
     * If signature is not {@link #PARAM_TYPE_EMPTY}, the actual connection object
     * will be passed at runtime. (NB: Requires special handling of param type in constructor.)
     */
    protected static final Class[][] UNWRAP_CONN_PARAM_TYPES =
            {
                null  /* Index 0 reserved for Oracle 10g - initialized in constructor */,
                PARAM_TYPE_EMPTY            /* Commons DBCP */,
                PARAM_TYPE_EMPTY            /* JBoss */,
                PARAM_TYPE_EMPTY            /* BEA WebLogic */,
                PARAM_TYPE_EMPTY            /* P6Spy */
            };
    /** Method names used by {@link #unwrapStatement}. */
    protected static final String UNWRAP_PS_METHOD_NAMES[] =
            {
                "getInnermostDelegate"      /* Commons DBCP */,
                "getUnderlyingStatement"    /* JBoss */,
                "getJDBC"                   /* P6Spy */,
                "ps"						/* XAPool, PATCHED: also genericUnwrap modified to unwrap fields */
            };
    /**
     * Method parameter signature used by {@link #unwrapStatement} for corresponding
     * {@link #UNWRAP_PS_METHOD_NAMES}-index.
     * If signature is not {@link #PARAM_TYPE_EMPTY}, the actual Statement object
     * will be passed at runtime. (NB: Requires special handling of param type in constructor.)
     */
    protected static final Class[][] UNWRAP_PS_PARAM_TYPES =
            {
                PARAM_TYPE_EMPTY            /* Commons DBCP */,
                PARAM_TYPE_EMPTY            /* JBoss */,
                PARAM_TYPE_EMPTY            /* P6Spy */,
                PARAM_TYPE_EMPTY			/* XAPool */
            };


    /**
     * Default constructor.
     */
    public PlatformOracle9iImpl()
    {
        super();
    }

    /**
     * Enables Oracle statement caching and row prefetching if supported by the JDBC-driver.
     * @param jcd the OJB <code>JdbcConnectionDescriptor</code> (metadata) for the connection to be initialized
     * @param conn the <code>Connection</code>-object (physical) to be initialized
     * @see PlatformDefaultImpl#initializeJdbcConnection
     * @see <a href="http://otn.oracle.com/sample_code/tech/java/sqlj_jdbc/files/jdbc30/StmtCacheSample/Readme.html">
     * Oracle TechNet Statement Caching Sample</a>
     * @see <a href="http://otn.oracle.com/sample_code/tech/java/sqlj_jdbc/files/advanced/RowPrefetchSample/Readme.html">
     * Oracle TechNet Row Pre-fetch Sample<a>
     */
    public void initializeJdbcConnection(final JdbcConnectionDescriptor jcd,
                                         final Connection conn)
            throws PlatformException
    {
        // Do all the generic initialization in PlatformDefaultImpl first
        super.initializeJdbcConnection(jcd, conn);

        // Check for managed environments known to reject Oracle extension at this level
        // (saves us from trying to unwrap just to catch exceptions next)
        final Class connClass = conn.getClass();
        if (JBOSS_CONN_CLASS != null && JBOSS_CONN_CLASS.isAssignableFrom(connClass))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("JBoss detected, Oracle Connection tuning left to J2EE container.");
            }
            return;
        }

        // Check if this is a wrapped connection and if so unwrap it
        final Connection oraConn = unwrapConnection(conn);
        if (oraConn == null)
        {
            return;
        }

        // At this point we know that we have an OracleConnection instance and can thus
        // try to invoke methods via reflection (if available)
        if (ORA_STATEMENT_CACHING_AVAILABLE)
        {
            try
            {
                // Set number of cached statements and enable implicit caching
                METHOD_SET_STATEMENT_CACHE_SIZE.invoke(oraConn, PARAM_STATEMENT_CACHE_SIZE);
                METHOD_SET_IMPLICIT_CACHING_ENABLED.invoke(oraConn, PARAM_BOOLEAN_TRUE);
            }
            catch (Exception e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("PlatformOracle9iImpl could not enable Oracle statement caching."
                                 + " Original/unwrapped connection classes="
                                 + connClass.getName() + "/" + oraConn.getClass().getName());
                }
            }
        }

        /*
        mkalen: Note from the Oracle documentation:
            Do not mix the JDBC 2.0 fetch size API and the Oracle row prefetching API
            in your application. You can use one or the other, but not both.
        */
        final ConnectionPoolDescriptor cpd = jcd.getConnectionPoolDescriptor();
        final int cpdFetchSizeHint = cpd.getFetchSize();
        if (cpdFetchSizeHint == 0 && ORA_ROW_PREFETCH_AVAILABLE)
        {
            try
            {
                final String prefetchFromJcd;
                prefetchFromJcd = cpd.getJdbcProperties().getProperty("defaultRowPrefetch");
                if (prefetchFromJcd == null)
                {
                    METHOD_SET_ROW_PREFETCH.invoke(oraConn, PARAM_ROW_PREFETCH_SIZE);
                }
                // Else, number of prefetched rows were set via Properties on Connection
            }
            catch (Exception e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("PlatformOracle9iImpl could not enable Oracle row pre-fetching."
                                 + "Original/unwrapped connection classes="
                                 + connClass.getName() + "/" + oraConn.getClass().getName());
                }
            }
        }
    }

    /**
     * Performs platform-specific operations on each statement.
     * @param stmt the statement just created
     */
    public void afterStatementCreate(Statement stmt)
    {
        // mkalen:  do NOT call super#afterStatementCreate since escape processing for SQL92
        //          syntax is enabled by default for Oracle9i and higher, and explicit calls
        //          to setEscapeProcessing for PreparedStatements will make Oracle 10g JDBC-
        //          driver throw exceptions (and is functionally useless).
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
        // Check for Oracle JDBC-driver LOB-support
        final Statement oraStmt;
        final Connection oraConn;
        final boolean oraLargeLobSupportAvailable;
        if (sqlType == Types.CLOB || sqlType == Types.BLOB)
        {
            oraStmt = unwrapStatement(ps);
            oraConn = unwrapConnection(ps.getConnection());
            oraLargeLobSupportAvailable =
                    oraStmt != null && oraConn != null &&
                    (sqlType == Types.CLOB ? ORA_CLOB_HANDLING_AVAILABLE : ORA_BLOB_HANDLING_AVAILABLE);
        }
        else
        {
            oraStmt = null;
            oraConn = null;
            oraLargeLobSupportAvailable = false;
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
        else if (sqlType == Types.CLOB && oraLargeLobSupportAvailable && value instanceof String)
        {
            // TODO: If using Oracle update batching with the thin driver, throw exception on 4k limit
            try
            {
                Object clob = Oracle9iLobHandler.createCLOBFromString(oraConn, (String) value);
                METHOD_SET_CLOB.invoke(oraStmt, new Object[]{new Integer(index), clob});
            }
            catch (Exception e)
            {
                throw new SQLException(e.getLocalizedMessage());
            }
        }
        else if (sqlType == Types.BLOB && oraLargeLobSupportAvailable && value instanceof byte[])
        {
            // TODO: If using Oracle update batching with the thin driver, throw exception on 2k limit
            try
            {
                Object blob = Oracle9iLobHandler.createBLOBFromByteArray(oraConn, (byte[]) value);
                METHOD_SET_BLOB.invoke(oraStmt, new Object[]{new Integer(index), blob});
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

    /**
     * Get join syntax type for this RDBMS.
     *
     * @return SQL92_NOPAREN_JOIN_SYNTAX
     */
    public byte getJoinSyntaxType()
    {
        return SQL92_NOPAREN_JOIN_SYNTAX;
    }

    /**
     * Return an OracleConnection after trying to unwrap from known Connection wrappers.
     * @param conn the connection to unwrap (if needed)
     * @return OracleConnection or null if not able to unwrap
     */
    protected Connection unwrapConnection(Connection conn)
    {
        Object unwrapped = genericUnwrap(ORA_CONN_CLASS, conn, UNWRAP_CONN_METHOD_NAMES, UNWRAP_CONN_PARAM_TYPES);
        try {
    		// We haven't received a different Connection, so we'll assume that there's
    		// some additional proxying going on. Let's check whether we get something
    		// different back from the DatabaseMetaData.getConnection() call.
    		DatabaseMetaData metaData = conn.getMetaData();
    		// The following check is only really there for mock Connections
    		// which might not carry a DatabaseMetaData instance.
    		if (metaData != null) {
    			Connection metaCon = metaData.getConnection();
    			if (metaCon != conn) {
    				// We've received a different Connection there:
    				// Let's retry the native extraction process with it.
    				unwrapped = genericUnwrap(ORA_CONN_CLASS, metaCon, UNWRAP_CONN_METHOD_NAMES, UNWRAP_CONN_PARAM_TYPES);
    			}
    		}
    	} catch (SQLException e) {
    		if (logger.isDebugEnabled())
            {
                logger.debug("Failed attempting to unwrap connection via database metadata.", e);
            }
    	}
        if (unwrapped == null)
        {
            // mkalen:  only log this as debug since it will be logged for every connection
            //          (ie only useful during development).
            if (logger.isDebugEnabled())
            {
                logger.debug("PlatformOracle9iImpl could not unwrap " + conn.getClass().getName() +
                             ", Oracle-extensions disabled.");
            }
        }
        return (Connection) unwrapped;
    }

    /**
     * Return an OraclePreparedStatement after trying to unwrap from known Statement wrappers.
     * @param ps the PreparedStatement to unwrap (if needed)
     * @return OraclePreparedStatement or null if not able to unwrap
     */
    protected Statement unwrapStatement(Statement ps)
    {
        final Object unwrapped;
        unwrapped = genericUnwrap(ORA_PS_CLASS, ps, UNWRAP_PS_METHOD_NAMES, UNWRAP_PS_PARAM_TYPES);
        if (unwrapped == null)
        {
            // mkalen:  only log this as debug since it will be logged for every connection
            //          (ie only useful during development).
            if (logger.isDebugEnabled())
            {
                logger.debug("PlatformOracle9iImpl could not unwrap " + ps.getClass().getName() +
                             ", large CLOB/BLOB support disabled.");
            }
        }
        return (Statement) unwrapped;
    }

    protected Object genericUnwrap(Class classToMatch, Object toUnwrap,
                                   String[] methodNameCandidates,
                                   Class[][] methodTypeCandidates)
    {
        if (classToMatch == null)
        {
            return null;
        }

        Object unwrapped = null;
        final Class psClass = toUnwrap.getClass();
        if (classToMatch.isAssignableFrom(psClass))
        {
            return toUnwrap;
        }
        try
        {
            String methodName;
            Class[] paramTypes;
            Object[] args;
            for (int i = 0; i < methodNameCandidates.length; i++)
            {
                methodName = methodNameCandidates[i];
                paramTypes = methodTypeCandidates[i];
                final Method method = ClassHelper.getMethod(toUnwrap, methodName, paramTypes);
                if (method != null)
                {
                    args = paramTypes == PARAM_TYPE_EMPTY ? PARAM_EMPTY : new Object[]{ toUnwrap };
                    unwrapped = method.invoke(toUnwrap, args);
				} else {
					// Check for a field:
					// PACTHED: This next section was added as a patch to allow for XAPool's org.enhydra.jdbc.core.CorePreparedStatement
					// which externalizes it's underlying prepared statement as a public field called 'ps'
					final Field field = ClassHelper.getField(psClass, methodName);
					if (field != null) {
						unwrapped = field.get(toUnwrap);
					}
				}
                if (unwrapped != null)
                {
                    if (classToMatch.isAssignableFrom(unwrapped.getClass()))
                    {
                        return unwrapped;
                    }
                    // When using eg both DBCP and P6Spy we have to recursively unwrap
                    return genericUnwrap(classToMatch, unwrapped,
                            methodNameCandidates, methodTypeCandidates);
                }
            }
            
            // PATCHED: Try to use JDBC 4 unwrap mechanism
            final Method jdbcUnwrapMethod = ClassHelper.getMethod(toUnwrap, "unwrap", new Class[] { Class.class });

            if (jdbcUnwrapMethod != null && !Modifier.isAbstract(jdbcUnwrapMethod.getModifiers())) {
            	Class jdbcClassToMatch = null;  // we're going to find 
            	if (java.sql.Connection.class.isAssignableFrom(classToMatch)) {
            		jdbcClassToMatch = java.sql.Connection.class;
            	} else if (java.sql.PreparedStatement.class.isAssignableFrom(classToMatch)) {
            		jdbcClassToMatch = java.sql.PreparedStatement.class;
            	} else if (java.sql.Statement.class.isAssignableFrom(classToMatch)) {
            		jdbcClassToMatch = java.sql.Statement.class;
            	}

            	Object result = null;
            	if (jdbcClassToMatch != null) {
            		result = jdbcUnwrapMethod.invoke(toUnwrap, jdbcClassToMatch);
            		if (result != null) {
            			// Sometimes we actually get a proxy back
            			if (classToMatch.isAssignableFrom(result.getClass()))
            			{
            				return result;
            			}
            			// When using eg both DBCP and P6Spy we have to recursively unwrap
            			return genericUnwrap(classToMatch, result,
            					methodNameCandidates, methodTypeCandidates);
            		}
            	}
            } // didn't work, fall back.
        }
        catch (Exception e)
        {
            // ignore
            if (logger.isDebugEnabled())
            {
                logger.debug("genericUnwrap failed", e);
            }
        }
        return null;
    }

    /**
     * Initializes static variables needed for Oracle-extensions and large BLOB/CLOB support.
     */
    protected void initOracleReflectedVars()
    {
        super.initOracleReflectedVars();
        try
        {
            /*
            Check for Oracle-specific classes, OracleConnection-specific
            statement caching/row pre-fetch methods and Oracle BLOB/CLOB access methods.
            We can do this in constructor in spite of possible mixing of instance being
            able vs unable passed at runtime (since withouth these classes and methods
            it's impossible to enable ORA-extensions at all even if instances are capable).
            */
            ORA_CONN_CLASS = ClassHelper.getClass("oracle.jdbc.OracleConnection", false);
            ORA_PS_CLASS = ClassHelper.getClass("oracle.jdbc.OraclePreparedStatement", false);
            ORA_CLOB_CLASS = ClassHelper.getClass("oracle.sql.CLOB", false);
            ORA_BLOB_CLASS = ClassHelper.getClass("oracle.sql.BLOB", false);
            PARAM_TYPE_INT_ORACLOB = new Class[]{ Integer.TYPE, ORA_CLOB_CLASS };
            PARAM_TYPE_INT_ORABLOB = new Class[]{ Integer.TYPE, ORA_BLOB_CLASS };

            // Index 0 reserved for Oracle 10g
            UNWRAP_CONN_PARAM_TYPES[0] = new Class[]{ ORA_CONN_CLASS };

            METHOD_SET_STATEMENT_CACHE_SIZE =
                    ClassHelper.getMethod(ORA_CONN_CLASS, "setStatementCacheSize", PARAM_TYPE_INTEGER);
            METHOD_SET_IMPLICIT_CACHING_ENABLED =
                    ClassHelper.getMethod(ORA_CONN_CLASS, "setImplicitCachingEnabled", PARAM_TYPE_BOOLEAN);
            METHOD_SET_ROW_PREFETCH = ClassHelper.getMethod(ORA_CONN_CLASS, "setDefaultRowPrefetch", PARAM_TYPE_INTEGER);
            METHOD_SET_CLOB = ClassHelper.getMethod(ORA_PS_CLASS, "setCLOB", PARAM_TYPE_INT_ORACLOB);
            METHOD_SET_BLOB = ClassHelper.getMethod(ORA_PS_CLASS, "setBLOB", PARAM_TYPE_INT_ORABLOB);

            ORA_STATEMENT_CACHING_AVAILABLE =
                    METHOD_SET_STATEMENT_CACHE_SIZE != null && METHOD_SET_IMPLICIT_CACHING_ENABLED != null;
            ORA_ROW_PREFETCH_AVAILABLE = METHOD_SET_ROW_PREFETCH != null;
            ORA_CLOB_HANDLING_AVAILABLE = METHOD_SET_CLOB != null;
            ORA_BLOB_HANDLING_AVAILABLE = METHOD_SET_BLOB != null;
        }
        catch (ClassNotFoundException e)
        {
            // ignore (we tried...)
        }
        // Isolated checks for other connection classes (OK when not found)
        try
        {
            JBOSS_CONN_CLASS = ClassHelper.getClass(JBOSS_CONN_NAME, false);
        }
        catch (ClassNotFoundException e)
        {
            // ignore (no problem)
        }
    }

}
