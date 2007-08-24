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

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.OJBRuntimeException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Connection factory which pools the requested
 * connections for different JdbcConnectionDescriptors
 * using Commons Pool API.
 *
 * @version $Id: ConnectionFactoryPooledImpl.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 * @see <a href="http://jakarta.apache.org/commons/pool/">Commons Pool Website</a>
 */
public class ConnectionFactoryPooledImpl extends ConnectionFactoryAbstractImpl
{

    private Logger log = LoggerFactory.getLogger(ConnectionFactoryPooledImpl.class);
    /** Key=PBKey, value=ObjectPool. */
    private Map poolMap = new HashMap();
    /** Synchronize object for operations not synchronized on Map only. */
    private final Object poolSynch = new Object();

    public void releaseJdbcConnection(JdbcConnectionDescriptor jcd, Connection con)
            throws LookupException
    {
        final ObjectPool op = (ObjectPool) poolMap.get(jcd.getPBKey());
        try
        {
            /* mkalen: NB - according to the Commons Pool API we should _not_ perform
             * any additional checks here since we will then break testOnX semantics
             *
             * To enable Connection validation on releaseJdbcConnection,
             * set a validation query and specify testOnRelease=true
             *
             * Destruction of pooled objects is performed by the actual Commons Pool
             * ObjectPool implementation when the object factory's validateObject method
             * returns false. See ConPoolFactory#validateObject.
             */
            op.returnObject(con);
        }
        catch (Exception e)
        {
            throw new LookupException(e);
        }
    }

    public Connection checkOutJdbcConnection(JdbcConnectionDescriptor jcd) throws LookupException
    {
        ObjectPool op = (ObjectPool) poolMap.get(jcd.getPBKey());
        if (op == null)
        {
            synchronized (poolSynch)
            {
                log.info("Create new connection pool:" + jcd);
                op = createConnectionPool(jcd);
                poolMap.put(jcd.getPBKey(), op);
            }
        }
        final Connection conn;
        try
        {
            conn = (Connection) op.borrowObject();
        }
        catch (NoSuchElementException e)
        {
            int active = 0;
            int idle = 0;
            try
            {
                active = op.getNumActive();
                idle = op.getNumIdle();
            }
            catch(Exception ignore){}
            throw new LookupException("Could not borrow connection from pool, seems ObjectPool is exhausted." +
                    " Active/Idle instances in pool=" + active + "/" +  idle
                    + ". "+ JdbcConnectionDescriptor.class.getName() + ":  " + jcd, e);
        }
        catch (Exception e)
        {
            int active = 0;
            int idle = 0;
            try
            {
                active = op.getNumActive();
                idle = op.getNumIdle();
            }
            catch(Exception ignore){}
            throw new LookupException("Could not borrow connection from pool." +
                    " Active/Idle instances in pool=" + active + "/" +  idle
                    + ". "+ JdbcConnectionDescriptor.class.getName() + ":  " + jcd, e);
        }
        return conn;
    }

    /**
     * Create the pool for pooling the connections of the given connection descriptor.
     * Override this method to implement your on {@link org.apache.commons.pool.ObjectPool}.
     */
    public ObjectPool createConnectionPool(JdbcConnectionDescriptor jcd)
    {
        if (log.isDebugEnabled()) log.debug("createPool was called");
        PoolableObjectFactory pof = new ConPoolFactory(this, jcd);
        GenericObjectPool.Config conf = jcd.getConnectionPoolDescriptor().getObjectPoolConfig();
        return (ObjectPool)new GenericObjectPool(pof, conf);
    }

    /**
     * Closes all managed pools.
     */
    public void releaseAllResources()
    {
        synchronized (poolSynch)
        {
            Collection pools = poolMap.values();
            poolMap = new HashMap(poolMap.size());
            ObjectPool op = null;
            for (Iterator iterator = pools.iterator(); iterator.hasNext();)
            {
                try
                {
                    op = ((ObjectPool) iterator.next());
                    op.close();
                }
                catch (Exception e)
                {
                    log.error("Exception occured while closing pool " + op, e);
                }
            }
        }
        super.releaseAllResources();
    }

    //**************************************************************************************
    // Inner classes
    //************************************************************************************

    /**
     * Inner class - {@link org.apache.commons.pool.PoolableObjectFactory}
     * used as factory for connection pooling.
     */
    class ConPoolFactory extends BasePoolableObjectFactory
    {
        final private JdbcConnectionDescriptor jcd;
        final private ConnectionFactoryPooledImpl cf;
        private int failedValidationQuery;

        public ConPoolFactory(ConnectionFactoryPooledImpl cf, JdbcConnectionDescriptor jcd)
        {
            this.cf = cf;
            this.jcd = jcd;
        }

        public boolean validateObject(Object obj)
        {
            boolean isValid = false;
            if (obj != null)
            {
                final Connection con = (Connection) obj;
                try
                {
                    isValid = !con.isClosed();
                }
                catch (SQLException e)
                {
                    log.warn("Connection validation failed: " + e.getMessage());
                    if (log.isDebugEnabled()) log.debug(e);
                    isValid = false;
                }
                if (isValid)
                {
                    final String validationQuery;
                    validationQuery = jcd.getConnectionPoolDescriptor().getValidationQuery();
                    if (validationQuery != null)
                    {
                        isValid = validateConnection(con, validationQuery);
                    }
                }
            }
            return isValid;
        }

        private boolean validateConnection(Connection conn, String query)
        {
            PreparedStatement stmt = null;
            ResultSet rset = null;
            boolean isValid = false;
            if (failedValidationQuery > 100)
            {
                --failedValidationQuery;
                throw new OJBRuntimeException("Validation of connection "+conn+" using validation query '"+
                        query + "' failed more than 100 times.");
            }
            try
            {
                stmt = conn.prepareStatement(query);
                stmt.setMaxRows(1);
                stmt.setFetchSize(1);
                rset = stmt.executeQuery();
                if (rset.next())
                {
                    failedValidationQuery = 0;
                    isValid = true;
                }
                else
                {
                    ++failedValidationQuery;
                    log.warn("Validation query '" + query +
                            "' result set does not match, discard connection");
                    isValid = false;
                }
            }
            catch (SQLException e)
            {
                ++failedValidationQuery;
                log.warn("Validation query for connection failed, discard connection. Query was '" +
                        query + "', Message was " + e.getMessage());
                if (log.isDebugEnabled()) log.debug(e);
            }
            finally
            {
                try
                {
                    if(rset != null) rset.close();
                }
                catch (SQLException t)
                {
                    if (log.isDebugEnabled()) log.debug("ResultSet already closed.", t);
                }
                try
                {
                    if(stmt != null) stmt.close();
                }
                catch (SQLException t)
                {
                    if (log.isDebugEnabled()) log.debug("Statement already closed.", t);
                }
            }
            return isValid;
        }

        public Object makeObject() throws Exception
        {
            if (log.isDebugEnabled()) log.debug("makeObject called");
            return cf.newConnectionFromDriverManager(jcd);
        }

        public void destroyObject(Object obj)
                throws Exception
        {
            log.info("Destroy object was called, try to close connection: " + obj);
            try
            {
                ((Connection) obj).close();
            }
            catch (SQLException ignore)
            {
                //ignore it
            }
        }
    }

}
