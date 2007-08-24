package org.apache.ojb.broker;


/** represents a product group containing a set of Articles.
 * @see Article
 */
public class ProductGroupWithTypedCollection implements java.io.Serializable
{

    /** return group id*/
    public int getId()
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
    private ArticleCollection allArticlesInGroup;
    /** a textual description of the group*/
    private String description;
    /** the unique id of a product group*/
    private int groupId;
    /** the name of a group*/
    private String groupName;

    public ProductGroupWithTypedCollection(int pGroupId, String pGroupName, String pDescription)
    {
        groupId = pGroupId;
        groupName = pGroupName;
        description = pDescription;
    }

    public ProductGroupWithTypedCollection()
    {
    }


    /** return List of all Articles in productgroup*/
    public ArticleCollection getAllArticles()
    {
        return allArticlesInGroup;
    }

    /** set group id*/
    public void setId(int newValue)
    {
        groupId = newValue;
    }
    /**
     * Gets the allArticlesInGroup.
     * @return Returns a ArticleCollection
     */
    public ArticleCollection getAllArticlesInGroup()
    {
        return allArticlesInGroup;
    }

    /**
     * Sets the allArticlesInGroup.
     * @param allArticlesInGroup The allArticlesInGroup to set
     */
    public void setAllArticlesInGroup(ArticleCollection allArticlesInGroup)
    {
        this.allArticlesInGroup = allArticlesInGroup;
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
     * Gets the groupId.
     * @return Returns a int
     */
    public int getGroupId()
    {
        return groupId;
    }

    /**
     * Sets the groupId.
     * @param groupId The groupId to set
     */
    public void setGroupId(int groupId)
    {
        this.groupId = groupId;
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

}
