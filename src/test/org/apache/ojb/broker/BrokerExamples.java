package org.apache.ojb.broker;

import java.util.List;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

/**
 * Demo Application that shows basic concepts for Applications using the PersistenceBroker
 * as a mediator for persistence
 */
public class BrokerExamples extends PBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {BrokerExamples.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public BrokerExamples(String name)
    {
        super(name);
    }

    Article createArticle(String name)
    {
        Article a = new Article();
        a.setArticleName(name);
        a.setIsSelloutArticle(true);
        a.setMinimumStock(100);
        a.setOrderedUnits(17);
        a.setPrice(0.45);
        a.setStock(234);
        a.setSupplierId(4);
        a.setUnit("bottle");
        return a;
    }

    ProductGroup createProductGroup(String name)
    {
        ProductGroup tmpPG = new ProductGroup();
        tmpPG.setGroupName(name);
        return tmpPG;
    }

    public void testCollectionRetrieval() throws Exception
    {
        // Use PersistenceBroker to lookup persistent objects.
        // Loop through categories with id 1 to 9
        // A ProductGroup holds a List of all Article Objects in the specific category
        // the repository.xml specifies that the List of Artikels has to be
        // materialized immediately.
        for (int i = 1; i < 9; i++)
        {
            ProductGroup example = new ProductGroup();
            example.setId(new Integer(i));

            ProductGroup group =
                    (ProductGroup) broker.getObjectByQuery(QueryFactory.newQuery(example));
            assertNotNull("Expect a ProductGroup with id " + i, group);
            assertEquals("should be equal", i, group.getId().intValue());
            List articleList = group.getAllArticles();
            for(int j = 0; j < articleList.size(); j++)
            {
                Object o =  articleList.get(j);
                assertNotNull(o);
            }
        }
    }

    public void testModifications() throws Exception
    {
        String name = "testModifications_" + System.currentTimeMillis();

        //create a new Article and play with it
        Article article = createArticle(name);

        Identity oid = null;
        broker.beginTransaction();
        for (int i = 1; i < 50; i++)
        {
            article.addToStock(10);
            broker.store(article);
            broker.delete(article);
            broker.store(article);
            if(i == 1)
            {
                // lookup identity
                oid = broker.serviceIdentity().buildIdentity(article);
            }
        }
        broker.commitTransaction();

        Article result = (Article) broker.getObjectByIdentity(oid);
        assertNotNull(result);
        assertEquals(article.getArticleName(), result.getArticleName());
    }

    public void testShallowAndDeepRetrieval() throws Exception
    {
        String name = "testShallowAndDeepRetrieval_" + System.currentTimeMillis();

        ObjectReferenceDescriptor ord = null;

        try
        {
            // prepare test, create article with ProductGroup
            Article tmpArticle = createArticle(name);
            ProductGroup pg = createProductGroup(name);
            tmpArticle.setProductGroup(pg);
            pg.add(tmpArticle);

            broker.beginTransaction();
            // in repository Article 1:1 refererence to PG hasn't enabled auto-update,
            // so first store the PG. PG has enabled auto-update and will store the
            // article automatic
            broker.store(pg);
            broker.commitTransaction();
            // after insert we can build the Article identity
            Identity tmpOID = broker.serviceIdentity().buildIdentity(tmpArticle);
            broker.clearCache();

            // switch to shallow retrieval
            ClassDescriptor cld = broker.getClassDescriptor(Article.class);
            ord = cld.getObjectReferenceDescriptorByName("productGroup");
            ord.setCascadeRetrieve(false);

            Article article = (Article) broker.getObjectByIdentity(tmpOID);
            assertNull("now reference should be null", article.getProductGroup());

            // now switch to deep retrieval
            ord.setCascadeRetrieve(true);
            // should work without setting cld
            // broker.setClassDescriptor(cld);
            broker.clearCache();
            article = (Article) broker.getObjectByIdentity(tmpOID);
            assertNotNull("now reference should NOT be null", article.getProductGroup());
        }
        finally
        {
            // restore old value
            if(ord != null) ord.setCascadeRetrieve(true);
        }
    }


    /**
     * tests the PB.retrieveReference() feature
     */
    public void testRetrieveReference() throws Exception
    {
        String name = "testRetrieveReference_" + System.currentTimeMillis();

        // ensure there is an item to find
        Article tmpArticle = createArticle(name);
        ProductGroup pg = createProductGroup(name);
        tmpArticle.setProductGroup(pg);
        broker.beginTransaction();
        broker.store(pg);
        broker.store(tmpArticle);
        broker.commitTransaction();
        Identity tmpOID = broker.serviceIdentity().buildIdentity(tmpArticle);
        broker.clearCache();

        ObjectReferenceDescriptor ord = null;
        try
        {
            // switch to shallow retrieval
            ClassDescriptor cld = broker.getClassDescriptor(Article.class);
            // article only has one ord
            ord = cld.getObjectReferenceDescriptorByName("productGroup");
            ord.setCascadeRetrieve(false);

            Article article = (Article) broker.getObjectByIdentity(tmpOID);
            assertNull("now reference should be null", article.getProductGroup());

            // now force loading:
            broker.retrieveReference(article, "productGroup");
            assertNotNull("now reference should NOT be null", article.getProductGroup());

            // repair cld
            ord.setCascadeRetrieve(true);
            // should work without setting cld
            // broker.setClassDescriptor(cld);
        }
        finally
        {
            // restore old value
            if(ord != null) ord.setCascadeRetrieve(true);
        }
    }

    /**
     * tests the PB.retrieveAllReferences() feature
     */
    public void testRetrieveAllReferences()
    {
        String name = "testRetrieveAllReferences_" + System.currentTimeMillis();

        // ensure there is an item to find
        Article tmpArticle = createArticle(name);
        ProductGroup pg = createProductGroup(name);
        tmpArticle.setProductGroup(pg);

        broker.beginTransaction();
        broker.store(pg);
        broker.store(tmpArticle);
        broker.commitTransaction();
        Identity tmpOID = broker.serviceIdentity().buildIdentity(tmpArticle);
        broker.clearCache();
        ObjectReferenceDescriptor ord = null;
        try
        {
            // switch to shallow retrieval
            ClassDescriptor cld = broker.getClassDescriptor(Article.class);
            ord = (ObjectReferenceDescriptor) cld.getObjectReferenceDescriptors().get(0);
            ord.setCascadeRetrieve(false);

            Article article = (Article) broker.getObjectByIdentity(tmpOID);
            assertNull("now reference should be null", article.getProductGroup());

            // now force loading:
            broker.retrieveAllReferences(article);
            assertNotNull("now reference should NOT be null", article.getProductGroup());

            // clean up cld
            ord.setCascadeRetrieve(true);
        }
        finally
        {
            // restore old value
            if(ord != null) ord.setCascadeRetrieve(true);
        }

    }
}
