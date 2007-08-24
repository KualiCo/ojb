/*
 * Created by IntelliJ IDEA.
 * User: Matt
 * Date: Jun 9, 2002
 * Time: 6:31:24 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.apache.ojb.broker;

import java.io.Serializable;

public class Table_2Object implements Serializable
{
    private int pk;

    public int getPk()
    {
        return pk;
    }

    public void setPk(int pk)
    {
        this.pk = pk;
    }
}
