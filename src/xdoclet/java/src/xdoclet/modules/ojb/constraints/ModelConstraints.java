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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections.SequencedHashMap;

import xdoclet.modules.ojb.CommaListIterator;
import xdoclet.modules.ojb.LogHelper;
import xdoclet.modules.ojb.model.ClassDescriptorDef;
import xdoclet.modules.ojb.model.CollectionDescriptorDef;
import xdoclet.modules.ojb.model.FeatureDescriptorDef;
import xdoclet.modules.ojb.model.FieldDescriptorDef;
import xdoclet.modules.ojb.model.ModelDef;
import xdoclet.modules.ojb.model.PropertyHelper;
import xdoclet.modules.ojb.model.ReferenceDescriptorDef;

/**
 * Checks constraints that span deal with parts of the model, not just with one class.
 * This for instance means relationships (collections, references).
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ModelConstraints extends ConstraintsBase
{
    /**
     * Checks the given model.
     * 
     * @param modelDef   The model
     * @param checkLevel The amount of checks to perform
     * @exception ConstraintException If a constraint has been violated
     */
    public void check(ModelDef modelDef, String checkLevel) throws ConstraintException
    {
        ensureReferencedKeys(modelDef, checkLevel);
        checkReferenceForeignkeys(modelDef, checkLevel);
        checkCollectionForeignkeys(modelDef, checkLevel);
        checkKeyModifications(modelDef, checkLevel);
    }

    /**
     * Ensures that the primary/foreign keys referenced by references/collections are present
     * in the target type even if generate-table-info="false", by evaluating the subtypes
     * of the target type.
     * 
     * @param modelDef   The model
     * @param checkLevel The current check level (this constraint is always checked)
     * @throws ConstraintException If there is an error with the keys of the subtypes or there
     *                             ain't any subtypes 
     */
    private void ensureReferencedKeys(ModelDef modelDef, String checkLevel) throws ConstraintException
    {
        ClassDescriptorDef      classDef;
        CollectionDescriptorDef collDef;
        ReferenceDescriptorDef  refDef;

        for (Iterator it = modelDef.getClasses(); it.hasNext();)
        {
            classDef = (ClassDescriptorDef)it.next();
            for (Iterator refIt = classDef.getReferences(); refIt.hasNext();)
            {
                refDef = (ReferenceDescriptorDef)refIt.next();
                if (!refDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false))
                {
                    ensureReferencedPKs(modelDef, refDef);
                }
            }
            for (Iterator collIt = classDef.getCollections(); collIt.hasNext();)
            {
                collDef = (CollectionDescriptorDef)collIt.next();
                if (!collDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false))
                {
                    if (collDef.hasProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE))
                    {
                        ensureReferencedPKs(modelDef, collDef);
                    }
                    else
                    {
                        ensureReferencedFKs(modelDef, collDef);
                    }
                }
            }
        }
    }

    /**
     * Ensures that the primary keys required by the given reference are present in the referenced class.
     * 
     * @param modelDef The model
     * @param refDef   The reference
     * @throws ConstraintException If there is a conflict between the primary keys
     */
    private void ensureReferencedPKs(ModelDef modelDef, ReferenceDescriptorDef refDef) throws ConstraintException
    {
        String             targetClassName = refDef.getProperty(PropertyHelper.OJB_PROPERTY_CLASS_REF);
        ClassDescriptorDef targetClassDef  = modelDef.getClass(targetClassName);

        ensurePKsFromHierarchy(targetClassDef);
    }

    /**
     * Ensures that the primary keys required by the given collection with indirection table are present in
     * the element class.
     * 
     * @param modelDef The model
     * @param collDef  The collection
     * @throws ConstraintException If there is a problem with the fitting collection (if any) or the primary keys
     */
    private void ensureReferencedPKs(ModelDef modelDef, CollectionDescriptorDef collDef) throws ConstraintException
    {
        String             elementClassName   = collDef.getProperty(PropertyHelper.OJB_PROPERTY_ELEMENT_CLASS_REF);
        ClassDescriptorDef elementClassDef    = modelDef.getClass(elementClassName);
        String             indirTable         = collDef.getProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE);
        String             localKey           = collDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY);
        String             remoteKey          = collDef.getProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY);
        boolean            hasRemoteKey       = remoteKey != null;
        ArrayList          fittingCollections = new ArrayList();

        // we're checking for the fitting remote collection(s) and also
        // use their foreignkey as remote-foreignkey in the original collection definition
        for (Iterator it = elementClassDef.getAllExtentClasses(); it.hasNext();)
        {
            ClassDescriptorDef subTypeDef = (ClassDescriptorDef)it.next();

            // find the collection in the element class that has the same indirection table
            for (Iterator collIt = subTypeDef.getCollections(); collIt.hasNext();)
            {
                CollectionDescriptorDef curCollDef = (CollectionDescriptorDef)collIt.next();

                if (indirTable.equals(curCollDef.getProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE)) &&
                    (collDef != curCollDef) &&
                    (!hasRemoteKey || CommaListIterator.sameLists(remoteKey, curCollDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY))) &&
                    (!curCollDef.hasProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY) ||
                         CommaListIterator.sameLists(localKey, curCollDef.getProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY))))
                {
                    fittingCollections.add(curCollDef);
                }
            }
        }
        if (!fittingCollections.isEmpty())
        {
            // if there is more than one, check that they match, i.e. that they all have the same foreignkeys
            if (!hasRemoteKey && (fittingCollections.size() > 1))
            {
                CollectionDescriptorDef firstCollDef = (CollectionDescriptorDef)fittingCollections.get(0);
                String                  foreignKey   = firstCollDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY);

                for (int idx = 1; idx < fittingCollections.size(); idx++)
                {
                    CollectionDescriptorDef curCollDef = (CollectionDescriptorDef)fittingCollections.get(idx);

                    if (!CommaListIterator.sameLists(foreignKey, curCollDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY)))
                    {
                        throw new ConstraintException("Cannot determine the element-side collection that corresponds to the collection "+
                                                      collDef.getName()+" in type "+collDef.getOwner().getName()+
                                                      " because there are at least two different collections that would fit."+
                                                      " Specifying remote-foreignkey in the original collection "+collDef.getName()+
                                                      " will perhaps help");
                    }
                }
                // store the found keys at the collections
                collDef.setProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY, foreignKey);
                for (int idx = 0; idx < fittingCollections.size(); idx++)
                {
                    CollectionDescriptorDef curCollDef = (CollectionDescriptorDef)fittingCollections.get(idx);

                    curCollDef.setProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY, localKey);
                }
            }
        }

        // copy subclass pk fields into target class (if not already present)
        ensurePKsFromHierarchy(elementClassDef);
    }

    /**
     * Ensures that the foreign keys required by the given collection are present in the element class.
     * 
     * @param modelDef The model
     * @param collDef  The collection
     * @throws ConstraintException If there is a problem with the foreign keys
     */
    private void ensureReferencedFKs(ModelDef modelDef, CollectionDescriptorDef collDef) throws ConstraintException
    {
        String             elementClassName = collDef.getProperty(PropertyHelper.OJB_PROPERTY_ELEMENT_CLASS_REF);
        ClassDescriptorDef elementClassDef  = modelDef.getClass(elementClassName);
        String             fkFieldNames     = collDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY);
        ArrayList          missingFields    = new ArrayList();
        SequencedHashMap   fkFields         = new SequencedHashMap();

        // first we gather all field names
        for (CommaListIterator it = new CommaListIterator(fkFieldNames); it.hasNext();)
        {
            String             fieldName = (String)it.next();
            FieldDescriptorDef fieldDef  = elementClassDef.getField(fieldName);

            if (fieldDef == null)
            {
                missingFields.add(fieldName);
            }
            fkFields.put(fieldName, fieldDef);
        }

        // next we traverse all sub types and gather fields as we go
        for (Iterator it = elementClassDef.getAllExtentClasses(); it.hasNext() && !missingFields.isEmpty();)
        {
            ClassDescriptorDef subTypeDef = (ClassDescriptorDef)it.next();

            for (int idx = 0; idx < missingFields.size();)
            {
                FieldDescriptorDef fieldDef = subTypeDef.getField((String)missingFields.get(idx));

                if (fieldDef != null)
                {
                    fkFields.put(fieldDef.getName(), fieldDef);
                    missingFields.remove(idx);
                }
                else
                {
                    idx++;
                }
            }
        }
        if (!missingFields.isEmpty())
        {
            throw new ConstraintException("Cannot find field "+missingFields.get(0).toString()+" in the hierarchy with root type "+
                                          elementClassDef.getName()+" which is used as foreignkey in collection "+
                                          collDef.getName()+" in "+collDef.getOwner().getName());
        }

        // copy the found fields into the element class
        ensureFields(elementClassDef, fkFields.values());
    }

    /**
     * Gathers the pk fields from the hierarchy of the given class, and copies them into the class.
     * 
     * @param classDef The root of the hierarchy
     * @throws ConstraintException If there is a conflict between the pk fields 
     */
    private void ensurePKsFromHierarchy(ClassDescriptorDef classDef) throws ConstraintException
    {
        SequencedHashMap pks = new SequencedHashMap();

        for (Iterator it = classDef.getAllExtentClasses(); it.hasNext();)
        {
            ClassDescriptorDef subTypeDef = (ClassDescriptorDef)it.next();

            ArrayList subPKs = subTypeDef.getPrimaryKeys();

            // check against already present PKs
            for (Iterator pkIt = subPKs.iterator(); pkIt.hasNext();)
            {
                FieldDescriptorDef fieldDef   = (FieldDescriptorDef)pkIt.next();
                FieldDescriptorDef foundPKDef = (FieldDescriptorDef)pks.get(fieldDef.getName());

                if (foundPKDef != null)
                {
                    if (!isEqual(fieldDef, foundPKDef))
                    {
                        throw new ConstraintException("Cannot pull up the declaration of the required primary key "+fieldDef.getName()+
                                                      " because its definitions in "+fieldDef.getOwner().getName()+" and "+
                                                      foundPKDef.getOwner().getName()+" differ");
                    }
                }
                else
                {
                    pks.put(fieldDef.getName(), fieldDef);
                }
            }
        }

        ensureFields(classDef, pks.values());
    }
    
    /**
     * Ensures that the specified fields are present in the given class.
     * 
     * @param classDef The class to copy the fields into
     * @param fields   The fields to copy
     * @throws ConstraintException If there is a conflict between the new fields and fields in the class  
     */
    private void ensureFields(ClassDescriptorDef classDef, Collection fields) throws ConstraintException
    {
        boolean forceVirtual = !classDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_GENERATE_REPOSITORY_INFO, true);

        for (Iterator it = fields.iterator(); it.hasNext();)
        {
            FieldDescriptorDef fieldDef = (FieldDescriptorDef)it.next();

            // First we check whether this field is already present in the class
            FieldDescriptorDef foundFieldDef = classDef.getField(fieldDef.getName());

            if (foundFieldDef != null)
            {
                if (isEqual(fieldDef, foundFieldDef))
                {
                    if (forceVirtual)
                    {
                        foundFieldDef.setProperty(PropertyHelper.OJB_PROPERTY_VIRTUAL_FIELD, "true");
                    }
                    continue;
                }
                else
                {
                    throw new ConstraintException("Cannot pull up the declaration of the required field "+fieldDef.getName()+
                            " from type "+fieldDef.getOwner().getName()+" to basetype "+classDef.getName()+
                            " because there is already a different field of the same name");
                }
            }

            // perhaps a reference or collection ?
            if (classDef.getCollection(fieldDef.getName()) != null)
            {
                throw new ConstraintException("Cannot pull up the declaration of the required field "+fieldDef.getName()+
                                              " from type "+fieldDef.getOwner().getName()+" to basetype "+classDef.getName()+
                                              " because there is already a collection of the same name");
            }
            if (classDef.getReference(fieldDef.getName()) != null)
            {
                throw new ConstraintException("Cannot pull up the declaration of the required field "+fieldDef.getName()+
                                              " from type "+fieldDef.getOwner().getName()+" to basetype "+classDef.getName()+
                                              " because there is already a reference of the same name");
            }
            classDef.addFieldClone(fieldDef);
            classDef.getField(fieldDef.getName()).setProperty(PropertyHelper.OJB_PROPERTY_VIRTUAL_FIELD, "true");
        }
    }

    /**
     * Tests whether the two field descriptors are equal, i.e. have same name, same column
     * and same jdbc-type.
     * 
     * @param first  The first field
     * @param second The second field
     * @return <code>true</code> if they are equal
     */
    private boolean isEqual(FieldDescriptorDef first, FieldDescriptorDef second)
    {
        return first.getName().equals(second.getName()) &&
               first.getProperty(PropertyHelper.OJB_PROPERTY_COLUMN).equals(second.getProperty(PropertyHelper.OJB_PROPERTY_COLUMN)) &&
               first.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE).equals(second.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE));
    }

    /**
     * Checks the foreignkeys of all collections in the model.
     * 
     * @param modelDef   The model
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     * @exception ConstraintException If the value for foreignkey is invalid
     */
    private void checkCollectionForeignkeys(ModelDef modelDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        ClassDescriptorDef      classDef;
        CollectionDescriptorDef collDef;

        for (Iterator it = modelDef.getClasses(); it.hasNext();)
        {
            classDef = (ClassDescriptorDef)it.next();
            for (Iterator collIt = classDef.getCollections(); collIt.hasNext();)
            {
                collDef = (CollectionDescriptorDef)collIt.next();
                if (!collDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false))
                {
                    if (collDef.hasProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE))
                    {
                        checkIndirectionTable(modelDef, collDef);
                    }
                    else
                    {    
                        checkCollectionForeignkeys(modelDef, collDef);
                    }
                }
            }
        }
    }

    /**
     * Checks the indirection-table and foreignkey of the collection. This constraint also ensures that
     * for the collections on both ends (if they exist), the remote-foreignkey property is set correctly.
     * 
     * @param modelDef The model
     * @param collDef  The collection descriptor
     * @exception ConstraintException If the value for foreignkey is invalid
     */
    private void checkIndirectionTable(ModelDef modelDef, CollectionDescriptorDef collDef) throws ConstraintException
    {
        String foreignkey = collDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY);

        if ((foreignkey == null) || (foreignkey.length() == 0))
        {
            throw new ConstraintException("The collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" has no foreignkeys");
        }

        // we know that the class is present because the collection constraints have been checked already
        // TODO: we must check whether there is a collection at the other side; if the type does not map to a
        // table then we have to check its subtypes
        String                  elementClassName = collDef.getProperty(PropertyHelper.OJB_PROPERTY_ELEMENT_CLASS_REF);
        ClassDescriptorDef      elementClass     = modelDef.getClass(elementClassName);
        CollectionDescriptorDef remoteCollDef    = collDef.getRemoteCollection();

        if (remoteCollDef == null)
        {
            // error if there is none and we don't have remote-foreignkey specified
            if (!collDef.hasProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY))
            {
                throw new ConstraintException("The collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" must specify remote-foreignkeys as the class on the other side of the m:n association has no corresponding collection");
            }
        }
        else
        {    
            String remoteKeys2 = remoteCollDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY);

            if (collDef.hasProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY))
            {
                // check that the specified remote-foreignkey equals the remote foreignkey setting
                String remoteKeys1 = collDef.getProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY);

                if (!CommaListIterator.sameLists(remoteKeys1, remoteKeys2))
                {
                    throw new ConstraintException("The remote-foreignkey property specified for collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" doesn't match the foreignkey property of the corresponding collection "+remoteCollDef.getName()+" in class "+elementClass.getName());
                }
            }
            else
            {
                // ensure the remote-foreignkey setting
                collDef.setProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY, remoteKeys2);
            }
        }

        // issue a warning if the foreignkey and remote-foreignkey columns are the same (issue OJB-67)
        String remoteForeignkey = collDef.getProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY);

        if (CommaListIterator.sameLists(foreignkey, remoteForeignkey))
        {
            LogHelper.warn(true,
                           getClass(),
                           "checkIndirectionTable",
                           "The remote foreignkey ("+remoteForeignkey+") for the collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" is identical (ignoring case) to the foreign key ("+foreignkey+").");
        }

        // for torque we generate names for the m:n relation that are unique across inheritance
        // but only if we don't have inherited collections
        if (collDef.getOriginal() != null)
        {
            CollectionDescriptorDef origDef       = (CollectionDescriptorDef)collDef.getOriginal();
            CollectionDescriptorDef origRemoteDef = origDef.getRemoteCollection();

            // we're removing any torque relation name properties from the base collection
            origDef.setProperty(PropertyHelper.TORQUE_PROPERTY_RELATION_NAME, null);
            origDef.setProperty(PropertyHelper.TORQUE_PROPERTY_INV_RELATION_NAME, null);
            if (origRemoteDef != null)
            {
                origRemoteDef.setProperty(PropertyHelper.TORQUE_PROPERTY_RELATION_NAME, null);
                origRemoteDef.setProperty(PropertyHelper.TORQUE_PROPERTY_INV_RELATION_NAME, null);
            }
        }
        else if (!collDef.hasProperty(PropertyHelper.TORQUE_PROPERTY_RELATION_NAME))
        {
            if (remoteCollDef == null)
            {
                collDef.setProperty(PropertyHelper.TORQUE_PROPERTY_RELATION_NAME, collDef.getName());
                collDef.setProperty(PropertyHelper.TORQUE_PROPERTY_INV_RELATION_NAME, "inverse "+collDef.getName());
            }
            else
            {    
                String relName = collDef.getName()+"-"+remoteCollDef.getName();
    
                collDef.setProperty(PropertyHelper.TORQUE_PROPERTY_RELATION_NAME, relName);
                remoteCollDef.setProperty(PropertyHelper.TORQUE_PROPERTY_INV_RELATION_NAME, relName);
    
                relName = remoteCollDef.getName()+"-"+collDef.getName();
    
                collDef.setProperty(PropertyHelper.TORQUE_PROPERTY_INV_RELATION_NAME, relName);
                remoteCollDef.setProperty(PropertyHelper.TORQUE_PROPERTY_RELATION_NAME, relName);
            }
        }
    }

    /**
     * Checks the foreignkeys of the collection.
     * 
     * @param modelDef The model
     * @param collDef  The collection descriptor
     * @exception ConstraintException If the value for foreignkey is invalid
     */
    private void checkCollectionForeignkeys(ModelDef modelDef, CollectionDescriptorDef collDef) throws ConstraintException
    {
        String foreignkey = collDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY);

        if ((foreignkey == null) || (foreignkey.length() == 0))
        {
            throw new ConstraintException("The collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" has no foreignkeys");
        }

        String remoteForeignkey = collDef.getProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY);

        if ((remoteForeignkey != null) && (remoteForeignkey.length() > 0))
        {
            // warning because a remote-foreignkey was specified for a 1:n collection (issue OJB-67)
            LogHelper.warn(true,
                           getClass(),
                           "checkCollectionForeignkeys",
                           "For the collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+", a remote foreignkey was specified though it is a 1:n, not a m:n collection");
        }
        
        ClassDescriptorDef ownerClass       = (ClassDescriptorDef)collDef.getOwner();
        ArrayList          primFields       = ownerClass.getPrimaryKeys();
        String             elementClassName = collDef.getProperty(PropertyHelper.OJB_PROPERTY_ELEMENT_CLASS_REF);
        ArrayList          queue            = new ArrayList();
        ClassDescriptorDef elementClass;
        ArrayList          keyFields;
        FieldDescriptorDef keyField;
        FieldDescriptorDef primField;
        String             primType;
        String             keyType;
        
        // we know that the class is present because the collection constraints have been checked already
        queue.add(modelDef.getClass(elementClassName));
        while (!queue.isEmpty())
        {
            elementClass = (ClassDescriptorDef)queue.get(0);
            queue.remove(0);

            for (Iterator it = elementClass.getExtentClasses(); it.hasNext();)
            {
                queue.add(it.next());
            }
            if (!elementClass.getBooleanProperty(PropertyHelper.OJB_PROPERTY_GENERATE_REPOSITORY_INFO, true))
            {
                continue;
            }
            try
            {
                keyFields = elementClass.getFields(foreignkey);
            }
            catch (NoSuchFieldException ex)
            {
                throw new ConstraintException("The collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" specifies a foreignkey "+ex.getMessage()+" that is not a persistent field in the element class (or its subclass) "+elementClass.getName());
            }
            if (primFields.size() != keyFields.size())
            {
                throw new ConstraintException("The number of foreignkeys ("+keyFields.size()+") of the collection "+collDef.getName()+" in class "+collDef.getOwner().getName()+" doesn't match the number of primarykeys ("+primFields.size()+") of its owner class "+ownerClass.getName());
            }
            for (int idx = 0; idx < keyFields.size(); idx++)
            {
                keyField  = (FieldDescriptorDef)keyFields.get(idx);
                if (keyField.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false))
                {
                    throw new ConstraintException("The collection "+collDef.getName()+" in class "+ownerClass.getName()+" uses the field "+keyField.getName()+" as foreignkey although this field is ignored in the element class (or its subclass) "+elementClass.getName());
                }
            }
            // the jdbc types of the primary keys must match the jdbc types of the foreignkeys (in the correct order)
            for (int idx = 0; idx < primFields.size(); idx++)
            {
                keyField  = (FieldDescriptorDef)keyFields.get(idx);
                if (keyField.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false))
                {
                    throw new ConstraintException("The collection "+collDef.getName()+" in class "+ownerClass.getName()+" uses the field "+keyField.getName()+" as foreignkey although this field is ignored in the element class (or its subclass) "+elementClass.getName());
                }
                primField = (FieldDescriptorDef)primFields.get(idx);
                primType  = primField.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE);
                keyType   = keyField.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE);
                if (!primType.equals(keyType))
                {
                    throw new ConstraintException("The jdbc-type of foreignkey "+keyField.getName()+" in the element class (or its subclass) "+elementClass.getName()+" used by the collection "+collDef.getName()+" in class "+ownerClass.getName()+" doesn't match the jdbc-type of the corresponding primarykey "+primField.getName());
                }
            }
        }
    }

    /**
     * Checks the foreignkeys of all references in the model.
     * 
     * @param modelDef   The model
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     * @exception ConstraintException If the value for foreignkey is invalid
     */
    private void checkReferenceForeignkeys(ModelDef modelDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        ClassDescriptorDef     classDef;
        ReferenceDescriptorDef refDef;

        for (Iterator it = modelDef.getClasses(); it.hasNext();)
        {
            classDef = (ClassDescriptorDef)it.next();
            for (Iterator refIt = classDef.getReferences(); refIt.hasNext();)
            {
                refDef = (ReferenceDescriptorDef)refIt.next();
                if (!refDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false))
                {
                    checkReferenceForeignkeys(modelDef, refDef);
                }
            }
        }
    }

    /**
     * Checks the foreignkeys of a reference.
     * 
     * @param modelDef The model
     * @param refDef   The reference descriptor
     * @exception ConstraintException If the value for foreignkey is invalid
     */
    private void checkReferenceForeignkeys(ModelDef modelDef, ReferenceDescriptorDef refDef) throws ConstraintException
    {
        String foreignkey = refDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY);

        if ((foreignkey == null) || (foreignkey.length() == 0))
        {
            throw new ConstraintException("The reference "+refDef.getName()+" in class "+refDef.getOwner().getName()+" has no foreignkeys");
        }

        // we know that the class is present because the reference constraints have been checked already
        ClassDescriptorDef ownerClass = (ClassDescriptorDef)refDef.getOwner();
        ArrayList          keyFields;
        FieldDescriptorDef keyField;
        
        try
        {
            keyFields = ownerClass.getFields(foreignkey);
        }
        catch (NoSuchFieldException ex)
        {
            throw new ConstraintException("The reference "+refDef.getName()+" in class "+refDef.getOwner().getName()+" specifies a foreignkey "+ex.getMessage()+" that is not a persistent field in its owner class "+ownerClass.getName());
        }
        for (int idx = 0; idx < keyFields.size(); idx++)
        {
            keyField = (FieldDescriptorDef)keyFields.get(idx);
            if (keyField.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false))
            {
                throw new ConstraintException("The reference "+refDef.getName()+" in class "+ownerClass.getName()+" uses the field "+keyField.getName()+" as foreignkey although this field is ignored in this class");
            }
        }
            
        // for the referenced class and any subtype that is instantiable (i.e. not an interface or abstract class)
        // there must be the same number of primary keys and the jdbc types of the primary keys must
        // match the jdbc types of the foreignkeys (in the correct order)
        String             targetClassName = refDef.getProperty(PropertyHelper.OJB_PROPERTY_CLASS_REF);
        ArrayList          queue           = new ArrayList();
        ClassDescriptorDef referencedClass;
        ArrayList          primFields;
        FieldDescriptorDef primField;
        String             primType;
        String             keyType;
        
        queue.add(modelDef.getClass(targetClassName));

        while (!queue.isEmpty())
        {
            referencedClass = (ClassDescriptorDef)queue.get(0);
            queue.remove(0);

            for (Iterator it = referencedClass.getExtentClasses(); it.hasNext();)
            {
                queue.add(it.next());
            }
            if (!referencedClass.getBooleanProperty(PropertyHelper.OJB_PROPERTY_GENERATE_REPOSITORY_INFO, true))
            {
                continue;
            }
            primFields = referencedClass.getPrimaryKeys();
            if (primFields.size() != keyFields.size())
            {
                throw new ConstraintException("The number of foreignkeys ("+keyFields.size()+") of the reference "+refDef.getName()+" in class "+refDef.getOwner().getName()+" doesn't match the number of primarykeys ("+primFields.size()+") of the referenced class (or its subclass) "+referencedClass.getName());
            }
            for (int idx = 0; idx < primFields.size(); idx++)
            {
                keyField  = (FieldDescriptorDef)keyFields.get(idx);
                primField = (FieldDescriptorDef)primFields.get(idx);
                primType  = primField.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE);
                keyType   = keyField.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE);
                if (!primType.equals(keyType))
                {
                    throw new ConstraintException("The jdbc-type of foreignkey "+keyField.getName()+" of the reference "+refDef.getName()+" in class "+refDef.getOwner().getName()+" doesn't match the jdbc-type of the corresponding primarykey "+primField.getName()+" of the referenced class (or its subclass) "+referencedClass.getName());
                }
            }
        }
    }

    /**
     * Checks the modifications of fields used as foreignkeys in references/collections or the corresponding primarykeys,
     * e.g. that the jdbc-type is not changed etc.
     * 
     * @param modelDef   The model to check
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     * @throws ConstraintException If such a field has invalid modifications
     */
    private void checkKeyModifications(ModelDef modelDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        ClassDescriptorDef classDef;
        FieldDescriptorDef fieldDef;

        // we check for every inherited field
        for (Iterator classIt = modelDef.getClasses(); classIt.hasNext();)
        {
            classDef = (ClassDescriptorDef)classIt.next();
            for (Iterator fieldIt = classDef.getFields(); fieldIt.hasNext();)
            {
                fieldDef = (FieldDescriptorDef)fieldIt.next();
                if (fieldDef.isInherited())
                {
                    checkKeyModifications(modelDef, fieldDef);
                }
            }
        }
    }

    /**
     * Checks the modifications of the given inherited field if it is used as a foreignkey in a
     * reference/collection or as the corresponding primarykey, e.g. that the jdbc-type is not changed etc.
     * 
     * @param modelDef The model to check
     * @throws ConstraintException If the field has invalid modifications
     */
    private void checkKeyModifications(ModelDef modelDef, FieldDescriptorDef keyDef) throws ConstraintException
    {
        // we check the field if it changes the primarykey-status or the jdbc-type
        FieldDescriptorDef baseFieldDef    = (FieldDescriptorDef)keyDef.getOriginal();
        boolean            isIgnored       = keyDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false);
        boolean            changesJdbcType = !baseFieldDef.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE).equals(keyDef.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE));
        boolean            changesPrimary  = baseFieldDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_PRIMARYKEY, false) !=
                                             keyDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_PRIMARYKEY, false) ;

        if (isIgnored || changesJdbcType || changesPrimary)
        {
            FeatureDescriptorDef usingFeature = null;

            do
            {    
                usingFeature = usedByReference(modelDef, baseFieldDef);
                if (usingFeature != null)
                {
                    if (isIgnored)
                    {
                        throw new ConstraintException("Cannot ignore field "+keyDef.getName()+" in class "+keyDef.getOwner().getName()+
                                                      " because it is used in class "+baseFieldDef.getOwner().getName()+
                                                      " by the reference "+usingFeature.getName()+" from class "+
                                                      usingFeature.getOwner().getName());
                    }
                    else if (changesJdbcType)
                    {    
                        throw new ConstraintException("Modification of the jdbc-type for the field "+keyDef.getName()+" in class "+
                                                      keyDef.getOwner().getName()+" is not allowed because it is used in class "+
                                                      baseFieldDef.getOwner().getName()+" by the reference "+usingFeature.getName()+
                                                      " from class "+usingFeature.getOwner().getName());
                    }
                    else
                    {
                        throw new ConstraintException("Cannot change the primarykey status of field "+keyDef.getName()+" in class "+
                                                      keyDef.getOwner().getName()+" as primarykeys are used in class "+
                                                      baseFieldDef.getOwner().getName()+" by the reference "+usingFeature.getName()+
                                                      " from class "+usingFeature.getOwner().getName());
                    }
                }

                usingFeature = usedByCollection(modelDef, baseFieldDef, changesPrimary);
                if (usingFeature != null)
                {
                    if (isIgnored)
                    {
                        throw new ConstraintException("Cannot ignore field "+keyDef.getName()+" in class "+keyDef.getOwner().getName()+
                                                      " because it is used in class "+baseFieldDef.getOwner().getName()+
                                                      " as a foreignkey of the collection "+usingFeature.getName()+" from class "+
                                                      usingFeature.getOwner().getName());
                    }
                    else if (changesJdbcType)
                    {    
                        throw new ConstraintException("Modification of the jdbc-type for the field "+keyDef.getName()+" in class "+
                                                      keyDef.getOwner().getName()+" is not allowed because it is used in class "+
                                                      baseFieldDef.getOwner().getName()+" as a foreignkey of the collecton "+
                                                      usingFeature.getName()+" from class "+usingFeature.getOwner().getName());
                    }
                    else
                    {    
                        throw new ConstraintException("Cannot change the primarykey status of field "+keyDef.getName()+" in class "+
                                                      keyDef.getOwner().getName()+" as primarykeys are used in class "+
                                                      baseFieldDef.getOwner().getName()+" by the collection "+usingFeature.getName()+
                                                      " from class "+usingFeature.getOwner().getName());
                    }
                }

                baseFieldDef = (FieldDescriptorDef)baseFieldDef.getOriginal();
            }
            while (baseFieldDef != null);
        }
    }

    /**
     * Checks whether the given field definition is used as a remote-foreignkey in an m:n
     * association where the class owning the field has no collection for the association.
     * 
     * @param modelDef             The model
     * @param fieldDef             The current field descriptor def
     * @param elementClassSuffices Whether it suffices that the owner class of the field is an
     *                             element class of a collection (for primary key tests)
     * @return The collection that uses the field or <code>null</code> if the field is not
     *         used in this way
     */
    private CollectionDescriptorDef usedByCollection(ModelDef modelDef, FieldDescriptorDef fieldDef, boolean elementClassSuffices)
    {
        ClassDescriptorDef      ownerClass     = (ClassDescriptorDef)fieldDef.getOwner();
        String                  ownerClassName = ownerClass.getQualifiedName();
        String                  name           = fieldDef.getName();
        ClassDescriptorDef      classDef;
        CollectionDescriptorDef collDef;
        String                  elementClassName;

        for (Iterator classIt = modelDef.getClasses(); classIt.hasNext();)
        {
            classDef = (ClassDescriptorDef)classIt.next();
            for (Iterator collIt = classDef.getCollections(); collIt.hasNext();)
            {
                collDef          = (CollectionDescriptorDef)collIt.next();
                elementClassName = collDef.getProperty(PropertyHelper.OJB_PROPERTY_ELEMENT_CLASS_REF).replace('$', '.');
                // if the owner class of the field is the element class of a normal collection
                // and the field is a foreignkey of this collection
                if (ownerClassName.equals(elementClassName))
                {
                    if (collDef.hasProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE))
                    {
                        if (elementClassSuffices)
                        {
                            return collDef;
                        }
                    }
                    else if (new CommaListIterator(collDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY)).contains(name))
                    {
                        // if the field is a foreignkey of this normal 1:n collection
                        return collDef;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks whether the given field definition is used as the primary key of a class referenced by
     * a reference.
     * 
     * @param modelDef The model
     * @param fieldDef The current field descriptor def
     * @return The reference that uses the field or <code>null</code> if the field is not used in this way
     */
    private ReferenceDescriptorDef usedByReference(ModelDef modelDef, FieldDescriptorDef fieldDef)
    {
        String                 ownerClassName = ((ClassDescriptorDef)fieldDef.getOwner()).getQualifiedName();
        ClassDescriptorDef     classDef;
        ReferenceDescriptorDef refDef;
        String                 targetClassName;

        // only relevant for primarykey fields
        if (PropertyHelper.toBoolean(fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_PRIMARYKEY), false))
        {
            for (Iterator classIt = modelDef.getClasses(); classIt.hasNext();)
            {
                classDef = (ClassDescriptorDef)classIt.next();
                for (Iterator refIt = classDef.getReferences(); refIt.hasNext();)
                {
                    refDef          = (ReferenceDescriptorDef)refIt.next();
                    targetClassName = refDef.getProperty(PropertyHelper.OJB_PROPERTY_CLASS_REF).replace('$', '.');
                    if (ownerClassName.equals(targetClassName))
                    {
                        // the field is a primary key of the class referenced by this reference descriptor
                        return refDef;
                    }
                }
            }
        }
        return null;
    }
}
