package org.apache.ojb.broker.metadata.fieldaccess;

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

import java.io.Serializable;

import org.apache.ojb.broker.metadata.MetadataException;

/**
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: PersistentField.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public interface PersistentField extends Serializable
{
	public Class getDeclaringClass();
	public String getName();
	public Class getType();

	/**
	 * Sets the field represented by this PersistentField object on the specified object argument to the specified new value.
	 * The new value is automatically unwrapped if the underlying field has a primitive type.
	 * This implementation invokes set() on its underlying Field object if the argument <b>is not null</b>.
	 * OBS IllegalArgumentExceptions are wrapped as PersistenceBrokerExceptions.
	 *
     * @param obj The target object (no proxy objects allowed).
     * @param value The value to set.
	 * @throws MetadataException if there is an error setting this field value on obj
	 * @see java.lang.reflect.Field
	 */
	public void set(Object obj, Object value) throws MetadataException;

	/**
	 * Returns the value of the field represented by this PersistentField, on the specified object.
	 * This implementation invokes get() on its underlying Field object.
	 *
	 * @param anObject - The object instance (proxy objects are not allowed here) which we are
     * trying to get the field value from.
	 * @throws MetadataException if there is an error getting this field value from obj
	 * @see java.lang.reflect.Field
	 */
	public Object get(Object anObject) throws MetadataException;

	public boolean usesAccessorsAndMutators();
}
