package org.apache.ojb.otm;

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
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.otm.copy.ObjectCopyStrategy;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.lock.map.LockMap;
import org.apache.ojb.otm.lock.wait.LockWaitStrategy;
import org.apache.ojb.otm.swizzle.Swizzling;
import org.apache.ojb.otm.transaction.TransactionFactory;

/**
 *
 * OTMKit implementations provide the initial point of entry
 * into the OTM layer.
 *
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 *
 */
public abstract class OTMKit implements Kit
{
    /**
     * Obtain an OTMConnection for the given persistence broker key
     */
    public OTMConnection acquireConnection(PBKey pbKey)
    {
        TransactionFactory txFactory = getTransactionFactory();
        return txFactory.acquireConnection(pbKey);
    }

    /**
     * Obtain the transaction which <code>conn</code> is currently
     * bound to.
     */
    public Transaction getTransaction(OTMConnection conn)
    {
        TransactionFactory txFactory = getTransactionFactory();
        Transaction tx = txFactory.getTransactionForConnection(conn);
        tx.setKit(this);
        return tx;
    }

    ////////////////////////////
    // Abstract Methods
    ////////////////////////////

    protected abstract TransactionFactory getTransactionFactory();

    public abstract Swizzling getSwizzlingStrategy();

    public abstract LockWaitStrategy getLockWaitStrategy();

    public abstract LockMap getLockMap();

    public abstract ObjectCopyStrategy getCopyStrategy(Identity oid);

    /**
     * Should OTM implicitely read lock all objects that are reachable
     * from the explicitely locked object? The updates to the read locked
     * objects are automatically stored to the database at the end
     * of transaction.
     **/
    public abstract boolean isImplicitLockingUsed();

    /**
     * Should OTM verify each inserted object for presence in the database?
     **/
    public abstract boolean isInsertVerified();

    /**
     * Should OTM perform INSERTs for the given object eagerly or during commit?
     **/
    public abstract boolean isEagerInsert(Object obj);

}
