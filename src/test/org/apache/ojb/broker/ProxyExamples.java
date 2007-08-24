package org.apache.ojb.broker;


import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

/**
 * Demo Application that shows basic concepts for Applications using the PersistenceBroker
 * as a mediator for persistence
 */
public class ProxyExamples extends PBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {ProxyExamples.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public ProxyExamples(String name)
    {
        super(name);
    }

    /**
     * This example shows how the PersistenceBroker can be used with a highly configurable proxy concept.
     * The main idea is, not to return materialized objects but rather lazy proxies, that defer materialization
     * until it is definitely neccesary (e.g. reading an Objects attribute).
     * <p/>
     * To achieve such a behaviour, you can define proxies for each persistent class.
     * As an example see the Repository.xml file in this examples directory.
     * <p/>
     * It is not always the best option to use lazy materialization. The usage of proxies can be completely configured
     * in the xml repository. That is, if you decide not to use proxies, you don't have to change program-code,
     * but only out-comment the corresponding entry in the repos
     * itory.
     */
    public void testProgrammedProxies() throws Exception
    {
        String name = "testDynamicProxies_" + System.currentTimeMillis();
        Vector myArticles = new Vector();
// In the following code we will generate 10 Proxy-objects.
        ProductGroup pg = new ProductGroup();
        pg.setGroupName(name);
        broker.beginTransaction();
        broker.store(pg);
        broker.commitTransaction();

        for(int i = 1; i < 10; i++)
        {
            Article a = new Article();
            a.setArticleName(name);
            a.setProductGroup(pg);
            broker.beginTransaction();
            broker.store(a);
            broker.commitTransaction();
            Identity id = broker.serviceIdentity().buildIdentity(a);
            InterfaceArticle A =
                    (InterfaceArticle) ((PersistenceBrokerInternal)broker).createProxy(Article.class, id);
            myArticles.add(A);
//System.out.println(A);
        }
// In the following code we call methods that reference the real subjects attributes.
// To access an articles name as in getArticleName(), the proxy object has to materialze the real subjects from db.
// but note: the references to an Articles productgroup are not materialized immediately,
// but contain proxy objects, representing ProductGroups.
        for(int i = 0; i < 9; i++)
        {
            InterfaceArticle a = (InterfaceArticle) myArticles.get(i);
//System.out.println("Article[" + a.getArticleId() + "] : " + a.getArticleName());
            assertNotNull(a);
        }
// In the following code we will access the real ProductGroup objects.
// thus the Proxies have to materialize them.
        for(int i = 0; i < 9; i++)
        {
            InterfaceArticle a = (InterfaceArticle) myArticles.get(i);
            assertNotNull(a.getProductGroup());
            assertNotNull(a.getProductGroup().getName());

//System.out.println("Article[" + a.getArticleId() + "] is in group " + a.getProductGroup().getName());
        }
// in the following code we will touch fields of the ProductGroup references.
// Now proxies in the AllArticlesInGroup collection need to be materialized
//System.out.println("now playing with product group no. 2");
        Object[] pkvals = new Object[1];
        pkvals[0] = new Integer(2);
        Identity id = new Identity(ProductGroup.class, ProductGroup.class, pkvals);
        InterfaceProductGroup group2 = null;
        try
        {
            group2 = (InterfaceProductGroup) ((PersistenceBrokerInternal)broker).createProxy(ProductGroupProxy.class, id);
        }
        catch(Exception ignored)
        {
        }
//System.out.println(group2.toString());
        broker.beginTransaction();
        for(int i = 0; i < group2.getAllArticles().size(); i++)
        {
            InterfaceArticle a = (InterfaceArticle) group2.getAllArticles().get(i);
//System.out.println(a.getArticleName());
            assertNotNull(a);
            broker.store(a);
        }
        broker.store(group2);
        broker.commitTransaction();
    }

//    private Class getDynamicProxyClass(Class clazz)
//    {
//        try
//        {
//            Class[] interfaces = clazz.getInterfaces();
//            Class proxyClass = Proxy.getProxyClass(clazz.getClassLoader(), interfaces);
//            return proxyClass;
//        }
//        catch(Throwable t)
//        {
//            System.out.println("OJB Warning: can not use dynamic proxy for class " + clazz.getName() + ": " + t.getMessage());
//            return null;
//        }
//
//    }

