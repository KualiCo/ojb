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

import java.sql.Connection;

/**
 * ConnectionFactory is responsible to lookup and release the
 * connections used by the
 * {@link org.apache.ojb.broker.accesslayer.ConnectionManagerIF}
 * implementation.
 *
 * @version $Id: ConnectionFactory.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 * @see org.apache.ojb.broker.accesslayer.ConnectionFactoryPooledImpl
 * @see org.apache.ojb.broker.accesslayer.ConnectionFactoryNotPooledImpl
 * @see org.apache.ojb.broker.accesslayer.ConnectionFactoryDBCPImpl
 * @see org.apache.ojb.broker.accesslayer.ConnectionFactoryManagedImpl
 */
public interface ConnectionFactory
{

    /**
     * Lookup a connection from the connection factory implementation.
     */
    Connection lookupConnection(JdbcConnectionDescriptor jcd) throws LookupException;

    /**
     * Release connection - CAUTION: Release every connection after use to avoid abandoned connections.
     * Depending on the used implementation connection will be closed, returned to pool, ...
     */
    void releaseConnection(JdbcConnectionDescriptor jcd, Connection con);

    /**
     * Release all resources
     * used by the implementing class (e.g. connection pool, ...)
     * for the given connection descriptor.
     */
    void releaseAllResources();

}
