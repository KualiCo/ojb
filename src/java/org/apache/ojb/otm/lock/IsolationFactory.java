package org.apache.ojb.otm.lock;

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

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.locking.IsolationLevels;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.otm.lock.isolation.ReadCommittedIsolation;
import org.apache.ojb.otm.lock.isolation.ReadUncommittedIsolation;
import org.apache.ojb.otm.lock.isolation.RepeatableReadIsolation;
import org.apache.ojb.otm.lock.isolation.SerializableIsolation;
import org.apache.ojb.otm.lock.isolation.TransactionIsolation;

/**
 *
 * <javadoc>
 *
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 *
 */
public class IsolationFactory
{

    private static final TransactionIsolation READ_UNCOMMITTED_ISOLATION
        = new ReadUncommittedIsolation();
    private static final TransactionIsolation READ_COMMITTED_ISOLATION
        = new ReadCommittedIsolation();
    private static final TransactionIsolation REPEATABLE_READ_ISOLATION
        = new RepeatableReadIsolation();
    private static final TransactionIsolation SERIALIZABLE_ISOLATION
        = new SerializableIsolation();


    /**
     *
     * Fetches the isolation level of given class from its ClassDescriptor.
     *
     */
    public static TransactionIsolation getIsolationLevel (PersistenceBroker pb,
                                                          ObjectLock lock)
    {
        /*
        arminw: use real object class instead of top-level class
        to match isolation level of given class
        */
        // Class clazz = lock.getTargetIdentity().getObjectsTopLevelClass();
        Class clazz = lock.getTargetIdentity().getObjectsRealClass();
        ClassDescriptor classDescriptor = pb.getClassDescriptor(clazz);
        int isolationLevel = classDescriptor.getIsolationLevel();

        TransactionIsolation isolation = null;
        switch (isolationLevel) {

            case IsolationLevels.IL_READ_UNCOMMITTED:
                isolation = READ_UNCOMMITTED_ISOLATION;
                break;

            case IsolationLevels.IL_READ_COMMITTED:
                isolation = READ_COMMITTED_ISOLATION;
                break;

            case IsolationLevels.IL_REPEATABLE_READ:
                isolation = REPEATABLE_READ_ISOLATION;
                break;

            case IsolationLevels.IL_SERIALIZABLE:
                isolation = SERIALIZABLE_ISOLATION;
                break;

            default:
                throw new UnknownIsolationException(
                    "Isolation level " + isolationLevel + " is not supported");
        }

        return isolation;
    }

}
