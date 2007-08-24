/*
 * Created by IntelliJ IDEA.
 * User: Matt
 * Date: Jun 8, 2002
 * Time: 3:20:08 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.Date;

public class Contract implements Serializable
{
    private String pk;
    private String contractValue1;
    private int contractValue2;
    private String contractValue3;
    private Date contractValue4;
    private String fkToRelated;
    private RelatedToContract relatedToContract;

    public String getFkToRelated()
    {
        return fkToRelated;
    }

    public void setFkToRelated(String fkToRelated)
    {
        this.fkToRelated = fkToRelated;
    }

    public RelatedToContract getRelatedToContract()
    {
        return relatedToContract;
    }

    public void setRelatedToContract(RelatedToContract relatedToContract)
    {
        this.relatedToContract = relatedToContract;
    }

    public String getPk()
    {
        return pk;
    }

    public void setPk(String pk)
    {
        this.pk = pk;
    }

    public String getContractValue1()
    {
        return contractValue1;
    }

    public void setContractValue1(String contractValue1)
    {
        this.contractValue1 = contractValue1;
    }

    public int getContractValue2()
    {
        return contractValue2;
    }

    public void setContractValue2(int contractValue2)
    {
        this.contractValue2 = contractValue2;
    }

    public String getContractValue3()
    {
        return contractValue3;
    }

    public void setContractValue3(String contractValue3)
    {
        this.contractValue3 = contractValue3;
    }

    public Date getContractValue4()
    {
        return contractValue4;
    }

    public void setContractValue4(Date contractValue4)
    {
        this.contractValue4 = contractValue4;
    }
}
