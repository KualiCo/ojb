package org.apache.ojb.odmg.shared;

import java.io.Serializable;
import java.util.Vector;
import java.util.List;

/** represents a product group containing a set of Articles.
 * @see org.apache.ojb.odmg.shared.Article
 */
public class ProductGroup implements org.apache.ojb.odmg.TransactionAware, Serializable
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

    public ProductGroup(int pGroupId, String pGroupName,
                        String pDescription)
    {
        groupId = pGroupId;
        groupName = pGroupName;
        description = pDescription;
    }

    public ProductGroup()
    {
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
    private int groupId;
    /** the name of a group*/
    private String groupName;

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
     * afterAbort will be called after a transaction has been aborted.
     * The values of fields which get persisted will have changed to
     * what they were at the begining of the transaction.  This method
     * should be overridden to reset any transient or non-persistent
     * fields.
     */
    public void afterAbort()
    {
        //System.out.println("afterAbort: " + new Identity(this));
    }

    /**
     * afterCommit is called only after a successful commit has taken
     * place.
     */
    public void afterCommit()
    {
        //System.out.println("afterCommit: " + new Identity(this));
    }

    /**
     * beforeAbort is called before a transaction is aborted.
     */
    public void beforeAbort()
    {
        //System.out.println("beforeAbort: " + new Identity(this));
    }

    /**
     * beforeCommit will give an object a chance to kill a
     * transaction before it is committed.
     *
     * To kill a transaction, throw a new TransactionAbortedException.
     */
    public void beforeCommit() throws org.odmg.TransactionAbortedException
    {
        //System.out.println("beforeCommit: " + new Identity(this));
    }


    /** set groupname*/
    public void setName(String newName)
    {
        groupName = newName;
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
    public void setAllArticlesInGroup(Vector allArticlesInGroup)
    {
        this.allArticlesInGroup = allArticlesInGroup;
    }

    public void addArticle(Article article)
    {
        if(allArticlesInGroup == null)
        {
            allArticlesInGroup = new Vector();
        }
        allArticlesInGroup.add(article);
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
