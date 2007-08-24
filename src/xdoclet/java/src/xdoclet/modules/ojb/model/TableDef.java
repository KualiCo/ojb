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

import org.apache.commons.collections.SequencedHashMap;

/**
 * Definition of a table for the torque schema.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class TableDef extends DefBase
{
    /** Map of columns keyed by their column name */
    private SequencedHashMap _columns = new SequencedHashMap();
    /** List of indices/uniques */
    private ArrayList _indices = new ArrayList();
    /** The foreignkeys to other tables */
    private ArrayList _foreignkeys = new ArrayList();

    /**
     * Creates a new table definition object.
     *
     * @param name The table name
     */
    public TableDef(String name)
    {
        super(name);
    }

    /**
     * Returns an iterator of all columns of this table.
     *
     * @return The iterator
     */
    public Iterator getColumns()
    {
        return _columns.values().iterator();
    }

    /**
     * Returns the column with the given name.
     *
     * @param name The name of the desired column
     * @return The column or <code>null</code> if there is no column with that name
     */
    public ColumnDef getColumn(String name)
    {
        return (ColumnDef)_columns.get(name);
    }

    /**
     * Adds a column to this table definition.
     *
     * @param columnDef The new column
     */
    public void addColumn(ColumnDef columnDef)
    {
        columnDef.setOwner(this);
        _columns.put(columnDef.getName(), columnDef);
    }

    /**
     * Adds an index to this table.
     * 
     * @param The index def
     */
    public void addIndex(IndexDef indexDef)
    {
        indexDef.setOwner(this);
        _indices.add(indexDef);
    }

    /**
     * Returns the index of the given name.
     * 
     * @param name The name of the index (null or empty string for the default index)
     * @return The index def or <code>null</code> if it does not exist
     */
    public IndexDef getIndex(String name)
    {
        String   realName = (name == null ? "" : name);
        IndexDef def      = null;

        for (Iterator it = getIndices(); it.hasNext();)
        {
            def = (IndexDef)it.next();
            if (def.getName().equals(realName))
            {
                return def;
            }
        }
        return null;
    }

    /**
     * Returns an iterator of all indices of this table.
     * 
     * @return The indices
     */
    public Iterator getIndices()
    {
        return _indices.iterator();
    }

    /**
     * Adds a foreignkey to this table.
     * 
     * @param relationName  The name of the relation represented by the foreignkey
     * @param remoteTable   The referenced table
     * @param localColumns  The local columns
     * @param remoteColumns The remote columns
     */
    public void addForeignkey(String relationName, String remoteTable, List localColumns, List remoteColumns)
    {
        ForeignkeyDef foreignkeyDef = new ForeignkeyDef(relationName, remoteTable);

        // the field arrays have the same length if we already checked the constraints
        for (int idx = 0; idx < localColumns.size(); idx++)
        {
            foreignkeyDef.addColumnPair((String)localColumns.get(idx),
                                        (String)remoteColumns.get(idx));
        }

        // we got to determine whether this foreignkey is already present 
        ForeignkeyDef def = null;

        for (Iterator it = getForeignkeys(); it.hasNext();)
        {
            def = (ForeignkeyDef)it.next();
            if (foreignkeyDef.equals(def))
            {
                return;
            }
        }
        foreignkeyDef.setOwner(this);
        _foreignkeys.add(foreignkeyDef);
    }

    /**
     * Determines whether this table has a foreignkey of the given name.
     * 
     * @param name The name of the foreignkey
     * @return <code>true</code> if there is a foreignkey of that name
     */
    public boolean hasForeignkey(String name)
    {
        String        realName = (name == null ? "" : name);
        ForeignkeyDef def      = null;

        for (Iterator it = getForeignkeys(); it.hasNext();)
        {
            def = (ForeignkeyDef)it.next();
            if (realName.equals(def.getName()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the foreignkey to the specified table.
     * 
     * @param name      The name of the foreignkey
     * @param tableName The name of the referenced table
     * @return The foreignkey def or <code>null</code> if it does not exist
     */
    public ForeignkeyDef getForeignkey(String name, String tableName)
    {
        String        realName = (name == null ? "" : name);
        ForeignkeyDef def      = null;

        for (Iterator it = getForeignkeys(); it.hasNext();)
        {
            def = (ForeignkeyDef)it.next();
            if (realName.equals(def.getName()) &&
                def.getTableName().equals(tableName))
            {
                return def;
            }
        }
        return null;
    }

    /**
     * Returns an iterator of all foreignkeys of this table.
     * 
     * @return The foreignkeys
     */
    public Iterator getForeignkeys()
    {
        return _foreignkeys.iterator();
    }
}
