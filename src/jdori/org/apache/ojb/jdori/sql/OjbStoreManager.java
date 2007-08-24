package org.apache.ojb.jdori.sql;
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

import java.util.BitSet;
import java.util.Iterator;

import javax.jdo.Extent;
import javax.jdo.JDOFatalInternalException;
import javax.jdo.JDOUserException;
import javax.jdo.spi.PersistenceCapable;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.util.ObjectModificationDefaultImpl;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

import com.sun.jdori.Connector;
import com.sun.jdori.FieldManager;
import com.sun.jdori.PersistenceManagerInternal;
import com.sun.jdori.StateManagerInternal;
import com.sun.jdori.StoreManager;
import com.sun.jdori.common.query.BasicQueryResult;
import com.sun.jdori.model.jdo.JDOClass;
import com.sun.jdori.query.QueryResult;
import com.sun.jdori.query.QueryResultHelper;


/**
 * StoreManager represents the datastore to the rest of the JDO components.
 * It provides the means to write and read instances, to get the extent of
 * classes, and to get the object id for a persistence capable object.
 *
 * @author Thomas Mahler
 */
class OjbStoreManager implements StoreManager
{
    private final OjbStorePMF pmf;
    private final OjbStoreConnector connector;

    private boolean optimistic;

    /** the logger used for debugging*/
    private Logger logger = LoggerFactory.getLogger("JDO");
    
    OjbStoreManager(OjbStorePMF pmf)
    {
        this.pmf = pmf;
        this.connector = new OjbStoreConnector(pmf);
    }


    /**
     * @see com.sun.jdori.StoreManager#getConnector
     */
    public Connector getConnector()
    {
        return connector;
    }

    /**
     * @see com.sun.jdori.StoreManager#getConnector(String userid,
     * String password)
     */
    public Connector getConnector(String userid, String password)
    {
        throw new JDOUserException("Not implemented"); 
    }

    /**
    * @see com.sun.jdori.StoreManager#insert(BitSet, BitSet, StateManagerInternal)
    */
    public synchronized int insert(
        BitSet loadedFields,
        BitSet dirtyFields,
        StateManagerInternal sm)
    {

        try
        {
            logger.debug("OjbStoreManager.insert");
            PersistenceBroker broker = connector.getBroker();
            Object instance = sm.getObject();
            broker.store(instance, ObjectModificationDefaultImpl.INSERT);
        }
        catch (Exception ex)
        {
            throw new OjbStoreFatalInternalException(getClass(), "insert", ex); 
        }
        dirtyFields.xor(dirtyFields);
        return StateManagerInternal.FLUSHED_COMPLETE;
    }

    /**
    * @see com.sun.jdori.StoreManager#update(BitSet, BitSet, StateManagerInternal)
    */
    public synchronized int update(
        BitSet loadedFields,
        BitSet dirtyFields,
        StateManagerInternal sm)
    {

        try
        {
        	logger.debug("OjbStoreManager.update");
            PersistenceBroker broker = connector.getBroker();
            fetch(sm, null);
            Object instance = sm.getObject();
            broker.store(instance, ObjectModificationDefaultImpl.UPDATE);
        }
        catch (Exception ex)
        {
            throw new OjbStoreFatalInternalException(getClass(), "update", ex); 
        }
        dirtyFields.xor(dirtyFields);
        return StateManagerInternal.FLUSHED_COMPLETE;
    }

    /**
     * @see com.sun.jdori.StoreManager#verifyFields(BitSet, BitSet, StateManagerInternal)
     */
    public synchronized int verifyFields(
        BitSet ignoredFields,
        BitSet fieldsToVerify,
        StateManagerInternal sm)
    {
        fieldsToVerify.xor(fieldsToVerify);
        return StateManagerInternal.FLUSHED_COMPLETE;
    }


    /**
     * @see com.sun.jdori.StoreManager#delete(BitSet, BitSet, StateManagerInternal)
     */
    public synchronized int delete(
        BitSet loadedFields,
        BitSet dirtyFields,
        StateManagerInternal sm)
    {
    	Identity oid = (Identity)sm.getInternalObjectId();
    	logger.debug("OjbStoreManager.delete(" + oid + ")");
        try
        {
        	fetch(sm,null);
            connector.getBroker().delete(sm.getObject());
        }
        catch (Exception ex)
        {
            throw new OjbStoreFatalInternalException(getClass(), "delete", ex); 
        }
        dirtyFields.xor(dirtyFields);
        return StateManagerInternal.FLUSHED_COMPLETE;
    }

