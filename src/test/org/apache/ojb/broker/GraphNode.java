package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.Collection;
import java.util.Vector;

/**
 * GraphNode and GraphEdge model oriented graph with named nodes.
 * In this case there are two relations between two classes.
 * @author Oleg Nitz
 */
public class GraphNode implements Serializable
{
    private int id;
    private String name;
    private Collection outgoingEdges;
    private Collection incomingEdges;
    private int locationId;
    private Point location;

    public GraphNode()
    {
    }

    public GraphNode(int id, String name, int locationId) {
        this.id = id;
        this.name = name;
        this.locationId = locationId;
    }

    public GraphNode(String name)
    {
        this.name = name;
    }

    public void addOutgoingEdge(GraphEdge edge)
    {
        if (outgoingEdges == null)
        {
            outgoingEdges = new Vector();
        }
        outgoingEdges.add(edge);
    }

    public void addIncomingEdge(GraphEdge edge)
    {
        if (incomingEdges == null)
        {
            incomingEdges = new Vector();
        }
        incomingEdges.add(edge);
    }

    public Collection getOutgoingEdges()
    {
        return outgoingEdges;
    }

    public Collection getIncomingEdges()
    {
        return incomingEdges;
    }

    public String getName()
    {
        return name;
    }

    public int getLocationId()
    {
        return locationId;
    }

    public Point getLocation()
    {
        return location;
    }

    public int getId()
    {
        return id;
    }

    public void setOutgoingEdges(Collection edges)
    {
        outgoingEdges = edges;
    }

    public void setIncomingEdges(Collection edges)
    {
        incomingEdges = edges;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setLocationId(int locationId)
    {
        this.locationId = locationId;
    }

    public void setLocation(Point location)
    {
        this.location = location;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String toString()
    {
        return name + " " + outgoingEdges;
    }

}
