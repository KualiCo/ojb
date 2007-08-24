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

import org.apache.ojb.broker.util.ObjectModification;

/**
 * Represents the state of object.
 */
public abstract class State implements ObjectModification
{
    public static final State TRANSIENT = new Transient();

    public static final State PERSISTENT_CLEAN = new PersistentClean();

    public static final State PERSISTENT_DIRTY = new PersistentDirty();

    public static final State PERSISTENT_NEW = new PersistentNew();

    public static final State PERSISTENT_DELETED = new PersistentDeleted();

    public static final State PERSISTENT_NEW_DELETED = new PersistentNewDeleted();

    public static final State HOLLOW = new Hollow();

    //-------------- State transitions --------------------

    /**
     * Describes the state transition when object is gotten from the cache
     * or is loaded from database (once per transaction).
     */
    public State getObject()
            throws IllegalObjectStateException
    {
        throw new IllegalObjectStateException(this + " during getObject");
    }

    /**
     * Describes the state transition when user modifies object
     */
    public State markDirty()
            throws IllegalObjectStateException
    {
        throw new IllegalObjectStateException(this + " during markDirty()");
    }

    /**
     * Describes the state transition on makePersistent()
     */
    public State makePersistent()
            throws IllegalObjectStateException
    {
        throw new IllegalObjectStateException(this + " during makePersistent()");
    }

    /**
     * Describes the state transition on deletePersistent()
     */
    public State deletePersistent()
            throws IllegalObjectStateException
    {
        throw new IllegalObjectStateException(this + " during deletePersistent()");
    }

    /**
     * Describes the state transition on commit()
     */
    public State commit()
            throws IllegalObjectStateException
    {
        throw new IllegalObjectStateException(this + " during commit()");
    }

    /**
     * Describes the state transition on rollback()
     */
    public State rollback()
            throws IllegalObjectStateException
    {
        throw new IllegalObjectStateException(this + " during rollback()");
    }

    /**
     * Describes the state transition on refresh()
     */
    public State refresh()
            throws IllegalObjectStateException
    {
        return this;
    }


    //-------------- State semantics --------------------

    /**
     * returns true is this state requires INSERT
     */
    public boolean needsInsert()
    {
        return false;
    }

    /**
     * returns true is this state requires UPDATE
     */
    public boolean needsUpdate()
    {
        return false;
    }

    /**
     * returns true is this state requires DELETE
     */
    public boolean needsDelete()
    {
        return false;
    }

    /**
     * returns true is this state means that the object has been deleted
     */
    public boolean isDeleted()
    {
        return false;
    }

}
