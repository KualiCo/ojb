package org.odbms;

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
 * database engine interface.
 * <br><br>The <code>ObjectContainer</code> interface provides all functions
 * to store, retrieve and delete objects and to change object state.
 */
public interface ObjectContainer
{

    /**
     * factory method to create a new <a href="Query.html">
     * <code>Query</code></a> object.
     * @return a new Query object
     */
    public Query query();
}
