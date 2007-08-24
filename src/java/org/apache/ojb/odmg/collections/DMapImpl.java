package org.apache.ojb.odmg.collections;

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

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Set;
import java.util.List;

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerAware;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.util.collections.ManageableHashSet;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.odmg.TransactionExt;
import org.apache.ojb.odmg.TransactionImpl;
import org.apache.ojb.odmg.TxManagerFactory;
import org.apache.ojb.odmg.RuntimeObject;
import org.odmg.DMap;
import org.odmg.Transaction;

/**
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: DMapImpl.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */

public class DMapImpl extends AbstractMap implements DMap, Serializable, PersistenceBrokerAware
{
	private static final long serialVersionUID = 7048246616243056480L;
    private transient Logger log;

    private Integer id;
    private Set entries;
    private PBKey pbKey;

    /**
     * DMapImpl constructor comment.
     */
    public DMapImpl()
    {
        this.entries = new ManageableHashSet();
//        if(getTransaction() == null)
//        {
//            throw new TransactionNotInProgressException("Materialization of DCollection instances must be done" +
//                    " within a odmg-tx");
//        }
        getPBKey();
    }

    /**
     * DListImpl constructor comment.
     */
    public DMapImpl(PBKey key)
    {
        this.entries = new ManageableHashSet();
        this.pbKey = key;
    }

    protected Logger getLog()
    {
        if (log == null)
        {
            log = LoggerFactory.getLogger(DMapImpl.class);
        }
        return log;
    }

    protected TransactionImpl getTransaction()
    {
        return TxManagerFactory.instance().getTransaction();
    }

    public PBKey getPBKey()
    {
        if(pbKey == null)
        {
            TransactionExt tx = getTransaction();
            if(tx != null && tx.isOpen())
            {
                pbKey = tx.getBroker().getPBKey();
            }
        }
        return pbKey;
    }

    public void setPBKey(PBKey pbKey)
    {
        this.pbKey = pbKey;
    }

    protected DMapEntry prepareEntry(Object key, Object value)
    {
        return new DMapEntry(this, key, value);
    }

    /**
     * Returns a set view of the mappings contained in this map.  Each element
     * in the returned set is a <tt>Map.Entry</tt>.  The set is backed by the
     * map, so changes to the map are reflected in the set, and vice-versa.
     * If the map is modified while an iteration over the set is in progress,
     * the results of the iteration are undefined.  The set supports element
     * removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not support
     * the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a set view of the mappings contained in this map.
     */
    public Set entrySet()
    {
        return entries;
    }

    /**
     * lazily retrieve the ID of the set, no need to precompute it.
     */
    public Integer getId()
    {
        return id;
    }

    /**
     *
     */
    public Object put(Object key, Object value)
    {

        DMapEntry entry = prepareEntry(key, value);
        boolean ok = entries.add(entry);
        if (ok)
        {
            TransactionImpl tx = getTransaction();
            if ((tx != null) && (tx.isOpen()))
            {
                List regList = tx.getRegistrationList();
                RuntimeObject rt = new RuntimeObject(this, tx);
                tx.lockAndRegister(rt, Transaction.WRITE, false, regList);

                rt = new RuntimeObject(key, tx);
                tx.lockAndRegister(rt, Transaction.READ, regList);

                rt = new RuntimeObject(value, tx);
                tx.lockAndRegister(rt, Transaction.READ, regList);

                rt = new RuntimeObject(entry, tx, true);
                tx.lockAndRegister(rt, Transaction.WRITE, false, regList);
            }
            return null;
        }
        else
        {
            return this.get(key);
        }
    }


    public Object remove(Object key)
    {
        Iterator i = entrySet().iterator();
        DMapEntry correctEntry = null;
        if (key == null)
        {
            while (correctEntry == null && i.hasNext())
            {
                DMapEntry e = (DMapEntry) i.next();
                if (e.getKey() == null)
                    correctEntry = e;
            }
        }
        else
        {
            while (correctEntry == null && i.hasNext())
            {
                DMapEntry e = (DMapEntry) i.next();
                if (key.equals(e.getKey()))
                    correctEntry = e;
            }
        }

        Object oldValue = null;
        if (correctEntry != null)
        {
            oldValue = correctEntry.getValue();
            i.remove();
            TransactionImpl tx = getTransaction();
            if ((tx != null) && (tx.isOpen()))
            {
                tx.deletePersistent(new RuntimeObject(correctEntry, tx));
            }
        }
        return oldValue;
    }


    /**
     * Gets the entries.
     * @return Returns a Set
     */
    public Set getEntries()
    {
        return entries;
    }

    /**
     * Sets the entries.
     * @param entries The entries to set
     */
    public void setEntries(ManageableHashSet entries)
    {
        this.entries = entries;
    }

    /**
     * Sets the id.
     * @param id The id to set
     */
    public void setId(Integer id)
    {
        this.id = id;
    }

    //***************************************************************
    // PersistenceBrokerAware interface
    //***************************************************************

    /**
     * prepare itself for persistence. Each DList entry generates an
     * {@link org.apache.ojb.broker.Identity} for the wrapped persistent
     * object.
     */
    public void beforeInsert(PersistenceBroker broker) throws PersistenceBrokerException
    {
//        for (Iterator it = entries.iterator(); it.hasNext();)
//        {
//            ((DMapEntry)it.next()).prepareForPersistency(broker);
//        }
    }

    /**
     * noop
     */
    public void beforeUpdate(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }

    /**
     * noop
     */
    public void beforeDelete(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }

    /**
     * noop
     */
    public void afterUpdate(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }

    /**
     * noop
     */
    public void afterInsert(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }

    /**
     * noop
     */
    public void afterDelete(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }

    /**
     * noop
     */
    public void afterLookup(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }

}
