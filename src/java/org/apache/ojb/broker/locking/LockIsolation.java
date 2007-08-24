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
 * This interface defines method that a Locking Strategy must implement
 * according to the isolation level it represents.
 */
abstract class LockIsolation
{
    /**
     * Returns the isolation level identity.
     * @return The isolation level number.
     */
    abstract int getIsolationLevel();

    /**
     * Returns the isolation level identity as string.
     * @return The isolation level as string.
     */
    abstract String getIsolationLevelAsString();

    /**
     * Decide if this lock strategy allows multiple read locks.
     *
     * @return <em>True</em> if multiple read locks allowed, else <em>False</em>.
     */
    abstract boolean allowMultipleRead();

    /**
     * Decide if this lock strategy allows a write lock when one or more read
     * locks already exists.
     *
     * @return <em>True</em> if write lock allowed when read lock exist, else <em>False</em>.
     */
    abstract boolean allowWriteWhenRead();

    /**
     * Decide if this lock strategy allows one or more read locks when a write
     * lock already exists.
     *
     * @return <em>True</em> if read locks allowed when write lock exist, else <em>False</em>.
     */
    abstract boolean allowReadWhenWrite();
}
