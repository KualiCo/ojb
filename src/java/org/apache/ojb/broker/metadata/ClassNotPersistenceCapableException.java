package org.apache.ojb.broker.metadata;

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
 * This exception is thrown if a class is not described in the MetaData Repository,
 * and thus cannot be handled properly by OJB
 * @author Thomas Mahler
 * @version $Id: ClassNotPersistenceCapableException.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class ClassNotPersistenceCapableException extends MetadataException
{

    /**
     *
     * Creates a new ClassNotPersistenceCapableException without a message and without a cause.
     *
     */
    public ClassNotPersistenceCapableException()
    {
        super();
    }

    /**
     *
     * Creates a new ClassNotPersistenceCapableException with the specified message.
     *
     * @param message the detail message
     *
     */
    public ClassNotPersistenceCapableException(String message)
    {
        super(message);
    }

    /**
     *
     * Creates a new ClassNotPersistenceCapableException with the specified cause.
     *
     * @param cause The cause of this Exception
     *
     */
    public ClassNotPersistenceCapableException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Creates a new ClassNotPersistenceCapableException with the specified message and the specified cause.
     * @param message the detail message
     * @param cause the root cause
     */
    public ClassNotPersistenceCapableException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
