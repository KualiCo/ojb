package org.odmg;

/* Copyright 2004-2005 The Apache Software Foundation
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
 * The interface that defines the operations of an ODMG array.
 * Nearly all of its operations are defined by the JavaSoft <code>List</code> interface.
 * All of the operations defined by the JavaSoft <code>List</code>
 * interface are supported by an ODMG implementation of <code>DArray</code>,
 * the exception <code>UnsupportedOperationException</code> is not thrown when a
 * call is made to any of the <code>List</code> methods.
 * An instance of a class implementing this interface can be obtained
 * by calling the method <code>Implementation.newDArray</code>.
 * @author	David Jordan (as Java Editor of the Object Data Management Group)
 * @version ODMG 3.0
 */
// @see java.lang.UnsupportedOperationException

public interface DArray extends org.odmg.DCollection, java.util.List
{
    /**
     * Resize the array to have <code>newSize</code> elements.
     * @param	newSize	The new size of the array.
     */
    public void resize(int newSize);
}
