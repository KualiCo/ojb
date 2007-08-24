package org.apache.ojb.otm;

import java.util.Collection;
import java.util.Iterator;

import org.apache.ojb.broker.Article;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.InterfaceArticle;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.ProductGroup;
import org.apache.ojb.broker.ProductGroupWithCollectionProxy;
import org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.OJBTestCase;
import org.apache.ojb.odmg.oql.EnhancedOQLQuery;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.lock.LockType;
import org.apache.ojb.otm.lock.LockingException;
import org.apache.ojb.otm.lock.wait.DeadlockException;
import org.apache.ojb.otm.lock.wait.NoWaitStrategy;
import org.apache.ojb.otm.lock.wait.TimeoutStrategy;

/**
 * Demo Application that shows basic concepts for Applications
 * using the OJB OTM layer directly.
 */
public class OtmExamples extends OJBTestCase
{
    private static Class CLASS = OtmExamples.class;
    private TestKit _kit;
    private OTMConnection _conn;

    public OtmExamples(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
		super.setUp();
        ojbChangeReferenceSetting(ProductGroup.class, "allArticlesInGroup", true, true, true, false);
		ojbChangeReferenceSetting(Article.class, "productGroup", true, true, true, false);
        _kit = TestKit.getTestInstance();
        _conn = _kit.acquireConnection(PersistenceBrokerFactory.getDefaultKey());
    }

    public void tearDown() throws Exception
    {
        _conn.close();
        _conn = null;
        super.tearDown();
    }

    public void testOtmSession() throws Throwable
    {
        Transaction tx = null;
        Criteria crit;
        Query q;
        EnhancedOQLQuery oql;
        Iterator it;
        Article example;

        //perform transaction
        try
        {
            tx = _kit.getTransaction(_conn);
            tx.begin();

            example = (Article) _conn.getObjectByIdentity(
                    new Identity(Article.class, Article.class,
                                 new Object[] {new Integer(77777)}));
            if (example == null)
            {
                example = Article.createInstance();
                example.setArticleId(new Integer(77777));
            }
            example.setProductGroupId(new Integer(7));
            example.setStock(333);
            example.setArticleName("333");
            _conn.makePersistent(example);

            tx.commit();

            Identity oid = _conn.getIdentity(example);

            // get from the cache
            tx = _kit.getTransaction(_conn);
            tx.begin();
            example = (Article) _conn.getObjectByIdentity(oid);
            assertEquals("should be equal", 7, example.getProductGroupId().intValue());
            assertEquals("should be equal", 333, example.getStock());
            assertEquals("should be equal", "333", example.getArticleName());
            tx.commit();

            // get from the database
            tx = _kit.getTransaction(_conn);
            tx.begin();
            _conn.invalidate(oid);
            example = (Article) _conn.getObjectByIdentity(oid);
            assertEquals("should be equal", 7, example.getProductGroupId().intValue());
            assertEquals("should be equal", "333", example.getArticleName());
            example.setArticleName("334"); // test update
            tx.commit();

            // get from the database via Query
            tx = _kit.getTransaction(_conn);
            _conn.invalidate(oid);
            tx.begin();
            crit = new Criteria();
            crit.addEqualTo("articleId", new Integer(77777));
            crit.addEqualTo("articleName", "334");
            q = QueryFactory.newQuery(Article.class, crit);
            it = _conn.getIteratorByQuery(q);
            if (it.hasNext())
            {
                InterfaceArticle article = (InterfaceArticle) it.next();
                assertEquals("should be equal", 77777, article.getArticleId().intValue());
                assertEquals("should be equal", "334", article.getArticleName());
                article.setArticleName("335"); // test update
                if (it.hasNext())
                {
                    fail("Query returned more than 1 object");
                }
            }
            else
            {
                fail("Query returned empty result set");
            }
            tx.commit();

            // get from the database via OQLQuery Iterator
            tx = _kit.getTransaction(_conn);
            _conn.invalidate(oid);
            tx.begin();
            oql = _conn.newOQLQuery();
            oql.create("select a from " + Article.class.getName()
                + " where articleId=$1 and articleName=$2");
            oql.bind(new Integer(77777));
            oql.bind("335");
            it = _conn.getIteratorByOQLQuery(oql);
            if (it.hasNext())
            {
                InterfaceArticle article = (InterfaceArticle) it.next();
                assertEquals("should be equal", 77777, article.getArticleId().intValue());
                assertEquals("should be equal", "335", article.getArticleName());
                article.setArticleName("336"); // test update
                if (it.hasNext())
                {
                    fail("Query returned more than 1 object");
                }
            }
            else
            {
                fail("Query returned empty result set");
            }
            tx.commit();

            // get from the database via OQLQuery Collection
            tx = _kit.getTransaction(_conn);
            _conn.invalidate(oid);
            tx.begin();
            oql.bind(new Integer(77777));
            oql.bind("336");
            it = ((Collection) oql.execute()).iterator();
            if (it.hasNext())
            {
                InterfaceArticle article = (InterfaceArticle) it.next();
                assertEquals("should be equal", 77777, article.getArticleId().intValue());
                assertEquals("should be equal", "336", article.getArticleName());
                article.setArticleName("337"); // test update
                if (it.hasNext())
                {
                    fail("Query returned more than 1 object");
                }
            }
            else
            {
                fail("Query returned empty result set");
            }
            tx.commit();

            // get from the database
            tx = _kit.getTransaction(_conn);
            tx.begin();
            _conn.invalidate(oid);
            example = (Article) _conn.getObjectByIdentity(oid);
            assertEquals("should be equal", "337", example.getArticleName());
            tx.commit();

            try
            {
                tx = _kit.getTransaction(_conn);
                tx.begin();
                example = (Article) _conn.getObjectByIdentity(oid);
                _conn.deletePersistent(example);
                tx.commit();
            }
            catch (Throwable ex)
            {
                ex.printStackTrace();
                tx.rollback();
            }
        }
        catch (Throwable ex)
        {
            try
            {
                if (tx != null && tx.isInProgress())
                {
                    tx.rollback();
                }
            }
            catch (Exception ex2)
            {
            }
            throw ex;
        }
    }

