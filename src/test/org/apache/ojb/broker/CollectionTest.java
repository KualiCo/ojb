package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.util.ObjectModification;
import org.apache.ojb.broker.util.collections.RemovalAwareCollection;
import org.apache.ojb.junit.PBTestCase;

/**
 * Test case for collection handling.
 *
 * Main class Gatherer have five collections of type CollectibleBase,
 * CollectibleB, CollectibleC, CollectibleD, CollectibleDD
 *
 * Class hierarchy:
 * [CollectibleBaseIF <--] CollectibleBase <-- CollectibleB <-- CollectibleC <-- CollectibleCC
 *                              |                                 |
 *                         CollectibleD <-- CollectibleDD       CollectibleCCC
 *
 * in repository interface CollectibleBaseIF was declared with five
 * extents (CollectibleBase, CollectibleB, CollectibleC, CollectibleD, CollectibleDD)
 *
 * CollectibleBase
 * auto-retrieve, auto-update, auto-delete all true
 * proxy false
 *
 * CollectibleB
 * auto-retrieve, auto-update set true, auto-delete false
 * proxy true
 *
 * CollectibleC
 * auto-retrieve, auto-update, auto-delete set true,
 * proxy true
 * CollectibleC has a reference back to the Gatherer object
 * (auto-retrieve, auto-update, auto-delete set false to avoid circular
 * object creation)
 *
 * CollectibleCC
 * auto-retrieve true, auto-update true, auto-delete set true,
 * proxy false
 *
 * CollectibleCCC
 * auto-retrieve false, auto-update true, auto-delete set true,
 * proxy false
 *
 * CollectibleD
 * auto-retrieve, auto-update, auto-delete all true
 * proxy false
 *
 * CollectibleDD
 * auto-retrieve, auto-update, auto-delete all true
 * proxy true
 *
 * TODO: Need some refactoring and more structured tests of different auto_xyz/proxy settings
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: CollectionTest.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class CollectionTest extends PBTestCase
{
    static final int NONE = ObjectReferenceDescriptor.CASCADE_NONE;
    static final int LINK = ObjectReferenceDescriptor.CASCADE_LINK;
    static final int OBJECT = ObjectReferenceDescriptor.CASCADE_OBJECT;

    public static void main(String[] args)
    {
        String[] arr = {CollectionTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**
     * test for OJB-82
     */
    public void testUsingExtentWhichIsNotInheritedFromBaseClass() throws Exception
    {
        // TODO: fix this bug
        if(ojbSkipKnownIssueProblem("Test for OJB-82 will be fixed in next version")) return;

        String prefix = "testUsingExtentWhichIsNotInheritedFromBaseClass_" + System.currentTimeMillis();

        ojbChangeReferenceSetting(BookShelf.class, "items", true, OBJECT, OBJECT, false);
        ojbChangeReferenceSetting(DVD.class, "shelf", true, NONE, NONE, false);
        ojbChangeReferenceSetting(Book.class, "shelf", true, NONE, NONE, false);
        ojbChangeReferenceSetting(Candie.class, "shelf", true, NONE, NONE, false);

        BookShelf bookShelf = new BookShelf(prefix);
        BookShelfItem dvd = new DVD(prefix+ "_dvd", bookShelf);
        BookShelfItem book = new Book(prefix + "_book", bookShelf);
        Candie candie = new Candie(prefix + "_book", bookShelf);
        List items = new ArrayList();
        items.add(dvd);
        items.add(book);
        items.add(candie);
        bookShelf.setItems(items);

        broker.beginTransaction();
        broker.store(bookShelf);
        broker.commitTransaction();

        broker.clearCache();
        BookShelf loadedCopy = (BookShelf) broker.getObjectByIdentity(
                broker.serviceIdentity().buildIdentity(BookShelf.class, bookShelf.getPk()));
        assertNotNull(loadedCopy);
        assertNotNull(loadedCopy.getItems());
        assertEquals(3, loadedCopy.getItems().size());

        broker.beginTransaction();
        broker.clearCache();
        Criteria criteria = new Criteria();
        criteria.addLike("name", prefix + "%");
        Query q = new QueryByCriteria(BookShelfItem.class, criteria);
        Collection result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    public void testMoveProxyCollectionFromOneToAnother() throws Exception
    {
        String prefix = "testMoveProxyCollectionFromOneToAnother_" + System.currentTimeMillis();

        ojbChangeReferenceSetting(BookShelf.class, "items", true, OBJECT, OBJECT, true);
        ojbChangeReferenceSetting(DVD.class, "shelf", true, NONE, NONE, true);
        ojbChangeReferenceSetting(Book.class, "shelf", true, NONE, NONE, true);

        BookShelf bookShelf = new BookShelf(prefix);
        BookShelf bookShelfSecond = new BookShelf(prefix+"_second");
        BookShelfItem ev1 = new DVD(prefix+ "_dvd", bookShelf);
        BookShelfItem ev2 = new Book(prefix + "_book", bookShelf);
        bookShelf.addItem(ev1);
        bookShelf.addItem(ev2);

        broker.beginTransaction();
        broker.store(bookShelfSecond);
        broker.store(bookShelf);
        broker.commitTransaction();

        broker.clearCache();
        BookShelf loadedCopy = (BookShelf) broker.getObjectByIdentity(
                broker.serviceIdentity().buildIdentity(BookShelf.class, bookShelf.getPk()));
        assertNotNull(loadedCopy);
        assertNotNull(loadedCopy.getItems());
        assertEquals(2, loadedCopy.getItems().size());

        broker.beginTransaction();
        /*
        now we move the unmaterialzed proxy collection from one to another object,
        it's important to first store the bookshelf object with the nullified items
        and then the bookshelf with moved item collection proxy - otherwise the PB-api
        doesn't recognize the changes
        */
        bookShelfSecond.setItems(bookShelf.getItems());
        bookShelf.setItems(null);
        broker.store(bookShelf, ObjectModification.UPDATE);
        broker.store(bookShelfSecond, ObjectModification.UPDATE);
        broker.commitTransaction();
        broker.clearCache();

        loadedCopy = (BookShelf) broker.getObjectByIdentity(
                broker.serviceIdentity().buildIdentity(BookShelf.class, bookShelf.getPk()));
        assertNotNull(loadedCopy);
        assertNotNull(loadedCopy.getItems());
        assertEquals(0, loadedCopy.getItems().size());

        BookShelf loadedCopySecond = (BookShelf) broker.getObjectByIdentity(
                broker.serviceIdentity().buildIdentity(BookShelf.class, bookShelfSecond.getPk()));
        assertNotNull(loadedCopySecond);
        assertNotNull(loadedCopySecond.getItems());
        assertEquals(2, loadedCopySecond.getItems().size());

        broker.clearCache();
        Criteria criteria = new Criteria();
        criteria.addLike("name", prefix + "%");
        Query q = new QueryByCriteria(BookShelfItem.class, criteria);
        Collection items = broker.getCollectionByQuery(q);
        assertNotNull(items);
        assertEquals(2, items.size());
        // we are using collection proxies, so we have to use the interface
        BookShelfItem item = (BookShelfItem) items.iterator().next();
        assertNotNull(item.getShelf());
        BookShelfIF bs = item.getShelf();
        assertEquals(bookShelfSecond.getPk(), bs.getPk());
    }

    public void testReadProxyCollection() throws Exception
    {
        String name = "testReadProxyCollection_"+System.currentTimeMillis();
        Gatherer gat = new Gatherer(null, name);
        CollectibleB[] cols = prepareCollectibleB(name);

        gat.setCollectiblesB(Arrays.asList(cols));
        broker.beginTransaction();
        broker.store(gat);
        broker.commitTransaction();

        broker.clearCache();
        Criteria crit = new Criteria();
        crit.addLike("name", name);
        Query q = QueryFactory.newQuery(Gatherer.class, crit);
        Gatherer newGat = (Gatherer)broker.getObjectByQuery(q);

        Iterator it = newGat.getCollectiblesB().iterator();
        int i = 0;
        while(it.hasNext())
        {
            CollectibleB colB = (CollectibleB) it.next();
            assertTrue(colB.getName().indexOf(name) > -1);
            i++;
        }
        assertEquals(4, i);
    }

    public void testStoreReadOfUserDefinedCollectionClass()
    {
        String name = "testStoreReadOfUserDefinedCollectionClass_"+System.currentTimeMillis();
        Gatherer gat = new Gatherer(null, name);

        CollectibleBase[] collBase = prepareCollectibleBase(name);
        CollectionClassDummy dummyList = new CollectionClassDummy();
        for (int i = 0; i < collBase.length; i++)
        {
            CollectibleBase collectibleBase = collBase[i];
            dummyList.ojbAdd(collectibleBase);
        }
        gat.setCollectionDummy(dummyList);

        broker.beginTransaction();
        broker.store(gat);
        broker.commitTransaction();

        Identity oid = broker.serviceIdentity().buildIdentity(gat);
        broker.clearCache();
        Gatherer new_gat = (Gatherer) broker.getObjectByIdentity(oid);
        assertNotNull(new_gat);
        assertNotNull(new_gat.getCollectionDummy());
        assertEquals(collBase.length, new_gat.getCollectionDummy().size());

    }

    public void testStoreReadOfUserDefinedCollectionClass_2()
    {
        String name = "testStoreReadOfUserDefinedCollectionClass_2_"+System.currentTimeMillis();
        Gatherer gat = new Gatherer(null, name);

        CollectibleBase[] collBase = prepareCollectibleBase(name);
        CollectionClassDummy dummyList = new CollectionClassDummy();
        for (int i = 0; i < collBase.length; i++)
        {
            CollectibleBase collectibleBase = collBase[i];
            dummyList.ojbAdd(collectibleBase);
        }
        gat.setCollectionDummy(dummyList);

        broker.beginTransaction();
        broker.store(gat);
        broker.commitTransaction();

        broker.clearCache();
        Criteria crit = new Criteria();
        crit.addEqualTo("name", name);
        Query q = QueryFactory.newQuery(Gatherer.class, crit);
        Collection result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals(1, result.size());
        Gatherer new_gat = (Gatherer) result.iterator().next();
        assertNotNull(new_gat);
        assertNotNull(new_gat.getCollectionDummy());
        assertEquals(collBase.length, new_gat.getCollectionDummy().size());

    }

    /**
     * generate main object with collections and store
     * main object to make all persistent
     */
    public void testStoreDeleteSimpleCollections()
    {
        long timestamp = System.currentTimeMillis();
        String colPrefix = "col_" + timestamp;
        String name = timestamp + "_testStoreDeleteSimpleCollections";

        // create gatherer with collections
        Gatherer gatherer = new Gatherer(null, name);
        gatherer.setCollectiblesBase(Arrays.asList(prepareCollectibleBase(colPrefix)));
        gatherer.setCollectiblesB(Arrays.asList(prepareCollectibleB(colPrefix)));

        broker.beginTransaction();
        broker.store(gatherer);
        broker.commitTransaction();
        assertEquals("CollectibleBase objects", 3, gatherer.getCollectiblesBase().size());
        assertEquals(gatherer.getGatId(), ((CollectibleBaseIF) gatherer.getCollectiblesBase().get(0)).getGathererId());
        assertEquals("CollectibleB objects", 4, gatherer.getCollectiblesB().size());
        assertEquals(gatherer.getGatId(), ((CollectibleBIF) gatherer.getCollectiblesB().get(0)).getGathererId());

        Identity oid = broker.serviceIdentity().buildIdentity(gatherer);
        broker.clearCache();
        Gatherer new_gatherer = (Gatherer) broker.getObjectByIdentity(oid);

        assertNotNull(new_gatherer);
        assertNotNull(new_gatherer.getCollectiblesBase());
        assertNotNull(new_gatherer.getCollectiblesB());
        assertEquals("CollectibleBase objects", 3, new_gatherer.getCollectiblesBase().size());
        assertEquals("CollectibleB objects", 4, new_gatherer.getCollectiblesB().size());
        assertEquals(new_gatherer.getGatId(), ((CollectibleBaseIF) new_gatherer.getCollectiblesBase().get(0)).getGathererId());
        assertEquals(new_gatherer.getGatId(), ((CollectibleBIF) new_gatherer.getCollectiblesB().get(0)).getGathererId());

        broker.clearCache();

        Criteria criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colBase*");
        Query q = new QueryByCriteria(CollectibleBase.class, criteria);
        Collection result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 3, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colB*");
        q = new QueryByCriteria(CollectibleB.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 4, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "*");
        q = new QueryByCriteria(CollectibleBaseIF.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 7, result.size());


        // now we delete the main object
        // and see what's going on with the dependend objects
        broker.beginTransaction();
        // auto-delete false set for CollectiblesB
        List manuallyList = new_gatherer.getCollectiblesB();
        for (Iterator iterator = manuallyList.iterator(); iterator.hasNext();)
        {
            broker.delete(iterator.next());
        }
        broker.delete(new_gatherer);
        broker.commitTransaction();

        broker.clearCache();
        new_gatherer = (Gatherer) broker.getObjectByIdentity(oid);

        assertNull(new_gatherer);

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colBase*");
        q = new QueryByCriteria(CollectibleBase.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        // auto-delete is set true
        assertEquals("Wrong number of queried objects", 0, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colB*");
        q = new QueryByCriteria(CollectibleB.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        // auto-delete is set false, but we removed manually
        assertEquals("Wrong number of queried objects", 0, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "*");
        q = new QueryByCriteria(CollectibleBaseIF.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        // since we delete all childs
        assertEquals("Wrong number of queried objects", 0, result.size());
    }

    /**
     * generate main object with collections and store
     * main object to make all persistent
     */
    public void testDeleteCollection()
    {
        long timestamp = System.currentTimeMillis();
        String colPrefix = "col_" + timestamp;
        String name = timestamp + "_testDeleteCollectionDoRemoveCollectionObjectBeforeDelete";

        // create gatherer with collections
        Gatherer gatherer = new Gatherer(null, name);
        gatherer.setCollectiblesBase(Arrays.asList(prepareCollectibleBase(colPrefix)));
        gatherer.setCollectiblesB(Arrays.asList(prepareCollectibleB(colPrefix)));
        gatherer.setCollectiblesCC(Arrays.asList(prepareCollectibleCC(colPrefix)));

        broker.beginTransaction();
        broker.store(gatherer);
        broker.commitTransaction();
        assertEquals("CollectibleBase objects", 3, gatherer.getCollectiblesBase().size());
        assertEquals(gatherer.getGatId(), ((CollectibleBaseIF) gatherer.getCollectiblesBase().get(0)).getGathererId());
        assertEquals("CollectibleB objects", 4, gatherer.getCollectiblesB().size());
        assertEquals(gatherer.getGatId(), ((CollectibleBIF) gatherer.getCollectiblesB().get(0)).getGathererId());

        Identity oid = broker.serviceIdentity().buildIdentity(gatherer);
        broker.clearCache();
        Gatherer new_gatherer = (Gatherer) broker.getObjectByIdentity(oid);

        assertNotNull(new_gatherer);
        assertNotNull(new_gatherer.getCollectiblesBase());
        assertNotNull(new_gatherer.getCollectiblesB());
        assertEquals("CollectibleBase objects", 3, new_gatherer.getCollectiblesBase().size());
        assertEquals("CollectibleB objects", 4, new_gatherer.getCollectiblesB().size());
        assertEquals(new_gatherer.getGatId(), ((CollectibleBaseIF) new_gatherer.getCollectiblesBase().get(0)).getGathererId());
        assertEquals(new_gatherer.getGatId(), ((CollectibleBIF) new_gatherer.getCollectiblesB().get(0)).getGathererId());

        broker.clearCache();

        Criteria criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colBase*");
        Query q = new QueryByCriteria(CollectibleBase.class, criteria);
        Collection result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 3, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colB*");
        q = new QueryByCriteria(CollectibleB.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 4, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "*");
        q = new QueryByCriteria(CollectibleBaseIF.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 7, result.size());

        broker.beginTransaction();
        // now get all CollectibleB
        List colBList = new_gatherer.getCollectiblesB();
        for (Iterator iterator = colBList.iterator(); iterator.hasNext();)
        {
            broker.delete(iterator.next());
        }
        broker.delete(new_gatherer);
        broker.commitTransaction();

        broker.clearCache();
        new_gatherer = (Gatherer) broker.getObjectByIdentity(oid);

        assertNull(new_gatherer);

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colBase*");
        q = new QueryByCriteria(CollectibleBase.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        // auto-delete is set true
        assertEquals("Wrong number of queried objects", 0, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colB*");
        q = new QueryByCriteria(CollectibleB.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        // auto-delete is set false
        assertEquals("Wrong number of queried objects", 0, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "*");
        q = new QueryByCriteria(CollectibleBaseIF.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 0, result.size());
    }

    /**
     * generate main object with collections and store
     * main object to make all persistent
     */
    public void testDeleteMainObjectWithOneToNRelation()
    {
        long timestamp = System.currentTimeMillis();
        String colPrefix = "col_" + timestamp;
        String name = timestamp + "_testDeleteMainObjectWithOneToNRelation";

        // create gatherer with collections
        Gatherer gatherer = new Gatherer(null, name);
        List colsList = Arrays.asList(prepareCollectibleC2(colPrefix));
        gatherer.setCollectiblesC2(colsList);
        for (Iterator iterator = colsList.iterator(); iterator.hasNext();)
        {
            ((CollectibleC2) iterator.next()).setGatherer(gatherer);
        }


        broker.beginTransaction();
        broker.store(gatherer);
        broker.commitTransaction();

        assertNotNull(gatherer.getCollectiblesC2());
        assertEquals(5, gatherer.getCollectiblesC2().size());
        assertEquals(gatherer.getGatId(), ((CollectibleC2) gatherer.getCollectiblesC2().get(0)).getGathererId());

        broker.clearCache();
        Identity oid = broker.serviceIdentity().buildIdentity(gatherer);
        gatherer = (Gatherer) broker.getObjectByIdentity(oid);
        assertNotNull(gatherer);
        // auto-retierve is set false
        assertNull(gatherer.getCollectiblesC2());

        Criteria criteria = new Criteria();
        criteria.addLike("name", colPrefix + "*");
        Query q = new QueryByCriteria(CollectibleC2.class, criteria);
        Collection result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 5, result.size());

        broker.beginTransaction();
        // auto-retieve is false, so get references manually
        broker.retrieveAllReferences(gatherer);
        assertNotNull(gatherer.getCollectiblesC2());
        List colList = gatherer.getCollectiblesC2();
        for (Iterator iterator = colList.iterator(); iterator.hasNext();)
        {
            // delete all references first
            broker.delete(iterator.next());
        }
        broker.delete(gatherer);
        broker.commitTransaction();

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "*");
        q = new QueryByCriteria(CollectibleC2.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 0, result.size());
    }

    /**
     * generate main object with collections and store
     * main object to make all persistent
     * using ojbConcreteClass feature to map different
     * objects to same table
     */
    public void testStoreDeleteSimpleCollections_2()
    {
        long timestamp = System.currentTimeMillis();
        String colPrefix = "col_" + timestamp;
        String name = timestamp + "_testStoreDeleteSimpleCollections";

        // create gatherer with collections
        Gatherer gatherer = new Gatherer(null, name);
        gatherer.setCollectiblesD(Arrays.asList(prepareCollectibleD(colPrefix)));
        gatherer.setCollectiblesDD(Arrays.asList(prepareCollectibleDD(colPrefix)));

        broker.beginTransaction();
        broker.store(gatherer);
        broker.commitTransaction();
        assertEquals("CollectibleD objects", 2, gatherer.getCollectiblesD().size());
        assertEquals(gatherer.getGatId(), ((CollectibleDIF) gatherer.getCollectiblesD().get(0)).getGathererId());
        assertEquals("CollectibleDD objects", 3, gatherer.getCollectiblesDD().size());
        assertEquals(gatherer.getGatId(), ((CollectibleDDIF) gatherer.getCollectiblesDD().get(0)).getGathererId());

        Identity oid = broker.serviceIdentity().buildIdentity(gatherer);
        broker.clearCache();
        Gatherer new_gatherer = (Gatherer) broker.getObjectByIdentity(oid);

        assertNotNull(new_gatherer);
        assertNotNull(new_gatherer.getCollectiblesD());
        assertNotNull(new_gatherer.getCollectiblesDD());
        assertEquals("CollectibleD objects", 2, new_gatherer.getCollectiblesD().size());
        assertEquals("CollectibleDD objects", 3, new_gatherer.getCollectiblesDD().size());
        assertEquals(new_gatherer.getGatId(), ((CollectibleDIF) new_gatherer.getCollectiblesD().get(0)).getGathererId());
        assertEquals(new_gatherer.getGatId(), ((CollectibleDDIF) new_gatherer.getCollectiblesDD().get(0)).getGathererId());

        broker.clearCache();

        Criteria criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colD*");
        criteria.addLike("name", colPrefix + "*");
        Query q = new QueryByCriteria(CollectibleD.class, criteria);
        Collection result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 2, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colDD*");
        q = new QueryByCriteria(CollectibleDD.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 3, result.size());

        // now test objConcreteClass feature
        // should only return CollectibleD class instances
        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "*");
        q = new QueryByCriteria(CollectibleD.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 2, result.size());
        // now test objConcreteClass feature
        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "*");
        q = new QueryByCriteria(CollectibleDD.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 3, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "*");
        q = new QueryByCriteria(CollectibleBaseIF.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 5, result.size());

        // now we delete the main object
        // and see what's going on with the dependend objects
        broker.beginTransaction();
        broker.delete(new_gatherer);
        broker.commitTransaction();

        broker.clearCache();
        new_gatherer = (Gatherer) broker.getObjectByIdentity(oid);

        assertNull(new_gatherer);

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colD*");
        q = new QueryByCriteria(CollectibleD.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        // auto-delete is set true
        assertEquals("Wrong number of queried objects", 0, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colDD*");
        q = new QueryByCriteria(CollectibleDD.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        // auto-delete is set true
        assertEquals("Wrong number of queried objects", 0, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "*");
        q = new QueryByCriteria(CollectibleBaseIF.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 0, result.size());
    }

    public void testStoreSimpleCollections()
    {
        long timestamp = System.currentTimeMillis();
        String colPrefix = "col_" + timestamp;
        String name = timestamp + "_testStoreSimpleCollections";

        // create gatherer with collections
        Gatherer gatherer = new Gatherer(null, name);

        broker.beginTransaction();
        broker.store(gatherer);
        gatherer.setCollectiblesBase(Arrays.asList(prepareCollectibleBase(colPrefix)));
        gatherer.setCollectiblesB(Arrays.asList(prepareCollectibleB(colPrefix)));
        broker.store(gatherer);
        broker.commitTransaction();

        Identity oid = broker.serviceIdentity().buildIdentity(gatherer);
        broker.clearCache();
        Gatherer new_gatherer = (Gatherer) broker.getObjectByIdentity(oid);

        assertNotNull(new_gatherer);
        assertNotNull(new_gatherer.getCollectiblesBase());
        assertNotNull(new_gatherer.getCollectiblesB());
        assertEquals("CollectibleBase objects", 3, new_gatherer.getCollectiblesBase().size());
        assertEquals("CollectibleB objects", 4, new_gatherer.getCollectiblesB().size());
        Integer gatId = ((CollectibleBaseIF) new_gatherer.getCollectiblesBase().get(0)).getGathererId();
        assertNotNull(gatId);
        assertEquals(new_gatherer.gatId, gatId);
        broker.clearCache();

        Criteria criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colBase*");
        Query q = new QueryByCriteria(CollectibleBase.class, criteria);
        Collection result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 3, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colB*");
        q = new QueryByCriteria(CollectibleB.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 4, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "*");
        q = new QueryByCriteria(CollectibleBaseIF.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 7, result.size());
    }

    /**
     * Add new reference objects to an existing collection reference (1:n)
     * of a main object.
     */
    public void testAddNewObjectsToExistingCollection()
    {
        long timestamp = System.currentTimeMillis();
        String colPrefix = "testAddNewObjectsToExistingCollection_" + timestamp;
        String name = "testAddNewObjectsToExistingCollection_"+timestamp;

        // create gatherer with collections
        Gatherer gatherer = new Gatherer(null, name);

        broker.beginTransaction();
        gatherer.setCollectiblesBase(Arrays.asList(prepareCollectibleBase(colPrefix)));
        broker.store(gatherer);
        broker.commitTransaction();

        Identity oid = broker.serviceIdentity().buildIdentity(gatherer);
        broker.clearCache();
        Gatherer new_gatherer = (Gatherer) broker.getObjectByIdentity(oid);

        assertNotNull(new_gatherer);
        assertNotNull(new_gatherer.getCollectiblesBase());
        assertEquals("CollectibleBase objects", 3, new_gatherer.getCollectiblesBase().size());
        Integer gatId = ((CollectibleBaseIF) new_gatherer.getCollectiblesBase().get(0)).getGathererId();
        assertNotNull(gatId);
        assertEquals(new_gatherer.gatId, gatId);
        broker.clearCache();

        // additional check, read reference objects by query
        Criteria criteria = new Criteria();
        criteria.addLike("name", colPrefix+"*");
        Query q = new QueryByCriteria(CollectibleBase.class, criteria);
        Collection result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 3, result.size());

        List newEntries = Arrays.asList(prepareCollectibleBase(colPrefix));
        new_gatherer.getCollectiblesBase().addAll(newEntries);
        broker.beginTransaction();
        broker.store(new_gatherer);
        broker.commitTransaction();
        broker.clearCache();

        new_gatherer = (Gatherer) broker.getObjectByIdentity(oid);
        assertNotNull(new_gatherer);
        assertNotNull(new_gatherer.getCollectiblesBase());
        assertEquals("CollectibleBase objects", 6, new_gatherer.getCollectiblesBase().size());
        gatId = ((CollectibleBaseIF) new_gatherer.getCollectiblesBase().get(5)).getGathererId();
        assertNotNull(gatId);
        assertEquals(new_gatherer.gatId, gatId);
    }

    /**
     * generate main object with collections and store
     * main object to make all persistent.
     * same like {@link #testStoreSimpleCollections} but now the
     * collection objects have a reference back to main object.
     *
     * Curious but this test does not pass
     */
    public void testStoreCollectionObjectsWithBackReference()
    {
        long timestamp = System.currentTimeMillis();
        String colPrefix = "col_" + timestamp;
        String name = timestamp + "_testStoreCollectionObjectsWithBackReference";

        // create gatherer with collections
        Gatherer gatherer = new Gatherer(null, name);
        List collsCList = Arrays.asList(prepareCollectibleC(colPrefix));
        gatherer.setCollectiblesC(collsCList);
        for (Iterator iterator = collsCList.iterator(); iterator.hasNext();)
        {
            ((CollectibleC) iterator.next()).setGatherer(gatherer);
        }

        broker.beginTransaction();
        broker.store(gatherer);
        broker.commitTransaction();
        assertEquals("CollectibleC objects", 5, gatherer.getCollectiblesC().size());
        assertNotNull(gatherer.getCollectiblesC());
        assertTrue(gatherer.getCollectiblesC().size() > 0);
        assertEquals(gatherer.getGatId(), ((CollectibleCIF) gatherer.getCollectiblesC().get(0)).getGathererId());

        Identity oid = broker.serviceIdentity().buildIdentity(gatherer);
        broker.clearCache();
        Gatherer new_gatherer = (Gatherer) broker.getObjectByIdentity(oid);

        assertNotNull(new_gatherer);
        assertNotNull(new_gatherer.getCollectiblesC());
        assertEquals("CollectibleC objects", 5, gatherer.getCollectiblesC().size());
        assertEquals(gatherer.getGatId(), ((CollectibleCIF) gatherer.getCollectiblesC().get(0)).getGathererId());

        broker.clearCache();

        Criteria criteria = new Criteria();
        criteria.addLike("name", colPrefix + "_colC*");
        Query q = new QueryByCriteria(CollectibleC.class, criteria);
        Collection result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 5, result.size());

        criteria = new Criteria();
        criteria.addLike("name", colPrefix + "*");
        q = new QueryByCriteria(CollectibleBaseIF.class, criteria);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals("Wrong number of queried objects", 5, result.size());
    }

    public void testOneBookShelfQueryByCollection() throws Exception
    {
        String prefix = "testOneBookShelfQueryByCollection_" + System.currentTimeMillis();

        BookShelf bookShelf = new BookShelf(prefix);
        BookShelfItem ev1 = new DVD(bookShelf);
        bookShelf.addItem(ev1);
        BookShelfItem ev2 = new Book(bookShelf);
        bookShelf.addItem(ev2);

        broker.beginTransaction();
        broker.store(bookShelf);
        broker.store(ev1);
        broker.store(ev2);
        broker.commitTransaction();
        assertTrue(bookShelf.getPk() != null);

        broker.clearCache();
        BookShelf loadedCopy = (BookShelf) broker.getObjectByIdentity(
                broker.serviceIdentity().buildIdentity(BookShelf.class, bookShelf.getPk()));
        assertNotNull(loadedCopy);
        assertNotNull(loadedCopy.getItems());
        assertEquals(2, loadedCopy.getItems().size());

        broker.clearCache();
        Criteria criteria = new Criteria();
        criteria.addLike("name", prefix);
        Query q = new QueryByCriteria(BookShelf.class, criteria);
        Collection books = broker.getCollectionByQuery(q);
        assertNotNull(books);
        assertEquals(1, books.size());
        // we are using collection proxies, so we have to use the interface
        BookShelfIF bookShelfIF = (BookShelfIF) books.iterator().next();
        assertNotNull(bookShelfIF.getItems());
        assertEquals("wrong number of items found", 2, bookShelfIF.getItems().size());
    }

    public void testOneBookShelfQueryByIterator() throws Exception
    {
        String prefix = "testOneBookShelfQueryByIterator_" + System.currentTimeMillis();

        BookShelf bookShelf = new BookShelf(prefix);
        BookShelfItem ev1 = new DVD(bookShelf);
        bookShelf.addItem(ev1);
        BookShelfItem ev2 = new Book(bookShelf);
        bookShelf.addItem(ev2);

        broker.beginTransaction();
        broker.store(bookShelf);
        broker.store(ev1);
        broker.store(ev2);
        broker.commitTransaction();

        assertTrue(bookShelf.getPk() != null);

        broker.clearCache();
        BookShelf loadedCopy = (BookShelf) broker.getObjectByIdentity(
                broker.serviceIdentity().buildIdentity(BookShelf.class, bookShelf.getPk()));
        assertNotNull(loadedCopy);
        assertNotNull(loadedCopy.getItems());
        assertEquals(2, loadedCopy.getItems().size());

        broker.clearCache();
        Criteria criteria = new Criteria();
        criteria.addLike("name", prefix);
        Query q = new QueryByCriteria(BookShelf.class, criteria);
        Iterator books = broker.getIteratorByQuery(q);
        assertTrue(books.hasNext());
        loadedCopy = (BookShelf) books.next();
        assertNotNull(loadedCopy.getItems());
        assertEquals("wrong number of items found", 2, loadedCopy.getItems().size());
    }


    /**
     * Test RemovalAwareCollection remove() and clear()
     */
    public void testRemovalAwareCollection()
    {
        String prefix = "testRemovalAwareCollection_" + System.currentTimeMillis();

        Identity gathererId;
        Gatherer loadedCopy;
        Gatherer gatherer = new Gatherer(null, "Gatherer_" + prefix);
        List coll = new ArrayList();
        coll.add(new CollectibleBase("Base_1_" + prefix));
        coll.add(new CollectibleBase("Base_2_" + prefix));
        coll.add(new CollectibleBase("Base_3_" + prefix));
        gatherer.setCollectiblesBase(coll);

        broker.beginTransaction();
        broker.store(gatherer);
        broker.commitTransaction();
        assertTrue(gatherer.getGatId() != null);
        gathererId = broker.serviceIdentity().buildIdentity(gatherer);

        broker.clearCache();
        loadedCopy = (Gatherer) broker.getObjectByIdentity(gathererId);
        assertNotNull(loadedCopy);
        assertNotNull(loadedCopy.getCollectiblesBase());
        assertTrue(loadedCopy.getCollectiblesBase() instanceof RemovalAwareCollection);
        assertEquals(3, loadedCopy.getCollectiblesBase().size());

        //
        // Remove a single element
        //
        broker.beginTransaction();
        loadedCopy.getCollectiblesBase().remove(2);
        broker.store(loadedCopy);
        broker.commitTransaction();

        broker.clearCache();
        loadedCopy = (Gatherer) broker.getObjectByIdentity(gathererId);
        assertNotNull(loadedCopy);
        assertNotNull(loadedCopy.getCollectiblesBase());
        assertTrue(loadedCopy.getCollectiblesBase() instanceof RemovalAwareCollection);
        assertEquals(2, loadedCopy.getCollectiblesBase().size());

        //
        // Remove all elements
        //
        broker.beginTransaction();
        loadedCopy.getCollectiblesBase().clear();
        broker.store(loadedCopy);
        broker.commitTransaction();

        broker.clearCache();
        loadedCopy = (Gatherer) broker.getObjectByIdentity(gathererId);
        assertNotNull(loadedCopy);
        assertNotNull(loadedCopy.getCollectiblesBase());
        assertTrue(loadedCopy.getCollectiblesBase() instanceof RemovalAwareCollection);
        assertEquals(0, loadedCopy.getCollectiblesBase().size());
    }

    /**
     * Test RemovalAwareCollection remove() of non persistent obj
     */
    public void testRemovalAwareCollection2()
    {
        String prefix = "testRemovalAwareCollection2_" + System.currentTimeMillis();

        Identity gathererId;
        Gatherer loadedCopy;
        Gatherer gatherer = new Gatherer(null, "Gatherer_" + prefix);
        List coll = new ArrayList();
        coll.add(new CollectibleBase("Base_1_" + prefix));
        coll.add(new CollectibleBase("Base_2_" + prefix));
        gatherer.setCollectiblesBase(coll);

        broker.beginTransaction();
        broker.store(gatherer);
        broker.commitTransaction();
        assertTrue(gatherer.getGatId() != null);
        gathererId = broker.serviceIdentity().buildIdentity(gatherer);

        broker.clearCache();
        loadedCopy = (Gatherer) broker.getObjectByIdentity(gathererId);
        assertNotNull(loadedCopy);
        assertNotNull(loadedCopy.getCollectiblesBase());
        assertTrue(loadedCopy.getCollectiblesBase() instanceof RemovalAwareCollection);
        assertEquals(2, loadedCopy.getCollectiblesBase().size());

        // add and remove non persistent obj
        loadedCopy.getCollectiblesBase().add(new CollectibleBase("Base_3_" + prefix));
        assertEquals(3, loadedCopy.getCollectiblesBase().size());
        loadedCopy.getCollectiblesBase().remove(2);

        broker.beginTransaction();
        broker.store(loadedCopy);
        broker.commitTransaction();
    }

    /**
     * Test RemovalAwareCollection remove() and clear()
     */
    public void testRemovalAwareCollectionProxy()
    {
        String prefix = "testRemovalAwareCollectionProxy_" + System.currentTimeMillis();

        Identity pgId;
        ProductGroup loadedCopy;
        InterfaceArticle article;
        ProductGroup pg = new ProductGroup(null, "PG_" + prefix, null);
        article = new Article();
        article.setArticleName("Art_1_" + prefix);
        pg.add(article);
        article = new Article();
        article.setArticleName("Art_2_" + prefix);
        pg.add(article);
        article = new Article();
        article.setArticleName("Art_3_" + prefix);
        pg.add(article);

        broker.beginTransaction();
        broker.store(pg);
        broker.commitTransaction();
        assertTrue(pg.getGroupId() != null);
        pgId = broker.serviceIdentity().buildIdentity(pg);

        broker.clearCache();
        loadedCopy = (ProductGroup) broker.getObjectByIdentity(pgId);
        assertNotNull(loadedCopy);
        assertNotNull(loadedCopy.getAllArticlesInGroup());
        assertTrue(loadedCopy.getAllArticlesInGroup() instanceof RemovalAwareCollection);
        assertEquals(3, loadedCopy.getAllArticlesInGroup().size());

        //
        // Remove a single element
        //
        broker.beginTransaction();
        loadedCopy.getAllArticlesInGroup().remove(2);
        broker.store(loadedCopy);
        broker.commitTransaction();

        broker.clearCache();
        loadedCopy = (ProductGroup) broker.getObjectByIdentity(pgId);
        assertNotNull(loadedCopy);
        assertNotNull(loadedCopy.getAllArticlesInGroup());
        assertTrue(loadedCopy.getAllArticlesInGroup() instanceof RemovalAwareCollection);
        assertEquals(2, loadedCopy.getAllArticlesInGroup().size());

        //
        // Remove all elements
        //
        broker.beginTransaction();
        loadedCopy.getAllArticlesInGroup().clear();
        broker.store(loadedCopy);
        broker.commitTransaction();

        broker.clearCache();
        loadedCopy = (ProductGroup) broker.getObjectByIdentity(pgId);
        assertNotNull(loadedCopy);
        assertNotNull(loadedCopy.getAllArticlesInGroup());
        assertTrue(loadedCopy.getAllArticlesInGroup() instanceof RemovalAwareCollection);
        assertEquals(0, loadedCopy.getAllArticlesInGroup().size());
    }

    //************************************************************
    // helper methods
    //************************************************************

    private CollectibleBase[] prepareCollectibleBase(String namePrefix)
    {
        return new CollectibleBase[]{
            new CollectibleBase(namePrefix + "_colBase_1"),
            new CollectibleBase(namePrefix + "_colBase_2"),
            new CollectibleBase(namePrefix + "_colBase_3")
        };
    }

    private CollectibleB[] prepareCollectibleB(String namePrefix)
    {
        return new CollectibleB[]{
            new CollectibleB(namePrefix + "_colB_1"),
            new CollectibleB(namePrefix + "_colB_2"),
            new CollectibleB(namePrefix + "_colB_3"),
            new CollectibleB(namePrefix + "_colB_4")
        };
    }

    private CollectibleC[] prepareCollectibleC(String namePrefix)
    {
        return new CollectibleC[]{
            new CollectibleC(namePrefix + "_colC_1", "ext1"),
            new CollectibleC(namePrefix + "_colC_2", "ext2"),
            new CollectibleC(namePrefix + "_colC_3", "ext3"),
            new CollectibleC(namePrefix + "_colC_4", "ext4"),
            new CollectibleC(namePrefix + "_colC_5", "ext5")
        };
    }

    private CollectibleCC[] prepareCollectibleCC(String namePrefix)
    {
        return new CollectibleCC[]{
            new CollectibleCC(namePrefix + "_colCC_1", "ext1"),
            new CollectibleCC(namePrefix + "_colCC_2", "ext2"),
            new CollectibleCC(namePrefix + "_colCC_3", "ext3"),
            new CollectibleCC(namePrefix + "_colCC_4", "ext4"),
            new CollectibleCC(namePrefix + "_colCC_5", "ext5")
        };
    }

    private CollectibleC2[] prepareCollectibleC2(String namePrefix)
    {
        return new CollectibleC2[]{
            new CollectibleC2(namePrefix + "_colC2_1", "ext1"),
            new CollectibleC2(namePrefix + "_colC2_2", "ext2"),
            new CollectibleC2(namePrefix + "_colC2_3", "ext3"),
            new CollectibleC2(namePrefix + "_colC2_4", "ext4"),
            new CollectibleC2(namePrefix + "_colC2_5", "ext5")
        };
    }

    private CollectibleD[] prepareCollectibleD(String namePrefix)
    {
        return new CollectibleD[]{
            new CollectibleD(namePrefix + "_colD_1"),
            new CollectibleD(namePrefix + "_colD_2"),
        };
    }

    private CollectibleDD[] prepareCollectibleDD(String namePrefix)
    {
        return new CollectibleDD[]{
            new CollectibleDD(namePrefix + "_colDD_1"),
            new CollectibleDD(namePrefix + "_colDD_2"),
            new CollectibleDD(namePrefix + "_colDD_3")
        };
    }


    //*********************************************************************
    // inner classes - persistent object
    //*********************************************************************

    public static class CollectionClassDummy implements ManageableCollection
    {
        ArrayList list = new ArrayList();

        public void ojbAdd(Object anObject)
        {
            list.add(anObject);
        }

        public void ojbAddAll(ManageableCollection otherCollection)
        {
            Iterator it = otherCollection.ojbIterator();
            while (it.hasNext())
            {
                list.add(it.next());
            }
        }

        public Iterator ojbIterator()
        {
            return list.iterator();
        }

        public void afterStore(PersistenceBroker broker) throws PersistenceBrokerException
        {
            //noop
        }

        public int size()
        {
            return list.size();
        }
    }


    public static class Gatherer implements Serializable
    {
        private Integer gatId;
        private String name;
        private List collectiblesBase;
        private List collectiblesB;
        private List collectiblesC;
        private List collectiblesCC;
        private List collectiblesC2;
        private List collectiblesD;
        private List collectiblesDD;
        private CollectionClassDummy collectionDummy;

        public Gatherer()
        {
        }

        public Gatherer(Integer gatId, String name)
        {
            this.gatId = gatId;
            this.name = name;
        }

        public Integer getGatId()
        {
            return gatId;
        }

        public void setGatId(Integer gatId)
        {
            this.gatId = gatId;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public CollectionClassDummy getCollectionDummy()
        {
            return collectionDummy;
        }

        public void setCollectionDummy(CollectionClassDummy collectionDummy)
        {
            this.collectionDummy = collectionDummy;
        }

        public List getCollectiblesBase()
        {
            return collectiblesBase;
        }

        public void setCollectiblesBase(List collectiblesBase)
        {
            this.collectiblesBase = collectiblesBase;
        }

        public List getCollectiblesB()
        {
            return collectiblesB;
        }

        public void setCollectiblesB(List collectiblesB)
        {
            this.collectiblesB = collectiblesB;
        }

        public List getCollectiblesC()
        {
            return collectiblesC;
        }

        public void setCollectiblesC(List collectiblesC)
        {
            this.collectiblesC = collectiblesC;
        }

        public List getCollectiblesCC()
        {
            return collectiblesCC;
        }

        public void setCollectiblesCC(List collectiblesCC)
        {
            this.collectiblesCC = collectiblesCC;
        }

        public List getCollectiblesC2()
        {
            return collectiblesC2;
        }

        public void setCollectiblesC2(List collectiblesC2)
        {
            this.collectiblesC2 = collectiblesC2;
        }

        public List getCollectiblesD()
        {
            return collectiblesD;
        }

        public void setCollectiblesD(List collectiblesD)
        {
            this.collectiblesD = collectiblesD;
        }

        public List getCollectiblesDD()
        {
            return collectiblesDD;
        }

        public void setCollectiblesDD(List collectiblesDD)
        {
            this.collectiblesDD = collectiblesDD;
        }

        public String toString()
        {
            ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
            buf.append("gatId", gatId);
            buf.append("name", name);
            buf.append("collectiblesBase", collectiblesBase);
            buf.append("collectiblesB", collectiblesB);
            buf.append("collectiblesC", collectiblesC);
            buf.append("collectiblesD", collectiblesD);
            buf.append("collectiblesDD", collectiblesDD);
            return buf.toString();
        }
    }

    public static interface CollectibleBaseIF extends Serializable
    {
        Integer getColId();

        void setColId(Integer colId);

        String getName();

        void setName(String name);

        Integer getGathererId();

        void setGathererId(Integer colId);

        Gatherer getGatherer();

        void setGatherer(Gatherer gatherer);
    }

    public static class CollectibleBase implements CollectibleBaseIF
    {
        private Integer colId;
        private String name;
        private Integer gathererId;
        private Gatherer gatherer;
        // protected String ojbConcreteClass;

        public CollectibleBase()
        {
            // ojbConcreteClass = CollectibleBase.class.getName();
        }

        public CollectibleBase(String name)
        {
            // ojbConcreteClass = CollectibleBase.class.getName();
            this.name = name;
        }

        public String toString()
        {
            ToStringBuilder buf = new ToStringBuilder(this);
            buf.append("colId", colId);
            buf.append("name", name);
            buf.append("gathererId", gathererId);
//            buf.append("ojbConcreteClass", ojbConcreteClass);
            return buf.toString();
        }

        public Gatherer getGatherer()
        {
            return gatherer;
        }

        public void setGatherer(Gatherer gatherer)
        {
            this.gatherer = gatherer;
        }

        public Integer getGathererId()
        {
            return gathererId;
        }

        public void setGathererId(Integer gathererId)
        {
            this.gathererId = gathererId;
        }

        public Integer getColId()
        {
            return colId;
        }

        public void setColId(Integer colId)
        {
            this.colId = colId;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    public static interface CollectibleBIF extends CollectibleBaseIF
    {

    }

    public static class CollectibleB extends CollectibleBase implements CollectibleBIF
    {
        public CollectibleB()
        {
//            ojbConcreteClass = CollectibleB.class.getName();
        }

        public CollectibleB(String name)
        {
            super(name);
//            ojbConcreteClass = CollectibleB.class.getName();
        }
    }

    public static interface CollectibleCIF extends CollectibleBIF
    {
        String getExtentName();

        void setExtentName(String extentName);
    }

    public static class CollectibleC extends CollectibleB implements CollectibleCIF
    {
        private String extentName;

        public CollectibleC()
        {
//            ojbConcreteClass = CollectibleC.class.getName();
        }

        public CollectibleC(String name)
        {
            super(name);
//            ojbConcreteClass = CollectibleC.class.getName();
        }

        public CollectibleC(String name, String extentName)
        {
            super(name);
//            ojbConcreteClass = CollectibleC.class.getName();
            this.extentName = extentName;
        }

        public String getExtentName()
        {
            return extentName;
        }

        public void setExtentName(String extentName)
        {
            this.extentName = extentName;
        }
    }

    public static class CollectibleCC extends CollectibleC
    {
        public CollectibleCC()
        {
        }

        public CollectibleCC(String name)
        {
            super(name);
        }

        public CollectibleCC(String name, String extentName)
        {
            super(name, extentName);
        }
    }

    public static class CollectibleC2 extends CollectibleC
    {
        public CollectibleC2()
        {
        }

        public CollectibleC2(String name)
        {
            super(name);
        }

        public CollectibleC2(String name, String extentName)
        {
            super(name, extentName);
        }
    }

    public static interface CollectibleDIF extends CollectibleBaseIF
    {

    }

    public static class CollectibleD extends CollectibleBase implements CollectibleDIF
    {
        protected String ojbConcreteClass;

        public CollectibleD()
        {
            ojbConcreteClass = CollectibleD.class.getName();
        }

        public CollectibleD(String name)
        {
            super(name);
            ojbConcreteClass = CollectibleD.class.getName();
        }

        public String getOjbConcreteClass()
        {
            return ojbConcreteClass;
        }

        public void setOjbConcreteClass(String ojbConcreteClass)
        {
            this.ojbConcreteClass = ojbConcreteClass;
        }
    }

    public static interface CollectibleDDIF extends CollectibleBaseIF
    {

    }

    public static class CollectibleDD extends CollectibleBase implements CollectibleDDIF
    {
        protected String ojbConcreteClass;

        public CollectibleDD()
        {
            ojbConcreteClass = CollectibleDD.class.getName();
        }

        public CollectibleDD(String name)
        {
            super(name);
            ojbConcreteClass = CollectibleDD.class.getName();
        }

        public String getOjbConcreteClass()
        {
            return ojbConcreteClass;
        }

        public void setOjbConcreteClass(String ojbConcreteClass)
        {
            this.ojbConcreteClass = ojbConcreteClass;
        }
    }

    public static interface BookShelfIF
    {
        public void addItem(BookShelfItem event);
        public List getItems();
        public Integer getPk();
        public void setPk(Integer pk);
        public String getName();
        public void setName(String name);
    }


    public static class BookShelf implements BookShelfIF
    {
        private Integer pk;
        private String name;
        private List items;

        public BookShelf()
        {
        }

        public BookShelf(String name)
        {
            this.name = name;
        }

        public void addItem(BookShelfItem event)
        {
            if (items == null)
                items = new ArrayList();

            items.add(event);
        }

        public void setItems(List items)
        {
            this.items = items;
        }

        public List getItems()
        {
            return items;
        }

        public Integer getPk()
        {
            return pk;
        }

        public void setPk(Integer pk)
        {
            this.pk = pk;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    public static abstract class BookShelfItem
    {
        private Integer pk;
        private String name;
        private BookShelfIF shelf;

        public BookShelfItem()
        {
        }

        public BookShelfItem(BookShelfIF shelf)
        {
            this.shelf = shelf;
        }

        protected BookShelfItem(String name, BookShelfIF shelf)
        {
            this.name = name;
            this.shelf = shelf;
        }

        public Integer getPk()
        {
            return pk;
        }

        public void setPk(Integer pk)
        {
            this.pk = pk;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public BookShelfIF getShelf()
        {
            return shelf;
        }

        public void setShelf(BookShelf shelf)
        {
            this.shelf = shelf;
        }
    }

    public static class DVD extends BookShelfItem
    {
        public DVD()
        {
        }

        public DVD(BookShelf shelf)
        {
            super(shelf);
        }

        public DVD(String name, BookShelfIF shelf)
        {
            super(name, shelf);
        }
    }

    public static class Book extends BookShelfItem
    {
        public Book()
        {
        }

        public Book(BookShelfIF shelf)
        {
            super(shelf);
        }

        public Book(String name, BookShelfIF shelf)
        {
            super(name, shelf);
        }
    }

     public static class Candie
    {
        private Integer pk;
        private String name;
        private String ingredients;
        private BookShelfIF shelf;

        public Candie()
        {
        }

        public Candie(BookShelfIF shelf)
        {
            this.shelf = shelf;
        }

        protected Candie(String name, BookShelfIF shelf)
        {
            this.name = name;
            this.shelf = shelf;
        }

        public Integer getPk()
        {
            return pk;
        }

        public void setPk(Integer pk)
        {
            this.pk = pk;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getIngredients()
        {
            return ingredients;
        }

        public void setIngredients(String ingredients)
        {
            this.ingredients = ingredients;
        }

        public BookShelfIF getShelf()
        {
            return shelf;
        }

        public void setShelf(BookShelf shelf)
        {
            this.shelf = shelf;
        }
    }
}
