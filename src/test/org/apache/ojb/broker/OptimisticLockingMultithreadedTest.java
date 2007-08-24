package org.apache.ojb.broker;

import org.apache.commons.lang.SerializationUtils;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.junit.JUnitExtensions;

/**
 * Test optimistic-locking implementation with multiple threads.
 * Different threads try to update the same instance / or a copy
 * of the same object.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: OptimisticLockingMultithreadedTest.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class OptimisticLockingMultithreadedTest extends JUnitExtensions.MultiThreadedTestCase
{
    private static int threadCount;
    static final String msg = "Thread write order: ";

    public static void main(String[] args)
    {
        String[] arr = {OptimisticLockingMultithreadedTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public OptimisticLockingMultithreadedTest(String s)
    {
        super(s);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testLockingOfObject() throws Exception
    {
        LockedByVersion targetObject = createLockedByVersion();
        storeObject(targetObject);

        // number of concurrent threads to run
        int threads = 6;
        // number of updates each thread performs against the object
        int objectUpdates = 20;

        TestCaseRunnable tct [] = new TestCaseRunnable[threads];
        for (int i = 0; i < threads; i++)
        {
            /*
            several threads try to lock the same object (shared object),
            the other threads lock deep copies of the shared object
            */
            if (i % 2 == 0)
                tct[i] = new LockHandle(targetObject, objectUpdates);
            else
                tct[i] = new LockHandle(
                        (LockedByVersion) SerializationUtils.clone(targetObject), objectUpdates);
        }
        System.out.println("*** START - Multithreaded lock test ***");
        System.out.println("Number of concurrent threads: " + threads);
        System.out.println("Number of object updates per thread: " + objectUpdates);
        System.out.println("Each thread try to update the same object. If an OptimisticLockException" +
                " was thrown, the thread wait and try later again (200 attempts, then fail)");
        // run test classes
        runTestCaseRunnables(tct);
        System.out.println(targetObject.getValue());
        System.out.println("An '-' indicate write success at first attempt");
        System.out.println("An '+' indicate write success after several OptimisticLockException");
        System.out.println("*** END - Multithreaded lock test  ***");
    }

    private LockedByVersion createLockedByVersion()
    {
        int number = newThreadKey();
        LockedByVersion lo = new LockedByVersion();
        lo.setValue(msg + number);
        return lo;
    }

    private void storeObject(Object obj) throws Exception
    {
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        try
        {
            broker.beginTransaction();
            broker.store(obj);
            broker.commitTransaction();
        }
        finally
        {
            if(broker != null) broker.close();
        }
    }

    public static synchronized int newThreadKey()
    {
        return threadCount++;
    }

    //=======================================================================
    // inner classes
    //=======================================================================

    class LockHandle extends TestCaseRunnable
    {
        LockedByVersion obj;
        int threadNumber;
        int objectUpdates = 30;

        public LockHandle(LockedByVersion obj, int objectUpdates)
        {
            super();
            this.objectUpdates = objectUpdates;
            this.obj = obj;
        }

        public void runTestCase() throws Throwable
        {
            threadNumber = newThreadKey();
            for (int i = 0; i < objectUpdates; i++)
            {
                updateObject(obj, false);
            }
        }

        private int counter = 0;
        private static final int maxAttempts = 200;
        private static final int nearMax = (int) (maxAttempts * 0.9);

        private void updateObject(LockedByVersion obj, boolean LNGEthrown) throws Exception
        {
            PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            try
            {
                broker.beginTransaction();
                updateName(obj, LNGEthrown);
                broker.store(obj);
                broker.commitTransaction();
            }
            catch (OptimisticLockException e)
            {
                if(broker != null)
                {
                    broker.abortTransaction();
                    broker.close();
                }
                // we try X times again to update the object
                if (counter < maxAttempts)
                {
                    counter++;
                    if (counter > nearMax)
                        LoggerFactory.getDefaultLogger().warn("OptimisticLockingMultithreadedTest: thread "
                                + threadNumber + " waits " + counter
                                + " times to update object. Maximal attempts before fail are " + maxAttempts
                                + ". This can be a result of low hardware.");
                    Thread.sleep(10);
                    PersistenceBroker pb = PersistenceBrokerFactory.defaultPersistenceBroker();
                    LockedByVersion temp;
                    try
                    {
                        // lookup object instance again to get vaild lock value
                        Identity oid = pb.serviceIdentity().buildIdentity(obj);
                        temp = (LockedByVersion) pb.getObjectByIdentity(oid);
                    }
                    finally
                    {
                        if(pb != null) pb.close();
                    }
                    updateObject(temp, true);
                }
                else
                {
                    LoggerFactory.getDefaultLogger().error("* Can't lock given object, will throw exception" +
                            " for thread number " + threadNumber + " *");
                    throw e;
                }
            }
            finally
            {
                counter = 0;
                if(broker != null)
                {
                    broker.close();
                }
            }
        }

        private void updateName(LockedByVersion obj, boolean LNGEthrown)
        {
            String token;
            if(LNGEthrown)
            {
                token = "+";
            }
            else
            {
                token = "-";
            }
            if (obj.getValue().length() < 120)
            {
                obj.setValue(obj.getValue() + token + threadNumber);
            }
            else
            {
                System.out.println(obj.getValue());
                obj.setValue(msg + threadNumber);
            }
        }
    }
}
