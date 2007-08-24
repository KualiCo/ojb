package org.apache.ojb.otm;

public class Debitor
{
    private int id;
    private AbstractPerson abstractPerson;
    private int personId;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public AbstractPerson getAbstractPerson()
    {
        return abstractPerson;
    }

    public void setAbstractPerson(AbstractPerson abstractPerson)
    {
        this.abstractPerson = abstractPerson;
    }

    public int getPersonId()
    {
        return personId;
    }

    public void setPersonId(int personId)
    {
        this.personId = personId;
    }
}
