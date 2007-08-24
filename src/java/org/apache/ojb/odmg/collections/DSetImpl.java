package org.apache.ojb.odmg.collections;

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
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Collection;

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerAware;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.core.ValueContainer;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.odmg.PBCapsule;
import org.apache.ojb.odmg.TransactionImpl;
import org.apache.ojb.odmg.TxManagerFactory;
import org.apache.ojb.odmg.RuntimeObject;
import org.apache.ojb.odmg.oql.OQLQueryImpl;
import org.odmg.DCollection;
import org.odmg.DList;
import org.odmg.DSet;
import org.odmg.ODMGRuntimeException;
import org.odmg.OQLQuery;
import org.odmg.Transaction;


/**
 *
 */
public class DSetImpl extends AbstractSet implements DSet, Serializable, PersistenceBrokerAware, ManageableCollection
{
	private static final long serialVersionUID = -4459673364598652639L;

    private transient Logger log;

    private Integer id;
    private List elements;

    private PBKey pbKey;

    /**
     * Used by PB-Kernel to instantiate ManageableCollections
     * FOR INTERNAL USE ONLY
     */
    public DSetImpl()
    {
        super();
        elements = new ArrayList();
//        if(getTransaction() == null)
//        {
//            throw new TransactionNotInProgressException("Materialization of DCollection instances must be done" +
//                    " within a odmg-tx");
//        }
        getPBKey();
    }

    /**
     * DSetImpl constructor comment.
     */
    public DSetImpl(PBKey pbKey)
    {
        this();
        this.pbKey = pbKey;
    }

    protected Logger getLog()
    {
        if (log == null)
        {
            log = LoggerFactory.getLogger(DSetImpl.class);
        }
        return log;
    }

    private DSetEntry prepareEntry(Object obj)
    {
        return new DSetEntry(this, obj);
    }

    protected TransactionImpl getTransaction()
    {
        return TxManagerFactory.instance().getTransaction();
    }

    protected boolean checkForOpenTransaction(TransactionImpl tx)
    {
        boolean result = false;
        if(tx != null && tx.isOpen())
        {
            result = true;
        }
        return result;
    }

    public PBKey getPBKey()
    {
        if(pbKey == null)
        {
            TransactionImpl tx = getTransaction();
            if(tx != null && tx.isOpen())
            {
                pbKey = tx.getBroker().getPBKey();
            }
        }
        return pbKey;
    }

    public void setPBKey(PBKey pbKey)
    {
        this.pbKey = pbKey;
    }

    public boolean remove(Object o)
    {
        return super.remove(o);
    }

    public boolean removeAll(Collection c)
    {
        return super.removeAll(c);
    }

