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

import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.odmg.ObjectEnvelope;

/**
 * this state represents old objects which have been marked for deletion during tx.
 */
public class StateOldDelete extends ModificationState
{
    private static StateOldDelete _instance = new StateOldDelete();

    /**
     * private constructor: use singleton instance
     */
    private StateOldDelete()
    {
    }

    /**
     * perform a checkpoint, i.e. perform updates on underlying db but keep locks on objects
     */
    public static StateOldDelete getInstance()
    {
        return _instance;
    }

    /**
     * return resulting state after marking clean
     */
    public ModificationState markClean()
    {
        return StateOldClean.getInstance();
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
        return StateOldDirty.getInstance();
    }

    /**
     * return resulting state after marking old
     */
    public ModificationState markOld()
    {
        return this;
    }

    /**
     * returns true is this state requires DELETE
     * @return boolean
     */
    public boolean needsDelete()
    {
        return true;
    }

    /**
     * rollback the transaction
     */
    public void checkpoint(ObjectEnvelope mod)
            throws org.apache.ojb.broker.PersistenceBrokerException
    {
        mod.doDelete();
        mod.setModificationState(StateTransient.getInstance());
    }

    /**
     * commit the associated transaction
     */
    public void commit(ObjectEnvelope mod) throws PersistenceBrokerException
    {
        mod.doDelete();
        mod.setModificationState(StateTransient.getInstance());
    }

    /**
     *
     */
    public void rollback(ObjectEnvelope mod)
    {
        mod.doEvictFromCache();
    }
}
