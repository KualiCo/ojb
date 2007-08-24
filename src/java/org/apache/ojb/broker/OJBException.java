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

import org.apache.commons.lang.exception.NestableException;

/**
 * Base class of all checked exceptions used in OJB.
 *
 * @author Thomas Mahler
 * @version $Id: OJBException.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class OJBException extends NestableException
{
    /**
     * Creates a new exception instance.
     */
    public OJBException()
    {
        super();
    }

    /**
     * Creates a new exception instance.
     * 
     * @param msg The exception message
     */
    public OJBException(String msg)
    {
        super(msg);
    }

    /**
     * Creates a new exception instance.
     * 
     * @param cause The base exception
     */
    public OJBException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Creates a new exception instance.
     * 
     * @param msg   The exception message
     * @param cause The base exception
     */
    public OJBException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
