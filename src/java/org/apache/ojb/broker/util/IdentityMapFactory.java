package org.apache.ojb.broker.util;

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

import java.util.Map;

public final class IdentityMapFactory
{
	private static boolean HAS_JDK_IDENTITY_MAP = true;
	private static final String CLASS_NAME = "java.util.IdentityHashMap";
	private static Class JDK_IDENTITY_MAP;

	static
	{
		try
		{
			JDK_IDENTITY_MAP = ClassHelper.getClassLoader().loadClass(CLASS_NAME);
		}
		catch (ClassNotFoundException e)
		{
			HAS_JDK_IDENTITY_MAP = false;
		}
	}
	
	private IdentityMapFactory() {}

	public static Map getIdentityMap()
	{
		Map retval = null;
		if (HAS_JDK_IDENTITY_MAP)
		{
			try
			{
				retval = (Map) JDK_IDENTITY_MAP.newInstance();
			}
			catch (InstantiationException e)
			{
				HAS_JDK_IDENTITY_MAP = false;
			}
			catch (IllegalAccessException e)
			{
				HAS_JDK_IDENTITY_MAP = false;
			}
		}
		if (!HAS_JDK_IDENTITY_MAP)
		{
			retval = new org.apache.ojb.broker.util.IdentityHashMap();
		}
		return retval;
	}
}
