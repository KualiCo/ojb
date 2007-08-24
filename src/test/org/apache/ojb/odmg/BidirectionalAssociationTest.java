/**
 * Author: Matthew Baird
 * mattbaird@yahoo.com
 */
package org.apache.ojb.odmg;

import java.util.Iterator;
import java.util.Collection;
import java.io.Serializable;

import org.apache.ojb.junit.ODMGTestCase;
import org.odmg.ODMGException;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * Tests a bidirectional association A<-->B
 * @see org.apache.ojb.broker.BidirectionalAssociationTest for equivalent test in PB API
 */
public class BidirectionalAssociationTest extends ODMGTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {BidirectionalAssociationTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testDeleteA() throws Exception
    {
        /**
         * create at least one A/B
         */
        createWithUpdate();
        deleteA();
    }

    public void testDeleteB() throws Exception
    {
        /**
         * create at least one A/B
         */
        createWithUpdate();
        deleteB();
    }

    public void testCreateWitUpdate() throws Exception
    {
        createWithUpdate();
    }

    /**
     * test that we can create 2 objects that have a bidirectional association in ODMG API
     */
    public void createWithUpdate() throws ODMGException
    {
        Transaction tx = odmg.newTransaction();
        long currentTime = System.currentTimeMillis();

        ObjectA a = new ObjectA();
        a.setPk("A" + currentTime);
        ObjectB b = new ObjectB();
        b.setPk("B" + currentTime);

        tx.begin();
        database.makePersistent(a);
        database.makePersistent(b);
        tx.commit();

        tx.begin();
        tx.lock(a, Transaction.WRITE);
        tx.lock(b, Transaction.WRITE);
        a.setRelatedB(b);
        b.setRelatedA(a);
        tx.commit();

         /**
         * now make sure they are in db, A first, then B
         */
        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        int i = 0;
        query.create("select bidirectionalAssociationObjectA from " + ObjectA.class.getName() + " where pk=$1");
        query.bind("A"+currentTime);
        Collection all = (Collection) query.execute();
        Iterator it = all.iterator();
        while (it.hasNext())
        {
            i++;
            a = (ObjectA) it.next();
            if (a.getRelatedB() == null)
                fail("a should have had a related b");
        }
        if (i > 1)
            fail("should have found only one bidirectionalAssociationObjectA, instead found: " + i);

        query = odmg.newOQLQuery();
        i = 0;
        query.create("select bidirectionalAssociationObjectB from " + ObjectB.class.getName() + " where pk=$1");
        query.bind("B"+currentTime);
        all = (Collection) query.execute();
        it = all.iterator();
        while (it.hasNext())
        {
            i++;
            b = (ObjectB) it.next();
            if (b.getRelatedA() == null)
                fail("b should have had a related a");

        }
        if (i > 1)
            fail("should have found only one bidirectionalAssociationObjectB, instead found: " + i);
        tx.commit();
    }

    /**
     * this test doesn't work as OJB won't do the insert then execute the update.
     * @throws ODMGException
     */
    public void testCreateWithoutUpdate() throws ODMGException
    {
        Transaction tx = odmg.newTransaction();
        long currentTime = System.currentTimeMillis();
        ObjectA a = new ObjectA();
        a.setPk("A" + currentTime);
        ObjectB b = new ObjectB();
        b.setPk("B" + currentTime);

        tx.begin();
        b.setRelatedA(a);
        a.setRelatedB(b);
        // we use a FK from ObjectB to ObjectA, thus we
        // make persistent B
        database.makePersistent(b);
        // not needed
        //database.makePersistent(a);
        tx.commit();

        /**
         * now make sure they are in db, A first, then B
         */
        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        int i = 0;
        query.create("select bidirectionalAssociationObjectA from " + ObjectA.class.getName() + " where pk=$1");
        query.bind("A"+currentTime);
         Collection all = (Collection) query.execute();
        Iterator it = all.iterator();
        while (it.hasNext())
        {
            i++;
            it.next();
        }
        if (i > 1)
            fail("should have found only one bidirectionalAssociationObjectA, instead found: " + i);

        query = odmg.newOQLQuery();
        i = 0;
        query.create("select bidirectionalAssociationObjectB from " + ObjectB.class.getName() + " where pk=$1");
        query.bind("B"+currentTime);
        all = (Collection) query.execute();
        it = all.iterator();
        while (it.hasNext())
        {
            i++;
            it.next();
        }
        if (i > 1)
            fail("should have found only one bidirectionalAssociationObjectB, instead found: " + i);

    }

