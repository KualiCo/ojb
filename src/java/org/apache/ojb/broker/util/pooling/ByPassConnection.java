package org.apache.ojb.broker.util.pooling;

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

import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.WrappedConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Simple wrapper for connections that make some methods to no-op for
 * use in managed environments.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 */
public class ByPassConnection extends WrappedConnection
{
    private Logger log = LoggerFactory.getLogger(ByPassConnection.class);

    public ByPassConnection(Connection c)
    {
        super(c);
        this.activateConnection();
    }

    /**
     * a no-op
     */
    public void setAutoCommit(boolean autoCommit) throws SQLException
    {
        /*
        we ignore this. in managed environments it is not
        allowed to change autoCommit state
        */
        if (log.isDebugEnabled()) log.debug("** we ignore setAutoCommit");
    }

    /**
     * a no-op
     */
    public void commit() throws SQLException
    {
        /*
        we ignore commit, cause this will do
        e.g. the J2EE environment for us, when using
        declarative or programmatic transaction in a j2ee container
        */
        if (log.isDebugEnabled()) log.debug("** we ignore commit");
    }

    /**
     * no-op
     */
    public void rollback() throws SQLException
    {
        /*
        arminw:
        rollback of the connection should be done by the AppServer
        so we ignore this call too. in beans user should use ctx.setRollbackOnly
        method
        */
        if (log.isDebugEnabled()) log.debug("** we ignore rollback, done by server");
    }
}
