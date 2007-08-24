package org.apache.ojb.odmg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.ODMGTestCase;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * Test case handles with collections.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: CollectionsTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class CollectionsTest extends ODMGTestCase
{
    public CollectionsTest(String s)
    {
        super(s);
    }

    public static void main(String[] args)
    {
        String[] arr = {CollectionsTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }


    public void testStoreDeleteCascadeDelete() throws Exception
    {
        String prefix = "testStoreDeleteCascadeDelete_" + System.currentTimeMillis();
        String queryStr = "select gatherer from " + Gatherer.class.getName() + " where gatId=$1";

        ojbChangeReferenceSetting(Gatherer.class, "collectiblesA", true,
                CollectionDescriptor.CASCADE_NONE, CollectionDescriptor.CASCADE_OBJECT, false);

        // prepare test case
        Gatherer gat = new Gatherer(null, prefix + "_Gatherer");
        CollectibleA[] cols = prepareCollectibleA(gat, prefix);
        List colList = Arrays.asList(cols);
        // set List of CollectiblesA objects
        gat.setCollectiblesA(colList);
        TransactionExt tx = (TransactionExt)odmg.newTransaction();
        tx.begin();
        database.makePersistent(gat);
        tx.commit();

        // check if gatherer was stored
        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create(queryStr);
        Integer gatId = gat.getGatId();
        assertNotNull(gatId);
        query.bind(gatId);
        Collection result = (Collection) query.execute();
        tx.commit();
        assertEquals("Wrong number of objects found", 1, result.size());
        Gatherer fetchedGat = (Gatherer) result.iterator().next();

        List colsA = fetchedGat.getCollectiblesA();
        assertEquals("Wrong number of CollectiblesA", 3, colsA.size());
        // check if gatherer contains list of CollectibleBase
        tx.begin();
        //*************************************
        database.deletePersistent(fetchedGat);
        //*************************************
        tx.commit();

        // check if the CollectibleBase was really deleted from DB
        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select allCollectibleA from " + CollectibleA.class.getName() +
                " where name like $1");
        query.bind(prefix + "%");
        result = (Collection) query.execute();
        assertEquals("Wrong number of objects found", 0, result.size());
        tx.commit();

        // check if the gatherer now contains a CollectibleBase list
        // reduced by the deleted
        tx.begin();
        query = odmg.newOQLQuery();
        query.create(queryStr);
        query.bind(gatId);
        result = (Collection) query.execute();
        assertEquals("Wrong number of objects found", 0, result.size());
        tx.commit();
    }

    public void testStoreCollectionElementWithoutBackReference() throws Exception
    {
        // String queryColl = "select colls from " + CollectibleC.class.getName() + " where name=$1";
        String queryGat = "select gatherer from " + Gatherer.class.getName() + " where gatId=$1";
        String prefix = "testStoreCollectionElementWithoutBackReference_" + System.currentTimeMillis();

        // prepare test case
        Gatherer gat = new Gatherer(null, prefix + "_Gatherer");
        TransactionExt tx = (TransactionExt)odmg.newTransaction();
        tx.begin();
        database.makePersistent(gat);
        tx.commit();
        // check if gatherer was stored
        tx.begin();
        tx.getBroker().clearCache();
        assertNotNull(gat.getGatId());
        OQLQuery query = odmg.newOQLQuery();
        query.create(queryGat);
        query.bind(gat.getGatId());
        Collection result = (Collection) query.execute();
        tx.commit();
        assertEquals("Wrong number of objects found", 1, result.size());
        gat = (Gatherer) result.iterator().next();
        assertNotNull(gat);
        //**********************************************
        CollectibleC child = new CollectibleC(prefix, null, "a new CollectibleC");
        tx.begin();
        tx.lock(gat, Transaction.WRITE);
        tx.lock(child, Transaction.WRITE);
        List childs = new ArrayList();
        childs.add(child);
        gat.setCollectiblesC(childs);
        tx.commit();
        //**********************************************
        // check if gatherer was stored
        tx.begin();
        tx.getBroker().clearCache();
        assertNotNull(gat.getGatId());
        query = odmg.newOQLQuery();
        query.create(queryGat);
        query.bind(gat.getGatId());
        result = (Collection) query.execute();
        tx.commit();
        assertEquals("Wrong number of objects found", 1, result.size());
        gat = (Gatherer) result.iterator().next();
        assertNotNull(gat);
        assertNotNull(gat.getCollectiblesC());
        assertEquals(1, gat.getCollectiblesC().size());
    }

    /**
     * Create an main object Gatherer with a collection of objects CollectiblesC.
     * CollectiblesC hasn't a reference back to the main object. After creation we
     * remove an collection element.
     */
    public void testRemoveCollectionElementWithoutBackReference() throws Exception
    {
        // String queryColl = "select colls from " + CollectibleC.class.getName() + " where name=$1";
        String queryGat = "select gatherer from " + Gatherer.class.getName() + " where gatId=$1";
        String prefix = "testDeleteCollectionElementWithoutBackReference_" + System.currentTimeMillis();

        // prepare test case
        Gatherer gat = new Gatherer(null, prefix + "_Gatherer");
        // we don't set the gatId in CollectiblesC, because we don't have one
        // Set List of CollectiblesC objects
        gat.setCollectiblesC(Arrays.asList(prepareCollectibleC(null, prefix)));
        TransactionExt tx = (TransactionExt)odmg.newTransaction();
        tx.begin();
        database.makePersistent(gat);
        tx.commit();

        // check if gatherer was stored
        tx.begin();
        tx.getBroker().clearCache();
        assertNotNull(gat.getGatId());

        OQLQuery query = odmg.newOQLQuery();
        query.create(queryGat);
        query.bind(gat.getGatId());

        Collection result = (Collection) query.execute();
        tx.commit();
        assertEquals("Wrong number of objects found", 1, result.size());
        Gatherer fetchedGat = (Gatherer) result.iterator().next();
        assertNotNull(fetchedGat);

        // check if gatherer contains list of CollectibleBase
        List colC = fetchedGat.getCollectiblesC();
        assertEquals("Wrong number of CollectiblesC", 3, colC.size());

        tx.begin();
        //*************************************
        tx.lock(fetchedGat, Transaction.WRITE);
        // Remove collection object
        Object toDelete = fetchedGat.getCollectiblesC().remove(0);
        // explicit persistent delete call needed
        database.deletePersistent(toDelete);
        // alternative use TransactionExt#autoDeleteRemovedCollectionReferences
        //*************************************
        tx.commit();

        // check if the Collectibles were really deleted from DB
        tx.begin();
        tx.getBroker().clearCache();

        query = odmg.newOQLQuery();
        query.create("select colls from " + CollectibleC.class.getName() +
                " where name like $1");
        query.bind(prefix + "%");
        result = (Collection) query.execute();

        assertEquals("Wrong number of objects found", 2, result.size());
        tx.commit();

        // check if the gatherer now contains a CollectibleBase list
        // increased by the added
        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create(queryGat);
        query.bind(gat.getGatId());
        result = (Collection) query.execute();
        assertEquals("Wrong number of objects found", 1, result.size());
        fetchedGat = (Gatherer) result.iterator().next();
        colC = fetchedGat.getCollectiblesC();
        assertEquals("Wrong number of CollectiblesA found in Gatherer", 2, colC.size());
        tx.commit();
        assertNotNull(colC.get(0));
    }

    public void testStoreFetchDeleteCollectionWithBackReference() throws Exception
    {
        String prefix = "testStoreFetchDeleteCollectionWithBackReference_" + System.currentTimeMillis();
        String queryStr = "select gatherer from " + Gatherer.class.getName() + " where gatId=$1";

        // prepare test case
        Gatherer gat = new Gatherer(null, prefix + "_Gatherer");
        CollectibleA[] cols = prepareCollectibleA(gat, prefix);
        List colList = Arrays.asList(cols);
        // set List of CollectiblesA objects
        gat.setCollectiblesA(colList);
        TransactionExt tx = (TransactionExt)odmg.newTransaction();
        tx.begin();
        database.makePersistent(gat);
        tx.commit();

        // check if gatherer was stored
        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create(queryStr);
        Integer gatId = gat.getGatId();
        assertNotNull(gatId);
        query.bind(gatId);
        Collection result = (Collection) query.execute();
        tx.commit();
        assertEquals("Wrong number of objects found", 1, result.size());
        Gatherer fetchedGat = (Gatherer) result.iterator().next();

        List colsA = fetchedGat.getCollectiblesA();
        assertEquals("Wrong number of CollectiblesA", 3, colsA.size());
        // check if gatherer contains list of CollectibleBase
        tx.begin();
        //*************************************
        // delete one of the CollectibleBase
        // we have to set the new reduced list in the
        // gatherer object
        List newCols = new ArrayList();
        newCols.add(colsA.get(1));
        newCols.add(colsA.get(2));
        fetchedGat.setCollectiblesA(newCols);
        tx.lock(fetchedGat, Transaction.WRITE);
        // todo: do we need to delete removed reference explicit?
        database.deletePersistent(colsA.get(0));
        //*************************************
        tx.commit();

        // check if the CollectibleBase was really deleted from DB
        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select allCollectibleA from " + CollectibleA.class.getName() +
                " where name like $1");
        query.bind(prefix + "%");
        result = (Collection) query.execute();
        assertEquals("Wrong number of objects found", 2, result.size());
        tx.commit();

        // check if the gatherer now contains a CollectibleBase list
        // reduced by the deleted
        tx.begin();
        query = odmg.newOQLQuery();
        query.create(queryStr);
        query.bind(gatId);
        result = (Collection) query.execute();
        assertEquals("Wrong number of objects found", 1, result.size());
        fetchedGat = (Gatherer) result.iterator().next();
        colsA = fetchedGat.getCollectiblesA();
        assertEquals("Wrong number of CollectiblesA found in Gatherer", 2, colsA.size());
        tx.commit();

        colsA.get(0);
    }

    public void testWithBackReference_1() throws Exception
    {
        String prefix = "testWithBackReference_1_" + System.currentTimeMillis();
        String queryStr = "select gatherer from " + Gatherer.class.getName() + " where gatId=$1";

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        // prepare test case
        Gatherer gat = new Gatherer(null, prefix + "_Gatherer");
        CollectibleA[] colsA = prepareCollectibleA(gat, prefix);
        CollectibleB[] colsB = prepareCollectibleB(gat, prefix);
        List colListA = Arrays.asList(colsA);
        List colListB = Arrays.asList(colsB);
        // set List of CollectibleBase objects
        gat.setCollectiblesA(colListA);
        gat.setCollectiblesB(colListB);

        database.makePersistent(gat);
        tx.commit();

        // check if gatherer was stored
        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create(queryStr);
        Integer gatId = gat.getGatId();
        assertNotNull(gatId);
        query.bind(gatId);
        Collection result = (Collection) query.execute();

        assertEquals("Wrong number of objects found", 1, result.size());
        Gatherer fetchedGat = (Gatherer) result.iterator().next();
        //*************************************
        tx.lock(fetchedGat, Transaction.WRITE);
        assertNotNull(fetchedGat.getCollectiblesA());
        assertNotNull(fetchedGat.getCollectiblesB());
        assertEquals(3, fetchedGat.getCollectiblesA().size());
        assertEquals(3, fetchedGat.getCollectiblesB().size());
        assertEquals(0, fetchedGat.getCollectiblesC().size());
        assertNotNull(fetchedGat.getCollectiblesA().get(0));
        assertNotNull(fetchedGat.getCollectiblesB().get(0));

        fetchedGat.getCollectiblesA().remove(0);
        fetchedGat.getCollectiblesB().remove(0);
        //*************************************
        //System.out.println("===> commit");
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create(queryStr);
        gatId = gat.getGatId();
        assertNotNull(gatId);
        query.bind(gatId);
        result = (Collection) query.execute();
        tx.commit();
        assertEquals("Wrong number of objects found", 1, result.size());
        fetchedGat = (Gatherer) result.iterator().next();

        assertNotNull(fetchedGat.getCollectiblesA());
        assertNotNull(fetchedGat.getCollectiblesB());
        assertEquals(2, fetchedGat.getCollectiblesA().size());
        assertEquals(2, fetchedGat.getCollectiblesB().size());
        assertNotNull(fetchedGat.getCollectiblesA().get(0));
        assertNotNull(fetchedGat.getCollectiblesB().get(0));
    }

    /**
     * This test shows an issue with circular references in conjunction with
     * lazy loading and a non-global-shared cache.
     */
    public void testWithBackReference_2() throws Exception
    {
        if(ojbSkipKnownIssueProblem("Issue using proxies with circular references and a non-global-shared cache")) return;

        /*
        Say we have an object with circular reference (A has a 1:1 reference to B
        and B has a 1:n collection reference to A)
        A1 -1:1-> B1 -1:n-> [A1,A4]
        and the 1:n is a collection proxy.

        Now user lookup A1 and get A1@11->B1@12-->[proxy@]. He wants to
        remove the A4 object in the 1:n reference in B@12. Because B has an
        proxy collection, the proxy materialize on
        B.getA's().remove(1) ==> remove A4
        call.
        While materialization of the collection proxy OJB lookup again an A1 instance.
        When the previous materialzed A1@11 instance isn't in the session
        cache (e.g. A1 was used in a previous session), OJB lookup the real cache.
        If the real cache is "empty" or works with copies of persistent objects (TLCache)
        new instance for A1 ==> A1@22 and a new B1@44 will be materialized.

        Thus we have after the remove of A4 with materialized proxy:
        A1@11 --> B1@12 -->proxy@[A1@22[-->B1@44]-->A1@22]] !!!!
        Needless to say this will cause problems on update.

        The workaround for the odmg-api is shown in test #testWithBackReference_2,
        if the materialization of the proxy object is done within a running tx and
        the changed object is locked again after materialization of the proxy (to replace
        the new created object instance with the old one).
        */

        String prefix = "testWithBackReference_2_" + System.currentTimeMillis();
        String queryStr = "select gatherer from " + Gatherer.class.getName() + " where gatId=$1";

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        // prepare test case
        Gatherer gat = new Gatherer(null, prefix + "_Gatherer");
        CollectibleA[] colsA = prepareCollectibleA(gat, prefix);
        CollectibleB[] colsB = prepareCollectibleB(gat, prefix);
        List colListA = Arrays.asList(colsA);
        List colListB = Arrays.asList(colsB);
        // set List of CollectibleBase objects
        gat.setCollectiblesA(colListA);
        gat.setCollectiblesB(colListB);

        database.makePersistent(gat);
        tx.commit();
//System.out.println("===> commit");
//System.out.println();
//System.out.println();

        // check if gatherer was stored
        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create(queryStr);
        Integer gatId = gat.getGatId();
        assertNotNull(gatId);
        query.bind(gatId);
        Collection result = (Collection) query.execute();
        tx.commit();

        assertEquals("Wrong number of objects found", 1, result.size());
        Gatherer fetchedGat = (Gatherer) result.iterator().next();
        assertNotNull(fetchedGat.getCollectiblesA());
        assertNotNull(fetchedGat.getCollectiblesB());
        assertEquals(3, fetchedGat.getCollectiblesA().size());
        assertEquals(3, fetchedGat.getCollectiblesB().size());
        assertEquals(0, fetchedGat.getCollectiblesC().size());
        assertNotNull(fetchedGat.getCollectiblesA().get(0));
        assertNotNull(fetchedGat.getCollectiblesB().get(0));
//System.out.println("A: " + fetchedGat.getCollectiblesA());
//System.out.println("B: " + fetchedGat.getCollectiblesB());
//        for(int i = 0; i < fetchedGat.getCollectiblesB().size(); i++)
//        {
//            System.out.println("  b="+fetchedGat.getCollectiblesB().get(i));
//        }
//System.out.println();
//System.out.println();
//System.out.println("## New tx begin");
        tx.begin();
        tx.getBroker().clearCache();
        //*************************************
        tx.lock(fetchedGat, Transaction.WRITE);
        // we want automatic delete of removed collection objects
        //tx.autoDeleteRemovedCollectionReferences(true);
        // alternative do explicit call Database#deletePersistent for removed objects
        fetchedGat.getCollectiblesA().remove(0);
        fetchedGat.getCollectiblesB().remove(0);
tx.getBroker().serviceObjectCache().cache(tx.getBroker().serviceIdentity().buildIdentity(fetchedGat), fetchedGat);
//System.out.println("remove: " + tx.getBroker().serviceIdentity().buildIdentity(fetchedGat.getCollectiblesA().remove(0)));
//System.out.println("remove: " + tx.getBroker().serviceIdentity().buildIdentity(fetchedGat.getCollectiblesB().remove(0)));
//System.out.println("A: " + fetchedGat.getCollectiblesA());
//System.out.println("B: " + fetchedGat.getCollectiblesB());
//System.out.println("===> commit after remove");
//System.out.println("===> commit after remove");
        //*************************************
        tx.commit();
//System.out.println("after commit <==");
//System.out.println("after commit <==");
//System.out.println("");System.out.println();

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create(queryStr);
        gatId = gat.getGatId();
        assertNotNull(gatId);
        query.bind(gatId);
        result = (Collection) query.execute();
        tx.commit();
        assertEquals("Wrong number of objects found", 1, result.size());
        fetchedGat = (Gatherer) result.iterator().next();

        assertNotNull(fetchedGat.getCollectiblesA());
        assertNotNull(fetchedGat.getCollectiblesB());
        assertEquals(2, fetchedGat.getCollectiblesA().size());
        assertEquals(2, fetchedGat.getCollectiblesB().size());
        assertNotNull(fetchedGat.getCollectiblesA().get(0));
        assertNotNull(fetchedGat.getCollectiblesB().get(0));
    }

    public void testUpdateCollectionWithBackReference() throws Exception
    {
        String name = "testUpdateCollectionWithBackReference" + System.currentTimeMillis();
        String queryStr = "select colls from " + CollectibleA.class.getName() + " where name=$1";

        // prepare test case
        Gatherer gat_1 = new Gatherer(null, "Gatherer_" + name);
        CollectibleA coll_1 = new CollectibleA(name);
        Gatherer gat_2 = new Gatherer(null, "Gatherer_" + name);
        CollectibleA coll_2 = new CollectibleA(name);

        coll_1.setGatherer(gat_1);
        ArrayList alist = new ArrayList();
        alist.add(coll_1);
        gat_1.setCollectiblesA(alist);

        coll_2.setGatherer(gat_2);
        ArrayList blist = new ArrayList();
        blist.add(coll_2);
        gat_2.setCollectiblesA(blist);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(coll_1);
        database.makePersistent(coll_2);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create(queryStr);
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertNotNull(result);
        assertEquals(2, result.size());

        for (Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            CollectibleA collectible = (CollectibleA) iterator.next();
            Gatherer gat = collectible.getGatherer();
            assertNotNull(gat);
            assertEquals("Gatherer_"+name, gat.getName());
            tx.lock(collectible, Transaction.WRITE);
            collectible.getGatherer().setName("New_"+name);
        }
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create(queryStr);
        query.bind(name);
        result = (Collection) query.execute();
        assertNotNull(result);
        assertEquals(2, result.size());

        // we don't want that Gatherer does some cascade delete
        tx.setCascadingDelete(Gatherer.class, false);
        for (Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            CollectibleA collectible = (CollectibleA) iterator.next();
            Gatherer gat = collectible.getGatherer();
            assertNotNull(gat);
            assertEquals("New_"+name, gat.getName());
            tx.lock(collectible, Transaction.WRITE);
            collectible.setGatherer(null);
            gat.getCollectiblesA().remove(collectible);
        }
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create(queryStr);
        query.bind(name);
        result = (Collection) query.execute();
        assertNotNull(result);
        assertEquals(2, result.size());

        for (Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            CollectibleA collectible = (CollectibleA) iterator.next();
            Gatherer gat = collectible.getGatherer();
            assertNull(gat);
        }
        tx.commit();
    }

    /**
     * Create object with 3 objects in associated collection.
     * We change one object of the collection
     */
    public void testUpdateCollection() throws Exception
    {
        String prefix = "testUpdateCollection" + System.currentTimeMillis();
        String queryStr = "select gatherer from " + Gatherer.class.getName() + " where gatId=$1";
        String modifiedName = "modified_name_" + System.currentTimeMillis();
        String queryMod = "select coll from " + CollectibleA.class.getName() + " where name like $1";

        // prepare test case
        Gatherer gat = new Gatherer(null, prefix + "_Gatherer");
        // set List of CollectiblesA objects
        gat.setCollectiblesA(Arrays.asList(prepareCollectibleA(gat, prefix + "_1_")));
        TransactionExt tx = (TransactionExt)odmg.newTransaction();
        tx.begin();
        database.makePersistent(gat);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        // check if gatherer was stored
        OQLQuery query = odmg.newOQLQuery();
        query.create(queryStr);
        assertNotNull(gat.getGatId());
        query.bind(gat.getGatId());
        Collection result = (Collection) query.execute();
        tx.commit();
        assertEquals("Wrong number of objects found", 1, result.size());
        Gatherer fetchedGat = (Gatherer) result.iterator().next();

        tx.begin();
        tx.lock(fetchedGat, Transaction.WRITE);
        List collsA = fetchedGat.getCollectiblesA();
        assertEquals(3, collsA.size());
        // now we change an object of the collection
        ((CollectibleA)collsA.get(0)).setName(modifiedName);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        // now check if the modification was stored
        query = odmg.newOQLQuery();
        query.create(queryMod);
        query.bind(modifiedName);
        result = (Collection) query.execute();
        tx.commit();
        assertEquals("Wrong number of objects found", 1, result.size());
        CollectibleA collA = (CollectibleA) result.iterator().next();
        assertNotNull(collA);
        assertEquals(modifiedName, collA.getName());
    }

    /**
     * we create two objects with one object in collection:
     * gat1{collC1} and gat2{collC2}
     * then we exchange the collections
     * gat1{collC2} and gat2{collC1}
     * and commit. So the size of the collection
     * hold by the main object doesn't change
     */
    public void testUpdateWhenExchangeObjectsInCollection() throws Exception
    {
        final String prefix = "testUpdateWhenExchangeObjectsInCollection" + System.currentTimeMillis();
        final String queryStr = "select gatherer from " + Gatherer.class.getName() +
                                " where gatId=$1 or gatId=$2 order by gatId asc";

        // prepare test case
        final String gat1Name = prefix + "_Gatherer";
        final String gat2Name = prefix + "_Gatherer2";
        Gatherer gat = new Gatherer(null, gat1Name);
        Gatherer gat2 = new Gatherer(null, gat2Name);
        // set List of CollectiblesC objects
        CollectibleC collC_1 = new CollectibleC(prefix + "NO_1", null, "nothing1");
        CollectibleC collC_2 = new CollectibleC(prefix + "NO_2", null, "nothing2");
        gat.setCollectiblesC(Arrays.asList(new CollectibleC[]{collC_1}));
        gat2.setCollectiblesC(Arrays.asList(new CollectibleC[]{collC_2}));
        TransactionExt tx = (TransactionExt)odmg.newTransaction();
        tx.begin();
        database.makePersistent(gat);
        database.makePersistent(gat2);
        tx.commit();

        // query and check the result
        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create(queryStr);
        assertNotNull(gat.getGatId());
        query.bind(gat.getGatId());
        query.bind(gat2.getGatId());
        Collection result = (Collection) query.execute();
        tx.commit();
        assertEquals("Wrong number of objects found", 2, result.size());
        Iterator it = result.iterator();
        Gatherer fetchedGat = (Gatherer) it.next();
        Gatherer fetchedGat2 = (Gatherer) it.next();
        assertNotNull(fetchedGat);
        assertNotNull(fetchedGat2);
        assertEquals("Wrong gatherer returned: fetchedGat should be first Gatherer",
                gat1Name, fetchedGat.getName());
        assertEquals("Wrong gatherer returned: fetchedGat2 should be second Gatherer",
                gat2Name, fetchedGat2.getName());
        assertNotNull(fetchedGat.collectiblesC);
        assertNotNull(fetchedGat2.collectiblesC);
        assertEquals(1, fetchedGat.getCollectiblesC().size());
        assertEquals(1, fetchedGat2.getCollectiblesC().size());

        collC_1 = (CollectibleC) fetchedGat.getCollectiblesC().get(0);
        collC_2 = (CollectibleC) fetchedGat2.getCollectiblesC().get(0);
        assertEquals(prefix + "NO_1", collC_1.getName());
        assertEquals(prefix + "NO_2", collC_2.getName());

        //*****************************************************
        List list1 = fetchedGat.getCollectiblesC();
        List list2 = fetchedGat2.getCollectiblesC();
        // now exchange the lists
        tx.begin();
        tx.lock(fetchedGat, Transaction.WRITE);
        tx.lock(fetchedGat2, Transaction.WRITE);
        fetchedGat.setCollectiblesC(list2);
        fetchedGat2.setCollectiblesC(list1);
        // System.out.println("#####===> start commit");
        tx.commit();
        //*****************************************************
        // System.out.println("#####===> end commit");

        // now we do same procedure to query and check
        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create(queryStr);
        assertNotNull(gat.getGatId());
        query.bind(gat.getGatId());
        query.bind(gat2.getGatId());
        result = (Collection) query.execute();
        tx.commit();
        assertEquals("Wrong number of objects found", 2, result.size());
        it = result.iterator();
        fetchedGat = (Gatherer) it.next();
        fetchedGat2 = (Gatherer) it.next();
        assertNotNull(fetchedGat);
        assertNotNull(fetchedGat2);
        assertNotNull(fetchedGat.getCollectiblesC());
        assertNotNull(fetchedGat2.getCollectiblesC());
        assertEquals(1, fetchedGat.getCollectiblesC().size());
        assertEquals(1, fetchedGat2.getCollectiblesC().size());

        collC_1 = (CollectibleC) fetchedGat.getCollectiblesC().get(0);
        collC_2 = (CollectibleC) fetchedGat2.getCollectiblesC().get(0);
        // we exchange the lists, thus we expect exchanged names
        assertEquals(prefix + "NO_2", collC_1.getName());
        assertEquals(prefix + "NO_1", collC_2.getName());
    }

    /**
     * Create an main object Gatherer with a collection of objects CollectiblesC.
     * CollectiblesC hasn't a reference back to the main object, thus we don't have to set
     * the main object in the collection objects. Further we can't set the object id of the
     * main object, because we don't know it at creation time.
     * Then we remove one object of the collection
     */
    public void testRemoveCollectionElementWithoutBackReference_2() throws Exception
    {
        // String queryColl = "select colls from " + CollectibleC.class.getName() + " where name=$1";
        String queryGat = "select gatherer from " + Gatherer.class.getName() + " where gatId=$1";
        String prefix = "testRemoveCollectionObjectWithoutBackReference_2_" + System.currentTimeMillis();

        // prepare test case
        Gatherer gat = new Gatherer(null, prefix + "_Gatherer");
        // we don't set the gatId in CollectiblesC, because we don't have one
        // Set List of CollectiblesC objects
        gat.setCollectiblesC(Arrays.asList(prepareCollectibleC(null, prefix)));
        TransactionExt tx = (TransactionExt)odmg.newTransaction();
        tx.begin();
        database.makePersistent(gat);
        tx.commit();

        // check if gatherer was stored
        tx.begin();
        tx.getBroker().clearCache();
        assertNotNull(gat.getGatId());

        OQLQuery query = odmg.newOQLQuery();
        query.create(queryGat);
        query.bind(gat.getGatId());

        Collection result = (Collection) query.execute();
        tx.commit();
        assertEquals("Wrong number of objects found", 1, result.size());
        Gatherer fetchedGat = (Gatherer) result.iterator().next();
        assertNotNull(fetchedGat);

        List colC = fetchedGat.getCollectiblesC();
        assertEquals("Wrong number of CollectiblesC", 3, colC.size());
        // check if gatherer contains list of CollectibleBase
        tx.begin();
        //**********************************************************
        // we replace the collection of main object with a new collection
        // reduced by one element
        List newCols = new ArrayList(colC);
        Object toDelete = newCols.remove(2);
        // lock object before do changes
        tx.lock(fetchedGat, Transaction.WRITE);
        fetchedGat.setCollectiblesC(newCols);
        // todo: we need to delete removed object explicit?
        database.deletePersistent(toDelete);
        //**********************************************************
        tx.commit();

        // check if the Collectibles were really deleted from DB
        tx.begin();
        tx.getBroker().clearCache();

        query = odmg.newOQLQuery();
        query.create("select colls from " + CollectibleC.class.getName() +
                " where name like $1");
        query.bind(prefix + "%");
        result = (Collection) query.execute();
        assertEquals("Wrong number of objects found", 2, result.size());
        tx.commit();

        // check if the gatherer now contains a CollectibleBase list
        // reduced by the deleted
        tx.begin();
        query = odmg.newOQLQuery();
        query.create(queryGat);
        query.bind(gat.getGatId());
        result = (Collection) query.execute();
        assertEquals("Wrong number of objects found", 1, result.size());
        fetchedGat = (Gatherer) result.iterator().next();
        colC = fetchedGat.getCollectiblesC();
        assertEquals("Wrong number of CollectiblesA found in Gatherer", 2, colC.size());
        tx.commit();

        colC.get(0);
    }

    /**
     * Create an main object Gatherer with a collection of objects CollectiblesC.
     * CollectiblesC hasn't a reference back to the main object, thus we don't have to set
     * the main object in the collection objects. Further we can't set the object id of the
     * main object, because we don't know it at creation time.
     * Then we ADD a new object to the collection
     */
    public void testAddCollectionElementWithoutBackReference() throws Exception
    {
        // String queryColl = "select colls from " + CollectibleC.class.getName() + " where name=$1";
        String queryGat = "select gatherer from " + Gatherer.class.getName() + " where gatId=$1";
        String prefix = "testAddCollectionElementWithoutBackReference_" + System.currentTimeMillis();

        // prepare test case
        Gatherer gat = new Gatherer(null, prefix + "_Gatherer");
        // we don't set the gatId in CollectiblesC, because we don't have one
        // Set List of CollectiblesC objects
        gat.setCollectiblesC(Arrays.asList(prepareCollectibleC(null, prefix)));
        TransactionExt tx = (TransactionExt)odmg.newTransaction();
        tx.begin();
        database.makePersistent(gat);
        tx.commit();

        // check if gatherer was stored
        tx.begin();
        tx.getBroker().clearCache();
        assertNotNull(gat.getGatId());

        OQLQuery query = odmg.newOQLQuery();
        query.create(queryGat);
        query.bind(gat.getGatId());

        Collection result = (Collection) query.execute();
        tx.commit();
        assertEquals("Wrong number of objects found", 1, result.size());
        Gatherer fetchedGat = (Gatherer) result.iterator().next();
        assertNotNull(fetchedGat);

        // check if gatherer contains list of CollectibleBase
        List colC = fetchedGat.getCollectiblesC();
        assertEquals("Wrong number of CollectiblesC", 3, colC.size());

        tx.begin();
        //*************************************
        tx.lock(fetchedGat, Transaction.WRITE);
        // Now add a new collection object
        CollectibleC newC = new CollectibleC(prefix, null, "### new added ###");
        fetchedGat.getCollectiblesC().add(newC);
        newC.setGathererId(fetchedGat.getGatId());
        tx.lock(newC, Transaction.WRITE);
        //*************************************
        tx.commit();

        // check if the Collectibles were really deleted from DB
        tx.begin();
        tx.getBroker().clearCache();

        query = odmg.newOQLQuery();
        query.create("select colls from " + CollectibleC.class.getName() +
                " where name like $1");
        query.bind(prefix + "%");
        result = (Collection) query.execute();
        assertEquals("Wrong number of objects found", 4, result.size());
        tx.commit();

        // check if the gatherer now contains a CollectibleBase list
        // increased by the added
        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create(queryGat);
        query.bind(gat.getGatId());
        result = (Collection) query.execute();
        assertEquals("Wrong number of objects found", 1, result.size());
        fetchedGat = (Gatherer) result.iterator().next();
        colC = fetchedGat.getCollectiblesC();
        assertEquals("Wrong number of CollectiblesA found in Gatherer", 4, colC.size());
        tx.commit();

        colC.get(0);
    }

    /**
     * Create an main object Gatherer with a collection of objects CollectiblesC
     * (CollectiblesC has a reference back to the main object).
     * Then we ADD a new object to the collection
     */
    public void testAddCollectionElementWithBackReference() throws Exception
    {
        String queryGat = "select gatherer from " + Gatherer.class.getName() + " where gatId=$1";
        String prefix = "testAddCollectionElementWithBackReference_" + System.currentTimeMillis();

        /*
        prepare test case
        If the back reference was not set, the test doesn't pass
        */
        Gatherer gat = new Gatherer(null, prefix + "_Gatherer");
        // Set List of CollectiblesB objects
        gat.setCollectiblesB(Arrays.asList(prepareCollectibleB(gat, prefix)));

        TransactionExt tx = (TransactionExt)odmg.newTransaction();
        tx.begin();
        database.makePersistent(gat);
        tx.commit();

        // check if gatherer was stored
        tx.begin();
        tx.getBroker().clearCache();
        assertNotNull(gat.getGatId());

        OQLQuery query = odmg.newOQLQuery();
        query.create(queryGat);
        query.bind(gat.getGatId());

        Collection result = (Collection) query.execute();
        tx.commit();
        assertEquals("Wrong number of objects found", 1, result.size());
        Gatherer fetchedGat = (Gatherer) result.iterator().next();
        assertNotNull(fetchedGat);


        tx.begin();
        // check if gatherer contains list of CollectibleBase
        List colB = fetchedGat.getCollectiblesB();
        assertEquals("Wrong number of CollectiblesB", 3, colB.size());

        //*************************************
        tx.lock(fetchedGat, Transaction.WRITE);
        // Now add a new collection object
        CollectibleB newB = new CollectibleB(prefix);
        newB.setGatherer(fetchedGat);
        fetchedGat.getCollectiblesB().add(newB);
        // lock the new object
        tx.lock(newB, Transaction.WRITE);
        //*************************************

        tx.commit();

        // check
        tx.begin();
        tx.getBroker().clearCache();

        query = odmg.newOQLQuery();
        query.create("select colls from " + CollectibleB.class.getName() +
                " where name like $1");
        query.bind(prefix + "%");
        result = (Collection) query.execute();
        assertEquals("Wrong number of objects found", 4, result.size());
        tx.commit();

        // check if the gatherer now contains a CollectibleBase list
        // increased by the added
        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create(queryGat);
        query.bind(gat.getGatId());
        result = (Collection) query.execute();
        assertEquals("Wrong number of objects found", 1, result.size());
        fetchedGat = (Gatherer) result.iterator().next();
        colB = fetchedGat.getCollectiblesB();
        assertEquals("Wrong number of CollectiblesA found in Gatherer", 4, colB.size());
        tx.commit();

        colB.get(0);
    }

