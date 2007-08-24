package org.apache.ojb.compare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.ojb.broker.OJBException;
import org.apache.ojb.broker.TestHelper;
import org.apache.ojb.broker.accesslayer.ConnectionFactory;
import org.apache.ojb.broker.accesslayer.ConnectionFactoryFactory;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.junit.PBTestCase;

/**
 * This TestCase contains the OJB performance benchmarks for the
 * JDBC API. The original testcases have been enhanced with
 * code dealing with db failover situations.
 * @author Thomas Mahler
 */
public class PerformanceJdbcFailoverTest extends PBTestCase
{
    private Logger logger = LoggerFactory.getLogger("failover");

    /**
     * the number of PerformanceArticle objects to work with.
     */
    static int articleCount = 10000;

    /**
     * the number of iterations to perform.
     */
    static int iterations = 2;

    /**
     * the maximum number of retries if db fails.
     */
    static int maxRetries = 5;

    /**
     * the maximum time to wait on db availability.
     */
    static int maxWait = 30;


    /**
     * the offset value for PerformanceArticle primary keys
     */
    int offsetId = 10000;
    private PerformanceArticle[] arr;
    private int actualRetries = 0;

    /**
     * BrokerTests constructor comment.
     * @param name java.lang.String
     */
    public PerformanceJdbcFailoverTest(String name)

    {
        super(name);
    }

