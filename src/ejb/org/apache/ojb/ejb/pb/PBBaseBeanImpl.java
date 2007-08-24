package org.apache.ojb.ejb.pb;

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

import java.util.Collection;
import java.util.Iterator;

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.core.PersistenceBrokerFactoryFactory;
import org.apache.ojb.broker.core.PersistenceBrokerFactoryIF;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.ejb.SessionBeanImpl;

/**
 * Base class for using OJB-PB api within SessionBeans,
 * subclass this class to implement your own bean
 * implementations.
 *
 * <br>
 * Use the {@link #getBroker} method to obtain a
 * PersistenceBroker instance, do PB.close() after
 * using.
 *
 * <br>
 * Additionally there are some basic methods for
 * storing, deleting, counting, get all objects
 * implemented.
 *
 *
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PBBaseBeanImpl.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public abstract class PBBaseBeanImpl extends SessionBeanImpl
{
    private Logger log = LoggerFactory.getLogger(PBBaseBeanImpl.class);
    private PersistenceBrokerFactoryIF pbf;

    public void ejbRemove()
    {
        super.ejbRemove();
        // we do explicit cleanup (not necessary)
        pbf = null;
        log = null;
    }

    public void ejbCreate()
    {
        if (log.isDebugEnabled()) log.info("ejbCreate was called");
        pbf = PersistenceBrokerFactoryFactory.instance();
    }

    /**
     * Return a PersistenceBroker instance.
     */
    public PersistenceBroker getBroker()
    {
        return pbf.defaultPersistenceBroker();
    }

    /**
     * Return a PersistenceBroker instance for
     * the given PBKey.
     */
    public PersistenceBroker getBroker(PBKey key)
    {
        return pbf.createPersistenceBroker(key);
    }

    /**
     * Return the count of all objects found
     * for given class.
     */
    public int getCount(Class target)
    {
        PersistenceBroker broker = getBroker();
        int result;
        try
        {
            result = broker.getCount(new QueryByCriteria(target));
        }
        finally
        {
            if (broker != null) broker.close();
        }
        return result;
    }

    /**
     * Return all objects for the given class.
     */
    public Collection getAllObjects(Class target)
    {
        PersistenceBroker broker = getBroker();
        Collection result;
        try
        {
            Query q = new QueryByCriteria(target);
            result = broker.getCollectionByQuery(q);
        }
        finally
        {
            if (broker != null) broker.close();
        }
        return result;
    }

    /**
     * Store an object.
     */
    public Object storeObject(Object object)
    {
        PersistenceBroker broker = getBroker();
        try
        {
            broker.store(object);
        }
        finally
        {
            if (broker != null) broker.close();
        }
        return object;
    }

    /**
     * Delete an object.
     */
    public void deleteObject(Object object)
    {
        PersistenceBroker broker = null;
        try
        {
            broker = getBroker();
            broker.delete(object);
        }
        finally
        {
            if (broker != null) broker.close();
        }
    }

    /**
     * Store a collection of objects.
     */
    public Collection storeObjects(Collection objects)
    {
        PersistenceBroker broker = null;
        Collection stored;
        try
        {
            broker = getBroker();
            stored = this.storeObjects(broker, objects);
        }
        finally
        {
            if (broker != null) broker.close();
        }
        return stored;
    }

    /**
     * Delete a Collection of objects.
     */
    public void deleteObjects(Collection objects)
    {
        PersistenceBroker broker = null;
        try
        {
            broker = getBroker();
            this.deleteObjects(broker, objects);
        }
        finally
        {
            if (broker != null) broker.close();
        }
    }

    protected Collection storeObjects(PersistenceBroker broker, Collection objects)
    {
        for (Iterator it = objects.iterator(); it.hasNext();)
        {
            broker.store(it.next());
        }
        return objects;
    }

    protected void deleteObjects(PersistenceBroker broker, Collection objects)
    {
        for (Iterator it = objects.iterator(); it.hasNext();)
        {
            broker.delete(it.next());
        }
    }
}
