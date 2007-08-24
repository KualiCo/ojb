package org.apache.ojb.otm.lock;

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

import org.apache.commons.lang.exception.NestableException;

public class LockingException extends NestableException
{

	/**
	 * Constructor for LockingException.
	 */
	public LockingException()
	{
		super();
	}

	/**
	 * Constructor for LockingException.
	 * @param arg0
	 */
	public LockingException(String message)
	{
		super(message);
	}

	/**
	 * Constructor for LockingException.
	 * @param arg0
	 */
	public LockingException(Throwable exception)
	{
		super(exception);
	}

	/**
	 * Constructor for LockingException.
	 * @param arg0
	 * @param arg1
	 */
	public LockingException(String message, Throwable exception)
	{
		super(message, exception);
	}

}
