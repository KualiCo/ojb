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
import org.apache.ojb.broker.util.pooling.ByPassConnection;

import java.sql.Connection;

/**
 * ConnectionFactory for use in managed environments - eg JBoss.
 *
 * @deprecated no longer needed to specify a specific <em>ConnectionFactory</em> class in
 * managed environments.
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ConnectionFactoryManagedImpl.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class ConnectionFactoryManagedImpl extends ConnectionFactoryNotPooledImpl
{
    public Connection lookupConnection(JdbcConnectionDescriptor jcd) throws LookupException
    {
        return new ByPassConnection(super.lookupConnection(jcd));
    }

    protected Connection newConnectionFromDriverManager(JdbcConnectionDescriptor jcd)
            throws LookupException
    {
        throw new UnsupportedOperationException("Not supported in managed environment");
    }
}
