package org.apache.ojb.broker;

import java.io.Serializable;

/**
 * @author <a href="mailto:schneider@mendel.imp.univie.ac.at">Georg Schneider</a>
 *
 */
public class Reptile implements InterfaceAnimal, Serializable
{
    private int animalId;
    private int age;
    private String name;
    private String color;
    private int zooId;
	/**
	 * Constructor for Plant.
	 */
	public Reptile()
	{
		super();
	}

    public Reptile(int age, String name, String color)
    {
     this.age = age;
     this.name = name;
     this.color = color;
    }

    public int getAge()
    {
        return age;
    }

    public String getName()
    {
        return name;
    }

    public String getColor()
    {
        return color;
    }

   public String toString()
    {
        return "Reptile: id = " + animalId + "\n name = " + name +
                "\n age = " + age +
                "\n color = " + color +
                "\n zooId = " + zooId;
    }

    /**
     * Returns the animalId.
     * @return int
     */
    public int getAnimalId()
    {
        return animalId;
    }

    /**
     * Returns the zooId.
     * @return int
     */
    public int getZooId()
    {
        return zooId;
    }

    /**
     * Sets the age.
     * @param age The age to set
     */
    public void setAge(int age)
    {
        this.age = age;
    }

    /**
     * Sets the animalId.
     * @param animalId The animalId to set
     */
    public void setAnimalId(int animalId)
    {
        this.animalId = animalId;
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
     * Sets the name.
     * @param name The name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Sets the zooId.
     * @param zooId The zooId to set
     */
    public void setZooId(int zooId)
    {
        this.zooId = zooId;
    }

}
