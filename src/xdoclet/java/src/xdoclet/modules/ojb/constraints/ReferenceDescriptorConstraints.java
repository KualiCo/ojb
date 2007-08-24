package xdoclet.modules.ojb.constraints;

import xdoclet.modules.ojb.LogHelper;
import xdoclet.modules.ojb.model.ClassDescriptorDef;
import xdoclet.modules.ojb.model.ModelDef;
import xdoclet.modules.ojb.model.PropertyHelper;
import xdoclet.modules.ojb.model.ReferenceDescriptorDef;

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
 * Checks constraints for reference descriptors. Note that constraints may modify the reference descriptor.
 * For checks of the relationships (e.g. foreignkey) see ModelConstraints.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ReferenceDescriptorConstraints extends FeatureDescriptorConstraints
{
    /**
     * Checks the given reference descriptor.
     * 
     * @param refDef     The reference descriptor
     * @param checkLevel The amount of checks to perform
     * @exception ConstraintException If a constraint has been violated
     */
    public void check(ReferenceDescriptorDef refDef, String checkLevel) throws ConstraintException
    {
        ensureClassRef(refDef, checkLevel);
        checkProxyPrefetchingLimit(refDef, checkLevel);
    }

    /**
     * Ensures that the given reference descriptor has the class-ref property.
     * 
     * @param refDef     The reference descriptor
     * @param checkLevel The current check level (this constraint is checked in basic (partly) and strict)
     * @exception ConstraintException If a constraint has been violated
     */
    private void ensureClassRef(ReferenceDescriptorDef refDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        if (!refDef.hasProperty(PropertyHelper.OJB_PROPERTY_CLASS_REF))
        {
            if (refDef.hasProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_CLASS_REF))
            {
                // we use the type of the reference variable
                refDef.setProperty(PropertyHelper.OJB_PROPERTY_CLASS_REF, refDef.getProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_CLASS_REF));
            }
            else
            {
                throw new ConstraintException("Reference "+refDef.getName()+" in class "+refDef.getOwner().getName()+" does not reference any class");
            }
        }

        // now checking the type
        ClassDescriptorDef ownerClassDef   = (ClassDescriptorDef)refDef.getOwner();
        ModelDef           model           = (ModelDef)ownerClassDef.getOwner();
        String             targetClassName = refDef.getProperty(PropertyHelper.OJB_PROPERTY_CLASS_REF);
        ClassDescriptorDef targetClassDef  = model.getClass(targetClassName);

        if (targetClassDef == null)
        {
            throw new ConstraintException("The class "+targetClassName+" referenced by "+refDef.getName()+" in class "+ownerClassDef.getName()+" is unknown or not persistent");
        }
        if (!targetClassDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_OJB_PERSISTENT, false))
        {
            throw new ConstraintException("The class "+targetClassName+" referenced by "+refDef.getName()+" in class "+ownerClassDef.getName()+" is not persistent");
        }
        
        if (CHECKLEVEL_STRICT.equals(checkLevel))
        {
            try
            {
                InheritanceHelper helper = new InheritanceHelper();

                if (refDef.isAnonymous())
                {
                    // anonymous reference: class must be a baseclass of the owner class
                    if (!helper.isSameOrSubTypeOf(ownerClassDef, targetClassDef.getName(), true))
                    {
                        throw new ConstraintException("The class "+targetClassName+" referenced by the anonymous reference "+refDef.getName()+" in class "+ownerClassDef.getName()+" is not a basetype of the class");
                    }
                }
                else
                {    
                    // specified element class must be a subtype of the variable type (if it exists, i.e. not for anonymous references)
                    String  varType      = refDef.getProperty(PropertyHelper.OJB_PROPERTY_VARIABLE_TYPE);
                    boolean performCheck = true;
    
                    // but we first check whether there is a useable type for the the variable type 
                    if (model.getClass(varType) == null)
                    {
                        try
                        {
                            InheritanceHelper.getClass(varType);
                        }
                        catch (ClassNotFoundException ex)
                        {
                            // no, so defer the check but issue a warning
                            performCheck = false;
                            LogHelper.warn(true,
                                           getClass(),
                                           "ensureClassRef",
                                           "Cannot check whether the type "+targetClassDef.getQualifiedName()+" specified as class-ref at reference "+refDef.getName()+" in class "+ownerClassDef.getName()+" is assignable to the declared type "+varType+" of the reference because this variable type cannot be found in source or on the classpath");
                        }
                    }
                    if (performCheck && !helper.isSameOrSubTypeOf(targetClassDef, varType, true))
                    {
                        throw new ConstraintException("The class "+targetClassName+" referenced by "+refDef.getName()+" in class "+ownerClassDef.getName()+" is not the same or a subtype of the variable type "+varType);
                    }
                }
            }
            catch (ClassNotFoundException ex)
            {
                throw new ConstraintException("Could not find the class "+ex.getMessage()+" on the classpath while checking the reference "+refDef.getName()+" in class "+refDef.getOwner().getName());
            }
        }
        // we're adjusting the property to use the classloader-compatible form
        refDef.setProperty(PropertyHelper.OJB_PROPERTY_CLASS_REF, targetClassDef.getName());
    }
}
