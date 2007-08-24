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

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.odmg.TransactionImpl;
import org.apache.ojb.odmg.TxManagerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 *
 * We use a HashMap and synchronize blocks of access for a get "check" then put
 * operation. We cannot use the hashtable as you could check in one synchronized call
 * then put in another while a different thread is doing the same thing.
 *
 * @deprecated
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird<a>
 * 		update for use of Hashmap with synchronization.
 * 		implemented timed out lock removal algo.
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * 		original author.
 * @version $Id: InMemoryLockMapImpl.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class InMemoryLockMapImpl implements LockMap
{
	/**
	 * MBAIRD: a LinkedHashMap returns objects in the order you put them in,
	 * while still maintaining an O(1) lookup like a normal hashmap. We can then
	 * use this to get the oldest entries very quickly, makes cleanup a breeze.
	 */
    private HashMap locktable = new HashMap();

    private long m_lastCleanupAt = System.currentTimeMillis();
	private static long CLEANUP_FREQUENCY = 500; // 500 milliseconds.
	private static int MAX_LOCKS_TO_CLEAN = 50;

    /**
     * returns the LockEntry for the Writer of object obj.
     * If now writer exists, null is returned.
     */
    public LockEntry getWriter(Object obj)
    {
        PersistenceBroker broker = getBroker();
        Identity oid = new Identity(obj, broker); 
        return getWriter(oid);
    }

    public LockEntry getWriter(Identity oid)
    {
        checkTimedOutLocks();
        /* TODO: smarter solution in future */
        // fix/workaround
        // When Identity needs new id's we must overgive
        // the a target broker when run with multiple databases
        // using H/L sequence manager
        ObjectLocks objectLocks = null;
        synchronized(locktable)
        {
            objectLocks = (ObjectLocks) locktable.get(oid.toString());
        }
        if (objectLocks == null)
        {
            return null;
        }
        else
        {
            return objectLocks.getWriter();
        }
    }

    /**
     * obtain a PersistenceBroker instance for persistence operations.
     */
    private PersistenceBroker getBroker()
    {
        return TxManagerFactory.instance().getCurrentTransaction().getBroker();
    }

    /**
     * returns a collection of Reader LockEntries for object obj.
     * If no LockEntries could be found an empty Vector is returned.
     */
    public Collection getReaders(Object obj)
    {
    	checkTimedOutLocks();
        Identity oid = new Identity(obj,getBroker());
        return getReaders(oid);
    }

    public Collection getReaders(Identity oid)
    {
        ObjectLocks objectLocks = null;
        synchronized (locktable)
        {
        	objectLocks = (ObjectLocks) locktable.get(oid.toString());
        }
        if (objectLocks == null)
        {
            return new Vector();
        }
        else
        {
            return objectLocks.getReaders().values();
        }
    }

    /**
     * Add a reader lock entry for transaction tx on object obj
     * to the persistent storage.
     */
    public boolean addReader(TransactionImpl tx, Object obj)
    {
        checkTimedOutLocks();

        Identity oid = new Identity(obj,getBroker());
        LockEntry reader = new LockEntry(oid.toString(),
                tx.getGUID(),
                System.currentTimeMillis(),
                LockStrategyFactory.getIsolationLevel(obj),
                LockEntry.LOCK_READ);

        addReaderInternal(reader);
        return true;
    }

    void addReaderInternal(LockEntry reader)
    {
        ObjectLocks objectLocks = null;
        /**
         * MBAIRD: We need to synchronize the get/put so we don't have two threads
         * competing to check if something is locked and double-locking it.
         */
        synchronized (locktable)
        {
        	String oidString = reader.getOidString();
        	objectLocks = (ObjectLocks) locktable.get(oidString);
            if (objectLocks == null)
            {
                objectLocks = new ObjectLocks();
                locktable.put(oidString, objectLocks);
            }
        }
        objectLocks.addReader(reader);
    }

    /**
     * remove a reader lock entry for transaction tx on object obj
     * from the persistent storage.
     */
    public void removeReader(TransactionImpl tx, Object obj)
    {
        checkTimedOutLocks();

        Identity oid = new Identity(obj, getBroker());
        String oidString = oid.toString();
        String txGuid = tx.getGUID();
        removeReaderInternal(oidString, txGuid);
    }

    private void removeReaderInternal(String oidString, String txGuid)
    {
        ObjectLocks objectLocks = null;
        synchronized (locktable)
        {
        	objectLocks = (ObjectLocks) locktable.get(oidString);
        }
        if (objectLocks == null)
        {
            return;
        }
        else
        {
        	/**
        	 * MBAIRD, last one out, close the door and turn off the lights.
        	 * if no locks (readers or writers) exist for this object, let's remove
        	 * it from the locktable.
        	 */
        	synchronized (locktable)
        	{
        		Map readers = objectLocks.getReaders();
        		readers.remove(txGuid);
            	if ((objectLocks.getWriter() == null) && (readers.size() == 0))
            	{
            		locktable.remove(oidString);
            	}
        	}
        }
    }

	void removeReaderByLock(LockEntry lock)
	{
		String oidString = lock.getOidString();
		String txGuid = lock.getTransactionId();
		removeReaderInternal(oidString, txGuid);
	}
	
    /**
     * remove a writer lock entry for transaction tx on object obj
     * from the persistent storage.
     */
    public void removeWriter(LockEntry writer)
    {
        checkTimedOutLocks();

        String oidString = writer.getOidString();
        ObjectLocks objectLocks = null;
        synchronized (locktable)
        {
        	objectLocks = (ObjectLocks) locktable.get(oidString);
        }
        if (objectLocks == null)
        {
            return;
        }
        else
        {
			/**
        	 * MBAIRD, last one out, close the door and turn off the lights.
        	 * if no locks (readers or writers) exist for this object, let's remove
        	 * it from the locktable.
        	 */
        	synchronized (locktable)
        	{
        		Map readers = objectLocks.getReaders();
        		objectLocks.setWriter(null);
        		// no need to check if writer is null, we just set it.
            	if (readers.size() == 0)
            	{
            		locktable.remove(oidString);
            	}
        	}
        }
    }

    /**
     * upgrade a reader lock entry for transaction tx on object obj
     * and write it to the persistent storage.
     */
    public boolean upgradeLock(LockEntry reader)
    {
        checkTimedOutLocks();

        String oidString = reader.getOidString();
        ObjectLocks objectLocks = null;
        synchronized (locktable)
        {
	        objectLocks = (ObjectLocks) locktable.get(oidString);
        }

        if (objectLocks == null)
        {
            return false;
        }
        else
        {
            // add writer entry
            LockEntry writer = new LockEntry(reader.getOidString(),
                    reader.getTransactionId(),
                    System.currentTimeMillis(),
                    reader.getIsolationLevel(),
                    LockEntry.LOCK_WRITE);
            objectLocks.setWriter(writer);
            // remove reader entry
            objectLocks.getReaders().remove(reader.getTransactionId());
            return true;
        }
    }

    /**
     * generate a writer lock entry for transaction tx on object obj
     * and write it to the persistent storage.
     */
    public boolean setWriter(TransactionImpl tx, Object obj)
    {
        checkTimedOutLocks();

        Identity oid = new Identity(obj, tx.getBroker());
        LockEntry writer = new LockEntry(oid.toString(),
                tx.getGUID(),
                System.currentTimeMillis(),
                LockStrategyFactory.getIsolationLevel(obj),
                LockEntry.LOCK_WRITE);
        String oidString = oid.toString();
        setWriterInternal(writer, oidString);
        return true;
    }

    private void setWriterInternal(LockEntry writer, String oidString)
    {
        ObjectLocks objectLocks = null;
        /**
         * MBAIRD: We need to synchronize the get/put so we don't have two threads
         * competing to check if something is locked and double-locking it.
         */
        synchronized (locktable)
        {
            objectLocks = (ObjectLocks) locktable.get(oidString);
            if (objectLocks == null)
            {
                objectLocks = new ObjectLocks();
                locktable.put(oidString, objectLocks);
            }
        }
        objectLocks.setWriter(writer);
    }

	void setWriterByLock(LockEntry writer)
	{
		String oidString = writer.getOidString();
		setWriterInternal(writer, oidString);
	}

    /**
     * check if there is a reader lock entry for transaction tx on object obj
     * in the persistent storage.
     */
    public boolean hasReadLock(TransactionImpl tx, Object obj)
    {
        checkTimedOutLocks();

        Identity oid = new Identity(obj,getBroker());
        String oidString = oid.toString();
		String txGuid = tx.getGUID();
        return hasReadLockInternal(oidString, txGuid);
    }

    private boolean hasReadLockInternal(String oidString, String txGuid)
    {
        ObjectLocks objectLocks = null;
        synchronized (locktable)
        {
        	objectLocks = (ObjectLocks) locktable.get(oidString);
        }
        
        if (objectLocks == null)
        {
            return false;
        }
        else
        {
            
            LockEntry reader = objectLocks.getReader(txGuid);
            if (reader != null)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }

	boolean hasReadLock(LockEntry entry)
	{
		String oidString = entry.getOidString();
		String txGuid = entry.getTransactionId();
		return hasReadLockInternal(oidString,txGuid);
	}

    private void checkTimedOutLocks()
    {
        if (System.currentTimeMillis() - m_lastCleanupAt > CLEANUP_FREQUENCY)
    	{
    		removeTimedOutLocks(AbstractLockStrategy.DEFAULT_LOCK_TIMEOUT);
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
    	synchronized (locktable)
    	{
	        Iterator it = locktable.values().iterator();
	        /**
	         * run this loop while:
	         * - we have more in the iterator
	         * - the breakFromLoop flag hasn't been set
	         * - we haven't removed more than the limit for this cleaning iteration.
	         */
	        while (it.hasNext() && !breakFromLoop && (count <= MAX_LOCKS_TO_CLEAN))
	        {
	        	temp = (ObjectLocks) it.next();
	        	if (temp.getWriter() != null)
	        	{
		        	if (temp.getWriter().getTimestamp() < maxAge)
		        	{
		        		// writer has timed out, set it to null
		        		temp.setWriter(null);
		        	}
	        	}
	        	if (temp.getYoungestReader() < maxAge)
	        	{
	        		// all readers are older than timeout.
	        		temp.getReaders().clear();
	        		if (temp.getWriter() == null)
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
	        		while (readerIt.hasNext())
	        		{
	        			readerLock = (LockEntry) readerIt.next();
	        			if (readerLock.getTimestamp() < maxAge)
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
    
    /* (non-Javadoc)
     * @see org.apache.ojb.broker.util.configuration.Configurable#configure(org.apache.ojb.broker.util.configuration.Configuration)
     */
    public void configure(Configuration pConfig) throws ConfigurationException
    {
        // noop

    }
    
    int getSize()
    {
    	return locktable.size();
    }

}
