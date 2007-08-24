package org.apache.ojb.odmg;

/* Copyright 2002-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.odmg.states.ModificationState;

/**
 * <p>Implements an algorithm for reordering the object envelopes of a pending
 * transaction to minimized the probability of foreign key constraint
 * violations.</p>
 * 
 * <p>The algorithm is based on a graph theoretical approach: Each object
 * envelope is represented by a vertex in a graph. Each possible constraint
 * on the order of database operations is represented by a directed edge
 * in this graph, in which the initial vertex represents the object envelope
 * to be sent to the database first and the terminal index represents the
 * object envelope that might cause a FK violation if the initial vertex
 * has not been sent to the database before.</p> 
 * 
 * <p>Additionally the edges in this graph are weighted. This is necessary
 * because the object envelopes provide only information on the relation
 * between objects <strong>after</strong> the transaction. FK violations, 
 * however, can also occur due to relations that existed <strong>before</strong>
 * the transaction. Therefore the algorithm also considers potential relations
 * between objects due to the fact that an object is of a class that is
 * the item class of a object or collection reference of another object.
 * Graph edges representing such potential relationships receive a lower
 * weight than edges representing concrete relationships that exist in the
 * current state of the object model.</p>
 * 
 * <p>Once all graph edges have been established, the algorithm proceeds as
 * follows:</p>
 * <ol>
 *  <li>Iterate through all vertices and sum up the weight of all incoming
 *      edges (i.e. those edges whose terminal vertex is the current vertex).</li>
 *  <li>Find the minimum value of this weight sums (ideally this minimum is zero,
 *      meaning that there are object envelopes that can be sent to the 
 *      database without risking FK violations)</li>
 *  <li>Add all vertices with a weight sum that is equal to this minimum to the
 *      reordered sequence of object envelopes and remove the vertices
 *      and all connected edges from the graph.</li>
 *  <li>If there are vertices left, repeat steps (1) through (3), otherwise
 *      we are done.
 * </ol>
 *
 * @author  Gerhard Grosse
 * @version $Id: ObjectEnvelopeOrdering.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 * @since   Nov 15, 2004
 */
class ObjectEnvelopeOrdering
{
    private static final int CONCRETE_EDGE_WEIGHT = 3;
    private static final int CONCRETE_EDGE_WEIGHT_WITH_FK = 4;
    private static final int POTENTIAL_EDGE_WEIGHT = 1;
    private static final int POTENTIAL_EDGE_WEIGHT_WITH_FK = 2;
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private static Logger log = LoggerFactory.getLogger(ObjectEnvelopeOrdering.class);

    private List originalOrder;
    private Map envelopes;

    private Vertex[] vertices;
    private List edgeList;

    private Identity[] newOrder;

    /**
     * Creates an object envelope ordering based on an original ordering
     * of Identity objects and an Identity-&gt;ObjectEnvelope map
     * @param originalOrder a list of Identity objects
     * @param envelopes a map with ObjectEnvelope-s with their respective
     *      Identity-s as key
     */
    public ObjectEnvelopeOrdering(List originalOrder, Map envelopes)
    {
        this.originalOrder = originalOrder;
        this.envelopes = envelopes;
    }

