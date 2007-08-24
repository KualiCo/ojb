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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.FieldHelper;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByMtoNCriteria;
import org.apache.ojb.broker.query.ReportQueryByMtoNCriteria;

/**
 * Relationship Prefetcher for MtoN-Collections.
 * 
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: MtoNCollectionPrefetcher.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class MtoNCollectionPrefetcher extends CollectionPrefetcher
{

    /**
     * @param aBroker the PersistenceBroker
     * @param anOrd the CollectionDescriptor
     */
    public MtoNCollectionPrefetcher(PersistenceBrokerImpl aBroker, ObjectReferenceDescriptor anOrd)
    {
        super(aBroker, anOrd);
    }

    /**
     * @see org.apache.ojb.broker.accesslayer.RelationshipPrefetcher#prefetchRelationship(Collection)
     */
    public void prefetchRelationship(Collection owners)
    {
        Query[] queries;
        Query[] mnQueries;
        Collection children = new ArrayList();
        Collection mnImplementors = new ArrayList();

        queries = buildPrefetchQueries(owners, children);
        mnQueries = buildMtoNImplementorQueries(owners, children);

        for (int i = 0; i < queries.length; i++)
        {
            Iterator iter = getBroker().getIteratorByQuery(queries[i]);
            while (iter.hasNext())
            {
                Object aChild = iter.next();

                // BRJ: simulate the distinct removed from the query
                if (!children.contains(aChild))
                {
                    children.add(aChild);
                }
            }

            Iterator mnIter = getBroker().getReportQueryIteratorByQuery(mnQueries[i]);
            while (mnIter.hasNext())
            {
                mnImplementors.add(mnIter.next());
            }
        }

        associateBatched(owners, children, mnImplementors);
    }

    /**
     * Build the prefetch query for a M-N relationship, The query looks like the following sample :
     * <br>
     * <pre>
     *       crit = new Criteria();
     *       crit.addIn("PERSON_PROJECT.PROJECT_ID", ids);
     *       crit.addEqualToField("id","PERSON_PROJECT.PERSON_ID");
     *       qry = new QueryByMtoNCriteria(Person.class, "PERSON_PROJECT", crit, true);
     * </pre>
     *
     * @param ids Collection containing all identities of objects of the M side
     * @return the prefetch Query
     */
    protected Query buildPrefetchQuery(Collection ids)
    {
        CollectionDescriptor cds = getCollectionDescriptor();
        String[] indFkCols = getFksToThisClass();
        String[] indItemFkCols = getFksToItemClass();
        FieldDescriptor[] itemPkFields = getItemClassDescriptor().getPkFields();

        Criteria crit = buildPrefetchCriteria(ids, indFkCols, indItemFkCols, itemPkFields);

        // BRJ: do not use distinct:
        //
        // ORA-22901 cannot compare nested table or VARRAY or LOB attributes of an object type
        // Cause: Comparison of nested table or VARRAY or LOB attributes of an
        // object type was attempted in the absence of a MAP or ORDER method.
        // Action: Define a MAP or ORDER method for the object type.
        //
        // Without the distinct the resultset may contain duplicate rows

        return new QueryByMtoNCriteria(cds.getItemClass(), cds.getIndirectionTable(), crit, false);
    }

    /**
     * Build a query to read the mn-implementors
     * @param ids
     */
    protected Query buildMtoNImplementorQuery(Collection ids)
    {
        String[] indFkCols = getFksToThisClass();
        String[] indItemFkCols = getFksToItemClass();
        FieldDescriptor[] pkFields = getOwnerClassDescriptor().getPkFields();
        FieldDescriptor[] itemPkFields = getItemClassDescriptor().getPkFields();
        String[] cols = new String[indFkCols.length + indItemFkCols.length];
        int[] jdbcTypes = new int[indFkCols.length + indItemFkCols.length];

        // concatenate the columns[]
        System.arraycopy(indFkCols, 0, cols, 0, indFkCols.length);
        System.arraycopy(indItemFkCols, 0, cols, indFkCols.length, indItemFkCols.length);

        Criteria crit = buildPrefetchCriteria(ids, indFkCols, indItemFkCols, itemPkFields);

        // determine the jdbcTypes of the pks
        for (int i = 0; i < pkFields.length; i++)
        {
            jdbcTypes[i] = pkFields[i].getJdbcType().getType();
        }
        for (int i = 0; i < itemPkFields.length; i++)
        {
            jdbcTypes[pkFields.length + i] = itemPkFields[i].getJdbcType().getType();
        }

        ReportQueryByMtoNCriteria q = new ReportQueryByMtoNCriteria(getItemClassDescriptor().getClassOfObject(), cols,
                crit, false);
        q.setIndirectionTable(getCollectionDescriptor().getIndirectionTable());
        q.setJdbcTypes(jdbcTypes);

        CollectionDescriptor cds = getCollectionDescriptor();
        //check if collection must be ordered
        if (!cds.getOrderBy().isEmpty())
        {
            Iterator iter = cds.getOrderBy().iterator();
            while (iter.hasNext())
            {
                q.addOrderBy((FieldHelper) iter.next());
            }
        }
        
        return q;
    }

    /**
     * prefix the this class fk columns with the indirection table
     */
    private String[] getFksToThisClass()
    {
        String indTable = getCollectionDescriptor().getIndirectionTable();
        String[] fks = getCollectionDescriptor().getFksToThisClass();
        String[] result = new String[fks.length];

        for (int i = 0; i < result.length; i++)
        {
            result[i] = indTable + "." + fks[i];
        }

        return result;
    }

    /**
     * prefix the item class fk columns with the indirection table
     */
    private String[] getFksToItemClass()
    {
        String indTable = getCollectionDescriptor().getIndirectionTable();
        String[] fks = getCollectionDescriptor().getFksToItemClass();
        String[] result = new String[fks.length];

        for (int i = 0; i < result.length; i++)
        {
            result[i] = indTable + "." + fks[i];
        }

        return result;
    }

    /**
     * Build the multiple queries for one relationship because of limitation of IN(...)
     * 
     * @param owners Collection containing all objects of the ONE side
     */
    protected Query[] buildMtoNImplementorQueries(Collection owners, Collection children)
    {
        ClassDescriptor cld = getOwnerClassDescriptor();
        PersistenceBroker pb = getBroker();
        //Class topLevelClass = pb.getTopLevelClass(cld.getClassOfObject());
        //BrokerHelper helper = pb.serviceBrokerHelper();
        Collection queries = new ArrayList(owners.size());
        Collection idsSubset = new HashSet(owners.size());
        //Object[] fkValues;
        Object owner;
        Identity id;

        Iterator iter = owners.iterator();
        while (iter.hasNext())
        {
            owner = iter.next();
            id = pb.serviceIdentity().buildIdentity(cld, owner);
            idsSubset.add(id);
            if (idsSubset.size() == pkLimit)
            {
                queries.add(buildMtoNImplementorQuery(idsSubset));
                idsSubset.clear();
            }
        }

        if (idsSubset.size() > 0)
        {
            queries.add(buildMtoNImplementorQuery(idsSubset));
        }

        return (Query[]) queries.toArray(new Query[queries.size()]);
    }

    /**
     * Build the prefetch criteria
     *
     * @param ids Collection of identities of M side
     * @param fkCols indirection table fks to this class
     * @param itemFkCols indirection table fks to item class
     * @param itemPkFields
     */
    private Criteria buildPrefetchCriteria(Collection ids, String[] fkCols, String[] itemFkCols,
            FieldDescriptor[] itemPkFields)
    {
        if (fkCols.length == 1 && itemFkCols.length == 1)
        {
            return buildPrefetchCriteriaSingleKey(ids, fkCols[0], itemFkCols[0], itemPkFields[0]);
        }
        else
        {
            return buildPrefetchCriteriaMultipleKeys(ids, fkCols, itemFkCols, itemPkFields);
        }

    }

    /**
     * Build the prefetch criteria
     *
     * @param ids Collection of identities of M side
     * @param fkCol indirection table fks to this class
     * @param itemFkCol indirection table fks to item class
     * @param itemPkField
     * @return the Criteria 
     */
    private Criteria buildPrefetchCriteriaSingleKey(Collection ids, String fkCol, String itemFkCol,
            FieldDescriptor itemPkField)
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
            case 0 :
                break;
            case 1 :
                crit.addEqualTo(fkCol, values.get(0));
                break;
            default :
                // create IN (...) for the single key field
                crit.addIn(fkCol, values);
                break;
        }

        crit.addEqualToField(itemPkField.getAttributeName(), itemFkCol);

        return crit;
    }

    /**
     * Build the prefetch criteria
     *
     * @param ids Collection of identities of M side
     * @param fkCols indirection table fks to this class
     * @param itemFkCols indirection table fks to item class
     * @param itemPkFields
     * @return the Criteria
     */
    private Criteria buildPrefetchCriteriaMultipleKeys(Collection ids, String[] fkCols, String[] itemFkCols,
            FieldDescriptor[] itemPkFields)
    {
        Criteria crit = new Criteria();
        Criteria critValue = new Criteria();
        Iterator iter = ids.iterator();

        for (int i = 0; i < itemPkFields.length; i++)
        {
            crit.addEqualToField(itemPkFields[i].getAttributeName(), itemFkCols[i]);
        }
        
        while (iter.hasNext())
        {
            Criteria c = new Criteria();
            Identity id = (Identity) iter.next();
            Object[] val = id.getPrimaryKeyValues();

            for (int i = 0; i < val.length; i++)
            {

                if (val[i] == null)
                {
                    c.addIsNull(fkCols[i]);
                }
                else
                {
                    c.addEqualTo(fkCols[i], val[i]);
                }

            }
          
            critValue.addOrCriteria(c);
        }

        crit.addAndCriteria(critValue);
        return crit;
    }

    /**
     * Answer the FieldConversions for the PkFields 
     * @param cld
     * @return the pk FieldConversions
     */
    private FieldConversion[] getPkFieldConversion(ClassDescriptor cld)
    {
        FieldDescriptor[] pks = cld.getPkFields();
        FieldConversion[] fc = new FieldConversion[pks.length]; 
        
        for (int i= 0; i < pks.length; i++)
        {
            fc[i] = pks[i].getFieldConversion();
        }
        
        return fc;
    }
    
    /**
     * Convert the Values using the FieldConversion.sqlToJava
     * @param fcs
     * @param values
     */
    private Object[] convert(FieldConversion[] fcs, Object[] values)
    {
        Object[] convertedValues = new Object[values.length];
        
        for (int i= 0; i < values.length; i++)
        {
            convertedValues[i] = fcs[i].sqlToJava(values[i]);
        }

        return convertedValues;
    }
    
    /**
     * associate the batched Children with their owner object loop over children
     * <br><br>
     * BRJ: There is a potential problem with the type of the pks used to build the Identities.
     * When creating an Identity for the owner, the type of pk is defined by the instvars 
     * representing the pk. When creating the Identity based on the mToNImplementor the
     * type of the pk is defined by the jdbc-type of field-descriptor of the referenced class.
     * This type mismatch results in Identities not being equal.
     * Integer[] {10,20,30} is not equal Long[] {10,20,30} 
     * <br><br>
     * This problem is documented in defect OJB296. 
     * The conversion of the keys of the mToNImplementor should solve this problem.
     */
    protected void associateBatched(Collection owners, Collection children, Collection mToNImplementors)
    {
        CollectionDescriptor cds = getCollectionDescriptor();
        PersistentField field = cds.getPersistentField();
        PersistenceBroker pb = getBroker();
        Class ownerTopLevelClass = pb.getTopLevelClass(getOwnerClassDescriptor().getClassOfObject());
        Class childTopLevelClass = pb.getTopLevelClass(getItemClassDescriptor().getClassOfObject());
        Class collectionClass = cds.getCollectionClass(); // this collection type will be used:
        HashMap childMap = new HashMap();
        HashMap ownerIdsToLists = new HashMap();
        FieldConversion[] ownerFc = getPkFieldConversion(getOwnerClassDescriptor()); 
        FieldConversion[] childFc = getPkFieldConversion(getItemClassDescriptor());  

        // initialize the owner list map
        for (Iterator it = owners.iterator(); it.hasNext();)
        {
            Object owner = it.next();
            Identity oid = pb.serviceIdentity().buildIdentity(owner);
            ownerIdsToLists.put(oid, new ArrayList());
        }

        // build the children map
        for (Iterator it = children.iterator(); it.hasNext();)
        {
            Object child = it.next();
            Identity oid = pb.serviceIdentity().buildIdentity(child);
            childMap.put(oid, child);
        }

        int ownerPkLen = getOwnerClassDescriptor().getPkFields().length;
        int childPkLen = getItemClassDescriptor().getPkFields().length;
        Object[] ownerPk = new Object[ownerPkLen];
        Object[] childPk = new Object[childPkLen];

        // build list of children based on m:n implementors
        for (Iterator it = mToNImplementors.iterator(); it.hasNext();)
        {
            Object[] mToN = (Object[]) it.next();
            System.arraycopy(mToN, 0, ownerPk, 0, ownerPkLen);
            System.arraycopy(mToN, ownerPkLen, childPk, 0, childPkLen);

            // BRJ: apply the FieldConversions, OJB296
            ownerPk = convert(ownerFc, ownerPk);
            childPk = convert(childFc, childPk);

            Identity ownerId = pb.serviceIdentity().buildIdentity(null, ownerTopLevelClass, ownerPk);
            Identity childId = pb.serviceIdentity().buildIdentity(null, childTopLevelClass, childPk);

            // Identities may not be equal due to type-mismatch
            Collection list = (Collection) ownerIdsToLists.get(ownerId);
            Object child = childMap.get(childId);
            list.add(child);
        }

        // connect children list to owners
        for (Iterator it = owners.iterator(); it.hasNext();)
        {
            Object result;
            Object owner = it.next();
            Identity ownerId = pb.serviceIdentity().buildIdentity(owner);

            List list = (List) ownerIdsToLists.get(ownerId);

            if ((collectionClass == null) && field.getType().isArray())
            {
                int length = list.size();
                Class itemtype = field.getType().getComponentType();

                result = Array.newInstance(itemtype, length);

                for (int j = 0; j < length; j++)
                {
                    Array.set(result, j, list.get(j));
                }
            }
            else
            {
                ManageableCollection col = createCollection(cds, collectionClass);

                for (Iterator it2 = list.iterator(); it2.hasNext();)
                {
                    col.ojbAdd(it2.next());
                }
                result = col;
            }

            Object value = field.get(owner);
            if ((value instanceof CollectionProxyDefaultImpl) && (result instanceof Collection))
            {
                ((CollectionProxyDefaultImpl) value).setData((Collection) result);
            }
            else
            {
                field.set(owner, result);
            }
        }

    }
}
