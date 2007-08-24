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
 * The implementation of the Commited Reads Locking stra
 * ReadCommitted - Reads and Writes require locks.
 *
 * Locks are acquired for reading and modifying the database.
 * Locks are released after reading but locks on modified objects
 * are held until EOT.
 *
 * Allows:
 * Non-Repeatable Reads
 * Phantom Readstegy.
 *
 * @author Thomas Mahler & David Dixon-Peugh
 */
public class ReadCommittedStrategy extends AbstractLockStrategy
{

    /**
     * acquire a read lock on Object obj for Transaction tx.
     * @param tx the transaction requesting the lock
     * @param obj the Object to be locked
     * @return true if successful, else false
     *
     */
    public boolean readLock(TransactionImpl tx, Object obj)
    {

        LockEntry writer = getWriter(obj);
        if (writer == null)
        {
            addReader(tx, obj);
            // if there has been a successful write locking, try again
            if (getWriter(obj) == null)
                return true;
            else
            {
                removeReader(tx, obj);
                return readLock(tx, obj);
            }
        }
        if (writer.isOwnedBy(tx))
        {
            return true;    // If I'm the writer, I can read.
        }
        else
        {
            return false;
        }
    }

    /**
     * acquire a write lock on Object obj for Transaction tx.
     * @param tx the transaction requesting the lock
     * @param obj the Object to be locked
     * @return true if successful, else false
     *
     */
    public boolean writeLock(TransactionImpl tx, Object obj)
    {
        LockEntry writer = getWriter(obj);
        // if there is no writer yet we can try to get the global write lock
        if (writer == null)
        {
            // if lock could be acquired return true
            if (setWriter(tx, obj))
                return true;
            // else try again
            else
                return writeLock(tx, obj);
        }
        if (writer.isOwnedBy(tx))
        {
            return true;    // If I'm the writer, then I can write.
        }

        return false;
    }

    /**
     * acquire a lock upgrade (from read to write) lock on Object obj for Transaction tx.
     * @param tx the transaction requesting the lock
     * @param obj the Object to be locked
     * @return true if successful, else false
     *
     */
    public boolean upgradeLock(TransactionImpl tx, Object obj)
    {
        LockEntry writer = getWriter(obj);
        if (writer == null)
        {
            // if lock could be acquired return true
            if (setWriter(tx, obj))
                return true;
            // else try again
            else
                return upgradeLock(tx, obj);
        }
        if (writer.isOwnedBy(tx))
        {
            return true;    // If I already have Write, then I've upgraded.
        }

        return false;
    }

    /**
     * release a lock on Object obj for Transaction tx.
     * @param tx the transaction releasing the lock
     * @param obj the Object to be unlocked
     * @return true if successful, else false
     *
     */
    public boolean releaseLock(TransactionImpl tx, Object obj)
    {
        LockEntry writer = getWriter(obj);

        if (writer != null && writer.isOwnedBy(tx))
        {
            removeWriter(writer);
            return true;
        }

        if (hasReadLock(tx, obj))
        {
            removeReader(tx, obj);
            return true;
        }
        return false;
    }

    /**
     * checks whether the specified Object obj is read-locked by Transaction tx.
     * @param tx the transaction
     * @param obj the Object to be checked
     * @return true if lock exists, else false
     */
    public boolean checkRead(TransactionImpl tx, Object obj)
    {
        if (hasReadLock(tx, obj))
        {
            return true;
        }
        LockEntry writer = getWriter(obj);
        if (writer.isOwnedBy(tx))
        {
            return true;
        }
        return false;
    }

    /**
     * checks whether the specified Object obj is write-locked by Transaction tx.
     * @param tx the transaction
     * @param obj the Object to be checked
     * @return true if lock exists, else false
     */
    public boolean checkWrite(TransactionImpl tx, Object obj)
    {
        LockEntry writer = getWriter(obj);
        if (writer == null)
            return false;
        else if (writer.isOwnedBy(tx))
            return true;
        else
            return false;
    }
}