/*
    User test case from user-list:
> A contains a collection of B.  B has a reference back to its parent A.
> Calling A.addB(B) sets B's reference to A.
>
> Create new A.
> Create new B.
> Add B to A.
> Make A persistent.
> A and B successfully persisted to the database.
> Clear the OJB cache
> Retrieve A (using PB)
> Start Tx (ODMG)
> Lock A for writing (ODMG)
> Create a new B2.
> Add B2 to A.
> Commit Tx.
> Clear Cache.
> Retrieve A (using PB)
> Assert(A count Bs == 2) FAIL.  B2 was never persisted.
>
> ODMG's Persistence by reachability should have persisted B2, should it not?
> I thought that using DList might fix this.
     */
    public void testAddCollectionElementCrossAPI() throws Exception
    {
        String name = "testAddCollectionElementCrossAPI_"+System.currentTimeMillis();
        // prepare test case
        Gatherer gat = new Gatherer(null, name);
        CollectibleB B = new CollectibleB(name);
        B.setGatherer(gat);
        ArrayList cols = new ArrayList();
        cols.add(B);
        gat.setCollectiblesB(cols);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(gat);
        tx.commit();

        // now cross ODMG with PB api
        Gatherer fetchedGatherer = null;
        PersistenceBroker pb = null;
        try
        {
            pb = PersistenceBrokerFactory.defaultPersistenceBroker();
            pb.clearCache();
            Criteria crit = new Criteria();
            crit.addLike("name", name);
            Query q = QueryFactory.newQuery(Gatherer.class, crit);
            fetchedGatherer = (Gatherer) pb.getObjectByQuery(q);
        }
        finally
        {
            if(pb != null) pb.close();
        }

        // check queried result
        assertNotNull(fetchedGatherer);
        assertEquals(gat.getGatId(), fetchedGatherer.getGatId());
        assertNotNull(fetchedGatherer.getCollectiblesB());
        assertEquals(1, fetchedGatherer.getCollectiblesB().size());
        CollectibleB fetched_B = (CollectibleB) fetchedGatherer.getCollectiblesB().iterator().next();
        assertNotNull(fetched_B);

        // Now work with queried result
        tx.begin();
        tx.getBroker().clearCache();
        //*************************************
        tx.lock(fetchedGatherer, Transaction.WRITE);
        CollectibleB newB = new CollectibleB(name);
        newB.setGatherer(fetchedGatherer);
        fetchedGatherer.getCollectiblesB().add(newB);
        tx.lock(newB, Transaction.WRITE);
        //*************************************
        assertEquals(2, fetchedGatherer.getCollectiblesB().size());
        tx.commit();

        // now cross again ODMG with PB api
        fetchedGatherer = null;
        pb = null;
        try
        {
            pb = PersistenceBrokerFactory.defaultPersistenceBroker();
            pb.clearCache();
            Criteria crit = new Criteria();
            crit.addLike("name", name);
            Query q = QueryFactory.newQuery(Gatherer.class, crit);
            fetchedGatherer = (Gatherer) pb.getObjectByQuery(q);
        }
        finally
        {
            if(pb != null) pb.close();
        }

        // check queried result
        assertNotNull(fetchedGatherer);
        assertEquals(gat.getGatId(), fetchedGatherer.getGatId());
        assertNotNull(fetchedGatherer.getCollectiblesB());
        assertEquals(2, fetchedGatherer.getCollectiblesB().size());
        CollectibleB fetched_B_1 = (CollectibleB) fetchedGatherer.getCollectiblesB().iterator().next();
        CollectibleB fetched_B_2 = (CollectibleB) fetchedGatherer.getCollectiblesB().iterator().next();
        assertNotNull(fetched_B_1);
        assertNotNull(fetched_B_2);
    }



    //**********************************************************
    // helper methods
    //**********************************************************

    private CollectibleA[] prepareCollectibleA(Gatherer gat, String namePrefix)
    {
        CollectibleA[] colA = new CollectibleA[]{
            new CollectibleA(namePrefix + " colA_1"),
            new CollectibleA(namePrefix + " colA_2"),
            new CollectibleA(namePrefix + " colA_3")
        };
        for (int i = 0; i < colA.length; i++)
        {
            CollectibleA collectibleA = colA[i];
            collectibleA.setGatherer(gat);
        }
        return colA;
    }

    private CollectibleB[] prepareCollectibleB(Gatherer gat, String namePrefix)
    {
        CollectibleB[] colB = new CollectibleB[]{
            new CollectibleB(namePrefix + " colB_1"),
            new CollectibleB(namePrefix + " colB_2"),
            new CollectibleB(namePrefix + " colB_3")
        };
        for (int i = 0; i < colB.length; i++)
        {
            CollectibleB collectibleB = colB[i];
            collectibleB.setGatherer(gat);
        }
        return colB;
    }

    private CollectibleC[] prepareCollectibleC(Gatherer gat, String namePrefix)
    {
        CollectibleC[] colC = new CollectibleC[]{
            new CollectibleC(namePrefix + " colC_1", null, "ext1"),
            new CollectibleC(namePrefix + " colC_2", null, "ext2"),
            new CollectibleC(namePrefix + " colC_3", null, "ext3")
        };
        for (int i = 0; i < colC.length; i++)
        {
            CollectibleC collectibleC = colC[i];
            collectibleC.setGathererId(gat != null ? gat.gatId : null);
        }
        return colC;
    }


    //****************************************************************************
    // inner classes
    //****************************************************************************
    public static class Gatherer implements Serializable
    {
        private Integer gatId;
        private String name;
        private List collectiblesA = new Vector();
        private List collectiblesB = new Vector();
        private List collectiblesC = new Vector();

        public Gatherer()
        {
        }

        public String toString()
        {
            return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                    .append("gatId", gatId)
                    .append("name", name)
                    .append("colA", collectiblesA)
                    .append("colB", collectiblesB)
                    .append("colc", collectiblesC)
                    .toString();
        }

        public void addCollectibleA(CollectibleA colA)
        {
            if (collectiblesA == null) collectiblesA = new Vector();
            collectiblesA.add(colA);
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

        public List getCollectiblesA()
        {
            return collectiblesA;
        }

        public void setCollectiblesA(List collectiblesA)
        {
            this.collectiblesA = collectiblesA;
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
    }

    public static interface CollectibleBIF extends Serializable
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

    public static class CollectibleB implements CollectibleBIF
    {
        private Integer colId;
        private String name;
        private Integer gathererId;
        private Gatherer gatherer;

        public CollectibleB()
        {
        }

        public CollectibleB(String name)
        {
            this.name = name;
        }

        public CollectibleB(String name, Integer gathererId)
        {
            this.name = name;
            this.gathererId = gathererId;
        }

        public CollectibleB(Integer colId, String name, Integer gathererId)
        {
            this.colId = colId;
            this.name = name;
            this.gathererId = gathererId;
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

    public static interface CollectibleAIF extends Serializable
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

    public static class CollectibleA implements CollectibleAIF
    {
        private Integer colId;
        private String name;
        private Integer gathererId;
        private Gatherer gatherer;

        public CollectibleA()
        {
        }

        public CollectibleA(Integer colId, String name, Integer gathererId)
        {
            this.colId = colId;
            this.name = name;
            this.gathererId = gathererId;
        }

        public CollectibleA(String name, Integer gathererId)
        {
            this.name = name;
            this.gathererId = gathererId;
        }

        public CollectibleA(String name)
        {
            this.name = name;
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

    public static interface CollectibleCIF extends Serializable
    {
        Integer getColId();
        void setColId(Integer colId);
        String getName();
        void setName(String name);
        Integer getGathererId();
        void setGathererId(Integer colId);
        Gatherer getGatherer();
        void setGatherer(Gatherer gatherer);
        String getExtentName();
        void setExtentName(String extentName);
    }

    public static class CollectibleC
    {
        private Integer colId;
        private String name;
        private Integer gathererId;
        private String extentName;

        public CollectibleC()
        {
        }

        public CollectibleC(String name, Integer gathererId, String extentName)
        {
            this.name = name;
            this.gathererId = gathererId;
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

        public Integer getGathererId()
        {
            return gathererId;
        }

        public void setGathererId(Integer gathererId)
        {
            this.gathererId = gathererId;
        }
    }
}
