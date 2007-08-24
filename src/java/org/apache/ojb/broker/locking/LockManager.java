package org.apache.ojb.broker.locking;

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


/**
 * This interface declares the functionality of the OJB locking-api for support of
 * pessimistic locking.
 * <p>
 * OJB allows to provide user defined implementations of this interface.
 * To activate a user defined LockManager implementation it must be configured in
 * the OJB.properties file.
 * </p>
 * <p>
 * All locks have to be reentrant, this means if you already have a lock for
 * writing and you try to acquire write access again you will not be blocked
 * by this first lock.
 * </p>
 * <p>
 * It's optional to support the <em>lockTimeout</em> and <em>blockTimeout</em> properties.
 * </p>
 *
 * @see LockManagerInMemoryImpl
 * @see LockManagerCommonsImpl
 * @version $Id: LockManager.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public interface LockManager extends IsolationLevels
{
    /**
     * Default lock timeout value - set to 60000 ms.
     */
    public final static long DEFAULT_LOCK_TIMEOUT = 60000;

    /**
     * Default lock wait time in millisecond - set to 1000 ms;
     */
    public final static long DEFAULT_BLOCK_TIMEOUT = 1000;

    /**
     * The maximal time to wait for acquire a lock.
     *
     * @return
     */
    public long getBlockTimeout();

    /**
     * Set the maximal time to wait for acquire a lock in milliseconds.
     * All so called <em>non-blocking</em> implementation will ignore this setting.
     *
     * @param timeout The time to wait for acquire a lock.
     */
    public void setBlockTimeout(long timeout);

    /**
     * Get the current used lock timeout value in milliseconds.
     * @return Current used locking timeout value in ms.
     */
    public long getLockTimeout();

    /**
     * Set the lock timeout value in milliseconds. If timeout was set to <em>-1</em>
     * the never will never timeout.
     *
     * @param timeout The lock timeout in <em>ms</em> of acquired read/write/upgrade locks.
      */
    public void setLockTimeout(long timeout);

    /**
     * Returns info about the used lock manager implementation and the state
     * of the lock manager.
     */
    public String getLockInfo();

    /**
     * Acquires a readlock for lock key on resource object.
     * Returns true if successful, else false.
     *
     * @param key            The owner key of the lock.
     * @param resourceId     The resource to lock.
     * @param isolationLevel The isolation level of the lock.
     * @return <em>True</em> if the lock was successfully acquired.
     */
    public boolean readLock(Object key, Object resourceId, int isolationLevel);

    /**
     * Acquires a write lock for lock key on resource object.
     * Returns true if successful, else false.
     *
     * @param key            The owner key of the lock.
     * @param resourceId     The resource to lock.
     * @param isolationLevel The isolation level of the lock.
     * @return <em>True</em> if the lock was successfully acquired.
     */
    public boolean writeLock(Object key, Object resourceId, int isolationLevel);

    /**
     * Acquire an upgrade lock.
     * (Current implementations always acquire a write lock instead).
     *
     * @param key            The owner key of the lock.
     * @param resourceId     The resource to lock.
     * @param isolationLevel The isolation level of the lock.
     * @return <em>True</em> if the lock was successfully acquired.
     */
    public boolean upgradeLock(Object key, Object resourceId, int isolationLevel);

    /**
     * Releases a lock for lock key on resource object.
     * Returns true if successful, else false.
     *
     * @param key            The owner key of the lock.
     * @param resourceId     The resource to release.
     * @return <em>True</em> if the lock was successfully released.
     */
    public boolean releaseLock(Object key, Object resourceId);

    /**
     * Release all resource locks hold by the specified owner key.
     *
     * @param key The owner key to release all associated locks.
     */
    public void releaseLocks(Object key);

    /**
     * Checks if there is a read lock for owner key on resource object.
     * Returns true if so, else false.
     *
     * @param key            The owner key of the lock.
     * @param resourceId     The resource to check.
     * @return <em>True</em> if the lock exists.
     */
    public boolean hasRead(Object key, Object resourceId);

    /**
     * Checks if there is a write lock for lock key on resource object.
     * Returns true if so, else false.
     *
     * @param key            The owner key of the lock.
     * @param resourceId     The resource to check.
     * @return <em>True</em> if the lock exists.
     */
    public boolean hasWrite(Object key, Object resourceId);

    /**
     * Checks if there is a upgrade lock for lock key on resource object.
     * Returns true if so, else false.
     *
     * @param key            The owner key of the lock.
     * @param resourceId     The resource to check.
     * @return <em>True</em> if the lock exists.
     */
    public boolean hasUpgrade(Object key, Object resourceId);
}
