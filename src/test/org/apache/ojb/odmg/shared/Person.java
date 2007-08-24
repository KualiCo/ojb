/*
 * Created by IntelliJ IDEA.
 * User: tom
 * Date: May 28, 2001
 * Time: 10:30:56 PM
 * To change template for new interface use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.apache.ojb.odmg.shared;

import java.io.Serializable;

public interface Person extends Serializable
{
    int getId();

    void setId(int id);

    public abstract String getFirstname();

    public abstract String getLastname();

    public abstract Person getMother();

    public abstract Person getFather();

    public abstract Person[] getChildren();


    public abstract void setFirstname(String pFirstname);

    public abstract void setLastname(String pLastname);

    public abstract void setMother(Person pMother);

    public abstract void setFather(Person pFather);

    public abstract void setChildren(Person[] pChildren);

    public abstract void addChild(Person pChild);
}
