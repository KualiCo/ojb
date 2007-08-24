package org.apache.ojb.otm.lock.wait;

import org.apache.ojb.otm.lock.LockingException;

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
public class ConcurrentModificationException extends LockingException
{

	/**
	 * Constructor for ConcurrentModificationException.
	 */
	public ConcurrentModificationException()
	{
		super();
	}

	/**
	 * Constructor for ConcurrentModificationException.
	 * @param message
	 */
	public ConcurrentModificationException(String message)
	{
		super(message);
	}

	/**
	 * Constructor for ConcurrentModificationException.
	 * @param exception
	 */
	public ConcurrentModificationException(Throwable exception)
	{
		super(exception);
	}

	/**
	 * Constructor for ConcurrentModificationException.
	 * @param message
	 * @param exception
	 */
	public ConcurrentModificationException(String message, Throwable exception)
	{
		super(message, exception);
	}

}
