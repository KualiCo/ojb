package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * class used in testing polymorphic m:n collections
 * @author <a href="mailto:schneider@mendel.imp.univie.ac.at">Georg Schneider</a>
 *
 */
public class Gourmet implements Serializable
{
    int gourmetId;
    String name;
    List favoriteFood = new Vector();
	/**
	 * Constructor for Gourmet.
	 */
	public Gourmet()
	{
		super();
	}
    
    public Gourmet(String name)
    {
        this.name = name;
    }
    
    public List getFavoriteFood()
    {
        return favoriteFood;
    }
    
    public void addFavoriteFood(InterfaceFood food)
    {
        favoriteFood.add(food);
    }

	/**
	 * Returns the gourmetId.
	 * @return int
	 */
	public int getGourmetId()
	{
		return gourmetId;
	}
    
    public String toString()
    {
     StringBuffer text = new StringBuffer("Gourmet: id = " + gourmetId + "\n");
     text.append("name = ");
     text.append(name);
     text.append("\nFavoriteFood:\n");
     for(Iterator it = favoriteFood.iterator(); it.hasNext();)
     {
        text.append(it.next().toString());
        text.append("\n-------\n");
     }
     return text.toString();   
    }


    /**
     * Returns the name.
     * @return String
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the favoriteFood.
     * @param favoriteFood The favoriteFood to set
     */
    public void setFavoriteFood(List favoriteFood)
    {
        this.favoriteFood = favoriteFood;
    }

    /**
     * Sets the gourmetId.
     * @param gourmetId The gourmetId to set
     */
    public void setGourmetId(int gourmetId)
    {
        this.gourmetId = gourmetId;
    }

    /**
     * Sets the name.
     * @param name The name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

}
