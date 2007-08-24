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
import org.apache.ojb.broker.platforms.Platform;

import java.sql.Connection;

/**
 * The connection manager handles the life cycle of a connection.
 * Each {@link org.apache.ojb.broker.PersistenceBroker} instance
 * use it's own connection manager.
 */
public interface ConnectionManagerIF
{

    /**
     * Return the associated {@link org.apache.ojb.broker.metadata.JdbcConnectionDescriptor}.
     */
    JdbcConnectionDescriptor getConnectionDescriptor();

    /**
     * Returns the supported {@link org.apache.ojb.broker.platforms.Platform}
     * determined by the {@link org.apache.ojb.broker.metadata.JdbcConnectionDescriptor}.
     * @see #getConnectionDescriptor
     */
    Platform getSupportedPlatform();

    /**
     * checks if Connection conn is still open.
     * returns true, if connection is open, else false.
     */
    boolean isAlive(Connection conn);

    /**
     * Return a connection.
     */
    Connection getConnection() throws LookupException;

    /**
     * Hold connection is in local transaction.
     */
    boolean isInLocalTransaction();

    /**
     * Begin local transaction on the hold connection
     * and set autocommit to false.
     */
    void localBegin();

    /**
     * Commit the local transaction on the hold connection.
     */
    void localCommit();

    /**
     * Rollback a changes on the hold connection.
     */
    void localRollback();

    /**
     * Release the hold connection.
     */
    void releaseConnection();

    /**
     * Sets the batch mode on (<code>true</code>) or
     * off (<code>false</code>).
     */
    void setBatchMode(boolean mode);

    /**
     * @return the batch mode.
     */
    boolean isBatchMode();

    /**
     * Execute batch (if the batch mode where used).
     */
    void executeBatch();

    /**
     * Execute batch if the number of statements in it
     * exceeded the limit (if the batch mode where used).
     */
    void executeBatchIfNecessary();

    /**
     * Clear batch (if the batch mode where used).
     */
    void clearBatch();

}
