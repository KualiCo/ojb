package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.List;

public class LockedByVersion implements Serializable
{
    private Integer id;
    private String value;
    private int version;
    private Integer fk;
    private List childs;

	public LockedByVersion()
	{
	}

    /**
     * Gets the id.
     * @return Returns a int
     */
    public Integer getId()
    {
        return id;
    }

    /**
     * Sets the id.
     * @param id The id to set
     */
    public void setId(Integer id)
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
     * Gets the version.
     * @return Returns a long
     */
    public int getVersion()
    {
        return version;
    }

    /**
     * Sets the version.
     * @param version The version to set
     */
    public void setVersion(int version)
    {
        this.version = version;
    }

    public Integer getFk()
    {
        return fk;
    }

    public void setFk(Integer fk)
    {
        this.fk = fk;
    }

    public List getChilds()
    {
        return childs;
    }

    public void setChilds(List childs)
    {
        this.childs = childs;
    }
}
