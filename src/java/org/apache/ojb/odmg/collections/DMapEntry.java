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
import java.util.Map.Entry;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerAware;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.odmg.PBCapsule;
import org.apache.ojb.odmg.TransactionExt;
import org.apache.ojb.odmg.TransactionImpl;
import org.apache.ojb.odmg.TxManagerFactory;

/**
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: DMapEntry.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */

public class DMapEntry implements Entry, Serializable, PersistenceBrokerAware
{
    private static final long serialVersionUID = 4382757889982004339L;
    private transient Logger log = LoggerFactory.getLogger(DMapEntry.class);

    private PBKey pbKey;

    private Integer id;
    private Integer dmapId;
    private Identity keyOid;
    private Identity valueOid;

    /* declare transient because the object is not required to be serializable and we can reload it via the oid */
    private transient Object keyRealSubject;
    /* declare transient because the object is not required to be serializable and we can reload it via the oid */
    private transient Object valueRealSubject;

    /**
     * Used to materialize DMaps from the database.
     */
    public DMapEntry()
    {
//        if(getTransaction() == null)
//        {
//            throw new TransactionNotInProgressException("Materialization of DCollection instances must be done with a tx");
//        }
        this.pbKey = getPBKey();
    }

    /**
     * DMapEntry constructor comment.
     */
    public DMapEntry(DMapImpl map, Object key, Object value)
    {
        if(map != null)
        {
            dmapId = map.getId();
        }
        keyRealSubject = key;
        valueRealSubject = value;
        getPBKey();
    }

    protected Logger getLog()
    {
        if(log == null)
        {
            log = LoggerFactory.getLogger(DMapEntry.class);
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
        if(keyOid == null)
        {
            if(keyRealSubject == null)
            {
                throw new OJBRuntimeException("Key identity and real key object are 'null' - Can not persist empty entry");
            }
            else
            {
                keyOid = broker.serviceIdentity().buildIdentity(keyRealSubject);
            }
        }
        if(valueOid == null)
        {
            if(valueRealSubject == null)
            {
                throw new OJBRuntimeException("Key identity and real key object are 'null' - Can not persist empty entry");
            }
            else
            {
                valueOid = broker.serviceIdentity().buildIdentity(valueRealSubject);
            }
        }
    }

    protected void prepareKeyRealSubject(PersistenceBroker broker)
    {
        if(keyOid == null)
        {
            getLog().info("Cannot retrieve real key object because its id is not known");
        }
        else
        {
            keyRealSubject = broker.getObjectByIdentity(keyOid);
        }
    }

    protected void prepareValueRealSubject(PersistenceBroker broker)
    {
        if(valueOid == null)
        {
            getLog().info("Cannot retrieve real key object because its id is not known");
        }
        else
        {
            valueRealSubject = broker.getObjectByIdentity(valueOid);
        }
    }

    /**
     * Returns the real key object.
     */
    public Object getRealKey()
    {
        if(keyRealSubject != null)
        {
            return keyRealSubject;
        }
        else
        {
            TransactionExt tx = getTransaction();

            if((tx != null) && tx.isOpen())
            {
                prepareKeyRealSubject(tx.getBroker());
            }
            else
            {
                if(getPBKey() != null)
                {
                    PBCapsule capsule = new PBCapsule(getPBKey(), null);

                    try
                    {
                        prepareKeyRealSubject(capsule.getBroker());
                    }
                    finally
                    {
                        capsule.destroy();
                    }
                }
                else
                {
                    getLog().warn("No tx, no PBKey - can't materialise key with Identity " + getKeyOid());
                }
            }
        }
        return keyRealSubject;
    }

    /* (non-Javadoc)
     * @see java.util.Map.Entry#getKey()
     */
    public Object getKey()
    {
        // we don't save the key object itself but only its identiy
        // so we now might have to load the object from the db
        if((keyRealSubject == null))
        {
            return getRealKey();
        }
        else
        {
            return keyRealSubject;
        }
    }

    /**
     * Returns the real value object.
     */
    public Object getRealValue()
    {
        if(valueRealSubject != null)
        {
            return valueRealSubject;
        }
        else
        {
            TransactionExt tx = getTransaction();

            if((tx != null) && tx.isOpen())
            {
                prepareValueRealSubject(tx.getBroker());
            }
            else
            {
                if(getPBKey() != null)
                {
                    PBCapsule capsule = new PBCapsule(getPBKey(), null);

                    try
                    {
                        prepareValueRealSubject(capsule.getBroker());
                    }
                    finally
                    {
                        capsule.destroy();
                    }
                }
                else
                {
                    getLog().warn("No tx, no PBKey - can't materialise value with Identity " + getKeyOid());
                }
            }
        }
        return valueRealSubject;
    }

    /* (non-Javadoc)
     * @see java.util.Map.Entry#getValue()
     */
    public Object getValue()
    {
        // we don't save the value object itself but only its identiy
        // so we now might have to load the object from the db
        if((valueRealSubject == null))
        {
            return getRealValue();
        }
        else
        {
            return valueRealSubject;
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map.Entry#setValue(java.lang.Object)
     */
    public Object setValue(Object obj)
    {
        Object old = valueRealSubject;

        valueRealSubject = obj;
        return old;
    }

    /**
     * Gets the dmapId.
     *
     * @return Returns a int
     */
    public Integer getDmapId()
    {
        return dmapId;
    }

    /**
     * Sets the dmapId.
     *
     * @param dmapId The dmapId to set
     */
    public void setDmapId(Integer dmapId)
    {
        this.dmapId = dmapId;
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

    public Identity getKeyOid()
    {
        return keyOid;
    }

    public void setKeyOid(Identity keyOid)
    {
        this.keyOid = keyOid;
    }

    public Identity getValueOid()
    {
        return valueOid;
    }

    public void setValueOid(Identity valueOid)
    {
        this.valueOid = valueOid;
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