    /**
     * Reorders the object envelopes. The new order is available from the
     * <code>ordering</code> property.
     * @see #getOrdering()
     */
    public void reorder()
    {
        int newOrderIndex = 0;
        long t1 = 0, t2 = 0, t3;

        if (log.isDebugEnabled())
        {
            t1 = System.currentTimeMillis();
        }
        newOrder = new Identity[originalOrder.size()];

        if(log.isDebugEnabled()) log.debug("Orginal order: " + originalOrder);
        // set up the vertex array in the order the envelopes were added
        List vertexList = new ArrayList(originalOrder.size());
        // int vertexIndex = 0;
        for (Iterator it = originalOrder.iterator(); it.hasNext();)
        {
            ObjectEnvelope envelope = (ObjectEnvelope) envelopes.get(it.next());
            if (envelope.needsUpdate() || envelope.needsInsert() || envelope.needsDelete())
            {
                Vertex vertex = new Vertex(envelope);
                vertexList.add(vertex);
                if (log.isDebugEnabled())
                {
                    log.debug("Add new Vertex object "+envelope.getIdentity()+" to VertexList");
                }
            }
            else
            {
                // envelope is clean - just add identity to new order
                newOrder[newOrderIndex++] = envelope.getIdentity();
                if (log.isDebugEnabled())
                {
                    log.debug("Add unmodified object "+envelope.getIdentity()+" to new OrderList");
                }
            }
        }
        vertices = (Vertex[]) vertexList.toArray(new Vertex[vertexList.size()]);

        // set up the edges
        edgeList = new ArrayList(2 * vertices.length);
        for (int i = 0; i < vertices.length; i++)
        {
            addEdgesForVertex(vertices[i]);
        }

        if (log.isDebugEnabled())
        {
            t2 = System.currentTimeMillis();
            log.debug("Building object envelope graph took " + (t2 - t1) + " ms");
            log.debug("Object envelope graph contains " + vertices.length + " vertices" + " and " + edgeList.size()
                    + " edges");
        }

        int remainingVertices = vertices.length;
        int iterationCount = 0;
        while (remainingVertices > 0)
        {
            // update iteration count
            iterationCount++;

            // update incoming edge counts
            for (Iterator it = edgeList.iterator(); it.hasNext();)
            {
                Edge edge = (Edge) it.next();
                if (!edge.isProcessed())
                {
                    if(log.isDebugEnabled())
                    {
                        final String msg = "Add weight '"+edge.getWeight()+"' for terminal vertex " + edge.getTerminalVertex() + " of edge " + edge;
                        log.debug(msg);
                    }
                    edge.getTerminalVertex().incrementIncomingEdgeWeight(edge.getWeight());
                }
            }

            // find minimum weight of incoming edges of a vertex
            int minIncomingEdgeWeight = Integer.MAX_VALUE;
            for (int i = 0; i < vertices.length; i++)
            {
                Vertex vertex = vertices[i];
                if (!vertex.isProcessed() && minIncomingEdgeWeight > vertex.getIncomingEdgeWeight())
                {
                    minIncomingEdgeWeight = vertex.getIncomingEdgeWeight();
                    if (minIncomingEdgeWeight == 0)
                    {
                        // we won't get any lower
                        break;
                    }
                }
            }

            // process vertices having minimum incoming edge weight
            int processCount = 0;
            for (int i = 0; i < vertices.length; i++)
            {
                Vertex vertex = vertices[i];
                if (!vertex.isProcessed() && vertex.getIncomingEdgeWeight() == minIncomingEdgeWeight)
                {
                    newOrder[newOrderIndex++] = vertex.getEnvelope().getIdentity();
                    vertex.markProcessed();
                    processCount++;
                    if (log.isDebugEnabled())
                    {
                        log.debug("add minimum edge weight - "+minIncomingEdgeWeight
                                + ", newOrderList: " + ArrayUtils.toString(newOrder));
                    }
                }
                vertex.resetIncomingEdgeWeight();
            }

            if (log.isDebugEnabled())
            {
                log.debug("Processed " + processCount + " of " + remainingVertices
                        + " remaining vertices in iteration #" + iterationCount);
            }
            remainingVertices -= processCount;
        }

        if (log.isDebugEnabled())
        {
            t3 = System.currentTimeMillis();
            log.debug("New ordering: " + ArrayUtils.toString(newOrder));
            log.debug("Processing object envelope graph took " + (t3 - t2) + " ms");
        }

    }

    /**
     * Gets the reordered sequence of object envelopes
     * @return an array of Identity objects representing the opimized sequence
     *      of database operations
     */
    public Identity[] getOrdering()
    {
        if (newOrder == null)
        {
            reorder();
        }
        return newOrder;
    }

