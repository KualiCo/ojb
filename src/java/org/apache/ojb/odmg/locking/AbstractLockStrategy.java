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
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;

import java.util.Collection;

/**
 * The base class of all LockingStrategies. It provides the basic
 * infrastructure to read and write locks to the persistent storage.
 * @deprecated
 * @author Thomas Mahler
 */
public abstract class AbstractLockStrategy implements LockStrategy
{
    /**
     * the timeout for lock entries
     */
    public static long DEFAULT_LOCK_TIMEOUT = 30000;

    /**
     * the map holding all locks
     */
    private static LockMap lockMap = null;


    public AbstractLockStrategy()
    {
        synchronized (AbstractLockStrategy.class)
        {
            if (lockMap == null)
            {
                lockMap = LockMapFactory.getLockMap();
                Configuration conf = OjbConfigurator.getInstance().getConfigurationFor(null);
                DEFAULT_LOCK_TIMEOUT = conf.getInteger("LockTimeout", 60000);
            }
        }
    }


    /**
     * returns the LockEntry for the Writer of object obj.
     * If now writer exists, null is returned.
     */
    protected LockEntry getWriter(Object obj)
    {
        return lockMap.getWriter(obj);
    }

    /**
     * returns a collection of Reader LockEntries for object obj.
     * If now LockEntries could be found an empty Vector is returned.
     */
    protected Collection getReaders(Object obj)
    {
        return lockMap.getReaders(obj);
    }

    /**
     * Add a reader lock entry for transaction tx on object obj
     * to the persistent storage.
     */
    protected boolean addReader(TransactionImpl tx, Object obj)
    {
        return lockMap.addReader(tx, obj);
    }

    /**
     * remove a reader lock entry for transaction tx on object obj
     * from the persistent storage.
     */
    protected void removeReader(TransactionImpl tx, Object obj)
    {
        lockMap.removeReader(tx, obj);
    }

    /**
     * remove a writer lock entry for transaction tx on object obj
     * from the persistent storage.
     */
    protected void removeWriter(LockEntry writer)
    {
        lockMap.removeWriter(writer);
    }


    /**
     * upgrade a reader lock entry for transaction tx on object obj
     * and write it to the persistent storage.
     */
    protected boolean upgradeLock(LockEntry reader)
    {
        return lockMap.upgradeLock(reader);
    }

    /**
     * generate a writer lock entry for transaction tx on object obj
     * and write it to the persistent storage.
     */
    protected boolean setWriter(TransactionImpl tx, Object obj)
    {
        return lockMap.setWriter(tx, obj);
    }

    /**
     * check if there is a reader lock entry for transaction tx on object obj
     * in the persistent storage.
     */
    protected boolean hasReadLock(TransactionImpl tx, Object obj)
    {
        return lockMap.hasReadLock(tx, obj);
    }

}
