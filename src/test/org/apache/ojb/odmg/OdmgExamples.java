package org.apache.ojb.odmg;

import java.util.List;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.TestHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.junit.OJBTestCase;
import org.apache.ojb.odmg.collections.DListImpl;
import org.apache.ojb.odmg.shared.Article;
import org.apache.ojb.odmg.shared.ProductGroup;
import org.apache.ojb.odmg.states.ModificationState;
import org.apache.ojb.odmg.states.StateNewClean;
import org.apache.ojb.odmg.states.StateNewDirty;
import org.odmg.DCollection;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.ODMGException;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/** Demo Application that shows basic concepts for Applications using the OJB ODMG
 * implementation as an transactional object server.
 */
public class OdmgExamples extends OJBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {OdmgExamples.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    private String databaseName;

    public OdmgExamples(String name)
    {
        super(name);
    }

    public void setUp()
    {
        databaseName = TestHelper.DEF_DATABASE_NAME;
    }

    public void tearDown()
    {
        databaseName = null;
    }

    /**TestThreadsNLocks state transition of modification states*/
    public void testModificationStates()
    {
        // get facade instance
        Implementation odmg = OJB.getInstance();
        Database db = odmg.newDatabase();
        //open database
        try
        {
            db.open(databaseName, Database.OPEN_READ_WRITE);
        }
        catch (ODMGException ex)
        {
            fail("ODMGException: " + ex.getMessage());
        }
        ModificationState oldState = StateNewClean.getInstance();
        ModificationState newState = oldState.markDirty();
        assertEquals(StateNewDirty.getInstance(), newState);

        oldState = newState;
        newState = oldState.markDirty();
        assertEquals(oldState, newState);

        // close database
        try
        {
            db.close();
        }
        catch (ODMGException ex)
        {
            fail("ODMGException: " + ex.getMessage());
        }

    }

    public void testOdmgSession()
    {
        // get facade instance
        Implementation odmg = OJB.getInstance();
        Database db = odmg.newDatabase();
        //open database
        try
        {
            db.open(databaseName, Database.OPEN_READ_WRITE);
        }
        catch (ODMGException ex)
        {
            fail("ODMGException: " + ex.getMessage());
        }
        Transaction tx = odmg.newTransaction();

        //perform transaction
        try
        {
            tx.begin();

            ProductGroup pg = new ProductGroup();
            pg.setName("PG A");
            Article example = new Article();
            example.setProductGroup(pg);
            pg.addArticle(example);
            db.makePersistent(pg);

            // modify Object after persist call is allowed
            example.setStock(333);
            example.addToStock(47);
            example.addToStock(7);
            example.addToStock(4);

            //System.out.println("now commit all changes...");
            tx.commit();
        }
        catch (Exception ex)
        {
            tx.abort();
        }

        // close database
        try
        {
            db.close();
        }
        catch (ODMGException ex)
        {
            fail("ODMGException: " + ex.getMessage());
        }
    }

    public void testOQLQuery()
    {
        // get facade instance
        Implementation odmg = OJB.getInstance();
        Database db = odmg.newDatabase();
        //open database
        try
        {
            db.open(databaseName, Database.OPEN_READ_WRITE);
        }
        catch (ODMGException ex)
        {
            fail("ODMGException: " + ex.getMessage());
        }
        Transaction tx = odmg.newTransaction();

        //perform transaction
        try
        {
            tx.begin();

            OQLQuery query = odmg.newOQLQuery();
            query.create("select anArticle from " + Article.class.getName() + " where articleId = 60");
            List results = (List) query.execute();

            Article a = (Article) results.get(0);

            // cross check with PersistenceBroker lookup
            // 1. get OID
            Article example = new Article();
            example.setArticleId(60);
            Identity oid = new Identity(example, ((TransactionImpl) tx).getBroker());
            // 2. lookup object by OID
            PersistenceBroker broker = ((TransactionImpl) tx).getBroker();
            broker.clearCache();
            Article b = (Article) broker.getObjectByIdentity(oid);

            assertEquals("should be same object", a, b);

            //System.out.println("now commit all changes...");
            tx.commit();
        }
        catch (Exception ex)
        {
            tx.abort();
            fail("ODMGException: " + ex.getMessage());
        }

        // close database
        try
        {
            db.close();
        }
        catch (ODMGException ex)
        {
            fail("ODMGException: " + ex.getMessage());
        }
    }

