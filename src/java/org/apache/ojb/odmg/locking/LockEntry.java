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

import org.apache.ojb.odmg.TransactionImpl;

import java.io.Serializable;

/**
 * a persistent entry for locks. All locks that are hold from
 * transaction on objects are represented by a LockENtry and made
 * persistent to the database.
 * @author Thomas Mahler
 */
public class LockEntry implements Serializable
{
	static final long serialVersionUID = 8060850552557793930L;    /**
     * marks a Read Lock.
     */
    public static int LOCK_READ = 0;

    /**
     * marks a Write Lock.
     */
    public static int LOCK_WRITE = 1;

    /**
     * the unique OID of the object to be locked.
     */
    private String oidString;

    /**
     * the GUID of the transaction that holds the lock
     */
    private String transactionId;

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
    public LockEntry(String oidString,
                     String transactionId,
                     long timestamp,
                     int isolationLevel,
                     int lockType)
    {
        this.oidString = oidString;
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.isolationLevel = isolationLevel;
        this.lockType = lockType;

    }

    /**
     * build a LockEntry from an OID and a Transaction ID
     */
    public LockEntry(String oidString, String transactionId)
    {
        this.oidString = oidString;
        this.transactionId = transactionId;
    }

    /**
     * default constructor
     */
    public LockEntry()
    {
    }

    /**
     * returns the OID STring of the locked object.
     */
    public String getOidString()
    {
        return oidString;
    }

    /**
     * returns the GUID string of the locking transaction.
     */
    public String getTransactionId()
    {
        return transactionId;
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
     * @return LOCK_READ if lock is a readlock,
     * LOCK_WRITE if lock is a Write lock.
     */
    public int getLockType()
    {
        return lockType;
    }

    /**
     * sets the locktype of this lockentry.
     * @param locktype LOCK_READ for read, LOCK_WRITE for write lock.
     */
    public void setLockType(int locktype)
    {
        this.lockType = locktype;
    }

    /**
     * returns true if this lock is owned by transaction tx, else false.
     */
    public boolean isOwnedBy(TransactionImpl tx)
    {
        return this.getTransactionId().equals(tx.getGUID());
    }


    /**
     * Sets the isolationLevel.
     * @param isolationLevel The isolationLevel to set
     */
    public void setIsolationLevel(int isolationLevel)
    {
        this.isolationLevel = isolationLevel;
    }

    /**
     * Sets the oidString.
     * @param oidString The oidString to set
     */
    public void setOidString(String oidString)
    {
        this.oidString = oidString;
    }

    /**
     * Sets the timestamp.
     * @param timestamp The timestamp to set
     */
    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }

    /**
     * Sets the transactionId.
     * @param transactionId The transactionId to set
     */
    public void setTransactionId(String transactionId)
    {
        this.transactionId = transactionId;
    }

}
