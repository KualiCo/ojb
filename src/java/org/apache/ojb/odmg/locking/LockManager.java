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
import org.apache.ojb.broker.Identity;

/**
 * This interface declares the functionality of the OJB internal Locking mechanism.
 * A default implementaion LockManagerDefaultImpl is provided. This implementaion
 * keeps distributed locks in the database. The locking mechanisms thus involves a
 * lot of database lookups and writes. For some environments this solution may not
 * be adequate. OJB allows to provide user defined implementations of this interface.
 * To activate a user defined LockManagerDefaultImpl it must be configured in the OJB.properties file.
 *
 *
 * @author thma
 */
public interface LockManager
{
    /**
     * aquires a readlock for transaction tx on object obj.
     * Returns true if successful, else false.
     */
    public abstract boolean readLock(TransactionImpl tx, Object obj);

    /**
     * aquires a readlock for transaction tx on object obj.
     * Returns true if successful, else false.
     */
    public abstract boolean readLock(TransactionImpl tx, Identity oid, Object obj);


    /**
     * aquires a writelock for transaction tx on object obj.
     * Returns true if successful, else false.
     */
    public abstract boolean writeLock(TransactionImpl tx, Object obj);

    /**
     * aquires a writelock for transaction tx on object obj.
     * Returns true if successful, else false.
     */
    public abstract boolean writeLock(TransactionImpl tx, Identity oid, Object obj);


    /**
     * upgrades readlock for transaction tx on object obj to a writelock.
     * If no readlock existed a writelock is acquired anyway.
     * Returns true if successful, else false.
     */
    public abstract boolean upgradeLock(TransactionImpl tx, Object obj);

    /**
     * upgrades readlock for transaction tx on object obj to a writelock.
     * If no readlock existed a writelock is acquired anyway.
     * Returns true if successful, else false.
     */
    public abstract boolean upgradeLock(TransactionImpl tx, Identity oid, Object obj);


    /**
     * releases a lock for transaction tx on object obj.
     * Returns true if successful, else false.
     */
    public abstract boolean releaseLock(TransactionImpl tx, Object obj);

    /**
     * releases a lock for transaction tx on object obj.
     * Returns true if successful, else false.
     */
    public abstract boolean releaseLock(TransactionImpl tx, Identity oid, Object obj);


    /**
     * checks if there is a readlock for transaction tx on object obj.
     * Returns true if so, else false.
     */
    public abstract boolean checkRead(TransactionImpl tx, Object obj);

    /**
     * checks if there is a readlock for transaction tx on object obj.
     * Returns true if so, else false.
     */
    public abstract boolean checkRead(TransactionImpl tx, Identity oid, Object obj);


    /**
     * checks if there is a writelock for transaction tx on object obj.
     * Returns true if so, else false.
     */
    public abstract boolean checkWrite(TransactionImpl tx, Object obj);

    /**
     * checks if there is a writelock for transaction tx on object obj.
     * Returns true if so, else false.
     */
    public abstract boolean checkWrite(TransactionImpl tx, Identity oid, Object obj);
}
