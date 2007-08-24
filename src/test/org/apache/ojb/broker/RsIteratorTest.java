package org.apache.ojb.broker;

import java.util.Iterator;

import junit.framework.TestCase;
import org.apache.ojb.broker.query.*;
import org.apache.ojb.broker.accesslayer.RsIterator;

/**
 * Test case for the RsIterator
 *
 * @author <a href="mailto:rongallagher@bellsouth.net">Ron Gallagher<a>
 * @version $Id: RsIteratorTest.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class RsIteratorTest extends TestCase
{
    private PersistenceBroker broker;

    public static void main(String[] args)
    {
        String[] arr = {RsIteratorTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public RsIteratorTest(String name)
    {
        super(name);
    }

    public void setUp()
    {
        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
    }

    public void tearDown()
    {
        if(broker != null)
        {
            broker.close();
        }
    }

    public void testRsIterator() throws Exception
    {
        String name = "testRsIterator_" + System.currentTimeMillis();
        prepareTest(name);

        Criteria criteria = new Criteria();
        criteria.addLike("name", name+"*");
        Query query = new QueryByCriteria(ObjectRepository.Component.class, criteria);

        Iterator it = broker.getIteratorByQuery(query);
        int k = 0;
        while(it.hasNext())
        {
            it.next();
            k++;
        }
        assertEquals("Wrong number of items found", 2, k);
    }

    /**
     * Test RsIterator cleanup on PB.commitTransaction()
     */
    public void testRsIteratorAutomaticCleanupCheck_1() throws Exception
    {
        String name = "testRsIteratorAutomaticCleanupCheck_1_" + System.currentTimeMillis();
        prepareTest(name);

        Criteria criteria = new Criteria();
        criteria.addLike("name", name+"*");
        Query query = new QueryByCriteria(ObjectRepository.Component.class, criteria);

        Iterator it = broker.getIteratorByQuery(query);
        it.hasNext();
        broker.beginTransaction();
        broker.commitTransaction();
        /*
        if tx was commited we invalidate RsIterator instance
        */
        try
        {
            it.next();
            fail("We expect RsIterator has released resources on pb.commit..");
        }
        catch (RsIterator.ResourceClosedException e)
        {
            assertTrue(true);
        }


        it = broker.getIteratorByQuery(query);
        it.hasNext();
        it.next();
        broker.beginTransaction();
        broker.commitTransaction();
        /*
        if tx was commited we invalidate RsIterator instance
        */
        try
        {
            it.hasNext();
            it.next();
            fail("We expect RsIterator has released resources on pb.commit..");
        }
        catch (RsIterator.ResourceClosedException e)
        {
            assertTrue(true);
        }
    }

    /**
     * Test RsIterator cleanup on PB.abortTransaction()
     */
    public void testRsIteratorAutomaticCleanupCheck_2() throws Exception
    {
        String name = "testRsIteratorAutomaticCleanupCheck_" + System.currentTimeMillis();
        prepareTest(name);

        Criteria criteria = new Criteria();
        criteria.addLike("name", name+"*");
        Query query = new QueryByCriteria(ObjectRepository.Component.class, criteria);

        Iterator it = broker.getIteratorByQuery(query);
        it.hasNext();
        broker.beginTransaction();
        broker.abortTransaction();
        /*
        if tx was aborted we invalidate RsIterator instance
        */
        try
        {
            it.next();
            fail("We expect RsIterator has released resources on pb.commit..");
        }
        catch (RsIterator.ResourceClosedException e)
        {
            assertTrue(true);
        }


        it = broker.getIteratorByQuery(query);
        it.hasNext();
        it.next();
        broker.beginTransaction();
        broker.abortTransaction();
        /*
        if tx was aborted we invalidate RsIterator instance
        */
        try
        {
            it.hasNext();
            it.next();
            fail("We expect RsIterator has released resources on pb.commit..");
        }
        catch (RsIterator.ResourceClosedException e)
        {
            assertTrue(true);
        }
    }

    /**
     * Test RsIterator cleanup on PB.close()
     */
    public void testRsIteratorAutomaticCleanupCheck_3() throws Exception
    {
        String name = "testRsIteratorAutomaticCleanupCheck_" + System.currentTimeMillis();
        prepareTest(name);

        Criteria criteria = new Criteria();
        criteria.addLike("name", name+"*");
        Query query = new QueryByCriteria(ObjectRepository.Component.class, criteria);

        Iterator it = broker.getIteratorByQuery(query);
        broker.close();
        /*
        if was closed we invalidate RsIterator instance
        */
        try
        {
            if(it.hasNext()) it.next();
            fail("We expect RsIterator has released resources on pb.commit..");
        }
        catch (RsIterator.ResourceClosedException e)
        {
            assertTrue(true);
        }
    }

    /**
     * Test RsIterator cleanup on PB.abortTransaction()
     */
    public void testRsIteratorUserCleanup_1() throws Exception
    {
        String name = "testRsIteratorAutomaticCleanupCheck_" + System.currentTimeMillis();
        prepareTest(name);

        Criteria criteria = new Criteria();
        criteria.addLike("name", name+"*");
        Query query = new QueryByCriteria(ObjectRepository.Component.class, criteria);

        Iterator it = broker.getIteratorByQuery(query);

        /*
        TODO: After integration of setAutoRelease into OJBIterator and changes
        in PB interface getIteratorXXX methods we don't need these casts any longer
        */
        if(!(it instanceof RsIterator))
        {
            // skip test
            return;
        }

        // TODO: Remove this cast one day
        ((RsIterator) it).setAutoRelease(false);

        it.hasNext();
        broker.beginTransaction();
        broker.abortTransaction();
        /*
        if tx was aborted we invalidate RsIterator instance
        */
        try
        {
            it.next();
            fail("We expect RsIterator has released resources on pb.commit..");
        }
        catch (RsIterator.ResourceClosedException e)
        {
            assertTrue(true);
        }


        it = broker.getIteratorByQuery(query);
        // TODO: Remove this cast one day
        ((RsIterator) it).setAutoRelease(false);

        it.hasNext();
        it.next();
        broker.beginTransaction();
        broker.abortTransaction();
        /*
        if tx was aborted we invalidate RsIterator instance
        */
        try
        {
            it.hasNext();
            it.next();
            fail("We expect RsIterator has released resources on pb.commit..");
        }
        catch (RsIterator.ResourceClosedException e)
        {
            assertTrue(true);
        }
    }

    /**
     * Test RsIterator cleanup on PB.abortTransaction()
     */
    public void testRsIteratorUserCleanup_2() throws Exception
    {
        String name = "testRsIteratorAutomaticCleanupCheck_" + System.currentTimeMillis();
        prepareTest(name);

        Criteria criteria = new Criteria();
        criteria.addLike("name", name+"*");
        Query query = new QueryByCriteria(ObjectRepository.Component.class, criteria);

        Iterator it = broker.getIteratorByQuery(query);

        /*
        TODO: After integration of setAutoRelease into OJBIterator and changes
        in PB interface getIteratorXXX methods we don't need these casts any longer
        */
        if(!(it instanceof RsIterator))
        {
            // skip test
            return;
        }

        // TODO: Remove this cast one day
        ((RsIterator) it).setAutoRelease(false);

        while(it.hasNext())
        {
            ObjectRepository.Component c = (ObjectRepository.Component) it.next();
            assertNotNull(c.getId());
        }

        try
        {
            ((RsIterator) it).relative(1);
        }
        catch(RsIterator.ResourceClosedException e)
        {
            fail("RsIterator should not close resources by itself");
        }
        catch (PersistenceBrokerException ignore)
        {
        }

        // TODO: Remove this cast one day
        ((RsIterator) it).releaseDbResources();
    }


    private void prepareTest(String objectName)
    {
        ObjectRepository.Component c1 = new ObjectRepository.Component();
        c1.setName(objectName + "_1");
        ObjectRepository.Component c2 = new ObjectRepository.Component();
        c2.setName(objectName + "_2");

        broker.beginTransaction();
        broker.store(c1);
        broker.store(c2);
        broker.commitTransaction();
    }

    /**
     * Test retrieving data via the rsIterator
     * test by Ron Gallagher
     */
    public void testInternUsedRsIterator() throws Exception
    {
        // Build the query
        Criteria criteria = new Criteria();
        criteria.addEqualTo("id", new Integer(1));
        Query query = new QueryByCriteria(Person.class, criteria);
        // Run the query.
        Person person = (Person) broker.getObjectByQuery(query);
        assertNotNull("Person with id 1 was not found", person);
    }

}
