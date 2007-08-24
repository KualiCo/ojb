package org.apache.ojb.compare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.platforms.PlatformHsqldbImpl;
import org.apache.ojb.broker.platforms.Platform;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.HsqldbShutdown;
import org.apache.ojb.junit.PBTestCase;

/**
 * This is the base class for single-threaded performance benchmarks.
 *
 * @author Thomas Mahler
 */
abstract class PerformanceBaseTest extends PBTestCase
{
    protected Logger logger = LoggerFactory.getLogger("performance");

    private String nameOfTest = "Test performance";

    /**
     * the number of PerformanceArticle objects to work with.
     */
    protected static int articleCount = 500;

    /**
     * the number of iterations to perform.
     */
    protected static int iterations = 2;

    /**
     * the offset value for PerformanceArticle primary keys
     */
    protected final static int offsetId = 12000;

    protected PerformanceArticle[] arr;

    /**
     * BrokerTests constructor comment.
     *
     * @param name java.lang.String
     */
    public PerformanceBaseTest(String name)
    {
        super(name);
        // setNameOfTest("No name");
    }


    /**
     * setting up the test fixture.
     */
    public void setUp() throws Exception
    {
        try
        {
            super.setUp();
            clearTable();

            arr = new PerformanceArticle[articleCount];
            for(int i = 0; i < articleCount; i++)
            {
                PerformanceArticle a = createArticle(offsetId + i);
                arr[i] = a;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * tearing down the test fixture.
     */
    public void tearDown() throws Exception
    {
        try
        {
            clearTable();
            shutdown();
            super.tearDown();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Set the name of the test.
     *
     * @param nameOfTest
     */
    public void setNameOfTest(String nameOfTest)
    {
        this.nameOfTest = nameOfTest;
    }

    protected void clearTable() throws Exception
    {
        Connection conn = getConnection();
        ClassDescriptor cld = broker.getClassDescriptor(PerformanceArticle.class);
        String table = cld.getFullTableName();
        String column = cld.getFieldDescriptorByName("articleId").getColumnName();
        String sql = "DELETE FROM " + table + " WHERE " + column + " >= " + offsetId;
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.execute();
        returnConnection(conn);
    }

    /**
     * factory method that createa an PerformanceArticle with a given id.
     *
     * @param id the primary key value for the new object
     * @return the created PerformanceArticle object
     */
    protected PerformanceArticle createArticle(int id)
    {
        PerformanceArticle a = new PerformanceArticle();
        a.setArticleId(new Integer(id));
        a.setArticleName("New Performance Article " + id);
        a.setMinimumStock(100);
        a.setOrderedUnits(17);
        a.setPrice(100.0);
        a.setProductGroupId(1);
        a.setStock(234);
        a.setSupplierId(4);
        a.setUnit("bottle");
        return a;
    }

    /**
     * obtain a JDBC Connection. OJB API is used to make this code portable for
     * other target dabases and different lookup methods.
     *
     * @return the Connection to be used
     */
    protected Connection getConnection() throws Exception
    {
        // use the PB instance to get access to connection, this doesn't impact performance
        Connection conn = broker.serviceConnectionManager().getConnection();
        return conn;
    }

    protected void returnConnection(Connection conn) throws Exception
    {

    }

    /**
     * deletes all PerformanceArticle created by <code>insertNewArticles</code>.
     */
    protected abstract void deleteArticles() throws Exception;

    /**
     * create new PerformanceArticle objects and insert them into the RDBMS.
     * The number of objects to create is defined by <code>articleCount</code>.
     */
    protected abstract void insertNewArticles() throws Exception;

    /**
     * read in all the PerformanceArticles from the RDBMS that have
     * been inserted by <code>insertNewArticles()</code>.
     * The lookup is done one by one, that is: a primary key based lookup is used.
     */
    protected abstract void readArticles() throws Exception;

    /**
     * read in all the PerformanceArticles from the RDBMS that have
     * been inserted by <code>insertNewArticles()</code>.
     * The lookup is done with a cursor fetch,
     * that is: a between Statement is used to select all inserted PerformanceArticles
     * and Objects are read in by fetching from the cursor (JDBC ResultSet).
     */
    protected abstract void readArticlesByCursor() throws Exception;

    /**
     * updates all PerformanceArticles inserted by <code>insertNewArticles()</code>.
     * All objects are modified and changes are written to the RDBMS with an UPDATE.
     */
    protected abstract void updateExistingArticles() throws Exception;

    /**
     * This method is the driver for the complete Benchmark.
     * It performs the following steps:
     * <p/>
     * 1.) n objects are created and inserted to the RDBMS.
     * 2.) the created objects are modified. Modifications are written to the RDBMS with updates.
     * 3.) All objects created in 1.) are read in by primary key based SELECT statements.
     * 4.) Step 3.) is repeated to test caching facilities.
     * 5.) All objects created in 1.) are read by iterating over a ResultSet.
     * 6.) All objects created in 1.) are deleted with n separate DELETE Statements.
     */
    public void testBenchmark() throws Exception
    {
        try
        {
            logger.info(nameOfTest);
            for(int i = 0; i < iterations; i++)
            {
                logger.info("");

                // store all Article objects
                insertNewArticles();

                // update all objects
                updateExistingArticles();

                // querying
                readArticles();

                readArticles();

                // fetching objects
                readArticlesByCursor();

                // delete all objects
                deleteArticles();
            }
        }
        catch(Exception e)
        {
            logger.error(e);
            throw e;
        }
    }

    public void shutdown()
    {
        Platform platform = broker.serviceConnectionManager().getSupportedPlatform();

        if(platform instanceof PlatformHsqldbImpl)
        {
            Connection con = null;
            Statement stmt = null;

            try
            {
                con = broker.serviceConnectionManager().getConnection();
                stmt = con.createStatement();
                stmt.execute("shutdown");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    if(con != null) con.close();
                    if(stmt != null) stmt.close();

                }
                catch (SQLException e1)
                {
                    e1.printStackTrace();
                }
            }
        }

    }
}
