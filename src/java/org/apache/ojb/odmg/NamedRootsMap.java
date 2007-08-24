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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.SerializationUtils;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.util.ObjectModification;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.odmg.ClassNotPersistenceCapableException;
import org.odmg.ObjectNameNotFoundException;
import org.odmg.ObjectNameNotUniqueException;
import org.odmg.Transaction;

/**
 * ODMG NamedRoots implementation.
 * this implementation stores the (name, Identity) pairs in
 * a database table.
 * therefore the NamedRootsMap underlies the same transaction management
 * as all other persistent objects
 *
 * @author Thomas Mahler
 * @version $Id: NamedRootsMap.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class NamedRootsMap
{
    private Logger log = LoggerFactory.getLogger(NamedRootsMap.class);
    private TransactionImpl tx;
    private HashMap tempBindings;
    private Map deletionMap;
    private Map insertMap;

    NamedRootsMap(TransactionImpl tx)
    {
        this.tx = tx;
        this.tempBindings = new HashMap();
    }

    private void addForDeletion(NamedEntry entry)
    {
        if(deletionMap == null)
        {
            deletionMap = new HashMap();
        }
        deletionMap.put(entry.getName(), entry);
    }

    private void addForInsert(NamedEntry entry)
    {
        if(insertMap == null)
        {
            insertMap = new HashMap();
        }
        insertMap.put(entry.getName(), entry);
        if(deletionMap != null) deletionMap.remove(entry.getName());
    }

    /**
     * Have to be performed after the "normal" objects be written
     * to DB and before method {@link #performInsert()}.
     */
    public void performDeletion()
    {
        if(deletionMap == null)
            return;
        else
        {
            PersistenceBroker broker = tx.getBroker();
            Iterator it = deletionMap.values().iterator();
            while(it.hasNext())
            {
                NamedEntry namedEntry = (NamedEntry) it.next();
                broker.delete(namedEntry);
            }
        }
    }

    /**
     * Have to be performed after the "normal" objects be written
     * to DB and after method {@link #performDeletion()}.
     */
    public void performInsert()
    {
        if(insertMap == null)
            return;
        else
        {
            PersistenceBroker broker = tx.getBroker();
            Iterator it = insertMap.values().iterator();
            while(it.hasNext())
            {
                NamedEntry namedEntry = (NamedEntry) it.next();
                namedEntry.prepareForStore(broker);
                broker.store(namedEntry, ObjectModification.INSERT);
            }
        }
    }

    public void afterWriteCleanup()
    {
        if(deletionMap != null) deletionMap.clear();
        if(insertMap != null) insertMap.clear();
    }

    private void localBind(String key, NamedEntry entry) throws ObjectNameNotUniqueException
    {
        if(tempBindings.containsKey(key))
        {
            throw new ObjectNameNotUniqueException("Object key already in use, the key '"
                    + key + "' is not unique");
        }
        else
        {
            tempBindings.put(key, entry);
        }
    }

    private void localUnbind(String key)
    {
        tempBindings.remove(key);
    }

    private NamedEntry localLookup(String key)
    {
        return (NamedEntry) tempBindings.get(key);
    }

    /**
     * Return a named object associated with the specified key.
     */
    Object lookup(String key) throws ObjectNameNotFoundException
    {
        Object result = null;
        NamedEntry entry = localLookup(key);
        // can't find local bound object
        if(entry == null)
        {
            try
            {
                PersistenceBroker broker = tx.getBroker();
                // build Identity to lookup entry
                Identity oid = broker.serviceIdentity().buildIdentity(NamedEntry.class, key);
                entry = (NamedEntry) broker.getObjectByIdentity(oid);
            }
            catch(Exception e)
            {
                log.error("Can't materialize bound object for key '" + key + "'", e);
            }
        }
        if(entry == null)
        {
            log.info("No object found for key '" + key + "'");
        }
        else
        {
            Object obj = entry.getObject();
            // found a persistent capable object associated with that key
            if(obj instanceof Identity)
            {
                Identity objectIdentity = (Identity) obj;
                result = tx.getBroker().getObjectByIdentity(objectIdentity);
                // lock the persistance capable object
                RuntimeObject rt = new RuntimeObject(result, objectIdentity, tx, false);
                tx.lockAndRegister(rt, Transaction.READ, tx.getRegistrationList());
            }
            else
            {
                // nothing else to do
                result = obj;
            }
        }
        if(result == null) throw new ObjectNameNotFoundException("Can't find named object for name '" + key + "'");
        return result;
    }

    /**
     * Remove a named object
     */
    void unbind(String key)
    {
        NamedEntry entry = new NamedEntry(key, null, false);
        localUnbind(key);
        addForDeletion(entry);
    }

    public void bind(Object object, String name) throws ObjectNameNotUniqueException
    {
        boolean useIdentity = true;
        PersistenceBroker broker = tx.getBroker();
        ClassDescriptor cld = null;
        try
        {
            cld = broker.getClassDescriptor(ProxyHelper.getRealClass(object));
        }
        catch(PersistenceBrokerException e)
        {
        }

        // if null a non-persistent capable object was specified
        if(cld == null)
        {
            useIdentity = false;
            if(!(object instanceof Serializable))
            {
                throw new ClassNotPersistenceCapableException(
                        "Can't bind named object, because it's not Serializable. Name=" + name + ", object=" + object);
            }
        }
        else
        {
            RuntimeObject rt = new RuntimeObject(object, tx);
            // if the object is already persistet, check for read
            // lock to make sure
            // that the used object is a valid version
            // else persist the specified named object
            if(!rt.isNew())
            {
                tx.lockAndRegister(rt, Transaction.READ, tx.getRegistrationList());
            }
            else
            {
                tx.makePersistent(rt);
            }
        }
        NamedEntry oldEntry = localLookup(name);
        if(oldEntry == null)
        {
            Identity oid = broker.serviceIdentity().buildIdentity(NamedEntry.class, name);
            oldEntry = (NamedEntry) broker.getObjectByIdentity(oid);
        }
        if(oldEntry != null)
        {
            throw new ObjectNameNotUniqueException("The name of the specified named object already exist, name=" + name);
        }

        NamedEntry entry = new NamedEntry(name, object, useIdentity);
        addForInsert(entry);
        localBind(name, entry);
    }



    //==============================================
    // inner class
    //==============================================
    /**
     * represents an entry to the named roots table.
     * maps names (Strings) to OJB Identities
     */
    public static final class NamedEntry implements Serializable
    {
        static final long serialVersionUID = 6179717896336300342L;
        /**
         * the name under which an object is registered in the NamedRoots Map
         */
        private String name;
        /**
         * the serialized Identity representing the named Object
         */
        private byte[] oid;

        private transient Object object;
        private transient boolean useIdentity;

        public NamedEntry()
        {
        }

        NamedEntry(final String aName, final Object object, final boolean useIdentity)
        {
            this.name = aName;
            this.object = object;
            this.useIdentity = useIdentity;
        }

        /**
         * This has to be called before this object will be persistet.
         */
        public void prepareForStore(PersistenceBroker broker)
        {
            if(object != null)
            {
                if(useIdentity)
                {
                    Identity oid = broker.serviceIdentity().buildIdentity(object);
                    this.oid = SerializationUtils.serialize(oid);
                }
                else
                {
                    this.oid = SerializationUtils.serialize((Serializable) object);
                }
            }
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public byte[] getOid()
        {
            return oid;
        }

        public void setOid(byte[] oid)
        {
            this.oid = oid;
        }

        Object getObject()
        {
            if(object != null)
            {
                return object;
            }
            else
            {
                return oid != null ? SerializationUtils.deserialize(oid) : null;
            }
        }
    }
}
