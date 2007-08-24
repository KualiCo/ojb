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
 * this state represents new objects which have been mrked for deletion during tx.
 */
public class StateNewDelete extends ModificationState
{
    private static StateNewDelete _instance = new StateNewDelete();

    /**
     * return resulting state after marking clean
     */
    public ModificationState markClean()
    {
        return StateNewClean.getInstance();
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
        return StateNewDirty.getInstance();
    }

    /**
     * return resulting state after marking old
     */
    public ModificationState markOld()
    {
        return StateOldDelete.getInstance();
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
     * private constructor: use singleton instance
     */
    private StateNewDelete()
    {
    }

    /**
     * perform a checkpoint, i.e. perform updates on underlying db but keep locks on objects
     */
    public static StateNewDelete getInstance()
    {
        return _instance;
    }

    /**
     * rollback the ObjectModification
     */
    public void checkpoint(ObjectEnvelope mod)
    {
    }

    /**
     * commit ObjectModification
     */
    public void commit(ObjectEnvelope mod)
    {
        mod.doEvictFromCache();
        mod.setModificationState(StateTransient.getInstance());
    }

    /**
     *
     */
    public void rollback(ObjectEnvelope mod)
    {
        mod.doEvictFromCache();
        mod.setModificationState(StateTransient.getInstance());
    }
}
