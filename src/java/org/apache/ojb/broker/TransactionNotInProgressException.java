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

/**
 * Is created when an action was tried that requires a transaction, and there was no
 * transaction in progress.
 * 
 * @author Thomas Mahler
 * @version $Id: TransactionNotInProgressException.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */ 
public class TransactionNotInProgressException extends PersistenceBrokerException
{
    /**
     * Creates a new exception instance.
     */
    public TransactionNotInProgressException()
    {
        super();
    }

    /**
     * Creates a new exception instance.
     * 
     * @param msg The exception message
     */
    public TransactionNotInProgressException(String msg)
    {
        super(msg);
    }

    /**
     * Creates a new exception instance.
     * 
     * @param cause The base exception
     */
    public TransactionNotInProgressException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Creates a new exception instance.
     * 
     * @param msg   The exception message
     * @param cause The base exception
     */
    public TransactionNotInProgressException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
