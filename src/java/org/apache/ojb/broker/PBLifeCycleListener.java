package org.apache.ojb.broker;

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

/**
 * The listener interface for receiving persistent object
 * life cycle information. This interface is intended for
 * non persistent objects which want to track persistent
 * object life cycle.
 * <br/>
 * NOTE:
 * <br/>
 * Persistent objects should implement the {@link PersistenceBrokerAware}
 * interface to be notified on persistent method calls via callback.
 *
 * @author Armin Waibel
 * @version $Id: PBLifeCycleListener.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public interface PBLifeCycleListener extends PBListener
{
    /**
     * Called before an object will be stored by a persistence broker.
     * 
     * @param event The event object
     */
    public void beforeInsert(PBLifeCycleEvent event) throws PersistenceBrokerException;

    /**
     * Called after an object instance has been stored by a persistence broker.
     * 
     * @param event The event object
     */
    public void afterInsert(PBLifeCycleEvent event) throws PersistenceBrokerException;

    /**
     * Called before an object will be updated by a persistence broker.
     * 
     * @param event The event object
     */
    public void beforeUpdate(PBLifeCycleEvent event) throws PersistenceBrokerException;

    /**
     * Called after an object has been stored by a persistence broker.
     * 
     * @param event The event object
     */
    public void afterUpdate(PBLifeCycleEvent event) throws PersistenceBrokerException;

    /**
     * Called before an object will be deleted by a persistence broker.
     * 
     * @param event The event object
     */
    public void beforeDelete(PBLifeCycleEvent event) throws PersistenceBrokerException;

    /**
     * Called after an object has been deleted by a persistence broker.
     * 
     * @param event The event object
     */
    public void afterDelete(PBLifeCycleEvent event) throws PersistenceBrokerException;

    /**
     * Called after an object has been looked up by a persistence broker.
     * 
     * @param event The event object
     */
    public void afterLookup(PBLifeCycleEvent event) throws PersistenceBrokerException;
}
