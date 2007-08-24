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

import java.lang.reflect.Constructor;

import org.apache.ojb.broker.metadata.ClassNotPersistenceCapableException;

/**
 * This class helps us to construct new instances.
 * We don't want to rely on public default constructors and
 * have to try hard to also use private or protected constructors.
 *
 * @author Thomas Mahler
 * @author Lance Eason
 *
 * @version $Id: ConstructorHelper.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class ConstructorHelper
{
    /**
     * represents a zero sized parameter array
     */
    private static final Object[] NO_ARGS = {};

    /**
     * no public constructor, please use the static method only.
     */
    private ConstructorHelper()
    {
    }

    /**
     * create a new instance of class clazz.
     * first use the public default constructor.
     * If this fails also try to use protected an private constructors.
     * @param clazz the class to instantiate
     * @return the fresh instance of class clazz
     * @throws InstantiationException
     */
    public static Object instantiate(Class clazz) throws InstantiationException
    {
        Object result = null;
        try
        {
            result = ClassHelper.newInstance(clazz);
        }
        catch(IllegalAccessException e)
        {
            try
            {
                result = ClassHelper.newInstance(clazz, true);
            }
            catch(Exception e1)
            {
                throw new ClassNotPersistenceCapableException("Can't instantiate class '"
                        + (clazz != null ? clazz.getName() : "null")
                        + "', message was: " + e1.getMessage() + ")", e1);
            }
        }
        return result;
    }

    /**
     * create a new instance of the class represented by the no-argument constructor provided
     * @param constructor the zero argument constructor for the class
     * @return a new instance of the class
     * @throws InstantiationException
     * @throws ClassNotPersistenceCapableException if the constructor is null or there is an
     *   exception while trying to create a new instance
     */
    public static Object instantiate(Constructor constructor) throws InstantiationException
    {
        if(constructor == null)
        {
            throw new ClassNotPersistenceCapableException(
                    "A zero argument constructor was not provided!");
        }

        Object result = null;
        try
        {
            result = constructor.newInstance(NO_ARGS);
        }
        catch(InstantiationException e)
        {
            throw e;
        }
        catch(Exception e)
        {
            throw new ClassNotPersistenceCapableException("Can't instantiate class '"
                    + (constructor != null ? constructor.getDeclaringClass().getName() : "null")
                    + "' with given constructor: " + e.getMessage(), e);
        }
        return result;
    }
}
