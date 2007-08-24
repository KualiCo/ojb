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
import org.apache.ojb.broker.util.logging.LoggerFactory;


/**
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: TracingInterceptor.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class TracingInterceptor extends Interceptor
{

	public TracingInterceptor(Object instanceToTrace)
	{
		this.setRealSubject(instanceToTrace);
	}

	/**
	 * @see org.apache.ojb.broker.util.InterceptingInvocationHandler#beforeInvoke(Object, Method, Object[])
	 */
	protected void beforeInvoke(Object proxy, Method methodToBeInvoked, Object[] args)
		throws Throwable
	{
		LoggerFactory.getDefaultLogger().info("before: " + getRealSubject().toString() + "." + methodToBeInvoked.getName());
	}

	/**
	 * @see org.apache.ojb.broker.util.InterceptingInvocationHandler#afterInvoke(Object, Method, Object[])
	 */
	protected void afterInvoke(Object proxy, Method methodToBeInvoked, Object[] args)
		throws Throwable
	{
		LoggerFactory.getDefaultLogger().info("after : " + getRealSubject().toString() + "." + methodToBeInvoked.getName());
	}

}
