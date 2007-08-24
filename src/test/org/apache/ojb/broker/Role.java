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
package org.apache.ojb.broker;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Collection;
import java.io.Serializable;

public class Role implements Serializable
{
    private int person_id;
    private int project_id;
    private Person person;
    private Project project;
    private String roleName;
    private Collection tasks;

    public Role()
    {
    }

    public Role(int pPersonId, int pProjectId, String pRolename)
    {
        person_id = pPersonId;
        project_id = pProjectId;
        roleName = pRolename;
    }


    public String getRoleName()
    {
        return roleName;
    }

    public void setRoleName(String roleName)
    {
        this.roleName = roleName;
    }

    public int getPerson_id()
    {
        return person_id;
    }

    public void setPerson_id(int person_id)
    {
        this.person_id = person_id;
    }

    public int getProject_id()
    {
        return project_id;
    }

    public void setProject_id(int project_id)
    {
        this.project_id = project_id;
    }

    public Person getPerson()
    {
        return person;
    }

    public void setPerson(Person person)
    {
        this.person = person;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project project)
    {
        this.project = project;
    }

    public Collection getTasks()
    {
        return tasks;
    }

    public void setTasks(Collection tasks)
    {
        this.tasks = tasks;
    }

    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this);
        buf.append("roleName", roleName).
        append("personId", person_id).
        append("person", person).
        append("projectId", project_id).
        append("project", project).
        append("tasks", tasks);
        return buf.toString();
    }
}