    public void testCollectionProxy() throws Throwable
    {
        Transaction tx = null;

        //perform transaction
        try
        {
            tx = _kit.getTransaction(_conn);
            tx.begin();

            ProductGroupWithCollectionProxy pg = new ProductGroupWithCollectionProxy();
            pg.setId(new Integer(77777));
            pg.setName("1");
            _conn.makePersistent(pg);

            tx.commit();

            Identity oid = _conn.getIdentity(pg);

            // get from the database
            tx = _kit.getTransaction(_conn);
            tx.begin();
            _conn.invalidate(oid);
            pg = (ProductGroupWithCollectionProxy) _conn.getObjectByIdentity(oid);
            assertTrue("CollectionProxy isn't loaded",
                    !((CollectionProxyDefaultImpl) pg.getAllArticlesInGroup()).isLoaded());
            Article article = Article.createInstance();
            article.setArticleId(new Integer(77777));
            article.setProductGroup(pg);
            article.setStock(333);
            article.setArticleName("333");
            pg.getAllArticlesInGroup().add(article);
            _conn.makePersistent(article);
            tx.commit();

            // get from the database
            tx = _kit.getTransaction(_conn);
            tx.begin();
            _conn.invalidate(oid);
            pg = (ProductGroupWithCollectionProxy) _conn.getObjectByIdentity(oid);
            assertEquals("CollectionProxy size", 1, pg.getAllArticlesInGroup().size());
            ((InterfaceArticle) pg.getAllArticlesInGroup().get(0)).setArticleName("444");
            tx.commit();

            // test isolation of the cache
            ((InterfaceArticle) pg.getAllArticlesInGroup().get(0)).setArticleName("555");

            tx = _kit.getTransaction(_conn);
            tx.begin();
            pg = (ProductGroupWithCollectionProxy) _conn.getObjectByIdentity(oid);
            assertEquals("Article name", "444",
                    ((InterfaceArticle) pg.getAllArticlesInGroup().get(0)).getArticleName());
            tx.commit();

            try
            {
                tx = _kit.getTransaction(_conn);
                tx.begin();
                pg = (ProductGroupWithCollectionProxy) _conn.getObjectByIdentity(oid);
                _conn.deletePersistent(pg.getAllArticlesInGroup().get(0));
                _conn.deletePersistent(pg);
                tx.commit();
            }
            catch (Throwable ex)
            {
                ex.printStackTrace();
                tx.rollback();
            }
        }
        catch (Throwable ex)
        {
            try
            {
                if (tx != null && tx.isInProgress())
                {
                    tx.rollback();
                }
            }
            catch (Exception ex2)
            {
            }
            throw ex;
        }
    }

