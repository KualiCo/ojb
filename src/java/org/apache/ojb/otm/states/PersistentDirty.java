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
 * this state represents persistent objects which have been altered 
 * during tx (ODMG StateOldDirty).
 */
public class PersistentDirty extends State
{

    PersistentDirty()
    {
    }

    /**
     * return a String representation
     */
    public String toString()
    {
        return "Persistent-dirty";
    }

    //-------------- State transitions --------------------

    /**
     * Describes the state transition when user modifies object
     */
    public State markDirty()
            throws IllegalObjectStateException
    {
        return this;
    }

    /**
     * Describes the state transition on makePersistent()
     */
    public State makePersistent()
            throws IllegalObjectStateException
    {
        return this;
    }

    /**
     * Describes the state transition on deletePersistent()
     */
    public State deletePersistent()
            throws IllegalObjectStateException
    {
        return State.PERSISTENT_DELETED;
    }

    /**
     * Describes the state transition on commit()
     */
    public State commit()
            throws IllegalObjectStateException
    {
        return State.HOLLOW;
    }

    /**
     * Describes the state transition on rollback()
     */
    public State rollback()
            throws IllegalObjectStateException
    {
        return State.HOLLOW;
    }

    public State refresh()
            throws IllegalObjectStateException
    {
        return State.PERSISTENT_CLEAN;
    }

    //-------------- State semantics --------------------

    /**
     * returns true is this state requires UPDATE
     */
    public boolean needsUpdate()
    {
        return true;
    }

}
