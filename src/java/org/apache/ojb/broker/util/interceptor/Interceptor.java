package org.apache.ojb.broker.util.interceptor;

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

import java.lang.reflect.Method;

//#ifdef JDK13
import java.lang.reflect.InvocationHandler;
//#else
/*
import com.develop.java.lang.reflect.InvocationHandler; 
*/
//#endif
/**
 * @author Thomas Mahler
 */
public abstract class Interceptor implements InvocationHandler
{

	private Object realSubject = null;

	/**
	 * @see com.develop.java.lang.reflect.InvocationHandler#invoke(Object, Method, Object[])
	 */
	public Object invoke(Object proxy, Method methodToBeInvoked, Object[] args) throws Throwable
	{
		beforeInvoke(proxy, methodToBeInvoked, args);
		Object result = null;
		result = doInvoke(proxy, methodToBeInvoked, args);
		afterInvoke(proxy, methodToBeInvoked, args);
		return result;
	}

	/**
	 * this method will be invoked before methodToBeInvoked is invoked
	 */
	protected abstract void beforeInvoke(Object proxy, Method methodToBeInvoked, Object[] args)
		throws Throwable;

	/**
	 * this method will be invoked after methodToBeInvoked is invoked
	 */
	protected abstract void afterInvoke(Object proxy, Method methodToBeInvoked, Object[] args)
		throws Throwable;

	/**
	 * this method will be invoked after methodToBeInvoked is invoked
	 */
	protected Object doInvoke(Object proxy, Method methodToBeInvoked, Object[] args)
		throws Throwable
	{
		Method m =
			getRealSubject().getClass().getMethod(
				methodToBeInvoked.getName(),
				methodToBeInvoked.getParameterTypes());
		return m.invoke(getRealSubject(), args);
	}

	/**
	 * Returns the realSubject.
	 * @return Object
	 */
	public Object getRealSubject()
	{
		return realSubject;
	}

	/**
	 * Sets the realSubject.
	 * @param realSubject The realSubject to set
	 */
	public void setRealSubject(Object realSubject)
	{
		this.realSubject = realSubject;
	}

}
