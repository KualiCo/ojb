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

/**
 * This interface defines a protocol for persistent objects that want to be aware
 * of the operations of the persistence broker. It defines callback methods that
 * allows implementors to interact with persistence operations.
 * <br/>
 * Non persistent objects could use the {@link PBLifeCycleListener} interface
 * to be notified of persistence broker operations.
 *
 * @author Thomas Mahler
 * @version $Id: PersistenceBrokerAware.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public interface PersistenceBrokerAware
{
    /**
     * Is called as the first operation before an object is updated in the underlying
     * persistence system.
     * 
     * @param broker The persistence broker performing the persistence operation
     */
    public void beforeUpdate(PersistenceBroker broker) throws PersistenceBrokerException;

    /**
     * Is called as the last operation after an object was updated in the underlying
     * persistence system.
     * 
     * @param broker The persistence broker performed the persistence operation
     */
    public void afterUpdate(PersistenceBroker broker) throws PersistenceBrokerException;

    /**
     * Is called as the first operation before an object is inserted into the underlying
     * persistence system.
     * 
     * @param broker The persistence broker performing the persistence operation
     */
    public void beforeInsert(PersistenceBroker broker) throws PersistenceBrokerException;

    /**
     * Is called as the last operation after an object was inserted into the underlying
     * persistence system.
     * 
     * @param broker The persistence broker performing the persistence operation
     */
    public void afterInsert(PersistenceBroker broker) throws PersistenceBrokerException;

    /**
     * Is called as the first operation before an object is deleted in the underlying
     * persistence system.
     * 
     * @param broker The persistence broker performing the persistence operation
     */
    public void beforeDelete(PersistenceBroker broker) throws PersistenceBrokerException;

    /**
     * Is called as the last operation after an object was deleted in the underlying
     * persistence system.
     * 
     * @param broker The persistence broker performing the persistence operation
     */
    public void afterDelete(PersistenceBroker broker) throws PersistenceBrokerException;

    /**
     * Is called as the last operation after an object was retrieved from the underlying
     * persistence system via a call to the <code>getObjectByXXX()</code> or
     * <code>getCollectionByXXX()</code>/<code>getIteratorByXXX()</code> methods in
     * {@link PersistenceBroker}.
     * 
     * @param broker The persistence broker performing the persistence operation
     */
    public void afterLookup(PersistenceBroker broker) throws PersistenceBrokerException;
}
