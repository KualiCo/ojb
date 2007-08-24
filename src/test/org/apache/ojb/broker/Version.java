package org.apache.ojb.broker;

/* Copyright 2002-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;
import java.util.Date;

public class Version implements Serializable
{
    private String pk;
    private String fkToContract;
    private String versionValue1;
    private int versionValue2;

    public String getPk()
    {
        return pk;
    }

    public void setPk(String pk)
    {
        this.pk = pk;
    }

    public String getFkToContract()
    {
        return fkToContract;
    }

    public void setFkToContract(String fkToContract)
    {
        this.fkToContract = fkToContract;
    }

    public String getVersionValue1()
    {
        return versionValue1;
    }

    public void setVersionValue1(String versionValue1)
    {
        this.versionValue1 = versionValue1;
    }

    public int getVersionValue2()
    {
        return versionValue2;
    }

    public void setVersionValue2(int versionValue2)
    {
        this.versionValue2 = versionValue2;
    }

    public Date getVersionValue3()
    {
        return versionValue3;
    }

    public void setVersionValue3(Date versionValue3)
    {
        this.versionValue3 = versionValue3;
    }

    public Contract getContract()
    {
        return contract;
    }

    public void setContract(Contract contract)
    {
        this.contract = contract;
    }

    private Date versionValue3;
    private Contract contract;
}
