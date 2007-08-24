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
 * Exception denoting an exception condition in the transaction handling within OTMKit.
 * 
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 * 
 */
public class TransactionException extends OTMGenericException
{

	/**
	 * Constructor for TransactionException.
	 */
	public TransactionException()
	{
		super();
	}

	/**
	 * Constructor for TransactionException.
	 * @param msg
	 */
	public TransactionException(String msg)
	{
		super(msg);
	}

	/**
	 * Constructor for TransactionException.
	 * @param cause
	 */
	public TransactionException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * Constructor for TransactionException.
	 * @param msg
	 * @param cause
	 */
	public TransactionException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

}
