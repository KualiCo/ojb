package org.apache.ojb.broker.util.batch;

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


import java.sql.Connection;
import java.sql.SQLException;

/**
 * The interface which is used to create dynamic proxy which will also
 * implement {@link java.sql.PreparedStatement} and allow to automatically gathers 
 * INSERT, UPDATE and DELETE PreparedStatements into batches.
 *
 * @author Oleg Nitz (<a href="mailto:olegnitz@apache.org">olegnitz@apache.org</a>)
 */
public interface BatchPreparedStatement
{

    /**
     * This method performs database modification at the very and of transaction.
     */
    public void doExecute(Connection conn) throws SQLException;
}
