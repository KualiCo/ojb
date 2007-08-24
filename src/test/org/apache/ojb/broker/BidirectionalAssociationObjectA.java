/*
 * Created by IntelliJ IDEA.
 * User: Matt
 * Date: Jun 9, 2002
 * Time: 6:51:35 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.apache.ojb.broker;

import java.io.Serializable;

public class BidirectionalAssociationObjectA implements Serializable
{
    private String pk;
    private String fkToB;
    private BidirectionalAssociationObjectB relatedB;

    public String getPk()
    {
        return pk;
    }

    public void setPk(String pk)
    {
        this.pk = pk;
    }

    public String getFkToB()
    {
        return fkToB;
    }

    public void setFkToB(String fkToB)
    {
        this.fkToB = fkToB;
    }

    public BidirectionalAssociationObjectB getRelatedB()
    {
        return relatedB;
    }

    public void setRelatedB(BidirectionalAssociationObjectB relatedB)
    {
        this.relatedB = relatedB;
    }
}
