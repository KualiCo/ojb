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

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.util.collections.ManageableArrayList;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.configuration.Configurator;
import org.apache.ojb.broker.util.factory.ConfigurableFactory;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.odmg.locking.LockManager;
import org.apache.ojb.odmg.locking.LockManagerFactory;
import org.apache.ojb.odmg.oql.EnhancedOQLQuery;
import org.apache.ojb.odmg.oql.OQLQueryImpl;
import org.odmg.DArray;
import org.odmg.DBag;
import org.odmg.DList;
import org.odmg.DMap;
import org.odmg.DSet;
import org.odmg.Database;
import org.odmg.DatabaseClosedException;
import org.odmg.Implementation;
import org.odmg.ODMGRuntimeException;
import org.odmg.Transaction;


/**
 * Default implementation of the {@link Implementation} interface.
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 *
 * @version $Id: ImplementationImpl.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class ImplementationImpl implements ImplementationExt
{
    private Logger log = LoggerFactory.getLogger(ImplementationImpl.class);

//    private List usedDatabases = new ArrayList();
    private DatabaseImpl currentDatabase;
    private Configurator configurator;
    private OJBTxManager ojbTxManager;
    private LockManager lockManager;

    private Class oqlCollectionClass;
    private boolean impliciteWriteLocks;
    private boolean implicitLocking;
    private boolean implicitLockingBackward;
    private boolean ordering;
//    private boolean noteUserOrder;

    /**
     * private Constructor: use static factory method
     * getInstance() to obtain an instance
     */
    protected ImplementationImpl()
    {
        ojbTxManager = TxManagerFactory.instance();
        lockManager = LockManagerFactory.getLockManager();
        setConfigurator(PersistenceBrokerFactory.getConfigurator());
        Configuration conf = getConfigurator().getConfigurationFor(null);
        oqlCollectionClass = conf.getClass("OqlCollectionClass", ManageableArrayList.class);
        impliciteWriteLocks = (conf.getString("LockAssociations", "WRITE").equalsIgnoreCase("WRITE"));
        implicitLocking = conf.getBoolean("ImplicitLocking", true);
        ordering = conf.getBoolean("Ordering", true);
//        noteUserOrder = conf.getBoolean("NoteUserOrder", true);
        implicitLockingBackward = conf.getBoolean("ImplicitLockingBackward", false);
        if(log.isEnabledFor(Logger.INFO))
        {
            log.info("Settings: " + this.toString());
        }
    }

    public OJBTxManager getTxManager()
    {
        return ojbTxManager;
    }

    protected LockManager getLockManager()
    {
        return lockManager;
    }

    protected synchronized void setCurrentDatabase(DatabaseImpl curDB)
    {
        currentDatabase = curDB;
    }

    protected synchronized DatabaseImpl getCurrentDatabase()
    {
        return currentDatabase;
    }

    public PBKey getCurrentPBKey()
    {
        return currentDatabase.getPBKey();
    }

    /**
     * Gets the configurator.
     * @return Returns a Configurator
     */
    public Configurator getConfigurator()
    {
        return configurator;
    }

    /**
     * Sets the configurator.
     * @param configurator The configurator to set
     */
    public void setConfigurator(Configurator configurator)
    {
        this.configurator = configurator;
    }

    /**
     * Create a <code>Transaction</code> object and associate it with the current thread.
     * @return The newly created <code>Transaction</code> instance.
     * @see Transaction
     */
    public Transaction newTransaction()
    {
        if ((getCurrentDatabase() == null))
        {
            throw new DatabaseClosedException("Database is NULL, must have a DB in order to create a transaction");
        }
        TransactionImpl tx = new TransactionImpl(this);
        try
        {
            getConfigurator().configure(tx);
        }
        catch (ConfigurationException e)
        {
            throw new ODMGRuntimeException("Error in configuration of TransactionImpl instance: " + e.getMessage());
        }
        return tx;
    }

    /**
     * Get the current <code>Transaction</code> for the thread.
     * @return The current <code>Transaction</code> object or null if there is none.
     * @see Transaction
     */
    public Transaction currentTransaction()
    {
        if ((getCurrentDatabase() == null))
        {
            throw new DatabaseClosedException("Database is NULL, must have a DB in order to create a transaction");
        }
        return ojbTxManager.getTransaction();
    }

    public boolean hasOpenTransaction()
    {
        TransactionImpl tx = ojbTxManager.getTransaction();
        return tx != null && tx.isOpen();
    }

    /**
     * Create a new <code>Database</code> object.
     * @return The new <code>Database</code> object.
     * @see Database
     */
    public Database newDatabase()
    {
        return new DatabaseImpl(this);
    }

    /**
     * Create a new <code>OQLQuery</code> object.
     * @return The new <code>OQLQuery</code> object.
     * @see org.odmg.OQLQuery
     */
    public EnhancedOQLQuery newOQLQuery()
    {
        if ((getCurrentDatabase() == null) || !getCurrentDatabase().isOpen())
        {
            throw new DatabaseClosedException("Database is not open");
        }
        return new OQLQueryImpl(this);
    }

    /**
     * Create a new <code>DList</code> object.
     * @return The new <code>DList</code> object.
     * @see DList
     */
    public DList newDList()
    {
        if ((getCurrentDatabase() == null))
        {
            throw new DatabaseClosedException("Database is NULL, cannot create a DList with a null database.");
        }
        return (DList) DListFactory.singleton.createCollectionOrMap(getCurrentPBKey());
    }

    /**
     * Create a new <code>DBag</code> object.
     * @return The new <code>DBag</code> object.
     * @see DBag
     */
    public DBag newDBag()
    {
        if ((getCurrentDatabase() == null))
        {
            throw new DatabaseClosedException("Database is NULL, cannot create a DBag with a null database.");
        }
        return (DBag) DBagFactory.singleton.createCollectionOrMap(getCurrentPBKey());
    }

    /**
     * Create a new <code>DSet</code> object.
     * @return The new <code>DSet</code> object.
     * @see DSet
     */
    public DSet newDSet()
    {
        if ((getCurrentDatabase() == null))
        {
            throw new DatabaseClosedException("Database is NULL, cannot create a DSet with a null database.");
        }
        return (DSet) DSetFactory.singleton.createCollectionOrMap(getCurrentPBKey());
    }

    /**
     * Create a new <code>DArray</code> object.
     * @return The new <code>DArray</code> object.
     * @see DArray
     */
    public DArray newDArray()
    {
        if ((getCurrentDatabase() == null))
        {
            throw new DatabaseClosedException("Database is NULL, cannot create a DArray with a null database.");
        }
        return (DArray) DArrayFactory.singleton.createCollectionOrMap(getCurrentPBKey());
    }

    /**
     * Create a new <code>DMap</code> object.
     * @return The new <code>DMap</code> object.
     * @see DMap
     */
    public DMap newDMap()
    {
        if ((getCurrentDatabase() == null))
        {
            throw new DatabaseClosedException("Database is NULL, cannot create a DMap with a null database.");
        }
        return (DMap) DMapFactory.singleton.createCollectionOrMap(getCurrentPBKey());
    }

    /**
     * Get a <code>String</code> representation of the object's identifier.
     * OJB returns the serialized Identity of the object.
     * @param obj The object whose identifier is being accessed.
     * @return The object's identifier in the form of a String
     */
    public String getObjectId(Object obj)
    {
        Identity oid = null;
        PersistenceBroker broker = null;

        try
        {
            if (getCurrentDatabase() != null)
            {
                /**
                 * is there an open database we are calling getObjectId against? if yes, use it
                 */
                broker = PersistenceBrokerFactory.createPersistenceBroker(getCurrentDatabase().getPBKey());
            }
            else
            {
                log.warn("Can't find open database, try to use the default configuration");
                /**
                 * otherwise, use default.
                 */
                broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            }

            oid = broker.serviceIdentity().buildIdentity(obj);
        }
        finally
        {
            if(broker != null)
            {
                broker.close();
            }
        }
        return new String(SerializationUtils.serialize(oid));
    }

    /**
     * Returns the current used database or null.
     */
    public Database getDatabase(Object obj)
    {
        /* @todo enhance functionality */
        return getCurrentDatabase();
    }

    /**
     * Register opened database via the PBKey.
     */
    protected synchronized void registerOpenDatabase(DatabaseImpl newDB)
    {
        DatabaseImpl old_db = getCurrentDatabase();
        if (old_db != null)
        {
            try
            {
                if (old_db.isOpen())
                {
                    log.warn("## There is still an opened database, close old one ##");
                    old_db.close();
                }
            }
            catch (Throwable t)
            {
                //ignore
            }
        }
        if (log.isDebugEnabled()) log.debug("Set current database " + newDB + " PBKey was " + newDB.getPBKey());
        setCurrentDatabase(newDB);
//        usedDatabases.add(newDB.getPBKey());
    }

    /**
     * <strong>Note:</strong> Method behavior changed between version 1.0.3 and
     * 1.0.4. Now this method is used to set the global property <em>implicit locking</em>,
     * use method {@link TransactionExt#setImplicitLocking(boolean)} to set the property
     * for a running transaction.
     *
     * @see ImplementationExt#setImplicitLocking(boolean)
     */
	public void setImplicitLocking(boolean value)
	{
        if(implicitLockingBackward)
        {
            ((TransactionExt)currentTransaction()).setImplicitLocking(value);
        }
        else
        {
            this.implicitLocking = value;
        }
	}

    /**
     * @see ImplementationExt#isImplicitLocking()
     */
    public boolean isImplicitLocking()
    {
        return implicitLocking;
    }

    /**
     * @see ImplementationExt#getOqlCollectionClass()
     */
    public Class getOqlCollectionClass()
    {
        return oqlCollectionClass;
    }

    /**
     * @see ImplementationExt#setOqlCollectionClass(Class)
     */
    public void setOqlCollectionClass(Class oqlCollectionClass)
    {
        this.oqlCollectionClass = oqlCollectionClass;
    }

    /**
     * @see ImplementationExt#setImpliciteWriteLocks(boolean)
     */
    public void setImpliciteWriteLocks(boolean impliciteWriteLocks)
    {
        this.impliciteWriteLocks = impliciteWriteLocks;
    }

    /**
     * @see ImplementationExt#isImpliciteWriteLocks()
     */
    public boolean isImpliciteWriteLocks()
    {
        return impliciteWriteLocks;
    }

    public boolean isOrdering()
    {
        return ordering;
    }

    public void setOrdering(boolean ordering)
    {
        this.ordering = ordering;
    }

