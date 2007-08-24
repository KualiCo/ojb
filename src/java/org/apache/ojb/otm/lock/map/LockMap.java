package org.apache.ojb.otm.lock.map;

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

import org.apache.ojb.broker.Identity;
import org.apache.ojb.otm.lock.ObjectLock;

/**
 *
 * <javadoc>
 *
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 *
 */
public interface LockMap
{
    
    public ObjectLock getLock(Identity oid);

    /**
     * This is "garbage collection" - remove free locks from 
     * the map. This method is called at the end of each transaction.
     * Maybe that would be better to call it by timer?
     */
    public void gc();

}
