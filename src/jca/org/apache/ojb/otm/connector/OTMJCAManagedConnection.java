package org.apache.ojb.otm.connector;

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

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.otm.OTMConnection;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.core.TransactionException;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird<a>
 */

public class OTMJCAManagedConnection implements ManagedConnection, LocalTransaction
{
    private PBKey m_pbKey;
    private OTMJCAManagedConnectionFactory m_managedConnectionFactory;
    private PrintWriter m_logWriter;
    private boolean m_destroyed;
    private final Set m_handles = new HashSet();
    private final Collection m_connectionEventListeners = new ArrayList();
    private boolean m_managed = false;
    /**
     * the wrapped connection
     */
    private OTMConnection m_connection;
    private Transaction m_tx;

    OTMJCAManagedConnection(ManagedConnectionFactory mcf, OTMConnection conn, PBKey pbKey)
    {
        Util.log("In OTMJCAManagedConnection");
        m_managedConnectionFactory = (OTMJCAManagedConnectionFactory) mcf;
        m_pbKey = pbKey;
        m_connection = conn;
    }

    /**
     * get the underlying wrapped connection
     * @return OTMConnection raw connection to the OTM.
     */
    OTMConnection getConnection()
    {
        if (m_connection == null)
        {
            OTMConnectionRuntimeException ex = new OTMConnectionRuntimeException("Connection is null.");
            sendEvents(ConnectionEvent.CONNECTION_ERROR_OCCURRED, ex, null);
        }
        return m_connection;
    }

    public Transaction getTransaction()
    {
        return this.m_tx;
    }

    public void setTransaction(Transaction tx)
    {
        if (this.m_tx != null) throw new IllegalStateException("Connection already has Transaction");
        this.m_tx = tx;
    }

    public String getUserName()
    {
        return m_pbKey.getUser();
    }

    public Object getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException
    {
        Util.log("In OTMJCAManagedConnection.getConnection");
        OTMJCAConnection myCon = new OTMJCAConnection(this);
        synchronized (m_handles)
        {
            m_handles.add(myCon);
        }
        return myCon;
    }

    public void destroy()
    {
        Util.log("In OTMJCAManagedConnection.destroy");
        cleanup();
        m_connection.close();
        m_destroyed = true;
    }

    public void cleanup()
    {
        Util.log("In OTMJCAManagedConnection.cleanup");
        synchronized (m_handles)
        {
            for (Iterator i = m_handles.iterator(); i.hasNext();)
            {
                OTMJCAConnection lc = (OTMJCAConnection) i.next();
                lc.setManagedConnection(null);
            }
            m_handles.clear();
        }
    }

    void closeHandle(OTMJCAConnection handle)
    {
        synchronized (m_handles)
        {
            m_handles.remove(handle);
        }
        sendEvents(ConnectionEvent.CONNECTION_CLOSED, null, handle);
    }

    public void associateConnection(Object connection)
    {
        Util.log("In OTMJCAManagedConnection.associateConnection");
        if (connection == null)
        {
            throw new OTMConnectionRuntimeException("Cannot associate a null connection");
        }
        if (!(connection instanceof OTMJCAConnection))
        {
            throw new OTMConnectionRuntimeException("Cannot associate a connection of type: " + connection.getClass().getName() + " to a handle that manages: " + OTMJCAConnection.class.getName());
        }
        ((OTMJCAConnection) connection).setManagedConnection(this);
        synchronized (m_handles)
        {
            m_handles.add(connection);
        }
    }

    public void addConnectionEventListener(ConnectionEventListener cel)
    {
        synchronized (m_connectionEventListeners)
        {
            m_connectionEventListeners.add(cel);
        }
    }

    public void removeConnectionEventListener(ConnectionEventListener cel)
    {
        synchronized (m_connectionEventListeners)
        {
            m_connectionEventListeners.remove(cel);
        }
    }

    public XAResource getXAResource()
            throws ResourceException
    {
        Util.log("In OTMJCAManagedConnection.getXAResource");
        throw new NotSupportedException("public XAResource getXAResource() not supported in this release.");
    }

    /**
     * the OTMConnection is the transaction
     * @return
     */
    public LocalTransaction getLocalTransaction()
    {
        Util.log("In OTMJCAManagedConnection.getLocalTransaction");
        return this;
    }

    public ManagedConnectionMetaData getMetaData()
            throws ResourceException
    {
        Util.log("In OTMJCAManagedConnection.getMetaData");
        return new OTMConnectionMetaData(this);
    }

    public void setLogWriter(PrintWriter out)
            throws ResourceException
    {
        Util.log("In OTMJCAManagedConnection.setLogWriter");
        m_logWriter = out;
    }

    public PrintWriter getLogWriter()
            throws ResourceException
    {
        Util.log("In OTMJCAManagedConnection.getLogWriter");
        return m_logWriter;
    }

    boolean isDestroyed()
    {
        Util.log("In OTMJCAManagedConnection.isDestroyed");
        return m_destroyed;
    }

