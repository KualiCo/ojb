package org.apache.ojb.broker.util.sequence;

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

import org.apache.ojb.broker.accesslayer.JdbcAccess;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;

/**
 * SequenceManagers are responsible for creating new unique
 * ID's - unique accross all "extent" object declarations in OJB metadata.
 * There are some standard sequence manager implementations in
 * this package.
 * <p/>
 * SequenceManager objects are obtained from a factory class called
 * {@link SequenceManagerFactory}.
 * This Factory can be configured to provide instances of user defined
 * implementors of this interface.
 * <p/>
 * NOTE: SequenceManagers should be aware of "extents" ("extent" is an OJB inheritance feature),
 * that is: if you ask for an uid for an Interface (more exact for one implementor class)
 * with several implementor classes, or a baseclass with several subclasses the returned uid
 * should be unique accross all tables representing objects of the extent in question.
 *
 * @version $Id: SequenceManager.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public interface SequenceManager
{
    /**
     * This method is called to get an unique value <strong>before</strong> the object
     * is written to persistent storage.
     * <br/>
     * Returns a unique object for the given field attribute.
     * The returned value takes in account the jdbc-type
     * and the FieldConversion.sql2java() conversion defined for <code>field</code>.
     * The returned object is unique accross all tables of "extent" classes the
     * field belongs to.
     * <br/>
     * Implementations using native identity columns should return a unique
     * incremented counter object for temporary use by OJB.
     */
    public Object getUniqueValue(FieldDescriptor field) throws SequenceManagerException;

    /**
     * This method is called <strong>after</strong> the object was written to the persistent storage.
     * <br/>
     * This is to support native Identity columns (auto_increment columns) on the db side.
     * Other implementations may ignore this method.
     * @param dbAccess Current used {@link org.apache.ojb.broker.accesslayer.JdbcAccess} instance
     * @param cld Descriptor for specified object
     * @param obj The object to associate with identity value
     */
    public void afterStore(JdbcAccess dbAccess, ClassDescriptor cld, Object obj) throws SequenceManagerException;
}
