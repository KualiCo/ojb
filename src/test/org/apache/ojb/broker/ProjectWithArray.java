package org.apache.ojb.broker;

/**
 *
 * @author <a href="mailto:om@ppi.de">Oliver Matz</a>
 * @version $Id: ProjectWithArray.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class ProjectWithArray
{
  private int id;
  private String title;
  private String description;
  private PersonWithArray[] persons;

  public ProjectWithArray()
  {
  }

  public ProjectWithArray(int pId, String pTitle, String pDescription)
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

  public PersonWithArray[] getPersons()
  {
      return persons;
  }

  public void setPersons(PersonWithArray[] persons)
  {
      this.persons = persons;
  }

  public String toString()
  {
      String result = title;
      result += " ";
      result += persons;

      return result;
  }

} 
