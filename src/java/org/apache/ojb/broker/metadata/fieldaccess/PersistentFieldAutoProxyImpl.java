package org.apache.ojb.broker.metadata.fieldaccess;

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

import java.io.Serializable;

import org.apache.ojb.broker.metadata.MetadataException;
import org.apache.ojb.broker.util.ClassHelper;

/**
 * PeristentField implementation that attempts to detect the nature of
 * the field it is persisting.
 * <p>
 * First checks to see if it is a Field, then Property, then DynaBean
 * <p>
 * It will match in that order.
 */
public class PersistentFieldAutoProxyImpl extends PersistentFieldBase
{
    static final long serialVersionUID = 6286229945422476325L;

    /**
     * Define the number and ordering of the used {@link PersistentField}
     * implementaions. Override this field to add new classes or to change
     * setection order.
     */
    protected Class[] persistentFieldClasses = new Class[]{
        PersistentFieldDirectImpl.class
        , PersistentFieldIntrospectorImpl.class
        , PersistentFieldPrivilegedImpl.class
        , PersistentFieldDynaBeanImpl.class};

    private PersistentField currentPF;
    private ExceptionWrapper latestException;
    int index = 0;

    public PersistentFieldAutoProxyImpl()
    {
    }

    public PersistentFieldAutoProxyImpl(Class clazz, String fieldname)
    {
        super(clazz, fieldname);
    }

    private PersistentField getCurrent()
    {
        if (currentPF == null)
        {
            if(index >= persistentFieldClasses.length)
            {
                index = 0;
                currentPF = null;
                throw new AutoDetectException("Can't autodetect valid PersistentField implementation: "
                        + latestException.message, latestException.exception);
            }
            try
            {
                currentPF = createPersistentFieldForIndex();
            }
            catch (Exception e)
            {
                throw new AutoDetectException("Can't create instance for " + persistentFieldClasses[index], e);
            }
        }
        return currentPF;
    }

    private void handleException(String message, Exception e)
    {
        latestException = new ExceptionWrapper(message, e);
        currentPF = null;
        ++index;
    }


    public Object get(Object anObject) throws MetadataException
    {
        try
        {
            return getCurrent().get(anObject);
        }
        catch (Exception e)
        {
            if(e instanceof AutoDetectException)
            {
                throw (MetadataException) e;
            }
            else
            {
                handleException("Can't extract field value for field " + getName()
                        + " from object " + (anObject != null ? anObject.getClass() : null), e);
                return get(anObject);
            }
        }
    }

    public void set(Object obj, Object value) throws MetadataException
    {
        try
        {
            getCurrent().set(obj, value);
        }
        catch (Exception e)
        {
            if(e instanceof AutoDetectException)
            {
                throw (MetadataException) e;
            }
            else
            {
                handleException("Can't set value for field " + getName()
                        + " to object " + (obj != null ? obj.getClass() : null), e);
                set(obj, value);
            }
        }
    }

    public Class getType()
    {
        try
        {
            return getCurrent().getType();
        }
        catch (Exception e)
        {
            if(e instanceof AutoDetectException)
            {
                throw (MetadataException) e;
            }
            else
            {
                handleException("Can't identify field type for field " + getName(), null);
                return getType();
            }
        }
    }

    protected boolean makeAccessible()
    {
        return false;
    }

    public boolean usesAccessorsAndMutators()
    {
        return false;
    }

    private PersistentField createPersistentFieldForIndex() throws Exception
    {
        return newInstance(persistentFieldClasses[index]);
    }

    private PersistentField newInstance(Class pfClass) throws Exception
    {
        Class[] types = new Class[]{Class.class, String.class};
        Object[] args = new Object[]{getDeclaringClass(), getName()};
        return (PersistentField) ClassHelper.newInstance(pfClass, types, args);
    }

    static class ExceptionWrapper implements Serializable
    {
        private static final long serialVersionUID = 3691042088451912249L;
        Exception exception;
        String message;

        public ExceptionWrapper(String message, Exception exception)
        {
            this.message = message;
            this.exception = exception;
        }
    }

    static class AutoDetectException extends MetadataException
    {
        private static final long serialVersionUID = 3257290223049585970L;

        public AutoDetectException()
        {
            super();
        }

        public AutoDetectException(Throwable t)
        {
            super(t);
        }

        public AutoDetectException(String message)
        {
            super(message);
        }

        public AutoDetectException(String message, Throwable t)
        {
            super(message, t);
        }
    }
}
