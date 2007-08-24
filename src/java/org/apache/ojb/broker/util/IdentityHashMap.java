package org.apache.ojb.broker.util;

/* Copyright 2002-2005 The Apache Software Foundation
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

import java.util.HashMap;
import java.util.Map;

/**
 * replacement for the JDK1.4 version
 * User: Matthew Baird
 * Date: Jul 8, 2003
 * Time: 8:37:00 AM
 */
public final class IdentityHashMap extends HashMap
{
	private static final class IdentityKey
	{
		private final Object m_key;

		public IdentityKey(final Object key)
		{
			m_key = key;
		}

		public boolean equals(final Object o)
		{
			return (o == m_key);
		}

		public int hashCode()
		{
			return System.identityHashCode(m_key);
		}
	}

	/**
	 * Constructor for IdentityHashMap.
	 * @param initialCapacity
	 * @param loadFactor
	 */
	public IdentityHashMap(final int initialCapacity, final float loadFactor)
	{
		super(initialCapacity, loadFactor);
	}

	/**
	 * Constructor for IdentityHashMap.
	 * @param initialCapacity
	 */
	public IdentityHashMap(final int initialCapacity)
	{
		super(initialCapacity);
	}

	/**
	 * Constructor for IdentityHashMap.
	 */
	public IdentityHashMap()
	{
		super();
	}

	/**
	 * Constructor for IdentityHashMap.
	 * @param t
	 */
	public IdentityHashMap(final Map t)
	{
		super(t);
	}

	/**
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public Object get(final Object key)
	{
		return super.get(new IdentityKey(key));
	}

	/**
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public Object put(final Object key, final Object value)
	{
		return super.put(new IdentityKey(key), value);
	}

	/**
	 * adds an object to the Map. new IdentityKey(obj) is used as key
	 */
	public Object add(final Object value)
	{
		final Object key = new IdentityKey(value);
		if (!super.containsKey(key))
		{
			return super.put(key, value);
		}
		else
		{
			return null;
		}
	}

	/**
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public Object remove(final Object key)
	{
		return super.remove(new IdentityKey(key));
	}

	/**
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(final Object key)
	{
		return super.containsKey(new IdentityKey(key));
	}
}