    /**
     * no clue why this isn't working.
     * @throws Exception
     */
    public void testGetA() throws Exception
    {
        /**
         * create at least one A/B combo
         */
        deleteA();
        deleteB();
        createWithUpdate();

        OQLQuery query = odmg.newOQLQuery();
        int i = 0;
        query.create("select allA from " + ObjectA.class.getName());
        Transaction tx = odmg.newTransaction();
        tx.begin();
        Collection all = (Collection) query.execute();
        Iterator it = all.iterator();
        ObjectA temp = null;
        while (it.hasNext())
        {
            temp = (ObjectA) it.next();
            if (temp.getRelatedB() == null)
                fail("should have relatedB");
            i++;
        }
        tx.commit();
        if (i == 0)
            fail("Should have found at least 1  bidirectionalAssociationObjectA object");
    }

    public void testGetB() throws Exception
    {
        /**
         * create at least one A/B combo
         */
        deleteA();
        deleteB();
        createWithUpdate();

        OQLQuery query = odmg.newOQLQuery();
        int i = 0;
        query.create("select bidirectionalAssociationObjectB from " + ObjectB.class.getName());
        Transaction tx = odmg.newTransaction();
        tx.begin();
        Collection all = (Collection) query.execute();
        Iterator it = all.iterator();
        ObjectB temp = null;
        while (it.hasNext())
        {
            temp = (ObjectB) it.next();
            if (temp.getRelatedA() == null)
                fail("should have relatedA");
            i++;
        }
        tx.commit();
        if (i == 0)
            fail("Should have found at least 1  bidirectionalAssociationObjectA object");
    }

    /**
     * test deleting an object participating in a bidirectional associative relationship. Will throw if it can't delete.
     * @throws Exception
     */
    public void deleteA() throws Exception
    {
        ObjectA a;
        ObjectB b;

        OQLQuery query = odmg.newOQLQuery();
        query.create("select bidirectionalAssociationObjectA from " + ObjectA.class.getName());
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        Collection all = (Collection) query.execute();
        Iterator it = all.iterator();

        while (it.hasNext())
        {
            a = (ObjectA)it.next();
            b = a.getRelatedB();
            if (b != null)
            {
                tx.lock(b, Transaction.WRITE);
                b.setRelatedA(null);   // break relationship to avoid ri violation
            }
            database.deletePersistent(a);
        }
        tx.commit();
    }

    /**
     * test deleting an object participating in a bidirectional associative relationship. Will throw if it can't delete.
     * @throws Exception
     */
    public void deleteB() throws Exception
    {
        ObjectA a;
        ObjectB b;

        OQLQuery query = odmg.newOQLQuery();
        query.create("select bidirectionalAssociationObjectB from " + ObjectB.class.getName());
        Transaction tx = odmg.newTransaction();
        tx.begin();
        Collection all = (Collection) query.execute();
        Iterator it = all.iterator();

        while (it.hasNext())
        {
            b = (ObjectB)it.next();
            a = b.getRelatedA();
            if (a != null)
            {
                tx.lock(a, Transaction.WRITE);
                a.setRelatedB(null);    // break relationship to avoid ri violation
            }
            database.deletePersistent(b);
        }

        tx.commit();
    }

    /**
     * Insert the method's description here.
     * Creation date: (24.12.2000 00:33:40)
     */
    public BidirectionalAssociationTest(String name)
    {
        super(name);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }



    public static class ObjectA implements Serializable
    {
        private String pk;
        private String fkToB;
        private ObjectB relatedB;

        public ObjectA()
        {
        }

        public String getPk()
        {
            return pk;
        }

        public void setPk(String pk)
        {
            this.pk = pk;
        }

        public String getFkToB()
        {
            return fkToB;
        }

        public void setFkToB(String fkToB)
        {
            this.fkToB = fkToB;
        }

        public ObjectB getRelatedB()
        {
            return relatedB;
        }

        public void setRelatedB(ObjectB relatedB)
        {
            this.relatedB = relatedB;
        }
    }


    public static class ObjectB implements Serializable
    {
        private String pk;
        private String fkToA;
        private ObjectA relatedA;

        public ObjectB()
        {
        }

        public String getPk()
        {
            return pk;
        }

        public void setPk(String pk)
        {
            this.pk = pk;
        }

        public String getFkToA()
        {
            return fkToA;
        }

        public void setFkToA(String fkToA)
        {
            this.fkToA = fkToA;
        }

        public ObjectA getRelatedA()
        {
            return relatedA;
        }

        public void setRelatedA(ObjectA relatedA)
        {
            this.relatedA = relatedA;
        }
    }
}
