package  org.odbms;

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

/**
 * handle to a query graph and reference to a specific node.
 * <br><br>The query graph consists of multiple nodes, each representing a
 * class or a member of a class. The structure can be linked in
 * any way that the class model used allows. A <code>Query</code>
 * object references a single node of this graph. <br><br>The graph can
 * be traversed with the functions descendant() and parent()
 * which return references to other nodes of the graph. These two
 * functions will automatically extend the graph, if necessary. <br><br>
 * execute() evaluates the entire graph against the objects stored
 * in the data container. execute() can be called from any node of the
 * graph and will create an <a href="ObjectSet.html">
 * <code>ObjectSet</code></a> filled with objects of the object type
 * that the node, it was called from, represents. Objects for all
 * descendant nodes of the caller <code>Query</code> object will be instantiated.
 * Objects of parent nodes will not be visible in the <a href="ObjectSet.html">
 * <code>ObjectSet</code></a> if they are
 * not referenced from the caller <code>Query</code> object.
 */
public interface Query {


    /**
	 * adds a constraint to this node. <br>
	 * If the object parameter is deeper than the entire query graph,
	 * the query graph is extended accordingly.
     * @param example object for comparison
     * @return Constraint
     */
    public Constraint constrain (Object example);



    /**
	 * executes the query.
     * @return ObjectSet - the resultset of the Query
     */
    public ObjectSet execute ();



    /**
	 * returns a reference to a descendant node in the query graph.
	 * If the node does not exist, it will be created.
     * Path notation:<br>
     * <code>"[membername].[membername].[membername]"</code><br>
     * (any number of members)<br><br>
     * To request references to elements of multi-element objects like
     * arrays, lists, vectors, maps, hashMaps, ...:<br>
     * <code>"[membername].[membername].[membername]<b>.</b>"</code><br>
     * (Note the extra "." at the end.)<br>
     * @param path path to the descendant. "[membername].[membername]"
     * @return Query descendant node - the member node at the end of the
     * <code>path</code> specified.
     */
    public Query descendant (String path);


    /**
	 * returns a reference to a parent node in the query graph.
	 * If the node does not exist, it will be created.
     * Path notation:<br>
     * <code>"[classname].[membername].[membername]"</code>
	 * <br>where the last member is this Query node.
     * @param path to the parent node "[classname].[membername]"
     * @return Query parent node - the class node at the beginning of the
     * <code>path</code> specified.
     */
    public Query parent (String path);


    /**
	 * limits the maximum amount of objects returned.
	 * Especially for sorted queries, large performance advantages are
	 * possible.
     * @param count - the maximum amount of objects desired.
     * @return this Query to allow the chaining of method calls
     */
    public Query limitSize (int count);


    /**
	 * adds an ascending order criteria to this node of
	 * the query graph. In case of multiple calls to ordering
	 * methods, the query graph is ordered by all criteria in the
	 * order they were called.<br><br>
	 * @return this Query to allow the chaining of method calls
     */
    public Query orderAscending ();


    /**
	 * adds a descending order criteria to this node of
	 * the query graph. In case of multiple calls to ordering
	 * methods, the query graph is ordered by all criteria in the
	 * order they were called.<br><br>
	 * @return this Query to allow the chaining of method calls
     */
    public Query orderDescending ();


}

