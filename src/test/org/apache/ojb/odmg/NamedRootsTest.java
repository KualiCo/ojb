package org.apache.ojb.odmg;


import java.util.ArrayList;
import java.util.List;

import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.shared.Article;
import org.apache.ojb.odmg.shared.ProductGroup;
import org.odmg.DList;
import org.odmg.ObjectNameNotFoundException;
import org.odmg.ObjectNameNotUniqueException;
import org.odmg.Transaction;

/**
 * Demo Application that shows basic concepts for Applications using the OJB ODMG
 * implementation as an transactional object server.
 *
 * @version $Id: NamedRootsTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class NamedRootsTest extends ODMGTestCase
{
    private ProductGroup testProductGroup;

    public static void main(String[] args)
    {
        String[] arr = {NamedRootsTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public NamedRootsTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        Transaction tx = odmg.newTransaction();
        tx.begin();
        testProductGroup = new ProductGroup();
        testProductGroup.setGroupName("NamedRootsTest_" + System.currentTimeMillis());
        database.makePersistent(testProductGroup);
        tx.commit();
    }

    private Article createArticle()
    {
        Article example = new Article();
        example.setArticleName(testProductGroup.getName());
        example.setProductGroupId(testProductGroup.getId());
        return example;
    }

    public void testBindPersistentCapableObjectCollection() throws Exception
    {
        String bindingName = "testBindPersistentCapableObjectCollection_" + System.currentTimeMillis();

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        //bind object to name
        tx.begin();

        // get new DList instance
        DList dlist = odmg.newDList();
        Article a1 = createArticle();
        Article a2 = createArticle();
        Article a3 = createArticle();
        dlist.add(a1);
        dlist.add(a2);
        dlist.add(a3);
        database.bind(dlist, bindingName);
        // lookup the named object - DList
        List value = (List) database.lookup(bindingName);
        assertNotNull("Could not lookup object for binding name: "+bindingName, value);
        tx.commit();

        try
        {
            tx.begin();
            database.bind(dlist, bindingName);
            tx.commit();
            fail("We expected a ObjectNameNotUniqueException, but was not thrown");
        }
        catch (ObjectNameNotUniqueException ex)
        {
            // we wait for this exception
            assertTrue(true);
            tx.abort();
        }

        try
        {
            tx.begin();
            tx.getBroker().clearCache();
            database.bind(dlist, bindingName);
            tx.commit();
            fail("We expected a ObjectNameNotUniqueException, but was not thrown");
        }
        catch (ObjectNameNotUniqueException ex)
        {
            // we wait for this exception
            assertTrue(true);
            tx.abort();
        }

        tx.begin();
        List result = (List) database.lookup(bindingName);
        assertNotNull(result);
        assertEquals(3, result.size());
        Article newA1 = (Article) result.get(0);
        assertNotNull(newA1);
        assertEquals(a1.getArticleName(), newA1.getArticleName());
        tx.commit();

        tx.begin();
        // we want to completely remove the named object
        // the persisted DList with all DList entries,
        // but the Article objects itself shouldn't be deleted:
        // 1. mandatory, clear the list to remove all entries
        result.clear();
        // 2. unbind named object
        database.unbind(bindingName);

        // alternative can be used
        //tx.setCascadingDelete(DListImpl.class, true);
        //database.unbind(bindingName);
        tx.commit();

        tx.begin();
        try
        {
            database.lookup(bindingName);
        }
        catch(ObjectNameNotFoundException e)
        {
            // expected exception
            assertTrue(true);
        }
        tx.commit();
    }

    public void testBindPersistentCapableObject() throws Exception
    {
        String bindingName = "testBindPersistentCapableObject_" + System.currentTimeMillis();
        TransactionImpl tx = (TransactionImpl) odmg.newTransaction();
        //bind object to name
        tx.begin();
        Article example = createArticle();
        database.bind(example, bindingName);
        Article value = (Article) database.lookup(bindingName);
        assertTrue("Could not lookup object for binding name: "+bindingName, value != null);
        tx.commit();

        try
        {
            tx.begin();
            database.bind(example, bindingName);
            tx.commit();
            fail("We expected a ObjectNameNotUniqueException, but was not thrown");
        }
        catch (ObjectNameNotUniqueException ex)
        {
            // we wait for this exception
            assertTrue(true);
            tx.abort();
        }

        tx.begin();
        // this only remove the named object link, the Article object
        // itself will not be touched
        database.unbind(bindingName);
        tx.commit();
    }

    public void testBindPersistentSerialzableObject() throws Exception
    {
        String bindingName = "testBindPersistentSerialzableObject_" + System.currentTimeMillis();
        TransactionImpl tx = (TransactionImpl) odmg.newTransaction();
        //bind object to name
        tx.begin();
        List example = new ArrayList();
        example.add("Merkur");
        example.add("Venus");
        database.bind(example, bindingName);
        List value = (List) database.lookup(bindingName);
        assertNotNull("Could not lookup object for binding name: "+bindingName, value);
        assertEquals(2, value.size());
        tx.commit();

        try
        {
            tx.begin();
            database.bind(example, bindingName);
            tx.commit();
            fail("We expected a ObjectNameNotUniqueException, but was not thrown");
        }
        catch (ObjectNameNotUniqueException ex)
        {
            // we wait for this exception
            assertTrue(true);
            tx.abort();
        }

        tx.begin();
        database.unbind(bindingName);
        tx.commit();

        tx.begin();
        example.add("earth");
        example.add("mars");
        database.bind(example, bindingName);
        value = (List) database.lookup(bindingName);
        assertNotNull("Could not lookup object for binding name: "+bindingName, value);
        assertEquals(4, value.size());
        tx.commit();

        tx.begin();
        database.unbind(bindingName);
        tx.commit();
    }

    public void testDoubleBindInOneTx() throws Exception
    {
        String bindingName = "testDoubleBindInOneTx_" + System.currentTimeMillis();

        Article article = createArticle();
        Article foundArticle = null;

        Transaction tx = odmg.newTransaction();
        tx.begin();
        database.bind(article, bindingName);

        foundArticle = (Article) database.lookup(bindingName);
        assertNotNull(foundArticle);

        foundArticle = null;
        database.unbind(bindingName);
        try
        {
            foundArticle = (Article) database.lookup(bindingName);
            fail("Found unbound DList");
        }
        catch (ObjectNameNotFoundException ex)
        {
            // expected exception
            assertTrue(true);
        }

        database.bind(article, bindingName);
        foundArticle = (Article) database.lookup(bindingName);

        foundArticle = null;
        tx.commit();

        tx = odmg.newTransaction();
        tx.begin();
        foundArticle = (Article) database.lookup(bindingName);
        assertNotNull(foundArticle);
        database.unbind(bindingName);
        tx.commit();
    }


    public void testLookup() throws Exception
    {
        String bindingName = "testLookup_" + System.currentTimeMillis();
        // clear named roots.
        TransactionImpl tx = (TransactionImpl) odmg.newTransaction();
        //bind object to name
        tx.begin();
        Article example = createArticle();
        database.makePersistent(example);
        tx.commit();

        tx.begin();
        database.bind(example, bindingName);
        tx.commit();

        // TestThreadsNLocks look up
        Article lookedUp1 = null;
        tx = (TransactionImpl) odmg.newTransaction();
        tx.begin();
        // lookup by name binding
        lookedUp1 = (Article) database.lookup(bindingName);
        tx.commit();

        // looking up object by OID should return same Object as by name
        assertEquals("lookups should return identical object", example, lookedUp1);

        tx.begin();
        database.unbind(bindingName);
        tx.commit();
    }

    public void testUnBind() throws Exception
    {
        String name = "testUnBind_" + System.currentTimeMillis();

        Transaction tx = odmg.newTransaction();
        //bind object to name
        tx.begin();
        Article example = createArticle();
        database.makePersistent(example);
        tx.commit();

        // 1. perform binding
        tx.begin();
        try
        {
            database.bind(example, name);
            tx.commit();
        }
        catch (ObjectNameNotUniqueException ex)
        {
            tx.abort();
            fail(ex.getMessage());
        }

        // 2. perform unbind
        tx = odmg.newTransaction();
        tx.begin();
        try
        {
            database.unbind(name);
            tx.commit();
        }
        catch (ObjectNameNotFoundException ex)
        {
            tx.abort();
            fail("name " + name + "should be known");
        }

        // 3. check if name is really unknown now
        tx = odmg.newTransaction();
        tx.begin();
        try
        {
            Article value = (Article) database.lookup(name);
            assertNotNull("Should not find unbind name '" + name+"'", value);
            fail("name " + name + " should not be known after unbind");
        }
        catch (ObjectNameNotFoundException ex)
        {
            // OK, expected
            assertTrue(true);
        }
        tx.abort();
    }
}
