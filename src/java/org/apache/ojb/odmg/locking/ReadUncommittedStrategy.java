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
 * The implementation of the Uncommited Reads Locking strategy.
 * This strategy is the loosest of them all.  It says
 * you shouldn't need to get any Read locks whatsoever,
 * but since it will probably try to get them, it will
 * always give it to them.
 *
 * Locks are obtained on modifications to the database and held until end of
 * transaction (EOT). Reading from the database does not involve any locking.
 *
 * Allows:
 * Dirty Reads
 * Non-Repeatable Reads
 * Phantom Reads
 *
 * @author Thomas Mahler & David Dixon-Peugh
 */
public class ReadUncommittedStrategy extends AbstractLockStrategy
{
    /**
     * acquire a read lock on Object obj for Transaction tx.
     * @param tx the transaction requesting the lock
     * @param obj the Object to be locked
     * @return true if successful, else false
     * When we read Uncommitted, we don't care about Reader locks
     */
    public boolean readLock(TransactionImpl tx, Object obj)
    {
        return true;
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
        if (writer == null)
        {
            if (setWriter(tx, obj))
                return true;
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
            if (setWriter(tx, obj))
                return true;
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
        // readlocks cannot (and need not) be released, thus:
        return true;
    }

    /**
     * checks whether the specified Object obj is read-locked by Transaction tx.
     * @param tx the transaction
     * @param obj the Object to be checked
     * @return true if lock exists, else false
     */
    public boolean checkRead(TransactionImpl tx, Object obj)
    {
        return true;
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
        return (writer != null && writer.isOwnedBy(tx));
    }
}
