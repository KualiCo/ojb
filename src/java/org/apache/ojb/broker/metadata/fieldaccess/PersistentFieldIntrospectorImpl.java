package org.apache.ojb.broker.metadata.fieldaccess;

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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.MetadataException;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.logging.Logger;

/**
 * A {@link PersistentField} implementation using
 * JavaBeans compliant calls only to access persistent attributes.
 * No Reflection is needed. But for each attribute xxx there must be
 * public getXxx() and setXxx() methods. In metadata the field name must be
 * the bean compliant 'xxx'.
 *
 * @version $Id: PersistentFieldIntrospectorImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PersistentFieldIntrospectorImpl extends PersistentFieldBase
{
    private static final long serialVersionUID = 8805309492150404444L;
    private Class type;
    private transient List propertyGraph;

    public PersistentFieldIntrospectorImpl()
    {
        super();
    }

    public PersistentFieldIntrospectorImpl(Class aClass, String aPropertyName)
    {
        super(aClass, aPropertyName);
    }

    public Class getType()
    {
        if (type == null)
        {
            type = getPropertyDescriptor().getPropertyType();
        }
        return type;
    }

    public void set(Object target, Object value) throws MetadataException
    {
        if(target == null) return;
        List propertyDescriptors = getPropertyGraph();
        int size = propertyDescriptors.size() - 1;
        PropertyDescriptor pd;
        for (int i = 0; i < size; i++)
        {
            Object attribute;
            pd = (PropertyDescriptor) propertyDescriptors.get(i);
            attribute = getValueFrom(pd, target);
            if (attribute != null || value != null)
            {
                if (attribute == null)
                {
                    try
                    {
                        attribute = ClassHelper.newInstance(pd.getPropertyType());
                    }
                    catch (Exception e)
                    {
                        throw new MetadataException("Can't instantiate nested object of type '"
                                + pd.getPropertyType() + "' for field '"
                                + pd.getName() + "'", e);
                    }
                }
                setValueFor(pd, target, attribute);
            }
            else
            {
                return;
            }
            target = attribute;
        }
        pd = (PropertyDescriptor) propertyDescriptors.get(size);
        setValueFor(pd, target, value);
    }

    public Object get(Object target) throws MetadataException
    {
        List propertyDescriptors = getPropertyGraph();
        for (int i = 0; i < propertyDescriptors.size(); i++)
        {
            PropertyDescriptor pd = (PropertyDescriptor) propertyDescriptors.get(i);
            target = getValueFrom(pd, target);
            if (target == null) break;
        }
        return target;
    }

    private Object getValueFrom(PropertyDescriptor pd, Object target)
    {
        if (target == null) return null;
        Method m = pd.getReadMethod();
        if (m != null)
        {
            try
            {
                return m.invoke(ProxyHelper.getRealObject(target), null);
            }
            catch (Throwable e)
            {
                logProblem(pd, target, null, "Can't read value from given object");
                throw new MetadataException("Error invoking method:" + m.getName() + " in object " + target.getClass().getName(), e);
            }
        }
        else
        {
            throw new MetadataException("Can't get ReadMethod for property:" + pd.getName() + " in object " + target.getClass().getName());
        }
    }

    private void setValueFor(PropertyDescriptor pd, Object target, Object value)
    {
        Method m = pd.getWriteMethod();
        Object[] args = {value};
        if (m != null)
        {
            try
            {
                /**
                 * MBAIRD: it is safe to call getParameterTypes()[0] because this is
                 * the "set" method and it needs to take one parameter only.
                 * we need to be able to set values to null. We can only set something to null if
                 * the type is not a primitive (assignable from Object).
                 */
                if ((value != null) || !m.getParameterTypes()[0].isPrimitive())
                {
                    m.invoke(ProxyHelper.getRealObject(target), args);
                }
            }
            catch (Throwable e)
            {
                logProblem(pd, target, value, "Can't set value on given object.");
                throw new MetadataException("Error invoking method:" + m.getName() + " in object:" + target.getClass().getName(), e);
            }
        }
        else
        {
            throw new MetadataException("Can't get WriteMethod for property:" + pd.getName() + " in object:" + target.getClass().getName());
        }
    }

    private List getPropertyGraph()
    {
        if (propertyGraph == null)
        {
            propertyGraph = buildPropertyGraph();
        }
        return propertyGraph;
    }

    private List buildPropertyGraph()
    {
        List result = new ArrayList();
        String[] fields = StringUtils.split(getName(), PATH_TOKEN);
        PropertyDescriptor pd = null;
        for (int i = 0; i < fields.length; i++)
        {
            String fieldName = fields[i];
            if (pd == null)
            {
                pd = findPropertyDescriptor(getDeclaringClass(), fieldName);
            }
            else
            {
                pd = findPropertyDescriptor(pd.getPropertyType(), fieldName);
            }
            result.add(pd);
        }
        return result;
    }

    /**
     * Get the PropertyDescriptor for aClass and aPropertyName
     */
    protected static PropertyDescriptor findPropertyDescriptor(Class aClass, String aPropertyName)
    {
        BeanInfo info;
        PropertyDescriptor[] pd;
        PropertyDescriptor descriptor = null;

        try
        {
            info = Introspector.getBeanInfo(aClass);
            pd = info.getPropertyDescriptors();
            for (int i = 0; i < pd.length; i++)
            {
                if (pd[i].getName().equals(aPropertyName))
                {
                    descriptor = pd[i];
                    break;
                }
            }
            if (descriptor == null)
            {
                /*
				 * Daren Drummond: 	Throw here so we are consistent
				 * 					with PersistentFieldDefaultImpl.
				 */
                throw new MetadataException("Can't find property " + aPropertyName + " in " + aClass.getName());
            }
            return descriptor;
        }
        catch (IntrospectionException ex)
        {
            /*
			 * Daren Drummond: 	Throw here so we are consistent
			 * 					with PersistentFieldDefaultImpl.
			 */
            throw new MetadataException("Can't find property " + aPropertyName + " in " + aClass.getName(), ex);
        }
    }

    /**
     * Returns the PropertyDescriptor.
     *
     * @return java.beans.PropertyDescriptor
     */
    protected PropertyDescriptor getPropertyDescriptor()
    {
        return (PropertyDescriptor) getPropertyGraph().get(getPropertyGraph().size() - 1);
    }

    /**
     * This implementation returns always 'false'.
     */
    public boolean makeAccessible()
    {
        return false;
    }

    /**
     * Always returns 'false'.
     *
     * @see PersistentField#usesAccessorsAndMutators
     */
    public boolean usesAccessorsAndMutators()
    {
        return true;
    }

    /**
     * Let's give the user some hints as to what could be wrong.
     */
    protected void logProblem(PropertyDescriptor pd, Object anObject, Object aValue, String msg)
    {
        Logger logger = getLog();
        logger.error("Error in [PersistentFieldPropertyImpl], " + msg);
        logger.error("Declaring class [" + getDeclaringClass().getName() + "]");
        logger.error("Property Name [" + getName() + "]");
        logger.error("Property Type [" + pd.getPropertyType().getName() + "]");

        if (anObject != null)
        {
            logger.error("anObject was class [" + anObject.getClass().getName() + "]");
        }
        else
        {
            logger.error("anObject was null");
        }
        if (aValue != null)
        {
            logger.error("aValue was class [" + aValue.getClass().getName() + "]");
        }
        else
        {
            logger.error("aValue was null");
        }
    }
}
