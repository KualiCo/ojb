package org.apache.ojb.otm.core;

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

/**
 *
 * Exception denoting that the transaction is already in progress. This typically
 * occurs when an operation destined for a running Transaction is invoked on an
 * unstarted Transaction
 * 
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 * 
 */
public class TransactionNotInProgressException extends TransactionException
{

	/**
	 * Constructor for TransactionNotInProgressException.
	 */
	public TransactionNotInProgressException()
	{
		super();
	}

	/**
	 * Constructor for TransactionNotInProgressException.
	 * @param msg
	 */
	public TransactionNotInProgressException(String msg)
	{
		super(msg);
	}

	/**
	 * Constructor for TransactionNotInProgressException.
	 * @param cause
	 */
	public TransactionNotInProgressException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * Constructor for TransactionNotInProgressException.
	 * @param msg
	 * @param cause
	 */
	public TransactionNotInProgressException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

}
