package org.apache.ojb.broker.locking;

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

import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.util.factory.ConfigurableFactory;

/**
 * This factory class creates LockManager instances according
 * to the setting in the OJB properties file.
 */
public class LockManagerHelper
{
    /**
     * Get a {@link org.apache.ojb.odmg.locking.LockManager} instance. The implementation class is
     * configured in the OJB properties file.
     */
    public static LockManager getLockManagerSpecifiedByConfiguration()
    {
        try
        {
            // create the kernel LockManager
            LockManager lm = (new LockManagerFactory()).createNewLockManager();
            lm.setLockTimeout(LockManager.DEFAULT_LOCK_TIMEOUT);
            return lm;
        }
        catch(Exception e)
        {
            throw new OJBRuntimeException("Unexpected failure while start LockManager", e);
        }
    }

    /**
     * Factory to create kernel {@link LockManager} instances.
     */
    private static class LockManagerFactory extends ConfigurableFactory
    {
        protected String getConfigurationKey()
        {
            return "LockManagerClass";
        }

        LockManager createNewLockManager()
        {
            return (LockManager) this.createNewInstance();
        }
    }

    /**
     * Get a {@link org.apache.ojb.odmg.locking.LockManager} instance. The implementation class is
     * configured in the OJB properties file.
     */
    public static LockManager getCommonsLockManager()
    {
        LockManager lm = new LockManagerCommonsImpl();
        lm.setLockTimeout(LockManager.DEFAULT_LOCK_TIMEOUT);
        return lm;
    }
}
