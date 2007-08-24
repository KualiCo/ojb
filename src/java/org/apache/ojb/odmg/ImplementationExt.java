package org.apache.ojb.odmg;

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

import org.odmg.Implementation;

/**
 * Offers useful none odmg-standard methods of the odmg {@link org.odmg.Implementation} interface.
 * <p>
 * Note: All listed methods are <strong>not</strong> part of the standard ODMG-api -
 * they are special (proprietary) OJB extensions.
 * </p>
 *
 * @version $Id: ImplementationExt.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public interface ImplementationExt extends Implementation
{
    /**
     * The used collection type class returned by OQL queries.
     *
     * @see org.apache.ojb.odmg.oql.EnhancedOQLQuery#execute()
     * @see org.odmg.OQLQuery#execute()
     * @return The collection class type
     */
    public Class getOqlCollectionClass();

    /**
     * Set the used collection type class returned by OQL queries.
     * <p/>
     * NOTE: Each specified class must implement interface {@link org.apache.ojb.broker.ManageableCollection}
     * to work proper with OJB.
     *
     * @param oqlCollectionClass The collection class used in OQL queries.
     */
    public void setOqlCollectionClass(Class oqlCollectionClass);

    /**
     * If the OJB implicit locking feature (see {@link TransactionExt#setImplicitLocking(boolean)}) is
     * enabled, this define the lock type of all implicit locked objects.
     * <p/>
     * If set to <em>true</em>, acquiring a write-lock on a given object x implies write locks on all
     * implicit locked objects.
     * <br/>
     * If set to <em>false</em>, in any case implicit read-locks are acquired.
     * Acquiring a read- or write lock on x thus allways results in implicit read-locks
     * on all associated objects.
     *
     * @param impliciteWriteLocks If <em>true</em> implicit write locks will enabled.
     */
    public void setImpliciteWriteLocks(boolean impliciteWriteLocks);

    /**
     * Is <em>true</em> when implicite write locks are enabled.
     *
     * @return <em>true</em> when implicit write locks are enabled.
     * @see #setImpliciteWriteLocks(boolean)
     */
    public boolean isImpliciteWriteLocks();

    /**
     * Set the global property <em>implicit locking</em>. This method can be used
     * to activate or deactivate the global implicit
     * locking mechanism.
     * <br/>
     * If set <em>true</em> OJB implicitly locks objects to ODMG transactions
     * after performing OQL queries. Also if implicit locking is used
     * locking objects is recursive, that is associated objects are also
     * locked. If ImplicitLocking is set to 'false', no locks are obtained
     * in OQL queries, lookup objects and there is also no recursive locking.
     * <p/>
     * However it's possible to set this value only for the current used {@link org.odmg.Transaction}
     * using {@link TransactionExt#setImplicitLocking(boolean)} and to detect the implicit locking
     * state of the used transaction instance call {@link TransactionExt#isImplicitLocking()}.
     * <br/>
     * Turning off implicit locking may improve performance but requires
     * additional care to make sure that all changed objects are properly
     * registered to the transaction.
     *
     * @param impliciteLocking If set <em>true</em> implicit locking is enabled,
     *        if <em>false</em>, implicit locking is disabled.
     */
    public void setImplicitLocking(boolean impliciteLocking);

    /**
     * Returns <em>true</em> if the global implicit locking is enabled
     * for this {@link org.odmg.Implementation} instance, else <em>false</em>.
     * <br/>
     * <strong>Important:</strong> The returned value is the global used setting for all
     * {@link org.odmg.Transaction#lock(Object, int)} calls.
     * <br/>
     * However it's possible to set this value only for the current used {@link org.odmg.Transaction}
     * using {@link TransactionExt#setImplicitLocking(boolean)} and to detect the implicit locking
     * state of the used transaction instance call {@link TransactionExt#isImplicitLocking()}.
     *
     * @return <em>true</em> if the global property <em>implicit locking</em><em>true</em> is enabled.
     * @see #setImplicitLocking(boolean)
     */
    public boolean isImplicitLocking();

    /**
     * Returns <em>true</em> if OJB's ordering algorithm is enabled.
     *
     * @see #setOrdering(boolean)
     */
    public boolean isOrdering();

    /**
     * Disable/enable OJB's ordering algorithm when insert, update, delete a
     * bunch of objects within a transaction. The ordering algorithm try to
     * calculate the correct order of the modified/new persistent objects to
     * prevent problems on commit of the transaction.
     * <br/>
     * If the used databases support 'deferred checks' it's recommended to
     * use this feature and to disable OJB's object ordering.
     * <p/>
     * However it's possible to set this value only for the current
     * used {@link org.odmg.Transaction} using {@link TransactionExt#setOrdering(boolean)}
     *
     * @param ordering If <em>true</em> OJB's ordering algorithm is used.
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
//     * However it's possible to set this value only for the current
//     * used {@link org.odmg.Transaction} using {@link TransactionExt#setNoteUserOrder(boolean)}
//     * <p/>
//     * <strong>NOTE:</strong> If OJB's ordering algorithm (see
//     * {@link #setOrdering(boolean)}) is enabled, the
//     * order of objects may change on commit.
//     *
//     * @param noteUserOrder If <em>true</em> the order of persisting
//     * method calls determine the order of objects.
//     */
//    public void setNoteUserOrder(boolean noteUserOrder);


//    /**
//     * Get object by OJB's {@link org.apache.ojb.broker.Identity}.
//     *
//     * @param id The identity of the object to look for.
//     * @return The matching object or <em>null</em>.
//     */
//    public Object getObjectByIdentity(Identity id);

//    /**
//     * If set <em>true</em> the odmg implementation do it's best to find out the user intension, if set
//     * <em>false</em> OJB use an optimized mode and the user has to adhere strictly the odmg-api:
//     * <ul>
//     * <li>
//     * New objects can only be made persistent by using {@link org.odmg.Database#makePersistent(Object)}
//     * </li>
//     * <li>
//     * Only persistent objects can be locked with {@link org.odmg.Transaction#lock(Object, int)}.
//     * </li>
//     * <li>
//     * When deleting an object with {@link org.odmg.Database#deletePersistent(Object)} to reuse it
//     * within a transaction a call to {@link org.odmg.Database#makePersistent(Object)} is needed and
//     * field changes on objects marked as "deleted" are not allowed.
//     * </li>
//     * </ul>
//     * When running odmg in <em>safe-mode</em> these restrictions are "softened" and it's e.g. possible
//     * to persist new objects with {@link org.odmg.Transaction#lock(Object, int)}.
//     * <p/>
//     * The <em>optimized-mode</em> show a significant better performance, but needs strictness in using the API.
//     *
//     * @param safeMode Set <em>true</em> to enable the <em>safe-mode</em>, use <em>false</em> to enable
//     *                 the <em>optimized-mode</em>.
//     */
//    void setSafeMode(boolean safeMode);
//
//    /**
//     * Returns <em>true</em> if this class use the safe-mode for
//     * user interaction, else the optimized-mode is used.
//     */
//    boolean isSafeMode();
}
