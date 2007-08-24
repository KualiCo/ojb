package org.apache.ojb.broker;

import org.apache.ojb.junit.PBTestCase;

/**
 * This TestClass tests OJB facilities for optimistic locking.
 */
public class OptimisticLockingTest extends PBTestCase
{

	public static void main(String[] args)
	{
		String[] arr = {OptimisticLockingTest.class.getName()};
		junit.textui.TestRunner.main(arr);
	}

	public OptimisticLockingTest(String name)
	{
		super(name);
	}

	/** Test optimistic Lock by version.*/
	public void testVersionLock() throws Exception
	{
        LockedByVersion obj = new LockedByVersion();
        obj.setValue("original");
        Identity oid = new Identity(obj, broker);
        broker.beginTransaction();
        broker.store(obj);
        broker.commitTransaction();

        broker.clearCache();
        LockedByVersion copy1 = (LockedByVersion) broker.getObjectByIdentity(oid);
        broker.clearCache();
        LockedByVersion copy2 = (LockedByVersion) broker.getObjectByIdentity(oid);

        copy1.setValue("copy 1");
        copy2.setValue("copy 2");
        assertEquals("Expect same version number", copy1.getVersion(), copy2.getVersion());

        broker.beginTransaction();
        broker.store(copy1);
        broker.commitTransaction();
        assertTrue("Expect different version number", copy1.getVersion() != copy2.getVersion());
        try
        {
            // as copy1 has already been stored the version info of copy2
            // is out of sync with the database !
            broker.beginTransaction();
            broker.store(copy2);
            broker.commitTransaction();
        }
        catch (OptimisticLockException ex)
        {
            assertTrue(true);
            //LoggerFactory.getDefaultLogger().debug(ex);
            broker.abortTransaction();
            return;
        }
        fail("Should throw an Optimistic Lock exception");
	}

	/**
	 * demonstrates how OptimisticLockExceptions can be used
	 * to handle resynchronization of conflicting instances.
	 */
	public void testLockHandling() throws Exception
	{
        LockedByVersion obj = new LockedByVersion();
        obj.setValue("original");
        Identity oid = new Identity(obj, broker);
        broker.beginTransaction();
        broker.store(obj);
        broker.commitTransaction();

        broker.clearCache();
        LockedByVersion copy1 = (LockedByVersion) broker.getObjectByIdentity(oid);
        broker.clearCache();
        LockedByVersion copy2 = (LockedByVersion) broker.getObjectByIdentity(oid);

        copy1.setValue("copy 1");
        copy2.setValue("copy 2");
        assertEquals("Expect same version number", copy1.getVersion(), copy2.getVersion());

        broker.beginTransaction();
        broker.store(copy1);
        broker.commitTransaction();
        assertTrue("Expect different version number", copy1.getVersion() != copy2.getVersion());
        try
        {
            // as copy1 has already been stored the version info of copy2
            // is out of sync with the database !
            broker.beginTransaction();
            broker.store(copy2);
            broker.commitTransaction();
        }
        catch (OptimisticLockException ex)
        {
            // obtain conflicting object from exception
            Object conflictingObject = ex.getSourceObject();

            // get a synchronized instance
            broker.removeFromCache(conflictingObject);
            Object syncronizedObject = broker.getObjectByIdentity(new Identity(conflictingObject, broker));

            // modify synchronized copy and call store again without trouble
            ((LockedByVersion) syncronizedObject).setValue("copy 3");
            if(!broker.isInTransaction()) broker.beginTransaction();
            broker.store(syncronizedObject);
            broker.commitTransaction();
            return;
        }
        fail("Should throw an Optimistic Lock exception");
	}


/** Test optimistic Lock by timestamp.*/
	public void testTimestampLock() throws Exception
	{
		LockedByTimestamp obj = new LockedByTimestamp();
        obj.setValue("original");
        Identity oid = new Identity(obj, broker);
        broker.beginTransaction();
        broker.store(obj);
        broker.commitTransaction();

        broker.clearCache();
        LockedByTimestamp copy1 = (LockedByTimestamp) broker.getObjectByIdentity(oid);
        broker.clearCache();
        LockedByTimestamp copy2 = (LockedByTimestamp) broker.getObjectByIdentity(oid);

        /*
        //mysql timestamp does not support milliseconds
        arminw:
        For proper test we need millisecond precision, so if mysql does not support
        this, better we let fail this test for mysql
        */
        Thread.sleep(50);

        copy1.setValue("copy 1");
        copy2.setValue("copy 2");
        assertEquals("Expect same version number", copy1.getTimestamp(), copy2.getTimestamp());

        broker.beginTransaction();
        broker.store(copy1);
        broker.commitTransaction();
        assertTrue("Expect different version number", copy1.getTimestamp() != copy2.getTimestamp());
        try
        {
            // as copy1 has already been stored the timestamp info
            // of copy2  is out of sync with the database !
            broker.beginTransaction();
            broker.store(copy2);
            broker.commitTransaction();

            fail("Should throw an Optimistic Lock exception");
        }
        catch (OptimisticLockException ex)
        {
            assertTrue(true);
            broker.abortTransaction();
        }

        if(!broker.isInTransaction()) broker.beginTransaction();
        broker.delete(copy1);
        broker.commitTransaction();

        try
        {
            broker.beginTransaction();
            broker.delete(copy1);
            broker.commitTransaction();
        }
        catch (OptimisticLockException e)
        {
           // BRJ: exception thrown if object has been modified or deleted
           //
           // fail("If an object which use optimistic locking was deleted two times, OJB" +
           //         " should not throw an optimistic locking exception: "+e.getMessage());
            broker.abortTransaction();
           return;
        }
        fail("Should throw an Optimistic Lock exception");
    }

}