//    public boolean isNoteUserOrder()
//    {
//        return noteUserOrder;
//    }
//
//    public void setNoteUserOrder(boolean noteUserOrder)
//    {
//        this.noteUserOrder = noteUserOrder;
//    }

    /**
     * Allow to use method {@link #setImplicitLocking(boolean)} in the same way
     * as before version 1.0.4 - if set 'true', recommended setting is 'false'.
     *
     * @deprecated is only for backward compatibility with older versions (before 1.0.4)
     * and will be removed in future versions.
     */
    public void setImplicitLockingBackward(boolean implicitLockingBackward)
    {
        this.implicitLockingBackward = implicitLockingBackward;
    }

    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("implicitLocking", isImplicitLocking())
                .append("implicitWriteLocks", isImpliciteWriteLocks())
                .append("ordering", isOrdering())
                .append("oqlCollectionClass", getOqlCollectionClass())
                .append("txManager", getTxManager())
                .append("lockManager", getLockManager())
                .toString();
    }


    //*****************************************************
    // inner classes
    //*****************************************************

    abstract static class BaseFactory extends ConfigurableFactory
    {
        Object createCollectionOrMap()
        {
            return this.createNewInstance();
        }

        Object createCollectionOrMap(PBKey key)
        {
            return createNewInstance(PBKey.class, key);
        }
    }

    static final class DListFactory extends BaseFactory
    {
        static final BaseFactory singleton = new DListFactory();
        protected String getConfigurationKey()
        {
            return "DListClass";
        }
    }

    static final class DArrayFactory extends BaseFactory
    {
        static final BaseFactory singleton = new DArrayFactory();
        protected String getConfigurationKey()
        {
            return "DArrayClass";
        }
    }

    static final class DBagFactory extends BaseFactory
    {
        static final BaseFactory singleton = new DBagFactory();
        protected String getConfigurationKey()
        {
            return "DBagClass";
        }
    }

    static final class DSetFactory extends BaseFactory
    {
        static final BaseFactory singleton = new DSetFactory();
        protected String getConfigurationKey()
        {
            return "DSetClass";
        }
    }

    static final class DMapFactory extends BaseFactory
    {
        static final BaseFactory singleton = new DMapFactory();
        protected String getConfigurationKey()
        {
            return "DMapClass";
        }
    }
}
