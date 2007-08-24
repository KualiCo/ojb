package org.apache.ojb.odmg.states;

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

import java.io.Serializable;

import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.odmg.ObjectEnvelope;

/**
 * Describes an objects transactional state regarding commiting and rollbacking
 */
public abstract class ModificationState implements Serializable
{
	static final long serialVersionUID = 4182870857709997816L;
    public ModificationState()
    {
    }

    /**
     * return resulting state after marking clean
     */
    public abstract ModificationState markClean();

    /**
     * return resulting state after marking delete
     */
    public abstract ModificationState markDelete();

    /**
     * return resulting state after marking dirty
     */
    public abstract ModificationState markDirty();

    /**
     * return resulting state after marking new
     */
    public abstract ModificationState markNew();

    /**
     * return resulting state after marking old
     */
    public abstract ModificationState markOld();

    /**
     *
     */
    public abstract void checkpoint(ObjectEnvelope mod)
            throws PersistenceBrokerException;

    /**
     *
     */
    public abstract void commit(ObjectEnvelope mod)
            throws PersistenceBrokerException;

    /**
     *
     */
    public abstract void rollback(ObjectEnvelope mod);

    /**
     * return a String representation
     * @return java.lang.String
     */
    public String toString()
    {
        return this.getClass().getName();
    }


    /**
     * returns true is this state requires INSERT
     * @return boolean
     */
    public boolean needsInsert()
    {
        return false;
    }

    /**
     * returns true is this state requires UPDATE
     * @return boolean
     */
    public boolean needsUpdate()
    {
        return false;
    }

    /**
     * returns true is this state requires DELETE
     * @return boolean
     */
    public boolean needsDelete()
    {
        return false;
    }

    public boolean isTransient()
    {
        return false;
    }
}
