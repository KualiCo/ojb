/*
 * (C) 2003 ppi Media
 * User: om
 */

package org.apache.ojb.broker.cloneable;

/**
 * implements Cloneable so that it can be used in a two-level cache
 *
 * @author <a href="mailto:om@ppi.de">Oliver Matz</a>
 * @version $Id: CloneableGroup.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class CloneableGroup implements Cloneable
{
  private Integer id;
  private String groupName;

  /**
   * called by reflection in {@link org.apache.ojb.broker.cache.ObjectCacheTwoLevelImpl}.
   * @return
   * @throws CloneNotSupportedException
   */
  public Object clone() throws CloneNotSupportedException
  {
    return super.clone();
  }

  /**
   * mapped to column KategorieName
   */
  public final String getName()
  {
    return groupName;
  }

  public final void setName(String name)
  {
    this.groupName = name;
  }

  public String toString()
  {
    return "CloneableGroup#" + System.identityHashCode(this) + " - " + id + ": " + groupName;
  }
}
