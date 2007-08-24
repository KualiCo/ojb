package org.apache.ojb.odmg.collections;

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

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerAware;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.odmg.PBCapsule;
import org.apache.ojb.odmg.RuntimeObject;
import org.apache.ojb.odmg.TransactionImpl;
import org.apache.ojb.odmg.TxManagerFactory;
import org.odmg.Transaction;

/**
 * Encapsulates an DList entry object.
 *
 * @version $Id: DListEntry.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class DListEntry implements Serializable, PersistenceBrokerAware
{
    private static final long serialVersionUID = 5251476492626009907L;
    /*
     * declare transient, because ManageableCollection entries need to be {@link java.io.Serializable}.
     */
    private transient Logger log;
    protected transient Object realSubject;
    protected PBKey pbKey;

    protected Integer id;
    protected Integer dlistId;
    protected Identity oid;
    protected int position;

    /**
     * Used to instantiate persistent DLists from DB by the kernel
     * FOR INTERNAL USE ONLY
     */
    public DListEntry()
    {
        /*
        arminw:
        When PB kernel fill DList with DListEntry, the DListEntry needs to know the current
        used PBKey, because we need to lookup the real objects when user iterates the list,
        thus we need the associated PBKey to find right PB/DB connection.
        TODO: Find a better solution
        */
//        if(getTransaction() == null)
//        {
//            throw new TransactionNotInProgressException("Materialization of DCollection instances must be done with a tx");
//        }
        getPBKey();
    }

    /**
     * Standard way to instantiate new entries
     */
    public DListEntry(DListImpl theDList, Object theObject)
    {
        this.dlistId = theDList.getId();
        this.position = theDList.size();
        this.realSubject = theObject;
        this.pbKey = getPBKey();
    }

    protected Logger getLog()
    {
        if(log == null)
        {
            log = LoggerFactory.getLogger(DListEntry.class);
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
            TransactionImpl tx = getTransaction();
            if(tx != null && tx.isOpen())
            {
                pbKey = tx.getBroker().getPBKey();
            }
        }
        return pbKey;
    }

    protected void prepareForPersistency(PersistenceBroker broker)
    {
        if(oid == null)
        {
            if(realSubject == null)
            {
                throw new OJBRuntimeException("Identity and real object are 'null' - Can not persist empty entry");
            }
            else
            {
                oid = broker.serviceIdentity().buildIdentity(realSubject);
            }
        }
    }

    protected void prepareRealSubject(PersistenceBroker broker)
    {
        if(oid == null)
        {
            throw new OJBRuntimeException("can not return real object, real object and Identity is null");
        }
        realSubject = broker.getObjectByIdentity(oid);
    }

    public Object getRealSubject()
    {
        if(realSubject != null)
        {
            return realSubject;
        }
        else
        {
            TransactionImpl tx = getTransaction();
            if(tx != null && tx.isOpen())
            {
                prepareRealSubject(tx.getBroker());
                if(realSubject != null)
                {
                    RuntimeObject rt = new RuntimeObject(realSubject, tx, false);
                    tx.lockAndRegister(rt, Transaction.READ, tx.getRegistrationList());
                }
            }
            else
            {
                PBKey aPbKey = getPBKey();
                if(aPbKey != null)
                {
                    PBCapsule capsule = new PBCapsule(aPbKey, null);
                    try
                    {
                        prepareRealSubject(capsule.getBroker());
                    }
                    finally
                    {
                        capsule.destroy();
                    }
                }
                else
                {
                    getLog().warn("No tx, no PBKey - can't materialise object with Identity " + getOid());
                }
            }
        }
        return realSubject;
    }

    public void setRealSubject(Object realSubject)
    {
        this.realSubject = realSubject;
    }

    public int getPosition()
    {
        return position;
    }

    public void setPosition(int newPosition)
    {
        position = newPosition;
    }

    /**
     * Gets the dlistId.
     *
     * @return Returns a int
     */
    public Integer getDlistId()
    {
        return dlistId;
    }

    /**
     * Sets the dlistId.
     *
     * @param dlistId The dlistId to set
     */
    public void setDlistId(Integer dlistId)
    {
        this.dlistId = dlistId;
    }

    /**
     * Gets the id.
     *
     * @return Returns a int
     */
    public Integer getId()
    {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id The id to set
     */
    public void setId(Integer id)
    {
        this.id = id;
    }

    public Identity getOid()
    {
        return oid;
    }

    public void setOid(Identity oid)
    {
        this.oid = oid;
    }

    /**
     * return String representation.
     */
    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this);
        buf.append("id", id);
        buf.append("dListId", dlistId);
        buf.append("position", position);
        buf.append("identity", oid);
        buf.append("realSubject", realSubject);
        return buf.toString();
    }


    //===================================================
    // PersistenceBrokerAware interface methods
    //===================================================
    public void beforeInsert(PersistenceBroker broker) throws PersistenceBrokerException
    {
        // before insert we have to build the Identity objects of the persistent objects
        // we can't do this ealier, because we can now expect that the persistent object
        // was written to DB (we make sure in code that the persistent object was locked before
        // this entry) and the generated Identity is valid
        prepareForPersistency(broker);
    }

    public void beforeUpdate(PersistenceBroker broker) throws PersistenceBrokerException{}
    public void beforeDelete(PersistenceBroker broker) throws PersistenceBrokerException{}
    public void afterLookup(PersistenceBroker broker) throws PersistenceBrokerException{}
    public void afterDelete(PersistenceBroker broker) throws PersistenceBrokerException{}
    public void afterInsert(PersistenceBroker broker) throws PersistenceBrokerException{}
    public void afterUpdate(PersistenceBroker broker) throws PersistenceBrokerException{}
}
