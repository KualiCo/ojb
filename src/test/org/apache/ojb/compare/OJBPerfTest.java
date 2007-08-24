package org.apache.ojb.compare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.TestHelper;
import org.apache.ojb.broker.accesslayer.LookupException;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.util.ObjectModification;
import org.apache.ojb.odmg.OJB;
import org.apache.ojb.odmg.TransactionExt;
import org.apache.ojb.otm.OTMConnection;
import org.apache.ojb.otm.OTMKit;
import org.apache.ojb.otm.kit.SimpleKit;
import org.apache.ojb.otm.lock.LockType;
import org.apache.ojb.performance.PerfArticle;
import org.apache.ojb.performance.PerfArticleImpl;
import org.apache.ojb.performance.PerfTest;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.ODMGException;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * Multi-threaded performance test implementation classes for testing
 * the PB-api, ODMG-api of OJB against native JDBC using
 * the performance-package test classes.
 *
 * @version $Id: OJBPerfTest.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class OJBPerfTest
{
    // =====================================================================================
    // Inner class, test handle using native JDBC
    // =====================================================================================
    public static class JdbcPerfTest extends PerfTest
    {
        private static final String TABLE_NAME = "PERF_ARTICLE";
        // cast to int to avoid problems with DB field length
        public static volatile int counter = (int) System.currentTimeMillis();
        private PersistenceBroker broker;

        public synchronized static Long nextKey()
        {
            return new Long(++counter);
        }

        public void init() throws Exception
        {
            if (broker == null)
            {
                broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            }
        }

        public void tearDown() throws Exception
        {
            if (broker != null)
            {
                broker.close();
                broker = null;
            }
        }

        public String testName()
        {
            return "JDBC";
        }

        public int articleCount()
        {
            int result = 0;
            try
            {
                String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
                Connection con = getConnection();
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                rs.next();
                result = rs.getInt(1);
                rs.close();
                stmt.close();
                releaseConnection();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return result;
        }

        public void insertNewArticles(PerfArticle[] arr) throws Exception
        {
            StringBuffer buf = new StringBuffer();
            buf.append("INSERT INTO ").append(TABLE_NAME);
            buf.append(" (ARTICLE_ID, ARTICLE_NAME, MINIMUM_STOCK, PRICE, UNIT, STOCK, SUPPLIER_ID)");
            buf.append(" VALUES (?,?,?,?,?,?,?)");
            // lookup the connection (using OJB's con pooling support to make the test more fair)
            Connection con = getConnection();
            con.setAutoCommit(false);
            for (int i = 0; i < arr.length; i++)
            {
                // OJB doesn't use a PS-pool (normally the JDBC driver supports statement pooling)
                // thus to make this test more fair lookup a new PS for each object
                PreparedStatement stmt = con.prepareStatement(buf.toString());
                PerfArticle article = arr[i];
                // generate PK value
                article.setArticleId(nextKey());
                stmt.setLong(1, article.getArticleId().longValue());
                stmt.setString(2, article.getArticleName());
                stmt.setInt(3, article.getMinimumStock());
                stmt.setDouble(4, article.getPrice());
                stmt.setString(5, article.getUnit());
                stmt.setInt(6, article.getStock());
                stmt.setInt(7, article.getSupplierId());
                stmt.executeUpdate();
                stmt.close();
            }
            con.commit();
            con.setAutoCommit(true);
            releaseConnection();
        }

        public void insertNewArticlesStress(PerfArticle[] arr) throws Exception
        {
            System.out.println("Stress-Mode is NOT supported");
        }

        public Collection readArticlesByCursor(String articleName) throws Exception
        {
            String sql = "SELECT * FROM " + TABLE_NAME + " WHERE ARTICLE_NAME LIKE '" + articleName + "'";
            Connection con = getConnection();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            ArrayList list = new ArrayList();
            while (rs.next())
            {
                PerfArticle article = new PerfArticleImpl();
                article.setArticleId(new Long(rs.getLong("ARTICLE_ID")));
                article.setArticleName(rs.getString("ARTICLE_NAME"));
                article.setMinimumStock(rs.getInt("MINIMUM_STOCK"));
                article.setPrice(rs.getDouble("PRICE"));
                article.setUnit(rs.getString("UNIT"));
                article.setStock(rs.getInt("STOCK"));
                article.setSupplierId(rs.getInt("SUPPLIER_ID"));
                list.add(article);
            }
            rs.close();
            stmt.close();
            releaseConnection();
            return list;
        }

        public PerfArticle getArticleByIdentity(Long articleId) throws Exception
        {
            String sql = "SELECT * FROM " + TABLE_NAME + " WHERE ARTICLE_ID=" + articleId.longValue() + "";
            Connection con = getConnection();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            PerfArticle result = null;
            while (rs.next())
            {
                result = new PerfArticleImpl();
                result.setArticleId(new Long(rs.getLong("ARTICLE_ID")));
                result.setArticleName(rs.getString("ARTICLE_NAME"));
                result.setMinimumStock(rs.getInt("MINIMUM_STOCK"));
                result.setPrice(rs.getDouble("PRICE"));
                result.setUnit(rs.getString("UNIT"));
                result.setStock(rs.getInt("STOCK"));
                result.setSupplierId(rs.getInt("SUPPLIER_ID"));
            }
            rs.close();
            stmt.close();
            releaseConnection();
            return result;
        }

        public void updateArticles(PerfArticle[] arr) throws Exception
        {
            // we don't know which field is to update, thus do do all
            StringBuffer buf = new StringBuffer();
            buf.append("UPDATE ").append(TABLE_NAME);
            buf.append(" SET ARTICLE_NAME = ?, MINIMUM_STOCK = ?, PRICE = ?, UNIT = ?, STOCK = ?, SUPPLIER_ID = ?");
            buf.append(" WHERE ARTICLE_ID = ?");
            Connection con = getConnection();
            con.setAutoCommit(false);
            for (int i = 0; i < arr.length; i++)
            {
                // OJB doesn't use a PS-pool (normally the JDBC driver supports statement pooling)
                // thus to make this test more fair lookup a new PS for each object
                PreparedStatement stmt = con.prepareStatement(buf.toString());
                PerfArticle article = arr[i];
                stmt.setString(1, article.getArticleName());
                stmt.setInt(2, article.getMinimumStock());
                stmt.setDouble(3, article.getPrice());
                stmt.setString(4, article.getUnit());
                stmt.setInt(5, article.getStock());
                stmt.setInt(6, article.getSupplierId());
                stmt.setLong(7, article.getArticleId().longValue());
                stmt.executeUpdate();
                stmt.close();
            }
            con.commit();
            con.setAutoCommit(true);
            releaseConnection();
        }

        public void updateArticlesStress(PerfArticle[] arr) throws Exception
        {
            System.out.println("Stress-Mode is NOT supported");
        }

        public void deleteArticles(PerfArticle[] arr) throws Exception
        {
            String sql = "DELETE FROM " + TABLE_NAME + " WHERE ARTICLE_ID = ?";
            Connection con = getConnection();
            con.setAutoCommit(false);
            for (int i = 0; i < arr.length; i++)
            {
                // OJB doesn't use a PS-pool (normally the JDBC driver supports statement pooling)
                // thus to make this test more fair lookup a new PS for each object
                PreparedStatement stmt = con.prepareStatement(sql);
                PerfArticle article = arr[i];
                stmt.setLong(1, article.getArticleId().longValue());
                stmt.execute();
                stmt.close();
            }
            con.commit();
            con.setAutoCommit(true);
            releaseConnection();
        }

        public void deleteArticlesStress(PerfArticle[] arr) throws Exception
        {
            System.out.println("Stress-Mode is NOT supported");
        }

        private Connection getConnection() throws LookupException
        {
            // don't let OJB handle batching
            broker.serviceConnectionManager().setBatchMode(false);
            return broker.serviceConnectionManager().getConnection();
        }

        private void releaseConnection()
        {
            broker.serviceConnectionManager().releaseConnection();
        }
    }



    // =====================================================================================
    // Inner class, test handle using PB-api
    // =====================================================================================
    public static class PBPerfTest extends PerfTest
    {
        public void init() throws Exception
        {
        }

        public void tearDown() throws Exception
        {
        }

        public String testName()
        {
            return "PB";
        }

        public int articleCount()
        {
            Criteria c = new Criteria();
            Query q = new QueryByCriteria(PerfArticleImpl.class, c);
            int count = 0;
            try
            {
                PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                count = broker.getCount(q);
                broker.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return count;
        }

        /**
         * A resource cumbering insert-method implementation,
         * this was used to test implementation.
         */
        public void insertNewArticlesStress(PerfArticle[] arr) throws Exception
        {
            for (int i = 0; i < arr.length; i++)
            {
                PersistenceBroker broker = null;
                try
                {
                    broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                    broker.beginTransaction();
                    broker.store(arr[i]);
                    broker.commitTransaction();
                }
                finally
                {
                    if (broker != null) broker.close();
                }
            }
        }

        /**
         * A performance optimized insert-method implementation,
         * used to test performance.
         */
        public void insertNewArticles(PerfArticle[] arr) throws Exception
        {
            PersistenceBroker broker = null;
            try
            {
                broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                broker.serviceConnectionManager().setBatchMode(true);
                broker.beginTransaction();
                for (int i = 0; i < arr.length; i++)
                {
                    broker.store(arr[i], ObjectModification.INSERT);
                }
                broker.commitTransaction();
            }
            finally
            {
                if (broker != null) broker.close();
            }
        }

        public Collection readArticlesByCursor(String articleName) throws Exception
        {
            Criteria c = new Criteria();
            c.addLike("articleName", articleName);
            Query q = new QueryByCriteria(PerfArticleImpl.class, c);

            Collection col = null;
            PersistenceBroker broker = null;
            try
            {
                broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                col = broker.getCollectionByQuery(q);
            }
            finally
            {
                if (broker != null) broker.close();
            }
            return col;
        }

        public PerfArticle getArticleByIdentity(Long articleId) throws Exception
        {
            PersistenceBroker broker = null;
            try
            {
                broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                return (PerfArticle) broker.getObjectByIdentity(broker.serviceIdentity().buildIdentity(PerfArticleImpl.class, articleId));
            }
            finally
            {
                if (broker != null) broker.close();
            }
        }

        public void updateArticles(PerfArticle[] arr) throws Exception
        {
            PersistenceBroker broker = null;
            try
            {
                broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                broker.serviceConnectionManager().setBatchMode(true);
                broker.beginTransaction();
                for (int i = 0; i < arr.length; i++)
                {
                    broker.store(arr[i], ObjectModification.UPDATE);
                    // broker.store(arr[i]);
                }
                broker.commitTransaction();
            }
            finally
            {
                if (broker != null) broker.close();
            }
        }

        public void updateArticlesStress(PerfArticle[] arr) throws Exception
        {
            for (int i = 0; i < arr.length; i++)
            {
                PersistenceBroker broker = null;
                try
                {
                    broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                    broker.beginTransaction();
                    broker.store(arr[i]);
                    broker.commitTransaction();
                }
                finally
                {
                    if (broker != null) broker.close();
                }
            }
        }

        /**
         * A resource cumbering delete-method implementation,
         * used to test implementation
         */
        public void deleteArticlesStress(PerfArticle[] arr) throws Exception
        {
            for (int i = 0; i < arr.length; i++)
            {
                PersistenceBroker broker = null;
                try
                {
                    broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                    broker.beginTransaction();
                    broker.delete(arr[i]);
                    broker.commitTransaction();
                }
                finally
                {
                    if (broker != null) broker.close();
                }
            }
        }

        /**
         * A performance optimized delete-method implementation,
         * used to test performance
         */
        public void deleteArticles(PerfArticle[] arr) throws Exception
        {
            PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            try
            {
                broker.serviceConnectionManager().setBatchMode(true);
                broker.beginTransaction();
                for (int i = 0; i < arr.length; i++)
                {
                    broker.delete(arr[i]);
                }
                broker.commitTransaction();
            }
            finally
            {
                if (broker != null) broker.close();
            }
        }
    }


    // =====================================================================================
    // Inner class, test handle using ODMG-api
    // =====================================================================================
    public static class ODMGPerfTest extends PerfTest
    {
        private Implementation odmg;
        private Database db;
        private TransactionExt m_tx;

        public void init()
        {
            try
            {
                odmg = OJB.getInstance();
                db = odmg.newDatabase();
                db.open(TestHelper.DEF_DATABASE_NAME, Database.OPEN_READ_WRITE);
                m_tx = (TransactionExt) odmg.newTransaction();
            }
            catch (ODMGException e)
            {
                e.printStackTrace();
            }
        }

        public void tearDown() throws Exception
        {
            if (m_tx.isOpen()) m_tx.abort();
            //db.close();
        }

        public String testName()
        {
            return "ODMG";
        }

        public int articleCount()
        {
            Criteria c = new Criteria();
            Query q = new QueryByCriteria(PerfArticleImpl.class, c);
            int count = 0;
            try
            {
                PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                count = broker.getCount(q);
                broker.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return count;
        }

        /**
         * A performance optimized insert-method implementation,
         * used to test performance
         */
        public void insertNewArticles(PerfArticle[] arr) throws Exception
        {
            m_tx.begin();
            for (int i = 0; i < arr.length; i++)
            {
                db.makePersistent(arr[i]);
            }
            m_tx.commit();
        }

        /**
         * A resource cumbering insert-method implementation,
         * used to test implementation
         */
        public void insertNewArticlesStress(PerfArticle[] arr) throws Exception
        {
            for (int i = 0; i < arr.length; i++)
            {
                Transaction tx = odmg.newTransaction();
                tx.begin();
                db.makePersistent(arr[i]);
                tx.commit();
            }
        }

        public Collection readArticlesByCursor(String articleName) throws Exception
        {
            m_tx.setImplicitLocking(false);
            m_tx.begin();
            OQLQuery query = odmg.newOQLQuery();
            String sql = "select allArticles from " + PerfArticleImpl.class.getName() +
                    " where articleName like $1";
            query.create(sql);
            query.bind(articleName);
            List allProducts = (List) query.execute();
            m_tx.commit();
            return allProducts;
        }

        public PerfArticle getArticleByIdentity(Long articleId) throws Exception
        {
//            m_tx.setImplicitLocking(false);
//            m_tx.begin();
//            OQLQuery query = odmg.newOQLQuery();
//            String sql = "select allArticles from " + PerfArticleImpl.class.getName() +
//                    " where articleId=$";
//            query.create(sql);
//            query.bind(articleId);
//            List result = (List) query.execute();
//            m_tx.commit();
//            return (PerfArticle) result.get(0);
// use OJB's extension for faster Identity lookup
            PerfArticle result;
            m_tx.setImplicitLocking(false);
            m_tx.begin();
            PersistenceBroker pb = m_tx.getBroker();
            result = (PerfArticle) pb.getObjectByIdentity(pb.serviceIdentity().buildIdentity(PerfArticleImpl.class, articleId));
            m_tx.commit();
            return result;
        }

        public void updateArticles(PerfArticle[] arr) throws Exception
        {
            m_tx.begin();
            for (int i = 0; i < arr.length; i++)
            {
                m_tx.lock(arr[i], Transaction.WRITE);
                m_tx.markDirty(arr[i]);
            }
            m_tx.commit();
        }

        public void updateArticlesStress(PerfArticle[] arr) throws Exception
        {
            for (int i = 0; i < arr.length; i++)
            {
                Transaction tx = odmg.newTransaction();
                tx.begin();
                tx.lock(arr[i], Transaction.WRITE);
                m_tx.markDirty(arr[i]);
                tx.commit();
            }
        }

        /**
         * A performance optimized delete-method implementation,
         * use to test performance
         */
        public void deleteArticles(PerfArticle[] arr) throws Exception
        {
            m_tx.begin();
            for (int i = 0; i < arr.length; i++)
            {
                db.deletePersistent(arr[i]);
            }
            m_tx.commit();
        }

        /**
         * A resource cumbering insert-method implementation,
         * use to test implementation
         */
        public void deleteArticlesStress(PerfArticle[] arr) throws Exception
        {
            for (int i = 0; i < arr.length; i++)
            {
                Transaction tx = odmg.newTransaction();
                tx.begin();
                db.deletePersistent(arr[i]);
                tx.commit();
            }
        }
    }

    // =====================================================================================
    // Inner class, test handle using OTM-api
    // =====================================================================================
    public static class OTMPerfTest extends PerfTest
    {
        private OTMKit _kit;

        private OTMConnection _conn;

        private org.apache.ojb.otm.core.Transaction _tx;

        public void init()
        {
            _kit = SimpleKit.getInstance();
            _conn = _kit.acquireConnection(PersistenceBrokerFactory.getDefaultKey());
        }

        public void tearDown() throws Exception
        {
            if ((_tx != null) && _tx.isInProgress())
            {
                _tx.rollback();
            }
            _conn.close();
        }

        public String testName()
        {
            return "OTM";
        }

        public int articleCount()
        {
            Criteria c = new Criteria();
            Query q = new QueryByCriteria(PerfArticleImpl.class, c);
            int count = 0;
            try
            {
                PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                count = broker.getCount(q);
                broker.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return count;
        }

        /**
         * A resource cumbering insert-method implementation,
         * this was used to test implementation.
         */
        public void insertNewArticlesStress(PerfArticle[] arr) throws Exception
        {
            for (int i = 0; i < arr.length; i++)
            {
                _tx = _kit.getTransaction(_conn);
                _tx.begin();
                _conn.makePersistent(arr[i]);
                _tx.commit();
            }
        }

        /**
         * A performance optimized insert-method implementation,
         * used to test performance.
         */
        public void insertNewArticles(PerfArticle[] arr) throws Exception
        {
            _tx = _kit.getTransaction(_conn);
            _tx.begin();
            for (int i = 0; i < arr.length; i++)
            {
                _conn.makePersistent(arr[i]);
            }
            _tx.commit();
        }

        public Collection readArticlesByCursor(String articleName) throws Exception
        {
            Criteria c = new Criteria();
            c.addLike("articleName", articleName);
            Query q = new QueryByCriteria(PerfArticleImpl.class, c);

            _tx = _kit.getTransaction(_conn);
            _tx.begin();
            Collection col = _conn.getCollectionByQuery(q, LockType.NO_LOCK);
            _tx.commit();
            return col;
        }

        public PerfArticle getArticleByIdentity(Long articleId) throws Exception
        {
            Criteria c = new Criteria();
            c.addEqualTo("articleId", articleId);
            Query q = new QueryByCriteria(PerfArticleImpl.class, c);

            _tx = _kit.getTransaction(_conn);
            _tx.begin();
            // the getByIdeneityMethod() needs Identity and this is currently not supported
            Collection col = _conn.getCollectionByQuery(q, LockType.NO_LOCK);
            _tx.commit();
            Iterator it = col.iterator();
            return it.hasNext() ? (PerfArticle) it.next() : null;
        }

        public void updateArticles(PerfArticle[] arr) throws Exception
        {
            _tx = _kit.getTransaction(_conn);
            _tx.begin();
            for (int i = 0; i < arr.length; i++)
            {
                Identity oid = _conn.getIdentity(arr[i]);
                PerfArticle a = (PerfArticle) _conn.getObjectByIdentity(oid, LockType.WRITE_LOCK);
                a.setArticleName("" + System.currentTimeMillis());
            }
            _tx.commit();
        }

        public void updateArticlesStress(PerfArticle[] arr) throws Exception
        {
            for (int i = 0; i < arr.length; i++)
            {
                _tx = _kit.getTransaction(_conn);
                _tx.begin();
                Identity oid = _conn.getIdentity(arr[i]);
                PerfArticle a = (PerfArticle) _conn.getObjectByIdentity(oid, LockType.WRITE_LOCK);
                a.setArticleName("" + System.currentTimeMillis());
                _tx.commit();
            }
        }

        /**
         * A resource cumbering delete-method implementation,
         * used to test implementation
         */
        public void deleteArticlesStress(PerfArticle[] arr) throws Exception
        {
            for (int i = 0; i < arr.length; i++)
            {
                _tx = _kit.getTransaction(_conn);
                _tx.begin();
                _conn.deletePersistent(arr[i]);
                _tx.commit();
            }
        }

        /**
         * A performance optimized delete-method implementation,
         * used to test performance
         */
        public void deleteArticles(PerfArticle[] arr) throws Exception
        {
            _tx = _kit.getTransaction(_conn);
            _tx.begin();
            for (int i = 0; i < arr.length; i++)
            {
                _conn.deletePersistent(arr[i]);
            }
            _tx.commit();
        }
    }
}