    public void testOtmCache() throws Throwable
    {
        Transaction tx = null;

        //perform transaction
        try
        {
            tx = _kit.getTransaction(_conn);
            tx.begin();

            ProductGroup pg = new ProductGroup();
            pg.setId(new Integer(77777));
            pg.setName("1");
            _conn.makePersistent(pg);
            Article article = Article.createInstance();
            article.setArticleId(new Integer(77777));
            article.setStock(373);
            pg.add(article);
            article.setProductGroup(pg);
            _conn.makePersistent(article);
            tx.commit();

            Identity aOid = _conn.getIdentity(article);
            Identity pgOid = _conn.getIdentity(pg);

            tx = _kit.getTransaction(_conn);
            tx.begin();
            pg = (ProductGroup) _conn.getObjectByIdentity(pgOid);
            pg.setName("2");
            _conn.makePersistent(pg);
            tx.rollback();

            tx = _kit.getTransaction(_conn);
            tx.begin();
            article = (Article) _conn.getObjectByIdentity(aOid);
            assertEquals("should be equal", "1", article.getProductGroup().getName());
            tx.commit();

            // test checkpoint
            tx = _kit.getTransaction(_conn);
            tx.begin();
            pg = (ProductGroup) _conn.getObjectByIdentity(pgOid);
            pg.setName("2");
            _conn.makePersistent(pg);
            tx.checkpoint();
            tx.rollback();

            tx = _kit.getTransaction(_conn);
            tx.begin();
            article = (Article) _conn.getObjectByIdentity(aOid);
            assertEquals("should be equal", "1", article.getProductGroup().getName());
            tx.commit();

            try
            {
                tx = _kit.getTransaction(_conn);
                tx.begin();
                article = (Article) _conn.getObjectByIdentity(aOid);
                _conn.deletePersistent(article);
                _conn.deletePersistent(article.getProductGroup());
                tx.commit();
            }
            catch (Throwable ex)
            {
                ex.printStackTrace();
                tx.rollback();
            }
        }
        catch (Throwable ex)
        {
            try
            {
                if (tx != null && tx.isInProgress())
                {
                    tx.rollback();
                }
            }
            catch (Exception ex2)
            {
            }
            throw ex;
        }
    }

    public void testOtmIsolation() throws Throwable
    {
        Transaction tx = null;
        Transaction tx2 = null;
        OTMConnection conn2;

        conn2 = _kit.acquireConnection(PersistenceBrokerFactory.getDefaultKey());

        try
        {
            tx = _kit.getTransaction(_conn);
            tx.begin();

            ProductGroup pg = new ProductGroup();
            pg.setId(new Integer(77777));
            pg.setName("1");
            _conn.makePersistent(pg);
            tx.commit();

            Identity pgOid = _conn.getIdentity(pg);

            tx = _kit.getTransaction(_conn);
            tx.begin();
            pg = (ProductGroup) _conn.getObjectByIdentity(pgOid);
            pg.setName("2");

            tx2 = _kit.getTransaction(conn2);
            tx2.begin();
            pg = (ProductGroup) conn2.getObjectByIdentity(pgOid);
            assertEquals("should be equal", "1", pg.getName());
            tx2.commit();
            tx.commit();

            try
            {
                tx = _kit.getTransaction(_conn);
                tx.begin();
                pg = (ProductGroup) _conn.getObjectByIdentity(pgOid);
                _conn.deletePersistent(pg);
                tx.commit();
            }
            catch (Throwable ex)
            {
                ex.printStackTrace();
                tx.rollback();
            }
        }
        catch (Throwable ex)
        {
            try
            {
                if (tx != null && tx.isInProgress())
                {
                    tx.rollback();
                }
            }
            catch (Exception ex2)
            {
            }
            throw ex;
        }
    }

