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
  * @author matthew.baird
  */
public class TransactionAbortedException extends TransactionException
{
	/**
	 * Constructor for TransactionAbortedException.
	 */
	public TransactionAbortedException()
	{
		super();
	}
	/**
	 * Constructor for TransactionAbortedException.
	 * @param message
	 */
	public TransactionAbortedException(String message)
	{
		super(message);
	}
	/**
	 * Constructor for TransactionAbortedException.
	 * @param message
	 * @param cause
	 */
	public TransactionAbortedException(String message, Throwable cause)
	{
		super(message + " ROOT: " + cause.getMessage());
	}
	/**
	 * Constructor for TransactionAbortedException.
	 * @param cause
	 */
	public TransactionAbortedException(Throwable cause)
	{
		super(cause.getMessage());
	}
}
