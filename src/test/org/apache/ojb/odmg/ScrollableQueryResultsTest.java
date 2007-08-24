package org.apache.ojb.odmg;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.oql.EnhancedOQLQuery;
import org.apache.ojb.odmg.shared.Person;
import org.apache.ojb.odmg.shared.PersonImpl;
import org.odmg.OQLQuery;
import org.odmg.QueryInvalidException;
import org.odmg.Transaction;

/**
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 * @version $Id: ScrollableQueryResultsTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class ScrollableQueryResultsTest extends ODMGTestCase
{
    private static final int CONTROL_SIZE = 50;

    public static void main(String[] args)
    {
        String[] arr = {ScrollableQueryResultsTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public ScrollableQueryResultsTest(String name)
    {
        super(name);
    }

    private void createData() throws Exception
    {
        Transaction tx = odmg.newTransaction();
        tx.begin();
        for(int i = 1; i <= CONTROL_SIZE; i++)
        {
            Person aPerson = new PersonImpl();
            aPerson.setFirstname("firstname" + i);
            aPerson.setLastname("lastname" + i);
            database.makePersistent(aPerson);
        }
        tx.commit();
    }

    private void removeAllData() throws Exception
    {
        Transaction tx = odmg.newTransaction();
        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        String sql = "select allPersons from " + Person.class.getName();
        query.create(sql);
        Collection allPersons = (Collection) query.execute();
        Iterator it = allPersons.iterator();
        while(it.hasNext())
        {
            database.deletePersistent(it.next());
        }
        tx.commit();
    }

    /**
     * test getting all (make sure basic operation is still functional)
     */
    public void testGetAllUnrestricted() throws Exception
    {
        // 1. remove all data
        removeAllData();
        // 2. Insert a bunch of articles objects into the database

        createData();

        // 3. Get a list of some articles
        Transaction tx = odmg.newTransaction();
        tx.begin();

        OQLQuery query = odmg.newOQLQuery();
        String sql = "select allPersons from " + Person.class.getName();
        query.create(sql);
        Collection allPersons = (Collection) query.execute();
        // Iterator over the restricted articles objects
        Iterator it = allPersons.iterator();
        int count = 0;
        while(it.hasNext())
        {
            it.next();
            count++;
        }
        tx.commit();

        // check that we got the right amount back.
        if(count != (CONTROL_SIZE))
        {
            fail("count not right, found <"
                    + count
                    + "> should have got <"
                    + (CONTROL_SIZE)
                    + "> This failure is expected if your driver doesn't support advanced JDBC operations.");
        }
    }

    /**
     * test starting at an index and ending at an index.
     */

    public void testGetSomeA() throws Exception
    {
        int start = 10;
        int end = 15;
        // 1. remove all data
        removeAllData();
        // 2. Insert a bunch of articles objects into the database
        createData();
        // 3. Get a list of some articles
        Transaction tx = odmg.newTransaction();
        tx.begin();
        EnhancedOQLQuery query = odmg.newOQLQuery();
        String sql = "select somePersons from " + Person.class.getName();
        query.create(sql, start, end);
        Collection somePersons = (Collection) query.execute();

        // Iterator over the restricted articles objects
        Iterator it = somePersons.iterator();
        int count = 0;
        while(it.hasNext())
        {
            it.next();
            count++;
        }
        tx.commit();
        // check that we got the right amount back.
        if(count != (end - start + 1))
        {
            fail("count not right, found <"
                    + count
                    + "> should have got <"
                    + (end - start)
                    + "> This failure is expected if your driver doesn't support advanced JDBC operations.");
        }
    }

    /**
     * test start at beginning, and go to an end index.
     */
    public void testGetSomeB() throws Exception
    {
        int start = Query.NO_START_AT_INDEX;
        int end = 15;
        try
        {
            // 1. remove all data
            removeAllData();
            // 2. Insert a bunch of articles objects into the database
            createData();

            // 3. Get a list of some articles
            Transaction tx = odmg.newTransaction();
            tx.begin();

            EnhancedOQLQuery query = odmg.newOQLQuery();
            String sql = "select somePersons from " + Person.class.getName();
            query.create(sql, start, end);
            Collection somePersons = (Collection) query.execute();
            // Iterator over the restricted articles objects
            Iterator it = somePersons.iterator();
            int count = 0;
            while(it.hasNext())
            {
                it.next();
                count++;
            }
            tx.commit();
            // check that we got the right amount back.
            if(count != end)
            {
                fail("count not right, found <"
                        + count
                        + "> should have got <"
                        + (end)
                        + "> This failure is expected if your driver doesn't support advanced JDBC operations.");
            }
        }
        catch(Throwable t)
        {
            t.printStackTrace(System.out);
            fail("testGetSomeB: " + t.getMessage());
        }
    }

    /**
     * test starting at a specific place, and have no ending index
     */
    public void testGetSomeC() throws Exception
    {
        int start = 10;
        int end = Query.NO_END_AT_INDEX;
        // 1. remove all data
        removeAllData();
        // 2. Insert a bunch of articles objects into the database

        createData();

        // 3. Get a list of some articles
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        PersistenceBroker broker = tx.getBroker();

        Connection conn = broker.serviceConnectionManager().getConnection();
        /**
         * only execute this test if scrolling is supported.
         */
        if(!conn.getMetaData()
                .supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE))
        {
            tx.commit();
            return;
        }

        EnhancedOQLQuery query = odmg.newOQLQuery();
        String sql = "select somePersons from " + Person.class.getName();
        query.create(sql, start, end);
        Collection somePersons = (Collection) query.execute();
        // Iterator over the restricted articles objects
        Iterator it = somePersons.iterator();
        int count = 0;
        while(it.hasNext())
        {
            it.next();
            count++;
        }
        tx.commit();
        // check that we got the right amount back.
        if(count != (CONTROL_SIZE - start + 1)) /* +1 because the last row is inclusive */
        {
            fail("count not right, found <"
                    + count
                    + "> should have got <"
                    + (CONTROL_SIZE - start + 1)
                    + "> This failure is expected if your driver doesn't support advanced JDBC operations.");
        }
    }

    /**
     * test the condition where start is after end.
     */
    public void testGetSomeD() throws Exception
    {
        int start = 10;
        int end = 5;
        Transaction tx = odmg.newTransaction();
        try
        {
            tx.begin();
            EnhancedOQLQuery query = odmg.newOQLQuery();
            String sql = "select somePersons from " + Person.class.getName();
            query.create(sql, start, end);
            query.execute();
            fail("should have thrown QueryInvalidException");
        }
        catch(QueryInvalidException iqe)
        {
            // we wait for this exception
            assertTrue(true);
            tx.abort();
        }
    }

    /**
     * test condition where start and end are the same.
     */
    public void testGetSomeE() throws Exception
    {
        int start = 10;
        int end = 10;
        Transaction tx = null;
        try
        {
            tx = odmg.newTransaction();
            tx.begin();
            EnhancedOQLQuery query = odmg.newOQLQuery();
            String sql = "select somePersons from " + Person.class.getName();
            query.create(sql, start, end);
            query.execute();
            fail("should have thrown QueryInvalidException");
        }
        catch(QueryInvalidException iqe)
        {
            // we expect that exception
            assertTrue(true);
        }
        catch(Throwable t)
        {
            t.printStackTrace(System.out);
            fail("testGetSomeC: " + t.getMessage());
        }
        finally
        {
            if(tx != null)
            {
                tx.abort();
            }
        }
    }
}
