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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ojb.broker.metadata.MetadataException;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Abstract {@link PersistentField} base implementation class.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PersistentFieldBase.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public abstract class PersistentFieldBase implements PersistentField
{
    public static final String PATH_TOKEN = "::";

    private String fieldName;
    protected Class rootObjectType;

    /**
     * For internal use only!!
     * TODO: Default constructor only needed to support
     * PersistentFieldFactory#usesAccessorsAndMutators()
     * method - find a better solution. Make 'public' to
     * allow helper class to instantiate class.
     */
    public PersistentFieldBase()
    {
    }

    public PersistentFieldBase(Class clazz, String fieldname)
    {
        this.rootObjectType = clazz;
        this.fieldName = fieldname;
    }

    /**
     * A value of true indicates that this field should
     * suppress Java language access checking when it is used.
     */
    protected abstract boolean makeAccessible();

    public String getName()
    {
        return fieldName;
    }

    public Class getDeclaringClass()
    {
        return rootObjectType;
    }

    protected List getFieldGraph(boolean makeAccessible)
    {
        List result = new ArrayList();
        String[] fields = StringUtils.split(getName(), PATH_TOKEN);
        Field fld = null;
        for (int i = 0; i < fields.length; i++)
        {
            String fieldName = fields[i];
            try
            {
                if (fld == null)
                {
                    fld = getFieldRecursive(rootObjectType, fieldName);
                }
                else
                {
                    fld = getFieldRecursive(fld.getType(), fieldName);
                }
                if (makeAccessible)
                {
                    fld.setAccessible(true);
                }
            }
            catch (NoSuchFieldException e)
            {
                throw new MetadataException("Can't find member '"
                        + fieldName + "' in class " + (fld != null ? fld.getDeclaringClass() : rootObjectType), e);
            }
            result.add(fld);
        }
        return result;
    }

    /**
     * try to find a field in class c, recurse through class hierarchy if necessary
     *
     * @throws NoSuchFieldException if no Field was found into the class hierarchy
     */
    private Field getFieldRecursive(Class c, String name) throws NoSuchFieldException
    {
        try
        {
            return c.getDeclaredField(name);
        }
        catch (NoSuchFieldException e)
        {
            // if field  could not be found in the inheritance hierarchy, signal error
            if ((c == Object.class) || (c.getSuperclass() == null) || c.isInterface())
            {
                throw e;
            }
            // if field could not be found in class c try in superclass
            else
            {
                return getFieldRecursive(c.getSuperclass(), name);
            }
        }
    }

    protected Logger getLog()
    {
        return LoggerFactory.getLogger("PersistentField");
    }

    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this);
        buf.append("rootType", rootObjectType);
        buf.append("fieldName", fieldName);
        return buf.toString();
    }

    /**
     * Build a String representation of given arguments.
     */
    protected String buildErrorSetMsg(Object obj, Object value, Field aField)
    {
        String eol = SystemUtils.LINE_SEPARATOR;
        StringBuffer buf = new StringBuffer();
        buf
                .append(eol + "[try to set 'object value' in 'target object'")
                .append(eol + "target obj class: " + (obj != null ? obj.getClass().getName() : null))
                .append(eol + "target field name: " + (aField != null ? aField.getName() : null))
                .append(eol + "target field type: " + (aField != null ? aField.getType() : null))
                .append(eol + "target field declared in: " + (aField != null ? aField.getDeclaringClass().getName() : null))
                .append(eol + "object value class: " + (value != null ? value.getClass().getName() : null))
                .append(eol + "object value: " + (value != null ? value : null))
                .append(eol + "]");
        return buf.toString();
    }

    /**
     * Build a String representation of given arguments.
     */
    protected String buildErrorGetMsg(Object obj, Field aField)
    {
        String eol = SystemUtils.LINE_SEPARATOR;
        StringBuffer buf = new StringBuffer();
        buf
                .append(eol + "[try to read from source object")
                .append(eol + "source obj class: " + (obj != null ? obj.getClass().getName() : null))
                .append(eol + "target field name: " + (aField != null ? aField.getName() : null))
                .append(eol + "target field type: " + (aField != null ? aField.getType() : null))
                .append(eol + "target field declared in: " + (aField != null ? aField.getDeclaringClass().getName() : null))
                .append(eol + "]");
        return buf.toString();
    }
}
