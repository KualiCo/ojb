package org.apache.ojb.otm.lock.isolation;

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

import java.util.Collection;

import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.lock.LockingException;
import org.apache.ojb.otm.lock.ObjectLock;

public class SerializableIsolation extends AbstractIsolation
{

    /**
     * @see org.apache.ojb.otm.lock.isolation.TransactionIsolation#readLock(Transaction, ObjectLock)
     */
    public void readLock (Transaction tx, ObjectLock lock)
        throws LockingException
    {
        Collection readers = lock.getReaders();
        if (readers.isEmpty())
        {
            lock.readLock(tx);
            readers = lock.getReaders();
            if (readers.size() > 1)
            {
                lock.releaseLock(tx);
                readLock(tx, lock);
            }
        }
        else
        {
            Transaction reader = (Transaction) readers.iterator().next();

            if (reader != tx) {
                lock.waitForTx(reader);
            }
        }
    }

    /**
     * @see org.apache.ojb.otm.lock.isolation.TransactionIsolation#writeLock(Transaction, ObjectLock)
     */
    public void writeLock (Transaction tx, ObjectLock lock)
        throws LockingException
    {
        Collection readers = lock.getReaders();
        if (readers.isEmpty())
        {
            readLock(tx, lock);
            writeLock(tx, lock);
        }
        else
        {
            Transaction reader = (Transaction) readers.iterator().next();
            if (reader == tx)
            {
                lock.writeLock(tx);
            }
            else
            {
                lock.waitForTx(reader);
                writeLock(tx, lock);
            }
        }
    }

}
