package org.apache.ojb.compare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;

/**
 * This TestCase contains the OJB single-threaded performance benchmarks for the
 * JDBC API. This is the reference for other benchmarks.
 *
 * @author Thomas Mahler
 */
public class PerformanceJdbcTest extends PerformanceBaseTest
{
    /**
     * BrokerTests constructor comment.
     *
     * @param name java.lang.String
     */
    public PerformanceJdbcTest(String name)
    {
        super(name);
        setNameOfTest("Test for JDBC");
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

        String[] arr = {PerformanceJdbcTest.class.getName()};
        junit.textui.TestRunner.main(arr);
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
            for(int i = 0; i < articleCount; i++)
            {
                PerformanceArticle a = arr[i];
                stmt.setInt(1, a.articleId.intValue());
                stmt.execute();
            }
            conn.commit();
        }
        catch(Throwable t)
        {
            logger.error(t);
            fail(t.getMessage());
        }
        finally
        {
            if(conn != null)
                returnConnection(conn);
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

            for(int i = 0; i < articleCount; i++)
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
        catch(Throwable t)
        {
            logger.error(t);
            fail(t.getMessage());
        }
        finally
        {
            if(conn != null)
                returnConnection(conn);
        }
        long stop = System.currentTimeMillis();
        logger.info("inserting " + articleCount + " Objects: " + (stop - start) + " msec");

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

        String colId = cld.getFieldDescriptorByName("articleId").getColumnName();
        String colName = cld.getFieldDescriptorByName("articleName").getColumnName();
        String colSupplier = cld.getFieldDescriptorByName("supplierId").getColumnName();
        String colGroup = cld.getFieldDescriptorByName("productGroupId").getColumnName();
        String colUnit = cld.getFieldDescriptorByName("unit").getColumnName();
        String colPrice = cld.getFieldDescriptorByName("price").getColumnName();
        String colStock = cld.getFieldDescriptorByName("stock").getColumnName();
        String colOrdered = cld.getFieldDescriptorByName("orderedUnits").getColumnName();
        String colMin = cld.getFieldDescriptorByName("minimumStock").getColumnName();

        long start = System.currentTimeMillis();
        try
        {
            conn.setAutoCommit(false);
            PreparedStatement stmt = conn.prepareStatement(sql);
            for(int i = 0; i < articleCount; i++)
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
        catch(Throwable t)
        {
            logger.error(t);
            fail(t.getMessage());
        }
        finally
        {
            if(conn != null)
                returnConnection(conn);
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
        long start = System.currentTimeMillis();
        try
        {
            conn.setAutoCommit(false);
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, offsetId);
            stmt.setInt(2, offsetId + articleCount);
            ResultSet rs = stmt.executeQuery();
            while(rs.next())
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
        catch(Throwable t)
        {
            logger.error(t);
            fail(t.getMessage());
        }
        finally
        {
            if(conn != null)
                returnConnection(conn);
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
        for(int i = 0; i < articleCount; i++)
        {
            arr[i].setPrice(arr[i].getPrice() * 1.95583);
        }

        long start = System.currentTimeMillis();
        try
        {
            conn.setAutoCommit(false);
            PreparedStatement stmt = conn.prepareStatement(sql);
            for(int i = 0; i < articleCount; i++)
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
        catch(Throwable t)
        {
            logger.error(t);
            fail(t.getMessage());
        }
        finally
        {
            if(conn != null)
                returnConnection(conn);
        }
        long stop = System.currentTimeMillis();
        logger.info("updating " + articleCount + " Objects: " + (stop - start) + " msec");
    }
}
