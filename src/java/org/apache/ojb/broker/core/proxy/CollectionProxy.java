package org.apache.ojb.broker.core.proxy;

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
 * Interface which Collection proxies need to implement to be
 * treated like collection proxies in ODMG.
 * <p> 
 * Presently the collection proxy impl class can be plugged in and
 * not implement this interface, but those implementations will
 * *not* be treated as proxies by OJB
 */
public interface CollectionProxy
{
    /**
     * Adds a listener to this collection.
     *
     * @param listener The listener to add
     */
    void addListener(CollectionProxyListener listener);

    /**
     * Removes the given listener from this collecton.
     *
     * @param listener The listener to remove
     */
    void removeListener(CollectionProxyListener listener);

    /**
     * Determines whether the collection data already has been loaded from the database.
     *
     * @return <code>true</code> if the data is already loaded
     */
    public boolean isLoaded();
}
