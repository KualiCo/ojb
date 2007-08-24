package org.apache.ojb.broker;

import java.io.Serializable;

import org.apache.ojb.broker.util.collections.RemovalAwareList;


/**
 * represents a product group containing a set of Articles.
 * @see Article
 */
public class ProductGroupWithRemovalAwareCollection implements Serializable
{

    /** collection containing all articles of a given product group*/
    private RemovalAwareList allArticlesInGroup;

    /** the unique id of a product group*/
    private int groupId;

    /** the name of a group*/
    private String groupName;


    /** return group id*/
    public int getId()
    {
        return groupId;
    }


    /** return groupname*/
    public String getName()
    {
        return groupName;
    }

    public ProductGroupWithRemovalAwareCollection()
    {
    }

    /** return List of all Articles in productgroup*/
    public RemovalAwareList getAllArticles()
    {
        return allArticlesInGroup;
    }
    
    public synchronized void add(Article art)
    {
    	if (allArticlesInGroup == null)
    	{	
    		allArticlesInGroup = new RemovalAwareList();
    	}
    	this.allArticlesInGroup.add(art);	
    }

    /** set group id*/
    public void setId(int newValue)
    {
        groupId = newValue;
    }

    /**
     * Sets the allArticlesInGroup.
     * @param allArticlesInGroup The allArticlesInGroup to set
     */
    public void setAllArticlesInGroup(RemovalAwareList allArticlesInGroup)
    {
        this.allArticlesInGroup = allArticlesInGroup;
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
