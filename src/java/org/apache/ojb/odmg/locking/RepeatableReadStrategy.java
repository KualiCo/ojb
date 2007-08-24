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

import java.util.Collection;

/**
 * The implementation of the Repeatable Reads Locking strategy.
 * Locks are obtained for reading and modifying the database.
 * Locks on all modified objects are held until EOT.
 * Locks obtained for reading data are held until EOT.
 * Allows:
 * Phantom Reads
 *
 * @author Thomas Mahler & David Dixon-Peugh
 */
public class RepeatableReadStrategy extends AbstractLockStrategy
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
            if (addReader(tx, obj))
                return true;
            else
                return readLock(tx, obj);
        }
        if (writer.isOwnedBy(tx))
        {
            return true;    // If I'm the writer, I can read.
        }
        else
            return false;

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
        Collection readers = getReaders(obj);
        if (writer == null)
        {
            if (readers.size() == 0)
            {
                if (setWriter(tx, obj))
                    return true;
                else
                    return writeLock(tx, obj);
            }

            else if (readers.size() == 1)
            {
                if (((LockEntry) readers.iterator().next()).isOwnedBy(tx))
                    return upgradeLock(tx, obj);
            }
        }
        else if (writer.isOwnedBy(tx))
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
            Collection readers = this.getReaders(obj);
            if (readers.size() == 1)
            {
                LockEntry reader = (LockEntry) readers.iterator().next();
                if (reader.isOwnedBy(tx))
                {
                    if (upgradeLock(reader))
                        return true;
                    else
                        return upgradeLock(tx, obj);
                }
            }
            else if (readers.size() == 0)
            {
                if (setWriter(tx, obj))
                    return true;
                else
                    return upgradeLock(tx, obj);
            }


        }
        else if (writer.isOwnedBy(tx))
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
        if (writer != null && writer.isOwnedBy(tx))
        {
            return true;
        }
        else
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
        return (writer != null && writer.isOwnedBy(tx));
    }
}
