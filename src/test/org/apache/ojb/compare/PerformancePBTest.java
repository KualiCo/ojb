package org.apache.ojb.compare;

import java.util.Iterator;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.util.ObjectModification;

/**
 * This TestCase contains the OJB single-threaded performance benchmarks for the
 * PersistenceBroker-API.
 *
 * @author Thomas Mahler
 */
public class PerformancePBTest extends PerformanceBaseTest
{
    public PerformancePBTest(String name)
    {
        super(name);
        setNameOfTest("Test for PB-api");
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

        String[] arr = {PerformancePBTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testBenchmark() throws Exception
    {
        super.testBenchmark();
    }

    /**
     * deletes all PerformanceArticle created by <code>insertNewArticles</code>.
     */
    protected void deleteArticles() throws PersistenceBrokerException
    {
        long start = System.currentTimeMillis();
        broker.beginTransaction();
        for(int i = 0; i < articleCount; i++)
        {
            broker.delete(arr[i]);
        }
        broker.commitTransaction();
        long stop = System.currentTimeMillis();
        logger.info("deleting " + articleCount + " Objects: " + (stop - start) + " msec");
    }


    /**
     * create new PerformanceArticle objects and insert them into the RDBMS.
     * The number of objects to create is defined by <code>articleCount</code>.
     */
    protected void insertNewArticles() throws PersistenceBrokerException
    {
        long start = System.currentTimeMillis();
        broker.beginTransaction();
        for(int i = 0; i < articleCount; i++)
        {
            broker.store(arr[i], ObjectModification.INSERT);
        }
        broker.commitTransaction();
        long stop = System.currentTimeMillis();
        logger.info("inserting " + articleCount + " Objects: " + (stop - start) + " msec");

    }

    /**
     * read in all the PerformanceArticles from the RDBMS that have
     * been inserted by <code>insertNewArticles()</code>.
     * The lookup is done one by one, that is: a primary key based lookup is used.
     */
    protected void readArticles() throws PersistenceBrokerException
    {
        long start = System.currentTimeMillis();
        broker.beginTransaction();
        for(int i = 0; i < articleCount; i++)
        {
            Identity oid = broker.serviceIdentity().
                    buildIdentity(PerformanceArticle.class, arr[i].getArticleId());
            broker.getObjectByIdentity(oid);
        }
        broker.commitTransaction();
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
    protected void readArticlesByCursor() throws PersistenceBrokerException

    {
        broker.clearCache();
        Criteria c = new Criteria();
        c.addBetween("articleId", new Integer(offsetId), new Integer(offsetId + articleCount));
        Query q = new QueryByCriteria(PerformanceArticle.class, c);

        long start = System.currentTimeMillis();
        Iterator iter = broker.getIteratorByQuery(q);
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
    protected void updateExistingArticles() throws PersistenceBrokerException
    {
        // update all objects
        for(int i = 0; i < articleCount; i++)
        {
            arr[i].setPrice(arr[i].getPrice() * 1.95583);
        }

        long start = System.currentTimeMillis();
        broker.beginTransaction();
        for(int i = 0; i < articleCount; i++)
        {
            broker.store(arr[i], ObjectModification.UPDATE);
        }
        broker.commitTransaction();
        long stop = System.currentTimeMillis();
        logger.info("updating " + articleCount + " Objects: " + (stop - start) + " msec");
    }
}