    public void testOtmLocks() throws Throwable
    {
        Transaction tx = null;
        Transaction tx2 = null;
        OTMConnection conn2;
        ProductGroup pg = null;
        ProductGroup pg2 = null;
        Identity pOid = null;
        Identity pOid2 = null;

        conn2 = _kit.acquireConnection(PersistenceBrokerFactory.getDefaultKey());

        try
        {
            tx = _kit.getTransaction(_conn);
            tx.begin();
            pg = new ProductGroup();
            pg.setId(new Integer(77777));
            pg.setName("1");
            pOid = _conn.getIdentity(pg);
            if (_conn.getObjectByIdentity(pOid) == null)
            {
                _conn.makePersistent(pg);
            }
            pg2 = new ProductGroup();
            pg2.setId(new Integer(77778));
            pg2.setName("1");
            pOid2 = _conn.getIdentity(pg2);
            if (_conn.getObjectByIdentity(pOid2) == null)
            {
                _conn.makePersistent(pg2);
            }
            tx.commit();

            final Identity pgOid = _conn.getIdentity(pg);
            final Identity pgOid2 = _conn.getIdentity(pg2);


            final Transaction tx3 = _kit.getTransaction(_conn);
            tx3.begin();
            _conn.getObjectByIdentity(pgOid, LockType.WRITE_LOCK);
            // we can write lock twice from the same tx
            _conn.getObjectByIdentity(pgOid, LockType.WRITE_LOCK);

            // test different LockWaitStrategies
            _kit.setLockWaitStrategy(new NoWaitStrategy());
            failIfLockForWrite(conn2, pgOid);
            _kit.setLockWaitStrategy(new TimeoutStrategy(1));
            failIfLockForWrite(conn2, pgOid);

            // Second test for the TimeoutStrategy:
            // let the second tx to lock
            _kit.setLockWaitStrategy(new TimeoutStrategy(2000));
            tx2 = _kit.getTransaction(conn2);
            tx2.begin();
            (new Thread()
            {
                public void run()
                {
                    try
                    {
                        Thread.sleep(1000);
                        tx3.commit();
                    }
                    catch (InterruptedException ex)
                    {
                    }
                }
            }).start();
            conn2.getObjectByIdentity(pgOid, LockType.WRITE_LOCK);
            tx2.commit();

            // Third test for the TimeoutStrategy:
            // test deadlock detection
            _kit.setLockWaitStrategy(new TimeoutStrategy(4000));
            tx = _kit.getTransaction(_conn);
            tx.begin();
            _conn.getObjectByIdentity(pgOid, LockType.WRITE_LOCK);
            tx2 = _kit.getTransaction(conn2);
            tx2.begin();
            conn2.getObjectByIdentity(pgOid2, LockType.WRITE_LOCK);
            (new Thread()
            {
                public void run()
                {
                    try
                    {
                        _conn.getObjectByIdentity(pgOid2, LockType.WRITE_LOCK);
                    }
                    catch (LockingException ex)
                    {
                        ex.printStackTrace();
                    }
                }
            }).start();

            try
            {
                Thread.sleep(2000);
            }
            catch (InterruptedException ex)
            {
            }

            try
            {
                conn2.getObjectByIdentity(pgOid, LockType.WRITE_LOCK);
                fail("DeadlockException was not thrown");
            }
            catch (DeadlockException ex)
            {
                // ok, deadlock was detected
            }

            tx2.rollback();
            try
            {
                Thread.sleep(2000);
            }
            catch (InterruptedException ex)
            {
            }

            tx.commit();
        }
        catch (Throwable ex)
        {
            try
            {
                if (tx != null && tx.isInProgress())
                {
                    tx.rollback();
                }
            }
            catch (Exception ex2)
            {
            }
            throw ex;
        }
        finally
        {
            try
            {
                tx = _kit.getTransaction(_conn);
                tx.begin();
                if (pOid != null)
                {
                    pg = (ProductGroup) _conn.getObjectByIdentity(pOid);
                    if (pg != null)
                    {
                        _conn.deletePersistent(pg);
                    }
                }
                if (pOid2 != null)
                {
                    pg2 = (ProductGroup) _conn.getObjectByIdentity(pOid2);
                    if (pg2 != null)
                    {
                        _conn.deletePersistent(pg2);
                    }
                }
                tx.commit();
            }
            catch (Throwable ex)
            {
                ex.printStackTrace();
                tx.rollback();
            }
        }
    }

