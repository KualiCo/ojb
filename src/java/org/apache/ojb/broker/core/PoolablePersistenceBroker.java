package org.apache.ojb.broker.core;

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

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.ojb.broker.PersistenceBrokerInternal;
import org.apache.ojb.broker.util.logging.LoggerFactory;

public class PoolablePersistenceBroker extends DelegatingPersistenceBroker
{
    private KeyedObjectPool pool;

    public PoolablePersistenceBroker(PersistenceBrokerInternal broker, KeyedObjectPool pool)
    {
        super(broker);
        this.pool = pool;
    }

    public boolean close()
    {
        super.close();
        try
        {
            pool.returnObject(this.getPBKey(), this);
            return true;
        }
        catch (Exception e)
        {
            LoggerFactory.getDefaultLogger().error("Unexpected exception when returning instance to pool", e);
            return false;
        }
    }

    public void destroy()
    {
        this.setDelegate(null);
    }
}
