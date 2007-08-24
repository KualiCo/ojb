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
 * Factory class used to obtain the proper {@link LockIsolation} level.
 *
 * @version $Id: LockIsolationManager.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
class LockIsolationManager
{
    private LockIsolation readUncommitedStrategy;
    private LockIsolation readCommitedStrategy;
    private LockIsolation readRepeatableStrategy;
    private LockIsolation serializableStrategy;

    LockIsolationManager()
    {
        readUncommitedStrategy = new ReadUncommittedIsolation();
        readCommitedStrategy = new ReadCommittedIsolation();
        readRepeatableStrategy = new RepeatableReadIsolation();
        serializableStrategy = new SerializableIsolation();
    }

    /**
     * Obtains a lock isolation for Object obj. The Strategy to be used is
     * selected by evaluating the ClassDescriptor of obj.getClass().
     */
    public LockIsolation getStrategyFor(int isolationLevel)
    {
        switch(isolationLevel)
        {
            case LockManager.IL_READ_UNCOMMITTED:
                return readUncommitedStrategy;
            case LockManager.IL_READ_COMMITTED:
                return readCommitedStrategy;
            case LockManager.IL_REPEATABLE_READ:
                return readRepeatableStrategy;
            case LockManager.IL_SERIALIZABLE:
                return serializableStrategy;
            default:
                return readUncommitedStrategy;
        }
    }

    //===============================================
    // inner class, LockIsolation implementation
    //===============================================
    /**
     * The implementation of the Uncommited Reads Locking strategy.
     * This strategy is the loosest of them all.  It says
     * you shouldn't need to get any Read locks whatsoever,
     * but since it will probably try to get them, it will
     * always give it to them.
     * <p/>
     * Allows:
     * Dirty Reads
     * Non-Repeatable Reads
     * Phantom Reads
     */
    class ReadUncommittedIsolation extends LockIsolation
    {
        ReadUncommittedIsolation()
        {
        }

        public int getIsolationLevel()
        {
            return LockManager.IL_READ_UNCOMMITTED;
        }

        public String getIsolationLevelAsString()
        {
            return LockManager.LITERAL_IL_READ_UNCOMMITTED;
        }

        public boolean allowMultipleRead()
        {
            return true;
        }

        public boolean allowWriteWhenRead()
        {
            return true;
        }

        public boolean allowReadWhenWrite()
        {
            return true;
        }
    }


    //===============================================
    // inner class, LockIsolation implementation
    //===============================================
    /**
     * The implementation of the Commited Reads Locking strategy.
     * ReadCommitted - Reads and Writes require locks.
     * <p/>
     * Locks are acquired for reading and modifying the database.
     * Locks are released after reading but locks on modified objects
     * are held until EOT.
     * <p/>
     * Allows:
     * Non-Repeatable Reads,
     * Phantom Reads.
     */
    class ReadCommittedIsolation extends LockIsolation
    {
        ReadCommittedIsolation()
        {
        }

        public int getIsolationLevel()
        {
            return LockManager.IL_READ_COMMITTED;
        }

        public String getIsolationLevelAsString()
        {
            return LockManager.LITERAL_IL_READ_COMMITTED;
        }

        public boolean allowMultipleRead()
        {
            return true;
        }

        public boolean allowWriteWhenRead()
        {
            return true;
        }

        public boolean allowReadWhenWrite()
        {
            return false;
        }
    }


    //===============================================
    // inner class, LockIsolation implementation
    //===============================================
    /**
     * The implementation of the Repeatable Reads Locking strategy.
     * Locks are obtained for reading and modifying the database.
     * Locks on all modified objects are held until EOT.
     * Locks obtained for reading data are held until EOT.
     * Allows:
     * Phantom Reads
     */
    class RepeatableReadIsolation extends LockIsolation
    {
        public RepeatableReadIsolation()
        {
        }

        public int getIsolationLevel()
        {
            return LockManager.IL_REPEATABLE_READ;
        }

        public String getIsolationLevelAsString()
        {
            return LockManager.LITERAL_IL_REPEATABLE_READ;
        }

        public boolean allowMultipleRead()
        {
            return true;
        }

        public boolean allowWriteWhenRead()
        {
            return false;
        }

        public boolean allowReadWhenWrite()
        {
            return false;
        }
    }


    //===============================================
    // inner class, LockIsolation implementation
    //===============================================
    /**
     * The implementation of the Serializable Locking strategy.
     */
    class SerializableIsolation extends LockIsolation
    {

        SerializableIsolation()
        {
        }

        public int getIsolationLevel()
        {
            return LockManager.IL_SERIALIZABLE;
        }

        public String getIsolationLevelAsString()
        {
            return LockManager.LITERAL_IL_SERIALIZABLE;
        }

        public boolean allowMultipleRead()
        {
            return false;
        }

        public boolean allowWriteWhenRead()
        {
            return false;
        }

        public boolean allowReadWhenWrite()
        {
            return false;
        }
    }
}
