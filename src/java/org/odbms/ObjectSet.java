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
 * query resultset.
 * <br><br>The <code>ObjectSet</code> interface providedes iterator functions to
 * navigate through a set of objects retrieved by a query.
 */
public interface ObjectSet {


    /**
	 * returns <code>true</code> if the <code>ObjectSet</code> has more elements.
     * @return boolean <code>true</code> if the <code>ObjectSet</code> has more
	 * elements.
     */
    public boolean hasNext ();


    /**
	 * returns the next object in the <code>ObjectSet</code>.
     * @return the next object in the <code>ObjectSet</code>.
     */
    public Object next ();


    /**
	 * resets the <code>ObjectSet</code> cursor before the first element. <br><br>
	 * A subsequent call to <code>next()</code> will return the first element.
     */
    public void reset ();



    /**
	 * returns the number of elements in the <code>ObjectSet</code>.
     * @return the number of elements in the <code>ObjectSet</code>.
     */
    public int size ();
}



