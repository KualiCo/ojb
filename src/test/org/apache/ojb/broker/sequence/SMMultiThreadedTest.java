package org.apache.ojb.broker.sequence;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;

/**
 * Test SequenceManager implementation with multiple threads.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: SMMultiThreadedTest.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class SMMultiThreadedTest extends org.apache.ojb.junit.JUnitExtensions.MultiThreadedTestCase
{
    String goodName;
    String badName;

    public SMMultiThreadedTest(String s)
    {
        super(s);
    }

    public static void main(String[] args)
    {
        String[] arr = {SMMultiThreadedTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testMultipleInsertAndRollback()
    {
        int testObjects = 200;
        int threads = 10;
        TestCaseRunnable tct [] = new TestCaseRunnable[threads];
        for (int i = 0; i < threads; i++)
        {
            if (i % 2 == 1)
                tct[i] = new BadThenGoodHandle(testObjects);
            else
                tct[i] = new GoodThenBadHandle(testObjects);
        }
        // run test classes
        runTestCaseRunnables(tct);
        checkGeneratedObjects(testObjects*threads);

    }

    private void checkGeneratedObjects(int testObjects)
    {
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        int res_1;
        int res_2;
        try
        {
            Criteria crit = new Criteria();
            crit.addLike("name", goodName+"%");
            Criteria crit2 = new Criteria();
            crit2.addLike("name", badName+"%");
            crit.addOrCriteria(crit2);
            Query q_1 = new QueryByCriteria(MTObjectA.class, crit);
            Query q_2 = new QueryByCriteria(MTObjectB.class, crit);
            res_1 = broker.getCount(q_1);
            res_2 = broker.getCount(q_2);
        }
        finally
        {
            if(broker != null && !broker.isClosed()) broker.close();
        }
        assertEquals(testObjects, res_1);
        assertEquals(testObjects, res_2);

    }


    class GoodThenBadHandle extends org.apache.ojb.junit.JUnitExtensions.MultiThreadedTestCase.TestCaseRunnable
    {
        int testObjects;

        PersistenceBroker broker;

        public GoodThenBadHandle(int testObjects)
        {
            this.testObjects = testObjects;
            goodName = "GoodThenBadHandle_" + (long)(System.currentTimeMillis()*Math.random()) + "_";
        }

        void prepare()
        {
            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        }

        void cleanup()
        {
            if (broker != null && !broker.isClosed()) broker.close();
        }

        public void runTestCase() throws Throwable
        {
            prepare();
            try
            {
                broker.beginTransaction();
                for (int i = testObjects - 1; i >= 0; i--)
                {
                    MTObjectA obj = new MTObjectA();
                    obj.setName(goodName + (i + 1));
                    MTObjectB obj_2 = new MTObjectB();
                    obj_2.setName(goodName + (i + 1));

                    broker.store(obj);
                    broker.store(obj_2);
                    // Thread.sleep((int) (Math.random() * 5));
                }
                // Thread.sleep((int)(Math.random()*10));
                broker.commitTransaction();

                broker.beginTransaction();
                for (int i = testObjects - 1; i >= 0; i--)
                {
                    MTObjectA obj = new MTObjectA();
                    obj.setName(badName + (i + 1));
                    MTObjectB obj_2 = new MTObjectB();
                    obj_2.setName(badName + (i + 1));

                    broker.store(obj);
                    broker.store(obj_2);
                    // Thread.sleep((int) (Math.random() * 5));
                }
                // Thread.sleep((int)(Math.random()*10));
                broker.abortTransaction();
            }
            finally
            {
                cleanup();
            }
        }
    }

    class BadThenGoodHandle extends org.apache.ojb.junit.JUnitExtensions.MultiThreadedTestCase.TestCaseRunnable
    {
        int testObjects;
        PersistenceBroker broker;

        public BadThenGoodHandle(int testObjects)
        {
            this.testObjects = testObjects;
            badName = "BadThenGoodHandle_" + System.currentTimeMillis() + "_";
        }

        void prepare()
        {
            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        }

        void cleanup()
        {
            if (broker != null && !broker.isClosed()) broker.close();
        }

        public void runTestCase() throws Throwable
        {
            prepare();
            try
            {
                broker.beginTransaction();
                for (int i = testObjects - 1; i >= 0; i--)
                {
                    MTObjectA obj = new MTObjectA();
                    obj.setName(badName + (i + 1));
                    MTObjectB obj_2 = new MTObjectB();
                    obj_2.setName(badName + (i + 1));

                    broker.store(obj);
                    broker.store(obj_2);
                    // Thread.sleep((int) (Math.random() * 5));
                }
                // Thread.sleep((int)(Math.random()*10));
                broker.abortTransaction();

                broker.beginTransaction();
                for (int i = testObjects - 1; i >= 0; i--)
                {
                    MTObjectA obj = new MTObjectA();
                    obj.setName(badName + (i + 1));
                    MTObjectB obj_2 = new MTObjectB();
                    obj_2.setName(badName + (i + 1));

                    broker.store(obj);
                    broker.store(obj_2);
                    // Thread.sleep((int) (Math.random() * 5));
                }
                // Thread.sleep((int)(Math.random()*10));
                broker.commitTransaction();
            }
            finally
            {
                cleanup();
            }
        }
    }

    public static class MTObjectA
    {
        private Integer objId;
        private String name;

        public Integer getObjId()
        {
            return objId;
        }

        public void setObjId(Integer objId)
        {
            this.objId = objId;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    public static class MTObjectB extends MTObjectA
    {

    }
}
