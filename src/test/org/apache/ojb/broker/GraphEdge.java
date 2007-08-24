package org.apache.ojb.broker;


/**
 * GraphNode and GraphEdge model oriented graph with named nodes.
 * In this case there are two relations between two classes.
 * @author Oleg Nitz
 */
public class GraphEdge implements java.io.Serializable
{
    private int id;
    private int sourceId;
    private int sinkId;
    private GraphNode source;
    private GraphNode sink;

    public GraphEdge()
    {
    }

    public GraphEdge(int id, int sourceId, int sinkId)
    {
        this.id = id;
        this.sourceId = sourceId;
        this.sinkId = sinkId;
    }

    public GraphEdge(GraphNode source, GraphNode sink)
    {
        source.addOutgoingEdge(this);
        sink.addIncomingEdge(this);
        
        // how could it ever work without this:
        this.source = source;
        this.sourceId = source.getId();
        this.sink = sink;
        this.sinkId = sink.getId();
        
    }

    public int getSourceId()
    {
        return sourceId;
    }

    public int getSinkId()
    {
        return sinkId;
    }

    public GraphNode getSource()
    {
        return source;
    }

    public GraphNode getSink()
    {
        return sink;
    }

    public int getId()
    {
        return id;
    }

    public void setSourceId(int sourceId)
    {
        this.sourceId = sourceId;
    }

    public void setSinkId(int sinkId)
    {
        this.sinkId = sinkId;
    }

    public void setSource(GraphNode source)
    {
        this.source = source;
    }

    public void setSink(GraphNode sink)
    {
        this.sink = sink;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String toString()
    {
        return "(" + (source == null ? "null" : source.getName()) + " -> "
                + (sink == null ? "null" : (sink == source ? sink.getName() : sink.toString())) + ")";
    }
}
