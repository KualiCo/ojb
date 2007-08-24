package org.apache.ojb.odmg.shared;

import java.io.Serializable;
import java.util.Vector;

/**
 * the facade to all TestCases in this package.
 * @author Thomas Mahler
 */
public class Master implements Serializable
{
    public Integer masterId;
    public String masterText;
    public Vector collDetailFKinPK = new Vector();
    public Vector collDetailFKnoPK = new Vector();

    public Master(Integer id, String text)
    {
        masterId = id;
        masterText = text;
    }

    public Master()
    {
        super();
    }


    public String toString()
    {
        StringBuffer strBuf = new StringBuffer("Master: masterId = " + masterId);
        strBuf.append(" collDetailFKinPK + [ ");
        if (collDetailFKinPK != null)
        {
            java.util.Iterator it = collDetailFKinPK.iterator();
            while (it.hasNext())
                strBuf.append(it.next().toString() + " ");
        }
        strBuf.append("] collDetailFKnoPK [");
        if (collDetailFKnoPK != null)
        {
            java.util.Iterator it = collDetailFKnoPK.iterator();
            while (it.hasNext())
                strBuf.append(it.next().toString() + " ");
            strBuf.append("]");
        }
        return strBuf.toString();
    }
    /**
     * Gets the collDetailFKinPK.
     * @return Returns a java.util.Vector
     */
    public Vector getCollDetailFKinPK()
    {
        return collDetailFKinPK;
    }

    /**
     * Sets the collDetailFKinPK.
     * @param collDetailFKinPK The collDetailFKinPK to set
     */
    public void setCollDetailFKinPK(java.util.Vector collDetailFKinPK)
    {
        this.collDetailFKinPK = collDetailFKinPK;
    }

    /**
     * Gets the collDetailFKnoPK.
     * @return Returns a java.util.Vector
     */
    public Vector getCollDetailFKnoPK()
    {
        return collDetailFKnoPK;
    }

    /**
     * Sets the collDetailFKnoPK.
     * @param collDetailFKnoPK The collDetailFKnoPK to set
     */
    public void setCollDetailFKnoPK(java.util.Vector collDetailFKnoPK)
    {
        this.collDetailFKnoPK = collDetailFKnoPK;
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

    /**
     * Gets the masterText.
     * @return Returns a String
     */
    public String getMasterText()
    {
        return masterText;
    }

    /**
     * Sets the masterText.
     * @param masterText The masterText to set
     */
    public void setMasterText(String masterText)
    {
        this.masterText = masterText;
    }

}
