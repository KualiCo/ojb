package org.apache.ojb.otm.kit;

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
import org.apache.ojb.otm.OTMKit;
import org.apache.ojb.otm.copy.*;
import org.apache.ojb.otm.lock.map.InMemoryLockMap;
import org.apache.ojb.otm.lock.map.LockMap;
import org.apache.ojb.otm.lock.wait.LockWaitStrategy;
import org.apache.ojb.otm.lock.wait.TimeoutStrategy;
import org.apache.ojb.otm.swizzle.CopySwizzling;
import org.apache.ojb.otm.swizzle.Swizzling;
import org.apache.ojb.otm.transaction.LocalTransactionFactory;
import org.apache.ojb.otm.transaction.TransactionFactory;

import java.io.Serializable;

/**
 * A base implementation of an OTMKit using local transactions, an
 * in-memory lock map, and metadata based object copying for
 * object swizzling in transactional contexts.
 *
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 */
public class SimpleKit extends OTMKit
{

    private static SimpleKit _instance;

    protected TransactionFactory _txFactory;
    protected Swizzling _swizzlingStrategy;
    protected LockWaitStrategy _lockWaitStrategy;
    protected LockMap _lockMap;
    protected ObjectCopyStrategy _noOpCopyStrategy;
    protected ObjectCopyStrategy _defaultCopyStrategy;
    protected ObjectCopyStrategy _cloneableCopyStrategy;

    /**
     * Constructor for SimpleKit.
     */
    protected SimpleKit()
    {
        super();
        _txFactory = new LocalTransactionFactory();
        _swizzlingStrategy = new CopySwizzling();
        _lockWaitStrategy = new TimeoutStrategy();
        _lockMap = new InMemoryLockMap();
        _noOpCopyStrategy = new NoOpObjectCopyStrategy();
        //_defaultCopyStrategy = new ReflectiveObjectCopyStrategy();
        //_defaultCopyStrategy = new SerializeObjectCopyStrategy();
        _defaultCopyStrategy = new MetadataObjectCopyStrategy();
        _cloneableCopyStrategy = new CloneableObjectCopyStrategy();
    }

    /**
     * Obtain the single instance of SimpleKit
     */
    public static SimpleKit getInstance()
    {
        if (_instance == null)
        {
            _instance = new SimpleKit();
        }
        return _instance;
    }

    ///////////////////////////////////////
    // OTMKit Protocol
    ///////////////////////////////////////

    /**
     * @see org.apache.ojb.otm.OTMKit#getTransactionFactory()
     */
    protected TransactionFactory getTransactionFactory()
    {
        return _txFactory;
    }

    /**
     * @see org.apache.ojb.otm.OTMKit#getSwizzlingStrategy()
     */
    public Swizzling getSwizzlingStrategy()
    {
        return _swizzlingStrategy;
    }


    /**
    * @see org.apache.ojb.otm.OTMKit#getLockWaitStrategy()
    */
    public LockWaitStrategy getLockWaitStrategy()
    {
        return _lockWaitStrategy;
    }

    /**
     * @see org.apache.ojb.otm.OTMKit#getLockMap()
     */
    public LockMap getLockMap()
    {
        return _lockMap;
    }

    /**
     * @see org.apache.ojb.otm.OTMKit#getCopyStrategy(Identity)
     */
    public ObjectCopyStrategy getCopyStrategy(Identity oid)
    {
        Class clazz = oid.getClass();

        if (OjbCloneable.class.isAssignableFrom(clazz))
        {
            return _cloneableCopyStrategy;
        }
        else if (Serializable.class.isAssignableFrom(clazz))
        {
            return _defaultCopyStrategy;
        }
        else
        {
            return _noOpCopyStrategy;
        }
    }

    /**
     * Should OTM implicitely lock all objects that are reachable
     * from the explicitely locked object? The updates to the locked
     * objects are automatically stored to the database at the end
     * of transaction.
     **/
    public boolean isImplicitLockingUsed()
    {
        return true;
    }


    /**
     * Should OTM verify each inserted object for presence in the database?
     **/
    public boolean isInsertVerified()
    {
        return false;
    }
    
    /**
     * Should OTM perform INSERTs for the given object eagerly or during commit?
     **/
    public boolean isEagerInsert(Object obj)
    {
        return false;
    }
}
