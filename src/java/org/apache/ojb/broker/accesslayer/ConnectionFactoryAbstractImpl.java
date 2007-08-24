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

import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.platforms.PlatformException;
import org.apache.ojb.broker.platforms.PlatformFactory;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Abstract base class to simplify implementation of {@link ConnectionFactory}'s.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ConnectionFactoryAbstractImpl.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public abstract class ConnectionFactoryAbstractImpl implements ConnectionFactory
{
    private Logger log = LoggerFactory.getLogger(ConnectionFactoryAbstractImpl.class);

    /**
     * holds the datasource looked up from JNDI in a map, keyed
     * by the JNDI name.
     */
    private Map dataSourceCache = new HashMap();

    /**
     * Returns a valid JDBC Connection. Implement this method in concrete subclasses.
     * Concrete implementations using Connection pooling are responsible for any validation
     * and pool removal management.
     * <p>
     * Note: This method is never called for a jdbc-connection-descriptor that uses datasources,
     * OJB only manages connections from DriverManager.
     * <p>
     * Note: If the concrete implementation does not callback to
     * {@link #newConnectionFromDriverManager(org.apache.ojb.broker.metadata.JdbcConnectionDescriptor)}
     * when creating a new Connection, it <em>must</em> call
     * {@link #initializeJdbcConnection(java.sql.Connection, org.apache.ojb.broker.metadata.JdbcConnectionDescriptor)}
     * so that the platform implementation can peform any RDBMS-specific init tasks for newly
     * created Connection objetcs.
     *
     * @param jcd the connection descriptor for which to return a validated Connection
     * @return a valid Connection, never null.
     * Specific implementations <em>must</em> guarantee that the connection is not null and
     * that it is valid.
     * @throws LookupException if a valid Connection could not be obtained
     */
    public abstract Connection checkOutJdbcConnection(JdbcConnectionDescriptor jcd)
            throws LookupException;

    /**
     * Releases a Connection after use. Implement this method in concrete subclasses.
     * Concrete implementations using Connection pooling are responsible for any validation
     * and pool removal management.
     * <p>
     * Note: This method is never called for a jdbc-connection-descriptor that uses datasources,
     * OJB only manages connections from DriverManager.
     *
     * @param jcd the connection descriptor for which the connection was created
     * @param con the connection to release.
     * Callers <em>must</em> guarantee that the passed connection was obtained by calling
     * {@link #checkOutJdbcConnection(org.apache.ojb.broker.metadata.JdbcConnectionDescriptor)}.
     * @throws LookupException if errors occured during release of object. Typically happens
     * if return of object to pool fails in a pooled implementation.
     */
    public abstract void releaseJdbcConnection(JdbcConnectionDescriptor jcd, Connection con)
            throws LookupException;

    public void releaseConnection(JdbcConnectionDescriptor jcd, Connection con)
    {
        if (con == null) return;
        if (jcd.isDataSource())
        {
            try
            {
                con.close();
            }
            catch (SQLException e)
            {
                log.error("Closing connection failed", e);
            }
        }
        else
        {
            try
            {
                releaseJdbcConnection(jcd, con);
            }
            catch (LookupException e)
            {
                log.error("Unexpected exception when return connection " + con +
                        " to pool using " + jcd, e);
            }
        }
    }

    public Connection lookupConnection(JdbcConnectionDescriptor jcd) throws LookupException
    {
        Connection conn;
        /*
        use JNDI datasourcelookup or ordinary jdbc DriverManager
        to obtain connection ?
        */
        if (jcd.isDataSource())
        {
            if (log.isDebugEnabled())
            {
                log.debug("do datasource lookup, name: " + jcd.getDatasourceName() +
                        ", user: " + jcd.getUserName());
            }
            conn = newConnectionFromDataSource(jcd);
        }
        else
        {
            conn = checkOutJdbcConnection(jcd);
            // connection is now guaranteed to be valid by API contract (else exception is thrown)
        }
        return conn;
    }

    /**
     * Initialize the connection with the specified properties in OJB
     * configuration files and platform depended properties.
     * Invoke this method after a NEW connection is created, not if re-using from pool.
     *
     * @see org.apache.ojb.broker.platforms.PlatformFactory
     * @see org.apache.ojb.broker.platforms.Platform
     */
    protected void initializeJdbcConnection(Connection con, JdbcConnectionDescriptor jcd)
            throws LookupException
    {
        try
        {
            PlatformFactory.getPlatformFor(jcd).initializeJdbcConnection(jcd, con);
        }
        catch (PlatformException e)
        {
            throw new LookupException("Platform dependent initialization of connection failed", e);
        }
    }

    /**
     * Override this method to do cleanup in your implementation.
     * Do a <tt>super.releaseAllResources()</tt> in your method implementation
     * to free resources used by this class.
     */
    public synchronized void releaseAllResources()
    {
        this.dataSourceCache.clear();
    }

    /**
     * Creates a new connection from the data source that the connection descriptor
     * represents. If the connection descriptor does not directly contain the data source
     * then a JNDI lookup is performed to retrieve the data source.
     * 
     * @param jcd The connection descriptor
     * @return A connection instance
     * @throws LookupException if we can't get a connection from the datasource either due to a
     *          naming exception, a failed sanity check, or a SQLException.
     */
    protected Connection newConnectionFromDataSource(JdbcConnectionDescriptor jcd)
            throws LookupException
    {
        Connection retval = null;
        // use JNDI lookup
        DataSource ds = jcd.getDataSource();

        if (ds == null)
        {
            // [tomdz] Would it suffice to store the datasources only at the JCDs ?
            //         Only possible problem would be serialization of the JCD because
            //         the data source object in the JCD does not 'survive' this
            ds = (DataSource) dataSourceCache.get(jcd.getDatasourceName());
        }
        try
        {
            if (ds == null)
            {
                /**
                 * this synchronization block won't be a big deal as we only look up
                 * new datasources not found in the map.
                 */
                synchronized (dataSourceCache)
                {
                    InitialContext ic = new InitialContext();
                    ds = (DataSource) ic.lookup(jcd.getDatasourceName());
                    /**
                     * cache the datasource lookup.
                     */
                    dataSourceCache.put(jcd.getDatasourceName(), ds);
                }
            }
            if (jcd.getUserName() == null)
            {
                retval = ds.getConnection();
            }
            else
            {
                retval = ds.getConnection(jcd.getUserName(), jcd.getPassWord());
            }
        }
        catch (SQLException sqlEx)
        {
            log.error("SQLException thrown while trying to get Connection from Datasource (" +
                    jcd.getDatasourceName() + ")", sqlEx);
            throw new LookupException("SQLException thrown while trying to get Connection from Datasource (" +
                    jcd.getDatasourceName() + ")", sqlEx);
        }
        catch (NamingException namingEx)
        {
            log.error("Naming Exception while looking up DataSource (" + jcd.getDatasourceName() + ")", namingEx);
            throw new LookupException("Naming Exception while looking up DataSource (" + jcd.getDatasourceName() +
                    ")", namingEx);
        }
        // initialize connection
        initializeJdbcConnection(retval, jcd);
        if(log.isDebugEnabled()) log.debug("Create new connection using DataSource: "+retval);
        return retval;
    }

    /**
     * Returns a new created connection
     *
     * @param jcd the connection descriptor
     * @return an instance of Connection from the drivermanager
     */
    protected Connection newConnectionFromDriverManager(JdbcConnectionDescriptor jcd)
            throws LookupException
    {
        Connection retval = null;
        // use JDBC DriverManager
        final String driver = jcd.getDriver();
        final String url = getDbURL(jcd);
        try
        {
            // loads the driver - NB call to newInstance() added to force initialisation
            ClassHelper.getClass(driver, true);
            final String user = jcd.getUserName();
            final String password = jcd.getPassWord();
            final Properties properties = getJdbcProperties(jcd, user, password);
            if (properties.isEmpty())
            {
                if (user == null)
                {
                    retval = DriverManager.getConnection(url);
                }
                else
                {
                    retval = DriverManager.getConnection(url, user, password);
                }
            }
            else
            {
                retval = DriverManager.getConnection(url, properties);
            }
        }
        catch (SQLException sqlEx)
        {
            log.error("Error getting Connection from DriverManager with url (" + url + ") and driver (" + driver + ")", sqlEx);
            throw new LookupException("Error getting Connection from DriverManager with url (" + url + ") and driver (" + driver + ")", sqlEx);
        }
        catch (ClassNotFoundException cnfEx)
        {
            log.error(cnfEx);
            throw new LookupException("A class was not found", cnfEx);
        }
        catch (Exception e)
        {
            log.error("Instantiation of jdbc driver failed", e);
            throw new LookupException("Instantiation of jdbc driver failed", e);
        }
        // initialize connection
        initializeJdbcConnection(retval, jcd);
        if(log.isDebugEnabled()) log.debug("Create new connection using DriverManager: "+retval);
        return retval;
    }

    /**
     * Returns connection properties for passing to DriverManager, after merging
     * JDBC driver-specific configuration settings with name/password from connection
     * descriptor.
     * @param jcd the connection descriptor with driver-specific settings
     * @param user the jcd username (or null if not using authenticated login)
     * @param password the jcd password (only used when user != null)
     * @return merged properties object to pass to DriverManager
     */
    protected Properties getJdbcProperties(JdbcConnectionDescriptor jcd,
                                           String user, String password)
    {
        final Properties jdbcProperties;
        jdbcProperties = jcd.getConnectionPoolDescriptor().getJdbcProperties();
        if (user != null)
        {
            jdbcProperties.put("user", user);
            jdbcProperties.put("password", password);
        }
        return jdbcProperties;
    }

    protected Properties getJdbcProperties(JdbcConnectionDescriptor jcd)
    {
        final String user = jcd.getUserName();
        final String password = jcd.getPassWord();
        return getJdbcProperties(jcd, user, password);
    }

    protected String getDbURL(JdbcConnectionDescriptor jcd)
    {
        return jcd.isDataSource() ? jcd.getDatasourceName() :
                jcd.getProtocol() + ":" + jcd.getSubProtocol() + ":" + jcd.getDbAlias();
    }

    protected String getJcdDescription(JdbcConnectionDescriptor jcd)
    {
        return "Connection for JdbcConnectionDescriptor (" +
               (jcd.getDatasourceName() != null ? "datasource: " + jcd.getDatasourceName() :
                "db-url: " + getDbURL(jcd) + ", user: " + jcd.getUserName()) +
               ")";
    }

}
