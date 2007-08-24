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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.commons.lang.SystemUtils;

/**
 * This implementation of the {@link LockManager} interface supports a simple, fast, non-blocking
 * pessimistic locking for single JVM applications.
 *
 * @version $Id: LockManagerInMemoryImpl.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class LockManagerInMemoryImpl implements LockManager
{
    private Logger log = LoggerFactory.getLogger(LockManagerInMemoryImpl.class);
    private static long CLEANUP_FREQUENCY = 1000; // 1000 milliseconds.
    private static int MAX_LOCKS_TO_CLEAN = 300;
    /**
     * MBAIRD: a LinkedHashMap returns objects in the order you put them in,
     * while still maintaining an O(1) lookup like a normal hashmap. We can then
     * use this to get the oldest entries very quickly, makes cleanup a breeze.
     */
    private HashMap locktable = new HashMap();
    private LockIsolationManager lockStrategyManager = new LockIsolationManager();
    private long m_lastCleanupAt = System.currentTimeMillis();
    private long lockTimeout;
    private long timeoutCounterRead;
    private long timeoutCounterWrite;

    public LockManagerInMemoryImpl()
    {
        this.lockTimeout = DEFAULT_LOCK_TIMEOUT;
    }

    public long getLockTimeout()
    {
        return lockTimeout;
    }

    public void setLockTimeout(long timeout)
    {
        this.lockTimeout = timeout;
    }

    /**
     * NOOP
     * @return Always '0'
     */
    public long getBlockTimeout()
    {
        return 0;
    }

    /**
     * NOOP
     */ 
    public void setBlockTimeout(long timeout)
    {
    }

    public String getLockInfo()
    {
        String eol = SystemUtils.LINE_SEPARATOR;
        StringBuffer msg = new StringBuffer("Class: " + LockManagerInMemoryImpl.class.getName() + eol);
        msg.append("lock timeout: " + getLockTimeout() + " [ms]" + eol);
        msg.append("concurrent lock owners: " + locktable.size() + eol);
        msg.append("timed out write locks: " + timeoutCounterWrite + eol);
        msg.append("timed out read locks: " + timeoutCounterRead + eol);
        return msg.toString();
    }

    public boolean readLock(Object key, Object resourceId, int isolationLevel)
    {
        if(log.isDebugEnabled()) log.debug("LM.readLock(tx-" + key + ", " + resourceId + ")");
        checkTimedOutLocks();
        LockEntry reader = new LockEntry(resourceId,
                key,
                System.currentTimeMillis(),
                isolationLevel,
                LockEntry.LOCK_READ);
        LockIsolation ls = lockStrategyManager.getStrategyFor(isolationLevel);
        return addReaderIfPossibleInternal(reader, ls.allowMultipleRead(), ls.allowReadWhenWrite());
    }

    private boolean addReaderIfPossibleInternal(LockEntry reader, boolean allowMultipleReader,
                                                boolean allowReaderWhenWriteLock)
    {
        boolean result = false;
        ObjectLocks objectLocks = null;
        Object oid = reader.getResourceId();
        /**
         * MBAIRD: We need to synchronize the get/put so we don't have two threads
         * competing to check if something is locked and double-locking it.
         */
        synchronized(locktable)
        {
            objectLocks = (ObjectLocks) locktable.get(oid);
            if(objectLocks == null)
            {
                // no write or read lock, go on
                objectLocks = new ObjectLocks();
                locktable.put(oid, objectLocks);
                objectLocks.addReader(reader);
                result = true;
            }
            else
            {
                // ObjectLocks exist, first check for a write lock
                LockEntry writer = objectLocks.getWriter();
                if(writer != null)
                {
                    // if writer is owned by current entity, read lock is
                    // successful (we have an write lock)
                    if(writer.isOwnedBy(reader.getKey()))
                    {
                        result = true;
                    }
                    else
                    {
                        // if read lock is allowed when different entity hold write lock
                        // go on if multiple reader allowed, else do nothing
                        if(allowReaderWhenWriteLock && allowMultipleReader)
                        {
                            objectLocks.addReader(reader);
                            result = true;
                        }
                        else
                        {
                            result = false;
                        }
                    }
                }
                else
                {
                    // no write lock exist, check for existing read locks
                    if(objectLocks.getReaders().size() > 0)
                    {
                        // if we have already an read lock, do nothing
                        if(objectLocks.getReader(reader.getKey()) != null)
                        {
                            result = true;
                        }
                        else
                        {
                            // we have read locks of other entities, add read lock
                            // if allowed
                            if(allowMultipleReader)
                            {
                                objectLocks.addReader(reader);
                                result = true;
                            }
                        }
                    }
                    else
                    {
                        // no read locks exist, so go on
                        objectLocks.addReader(reader);
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Remove an read lock.
     */
    public boolean removeReader(Object key, Object resourceId)
    {
        boolean result = false;
        ObjectLocks objectLocks = null;
        synchronized(locktable)
        {
            objectLocks = (ObjectLocks) locktable.get(resourceId);
            if(objectLocks != null)
            {
                /**
                 * MBAIRD, last one out, close the door and turn off the lights.
                 * if no locks (readers or writers) exist for this object, let's remove
                 * it from the locktable.
                 */
                Map readers = objectLocks.getReaders();
                result = readers.remove(key) != null;
                if((objectLocks.getWriter() == null) && (readers.size() == 0))
                {
                    locktable.remove(resourceId);
                }
            }
        }
        return result;
    }

    /**
     * Remove an write lock.
     */
    public boolean removeWriter(Object key, Object resourceId)
    {
        boolean result = false;
        ObjectLocks objectLocks = null;
        synchronized(locktable)
        {
            objectLocks = (ObjectLocks) locktable.get(resourceId);
            if(objectLocks != null)
            {
                /**
                 * MBAIRD, last one out, close the door and turn off the lights.
                 * if no locks (readers or writers) exist for this object, let's remove
                 * it from the locktable.
                 */
                LockEntry entry = objectLocks.getWriter();
                if(entry != null && entry.isOwnedBy(key))
                {
                    objectLocks.setWriter(null);
                    result = true;

                    // no need to check if writer is null, we just set it.
                    if(objectLocks.getReaders().size() == 0)
                    {
                        locktable.remove(resourceId);
                    }
                }
            }
        }
        return result;
    }

    public boolean releaseLock(Object key, Object resourceId)
    {
        if(log.isDebugEnabled()) log.debug("LM.releaseLock(tx-" + key + ", " + resourceId + ")");
        boolean result = removeReader(key, resourceId);
        // if no read lock could be removed, try write lock
        if(!result)
        {
            result = removeWriter(key, resourceId);
        }
        return result;
    }

    /**
     * @see LockManager#releaseLocks(Object)
     */
    public void releaseLocks(Object key)
    {
        if(log.isDebugEnabled()) log.debug("LM.releaseLocks(tx-" + key + ")");
        checkTimedOutLocks();
        releaseLocksInternal(key);
    }

    private void releaseLocksInternal(Object key)
    {
        synchronized(locktable)
        {
            Collection values = locktable.values();
            ObjectLocks entry;
            for(Iterator iterator = values.iterator(); iterator.hasNext();)
            {
                entry = (ObjectLocks) iterator.next();
                entry.removeReader(key);
                if(entry.getWriter() != null && entry.getWriter().isOwnedBy(key))
                {
                    entry.setWriter(null);
                }
            }
        }
    }

    public boolean writeLock(Object key, Object resourceId, int isolationLevel)
    {
        if(log.isDebugEnabled()) log.debug("LM.writeLock(tx-" + key + ", " + resourceId + ")");
        checkTimedOutLocks();
        LockEntry writer = new LockEntry(resourceId,
                key,
                System.currentTimeMillis(),
                isolationLevel,
                LockEntry.LOCK_WRITE);
        LockIsolation ls = lockStrategyManager.getStrategyFor(isolationLevel);
        return setWriterIfPossibleInternal(writer, ls.allowWriteWhenRead());
    }

    private boolean setWriterIfPossibleInternal(LockEntry writer, boolean allowReaders)
    {
        boolean result = false;
        ObjectLocks objectLocks = null;
        /**
         * MBAIRD: We need to synchronize the get/put so we don't have two threads
         * competing to check if something is locked and double-locking it.
         */
        synchronized(locktable)
        {
            objectLocks = (ObjectLocks) locktable.get(writer.getResourceId());
            // if we don't upgrade, go on
            if(objectLocks == null)
            {
                // no locks for current entity exist, so go on
                objectLocks = new ObjectLocks();
                objectLocks.setWriter(writer);
                locktable.put(writer.getResourceId(), objectLocks);
                result = true;
            }
            else
            {
                // the ObjectLock exist, check if there is already a write lock
                LockEntry oldWriter = objectLocks.getWriter();
                if(oldWriter != null)
                {
                    // if already a write lock exists, check owner
                    if(oldWriter.isOwnedBy(writer.getKey()))
                    {
                        // if current entity has already a write lock
                        // signal success
                        result = true;
                    }
                }
                else
                {
                    // current ObjectLock has no write lock, so check for readers
                    int readerSize = objectLocks.getReaders().size();
                    if(readerSize > 0)
                    {
                        // does current entity have already an read lock
                        if(objectLocks.getReader(writer.getKey()) != null)
                        {
                            if(readerSize == 1)
                            {
                                // only current entity has a read lock, so go on
                                objectLocks.readers.remove(writer.getKey());
                                objectLocks.setWriter(writer);
                                result = true;
                            }
                            else
                            {
                                // current entity and others have already a read lock
                                // if aquire a write is allowed, go on
                                if(allowReaders)
                                {
                                    objectLocks.readers.remove(writer.getKey());
                                    objectLocks.setWriter(writer);
                                    result = true;
                                }
                            }
                        }
                        else
                        {
                            // current entity has no read lock, but others
                            // if aquire a write is allowed, go on
                            if(allowReaders)
                            {
                                objectLocks.setWriter(writer);
                                result = true;
                            }
                        }
                    }
                    else
                    {
                        // no readers and writers, so go on if we don't upgrade
                        objectLocks.setWriter(writer);
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    public boolean upgradeLock(Object key, Object resourceId, int isolationLevel)
    {
        if(log.isDebugEnabled()) log.debug("LM.upgradeLock(tx-" + key + ", " + resourceId + ")");
        return writeLock(key, resourceId, isolationLevel);
    }

    /**
     * @see LockManager#hasWrite(Object, Object)
     */
    public boolean hasWrite(Object key, Object resourceId)
    {
        if(log.isDebugEnabled()) log.debug("LM.hasWrite(tx-" + key + ", " + resourceId + ")");
        checkTimedOutLocks();
        return hasWriteLockInternal(resourceId, key);
    }

    private boolean hasWriteLockInternal(Object resourceId, Object key)
    {
        boolean result = false;
        ObjectLocks objectLocks = null;
        synchronized(locktable)
        {
            objectLocks = (ObjectLocks) locktable.get(resourceId);
            if(objectLocks != null)
            {
                LockEntry writer = objectLocks.getWriter();
                if(writer != null)
                {
                    result = writer.isOwnedBy(key);
                }
            }
        }
        return result;
    }

    public boolean hasUpgrade(Object key, Object resourceId)
    {
        if(log.isDebugEnabled()) log.debug("LM.hasUpgrade(tx-" + key + ", " + resourceId + ")");
        return hasWrite(key, resourceId);
    }

    /**
     * @see LockManager#hasRead(Object, Object)
     */
    public boolean hasRead(Object key, Object resourceId)
    {
        if(log.isDebugEnabled()) log.debug("LM.hasRead(tx-" + key + ", " + resourceId + ')');
        checkTimedOutLocks();
        return hasReadLockInternal(resourceId, key);
    }

    private boolean hasReadLockInternal(Object resourceId, Object key)
    {
        boolean result = false;
        ObjectLocks objectLocks = null;
        synchronized(locktable)
        {
            objectLocks = (ObjectLocks) locktable.get(resourceId);
            if(objectLocks != null)
            {
                LockEntry reader = objectLocks.getReader(key);
                if(reader != null || (objectLocks.getWriter() != null && objectLocks.getWriter().isOwnedBy(key)))
                {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     *
     */
    public int lockedObjects()
    {
        return locktable.size();
    }

    private void checkTimedOutLocks()
    {
        if(System.currentTimeMillis() - m_lastCleanupAt > CLEANUP_FREQUENCY)
        {
            removeTimedOutLocks(getLockTimeout());
            m_lastCleanupAt = System.currentTimeMillis();
        }
    }

    /**
     * removes all timed out lock entries from the persistent storage.
     * The timeout value can be set in the OJB properties file.
     */
    private void removeTimedOutLocks(long timeout)
    {
        int count = 0;
        long maxAge = System.currentTimeMillis() - timeout;
        boolean breakFromLoop = false;
        ObjectLocks temp = null;
        synchronized(locktable)
        {
            Iterator it = locktable.values().iterator();
            /**
             * run this loop while:
             * - we have more in the iterator
             * - the breakFromLoop flag hasn't been set
             * - we haven't removed more than the limit for this cleaning iteration.
             */
            while(it.hasNext() && !breakFromLoop && (count <= MAX_LOCKS_TO_CLEAN))
            {
                temp = (ObjectLocks) it.next();
                if(temp.getWriter() != null)
                {
                    if(temp.getWriter().getTimestamp() < maxAge)
                    {
                        // writer has timed out, set it to null
                        temp.setWriter(null);
                        ++timeoutCounterWrite;
                    }
                }
                if(temp.getYoungestReader() < maxAge)
                {
                    // all readers are older than timeout.
                    temp.getReaders().clear();
                    ++timeoutCounterRead;
                    if(temp.getWriter() == null)
                    {
                        // all readers and writer are older than timeout,
                        // remove the objectLock from the iterator (which
                        // is backed by the map, so it will be removed.
                        it.remove();
                    }
                }
                else
                {
                    // we need to walk each reader.
                    Iterator readerIt = temp.getReaders().values().iterator();
                    LockEntry readerLock = null;
                    while(readerIt.hasNext())
                    {
                        readerLock = (LockEntry) readerIt.next();
                        if(readerLock.getTimestamp() < maxAge)
                        {
                            // this read lock is old, remove it.
                            readerIt.remove();
                        }
                    }
                }
                count++;
            }
        }
    }


    //===============================================================
    // inner class
    //===============================================================
    static final class ObjectLocks
    {
        private LockEntry writer;
        private Hashtable readers;
        private long m_youngestReader = 0;

        ObjectLocks()
        {
            this(null);
        }

        ObjectLocks(LockEntry writer)
        {
            this.writer = writer;
            readers = new Hashtable();
        }

        LockEntry getWriter()
        {
            return writer;
        }

        void setWriter(LockEntry writer)
        {
            this.writer = writer;
        }

        Hashtable getReaders()
        {
            return readers;
        }

        void addReader(LockEntry reader)
        {
            /**
             * MBAIRD:
             * we want to track the youngest reader so we can remove all readers at timeout
             * if the youngestreader is older than the timeoutperiod.
             */
            if((reader.getTimestamp() < m_youngestReader) || (m_youngestReader == 0))
            {
                m_youngestReader = reader.getTimestamp();
            }
            this.readers.put(reader.getKey(), reader);
        }

        long getYoungestReader()
        {
            return m_youngestReader;
        }

        LockEntry getReader(Object key)
        {
            return (LockEntry) this.readers.get(key);
        }

        LockEntry removeReader(Object key)
        {
            return (LockEntry) this.readers.remove(key);
        }
    }


    //===============================================================
    // inner class
    //===============================================================
    /**
     * A lock entry encapsulates locking information.
     */
    final class LockEntry implements Serializable
    {
        /**
         * marks a Read Lock.
         */
        static final int LOCK_READ = 0;

        /**
         * marks a Write Lock.
         */
        static final int LOCK_WRITE = 1;

        /**
         * the object to be locked.
         */
        private Object resourceId;

        /**
         * key for locked object
         */
        private Object key;

        /**
         * the timestamp marking the time of acquisition of this lock
         */
        private long timestamp;

        /**
         * the isolationlevel for this lock.
         */
        private int isolationLevel;

        /**
         * marks if this is a read or a write lock.
         * LOCK_READ = 0;
         * LOCK_WRITE = 1;
         */
        private int lockType;

        /**
         * Multiargument constructor for fast loading of LockEntries by OJB.
         */
        public LockEntry(Object resourceId,
                         Object key,
                         long timestamp,
                         int isolationLevel,
                         int lockType)
        {
            this.resourceId = resourceId;
            this.key = key;
            this.timestamp = timestamp;
            this.isolationLevel = isolationLevel;
            this.lockType = lockType;

        }

        /**
         * Returns the resource id of the locked object (or the locked object itself).
         */
        public Object getResourceId()
        {
            return resourceId;
        }

        /**
         * Returns lock key.
         */
        public Object getKey()
        {
            return key;
        }

        /**
         * returns the timestamp of the acqusition of the lock.
         */
        public long getTimestamp()
        {
            return timestamp;
        }

        /**
         * returns the isolation level of this lock
         */
        public int getIsolationLevel()
        {
            return isolationLevel;
        }

        /**
         * returns the locktype of this lock.
         *
         * @return LOCK_READ if lock is a readlock,
         *         LOCK_WRITE if lock is a Write lock.
         */
        public int getLockType()
        {
            return lockType;
        }

        /**
         * sets the locktype of this lockentry.
         *
         * @param locktype LOCK_READ for read, LOCK_WRITE for write lock.
         */
        public void setLockType(int locktype)
        {
            this.lockType = locktype;
        }

        /**
         * Returns true if this lock is owned by the specified key.
         */
        public boolean isOwnedBy(Object key)
        {
            return this.getKey().equals(key);
        }


        /**
         * Sets the isolationLevel.
         *
         * @param isolationLevel The isolationLevel to set
         */
        public void setIsolationLevel(int isolationLevel)
        {
            this.isolationLevel = isolationLevel;
        }

        /**
         * Sets the resourceId.
         *
         * @param resourceId The resourceId to set
         */
        public void setresourceId(String resourceId)
        {
            this.resourceId = resourceId;
        }

        /**
         * Sets the timestamp.
         *
         * @param timestamp The timestamp to set
         */
        public void setTimestamp(long timestamp)
        {
            this.timestamp = timestamp;
        }

        /**
         * Sets the key.
         *
         * @param key The key to set
         */
        public void setKey(Object key)
        {
            this.key = key;
        }
    }
}
