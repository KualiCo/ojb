package org.apache.ojb.otm.copy;

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

import org.apache.ojb.broker.PersistenceBroker;


/**
 * @author matthew.baird
 */
public class CloneableObjectCopyStrategy implements ObjectCopyStrategy
{
	/**
	 * If users want to implement clone on all their objects, we can use this
	 * to make copies. This is hazardous as user may mess it up, but it is also
	 * potentially the fastest way of making a copy.
	 *
	 * Usually the OjbCloneable interface should just be delegating to the clone()
	 * operation that the user has implemented.
	 *
	 * @see org.apache.ojb.otm.copy.ObjectCopyStrategy#copy(Object)
	 *
	 */
	public Object copy(Object obj, PersistenceBroker broker)
			throws ObjectCopyException
	{
		if (obj instanceof OjbCloneable)
		{
			try
			{
				return ((OjbCloneable) obj).ojbClone();
			}
			catch (Exception e)
			{
				throw new ObjectCopyException(e);
			}
		}
		else
		{
			throw new ObjectCopyException("Object must implement OjbCloneable in order to use the"
										  + " CloneableObjectCopyStrategy");
		}
	}
}
