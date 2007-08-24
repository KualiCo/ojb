package org.apache.ojb.broker;


/**
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: FarAwayReference.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class FarAwayReference
        implements FarAwayReferenceIF
{
    private String name;
    private int id;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String toString()
    {
        return this.getClass().getName()+" [ "+"id: "+id+", name: "+name+"]";
    }
}
