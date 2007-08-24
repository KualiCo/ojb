package org.apache.ojb.odmg;

/* Copyright 2003-2005 The Apache Software Foundation
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

import org.odmg.TransactionAbortedException;

/**
 * This specialised exception allows us to capture the cause of an
 * ODMG TransactionAbortedException. This, in turn,
 * gives the "catcher" of the exception the ability to take different
 * courses of action dependent upon why the transaction was aborted. <p>
 * This exception has been created as a subclass of
 * org.odmg.TransactionAbortedException so that we don't
 * <ul>
 * <li>Modify the ODMG exception - as it is defined by the ODMG spec
 * <li>Break the spec by throwing a non ODMG exception
 * </ul>
 *
 * @author Charles Anthony
 */
public class TransactionAbortedExceptionOJB extends TransactionAbortedException
{
    /**
     * The cause of a TransactionAbortedException
     */
    private Throwable cause;

    public TransactionAbortedExceptionOJB()
    {
        super();
    }

    public TransactionAbortedExceptionOJB(String msg)
    {
        super(msg);
    }

    public TransactionAbortedExceptionOJB(String msg, Throwable th)
    {
        super(msg);
        this.cause = th;
    }

    public TransactionAbortedExceptionOJB(Throwable cause)
    {
        this.cause = cause;
    }

    /**
     * Returns the cause of the exception. May be null.
     */
    public Throwable getCause()
    {
        return cause;
    }
}
