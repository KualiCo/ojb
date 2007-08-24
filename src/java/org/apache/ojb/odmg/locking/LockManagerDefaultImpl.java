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

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.odmg.TransactionImpl;

/**
 * The OJB default implementation of a Locking mechanism.
 * This Implementation supports 4 transaction isolation levels
 * as specified in the interface {@link org.apache.ojb.broker.locking.IsolationLevels}:
 *     public final int IL_READ_UNCOMMITTED = 0;
 *     public final int IL_READ_COMMITTED = 1;
 *     public final int IL_REPEATABLE_READ = 2;
 *     public final int IL_SERIALIZABLE = 3;
 * Isolationlevels can be adjusted per class.
 * The proper lockhandling is done in the respective LockStrategy implementation.
 * This default implementation provides persistent Locks that are stored in
 * a special database table.
 * To keep the locks in the database and not in memory allows to use
 * them accross multiple distributed ODMG clients.
 *
 * Of course this solution causes a lot of database reads and writes even if
 * no real application data is written to the database. This solution may
 * thus not be suited for all environments. As the LockManager is pluggable
 * its possible to replace the default implementation by user defined
 * implementations.
 * A different solution might be to implement the LockManager as an additional
 * standalone server, that allows to elminate additional db reads and writes.
 *
 * @author thma
 * @deprecated 
 */
public class LockManagerDefaultImpl implements LockManager
{
    private Logger log = LoggerFactory.getLogger(LockManagerDefaultImpl.class);

    public LockManagerDefaultImpl()
    {
    }

    /**
     * aquires a readlock for transaction tx on object obj.
     * Returns true if successful, else false.
     */
    public synchronized boolean readLock(TransactionImpl tx, Object obj)
    {
        if (log.isDebugEnabled()) log.debug("LM.readLock(tx-" + tx.getGUID() + ", " + new Identity(obj, tx.getBroker()).toString() + ")");
        LockStrategy lockStrategy = LockStrategyFactory.getStrategyFor(obj);
        return lockStrategy.readLock(tx, obj);
    }

    public boolean readLock(TransactionImpl tx, Identity oid, Object obj)
    {
        return readLock(tx,obj);
    }

    /**
     * aquires a writelock for transaction tx on object obj.
     * Returns true if successful, else false.
     */
    public synchronized boolean writeLock(TransactionImpl tx, Object obj)
    {
        if (log.isDebugEnabled()) log.debug("LM.writeLock(tx-" + tx.getGUID() + ", " + new Identity(obj, tx.getBroker()).toString() + ")");
        LockStrategy lockStrategy = LockStrategyFactory.getStrategyFor(obj);
        return lockStrategy.writeLock(tx, obj);
    }

    public boolean writeLock(TransactionImpl tx, Identity oid, Object obj)
    {
        return writeLock(tx, obj);
    }

    /**
     * upgrades readlock for transaction tx on object obj to a writelock.
     * If no readlock existed a writelock is acquired anyway.
     * Returns true if successful, else false.
     */
    public synchronized boolean upgradeLock(TransactionImpl tx, Object obj)
    {
        if (log.isDebugEnabled()) log.debug("LM.upgradeLock(tx-" + tx.getGUID() + ", " + new Identity(obj, tx.getBroker()).toString() + ")");
        LockStrategy lockStrategy = LockStrategyFactory.getStrategyFor(obj);
        return lockStrategy.upgradeLock(tx, obj);
    }

    public boolean upgradeLock(TransactionImpl tx, Identity oid, Object obj)
    {
        return upgradeLock(tx, obj);
    }

    /**
     * releases a lock for transaction tx on object obj.
     * Returns true if successful, else false.
     */
    public synchronized boolean releaseLock(TransactionImpl tx, Object obj)
    {
        if (log.isDebugEnabled()) log.debug("LM.releaseLock(tx-" + tx.getGUID() + ", " + new Identity(obj, tx.getBroker()).toString() + ")");
        LockStrategy lockStrategy = LockStrategyFactory.getStrategyFor(obj);
        return lockStrategy.releaseLock(tx, obj);
    }

    public boolean releaseLock(TransactionImpl tx, Identity oid, Object obj)
    {
        return releaseLock(tx, obj);
    }

    /**
     * checks if there is a readlock for transaction tx on object obj.
     * Returns true if so, else false.
     */
    public synchronized boolean checkRead(TransactionImpl tx, Object obj)
    {
        if (log.isDebugEnabled()) log.debug("LM.checkRead(tx-" + tx.getGUID() + ", " + new Identity(obj, tx.getBroker()).toString() + ")");
        LockStrategy lockStrategy = LockStrategyFactory.getStrategyFor(obj);
        return lockStrategy.checkRead(tx, obj);
    }

    public boolean checkRead(TransactionImpl tx, Identity oid, Object obj)
    {
        return checkRead(tx, obj);
    }

    /**
     * checks if there is a writelock for transaction tx on object obj.
     * Returns true if so, else false.
     */
    public synchronized boolean checkWrite(TransactionImpl tx, Object obj)
    {
        if (log.isDebugEnabled()) log.debug("LM.checkWrite(tx-" + tx.getGUID() + ", " + new Identity(obj, tx.getBroker()).toString() + ")");
        LockStrategy lockStrategy = LockStrategyFactory.getStrategyFor(obj);
        return lockStrategy.checkWrite(tx, obj);
    }

    public boolean checkWrite(TransactionImpl tx, Identity oid, Object obj)
    {
        return checkWrite(tx, obj);
    }
}
