package org.apache.ojb.odmg;


import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.shared.Article;
import org.apache.ojb.odmg.shared.ProductGroup;
import org.odmg.DMap;
import org.odmg.Transaction;

/**
 * Tests for OJB {@link org.odmg.DSet} implementation.
 */
public class DMapTest extends ODMGTestCase
{
    private ProductGroup productGroup;

    public static void main(String[] args)
    {
        String[] arr = {DMapTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public DMapTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        Transaction tx = odmg.newTransaction();
        tx.begin();
        productGroup = new ProductGroup();
        productGroup.setName("DMapTest_" + System.currentTimeMillis() + "_");
        database.makePersistent(productGroup);
        tx.commit();
    }

    protected Article createArticle(String name) throws Exception
    {

        Article a = new Article();
        a.setArticleName(productGroup.getName() + name);
        a.setStock(234);
        a.setProductGroup(productGroup);
        return a;
    }

    public void testAdding() throws Exception
    {
        String name = "testAdding";
        String namedObject = "testAdding_" + System.currentTimeMillis();
        TransactionExt tx = (TransactionExt) odmg.newTransaction();

        tx.begin();
        DMap map = odmg.newDMap();

        database.bind(map, namedObject);
        Article key1 = createArticle(name + "_key1");
        Article val1 = createArticle(name + "_val1");
        Article key2 = createArticle(name + "_key2");
        Article val2 = createArticle(name + "_val2");

        map.put(key1, val1);
        map.put(key2, val2);
        tx.commit();


        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();

        DMap mapA = (DMap) database.lookup(namedObject);
        assertNotNull(mapA);
        Article val1A = (Article) mapA.get(key1);
        assertNotNull(val1A);
        assertEquals(val1.getArticleId(), val1A.getArticleId());
        Article val2A = (Article) mapA.get(key2);
        assertNotNull(val2A);
        assertEquals(val2.getArticleId(), val2A.getArticleId());
        tx.commit();
    }

    public void testRemove() throws Exception
    {
        String name = "testAdding";
        String namedObject = "testAdding_" + System.currentTimeMillis();
        TransactionExt tx = (TransactionExt) odmg.newTransaction();

        tx.begin();
        DMap map = odmg.newDMap();

        database.bind(map, namedObject);
        Article key1 = createArticle(name + "_key1");
        Article val1 = createArticle(name + "_val1");
        Article key2 = createArticle(name + "_key2");
        Article val2 = createArticle(name + "_val2");

        map.put(key1, val1);
        map.put(key2, val2);
        tx.commit();


        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();

        DMap mapA = (DMap) database.lookup(namedObject);
        assertNotNull(mapA);
        Article val1A = (Article) mapA.get(key1);
        assertNotNull(val1A);
        assertEquals(val1.getArticleId(), val1A.getArticleId());
        Article val2A = (Article) mapA.get(key2);
        assertNotNull(val2A);
        assertEquals(val2.getArticleId(), val2A.getArticleId());
        tx.commit();

        tx.begin();
        mapA.remove(key1);

        tx.checkpoint();

        mapA = (DMap) database.lookup(namedObject);
        assertNotNull(mapA);
        val1A = (Article) mapA.get(key1);
        assertNull(val1A);
        val2A = (Article) mapA.get(key2);
        assertNotNull(val2A);
        assertEquals(val2.getArticleId(), val2A.getArticleId());
        tx.commit();

        tx.begin();
        mapA.remove(key2);
        mapA.put(key2, val2);

        tx.checkpoint();

        mapA = (DMap) database.lookup(namedObject);
        assertNotNull(mapA);
        val1A = (Article) mapA.get(key1);
        assertNull(val1A);
        val2A = (Article) mapA.get(key2);
        assertNotNull(val2A);
        assertEquals(val2.getArticleId(), val2A.getArticleId());
        tx.commit();

        tx.begin();
        mapA.remove(key2);
        tx.commit();

        tx.begin();
        mapA = (DMap) database.lookup(namedObject);
        assertNotNull(mapA);
        val1A = (Article) mapA.get(key1);
        assertNull(val1A);
        val2A = (Article) mapA.get(key2);
        assertNull(val2A);
        tx.commit();
    }
}
