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

import org.apache.ojb.odmg.TransactionImpl;

/**
 * this interface defines method that a Locking Strategy must implement
 * according to the transaction isolation level it represents.
 * @deprecated 
 */
public interface LockStrategy
{

    /**
     * acquire a read lock on Object obj for Transaction tx.
     * @param tx the transaction requesting the lock
     * @param obj the Object to be locked
     * @return true if successful, else false
     *
     */
    public boolean readLock(TransactionImpl tx, Object obj);

    /**
     * acquire a write lock on Object obj for Transaction tx.
     * @param tx the transaction requesting the lock
     * @param obj the Object to be locked
     * @return true if successful, else false
     *
     */
    public boolean writeLock(TransactionImpl tx, Object obj);

    /**
     * acquire a lock upgrade (from read to write) lock on Object obj for Transaction tx.
     * @param tx the transaction requesting the lock
     * @param obj the Object to be locked
     * @return true if successful, else false
     *
     */
    public boolean upgradeLock(TransactionImpl tx, Object obj);

    /**
     * release a lock on Object obj for Transaction tx.
     * @param tx the transaction releasing the lock
     * @param obj the Object to be unlocked
     * @return true if successful, else false
     *
     */
    public boolean releaseLock(TransactionImpl tx, Object obj);

    /**
     * checks whether the specified Object obj is read-locked by Transaction tx.
     * @param tx the transaction
     * @param obj the Object to be checked
     * @return true if lock exists, else false
     */
    public boolean checkRead(TransactionImpl tx, Object obj);

    /**
     * checks whether the specified Object obj is write-locked by Transaction tx.
     * @param tx the transaction
     * @param obj the Object to be checked
     * @return true if lock exists, else false
     */
    public boolean checkWrite(TransactionImpl tx, Object obj);
}
