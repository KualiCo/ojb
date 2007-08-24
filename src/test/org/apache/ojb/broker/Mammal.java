package org.apache.ojb.broker;

import java.io.Serializable;

/**
 * @author <a href="mailto:schneider@mendel.imp.univie.ac.at">Georg Schneider</a>
 *
 */
public class Mammal implements InterfaceAnimal, Serializable
{
    private int animalId;
    private int age;
    private String name;
    private int numLegs;
    private int zooId;


	/**
	 * Constructor for Animal.
	 */
	public Mammal()
	{
		super();
	}

    public Mammal(int age, String name, int numLegs)
    {
        this.age = age;
        this.name = name;
        this.numLegs = numLegs;
    }


    public int getAge()
    {
        return age;
    }

    public String getName()
    {
        return name;
    }

    public int getNumLegs()
    {
        return numLegs;
    }

    public String toString()
    {
        return "Mammal: id = " + animalId + "\n name = " + name +
                "\n age = " + age +
                "\n Number of legs = " + numLegs +
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
     * Sets the name.
     * @param name The name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Sets the numLegs.
     * @param numLegs The numLegs to set
     */
    public void setNumLegs(int numLegs)
    {
        this.numLegs = numLegs;
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
