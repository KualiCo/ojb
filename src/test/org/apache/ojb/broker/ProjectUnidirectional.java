/*
 * ObJectRelationalBridge - Bridging Java Objects and Relational Databases
 * http://objectbridge.sourceforge.net
 * Copyright (C) 2000, 2001 Thomas Mahler, et al.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */

package org.apache.ojb.broker;

import java.io.Serializable;

public class ProjectUnidirectional implements Serializable
{
    private int id;
    private String title;
    private String description;


    public ProjectUnidirectional()
    {
    }

    public ProjectUnidirectional(int pId, String pTitle, String pDescription)
    {
        id = pId;
        title = pTitle;
        description = pDescription;
    }


    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String toString()
    {
        String result = title;
        result += " ";

        return result;
    }
}
