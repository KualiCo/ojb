package org.apache.ojb.compare;

import java.util.Iterator;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.otm.OTMConnection;
import org.apache.ojb.otm.TestKit;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.lock.LockType;

/**
 * This TestCase contains the OJB performance benchmarks for the
 * OTM API.
 *
 * @author Oleg Nitz, Matthew Baird, borrowing heavily from Thomas Mahler
 */
public class PerformanceOTMTest extends PerformanceBaseTest
{
    private TestKit _kit;
    private OTMConnection _conn;

    public PerformanceOTMTest(String name)
    {
        super(name);
        setNameOfTest("Test for OTM-api");
    }

    /**
     * launches the TestCase.
     * The number of Objects to work with and the number of iterations
     * to be performed can be adjusted by setting them as commandline parameters.
     *
     * @param args the String[] holding the commandline parameters.
     */
    public static void main(String[] args)
    {
        if(args.length > 0)
        {
            articleCount = Integer.parseInt(args[0]);
        }
        if(args.length > 1)
        {
            iterations = Integer.parseInt(args[1]);
        }
        String[] arr = {PerformanceOTMTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        _kit = TestKit.getTestInstance();
        _conn = _kit.acquireConnection(PersistenceBrokerFactory.getDefaultKey());
        arr = new PerformanceArticle[articleCount];
        for(int i = 0; i < articleCount; i++)
        {
            PerformanceArticle a = createArticle(offsetId + i);
            arr[i] = a;
        }
    }

    public void tearDown() throws Exception
    {
        _conn.close();
        _conn = null;

        super.tearDown();
    }

    public void testBenchmark() throws Exception
    {
        super.testBenchmark();
    }

    /**
     * deletes all PerformanceArticle created by <code>insertNewArticles</code>.
     */
    protected void deleteArticles() throws Exception
    {
        long start = System.currentTimeMillis();
        Transaction tx = _kit.getTransaction(_conn);
        tx.begin();
        for(int i = 0; i < articleCount; i++)
        {
            _conn.deletePersistent(arr[i]);
        }
        tx.commit();
        long stop = System.currentTimeMillis();
        logger.info("deleting " + articleCount + " Objects: " + (stop - start) + " msec");
    }

    /**
     * create new PerformanceArticle objects and insert them into the RDBMS.
     * <p/>
     * The number of objects to create is defined by <code>articleCount</code>.
     */
    protected void insertNewArticles() throws Exception
    {
        long start = System.currentTimeMillis();
        Transaction tx = _kit.getTransaction(_conn);
        tx.begin();
        for(int i = 0; i < articleCount; i++)
        {
            _conn.makePersistent(arr[i]);
        }
        tx.commit();
        long stop = System.currentTimeMillis();
        logger.info("inserting " + articleCount + " Objects: " + (stop - start) + " msec");
    }

    /**
     * read in all the PerformanceArticles from the RDBMS that have
     * been inserted by <code>insertNewArticles()</code>.
     * The lookup is done one by one, that is: a primary key based lookup is used.
     */
    protected void readArticles() throws Exception
    {
        long start = System.currentTimeMillis();
        Transaction tx = _kit.getTransaction(_conn);
        tx.begin();
        for(int i = 0; i < articleCount; i++)
        {
            Object[] pks = {new Integer(offsetId + i)};
            Identity oid = new Identity(PerformanceArticle.class, PerformanceArticle.class, pks);
            _conn.getObjectByIdentity(oid, LockType.NO_LOCK);
        }
        tx.commit();
        long stop = System.currentTimeMillis();
        logger.info("querying " + articleCount + " Objects: " + (stop - start) + " msec");
    }

    /**
     * read in all the PerformanceArticles from the RDBMS that have
     * been inserted by <code>insertNewArticles()</code>.
     * The lookup is done with a cursor fetch,
     * that is: a between Statement is used to select all inserted PerformanceArticles
     * and Objects are read in by fetching from the cursor (JDBC ResultSet).
     */
    protected void readArticlesByCursor() throws Exception
    {
        // clear the cache
        _conn.invalidateAll();

        Transaction tx = _kit.getTransaction(_conn);
        Criteria c = new Criteria();
        c.addBetween("articleId", new Integer(offsetId), new Integer(offsetId + articleCount));
        Query q = new QueryByCriteria(PerformanceArticle.class, c);
        long start = System.currentTimeMillis();
        tx.begin();
        Iterator iter = _conn.getIteratorByQuery(q, LockType.NO_LOCK);
        int fetchCount = 0;
        while(iter.hasNext())
        {
            fetchCount++;
            iter.next();
        }
        tx.commit();
        long stop = System.currentTimeMillis();
        logger.info("fetching " + fetchCount + " Objects: " + (stop - start) + " msec");
    }

    /**
     * updates all PerformanceArticles inserted by <code>insertNewArticles()</code>.
     * All objects are modified and changes are written to the RDBMS with an UPDATE.
     */
    protected void updateExistingArticles() throws Exception
    {
        long start = System.currentTimeMillis();
        Transaction tx = _kit.getTransaction(_conn);
        tx.begin();
        // update all objects
        for(int i = 0; i < articleCount; i++)
        {
            _conn.lockForWrite(arr[i]);
            arr[i].setPrice(arr[i].getPrice() * 1.95583);
        }
        tx.commit();
        long stop = System.currentTimeMillis();
        logger.info("updating " + articleCount + " Objects: " + (stop - start) + " msec");
    }
}

