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

import org.apache.ojb.odmg.ObjectEnvelope;
import org.apache.ojb.broker.PersistenceBrokerException;

/**
 * this state represents objects which are not persisted and no longer used.
 */
public class StateTransient extends ModificationState
{
    private static StateTransient _instance = new StateTransient();

    /**
     * private constructor: we use singleton instances
     */
    private StateTransient()
    {
    }

    /**
     * perform a checkpoint, i.e. perform updates on underlying db but keep locks on objects
     */
    public static StateTransient getInstance()
    {
        return _instance;
    }

    /**
     * return resulting state after marking clean
     */
    public ModificationState markClean()
    {
        return this;
    }

    /**
     * return resulting state after marking delete
     */
    public ModificationState markDelete()
    {
        return this;
    }

    /**
     * return resulting state after marking dirty
     */
    public ModificationState markDirty()
    {
        return this;
    }

    /**
     * return resulting state after marking new
     */
    public ModificationState markNew()
    {
        return StateNewClean.getInstance();
    }

    /**
     * return resulting state after marking old
     */
    public ModificationState markOld()
    {
        return this;
    }

    /**
     * object is new, thus we need an INSERT to store it
     */
    public boolean needsInsert()
    {
        return false;
    }

    /**
     * checkpoint the transaction
     */
    public void checkpoint(ObjectEnvelope mod) throws PersistenceBrokerException
    {
    }


    /**
     * commit the associated transaction
     */
    public void commit(ObjectEnvelope mod) throws PersistenceBrokerException
    {
    }

    /**
     * rollback the transaction
     */
    public void rollback(ObjectEnvelope mod)
    {
    }

    public boolean isTransient()
    {
        return true;
    }
}
