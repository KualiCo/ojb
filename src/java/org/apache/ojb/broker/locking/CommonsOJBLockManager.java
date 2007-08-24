package org.apache.ojb.broker.locking;

/* Copyright 2005 The Apache Software Foundation
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

import org.apache.commons.transaction.locking.GenericLock;
import org.apache.commons.transaction.locking.GenericLockManager;
import org.apache.commons.transaction.locking.LockException;
import org.apache.commons.transaction.locking.MultiLevelLock;
import org.apache.commons.transaction.util.LoggerFacade;

/**
 * Extension of {@link org.apache.commons.transaction.locking.GenericLockManager} to
 * support all locking isolation level defined in OJB locking api and a provider of
 * specific {@link org.apache.commons.transaction.locking.GenericLock} implementation classes
 * representing the isolation levels specified in {@link org.apache.ojb.broker.locking.LockManager}, like
 * {@link org.apache.ojb.broker.locking.LockManager#IL_READ_COMMITTED}, ... .
 * <p/>
 * The specific lock classes will be returned on call of
 * {@link #createIsolationLevel(Object, Object, org.apache.commons.transaction.util.LoggerFacade)}
 * dependend on the specified isolation level.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: CommonsOJBLockManager.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
class CommonsOJBLockManager extends GenericLockManager
{
    static final int COMMON_READ_LOCK = 101;
    static final int COMMON_WRITE_LOCK = 107;
    static final int COMMON_UPGRADE_LOCK = 113;

    public CommonsOJBLockManager(LoggerFacade logger, long timeoutMSecs, long checkThreshholdMSecs)
            throws IllegalArgumentException
    {
        super(1, logger, timeoutMSecs, checkThreshholdMSecs);
    }

    /**
     * @see org.apache.commons.transaction.locking.GenericLockManager#tryLock(Object, Object, int, boolean)
     */
    public boolean tryLock(Object ownerId, Object resourceId, int targetLockLevel, boolean reentrant)
    {
        return tryLock(ownerId, resourceId, targetLockLevel, reentrant, null);
    }

    /**
     * Tries to acquire a lock on a resource. <br>
     * <br>
     * This method does not block, but immediatly returns. If a lock is not
     * available <code>false</code> will be returned.
     *
     * @param ownerId         a unique id identifying the entity that wants to acquire this
     *                        lock
     * @param resourceId      the resource to get the level for
     * @param targetLockLevel the lock level to acquire
     * @param reentrant       <code>true</code> if this request shall not be influenced by
     *                        other locks held by the same owner
     * @param isolationId     the isolation level identity key. See {@link CommonsOJBLockManager}.
     * @return <code>true</code> if the lock has been acquired, <code>false</code> otherwise
     */
    public boolean tryLock(Object ownerId, Object resourceId, int targetLockLevel, boolean reentrant, Object isolationId)
    {
        timeoutCheck(ownerId);

        OJBLock lock = atomicGetOrCreateLock(resourceId, isolationId);
        boolean acquired = lock.tryLock(ownerId, targetLockLevel,
                reentrant ? GenericLock.COMPATIBILITY_REENTRANT : GenericLock.COMPATIBILITY_NONE,
                false);

        if(acquired)
        {
            addOwner(ownerId, lock);
        }
        return acquired;
    }

    /**
     * @see org.apache.commons.transaction.locking.GenericLockManager#lock(Object, Object, int, int, boolean, long)
     */
    public void lock(Object ownerId, Object resourceId, int targetLockLevel, int compatibility,
                     boolean preferred, long timeoutMSecs) throws LockException
    {
        lock(ownerId, resourceId, targetLockLevel, compatibility, preferred, timeoutMSecs, null);
    }

    /**
     * Most flexible way to acquire a lock on a resource. <br>
     * <br>
     * This method blocks and waits for the lock in case it is not avaiable. If
     * there is a timeout or a deadlock or the thread is interrupted a
     * LockException is thrown.
     *
     * @param ownerId         a unique id identifying the entity that wants to acquire this
     *                        lock
     * @param resourceId      the resource to get the level for
     * @param targetLockLevel the lock level to acquire
     * @param compatibility   {@link GenericLock#COMPATIBILITY_NONE}if no additional compatibility is
     *                        desired (same as reentrant set to false) ,
     *                        {@link GenericLock#COMPATIBILITY_REENTRANT}if lock level by the same
     *                        owner shall not affect compatibility (same as reentrant set to
     *                        true), or {@link GenericLock#COMPATIBILITY_SUPPORT}if lock levels that
     *                        are the same as the desired shall not affect compatibility, or
     *                        finally {@link GenericLock#COMPATIBILITY_REENTRANT_AND_SUPPORT}which is
     *                        a combination of reentrant and support
     * @param preferred       in case this lock request is incompatible with existing ones
     *                        and we wait, it shall be granted before other waiting requests
     *                        that are not preferred
     * @param timeoutMSecs    specifies the maximum wait time in milliseconds
     * @param isolationId     the isolation level identity key. See {@link CommonsOJBLockManager}.
     * @throws LockException will be thrown when the lock can not be acquired
     */
    public void lock(Object ownerId, Object resourceId, int targetLockLevel, int compatibility,
                     boolean preferred, long timeoutMSecs, Object isolationId) throws LockException
    {
        timeoutCheck(ownerId);
        GenericLock lock = atomicGetOrCreateLock(resourceId, isolationId);
        super.doLock(lock, ownerId, resourceId, targetLockLevel, compatibility, preferred, timeoutMSecs);
    }

    /**
     * @see org.apache.commons.transaction.locking.GenericLockManager#atomicGetOrCreateLock(Object)
     */
    public MultiLevelLock atomicGetOrCreateLock(Object resourceId)
    {
        return atomicGetOrCreateLock(resourceId, null);
    }

    /**
     * Either gets an existing lock on the specified resource or creates one if none exists.
     * This methods guarantees to do this atomically.
     *
     * @param resourceId  the resource to get or create the lock on
     * @param isolationId the isolation level identity key. See {@link CommonsOJBLockManager}.
     * @return the lock for the specified resource
     */
    public OJBLock atomicGetOrCreateLock(Object resourceId, Object isolationId)
    {
        synchronized(globalLocks)
        {
            MultiLevelLock lock = getLock(resourceId);
            if(lock == null)
            {
                lock = createLock(resourceId, isolationId);
            }
            return (OJBLock) lock;
        }
    }

    /**
     * @see org.apache.commons.transaction.locking.GenericLockManager#createLock(Object)
     */
    protected GenericLock createLock(Object resourceId)
    {
        return createLock(resourceId, null);
    }

    protected GenericLock createLock(Object resourceId, Object isolationId)
    {
        synchronized(globalLocks)
        {
            if(isolationId != null)
            {
                GenericLock lock = createIsolationLevel(resourceId, isolationId, logger);
                globalLocks.put(resourceId, lock);
                return lock;
            }
            else
            {
                GenericLock lock = new GenericLock(resourceId, maxLockLevel, logger);
                globalLocks.put(resourceId, lock);
                return lock;
            }
        }
    }

    /**
     * Creates {@link org.apache.commons.transaction.locking.GenericLock} based
     * {@link org.apache.commons.transaction.locking.MultiLevelLock2} instances
     * dependend on the specified isolation identity object.
     */
    public OJBLock createIsolationLevel(Object resourceId, Object isolationId, LoggerFacade logger)
    {
        OJBLock result = null;
        switch(((Integer) isolationId).intValue())
        {
            case LockManager.IL_READ_UNCOMMITTED:
                result = new ReadUncommittedLock(resourceId, logger);
                break;
            case LockManager.IL_READ_COMMITTED:
                result = new ReadCommitedLock(resourceId, logger);
                break;
            case LockManager.IL_REPEATABLE_READ:
                result = new RepeadableReadsLock(resourceId, logger);
                break;
            case LockManager.IL_SERIALIZABLE:
                result = new SerializeableLock(resourceId, logger);
                break;
            case LockManager.IL_OPTIMISTIC:
                throw new LockRuntimeException("Optimistic locking must be handled on top of this class");
            default:
                throw new LockRuntimeException("Unknown lock isolation level specified");
        }
        return result;
    }

    /**
     * Helper method to map the specified common lock level (e.g like
     * {@link #COMMON_READ_LOCK}, {@link #COMMON_UPGRADE_LOCK, ...}) based
     * on the isolation level to the internal used lock level value by the
     * {@link org.apache.commons.transaction.locking.MultiLevelLock2} implementation.
     *
     * @param isolationId
     * @param lockLevel
     * @return
     */
    int mapLockLevelDependendOnIsolationLevel(Integer isolationId, int lockLevel)
    {
        int result = 0;
        switch(isolationId.intValue())
        {
            case LockManager.IL_READ_UNCOMMITTED:
                result = ReadUncommittedLock.mapLockLevel(lockLevel);
                break;
            case LockManager.IL_READ_COMMITTED:
                result = ReadCommitedLock.mapLockLevel(lockLevel);
                break;
            case LockManager.IL_REPEATABLE_READ:
                result = RepeadableReadsLock.mapLockLevel(lockLevel);
                break;
            case LockManager.IL_SERIALIZABLE:
                result = SerializeableLock.mapLockLevel(lockLevel);
                break;
            case LockManager.IL_OPTIMISTIC:
                throw new LockRuntimeException("Optimistic locking must be handled on top of this class");
            default:
                throw new LockRuntimeException("Unknown lock isolation level specified");
        }
        return result;
    }



    //===================================================
    // inner class, commons-tx lock
    //===================================================
    /**
     * Abstract base class to implement the different {@link org.apache.commons.transaction.locking.GenericLock}
     * extension classes representing the OJB isolation levels (e.g. READ_COMMITTED, REPEADABLE_READ,...)
     */
    abstract static class OJBLock extends GenericLock
    {
        public OJBLock(Object resourceId, int maxLockLevel, LoggerFacade logger)
        {
            super(resourceId, maxLockLevel, logger);
        }

        /**
         * Need to override this method to make it accessible in
         * {@link CommonsOJBLockManager}.
         *
         * @see GenericLock#tryLock(Object, int, int, boolean)
         */
        protected boolean tryLock(Object ownerId, int targetLockLevel, int compatibility, boolean preferred)
        {
            return super.tryLock(ownerId, targetLockLevel, compatibility, preferred);
        }

        /**
         * Convenience method.
         */
        abstract boolean hasRead(Object ownerId);

        /**
         * Convenience method.
         */
        abstract boolean hasWrite(Object ownerId);

        /**
         * Convenience method.
         */
        abstract boolean hasUpgrade(Object ownerId);

        /**
         * Convenience method.
         */
        abstract boolean readLock(Object ownerId, long timeout) throws InterruptedException;

        /**
         * Convenience method.
         */
        abstract boolean writeLock(Object ownerId, long timeout) throws InterruptedException;

        /**
         * Convenience method.
         */
        abstract boolean upgradeLock(Object ownerId, long timeout) throws InterruptedException;
    }



    //===================================================
    // inner class, commons-tx lock
    //===================================================
    /**
     * Implementation of isolation level {@link LockManager#IL_READ_UNCOMMITTED}.
     */
    static class ReadUncommittedLock extends RepeadableReadsLock
    {
        public ReadUncommittedLock(Object resourceId, LoggerFacade logger)
        {
            super(resourceId, logger);
        }

        protected boolean isCompatible(int targetLockLevel, int currentLockLevel)
        {
            if(currentLockLevel == READ_LOCK || targetLockLevel == READ_LOCK)
            {
                return true;
            }
            else
            {
                return super.isCompatible(targetLockLevel, currentLockLevel);
            }
        }
    }

    //===================================================
    // inner class, commons-tx lock
    //===================================================
    /**
     * Implementation of isolation level {@link LockManager#IL_READ_COMMITTED}.
     */
    static final class ReadCommitedLock extends RepeadableReadsLock
    {
        public ReadCommitedLock(Object resourceId, LoggerFacade logger)
        {
            super(resourceId, logger);
        }

        protected boolean isCompatible(int targetLockLevel, int currentLockLevel)
        {
            if(currentLockLevel == READ_LOCK)
            {
                return true;
            }
            else
            {
                return super.isCompatible(targetLockLevel, currentLockLevel);
            }
        }
    }

    //===================================================
    // inner class, commons-tx lock
    //===================================================
    /**
     * Implementation of isolation level {@link LockManager#IL_REPEATABLE_READ}.
     */
    static class RepeadableReadsLock extends OJBLock
    {
        static final int NO_LOCK = 0;
        static final int READ_LOCK = 1;
        static final int UPGRADE_LOCK = 2;
        static final int WRITE_LOCK = 3;

        public RepeadableReadsLock(Object resourceId, LoggerFacade logger)
        {
            super(resourceId, WRITE_LOCK, logger);
        }

        static int mapLockLevel(int commonLockLevel)
        {
            int result = 0;
            switch(commonLockLevel)
            {
                case COMMON_READ_LOCK:
                    result = READ_LOCK;
                    break;
                case COMMON_UPGRADE_LOCK:
                    result = UPGRADE_LOCK;
                    break;
                case COMMON_WRITE_LOCK:
                    result = WRITE_LOCK;
                    break;
                default:
                    throw new LockRuntimeException("Unknown common lock type: " + commonLockLevel);
            }
            return result;
        }

        public boolean readLock(Object ownerId, long timeout) throws InterruptedException
        {
            return acquire(ownerId, READ_LOCK, false, GenericLock.COMPATIBILITY_REENTRANT, false, timeout);
        }

        public boolean writeLock(Object ownerId, long timeout) throws InterruptedException
        {
            return acquire(ownerId, WRITE_LOCK, true, GenericLock.COMPATIBILITY_REENTRANT, false, timeout);
        }

        public boolean upgradeLock(Object ownerId, long timeout) throws InterruptedException
        {
            return acquire(ownerId, UPGRADE_LOCK, true, GenericLock.COMPATIBILITY_REENTRANT, true, timeout);
        }

        public boolean hasRead(Object ownerId)
        {
            return has(ownerId, READ_LOCK);
        }

        public boolean hasWrite(Object ownerId)
        {
            return has(ownerId, WRITE_LOCK);
        }

        public boolean hasUpgrade(Object ownerId)
        {
            return has(ownerId, UPGRADE_LOCK);
        }
    }


    //===================================================
    // inner class, commons-tx lock
    //===================================================
    /**
     * Implementation of isolation level {@link LockManager#IL_SERIALIZABLE}.
     */
    static final class SerializeableLock extends ReadUncommittedLock
    {
        public SerializeableLock(Object resourceId, LoggerFacade logger)
        {
            super(resourceId, logger);
        }

        protected boolean isCompatible(int targetLockLevel, int currentLockLevel)
        {
            return currentLockLevel > NO_LOCK ? false : true;
        }
    }
}
