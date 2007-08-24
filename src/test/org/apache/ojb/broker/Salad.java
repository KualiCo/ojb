package org.apache.ojb.broker;

import java.io.Serializable;

/**
 * @author <a href="mailto:schneider@mendel.imp.univie.ac.at">Georg Schneider</a>
 *
 */
public class Salad implements InterfaceFood, Serializable
{

    int foodId;
    String name;
    int calories;
    String color;
	/**
	 * Constructor for Salad.
	 */
	public Salad()
	{
		super();
	}
    
    public Salad(String name, int calories, String color)
    {
        this.name = name;
        this.calories = calories;
        this.color = color;   
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
	 * Returns the color.
	 * @return String
	 */
	public String getColor()
	{
		return color;
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
       return "Salad: id = " + foodId + "\n name = " + name +
                "\n calories = " + calories +
                "\n Color = " + color;        
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
     * Sets the color.
     * @param color The color to set
     */
    public void setColor(String color)
    {
        this.color = color;
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

}
