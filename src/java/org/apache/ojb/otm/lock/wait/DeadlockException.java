package org.apache.ojb.otm.lock.wait;

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

import org.apache.ojb.otm.lock.LockingException;

/**
 *
 * <javadoc>
 * 
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 * 
 */
public class DeadlockException extends LockingException
{

	/**
	 * Constructor for DeadlockException.
	 */
	public DeadlockException()
	{
		super();
	}

	/**
	 * Constructor for DeadlockException.
	 * @param message
	 */
	public DeadlockException(String message)
	{
		super(message);
	}

	/**
	 * Constructor for DeadlockException.
	 * @param exception
	 */
	public DeadlockException(Throwable exception)
	{
		super(exception);
	}

	/**
	 * Constructor for DeadlockException.
	 * @param message
	 * @param exception
	 */
	public DeadlockException(String message, Throwable exception)
	{
		super(message, exception);
	}

}
