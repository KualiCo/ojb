package org.apache.ojb.otm.lock;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.otm.OTMKit;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.lock.wait.LockWaitStrategy;

/**
 *
 * Represents the locks held for an object. The basic assertion is that at any given point
 * in time, there can be multiple readers, but just one writer.
 *
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 *
 */
public class ObjectLock
{
    //////////////////////////////////
    // IVars
    //////////////////////////////////

    private Identity _oid;
    private LockEntry _writer;
    private HashMap _readers = new HashMap();


    ////////////////////////////////////////
    // Constructor
    ////////////////////////////////////////

    public ObjectLock(Identity oid)
    {
        _oid = oid;
    }

    ////////////////////////////////////////
    // Operations
    ////////////////////////////////////////

    public Identity getTargetIdentity()
    {
        return _oid;
    }

    public Transaction getWriter()
    {
        return (_writer == null)? null: _writer.getTx();
    }

    public boolean isReader(Transaction tx)
    {
        return _readers.containsKey(tx);
    }

    public boolean doesReaderExists()
    {
        return (_readers.size() > 0);
    }

    public Collection getReaders()
    {
        return Collections.unmodifiableCollection(_readers.keySet());
    }

    public void readLock(Transaction tx)
    {
        if (!isReader(tx))
        {
            new LockEntry(tx);
        }
    }

    public void writeLock(Transaction tx)
        throws LockingException
    {
        if (getWriter() != tx)
        {
            LockEntry lock = (LockEntry) _readers.get(tx);

            if (lock == null)
            {
                lock = new LockEntry(tx);
            }
            lock.writeLock();
        }
    }

    public void releaseLock(Transaction tx)
    {
        LockEntry lock = (LockEntry)_readers.get(tx);

        if (lock != null)
        {
            lock.release();
        }
    }

    public void waitForTx(Transaction tx)
        throws LockingException
    {
        OTMKit kit = tx.getKit();
        LockWaitStrategy waitStrategy = kit.getLockWaitStrategy();
        waitStrategy.waitForLock(this, tx);
    }

    public boolean isFree()
    {
        return ((_writer == null) && _readers.isEmpty());
    }

    /////////////////////////////////////////
    // Inner classes
    /////////////////////////////////////////

    private class LockEntry
    {
        public Transaction _tx;
        public ArrayList _listeners;

        public LockEntry(Transaction tx)
        {
            _tx = tx;
            _listeners = null;
            _readers.put(_tx, LockEntry.this);
        }

        /**
         *
         * Returns the LockListeners for this entry.
         *
         * @return      ArrayList of LockListeners
         *
         */
        public ArrayList getListeners()
        {
            return _listeners;
        }

        /**
         *
         * Returns the transaction held by this LockEntry.
         *
         * @return      Transaction
         *
         */
        public Transaction getTx()
        {
            return _tx;
        }

        /**
         *
         *  Add a listener to the list of LockListeners. LockListener is notified, when this
         *  LockEntry is released.
         *
         *  @param listener         the LockListener
         *
         */
        public void addListener(LockListener listener)
        {
            if (listener != null)
            {
                if (_listeners == null)
                {
                    _listeners = new ArrayList();
                }

                _listeners.add(listener);
            }
        }

        /**
         *
         *  Make this lock a writer. If a writer is already present, the call will block until
         *  the lock is released by the writer.
         *
         */
        public void writeLock() throws LockingException
        {
            while (true)
            {
                if (_writer != null && _writer._tx != _tx)
                {
                    waitForTx(_tx);
                }

                synchronized (ObjectLock.this)
                {
                    if (_writer == null || _writer._tx == _tx)
                    {
                        _writer = this;
                        return;
                    }
                }
            }
        }

        public void release()
        {
            synchronized (ObjectLock.this)
            {
                if (_writer == this)
                {
                    _writer = null;
                }
            }

            _readers.remove(_tx);

            if (_listeners != null)
            {
                for (Iterator iterator = _listeners.iterator(); iterator.hasNext();)
                {
                  LockListener listener = (LockListener) iterator.next();
                  listener.lockReleased(_tx, _oid);
                }
            }
        }
    }
}
