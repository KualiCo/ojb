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

import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.odmg.oql.EnhancedOQLQuery;
import org.odmg.DArray;
import org.odmg.DBag;
import org.odmg.DList;
import org.odmg.DMap;
import org.odmg.DSet;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.Transaction;

/**
 * Implementation of the ODMG {@link Implementation} interface for use in
 * managed enviroments.
 *
 * @version $Id: ImplementationJTAImpl.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class ImplementationJTAImpl extends ImplementationImpl
{
    private Logger log = LoggerFactory.getLogger(ImplementationJTAImpl.class);

    protected ImplementationJTAImpl()
    {
        super();
    }

    public Database getDatabase(Object obj)
    {
        beginInternTransaction();
        return this.getCurrentDatabase();
    }

    public Transaction currentTransaction()
    {
        beginInternTransaction();
        /*
        we wrap the intern odmg transaction to avoid unauthorised calls
        since we use proprietary extensions for Transaction interface
        do cast to enhanced interface
        */
        return new NarrowTransaction((TransactionImpl) super.currentTransaction());
    }

    public EnhancedOQLQuery newOQLQuery()
    {
        beginInternTransaction();
        return super.newOQLQuery();
    }

    public DList newDList()
    {
        beginInternTransaction();
        return super.newDList();
    }

    public DBag newDBag()
    {
        beginInternTransaction();
        return super.newDBag();
    }

    public DSet newDSet()
    {
        beginInternTransaction();
        return super.newDSet();
    }

    public DArray newDArray()
    {
        beginInternTransaction();
        return super.newDArray();
    }

    public DMap newDMap()
    {
        beginInternTransaction();
        return super.newDMap();
    }

    /**
     * Here we start a intern odmg-Transaction to hide transaction demarcation
     * This method could be invoked several times within a transaction, but only
     * the first call begin a intern odmg transaction
     */
    private void beginInternTransaction()
    {
        if (log.isDebugEnabled()) log.debug("beginInternTransaction was called");
        J2EETransactionImpl tx = (J2EETransactionImpl) super.currentTransaction();
        if (tx == null) tx = newInternTransaction();
        if (!tx.isOpen())
        {
            // start the transaction
            tx.begin();
            tx.setInExternTransaction(true);
        }
    }

    /**
     * Returns a new intern odmg-transaction for the current database.
     */
    private J2EETransactionImpl newInternTransaction()
    {
        if (log.isDebugEnabled()) log.debug("obtain new intern odmg-transaction");
        J2EETransactionImpl tx = new J2EETransactionImpl(this);
        try
        {
            getConfigurator().configure(tx);
        }
        catch (ConfigurationException e)
        {
            throw new OJBRuntimeException("Cannot create new intern odmg transaction", e);
        }
        return tx;
    }

    /**
     * Not supported in managed-environment.
     */
    public Transaction newTransaction()
    {
        throw new UnsupportedOperationException("Not supported in managed environment");
    }
}
