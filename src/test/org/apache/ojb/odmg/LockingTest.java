package org.apache.ojb.odmg;


import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.TestHelper;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.OJBTestCase;
import org.apache.ojb.odmg.locking.LockManager;
import org.apache.ojb.odmg.locking.LockManagerFactory;
import org.apache.ojb.odmg.shared.Article;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.LockNotGrantedException;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * Test optimistic and pessimistic locking mechanisms
 */
public class LockingTest extends OJBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {LockingTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    private static String PRE = "LockingTest_" + System.currentTimeMillis() + "_";

    private Implementation odmg1;
    private Database db1;

    private Implementation odmg2;
    private Database db2;

    public LockingTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        String databaseName = TestHelper.DEF_DATABASE_NAME;

        odmg1 = OJB.getInstance();
        db1 = odmg1.newDatabase();
        db1.open(databaseName, Database.OPEN_READ_WRITE);

        odmg2 = OJB.getInstance();
        db2 = odmg2.newDatabase();
        db2.open(databaseName, Database.OPEN_READ_WRITE);

    }

    public void tearDown() throws Exception
    {
        if(odmg1.currentTransaction() != null) odmg1.currentTransaction().abort();
        db1.close();

        if(odmg2.currentTransaction() != null) odmg2.currentTransaction().abort();
        db2.close();
        super.tearDown();
    }

    public void testWrite_1() throws Exception
    {
        String name = "testWrite_1_" + System.currentTimeMillis();
        LockObject bean = new LockObject(name + "_bean_dummy");

        performSaveMethod(bean.getId(), bean);

        Transaction tx = odmg1.newTransaction();
        tx.begin();
        OQLQuery query = odmg1.newOQLQuery();
        query.create("select objs from " + LockObject.class.getName() + " where value = $1");
        query.bind(name + "_bean_dummy");
        List result = (List) query.execute();
        tx.commit();
        assertEquals(1, result.size());
        LockObject tmp = (LockObject) result.get(0);
        assertEquals(bean, tmp);
    }

    public void testWrite_2() throws Exception
    {
        String name = "testWrite_2_" + System.currentTimeMillis();

        Transaction tx = odmg1.newTransaction();
        tx.begin();
        LockObject tmp = new LockObject(name + "_temp");
        db1.makePersistent(tmp);
        tx.commit();

        LockObject bean = new LockObject(name + "_bean_dummy");
        bean.setId(tmp.getId());

        performSaveMethod(tmp.getId(), bean);

        tx = odmg1.newTransaction();
        tx.begin();
        OQLQuery query = odmg1.newOQLQuery();
        query.create("select objs from " + LockObject.class.getName() + " where value = $1");
        query.bind(name + "_bean_dummy");
        List result = (List) query.execute();
        tx.commit();
        assertEquals(1, result.size());
        tmp = (LockObject) result.get(0);
        assertEquals(bean, tmp);
    }

    /**
     * This method should reproduce a problem with Cocoon and OJB1.0.3
     */
    private LockObject performSaveMethod(Integer testId, LockObject bean) throws Exception
    {
        LockObject toBeEdited = null;
        Transaction tx = odmg1.newTransaction();
        tx.begin();
        OQLQuery query = odmg1.newOQLQuery();
        query.create("select objs from " + LockObject.class.getName() + " where id = $1");
        query.bind(testId);
        List result = (List) query.execute();
        if(result.size() != 0)
        {
            toBeEdited = (LockObject) result.get(0);
            if(toBeEdited != null)
            {
                try
                {
                    PropertyUtils.copyProperties(toBeEdited, bean);
                    tx.commit();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    fail("Unexpected exception: " + e.getMessage());
                }
                // use new tx to check released locks for objects
                tx = odmg1.newTransaction();
                tx.begin();
                tx.lock(toBeEdited, Transaction.UPGRADE);
                tx.commit();
            }
            else
            {
                tx.abort();
            }
        }
        else
        {
            try
            {
                tx.lock(bean, Transaction.WRITE);
                tx.commit();
            }
            catch(Exception e)
            {
                e.printStackTrace();
                fail("Unexpected exception: " + e.getMessage());
            }
        }
        return toBeEdited;
    }

    /**
     * Test multiple locks on the same object
     */
    public void testMultipleLocks() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        String name = "testMultipleLocks_" + timestamp;
        String nameUpdated = "testMultipleLocks_Updated_" + timestamp;
        TransactionExt tx = (TransactionExt) odmg1.newTransaction();
        LockObjectOpt obj = new LockObjectOpt();
        tx.begin();
        tx.lock(obj, Transaction.WRITE);
        obj.setValue(name);
        tx.lock(obj, Transaction.WRITE);
        tx.lock(obj, Transaction.UPGRADE);
        tx.commit();

        OQLQuery query = odmg1.newOQLQuery();
        query.create("select all from " + LockObjectOpt.class.getName() + " where value like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertNotNull(result);
        assertEquals(1, result.size());

        tx.begin();
        tx.lock(obj, Transaction.WRITE);
        tx.lock(obj, Transaction.WRITE);
        obj.setValue(nameUpdated);
        tx.lock(obj, Transaction.WRITE);
        tx.lock(obj, Transaction.UPGRADE);
        tx.commit();

        query = odmg1.newOQLQuery();
        query.create("select all from " + LockObjectOpt.class.getName() + " where value like $1");
        query.bind(nameUpdated);
        result = (Collection) query.execute();
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    public void testLockBasics() throws Exception
    {
        TransactionImpl tx1 = (TransactionImpl) odmg1.newTransaction();
        TransactionImpl tx2 = (TransactionImpl) odmg2.newTransaction();

        Article a = new Article();
        a.setArticleId(333);

        tx1.begin();
        tx2.begin();
        LockManager lm = LockManagerFactory.getLockManager();
        boolean success1 = lm.writeLock(tx1, a);
        boolean success2 = lm.writeLock(tx2, a);

        boolean success3 = lm.releaseLock(tx1, a);

        assertTrue("1st lock should succeed", success1);
        assertTrue("2nd lock should not succeed", !success2);
        assertTrue("release should succeed", success3);

        try
        {
            tx1.abort();
            tx2.abort();
        }
        catch(Exception e)
        {
        }
    }

    public void testLockBasics2() throws Exception
    {
        TransactionImpl tx1 = (TransactionImpl) odmg1.newTransaction();
        TransactionImpl tx2 = (TransactionImpl) odmg2.newTransaction();

        Article a1 = new Article();
        a1.setArticleId(333);

        Article a2 = new Article();
        a2.setArticleId(333);

        tx1.begin();
        tx2.begin();
        LockManager lm = LockManagerFactory.getLockManager();

        assertFalse(tx1.getGUID().equals(tx2.getGUID()));

        assertTrue("1st lock should succeed", lm.writeLock(tx1, a1));
        assertFalse("2nd lock should not succeed", lm.writeLock(tx2, a2));
        lm.releaseLock(tx2, a2);
        lm.releaseLock(tx2, a1);
        assertFalse(lm.checkWrite(tx2, a1));
        assertFalse(lm.checkWrite(tx2, a2));
        assertTrue(lm.checkWrite(tx1, a1));
        assertTrue(lm.checkWrite(tx1, a2));
        //assertFalse("2nd release should not succeed", lm.releaseLock(tx2, a2));
        //assertFalse("2nd release should not succeed", lm.releaseLock(tx2, a1));
        assertTrue("release should succeed", lm.releaseLock(tx1, a2));
        assertTrue("2nd object lock should succeed", lm.writeLock(tx2, a2));
        assertTrue("release 2nd object lock should succeed", lm.releaseLock(tx2, a2));

        try
        {
            tx1.abort();
            tx2.abort();
        }
        catch(Exception e)
        {
        }
    }

    /**
     * test proper treatment of Optimistic Locking in
     * ODMG transactions
     */
    public void testOptimisticLockBasics() throws Exception
    {
        TransactionImpl tx1 = (TransactionImpl) odmg1.newTransaction();
        TransactionImpl tx2 = (TransactionImpl) odmg2.newTransaction();

        LockObjectOpt obj = new LockObjectOpt();


        tx1.begin();

        tx1.lock(obj, Transaction.WRITE);
        obj.setValue("tx1");
        tx1.commit();

        obj.setVersion(obj.getVersion() - 1);
        tx2.begin();
        tx2.lock(obj, Transaction.WRITE);

        obj.setValue("tx2");
        try
        {
            tx2.commit();
// OL exceptions should be signalled as ODMG LockNotGrantedExceptions
// so that users can react accordingly
            fail("Optimistic locking exception expected");
        }
        catch(LockNotGrantedException ex)
        {
            assertTrue("expected that a OL exception is caught", true);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fail("Wrong kind of exception thrown, expected 'LockNotGrantedException', but was " + e.getMessage());
        }
    }


    /**
     * factory method that createa an PerformanceArticle
     *
     * @return the created PerformanceArticle object
     */
    private Article createArticle(String name)
    {
        Article a = new Article();

        a.setArticleName(PRE + name);
        a.setMinimumStock(100);
        a.setOrderedUnits(17);
        a.setPrice(0.45);
        a.setProductGroupId(1);
        a.setStock(234);
        a.setSupplierId(4);
        a.setUnit("bottle");
        return a;
    }

    public void testLockLoop() throws Exception
    {
        int loops = 10;
        Article[] arr = new Article[loops];
        for(int i = 0; i < loops; i++)
        {
            Article a = createArticle("testLockLoop");
            arr[i] = a;
        }

        TransactionImpl tx = (TransactionImpl) odmg1.newTransaction();
        tx.begin();
        for(int i = 0; i < arr.length; i++)
        {
            tx.lock(arr[i], Transaction.WRITE);
        }
        LockManager lm = LockManagerFactory.getLockManager();
        boolean success = lm.writeLock(tx, arr[(loops - 2)]);
        assertTrue("lock should succeed", success);
        tx.commit();

        TransactionImpl tx2 = (TransactionImpl) odmg2.newTransaction();
        tx2.begin();
        success = lm.writeLock(tx2, arr[(loops - 2)]);
        assertTrue("lock should succeed", success);

        OQLQuery query = odmg2.newOQLQuery();
        String sql = "select allArticles from " + Article.class.getName() +
                " where articleName = \"" + PRE + "testLockLoop" + "\"";
        query.create(sql);
        int result = ((List) query.execute()).size();
        tx2.commit();
        assertEquals("Wrong number of objects wrote to DB", loops, result);

        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        broker.clearCache();
        Criteria crit = new Criteria();
        crit.addLike("articleName", PRE + "testLockLoop");
        result = broker.getCount(QueryFactory.newQuery(Article.class, crit));
        broker.close();
        assertEquals("Wrong number of objects wrote to DB", loops, result);
    }


    /**
     * Test object.
     */
    public static class LockObject implements Serializable
    {
        private Integer id;
        private String value;

        public LockObject()
        {
        }

        public LockObject(String value)
        {
            this.value = value;
        }

        public boolean equals(Object obj)
        {
            if(obj == null || !(obj instanceof LockObject))
            {
                return false;
            }
            else
            {
                LockObject tmp = (LockObject) obj;
                return new EqualsBuilder()
                        .append(id, tmp.id)
                        .append(value, tmp.value)
                        .isEquals();
            }
        }

        public String toString()
        {
            return new ToStringBuilder(this)
                    .append("id", id)
                    .append("value", value)
                    .toString();
        }

        public Integer getId()
        {
            return id;
        }
        public void setId(Integer id)
        {
            this.id = id;
        }
        public String getValue()
        {
            return value;
        }
        public void setValue(String value)
        {
            this.value = value;
        }
    }

    /**
     * Test object with optimistic locking enabled.
     */
    public static class LockObjectOpt extends LockObject
    {
        private int version;

        public LockObjectOpt()
        {
        }

        public LockObjectOpt(String value)
        {
            super(value);
        }

        public boolean equals(Object obj)
        {
            if(obj == null || !(obj instanceof LockObjectOpt))
            {
                return false;
            }
            else
            {
                LockObjectOpt tmp = (LockObjectOpt) obj;
                return new EqualsBuilder()
                        .append(version, tmp.version)
                        .isEquals() && super.equals(obj);
            }
        }

        public String toString()
        {
            return new ToStringBuilder(this)
                    .append("super", super.toString())
                    .append("version", version)
                    .toString();
        }

        public int getVersion()
        {
            return version;
        }
        public void setVersion(int version)
        {
            this.version = version;
        }
    }
}
