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

import javax.jdo.JDODataStoreException;
import javax.jdo.JDOUserException;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

import com.sun.jdori.Connector;

/**
 * OjbStoreConnector represents a OJB PB connection
 *
 * @author Thomas Mahler
 */
class OjbStoreConnector implements Connector
{
    /** 
     * rollback only flag
	 */
    private boolean rollbackOnlyFlag = false;

    /**
     * Datasource to which this Connector writes its Message.
     */
    private final OjbStorePMF pmf;

    /**
     * broker represents the backend store.
     */
    PersistenceBroker broker = null;

    /**
     * if true underlying connection can be released
     */
    private boolean connectionReadyForRelease = true;

    /**
     * the logger used for debugging
     */
    private Logger logger = LoggerFactory.getLogger("JDO");

    OjbStoreConnector(OjbStorePMF pmf)
    {
        this.pmf = pmf;
    }


    /**
     * @see com.sun.jdori.Connector#begin
     */
    public void begin(boolean optimistic)
    {
        assertNotRollbackOnly();

        connectionReadyForRelease = false;
        logger.debug("OjbStoreConnector.begin: connectionReadyForRelease=" + connectionReadyForRelease);

        // obtain a fresh broker and open a tx on it 
        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        broker.beginTransaction();
    }

    /**
     * @see com.sun.jdori.Connector#beforeCompletion
     */
    public void beforeCompletion()
    {
        assertNotRollbackOnly();
        // Nothing to do.
    }

    /**
     * @see com.sun.jdori.Connector#flush
     */
    public void flush()
    {
        assertNotRollbackOnly();
        logger.debug("OjbStoreConnector.flush: " + "connectionReadyForRelease=" + connectionReadyForRelease);
        // thma: noop?
    }

    /**
     * @see com.sun.jdori.Connector#commit
     */
    public synchronized void commit()
    {
        assertNotRollbackOnly();

        try
        {
            logger.debug("OjbStoreConnector.commit"); 
            broker.commitTransaction();
            broker.close();
            broker = null;
        }
        catch (Exception ex)
        {
            throw new OjbStoreFatalInternalException(getClass(), "commit", ex); 
        }
        finally
        {
            connectionReadyForRelease = true;
        }
    }

    /**
     * @see com.sun.jdori.Connector#rollback
     */
    public synchronized void rollback()
    {
        logger.debug("OjbStoreConnector.rollback");

        if (!rollbackOnlyFlag)
        {
            try
            {
                broker.abortTransaction();
                broker.close();
                broker = null;
            }
            catch (Exception ex)
            {
                throw new OjbStoreFatalInternalException(getClass(), "rollback", ex); 
            }
            finally
            {
                connectionReadyForRelease = true;
            }
        }
    }

    /**
     * @see com.sun.jdori.Connector#setRollbackOnly
     */
    public void setRollbackOnly()
    {
        rollbackOnlyFlag = true;
    }

    /**
     * @see com.sun.jdori.Connector#getRollbackOnly
     */
    public boolean getRollbackOnly()
    {
        return rollbackOnlyFlag;
    }

    private void assertNotRollbackOnly()
    {
        if (rollbackOnlyFlag)
        {
            throw new JDODataStoreException("Rollback Only !");
        }
    }

    /**
     * Returns the broker.
     * @return PersistenceBroker
     */
    public PersistenceBroker getBroker()
    {
    	if (broker == null)
    	{
    		throw new JDOUserException("No transaction in progress.");
    	}
        return broker;
    }

    /**
     * Sets the broker.
     * @param broker The broker to set
     */
    public void setBroker(PersistenceBroker broker)
    {
        this.broker = broker;
    }

}
