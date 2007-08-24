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

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.locking.IsolationLevels;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.odmg.TransactionImpl;

/**
 * The odmg lock manager implementation of the {@link LockManager} interface. This class
 * using the OJB kernel locking api {@link org.apache.ojb.broker.locking.LockManager}
 * to manage object locking.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: LockManagerOdmgImpl.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class LockManagerOdmgImpl implements LockManager
{
    private org.apache.ojb.broker.locking.LockManager lm;

    public LockManagerOdmgImpl(org.apache.ojb.broker.locking.LockManager lm)
    {
        this.lm = lm;
    }

    private boolean ignore(int isolationLevel)
    {
        return isolationLevel == IsolationLevels.IL_OPTIMISTIC || isolationLevel == IsolationLevels.IL_NONE;
    }

    public boolean readLock(TransactionImpl tx, Object obj)
    {
        Identity oid = tx.getBroker().serviceIdentity().buildIdentity(obj);
        return readLock(tx, oid, obj);
    }

    public boolean readLock(TransactionImpl tx, Identity oid, Object obj)
    {
        ClassDescriptor cld = tx.getBroker().getClassDescriptor(ProxyHelper.getRealClass(obj));
        int isolationLevel = cld.getIsolationLevel();
        return ignore(isolationLevel) ? true : lm.readLock(tx.getGUID(), oid, isolationLevel);
    }

    public boolean writeLock(TransactionImpl tx, Object obj)
    {
        Identity oid = tx.getBroker().serviceIdentity().buildIdentity(obj);
        return writeLock(tx, oid, obj);
    }

    public boolean writeLock(TransactionImpl tx, Identity oid, Object obj)
    {
        ClassDescriptor cld = tx.getBroker().getClassDescriptor(ProxyHelper.getRealClass(obj));
        int isolationLevel = cld.getIsolationLevel();
        return ignore(isolationLevel) ? true : lm.writeLock(tx.getGUID(), oid, isolationLevel);
    }

    public boolean upgradeLock(TransactionImpl tx, Object obj)
    {
        Identity oid = tx.getBroker().serviceIdentity().buildIdentity(obj);
        return upgradeLock(tx, oid, obj);
    }

    public boolean upgradeLock(TransactionImpl tx, Identity oid, Object obj)
    {
        ClassDescriptor cld = tx.getBroker().getClassDescriptor(ProxyHelper.getRealClass(obj));
        int isolationLevel = cld.getIsolationLevel();
        return ignore(isolationLevel) ? true : lm.upgradeLock(tx.getGUID(), oid, isolationLevel);
    }

    public boolean releaseLock(TransactionImpl tx, Object obj)
    {
        Identity oid = tx.getBroker().serviceIdentity().buildIdentity(obj);
        return releaseLock(tx, oid, obj);
    }

    public boolean releaseLock(TransactionImpl tx, Identity oid, Object obj)
    {
        return lm.releaseLock(tx.getGUID(), oid);
    }

    public boolean checkRead(TransactionImpl tx, Object obj)
    {
        Identity oid = tx.getBroker().serviceIdentity().buildIdentity(obj);
        return checkRead(tx, oid, obj);
    }

    public boolean checkRead(TransactionImpl tx, Identity oid, Object obj)
    {
        return lm.hasRead(tx.getGUID(), oid);
    }

    public boolean checkWrite(TransactionImpl tx, Object obj)
    {
        Identity oid = tx.getBroker().serviceIdentity().buildIdentity(obj);
        return checkWrite(tx, oid, obj);
    }

    public boolean checkWrite(TransactionImpl tx, Identity oid, Object obj)
    {
        return lm.hasWrite(tx.getGUID(), oid);
    }
}
