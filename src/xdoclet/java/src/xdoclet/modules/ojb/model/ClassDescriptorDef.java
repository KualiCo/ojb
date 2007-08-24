package xdoclet.modules.ojb.model;

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

import java.util.*;

import xdoclet.modules.ojb.CommaListIterator;
import xdoclet.modules.ojb.LogHelper;
import xdoclet.modules.ojb.constraints.*;
import xjavadoc.XClass;

/**
 * Definition of a class for the ojb repository file.
 *
 * @author <a href="mailto:tomdz@apache.org">Thomas Dudziak</a>
 */
public class ClassDescriptorDef extends DefBase
{
    /** The original class */
    private XClass  _origin;
    /** The direct base types (available after this class has been processed */
    private HashMap _directBaseTypes = new HashMap();
    /** Sub class defs */
    private ArrayList _extents = new ArrayList();
    /** Fields */
    private ArrayList _fields = new ArrayList();
    /** References */
    private ArrayList _references = new ArrayList();
    /** Collections */
    private ArrayList _collections = new ArrayList();
    /** Nested objects */
    private ArrayList _nested = new ArrayList();
    /** Contains modifications to inherited fields/references/collections
      * (key = name, value = Properties object with the modifications) */
    private HashMap _modifications = new HashMap();
    /** Index descriptors */
    private ArrayList _indexDescriptors = new ArrayList();
    /** The object cache */
    private ObjectCacheDef _objectCache = null;
    /** The procedures */
    private SortedMap _procedures = new TreeMap();
    /** The procedure arguments */
    private HashMap _procedureArguments = new HashMap();
    /** Whether this class descriptor has been processed */
    private boolean _hasBeenProcessed = false;

    /**
     * Creates a new class definition object.
     *
     * @param origin The original class
     */
    public ClassDescriptorDef(XClass origin)
    {
        super(origin.getTransformedQualifiedName());
        _origin = origin;
    }

    /**
     * Returns the original class.
     *
     * @return The original XDoclet class object
     */
    public XClass getOriginalClass()
    {
        return _origin;
    }

    /**
     * Returns the qualified name of this class as per Java spec.
     * 
     * @return The qualified name
     */
    public String getQualifiedName()
    {
        return getName().replace('$', '.');
    }

    /**
     * Returns the default table name for this class which is the unqualified class name.
     * 
     * @return The default table name
     */
    public String getDefaultTableName()
    {
        String name          = getName();
        int    lastDotPos    = name.lastIndexOf('.');
        int    lastDollarPos = name.lastIndexOf('$');

        return lastDollarPos > lastDotPos ? name.substring(lastDollarPos + 1) : name.substring(lastDotPos + 1);
    }
    
    /**
     * Determines whether this class descriptor has been processed.
     * 
     * @return <code>true</code> if this class descriptor has been processed
     */
    boolean hasBeenProcessed()
    {
        return _hasBeenProcessed;
    }
    
