package org.apache.ojb.odmg.collections;

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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerAware;
import org.apache.ojb.broker.PersistenceBrokerException;
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
import org.odmg.DArray;
import org.odmg.DCollection;
import org.odmg.DList;
import org.odmg.ODMGRuntimeException;
import org.odmg.OQLQuery;
import org.odmg.QueryInvalidException;
import org.odmg.Transaction;


/**
 *
 * @author Thomas Mahler
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: DListImpl.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class DListImpl extends AbstractList implements DList, DArray,
        ManageableCollection, PersistenceBrokerAware
{
    private static final long serialVersionUID = -9219943066614026526L;

    private transient Logger log;

    private Integer id;
    private List elements;

    private PBKey pbKey;

    /**
     * Used by PB-Kernel to instantiate ManageableCollections
     * FOR INTERNAL USE ONLY
     */
    public DListImpl()
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
     * Used on odmg-level
     */
    public DListImpl(PBKey pbKey)
    {
        this();
        this.pbKey = pbKey;
    }

    protected Logger getLog()
    {
        if (log == null)
        {
            log = LoggerFactory.getLogger(DListImpl.class);
        }
        return log;
    }

    private DListEntry prepareEntry(Object obj)
    {
        return new DListEntry(this, obj);
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

    /**
     * Inserts the specified element at the specified position in this list
     * (optional operation).  Shifts the element currently at that position
     * (if any) and any subsequent elements to the right (adds one to their
     * indices).
     *
     * @param index index at which the specified element is to be inserted.
     * @param element element to be inserted.
     *
     * @throws UnsupportedOperationException if the <tt>add</tt> method is not
     * supported by this list.
     * @throws    ClassCastException if the class of the specified element
     * prevents it from being added to this list.
     * @throws    IllegalArgumentException if some aspect of the specified
     * element prevents it from being added to this list.
     * @throws    IndexOutOfBoundsException if the index is out of range
     * (index &lt; 0 || index &gt; size()).
     */
    public void add(int index, Object element)
    {
        DListEntry entry = prepareEntry(element);
        elements.add(index, entry);
        // if we are in a transaction: acquire locks !
        TransactionImpl tx = getTransaction();
        if (checkForOpenTransaction(tx))
        {
            RuntimeObject rt = new RuntimeObject(this, tx);
            List regList = tx.getRegistrationList();
            tx.lockAndRegister(rt, Transaction.WRITE, false, regList);

            rt = new RuntimeObject(element, tx);
            tx.lockAndRegister(rt, Transaction.READ, regList);

            rt = new RuntimeObject(entry, tx, true);
            tx.lockAndRegister(rt, Transaction.WRITE, false, regList);
        }

        // changing the position markers of entries:
        int offset = 0;
        try
        {
            offset = ((DListEntry) elements.get(index - 1)).getPosition();
        }
        catch (Exception ignored)
        {
        }
        for (int i = offset; i < elements.size(); i++)
        {
            entry = (DListEntry) elements.get(i);
            entry.setPosition(i);
        }
    }

    /**
     * Removes the element at the specified position in this list (optional
     * operation).  Shifts any subsequent elements to the left (subtracts one
     * from their indices).  Returns the element that was removed from the
     * list.<p>
     *
     * This implementation always throws an
     * <tt>UnsupportedOperationException</tt>.
     *
     * @param index the index of the element to remove.
     * @return the element previously at the specified position.
     *
     * @throws UnsupportedOperationException if the <tt>remove</tt> method is
     *		  not supported by this list.
     * @throws IndexOutOfBoundsException if the specified index is out of
     * 		  range (<tt>index &lt; 0 || index &gt;= size()</tt>).
     */
    public Object remove(int index)
    {
        DListEntry entry = (DListEntry) elements.get(index);
        // if we are in a transaction: acquire locks !
        TransactionImpl tx = getTransaction();
        if (checkForOpenTransaction(tx))
        {
            tx.deletePersistent(new RuntimeObject(entry, tx));
        }
        elements.remove(index);
        // changing the position markers of entries:
        int offset = 0;
        try
        {
            offset = ((DListEntry) elements.get(index)).getPosition();
        }
        catch (Exception ignored)
        {
        }
        for (int i = offset; i < elements.size(); i++)
        {
            entry = (DListEntry) elements.get(i);
            entry.setPosition(i);
        }

        return entry.getRealSubject();
    }

    /**
     * Creates a new <code>DList</code> object that contains the contents of this
     * <code>DList</code> object concatenated
     * with the contents of the <code>otherList</code> object.
     * @param	otherList	The list whose elements are placed at the end of the list
     * returned by this method.
     * @return	A new <code>DList</code> that is the concatenation of this list and
     * the list referenced by <code>otherList</code>.
     */
    public DList concat(DList otherList)
    {
        DListImpl result = new DListImpl(pbKey);
        result.addAll(this);
        result.addAll(otherList);
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

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of element to return.
     * @return the element at the specified position in this list.
     *
     * @throws IndexOutOfBoundsException if the index is out of range (index
     * &lt; 0 || index &gt;= size()).
     */
    public Object get(int index)
    {
        DListEntry entry = (DListEntry) elements.get(index);
        return entry.getRealSubject();
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.02.2001 20:53:01)
     * @return java.util.Vector
     */
    public List getElements()
    {
        return elements;
    }

    /**
     * Lazily return the Id, no point in precomputing it.
     * @return int
     */
    public Integer getId()
    {
        return id;
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
        return new DListIterator(this);
    }

    /**
     * Returns a list iterator of the elements in this list (in proper
     * sequence).
     *
     * @return a list iterator of the elements in this list (in proper
     * sequence).
     */
    public ListIterator listIterator()
    {
        return new DListIterator(this);
    }

    /**
     * Returns a list iterator of the elements in this list (in proper
     * sequence), starting at the specified position in this list.  The
     * specified index indicates the first element that would be returned by
     * an initial call to the <tt>next</tt> method.  An initial call to
     * the <tt>previous</tt> method would return the element with the
     * specified index minus one.
     *
     * @param index index of first element to be returned from the
     * list iterator (by a call to the <tt>next</tt> method).
     * @return a list iterator of the elements in this list (in proper
     * sequence), starting at the specified position in this list.
     * @throws IndexOutOfBoundsException if the index is out of range (index
     * &lt; 0 || index &gt; size()).
     */
    public ListIterator listIterator(int index)
    {
        return new DListIterator(this, index);
    }

    private Criteria getPkCriteriaForAllElements(PersistenceBroker brokerForClass)
    {
        try
        {
            Criteria crit = null;
            for (int i = 0; i < elements.size(); i++)
            {
                DListEntry entry = (DListEntry) elements.get(i);
                Object obj = entry.getRealSubject();
                ClassDescriptor cld = brokerForClass.getClassDescriptor(obj.getClass());

                FieldDescriptor[] pkFields = cld.getPkFields();
                ValueContainer[] pkValues = brokerForClass.serviceBrokerHelper().getKeyValues(cld, obj);

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
            return null;
        }
    }

    private Class getElementsExtentClass(PersistenceBroker brokerForClass) throws PersistenceBrokerException
    {
        // we ll have to compute the most general extent class here !!!
        DListEntry entry = (DListEntry) elements.get(0);
        Class elementsClass = entry.getRealSubject().getClass();
        Class extentClass = brokerForClass.getTopLevelClass(elementsClass);
        return extentClass;
    }

    /**
     * Evaluate the boolean query predicate for each element of the collection and
     * return a new collection that contains each element that evaluated to true.
     * @param	predicate	An OQL boolean query predicate.
     * @return	A new collection containing the elements that evaluated true for the predicate.
     * @exception	org.odmg.QueryInvalidException	The query predicate is invalid.
     */
    public DCollection query(String predicate) throws QueryInvalidException
    {
        // 1.build complete OQL statement
        String oql = "select all from java.lang.Object where " + predicate;
        TransactionImpl tx = getTransaction();
        if (tx == null) throw new QueryInvalidException("Need running transaction to do query");

        OQLQuery predicateQuery = tx.getImplementation().newOQLQuery();
        predicateQuery.create(oql);
        Query pQ = ((OQLQueryImpl) predicateQuery).getQuery();
        Criteria pCrit = pQ.getCriteria();

        PBCapsule handle = new PBCapsule(pbKey, tx);
        DList result;
        try
        {
            PersistenceBroker broker = handle.getBroker();
            Criteria allElementsCriteria = this.getPkCriteriaForAllElements(broker);
            // join selection of elements with predicate criteria:
            allElementsCriteria.addAndCriteria(pCrit);

            Class clazz = null;
            try
            {
                clazz = this.getElementsExtentClass(broker);
            }
            catch (PersistenceBrokerException e)
            {
                getLog().error(e);
                throw new ODMGRuntimeException(e.getMessage());
            }
            Query q = new QueryByCriteria(clazz, allElementsCriteria);
            if (getLog().isDebugEnabled()) getLog().debug(q.toString());

            result = null;
            try
            {
                result = (DList) broker.getCollectionByQuery(DListImpl.class, q);
            }
            catch (PersistenceBrokerException e)
            {
                getLog().error("Query failed", e);
                throw new OJBRuntimeException(e);
            }
        }
        finally
        {
            // cleanup stuff
            if (handle != null) handle.destroy();
        }

        // 3. return resulting collection
        return result;

    }

    public int hashCode()
    {
        int hashCode = 1;
        Iterator it = elements.iterator();
        while (it.hasNext())
        {
            Object obj = it.next();
            hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
        }
        return hashCode;
    }

    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this);
        buf.append("id", id);
        buf.append("pbKey", pbKey);
        buf.append("[containing elements: ");
        Iterator it = elements.iterator();
        while (it.hasNext())
        {
            Object obj = it.next();
            buf.append(obj != null ? obj.toString() : null);
        }
        buf.append("]");
        return buf.toString();
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
     * add a single Object to the Collection. This method is used during reading Collection elements
     * from the database. Thus it is is save to cast anObject to the underlying element type of the
     * collection.
     */
    public void ojbAdd(Object anObject)
    {
        DListEntry entry = prepareEntry(anObject);
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

    /**
     * Resize the array to have <code>newSize</code> elements.
     * @param	newSize	The new size of the array.
     */
    public void resize(int newSize)
    {
    }

    /**
     * Sets the elements.
     * @param elements The elements to set
     */
    public void setElements(List elements)
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
//        DListEntry entry;
//        while (it.hasNext())
//        {
//            entry = (DListEntry) it.next();
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
