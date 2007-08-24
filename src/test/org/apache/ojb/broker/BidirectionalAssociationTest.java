package org.apache.ojb.broker;

import java.util.Iterator;
import java.util.Collection;

import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.junit.PBTestCase;

/**
 * tests a bidirectional association A<-->B
 * @see org.apache.ojb.odmg.BidirectionalAssociationTest for equivalent test in ODMG
 */
public class BidirectionalAssociationTest extends PBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {BidirectionalAssociationTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public BidirectionalAssociationTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testAutoRefreshTrue()
    {
        String pkSuffix = "_" + System.currentTimeMillis();
        ObjectReferenceDescriptor ord_A = null;
        ObjectReferenceDescriptor ord_B = null;
        ClassDescriptor cld_A = broker.getClassDescriptor(BidirectionalAssociationObjectA.class);
        ord_A = cld_A.getObjectReferenceDescriptorByName("relatedB");
        ClassDescriptor cld_B = broker.getClassDescriptor(BidirectionalAssociationObjectB.class);
        ord_B = cld_B.getObjectReferenceDescriptorByName("relatedA");
        boolean oldA = ord_A.isRefresh();
        boolean oldB = ord_B.isRefresh();
        try
        {
            ord_A.setRefresh(true);
            ord_B.setRefresh(true);
            createWithUpdate(pkSuffix);
            Criteria crit = new Criteria();
            crit.addLike("pk", "%" + pkSuffix);
            Query query = QueryFactory.newQuery(BidirectionalAssociationObjectB.class, crit);
            Collection result = broker.getCollectionByQuery(query);
            assertEquals(1, result.size());
        }
        finally
        {
            if(ord_A != null) ord_A.setRefresh(oldA);
            if(ord_B != null) ord_B.setRefresh(oldB);
        }
    }

    public void testCreateDelete()
    {
        String pkSuffix = "_" + System.currentTimeMillis();
        createWithUpdate(pkSuffix);
        deleteAllA();
        deleteAllB();
    }

    private void createWithUpdate(String pkSuffix)
    {
        broker.beginTransaction();
        BidirectionalAssociationObjectA a = new BidirectionalAssociationObjectA();
        a.setPk("A" + pkSuffix);
        BidirectionalAssociationObjectB b = new BidirectionalAssociationObjectB();
        b.setPk("B" + pkSuffix);
        broker.store(a);
        broker.store(b);
        /**
         * now set relations
         */
        b.setRelatedA(a);
        a.setRelatedB(b);
        /**
         * and update
         */
        broker.store(a);
        broker.store(b);
        broker.commitTransaction();
    }

    public void testGetA() throws Exception
    {
        String pkSuffix = "_" + System.currentTimeMillis();
        createWithUpdate(pkSuffix);

        Criteria crit = new Criteria();
        Query q;
        Iterator iter;
        q = QueryFactory.newQuery(BidirectionalAssociationObjectA.class, crit);
        iter = broker.getIteratorByQuery(q);
        BidirectionalAssociationObjectA temp = null;
        while (iter.hasNext())
        {
            temp = (BidirectionalAssociationObjectA) iter.next();
            if (temp.getRelatedB() == null)
            {
                fail("relatedB not found");
            }
        }

        deleteAllA();
        deleteAllB();
    }

    public void testGetB() throws Exception
    {
        String pkSuffix = "_" + System.currentTimeMillis();
        createWithUpdate(pkSuffix);

        Criteria crit = new Criteria();
        Query q;
        Iterator iter;
        q = QueryFactory.newQuery(BidirectionalAssociationObjectB.class, crit);
        iter = broker.getIteratorByQuery(q);
        BidirectionalAssociationObjectB temp = null;
        while (iter.hasNext())
        {
            temp = (BidirectionalAssociationObjectB) iter.next();
            if (temp.getRelatedA() == null)
            {
                fail("relatedA not found");
            }
        }

        deleteAllA();
        deleteAllB();
    }

    public void testDeleteA()
    {
        String pkSuffix = "_" + System.currentTimeMillis();
        createWithUpdate(pkSuffix);
        deleteAllA();
        deleteAllB();
    }

    public void testDeleteB()
    {
        String pkSuffix = "_" + System.currentTimeMillis();
        createWithUpdate(pkSuffix);
        deleteAllB();
        deleteAllA();
    }

    private void deleteAllA()
    {
        Criteria crit = new Criteria();
        Query q;
        Iterator iter;
        q = QueryFactory.newQuery(BidirectionalAssociationObjectA.class, crit);
        iter = broker.getIteratorByQuery(q);
        BidirectionalAssociationObjectA temp = null;
        broker.beginTransaction();
        while (iter.hasNext())
        {
            temp = (BidirectionalAssociationObjectA) iter.next();
            BidirectionalAssociationObjectB b = temp.getRelatedB();
            if (b != null)
            {
                b.setRelatedA(null);
                broker.store(b);
            }
            broker.delete(temp);
        }
        broker.commitTransaction();
    }

    private void deleteAllB()
    {
        Criteria crit = new Criteria();
        Query q;
        Iterator iter;
        q = QueryFactory.newQuery(BidirectionalAssociationObjectB.class, crit);
        iter = broker.getIteratorByQuery(q);
        BidirectionalAssociationObjectB temp = null;
        broker.beginTransaction();
        while (iter.hasNext())
        {
            temp = (BidirectionalAssociationObjectB) iter.next();
            BidirectionalAssociationObjectA a = temp.getRelatedA();
            if (a != null)
            {
                a.setRelatedB(null);
                broker.store(a);
            }
            broker.delete(temp);
        }
        broker.commitTransaction();
    }
}
