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

import java.lang.reflect.Field;
import java.util.List;

import org.apache.ojb.broker.metadata.MetadataException;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.core.proxy.ProxyHelper;

/**
 * This {@link org.apache.ojb.broker.metadata.fieldaccess.PersistentField} implementation
 * is the high-speed version of the access strategies.
 * <br/>
 * It does not cooperate with an AccessController,
 * but accesses the fields directly. This implementation persistent
 * attributes don't need getters and setters
 * and don't have to be declared public or protected. Only the the
 * metadata field names have to match the class fields.
 *
 * @version $Id: PersistentFieldDirectImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PersistentFieldDirectImpl extends PersistentFieldBase
{
    private static final long serialVersionUID = -5458024240998909205L;

    private transient boolean isInitialized;
    private transient List fieldsList;
    private transient Field field;
    private transient boolean nonNested;

    public PersistentFieldDirectImpl()
    {
    }

    public PersistentFieldDirectImpl(Class type, String fieldname)
    {
        super(type, fieldname);
    }

    public Class getType()
    {
        return getField().getType();
    }

    /**
     * Returns the underlying field object.
     * If parameter <tt>setAccessible</tt> is true the
     * field access checking was suppressed.
     */
    protected Field getField()
    {
        // make sure class was initialized
        if (!isInitialized)
        {
            /*
            first we build a graph of fields for nested fields support,
            but for best performance on non-nested fields we also keep
            the latest field separate and set a 'is nested' flag.
            */
            fieldsList = getFieldGraph(makeAccessible());
            field = (Field) fieldsList.get(fieldsList.size() - 1);
            nonNested = fieldsList.size() == 1;
            isInitialized = true;
        }
        return field;
    }

    private List getFieldsList()
    {
        // make sure class was initialized
        if (!isInitialized) getField();
        return fieldsList;
    }

    protected boolean isNestedField()
    {
        return !nonNested;
    }

    /**
     * do not override this method, have a look at {@link #setValueFor(java.lang.reflect.Field, Object, Object)}
     */
    public void set(Object target, Object value) throws MetadataException
    {
        // if target null, we have nothing to do
        if(target == null) return;
        Object current = target;
        if (isNestedField())
        {
            List fields = getFieldsList();
            int size = fields.size() - 1;
            Field field;
            for (int i = 0; i < size; i++)
            {
                field = (Field) fields.get(i);
                Object attribute;
                try
                {
                    attribute = getValueFrom(field, current);
                }
                catch (Exception e)
                {
                    throw new MetadataException("Can't read field '" + field.getName() + "' of type " + field.getType().getName(), e);
                }
                if (attribute != null || value != null)
                {
                    // if the intermediary nested object is null, we have to create
                    // a new instance to set the value
                    if (attribute == null)
                    {
                        try
                        {
                            attribute = ClassHelper.newInstance(field.getType());
                        }
                        catch (Exception e)
                        {
                            throw new MetadataException("Can't create nested object of type '"
                                    + field.getType() + "' for field '"
                                    + field.getName() + "'", e);
                        }
                    }
                    try
                    {
                        //field.set(current, attribute);
                        setValueFor(field, current, attribute);
                    }
                    //catch (IllegalAccessException e)
                    catch (Exception e)
                    {
                        throw new MetadataException("Can't set nested object of type '"
                                    + field.getType() + "' for field '"
                                    + field.getName() + "'", e);
                    }
                }
                else
                {
                    return;
                }
                current = attribute;
            }
        }
        setValueFor(getField(), current, value);
    }

    /**
     * do not override this method, have a look at {@link #getValueFrom(java.lang.reflect.Field, Object)}
     */
    public Object get(Object target) throws MetadataException
    {
        Object result = target;
        if (isNestedField())
        {
            List fields = getFieldsList();
            for (int i = 0; i < fields.size(); i++)
            {
                if (result == null) break;
                result = getValueFrom((Field) fields.get(i), result);
            }
        }
        else
        {
            result = result != null ? getValueFrom(getField(), result) : null;
        }
        return result;
    }



    protected Object getValueFrom(Field field, Object target)
    {
        try
        {
            return field.get(ProxyHelper.getRealObject(target));
            // TODO: don't make costly proxy test on field level use
            // return field.get(target);
        }
        catch (IllegalAccessException e)
        {
            throw new MetadataException(
                    "IllegalAccess error reading field: " +
                    (field != null ? field.getName() : null) + " from object: "
                    + (target != null ? target.getClass().getName() : null), e);
        }
        catch (IllegalArgumentException e)
        {
            throw new MetadataException(
                    "IllegalArgument error reading field: " +
                    buildErrorGetMsg(target, field), e);
        }
    }

    protected void setValueFor(Field field, Object target, final Object value)
    {
        try
        {
            /**
             * MBAIRD
             * we need to be able to set values to null. We can only set something to null if
             * the type is not a primitive (assignable from Object).
             */
            // thanks to Tomasz Wysocki for this trick
            if ((value != null) || !field.getType().isPrimitive())
            {
                field.set(ProxyHelper.getRealObject(target), value);
                // TODO: don't make costly proxy test on field level use
                // field.set(target, value);
            }
        }
        catch (NullPointerException ignored)
        {
            getLog().info("Target object '" + (target != null ? target.getClass().getName() : null)
                    + "' for field '" + (field != null ? field.getName() : null)
                    + "' of type '" + (field != null ? field.getType().getName() : null)
                    + "' seems to be null. Can't write into null.", ignored);
        }
        catch (Exception e)
        {
            getLog().error("while set field: " + buildErrorSetMsg(target, value, field));
            throw new MetadataException("IllegalAccess error setting field:" +
                    (field != null ? field.getName() : null) + " in object:" + target.getClass().getName(), e);
        }
    }

    /**
     * This implementation returns always 'true'.
     */
    protected boolean makeAccessible()
    {
        return true;
    }

    /**
     * Always returns 'false'.
     * @see org.apache.ojb.broker.metadata.fieldaccess.PersistentField#usesAccessorsAndMutators
     */
    public boolean usesAccessorsAndMutators()
    {
        return false;
    }
}
