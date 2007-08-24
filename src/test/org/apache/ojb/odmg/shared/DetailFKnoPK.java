package org.apache.ojb.odmg.shared;

import java.io.Serializable;

/**
 * the facade to all TestCases in this package.
 * @author Thomas Mahler
 */
public class DetailFKnoPK implements Serializable
{
    public Integer masterId;
    public Integer detailId;
    public String detailText;
    public Master master;
    
    public DetailFKnoPK(Integer dId, Integer mId, String text)
    {
        masterId = mId;
        detailId = dId;
        detailText = text;
    }
    
    public DetailFKnoPK()
    {
        super();
    }
    
    
    public String toString()
    {
        return " DetailFKnoPK detailId = " + detailId + " masterId = " + masterId;
    }
    
    /**
     * Gets the detailId.
     * @return Returns a Integer
     */
    public Integer getDetailId()
    {
        return detailId;
    }

    /**
     * Sets the detailId.
     * @param detailId The detailId to set
     */
    public void setDetailId(Integer detailId)
    {
        this.detailId = detailId;
    }

    /**
     * Gets the detailText.
     * @return Returns a String
     */
    public String getDetailText()
    {
        return detailText;
    }

    /**
     * Sets the detailText.
     * @param detailText The detailText to set
     */
    public void setDetailText(String detailText)
    {
        this.detailText = detailText;
    }

    /**
     * Gets the master.
     * @return Returns a Master
     */
    public Master getMaster()
    {
        return master;
    }

    /**
     * Sets the master.
     * @param master The master to set
     */
    public void setMaster(Master master)
    {
        this.master = master;
    }

    /**
     * Gets the masterId.
     * @return Returns a Integer
     */
    public Integer getMasterId()
    {
        return masterId;
    }

    /**
     * Sets the masterId.
     * @param masterId The masterId to set
     */
    public void setMasterId(Integer masterId)
    {
        this.masterId = masterId;
    }

}
