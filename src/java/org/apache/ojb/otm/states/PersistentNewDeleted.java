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
 * this state represents new objects which have been mrked for deletion during tx
 * (ODMG StateNewDelete).
 */
public class PersistentNewDeleted extends State
{

    PersistentNewDeleted()
    {
    }

    /**
     * return a String representation
     */
    public String toString()
    {
        return "Persistent-new-deleted";
    }

    //-------------- State transitions --------------------

    /**
     * Describes the state transition on makePersistent()
     */
    public State makePersistent()
            throws IllegalObjectStateException
    {
        return State.PERSISTENT_NEW;
    }

    /**
     * Describes the state transition on deletePersistent()
     */
    public State deletePersistent()
            throws IllegalObjectStateException
    {
        return this;
    }

    /**
     * Describes the state transition on commit()
     */
    public State commit()
            throws IllegalObjectStateException
    {
        return State.TRANSIENT;
    }

    /**
     * Describes the state transition on rollback()
     */
    public State rollback()
            throws IllegalObjectStateException
    {
        return State.TRANSIENT;
    }

    public boolean isDeleted()
    {
        return true;
    }
}
