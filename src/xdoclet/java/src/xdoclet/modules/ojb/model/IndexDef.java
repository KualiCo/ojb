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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Definition of an index descriptor for a ojb-persistent class.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class IndexDef extends DefBase
{
    /** The columns */
    private ArrayList _columns = new ArrayList();
    /** Whether this index is unique */
    private boolean _isUnique = false;

    /**
     * Creates a new index descriptor definition object.
     *
     * @param name     The name of the index
     * @param isUnique Whether this index is unique
     */
    public IndexDef(String name, boolean isUnique)
    {
        super(name == null ? "" : name);
        _isUnique = isUnique;
    }

    /**
     * Determines whether this index is the default index.
     * 
     * @return <code>true</code> if it is the default index
     */
    public boolean isDefault()
    {
        return (getName() == null) || (getName().length() == 0);
    }

    /**
     * Returns whether this index is unique.
     * 
     * @return <code>true</code> if this index is an unique index
     */
    public boolean isUnique()
    {
        return _isUnique;
    }

    /**
     * Adds a column to this index.
     * 
     * @param column The column
     */
    public void addColumn(String column)
    {
        if (!_columns.contains(column))
        {
            _columns.add(column);
        }
    }

    /**
     * Returns an iterator of the columns of this index.
     * 
     * @return The iterator
     */
    public Iterator getColumns()
    {
        return _columns.iterator();
    }
}
