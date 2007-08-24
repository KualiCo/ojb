package xdoclet.modules.ojb.constraints;

import xdoclet.modules.ojb.CommaListIterator;
import xdoclet.modules.ojb.model.ClassDescriptorDef;
import xdoclet.modules.ojb.model.CollectionDescriptorDef;
import xdoclet.modules.ojb.model.FieldDescriptorDef;
import xdoclet.modules.ojb.model.ModelDef;
import xdoclet.modules.ojb.model.PropertyHelper;

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
 * Checks constraints for collection descriptors. Note that constraints may modify the collection descriptor.
 * For checks of the relationships (e.g. foreignkey) see ModelConstraints.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class CollectionDescriptorConstraints extends FeatureDescriptorConstraints
{
    /** The collection interface (this type or subtypes of it can be handled by OJB) */
    private final static String JAVA_COLLECTION_INTERFACE       = "java.util.Collection";
    /** The interface that user-defined collection classes must implement */
    private final static String MANAGEABLE_COLLECTION_INTERFACE = "org.apache.ojb.broker.ManageableCollection";
    /** The interface that user-defined query customizers must implement */
    private final static String QUERY_CUSTOMIZER_INTERFACE      = "org.apache.ojb.broker.accesslayer.QueryCustomizer";

    /**
     * Checks the given collection descriptor.
     * 
     * @param collDef    The collection descriptor
     * @param checkLevel The amount of checks to perform
     * @exception ConstraintException If a constraint has been violated
     */
    public void check(CollectionDescriptorDef collDef, String checkLevel) throws ConstraintException
    {
        ensureElementClassRef(collDef, checkLevel);
        checkInheritedForeignkey(collDef, checkLevel);
        ensureCollectionClass(collDef, checkLevel);
        checkProxyPrefetchingLimit(collDef, checkLevel);
        checkOrderby(collDef, checkLevel);
        checkQueryCustomizer(collDef, checkLevel);
    }

    /**
     * Ensures that the given collection descriptor has a valid element-class-ref property.
     * 
     * @param collDef    The collection descriptor
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     * @exception ConstraintException If element-class-ref could not be determined or is invalid
     */
    private void ensureElementClassRef(CollectionDescriptorDef collDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        String arrayElementClassName = collDef.getProperty(PropertyHelper.OJB_PROPERTY_ARRAY_ELEMENT_CLASS_REF);

        if (!collDef.hasProperty(PropertyHelper.OJB_PROPERTY_ELEMENT_CLASS_REF))
        {
            if (arrayElementClassName != null)
            {
                // we use the array element type
                collDef.setProperty(PropertyHelper.OJB_PROPERTY_ELEMENT_CLASS_REF, arrayElementClassName);
            }
            else
            {
                throw new ConstraintException("Collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" does not specify its element class");
            }
        }

        // now checking the element type
        ModelDef           model            = (ModelDef)collDef.getOwner().getOwner();
        String             elementClassName = collDef.getProperty(PropertyHelper.OJB_PROPERTY_ELEMENT_CLASS_REF);
        ClassDescriptorDef elementClassDef  = model.getClass(elementClassName);

        if (elementClassDef == null)
        {
            throw new ConstraintException("Collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" references an unknown class "+elementClassName);
        }
        if (!elementClassDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_OJB_PERSISTENT, false))
        {
            throw new ConstraintException("The element class "+elementClassName+" of the collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" is not persistent");
        }
        if (CHECKLEVEL_STRICT.equals(checkLevel) && (arrayElementClassName != null))
        {
            // specified element class must be a subtype of the element type
            try
            {
                InheritanceHelper helper = new InheritanceHelper();

                if (!helper.isSameOrSubTypeOf(elementClassDef, arrayElementClassName, true))
                {
                    throw new ConstraintException("The element class "+elementClassName+" of the collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" is not the same or a subtype of the array base type "+arrayElementClassName);
                }
            }
            catch (ClassNotFoundException ex)
            {
                throw new ConstraintException("Could not find the class "+ex.getMessage()+" on the classpath while checking the collection "+collDef.getName()+" in class "+collDef.getOwner().getName());
            }
        }
        // we're adjusting the property to use the classloader-compatible form
        collDef.setProperty(PropertyHelper.OJB_PROPERTY_ELEMENT_CLASS_REF, elementClassDef.getName());
    }

    /**
     * Checks that the foreignkey is not modified in an inherited/nested m:n collection descriptor.
     *  
     * @param collDef    The collection descriptor
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     */
    private void checkInheritedForeignkey(CollectionDescriptorDef collDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }
        if (!collDef.isInherited() && !collDef.isNested())
        {
            return;
        }
        if (!collDef.hasProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE))
        {
            return;
        }

        String localFk     = collDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY);
        String inheritedFk = collDef.getOriginal().getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY);

        if (!CommaListIterator.sameLists(localFk, inheritedFk))
        {
            throw new ConstraintException("The foreignkey property has been changed for the m:n collection "+collDef.getName()+" in class "+collDef.getOwner().getName());
        }
    }

    /**
     * Ensures that the given collection descriptor has the collection-class property if necessary.
     * 
     * @param collDef    The collection descriptor
     * @param checkLevel The current check level (this constraint is checked in basic (partly) and strict)
     * @exception ConstraintException If collection-class is given for an array or if no collection-class is given but required
     */
    private void ensureCollectionClass(CollectionDescriptorDef collDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        if (collDef.hasProperty(PropertyHelper.OJB_PROPERTY_ARRAY_ELEMENT_CLASS_REF))
        {
            // an array cannot have a collection-class specified 
            if (collDef.hasProperty(PropertyHelper.OJB_PROPERTY_COLLECTION_CLASS))
            {
                throw new ConstraintException("Collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" is an array but does specify collection-class");
            }
            else
            {
                // no further processing necessary as its an array
                return;
            }
        }

        if (CHECKLEVEL_STRICT.equals(checkLevel))
        {    
            InheritanceHelper helper         = new InheritanceHelper();
            ModelDef          model          = (ModelDef)collDef.getOwner().getOwner();
            String            specifiedClass = collDef.getProperty(PropertyHelper.OJB_PROPERTY_COLLECTION_CLASS);
            String            variableType   = collDef.getProperty(PropertyHelper.OJB_PROPERTY_VARIABLE_TYPE);
    
            try
            {
                if (specifiedClass != null)
                {
                    // if we have a specified class then it has to implement the manageable collection and be a sub type of the variable type
                    if (!helper.isSameOrSubTypeOf(specifiedClass, variableType))
                    {
                        throw new ConstraintException("The type "+specifiedClass+" specified as collection-class of the collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" is not a sub type of the variable type "+variableType);
                    }
                    if (!helper.isSameOrSubTypeOf(specifiedClass, MANAGEABLE_COLLECTION_INTERFACE))
                    {
                        throw new ConstraintException("The type "+specifiedClass+" specified as collection-class of the collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" does not implement "+MANAGEABLE_COLLECTION_INTERFACE);
                    }
                }
                else
                {
                    // no collection class specified so the variable type has to be a collection type
                    if (helper.isSameOrSubTypeOf(variableType, MANAGEABLE_COLLECTION_INTERFACE))
                    {
                        // we can specify it as a collection-class as it is an manageable collection
                        collDef.setProperty(PropertyHelper.OJB_PROPERTY_COLLECTION_CLASS, variableType);
                    }
                    else if (!helper.isSameOrSubTypeOf(variableType, JAVA_COLLECTION_INTERFACE))
                    {
                        throw new ConstraintException("The collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" needs the collection-class attribute as its variable type does not implement "+JAVA_COLLECTION_INTERFACE);
                    }
                }
            }
            catch (ClassNotFoundException ex)
            {
                throw new ConstraintException("Could not find the class "+ex.getMessage()+" on the classpath while checking the collection "+collDef.getName()+" in class "+collDef.getOwner().getName());
            }
        }
    }

    /**
     * Checks the orderby attribute.
     * 
     * @param collDef    The collection descriptor
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     * @exception ConstraintException If the value for orderby is invalid (unknown field or ordering)
     */
    private void checkOrderby(CollectionDescriptorDef collDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        String orderbySpec = collDef.getProperty(PropertyHelper.OJB_PROPERTY_ORDERBY);

        if ((orderbySpec == null) || (orderbySpec.length() == 0))
        {
            return;
        }

        ClassDescriptorDef ownerClass       = (ClassDescriptorDef)collDef.getOwner();
        String             elementClassName = collDef.getProperty(PropertyHelper.OJB_PROPERTY_ELEMENT_CLASS_REF).replace('$', '.');
        ClassDescriptorDef elementClass     = ((ModelDef)ownerClass.getOwner()).getClass(elementClassName);
        FieldDescriptorDef fieldDef;
        String             token;
        String             fieldName;
        String             ordering;
        int                pos;

        for (CommaListIterator it = new CommaListIterator(orderbySpec); it.hasNext();)
        {
            token = it.getNext();
            pos   = token.indexOf('=');
            if (pos == -1)
            {
                fieldName = token;
                ordering  = null;
            }
            else
            {
                fieldName = token.substring(0, pos);
                ordering  = token.substring(pos + 1);
            }
            fieldDef = elementClass.getField(fieldName);
            if (fieldDef == null)
            {
                throw new ConstraintException("The field "+fieldName+" specified in the orderby attribute of the collection "+collDef.getName()+" in class "+ownerClass.getName()+" hasn't been found in the element class "+elementClass.getName());
            }
            if ((ordering != null) && (ordering.length() > 0) &&
                !"ASC".equals(ordering) && !"DESC".equals(ordering))
            {
                throw new ConstraintException("The ordering "+ordering+" specified in the orderby attribute of the collection "+collDef.getName()+" in class "+ownerClass.getName()+" is invalid");
            }
        }
    }

    /**
     * Checks the query-customizer setting of the given collection descriptor.
     * 
     * @param collDef    The collection descriptor
     * @param checkLevel The current check level (this constraint is only checked in strict)
     * @exception ConstraintException If the constraint has been violated
     */
    private void checkQueryCustomizer(CollectionDescriptorDef collDef, String checkLevel) throws ConstraintException
    {
        if (!CHECKLEVEL_STRICT.equals(checkLevel))
        {
            return;
        }
        
        String queryCustomizerName  = collDef.getProperty(PropertyHelper.OJB_PROPERTY_QUERY_CUSTOMIZER);

        if (queryCustomizerName == null)
        {
            return;
        }

        try
        {
            InheritanceHelper helper = new InheritanceHelper();

            if (!helper.isSameOrSubTypeOf(queryCustomizerName, QUERY_CUSTOMIZER_INTERFACE))
            {
                throw new ConstraintException("The class "+queryCustomizerName+" specified as query-customizer of collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" does not implement the interface "+QUERY_CUSTOMIZER_INTERFACE);
            }
        }
        catch (ClassNotFoundException ex)
        {
            throw new ConstraintException("The class "+ex.getMessage()+" specified as query-customizer of collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" was not found on the classpath");
        }
    }
}
