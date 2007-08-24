package org.apache.ojb.broker;

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

import org.apache.ojb.broker.metadata.ClassDescriptor;

/**
 * Builds {@link org.apache.ojb.broker.Identity} objects to identify persistence capable objects within OJB.
 * In many cases the primary key (based on metadata declaration) of an object is known
 * and the whole object should be materialized (e.g. findByPrimaryKey(...) calls).
 * This class make available a bunch of methods help to create {@link org.apache.ojb.broker.Identity}
 * objects based on
 * <ul>
 *    <li>the persistence capable object itself</li>
 *    <li>the primary key values of a persistence capable object</li>
 * </ul>
 * NOTE:
 * <br/>
 * It is possible to create transient {@link Identity} objects for transient,
 * "new created" persistence capable objects. But keep in mind that this transient
 * {@link Identity} object is only valid till the persistence capable object was written
 * to datastore. After this the {@link Identity} have to be renewed by calling
 * <code>IdentityFactory.buildIdentity(...)</code> again (then the transient Identity
 * will be replaced by the persistent Identity).
 *
 * @version $Id: IdentityFactory.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public interface IdentityFactory
{
    /**
     * Build a unique {@link org.apache.ojb.broker.Identity} for the given
     * persistence capable object.
     *
     * @param obj The object to build the {@link Identity} for
     * @return The new <em>Identity</em> object
     */
    Identity buildIdentity(Object obj);

    /**
     * Build a unique {@link org.apache.ojb.broker.Identity} for the given
     * persistence capable object.
     *
     * @param cld The {@link org.apache.ojb.broker.metadata.ClassDescriptor} of the object
     * @param obj The object to build the {@link Identity} for
     * @return The new <em>Identity</em> object.
     */
    Identity buildIdentity(ClassDescriptor cld, Object obj);

    /**
     * Build a unique {@link org.apache.ojb.broker.Identity}
     * for the given primary key values (composite PK's) of a
     * persistence capable object.
     *
     * @param realClass     The class of the associated object
     * @param topLevelClass The top-level class of the associated object
     * @param pkFieldName   The field names of the PK fields
     * @param pkValues      The PK values
     * @return The new <em>Identity</em> object
     */
    Identity buildIdentity(Class realClass, Class topLevelClass, String[] pkFieldName, Object[] pkValues);

    /**
     * Convenience shortcut method for
     * {@link #buildIdentity(java.lang.Class, java.lang.Class, java.lang.String[], java.lang.Object[])}.
     * 
     * @param realClass     The class of the associated object
     * @param pkFieldName   The field names of the PK fields
     * @param pkValues      The PK values
     * @return The new <em>Identity</em> object
     */
    Identity buildIdentity(Class realClass, String[] pkFieldName, Object[] pkValues);

    /**
     * Convenience method for persistent objects with single primary key.
     * NOTE: Do not use for objects with composed PK!
     *
     * @param realClass The class of the associated object
     * @param pkValue   The PK value
     * @return The new <em>Identity</em> object
     * @see #buildIdentity(java.lang.Class, java.lang.String[], java.lang.Object[])
     */
    Identity buildIdentity(Class realClass, Object pkValue);

    /**
     * Create a new {@link Identity} object based on given arguments - NOTE: There
     * will be no check to resolve the order of the PK values. This method expect
     * the correct order based on the declaration of the {@link org.apache.ojb.broker.metadata.FieldDescriptor}
     * in the mapping file.
     *
     * @param realClass     The class of the associated object
     * @param topLevelClass The top-level class of the associated object
     * @param pkValues      The PK values
     * @return The new <em>Identity</em> object
     */
    Identity buildIdentity(Class realClass, Class topLevelClass, Object[] pkValues);
}
