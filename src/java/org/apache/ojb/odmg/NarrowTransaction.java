package org.apache.ojb.odmg;

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

import org.odmg.LockNotGrantedException;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBrokerException;

/**
 * Wraps {@link org.odmg.Transaction} in managed environments.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 */
public class NarrowTransaction implements TransactionExt
{
    private TransactionImpl tx;

    public NarrowTransaction(TransactionImpl tx)
    {
        this.tx = tx;
    }

    public TransactionImpl getRealTransaction()
    {
        return tx;
    }

    public void markDelete(Object anObject)
    {
        this.tx.markDelete(anObject);
    }

    public void markDirty(Object anObject)
    {
        this.tx.markDirty(anObject);
    }

    public void flush()
    {
        tx.flush();
    }

    /**
     * Return associated PB instance, or null if not found.
     */
    public PersistenceBroker getBroker()
    {
        return tx.getBroker();
    }

    /**
     * Not supported!!
     */
    public void join()
    {
        throw new UnsupportedOperationException("Not supported operation");
    }

    /**
     * Not supported!!
     */
    public void leave()
    {
        throw new UnsupportedOperationException("Not supported operation");
    }

    /**
     * Not supported!!
     */
    public void begin()
    {
        throw new UnsupportedOperationException("Not supported operation");
    }

    /**
     * Not supported!!
     */
    public boolean isOpen()
    {
        return tx.isOpen();
    }

    /**
     * Not supported!!
     */
    public void commit()
    {
        throw new UnsupportedOperationException("Not supported operation");
    }

    /**
     * Abort the underlying odmg-transaction
     */
    public void abort()
    {
        tx.abort();
        //throw new UnsupportedOperationException("Not supported operation");
    }

    /**
     * Not supported!!
     */
    public void checkpoint()
    {
        throw new UnsupportedOperationException("Not supported operation");
    }

    /**
     * lock the given object
     * @see org.odmg.Transaction#lock
     */
    public void lock(Object obj, int lockMode)
            throws LockNotGrantedException
    {
        tx.lock(obj, lockMode);
    }

    /**
     * lock the given object if possible
     * @see org.odmg.Transaction#tryLock
     */
    public boolean tryLock(Object obj, int lockMode)
    {
        return tx.tryLock(obj, lockMode);
    }

	public Object getObjectByIdentity(Identity id)
			throws PersistenceBrokerException
	{
		return tx.getObjectByIdentity(id);
	}
    /**
     * @see org.apache.ojb.odmg.TransactionExt#setImplicitLocking(boolean)
     */
    public void setImplicitLocking(boolean value)
    {
    	tx.setImplicitLocking(value);
    }

    /**
     * @see org.apache.ojb.odmg.TransactionExt#isImplicitLocking
     */
    public boolean isImplicitLocking()
    {
        return tx.isImplicitLocking();
    }

    /**
     * @see org.apache.ojb.odmg.TransactionExt#setCascadingDelete(Class, String, boolean)
     */
    public void setCascadingDelete(Class target, String referenceField, boolean doCascade)
    {
        tx.setCascadingDelete(target, referenceField, doCascade);
    }

    /**
     * @see org.apache.ojb.odmg.TransactionExt#setCascadingDelete(Class, boolean) 
     */
    public void setCascadingDelete(Class target, boolean doCascade)
    {
        tx.setCascadingDelete(target, doCascade);
    }

    public boolean isOrdering()
    {
        return tx.isOrdering();
    }

    public void setOrdering(boolean ordering)
    {
        tx.setOrdering(ordering);
    }

//    public boolean isNoteUserOrder()
//    {
//        return tx.isNoteUserOrder();
//    }
//
//    public void setNoteUserOrder(boolean noteUserOrder)
//    {
//        tx.setNoteUserOrder(noteUserOrder);
//    }

    public boolean isDeleted(Identity id)
    {
        return tx.isDeleted(id);
    }
}
