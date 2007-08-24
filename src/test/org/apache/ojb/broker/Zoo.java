package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author <a href="mailto:schneider@mendel.imp.univie.ac.at">Georg Schneider</a>
 *
 */
public class Zoo implements Serializable
{
    private int zooId;
    private String name;
    private List animals = new Vector();

	/**
	 * Constructor for Zoo.
	 */
	public Zoo()
	{
		super();
	}
    
    public Zoo(String name)
    {
        this.name = name;
    }
    
    public List getAnimals()
    {
        return animals;
    }

    public void addAnimal(InterfaceAnimal animal)
    {
        animals.add(animal);
    }
    
    public int getZooId()
    {
        return zooId;
    }
    
    public String toString()
    {
        return new ToStringBuilder(this)
                .append("id", zooId)
                .append("name", name)
                .append("animals", animals)
                .toString();
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
     * Sets the animals.
     * @param animals The animals to set
     */
    public void setAnimals(List animals)
    {
        this.animals = animals;
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
