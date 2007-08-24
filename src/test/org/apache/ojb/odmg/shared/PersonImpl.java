/*
 * Created by IntelliJ IDEA.
 * User: tom
 * Date: May 28, 2001
 * Time: 10:35:05 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.apache.ojb.odmg.shared;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class PersonImpl implements Person, Serializable
{
    // technical attributes only needed for O/R mapping
    private int id;
    private int motherId;
    private int fatherId;

    // domain specific attributes
    private String firstname;
    private String lastname;
    private Person mother;
    private Person father;
    private Person[] children;

    public PersonImpl()
    {
    }

    public String getFirstname()
    {
        return firstname;
    }

    public String getLastname()
    {
        return lastname;
    }

    public Person getMother()
    {
        return mother;
    }

    public Person getFather()
    {
        return father;
    }

    public Person[] getChildren()
    {
        return children;
    }

    public void setFirstname(String pFirstname)
    {
        firstname = pFirstname;
    }

    public void setLastname(String pLastname)
    {
        lastname = pLastname;
    }

    public void setMother(Person pMother)
    {
        mother = pMother;
    }

    public void setFather(Person pFather)
    {
        father = pFather;
    }

    public void setChildren(Person[] pChildren)
    {
        children = pChildren;
    }

    public void addChild(Person pChild)
    {
        int numOfChildren = ((children == null) ? 0 : children.length);
        Person[] newKids = new Person[numOfChildren + 1];
        ArrayList list = new ArrayList(Arrays.asList(children));
        list.add(pChild);
        list.toArray(newKids);
    }

    /**
     * Gets the fatherId.
     * @return Returns a int
     */
    public int getFatherId()
    {
        return fatherId;
    }

    /**
     * Sets the fatherId.
     * @param fatherId The fatherId to set
     */
    public void setFatherId(int fatherId)
    {
        this.fatherId = fatherId;
    }

    /**
     * Gets the id.
     * @return Returns a int
     */
    public int getId()
    {
        return id;
    }

    /**
     * Sets the id.
     * @param id The id to set
     */
    public void setId(int id)
    {
        this.id = id;
    }

    /**
     * Gets the motherId.
     * @return Returns a int
     */
    public int getMotherId()
    {
        return motherId;
    }

    /**
     * Sets the motherId.
     * @param motherId The motherId to set
     */
    public void setMotherId(int motherId)
    {
        this.motherId = motherId;
    }

    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        buf.append("id", id);
        buf.append("firstname", firstname);
        buf.append("lastname", lastname);
        buf.append("motherId", motherId);
        buf.append("mother", mother != null ? "PersonImpl@" + System.identityHashCode(mother) + "(" + motherId + ")" : null);
        buf.append("fatherId", fatherId);
        buf.append("father", father != null ? "PersonImpl@" + System.identityHashCode(father) + "(" + fatherId + ")" : null);
        return buf.toString();
    }
}
