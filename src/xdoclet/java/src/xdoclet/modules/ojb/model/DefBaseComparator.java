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
 * Helper class for sorting a list of definitions according to their names.
 * 
 * @author  <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 * @created Juli 12, 2003
 */
public class DefBaseComparator implements Comparator
{

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object objA, Object objB)
    {
        if (!(objA instanceof DefBase) || !(objB instanceof DefBase))
        {
            return 0;
        }
        else
        {
            return ((DefBase)objA).getName().compareTo(((DefBase)objB).getName());
        }
    }

}
