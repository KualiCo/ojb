package org.apache.ojb.otm.transaction;

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

import org.apache.ojb.otm.core.TransactionException;

public class TransactionFactoryException extends TransactionException
{

	/**
	 * Constructor for TransactionFactoryException.
	 */
	public TransactionFactoryException()
	{
		super();
	}

	/**
	 * Constructor for TransactionFactoryException.
	 * @param msg
	 */
	public TransactionFactoryException(String msg)
	{
		super(msg);
	}

	/**
	 * Constructor for TransactionFactoryException.
	 * @param cause
	 */
	public TransactionFactoryException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * Constructor for TransactionFactoryException.
	 * @param msg
	 * @param cause
	 */
	public TransactionFactoryException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

}
