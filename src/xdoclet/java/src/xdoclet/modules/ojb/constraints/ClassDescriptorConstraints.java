package xdoclet.modules.ojb.constraints;

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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import xdoclet.modules.ojb.CommaListIterator;
import xdoclet.modules.ojb.LogHelper;
import xdoclet.modules.ojb.model.*;

/**
 * Checks constraints for class descriptors. Note that constraints may modify the class descriptor.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ClassDescriptorConstraints extends ConstraintsBase
{
    /** The interface that row readers must implement */
    private final static String ROW_READER_INTERFACE   = "org.apache.ojb.broker.accesslayer.RowReader";
    /** The interface that object caches must implement */
    private final static String OBJECT_CACHE_INTERFACE = "org.apache.ojb.broker.cache.ObjectCache";
    
    /**
     * Checks the given class descriptor.
     * 
     * @param classDef   The class descriptor
     * @param checkLevel The amount of checks to perform
     * @exception ConstraintException If a constraint has been violated
     */
    public void check(ClassDescriptorDef classDef, String checkLevel) throws ConstraintException
    {
        ensureNoTableInfoIfNoRepositoryInfo(classDef, checkLevel);
        checkModifications(classDef, checkLevel);
        checkExtents(classDef, checkLevel);
        ensureTableIfNecessary(classDef, checkLevel);
        checkFactoryClassAndMethod(classDef, checkLevel);
        checkInitializationMethod(classDef, checkLevel);
        checkPrimaryKey(classDef, checkLevel);
        checkProxyPrefetchingLimit(classDef, checkLevel);
        checkRowReader(classDef, checkLevel);
        checkObjectCache(classDef, checkLevel);
        checkProcedures(classDef, checkLevel);
    }

    /**
     * Ensures that generate-table-info is set to false if generate-repository-info is set to false.
     * 
     * @param classDef   The class descriptor
     * @param checkLevel The current check level (this constraint is checked in all levels)
     */
    private void ensureNoTableInfoIfNoRepositoryInfo(ClassDescriptorDef classDef, String checkLevel)
    {
        if (!classDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_GENERATE_REPOSITORY_INFO, true))
        {
            classDef.setProperty(PropertyHelper.OJB_PROPERTY_GENERATE_TABLE_INFO, "false");
        }
    }

    /**
     * Checks that the modified features exist.
     * 
     * @param classDef   The class descriptor
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     * @exception ConstraintException If the constraint has been violated
     */
    private void checkModifications(ClassDescriptorDef classDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        HashMap              features = new HashMap();
        FeatureDescriptorDef def;

        for (Iterator it = classDef.getFields(); it.hasNext();)
        {
            def = (FeatureDescriptorDef)it.next();
            features.put(def.getName(), def);
        }
        for (Iterator it = classDef.getReferences(); it.hasNext();)
        {
            def = (FeatureDescriptorDef)it.next();
            features.put(def.getName(), def);
        }
        for (Iterator it = classDef.getCollections(); it.hasNext();)
        {
            def = (FeatureDescriptorDef)it.next();
            features.put(def.getName(), def);
        }

        // now checking the modifications
        Properties mods;
        String     modName;
        String     propName;

        for (Iterator it = classDef.getModificationNames(); it.hasNext();)
        {
            modName = (String)it.next();
            if (!features.containsKey(modName))
            {
                throw new ConstraintException("Class "+classDef.getName()+" contains a modification for an unknown feature "+modName);
            }
            def = (FeatureDescriptorDef)features.get(modName);
            if (def.getOriginal() == null)
            {
                throw new ConstraintException("Class "+classDef.getName()+" contains a modification for a feature "+modName+" that is not inherited but defined in the same class");
            }
            // checking modification
            mods = classDef.getModification(modName);
            for (Iterator propIt = mods.keySet().iterator(); propIt.hasNext();)
            {
                propName = (String)propIt.next();
                if (!PropertyHelper.isPropertyAllowed(def.getClass(), propName))
                {
                    throw new ConstraintException("The modification of attribute "+propName+" in class "+classDef.getName()+" is not applicable to the feature "+modName);
                }
            }
        }
    }

    /**
     * Checks the extents specifications and removes unnecessary entries.
     * 
     * @param classDef   The class descriptor
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     * @exception ConstraintException If the constraint has been violated
     */
    private void checkExtents(ClassDescriptorDef classDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        HashMap            processedClasses = new HashMap();
        InheritanceHelper  helper           = new InheritanceHelper();
        ClassDescriptorDef curExtent;
        boolean            canBeRemoved;

        for (Iterator it = classDef.getExtentClasses(); it.hasNext();)
        {
            curExtent    = (ClassDescriptorDef)it.next();
            canBeRemoved = false;
            if (classDef.getName().equals(curExtent.getName()))
            {
                throw new ConstraintException("The class "+classDef.getName()+" specifies itself as an extent-class");
            }
            else if (processedClasses.containsKey(curExtent))
            {
                canBeRemoved = true;
            }
            else
            {
                try
                {
                    if (!helper.isSameOrSubTypeOf(curExtent, classDef.getName(), false))
                    {
                        throw new ConstraintException("The class "+classDef.getName()+" specifies an extent-class "+curExtent.getName()+" that is not a sub-type of it");
                    }
                    // now we check whether we already have an extent for a base-class of this extent-class
                    for (Iterator processedIt = processedClasses.keySet().iterator(); processedIt.hasNext();)
                    {
                        if (helper.isSameOrSubTypeOf(curExtent, ((ClassDescriptorDef)processedIt.next()).getName(), false))
                        {
                            canBeRemoved = true;
                            break;
                        }
                    }
                }
                catch (ClassNotFoundException ex)
                {
                    // won't happen because we don't use lookup of the actual classes
                }
            }
            if (canBeRemoved)
            {
                it.remove();
            }
            processedClasses.put(curExtent, null);
        }
    }
    
    /**
     * Makes sure that the class descriptor has a table attribute if it requires it (i.e. it is
     * relevant for the repository descriptor).
     * 
     * @param classDef   The class descriptor
     * @param checkLevel The current check level (this constraint is checked in all levels)
     */
    private void ensureTableIfNecessary(ClassDescriptorDef classDef, String checkLevel)
    {
        if (classDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_OJB_PERSISTENT, false))
        {
            if (!classDef.hasProperty(PropertyHelper.OJB_PROPERTY_TABLE))
            {
                classDef.setProperty(PropertyHelper.OJB_PROPERTY_TABLE, classDef.getDefaultTableName());
            }
        }
    }

    /**
     * Checks the given class descriptor for correct factory-class and factory-method.
     * 
     * @param classDef   The class descriptor
     * @param checkLevel The current check level (this constraint is checked in basic (partly) and strict)
     * @exception ConstraintException If the constraint has been violated
     */
    private void checkFactoryClassAndMethod(ClassDescriptorDef classDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }
        String factoryClassName  = classDef.getProperty(PropertyHelper.OJB_PROPERTY_FACTORY_CLASS);
        String factoryMethodName = classDef.getProperty(PropertyHelper.OJB_PROPERTY_FACTORY_METHOD);

        if ((factoryClassName == null) && (factoryMethodName == null))
        {
            return;
        }
        if ((factoryClassName != null) && (factoryMethodName == null))
        {
            throw new ConstraintException("Class "+classDef.getName()+" has a factory-class but no factory-method.");
        }
        if ((factoryClassName == null) && (factoryMethodName != null))
        {
            throw new ConstraintException("Class "+classDef.getName()+" has a factory-method but no factory-class.");
        }

        if (CHECKLEVEL_STRICT.equals(checkLevel))
        {    
            Class  factoryClass;
            Method factoryMethod;
    
            try
            {
                factoryClass = InheritanceHelper.getClass(factoryClassName);
            }
            catch (ClassNotFoundException ex)
            {
                throw new ConstraintException("The class "+factoryClassName+" specified as factory-class of class "+classDef.getName()+" was not found on the classpath");
            }
            try
            {
                factoryMethod = factoryClass.getDeclaredMethod(factoryMethodName, new Class[0]);
            }
            catch (NoSuchMethodException ex)
            {
                factoryMethod = null;
            }
            catch (Exception ex)
            {
                throw new ConstraintException("Exception while checking the factory-class "+factoryClassName+" of class "+classDef.getName()+": "+ex.getMessage());
            }
            if (factoryMethod == null)
            {    
                try
                {
                    factoryMethod = factoryClass.getMethod(factoryMethodName, new Class[0]);
                }
                catch (NoSuchMethodException ex)
                {
                    throw new ConstraintException("No suitable factory-method "+factoryMethodName+" found in the factory-class "+factoryClassName+" of class "+classDef.getName());
                }
                catch (Exception ex)
                {
                    throw new ConstraintException("Exception while checking the factory-class "+factoryClassName+" of class "+classDef.getName()+": "+ex.getMessage());
                }
            }
    
            // checking return type and modifiers
            Class             returnType = factoryMethod.getReturnType();
            InheritanceHelper helper     = new InheritanceHelper();
    
            if ("void".equals(returnType.getName()))
            {
                throw new ConstraintException("The factory-method "+factoryMethodName+" in factory-class "+factoryClassName+" of class "+classDef.getName()+" must return a value");
            }
            try
            {
                if (!helper.isSameOrSubTypeOf(returnType.getName(), classDef.getName()))
                {
                    throw new ConstraintException("The method "+factoryMethodName+" in factory-class "+factoryClassName+" of class "+classDef.getName()+" must return the type "+classDef.getName()+" or a subtype of it");
                }
            }
            catch (ClassNotFoundException ex)
            {
                throw new ConstraintException("Could not find the class "+ex.getMessage()+" on the classpath while checking the factory-method "+factoryMethodName+" in the factory-class "+factoryClassName+" of class "+classDef.getName());
            }
            
            if (!Modifier.isStatic(factoryMethod.getModifiers()))
            {
                throw new ConstraintException("The factory-method "+factoryMethodName+" in factory-class "+factoryClassName+" of class "+classDef.getName()+" must be static");
            }
        }
    }

    /**
     * Checks the initialization-method of given class descriptor.
     * 
     * @param classDef   The class descriptor
     * @param checkLevel The current check level (this constraint is only checked in strict)
     * @exception ConstraintException If the constraint has been violated
     */
    private void checkInitializationMethod(ClassDescriptorDef classDef, String checkLevel) throws ConstraintException
    {
        if (!CHECKLEVEL_STRICT.equals(checkLevel))
        {
            return;
        }
        
        String initMethodName = classDef.getProperty(PropertyHelper.OJB_PROPERTY_INITIALIZATION_METHOD);

        if (initMethodName == null)
        {
            return;
        }

        Class  initClass;
        Method initMethod;

        try
        {
            initClass = InheritanceHelper.getClass(classDef.getName());
        }
        catch (ClassNotFoundException ex)
        {
            throw new ConstraintException("The class "+classDef.getName()+" was not found on the classpath");
        }
        try
        {
            initMethod = initClass.getDeclaredMethod(initMethodName, new Class[0]);
        }
        catch (NoSuchMethodException ex)
        {
            initMethod = null;
        }
        catch (Exception ex)
        {
            throw new ConstraintException("Exception while checking the class "+classDef.getName()+": "+ex.getMessage());
        }
        if (initMethod == null)
        {    
            try
            {
                initMethod = initClass.getMethod(initMethodName, new Class[0]);
            }
            catch (NoSuchMethodException ex)
            {
                throw new ConstraintException("No suitable initialization-method "+initMethodName+" found in class "+classDef.getName());
            }
            catch (Exception ex)
            {
                throw new ConstraintException("Exception while checking the class "+classDef.getName()+": "+ex.getMessage());
            }
        }

        // checking modifiers
        int mods = initMethod.getModifiers();

        if (Modifier.isStatic(mods) || Modifier.isAbstract(mods))
        {
            throw new ConstraintException("The initialization-method "+initMethodName+" in class "+classDef.getName()+" must be a concrete instance method");
        }
    }

    /**
     * Checks whether given class descriptor has a primary key.
     * 
     * @param classDef   The class descriptor
     * @param checkLevel The current check level (this constraint is only checked in strict)
     * @exception ConstraintException If the constraint has been violated
     */
    private void checkPrimaryKey(ClassDescriptorDef classDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        if (classDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_GENERATE_TABLE_INFO, true) &&
            classDef.getPrimaryKeys().isEmpty())
        {
            LogHelper.warn(true,
                           getClass(),
                           "checkPrimaryKey",
                           "The class "+classDef.getName()+" has no primary key");
        }
    }

    /**
     * Checks the given class descriptor for correct row-reader setting.
     * 
     * @param classDef   The class descriptor
     * @param checkLevel The current check level (this constraint is only checked in strict)
     * @exception ConstraintException If the constraint has been violated
     */
    private void checkRowReader(ClassDescriptorDef classDef, String checkLevel) throws ConstraintException
    {
        if (!CHECKLEVEL_STRICT.equals(checkLevel))
        {
            return;
        }
        
        String rowReaderName  = classDef.getProperty(PropertyHelper.OJB_PROPERTY_ROW_READER);

        if (rowReaderName == null)
        {
            return;
        }

        try
        {
            InheritanceHelper helper = new InheritanceHelper();

            if (!helper.isSameOrSubTypeOf(rowReaderName, ROW_READER_INTERFACE))
            {
                throw new ConstraintException("The class "+rowReaderName+" specified as row-reader of class "+classDef.getName()+" does not implement the interface "+ROW_READER_INTERFACE);
            }
        }
        catch (ClassNotFoundException ex)
        {
            throw new ConstraintException("Could not find the class "+ex.getMessage()+" on the classpath while checking the row-reader class "+rowReaderName+" of class "+classDef.getName());
        }
    }

    /**
     * Checks the given class descriptor for correct object cache setting.
     * 
     * @param classDef   The class descriptor
     * @param checkLevel The current check level (this constraint is only checked in strict)
     * @exception ConstraintException If the constraint has been violated
     */
    private void checkObjectCache(ClassDescriptorDef classDef, String checkLevel) throws ConstraintException
    {
        if (!CHECKLEVEL_STRICT.equals(checkLevel))
        {
            return;
        }
        
        ObjectCacheDef objCacheDef = classDef.getObjectCache();

        if (objCacheDef == null)
        {
            return;
        }

        String objectCacheName = objCacheDef.getName();

        if ((objectCacheName == null) || (objectCacheName.length() == 0))
        {
            throw new ConstraintException("No class specified for the object-cache of class "+classDef.getName());
        }

        try
        {
            InheritanceHelper helper = new InheritanceHelper();

            if (!helper.isSameOrSubTypeOf(objectCacheName, OBJECT_CACHE_INTERFACE))
            {
                throw new ConstraintException("The class "+objectCacheName+" specified as object-cache of class "+classDef.getName()+" does not implement the interface "+OBJECT_CACHE_INTERFACE);
            }
        }
        catch (ClassNotFoundException ex)
        {
            throw new ConstraintException("Could not find the class "+ex.getMessage()+" on the classpath while checking the object-cache class "+objectCacheName+" of class "+classDef.getName());
        }
    }

    /**
     * Checks the given class descriptor for correct procedure settings.
     * 
     * @param classDef   The class descriptor
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     * @exception ConstraintException If the constraint has been violated
     */
    private void checkProcedures(ClassDescriptorDef classDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        ProcedureDef procDef;
        String       type;
        String       name;
        String       fieldName;
        String       argName;
        
        for (Iterator it = classDef.getProcedures(); it.hasNext();)
        {
            procDef = (ProcedureDef)it.next();
            type    = procDef.getName();
            name    = procDef.getProperty(PropertyHelper.OJB_PROPERTY_NAME);
            if ((name == null) || (name.length() == 0))
            {
                throw new ConstraintException("The "+type+"-procedure in class "+classDef.getName()+" doesn't have a name");
            }
            fieldName = procDef.getProperty(PropertyHelper.OJB_PROPERTY_RETURN_FIELD_REF);
            if ((fieldName != null) && (fieldName.length() > 0))
            {
                if (classDef.getField(fieldName) == null)
                {
                    throw new ConstraintException("The "+type+"-procedure "+name+" in class "+classDef.getName()+" references an unknown or non-persistent return field "+fieldName);
                }
            }
            for (CommaListIterator argIt = new CommaListIterator(procDef.getProperty(PropertyHelper.OJB_PROPERTY_ARGUMENTS)); argIt.hasNext();)
            {
                argName = argIt.getNext();
                if (classDef.getProcedureArgument(argName) == null)
                {
                    throw new ConstraintException("The "+type+"-procedure "+name+" in class "+classDef.getName()+" references an unknown argument "+argName);
                }
            }
        }

        ProcedureArgumentDef argDef;

        for (Iterator it = classDef.getProcedureArguments(); it.hasNext();)
        {
            argDef = (ProcedureArgumentDef)it.next();
            type   = argDef.getProperty(PropertyHelper.OJB_PROPERTY_TYPE);
            if ("runtime".equals(type))
            {
                fieldName = argDef.getProperty(PropertyHelper.OJB_PROPERTY_FIELD_REF);
                if ((fieldName != null) && (fieldName.length() > 0))
                {
                    if (classDef.getField(fieldName) == null)
                    {
                        throw new ConstraintException("The "+type+"-argument "+argDef.getName()+" in class "+classDef.getName()+" references an unknown or non-persistent return field "+fieldName);
                    }
                }
            }
        }
    }
}
