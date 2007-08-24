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

/**
 * Definition of a foreignkey for a torque table.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ForeignkeyDef extends DefBase
{
    /** The local columns */
    private ArrayList _localColumns = new ArrayList();
    /** The remote columns */
    private ArrayList _remoteColumns = new ArrayList();
    
    /**
     * Creates a new foreignkey definition object.
     *
     * @param name      The name of the foreignkey element
     * @param tableName The name of the remote table
     */
    public ForeignkeyDef(String name, String tableName)
    {
        super(name == null ? "" : name);
        setProperty(PropertyHelper.TORQUE_PROPERTY_FOREIGNTABLE, tableName);
    }

    /**
     * Returns the name of the referenced table.
     * 
     * @return The table name
     */
    public String getTableName()
    {
        return getProperty(PropertyHelper.TORQUE_PROPERTY_FOREIGNTABLE);
    }

    /**
     * Adds a column pair to this foreignkey.
     * 
     * @param localColumn  The column in the local table
     * @param remoteColumn The column in the remote table
     */
    public void addColumnPair(String localColumn, String remoteColumn)
    {
        if (!_localColumns.contains(localColumn))
        {    
            _localColumns.add(localColumn);
        }
        if (!_remoteColumns.contains(remoteColumn))
        {    
            _remoteColumns.add(remoteColumn);
        }
    }

    /**
     * Returns the number of column pairs of this foreignkey definition.
     * 
     * @return The number of pairs
     */
    public int getNumColumnPairs()
    {
        return _localColumns.size();
    }
    
    /**
     * Returns the local column of the specified pair.
     * 
     * @param idx Specifies the pair
     * @return The local column of the pair
     */
    public String getLocalColumn(int idx)
    {
        return (String)_localColumns.get(idx);
    }

    /**
     * Returns the remote column of the specified pair.
     * 
     * @param idx Specifies the pair
     * @return The remote column of the pair
     */
    public String getRemoteColumn(int idx)
    {
        return (String)_remoteColumns.get(idx);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        if (!(obj instanceof ForeignkeyDef))
        {
            return false;
        }

        ForeignkeyDef otherForeignkeyDef = (ForeignkeyDef)obj;

        if (!getTableName().equals(otherForeignkeyDef.getTableName()))
        {
            return false;
        }
        if (!_localColumns.equals(otherForeignkeyDef._localColumns))
        {
            return false;
        }
        if (!_remoteColumns.equals(otherForeignkeyDef._remoteColumns))
        {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        StringBuffer textRep = new StringBuffer();

        textRep.append(getTableName());
        textRep.append(" ");
        textRep.append(_localColumns.toString());
        textRep.append(" ");
        textRep.append(_remoteColumns.toString());

        return textRep.toString().hashCode();
    }
}
