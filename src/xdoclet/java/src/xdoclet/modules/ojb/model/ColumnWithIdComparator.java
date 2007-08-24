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

import java.util.Comparator;

/**
 * Comparator used for sorting collections that compares column names according to the id properties of the corresponding
 * columns.
 *
 * @author    <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 * @created   May 1, 2003
 */
public class ColumnWithIdComparator implements Comparator
{
    /**
     * The table owning the columns
     */
    private TableDef _table = null;

    /**
     * Creates a new comparator object.
     *
     * @param table The table owning the columns
     */
    public ColumnWithIdComparator(TableDef table)
    {
        _table = table;
    }

    /**
     * Compares two columns given by their names.
     *
     * @param objA  The name of the first column
     * @param objB  The name of the second column
     * @return
     * @see         java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object objA, Object objB)
    {
        String idAStr = _table.getColumn((String)objA).getProperty("id");
        String idBStr = _table.getColumn((String)objB).getProperty("id");
        int idA;
        int idB;

        try {
            idA = Integer.parseInt(idAStr);
        }
        catch (Exception ex) {
            return 1;
        }
        try {
            idB = Integer.parseInt(idBStr);
        }
        catch (Exception ex) {
            return -1;
        }
        return idA < idB ? -1 : (idA > idB ? 1 : 0);
    }

}
