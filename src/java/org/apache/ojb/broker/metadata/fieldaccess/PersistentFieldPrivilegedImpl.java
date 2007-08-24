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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A {@link PersistentField} implementation using
 * reflection to access but does cooperate with
 * AccessController and do not suppress the java
 * language access check.
 *
 * @version $Id: PersistentFieldPrivilegedImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 * @see PersistentFieldDirectImpl
 */
public class PersistentFieldPrivilegedImpl extends PersistentFieldDirectImpl
{
    private static final long serialVersionUID = -6110158693763128846L;

    private SetAccessibleAction setAccessibleAction = new SetAccessibleAction();
    private UnsetAccessibleAction unsetAccessibleAction = new UnsetAccessibleAction();
    private static final int ACCESSIBLE_STATE_UNKOWN = 0;
    private static final int ACCESSIBLE_STATE_FALSE = 1;
    private static final int ACCESSIBLE_STATE_SET_TRUE = 2;

    public PersistentFieldPrivilegedImpl()
    {
    }

    public PersistentFieldPrivilegedImpl(Class type, String fieldname)
    {
        super(type, fieldname);
    }

    protected Object getValueFrom(Field field, Object target)
    {
        int accessibleState = ACCESSIBLE_STATE_UNKOWN;
        Object result = null;
        if (!field.isAccessible()) accessibleState = ACCESSIBLE_STATE_FALSE;
        if (accessibleState == ACCESSIBLE_STATE_FALSE)
        {
            accessibleState = ACCESSIBLE_STATE_SET_TRUE;
            setAccessibleAction.current = field;
            AccessController.doPrivileged(setAccessibleAction);
        }
        try
        {
            result = super.getValueFrom(field, target);
        }
        finally
        {
            if (accessibleState == ACCESSIBLE_STATE_SET_TRUE)
            {
                unsetAccessibleAction.current = field;
                AccessController.doPrivileged(unsetAccessibleAction);
            }
        }
        return result;
    }

    protected void setValueFor(Field field, Object target, Object value)
    {
        int accessibleState = ACCESSIBLE_STATE_UNKOWN;
        if (!field.isAccessible()) accessibleState = ACCESSIBLE_STATE_FALSE;
        if (accessibleState == ACCESSIBLE_STATE_FALSE)
        {
            accessibleState = ACCESSIBLE_STATE_SET_TRUE;
            setAccessibleAction.current = field;
            AccessController.doPrivileged(setAccessibleAction);
        }
        try
        {
            super.setValueFor(field, target, value);
        }
        finally
        {
            if (accessibleState == ACCESSIBLE_STATE_SET_TRUE)
            {
                unsetAccessibleAction.current = field;
                AccessController.doPrivileged(unsetAccessibleAction);
            }
        }
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
        return false;
    }

    //************************************************************
    // inner class
    //************************************************************
    private static class SetAccessibleAction implements PrivilegedAction, Serializable
    {
        static final long serialVersionUID = 8152025069698028050L;
        transient Field current;

        public Object run()
        {
            current.setAccessible(true);
            return null;
        }
    }

    private static class UnsetAccessibleAction implements PrivilegedAction, Serializable
    {
        static final long serialVersionUID = -2284913657454430305L;
        transient Field current;

        public Object run()
        {
            current.setAccessible(false);
            return null;
        }
    }
}
