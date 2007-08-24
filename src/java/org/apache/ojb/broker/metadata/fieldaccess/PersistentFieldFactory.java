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

import org.apache.ojb.broker.core.PersistenceBrokerConfiguration;
import org.apache.ojb.broker.metadata.MetadataException;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: PersistentFieldFactory.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */

public class PersistentFieldFactory
{
    private static Logger log = LoggerFactory.getLogger(PersistentFieldFactory.class);
    private static final Class DEFAULT_PERSISTENT_FIELD_IMPL = PersistentFieldDirectImpl.class;
    private static final Class[] METHOD_PARAMETER_TYPES = {Class.class, String.class};

//    private static boolean usesAccessorsAndMutators = false;
//    private static boolean usesAccessorsAndMutatorsCheck = false;

    /**
     * @throws MetadataException if an erros occours when creating the PersistenteField
     */
	public static PersistentField createPersistentField(Class attributeType, String attributeName)
	{
		return createPersistentField(null,attributeType,attributeName);
	}
    
    public static PersistentField createPersistentField(String persistentFieldClassName, Class attributeType, String attributeName)
    {
        try
        {
            if (persistentFieldClassName == null)
            {
                synchronized (PersistentFieldFactory.class)
                {
                    persistentFieldClassName = getDefaultPersistentFieldClassName();
                }
            }
            Object[] args = {attributeType, attributeName};
            return (PersistentField) ClassHelper.newInstance(persistentFieldClassName, METHOD_PARAMETER_TYPES, args);
            
        }
        catch (Exception ex)
        {
            throw new MetadataException("Error creating PersistentField: " +
                    attributeType.getName() + ", " + attributeName, ex);
        }
    }

//    public static boolean usesAccessorsAndMutators()
//    {
//        boolean retval = false;
//        if (usesAccessorsAndMutatorsCheck)
//            retval = usesAccessorsAndMutators;
//        else
//        {
//            String className = getDefaultPersistentFieldClassName();
//            PersistentField field = null;
//            try
//            {
//                field = (PersistentField) ClassHelper.newInstance(className);
//                usesAccessorsAndMutators = field.usesAccessorsAndMutators();
//                retval = usesAccessorsAndMutators;
//            }
//            catch (Exception e)
//            {
//                log.error("Cannot verify 'usesAccessorsAndMutators' attribute for class " + className, e);
//            }
//            finally
//            {
//                usesAccessorsAndMutatorsCheck = true;
//            }
//        }
//        return retval;
//    }

    private static String getDefaultPersistentFieldClassName()
    {
        try
        {
            PersistenceBrokerConfiguration config =
                    (PersistenceBrokerConfiguration) OjbConfigurator.getInstance().getConfigurationFor(
                            null);

            Class clazz = config.getPersistentFieldClass();
            return clazz.getName();
        }
        catch (ConfigurationException e)
        {
            log.error("Cannot look-up PersistentField class, use default implementation instead", e);
            return DEFAULT_PERSISTENT_FIELD_IMPL.getName();
        }
    }

}
