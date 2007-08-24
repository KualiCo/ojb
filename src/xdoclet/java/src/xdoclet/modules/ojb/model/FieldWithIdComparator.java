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

import java.util.HashMap;
import java.util.Comparator;

/**
 * Comparator used for sorting collections that compares field names according to the id properties of the corresponding
 * fields.
 *
 * @author    <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 * @created   April 20, 2003
 */
public class FieldWithIdComparator implements Comparator
{
    /**
     * Contains the actual fields
     */
    private HashMap _fields = null;

    /**
     * Creates a new comparator object.
     *
     * @param fields  The actual fields
     */
    public FieldWithIdComparator(HashMap fields)
    {
        _fields = fields;
    }

    /**
     * Compares two fields given by their names.
     *
     * @param objA  The name of the first field
     * @param objB  The name of the second field
     * @return
     * @see         java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object objA, Object objB)
    {
        String idAStr = ((FieldDescriptorDef)_fields.get(objA)).getProperty("id");
        String idBStr = ((FieldDescriptorDef)_fields.get(objB)).getProperty("id");
        int    idA;
        int    idB;

        try
        {
            idA = Integer.parseInt(idAStr);
        }
        catch (Exception ex)
        {
            return 1;
        }
        try
        {
            idB = Integer.parseInt(idBStr);
        }
        catch (Exception ex)
        {
            return -1;
        }
        return idA < idB ? -1 : (idA > idB ? 1 : 0);
    }

}
