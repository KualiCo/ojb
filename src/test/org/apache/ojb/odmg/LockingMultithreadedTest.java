package org.apache.ojb.odmg;

import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.TestHelper;
import org.apache.ojb.junit.JUnitExtensions;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.LockNotGrantedException;
import org.odmg.Transaction;

/**
 * Test odmg-locking implementation with multiple threads.
 * Different threads try to update the same instance / or a copy
 * of the same object.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: LockingMultithreadedTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class LockingMultithreadedTest extends JUnitExtensions.MultiThreadedTestCase
{
    private static int threadCount;
    private static Implementation odmg;
    private static Database db;
    private final StringBuffer result = new StringBuffer(2000);
    private static final String eol = SystemUtils.LINE_SEPARATOR;
    // number of concurrent threads to run
    private final int concurrentThreads = 10;
    // number of updates each thread performs against the object
    private final int objectUpdates = 30;
    // max number of attemps to get a lock
    private static final int maxAttempts = 100;
    private static final int nearMax = (int) (maxAttempts * 0.75);


    public static void main(String[] args)
    {
        String[] arr = {LockingMultithreadedTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public LockingMultithreadedTest(String s)
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
        /*
        odmg api is threadsafe, so we can share Implementation instance
        among the different threads
        */
        odmg = OJB.getInstance();
        db = odmg.newDatabase();
        db.open(TestHelper.DEF_DATABASE_NAME, Database.OPEN_READ_WRITE);

        LockObject targetObject = createLockObjectWithRef();
        storeObject(targetObject);

        TestCaseRunnable tct [] = new TestCaseRunnable[concurrentThreads];
        for (int i = 0; i < concurrentThreads; i++)
        {
            /*
            several threads try to lock the same object (shared object),
            the other threads lock deep copies of the shared object
            */
            if (i % 2 == 0)
                tct[i] = new LockHandle(targetObject);
            else
                tct[i] = new LockHandle(targetObject.makeCopy());
        }
        // run test classes
        runTestCaseRunnables(tct);
        System.out.println("*** Result of multithreaded lock test ***");
        System.out.println(result.toString());
        //System.out.println(targetObject.getReference().getName());
    }

    private LockObject createLockObjectWithRef()
    {
        int number = newThreadKey();
        LockObject lo = new LockObject();
        lo.setName("modified by thread: " + number);

        LockObjectRef lor = new LockObjectRef();
        lor.setName("modified by thread: " + number);

        lo.setReference(lor);

        return lo;
    }

    private void storeObject(Object obj) throws Exception
    {
        Transaction tx = odmg.newTransaction();

        tx.begin();
        tx.lock(obj, Transaction.WRITE);
        tx.commit();
    }

    public static synchronized int newThreadKey()
    {
        return threadCount++;
    }

    //=======================================================================
    // inner classes
    //=======================================================================

    class LockHandle extends JUnitExtensions.MultiThreadedTestCase.TestCaseRunnable
    {
        final LockObject obj;
        int threadNumber;
        private int counter = 0;

        public LockHandle(LockObject obj)
        {
            super();
            this.obj = obj;
        }

        public void runTestCase() throws Throwable
        {
            threadNumber = newThreadKey();
            Transaction tx = odmg.newTransaction();
            for (int i = 0; i < objectUpdates; i++)
            {
                tx.begin();
                updateObject(tx, obj);
                tx.commit();
                counter = 0;
            }
        }

        private void updateObject(final Transaction tx, final LockObject obj) throws Exception
        {
            try
            {
                tx.lock(obj, Transaction.WRITE);
                tx.lock(obj.getReference(), Transaction.WRITE);
                updateName(obj);
                updateName(obj.getReference());
            }
            catch (LockNotGrantedException e)
            {
                if (counter < maxAttempts)
                {
                    counter++;
                    if (counter > nearMax)
                        System.out.println("LockingMultithreadedTest: thread "
                                + threadNumber + " waits " + counter
                                + " times to update object. Maximal attempts before fail are " + maxAttempts
                                + ". This can be a result of low hardware.");
                    try
                    {
                        Thread.sleep(30);
                    }
                    catch(InterruptedException e1)
                    {
                    }
                    updateObject(tx, obj);
                }
                else
                {
                    System.out.println("* Can't lock given object, will throw exception" +
                            " for thread number " + threadNumber + " *");
                    throw e;
                }
            }
        }

        private void updateName(LockObject obj)
        {
            if (obj.getName().length() < 100)
            {
                obj.setName(obj.getName() + "-" + threadNumber);
            }
            else
            {
                result.append(eol).append(obj.getName());
                obj.setName("modified by thread: " + threadNumber);
            }
        }

        private void updateName(LockObjectRef obj)
        {
            if (obj.getName().length() < 100)
            {
                obj.setName(obj.getName() + "-" + threadNumber);
            }
            else
            {
                obj.setName("modified by thread: " + threadNumber);
            }
        }
    }

    public static class LockObject
    {
        private Integer id;
        private String name;
        private Integer version;
        private LockObjectRef reference;

        public LockObject()
        {
        }

        private LockObject(Integer id, String name, Integer version, LockObjectRef reference)
        {
            this.id = id;
            this.name = name;
            this.version = version;
            this.reference = reference;
        }

        public LockObject makeCopy()
        {
            return new LockObject(id, name, version, reference != null ? reference.makeCopy() : null);
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

        public Integer getVersion()
        {
            return version;
        }

        public void setVersion(Integer version)
        {
            this.version = version;
        }

        public LockObjectRef getReference()
        {
            return reference;
        }

        public void setReference(LockObjectRef reference)
        {
            this.reference = reference;
        }
    }

    public static class LockObjectRef
    {
        private Integer id;
        private String name;
        private Integer version;

        public LockObjectRef()
        {
        }

        private LockObjectRef(Integer id, String name, Integer version)
        {
            this.id = id;
            this.name = name;
            this.version = version;
        }

        public LockObjectRef makeCopy()
        {
            return new LockObjectRef(id, name, version);
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

        public Integer getVersion()
        {
            return version;
        }

        public void setVersion(Integer version)
        {
            this.version = version;
        }
    }
}
