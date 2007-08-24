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

//#ifdef JDK13
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
//#else
/*
import net.sf.cglib.proxy.Proxy;
import net.sf.cglib.proxy.InvocationHandler;
*/
//#endif

import java.util.HashMap;

import org.apache.ojb.broker.util.configuration.Configurable;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.ClassHelper;

/**
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: InterceptorFactory.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class InterceptorFactory implements Configurable
{

	private static InterceptorFactory instance = null;

	private Class interceptorClassToBeUsed = null;

	/**
	 * Returns the instance.
	 * @return InterceptorFactory
	 */
	public static InterceptorFactory getInstance()
	{
		if (instance == null)
		{
			instance = new InterceptorFactory();
			OjbConfigurator.getInstance().configure(instance);
		}
		return instance;
	}


	/**
	 * @see org.apache.ojb.broker.util.configuration.Configurable#configure(Configuration)
	 */
	public void configure(Configuration pConfig) throws ConfigurationException
	{
		Class clazz = pConfig.getClass("InterceptorClass", Object.class);
		if(!clazz.equals(Object.class)) setInterceptorClassToBeUsed(clazz);
	}

	public Object createInterceptorFor(Object instanceToIntercept)
	{
		if (getInterceptorClassToBeUsed() != null)
		{
			try
			{


//				Class[] parameterTypes = {Object.class};
//				Object[] parameters = {instanceToIntercept};
//              Constructor constructor = getInterceptorClassToBeUsed().getConstructor(parameterTypes);
//				InvocationHandler handler = (InvocationHandler) constructor.newInstance(parameters);
                // use helper class to instantiate
                InvocationHandler handler = (InvocationHandler) ClassHelper.newInstance(
                                            getInterceptorClassToBeUsed(), Object.class, instanceToIntercept);
				Class[] interfaces = computeInterfaceArrayFor(instanceToIntercept.getClass());
				Object result =
					Proxy.newProxyInstance(
						ClassHelper.getClassLoader(),
						interfaces,
						handler);
				return result;
			}
			catch (Throwable t)
			{
				LoggerFactory.getDefaultLogger().error("can't use Interceptor " + getInterceptorClassToBeUsed().getName() +
					"for " + instanceToIntercept.getClass().getName(), t);
				return instanceToIntercept;
			}
		}
		else
		{
			return instanceToIntercept;
		}
	}



	public Class[] computeInterfaceArrayFor(Class clazz)
	{
		Class superClass = clazz;
		Class[] interfaces = clazz.getInterfaces();

		// clazz can be an interface itself and when getInterfaces()
		// is called on an interface it returns only the extending
		// interfaces, not the interface itself.
		if (clazz.isInterface())
		{
			Class[] tempInterfaces = new Class[interfaces.length + 1];
			tempInterfaces[0] = clazz;

			System.arraycopy(interfaces, 0, tempInterfaces, 1, interfaces.length);
			interfaces = tempInterfaces;
		}

		// add all interfaces implemented by superclasses to the interfaces array
		while ((superClass = superClass.getSuperclass()) != null)
		{
			Class[] superInterfaces = superClass.getInterfaces();
			Class[] combInterfaces = new Class[interfaces.length + superInterfaces.length];
			System.arraycopy(interfaces, 0, combInterfaces, 0, interfaces.length);
			System.arraycopy(
				superInterfaces,
				0,
				combInterfaces,
				interfaces.length,
				superInterfaces.length);
			interfaces = combInterfaces;
		}

		/**
		 * Must remove duplicate interfaces before calling Proxy.getProxyClass().
		 * Duplicates can occur if a subclass re-declares that it implements
		 * the same interface as one of its ancestor classes.
		**/
		HashMap unique = new HashMap();
		for (int i = 0; i < interfaces.length; i++)
		{
			unique.put(interfaces[i].getName(), interfaces[i]);
		}
		interfaces = (Class[]) unique.values().toArray(new Class[unique.size()]);

		return interfaces;
	}



	/**
	 * Returns the interceptorClassToBeUsed.
	 * @return Class
	 */
	public Class getInterceptorClassToBeUsed()
	{
		return interceptorClassToBeUsed;
	}

	/**
	 * Sets the interceptorClassToBeUsed.
	 * @param interceptorClassToBeUsed The interceptorClassToBeUsed to set
	 */
	public void setInterceptorClassToBeUsed(Class interceptorClassToBeUsed)
	{
		this.interceptorClassToBeUsed = interceptorClassToBeUsed;
	}

}
