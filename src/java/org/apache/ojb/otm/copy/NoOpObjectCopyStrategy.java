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
 * The NoOpObjectCopyStrategy does not make a copy. It merely returns the same object.
 *
 * For backwards compatability with OJB 0.9, we include a way to no-op copy
 * the object into the transactional context. This means that we are operating
 * on a live object, and can potentially mess stuff up. This is essentially
 * supporting a uncommitted-read only strategy.
 *
 * @author matthew.baird
 */
public class NoOpObjectCopyStrategy implements ObjectCopyStrategy
{
	/**
	 * @see org.apache.ojb.otm.copy.ObjectCopyStrategy#copy(Object)
	 *
	 */
	public Object copy(Object obj, PersistenceBroker broker)
			throws ObjectCopyException
	{
		return obj;
	}
}
