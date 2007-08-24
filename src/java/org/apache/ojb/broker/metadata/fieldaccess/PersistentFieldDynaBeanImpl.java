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

import org.apache.commons.beanutils.DynaBean;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.metadata.MetadataException;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * A {@link org.apache.ojb.broker.metadata.fieldaccess.PersistentField} implementation accesses a property
 * from a {@link org.apache.commons.beanutils.DynaBean}.
 * Note that because of the way that PersistentField works,
 * at run time the type of the field could actually be different, since
 * it depends on the DynaClass of the DynaBean that is given at runtime.
 * <p>
 * This implementation does not support nested fields.
 * </p>
 *
 * @version $Id: PersistentFieldDynaBeanImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PersistentFieldDynaBeanImpl extends PersistentFieldBase
{
    /*
    TODO: Don't know if it is possible to support nested fields with DynaBeans. This
    version does not support nested fields
    */
    private static final long serialVersionUID = 4728858060905429509L;

    public PersistentFieldDynaBeanImpl()
    {
        super();
    }

    public PersistentFieldDynaBeanImpl(Class aPropertyType, String aPropertyName)
    {
        super(aPropertyType, aPropertyName);
        checkNested(aPropertyName);
    }

    public void set(Object anObject, Object aValue) throws MetadataException
    {
        if(anObject == null) return;
        if (anObject instanceof DynaBean)
        {
            DynaBean dynaBean = (DynaBean) anObject;
            try
            {
                dynaBean.set(getName(), aValue);
            }
            catch (Throwable t)
            {
                String msg = dynaBean.getClass().getName();
                logSetProblem(anObject, aValue, msg);
                throw new PersistenceBrokerException(t);
            }
        }
        else
        {
            String msg = "the object is not a DynaBean";
            logSetProblem(anObject, aValue, msg);
            throw new PersistenceBrokerException(msg);
        }
    }

    public Object get(Object anObject) throws MetadataException
    {
        if(anObject == null) return null;
        if (anObject instanceof DynaBean)
        {
            DynaBean dynaBean = (DynaBean) anObject;
            try
            {
                return dynaBean.get(getName());
            }
            catch (Throwable t)
            {
                String msg = dynaBean.getClass().getName();
                logGetProblem(anObject, msg);
                throw new PersistenceBrokerException(t);
            }
        }
        else
        {
            String msg = "the object is not a DynaBean";
            logGetProblem(anObject, msg);
            throw new PersistenceBrokerException(msg);
        }
    }

    private void checkNested(String fieldName)
    {
        if(fieldName.indexOf(PATH_TOKEN) > -1)
        {
            throw new MetadataException("This implementation does not support nested fields");
        }
    }

    public Class getType()
    {
        return getDeclaringClass();
    }

    protected boolean makeAccessible()
    {
        return false;
    }

    public boolean usesAccessorsAndMutators()
    {
        return false;
    }

    /**
     * Let's give the user some hints as to what could be wrong.
     */
    protected void logSetProblem(Object anObject, Object aValue, String msg)
    {
        Logger logger = LoggerFactory.getDefaultLogger();
        logger.error("Error in operation [set] of object [" + this.getClass().getName() + "], " + msg);
        logger.error("Property Name [" + getName() + "]");
        if (anObject instanceof DynaBean)
        {
            DynaBean dynaBean = (DynaBean) anObject;
            logger.error("anObject was DynaClass [" + dynaBean.getDynaClass().getName() + "]");
        }
        else if (anObject != null)
        {
            logger.error("anObject was class [" + anObject.getClass().getName() + "]");
        }
        else
        {
            logger.error("anObject was null");
        }
        if (aValue != null)
            logger.error("aValue was class [" + aValue.getClass().getName() + "]");
        else
            logger.error("aValue was null");
    }

    /**
     * Let's give the user some hints as to what could be wrong.
     */
    protected void logGetProblem(Object anObject, String msg)
    {
        Logger logger = LoggerFactory.getDefaultLogger();
        logger.error("Error in operation [get of object [" + this.getClass().getName() + "], " + msg);
        logger.error("Property Name [" + getName() + "]");
        if (anObject instanceof DynaBean)
        {
            DynaBean dynaBean = (DynaBean) anObject;
            logger.error("anObject was DynaClass [" + dynaBean.getDynaClass().getName() + "]");
        }
        else if (anObject != null)
        {
            logger.error("anObject was class [" + anObject.getClass().getName() + "]");
        }
        else
        {
            logger.error("anObject was null");
        }
    }
}
