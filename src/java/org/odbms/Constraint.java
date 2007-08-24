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
 * constraint for a single query node.  
 * <br><br>A Constraint is associated with one single <code>Query</code> node 
 * - a single member of a class.<br><br>
 * Constraints are constructed by calling 
 * <a href="Query.html#constrain(java.lang.object)">
 * <code>Query.constrain()</code></a>.
 * <br><br>
 * Constraints can be joined with the methods and() and or().<br><br>
 * The following mutual exclusive functions set the evaluation mode.
 * The subsequent call prevails:<br>
 * identity(), equal(), greater(), greaterOrEqual(), smaller(), 
 * smallerOrEqual(), like(), contains()<br><br>
 * is(), and not() are also mutually exclusive.<br><br>
 */
public interface Constraint {

    /**
	 * links two Constraints for AND evaluation
     * @param andWith the other Constraint
     * @return a new Constraint, that can be used for further calls to and() and or()
     */
    public Constraint and (Constraint andWith);



    /**
	 * links two Constraints for OR evaluation
     * @param orWith the other Constraint
     * @return a new Constraint, that can be used for further calls to and() and or()
     */
    public Constraint or (Constraint orWith);



    /**
     * sets the evaluation mode to "=="
     * @return this Constraint to allow the chaining of method calls
     */
    public Constraint equal ();



    /**
     * sets the evaluation mode to ">"
     * @return this Constraint to allow the chaining of method calls
     */
    public Constraint greater ();



    /**
     * sets the evaluation mode to ">="
     * @return this Constraint to allow the chaining of method calls
     */
    public Constraint greaterOrEqual ();



    /**
     * sets the evaluation mode to "<"
     * @return this Constraint to allow the chaining of method calls
     */
    public Constraint smaller ();



    /**
     * sets the evaluation mode to "<="
     * @return this Constraint to allow the chaining of method calls
     */
    public Constraint smallerOrEqual ();


    /**
     * sets the evaluation mode to identity comparison
     * @return this Constraint to allow the chaining of method calls
     */
    public Constraint identity ();
	
	
    /**
     * sets the evaluation mode to "like" comparison
     * @return this Constraint to allow the chaining of method calls
     */
    public Constraint like ();
	
	
    /**
     * sets the evaluation mode to containment comparison
     * @return this Constraint to allow the chaining of method calls
     */
    public Constraint contains ();


    /**
     * turns off not() comparison
     * @return this Constraint to allow the chaining of method calls
     */
    public Constraint is ();



    /**
     * turns on not() comparison
     * @return this Constraint to allow the chaining of method calls
     */
    public Constraint not ();
}

