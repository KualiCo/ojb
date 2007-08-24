package org.apache.ojb.odmg;

import java.util.List;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.shared.Person;
import org.apache.ojb.odmg.shared.PersonImpl;
import org.apache.ojb.odmg.shared.Site;
import org.odmg.Implementation;
import org.odmg.OQLQuery;
import org.odmg.Transaction;
import org.odmg.TransactionNotInProgressException;


/**
 * Collection of test cases sent by OJB users
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: UserTestCases.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class UserTestCases extends ODMGTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {UserTestCases.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**
     * Send by Antonio
     * Note: The name attribute was declared as
     * unique in the DB.
     */
    public void testDuplicateInsertion() throws Exception
    {
        String name = "testDuplicateInsertion_" + System.currentTimeMillis();
        String nameNew = "testDuplicateInsertion_New_" + System.currentTimeMillis();

        //System.out.println("TEST: Database open");

        // insert an object with UNIQUE field NAME="A site"
        //System.out.println("TEST: Insert first object");
        newSite(odmg, name, 2, 1);

        // insert another object with UNIQUE field NAME="A site"
        // This should not create a new object (UNIQUE fields conflict) but
        // should resume gracefuly
        //System.out.println("TEST: Insert second object, should fail");
        try
        {
            newSite(odmg, name, 3, 2);
            assertTrue("We should get a SqlException 'Violation of unique index'", false);
        }
        catch(Exception e)
        {
            // we wait for this exception
            assertTrue(true);
        }

        // insert an object with new UNIQUE field NAME
        // should always work
        //System.out.println("TEST: Insert third object");
        try
        {
            newSite(odmg, nameNew, 1, 2);
            assertTrue(true);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            assertTrue("This exception should not happend: " + e.getMessage(), false);
            throw e;
        }
        //System.out.println("TEST: Database closed");
    }

    private void newSite(Implementation odmg, String name, int year, int semester) throws Exception
    {
        Transaction tx = null;
        tx = odmg.newTransaction();
        Site site = null;
        tx.begin();

        site = new Site();
        site.setName(name);
        site.setYear(new Integer(year));
        site.setSemester(new Integer(semester));

        tx.lock(site, Transaction.WRITE);
        tx.commit();
    }

    public void testSimpleQueryDelete() throws Exception
    {
        String name = "testSimpleQueryDelete - " + System.currentTimeMillis();

        Site site = new Site();
        site.setName(name);
        Transaction tx = odmg.newTransaction();
        tx.begin();
        tx.lock(site, Transaction.WRITE);
        tx.commit();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select sites from " + Site.class.getName() + " where name=$1");
        query.bind(name);
        tx.begin();
        List result = (List) query.execute();
        if(result.size() == 0)
        {
            fail("Stored object not found");
        }
        tx.commit();

        tx.begin();
        database.deletePersistent(site);
        tx.commit();

        query = odmg.newOQLQuery();
        query.create("select sites from " + Site.class.getName() + " where name=$1");
        query.bind(name);
        tx.begin();
        List result2 = (List) query.execute();
        if(result2.size() > 0)
        {
            fail("We should not found deleted objects");
        }
        tx.commit();
    }

    /**
     * User test case posted by Charles:
     * <p/>
     * Up to now, we've been just using the broker layer. I now have a usecase
     * where we will need to use the ODMG layer. We do not want to use implicit
     * locking; I want my developers to explicit lock each object to an ODMG
     * transaction (implicit locking generates loads of queries for all the proxy
     * collections - should be fixed since OJB1.0.3).
     * <p/>
     * It seems that something 'funny' happens if implicit locking is turned off -
     * objects are not marked as being "dirty" when changed - even when they are
     * explicitly lock to the transaction.
     * <p/>
     * As I am a complete novice in the ways of the ODMG, I don't really know where
     * to look to sort this issue out so I have added a new test method to
     * org.apache.ojb.odmg.UserTestCases (it should be attached to this email).
     * Essentially, it creates an object and persists it; retrieves and updates it;
     * then flushes the cache, and retrieves it again to ensure the update worked.
     * If ImplicitLocking is TRUE, the test passes. If ImplicitLocking is FALSE,
     * the test fails.
     * <p/>
     * I think this is incorrect, and would dearly like this to be resolved.
     * <p/>
     * thma's comment: IMO this works as designed. objects must be locked to
     * an ODMG tx before any modifications are taking place.
     * I simply moved the lock two lines up and the test passed.
     */
    public void testImplicitLocking() throws Exception
    {
        String name = "testImplicitLocking - " + System.currentTimeMillis();
        String queryString = "select sites from " + Site.class.getName() + " where name = $1";

        /* Create an object */
        Site site = new Site();
        site.setName(name);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        // disable implicit locking for this tx instance
        // this setting was used for the life-time of the tx instance
        tx.setImplicitLocking(false);
        tx.begin();
        database.makePersistent(site);
        tx.commit();

        /* Retrieve from the object created, and set the year*/
        OQLQuery query = odmg.newOQLQuery();
        query.create(queryString);
        query.bind(name);

        tx.begin();
        List result = (List) query.execute();
        assertEquals(1, result.size());
        site = (Site) result.get(0);
        assertNotNull(site);
        assertNull(site.getYear());
        tx.lock(site, Transaction.WRITE);
        site.setYear(new Integer(2003));
        tx.commit();

        /* Flush the cache, and retrieve the object again */
        query = odmg.newOQLQuery();
        query.create(queryString);
        query.bind(name);
        tx.begin();
        tx.getBroker().clearCache();
        result = (List) query.execute();
        assertEquals(1, result.size());
        site = (Site) result.get(0);
        assertNotNull(site);
        assertNotNull("year should not be null", site.getYear());
        tx.commit();
    }

    /**
     * store an object and then retrieve it by id.
     */
    public void testStoreRetrieveSameTxn() throws Exception
    {
        String name = "testStoreRetrieveSameTxn_" + System.currentTimeMillis();
        Person mum = new PersonImpl();
        mum.setFirstname(name);

        TransactionExt txn = (TransactionExt) odmg.newTransaction();
        txn.begin();
        txn.lock(mum, Transaction.WRITE);
        // System.out.println("locked for write: " + mum);
        txn.commit();

        txn.begin();
        txn.getBroker().clearCache();
        Identity mumId = txn.getBroker().serviceIdentity().buildIdentity(mum);
        Person mum2 = (Person) txn.getBroker().getObjectByIdentity(mumId);
        // System.out.println("retrieved: " + mum2);
        txn.commit();
        assertNotNull(mum2);
        assertEquals(name, mum2.getFirstname());
    }

    public void testRetrieveNonExistent()
    {
        try
        {
            TransactionExt tx = (TransactionExt) odmg.newTransaction();
            tx.begin();
            // construct an id that does not exist in the database
            Identity id = tx.getBroker().serviceIdentity().buildIdentity(PersonImpl.class, new Integer(-1));
            tx.getBroker().getObjectByIdentity(id);
            tx.abort();
        }
        catch(Exception exc)
        {
            exc.printStackTrace();
            fail("caught unexpected exception: " + exc.toString());
        }
    }

    /**
     * Not recommended to use such a construct!!!
     */
    public void testRetrieveOutsideTxn()
    {
        try
        {
            // construct an id that does not exist in the database
            Identity id = new Identity(Person.class, Person.class, new Integer[]{new Integer(-1)});
            TransactionImpl txn = (TransactionImpl) odmg.newTransaction();
            try
            {
                txn.getObjectByIdentity(id);
                fail("expected TransactionNotInProgressException not thrown");
            }
            catch(TransactionNotInProgressException exc)
            {
                // expected.
            }
        }
        catch(Exception exc)
        {
            exc.printStackTrace();
            fail("caught unexpected exception: " + exc.toString());
        }
    }
}