    public void testUpdateByReachability() throws Throwable
    {
        if(ojbSkipKnownIssueProblem("Update by reachabilitiy doesn't work proper"))
        {
            return;
        }
        Transaction tx = null;
        ProductGroup pg;
        Article article;
        Article article2;
        org.apache.ojb.broker.Person person;
        org.apache.ojb.broker.Project project;
        Identity aOid = null;
        Identity aOid2 = null;
        Identity pgOid = null;
        Identity prsOid = null;
        Identity prjOid = null;

        //perform transaction
        try
        {
            tx = _kit.getTransaction(_conn);
            tx.begin();

            pg = new ProductGroup();
            pg.setId(new Integer(77777));
            pgOid = _conn.getIdentity(pg);
            pg.setName("1");
            _conn.makePersistent(pg);
            article = Article.createInstance();
            article.setArticleId(new Integer(77777));
            aOid = _conn.getIdentity(article);
            article.setStock(333);
            pg.add(article);
            article.setProductGroup(pg);
            article2 = Article.createInstance();
            article2.setArticleId(new Integer(77778));
            aOid2 = _conn.getIdentity(article2);
            article2.setStock(334);
            pg.add(article2);
            article2.setProductGroup(pg);
            _conn.makePersistent(article);
            _conn.makePersistent(article2);
            person = new org.apache.ojb.broker.Person(77777, "first", "last");
            prsOid = _conn.getIdentity(person);
            project = new org.apache.ojb.broker.Project(77777, "title", "desc");
            prjOid = _conn.getIdentity(project);
            _conn.makePersistent(person);
            _conn.makePersistent(project);
            tx.commit();


            tx = _kit.getTransaction(_conn);
            tx.begin();
            pg = (ProductGroup) _conn.getObjectByIdentity(pgOid);
            InterfaceArticle articleNew1 = (InterfaceArticle) pg.getAllArticles().get(0);
            InterfaceArticle articleNew2 = (InterfaceArticle) pg.getAllArticles().get(1);
            if (!_conn.getIdentity(articleNew2).equals(aOid2))
            {
                articleNew2 = (InterfaceArticle) pg.getAllArticles().get(0);
                articleNew1 = (InterfaceArticle) pg.getAllArticles().get(1);
                if (!_conn.getIdentity(article2).equals(aOid2))
                {
                    fail("Missing the second article");
                }
            }
            articleNew1.setStock(433);
            articleNew2.setStock(434);
            pg.setName("2");
            tx.commit();

            tx = _kit.getTransaction(_conn);
            tx.begin();
            _conn.invalidateAll();
            articleNew1 = (InterfaceArticle) _conn.getObjectByIdentity(aOid);
            articleNew2 = (InterfaceArticle) _conn.getObjectByIdentity(aOid2);
            assertEquals("should be equal", "2", article.getProductGroup().getName());
            assertEquals("should be equal", 433, article.getStock());
            assertEquals("should be equal", 434, article2.getStock());
            tx.commit();

            // Test M:N relations
            tx = _kit.getTransaction(_conn);
            tx.begin();
            person = (org.apache.ojb.broker.Person) _conn.getObjectByIdentity(prsOid);
            project = (org.apache.ojb.broker.Project) _conn.getObjectByIdentity(prjOid);
            person.getProjects().add(project);
            tx.commit();

            tx = _kit.getTransaction(_conn);
            tx.begin();
            _conn.invalidateAll();
            person = (org.apache.ojb.broker.Person) _conn.getObjectByIdentity(prsOid);
            project = (org.apache.ojb.broker.Project) _conn.getObjectByIdentity(prjOid);
            assertEquals("should be equal", 1, person.getProjects().size());
            tx.commit();
        }
        catch (Throwable ex)
        {
            try
            {
                if (tx != null && tx.isInProgress())
                {
                    tx.rollback();
                }
            }
            catch (Exception ex2)
            {
            }
            throw ex;
        }
        finally
        {
            try
            {
                if (tx == null || !tx.isInProgress())
                {
                    tx = _kit.getTransaction(_conn);
                    tx.begin();
                }

                if (aOid != null)
                {
                    article = (Article) _conn.getObjectByIdentity(aOid);
                    if (article != null)
                    {
                        _conn.deletePersistent(article);
                    }
                }
                if (aOid2 != null)
                {
                    article2 = (Article) _conn.getObjectByIdentity(aOid2);
                    if (article2 != null)
                    {
                        _conn.deletePersistent(article2);
                    }
                }
                if (pgOid != null)
                {
                    pg = (ProductGroup) _conn.getObjectByIdentity(pgOid);
                    if (pg != null)
                    {
                        _conn.deletePersistent(pg);
                    }
                }
                if (prsOid != null)
                {
                    person = (org.apache.ojb.broker.Person) _conn.getObjectByIdentity(prsOid);
                    if (person != null)
                    {
                        _conn.deletePersistent(person);
                    }
                }
                if (prjOid != null)
                {
                    project = (org.apache.ojb.broker.Project) _conn.getObjectByIdentity(prjOid);
                    if (project != null)
                    {
                        _conn.deletePersistent(project);
                    }
                }
                tx.commit();
            }
            catch (Throwable ex)
            {
                ex.printStackTrace();
                tx.rollback();
            }
        }
    }