    /**
     * Processes theis class (ensures that all base types are processed, copies their features to this class, and applies
     * modifications (removes ignored features, changes declarations).
     * 
     * @throws ConstraintException If a constraint has been violated 
     */
    public void process() throws ConstraintException
    {
        ClassDescriptorDef otherClassDef;
        
        for (Iterator it = getDirectBaseTypes(); it.hasNext();)
        {
            otherClassDef = (ClassDescriptorDef)it.next();
            if (!otherClassDef.hasBeenProcessed())
            {
                otherClassDef.process();
            }
        }
        for (Iterator it = getNested(); it.hasNext();)
        {
            otherClassDef = ((NestedDef)it.next()).getNestedType();
            if (!otherClassDef.hasBeenProcessed())
            {
                otherClassDef.process();
            }
        }
        
        ArrayList               newFields      = new ArrayList();
        ArrayList               newReferences  = new ArrayList();
        ArrayList               newCollections = new ArrayList();
        FieldDescriptorDef      newFieldDef;
        ReferenceDescriptorDef  newRefDef;
        CollectionDescriptorDef newCollDef;
        
        // adding base features
        if (getBooleanProperty(PropertyHelper.OJB_PROPERTY_INCLUDE_INHERITED, true))
        {
            ArrayList baseTypes = new ArrayList();
            DefBase   featureDef;
            
            addRelevantBaseTypes(this, baseTypes);
            for (Iterator it = baseTypes.iterator(); it.hasNext();)
            {
                cloneInheritedFeatures((ClassDescriptorDef)it.next(), newFields, newReferences, newCollections);
            }

            for (Iterator it = newFields.iterator(); it.hasNext();)
            {
                newFieldDef = (FieldDescriptorDef)it.next();
                featureDef  = getFeature(newFieldDef.getName());
                if (featureDef != null)
                {    
                    if (!getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false))
                    {
                        // we have the implicit constraint that an anonymous field cannot redefine/be redefined
                        // except if it is ignored
                        if ("anonymous".equals(featureDef.getProperty(PropertyHelper.OJB_PROPERTY_ACCESS)))
                        {
                            throw new ConstraintException("The anonymous field "+featureDef.getName()+" in class "+getName()+" overrides an inherited field");
                        }
                        if ("anonymous".equals(newFieldDef.getProperty(PropertyHelper.OJB_PROPERTY_ACCESS)))
                        {
                            throw new ConstraintException("The inherited anonymous field "+newFieldDef.getName()+" is overriden in class "+getName());
                        }
                    }
                    LogHelper.warn(true, ClassDescriptorDef.class, "process", "Class "+getName()+" redefines the inherited field "+newFieldDef.getName());
                    it.remove();
                }
            }
            for (Iterator it = newReferences.iterator(); it.hasNext();)
            {
                newRefDef = (ReferenceDescriptorDef)it.next();
                if ("super".equals(newRefDef.getName()))
                {
                    // we don't inherit super-references
                    it.remove();
                }
                else if (hasFeature(newRefDef.getName()))
                {
                    LogHelper.warn(true, ClassDescriptorDef.class, "process", "Class "+getName()+" redefines the inherited reference "+newRefDef.getName());
                    it.remove();
                }
            }
            for (Iterator it = newCollections.iterator(); it.hasNext();)
            {
                newCollDef = (CollectionDescriptorDef)it.next();
                if (hasFeature(newCollDef.getName()))
                {
                    LogHelper.warn(true, ClassDescriptorDef.class, "process", "Class "+getName()+" redefines the inherited collection "+newCollDef.getName());
                    it.remove();
                }
            }
        }
        // adding nested features
        for (Iterator it = getNested(); it.hasNext();)
        {
            cloneNestedFeatures((NestedDef)it.next(), newFields, newReferences, newCollections);
        }
        _fields.addAll(0, newFields);
        _references.addAll(0, newReferences);
        _collections.addAll(0, newCollections);
        sortFields();
        _hasBeenProcessed = true;
    }

    /**
     * Sorts the fields.
     */
    private void sortFields()
    {
        HashMap            fields          = new HashMap();
        ArrayList          fieldsWithId    = new ArrayList();
        ArrayList          fieldsWithoutId = new ArrayList();
        FieldDescriptorDef fieldDef;

        for (Iterator it = getFields(); it.hasNext(); )
        {
            fieldDef = (FieldDescriptorDef)it.next();
            fields.put(fieldDef.getName(), fieldDef);
            if (fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_ID))
            {
                fieldsWithId.add(fieldDef.getName());
            }
            else
            {
                fieldsWithoutId.add(fieldDef.getName());
            }
        }

        Collections.sort(fieldsWithId, new FieldWithIdComparator(fields));

        ArrayList result = new ArrayList();

        for (Iterator it = fieldsWithId.iterator(); it.hasNext();)
        {
            result.add(getField((String)it.next()));
        }
        for (Iterator it = fieldsWithoutId.iterator(); it.hasNext();)
        {
            result.add(getField((String)it.next()));
        }

        _fields = result;
    }
    
    /**
     * Checks the constraints on this class.
     * 
     * @param checkLevel The amount of checks to perform
     * @exception ConstraintException If a constraint has been violated
     */
    public void checkConstraints(String checkLevel) throws ConstraintException
    {
        // now checking constraints
        FieldDescriptorConstraints      fieldConstraints = new FieldDescriptorConstraints();
        ReferenceDescriptorConstraints  refConstraints   = new ReferenceDescriptorConstraints();
        CollectionDescriptorConstraints collConstraints  = new CollectionDescriptorConstraints();

        for (Iterator it = getFields(); it.hasNext();)
        {
            fieldConstraints.check((FieldDescriptorDef)it.next(), checkLevel);
        }
        for (Iterator it = getReferences(); it.hasNext();)
        {
            refConstraints.check((ReferenceDescriptorDef)it.next(), checkLevel);
        }
        for (Iterator it = getCollections(); it.hasNext();)
        {
            collConstraints.check((CollectionDescriptorDef)it.next(), checkLevel);
        }
        new ClassDescriptorConstraints().check(this, checkLevel);
    }

    /**
     * Adds all relevant base types (depending on their include-inherited setting) to the given list.
     * 
     * @param curType   The type to process
     * @param baseTypes The list of basetypes
     */
    private void addRelevantBaseTypes(ClassDescriptorDef curType, ArrayList baseTypes)
    {
        ClassDescriptorDef baseDef;

        for (Iterator it = curType.getDirectBaseTypes(); it.hasNext();)
        {
            baseDef = (ClassDescriptorDef)it.next();
            if (!baseDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_INCLUDE_INHERITED, true))
            {
                // the base type has include-inherited set to false which means that
                // it does not include base features
                // since we do want these base features, we have to traverse its base types
                addRelevantBaseTypes(baseDef, baseTypes);
            }
            baseTypes.add(baseDef);
        }
    }

    /**
     * Determines whether the given list contains a descriptor with the same name.
     * 
     * @param defs The list to search
     * @param obj  The object that is searched for
     * @return <code>true</code> if the list contains a descriptor with the same name
     */
    private boolean contains(ArrayList defs, DefBase obj)
    {
        for (Iterator it = defs.iterator(); it.hasNext();)
        {
            if (obj.getName().equals(((DefBase)it.next()).getName()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Clones the features of the given base class (using modifications if available) and puts them into
     * the given lists.
     * 
     * @param baseDef        The base class descriptor
     * @param newFields      Recieves the field copies
     * @param newReferences  Recieves the reference copies
     * @param newCollections Recieves the collection copies
     */
    private void cloneInheritedFeatures(ClassDescriptorDef baseDef, ArrayList newFields, ArrayList newReferences, ArrayList newCollections)
    {
        FieldDescriptorDef      copyFieldDef;
        ReferenceDescriptorDef  copyRefDef;
        CollectionDescriptorDef copyCollDef;

        // note that we also copy features with the ignore-property set to true
        // they will be ignored later on
        for (Iterator fieldIt = baseDef.getFields(); fieldIt.hasNext();)
        {
            copyFieldDef = cloneField((FieldDescriptorDef)fieldIt.next(), null);
            if (!contains(newFields, copyFieldDef))
            {
                copyFieldDef.setInherited();
                newFields.add(copyFieldDef);
            }
        }
        for (Iterator refIt = baseDef.getReferences(); refIt.hasNext();)
        {
            copyRefDef = cloneReference((ReferenceDescriptorDef)refIt.next(), null);
            if (!contains(newReferences, copyRefDef))
            {
                copyRefDef.setInherited();
                newReferences.add(copyRefDef);
            }
        }
        for (Iterator collIt = baseDef.getCollections(); collIt.hasNext();)
        {
            copyCollDef = cloneCollection((CollectionDescriptorDef)collIt.next(), null);
            if (!contains(newCollections, copyCollDef))
            {
                copyCollDef.setInherited();
                newCollections.add(copyCollDef);
            }
        }
    }

    /**
     * Clones the features of the given nested object (using modifications if available) and puts them into
     * the given lists.
     * 
     * @param nestedDef      The nested object
     * @param newFields      Recieves the field copies
     * @param newReferences  Recieves the reference copies
     * @param newCollections Recieves the collection copies
     */
    private void cloneNestedFeatures(NestedDef nestedDef, ArrayList newFields, ArrayList newReferences, ArrayList newCollections)
    {
        ClassDescriptorDef      nestedClassDef = nestedDef.getNestedType();
        String                  prefix         = nestedDef.getName()+"::";
        FieldDescriptorDef      copyFieldDef;
        ReferenceDescriptorDef  copyRefDef;
        CollectionDescriptorDef copyCollDef;
        StringBuffer            newForeignkey;

        for (Iterator fieldIt = nestedClassDef.getFields(); fieldIt.hasNext();)
        {
            copyFieldDef = cloneField((FieldDescriptorDef)fieldIt.next(), prefix);
            if (!contains(newFields, copyFieldDef))
            {
                copyFieldDef.setNested();
                newFields.add(copyFieldDef);
            }
        }
        for (Iterator refIt = nestedClassDef.getReferences(); refIt.hasNext();)
        {
            copyRefDef = cloneReference((ReferenceDescriptorDef)refIt.next(), prefix);
            if (contains(newReferences, copyRefDef))
            {
                continue;
            }
            copyRefDef.setNested();

            // we have to modify the foreignkey setting as it specifies a field of the same (nested) class
            newForeignkey = new StringBuffer();

            for (CommaListIterator it = new CommaListIterator(copyRefDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY)); it.hasNext();)
            {
                if (newForeignkey.length() > 0)
                {
                    newForeignkey.append(",");
                }
                newForeignkey.append(prefix);
                newForeignkey.append(it.next());
            }
            copyRefDef.setProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY, newForeignkey.toString());
            newReferences.add(copyRefDef);
        }
        for (Iterator collIt = nestedClassDef.getCollections(); collIt.hasNext();)
        {
            copyCollDef = cloneCollection((CollectionDescriptorDef)collIt.next(), prefix);
            if (!contains(newCollections, copyCollDef))
            {
                copyCollDef.setNested();
                newCollections.add(copyCollDef);
            }
        }
    }

    /**
     * Clones the given field.
     * 
     * @param fieldDef The field descriptor
     * @param prefix   A prefix for the name
     * @return The cloned field
     */
    private FieldDescriptorDef cloneField(FieldDescriptorDef fieldDef, String prefix)
    {
        FieldDescriptorDef copyFieldDef = new FieldDescriptorDef(fieldDef, prefix);

        copyFieldDef.setOwner(this);
        // we remove properties that are only relevant to the class the features are declared in
        copyFieldDef.setProperty(PropertyHelper.OJB_PROPERTY_IGNORE, null);

        Properties mod = getModification(copyFieldDef.getName());

        if (mod != null)
        {
            if (!PropertyHelper.toBoolean(mod.getProperty(PropertyHelper.OJB_PROPERTY_IGNORE), false) &&
                hasFeature(copyFieldDef.getName()))
            {
                LogHelper.warn(true,
                               ClassDescriptorDef.class,
                               "process",
                               "Class "+getName()+" has a feature that has the same name as its included field "+
                               copyFieldDef.getName()+" from class "+fieldDef.getOwner().getName()); 
            }
            copyFieldDef.applyModifications(mod);
        }
        return copyFieldDef;
    }

    /**
     * Clones the given reference.
     * 
     * @param refDef The reference descriptor
     * @param prefix A prefix for the name
     * @return The cloned reference
     */
    private ReferenceDescriptorDef cloneReference(ReferenceDescriptorDef refDef, String prefix)
    {
        ReferenceDescriptorDef copyRefDef = new ReferenceDescriptorDef(refDef, prefix);

        copyRefDef.setOwner(this);
        // we remove properties that are only relevant to the class the features are declared in
        copyRefDef.setProperty(PropertyHelper.OJB_PROPERTY_IGNORE, null);
        
        Properties mod = getModification(copyRefDef.getName());

        if (mod != null)
        {
            if (!PropertyHelper.toBoolean(mod.getProperty(PropertyHelper.OJB_PROPERTY_IGNORE), false) &&
                hasFeature(copyRefDef.getName()))
            {
                LogHelper.warn(true,
                               ClassDescriptorDef.class,
                               "process",
                               "Class "+getName()+" has a feature that has the same name as its included reference "+
                               copyRefDef.getName()+" from class "+refDef.getOwner().getName()); 
            }
            copyRefDef.applyModifications(mod);
        }
        return copyRefDef;
    }

    /**
     * Clones the given collection.
     * 
     * @param collDef The collection descriptor
     * @param prefix  A prefix for the name
     * @return The cloned collection
     */
    private CollectionDescriptorDef cloneCollection(CollectionDescriptorDef collDef, String prefix)
    {
        CollectionDescriptorDef copyCollDef = new CollectionDescriptorDef(collDef, prefix);

        copyCollDef.setOwner(this);
        // we remove properties that are only relevant to the class the features are declared in
        copyCollDef.setProperty(PropertyHelper.OJB_PROPERTY_IGNORE, null);

        Properties mod = getModification(copyCollDef.getName());

        if (mod != null)
        {
            if (!PropertyHelper.toBoolean(mod.getProperty(PropertyHelper.OJB_PROPERTY_IGNORE), false) &&
                hasFeature(copyCollDef.getName()))
            {
                LogHelper.warn(true,
                               ClassDescriptorDef.class,
                               "process",
                               "Class "+getName()+" has a feature that has the same name as its included collection "+
                               copyCollDef.getName()+" from class "+collDef.getOwner().getName()); 
            }
            copyCollDef.applyModifications(mod);
        }
        return copyCollDef;
    }

    /**
     * Adds a direct base type.
     *  
     * @param baseType The base type descriptor
     */
    public void addDirectBaseType(ClassDescriptorDef baseType)
    {
        _directBaseTypes.put(baseType.getName(), baseType);
    }

    /**
     * Returns the direct base types.
     * 
     * @return An iterator of the direct base types
     */
    public Iterator getDirectBaseTypes()
    {
        return _directBaseTypes.values().iterator();
    }
    
    /**
     * Returns all base types.
     * 
     * @return An iterator of the base types
     */
    public Iterator getAllBaseTypes()
    {
        ArrayList baseTypes = new ArrayList();

        baseTypes.addAll(_directBaseTypes.values());

        for (int idx = baseTypes.size() - 1; idx >= 0; idx--)
        {
            ClassDescriptorDef curClassDef = (ClassDescriptorDef)baseTypes.get(idx);

            for (Iterator it = curClassDef.getDirectBaseTypes(); it.hasNext();)
            {
                ClassDescriptorDef curBaseTypeDef = (ClassDescriptorDef)it.next();

                if (!baseTypes.contains(curBaseTypeDef))
                {
                    baseTypes.add(0, curBaseTypeDef);
                    idx++;
                }
            }
        }
        return baseTypes.iterator();
    }

    /**
     * Adds a sub type.
     *
     * @param subType The sub type definition
     */
    public void addExtentClass(ClassDescriptorDef subType)
    {
        if (!_extents.contains(subType))
        {
            _extents.add(subType);
        }
    }

    /**
     * Returns an iterator of the extents of this class.
     *
     * @return The extents iterator
     */
    public Iterator getExtentClasses()
    {
        // we sort the extents prior to returning them
        Collections.sort(_extents, new DefBaseComparator());
        return _extents.iterator();
    }

    /**
     * Returns an iterator of all direct and indirect extents of this class.
     *
     * @return The extents iterator
     */
    public Iterator getAllExtentClasses()
    {
        ArrayList subTypes = new ArrayList();

        subTypes.addAll(_extents);

        for (int idx = 0; idx < subTypes.size(); idx++)
        {
            ClassDescriptorDef curClassDef = (ClassDescriptorDef)subTypes.get(idx);

            for (Iterator it = curClassDef.getExtentClasses(); it.hasNext();)
            {
                ClassDescriptorDef curSubTypeDef = (ClassDescriptorDef)it.next();

                if (!subTypes.contains(curSubTypeDef))
                {
                    subTypes.add(curSubTypeDef);
                }
            }
        }
        return subTypes.iterator();
    }

    /**
     * Determines whether this class can be instantiated, i.e. not an interface or abstract class.
     * 
     * @return <code>true</code> if objects can be created of this class
     */
    public boolean canBeInstantiated()
    {
        return !_origin.isAbstract() && !_origin.isInterface();
    }
    
    /**
     * Determines whether this class has a feature (field, reference, collection) of the given name.
     * 
     * @param name The name
     * @return <code>true</code> if there is such a feature
     */
    public boolean hasFeature(String name)
    {
        return getFeature(name) != null;
    }
    
    /**
     * Returns the feature (field, reference, collection) of the given name.
     * 
     * @param name The name
     * @return The feature or <code>null</code> if there is no such feature in the current class
     */
    public DefBase getFeature(String name)
    {
        DefBase result = getField(name);

        if (result == null)
        {
            result = getReference(name);
        }
        if (result == null)
        {
            result = getCollection(name);
        }
        return result;
    }

    /**
     * Adds a field descriptor to this class.
     *
     * @param fieldDef The field descriptor
     */
    public void addField(FieldDescriptorDef fieldDef)
    {
        fieldDef.setOwner(this);
        _fields.add(fieldDef);
    }

    /**
     * Adds a clone of the given field descriptor to this class.
     *
     * @param fieldDef The field descriptor
     */
    public void addFieldClone(FieldDescriptorDef fieldDef)
    {
        _fields.add(cloneField(fieldDef, ""));
    }

    /**
     * Returns the field definition with the specified name.
     *
     * @param name The name of the desired field
     * @return The field definition or <code>null</code> if there is no such field
     */
    public FieldDescriptorDef getField(String name)
    {
        FieldDescriptorDef fieldDef = null;

        for (Iterator it = _fields.iterator(); it.hasNext(); )
        {
            fieldDef = (FieldDescriptorDef)it.next();
            if (fieldDef.getName().equals(name))
            {
                return fieldDef;
            }
        }
        return null;
    }

    /**
     * Returns an iterator of the fields definitions.
     *
     * @return The field iterator
     */
    public Iterator getFields()
    {
        return _fields.iterator();
    }

    /**
     * Returns the field descriptors given in the the field names list.
     * 
     * @param fieldNames The field names, separated by commas
     * @return The field descriptors in the order given by the field names
     * @throws NoSuchFieldException If a field hasn't been found
     */
    public ArrayList getFields(String fieldNames) throws NoSuchFieldException
    {
        ArrayList          result    = new ArrayList();
        FieldDescriptorDef fieldDef;
        String             name;

        for (CommaListIterator it = new CommaListIterator(fieldNames); it.hasNext();)
        {
            name = it.getNext();
            fieldDef = getField(name);
            if (fieldDef == null)
            {
                throw new NoSuchFieldException(name);
            }
            result.add(fieldDef);
        }
        return result;
    }

    /**
     * Returns the primarykey fields.
     * 
     * @return The field descriptors of the primarykey fields
     */
    public ArrayList getPrimaryKeys()
    {
        ArrayList          result = new ArrayList();
        FieldDescriptorDef fieldDef;

        for (Iterator it = getFields(); it.hasNext();)
        {
            fieldDef = (FieldDescriptorDef)it.next();
            if (fieldDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_PRIMARYKEY, false))
            {
                result.add(fieldDef);
            }
        }
        return result;
    }
    
    /**
     * Adds a reference descriptor to this class.
     *
     * @param refDef The reference descriptor
     */
    public void addReference(ReferenceDescriptorDef refDef)
    {
        refDef.setOwner(this);
        _references.add(refDef);
    }

    /**
     * Returns a reference definition of the given name if it exists.
     *
     * @param name  The name of the reference
     * @return      The reference def or <code>null</code> if there is no such reference
     */
    public ReferenceDescriptorDef getReference(String name)
    {
        ReferenceDescriptorDef refDef;

        for (Iterator it = _references.iterator(); it.hasNext(); )
        {
            refDef = (ReferenceDescriptorDef)it.next();
            if (refDef.getName().equals(name))
            {
                return refDef;
            }
        }
        return null;
    }

    /**
     * Returns an iterator of the reference definitionss.
     *
     * @return The iterator
     */
    public Iterator getReferences()
    {
        return _references.iterator();
    }

    /**
     * Adds a collection descriptor to this class.
     *
     * @param collDef The collection descriptor
     */
    public void addCollection(CollectionDescriptorDef collDef)
    {
        collDef.setOwner(this);
        _collections.add(collDef);
    }

    /**
     * Returns the collection definition of the given name if it exists.
     *
     * @param name The name of the collection
     * @return The collection definition or <code>null</code> if there is no such collection
     */
    public CollectionDescriptorDef getCollection(String name)
    {
        CollectionDescriptorDef collDef = null;

        for (Iterator it = _collections.iterator(); it.hasNext(); )
        {
            collDef = (CollectionDescriptorDef)it.next();
            if (collDef.getName().equals(name))
            {
                return collDef;
            }
        }
        return null;
    }

    /**
     * Returns an iterator of the collection definitions.
     *
     * @return The collection iterator
     */
    public Iterator getCollections()
    {
        return _collections.iterator();
    }

    /**
     * Adds a nested object to this class.
     *
     * @param nestedDef The nested object
     */
    public void addNested(NestedDef nestedDef)
    {
        nestedDef.setOwner(this);
        _nested.add(nestedDef);
    }

    /**
     * Returns the nested object definition with the specified name.
     *
     * @param name The name of the attribute of the nested object
     * @return The nested object definition or <code>null</code> if there is no such nested object
     */
    public NestedDef getNested(String name)
    {
        NestedDef nestedDef = null;

        for (Iterator it = _nested.iterator(); it.hasNext(); )
        {
            nestedDef = (NestedDef)it.next();
            if (nestedDef.getName().equals(name))
            {
                return nestedDef;
            }
        }
        return null;
    }

    /**
     * Returns an iterator of the nested object definitions.
     *
     * @return The nested object iterator
     */
    public Iterator getNested()
    {
        return _nested.iterator();
    }

    /**
     * Adds an index descriptor definition to this class descriptor.
     *
     * @param indexDef The index descriptor definition
     */
    public void addIndexDescriptor(IndexDescriptorDef indexDef)
    {
        indexDef.setOwner(this);
        _indexDescriptors.add(indexDef);
    }

    /**
     * Returns the index descriptor definition of the given name if it exists.
     *
     * @param name The name of the index
     * @return The index descriptor definition or <code>null</code> if there is no such index
     */
    public IndexDescriptorDef getIndexDescriptor(String name)
    {
        IndexDescriptorDef indexDef = null;

        for (Iterator it = _indexDescriptors.iterator(); it.hasNext(); )
        {
            indexDef = (IndexDescriptorDef)it.next();
            if (indexDef.getName().equals(name))
            {
                return indexDef;
            }
        }
        return null;
    }

    /**
     * Returns an iterator of the index descriptor definitions.
     *
     * @return The index descriptor iterator
     */
    public Iterator getIndexDescriptors()
    {
        return _indexDescriptors.iterator();
    }

    /**
     * Sets an object cache definition to an object cache of the given name (if necessary), and returns it.
     *
     * @param name The name of the object cache class
     * @return The object cache definition
     */
    public ObjectCacheDef setObjectCache(String name)
    {
        if ((_objectCache == null) || !_objectCache.getName().equals(name))
        {    
            _objectCache = new ObjectCacheDef(name);
    
            _objectCache.setOwner(this);
        }
        return _objectCache;
    }

    /**
     * Returns the object cache definition.
     *
     * @return The object cache definition
     */
    public ObjectCacheDef getObjectCache()
    {
        return _objectCache;
    }

    /**
     * Adds a procedure definition to this class descriptor.
     *
     * @param procDef The procedure definition
     */
    public void addProcedure(ProcedureDef procDef)
    {
        procDef.setOwner(this);
        _procedures.put(procDef.getName(), procDef);
    }

    /**
     * Returns the procedure definition of the given name if it exists.
     *
     * @param name The name of the procedure
     * @return The procedure definition or <code>null</code> if there is no such procedure
     */
    public ProcedureDef getProcedure(String name)
    {
        return (ProcedureDef)_procedures.get(name);
    }

    /**
     * Returns an iterator of the procedure definitions.
     *
     * @return The procedure iterator
     */
    public Iterator getProcedures()
    {
        return _procedures.values().iterator();
    }
    
    /**
     * Adds a procedure argument definition to this class descriptor.
     *
     * @param argDef The procedure argument definition
     */
    public void addProcedureArgument(ProcedureArgumentDef argDef)
    {
        argDef.setOwner(this);
        _procedureArguments.put(argDef.getName(), argDef);
    }

    /**
     * Returns the procedure argument definition of the given name if it exists.
     *
     * @param name The name of the procedure argument
     * @return The procedure argument definition or <code>null</code> if there is no such argument
     */
    public ProcedureArgumentDef getProcedureArgument(String name)
    {
        return (ProcedureArgumentDef)_procedureArguments.get(name);
    }

    /**
     * Returns an iterator of all procedure argument definitions.
     *
     * @return The procedure argument iterator
     */
    public Iterator getProcedureArguments()
    {
        return _procedureArguments.values().iterator();
    }

    /**
     * Adds a modification for the given inherited field/reference/collection.
     *
     * @param name The name of the inherited field, reference or collection
     * @param mods The modified properties
     */
    public void addModification(String name, Properties mods)
    {
        _modifications.put(name, mods);
    }

    /**
     * Returns an iterator of all field/reference/collection names for which modifications are stored in this class def.
     * 
     * @return The iterator
     */
    public Iterator getModificationNames()
    {
        return _modifications.keySet().iterator();
    }

    /**
     * Returns the modification for the inherited field/reference/collection with the given name.
     * 
     * @param name The name of the inherited field, reference or collection
     * @return The modified properties or <code>null</code> if there are no modifications for it
     */
    public Properties getModification(String name)
    {
        return (Properties)_modifications.get(name);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object other)
    {
        if ((other == null) || !(other instanceof ClassDescriptorDef))
        {
            return false;
        }
        return _origin == ((ClassDescriptorDef)other)._origin; 
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return _origin.hashCode();
    }
}
