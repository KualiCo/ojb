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

import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.locking.IsolationLevels;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.odmg.TransactionImpl;
import org.apache.ojb.odmg.TxManagerFactory;

/**
 * Factory class used to obtain the proper LockingStrategy for an Object.
 * @author Thomas Mahler & David Dixon-Peugh
 * @deprecated 
 * @version $Id: LockStrategyFactory.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class LockStrategyFactory
{

    /**
     * private constructor: use static methods only.
     *
     */
    private LockStrategyFactory()
    {
    }

    private static LockStrategy readUncommitedStrategy = new ReadUncommittedStrategy();
    private static LockStrategy readCommitedStrategy = new ReadCommittedStrategy();
    private static LockStrategy readRepeatableStrategy = new RepeatableReadStrategy();
    private static LockStrategy serializableStrategy = new SerializableStrategy();
    private static LockStrategy noopStrategy = new NOOPStrategy();


    /**
     * obtains a LockStrategy for Object obj. The Strategy to be used is
     * selected by evaluating the ClassDescriptor of obj.getClass().
     *
     * @return LockStrategy
     */
    public static LockStrategy getStrategyFor(Object obj)
    {
        int isolationLevel = getIsolationLevel(obj);
        switch (isolationLevel)
        {
            case IsolationLevels.IL_READ_UNCOMMITTED:
                return readUncommitedStrategy;
            case IsolationLevels.IL_READ_COMMITTED:
                return readCommitedStrategy;
            case IsolationLevels.IL_REPEATABLE_READ:
                return readRepeatableStrategy;
            case IsolationLevels.IL_SERIALIZABLE:
                return serializableStrategy;
            case IsolationLevels.IL_OPTIMISTIC:
                return noopStrategy;
            case IsolationLevels.IL_NONE:
                return noopStrategy;
            default:
                return readUncommitedStrategy;
        }
    }

    /**
     * determines the isolationlevel of class c by evaluating
     * the ClassDescriptor of obj.getClass().
     *
     * @return int the isolationlevel
     */
    public static int getIsolationLevel(Object obj)
    {
        Class c = ProxyHelper.getRealClass(obj);
        int isolationLevel = IsolationLevels.IL_READ_UNCOMMITTED;

        try
        {
            ClassDescriptor cld = TxManagerFactory.instance().getCurrentTransaction().getBroker().getClassDescriptor(c);
            isolationLevel = cld.getIsolationLevel();
        }
        catch (PersistenceBrokerException e)
        {
            LoggerFactory.getDefaultLogger().error("[LockStrategyFactory] Can't detect locking isolation level", e);
        }
        return isolationLevel;
    }

    static class NOOPStrategy implements LockStrategy
    {
        public boolean readLock(TransactionImpl tx, Object obj)
        {
            return true;
        }

        public boolean writeLock(TransactionImpl tx, Object obj)
        {
            return true;
        }

        public boolean upgradeLock(TransactionImpl tx, Object obj)
        {
            return true;
        }

        public boolean releaseLock(TransactionImpl tx, Object obj)
        {
            return false;
        }

        public boolean checkRead(TransactionImpl tx, Object obj)
        {
            return false;
        }

        public boolean checkWrite(TransactionImpl tx, Object obj)
        {
            return false;
        }
    }
}
