package org.apache.ojb.broker;

import java.util.Iterator;

import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.junit.PBTestCase;

/**
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 */
public class TransactionDemarcationTest extends PBTestCase
{
    private Article[] articleArr;
    private Person[] personArr;
    private static final int COUNT = 20;

    public TransactionDemarcationTest(String s)
    {
        super(s);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        articleArr = new Article[COUNT];
        for (int i = 0; i < COUNT; i++)
        {
            Article a = createArticle(i);
            articleArr[i] = a;
        }

        personArr = new Person[COUNT];
        for (int i = 0; i < COUNT; i++)
        {
            Person a = createPerson(i);
            personArr[i] = a;
        }
    }

    public void testInsertDelete() throws Exception
    {
        doInsert();
        doDelete();
    }

    public void doInsert() throws Exception
    {
        int beforeStore = getArticleCount();
        broker.beginTransaction();
        for (int i=0; i<articleArr.length; i++)
        {
            broker.store(articleArr[i]);
        }
        broker.commitTransaction();
        int afterStore = getArticleCount();
        assertEquals("Wrong number of articles stored", beforeStore+articleArr.length, afterStore);
    }



    public void doDelete() throws Exception
    {
        int beforeDelete = getArticleCount();
        broker.beginTransaction();
        for (int i=0; i<COUNT; i++)
        {
            broker.delete(articleArr[i]);
        }
        broker.commitTransaction();
        int afterDelete = getArticleCount();
        assertEquals("Wrong number of articles deleted", beforeDelete-articleArr.length, afterDelete);
    }

    public void testInsertDifferentObjects() throws Exception
    {
        int beforeStore = getArticleCount();
        int beforeStorePersons = getPersonCount();
        broker.beginTransaction();
        for (int i=0; i<articleArr.length; i++)
        {
            broker.store(articleArr[i]);
            broker.store(personArr[i]);
        }
        broker.commitTransaction();
        int afterStore = getArticleCount();
        int afterStorePersons = getPersonCount();
        assertEquals("Wrong number of articles stored", beforeStore+articleArr.length, afterStore);
        assertEquals("Wrong number of articles stored", beforeStorePersons+personArr.length, afterStorePersons);

        int beforeDelete = getArticleCount();
        int beforeDeletePerson = getPersonCount();
        broker.beginTransaction();
        for (int i=0; i<COUNT; i++)
        {
            broker.delete(personArr[i]);
            broker.delete(articleArr[i]);
        }
        broker.commitTransaction();
        int afterDelete = getArticleCount();
        int afterDeletePerson = getPersonCount();
        assertEquals("Wrong number of articles deleted", beforeDelete-articleArr.length, afterDelete);
        assertEquals("Wrong number of articles deleted", beforeDeletePerson - personArr.length, afterDeletePerson);

    }

    public void testInsertDifferentObjectsWithinTransaction() throws Exception
    {
        int beforeStore = getArticleCount();
        int beforeStorePersons = getPersonCount();

        broker.beginTransaction();
        broker.store(createPerson(99));
        broker.store(createArticle(99));
        broker.commitTransaction();

        int afterStore = getArticleCount();
        int afterStorePersons = getPersonCount();
        assertEquals("Wrong number of articles stored", beforeStore+1, afterStore);
        assertEquals("Wrong number of articles stored", beforeStorePersons+1, afterStorePersons);
    }

    public void testIterator() throws Exception
    {
        Criteria c = new Criteria();
        Query q = new QueryByCriteria(Article.class, c);
        Iterator it = broker.getIteratorByQuery(q);
        it.hasNext();
    }

    public int getArticleCount()
    {
        Criteria c = new Criteria();
        Query q = new QueryByCriteria(Article.class, c);
        int count = 0;
        count = broker.getCount(q);
        return count;
    }

    public int getPersonCount()
    {
        Criteria c = new Criteria();
        Query q = new QueryByCriteria(Person.class, c);
        int count = 0;
        count = broker.getCount(q);
        return count;
    }

    private Article createArticle(int counter)
    {
        Article a = new Article();
        a.setArticleName("New Performance Article " + counter);
        a.setMinimumStock(100);
        a.setOrderedUnits(17);
        a.setPrice(0.45);
        a.setStock(234);
        a.setSupplierId(4);
        a.setUnit("bottle");
        return a;
    }

    private Person createPerson(int counter)
    {
        Person p = new Person();
        p.setFirstname("firstname "+counter);
        p.setLastname("lastname "+counter);
        return p;
    }

    public static void main(String[] args)
    {
        String[] arr = {TransactionDemarcationTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }
}
