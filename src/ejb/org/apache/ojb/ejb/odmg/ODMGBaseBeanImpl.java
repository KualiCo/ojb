package org.apache.ojb.ejb.odmg;

/* Copyright 2004-2005 The Apache Software Foundation
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

import javax.ejb.EJBException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.ejb.SessionBeanImpl;
import org.apache.ojb.odmg.HasBroker;
import org.apache.ojb.odmg.TransactionExt;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.LockNotGrantedException;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * Base class for using OJB-ODMG api within SessionBeans,
 * subclass this class to implement your own bean
 * implementations.
 *
 * To keep this example as simple as possible, we lookup a static OJB ODMG
 * implementation instance from an helper class.
 * But it's recommended to bind an instances of the ODMG main/access classes in JNDI
 * (at appServer start), open the database and lookup these instances instance via JNDI in
 * ejbCreate(), instead of lookup a static instance on each bean creation.
 *
 * To get the {@link org.odmg.Database} or
 * {@link org.odmg.Implementation} instance use
 * the {@link #getDatabase} and {@link #getImplementation}
 * methods.
 *
 * Additionally there are some basic methods for
 * storing, deleting, counting, get all objects
 * implemented.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ODMGBaseBeanImpl.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public abstract class ODMGBaseBeanImpl extends SessionBeanImpl
{
    private Logger log = LoggerFactory.getLogger(ODMGBaseBeanImpl.class);
    private Implementation odmg;

    /**
     * Lookup the OJB ODMG implementation.
     * It's recommended to bind an instance of the Implementation class in JNDI
     * (at appServer start), open the database and lookup this instance via JNDI in
     * ejbCreate().
     */
    public void ejbCreate()
    {
        if (log.isDebugEnabled()) log.debug("ejbCreate was called");
        odmg = ODMGHelper.getODMG();
    }

    /**
     * Here we do the OJB cleanup.
     */
    public void ejbRemove()
    {
        super.ejbRemove();
        odmg = null;
    }

    /**
     * Return the Database associated with
     * this bean.
     */
    public Database getDatabase()
    {
        return odmg.getDatabase(null);
    }

    /**
     * Return the Implementation instance associated
     * with this bean.
     */
    public Implementation getImplementation()
    {
        return odmg;
    }

    /**
     * Store an object.
     */
    public Object storeObject(Object object)
    {
        /* One possibility of storing objects is to use the current transaction
         associated with the container */
        try
        {
            TransactionExt tx = (TransactionExt) odmg.currentTransaction();
            tx.lock(object, Transaction.WRITE);
            tx.markDirty(object);
        }
        catch (LockNotGrantedException e)
        {
            log.error("Failure while storing object " + object, e);
            throw new EJBException("Failure while storing object", e);
        }
        return object;
    }

    /**
     * Delete an object.
     */
    public void deleteObject(Object object)
    {
        getDatabase().deletePersistent(object);
    }

    /**
     * Store a collection of objects.
     */
    public Collection storeObjects(Collection objects)
    {
        try
        {
            /* One possibility of storing objects is to use the current transaction
             associated with the container */
            Transaction tx = odmg.currentTransaction();
            for (Iterator iterator = objects.iterator(); iterator.hasNext();)
            {
                tx.lock(iterator.next(), Transaction.WRITE);
            }
        }
        catch (LockNotGrantedException e)
        {
            log.error("Failure while storing objects " + objects, e);
            throw new EJBException("Failure while storing objects", e);
        }
        return objects;
    }

    /**
     * Delete a Collection of objects.
     */
    public void deleteObjects(Collection objects)
    {
        for (Iterator iterator = objects.iterator(); iterator.hasNext();)
        {
            getDatabase().deletePersistent(iterator.next());
        }
    }

    /**
     * Return the count of all objects found
     * for given class, using the PB-api within
     * ODMG - this may change in further versions.
     */
    public int getCount(Class target)
    {
        PersistenceBroker broker = ((HasBroker) odmg.currentTransaction()).getBroker();
        int result = broker.getCount(new QueryByCriteria(target));
        return result;
    }

    public Collection getAllObjects(Class target)
    {
        OQLQuery query = odmg.newOQLQuery();
        try
        {
            query.create("select allObjects from " + target.getName());
            return (Collection) query.execute();
        }
        catch (Exception e)
        {
            log.error("OQLQuery failed", e);
            throw new EJBException("OQLQuery failed", e);
        }
    }
}
