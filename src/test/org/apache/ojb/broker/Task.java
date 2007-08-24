package org.apache.ojb.broker;

import java.io.Serializable;

public class Task implements Serializable
{
    private int task_id;
    
    private int person_id;

    private int project_id;

    private String taskName;
    
    private Role role;

    public Task()
    {
    }

    public Task(int pTaskId, int pPersonId, int pProjectId, String pTaskname)
    {
        task_id = pTaskId;
        person_id = pPersonId;
        project_id = pProjectId;
        taskName = pTaskname;
    }


    public int getTask_id()
    {
        return task_id;
    }

    public void setTask_id(int task_id)
    {
        this.task_id = task_id;
    }

    public String getTaskName()
    {
        return taskName;
    }

    public void setTaskName(String taskName)
    {
        this.taskName = taskName;
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

    public Role getRole()
    {
        return role;
    }

    public void setRole(Role role)
    {
        this.role = role;
    }

    public String toString()
    {
        return taskName + " has the " + role.getRoleName() + " role assigned to it.";
    }
}
