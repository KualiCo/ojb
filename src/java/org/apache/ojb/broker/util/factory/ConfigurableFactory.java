package org.apache.ojb.broker.util.factory;

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

import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.configuration.Configurable;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;
import org.apache.ojb.broker.util.interceptor.InterceptorFactory;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * ConfigurableFactory is an abstract baseclass for OJB factory classes.
 * It provides all infrastructure for configuration through OJB.properties.
 * A derived class must implement the getConfigurationKey() method.
 * The returned configuration key is used to lookup the class to be instantiated
 * by the derived factory.
 * The lookup is performed in the configure() method and uses the OJB.properties
 * information.
 *
 * @author Thomas Mahler
 */
public abstract class ConfigurableFactory implements Configurable
{
    private Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * the class to be served
     */
    private Class classToServe = null;


    /**
     * the public constructor calls configure() to perform configuration
     * of the factory instance.
     */
    public ConfigurableFactory()
    {
        OjbConfigurator.getInstance().configure(this);
    }

    /**
     * must be implemented in the concrete factory classes.
     * the configuration key is used to lookup the Class to serve
     * from the OjbConfiguration in configure().
     */
    protected abstract String getConfigurationKey();

    /**
     * @see org.apache.ojb.broker.util.configuration.Configurable#configure(Configuration)
     * looks up the the key getConfigurationKey() in the OjbConfiguration
     * to determine the Class to be served.
     */
    public void configure(Configuration pConfig) throws ConfigurationException
    {
        if (getConfigurationKey() == null)
        {
            getLogger().error("ConfigurableFactory configuration key is 'null'");
            throw new PersistenceBrokerException("ConfigurableFactory configuration key is 'null'");
        }
        Class clazz = pConfig.getClass(getConfigurationKey(), null);
        if (clazz == null)
        {
            getLogger().error("ConfigurableFactory configuration key class for key'" + getConfigurationKey() + "' does not exist.");
            throw new PersistenceBrokerException(
                    "ConfigurableFactory configuration key class for key'" + getConfigurationKey() + "' does not exist.");
        }
        this.setClassToServe(clazz);
    }

    /**
     * factory method for creating new instances
     * the Class to be instantiated is defined by getClassToServe().
     * @return Object the created instance
     */
    public Object createNewInstance(Class[] types, Object[] args)
    {
        try
        {
            Object result;
            // create an instance of the target class
            if (types != null)
            {
                result = ClassHelper.newInstance(getClassToServe(), types, args, true);
            }
            else
            {
                result = ClassHelper.newInstance(getClassToServe(), true);
            }
            // if defined in OJB.properties all instances are wrapped by an interceptor
            result = InterceptorFactory.getInstance().createInterceptorFor(result);
            return result;

        }
        catch (InstantiationException e)
        {
            getLogger().error("ConfigurableFactory can't instantiate class " +
                    getClassToServe() + buildArgumentString(types, args), e);
            throw new PersistenceBrokerException(e);
        }
        catch (IllegalAccessException e)
        {
            getLogger().error("ConfigurableFactory can't access constructor for class " +
                    getClassToServe() + buildArgumentString(types, args), e);
            throw new PersistenceBrokerException(e);
        }
        catch (Exception e)
        {
            getLogger().error("ConfigurableFactory instantiation failed for class " +
                    getClassToServe() + buildArgumentString(types, args), e);
            throw new PersistenceBrokerException(e);
        }
    }

    protected String buildArgumentString(Class[] types, Object[] args)
    {
        StringBuffer buf = new StringBuffer();
        String eol = SystemUtils.LINE_SEPARATOR;
        buf.append(eol + "* Factory types: ");
        if (types != null)
        {
            for (int i = 0; i < types.length; i++)
            {
                Class type = types[i];
                buf.append(eol + (i + 1) + " - Type: " + (type != null ? type.getName() : null));
            }
        }
        else
            buf.append(eol + "none");

        buf.append(eol + "* Factory arguments: ");
        if (args != null)
        {
            for (int i = 0; i < args.length; i++)
            {
                Object obj = args[i];
                buf.append(eol + (i + 1) + " - Argument: " + obj);
            }
        }
        else
            buf.append(eol + "none");
        return buf.toString();
    }

    /**
     * factory method for creating new instances
     * the Class to be instantiated is defined by getClassToServe().
     * @return Object the created instance
     */
    public Object createNewInstance()
    {
        return createNewInstance((Class) null, (Object) null);
    }


    /**
     * factory method for creating new instances
     * the Class to be instantiated is defined by getClassToServe().
     * @return Object the created instance
     */
    public Object createNewInstance(Class type, Object arg)
    {
        if (type != null)
            return createNewInstance(new Class[]{type}, new Object[]{arg});
        else
            return createNewInstance((Class[]) null, (Object[]) null);
    }

    /**
     * Returns the classToServe.
     * @return Class
     */
    public Class getClassToServe()
    {
        return classToServe;
    }

    /**
     * Sets the classToServe.
     * <br/>
     * Normally this is done by the factory using
     * {@link #getConfigurationKey}.
     * <br/>
     * <b>Note:</b> For internal use only!
     * @param classToServe The classToServe to set
     */
    public void setClassToServe(Class classToServe)
    {
        this.classToServe = classToServe;
    }

    /**
     * the logger for the ConfigurableFactory
     */
    protected Logger getLogger()
    {
        return log;
    }
}
