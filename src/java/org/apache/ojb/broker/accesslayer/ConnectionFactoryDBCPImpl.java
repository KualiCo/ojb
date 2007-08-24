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

import org.apache.commons.dbcp.AbandonedConfig;
import org.apache.commons.dbcp.AbandonedObjectPool;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.logging.LoggerWrapperPrintWriter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * ConnectionFactory implementation using Commons DBCP and Commons Pool API
 * to pool connections.
 *
 * Based on a proposal of Dirk Verbeek - Thanks.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ConnectionFactoryDBCPImpl.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 * @see <a href="http://jakarta.apache.org/commons/pool/">Commons Pool Website</a>
 * @see <a href="http://jakarta.apache.org/commons/dbcp/">Commons DBCP Website</a>
 */
public class ConnectionFactoryDBCPImpl extends ConnectionFactoryAbstractImpl
{

    public static final String PARAM_NAME_UNWRAP_ALLOWED = "accessToUnderlyingConnectionAllowed";
    public static final String PARAM_NAME_POOL_STATEMENTS = "poolPreparedStatements";
    public static final String PARAM_NAME_STATEMENT_POOL_MAX_TOTAL = "maxOpenPreparedStatements";

    private Logger log = LoggerFactory.getLogger(ConnectionFactoryDBCPImpl.class);

    /** Key=PBKey, value=ObjectPool. */
    private Map poolMap = Collections.synchronizedMap(new HashMap());
    /** Key=PBKey, value=PoolingDataSource. */
    private Map dsMap = Collections.synchronizedMap(new HashMap());
    /** Synchronize object for operations not synchronized on Map only. */
    private final Object poolSynch = new Object();

    public Connection checkOutJdbcConnection(JdbcConnectionDescriptor jcd) throws LookupException
    {
        final DataSource ds = getDataSource(jcd);

        // Returned DS is never null, exception are logged by getDataSource and gets
        // re-thrown here since we don't catch them

        Connection conn;
        try
        {
            conn = ds.getConnection();
        }
        catch (SQLException e)
        {
            throw new LookupException("Could not get connection from DBCP DataSource", e);
        }
        return conn;
    }

    public void releaseJdbcConnection(JdbcConnectionDescriptor jcd, Connection con)
            throws LookupException
    {
        try
        {
            // We are using datasources, thus close returns connection to pool
            con.close();
        }
        catch (SQLException e)
        {
            log.warn("Connection close failed", e);
        }
    }

    /**
     * Closes all managed pools.
     */
    public void releaseAllResources()
    {
        super.releaseAllResources();
        synchronized (poolSynch)
        {
            if (!poolMap.isEmpty())
            {
                Collection pools = poolMap.values();
                Iterator iterator = pools.iterator();
                ObjectPool op = null;
                while (iterator.hasNext())
                {
                    try
                    {
                        op = (ObjectPool) iterator.next();
                        op.close();
                    }
                    catch (Exception e)
                    {
                        log.error("Exception occured while closing ObjectPool " + op, e);
                    }
                }
                poolMap.clear();
            }
            dsMap.clear();
        }
    }

    /**
     * Returns the DBCP DataSource for the specified connection descriptor,
     * after creating a new DataSource if needed.
     * @param jcd the descriptor for which to return a DataSource
     * @return a DataSource, after creating a new pool if needed.
     * Guaranteed to never be null.
     * @throws LookupException if pool is not in cache and cannot be created
     */
    protected DataSource getDataSource(JdbcConnectionDescriptor jcd)
            throws LookupException
    {
        final PBKey key = jcd.getPBKey();
        DataSource ds = (DataSource) dsMap.get(key);
        if (ds == null)
        {
            // Found no pool for PBKey
            try
            {
                synchronized (poolSynch)
                {
                    // Setup new object pool
                    ObjectPool pool = setupPool(jcd);
                    poolMap.put(key, pool);
                    // Wrap the underlying object pool as DataSource
                    ds = wrapAsDataSource(jcd, pool);
                    dsMap.put(key, ds);
                }
            }
            catch (Exception e)
            {
                log.error("Could not setup DBCP DataSource for " + jcd, e);
                throw new LookupException(e);
            }
        }
        return ds;
    }

