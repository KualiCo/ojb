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
 * The listener interface for receiving <code>PersistenceBroker</code>
 * state changes.
 *
 * @author Armin Waibel
 * @version $Id: PBStateListener.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public interface PBStateListener extends PBListener
{
    /**
     * Called after the {@link org.apache.ojb.broker.PersistenceBroker} instance was
     * obtained from the pool.
     * 
     * @param event The event object
     */
    public void afterOpen(PBStateEvent event);

    /**
     * Called before a transaction was started.
     * 
     * @param event The event object
     */
    public void beforeBegin(PBStateEvent event);

    /**
     * Called after a transaction was started.
     * 
     * @param event The event object
     */
    public void afterBegin(PBStateEvent event);

    /**
     * Called before a transaction will be comitted.
     * 
     * @param event The event object
     */
    public void beforeCommit(PBStateEvent event);

    /**
     * Called after a transaction was comitted.
     * 
     * @param event The event object
     */
    public void afterCommit(PBStateEvent event);

    /**
     * Called before a transaction will be rolled back.
     * 
     * @param event The event object
     */
    public void beforeRollback(PBStateEvent event);

    /**
     * Called after a transaction was rolled back.
     * 
     * @param event The event object
     */
    public void afterRollback(PBStateEvent event);

    /**
     * Called before the {@link org.apache.ojb.broker.PersistenceBroker}
     * instance will be returned to the pool.
     * 
     * @param event The event object
     */
    public void beforeClose(PBStateEvent event);
}
