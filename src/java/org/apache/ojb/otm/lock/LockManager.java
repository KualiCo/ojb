package org.apache.ojb.otm.lock;

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
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.lock.isolation.TransactionIsolation;
import org.apache.ojb.otm.lock.map.LockMap;

/**
 *
 * Manages locks on objects across transactions.
 *
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 *
 */
public class LockManager
{
    private static LockManager _Instance = new LockManager();

    public static LockManager getInstance ()
    {
        return _Instance;
    }

    private LockManager()
    {

    }

    public void ensureLock(Identity oid, Transaction tx, int lock,
                           PersistenceBroker pb)
        throws LockingException
    {
        LockMap lockMap = tx.getKit().getLockMap();
        ObjectLock objectLock = lockMap.getLock(oid);
        TransactionIsolation isolation;

        isolation = IsolationFactory.getIsolationLevel(pb, objectLock);

        if (lock == LockType.READ_LOCK)
        {
            isolation.readLock(tx, objectLock);
        }
        else if (lock == LockType.WRITE_LOCK)
        {
            isolation.writeLock(tx, objectLock);
        }
    }

    public int getLockHeld(Identity oid, Transaction tx)
    {
        LockMap lockMap = tx.getKit().getLockMap();
        ObjectLock lock = lockMap.getLock(oid);

        int lockHeld = LockType.NO_LOCK;
        if (tx.equals(lock.getWriter()))
        {
            lockHeld = LockType.WRITE_LOCK;
        }
        else if (lock.isReader(tx))
        {
            lockHeld = LockType.READ_LOCK;
        }

        return lockHeld;
    }

    public void releaseLock(Identity oid, Transaction tx)
    {
        LockMap lockMap = tx.getKit().getLockMap();

        ObjectLock objectLock = lockMap.getLock(oid);
        objectLock.releaseLock(tx);
    }
}