    /**
     * Returns a new ObjectPool for the specified connection descriptor.
     * Override this method to setup your own pool.
     * @param jcd the connection descriptor for which to set up the pool
     * @return a newly created object pool
     */
    protected ObjectPool setupPool(JdbcConnectionDescriptor jcd)
    {
        log.info("Create new ObjectPool for DBCP connections:" + jcd);

        try
        {
            ClassHelper.newInstance(jcd.getDriver());
        }
        catch (InstantiationException e)
        {
            log.fatal("Unable to instantiate the driver class: " + jcd.getDriver() + " in ConnectionFactoryDBCImpl!" , e);
        }
        catch (IllegalAccessException e)
        {
            log.fatal("IllegalAccessException while instantiating the driver class: " + jcd.getDriver() + " in ConnectionFactoryDBCImpl!" , e);
        }
        catch (ClassNotFoundException e)
        {
            log.fatal("Could not find the driver class : " + jcd.getDriver() + " in ConnectionFactoryDBCImpl!" , e);
        }

        // Get the configuration for the connection pool
        GenericObjectPool.Config conf = jcd.getConnectionPoolDescriptor().getObjectPoolConfig();

        // Get the additional abandoned configuration
        AbandonedConfig ac = jcd.getConnectionPoolDescriptor().getAbandonedConfig();

        // Create the ObjectPool that serves as the actual pool of connections.
        final ObjectPool connectionPool = createConnectionPool(conf, ac);

        // Create a DriverManager-based ConnectionFactory that
        // the connectionPool will use to create Connection instances
        final org.apache.commons.dbcp.ConnectionFactory connectionFactory;
        connectionFactory = createConnectionFactory(jcd);

        // Create PreparedStatement object pool (if any)
        KeyedObjectPoolFactory statementPoolFactory = createStatementPoolFactory(jcd);

        // Set validation query and auto-commit mode
        final String validationQuery;
        final boolean defaultAutoCommit;
        final boolean defaultReadOnly = false;
        validationQuery = jcd.getConnectionPoolDescriptor().getValidationQuery();
        defaultAutoCommit = (jcd.getUseAutoCommit() != JdbcConnectionDescriptor.AUTO_COMMIT_SET_FALSE);

        //
        // Now we'll create the PoolableConnectionFactory, which wraps
        // the "real" Connections created by the ConnectionFactory with
        // the classes that implement the pooling functionality.
        //
        final PoolableConnectionFactory poolableConnectionFactory;
        poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,
                connectionPool,
                statementPoolFactory,
                validationQuery,
                defaultReadOnly,
                defaultAutoCommit,
                ac);
        return poolableConnectionFactory.getPool();
    }

    protected ObjectPool createConnectionPool(GenericObjectPool.Config config,
                                              AbandonedConfig ac)
    {
        final GenericObjectPool connectionPool;
        final boolean doRemoveAbandoned = ac != null && ac.getRemoveAbandoned();

        if (doRemoveAbandoned) {
            connectionPool = new AbandonedObjectPool(null, ac);
        } else {
            connectionPool = new GenericObjectPool();
        }
        connectionPool.setMaxActive(config.maxActive);
        connectionPool.setMaxIdle(config.maxIdle);
        connectionPool.setMinIdle(config.minIdle);
        connectionPool.setMaxWait(config.maxWait);
        connectionPool.setTestOnBorrow(config.testOnBorrow);
        connectionPool.setTestOnReturn(config.testOnReturn);
        connectionPool.setTimeBetweenEvictionRunsMillis(config.timeBetweenEvictionRunsMillis);
        connectionPool.setNumTestsPerEvictionRun(config.numTestsPerEvictionRun);
        connectionPool.setMinEvictableIdleTimeMillis(config.minEvictableIdleTimeMillis);
        connectionPool.setTestWhileIdle(config.testWhileIdle);
        return connectionPool;
    }

    protected KeyedObjectPoolFactory createStatementPoolFactory(JdbcConnectionDescriptor jcd)
    {
        final String platform = jcd.getDbms();
        if (platform.startsWith("Oracle9i"))
        {
            // mkalen: let the platform set Oracle-specific statement pooling
            return null;
        }

        // Set up statement pool, if desired
        GenericKeyedObjectPoolFactory statementPoolFactory = null;
        final Properties properties = jcd.getConnectionPoolDescriptor().getDbcpProperties();
        final String poolStmtParam = properties.getProperty(PARAM_NAME_POOL_STATEMENTS);
        if (poolStmtParam != null && Boolean.valueOf(poolStmtParam).booleanValue())
        {
            int maxOpenPreparedStatements = GenericKeyedObjectPool.DEFAULT_MAX_TOTAL;
            final String maxOpenPrepStmtString = properties.getProperty(PARAM_NAME_STATEMENT_POOL_MAX_TOTAL);
            if (maxOpenPrepStmtString != null)
            {
                maxOpenPreparedStatements = Integer.parseInt(maxOpenPrepStmtString);
            }
            // Use the same values as Commons DBCP BasicDataSource
            statementPoolFactory = new GenericKeyedObjectPoolFactory(null,
                        -1, // unlimited maxActive (per key)
                        GenericKeyedObjectPool.WHEN_EXHAUSTED_FAIL,
                        0, // maxWait
                        1, // maxIdle (per key)
                        maxOpenPreparedStatements);
        }
        return statementPoolFactory;
    }

    /**
     * Wraps the specified object pool for connections as a DataSource.
     *
     * @param jcd the OJB connection descriptor for the pool to be wrapped
     * @param connectionPool the connection pool to be wrapped
     * @return a DataSource attached to the connection pool.
     * Connections will be wrapped using DBCP PoolGuard, that will not allow
     * unwrapping unless the "accessToUnderlyingConnectionAllowed=true" configuration
     * is specified.
     */
    protected DataSource wrapAsDataSource(JdbcConnectionDescriptor jcd,
                                          ObjectPool connectionPool)
    {
        final boolean allowConnectionUnwrap;
        if (jcd == null)
        {
            allowConnectionUnwrap = false;
        }
        else
        {
            final Properties properties = jcd.getConnectionPoolDescriptor().getDbcpProperties();
            final String allowConnectionUnwrapParam;
            allowConnectionUnwrapParam = properties.getProperty(PARAM_NAME_UNWRAP_ALLOWED);
            allowConnectionUnwrap = allowConnectionUnwrapParam != null &&
                    Boolean.valueOf(allowConnectionUnwrapParam).booleanValue();
        }
        final PoolingDataSource dataSource;
        dataSource = new PoolingDataSource(connectionPool);
        dataSource.setAccessToUnderlyingConnectionAllowed(allowConnectionUnwrap);

        if(jcd != null)
        {
            final AbandonedConfig ac = jcd.getConnectionPoolDescriptor().getAbandonedConfig();
            if (ac.getRemoveAbandoned() && ac.getLogAbandoned()) {
                final LoggerWrapperPrintWriter loggerPiggyBack;
                loggerPiggyBack = new LoggerWrapperPrintWriter(log, Logger.ERROR);
                dataSource.setLogWriter(loggerPiggyBack);
            }
        }
        return dataSource;
    }

    /**
     * Creates a DriverManager-based ConnectionFactory for creating the Connection
     * instances to feed into the object pool of the specified jcd-alias.
     * <p>
     * <b>NB!</b> If you override this method to specify your own ConnectionFactory
     * you <em>must</em> make sure that you follow OJB's lifecycle contract defined in the
     * {@link org.apache.ojb.broker.platforms.Platform} API - ie that you call
     * initializeJdbcConnection when a new Connection is created. For convenience, use
     * {@link ConnectionFactoryAbstractImpl#initializeJdbcConnection} instead of Platform call.
     * <p>
     * The above is automatically true if you re-use the inner class {@link ConPoolFactory}
     * below and just override this method for additional user-defined "tweaks".
     *
     * @param jcd the jdbc-connection-alias for which we are creating a ConnectionFactory
     * @return a DriverManager-based ConnectionFactory that creates Connection instances
     * using DriverManager, and that follows the lifecycle contract defined in OJB
     * {@link org.apache.ojb.broker.platforms.Platform} API.
     */
    protected org.apache.commons.dbcp.ConnectionFactory createConnectionFactory(JdbcConnectionDescriptor jcd)
    {
        final ConPoolFactory result;
        final Properties properties = getJdbcProperties(jcd);
        result = new ConPoolFactory(jcd, properties);
        return result;
    }

    // ----- deprecated methods, to be removed -----

    /**
     * mkalen: Left for binary API-compatibility with OJB 1.0.3 (don't break users' factories)
     * @deprecated since OJB 1.0.4,
     * please use {@link #createConnectionPool(org.apache.commons.pool.impl.GenericObjectPool.Config, org.apache.commons.dbcp.AbandonedConfig)}
     */
    protected ObjectPool createObjectPool(GenericObjectPool.Config config)
    {
        return createConnectionPool(config, null);
    }

    /**
     * mkalen: Left for binary API-compatibility with OJB 1.0.3 (don't break users' factories)
     * @deprecated since OJB 1.0.4,
     * please use {@link #wrapAsDataSource(org.apache.ojb.broker.metadata.JdbcConnectionDescriptor, org.apache.commons.pool.ObjectPool)}
     */
    protected PoolingDataSource createPoolingDataSource(ObjectPool connectionPool)
    {
        // mkalen: not a nice cast but we do not want to break signature and it is safe
        // since any new implementations will not be based on this method and the wrapper-
        // call here goes to code we control (where we know it's PoolingDataSource)
        return (PoolingDataSource) wrapAsDataSource(null, connectionPool);
    }

    // ----- end deprecated methods -----

    //**************************************************************************************
    // Inner classes
    //************************************************************************************

    /**
     * Inner class used as factory for DBCP connection pooling.
     * Adhers to OJB platform specification by calling platform-specific init methods
     * on newly created connections.
     * @see org.apache.ojb.broker.platforms.Platform#initializeJdbcConnection
     */
    class ConPoolFactory extends DriverManagerConnectionFactory
    {

        private final JdbcConnectionDescriptor jcd;

        public ConPoolFactory(JdbcConnectionDescriptor jcd, Properties properties)
        {
            super(getDbURL(jcd), properties);
            this.jcd = jcd;
        }

        public Connection createConnection() throws SQLException
        {
            final Connection conn = super.createConnection();
            if (conn != null)
            {
                try
                {
                    initializeJdbcConnection(conn, jcd);
                }
                catch (LookupException e)
                {
                    log.error("Platform dependent initialization of connection failed", e);
                }
            }
            return conn;
        }

    }

}
