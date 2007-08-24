package org.apache.ojb.otm.states;

/* Copyright 2003-2005 The Apache Software Foundation
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
 * this state represents persistent objects outside of transaction
 * (no ODMG equivalent).
 */
public class Hollow extends State
{

    Hollow()
    {
    }

    /**
     * return a String representation
     */
    public String toString()
    {
        return "Hollow";
    }

    //-------------- State transitions --------------------

    /**
     * Describes the state transition when object is gotten from the cache
     * or is loaded from database (once per transaction).
     */
    public State getObject()
            throws IllegalObjectStateException
    {
        return State.PERSISTENT_CLEAN;
    }

    /**
     * Describes the state transition when user modifies object
     */
    public State markDirty()
            throws IllegalObjectStateException
    {
        return State.PERSISTENT_DIRTY;
    }

    /**
     * Describes the state transition on deletePersistent()
     */
    public State deletePersistent()
            throws IllegalObjectStateException
    {
        return State.PERSISTENT_DELETED;
    }

    public State rollback()
            throws IllegalObjectStateException
    {
        return this;
    }
}
