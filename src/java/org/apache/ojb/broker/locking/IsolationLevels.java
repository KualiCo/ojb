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
 * This interface defines the lock isolation level constants used by
 * OJB locking api. It contains numeric constants and literal constants
 * representing all known isolation levels.
 * <p/>
 * NOTE: The lock isolation levels are labeled like the database transaction level but
 * the definition of the levels is different - take care of that.
 *
 * @version $Id: IsolationLevels.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public interface IsolationLevels
{
    /**
     * Numeric constant representing an no-op isolation level.
     * <p/>
     * The lock manager completely ignores locking.
     * <p/>
     * Allows:<br/>
     * all possible concurrent side-effects<br/>
     */
    public final static int IL_NONE = -1;

    /**
     * Numeric constant representing the uncommited read isolation level.
     *  <p/>
     * Obtaining two concurrent write locks on a given object is not
     * allowed. Obtaining read locks is allowed even if
     * another transaction is writing to that object
     * (Thats why this level is also called "dirty reads").
     * <p/>
     * Allows:<br/>
     * Dirty Reads<br/>
     * Non-Repeatable Reads<br/>
     * Phantom Reads<br/>
     */
    public final static int IL_READ_UNCOMMITTED = 2;

    /**
     * Numeric constant representing the commited read isolation level.
     * <p/>
     * Obtaining two concurrent write locks on a given object is not allowed.
     * Obtaining read locks is allowed only if there is no write lock on
     * the given object.
     * <p/>
     * Allows:<br/>
     * Non-Repeatable Reads<br/>
     * Phantom Reads<br/>
     */
    public final static int IL_READ_COMMITTED = 3;

    /**
     * Numeric constant representing the repeatable read isolation level.
     * <p/>
     * As commited reads, but obtaining a write lock on an object that has
     * been locked for reading by another transaction is not allowed.
     * <p/>
     * Allows:<br/>
     * Phantom Reads<br/>
     */
    public final static int IL_REPEATABLE_READ = 5;

    /**
     * Numeric constant representing the serializable transactions isolation level.
     * <p/>
     * As Repeatable Reads, but it is even not allowed to have multiple
     * read locks on a given object.
     * <p/>
     * Allows:<br/>
     * -<br/>
     */
    public final static int IL_SERIALIZABLE = 7;

    /**
     * Numeric constant representing the optimistic locking isolation level.
     * <p/>
     * The lock manager does not perform any pessimistic locking action. Normally
     * it's not needed to declare this isolation level in persistent object metadata,
     * because OJB will automatically detect an enabled optimistic locking.
     * <br/>
     * NOTE: Usage of this isolation level needs an specific optimistic locking
     * declaration for the specified object. This declaration is <strong>not</strong>
     * automatically handled by OJB and need setting of configuration properties - see OJB docs.
     */
    public final static int IL_OPTIMISTIC = 4;

    /**
     * Numeric constant representing the default isolation level used by
     * OJB - current used default level is {@link #IL_READ_UNCOMMITTED}.
     */
    public final static int IL_DEFAULT = IL_READ_UNCOMMITTED;

    /**
     * Literal constant representing the uncommited read isolation level.
     */
    public final static String LITERAL_IL_NONE = "none";

    /**
     * Literal constant representing the uncommited read isolation level.
     */
    public final static String LITERAL_IL_READ_UNCOMMITTED = "read-uncommitted";

    /**
     * Literal constant representing the commited read isolation level.
     */
    public final static String LITERAL_IL_READ_COMMITTED = "read-committed";

    /**
     * Literal constant representing the repeatable read isolation level.
     */
    public final static String LITERAL_IL_REPEATABLE_READ = "repeatable-read";

    /**
     * Literal constant representing the serializable transactions isolation level.
     */
    public final static String LITERAL_IL_SERIALIZABLE = "serializable";

    /**
     * Literal constant representing the optimistic locking isolation level.
     */
    public final static String LITERAL_IL_OPTIMISTIC = "optimistic";
}