    public void testSwizzling() throws Throwable
    {
        Transaction tx = null;
        ProductGroup pg;
        Article article;
        Article article2;

        try
        {
            tx = _kit.getTransaction(_conn);
            tx.begin();

            pg = new ProductGroup();
            pg.setId(new Integer(77777));
            _conn.makePersistent(pg);
            article = Article.createInstance();
            article.setArticleId(new Integer(77777));
            article.setStock(333);
            pg.add(article);
            article.setProductGroup(pg);
            _conn.makePersistent(article);
            article2 = Article.createInstance();
            article2.setArticleId(article.getArticleId());
            article2.setStock(334);
            article2.setProductGroup(pg);
            _conn.makePersistent(article2);
            article = (Article) pg.getAllArticles().get(0);
            assertEquals("should be equal", 334, article.getStock());
        }
        finally
        {
            if (tx != null)
            {
                try
                {
                    tx.rollback();
                }
                catch (Throwable ex)
                {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void failIfLockForWrite(OTMConnection conn2, Identity oid)
            throws Exception
    {
        Transaction tx2 = null;

        tx2 = _kit.getTransaction(conn2);
        tx2.begin();
        try {
            conn2.getObjectByIdentity(oid, LockType.WRITE_LOCK);
            fail("LockingException was not thrown");
        } catch (LockingException ex) {
            // ok: we cannot write lock from another tx
            tx2.rollback();
        }
    }

    public static void main(String[] args)
    {
        String[] arr = {CLASS.getName()};
        junit.textui.TestRunner.main(arr);
    }

}
