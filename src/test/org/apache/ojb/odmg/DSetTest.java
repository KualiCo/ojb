package org.apache.ojb.odmg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.ojb.junit.ODMGTestCase;
import org.odmg.DSet;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * Tests for OJB {@link org.odmg.DSet} implementation.
 */
public class DSetTest extends ODMGTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {DSetTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public DSetTest(String name)

    {
        super(name);
    }

    protected DListTest.DObject createObject(String name) throws Exception
    {
        DListTest.DObject obj = new DListTest.DObject();
        obj.setName(name);
        obj.setRandomName("rnd_"+((int)(Math.random()*1000)));

        return obj;
    }

    public void testAddingLockupWithTx() throws Exception
    {
        // create a unique name:
        final String name = "testAdding_" + System.currentTimeMillis();

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        // create DSet and bound by name
        DSet list = odmg.newDSet();
        database.bind(list, name);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        Object obj = database.lookup(name);
        tx.commit();
        assertNotNull("binded DSet not found", obj);

        tx.begin();
        // add objects to list
        for (int i = 0; i < 5; i++)
        {
            DListTest.DObject a = createObject(name);
            list.add(a);
        }
        tx.commit();

        // check current list
        Iterator iter = list.iterator();
        while (iter.hasNext())
        {
            DListTest.DObject a = (DListTest.DObject) iter.next();
            assertNotNull(a);
        }

        tx.begin();
        tx.getBroker().clearCache();

        // lookup list and check entries
        DSet lookedUp = (DSet) database.lookup(name);
        assertNotNull("binded DSet not found", lookedUp);

        //System.out.println("sequence of items in lookedup list:");
        iter = lookedUp.iterator();
        Iterator iter1 = list.iterator();
        while (iter.hasNext())
        {
            DListTest.DObject a = (DListTest.DObject) iter.next();
            DListTest.DObject b = (DListTest.DObject) iter1.next();
            assertNotNull(a);
            assertNotNull(b);
            assertEquals(a.getId(), b.getId());
        }
        tx.commit();

        // add new entries to list
        tx.begin();
        for (int i = 0; i < 3; i++)
        {
            DListTest.DObject a = createObject(name + "_new_entry");
            list.add(a);
        }
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        lookedUp = (DSet) database.lookup(name);
        iter = lookedUp.iterator();
        iter1 = list.iterator();
        assertEquals("Wrong number of DListEntry found", 8, list.size());
        while (iter.hasNext())
        {
            DListTest.DObject a = (DListTest.DObject) iter.next();
            DListTest.DObject b = (DListTest.DObject) iter1.next();
            assertNotNull(a);
            assertNotNull(b);
            assertEquals(a.getId(), b.getId());
        }
        tx.commit();
        assertNotNull("binded DSet not found", lookedUp);
    }

    public void testReadAndStore() throws Exception
    {
        // create a unique name:
        final String name = "testReadAndStore_" + System.currentTimeMillis();

        // create test objects
        Transaction tx = odmg.newTransaction();
        tx.begin();
        // add objects to list
        for (int i = 0; i < 5; i++)
        {
            tx.lock(createObject(name), Transaction.WRITE);
        }
        tx.commit();

        tx.begin();
        // query test objects
        OQLQuery q = odmg.newOQLQuery();
        q.create("select all from "+DListTest.DObject.class.getName()+" where name=$1");
        q.bind(name);
        Collection ret = (Collection) q.execute();
        // check result list size
        assertEquals(5, ret.size());
        // do read lock
        for (Iterator it = ret.iterator(); it.hasNext(); )
        {
            tx.lock(it.next(), Transaction.READ);
        }
        // create new list for results
        ArrayList result = new ArrayList();
        result.addAll(ret);
        tx.commit();
    }

    public void testIterateWithoutTx() throws Exception
    {
        // create a unique name:
        final String name = "testAdding_" + System.currentTimeMillis();

        // get DSet and fill with objects
        DSet list = odmg.newDSet();
        Transaction tx = odmg.newTransaction();
        tx.begin();
        for (int i = 0; i < 5; i++)
        {
            DListTest.DObject a = createObject(name);
            list.add(a);
        }
        // bind the new list
        database.bind(list, name);
        tx.commit();

        tx = odmg.newTransaction();
        tx.begin();
        Object obj = database.lookup(name);
        tx.commit();
        assertNotNull("binded DSet not found", obj);

        // iterate list
        Iterator iter = list.iterator();
        while (iter.hasNext())
        {
            DListTest.DObject a = (DListTest.DObject) iter.next();
            assertNotNull(a);
        }
        assertEquals(5, list.size());

        tx = odmg.newTransaction();
        tx.begin();
        ((TransactionExt) odmg.currentTransaction()).getBroker().clearCache();
        DSet lookedUp = (DSet) database.lookup(name);
        tx.commit();
        assertNotNull("binded DSet not found", lookedUp);

        //System.out.println("sequence of items in lookedup list:");
        iter = lookedUp.iterator();
        Iterator iter1 = list.iterator();
        while (iter.hasNext())
        {
            DListTest.DObject a = (DListTest.DObject) iter.next();
            DListTest.DObject b = (DListTest.DObject) iter1.next();
            assertNotNull(a);
            assertNotNull(b);
            assertEquals(a.getId(), b.getId());
        }
    }