    /**
     * Adds all edges for a given object envelope vertex. All edges are
     * added to the edgeList map.
     * @param vertex the Vertex object to find edges for
     */
    private void addEdgesForVertex(Vertex vertex)
    {
        ClassDescriptor cld = vertex.getEnvelope().getClassDescriptor();
        Iterator rdsIter = cld.getObjectReferenceDescriptors(true).iterator();
        while (rdsIter.hasNext())
        {
            ObjectReferenceDescriptor rds = (ObjectReferenceDescriptor) rdsIter.next();
            addObjectReferenceEdges(vertex, rds);
        }
        Iterator cdsIter = cld.getCollectionDescriptors(true).iterator();
        while (cdsIter.hasNext())
        {
            CollectionDescriptor cds = (CollectionDescriptor) cdsIter.next();
            addCollectionEdges(vertex, cds);
        }
    }

    /**
     * Finds edges based to a specific object reference descriptor and
     * adds them to the edge map.
     * @param vertex the object envelope vertex holding the object reference
     * @param rds the object reference descriptor
     */
    private void addObjectReferenceEdges(Vertex vertex, ObjectReferenceDescriptor rds)
    {
        Object refObject = rds.getPersistentField().get(vertex.getEnvelope().getRealObject());
        Class refClass = rds.getItemClass();
        for (int i = 0; i < vertices.length; i++)
        {
            Edge edge = null;
            // ObjectEnvelope envelope = vertex.getEnvelope();
            Vertex refVertex = vertices[i];
            ObjectEnvelope refEnvelope = refVertex.getEnvelope();
            if (refObject == refEnvelope.getRealObject())
            {
                edge = buildConcrete11Edge(vertex, refVertex, rds.hasConstraint());
            }
            else if (refClass.isInstance(refVertex.getEnvelope().getRealObject()))
            {
                edge = buildPotential11Edge(vertex, refVertex, rds.hasConstraint());
            }
            if (edge != null)
            {
                if (!edgeList.contains(edge))
                {
                    edgeList.add(edge);
                }
                else
                {
                    edge.increaseWeightTo(edge.getWeight());
                }
            }
        }
    }

    /**
     * Finds edges base on a specific collection descriptor (1:n and m:n)
     * and adds them to the edge map.
     * @param vertex the object envelope vertex holding the collection
     * @param cds the collection descriptor
     */
    private void addCollectionEdges(Vertex vertex, CollectionDescriptor cds)
    {
        ObjectEnvelope envelope = vertex.getEnvelope();
        Object col = cds.getPersistentField().get(envelope.getRealObject());
        Object[] refObjects;
        if (col == null || (ProxyHelper.isCollectionProxy(col) && !ProxyHelper.getCollectionProxy(col).isLoaded()))
        {
            refObjects = EMPTY_OBJECT_ARRAY;
        }
        else
        {
            refObjects = BrokerHelper.getCollectionArray(col);
        }
        Class refClass = cds.getItemClass();

        for (int i = 0; i < vertices.length; i++)
        {
            Edge edge = null;
            Vertex refVertex = vertices[i];
            ObjectEnvelope refEnvelope = refVertex.getEnvelope();

            if (refClass.isInstance(refEnvelope.getRealObject()))
            {
                if (containsObject(refEnvelope.getRealObject(), refObjects))
                {
                    if (cds.isMtoNRelation())
                    {
                        edge = buildConcreteMNEdge(vertex, refVertex);
                    }
                    else
                    {
                        edge = buildConcrete1NEdge(vertex, refVertex);
                    }
                }
                else
                {
                    if (cds.isMtoNRelation())
                    {
                        edge = buildPotentialMNEdge(vertex, refVertex);
                    }
                    else
                    {
                        edge = buildPotential1NEdge(vertex, refVertex);
                    }
                }
            }
            if (edge != null)
            {
                if (!edgeList.contains(edge))
                {
                    edgeList.add(edge);
                }
                else
                {
                    edge.increaseWeightTo(edge.getWeight());
                }
            }
        }
    }

