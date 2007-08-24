package org.apache.ojb.odmg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.ojb.junit.ODMGTestCase;
import org.odmg.DBag;
import org.odmg.DList;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * Tests for OJB {@link org.odmg.DList} implementation.
 */
public class DListTest extends ODMGTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {DListTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public DListTest(String name)

    {
        super(name);
    }

    protected DObject createObject(String name) throws Exception
    {
        DObject obj = new DObject();
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
        // create DList and bound by name
        DList list = odmg.newDList();
        database.bind(list, name);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        Object obj = database.lookup(name);
        tx.commit();
        assertNotNull("binded DList not found", obj);

        tx.begin();
        // add objects to list
        for (int i = 0; i < 5; i++)
        {
            DObject a = createObject(name);
            list.add(a);
        }
        tx.commit();

        // check current list
        Iterator iter = list.iterator();
        while (iter.hasNext())
        {
            DObject a = (DObject) iter.next();
            assertNotNull(a);
        }

        tx.begin();
        tx.getBroker().clearCache();

        // lookup list and check entries
        DList lookedUp = (DList) database.lookup(name);
        assertNotNull("binded DList not found", lookedUp);

        //System.out.println("sequence of items in lookedup list:");
        iter = lookedUp.iterator();
        Iterator iter1 = list.iterator();
        while (iter.hasNext())
        {
            DObject a = (DObject) iter.next();
            DObject b = (DObject) iter1.next();
            assertNotNull(a);
            assertNotNull(b);
            assertEquals(a.getId(), b.getId());
        }
        tx.commit();

        // add new entries to list
        tx.begin();
        for (int i = 0; i < 3; i++)
        {
            DObject a = createObject(name + "_new_entry");
            list.add(a);
        }
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        lookedUp = (DList) database.lookup(name);
        iter = lookedUp.iterator();
        iter1 = list.iterator();
        assertEquals("Wrong number of DListEntry found", 8, list.size());
        while (iter.hasNext())
        {
            DObject a = (DObject) iter.next();
            DObject b = (DObject) iter1.next();
            assertNotNull(a);
            assertNotNull(b);
            assertEquals(a.getId(), b.getId());
        }
        tx.commit();
        assertNotNull("binded DList not found", lookedUp);
    }

    public void testRemoveAdd() throws Exception
    {
        // create a unique name:
        final String name = "testRemoveAdd_" + System.currentTimeMillis();

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        // create DList and bound by name
        DList list = odmg.newDList();
        database.bind(list, name);

        // add object to list
        for (int i = 0; i < 5; i++)
        {
            DObject a = createObject(name);
            list.add(a);
        }
        tx.commit();

        // check current list
        Iterator iter = list.iterator();
        while (iter.hasNext())
        {
            DObject a = (DObject) iter.next();
            assertNotNull(a);
        }

        tx.begin();
        tx.getBroker().clearCache();

        // lookup list and check entries
        DList lookedUp = (DList) database.lookup(name);
        assertNotNull("binded DList not found", lookedUp);

        //System.out.println("sequence of items in lookedup list:");
        iter = lookedUp.iterator();
        Iterator iter1 = list.iterator();
        while (iter.hasNext())
        {
            DObject a = (DObject) iter.next();
            DObject b = (DObject) iter1.next();
            assertNotNull(a);
            assertNotNull(b);
            assertEquals(a.getId(), b.getId());
        }
        tx.commit();

        // add and remove new entries
        tx.begin();
        for (int i = 0; i < 3; i++)
        {
            DObject a = createObject(name + "_new_entry_NOT_PERSIST");
            list.add(a);
            list.remove(list.size()-1);
        }
        tx.commit();


        tx.begin();
        tx.getBroker().clearCache();
        lookedUp = (DList) database.lookup(name);
        iter = lookedUp.iterator();
        iter1 = list.iterator();
        assertEquals("Wrong number of DListEntry found", 5, list.size());
        while (iter.hasNext())
        {
            DObject a = (DObject) iter.next();
            DObject b = (DObject) iter1.next();
            assertNotNull(a);
            assertNotNull(b);
            assertEquals(a.getId(), b.getId());
        }
        tx.commit();
        assertNotNull("binded DList not found", lookedUp);


        tx.begin();
        for (int i = 0; i < 3; i++)
        {
            DObject a = createObject(name + "_new_entry_new_persist");
            list.add(a);
            list.remove(0);
        }
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        lookedUp = (DList) database.lookup(name);
        iter = lookedUp.iterator();
        iter1 = list.iterator();
        assertEquals("Wrong number of DListEntry found", 5, list.size());
        while (iter.hasNext())
        {
            DObject a = (DObject) iter.next();
            DObject b = (DObject) iter1.next();
            assertNotNull(a);
            assertNotNull(b);
            assertEquals(a.getId(), b.getId());
        }
        tx.commit();
        assertNotNull("binded DList not found", lookedUp);
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
        q.create("select all from "+DObject.class.getName()+" where name=$1");
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

        // get DList and fill with objects
        List list = odmg.newDList();
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        for (int i = 0; i < 5; i++)
        {
            DObject a = createObject(name);
            list.add(a);
        }
        // bind the new list
        database.bind(list, name);
        tx.commit();

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        Object obj = database.lookup(name);
        tx.commit();
        assertNotNull("binded DList not found", obj);

        // iterate list
        Iterator iter = list.iterator();
        while (iter.hasNext())
        {
            DObject a = (DObject) iter.next();
            assertNotNull(a);
        }
        assertEquals(5, list.size());

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();

        List lookedUp = (List) database.lookup(name);
        tx.commit();
        assertNotNull("binded DList not found", lookedUp);

        // DList doesn't support #set(...) method, so 
        list = new ArrayList(list);
        lookedUp = new ArrayList(lookedUp);

        Collections.sort(list, new Comparator(){
            public int compare(Object o1, Object o2)
            {
                DObject d1 = (DObject) o1;
                DObject d2 = (DObject) o2;
                return d1.getId().compareTo(d2.getId());
            }
        });

        Collections.sort(lookedUp, new Comparator(){
            public int compare(Object o1, Object o2)
            {
                DObject d1 = (DObject) o1;
                DObject d2 = (DObject) o2;
                return d1.getId().compareTo(d2.getId());
            }
        });

        assertEquals(list.size(), lookedUp.size());

        for(int i = 0; i < lookedUp.size(); i++)
        {
            DObject a = (DObject) lookedUp.get(i);
            DObject aa = (DObject) list.get(i);
            assertNotNull(a);
            assertNotNull(aa);
            assertEquals(a.getId(), aa.getId());
        }
    }

