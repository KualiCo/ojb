/*
 * Created by IntelliJ IDEA.
 * User: Matt
 * Date: Jun 8, 2002
 * Time: 3:30:25 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.Date;

public class Effectiveness implements Serializable
{
    private String pk;
    private String fkToVersion;
    private String effValue1;
    private int effValue2;
    private Date effValue3;
    private Version version;

    public String getPk()
    {
        return pk;
    }

    public void setPk(String pk)
    {
        this.pk = pk;
    }

    public String getFkToVersion()
    {
        return fkToVersion;
    }

    public void setFkToVersion(String fkToVersion)
    {
        this.fkToVersion = fkToVersion;
    }

    public String getEffValue1()
    {
        return effValue1;
    }

    public void setEffValue1(String effValue1)
    {
        this.effValue1 = effValue1;
    }

    public int getEffValue2()
    {
        return effValue2;
    }

    public void setEffValue2(int effValue2)
    {
        this.effValue2 = effValue2;
    }

    public Date getEffValue3()
    {
        return effValue3;
    }

    public void setEffValue3(Date effValue3)
    {
        this.effValue3 = effValue3;
    }

    public Version getVersion()
    {
        return version;
    }

    public void setVersion(Version version)
    {
        this.version = version;
    }
}
