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


/*
 * Created by: thma
 * Date: May 6, 2001
 */
package org.apache.ojb.odmg.shared;

import java.util.Collection;
import java.io.Serializable;

public class Member implements Serializable
{

    private int id;
    private String firstname;
    private String lastname;
    private Collection projects;
    private Collection roles;

    public Member()
    {
    }

    public Member(int pId, String pFirstname, String pLastname)
    {
        id = pId;
        firstname = pFirstname;
        lastname = pLastname;
    }

    public Collection getRoles()
    {
        return roles;
    }

    public void setRoles(Collection roles)
    {
        this.roles = roles;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getFirstname()
    {
        return firstname;
    }

    public void setFirstname(String firstname)
    {
        this.firstname = firstname;
    }

    public String getLastname()
    {
        return lastname;
    }

    public void setLastname(String lastname)
    {
        this.lastname = lastname;
    }

    public Collection getProjects()
    {
        return projects;
    }

    public void setProjects(Collection projects)
    {
        this.projects = projects;
    }

    public String toString()
    {
        String result = firstname;
        return result;
    }

}
