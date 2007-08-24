/*
 * Created by IntelliJ IDEA.
 * User: Matt
 * Date: Jun 9, 2002
 * Time: 6:51:40 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.apache.ojb.broker;

import java.io.Serializable;

public class BidirectionalAssociationObjectB implements Serializable
{
    private String pk;
    private String fkToA;
    private BidirectionalAssociationObjectA relatedA;

    public String getPk()
    {
        return pk;
    }

    public void setPk(String pk)
    {
        this.pk = pk;
    }

    public String getFkToA()
    {
        return fkToA;
    }

    public void setFkToA(String fkToA)
    {
        this.fkToA = fkToA;
    }

    public BidirectionalAssociationObjectA getRelatedA()
    {
        return relatedA;
    }

    public void setRelatedA(BidirectionalAssociationObjectA relatedA)
    {
        this.relatedA = relatedA;
    }
}
