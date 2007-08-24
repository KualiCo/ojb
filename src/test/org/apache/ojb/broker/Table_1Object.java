/*
 * Created by IntelliJ IDEA.
 * User: Matt
 * Date: Jun 9, 2002
 * Time: 6:31:12 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.apache.ojb.broker;

import java.io.Serializable;

public class Table_1Object implements Serializable
{
    private int pk;
    private Table_2Object table2Object;
    private Integer table2ObjectFK;

    public int getPk()
    {
        return pk;
    }

    public void setPk(int pk)
    {
        this.pk = pk;
    }

    public Table_2Object getTable2Object()
    {
        return table2Object;
    }

    public void setTable2Object(Table_2Object table2Object)
    {
        this.table2Object = table2Object;
    }

    public Integer getTable2ObjectFK()
    {
        return table2ObjectFK;
    }

    public void setTable2ObjectFK(Integer table2ObjectFK)
    {
        this.table2ObjectFK = table2ObjectFK;
    }
}
