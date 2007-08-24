package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.Date;

import org.apache.ojb.broker.util.GUID;

/**
 * @author tom
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class GuidTestEntity implements Serializable
{
	private GUID guid;
	private String value;
	
    /**
     * Constructor for GuidTestEntity.
     */
    public GuidTestEntity()
    {
        super();
        guid = new GUID();
        value = new Date().toString();
    }

    /**
     * Returns the guid.
     * @return GUID
     */
    public GUID getGuid()
    {
        return guid;
    }

    /**
     * Returns the value.
     * @return String
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Sets the guid.
     * @param guid The guid to set
     */
    public void setGuid(GUID guid)
    {
        this.guid = guid;
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
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return guid.hashCode();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return guid.toString() + " : " + value; 
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    public boolean equals(Object obj)
    {
    	if (obj instanceof GuidTestEntity)
    	{
    		if (((GuidTestEntity) obj).getGuid().equals(this.getGuid()))
    		{
    			return true;
    		}
    	}
        return false;
    }

}
