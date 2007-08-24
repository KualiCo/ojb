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

/**
 * this state represents new objects which have not been altered during tx.
 */
public class StateNewClean extends ModificationState
{
    private static StateNewClean _instance = new StateNewClean();

    /**
     * private constructor: we use singleton instances
     */
    private StateNewClean()
    {
    }

    /**
     * perform a checkpoint, i.e. perform updates on underlying db but keep locks on objects
     */
    public static StateNewClean getInstance()
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
        return StateNewDelete.getInstance();
    }

    /**
     * return resulting state after marking dirty
     */
    public ModificationState markDirty()
    {
        return StateNewDirty.getInstance();
    }

    /**
     * return resulting state after marking new
     */
    public ModificationState markNew()
    {
        return this;
    }

    /**
     * return resulting state after marking old
     */
    public ModificationState markOld()
    {
        return StateOldClean.getInstance();
    }

    /**
     * object is new, thus we need an INSERT to store it
     */
    public boolean needsInsert()
    {
        return true;
    }

    /**
     * checkpoint the transaction
     */
    public void checkpoint(ObjectEnvelope mod)
            throws org.apache.ojb.broker.PersistenceBrokerException
    {
        mod.doInsert();
        mod.setModificationState(StateOldClean.getInstance());
    }


    /**
     * commit the associated transaction
     */
    public void commit(ObjectEnvelope mod) throws org.apache.ojb.broker.PersistenceBrokerException
    {
        mod.doInsert();
        mod.setModificationState(StateOldClean.getInstance());
    }

    /**
     * rollback the transaction
     */
    public void rollback(ObjectEnvelope mod)
    {
        mod.doEvictFromCache();
    }
}
