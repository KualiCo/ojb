package org.apache.ojb.broker;

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

import java.sql.SQLException;

/**
 * Encapsulates a SQL exception thrown during a broker action.
 * 
 * @author Thomas Mahler
 * @version $Id: PersistenceBrokerSQLException.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */ 
public class PersistenceBrokerSQLException extends PersistenceBrokerException
{
	private String sqlState = null;
	
    /**
     * Creates a new exception instance.
     */
    public PersistenceBrokerSQLException()
    {
        super();
    }

    /**
     * Creates a new exception instance.
     * 
     * @param cause The base exception
     */
    public PersistenceBrokerSQLException(SQLException cause)
    {
        super(cause);
        sqlState = cause.getSQLState();
    }
    
    /**
     * Creates a new exception instance.
     * 
     * @param msg The exception message
     */
    public PersistenceBrokerSQLException(String msg)
    {
        super(msg);
    }

    /**
     * Creates a new exception instance.
     * 
     * @param msg   The exception message
     * @param cause The base exception
     */
    public PersistenceBrokerSQLException(String msg, SQLException cause)
    {
        super(msg, cause);
        sqlState = cause.getSQLState();
    }

    /**
     * Returns the SQL state of the underlying {@link java.sql.SQLException}.
     * 
     * @return The SQL state
     */
    public String getSQLState()
    {
        return sqlState;
    }
}