    /**
     * this test checks if removing item from DSet works
     */
    public void testRemoving() throws Exception
    {
        // create a unique name:
        String name = "testRemoving_" + System.currentTimeMillis();

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        DSet list = odmg.newDSet();
        // bind the list to the name:
        database.bind(list, name);

        Object first = null;
        Object second = null;
        for (int i = 0; i < 5; i++)
        {
            DListTest.DObject a = createObject(name);
            list.add(a);
            if(i==1) first = a;
            if(i==2) second = a;
        }
        assertEquals(5, list.size());
        tx.commit();

        // delete two items
        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        ((HasBroker) odmg.currentTransaction()).getBroker().clearCache();
        DSet lookedUp = (DSet) database.lookup(name);
        assertNotNull("database lookup does not find the named DSet", lookedUp);
        assertEquals("Wrong number of list entries", 5, lookedUp.size());
        lookedUp.remove(first);
        lookedUp.remove(second);
        tx.commit();

        // check if deletion was successful
        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();
        lookedUp = (DSet) database.lookup(name);
        tx.commit();

        assertEquals(3, lookedUp.size());
    }


    public void testAdding() throws Exception
    {
        // create a unique name:
        String name = "testAdding_" + System.currentTimeMillis();

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        DSet list = odmg.newDSet();
        database.bind(list, name);
        tx.commit();

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        for (int i = 0; i < 5; i++)
        {
            DListTest.DObject a = createObject(name);
            list.add(a);
        }

        list.add(createObject(name+"_posNew1"));
        list.add(createObject(name+"_posNew2"));
        list.add(createObject(name+"_posNew3"));
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        // System.out.println("list: " + list);
        // System.out.println("lookup list: " + db.lookup(name));
        tx.commit();

        //System.out.println("sequence of items in list:");
        Iterator iter = list.iterator();
        DListTest.DObject a;
        while (iter.hasNext())
        {
            a = (DListTest.DObject) iter.next();
            assertNotNull(a);
            //System.out.print(a.getArticleId() + ", ");
        }
        assertEquals(8, list.size());


        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();
        DSet lookedUp = (DSet) database.lookup(name);
        // System.out.println("lookup list: " + lookedUp);
        assertNotNull("database lookup does not find DSet", lookedUp);
        assertEquals(8, lookedUp.size());
        iter = lookedUp.iterator();
        while (iter.hasNext())
        {
            a = (DListTest.DObject) iter.next();
        }
        tx.commit();
    }

    public void testDSet() throws Exception
    {
        String name = "testDSet_" + System.currentTimeMillis();
        String set_1 = "set_1_" + System.currentTimeMillis();
        String set_2 = "set_2_" + System.currentTimeMillis();

        Transaction tx = odmg.newTransaction();
        tx.begin();

        DListTest.DObject a, b, c, d, e;
        a = createObject(name);
        b = createObject(name);
        c = createObject(name);
        d = createObject(name);
        e = createObject(name);

        DSet set1 = odmg.newDSet();
        DSet set2 = odmg.newDSet();

        set1.add(a);
        set1.add(b);
        set1.add(c);

        set2.add(b);
        set2.add(c);
        set2.add(d);
        set2.add(e);

        database.bind(set1, set_1);
        database.bind(set2, set_2);
        tx.commit();

        // low lookup both sets
        tx = odmg.newTransaction();
        tx.begin();
        ((HasBroker) tx).getBroker().clearCache();
        DSet set1a = (DSet) database.lookup(set_1);
        DSet set2a = (DSet) database.lookup(set_2);

        // check looked up sets
        assertTrue(set1a.containsAll(set1));
        assertTrue(set2a.containsAll(set2));

        // now TestThreadsNLocks set operations:
        DSet set3 = set1.difference(set2);
        assertEquals(1, set3.size());

        set3 = set1.intersection(set2);
        assertEquals(2, set3.size());

        set3 = set1.union(set2);
        assertEquals(5, set3.size());

        assertTrue(set1.properSubsetOf(set3));
        assertTrue(set2.properSubsetOf(set3));

        assertTrue(set3.properSupersetOf(set1));
        assertTrue(set3.properSupersetOf(set2));

        assertTrue(!set1.properSubsetOf(set2));

        tx.commit();
    }
}
