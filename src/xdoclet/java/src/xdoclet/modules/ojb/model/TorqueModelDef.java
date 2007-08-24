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

/**
 * Represents the model used for generating the torque database schema.
 */
public class TorqueModelDef extends DefBase
{
    /** The tables keyed by their names*/
    private SortedMap _tableDefs = new TreeMap();

    /**
     * Generates a new torque database model from the given ojb model.
     * 
     * @param dbName   The name of the database
     * @param ojbModel The ojb model
     * @return The torque model
     */
    public TorqueModelDef(String dbName, ModelDef ojbModel)
    {
        super(dbName);

        ClassDescriptorDef classDef;
        TableDef           tableDef;
        FieldDescriptorDef fieldDef;
        ColumnDef          columnDef;
        String             name;

        for (Iterator classIt = ojbModel.getClasses(); classIt.hasNext();)
        {
            classDef = (ClassDescriptorDef)classIt.next();
            if (classDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_OJB_PERSISTENT, false) &&
                classDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_GENERATE_TABLE_INFO, true))
            {
                addTableFor(classDef);
            }
        }
    }

    // The conversion algorithm
    // Note that the complete algorithm is here rather than splitting in onto the
    // various torque model elements because maintaining it is easier this way
    
    /**
     * Adds a table for the given class descriptor (if necessary).
     * 
     * @param classDef The class descriptor
     */
    private void addTableFor(ClassDescriptorDef classDef)
    {
        String                  name     = classDef.getProperty(PropertyHelper.OJB_PROPERTY_TABLE);
        TableDef                tableDef = getTable(name);
        FieldDescriptorDef      fieldDef;
        ReferenceDescriptorDef  refDef;
        CollectionDescriptorDef collDef;
        ColumnDef               columnDef;
        IndexDef                indexDef;

        if (tableDef == null)
        {
            tableDef = new TableDef(name);
            addTable(tableDef);
        }
        if (classDef.hasProperty(PropertyHelper.OJB_PROPERTY_DOCUMENTATION))
        {
            tableDef.setProperty(PropertyHelper.OJB_PROPERTY_DOCUMENTATION,
                                 classDef.getProperty(PropertyHelper.OJB_PROPERTY_DOCUMENTATION));
        }
        if (classDef.hasProperty(PropertyHelper.OJB_PROPERTY_TABLE_DOCUMENTATION))
        {
            tableDef.setProperty(PropertyHelper.OJB_PROPERTY_TABLE_DOCUMENTATION,
                                 classDef.getProperty(PropertyHelper.OJB_PROPERTY_TABLE_DOCUMENTATION));
        }
        for (Iterator fieldIt = classDef.getFields(); fieldIt.hasNext();)
        {
            fieldDef = (FieldDescriptorDef)fieldIt.next();
            if (fieldDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false) ||
                fieldDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_VIRTUAL_FIELD, false))
            {
                continue;
            }
            columnDef = addColumnFor(fieldDef, tableDef);
            if (fieldDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_INDEXED, false))
            {
                // add the field to the default index
                indexDef = tableDef.getIndex(null);
                if (indexDef == null)
                {
                    indexDef = new IndexDef(null, false);
                    tableDef.addIndex(indexDef);
                }
                indexDef.addColumn(columnDef.getName());
            }
        }
        for (Iterator refIt = classDef.getReferences(); refIt.hasNext();)
        {
            refDef = (ReferenceDescriptorDef)refIt.next();
            if (!refDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false))
            {
                addForeignkeys(refDef, tableDef);
            }
        }
        for (Iterator collIt = classDef.getCollections(); collIt.hasNext();)
        {
            collDef = (CollectionDescriptorDef)collIt.next();
            if (!collDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false))
            {
                if (collDef.hasProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE))
                {
                    addIndirectionTable(collDef);
                }
                else
                {
                    addForeignkeys(collDef, tableDef);
                }
            }
        }
        for (Iterator indexIt = classDef.getIndexDescriptors(); indexIt.hasNext();)
        {
            addIndex((IndexDescriptorDef)indexIt.next(), tableDef);
        }
    }

    /**
     * Generates a column for the given field and adds it to the table.
     * 
     * @param fieldDef The field
     * @param tableDef The table
     * @return The column def
     */
    private ColumnDef addColumnFor(FieldDescriptorDef fieldDef, TableDef tableDef)
    {
        String    name      = fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_COLUMN);
        ColumnDef columnDef = tableDef.getColumn(name);

        if (columnDef == null)
        {
            columnDef = new ColumnDef(name);
            tableDef.addColumn(columnDef);
        }
        if (!fieldDef.isNested())
        {    
            columnDef.setProperty(PropertyHelper.TORQUE_PROPERTY_JAVANAME, fieldDef.getName());
        }
        columnDef.setProperty(PropertyHelper.TORQUE_PROPERTY_TYPE, fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE));
        columnDef.setProperty(PropertyHelper.TORQUE_PROPERTY_ID, fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_ID));
        if (fieldDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_PRIMARYKEY, false))
        {
            columnDef.setProperty(PropertyHelper.TORQUE_PROPERTY_PRIMARYKEY, "true");
            columnDef.setProperty(PropertyHelper.TORQUE_PROPERTY_REQUIRED, "true");
        }
        else if (!fieldDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_NULLABLE, true))
        {
            columnDef.setProperty(PropertyHelper.TORQUE_PROPERTY_REQUIRED, "true");
        }
        if ("database".equals(fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_AUTOINCREMENT)))
        {
            columnDef.setProperty(PropertyHelper.TORQUE_PROPERTY_AUTOINCREMENT, "true");
        }
        columnDef.setProperty(PropertyHelper.TORQUE_PROPERTY_SIZE, fieldDef.getSizeConstraint());
        if (fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_DOCUMENTATION))
        {
            columnDef.setProperty(PropertyHelper.OJB_PROPERTY_DOCUMENTATION,
                                  fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_DOCUMENTATION));
        }
        if (fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_COLUMN_DOCUMENTATION))
        {
            columnDef.setProperty(PropertyHelper.OJB_PROPERTY_COLUMN_DOCUMENTATION,
                                  fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_COLUMN_DOCUMENTATION));
        }
        return columnDef;
    }

    /**
     * Adds foreignkey(s) for the reference to the corresponding table(s).
     * 
     * @param refDef   The reference
     * @param tableDef The table of the class owning the reference
     */
    private void addForeignkeys(ReferenceDescriptorDef refDef, TableDef tableDef)
    {
        if (!refDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_DATABASE_FOREIGNKEY, true))
        {
            // we shall not generate a database foreignkey
            return;
        }

        // a foreignkey is added to the table schema if
        // the referenced table exists (i.e. the referenced type has an associated table)
        // then the foreignkey consists of:
        //   remote table  = table of referenced type
        //   local fields  = foreignkey fields of the reference 
        //   remote fields = primarykeys of the referenced type
        ClassDescriptorDef ownerClassDef      = (ClassDescriptorDef)refDef.getOwner();
        String             targetClassName    = refDef.getProperty(PropertyHelper.OJB_PROPERTY_CLASS_REF);
        ClassDescriptorDef referencedClassDef = ((ModelDef)ownerClassDef.getOwner()).getClass(targetClassName);

        // we can add a foreignkey only if the target type and all its subtypes either
        // map to the same table or do not map to a table at all
        String tableName = getHierarchyTable(referencedClassDef);

        if (tableName == null)
        {
            return;
        }

        try
        {
            String    name         = refDef.getName();
            ArrayList localFields  = ownerClassDef.getFields(refDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY));
            ArrayList remoteFields = referencedClassDef.getPrimaryKeys();

            tableDef.addForeignkey(name, tableName, getColumns(localFields), getColumns(remoteFields));
        }
        catch (NoSuchFieldException ex)
        {
            // won't happen if we already checked the constraints
        }
    }

    /**
     * Adds foreignkey(s) for the collection to the corresponding table(s).
     * 
     * @param collDef  The collection
     * @param tableDef The table
     */
    private void addForeignkeys(CollectionDescriptorDef collDef, TableDef tableDef)
    {
        if (!collDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_DATABASE_FOREIGNKEY, true))
        {
            // we shall not generate a database foreignkey
            return;
        }

        // a foreignkey is added to the table schema if for both ends of the collection
        // a table exists
        // then the foreignkey consists of:
        //   remote table  = table of collection owner
        //   local fields  = foreignkey fields in the element type 
        //   remote fields = primarykeys of the collection owner type
        ClassDescriptorDef ownerClassDef    = (ClassDescriptorDef)collDef.getOwner();
        String             elementClassName = collDef.getProperty(PropertyHelper.OJB_PROPERTY_ELEMENT_CLASS_REF);
        ClassDescriptorDef elementClassDef  = ((ModelDef)ownerClassDef.getOwner()).getClass(elementClassName);

        // we can only generate foreignkeys if the collection itself is not shared by
        // several classes in the hierarchy
        for (Iterator it = ownerClassDef.getAllBaseTypes(); it.hasNext();)
        {
            if (containsCollectionAndMapsToDifferentTable(collDef, tableDef, (ClassDescriptorDef)it.next()))
            {
                return;
            }
        }
        for (Iterator it = ownerClassDef.getAllExtentClasses(); it.hasNext();)
        {
            if (containsCollectionAndMapsToDifferentTable(collDef, tableDef, (ClassDescriptorDef)it.next()))
            {
                return;
            }
        }

        // We add a foreignkey to all classes in the subtype hierarchy of the element type
        // that map to a table (we're silently assuming that they contain the fk fields) 
        ArrayList candidates = new ArrayList();

        if (elementClassDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_GENERATE_TABLE_INFO, true))
        {
            candidates.add(elementClassDef);
        }
        for (Iterator it = elementClassDef.getAllExtentClasses(); it.hasNext();)
        {
            ClassDescriptorDef curSubTypeDef = (ClassDescriptorDef)it.next();

            if (curSubTypeDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_GENERATE_TABLE_INFO, true))
            {
                candidates.add(curSubTypeDef);
            }
        }

        String    name            = collDef.getName();
        ArrayList remoteFields    = ownerClassDef.getPrimaryKeys();
        HashMap   processedTables = new HashMap();

        for (Iterator it = candidates.iterator(); it.hasNext();)
        {
            elementClassDef = (ClassDescriptorDef)it.next();
            try
            {
                // for the element class and its subclasses
                String elementTableName = elementClassDef.getProperty(PropertyHelper.OJB_PROPERTY_TABLE);

                // ensure that tables are only processed once
                if (!processedTables.containsKey(elementTableName))
                {
                    ArrayList localFields     = elementClassDef.getFields(collDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY));
                    TableDef  elementTableDef = getTable(elementTableName);
    
                    if (elementTableDef == null)
                    {
                        elementTableDef = new TableDef(elementTableName);
                        addTable(elementTableDef);
                    }
                    elementTableDef.addForeignkey(name, tableDef.getName(), getColumns(localFields), getColumns(remoteFields));
                    processedTables.put(elementTableName, null);
                }
            }
            catch (NoSuchFieldException ex)
            {
                // Shouldn't happen, but even if, then we're ignoring it and simply don't add the fk
            }
        }
    }

    /**
     * Extracts the list of columns from the given field list.
     * 
     * @param fields The fields
     * @return The corresponding columns
     */
    private List getColumns(List fields)
    {
        ArrayList columns = new ArrayList();

        for (Iterator it = fields.iterator(); it.hasNext();)
        {
            FieldDescriptorDef fieldDef = (FieldDescriptorDef)it.next();

            columns.add(fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_COLUMN));
        }
        return columns;
    }
    
    /**
     * Checks whether the given class maps to a different table but also has the given collection.
     * 
     * @param origCollDef  The original collection to search for
     * @param origTableDef The original table
     * @param classDef     The class descriptor to test
     * @return <code>true</code> if the class maps to a different table and has the collection
     */
    private boolean containsCollectionAndMapsToDifferentTable(CollectionDescriptorDef origCollDef, TableDef origTableDef, ClassDescriptorDef classDef)
    {
        if (classDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_GENERATE_TABLE_INFO, true) &&
            !origTableDef.getName().equals(classDef.getProperty(PropertyHelper.OJB_PROPERTY_TABLE)))
        {
            CollectionDescriptorDef curCollDef = classDef.getCollection(origCollDef.getName());

            if ((curCollDef != null) &&
                !curCollDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Tries to return the single target table to which the given foreign key columns map in
     * all m:n collections that target this indirection table. 
     * 
     * @param targetClassDef   The original target class
     * @param indirectionTable The indirection table
     * @param foreignKeys      The foreign keys columns in the indirection table pointing back to the
     *                         class' table
     * @return The table name or <code>null</code> if there is not exactly one table
     */
    private String getTargetTable(ClassDescriptorDef targetClassDef, String indirectionTable, String foreignKeys)
    {
        ModelDef modelDef  = (ModelDef)targetClassDef.getOwner();
        String   tableName = null;

        for (Iterator classIt = modelDef.getClasses(); classIt.hasNext();)
        {
            ClassDescriptorDef curClassDef = (ClassDescriptorDef)classIt.next();

            if (!curClassDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_GENERATE_TABLE_INFO, true))
            {
                continue;
            }
            for (Iterator collIt = curClassDef.getCollections(); collIt.hasNext();)
            {
                CollectionDescriptorDef curCollDef = (CollectionDescriptorDef)collIt.next();

                if (!indirectionTable.equals(curCollDef.getProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE)) ||
                    !CommaListIterator.sameLists(foreignKeys, curCollDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY)))
                {
                    continue;
                }
                // ok, collection fits
                if (tableName != null)
                {
                    if (!tableName.equals(curClassDef.getProperty(PropertyHelper.OJB_PROPERTY_TABLE)))
                    {
                        // maps to a different table
                        return null;
                    }
                }
                else
                {
                    tableName = curClassDef.getProperty(PropertyHelper.OJB_PROPERTY_TABLE);
                }
            }
        }
        if (tableName == null)
        {
            // no fitting collection found -> indirection table with only one collection
            // we have to check whether the hierarchy of the target class maps to one table only
            return getHierarchyTable(targetClassDef);
        }
        else
        {
            return tableName;
        }
    }

    /**
     * Tries to return the single table to which all classes in the hierarchy with the given
     * class as the root map.
     * 
     * @param classDef The root class of the hierarchy
     * @return The table name or <code>null</code> if the classes map to more than one table
     *         or no class in the hierarchy maps to a table
     */
    private String getHierarchyTable(ClassDescriptorDef classDef)
    {
        ArrayList queue     = new ArrayList();
        String    tableName = null;

        queue.add(classDef);

        while (!queue.isEmpty())
        {
            ClassDescriptorDef curClassDef = (ClassDescriptorDef)queue.get(0);

            queue.remove(0);

            if (curClassDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_GENERATE_TABLE_INFO, true))
            {
                if (tableName != null)
                {
                    if (!tableName.equals(curClassDef.getProperty(PropertyHelper.OJB_PROPERTY_TABLE)))
                    {
                        return null;
                    }
                }
                else
                {
                    tableName = curClassDef.getProperty(PropertyHelper.OJB_PROPERTY_TABLE);
                }
            }
            for (Iterator it = curClassDef.getExtentClasses(); it.hasNext();)
            {
                curClassDef = (ClassDescriptorDef)it.next();

                if (curClassDef.getReference("super") == null)
                {
                    queue.add(curClassDef);
                }
            }
        }
        return tableName;
    }

    /**
     * Adds an index to the table for the given index descriptor.
     * 
     * @param indexDescDef The index descriptor
     * @param tableDef     The table
     */
    private void addIndex(IndexDescriptorDef indexDescDef, TableDef tableDef)
    {
        IndexDef indexDef = tableDef.getIndex(indexDescDef.getName());

        if (indexDef == null)
        {
            indexDef = new IndexDef(indexDescDef.getName(),
                                    indexDescDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_UNIQUE, false));
            tableDef.addIndex(indexDef);
        }

        try
        {
            String             fieldNames = indexDescDef.getProperty(PropertyHelper.OJB_PROPERTY_FIELDS);
            ArrayList          fields     = ((ClassDescriptorDef)indexDescDef.getOwner()).getFields(fieldNames);
            FieldDescriptorDef fieldDef;

            for (Iterator it = fields.iterator(); it.hasNext();)
            {
                fieldDef = (FieldDescriptorDef)it.next();
                indexDef.addColumn(fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_COLUMN));
            }
        }
        catch (NoSuchFieldException ex)
        {
            // won't happen if we already checked the constraints
        }
    }

    /**
     * Adds the indirection table for the given collection descriptor.
     * 
     * @param collDef The collection descriptor
     */
    private void addIndirectionTable(CollectionDescriptorDef collDef)
    {
        String   tableName = collDef.getProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE);
        TableDef tableDef  = getTable(tableName);

        if (tableDef == null)
        {
            tableDef = new TableDef(tableName);
            addTable(tableDef);
        }
        if (collDef.hasProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE_DOCUMENTATION))
        {
            tableDef.setProperty(PropertyHelper.OJB_PROPERTY_TABLE_DOCUMENTATION,
                                 collDef.getProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE_DOCUMENTATION));
        }

        // we add columns for every primarykey in this and the element type
        //   collection.foreignkeys        <-> ownerclass.primarykeys
        //   collection.remote-foreignkeys <-> elementclass.primarykeys
        // we also add foreignkeys to the table
        //   name is empty (default foreignkey)
        //   remote table   = table of ownerclass/elementclass
        //   local columns  = columns in indirection table 
        //   remote columns = columns of corresponding primarykeys in ownerclass/elementclass
        ClassDescriptorDef ownerClassDef    = (ClassDescriptorDef)collDef.getOwner();
        ModelDef           modelDef         = (ModelDef)ownerClassDef.getOwner();
        String             elementClassName = collDef.getProperty(PropertyHelper.OJB_PROPERTY_ELEMENT_CLASS_REF);
        ClassDescriptorDef elementClassDef  = modelDef.getClass(elementClassName);
        ArrayList          localPrimFields  = ownerClassDef.getPrimaryKeys();
        ArrayList          remotePrimFields = elementClassDef.getPrimaryKeys();
        String             localKeyList     = collDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY);
        String             remoteKeyList    = collDef.getProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY);
        String             ownerTable       = getTargetTable(ownerClassDef, tableName, localKeyList);
        String             elementTable     = getTargetTable(elementClassDef, tableName, remoteKeyList);
        CommaListIterator  localKeys        = new CommaListIterator(localKeyList);
        CommaListIterator  localKeyDocs     = new CommaListIterator(collDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY_DOCUMENTATION));
        CommaListIterator  remoteKeys       = new CommaListIterator(remoteKeyList);
        CommaListIterator  remoteKeyDocs    = new CommaListIterator(collDef.getProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY_DOCUMENTATION));
        ArrayList          localColumns     = new ArrayList();
        ArrayList          remoteColumns    = new ArrayList();
        boolean            asPrimarykeys    = collDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE_PRIMARYKEYS, false);
        FieldDescriptorDef fieldDef;
        ColumnDef          columnDef;
        String             relationName;
        String             name;
        int                idx;

        for (idx = 0; localKeys.hasNext(); idx++)
        {
            fieldDef  = (FieldDescriptorDef)localPrimFields.get(idx);
            name      = localKeys.getNext();
            columnDef = tableDef.getColumn(name);
            if (columnDef == null)
            {
                columnDef = new ColumnDef(name);
                tableDef.addColumn(columnDef);
            }
            columnDef.setProperty(PropertyHelper.TORQUE_PROPERTY_TYPE, fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE));
            columnDef.setProperty(PropertyHelper.TORQUE_PROPERTY_SIZE, fieldDef.getSizeConstraint());
            if (asPrimarykeys)
            {
                columnDef.setProperty(PropertyHelper.TORQUE_PROPERTY_PRIMARYKEY, "true");
            }
            if (localKeyDocs.hasNext())
            {
                columnDef.setProperty(PropertyHelper.OJB_PROPERTY_COLUMN_DOCUMENTATION, localKeyDocs.getNext());
            }
            localColumns.add(name);
            remoteColumns.add(fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_COLUMN));
        }
        if (collDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_DATABASE_FOREIGNKEY, true))
        {
            relationName = collDef.getProperty(PropertyHelper.TORQUE_PROPERTY_RELATION_NAME);
            if ((relationName != null) && (ownerTable != null))
            {
                tableDef.addForeignkey(relationName, ownerTable, localColumns, remoteColumns);
            }
        }
        localColumns.clear();
        remoteColumns.clear();

        for (idx = 0; remoteKeys.hasNext(); idx++)
        {
            fieldDef  = (FieldDescriptorDef)remotePrimFields.get(idx);
            name      = remoteKeys.getNext();

            columnDef = tableDef.getColumn(name);
            if (columnDef == null)
            {
                columnDef = new ColumnDef(name);
                tableDef.addColumn(columnDef);
            }
            columnDef.setProperty(PropertyHelper.TORQUE_PROPERTY_TYPE, fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE));
            columnDef.setProperty(PropertyHelper.TORQUE_PROPERTY_SIZE, fieldDef.getSizeConstraint());
            if (asPrimarykeys)
            {
                columnDef.setProperty(PropertyHelper.TORQUE_PROPERTY_PRIMARYKEY, "true");
            }
            if (remoteKeyDocs.hasNext())
            {
                columnDef.setProperty(PropertyHelper.OJB_PROPERTY_COLUMN_DOCUMENTATION, remoteKeyDocs.getNext());
            }
            localColumns.add(name);
            remoteColumns.add(fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_COLUMN));
        }

        CollectionDescriptorDef elementCollDef = collDef.getRemoteCollection();

        if (((elementCollDef != null) && elementCollDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_DATABASE_FOREIGNKEY, true)) ||
            ((elementCollDef == null) && collDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_DATABASE_FOREIGNKEY, true)))
        {
            relationName = collDef.getProperty(PropertyHelper.TORQUE_PROPERTY_INV_RELATION_NAME);
            if ((relationName != null) && (elementTable != null))
            {
                tableDef.addForeignkey(relationName, elementTable, localColumns, remoteColumns);
            }
        }
    }

    // Access methods
    
    /**
     * Returns an iterator of the tables.
     * 
     * @return The tables
     */
    public Iterator getTables()
    {
        return _tableDefs.values().iterator();
    }
    
    /**
     * Returns the table of the given name if it exists.
     * 
     * @param name The table name
     * @return The table def or <code>null</code> if there is no such table
     */
    public TableDef getTable(String name)
    {
        return (TableDef)_tableDefs.get(name);
    }

    /**
     * Adds a table to this model.
     * 
     * @param table The table
     */
    private void addTable(TableDef table)
    {
        table.setOwner(this);
        _tableDefs.put(table.getName(), table);
    }
}
