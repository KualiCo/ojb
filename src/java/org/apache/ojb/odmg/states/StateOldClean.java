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
 * this state represents old Objects (i.e. already persistent but not changed during tx)
 * --> no need to do anything for commit or rollback
 */
public class StateOldClean extends ModificationState
{
    private static StateOldClean _instance = new StateOldClean();

    /**
     * private constructor: use singleton instance
     */
    private StateOldClean()
    {

    }

    /**
     * perform a checkpoint, i.e. perform updates on underlying db but keep locks on objects
     */
    public static StateOldClean getInstance()
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
        return StateOldDelete.getInstance();
    }

    /**
     * return resulting state after marking dirty
     */
    public ModificationState markDirty()
    {
        return StateOldDirty.getInstance();
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

    /**
     * rollback the ObjectModification
     */
    public void checkpoint(ObjectEnvelope mod)
    {

    }

    /**
     * commit the associated transaction
     */
    public void commit(ObjectEnvelope mod)
    {

    }

    /**
     *
     */
    public void rollback(ObjectEnvelope mod)
    {

    }
}
