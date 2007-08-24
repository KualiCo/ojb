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
 * Exception indicate an SQL key contraint violation.
 *
 * @author Matthew Baird
 * @version $Id: KeyConstraintViolatedException.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class KeyConstraintViolatedException extends PersistenceBrokerSQLException
{
    /**
     * Creates a new exception instance.
     */
    public KeyConstraintViolatedException()
    {
        super();
    }

    /**
     * Creates a new exception instance.
     * 
     * @param base The wrapped exception
     */
    public KeyConstraintViolatedException(SQLException base)
    {
        super(base);
    }

    /**
     * Creates a new exception instance.
     * 
     * @param msg The exception message
     */
    public KeyConstraintViolatedException(String msg)
    {
        super(msg);
    }

    /**
     * Creates a new exception instance.
     * 
     * @param msg The exception message
     * @param base    The wrapped exception
     */
    public KeyConstraintViolatedException(String msg, SQLException base)
    {
        super(msg, base);
    }
}
