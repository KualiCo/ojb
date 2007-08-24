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
import java.util.Iterator;

import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.lock.LockingException;
import org.apache.ojb.otm.lock.ObjectLock;

public class RepeatableReadIsolation extends AbstractIsolation
{

    /**
     * @see org.apache.ojb.otm.lock.isolation.TransactionIsolation#readLock(Transaction, ObjectLock)
     */
    public void readLock(Transaction tx, ObjectLock lock)
        throws LockingException
    {
        Transaction writer = lock.getWriter();
        if (writer == null)
        {
            lock.readLock(tx);
            if (lock.getWriter() != null)
            {
                lock.releaseLock(tx);
                readLock(tx, lock);
            }
        }
        else if (tx != writer)
        {
            lock.waitForTx(writer);
            readLock(tx, lock);
        }
        // else if tx is the writer, it can also read.
    }

    /**
     * @see org.apache.ojb.otm.lock.isolation.TransactionIsolation#writeLock(Transaction, ObjectLock)
     */
    public void writeLock(Transaction tx, ObjectLock lock)
        throws LockingException
    {
        Collection readers = lock.getReaders();
        
        if (!readers.isEmpty())
        {
            for (Iterator it = readers.iterator(); it.hasNext(); )
            {
                Transaction reader = (Transaction) it.next();

                if (reader != tx)
                {
                    lock.waitForTx(reader);
                    writeLock(tx, lock);
                    return;
                }
            }
        }
        
        lock.writeLock(tx);
        readers = lock.getReaders();
        if (readers.size() > 1)
        {
            lock.releaseLock(tx);
            writeLock(tx, lock);
        }
    }
}