    /**
     * Helper method that searches an object array for the occurence of a
     * specific object based on reference equality
     * @param searchFor the object to search for
     * @param searchIn the array to search in
     * @return true if the object is found, otherwise false
     */
    private static boolean containsObject(Object searchFor, Object[] searchIn)
    {
        for (int i = 0; i < searchIn.length; i++)
        {
            if (searchFor == searchIn[i])
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the database operations associated with two object envelopes
     * that are related via an 1:1 (or n:1) reference needs to be performed 
     * in a particular order and if so builds and returns a corresponding 
     * directed edge weighted with <code>CONCRETE_EDGE_WEIGHT</code>.
     * The following cases are considered (* means object needs update, + means
     * object needs insert, - means object needs to be deleted):
     * <table>
     *  <tr><td>(1)* -(1:1)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)* -(1:1)-&gt; (2)+</td><td>(2)-&gt;(1) edge</td></tr>
     *  <tr><td>(1)* -(1:1)-&gt; (2)-</td><td>no edge (cannot occur)</td></tr>
     *  <tr><td>(1)+ -(1:1)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)+ -(1:1)-&gt; (2)+</td><td>(2)-&gt;(1) edge</td></tr>
     *  <tr><td>(1)+ -(1:1)-&gt; (2)-</td><td>no edge (cannot occur)</td></tr>
     *  <tr><td>(1)- -(1:1)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)- -(1:1)-&gt; (2)+</td><td>no edge</td></tr>
     *  <tr><td>(1)- -(1:1)-&gt; (2)-</td><td>(1)-&gt;(2) edge</td></tr>
     * <table>
     * @param vertex1 object envelope vertex of the object holding the reference
     * @param vertex2 object envelope vertex of the referenced object
     * @return an Edge object or null if the two database operations can
     *      be performed in any order
     */
    protected Edge buildConcrete11Edge(Vertex vertex1, Vertex vertex2, boolean fkToRef)
    {
        ModificationState state1 = vertex1.getEnvelope().getModificationState();
        ModificationState state2 = vertex2.getEnvelope().getModificationState();
        if (state1.needsUpdate() || state1.needsInsert())
        {
            if (state2.needsInsert())
            {
                // (2) must be inserted before (1) can point to it
                return new Edge(vertex2, vertex1, fkToRef ? CONCRETE_EDGE_WEIGHT_WITH_FK : CONCRETE_EDGE_WEIGHT);
            }
        }
        else if (state1.needsDelete())
        {
            if (state2.needsDelete())
            {
                // (1) points to (2) and must be deleted first 
                return new Edge(vertex1, vertex2, fkToRef ? CONCRETE_EDGE_WEIGHT_WITH_FK : CONCRETE_EDGE_WEIGHT);
            }
        }
        return null;
    }

    /**
     * Checks if the database operations associated with two object envelopes
     * that might have been related via an 1:1 (or n:1) reference before
     * the current transaction needs to be performed 
     * in a particular order and if so builds and returns a corresponding 
     * directed edge weighted with <code>POTENTIAL_EDGE_WEIGHT</code>. 
     * The following cases are considered (* means object needs update, + means
     * object needs insert, - means object needs to be deleted):
     * <table>
     *  <tr><td>(1)* -(1:1)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)* -(1:1)-&gt; (2)+</td><td>no edge</td></tr>
     *  <tr><td>(1)* -(1:1)-&gt; (2)-</td><td>(1)-&gt;(2) edge</td></tr>
     *  <tr><td>(1)+ -(1:1)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)+ -(1:1)-&gt; (2)+</td><td>no edge</td></tr>
     *  <tr><td>(1)+ -(1:1)-&gt; (2)-</td><td>no edge</td></tr>
     *  <tr><td>(1)- -(1:1)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)- -(1:1)-&gt; (2)+</td><td>no edge</td></tr>
     *  <tr><td>(1)- -(1:1)-&gt; (2)-</td><td>(1)-&gt;(2) edge</td></tr>
     * <table>
     * @param vertex1 object envelope vertex of the object that might have 
     *      hold the reference
     * @param vertex2 object envelope vertex of the potentially referenced 
     *      object
     * @return an Edge object or null if the two database operations can
     *      be performed in any order
     */
    protected Edge buildPotential11Edge(Vertex vertex1, Vertex vertex2, boolean fkToRef)
    {
        ModificationState state1 = vertex1.getEnvelope().getModificationState();
        ModificationState state2 = vertex2.getEnvelope().getModificationState();
        if (state1.needsUpdate() || state1.needsDelete())
        {
            if (state2.needsDelete())
            {
                // old version of (1) might point to (2)
                return new Edge(vertex1, vertex2, fkToRef ? POTENTIAL_EDGE_WEIGHT_WITH_FK : POTENTIAL_EDGE_WEIGHT);
            }
        }
        return null;
    }

    /**
     * Checks if the database operations associated with two object envelopes
     * that are related via an 1:n collection reference needs to be performed 
     * in a particular order and if so builds and returns a corresponding 
     * directed edge weighted with <code>CONCRETE_EDGE_WEIGHT</code>.
     * The following cases are considered (* means object needs update, + means
     * object needs insert, - means object needs to be deleted):
     * <table>
     *  <tr><td>(1)* -(1:n)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)* -(1:n)-&gt; (2)+</td><td>no edge</td></tr>
     *  <tr><td>(1)* -(1:n)-&gt; (2)-</td><td>no edge</td></tr>
     *  <tr><td>(1)+ -(1:n)-&gt; (2)*</td><td>(1)-&gt;(2) edge</td></tr>
     *  <tr><td>(1)+ -(1:n)-&gt; (2)+</td><td>(1)-&gt;(2) edge</td></tr>
     *  <tr><td>(1)+ -(1:n)-&gt; (2)-</td><td>no edge (cannot occur)</td></tr>
     *  <tr><td>(1)- -(1:n)-&gt; (2)*</td><td>(2)-&gt;(1) edge</td></tr>
     *  <tr><td>(1)- -(1:n)-&gt; (2)+</td><td>no edge</td></tr>
     *  <tr><td>(1)- -(1:n)-&gt; (2)-</td><td>(2)-&gt;(1) edge</td></tr>
     * <table>
     * @param vertex1 object envelope vertex of the object holding the 
     *      collection
     * @param vertex2 object envelope vertex of the object contained in the 
     *      collection
     * @return an Edge object or null if the two database operations can
     *      be performed in any order
     */
    protected Edge buildConcrete1NEdge(Vertex vertex1, Vertex vertex2)
    {
        ModificationState state1 = vertex1.getEnvelope().getModificationState();
        ModificationState state2 = vertex2.getEnvelope().getModificationState();
        if (state1.needsInsert())
        {
            if (state2.needsUpdate() || state2.needsInsert())
            {
                // (2) now contains an FK to (1) thus (1) must be inserted first
                return new Edge(vertex1, vertex2, CONCRETE_EDGE_WEIGHT);
            }
        }
        else if (state1.needsDelete())
        {
            if (state2.needsUpdate() || state2.needsDelete())
            {
                // Before deleting (1) give (2) a chance to drop its FK to it 
                return new Edge(vertex2, vertex1, CONCRETE_EDGE_WEIGHT);
            }
        }
        return null;
    }

    /**
     * Checks if the database operations associated with two object envelopes
     * that are might have been related via an 1:n collection reference before
     * the current transaction needs to be performed 
     * in a particular order and if so builds and returns a corresponding 
     * directed edge weighted with <code>POTENTIAL_EDGE_WEIGHT</code>.
     * The following cases are considered (* means object needs update, + means
     * object needs insert, - means object needs to be deleted):
     * <table>
     *  <tr><td>(1)* -(1:n)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)* -(1:n)-&gt; (2)+</td><td>no edge</td></tr>
     *  <tr><td>(1)* -(1:n)-&gt; (2)-</td><td>no edge</td></tr>
     *  <tr><td>(1)+ -(1:n)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)+ -(1:n)-&gt; (2)+</td><td>no edge</td></tr>
     *  <tr><td>(1)+ -(1:n)-&gt; (2)-</td><td>no edge</td></tr>
     *  <tr><td>(1)- -(1:n)-&gt; (2)*</td><td>(2)-&gt;(1) edge</td></tr>
     *  <tr><td>(1)- -(1:n)-&gt; (2)+</td><td>no edge</td></tr>
     *  <tr><td>(1)- -(1:n)-&gt; (2)-</td><td>(2)-&gt;(1) edge</td></tr>
     * <table>
     * @param vertex1 object envelope vertex of the object holding the 
     *      collection
     * @param vertex2 object envelope vertex of the object that might have
     *      been contained in the collection
     * @return an Edge object or null if the two database operations can
     *      be performed in any order
     */
    protected Edge buildPotential1NEdge(Vertex vertex1, Vertex vertex2)
    {
        ModificationState state1 = vertex1.getEnvelope().getModificationState();
        ModificationState state2 = vertex2.getEnvelope().getModificationState();
        if (state1.needsDelete())
        {
            if (state2.needsUpdate() || state2.needsDelete())
            {
                // Before deleting (1) give potential previous collection 
                // members a chance to drop their FKs to it 
                return new Edge(vertex2, vertex1, POTENTIAL_EDGE_WEIGHT);
            }
        }
        return null;
    }

    /**
     * Checks if the database operations associated with two object envelopes
     * that are related via an m:n collection reference needs to be performed 
     * in a particular order and if so builds and returns a corresponding 
     * directed edge weighted with <code>CONCRETE_EDGE_WEIGHT</code>.
     * The following cases are considered (* means object needs update, + means
     * object needs insert, - means object needs to be deleted):
     * <table>
     *  <tr><td>(1)* -(m:n)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)* -(m:n)-&gt; (2)+</td><td>(2)-&gt;(1) edge</td></tr>
     *  <tr><td>(1)* -(m:n)-&gt; (2)-</td><td>no edge (cannot occur)</td></tr>
     *  <tr><td>(1)+ -(m:n)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)+ -(m:n)-&gt; (2)+</td><td>(2)-&gt;(1) edge</td></tr>
     *  <tr><td>(1)+ -(m:n)-&gt; (2)-</td><td>no edge (cannot occur)</td></tr>
     *  <tr><td>(1)- -(m:n)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)- -(m:n)-&gt; (2)+</td><td>no edge</td></tr>
     *  <tr><td>(1)- -(m:n)-&gt; (2)-</td><td>(1)-&gt;(2) edge</td></tr>
     * <table>
     * @param vertex1 object envelope vertex of the object holding the 
     *      collection
     * @param vertex2 object envelope vertex of the object contained in the 
     *      collection
     * @return an Edge object or null if the two database operations can
     *      be performed in any order
     */
    protected Edge buildConcreteMNEdge(Vertex vertex1, Vertex vertex2)
    {
        ModificationState state1 = vertex1.getEnvelope().getModificationState();
        ModificationState state2 = vertex2.getEnvelope().getModificationState();
        if (state1.needsUpdate() || state1.needsInsert())
        {
            if (state2.needsInsert())
            {
                // (2) must be inserted before we can create a link to it
                return new Edge(vertex2, vertex1, CONCRETE_EDGE_WEIGHT);
            }
        }
        else if (state1.needsDelete())
        {
            if (state2.needsDelete())
            {
                // there is a link from (1) to (2) which must be deleted first,
                // which will happen when deleting (1) - thus:
                return new Edge(vertex1, vertex2, POTENTIAL_EDGE_WEIGHT);
            }
        }
        return null;
    }

    /**
     * Checks if the database operations associated with two object envelopes
     * that might have been related via an m:n collection reference before
     * the current transaction needs to be performed 
     * in a particular order and if so builds and returns a corresponding 
     * directed edge weighted with <code>POTENTIAL_EDGE_WEIGHT</code>.
     * The following cases are considered (* means object needs update, + means
     * object needs insert, - means object needs to be deleted):
     * <table>
     *  <tr><td>(1)* -(m:n)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)* -(m:n)-&gt; (2)+</td><td>no edge</td></tr>
     *  <tr><td>(1)* -(m:n)-&gt; (2)-</td><td>(1)-&gt;(2) edge</td></tr>
     *  <tr><td>(1)+ -(m:n)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)+ -(m:n)-&gt; (2)+</td><td>no edge</td></tr>
     *  <tr><td>(1)+ -(m:n)-&gt; (2)-</td><td>no edge</td></tr>
     *  <tr><td>(1)- -(m:n)-&gt; (2)*</td><td>no edge</td></tr>
     *  <tr><td>(1)- -(m:n)-&gt; (2)+</td><td>no edge</td></tr>
     *  <tr><td>(1)- -(m:n)-&gt; (2)-</td><td>(1)-&gt;(2) edge</td></tr>
     * <table>
     * @param vertex1 object envelope vertex of the object holding the 
     *      collection
     * @param vertex2 object envelope vertex of the object that might have
     *      been contained in the collection
     * @return an Edge object or null if the two database operations can
     *      be performed in any order
     */
    protected Edge buildPotentialMNEdge(Vertex vertex1, Vertex vertex2)
    {
        ModificationState state1 = vertex1.getEnvelope().getModificationState();
        ModificationState state2 = vertex2.getEnvelope().getModificationState();
        if (state1.needsUpdate() || state1.needsDelete())
        {
            if (state2.needsDelete())
            {
                // old version of (1) might comprise a link to (2)
                return new Edge(vertex1, vertex2, POTENTIAL_EDGE_WEIGHT);
            }
        }
        return null;
    }

    /**
     * Represents an edge in the object envelope graph
     */
    private static class Edge
    {
        private Vertex initial;
        private Vertex terminal;
        private Identity initialIdentity;
        private Identity terminalIdentity;
        private int weight;
        private boolean knownToBeProcessed;
        private int hashCode;

        public Edge(Vertex initial, Vertex terminal, int weight)
        {
            this.initial = initial;
            this.terminal = terminal;
            this.initialIdentity = initial.getEnvelope().getIdentity();
            this.terminalIdentity = terminal.getEnvelope().getIdentity();
            this.weight = weight;
            this.knownToBeProcessed = false;
            this.hashCode = initialIdentity.hashCode() + 13 * terminalIdentity.hashCode();
        }

        public Vertex getInitialVertex()
        {
            return initial;
        }

        public Vertex getTerminalVertex()
        {
            return terminal;
        }

        public boolean connects(Vertex vertex)
        {
            return initial == vertex || terminal == vertex;
        }

        public void increaseWeightTo(int minWeight)
        {
            if (weight < minWeight)
            {
                weight = minWeight;
            }
        }

        public int getWeight()
        {
            return weight;
        }

        public boolean isProcessed()
        {
            if (knownToBeProcessed)
            {
                return true;
            }
            else
            {
                boolean processed = initial.isProcessed() || terminal.isProcessed();
                if (processed)
                {
                    knownToBeProcessed = true;
                }
                return processed;
            }
        }

        public boolean equals(Object obj)
        {
            if (obj instanceof Edge)
            {
                Edge other = (Edge) obj;
                return this.initialIdentity.equals(other.initialIdentity)
                        && this.terminalIdentity.equals(other.terminalIdentity);
            }
            else
            {
                return false;
            }
        }

        public int hashCode()
        {
            return hashCode;
        }

        public String toString()
        {
            return new ToStringBuilder(this)
                    .append("initial", initialIdentity)
                    .append("terminal", terminalIdentity)
                    .append("weight", weight)
                    .append("processed", knownToBeProcessed)
                    .toString();
        }
    }

    /**
     * Represents a vertex in the object envelope graph
     */
    private static class Vertex
    {
        private ObjectEnvelope envelope;
        private boolean processed;
        private int incomingEdgeWeight;

        public Vertex(ObjectEnvelope envelope)
        {
            this.envelope = envelope;
            this.incomingEdgeWeight = 0;
            this.processed = false;
        }

        public ObjectEnvelope getEnvelope()
        {
            return envelope;
        }

        public void markProcessed()
        {
            processed = true;
        }

        public boolean isProcessed()
        {
            return processed;
        }

        public void resetIncomingEdgeWeight()
        {
            incomingEdgeWeight = 0;
        }

        public void incrementIncomingEdgeWeight(int weight)
        {
            incomingEdgeWeight += weight;
        }

        public int getIncomingEdgeWeight()
        {
            return incomingEdgeWeight;
        }

        public String toString()
        {
            return new ToStringBuilder(this)
                    .append("identity", envelope.getIdentity())
                    .append("processed", processed)
                    .append("incomingEdgeWeight", incomingEdgeWeight)
                    .toString();
        }
    }

}