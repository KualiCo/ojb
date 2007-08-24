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
 * Exception that is thrown if a violation of an optimistic lock was detected.
 * 
 * @author Thomas Mahler
 * @version $Id: OptimisticLockException.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class OptimisticLockException extends PersistenceBrokerException
{
    /** The violating object. */
	private Object sourceObject;
	
    /**
     * Creates a new exception instance.
     */
    public OptimisticLockException()
    {
        super();
    }

    /**
     * Creates a new exception instance.
     * 
     * @param msg The exception message
     */
    public OptimisticLockException(String msg)
    {
        super(msg);
    }
    
    /**
     * Creates a new exception instance.
     * 
     * @param msg    The exception message
     * @param source The violating object
     */
    public OptimisticLockException(String msg, Object source)
    {
        super(msg);
        sourceObject = source;
    }    

    /**
     * Creates a new exception instance.
     * 
     * @param cause The base exception
     */
    public OptimisticLockException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Gets the violating object.
     * 
     * @return The object
     */
    public Object getSourceObject()
    {
        return sourceObject;
    }

    /**
     * Sets the violating object.
     * 
     * @param sourceObject The object
     */
    public void setSourceObject(Object sourceObject)
    {
        this.sourceObject = sourceObject;
    }

}
