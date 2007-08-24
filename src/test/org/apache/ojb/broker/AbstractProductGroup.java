package org.apache.ojb.broker;

import java.util.List;
import java.util.Vector;

/** represents a product group containing a set of Articles.
 * @see Article
 */
public abstract class AbstractProductGroup implements InterfaceProductGroup
{
    /** add article to group*/
    public synchronized void add(InterfaceArticle article)
    {
        if (allArticlesInGroup == null)
        {
            allArticlesInGroup = new Vector();
        }
        article.setProductGroup(this);
        allArticlesInGroup.add(article);
    }

    /** return group id*/
    public Integer getId()
    {
        return groupId;
    }

    /**return string representation*/
    public String toString()
    {
        return
                "----\n" +
                "group Id:    " + groupId + "\n" +
                "name:        " + groupName + "\n" +
                "description: " + description + "\n" +
                "articles in group: " + allArticlesInGroup;
    }

    /** return groupname*/
    public String getName()
    {
        return groupName;
    }

    /** collection containing all articles of a given product group*/
    private List allArticlesInGroup;
    /** a textual description of the group*/
    private String description;
    /** the unique id of a product group*/
    private Integer groupId;

    public AbstractProductGroup()
    {
    }

    public AbstractProductGroup(Integer pGroupId, String pGroupName, String pDescription)
    {
        groupId = pGroupId;
        groupName = pGroupName;
        description = pDescription;
    }

    public void setName(String groupName)
    {
        this.groupName = groupName;
    }

    /** the name of a group*/
    private String groupName;

    /** return List of all Articles in productgroup*/
    public List getAllArticles()
    {
        return allArticlesInGroup;
    }

    /** set group id*/
    public void setId(Integer newValue)
    {
        groupId = newValue;
    }
    /**
     * Gets the groupId.
     * @return Returns a int
     */
    public Integer getGroupId()
    {
        return groupId;
    }

    /**
     * Sets the groupId.
     * @param groupId The groupId to set
     */
    public void setGroupId(Integer groupId)
    {
        this.groupId = groupId;
    }

    /**
     * Gets the description.
     * @return Returns a String
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the description.
     * @param description The description to set
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Gets the groupName.
     * @return Returns a String
     */
    public String getGroupName()
    {
        return groupName;
    }

    /**
     * Sets the groupName.
     * @param groupName The groupName to set
     */
    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    /**
     * Gets the allArticlesInGroup.
     * @return Returns a List
     */
    public List getAllArticlesInGroup()
    {
        return allArticlesInGroup;
    }

    /**
     * Sets the allArticlesInGroup.
     * @param allArticlesInGroup The allArticlesInGroup to set
     */
    public void setAllArticlesInGroup(List allArticlesInGroup)
    {
        this.allArticlesInGroup = allArticlesInGroup;
    }

}
