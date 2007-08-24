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

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.transaction.locking.GenericLock;
import org.apache.commons.transaction.locking.GenericLockManager;
import org.apache.commons.transaction.locking.LockException;
import org.apache.commons.transaction.util.LoggerFacade;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * A {@link LockManager} implementation based on apache's commons-transaction
 * locking part.
 * <p/>
 * The timeout of locks is currently (OJB 1.0.2) not supported, maybe
 * in further versions.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: LockManagerCommonsImpl.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class LockManagerCommonsImpl implements LockManager
{
    private Logger log = LoggerFactory.getLogger(LockManagerCommonsImpl.class);

    /**
     * Timeout of the obtained lock.
     */
    private long lockTimeout;
    /**
     * Time to wait when lock call is blocked.
     */
    private long blockTimeout;
    private LoggerFacade logFacade;
    private OJBLockManager lm;

    public LockManagerCommonsImpl()
    {
        logFacade = new LoggerFacadeImpl();
        // default lock timeout
        this.lockTimeout = DEFAULT_LOCK_TIMEOUT;
        // default time to wait for a lock
        this.blockTimeout = DEFAULT_BLOCK_TIMEOUT;
        lm = new OJBLockManager(logFacade, blockTimeout, GenericLockManager.DEFAULT_CHECK_THRESHHOLD);
    }

    private boolean ignore(int isolationLevel)
    {
        return isolationLevel == IsolationLevels.IL_OPTIMISTIC || isolationLevel == IsolationLevels.IL_NONE;
    }

    public long getLockTimeout()
    {
        return lockTimeout;
    }

    public void setLockTimeout(long timeout)
    {
        this.lockTimeout = timeout;
    }

    public long getBlockTimeout()
    {
        return blockTimeout;
    }

    public void setBlockTimeout(long blockTimeout)
    {
        this.blockTimeout = blockTimeout;
    }

    public String getLockInfo()
    {
        String eol = SystemUtils.LINE_SEPARATOR;
        StringBuffer msg = new StringBuffer("Class: " + LockManagerCommonsImpl.class.getName() + eol);
        msg.append("lock timeout: " + getLockTimeout() + " [ms]" + eol);
        msg.append("block timeout: " + getBlockTimeout() + " [ms]" + eol);
        msg.append("commons-tx lock-manger info ==> " + eol);
        msg.append(lm);
        return msg.toString();
    }

    public boolean readLock(Object key, Object resourceId, int isolationLevel)
    {
        return ignore(isolationLevel) ? true : lm.readLock(key, resourceId, new Integer(isolationLevel), blockTimeout);
    }

    public boolean writeLock(Object key, Object resourceId, int isolationLevel)
    {
        return ignore(isolationLevel) ? true : lm.writeLock(key, resourceId, new Integer(isolationLevel), blockTimeout);
    }

    public boolean upgradeLock(Object key, Object resourceId, int isolationLevel)
    {
        return ignore(isolationLevel) ? true : lm.upgradeLock(key, resourceId, new Integer(isolationLevel), blockTimeout);
    }

    public boolean releaseLock(Object key, Object resourceId)
    {
        boolean result = true;
        try
        {
            lm.release(key, resourceId);
        }
        catch(RuntimeException e)
        {
            log.error("Can't release lock for owner key " + key + ", on resource " + resourceId, e);
            result = false;
        }
        return result;
    }

    public void releaseLocks(Object key)
    {
        try
        {
            lm.releaseAll(key);
        }
        catch(RuntimeException e)
        {
            log.error("Can't release all locks for owner key " + key, e);
        }
    }

    public boolean hasRead(Object key, Object resourceId)
    {
        return lm.hasRead(key, resourceId);
    }

    public boolean hasWrite(Object key, Object resourceId)
    {
        return lm.hasWrite(key, resourceId);
    }

    public boolean hasUpgrade(Object key, Object resourceId)
    {
        return lm.hasUpgrade(key, resourceId);
    }


    //===================================================
    // inner class, commons-tx lock manager
    //===================================================
    /**
     * Extension class of {@link CommonsOJBLockManager}
     * which supports additionally convenience methods for read/write/upgrade locks and checks
     * for these locks.
     */
    final class OJBLockManager extends CommonsOJBLockManager
    {
        public OJBLockManager(LoggerFacade logger, long timeoutMSecs, long checkThreshholdMSecs)
                throws IllegalArgumentException
        {
            super(logger, timeoutMSecs, checkThreshholdMSecs);
        }

        private CommonsOJBLockManager.OJBLock lookupLock(Object resourceId)
        {
            return (CommonsOJBLockManager.OJBLock) getLock(resourceId);
        }

        boolean readLock(Object key, Object resourceId, Integer isolationLevel, long timeout)
        {
            /*
            arminw: Not sure what's the best way to go
            - a normal 'lock' call with enabled lock wait time (blocking)
            - or an immediately returning 'tryLock' call.
            E.g. assume the user query for 1000 objects and 100 objects has write locks
            by concurrent threads, then blocking could be counterproductive, because it could
            be that the app will wait seconds for the first read lock, get it, wait seconds
            for the next read lock,.... In the worst case app will wait for all 100 locked objects.
            So I chose the 'tryLock' call for read locks which immediately return.
            */
            int lockLevel = mapLockLevelDependendOnIsolationLevel(isolationLevel, COMMON_READ_LOCK);
            return tryLock(key, resourceId, lockLevel, true, isolationLevel);
        }

        boolean writeLock(Object key, Object resourceId, Integer isolationLevel, long timeout)
        {
            try
            {
                int lockLevel = mapLockLevelDependendOnIsolationLevel(isolationLevel, COMMON_WRITE_LOCK);
                lock(key, resourceId, lockLevel, GenericLock.COMPATIBILITY_REENTRANT,
                        false, timeout, isolationLevel);
                return true;
            }
            catch(LockException e)
            {
                if(log.isEnabledFor(Logger.INFO)) log.info("Can't get write lock for " + key, e);
                return false;
            }
        }

        boolean upgradeLock(Object key, Object resourceId, Integer isolationLevel, long timeout)
        {
            try
            {
                int lockLevel = mapLockLevelDependendOnIsolationLevel(isolationLevel, COMMON_UPGRADE_LOCK);
                lock(key, resourceId, lockLevel, GenericLock.COMPATIBILITY_REENTRANT,
                        false, timeout, isolationLevel);
                return true;
            }
            catch(LockException e)
            {
                if(log.isEnabledFor(Logger.INFO)) log.info("Can't get upgrade lock for " + key, e);
                return false;
            }
        }

        boolean hasRead(Object key, Object resourceId)
        {
            CommonsOJBLockManager.OJBLock lock = lookupLock(resourceId);
            boolean result = false;
            if(lock != null)
            {
                result = lock.hasRead(key);
            }
            return result;
        }

        boolean hasWrite(Object key, Object resourceId)
        {
            CommonsOJBLockManager.OJBLock lock = lookupLock(resourceId);
            boolean result = false;
            if(lock != null)
            {
                result = lock.hasWrite(key);
            }
            return result;
        }

        boolean hasUpgrade(Object key, Object resourceId)
        {
            CommonsOJBLockManager.OJBLock lock = lookupLock(resourceId);
            boolean result = false;
            if(lock != null)
            {
                result = lock.hasUpgrade(key);
            }
            return result;
        }
    }


    //===================================================
    // inner class, logging facade
    //===================================================
    /**
     * Logging facade for apache's commons-transaction.
     */
    final class LoggerFacadeImpl implements LoggerFacade
    {

        public LoggerFacade createLogger(String name)
        {
            return this;
        }

        public void logInfo(String message)
        {
            log.info(message);
        }

        public void logFine(String message)
        {
            log.debug(message);
        }

        public boolean isFineEnabled()
        {
            return log.isDebugEnabled();
        }

        public void logFiner(String message)
        {
            log.debug(message);
        }

        public boolean isFinerEnabled()
        {
            return log.isDebugEnabled();
        }

        public void logFinest(String message)
        {
            log.debug(message);
        }

        public boolean isFinestEnabled()
        {
            return log.isDebugEnabled();
        }

        public void logWarning(String message)
        {
            log.warn(message);
        }

        public void logWarning(String message, Throwable t)
        {
            log.warn(message, t);
        }

        public void logSevere(String message)
        {
            log.error(message);
        }

        public void logSevere(String message, Throwable t)
        {
            log.error(message, t);
        }
    }
}
