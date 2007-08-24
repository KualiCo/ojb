/*
 * Created by IntelliJ IDEA.
 * User: Matt
 * Date: Jun 9, 2002
 * Time: 5:19:26 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.Date;

public class RelatedToContract implements Serializable
{
    private String pk;
    private String relatedValue1;
    private int relatedValue2;
    private Date relatedValue3;

    public String getPk()
    {
        return pk;
    }

    public void setPk(String pk)
    {
        this.pk = pk;
    }

    public String getRelatedValue1()
    {
        return relatedValue1;
    }

    public void setRelatedValue1(String relatedValue1)
    {
        this.relatedValue1 = relatedValue1;
    }

    public int getRelatedValue2()
    {
        return relatedValue2;
    }

    public void setRelatedValue2(int relatedValue2)
    {
        this.relatedValue2 = relatedValue2;
    }

    public Date getRelatedValue3()
    {
        return relatedValue3;
    }

    public void setRelatedValue3(Date relatedValue3)
    {
        this.relatedValue3 = relatedValue3;
    }
}
