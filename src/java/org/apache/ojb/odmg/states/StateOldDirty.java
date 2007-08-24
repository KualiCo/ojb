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
 * this state represents old objects which have been altered during tx.
 */
public class StateOldDirty extends ModificationState
{

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
        return StateOldDelete.getInstance();
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
        return this;
    }

    /**
     * return resulting state after marking old
     */
    public ModificationState markOld()
    {
        return this;
    }

    private static StateOldDirty _instance = new StateOldDirty();

    /**
     * private constructor: use singleton instance
     */
    private StateOldDirty()
    {
    }

    /**
     * perform a checkpoint, i.e. perform updates on underlying db but keep locks on objects
     */
    public static StateOldDirty getInstance()
    {
        return _instance;
    }

    /**
     * checkpoint the transaction
     */
    public void checkpoint(ObjectEnvelope mod)
            throws org.apache.ojb.broker.PersistenceBrokerException
    {
        mod.doUpdate();
    }


    /**
     * commit the associated transaction
     */
    public void commit(ObjectEnvelope mod) throws org.apache.ojb.broker.PersistenceBrokerException
    {
        mod.doUpdate();
        mod.setModificationState(StateOldClean.getInstance());
    }

    /**
     * rollback transaction.
     */
    public void rollback(ObjectEnvelope mod)
    {
        mod.doEvictFromCache();
/*
arminw: we can't really restore object state with all dependencies and fields
without having a deep copy of the clean object. To avoid side-effects disable this
feature
*/
//        // Call added to rollback the object itself so it has the previous values again when it is used further on.
//        mod.rollback();
    }

    /*
     * @see ModificationState#needsUpdate()
     */
    public boolean needsUpdate()
    {
        return true;
    }

}
