package org.apache.ojb.odmg.locking;

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

import org.apache.ojb.broker.util.configuration.Configurable;
import org.apache.ojb.odmg.TransactionImpl;

import java.util.Collection;

/**
 * @deprecated 
 */
public interface LockMap extends Configurable
{
    /**
     * returns the LockEntry for the Writer of object obj.
     * If now writer exists, null is returned.
     */
    public LockEntry getWriter(Object obj);

    /**
     * returns a collection of Reader LockEntries for object obj.
     * If now LockEntries could be found an empty Vector is returned.
     */
    public Collection getReaders(Object obj);

    /**
     * Add a reader lock entry for transaction tx on object obj
     * to the persistent storage.
     */
    public boolean addReader(TransactionImpl tx, Object obj);

    /**
     * remove a reader lock entry for transaction tx on object obj
     * from the persistent storage.
     */
    public void removeReader(TransactionImpl tx, Object obj);

    /**
     * remove a writer lock entry for transaction tx on object obj
     * from the persistent storage.
     */
    public void removeWriter(LockEntry writer);

    /**
     * upgrade a reader lock entry for transaction tx on object obj
     * and write it to the persistent storage.
     */
    public boolean upgradeLock(LockEntry reader);

    /**
     * generate a writer lock entry for transaction tx on object obj
     * and write it to the persistent storage.
     */
    public boolean setWriter(TransactionImpl tx, Object obj);

    /**
     * check if there is a reader lock entry for transaction tx on object obj
     * in the persistent storage.
     */
    public boolean hasReadLock(TransactionImpl tx, Object obj);
}
