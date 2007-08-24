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

import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerInternal;

public class PersistenceBrokerHandle extends DelegatingPersistenceBroker
{
    /**
     * Constructor for the handle, set itself in
     * {@link PersistenceBrokerThreadMapping#setCurrentPersistenceBroker}
     */
    public PersistenceBrokerHandle(final PersistenceBrokerInternal broker)
    {
        super(broker);
        PersistenceBrokerThreadMapping.setCurrentPersistenceBroker(broker.getPBKey(), this);
    }

    public boolean isClosed()
    {
        return super.isClosed();
    }

    public boolean isInTransaction() throws PersistenceBrokerException
    {
        return !isClosed() && super.isInTransaction();
    }

    /**
     * Destroy this handle and return the underlying (wrapped) PB instance
     * to pool (when using default implementation of PersistenceBrokerFactory),
     * unset this instance from {@link PersistenceBrokerThreadMapping}.
     */
    public boolean close()
    {
        if (getDelegate() == null) return true;
        try
        {
            PersistenceBrokerThreadMapping.unsetCurrentPersistenceBroker(getPBKey(), this);
            super.close();
        }
        finally
        {
            /*
            here we destroy the handle. set the
            underlying PB instance 'null', thus it's not
            possible to corrupt a closed PB instance.
            */
            setDelegate(null);
        }
        return true;
    }
}
