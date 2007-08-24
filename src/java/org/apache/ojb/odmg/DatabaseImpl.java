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

import org.apache.ojb.broker.PBFactoryException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.odmg.DatabaseClosedException;
import org.odmg.DatabaseNotFoundException;
import org.odmg.DatabaseOpenException;
import org.odmg.ODMGException;
import org.odmg.ObjectNameNotFoundException;
import org.odmg.ObjectNameNotUniqueException;
import org.odmg.TransactionInProgressException;
import org.odmg.TransactionNotInProgressException;

/**
 * Implementation class of the {@link org.odmg.Database} interface.
 * 
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: DatabaseImpl.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class DatabaseImpl implements org.odmg.Database
{
    private Logger log = LoggerFactory.getLogger(DatabaseImpl.class);

    private PBKey pbKey;
    private boolean isOpen;
    private ImplementationImpl odmg;

    public DatabaseImpl(ImplementationImpl ojb)
    {
        isOpen = false;
        this.odmg = ojb;
    }

    private TransactionImpl getTransaction()
    {
        Object result = odmg.currentTransaction();
        // TODO: remove this workaround
        // In managed environments only wrapped tx are returned, so
        // we have to extract the real tx first
        if(result instanceof NarrowTransaction)
        {
            return ((NarrowTransaction) result).getRealTransaction();
        }
        return (TransactionImpl) result;
    }

    /**
     * Return the {@link org.apache.ojb.broker.PBKey} associated with this Database.
     */
    public PBKey getPBKey()
    {
        if (pbKey == null)
        {
            log.error("## PBKey not set, Database isOpen=" + isOpen + " ##");
            if (!isOpen) throw new DatabaseClosedException("Database is not open");
        }
        return pbKey;
    }

    public boolean isOpen()
    {
        return this.isOpen;
    }

    /**
     * Open the named database with the specified access mode.
     * Attempts to open a database when it has already been opened will result in
     * the throwing of the exception <code>DatabaseOpenException</code>.
     * A <code>DatabaseNotFoundException</code> is thrown if the database does not exist.
     * Some implementations may throw additional exceptions that are also derived from
     * <code>ODMGException</code>.
     * @param name The name of the database.
     * @param accessMode The access mode, which should be one of the static fields:
     * <code>OPEN_READ_ONLY</code>, <code>OPEN_READ_WRITE</code>,
     * or <code>OPEN_EXCLUSIVE</code>.
     * @exception ODMGException The database could not be opened.
     */
    public synchronized void open(String name, int accessMode) throws ODMGException
    {
        if (isOpen())
        {
            throw new DatabaseOpenException("Database is already open");
        }
        PersistenceBroker broker = null;
        try
        {
            if (name == null)
            {
                log.info("Given argument was 'null', open default database");
                broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            }
            else
            {
                broker = PersistenceBrokerFactory.createPersistenceBroker(
                        BrokerHelper.extractAllTokens(name));
            }
            pbKey = broker.getPBKey();
            isOpen = true;
            //register opened database
            odmg.registerOpenDatabase(this);
            if (log.isDebugEnabled()) log.debug("Open database using PBKey " + pbKey);
        }
        catch (PBFactoryException ex)
        {
            log.error("Open database failed: " + ex.getMessage(), ex);
            throw new DatabaseNotFoundException(
                    "OJB can't open database " + name + "\n" + ex.getMessage());
        }
        finally
        {
            // broker must be immediately closed
            if (broker != null)
            {
                broker.close();
            }
        }
    }

    /**
     * Close the database.
     * After you have closed a database, further attempts to access objects in the
     * database will cause the exception <code>DatabaseClosedException</code> to be thrown.
     * Some implementations may throw additional exceptions that are also derived
     * from <code>ODMGException</code>.
     * @exception ODMGException Unable to close the database.
     */
    public void close() throws ODMGException
    {
        /**
         * is the DB open? ODMG 3.0 says we can't close an already open database.
         */
        if (!isOpen())
        {
            throw new DatabaseClosedException("Database is not Open. Must have an open DB to call close.");
        }
        /**
         * is the associated Tx open? ODMG 3.0 says we can't close the database with an open Tx pending.
         * check if a tx was found, the tx was associated with database
         */
        if (odmg.hasOpenTransaction() &&
                getTransaction().getAssociatedDatabase().equals(this))
        {
            String msg = "Database cannot be closed, associated Tx is still open." +
                    " Transaction status is '" + TxUtil.getStatusString(getTransaction().getStatus()) + "'." +
                    " Used PBKey was "+getTransaction().getBroker().getPBKey();
            log.error(msg);
            throw new TransactionInProgressException(msg);
        }
        isOpen = false;
        // remove the current PBKey
        pbKey = null;
        // if we close current database, we have to notify implementation instance
        if (this == odmg.getCurrentDatabase())
        {
            odmg.setCurrentDatabase(null);
        }
    }

    /**
     * Associate a name with an object and make it persistent.
     * An object instance may be bound to more than one name.
     * Binding a previously transient object to a name makes that object persistent.
     * @param object The object to be named.
     * @param name The name to be given to the object.
     * @exception org.odmg.ObjectNameNotUniqueException
     * If an attempt is made to bind a name to an object and that name is already bound
     * to an object.
     */
    public void bind(Object object, String name)
            throws ObjectNameNotUniqueException
    {
        /**
         * Is DB open? ODMG 3.0 says it has to be to call bind.
         */
        if (!this.isOpen())
        {
            throw new DatabaseClosedException("Database is not open. Must have an open DB to call bind.");
        }
        /**
         * Is Tx open? ODMG 3.0 says it has to be to call bind.
         */
        TransactionImpl tx = getTransaction();
        if (tx == null || !tx.isOpen())
        {
            throw new TransactionNotInProgressException("Tx is not open. Must have an open TX to call bind.");
        }

        tx.getNamedRootsMap().bind(object, name);
    }

    /**
     * Lookup an object via its name.
     * @param name The name of an object.
     * @return The object with that name.
     * @exception ObjectNameNotFoundException There is no object with the specified name.
     * ObjectNameNotFoundException
     */
    public Object lookup(String name) throws ObjectNameNotFoundException
    {
        /**
         * Is DB open? ODMG 3.0 says it has to be to call bind.
         */
        if (!this.isOpen())
        {
            throw new DatabaseClosedException("Database is not open. Must have an open DB to call lookup");
        }
        /**
         * Is Tx open? ODMG 3.0 says it has to be to call bind.
         */
        TransactionImpl tx = getTransaction();
        if (tx == null || !tx.isOpen())
        {
            throw new TransactionNotInProgressException("Tx is not open. Must have an open TX to call lookup.");
        }

        return tx.getNamedRootsMap().lookup(name);
    }

    /**
     * Disassociate a name with an object
     * @param name The name of an object.
     * @exception ObjectNameNotFoundException No object exists in the database with that name.
     */
    public void unbind(String name) throws ObjectNameNotFoundException
    {
        /**
         * Is DB open? ODMG 3.0 says it has to be to call unbind.
         */
        if (!this.isOpen())
        {
            throw new DatabaseClosedException("Database is not open. Must have an open DB to call unbind");
        }
        TransactionImpl tx = getTransaction();
        if (tx == null || !tx.isOpen())
        {
            throw new TransactionNotInProgressException("Tx is not open. Must have an open TX to call lookup.");
        }

        tx.getNamedRootsMap().unbind(name);
    }

    /**
     * Make a transient object durable in the database.
     * It must be executed in the context of an open transaction.
     * If the transaction in which this method is executed commits,
     * then the object is made durable.
     * If the transaction aborts,
     * then the makePersistent operation is considered not to have been executed,
     * and the target object is again transient.
     * ClassNotPersistenceCapableException is thrown if the implementation cannot make
     * the object persistent because of the type of the object.
     * @param	object	The object to make persistent.
     * @throws TransactionNotInProgressException if there is no current transaction.
     */
    public void makePersistent(Object object)
    {
        /**
         * Is DB open? ODMG 3.0 says it has to be to call makePersistent.
         */
        if (!this.isOpen())
        {
            throw new DatabaseClosedException("Database is not open");
        }
        /**
         * Is Tx open? ODMG 3.0 says it has to be to call makePersistent.
         */
        TransactionImpl tx = getTransaction();
        if (tx == null || !tx.isOpen())
        {
            throw new TransactionNotInProgressException("No transaction in progress, cannot persist");
        }
        RuntimeObject rt = new RuntimeObject(object, getTransaction());
        tx.makePersistent(rt);
//        tx.moveToLastInOrderList(rt.getIdentity());
    }

    /**
     * Deletes an object from the database.
     * It must be executed in the context of an open transaction.
     * If the object is not persistent, then ObjectNotPersistent is thrown.
     * If the transaction in which this method is executed commits,
     * then the object is removed from the database.
     * If the transaction aborts,
     * then the deletePersistent operation is considered not to have been executed,
     * and the target object is again in the database.
     * @param	object	The object to delete.
     */
    public void deletePersistent(Object object)
    {
        if (!this.isOpen())
        {
            throw new DatabaseClosedException("Database is not open");
        }
        TransactionImpl tx = getTransaction();
        if (tx == null || !tx.isOpen())
        {
            throw new TransactionNotInProgressException("No transaction in progress, cannot delete persistent");
        }
        RuntimeObject rt = new RuntimeObject(object, tx);
        tx.deletePersistent(rt);
//        tx.moveToLastInOrderList(rt.getIdentity());
    }
}
