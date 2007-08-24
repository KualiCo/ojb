package org.apache.ojb.otm;

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

import org.apache.ojb.broker.Identity;
import org.apache.ojb.otm.lock.LockingException;
import org.apache.ojb.otm.states.State;

import java.util.Collection;

/**
 *
 * The EditingContext contains and manages the set of object read/edited within the context of a
 * transaction. Logically, this could be considered similar to a document that is being edited.
 * During commit, all objects within this transaction that are marked as being written to (ones
 * with a write lock) are written back to the persistent store.
 *
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 *
 */
public interface EditingContext
{

    /**
     *
     * Insert the given object into the EditingContext, acquiring the specified lock.
     *
     * @param oid                   the identity of the object to be inserted
     * @param userObject            the object to insert, for user operations
     * @param lock                  the lock to be acquired.
     * @throws LockingException     thrown by the Lock Manager to avoid deadlocks. The insertion
     *                              could be re-attempted if the lock fails.
     *
     */
    public void insert (Identity oid, Object userObject, int lock)
            throws LockingException;

    /**
     *
     * Remove a managed object from the management of this EditingContext. All edits on the object
     * will be lost. All locks kept by this object will be released.
     *
     * @param oid                   the Identity of the object to be removed from this context.
     *
     */
    public void remove (Identity oid);

    /**
     *
     * Lookup object with the given oid in the Context.
     *
     * @param oid           the oid of the object to lookup
     *
     */
    public Object lookup (Identity oid)
            throws LockingException;

	/**
	 * lookup the state of an object, given the oid, in the context
	 * @param oid
	 * @return the state of that object in the context, null if the object is not in the context
	 * @throws LockingException
	 */
	State lookupState(Identity oid)
			throws LockingException;

	void setState(Identity oid, State state);

	Collection getAllObjectsInContext();

    /**
     *  Rollback all changes made during this transaction to the given object.
     */
    public void refresh(Identity oid, Object object);
}
