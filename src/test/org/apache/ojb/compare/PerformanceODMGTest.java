package org.apache.ojb.compare;

import java.util.Iterator;

import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.TestHelper;
import org.apache.ojb.odmg.OJB;
import org.apache.ojb.odmg.TransactionExt;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * This TestCase contains the OJB performance benchmarks for the
 * ODMG API.
 *
 * @author Matthew Baird, borrowing heavily from Thomas Mahler
 */
public class PerformanceODMGTest extends PerformanceBaseTest
{
    private Implementation odmg;
    private Database db;

    public PerformanceODMGTest(String name)
    {
        super(name);
        setNameOfTest("Test for ODMG-api");
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
        String[] arr = {PerformanceODMGTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testBenchmark() throws Exception
    {
        super.testBenchmark();
    }

    public void setUp() throws Exception
    {
        // madatory to call super class method
        super.setUp();

        odmg = OJB.getInstance();
        db = odmg.newDatabase();
        db.open(TestHelper.DEF_DATABASE_NAME, Database.OPEN_READ_WRITE);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * deletes all PerformanceArticle created by <code>insertNewArticles</code>.
     */
    protected void deleteArticles() throws Exception
    {
        Transaction tx = odmg.newTransaction();
        long start = System.currentTimeMillis();
        tx.begin();
        for(int i = 0; i < articleCount; i++)
        {
            db.deletePersistent(arr[i]);
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
        Transaction tx = odmg.newTransaction();
        long start = System.currentTimeMillis();
        tx.begin();
        for(int i = 0; i < articleCount; i++)
        {
            db.makePersistent(arr[i]);
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
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        // we don't want implicite locks when compare performance
        tx.setImplicitLocking(false);
        String sql = "select allArticles from " + PerformanceArticle.class.getName() + " where articleId=$1";
        long start = System.currentTimeMillis();
        tx.begin();
        for(int i = 0; i < articleCount; i++)
        {
            OQLQuery query = odmg.newOQLQuery();
            query.create(sql);
            query.bind(arr[i].getArticleId());
            query.execute();
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
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        // we don't want implicite locks when compare performance
        tx.setImplicitLocking(false);
        tx.begin();
        // clear cache to read from DB
        tx.getBroker().clearCache();

        long start = System.currentTimeMillis();
        OQLQuery query = odmg.newOQLQuery();
        String sql = "select allArticles from " + PerformanceArticle.class.getName()
                + " where articleId between " + new Integer(offsetId) + " and "
                + new Integer(offsetId + articleCount);
        query.create(sql);
        ManageableCollection collection = (ManageableCollection) query.execute();
        Iterator iter = collection.ojbIterator();
        int fetchCount = 0;
        while(iter.hasNext())
        {
            fetchCount++;
            iter.next();
        }
        long stop = System.currentTimeMillis();
        logger.info("fetching " + fetchCount + " Objects: " + (stop - start) + " msec");
    }

    /**
     * updates all PerformanceArticles inserted by <code>insertNewArticles()</code>.
     * All objects are modified and changes are written to the RDBMS with an UPDATE.
     */
    protected void updateExistingArticles() throws Exception
    {
        Transaction tx = odmg.newTransaction();
        long start = System.currentTimeMillis();
        tx.begin();
        // update all objects
        for(int i = 0; i < articleCount; i++)
        {
            tx.lock(arr[i], Transaction.WRITE);
            arr[i].setPrice(arr[i].getPrice() * 1.95583);
        }
        tx.commit();
        long stop = System.currentTimeMillis();
        logger.info("updating " + articleCount + " Objects: " + (stop - start) + " msec");
    }
}

