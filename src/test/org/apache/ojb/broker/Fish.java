package org.apache.ojb.broker;

import java.io.Serializable;


/**
 * class used in testing polymorphic m:n collections
 * @author <a href="mailto:schneider@mendel.imp.univie.ac.at">Georg Schneider</a>
 *
 */
public class Fish implements InterfaceFood, Serializable
{
    int foodId;
    String name;
    int calories;
    String typeOfWater;

	/**
	 * Constructor for Fish.
	 */
	public Fish()
	{
		super();
	}
    
    public Fish(String name, int calories, String typeOfWater)
    {
        this.calories = calories;
        this.name = name;
        this.typeOfWater = typeOfWater;
    }

	/**
	 * @see org.apache.ojb.broker.InterfaceFood#getName()
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @see org.apache.ojb.broker.InterfaceFood#getCalories()
	 */
	public int getCalories()
	{
		return calories;
	}



	/**
	 * Returns the typeOfWater.
	 * @return String
	 */
	public String getTypeOfWater()
	{
		return typeOfWater;
	}

	/**
	 * Returns the foodId.
	 * @return int
	 */
	public int getFoodId()
	{
		return foodId;
	}
    
    public String toString()
    {
       return "Fish: id = " + foodId + "\n name = " + name +
                "\n calories = " + calories +
                "\n Type of water = " + typeOfWater;        
    }


    /**
     * Sets the calories.
     * @param calories The calories to set
     */
    public void setCalories(int calories)
    {
        this.calories = calories;
    }

    /**
     * Sets the foodId.
     * @param foodId The foodId to set
     */
    public void setFoodId(int foodId)
    {
        this.foodId = foodId;
    }

    /**
     * Sets the name.
     * @param name The name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Sets the typeOfWater.
     * @param typeOfWater The typeOfWater to set
     */
    public void setTypeOfWater(String typeOfWater)
    {
        this.typeOfWater = typeOfWater;
    }

}