    /**
     * this test checks if removing item from DList works
     */
    public void testRemoving() throws Exception
    {
        // create a unique name:
        String name = "testRemoving_" + System.currentTimeMillis();

        Transaction tx = odmg.newTransaction();
        tx.begin();
        DList list = odmg.newDList();
        // bind the list to the name:
        database.bind(list, name);

        for (int i = 0; i < 5; i++)
        {
            DObject a = createObject(name);
            list.add(a);
        }
        assertEquals(5, list.size());
        tx.commit();

        // delete two items
        tx = odmg.newTransaction();
        tx.begin();
        ((HasBroker) odmg.currentTransaction()).getBroker().clearCache();
        DList lookedUp = (DList) database.lookup(name);
        assertNotNull("database lookup does not find the named DList", lookedUp);
        assertEquals("Wrong number of list entries", 5, lookedUp.size());
        lookedUp.remove(2);
        lookedUp.remove(1);
        tx.commit();

        // check if deletion was successful
        tx = odmg.newTransaction();
        tx.begin();
        ((HasBroker) odmg.currentTransaction()).getBroker().clearCache();
        lookedUp = (DList) database.lookup(name);
        tx.commit();

        assertEquals(3, lookedUp.size());
    }


    public void testAddingWithIndex() throws Exception
    {
        // create a unique name:
        String name = "testAddingWithIndex_" + System.currentTimeMillis();

        Transaction tx = odmg.newTransaction();
        tx.begin();
        DList list = odmg.newDList();
        database.bind(list, name);
        tx.commit();

        tx = odmg.newTransaction();
        tx.begin();
        for (int i = 0; i < 5; i++)
        {
            DObject a = createObject(name);
            list.add(a);
        }

        list.add(2, createObject(name+"_pos2"));
        list.add(0, createObject(name+"_pos0"));
        list.add(7, createObject(name+"_pos7"));
        tx.commit();

        tx.begin();
        ((TransactionImpl) tx).getBroker().clearCache();
        // System.out.println("list: " + list);
        // System.out.println("lookup list: " + db.lookup(name));
        tx.commit();

        //System.out.println("sequence of items in list:");
        Iterator iter = list.iterator();
        DObject a;
        while (iter.hasNext())
        {
            a = (DObject) iter.next();
            assertNotNull(a);
            //System.out.print(a.getArticleId() + ", ");
        }


        tx = odmg.newTransaction();
        tx.begin();
        ((TransactionImpl) tx).getBroker().clearCache();
        DList lookedUp = (DList) database.lookup(name);
        // System.out.println("lookup list: " + lookedUp);
        assertNotNull("database lookup does not find DList", lookedUp);
        assertEquals(8, lookedUp.size());
        iter = lookedUp.iterator();
        while (iter.hasNext())
        {
            a = (DObject) iter.next();
        }
        tx.commit();
    }

    public void testDBag() throws Exception
    {
        String name = "testDBag_" + System.currentTimeMillis();

        Transaction tx = odmg.newTransaction();
        tx.begin();
        DBag bag1 = odmg.newDBag();
        DBag bag2 = odmg.newDBag();
        DObject a, b, c, d, e;
        a = createObject(name);
        b = createObject(name);
        c = createObject(name);
        d = createObject(name);
        e = createObject(name);
        bag1.add(a);
        bag1.add(b);
        bag1.add(c);
        bag2.add(b);
        bag2.add(c);
        bag2.add(d);
        bag2.add(e);
        DBag bag3 = bag1.difference(bag2);
        assertEquals("should contain only 1 element", 1, bag3.size());

        bag3 = bag1.intersection(bag2);
        assertEquals("should contain two elements", 2, bag3.size());

        tx.commit();
    }

    public static class DObject
    {
        Integer id;
        String name;
        String randomName;

        public DObject()
        {
        }

        public boolean equals(Object obj)
        {
            if(obj instanceof DObject)
            {
                DObject target = ((DObject)obj);
                return new EqualsBuilder()
                        .append(id, target.getId())
                        .append(name, target.getName())
                        .append(randomName, target.getRandomName())
                        .isEquals();
            }
            else
            {
                return false;
            }
        }

        public int hashCode()
        {
            return new HashCodeBuilder().append(id).append(name).append(randomName).hashCode();
        }

        public String toString()
        {
            ToStringBuilder buf = new ToStringBuilder(this);
            buf.append("id", id);
            buf.append("name", name);
            buf.append("randonName", randomName);
            return buf.toString();
        }

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getRandomName()
        {
            return randomName;
        }

        public void setRandomName(String randomName)
        {
            this.randomName = randomName;
        }
    }
}
