package org.apache.ojb.otm;

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractPerson
{
    private int id;
    protected Collection addresses = new ArrayList();
    protected String name;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public Collection getAddresses()
    {
        return addresses;
    }

    public void setAddresses(Collection addresses)
    {
        this.addresses = addresses;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
