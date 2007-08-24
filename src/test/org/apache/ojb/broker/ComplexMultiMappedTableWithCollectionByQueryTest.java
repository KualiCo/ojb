/**
 * Author: Matthew Baird
 * mattbaird@yahoo.com
 */
package org.apache.ojb.broker;

import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

import java.sql.Timestamp;
import java.util.Iterator;

/**
 * This TestClass tests OJB ability to handle Contract Version Effectiveness patterns.
 */
public class ComplexMultiMappedTableWithCollectionByQueryTest extends PBTestCase
{
    private int COUNT = 10;
    private static Class CLASS = ComplexMultiMappedTableWithCollectionByQueryTest.class;

    public static void main(String[] args)
    {
        String[] arr = {CLASS.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**
     * Insert the method's description here.
     * Creation date: (24.12.2000 00:33:40)
     */
    public ComplexMultiMappedTableWithCollectionByQueryTest(String name)
    {
        super(name);
    }

    /**
     * Insert the method's description here.
     * Creation date: (06.12.2000 21:58:53)
     */
    public void setUp() throws Exception
    {
        super.setUp();
        createTestData();
    }

    private int deleteAllData()
    {
        int number_deleted = 0;
        Criteria crit = new Criteria();
        Query q = QueryFactory.newQuery(ComplexMultiMapped.PersistentA.class, crit);
        broker.beginTransaction();
        Iterator iter = broker.getCollectionByQuery(q).iterator();
        while (iter.hasNext())
        {
            broker.delete(iter.next());
            number_deleted++;
        }
        /**
         * will delete all B and D and E (both of which extends B)
         */
        crit = new Criteria();
        q = QueryFactory.newQuery(ComplexMultiMapped.PersistentB.class, crit);
        iter = broker.getCollectionByQuery(q).iterator();

        while (iter.hasNext())
        {
            broker.delete(iter.next());
            number_deleted++;
        }
        /**
         * will delete all C
         */
        crit = new Criteria();
        q = QueryFactory.newQuery(ComplexMultiMapped.PersistentC.class, crit);
        iter = broker.getCollectionByQuery(q).iterator();
        while (iter.hasNext())
        {
            broker.delete(iter.next());
            number_deleted++;
        }
        broker.commitTransaction();
        return number_deleted;
    }

    private void createTestData()
    {
        /**
         * create COUNT of each object
         */
        broker.beginTransaction();
        for (int i = 0; i < COUNT; i++)
        {
            ComplexMultiMapped.PersistentA a = new ComplexMultiMapped.PersistentA();
            a.setValue1("a");
            a.setValue2(i);
            a.setValue3(new Timestamp(System.currentTimeMillis()));
            broker.store(a);

            ComplexMultiMapped.PersistentB b = new ComplexMultiMapped.PersistentB();
            b.setValue4("b");
            b.setValue5(i);
            b.setValue6(new Timestamp(System.currentTimeMillis()));
            broker.store(b);

            ComplexMultiMapped.PersistentC c = new ComplexMultiMapped.PersistentC();
            c.setValue1("c");
            c.setValue2(i);
            c.setValue3(new Timestamp(System.currentTimeMillis()));
            c.setValue4("c");
            c.setValue5(i);
            c.setValue6(new Timestamp(System.currentTimeMillis()));
            broker.store(c);

            ComplexMultiMapped.PersistentD d = new ComplexMultiMapped.PersistentD();
            d.setValue1("d");
            d.setValue2(i);
            d.setValue3(new Timestamp(System.currentTimeMillis()));
            d.setValue4("d");
            d.setValue5(i);
            d.setValue6(new Timestamp(System.currentTimeMillis()));
            broker.store(d);

            ComplexMultiMapped.PersistentE e = new ComplexMultiMapped.PersistentE();
            e.setValue1("e");
            e.setValue2(i);
            e.setValue3(new Timestamp(System.currentTimeMillis()));
            e.setValue4("e");
            e.setValue5(i);
            e.setValue6(new Timestamp(System.currentTimeMillis()));
            broker.store(e);

            ComplexMultiMapped.PersistentF f = new ComplexMultiMapped.PersistentF();
            f.setValue1("f");
            f.setValue2(i);
            f.setValue3(new Timestamp(System.currentTimeMillis()));
            f.setValue4("f");
            f.setValue5(i);
            f.setValue6(new Timestamp(System.currentTimeMillis()));
            broker.store(f);
        }
        broker.commitTransaction();
    }

    public void testCreate()
    {
        createTestData();
    }

    public void testGet()
    {
        /**
         * get to a clean, known state.
         */
        deleteAllData();
        /**
         * now create a bunch of test data.
         */
        createTestData();

        Criteria crit = new Criteria();
        Query q;
        Iterator iter;
        int count;
        /**
         * check all A's
         */
        q = QueryFactory.newQuery(ComplexMultiMapped.PersistentA.class, crit);
        iter = broker.getCollectionByQuery(q).iterator();
        ComplexMultiMapped.PersistentA a = null;
        count = 0;
        while (iter.hasNext())
        {
            a = (ComplexMultiMapped.PersistentA) iter.next();
            if (!a.getValue1().equals("a"))
            {
                fail("getValue1 should have returned 'a', it in fact returned '" + a.getValue1() + "'");
            }
            count++;
        }
        if (count != COUNT)
            fail("should have found " + COUNT + " ComplexMultiMapped.PersistentA's, in fact found " + count);

		assertEquals("counted size", broker.getCount(q), count);

        /**
         * check all B's
         */
        crit = new Criteria();
        q = QueryFactory.newQuery(ComplexMultiMapped.PersistentB.class, crit);
        iter = broker.getCollectionByQuery(q).iterator();
        ComplexMultiMapped.PersistentB b = null;
        count = 0;
        while (iter.hasNext())
        {
            b = (ComplexMultiMapped.PersistentB) iter.next();
            if (!b.getValue4().equals("b") && !b.getValue4().equals("d") && !b.getValue4().equals("e") && !b.getValue4().equals("f"))
            {
                fail("getValue4 should have returned 'b' or 'd' or 'e' or 'f' (from extent), it in fact returned '" + b.getValue4() + "'");
            }
            count++;
        }
        /**
         * should find ALL b's, d's, e's and f's, so COUNT*4 is the expected result
         */
        if (count != COUNT*4)
            fail("should have found " + (COUNT *4) + " ComplexMultiMapped.PersistentB's, in fact found " + count);

		assertEquals("counted size", broker.getCount(q), count);

        /**
         * check all C's
         */
        crit = new Criteria();
        q = QueryFactory.newQuery(ComplexMultiMapped.PersistentC.class, crit);
        iter = broker.getCollectionByQuery(q).iterator();
        ComplexMultiMapped.PersistentC c = null;
        count = 0;
        while (iter.hasNext())
        {
            c = (ComplexMultiMapped.PersistentC) iter.next();
            if (!c.getValue1().equals("c"))
            {
                fail("getValue1 should have returned 'c', it in fact returned '" + c.getValue1() + "'");
            }
            count++;
        }
        if (count != COUNT)
            fail("should have found " + COUNT + " ComplexMultiMapped.PersistentC's, in fact found " + count);

		assertEquals("counted size", broker.getCount(q), count);

        /**
         * check all D's
         */
        crit = new Criteria();
        q = QueryFactory.newQuery(ComplexMultiMapped.PersistentD.class, crit);
        iter = broker.getCollectionByQuery(q).iterator();
        ComplexMultiMapped.PersistentD d = null;
        count = 0;
        while (iter.hasNext())
        {
            d = (ComplexMultiMapped.PersistentD) iter.next();
            if (!d.getValue1().equals("d"))
            {
                fail("getValue1 should have returned 'd', it in fact returned '" + d.getValue1() + "'");
            }
            count++;
        }
        if (count != COUNT)
            fail("should have found " + COUNT + " ComplexMultiMapped.PersistentD's, in fact found " + count);

		assertEquals("counted size", broker.getCount(q), count);

		/**
         * check all E's
         */
        crit = new Criteria();
        q = QueryFactory.newQuery(ComplexMultiMapped.PersistentE.class, crit);
        iter = broker.getCollectionByQuery(q).iterator();
        ComplexMultiMapped.PersistentE e = null;
        count = 0;
        while (iter.hasNext())
        {
            e = (ComplexMultiMapped.PersistentE) iter.next();
            if (!e.getValue1().equals("e") && !e.getValue1().equals("f"))
            {
                fail("getValue1 should have returned 'e' or 'f' (extent), it in fact returned '" + e.getValue1() + "'");
            }
            count++;
        }
        if (count != COUNT *2)
            fail("should have found " + (COUNT*2) + " ComplexMultiMapped.PersistentE's, in fact found " + count);

		assertEquals("counted size", broker.getCount(q), count);

        /**
                 * check all F's NEeds to be figured out.
                crit = new Criteria();
                q = QueryFactory.newQuery(ComplexMultiMapped.PersistentF.class, crit);
                iter = broker.getCollectionByQuery(q).iterator();
                ComplexMultiMapped.PersistentF f = null;
                count = 0;
                while (iter.hasNext())
                {
                    f = (ComplexMultiMapped.PersistentF) iter.next();
                    if (!f.getValue1().equals("f"))
                    {
                        fail("getValue1 should have returned 'f', it in fact returned '" + f.getValue1() + "'");
                    }
                    count++;
                }
                if (count != COUNT)
                    fail("should have found " + COUNT + " ComplexMultiMapped.PersistentF's, in fact found " + count);
                 */

    }

    public void testDeleteWithData()
    {
        /**
         * put some data in
         */
        createTestData();
        /**
         * then delete it.
         */
        int number_deleted = deleteAllData();
        /**
         * we should have the number of classes we put in (4) * the number we put in (COUNT of each)
         */
        if (number_deleted < (5*COUNT))
        {
            fail("Should have deleted at least " + (4*COUNT) + " actually deleted " + number_deleted);
        }
    }

    public void testDeleteWithNoData()
    {
        /**
         * clear all data
         */
        deleteAllData();
        /**
         * call delete again: there should be nothing.
         */
        int number_deleted = deleteAllData();
        if (number_deleted !=0)
        {
            fail("Should have deleted 0, instead deleted " + number_deleted);
        }
    }
}
