package org.apache.ojb.odmg.oql;

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

import java.io.StringReader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;
import java.util.List;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.accesslayer.OJBIterator;
import org.apache.ojb.broker.query.BetweenCriteria;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.ReportQuery;
import org.apache.ojb.broker.query.SelectionCriteria;
import org.apache.ojb.broker.util.collections.ManageableArrayList;
import org.apache.ojb.broker.util.configuration.Configurable;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.odmg.ImplementationImpl;
import org.apache.ojb.odmg.OdmgConfiguration;
import org.apache.ojb.odmg.PBCapsule;
import org.apache.ojb.odmg.RuntimeObject;
import org.apache.ojb.odmg.TransactionImpl;
import org.odmg.QueryInvalidException;
import org.odmg.Transaction;

/**
 * The OQL query interface implementation.
 *
 * @version $Id: OQLQueryImpl.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class OQLQueryImpl implements EnhancedOQLQuery, Configurable
{
    private Logger log = LoggerFactory.getLogger(OQLQueryImpl.class);

    /**
     * holds the compiled query object
     */
    private Query query = null;
    private ListIterator bindIterator = null;
    private ImplementationImpl odmg;

    public OQLQueryImpl(ImplementationImpl odmg)
    {
        this.odmg = odmg;
    }

    /**
     * @deprecated
     * @param pbKey
     */
    public OQLQueryImpl(PBKey pbKey)
    {
    }


    /**
     * returns the compiled query object
     */
    public Query getQuery()
    {
        return query;
    }

    /**
     * returns the collection type to be used to contain oql query result sets
     */
    protected Class getCollectionClass()
    {
        return odmg.getOqlCollectionClass();
    }

    /**
     * Bind a parameter to the query.
     * A parameter is denoted in the query string passed to <code>create</code> by <i>$i</i>,
     * where <i>i</i> is the rank of the parameter, beginning with 1.
     * The parameters are set consecutively by calling this method <code>bind</code>.
     * The <i>ith</i> variable is set by the <i>ith</i> call to the <code>bind</code> method.
     * If any of the <i>$i</i> are not set by a call to <code>bind</code> at the point
     * <code>execute</code> is called, <code>QueryParameterCountInvalidException</code> is thrown.
     * The parameters must be objects, and the result is an <code>Object</code>.
     * Objects must be used instead of primitive types (<code>Integer</code> instead
     * of <code>int</code>) for passing the parameters.
     * <p>
     * If the parameter is of the wrong type,
     * <code>QueryParameterTypeInvalidException</code> is thrown.
     * After executing a query, the parameter list is reset.
     * @param parameter	A value to be substituted for a query parameter.
     * @exception org.odmg.QueryParameterCountInvalidException The number of calls to
     * <code>bind</code> has exceeded the number of parameters in the query.
     * @exception org.odmg.QueryParameterTypeInvalidException The type of the parameter does
     * not correspond with the type of the parameter in the query.
     */
    public void bind(Object parameter)
            throws org.odmg.QueryParameterCountInvalidException, org.odmg.QueryParameterTypeInvalidException
    {
        try
        {
            SelectionCriteria crit = (SelectionCriteria) getBindIterator().next();
            crit.bind(parameter);

            // BRJ: bind is called twice for between
            if (crit instanceof BetweenCriteria && !crit.isBound())
            {
                getBindIterator().previous();
            }
        }
        catch (Exception e)
        {
            throw new org.odmg.QueryParameterCountInvalidException(e.getMessage());
        }
    }

    private Vector flatten(Criteria crit, Vector acc)
    {
        Enumeration e = crit.getElements();
        while (e.hasMoreElements())
        {
            Object o = e.nextElement();
            if (o instanceof Criteria)
            {
                Criteria pc = (Criteria) o;
                flatten(pc, acc);
            }
            else
            {
                SelectionCriteria c = (SelectionCriteria) o;
                // BRJ : only add bindable criteria
                if (!c.isBound())
                {
                    acc.add(c);
                }
            }
        }
        return acc;
    }

    /**
     * Create an OQL query from the string parameter.
     * In order to execute a query, an <code>OQLQuery</code> object must be created
     * by calling <code>Implementation.newOQLQuery</code>, then calling the
     * <code>create</code> method with the query string.
     * The <code>create</code> method might throw <code>QueryInvalidException</code>
     * if the query could not be compiled properly. Some implementations may not want
     * to compile the query before <code>execute</code> is called. In this case
     * <code>QueryInvalidException</code> is thrown when <code>execute</code> is called.
     * @param	queryString	An OQL query.
     * @exception	QueryInvalidException	The query syntax is invalid.
     */
    public void create(String queryString) throws org.odmg.QueryInvalidException
    {
        create(queryString, Query.NO_START_AT_INDEX, Query.NO_END_AT_INDEX);
    }

    public void create(String queryString, int startAtIndex, int endAtIndex) throws QueryInvalidException
    {
        if (log.isDebugEnabled()) log.debug("create query for query-string: " + queryString);
        /**
         * Check preconditions.
         * End Index cannot be set before start index.
         * End Index cannot equal StartAtIndex
         */
        if ((endAtIndex != Query.NO_END_AT_INDEX) && (endAtIndex < startAtIndex))
        {
            throw new QueryInvalidException("endAtIndex must be greater than startAtIndex");
        }
        if (((endAtIndex != Query.NO_END_AT_INDEX) && (startAtIndex != Query.NO_START_AT_INDEX))
                && (endAtIndex == startAtIndex))
        {
            throw new QueryInvalidException("endAtIndex cannot be set equal to startAtIndex");
        }

        try
        {
//			Query query = QueryPool.getQuery(queryString);
            // Use the OQL parser to transform a query string to a valid org.apache.ojb.broker.query object
            Query _query;
            StringReader reader = new StringReader(queryString);
            OQLLexer lexer = new OQLLexer(reader);
            OQLParser parser = new OQLParser(lexer);
            _query = parser.buildQuery();
            setBindIterator(flatten(_query.getCriteria(), new Vector()).listIterator());
            _query.setStartAtIndex(startAtIndex);
            _query.setEndAtIndex(endAtIndex);
            setQuery(_query);
        }
        catch (RecognitionException e)
        {
            throw new QueryInvalidException(e.getMessage());
        }
        catch (TokenStreamException e)
        {
            throw new QueryInvalidException(e.getMessage());
        }
    }

    /**
     * Execute the query.
     * After executing a query, the parameter list is reset.
     * Some implementations may throw additional exceptions that are also derived
     * from <code>ODMGException</code>.
     * @return	The object that represents the result of the query.
     * The returned data, whatever its OQL type, is encapsulated into an object.
     * For instance, when OQL returns an integer, the result is put into an
     * <code>Integer</code> object. When OQL returns a collection (literal or object),
     * the result is always a Java collection object of the same kind
     * (for instance, a <code>DList</code>).
     * @exception	org.odmg.QueryException	An exception has occurred while executing the query.
     */
    public Object execute() throws org.odmg.QueryException
    {
        if (log.isDebugEnabled()) log.debug("Start execute query");

        //obtain current ODMG transaction
        TransactionImpl tx = odmg.getTxManager().getTransaction();
        // create PBCapsule
        PBCapsule capsule = null;
        ManageableCollection result = null;

        try
        {
            capsule = new PBCapsule(odmg.getCurrentPBKey(), tx);
            PersistenceBroker broker = capsule.getBroker();

            // ask the broker to perfom the query.
            // the concrete result type is configurable

            if (!(query instanceof ReportQuery))
            {
                result = broker.getCollectionByQuery(this.getCollectionClass(), query);
                performLockingIfRequired(tx, broker, result);
            }
            else
            {
                Iterator iter = null;
                result = new ManageableArrayList();
                iter = broker.getReportQueryIteratorByQuery(query);
                try
                {
                    while (iter.hasNext())
                    {
                        Object[] res = (Object[]) iter.next();

                        if (res.length == 1)
                        {
                            if (res[0] != null) // skip null values
                            {
                                result.ojbAdd(res[0]);
                            }
                        }
                        else
                        {
                            // skip null tuples
                            for (int i = 0; i < res.length; i++)
                            {
                                if (res[i] != null)
                                {
                                    result.ojbAdd(res);
                                    break;
                                }
                            }
                        }
                    }
                }
                finally
                {
                    if (iter instanceof OJBIterator)
                    {
                        ((OJBIterator) iter).releaseDbResources();
                    }
                }
            }
            // reset iterator to start of list so we can reuse this query
            ListIterator it = getBindIterator();
            while (it.hasPrevious())
            {
                it.previous();
            }
        }
        finally
        {
            if (capsule != null) capsule.destroy();
        }
        return result;
    }


    protected void performLockingIfRequired(
            TransactionImpl tx,
            PersistenceBroker broker,
            ManageableCollection result)
    {
        // if tx is available and implicit locking is required,
        // we do READ-lock all found objects
        if ((tx != null) && tx.isImplicitLocking() && tx.isOpen())
        {
            // read-lock all resulting objects to the current transaction
            Iterator iter = result.ojbIterator();
            Object toBeLocked = null;
            try
            {
                List regList = tx.getRegistrationList();
                while (iter.hasNext())
                {
                    toBeLocked = iter.next();
                    RuntimeObject rt = new RuntimeObject(toBeLocked, tx, false);
                    tx.lockAndRegister(rt, Transaction.READ, true, regList);
                }
            }
            finally
            {
                tx.clearRegistrationList();
            }
        }
    }


    protected OdmgConfiguration getConfiguration()
    {
        OdmgConfiguration config = (OdmgConfiguration) PersistenceBrokerFactory.getConfigurator().getConfigurationFor(null);
        return config;
    }


    /**
     * Sets the query.
     * @param query The query to set
     */
    private void setQuery(Query query)
    {
        this.query = query;
    }

    /**
     * Gets the bindIterator.
     * @return Returns a ListIterator
     */
    protected ListIterator getBindIterator()
    {
        return bindIterator;
    }

    /**
     * Sets the bindIterator.
     * @param bindIterator The bindIterator to set
     */
    private void setBindIterator(ListIterator bindIterator)
    {
        this.bindIterator = bindIterator;
    }

    /**
     * @see Configurable#configure(Configuration)
     */
    public void configure(Configuration pConfig) throws ConfigurationException
    {
    }

    public int fullSize()
    {
        return this.query.fullSize();
    }
}
