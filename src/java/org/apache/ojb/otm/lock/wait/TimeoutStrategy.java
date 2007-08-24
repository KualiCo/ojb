package org.apache.ojb.otm.lock.wait;

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

import java.util.HashMap;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.lock.LockingException;
import org.apache.ojb.otm.lock.ObjectLock;

public class TimeoutStrategy implements LockWaitStrategy
{

    /**
     * Maps tx to the lock that tx waits for.
     * Is used for deadlock detection
     */
    private static HashMap _waitsFor = new HashMap();

    private long _timeout;

    /**
     * @param timeout the number of milliseconds to wait before throwing exception
     */
    public TimeoutStrategy(long timeout)
    {
        if (timeout <= 0)
        {
            throw new IllegalArgumentException("Illegal timeout value: " + timeout);
        }
        _timeout = timeout;
    }

    /**
     * The default timeout is 30 seconds
     */
    public TimeoutStrategy() 
    {
        this(30000);
    }

    /**
    * @see org.apache.ojb.otm.lock.wait.LockWaitStrategy#waitForLock(ObjectLock, Transaction)
    */
    public void waitForLock(ObjectLock lock, Transaction tx)
        throws LockingException
    {
        Transaction writerTx;
        ObjectLock writerWaitsForLock;

        // test for deadlock
        writerTx = lock.getWriter();
        while (writerTx != null)
        {
            writerWaitsForLock = (ObjectLock) _waitsFor.get(writerTx);
            if (writerWaitsForLock == null)
            {
                break;
            }
            writerTx = writerWaitsForLock.getWriter();
            if (writerTx == null)
            {
                break;
            }
            if (writerTx == tx)
            {
                StringBuffer sb = new StringBuffer();

                // deadlock detected.
                // Now we traverse the cycle once more to provide more info
                writerTx = lock.getWriter();
                sb.append(lock.getTargetIdentity());
                while (writerTx != tx)
                {
                    writerWaitsForLock = (ObjectLock) _waitsFor.get(writerTx);
                    sb.append(" -> ");
                    sb.append(writerWaitsForLock.getTargetIdentity());
                    writerTx = writerWaitsForLock.getWriter();
                }
                throw new DeadlockException(sb.toString());
            }
        }

        // No deadlock detected, then wait the given timeout
        _waitsFor.put(tx, lock);
        try
        {
            long now = System.currentTimeMillis();
            long deadline = System.currentTimeMillis() + _timeout;

            do
            {
                if (lock.getWriter() == null)
                {
                    return;
                }

                try
                {
                    long toSleep = Math.min(deadline - now, 1000);
                    Thread.sleep(toSleep);
                    now += toSleep;
                } catch (InterruptedException ex) {
                    now = System.currentTimeMillis();
                }
            }
            while (now < deadline);

            writerTx = lock.getWriter();

            if (writerTx != null)
            {
                throw new ConcurrentModificationException(
                        "Object [id: " + lock.getTargetIdentity()
                        + "] locked by Transaction " + writerTx);
            }
        }
        finally
        {
            _waitsFor.remove(tx);
        }
    }
}
