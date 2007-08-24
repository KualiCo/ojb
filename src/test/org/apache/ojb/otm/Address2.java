package org.apache.ojb.otm;

public class Address2
{

    private int id;
    private int personId;
    private Person person;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getPersonId()
    {
        return personId;
    }

    public void setPersonId(int personId)
    {
        this.personId = personId;
    }

    public Person getPerson()
    {
        return person;
    }

    public void setPerson(Person person)
    {
        this.person = person;
    }
}
