package org.apache.ojb.broker;


/**
 * Point serves for graphic representation of graphs.
 * @author Oleg Nitz
 */
public class Point implements java.io.Serializable
{
    private int id;
    private int x;
    private int y;

    public Point()
    {
    }

    public Point(int id, int x, int y)
    {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public Point(int x, int y)
    {
        this.x = x;
        this.y = y;
    }


    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public int getId()
    {
        return id;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public void setY(int y)
    {
        this.y = y;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String toString()
    {
        return "(" + x + "," + y + ")";
    }
}