    ManagedConnectionFactory getManagedConnectionFactory()
    {
        Util.log("In OTMJCAManagedConnection.getManagedConnectionFactory");
        return m_managedConnectionFactory;
    }

    public void begin() throws ResourceException
    {
        Util.log("In OTMJCAManagedConnection.begin");
        if (!isManaged())
        {
            try
            {
                m_tx = m_managedConnectionFactory.getKit().getTransaction(m_connection);
                m_tx.begin();
                setManaged(true);
            }
            catch (TransactionException e)
            {
                ResourceException ex = new ResourceException(e.getMessage());
                sendEvents(ConnectionEvent.CONNECTION_ERROR_OCCURRED, ex, null);
                throw ex;
            }
        }
        else
        {
            ResourceException ex = new ResourceException("You probably called begin again without calling Commit or Rollback, OTM does not support nested Local Transactions.");
            sendEvents(ConnectionEvent.CONNECTION_ERROR_OCCURRED, ex, null);
            throw ex;
        }
    }

    public void commit() throws ResourceException
    {
        Util.log("In OTMJCAManagedConnection.commit");
        if (isManaged())
        {
            try
            {
                setManaged(false);
                m_tx.commit();
            }
            catch (TransactionException e)
            {
                m_tx.rollback();
                ResourceException ex = new ResourceException(e.getMessage());
                sendEvents(ConnectionEvent.CONNECTION_ERROR_OCCURRED, ex, null);
                throw ex;
            }
        }
        else
        {
            ResourceException ex = new ResourceException("Cannot call commit when you are not in a Local Transaction.");
            sendEvents(ConnectionEvent.CONNECTION_ERROR_OCCURRED, ex, null);
            throw ex;
        }
    }

    public void rollback() throws ResourceException
    {
        Util.log("In OTMJCAManagedConnection.rollback");
        if (isManaged())
        {
            try
            {
                m_tx.rollback();
                setManaged(false);
            }
            catch (TransactionException e)
            {
                ResourceException ex = new ResourceException(e.getMessage());
                sendEvents(ConnectionEvent.CONNECTION_ERROR_OCCURRED, ex, null);
                throw ex;
            }
        }
        else
        {
            ResourceException ex = new ResourceException("Cannot call rollback when you are not in a Local Transaction.");
            sendEvents(ConnectionEvent.CONNECTION_ERROR_OCCURRED, ex, null);
            throw ex;
        }
    }

    private boolean isManaged()
    {
        return m_managed;
    }

    private void setManaged(boolean flag)
    {
        m_managed = flag;
    }

    /**
     * Section 6.5.6 of the JCA 1.5 spec instructs ManagedConnection instances to notify connection listeners with
     * close/error and local transaction-related events to its registered set of listeners.
     *
     * The events for begin/commit/rollback are only sent if the application server did NOT
     * initiate the actions.
     *
     * This method dispatchs all events to the listeners based on the eventType
     * @param eventType as enumerated in the ConnectionEvents interface
     * @param ex an optional exception if we are sending an error message
     * @param connectionHandle an optional connectionHandle if we have access to it.
     */
    void sendEvents(int eventType, Exception ex, Object connectionHandle)
    {
        ConnectionEvent ce = null;
        if (ex == null)
        {
            ce = new ConnectionEvent(this, eventType);
        }
        else
        {
            ce = new ConnectionEvent(this, eventType, ex);
        }
        ce.setConnectionHandle(connectionHandle);
        Collection copy = null;
        synchronized (m_connectionEventListeners)
        {
            copy = new ArrayList(m_connectionEventListeners);
        }
        switch (ce.getId())
        {
            case ConnectionEvent.CONNECTION_CLOSED:
                for (Iterator i = copy.iterator(); i.hasNext();)
                {
                    ConnectionEventListener cel = (ConnectionEventListener) i.next();
                    cel.connectionClosed(ce);
                }
                break;
            case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
                for (Iterator i = copy.iterator(); i.hasNext();)
                {
                    ConnectionEventListener cel = (ConnectionEventListener) i.next();
                    cel.localTransactionStarted(ce);
                }
                break;
            case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
                for (Iterator i = copy.iterator(); i.hasNext();)
                {
                    ConnectionEventListener cel = (ConnectionEventListener) i.next();
                    cel.localTransactionCommitted(ce);
                }
                break;
            case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
                for (Iterator i = copy.iterator(); i.hasNext();)
                {
                    ConnectionEventListener cel = (ConnectionEventListener) i.next();
                    cel.localTransactionRolledback(ce);
                }
                break;
            case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
                for (Iterator i = copy.iterator(); i.hasNext();)
                {
                    ConnectionEventListener cel = (ConnectionEventListener) i.next();
                    cel.connectionErrorOccurred(ce);
                }
                break;
            default:
                throw new IllegalArgumentException("Illegal eventType: " + ce.getId());
        }
    }
}