    public void testPathExpressionOqlQuery() throws Exception
    {
        // get facade instance
        Implementation odmg = OJB.getInstance();
        Database db = odmg.newDatabase();
        //open database
        try
        {
            db.open(databaseName, Database.OPEN_READ_WRITE);
        }
        catch (ODMGException ex)
        {
            fail("ODMGException: " + ex.getMessage());
        }
        Transaction tx = odmg.newTransaction();

        // perform transaction
        tx.begin();

        OQLQuery query = odmg.newOQLQuery();
        // use 'like' instead of '=' when perform query with wildcards
        query.create(
                "select anArticle from " + Article.class.getName() + " where productGroup.groupName like \"Fruit*\"");
        List results = (List) query.execute();

        // crosscheck
        query = odmg.newOQLQuery();
        query.create("select aPG from " + ProductGroup.class.getName() + " where groupName like \"Fruit*\"");
        List check = (List) query.execute();
        if (check.size() < 1)
            fail("Could not found ProductGroup's for: " +
                    "select aPG from " + ProductGroup.class.getName() + " where groupName like \"Fruit*\"");
        ProductGroup pg = (ProductGroup) check.get(0);

        assertEquals(pg.getAllArticlesInGroup().size(), results.size());
        assertTrue((results.size() > 0));

        tx.commit();


        // close database

        db.close();
    }

    public void testNrmAndDlists() throws Exception
    {
        // get facade instance
        Implementation odmg = OJB.getInstance();
        Database db = odmg.newDatabase();
        //open database
        try
        {
            db.open(databaseName, Database.OPEN_READ_WRITE);
        }
        catch (ODMGException ex)
        {
            fail("ODMGException: " + ex.getMessage());
        }
        Transaction tx = odmg.newTransaction();

        //perform transaction
        try
        {
            //=============================
            // this test needs DList impl as oql query collection class
            ((ImplementationImpl) odmg).setOqlCollectionClass(DListImpl.class);
            //=============================

            tx.begin();

            OQLQuery query = odmg.newOQLQuery();
            query.create("select x from " + Article.class.getName() + " where productGroupId = 7");
            List results = (List) query.execute();

            int originalSize = results.size();
            assertTrue("result count have to be > 0", originalSize > 0);

//            OJB.getLogger().debug(results);

            String name = "gimme fruits_" + System.currentTimeMillis();

            db.bind(results, name);
            tx.commit();

            tx = odmg.newTransaction();
            tx.begin();

            ((TransactionImpl) tx).getBroker().clearCache();

            // look it up again
            List newResults = (List) db.lookup(name);

            assertEquals(originalSize, newResults.size());
            Article art = (Article) newResults.get(0);
            assertNotNull(art);
//            OJB.getLogger().info(results);

            tx.commit();

        }
        catch (Exception e)

        {
            tx.abort();
            throw e;
        }

        // close database
        try
        {
            db.close();
        }
        catch (ODMGException ex)
        {
            fail("ODMGException: " + ex.getMessage());
        }
    }

