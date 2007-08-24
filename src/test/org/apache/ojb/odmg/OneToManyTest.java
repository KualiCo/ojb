/*
 * Created by IntelliJ IDEA.
 * User: Matt
 * Date: Jun 10, 2002
 * Time: 9:22:36 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.apache.ojb.odmg;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.Article;
import org.apache.ojb.broker.InterfaceArticle;
import org.apache.ojb.broker.InterfaceProductGroup;
import org.apache.ojb.broker.Mammal;
import org.apache.ojb.broker.ProductGroup;
import org.apache.ojb.broker.Reptile;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.shared.ODMGZoo;
import org.odmg.ODMGException;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

public class OneToManyTest extends ODMGTestCase
{
    private static final int COUNT = 10;
    int oldValue;

    public static void main(String[] args)
    {
        String[] arr = {OneToManyTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public OneToManyTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        ClassDescriptor cld = MetadataManager.getInstance().getRepository().getDescriptorFor(ProductGroup.class);
        CollectionDescriptor cod = cld.getCollectionDescriptorByName("allArticlesInGroup");
        oldValue = cod.getCascadingStore();
        // odmg-api need false
        cod.setCascadeStore(false);
    }

    public void tearDown() throws Exception
    {
        ClassDescriptor cld = MetadataManager.getInstance().getRepository().getDescriptorFor(ProductGroup.class);
        cld.getCollectionDescriptorByName("allArticlesInGroup").setCascadingStore(oldValue);
        super.tearDown();
    }

    /**
     * tests creation of new object that has a one to many relationship
     *
     * @throws Exception
     */
    public void testCreate() throws Exception
    {
        String name = "testCreate_" + System.currentTimeMillis();
        /**
         * 1. create the article.
         */
        Transaction tx = odmg.newTransaction();
        tx.begin();
        ProductGroup group = new ProductGroup();
        group.setGroupName(name);
        tx.lock(group, Transaction.WRITE);
        for (int i = 0; i < COUNT; i++)
        {
            Article article = createArticle(name);
            group.add(article);
        }
        tx.commit();
        /**
         * 2. query on the article to make sure it is everything we set it up to be.
         */
        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select productGroup from " + ProductGroup.class.getName() + " where groupName=$1");
        query.bind(name);
        Collection results = (Collection) query.execute();
        tx.commit();
        Iterator it = results.iterator();
        assertTrue(it.hasNext());
        InterfaceProductGroup pg = (InterfaceProductGroup) it.next();
        assertFalse(it.hasNext());
        assertNotNull(pg.getAllArticles());
        assertEquals(COUNT, pg.getAllArticles().size());
    }

    /**
     * tests creation of new object that has a one to many relationship.
     * thma: this test will not work, because ODMG is no able to track
     * modifictations to normal collections.
     * Only Odmg Collections like DList will be treated properly.
     *
     * @throws Exception
     */
    public void testUpdateWithProxy() throws Exception
    {
        // arminw: fixed
        // if(ojbSkipKnownIssueProblem()) return;
        String name = "testUpdateWithProxy_" + System.currentTimeMillis();

        ProductGroup pg1 = new ProductGroup(null, name + "_1", "a group");
        ProductGroup pg2 = new ProductGroup(null, name + "_2", "a group");
        Article a1 = createArticle(name);
        a1.setProductGroup(pg1);
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(a1);
        database.makePersistent(pg1);
        database.makePersistent(pg2);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        /**
         * 1. get all articles from groups and add a new article
         */
        OQLQuery query = odmg.newOQLQuery();
        query.create("select productGroup from " + ProductGroup.class.getName() + " where groupName like $1");
        query.bind(name + "%");
        Collection results = (Collection) query.execute();
        assertEquals(2, results.size());
        Iterator it = results.iterator();
        InterfaceProductGroup temp = null;
        /**
         * to each productgroup add an article with a pre-determined name.
         */
        while (it.hasNext())
        {
            Article article = createArticle(name);
            temp = (InterfaceProductGroup) it.next();
            tx.lock(temp, Transaction.WRITE);
            temp.add(article);
            article.setProductGroup(temp);
        }
        tx.commit();

        /**
         * 2. requery and find the articles we added.
         */
        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select productGroup from " + ProductGroup.class.getName() + " where groupName like $1");
        query.bind(name + "%");
        results = (Collection) query.execute();
        tx.commit();

        assertEquals(2, results.size());
        it = results.iterator();
        int counter = 0;
        temp = null;
        while (it.hasNext())
        {
            temp = (InterfaceProductGroup) it.next();
            Collection articles = temp.getAllArticles();
            counter = counter + articles.size();
            Iterator it2 = articles.iterator();
            while (it2.hasNext())
            {
                InterfaceArticle art = (InterfaceArticle) it2.next();
                if (art.getArticleName() != null)
                {
                    assertEquals(name, art.getArticleName());
                }
            }
        }
        assertEquals(3, counter);
    }

    /**
     * this tests if polymorph collections (i.e. collections of objects
     * implementing a common interface) are treated correctly
     */
    public void testPolymorphOneToMany()
    {

        ODMGZoo myZoo = new ODMGZoo("London");
        Mammal elephant = new Mammal(37, "Jumbo", 4);
        Mammal cat = new Mammal(11, "Silvester", 4);
        Reptile snake = new Reptile(3, "Kaa", "green");

        myZoo.addAnimal(snake);
        myZoo.addAnimal(elephant);
        myZoo.addAnimal(cat);

        try
        {
            Transaction tx = odmg.newTransaction();
            tx.begin();
            database.makePersistent(myZoo);
            tx.commit();

            int id = myZoo.getZooId();

            tx = odmg.newTransaction();
            tx.begin();
            OQLQuery query = odmg.newOQLQuery();
            query.create("select zoos from " + ODMGZoo.class.getName() +
                    " where zooId=$1");
            query.bind(new Integer(id));
            List zoos = (List) query.execute();
            assertEquals(1, zoos.size());
            ODMGZoo zoo = (ODMGZoo) zoos.get(0);
            tx.commit();
            assertEquals(3, zoo.getAnimals().size());


        }
        catch (ODMGException e)
        {
            e.printStackTrace();
            fail("ODMGException thrown " + e.getMessage());
        }
    }

    /**
     * Create an article with 4 product groups related to it in a 1-N relationship
     */
    protected Article createArticle(String name)
    {
        Article a = Article.createInstance();
        a.setArticleName(name);
        a.setIsSelloutArticle(true);
        a.setMinimumStock(100);
        a.setOrderedUnits(17);
        a.setPrice(0.45);
        //a.setProductGroupId(1);
        a.setStock(234);
        a.setSupplierId(4);
        a.setUnit("bottle");
        return a;
    }
}
