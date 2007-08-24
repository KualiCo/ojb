package org.apache.ojb.broker;

import java.io.Serializable;

/**
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: FarAwayClass.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class FarAwayClass implements Serializable
{
    private String name;
    private int id;
    private String description;
    private Integer referenceId;
    private FarAwayReferenceIF reference;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public FarAwayReferenceIF getReference()
    {
        return reference;
    }

    public void setReference(FarAwayReferenceIF reference)
    {
        this.reference = reference;
    }

    public Integer getReferenceId()
    {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId)
    {
        this.referenceId = referenceId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String toString()
    {
        return this.getClass().getName()+" [ "+"id: "+id+", name: "+name+", description: "+description+
                        ", reference: "+reference+"]";
    }
}
