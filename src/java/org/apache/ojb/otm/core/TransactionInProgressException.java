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
 * occurs an operation destined for an unstarted transaction is invoked on a running
 * Transaction.
 * 
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 * 
 */
public class TransactionInProgressException extends TransactionException
{

	/**
	 * Constructor for TransactionInProgressException.
	 */
	public TransactionInProgressException()
	{
		super();
	}

	/**
	 * Constructor for TransactionInProgressException.
	 * @param msg
	 */
	public TransactionInProgressException(String msg)
	{
		super(msg);
	}

	/**
	 * Constructor for TransactionInProgressException.
	 * @param cause
	 */
	public TransactionInProgressException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * Constructor for TransactionInProgressException.
	 * @param msg
	 * @param cause
	 */
	public TransactionInProgressException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

}