    public boolean add(Object o)
    {
        if (!this.contains(o))
        {
            DSetEntry entry = prepareEntry(o);
            elements.add(entry);
            // if we are in a transaction: get locks !
            TransactionImpl tx = getTransaction();
            if ((tx != null) && (tx.isOpen()))
            {
                List regList = tx.getRegistrationList();
                RuntimeObject rt = new RuntimeObject(this, tx);
                tx.lockAndRegister(rt, Transaction.WRITE, false, regList);

                rt = new RuntimeObject(o, tx);
                tx.lockAndRegister(rt, Transaction.READ, regList);

                rt = new RuntimeObject(entry, tx, true);
                tx.lockAndRegister(rt, Transaction.WRITE, false, regList);
            }
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Create a new <code>DSet</code> object that contains the elements of this
     * collection minus the elements in <code>otherSet</code>.
     * @param	otherSet	A set containing elements that should not be in the result set.
     * @return	A newly created <code>DSet</code> instance that contains the elements
     * of this set minus those elements in <code>otherSet</code>.
     */
    public DSet difference(DSet otherSet)
    {
        DSetImpl result = new DSetImpl(getPBKey());
        Iterator iter = this.iterator();
        while (iter.hasNext())
        {
            Object candidate = iter.next();
            if (!otherSet.contains(candidate))
            {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * Determines whether there is an element of the collection that evaluates to true
     * for the predicate.
     * @param	predicate	An OQL boolean query predicate.
     * @return	True if there is an element of the collection that evaluates to true
     * for the predicate, otherwise false.
     * @exception	org.odmg.QueryInvalidException	The query predicate is invalid.
     */
    public boolean existsElement(String predicate) throws org.odmg.QueryInvalidException
    {
        DList results = (DList) this.query(predicate);
        if (results == null || results.size() == 0)
            return false;
        else
            return true;
    }

    public List getElements()
    {
        return elements;
    }

    public void setElements(List elements)
    {
        this.elements = elements;
    }

    public Integer getId()
    {
        return id;
    }

    /**
     * Create a new <code>DSet</code> object that is the set intersection of this
     * <code>DSet</code> object and the set referenced by <code>otherSet</code>.
     * @param	otherSet	The other set to be used in the intersection operation.
     * @return	A newly created <code>DSet</code> instance that contains the
     * intersection of the two sets.
     */
    public DSet intersection(DSet otherSet)
    {
        DSet union = this.union(otherSet);
        DSetImpl result = new DSetImpl(getPBKey());
        Iterator iter = union.iterator();
        while (iter.hasNext())
        {
            Object candidate = iter.next();
            if (this.contains(candidate) && otherSet.contains(candidate))
            {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * Returns an iterator over the elements in this collection.  There are no
     * guarantees concerning the order in which the elements are returned
     * (unless this collection is an instance of some class that provides a
     * guarantee).
     *
     * @return an <tt>Iterator</tt> over the elements in this collection
     */
    public Iterator iterator()
    {
        return new DSetIterator(this);
    }

    /**
     * Determine whether this set is a proper subset of the set referenced by
     * <code>otherSet</code>.
     * @param	otherSet	Another set.
     * @return True if this set is a proper subset of the set referenced by
     * <code>otherSet</code>, otherwise false.
     */
    public boolean properSubsetOf(org.odmg.DSet otherSet)
    {
        return (this.size() > 0 && this.size() < otherSet.size() && this.subsetOf(otherSet));
    }

    /**
     * Determine whether this set is a proper superset of the set referenced by
     * <code>otherSet</code>.
     * @param	otherSet	Another set.
     * @return True if this set is a proper superset of the set referenced by
     * <code>otherSet</code>, otherwise false.
     */
    public boolean properSupersetOf(org.odmg.DSet otherSet)
    {
        return (otherSet.size() > 0 && otherSet.size() < this.size() && this.supersetOf(otherSet));
    }

    /**
     * Evaluate the boolean query predicate for each element of the collection and
     * return a new collection that contains each element that evaluated to true.
     * @param	predicate	An OQL boolean query predicate.
     * @return	A new collection containing the elements that evaluated true for the predicate.
     * @exception	org.odmg.QueryInvalidException	The query predicate is invalid.
     */
    public DCollection query(String predicate) throws org.odmg.QueryInvalidException
    {
        // 1.build complete OQL statement
        String oql = "select all from java.lang.Object where " + predicate;
        TransactionImpl tx = getTransaction();

        OQLQuery predicateQuery = tx.getImplementation().newOQLQuery();

        PBCapsule capsule = new PBCapsule(tx.getImplementation().getCurrentPBKey(), tx);
        PersistenceBroker broker = capsule.getBroker();

        try
        {
            predicateQuery.create(oql);
            Query pQ = ((OQLQueryImpl) predicateQuery).getQuery();
            Criteria pCrit = pQ.getCriteria();

            Criteria allElementsCriteria = this.getPkCriteriaForAllElements(broker);
            // join selection of elements with predicate criteria:
            pCrit.addAndCriteria(allElementsCriteria);
            Class clazz = this.getElementsExtentClass(broker);
            Query q = new QueryByCriteria(clazz, pCrit);
            if (log.isDebugEnabled()) log.debug(q.toString());
            // 2. perfom query
            return (DSetImpl) broker.getCollectionByQuery(DSetImpl.class, q);
        }
        catch (PersistenceBrokerException e)
        {
            throw new ODMGRuntimeException(e.getMessage());
        }
        finally
        {
            capsule.destroy();
        }
    }

    private Criteria getPkCriteriaForAllElements(PersistenceBroker broker)
    {
        try
        {
            Criteria crit = null;
            for (int i = 0; i < elements.size(); i++)
            {
                DListEntry entry = (DListEntry) elements.get(i);
                Object obj = entry.getRealSubject();
                ClassDescriptor cld = broker.getClassDescriptor(obj.getClass());

                FieldDescriptor[] pkFields = cld.getPkFields();
                ValueContainer[] pkValues = broker.serviceBrokerHelper().getKeyValues(cld, obj);

                Criteria criteria = new Criteria();
                for (int j = 0; j < pkFields.length; j++)
                {
                    FieldDescriptor fld = pkFields[j];
                    criteria.addEqualTo(fld.getPersistentField().getName(), pkValues[j].getValue());
                }

                if (crit == null)
                    crit = criteria;
                else
                    crit.addOrCriteria(criteria);
            }
            return crit;
        }
        catch (PersistenceBrokerException e)
        {
            log.error(e);
            return null;
        }
    }

    private Class getElementsExtentClass(PersistenceBroker broker) throws PersistenceBrokerException
    {
        // we ll have to compute the most general extent class here !!!
        DListEntry entry = (DListEntry) elements.get(0);
        Class elementsClass = entry.getRealSubject().getClass();
        Class extentClass = broker.getTopLevelClass(elementsClass);
        return extentClass;
    }


    /**
     * Access all of the elements of the collection that evaluate to true for the
     * provided query predicate.
     * @param	predicate	An OQL boolean query predicate.
     * @return	An iterator used to iterate over the elements that evaluated true for the predicate.
     * @exception	org.odmg.QueryInvalidException	The query predicate is invalid.
     */
    public Iterator select(String predicate) throws org.odmg.QueryInvalidException
    {
        return this.query(predicate).iterator();
    }

    /**
     * Selects the single element of the collection for which the provided OQL query
     * predicate is true.
     * @param	predicate	An OQL boolean query predicate.
     * @return The element that evaluates to true for the predicate. If no element
     * evaluates to true, null is returned.
     * @exception	org.odmg.QueryInvalidException	The query predicate is invalid.
     */
    public Object selectElement(String predicate) throws org.odmg.QueryInvalidException
    {
        return ((DList) this.query(predicate)).get(0);
    }

    /**
     * Sets the elements.
     * @param elements The elements to set
     */
    public void setElements(Vector elements)
    {
        this.elements = elements;
    }

    /**
     * Sets the id.
     * @param id The id to set
     */
    public void setId(Integer id)
    {
        this.id = id;
    }

    /**
     * Returns the number of elements in this collection.  If this collection
     * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of elements in this collection
     */
    public int size()
    {
        return elements.size();
    }

    /**
     * Determine whether this set is a subset of the set referenced by <code>otherSet</code>.
     * @param	otherSet	Another set.
     * @return True if this set is a subset of the set referenced by <code>otherSet</code>,
     * otherwise false.
     */
    public boolean subsetOf(DSet otherSet)
    {
        return otherSet.containsAll(this);
    }

    /**
     * Determine whether this set is a superset of the set referenced by <code>otherSet</code>.
     * @param	otherSet	Another set.
     * @return True if this set is a superset of the set referenced by <code>otherSet</code>,
     * otherwise false.
     */
    public boolean supersetOf(DSet otherSet)
    {
        return this.containsAll(otherSet);
    }

    /**
     * Create a new <code>DSet</code> object that is the set union of this
     * <code>DSet</code> object and the set referenced by <code>otherSet</code>.
     * @param	otherSet	The other set to be used in the union operation.
     * @return	A newly created <code>DSet</code> instance that contains the union of the two sets.
     */
    public DSet union(DSet otherSet)
    {
        DSetImpl result = new DSetImpl(getPBKey());
        result.addAll(this);
        result.addAll(otherSet);
        return result;
    }


    //***************************************************************
    // ManageableCollection interface
    //***************************************************************

    /**
     * add a single Object to the Collection. This method is used during reading Collection elements
     * from the database. Thus it is is save to cast anObject to the underlying element type of the
     * collection.
     */
    public void ojbAdd(Object anObject)
    {
        DSetEntry entry = prepareEntry(anObject);
        entry.setPosition(elements.size());
        elements.add(entry);
    }

    /**
     * adds a Collection to this collection. Used in reading Extents from the Database.
     * Thus it is save to cast otherCollection to this.getClass().
     */
    public void ojbAddAll(ManageableCollection otherCollection)
    {
        // don't use this to avoid locking
        // this.addAll((DListImpl) otherCollection);
        Iterator it = otherCollection.ojbIterator();
        while (it.hasNext())
        {
            ojbAdd(it.next());
        }
    }

    public void afterStore(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }

    /**
     * returns an Iterator over all elements in the collection. Used during store and delete Operations.
     * If the implementor does not return an iterator over ALL elements, OJB cannot store and delete all elements properly.
     */
    public Iterator ojbIterator()
    {
        return this.iterator();
    }

    //***************************************************************
    // PersistenceBrokerAware interface
    //***************************************************************

    /**
     * prepare itself for persistence. Each DList entry generates an
     * {@link org.apache.ojb.broker.Identity} for the wrapped persistent
     * object.
     */
    public void beforeInsert(PersistenceBroker broker) throws PersistenceBrokerException
    {
//        Iterator it = elements.iterator();
//        DSetEntry entry;
//        while (it.hasNext())
//        {
//            entry = (DSetEntry) it.next();
//            entry.prepareForPersistency(broker);
//        }
    }

    /**
     * noop
     */
    public void beforeUpdate(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }

    /**
     * noop
     */
    public void beforeDelete(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }

    /**
     * noop
     */
    public void afterUpdate(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }

    /**
     * noop
     */
    public void afterInsert(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }

    /**
     * noop
     */
    public void afterDelete(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }

    /**
     * noop
     */
    public void afterLookup(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }
}
