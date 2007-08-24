package org.apache.ojb.broker.accesslayer;

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

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.TransactionAbortedException;
import org.apache.ojb.broker.TransactionInProgressException;
import org.apache.ojb.broker.TransactionNotInProgressException;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.platforms.Platform;
import org.apache.ojb.broker.platforms.PlatformFactory;
import org.apache.ojb.broker.util.batch.BatchConnection;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Manages Connection ressources.
 *
 * @see ConnectionManagerIF
 * @author Thomas Mahler
 * @version $Id: ConnectionManagerImpl.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class ConnectionManagerImpl implements ConnectionManagerIF
{
    private Logger log = LoggerFactory.getLogger(ConnectionManagerImpl.class);

    private PersistenceBrokerImpl broker = null;
    private ConnectionFactory connectionFactory;
    private JdbcConnectionDescriptor jcd;
    private Platform platform;
    private Connection con = null;
    private PBKey pbKey;
    private boolean originalAutoCommitState;
    private boolean isInLocalTransaction;
    private boolean batchMode;
    private BatchConnection batchCon = null;

    public ConnectionManagerImpl(PersistenceBroker broker)
    {
        // TODO: avoid this cast
        this.broker = (PersistenceBrokerImpl) broker;
        this.pbKey = broker.getPBKey();
        this.jcd = MetadataManager.getInstance().connectionRepository().getDescriptor(pbKey);
        this.connectionFactory = ConnectionFactoryFactory.getInstance().createConnectionFactory();
        this.platform = PlatformFactory.getPlatformFor(jcd);
        /*
        by default batch mode is not enabled and after use of a PB
        instance, before instance was returned to pool, batch mode
        was set to false again (PB implementation close method)
        Be carefully in modify this behaviour, changes could cause
        unexpected behaviour
        */
        setBatchMode(false);
    }

    /**
     * Returns the associated {@link org.apache.ojb.broker.metadata.JdbcConnectionDescriptor}
     */
    public JdbcConnectionDescriptor getConnectionDescriptor()
    {
        return jcd;
    }

    public Platform getSupportedPlatform()
    {
        return this.platform;
    }

    /**
     * Returns the underlying connection, requested from
     * {@link org.apache.ojb.broker.accesslayer.ConnectionFactory}.
     * <p>
     * PB#beginTransaction() opens a single jdbc connection via
	 * PB#serviceConnectionManager().localBegin().
	 * If you call PB#serviceConnectionManager().getConnection() later
	 * it returns the already opened connection.
	 * The PB instance will release the used connection during
	 * PB#commitTransaction() or PB#abortTransaction() or PB#close().
     * </p>
     * <p>
     * NOTE: Never call Connection.close() on the connection requested from the ConnectionManager.
     * Cleanup of used connection is done by OJB itself. If you need to release a used connection
     * call {@link #releaseConnection()}.
     * </p>
     */
    public Connection getConnection() throws LookupException
    {
        /*
        if the connection is not null and we are not in a local tx, we check
        the connection state and release "dead" connections.
        if connection is in local tx we do nothing, the dead connection will cause
        an exception and PB instance have to handle rollback
        */
        if(con != null && !isInLocalTransaction() && !isAlive(con))
        {
            releaseConnection();
        }
        if (con == null)
        {
            con = this.connectionFactory.lookupConnection(jcd);
            if (con == null) throw new PersistenceBrokerException("Cannot get connection for " + jcd);
            if (jcd.getUseAutoCommit() == JdbcConnectionDescriptor.AUTO_COMMIT_SET_TRUE_AND_TEMPORARY_FALSE)
            {
                try
                {
                    this.originalAutoCommitState = con.getAutoCommit();
                }
                catch (SQLException e)
                {
                    throw new PersistenceBrokerException("Cannot request autoCommit state on the connection", e);
                }
            }
            if (log.isDebugEnabled()) log.debug("Request new connection from ConnectionFactory: " + con);
        }

        if (isBatchMode())
        {
            if (batchCon == null)
            {
                batchCon = new BatchConnection(con, broker);
            }
            return batchCon;
        }
        else
        {
            return con;
        }
    }

    /**
     * Start transaction on the underlying connection.
     */
    public void localBegin()
    {
        if (this.isInLocalTransaction)
        {
            throw new TransactionInProgressException("Connection is already in transaction");
        }
        Connection connection = null;
        try
        {
            connection = this.getConnection();
        }
        catch (LookupException e)
        {
            /**
             * must throw to notify user that we couldn't start a connection
             */
            throw new PersistenceBrokerException("Can't lookup a connection", e);
        }
        if (log.isDebugEnabled()) log.debug("localBegin was called for con " + connection);
        // change autoCommit state only if we are not in a managed environment
        // and it is enabled by user
        if(!broker.isManaged())
        {
            if (jcd.getUseAutoCommit() == JdbcConnectionDescriptor.AUTO_COMMIT_SET_TRUE_AND_TEMPORARY_FALSE)
            {
                if (log.isDebugEnabled()) log.debug("Try to change autoCommit state to 'false'");
                platform.changeAutoCommitState(jcd, connection, false);
            }
        }
        else
        {
            if(log.isDebugEnabled()) log.debug(
                        "Found managed environment setting in PB, will skip Platform.changeAutoCommitState(...) call");
        }
        this.isInLocalTransaction = true;
    }

    /**
     * Call commit on the underlying connection.
     */
    public void localCommit()
    {
        if (log.isDebugEnabled()) log.debug("commit was called");
        if (!this.isInLocalTransaction)
        {
            throw new TransactionNotInProgressException("Not in transaction, call begin() before commit()");
        }
        try
        {
            if(!broker.isManaged())
            {
                if (batchCon != null)
                {
                    batchCon.commit();
                }
                else if (con != null)
                {
                    con.commit();
                }
            }
            else
            {
                if(log.isDebugEnabled()) log.debug(
                        "Found managed environment setting in PB, will skip Connection.commit() call");
            }
        }
        catch (SQLException e)
        {
            log.error("Commit on underlying connection failed, try to rollback connection", e);
            this.localRollback();
            throw new TransactionAbortedException("Commit on connection failed", e);
        }
        finally
        {
            this.isInLocalTransaction = false;
            restoreAutoCommitState();
            this.releaseConnection();
        }
    }

    /**
     * Call rollback on the underlying connection.
     */
    public void localRollback()
    {
        log.info("Rollback was called, do rollback on current connection " + con);
        if (!this.isInLocalTransaction)
        {
            throw new PersistenceBrokerException("Not in transaction, cannot abort");
        }
        try
        {
            //truncate the local transaction
            this.isInLocalTransaction = false;
            if(!broker.isManaged())
            {
                if (batchCon != null)
                {
                    batchCon.rollback();
                }
                else if (con != null && !con.isClosed())
                {
                    con.rollback();
                }
            }
            else
            {
                if(log.isEnabledFor(Logger.INFO)) log.info(
                        "Found managed environment setting in PB, will ignore rollback call on connection, this should be done by JTA");
            }
        }
        catch (SQLException e)
        {
            log.error("Rollback on the underlying connection failed", e);
        }
        finally
        {
            try
            {
            	restoreAutoCommitState();
		    }
            catch(OJBRuntimeException ignore)
            {
			    // Ignore or log exception
		    }
            releaseConnection();
        }
    }

    /**
     * Reset autoCommit state.
     */
    protected void restoreAutoCommitState()
    {
        try
        {
            if(!broker.isManaged())
            {
                if (jcd.getUseAutoCommit() == JdbcConnectionDescriptor.AUTO_COMMIT_SET_TRUE_AND_TEMPORARY_FALSE
                        && originalAutoCommitState == true && con != null && !con.isClosed())
                {
                    platform.changeAutoCommitState(jcd, con, true);
                }
            }
            else
            {
                if(log.isDebugEnabled()) log.debug(
                        "Found managed environment setting in PB, will skip Platform.changeAutoCommitState(...) call");
            }
        }
        catch (SQLException e)
        {
            // should never be reached
            throw new OJBRuntimeException("Restore of connection autocommit state failed", e);
        }
    }

    /**
     * Check if underlying connection was alive.
     */
    public boolean isAlive(Connection conn)
    {
        try
        {
            return con != null ? !con.isClosed() : false;
        }
        catch (SQLException e)
        {
            log.error("IsAlive check failed, running connection was invalid!!", e);
            return false;
        }
    }

    public boolean isInLocalTransaction()
    {
        return this.isInLocalTransaction;
    }

    /**
     * Release connection to the {@link org.apache.ojb.broker.accesslayer.ConnectionFactory}, make
     * sure that you call the method in either case, it's the only way to free the connection.
     */
    public void releaseConnection()
    {
        if (this.con == null)
        {
            return;
        }
        if(isInLocalTransaction())
        {
            log.error("Release connection: connection is in local transaction, missing 'localCommit' or" +
                    " 'localRollback' call - try to rollback the connection");
            localRollback();
        }
        else
        {
            this.connectionFactory.releaseConnection(this.jcd, this.con);
            this.con = null;
            this.batchCon = null;
        }
    }

    /**
     * Returns the underlying used {@link org.apache.ojb.broker.accesslayer.ConnectionFactory}
     * implementation.
     */
    public ConnectionFactory getUnderlyingConnectionFactory()
    {
        return connectionFactory;
    }

    /**
     * Sets the batch mode on or off - this
     * switch only works if you set attribute <code>batch-mode</code>
     * in <code>jdbc-connection-descriptor</code> true and your database
     * support batch mode.
     *
     * @param mode the batch mode
     */
    public void setBatchMode(boolean mode)
    {
        /*
        arminw:
        if batch mode was set 'false' in repository,
        never enable it.
        There are many users having weird problems
        when batch mode was enabled behind the scenes
        */
        batchMode = mode && jcd.getBatchMode();
    }

    /**
     * @return the batch mode.
     */
    public boolean isBatchMode()
    {
        return batchMode && platform.supportsBatchOperations();
    }

    /**
     * Execute batch (if the batch mode where used).
     */
    public void executeBatch() throws OJBBatchUpdateException
    {
        if (batchCon != null)
        {
            try
            {
                batchCon.executeBatch();
            }
            catch (Throwable th)
            {
                throw new OJBBatchUpdateException(th);
            }
        }
    }

    /**
     * Execute batch if the number of statements in it
     * exceeded the limit (if the batch mode where used).
     */
    public void executeBatchIfNecessary() throws OJBBatchUpdateException
    {
        if (batchCon != null)
        {
            try
            {
                batchCon.executeBatchIfNecessary();
            }
            catch (Throwable th)
            {
                throw new OJBBatchUpdateException(th);
            }
        }
    }

    /**
     * Clear batch (if the batch mode where used).
     */
    public void clearBatch()
    {
        if (batchCon != null)
        {
            batchCon.clearBatch();
        }
    }
}
