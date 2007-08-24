package org.apache.ojb.odmg;

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

import org.odmg.Transaction;
import org.apache.ojb.broker.Identity;

/**
 * Offers useful none odmg-standard methods of the odmg {@link org.odmg.Transaction} interface.
 * <p>
 * Note: All listed methods are <strong>not</strong> part of the standard ODMG-api -
 * they are special (proprietary) OJB extensions.
 * </p>
 *
 * @version $Id: TransactionExt.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public interface TransactionExt extends Transaction, HasBroker
{
    /**
     * Marks an object for deletion without
     * locking the object. If the object wasn't locked before,
     * OJB will ask for a WRITE lock at commit.
     *
     * @param  anObject Object to be marked
     */
    public void markDelete(Object anObject);

    /**
     * Marks an object as dirty without
     * locking the object. If the object wasn't locked before,
     * OJB will ask for a WRITE lock at commit.
     *
     * @param  anObject Object to be marked
     */
    public void markDirty(Object anObject);

    /**
     * <p>
     * Calling <code>flush</code> flushes persistent object modifications
     * made within the ODMG transaction since the last checkpoint to the underlying
     * database transaction, but does <b<not</b> commit the database transaction.
     * The ODMG transaction retains all locks it held on those objects at the time the flush
     * was invoked.
     * <p/>
     * This method is very similair to {@link org.odmg.Transaction#checkpoint}.
     */
    public void flush();

    /**
     * This method can be used to activate or deactivate the implicit
     * locking mechanism for the current transaction.
     * <br/>
     * If set <em>true</em> OJB implicitly locks objects to ODMG transactions
     * after performing OQL queries. Also if implicit locking is used
     * locking objects is recursive, that is associated objects are also
     * locked. If ImplicitLocking is set to 'false', no locks are obtained
     * in OQL queries, lookup objects and there is also no recursive locking.
     * <p/>
     * Turning off implicit locking may improve performance but requires
     * additional care to make sure all changed objects are properly
     * registered to the transaction.
     *
     * @param value If set <em>true</em> implicit locking is enabled,
     *        if <em>false</em>, implicit locking is disabled.
     * @see ImplementationExt#setImplicitLocking(boolean)
     **/
    public void setImplicitLocking(boolean value);

    /**
     * Returns <em>true</em> if implicite locking is enabled.
     * @see #setImplicitLocking(boolean)
     */
    public boolean isImplicitLocking();

    /**
     * Allows to change the <em>cascading delete</em> behavior of the target class's
     * reference field while this transaction is in use.
     *
     * @param target The class to change cascading delete behavior of the references.
     * @param referenceField The field name of the 1:1, 1:n or m:n reference.
     * @param doCascade If <em>true</em> cascading delete is enabled, <em>false</em> disabled.
     */
    public void setCascadingDelete(Class target, String referenceField, boolean doCascade);

    /**
     * Allows to change the <em>cascading delete</em> behavior of all references of the
     * specified class while this transaction is in use.
     *
     * @param target The class to change cascading delete behavior of all references.
     * @param doCascade If <em>true</em> cascading delete is enabled, <em>false</em> disabled.
     */
    public void setCascadingDelete(Class target, boolean doCascade);

    /**
     * Return <em>true</em> if the OJB ordering algorithm is enabled.
     * @see #setOrdering(boolean)
     */
    public boolean isOrdering();

    /**
     * Allows to enable/disable the OJB persistent object ordering algorithm. If
     * <em>true</em> OJB try to order the modified/new/deleted objects in a correct order
     * (based on a algorithm) before the objects are written to the persistent storage.
     * <br/>
     * If the used databases support 'deferred checks' it's recommended to
     * use this feature and to disable OJB's object ordering.
     * <p/>
     * If <em>false</em> the order of the objects rely on the order specified by
     * the user and on settings like {@link #setImplicitLocking(boolean)}.
     *
     * @param ordering Set <em>true</em> to enable object ordering on commit.
     * @see ImplementationExt#setOrdering(boolean)
     */
    public void setOrdering(boolean ordering);

//    /**
//     * Returns whether or not the persistent method calls determine
//     * the persistent object order on commit.
//     *
//     * @see #setNoteUserOrder(boolean)
//     */
//    public boolean isNoteUserOrder();
//
//    /**
//     * If <em>true</em> the order of persisting method calls like
//     * <br/> - {@link org.odmg.Transaction#lock(Object, int)}).
//     * <br/> - {@link org.odmg.Database#deletePersistent(Object)}).
//     * <br/> - {@link org.odmg.Database#makePersistent(Object)})
//     * determine the order of objects before commit.
//     * <br/>
//     * If <em>false</em> the ordering was determined by OJB's internal
//     * method calls and user calls.
//     * <br/>
//     * However it's possible to set this value as a global property
//     * for all transactions using {@link ImplementationExt#setNoteUserOrder(boolean)}.
//     * <p/>
//     * <strong>NOTE:</strong> If OJB's ordering algorithm (see
//     * {@link #setOrdering(boolean)}) is enabled, the
//     * order of objects may change on commit.
//     *
//     * @param noteUserOrder If <em>true</em> the order of persisting
//     * method calls determine the order of objects.
//     * @see ImplementationExt#setNoteUserOrder(boolean)
//     */
//    public void setNoteUserOrder(boolean noteUserOrder);

    /**
     * Checks if the object with the given {@link org.apache.ojb.broker.Identity}
     * has been deleted within the transaction using
     * {@link org.odmg.Database#deletePersistent(Object)} or {@link #markDelete(Object)}.
     *
     * @param id The identity of the object.
     * @return <em>true</em> if the object has been deleted within the transaction.
     */
    public boolean isDeleted(Identity id);
}
