package org.apache.ojb.odmg.locking;

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

import org.apache.ojb.broker.util.factory.ConfigurableFactory;

/**
 * @deprecated 
 */
public class LockMapFactory
{
    private static LockMap LOCKMAP = null;

    /**
     * get a lockManager instance. The implementation class is
     * configured in the OJB properties file.
     */
    public synchronized static LockMap getLockMap()
    {
        if (LOCKMAP == null)
        {
            LOCKMAP = new Factory().createNewLockMap();
        }
        return LOCKMAP;
    }

    /**
     * Factory to create {@link LockMap} instances
     */
    private static class Factory extends ConfigurableFactory
    {
        protected String getConfigurationKey()
        {
            return "LockMapClass";
        }

        LockMap createNewLockMap()
        {
            return (LockMap) this.createNewInstance();
        }
    }
}
