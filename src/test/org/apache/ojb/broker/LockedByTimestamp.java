package org.apache.ojb.broker;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 *
 */
public class LockedByTimestamp implements Serializable
{
    private int id;
    private String value;

    private Timestamp timestamp;

	public LockedByTimestamp()
	{

	}

	public LockedByTimestamp(int theId, String theValue, Timestamp theTimestamp)
	{
	 	id = theId;
	 	value = theValue;
	 	timestamp = theTimestamp;
	}

    /**
     * Gets the id.
     * @return Returns a int
     */
    public int getId()
    {
        return id;
    }

    /**
     * Sets the id.
     * @param id The id to set
     */
    public void setId(int id)
    {
        this.id = id;
    }

    /**
     * Gets the value.
     * @return Returns a String
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Sets the value.
     * @param value The value to set
     */
    public void setValue(String value)
    {
        this.value = value;
    }


    /**
     * Gets the timestamp.
     * @return Returns a Timestamp
     */
    public Timestamp getTimestamp()
    {
        return timestamp;
    }

    /**
     * Sets the timestamp.
     * @param timestamp The timestamp to set
     */
    public void setTimestamp(Timestamp timestamp)
    {
        this.timestamp = timestamp;
    }

}