    /**
    * @see com.sun.jdori.StoreManager#fetch
    */
    public synchronized void fetch(StateManagerInternal sm, int fieldNums[])
    {
        PersistenceBroker broker = connector.getBroker();
        try
        {
        	Object instance = sm.getObject();
            Identity oid = (Identity) sm.getInternalObjectId();
            if (oid == null)
            {                
                oid = new Identity(instance,broker);
            }
            broker.removeFromCache(instance);
            PersistenceCapable pc = (PersistenceCapable) broker.getObjectByIdentity(oid);

            JDOClass jdoClass = Helper.getJDOClass(pc.getClass());
            if (fieldNums == null)
            {
                fieldNums = jdoClass.getManagedFieldNumbers();
            }

            FieldManager fm = new OjbFieldManager(pc, broker);
            sm.replaceFields(fieldNums, fm);

            getConnector().flush();
        }

        catch (Exception ex)
        {
            throw new OjbStoreFatalInternalException(getClass(), "fetch", ex); 
        }
    }

    /**
     * @see com.sun.jdori.StoreManager#getExtent
     */
    public synchronized Extent getExtent(
        Class pcClass,
        boolean subclasses,
        PersistenceManagerInternal pm)
    {
        PersistenceBroker broker = connector.getBroker();
        return new OjbExtent(pcClass, broker, pm);
    }

    /**
     * @see com.sun.jdori.StoreManager#createObjectId
     */
    public synchronized Object createObjectId(
        StateManagerInternal sm,
        PersistenceManagerInternal pm)
    {
        PersistenceCapable obj = sm.getObject();
        Identity oid = new Identity(obj, connector.getBroker());
        return oid;
    }

    /**
     * @see com.sun.jdori.StoreManager#createObjectId
     */
    public synchronized Object createInternalObjectId(
        StateManagerInternal sm,
        PersistenceCapable pc,
        Object oid,
        Class cls,
        PersistenceManagerInternal pm)
    {
        return new Identity(pc, connector.getBroker());
    }

    /**
     * @see com.sun.jdori.StoreManager#getExternalObjectId(Object oid,
     * PersistenceCapable pc)
     */
    public synchronized Object getExternalObjectId(Object objectId, PersistenceCapable pc)
    {
        return new Identity(pc, connector.getBroker());
    }

    /**
     * @see com.sun.jdori.StoreManager#copyKeyFieldsFromObjectId
     */
    public void copyKeyFieldsFromObjectId(StateManagerInternal sm, Class pcClass)
    {
        new Identity(sm.getObject(), connector.getBroker());
    }

    /**
     * @see com.sun.jdori.StoreManager#hasActualPCClass
     */
    public boolean hasActualPCClass(Object objectId)
    {
        boolean rc = true;
        return rc;
    }

    /**
     * @see com.sun.jdori.StoreManager#getInternalObjectId
     */
    public synchronized Object getInternalObjectId(Object objectId, PersistenceManagerInternal pm)
    {
        return objectId;
    }

    /**
     * @see com.sun.jdori.StoreManager#getPCClassForOid
     */
    public synchronized Class getPCClassForOid(Object objectId, PersistenceManagerInternal pm)
    {
        return ((Identity) objectId).getObjectsTopLevelClass();
    }

    /**
     * @see com.sun.jdori.StoreManager#newObjectIdInstance
     */
    public Object newObjectIdInstance(Class pcClass, String str)
    {
        return Identity.fromByteArray(str.getBytes());
    }

    /**
     * @see com.sun.jdori.StoreManager#flush
     */
    public void flush(Iterator it, PersistenceManagerInternal pm)
    {
        this.optimistic = pm.currentTransaction().getOptimistic();
        boolean err = false;

        while (it.hasNext())
        {
            StateManagerInternal sm = (StateManagerInternal) it.next();
            logger.debug("OjbStoreManager.flush: " + sm.getInternalObjectId() + ", " + Helper.getLCState(sm));
            sm.preStore();
            sm.replaceSCOFields();
            sm.flush(this);
            if (!sm.isFlushed())
            {
                err = true;
                break;
            }
        }

        logger.debug("OjbStoreManager.flush: end, err=" + err);

        if (err)
        {
            throw new JDOFatalInternalException("Error in flush");
        }
    }



    /**
     * @see com.sun.jdori.StoreManager#newObjectIdInstance
     */
    public QueryResult newQueryResult(QueryResultHelper queryResultHelper)
    {
        return new BasicQueryResult(queryResultHelper);
    }

}