    /**
     * launches the TestCase.
     * The number of Objects to work with and the number of iterations
     * to be performed can be adjusted by setting them as commandline parameters.
     * @param args the String[] holding the commandline parameters.
     */
    public static void main(String[] args)
    {
        if (args.length > 0)
        {
            articleCount = Integer.parseInt(args[0]);
        }
        if (args.length > 1)
        {
            iterations = Integer.parseInt(args[1]);
        }
        if (args.length > 2)
        {
            maxRetries = Integer.parseInt(args[2]);
        }
        if (args.length > 3)
        {
            maxWait = Integer.parseInt(args[3]);
        }


        String[] arr = {PerformanceJdbcFailoverTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**
     * setting up the test fixture.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        clearTable();
        arr = new PerformanceArticle[articleCount];
        for (int i = 0; i < articleCount; i++)
        {
            PerformanceArticle a = createArticle(offsetId + i);
            arr[i] = a;
        }
    }

    /**
     * tearing down the test fixture.
     */
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * factory method that createa an PerformanceArticle with a given id.
     * @return the created PerformanceArticle object
     * @param id the primary key value for the new object
     */
    private PerformanceArticle createArticle(int id)
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
     * @return the Connection to be used
     */
    private Connection getConnection() throws Exception
    {
        Connection conn = null;
        long startToWait = System.currentTimeMillis();
        System.out.print("[");
        System.out.flush();
        while (true)
        {
            try
            {
                // Use OJB API to obtain JDBC Connection. All settings are read from
                // the repository.xml file.
                JdbcConnectionDescriptor jcd =
                        MetadataManager.getInstance().connectionRepository().getDescriptor(
                                TestHelper.DEF_KEY);
                ConnectionFactory cf =
                        ConnectionFactoryFactory.getInstance().createConnectionFactory();
                conn = cf.lookupConnection(jcd);
                System.out.println("] Waited for connection " + (System.currentTimeMillis() - startToWait) + "msecs");
                break;
            }
            catch (Throwable t)
            {
            	long now = System.currentTimeMillis(); 
                if ((now - startToWait) > (1000* maxWait))
                {
                    System.out.print("Timeout exceeded in getConnection(), DB not available!");
                    throw new OJBException(t);
                }
                else
                {
                	if ((now % 1000) == 0)
                	{
                		System.out.print("#");
                		System.out.flush();
                	}
        			
                }
            }
        }
        return conn;
    }

    /**
     * deletes all PerformanceArticle created by <code>insertNewArticles</code>.
     */
    protected void deleteArticles() throws Exception
    {
        Connection conn = getConnection();

        // Use the OJB SqlGenerator to generate SQL Statements. All details about
        // Table and column names are read from the repository.xml file.
        ClassDescriptor cld = broker.getClassDescriptor(PerformanceArticle.class);
        String sql = broker.serviceSqlGenerator().getPreparedDeleteStatement(cld).getStatement();

        logger.debug("delete stmt: " + sql);

        long start = System.currentTimeMillis();
        try
        {
            conn.setAutoCommit(false);
            PreparedStatement stmt = conn.prepareStatement(sql);
            for (int i = 0; i < articleCount; i++)
            {
                PerformanceArticle a = arr[i];
                stmt.setInt(1, a.articleId.intValue());
                stmt.execute();
            }
            conn.commit();
        }
        catch (Throwable t)
        {
            actualRetries++;
            if (actualRetries <= maxRetries)
            {
                logger.error("error during db operations:", t);
                try
                {
                    conn.close();
                }
                catch (Throwable ignored)
                {
                }
                deleteArticles();
            }
            else
            {
                logger.error("retry count exceeded!");
                fail(t.getMessage());
            }
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (Throwable ignored)
            {
            }
        }
        long stop = System.currentTimeMillis();
        logger.info("deleting " + articleCount + " Objects: " + (stop - start) + " msec");
    }

    /**
     * create new PerformanceArticle objects and insert them into the RDBMS.
     * The number of objects to create is defined by <code>articleCount</code>.
     */
    protected void insertNewArticles() throws Exception
    {
        Connection conn = getConnection();

        // Use the OJB SqlGenerator to generate SQL Statements. All details about
        // Table and column names are read from the repository.xml file.
        ClassDescriptor cld = broker.getClassDescriptor(PerformanceArticle.class);
        String sql = broker.serviceSqlGenerator().getPreparedInsertStatement(cld).getStatement();

        logger.debug("insert stmt: " + sql);

        long start = System.currentTimeMillis();
        try
        {
            conn.setAutoCommit(false);
            PreparedStatement stmt = conn.prepareStatement(sql);

            for (int i = 0; i < articleCount; i++)
            {
                PerformanceArticle a = arr[i];

                stmt.setInt(1, a.articleId.intValue());
                stmt.setString(2, a.articleName);
                stmt.setInt(3, a.supplierId);
                stmt.setInt(4, a.productGroupId);
                stmt.setString(5, a.unit);
                stmt.setDouble(6, a.price);
                stmt.setInt(7, a.stock);
                stmt.setInt(8, a.orderedUnits);
                stmt.setInt(9, a.minimumStock);

                stmt.execute();
            }
            conn.commit();
        }
        catch (Throwable t)
        {
            actualRetries++;
            if (actualRetries <= maxRetries)
            {
                logger.error("error during db operations:", t);
	            try
	            {
	                conn.close();
	            }
	            catch (Throwable ignored)
	            {
	            }                
                insertNewArticles();
            }
            else
            {
                logger.error("retry count exceeded!");
                fail(t.getMessage());
            }
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (Throwable ignored)
            {
            }
        }
        long stop = System.currentTimeMillis();
        logger.info("inserting " + articleCount + " Objects: " + (stop - start) + " msec");

    }

    protected void clearTable() throws Exception
    {
        Connection conn = getConnection();
        try
        {
            ClassDescriptor cld = broker.getClassDescriptor(PerformanceArticle.class);
            String table = cld.getFullTableName();
            String sql = "DELETE FROM " + table;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.execute();
            conn.close();
        }
        catch (Throwable t)
        {
            actualRetries++;
            if (actualRetries <= maxRetries)
            {
                logger.error("error during db operations:", t);
                try
                {
                    conn.close();
                }
                catch (Throwable ignored)
                {
                }
                clearTable();
            }
            else
            {
                logger.error("retry count exceeded!");
                fail(t.getMessage());
            }
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (Throwable ignored)
            {
            }
        }
    }

    /**
     * read in all the PerformanceArticles from the RDBMS that have
     * been inserted by <code>insertNewArticles()</code>.
     * The lookup is done one by one, that is: a primary key based lookup is used.
     */
    protected void readArticles() throws Exception
    {
        Connection conn = getConnection();

        // Use the OJB SqlGenerator to generate SQL Statements. All details about
        // Table and column names are read from the repository.xml file.
        ClassDescriptor cld = broker.getClassDescriptor(PerformanceArticle.class);
        String sql = broker.serviceSqlGenerator().getPreparedSelectByPkStatement(cld).getStatement();
        logger.debug("select stmt: " + sql);
        long start = System.currentTimeMillis();

        String colId = cld.getFieldDescriptorByName("articleId").getColumnName();
        String colName = cld.getFieldDescriptorByName("articleName").getColumnName();
        String colSupplier = cld.getFieldDescriptorByName("supplierId").getColumnName();
        String colGroup = cld.getFieldDescriptorByName("productGroupId").getColumnName();
        String colUnit = cld.getFieldDescriptorByName("unit").getColumnName();
        String colPrice = cld.getFieldDescriptorByName("price").getColumnName();
        String colStock = cld.getFieldDescriptorByName("stock").getColumnName();
        String colOrdered = cld.getFieldDescriptorByName("orderedUnits").getColumnName();
        String colMin = cld.getFieldDescriptorByName("minimumStock").getColumnName();

        try
        {
            conn.setAutoCommit(false);
            PreparedStatement stmt = conn.prepareStatement(sql);
            for (int i = 0; i < articleCount; i++)
            {
                stmt.setInt(1, offsetId + i);
                ResultSet rs = stmt.executeQuery();
                rs.next();

                PerformanceArticle a = new PerformanceArticle();
                a.articleId = new Integer(rs.getInt(colId));
                a.articleName = rs.getString(colName);
                a.supplierId = rs.getInt(colSupplier);
                a.productGroupId = rs.getInt(colGroup);
                a.unit = rs.getString(colUnit);
                a.price = rs.getFloat(colPrice);
                a.stock = rs.getInt(colStock);
                a.orderedUnits = rs.getInt(colOrdered);
                a.minimumStock = rs.getInt(colMin);
            }
        }
        catch (Throwable t)
        {
            actualRetries++;
            if (actualRetries <= maxRetries)
            {
                logger.error("error during db operations:", t);
	            try
	            {
	                conn.close();
	            }
	            catch (Throwable ignored)
	            {
	            }
                readArticles();
            }
            else
            {
                logger.error("retry count exceeded!");
                fail(t.getMessage());
            }
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (Throwable ignored)
            {
            }
        }


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
        Connection conn = getConnection();

        Criteria c = new Criteria();
        c.addBetween("articleId", new Integer(offsetId), new Integer(offsetId + articleCount));
        Query query = new QueryByCriteria(PerformanceArticle.class, c);

        // Use the OJB SqlGenerator to generate SQL Statements. All details about
        // Table and column names are read from the repository.xml file.
        ClassDescriptor cld = broker.getClassDescriptor(PerformanceArticle.class);
        String sql = broker.serviceSqlGenerator().getPreparedSelectStatement(query, cld).getStatement();

        logger.debug("select stmt: " + sql);
        long start = System.currentTimeMillis();

        String colId = cld.getFieldDescriptorByName("articleId").getColumnName();
        String colName = cld.getFieldDescriptorByName("articleName").getColumnName();
        String colSupplier = cld.getFieldDescriptorByName("supplierId").getColumnName();
        String colGroup = cld.getFieldDescriptorByName("productGroupId").getColumnName();
        String colUnit = cld.getFieldDescriptorByName("unit").getColumnName();
        String colPrice = cld.getFieldDescriptorByName("price").getColumnName();
        String colStock = cld.getFieldDescriptorByName("stock").getColumnName();
        String colOrdered = cld.getFieldDescriptorByName("orderedUnits").getColumnName();
        String colMin = cld.getFieldDescriptorByName("minimumStock").getColumnName();

        int fetchCount = 0;
        try
        {
            conn.setAutoCommit(false);
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, offsetId);
            stmt.setInt(2, offsetId + articleCount);
            ResultSet rs = stmt.executeQuery();
            while (rs.next())
            {
                fetchCount++;

                PerformanceArticle a = new PerformanceArticle();
                a.articleId = new Integer(rs.getInt(colId));
                a.articleName = rs.getString(colName);
                a.supplierId = rs.getInt(colSupplier);
                a.productGroupId = rs.getInt(colGroup);
                a.unit = rs.getString(colUnit);
                a.price = rs.getFloat(colPrice);
                a.stock = rs.getInt(colStock);
                a.orderedUnits = rs.getInt(colOrdered);
                a.minimumStock = rs.getInt(colMin);
            }
        }
        catch (Throwable t)
        {
            actualRetries++;
            if (actualRetries <= maxRetries)
            {
                logger.error("error during db operations:", t);
	            try
	            {
	                conn.close();
	            }
	            catch (Throwable ignored)
	            {
	            }                
                readArticlesByCursor();
            }
            else
            {
                logger.error("retry count exceeded!");
                fail(t.getMessage());
            }
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (Throwable ignored)
            {
            }
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
        Connection conn = getConnection();

        // Use the OJB SqlGenerator to generate SQL Statements. All details about
        // Table and column names are read from the repository.xml file.
        ClassDescriptor cld = broker.getClassDescriptor(PerformanceArticle.class);
        String sql = broker.serviceSqlGenerator().getPreparedUpdateStatement(cld).getStatement();
        logger.debug("update stmt: " + sql);

        // update all objects
        for (int i = 0; i < articleCount; i++)
        {
            arr[i].setPrice(arr[i].getPrice() * 1.95583);
        }

        long start = System.currentTimeMillis();
        try
        {
            conn.setAutoCommit(false);
            PreparedStatement stmt = conn.prepareStatement(sql);
            for (int i = 0; i < articleCount; i++)
            {
                PerformanceArticle a = arr[i];
                stmt.setString(1, a.articleName);
                stmt.setInt(2, a.supplierId);
                stmt.setInt(3, a.productGroupId);
                stmt.setString(4, a.unit);
                stmt.setDouble(5, a.price);
                stmt.setInt(6, a.stock);
                stmt.setInt(7, a.orderedUnits);
                stmt.setInt(8, a.minimumStock);
                stmt.setInt(9, a.articleId.intValue());
                stmt.execute();
            }
            conn.commit();
        }
        catch (Throwable t)
        {
            actualRetries++;
            if (actualRetries <= maxRetries)
            {
                logger.error("error during db operations:", t);
	            try
	            {
	                conn.close();
	            }
	            catch (Throwable ignored)
	            {
	            }                
                updateExistingArticles();
            }
            else
            {
                logger.error("retry count exceeded!");
                fail(t.getMessage());
            }
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (Throwable ignored)
            {
            }
        }
        long stop = System.currentTimeMillis();
        logger.info("updating " + articleCount + " Objects: " + (stop - start) + " msec");

    }

    /**
     * this method is the driver for the complete Benchmark.
     * It performs the following steps:
     *
     * 1.) n objects are created and inserted to the RDBMS.
     * 2.) the created objects are modified. Modifications are written to the RDBMS with updates.
     * 3.) All objects created in 1.) are read in by primary key based SELECT statements.
     * 4.) Step 3.) is repeated to test caching facilities.
     * 5.) All objects created in 1.) are read by iterating over a ResultSet.
     * 6.) All objects created in 1.) are deleted with n separate DELETE Statements.
     */
    public void testBenchmark()
    {
        try
        {
            logger.info("Test for native JDBC");
            for (int i = 0; i < iterations; i++)
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
        catch (Throwable t)
        {
            logger.error(t);
            fail(t.getMessage());
        }
    }

}