    public void testOQLQueryBind()
    {
        // get facade instance
        Implementation odmg = OJB.getInstance();
        Database db = odmg.newDatabase();
        //open database
        try
        {
            db.open(databaseName, Database.OPEN_READ_WRITE);
        }
        catch (ODMGException ex)
        {
            fail("ODMGException: " + ex.getMessage());
        }
        Transaction tx = odmg.newTransaction();

        //perform transaction
        try
        {
            tx.begin();

            OQLQuery query = odmg.newOQLQuery();
            query.create("select anArticle from " + Article.class.getName() + " where articleId = $678");
            query.bind(new Integer(30));

            List results = (List) query.execute();

            Article a = (Article) results.get(0);

            //crosscheck with PersistenceBroker lookup
            // 1. get OID
            Article example = new Article();
            example.setArticleId(30);
            Identity oid = new Identity(example, ((TransactionImpl) tx).getBroker());

            // 2. lookup object by OID
            PersistenceBroker broker = ((TransactionImpl) tx).getBroker();
            broker.clearCache();
            Article b = (Article) broker.getObjectByIdentity(oid);

            assertEquals("should be same object", a, b);

            //System.out.println("now commit all changes...");
            tx.commit();
        }
        catch (Exception ex)

        {
            tx.abort();
            fail("ODMGException: " + ex.getMessage());
        }

        // close database
        try
        {
            db.close();
        }
        catch (ODMGException ex)
        {
            fail("ODMGException: " + ex.getMessage());
        }
    }

    public void YYYtestOQLQueryOnCollections() throws Exception
    {
        // get facade instance
        Implementation odmg = OJB.getInstance();
        Database db = odmg.newDatabase();
        //open database
        db.open(databaseName, Database.OPEN_READ_WRITE);
        Transaction tx = odmg.newTransaction();

        //perform transaction
        try
        {
            tx.begin();
            OQLQuery query = odmg.newOQLQuery();
            query.create("select aLotOfArticles from " + Article.class.getName() + " where productGroupId = 4");

            DCollection results = (DCollection) query.execute();
            results = results.query("price > 35");

            // now perform control query
            query = odmg.newOQLQuery();
            query.create(
                    "select aLotOfArticles from "
                    + Article.class.getName()
                    + " where productGroupId = 4 and price  > 35");

            DCollection check = (DCollection) query.execute();

            assertEquals(results, check);

            tx.commit();
        }
                // close database
        finally
        {
            db.close();
        }
    }

    /**try to open non-existing db*/
    public void YYYtestWrongDbName()
    {
        // get facade instance
        Implementation objectserver = OJB.getInstance();
        Database db = objectserver.newDatabase();

        //try open database with non existing repository file:
        String wrongDatabaseName = "ThereIsNoSuchFile";
        try
        {
            db.open(wrongDatabaseName, Database.OPEN_READ_WRITE);
            fail("should not be able to open database " + wrongDatabaseName);
        }
        catch (ODMGException ex)
        {
            return;
        }
    }

    /**try to crash odmg and broker tx*/
    public void YYYtestBrokerCrash()
    {
        // get facade instance
        Implementation odmg = OJB.getInstance();
        Database db = odmg.newDatabase();
        PersistenceBroker broker = null;
        ClassDescriptor cld = null;
        String tablename = null;

        //open database
        try
        {
            db.open(databaseName, Database.OPEN_READ_WRITE);
        }
        catch (ODMGException ex)
        {
            fail("ODMGException: " + ex.getMessage());
        }
        try
        {
            Transaction tx = odmg.newTransaction();
            tx.begin();

            // retrieve an Article
            OQLQuery query = odmg.newOQLQuery();
            query.create("select anArticle from " + Article.class.getName() + " where articleId = $678");
            query.bind(new Integer(30));
            List results = (List) query.execute();
            Article a = (Article) results.get(0);

            // manipulate metadata
            broker = ((TransactionImpl) tx).getBroker();
            cld = broker.getClassDescriptor(Article.class);
            tablename = cld.getFullTableName();
            cld.setTableName("ELVIS");
            broker.getDescriptorRepository().setClassDescriptor(cld);

            //broker will crash as metadata is corrupt
            a.addToStock(5);
            tx.commit();
            fail("Can commit tx with corrupt metadata");
        }
        catch (Throwable t)
        {
            //ignore
        }
        finally
        {
            cld.setTableName(tablename);
            broker.getDescriptorRepository().setClassDescriptor(cld);
        }

    }
}
