package org.apache.ojb.broker;

/**
 * @author <a href="mailto:om@ppi.de">Oliver Matz</a>
 * @version $Id: PersonWithArray.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class PersonWithArray
{

  private int id;
  private String firstname;
  private String lastname;
  private ProjectWithArray[] projects;

  public PersonWithArray()
  {
  }

  public PersonWithArray(int pId, String pFirstname, String pLastname)
  {
      id = pId;
      firstname = pFirstname;
      lastname = pLastname;
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

  public ProjectWithArray[] getProjects()
  {
      return projects;
  }

  public void setProjects(ProjectWithArray[] projects)
  {
      this.projects = projects;
  }

  public String toString()
  {
      String result = firstname;
      return result;
  }
}
