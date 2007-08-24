package xdoclet.modules.ojb.constraints;

import java.util.ArrayList;
import java.util.Iterator;

import xdoclet.modules.ojb.model.*;
import xjavadoc.XClass;

/* Copyright 2004-2005 The Apache Software Foundation
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

/**
 * Helper class for functionality related to inheritance between classes/interfaces.
 * 
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class InheritanceHelper
{
    /**
     * Retrieves the class object for the class with the given name.
     * 
     * @param name The class name
     * @return The class object
     * @throws ClassNotFoundException If the class is not on the classpath (the exception message contains the class name)
     */
    public static Class getClass(String name) throws ClassNotFoundException
    {
        try
        {
            return Class.forName(name);
        }
        catch (ClassNotFoundException ex)
        {
            throw new ClassNotFoundException(name);
        }
    }
    /**
     * Determines whether the given type is the same or a sub type of the other type.
     *  
     * @param type               The type
     * @param baseType           The possible base type
     * @param checkActualClasses Whether to use the actual classes for the test 
     * @return <code>true</code> If <code>type</code> specifies the same or a sub type of <code>baseType</code>
     * @throws ClassNotFoundException If the two classes are not on the classpath
     */
    public boolean isSameOrSubTypeOf(XClass type, String baseType, boolean checkActualClasses) throws ClassNotFoundException
    {
        String qualifiedBaseType = baseType.replace('$', '.');

        if (type.getQualifiedName().equals(qualifiedBaseType))
        {
            return true;
        }

        // first search via XDoclet
        ArrayList queue      = new ArrayList();
        boolean   canSpecify = false;
        XClass    curType;

        queue.add(type);
        while (!queue.isEmpty())
        {
            curType = (XClass)queue.get(0);
            queue.remove(0);
            if (qualifiedBaseType.equals(curType.getQualifiedName()))
            {
                return true;
            }
            if (curType.getInterfaces() != null)
            {
                for (Iterator it = curType.getInterfaces().iterator(); it.hasNext(); )
                {
                    queue.add(it.next());
                }
            }
            if (!curType.isInterface())
            {
                if (curType.getSuperclass() != null)
                {
                    queue.add(curType.getSuperclass());
                }
            }
        }

        // if not found, we try via actual classes
        return checkActualClasses ? isSameOrSubTypeOf(type.getQualifiedName(), qualifiedBaseType) : false;
    }

    /**
     * Determines whether the given type is the same or a sub type of the other type.
     *  
     * @param type               The type
     * @param baseType           The possible base type
     * @param checkActualClasses Whether to use the actual classes for the test 
     * @return <code>true</code> If <code>type</code> specifies the same or a sub type of <code>baseType</code>
     * @throws ClassNotFoundException If the two classes are not on the classpath
     */
    public boolean isSameOrSubTypeOf(ClassDescriptorDef type, String baseType, boolean checkActualClasses) throws ClassNotFoundException
    {
        if (type.getQualifiedName().equals(baseType.replace('$', '.')))
        {
            return true;
        }
        else if (type.getOriginalClass() != null)
        {
            return isSameOrSubTypeOf(type.getOriginalClass(), baseType, checkActualClasses);
        }
        else
        {
            return checkActualClasses ? isSameOrSubTypeOf(type.getName(), baseType) : false;
        }
    }

    /**
     * Determines whether the given type is the same or a sub type of the other type.
     *  
     * @param type     The type
     * @param baseType The possible base type
     * @return <code>true</code> If <code>type</code> specifies the same or a sub type of <code>baseType</code>
     * @throws ClassNotFoundException If the two classes are not on the classpath
     */
    public boolean isSameOrSubTypeOf(String type, String baseType) throws ClassNotFoundException
    {
        return type.replace('$', '.').equals(baseType.replace('$', '.')) ? true : isSameOrSubTypeOf(getClass(type), baseType);
    }

    /**
     * Determines whether the given type is the same or a sub type of the other type.
     *  
     * @param type     The type
     * @param baseType The possible base type
     * @return <code>true</code> If <code>type</code> specifies the same or a sub type of <code>baseType</code>
     * @throws ClassNotFoundException If the two classes are not on the classpath
     */
    public boolean isSameOrSubTypeOf(Class type, String baseType) throws ClassNotFoundException
    {
        return type.getName().equals(baseType.replace('$', '.')) ? true : getClass(baseType).isAssignableFrom(type);
    }
}
