package org.apache.ojb.otm;

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

import java.util.ArrayList;
import java.io.Serializable;

public class Person implements Serializable
{

    private int id;
    private String firstname;
    private String lastname;
    private Integer mainAddressId;
    private Address mainAddress;
    private ArrayList otherAddresses;

    public Person()
    {
    }

    public Person(String firstname, String lastname)
    {
        this.firstname = firstname;
        this.lastname = lastname;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getFirstname()
    {
        return firstname;
    }

    public void setFirstname(String firstname)
    {
        this.firstname = firstname;
    }

    public String getLastname()
    {
        return lastname;
    }

    public void setLastname(String lastname)
    {
        this.lastname = lastname;
    }

    public Integer getMainAddressId()
    {
        return mainAddressId;
    }

    public void setMainAddressId(Integer mainAddressId)
    {
        this.mainAddressId = mainAddressId;
    }

    public Address getMainAddress()
    {
        return mainAddress;
    }

    public void setMainAddress(Address mainAddress)
    {
        this.mainAddress = mainAddress;
    }

    public ArrayList getOtherAddresses()
    {
        return otherAddresses;
    }

    public void setOtherAddresses(ArrayList otherAddresses)
    {
        this.otherAddresses = otherAddresses;
    }

    public void addOtherAddress(String desc, Address address)
    {
        if (otherAddresses == null)
        {
            otherAddresses = new ArrayList();
        }
        AddressDesc addrDesc = new AddressDesc(desc, address);
        this.otherAddresses.add(addrDesc);
        addrDesc.setPerson(this);
    }

}
