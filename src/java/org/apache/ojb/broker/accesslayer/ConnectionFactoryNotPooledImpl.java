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
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base implementation without connection pooling.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ConnectionFactoryNotPooledImpl.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class ConnectionFactoryNotPooledImpl extends ConnectionFactoryAbstractImpl
{
    private Logger log = LoggerFactory.getLogger(ConnectionFactoryNotPooledImpl.class);

    public Connection checkOutJdbcConnection(JdbcConnectionDescriptor jcd) throws LookupException
    {
        if (log.isDebugEnabled())
        {
            log.debug("checkOutJdbcConnection: this implementation always return a new Connection");
        }
        final Connection conn = newConnectionFromDriverManager(jcd);
        validateConnection(conn, jcd);
        // Connection is now guaranteed to be valid (else validateConnection must throw exception)
        return conn;
    }

    protected void validateConnection(Connection conn, JdbcConnectionDescriptor jcd)
            throws LookupException
    {
        if (conn == null)
        {
            log.error(getJcdDescription(jcd) + " failed, DriverManager returned null");
            throw new LookupException("No Connection returned from DriverManager");
        }
        try
        {
            if (conn.isClosed())
            {
                log.error(getJcdDescription(jcd) + " is invalid (closed)");
                throw new LookupException("Could not create valid connection, connection was " +
                                          conn);
            }
        }
        catch (SQLException e)
        {
            log.error(getJcdDescription(jcd) + " failed validation with exception");
            throw new LookupException("Connection validation failed", e);
        }
    }

    public void releaseJdbcConnection(JdbcConnectionDescriptor jcd, Connection con)
            throws LookupException
    {
        try
        {
            con.close();
        }
        catch (SQLException e)
        {
            log.warn("Connection.close() failed, message was " + e.getMessage());
        }
    }

}