    /**
     * This example shows how the PersistenceBroker can be used with a highly configurable proxy concept.
     * The main idea is, not to return materialized objects but rather lazy proxies, that defer materialization
     * until it is definitely neccesary (e.g. reading an Objects attribute).
     * <p/>
     * To achieve such a behaviour, you can define proxies for each persistent class.
     * As an example see the Repository.xml file in this examples directory.
     * <p/>
     * It is not always the best option to use lazy materialization. The usage of proxies can be completely configured
     * in the xml repository. That is, if you decide not to use proxies, you don't have to change program-code,
     * but only out-comment the corresponding entry in the repos
     * itory.
     */
    public void testDynamicProxies()
    {
        String name = "testDynamicProxies_" + System.currentTimeMillis();
        Vector myArticles = new Vector();
// In the following code we will generate 10 Proxy-objects.
        ProductGroup pg = new ProductGroup();
        pg.setGroupName(name);
        broker.beginTransaction();
        broker.store(pg);
        broker.commitTransaction();

        for(int i = 1; i < 10; i++)
        {
            Article a = new Article();
            a.setArticleName(name);
            a.setProductGroup(pg);
            broker.beginTransaction();
            broker.store(a);
            broker.commitTransaction();
            Identity id = broker.serviceIdentity().buildIdentity(a);
            InterfaceArticle A =
                    (InterfaceArticle) ((PersistenceBrokerInternal)broker).createProxy(Article.class, id);
            myArticles.add(A);
//System.out.println(A);
        }
// In the following code we call methods that reference the real subjects attributes.
// To access an articles name as in getArticleName(), the proxy object has to materialze the real subjects from db.
// but note: the references to an Articles productgroup are not materialized immediately,
// but contain proxy objects, representing ProductGroups.
        for(int i = 0; i < 9; i++)
        {
            InterfaceArticle a = (InterfaceArticle) myArticles.get(i);
//System.out.println("Article[" + a.getArticleId() + "] : " + a.getArticleName());
        }
// In the following code we will access the real ProductGroup objects.
// thus the Proxies have to materialize them.
        for(int i = 0; i < 9; i++)
        {
            InterfaceArticle a = (InterfaceArticle) myArticles.get(i);
//System.out.println("Article[" + a.getArticleId() + "] is in group " + a.getProductGroup().getName());
        }
    }

    public void testCollectionProxies() throws Exception
    {
        ProductGroupWithCollectionProxy org_pg = new ProductGroupWithCollectionProxy();
        org_pg.setId(new Integer(7));
        Identity pgOID = broker.serviceIdentity().buildIdentity(org_pg);

        ProductGroupWithCollectionProxy pg = (ProductGroupWithCollectionProxy) broker.getObjectByIdentity(pgOID);
        assertEquals(org_pg.getId(), pg.getId());

        Collection col = pg.getAllArticles();
        int countedSize = col.size(); // force count query
        Iterator iter = col.iterator();
        while(iter.hasNext())
        {
            InterfaceArticle a = (InterfaceArticle) iter.next();
        }

        assertEquals("compare counted and loaded size", countedSize, col.size());
    }

    public void testCollectionProxiesAndExtents() throws Exception
    {
        ProductGroupWithCollectionProxy pg = new ProductGroupWithCollectionProxy();
        pg.setId(new Integer(5));
        Identity pgOID = broker.serviceIdentity().buildIdentity(pg);

        pg = (ProductGroupWithCollectionProxy) broker.getObjectByIdentity(pgOID);
        assertEquals(5, pg.getId().intValue());

        Collection col = pg.getAllArticles();
        int countedSize = col.size(); // force count query
        Iterator iter = col.iterator();
        while(iter.hasNext())
        {
            InterfaceArticle a = (InterfaceArticle) iter.next();
        }

        assertEquals("compare counted and loaded size", countedSize, col.size());

        // 7 Articles, 2 Books, 3 Cds
        assertEquals("check size", col.size(), 12);
    }

    public void testReferenceProxies()
    {
        ArticleWithReferenceProxy a = new ArticleWithReferenceProxy();
//		a.setArticleId(8888);
        a.setArticleName("ProxyExamples.testReferenceProxy article");

        Query q = QueryFactory.newQuery(a);

        ProductGroup pg = new ProductGroup();
//		pg.setId(10);
        pg.setGroupName("ProxyExamples test group");

        a.setProductGroup(pg);
        broker.beginTransaction();
        broker.store(a);
        broker.commitTransaction();
        int id = pg.getGroupId().intValue();

        broker.clearCache();
        ArticleWithReferenceProxy ar = (ArticleWithReferenceProxy) broker.getObjectByQuery(q);

        assertEquals(id, ar.getProductGroup().getId().intValue());
    }

    /**
     * Default the transaction isolation level of a JDBC connection is
     * READ-COMMITED.
     * So if a proxy uses another broker instance (i.e. JDBC connecction)
     * than the current one, it's possible that program blocks.
     */
    public void testProxiesAndJDBCTransactionIsolation()
    {
        boolean commit = false;
        try
        {
            // Start transaction
            broker.beginTransaction();

            // Create productgroup
            ProductGroupWithCollectionProxy pg = new ProductGroupWithCollectionProxy();
            pg.setGroupName("TESTPRODUCTGROUP");
            broker.store(pg);

            // Create 2 articles for this productgroup
            for(int j = 1; j <= 2; j++)
            {
                Article ar = new Article();
                ar.setArticleName("ARTICLE " + j);
                ar.setProductGroup(pg);
                broker.store(ar);
            }

            // Reload the productgroup
            broker.clearCache();
            pg = (ProductGroupWithCollectionProxy) broker.getObjectByQuery(QueryFactory.newQuery(pg));
            assertTrue(pg != null);

            // Try to load the articles
            // The proxy is using another broker instance (i.e. JDBC cconnection).
            // Default the JDBC transaction isolationlevel is READ_COMMITTED.
            // So the program will wait until the inserted articles are committed.
            Collection articles = pg.getAllArticlesInGroup();
            assertEquals(2, articles.size());

            // Commit
            broker.commitTransaction();
            commit = true;
        }
        finally
        {
            if(!commit)
                broker.abortTransaction();
        }
    }

}
