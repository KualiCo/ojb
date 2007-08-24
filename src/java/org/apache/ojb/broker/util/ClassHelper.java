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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;

import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ClassNotPersistenceCapableException;

/**
 * Helper class with static methods for java class, method, and field handling.
 *
 * @version $Id: ClassHelper.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class ClassHelper
{
    /** Arguments for invoking a default or no-arg constructor */
    private static final Object[] NO_ARGS = {};
    /** Parameter types of a default/no-arg constructor */
    private static final Class[] NO_ARGS_CLASS = {};

    /** The class loader currently used by OJB */
    private static ClassLoader _classLoader = null;
    /** A mutex for changing the class loader */
    private static Object      _mutex       = new Object();
    
    /**
     * Prevents instatiation.
     */
    private ClassHelper()
    {
    }

    /**
     * Sets the classloader to be used by OJB. This can be set by external
     * application that need to pass a specific classloader to OJB.
     * 
     * @param loader The class loader. If <code>null</code> then OJB will use
     *               the class loader of the current thread
     */
    public static void setClassLoader(ClassLoader loader)
    {
        synchronized (_mutex)
        {
            _classLoader = loader;
        }
    }

    /**
     * Returns the class loader currently used by OJB. Defaults to the class loader of
     * the current thread (<code>Thread.currentThread().getContextClassLoader()</code>)
     * if not set differently. If class loader is not explicitly set and the loader for
     * the current thread context is null, the JVM default class loader will be used.
     * 
     * @return The classloader used by OJB
     * @see #setClassLoader(ClassLoader)
     */
    public static ClassLoader getClassLoader()
    {
        final ClassLoader ojbClassLoader;
        if (_classLoader != null)
        {
            ojbClassLoader = _classLoader;
        }
        else
        {
            final ClassLoader threadCtxtClassLoader;
            threadCtxtClassLoader = Thread.currentThread().getContextClassLoader();
            if (threadCtxtClassLoader == null)
            {
                // mkalen: happens only in "obscure" situations using JNI, revert to system CL
                ojbClassLoader = ClassLoader.getSystemClassLoader();
            }
            else
            {
                ojbClassLoader = threadCtxtClassLoader;
            }
        }
        return ojbClassLoader;
    }

    /**
     * Determines the url of the indicated resource using the currently set class loader.
     * 
     * @param name The resource name
     * @return The resource's url
     */
    public static URL getResource(String name)
    {
        return getClassLoader().getResource(name);
    }
    
    /**
     * Retrieves the class object for the given qualified class name.
     * 
     * @param className  The qualified name of the class
     * @param initialize Whether the class shall be initialized
     * @return The class object
     */
    public static Class getClass(String className, boolean initialize) throws ClassNotFoundException
    {
        return Class.forName(className, initialize, getClassLoader());
    }

    /**
     * Returns a new instance of the given class, using the default or a no-arg constructor.
     * 
     * @param target The class to instantiate
     * @return The instance
     */
    public static Object newInstance(Class target) throws InstantiationException,
                                                          IllegalAccessException
    {
        return target.newInstance();
    }

    /**
     * Returns a new instance of the given class, using the default or a no-arg constructor.
     * This method can also use private no-arg constructors if <code>makeAccessible</code>
     * is set to <code>true</code> (and there are no other security constraints).
     *  
     * @param target         The class to instantiate
     * @param makeAccessible If the constructor shall be made accessible prior to using it
     * @return The instance
     */
    public static Object newInstance(Class target, boolean makeAccessible) throws InstantiationException,
                                                                                  IllegalAccessException
    {
        if (makeAccessible)
        {
            try
            {
                return newInstance(target, NO_ARGS_CLASS, NO_ARGS, makeAccessible);
            }
            catch (InvocationTargetException e)
            {
                throw new OJBRuntimeException("Unexpected exception while instantiate class '"
                        + target + "' with default constructor", e);
            }
            catch (NoSuchMethodException e)
            {
                throw new OJBRuntimeException("Unexpected exception while instantiate class '"
                        + target + "' with default constructor", e);
            }
        }
        else
        {
            return target.newInstance();
        }
    }

    /**
     * Returns a new instance of the given class, using the constructor with the specified parameter types.
     * 
     * @param target The class to instantiate
     * @param types  The parameter types
     * @param args   The arguments
     * @return The instance
     */
    public static Object newInstance(Class target, Class[] types, Object[] args) throws InstantiationException,
                                                                                        IllegalAccessException,
                                                                                        IllegalArgumentException,
                                                                                        InvocationTargetException,
                                                                                        NoSuchMethodException,
                                                                                        SecurityException
    {
        return newInstance(target, types, args, false);
    }

    /**
     * Returns a new instance of the given class, using the constructor with the specified parameter types.
     * This method can also use private constructors if <code>makeAccessible</code> is set to
     * <code>true</code> (and there are no other security constraints).
     * 
     * @param target         The class to instantiate
     * @param types          The parameter types
     * @param args           The arguments
     * @param makeAccessible If the constructor shall be made accessible prior to using it
     * @return The instance
     */
    public static Object newInstance(Class target, Class[] types, Object[] args, boolean makeAccessible) throws InstantiationException,
                                                                                                                IllegalAccessException,
                                                                                                                IllegalArgumentException,
                                                                                                                InvocationTargetException,
                                                                                                                NoSuchMethodException,
                                                                                                                SecurityException
    {
        Constructor con;

        if (makeAccessible)
        {
            con = target.getDeclaredConstructor(types);
            if (makeAccessible && !con.isAccessible())
            {
                con.setAccessible(true);
            }
        }
        else
        {
            con = target.getConstructor(types);
        }
        return con.newInstance(args);
    }

    /**
     * Determines the method with the specified signature via reflection look-up.
     * 
     * @param clazz      The java class to search in
     * @param methodName The method's name
     * @param params     The parameter types
     * @return The method object or <code>null</code> if no matching method was found
     */
    public static Method getMethod(Class clazz, String methodName, Class[] params)
    {
        try
        {
            return clazz.getMethod(methodName, params);
        }
        catch (Exception ignored)
        {}
        return null;
    }

    /**
     * Determines the field via reflection look-up.
     * 
     * @param clazz     The java class to search in
     * @param fieldName The field's name
     * @return The field object or <code>null</code> if no matching field was found
     */
    public static Field getField(Class clazz, String fieldName)
    {
        try
        {
            return clazz.getField(fieldName);
        }
        catch (Exception ignored)
        {}
        return null;
    }


    // *******************************************************************
    // Convenience methods
    // *******************************************************************

    /**
     * Convenience method for {@link #getClass(String, boolean) getClass(name, true)}
     * 
     * @param name The qualified class name
     * @return The class object
     */
    public static Class getClass(String name) throws ClassNotFoundException
    {
        return getClass(name, true);
    }


    /**
     * Returns a new instance of the class with the given qualified name using the default or
     * or a no-arg constructor.
     * 
     * @param className The qualified name of the class to instantiate
     */
    public static Object newInstance(String className) throws InstantiationException,
                                                              IllegalAccessException,
                                                              ClassNotFoundException
    {
        return newInstance(getClass(className));
    }

    /**
     * Returns a new instance of the class with the given qualified name using the constructor with
     * the specified signature.
     * 
     * @param className The qualified name of the class to instantiate
     * @param types     The parameter types
     * @param args      The arguments
     * @return The instance
     */
    public static Object newInstance(String className, Class[] types, Object[] args) throws InstantiationException,
                                                                                            IllegalAccessException,
                                                                                            IllegalArgumentException,
                                                                                            InvocationTargetException,
                                                                                            NoSuchMethodException,
                                                                                            SecurityException,
                                                                                            ClassNotFoundException
    {
        return newInstance(getClass(className), types, args);
    }

    /**
     * Returns a new instance of the given class using the constructor with the specified parameter.
     * 
     * @param target The class to instantiate
     * @param type   The types of the single parameter of the constructor
     * @param arg    The argument
     * @return The instance
     */
    public static Object newInstance(Class target, Class type, Object arg) throws InstantiationException,
                                                                                  IllegalAccessException,
                                                                                  IllegalArgumentException,
                                                                                  InvocationTargetException,
                                                                                  NoSuchMethodException,
                                                                                  SecurityException
    {
        return newInstance(target, new Class[]{ type }, new Object[]{ arg });
    }

    /**
     * Returns a new instance of the class with the given qualified name using the constructor with
     * the specified parameter.
     * 
     * @param className The qualified name of the class to instantiate
     * @param type      The types of the single parameter of the constructor
     * @param arg       The argument
     * @return The instance
     */
    public static Object newInstance(String className, Class type, Object arg) throws InstantiationException,
                                                                                      IllegalAccessException,
                                                                                      IllegalArgumentException,
                                                                                      InvocationTargetException,
                                                                                      NoSuchMethodException,
                                                                                      SecurityException,
                                                                                      ClassNotFoundException
    {
        return newInstance(className, new Class[]{type}, new Object[]{arg});
    }

    /**
     * Determines the method with the specified signature via reflection look-up.
     * 
     * @param object     The instance whose class is searched for the method
     * @param methodName The method's name
     * @param params     The parameter types
     * @return A method object or <code>null</code> if no matching method was found
     */
    public static Method getMethod(Object object, String methodName, Class[] params)
    {
        return getMethod(object.getClass(), methodName, params);
    }

    /**
     * Determines the method with the specified signature via reflection look-up.
     * 
     * @param className  The qualified name of the searched class
     * @param methodName The method's name
     * @param params     The parameter types
     * @return A method object or <code>null</code> if no matching method was found
     */
    public static Method getMethod(String className, String methodName, Class[] params)
    {
        try
        {
            return getMethod(getClass(className, false), methodName, params);
        }
        catch (Exception ignored)
        {}
        return null;
    }

    /**
     * Builds a new instance for the class represented by the given class descriptor.
     * 
     * @param cld The class descriptor
     * @return The instance
     */
    public static Object buildNewObjectInstance(ClassDescriptor cld)
    {
        Object result = null;

        // If either the factory class and/or factory method is null,
        // just follow the normal code path and create via constructor
        if ((cld.getFactoryClass() == null) || (cld.getFactoryMethod() == null))
        {
            try
            {
                // 1. create an empty Object (persistent classes need a public default constructor)
                Constructor con = cld.getZeroArgumentConstructor();
                if(con == null)
                {
                    throw new ClassNotPersistenceCapableException(
                    "A zero argument constructor was not provided! Class was '" + cld.getClassNameOfObject() + "'");
                }
                result = ConstructorHelper.instantiate(con);
            }
            catch (InstantiationException e)
            {
                throw new ClassNotPersistenceCapableException(
                        "Can't instantiate class '" + cld.getClassNameOfObject()+"'");
            }
        }
        else
        {
            try
            {
                // 1. create an empty Object by calling the no-parms factory method
                Method method = cld.getFactoryMethod();

                if (Modifier.isStatic(method.getModifiers()))
                {
                    // method is static so call it directly
                    result = method.invoke(null, null);
                }
                else
                {
                    // method is not static, so create an object of the factory first
                    // note that this requires a public no-parameter (default) constructor
                    Object factoryInstance = cld.getFactoryClass().newInstance();

                    result = method.invoke(factoryInstance, null);
                }
            }
            catch (Exception ex)
            {
                throw new PersistenceBrokerException("Unable to build object instance of class '"
                        + cld.getClassNameOfObject() + "' from factory:" + cld.getFactoryClass()
                        + "." + cld.getFactoryMethod(), ex);
            }
        }
        return result;
    }
}
