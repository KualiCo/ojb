package org.apache.ojb.broker.accesslayer;

/* Copyright 2003-2005 The Apache Software Foundation
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.core.PersistenceBrokerConfiguration;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Abstract Prefetcher.
 * @author <a href="mailto:olegnitz@apache.org">Oleg Nitz</a>
 * @version $Id: BasePrefetcher.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public abstract class BasePrefetcher implements RelationshipPrefetcher
{
    /** The numer of columns to query for. */
    protected static final int IN_LIMIT = getPrefetchInLimit();

    private Logger logger;
    private PersistenceBrokerImpl broker;
    /** Class descriptor for the item type. */ 
    protected ClassDescriptor itemClassDesc;
    /** Maximum number of pk's in one query. */ 
    protected final int pkLimit;

    /**
     * Returns the number of column to query for, from the configuration.
     * 
     * @return The prefetch limit
     */
    private static int getPrefetchInLimit()
    {
        try
        {
            PersistenceBrokerConfiguration config = (PersistenceBrokerConfiguration) PersistenceBrokerFactory.getConfigurator().getConfigurationFor(null);

            return config.getSqlInLimit();
        }
        catch (ConfigurationException e)
        {
            return 200;
        }
    }

    /**
     * Constructor for BasePrefetcher.
     */
    public BasePrefetcher(PersistenceBrokerImpl aBroker, Class anItemClass)
    {
        super();
        broker = aBroker;
        itemClassDesc = aBroker.getDescriptorRepository().getDescriptorFor(anItemClass);
        logger = LoggerFactory.getLogger(this.getClass());
        pkLimit = getPrefetchInLimit() / getItemClassDescriptor().getPkFields().length;
    }

    /**
     * The limit of objects loaded by one SQL query
     */
    public int getLimit()
    {
        return pkLimit;
    }

    /**
     * associate the batched Children with their owner object <br>
     */
    protected abstract void associateBatched(Collection owners, Collection children);

    /**
    * @see org.apache.ojb.broker.accesslayer.RelationshipPrefetcher#prefetchRelationship(Collection)
    */
    public void prefetchRelationship(Collection owners)
    {
        Query queries[];
        Collection children = new ArrayList();

        queries = buildPrefetchQueries(owners, children);

        for (int i = 0; i < queries.length; i++)
        {
            Iterator iter = getBroker().getIteratorByQuery(queries[i]);
            while (iter.hasNext())
            {
                children.add(iter.next());
            }
        }

        // BRJ: performRetrieval of childrens references BEFORE associating with owners
        // TODO: this is a quick fix ! 
        getBroker().getReferenceBroker().performRetrievalTasks();
        
        associateBatched(owners, children);
    }

    
    protected QueryByCriteria buildPrefetchQuery(Collection ids, FieldDescriptor[] fields)
    {
        return buildPrefetchQuery(getItemClassDescriptor().getClassOfObject(), ids, fields);
    }

    
    /**
     * 
     * @param ids collection of identities
     * @param fields
     * @return
     */
    protected Criteria buildPrefetchCriteria(Collection ids, FieldDescriptor[] fields)
    {
        if (fields.length == 1)
        {
            return buildPrefetchCriteriaSingleKey(ids, fields[0]);
        }
        else
        {
            return buildPrefetchCriteriaMultipleKeys(ids, fields);
        }
        
    }
   
    /**
     * 
     * @param clazz
     * @param ids collection of identities
     * @param fields
     * @return
     */
    protected QueryByCriteria buildPrefetchQuery(Class clazz, Collection ids, FieldDescriptor[] fields)
    {
        return QueryFactory.newQuery(clazz, buildPrefetchCriteria(ids, fields));
    }

    /**
     * Build the Criteria using IN(...) for single keys
     * @param ids collection of identities
     * @param field
     * @return Criteria
     */
    private Criteria buildPrefetchCriteriaSingleKey(Collection ids, FieldDescriptor field)
    {
        Criteria crit = new Criteria();
        ArrayList values = new ArrayList(ids.size());
        Iterator iter = ids.iterator();
        Identity id;

        while (iter.hasNext())
        {
            id = (Identity) iter.next();
            values.add(id.getPrimaryKeyValues()[0]);
        }

        switch (values.size())
        {
            case 0:
                break;
            case 1:
                crit.addEqualTo(field.getAttributeName(), values.get(0));
                break;
            default:
                // create IN (...) for the single key field
                crit.addIn(field.getAttributeName(), values);
                break;
        }

        return crit;
    }

    /**
     * Build the Criteria using multiple ORs
     * @param ids collection of identities
     * @param fields
     * @return Criteria
     */
    private Criteria buildPrefetchCriteriaMultipleKeys(Collection ids, FieldDescriptor fields[])
    {
        Criteria crit = new Criteria();
        Iterator iter = ids.iterator();
        Object[] val;
        Identity id;

        while (iter.hasNext())
        {
            Criteria c = new Criteria();
            id = (Identity) iter.next();
            val = id.getPrimaryKeyValues();
            for (int i = 0; i < val.length; i++)
            {
                if (val[i] == null)
                {
                    c.addIsNull(fields[i].getAttributeName());
                }
                else
                {
                    c.addEqualTo(fields[i].getAttributeName(), val[i]);
                }
            }
            crit.addOrCriteria(c);
        }

        return crit;
    }

    /**
     * Return the DescriptorRepository
     */
    protected DescriptorRepository getDescriptorRepository()
    {
        return getBroker().getDescriptorRepository();
    }

    /**
     * Returns the ClassDescriptor of the item Class
     * @return ClassDescriptor
     */
    public ClassDescriptor getItemClassDescriptor()
    {
        return itemClassDesc;
    }

    protected abstract Query[] buildPrefetchQueries(Collection owners, Collection children);

    /**
     * Returns the broker.
     * @return PersistenceBrokerImpl
     */
    protected PersistenceBrokerImpl getBroker()
    {
        return broker;
    }

    /**
     * Returns the logger.
     * @return Logger
     */
    protected Logger getLogger()
    {
        return logger;
    }
}
