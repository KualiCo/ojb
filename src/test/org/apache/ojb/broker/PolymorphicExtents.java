package org.apache.ojb.broker;

import java.util.Collection;

import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

/** This TestClass tests OJB facilities to work with polymorphism.
 */
public class PolymorphicExtents extends PBTestCase
{
	public static void main(String[] args)
	{
		String[] arr = {PolymorphicExtents.class.getName()};
		junit.textui.TestRunner.main(arr);
	}

	public PolymorphicExtents(String name)
	{
		super(name);
	}

	protected Article createArticle(String name)
	{
		Article a = new Article();
		a.setArticleName(name);
		a.setIsSelloutArticle(true);
		a.setMinimumStock(100);
		a.setOrderedUnits(17);
		a.setPrice(0.45);
		a.setProductGroupId(new Integer(1));
		a.setStock(234);
		a.setSupplierId(4);
		a.setUnit("bottle");
		ProductGroup tmpPG = new ProductGroup();
		tmpPG.setId(new Integer(1));
		Identity pgID = new Identity(tmpPG, broker);
		ProductGroupProxy pgProxy = new ProductGroupProxy(broker.getPBKey(),pgID);
		a.setProductGroup(pgProxy);
		return a;
	}

	/** TestThreadsNLocks query support for polymorphic extents*/
	public void testCollectionByQuery()
	{
        Criteria crit = new Criteria();
        crit.addEqualTo("articleName", "Hamlet");
        Query q = QueryFactory.newQuery(InterfaceArticle.class, crit);

        Collection result = broker.getCollectionByQuery(q);

        //System.out.println(result);

        assertNotNull("should return at least one item", result);
        assertTrue("should return at least one item", result.size() > 0);
}

	/**
	 * try to retrieve a polymorphic collection attribute
	 * (ProductGroup.allArticlesInGroup contains items
	 * of type TestThreadsNLocks.org.apache.ojb.broker.Article which forms an extent)
	 *  ProductGroup 5 contain items from table Artikel, BOOKS and CDS
	 */
	public void testCollectionRetrieval()
	{
		try
		{
			ProductGroup example = new ProductGroup();
			example.setId(new Integer(5));

			ProductGroup group =
				(ProductGroup) broker.getObjectByQuery(QueryFactory.newQuery(example));

            // 7 Articles, 2 Books, 3 Cds
            assertEquals("check size",group.getAllArticles().size(),12);

		}
		catch (Throwable t)
		{
			fail(t.getMessage());
		}

	}

	/** TestThreadsNLocks EXTENT lookup: a collection with ALL objects in the Article extent*/
	public void testExtentByQuery() throws Exception
    {
        // no criteria signals to omit a WHERE clause
        Criteria selectAll = null;
        Query q = QueryFactory.newQuery(InterfaceArticle.class, selectAll);

        Collection result = broker.getCollectionByQuery(q);

        //System.out.println("OJB proudly presents: The InterfaceArticle Extent\n" + result);

        assertNotNull("should return at least one item", result);
        assertTrue("should return at least one item", result.size() > 0);
	}

	/** TestThreadsNLocks to lookup items from extent classes*/
	public void testRetrieveObjectByIdentity()
	{
        String name = "testRetrieveObjectByIdentity_" + System.currentTimeMillis();
        BookArticle book = new BookArticle();
        book.setArticleName(name);
        CdArticle cd = new CdArticle();
        cd.setArticleName(name);

        broker.beginTransaction();
        broker.store(book);
        broker.store(cd);
        broker.commitTransaction();

        Article example = new Article();
        example.setArticleId(cd.getArticleId());
        // id not present in table ARTICLES but int table CDS
        Identity oid = broker.serviceIdentity().buildIdentity(example);
        InterfaceArticle result = (InterfaceArticle) broker.getObjectByIdentity(oid);
        assertNotNull("should find a CD-article", result);
        assertTrue("should be of type CdArticle", (result instanceof CdArticle));

        example = new Article();
        example.setArticleId(book.getArticleId());
        // id not present in table ARTICLES but int table BOOKS
        oid = broker.serviceIdentity().buildIdentity(example);
        result = (InterfaceArticle) broker.getObjectByIdentity(oid);
        assertNotNull("should find a Book-article", result);
        assertTrue("should be of type BookArticle", (result instanceof BookArticle));
	}

	/**
     * try to load polymorphic references
	 * (OrderPosition.article is of type InterfaceArticle)
	 */
	public void testRetrieveReferences() throws Exception
	{
        for (int i = 1; i < 4; i++)
        {
            OrderPosition tmp = new OrderPosition();
            tmp.setId(i);
            Identity oid = new Identity(tmp, broker);
            OrderPosition pos = (OrderPosition) broker.getObjectByIdentity(oid);
            assertNotNull(pos);
        }
	}
}
