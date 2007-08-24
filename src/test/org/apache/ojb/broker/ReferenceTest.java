package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Test case for checking the management of references.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ReferenceTest.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class ReferenceTest extends PBTestCase
{
    private static String REF_TEST_STRING = "refTest";

    public static void main(String[] args)
    {
        String[] arr = {ReferenceTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void tearDown()
    {
        if(broker != null)
        {
            changeRepositoryAutoSetting("ref", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT);
            changeRepositoryAutoSetting("refA", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT);
            changeRepositoryAutoSetting("refB", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT);
            changeRepositoryAutoSetting("animal", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT);
            broker.close();
        }
    }

    /**
     * Test for OJB-49
     */
    public void testQueryExtentsWithAutoRefreshEnabled() throws Exception
    {
        String name = "testQueryExtentsWithAutoRefreshEnabled_"+ System.currentTimeMillis();

        int cascadeObject = ObjectReferenceDescriptor.CASCADE_OBJECT;
        ojbChangeReferenceSetting(ObjA.class, "ref", true, cascadeObject, cascadeObject, false);
        ojbChangeReferenceSetting(ObjB.class, "ref", true, cascadeObject, cascadeObject, false);

        ClassDescriptor cldA = broker.getClassDescriptor(ObjA.class);
        ClassDescriptor cldB = broker.getClassDescriptor(ObjB.class);
        boolean oldRefreshA = cldA.isAlwaysRefresh();
        boolean oldRefreshB = cldB.isAlwaysRefresh();
        cldA.setAlwaysRefresh(true);
        cldB.setAlwaysRefresh(true);

        try
        {
            ObjA objA = new ObjA();
            objA.setName(name);
            ObjB objB = new ObjB();
            objB.setName(name);
            ObjC objC = new ObjC();
            objC.setName(name);

            ObjA objA2 = new ObjA();
            objA2.setName(name);
            ObjB objB2 = new ObjB();
            objB2.setName(name);
            ObjC objC2 = new ObjC();
            objC2.setName(name);

            List refs = new ArrayList();
            refs.add(objA2);
            refs.add(objB2);

            objA.setRef(objB);
            objC2.setReferences(refs);
            objB.setRef(objC);
            objC2.setRef(objA);

            broker.beginTransaction();
            broker.store(objA);
            broker.store(objC2);
            broker.commitTransaction();

            Criteria crit = new Criteria();
            crit.addLike("name", name);
            Query q = QueryFactory.newQuery(RefObject.class, crit);
            Collection result = broker.getCollectionByQuery(q);
            assertEquals(6, result.size());
        }
        finally
        {
            cldA.setAlwaysRefresh(oldRefreshA);
            cldB.setAlwaysRefresh(oldRefreshB);
        }
    }

    /**
     * Test the usage of interface as class-ref in collection-descriptor
     * when using inheritance.
     */
    public void testInterfaceAsCollectionRef_1()
    {
        // if(skipKnownIssueProblem("query using path via reference, like 'ref1.ref2.name'")) return;
        String name = "testQueryWithCollectionRef_" + System.currentTimeMillis();
        RefObject a = new ObjA();
        RefObject a2 = new ObjA();
        RefObject b = new ObjB();
        RefObject b2 = new ObjB();
        // this object has a 1:n relation
        ObjC c1 = new ObjC();
        // only used in the c object reference collection
        RefObject d1 = new ObjC();
        RefObject d2 = new ObjC();
        ObjC c2 = new ObjC();

        c1.setName(name+"_third");
        b.setName(name+"_second_1");
        b2.setName(name+"_second_2");
        a.setName(name+"_first_1");
        a2.setName(name+"_first_2");
        d1.setName(name+"_d1");
        d2.setName(name+"_d2");
        c2.setName(name + "_c2");

        c1.setNameC(name + "_1");
        c2.setNameC(name + "_2");

        a.setRef(b);
        b.setRef(c1);
        a2.setRef(b2);

        List refList = new ArrayList();
        refList.add(a);
        refList.add(b2);
        refList.add(d1);
        c1.setReferences(refList);
        List refList2 = new ArrayList();
        refList2.add(d2);
        c2.setReferences(refList2);

        broker.beginTransaction();
        broker.store(a);
        broker.store(a2);
        broker.store(c2);
        broker.commitTransaction();

        // check existence of objects
        Criteria crit = new Criteria();
        crit.addLike("name", name + "%");
        Query q = QueryFactory.newQuery(RefObject.class, crit);
        Collection result = broker.getCollectionByQuery(q);
        assertEquals(8, result.size());

        // expect all 'C' objects with 1:n reference object
        // with name '..._d1' --> 'c1'
        crit = new Criteria();
        crit.addEqualTo("references.name", name+"_d1");
        q = QueryFactory.newQuery(ObjC.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());
        ObjC newC = (ObjC) result.iterator().next();
        assertEquals(name + "_1", newC.getNameC());

        // expect all 'C' objects with 1:n reference object
        // with nameC '..._%' --> 'c1' 'c2'
        crit = new Criteria();
        crit.addLike("nameC", name+"_%");
        q = QueryFactory.newQuery(ObjC.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(2, result.size());

        // expect all 'B' objects with 1:1 to an RefObject which
        // has an 1:n reference object
        // with name '..._d1' --> 'b'
        crit = new Criteria();
        crit.addEqualTo("ref.references.name", name+"_d1");
        // add this because only 'C' objects have a 1:n reference
        crit.addPathClass("ref", ObjC.class);
        q = QueryFactory.newQuery(ObjB.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());
        ObjB newB = (ObjB) result.iterator().next();
        assertNotNull(newB.getRef());
        assertTrue(newB.getRef() instanceof ObjC);
        newC = (ObjC) newB.getRef();
        assertEquals(3, newC.getReferences().size());

        // expect all 'B' objects with 1:1 to an RefObject which
        // has an 1:n reference object
        // with name '..._d1' --> 'b'
        crit = new Criteria();
        crit.addLike("ref.nameC", name+"_%");
        // add this because only 'C' objects have a 1:n reference
        crit.addPathClass("ref", ObjC.class);
        q = QueryFactory.newQuery(ObjB.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());
        newB = (ObjB) result.iterator().next();
        assertNotNull(newB.getRef());
        assertTrue(newB.getRef() instanceof ObjC);
        newC = (ObjC) newB.getRef();
        assertEquals(3, newC.getReferences().size());

        // expect all A's which have a B called '_second_1'
        crit = new Criteria();
        crit.addLike("name", name+"_%");
        crit.addEqualTo("ref.name", name+"_second_1");
        crit.addPathClass("ref", ObjB.class);
        q = QueryFactory.newQuery(ObjA.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());

        // expect all A's which have a B called '_second_1' and 
        // a C called '_third' 
        crit = new Criteria();
        crit.addLike("name", name+"_%");
        crit.addEqualTo("ref.name", name+"_second_1");
        crit.addEqualTo("ref.ref.name", name+"_third");
        crit.addPathClass("ref", ObjB.class);
        crit.addPathClass("ref.ref", ObjC.class);
        q = QueryFactory.newQuery(ObjA.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());

        // expect all A's which third level 'ref' has a 'references'
        // field collection, this is only valid for 'C' class objects
        // and references contain '..._d1' object --> 'a'
        crit = new Criteria();
        crit.addLike("name", name+"_%");
        crit.addEqualTo("ref.ref.references.name", name+"_d1");
        crit.addPathClass("ref", ObjB.class);
        crit.addPathClass("ref.ref", ObjC.class);
        q = QueryFactory.newQuery(ObjA.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            RefObject ref = (RefObject) iterator.next();
            assertTrue(ref instanceof ObjA);
            String refName = ref.getName();
            assertTrue(!(refName.indexOf(name)<0));
        }

        // expect all A's with reference object named '_second%' and
        // which third level 'ref' has a 'references'
        // field collection, this is only valid for 'C' class objects
        // and references contain '..._second%' objects --> 'a'
        crit = new Criteria();
        crit.addLike("name", name+"_%");
        crit.addLike("ref.name", name+"_second%");
        crit.addLike("ref.ref.references.name", name+"_second%");
        crit.addPathClass("ref", ObjB.class);
        crit.addPathClass("ref.ref", ObjC.class);
        q = QueryFactory.newQuery(ObjA.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            RefObject ref = (RefObject) iterator.next();
            assertTrue(ref instanceof ObjA);
            String refName = ref.getName();
            assertTrue(!(refName.indexOf(name)<0));
        }
    }

    /**
     * Test the usage of interface as class-ref in collection-descriptor
     * when using inheritance.
     */
    public void testInterfaceAsCollectionRef_2()
    {
        // if(skipKnownIssueProblem("query using path via reference, like 'ref1.ref2.name'")) return;
        String name = "testQueryWithCollectionRef_" + System.currentTimeMillis();
        RefObject a = new ObjA();
        RefObject a2 = new ObjA();
        RefObject b = new ObjB();
        RefObject b2 = new ObjB();
        // this object has a 1:n relation
        ObjC c = new ObjC();
        // only used in the c object reference collection
        RefObject d = new ObjC();

        c.setName(name+"_third");
        b.setName(name+"_second_1");
        b2.setName(name+"_second_2");
        a.setName(name+"_first_1");
        a2.setName(name+"_first_2");
        d.setName(name+"_none");

        a.setRef(b);
        b.setRef(c);
        a2.setRef(b2);

        List refList = new ArrayList();
        refList.add(a);
        refList.add(b2);
        refList.add(d);
        c.setReferences(refList);

        broker.beginTransaction();
        broker.store(a);
        broker.store(a2);
        broker.commitTransaction();

        // check existence of objects
        Criteria crit = new Criteria();
        crit.addEqualTo("name", name+"_third");
        Query q = QueryFactory.newQuery(RefObject.class, crit);
        Collection result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());
        ObjC newC = (ObjC) result.iterator().next();
        assertNotNull(newC.getReferences());
        assertEquals(3, newC.getReferences().size());

        // test n-level depth
        //*****************************************
        crit = new Criteria();
        crit.addEqualTo("ref.ref.name", name+"_third");
        q = QueryFactory.newQuery(ObjA.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());
        //*****************************************

        crit = new Criteria();
        crit.addLike("references.name", name+"_first%");
        q = QueryFactory.newQuery(ObjC.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());

        // expect all A's with name "_first_2" or with second
        // level 'ref' "_third" in this case object 'a' and 'a2'
        crit = new Criteria();
        crit.addEqualTo("name", name+"_first_2");
        Criteria critOr = new Criteria();
        critOr.addEqualTo("ref.ref.name", name+"_third");
        crit.addOrCriteria(critOr);
        q = QueryFactory.newQuery(ObjA.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(2, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            RefObject ref = (RefObject) iterator.next();
            assertTrue(ref instanceof ObjA);
            String refName = ref.getName();
            assertTrue(!(refName.indexOf(name)<0));
        }

        // expect all A's which second level 'ref' is "_third"
        // in this case object 'a'
        crit = new Criteria();
        crit.addLike("name", name+"_%");
        Criteria critAnd = new Criteria();
        critAnd.addEqualTo("ref.ref.name", name+"_third");
        crit.addAndCriteria(critAnd);
        q = QueryFactory.newQuery(ObjA.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            RefObject ref = (RefObject) iterator.next();
            assertTrue(ref instanceof ObjA);
            String refName = ref.getName();
            assertTrue(!(refName.indexOf(name)<0));
        }

        // expect all A's with first level 'ref' "_second%"
        // in this case object 'a' and 'a2'
        crit = new Criteria();
        crit.addLike("ref.name", name+"_second%");
        critAnd = new Criteria();
        critAnd.addLike("name", name+"%");
        crit.addAndCriteria(critAnd);
        q = QueryFactory.newQuery(ObjA.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(2, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            RefObject ref = (RefObject) iterator.next();
            assertTrue(ref instanceof ObjA);
            String refName = ref.getName();
            assertTrue(!(refName.indexOf(name)<0));
        }
    }

    public void testDeepPathQuery()
    {
        // if(skipKnownIssueProblem("query using path via reference, like 'ref1.ref2.name'")) return;
        String name = "testDeepPathQuery_" + System.currentTimeMillis();
        RefObject a = new ObjA();
        RefObject a2 = new ObjA();
        RefObject b = new ObjB();
        RefObject b2 = new ObjB();
        ObjC c = new ObjC();

        c.setName(name+"_third");
        b.setName(name+"_second_1");
        b2.setName(name+"_second_2");
        a.setName(name+"_first_1");
        a2.setName(name+"_first_2");

        a.setRef(b);
        b.setRef(c);
        a2.setRef(b2);

        List refList = new ArrayList();
        refList.add(a);
        refList.add(b2);
        c.setReferences(refList);

        broker.beginTransaction();
        broker.store(a);
        broker.store(a2);
        broker.commitTransaction();

        // check existence of objects
        Criteria crit = new Criteria();
        crit.addLike("name", name+"%");
        Query q = QueryFactory.newQuery(ObjA.class, crit);
        Collection result = broker.getCollectionByQuery(q);
        assertEquals(2, result.size());

        // check existence of object
        crit = new Criteria();
        crit.addLike("name", name+"_third%");
        q = QueryFactory.newQuery(ObjC.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());

        // check existence of object
        crit = new Criteria();
        crit.addLike("name", name+"%");
        q = QueryFactory.newQuery(ObjC.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());

        // test one level depth
        crit = new Criteria();
        crit.addLike("ref.name", name+"_second%");
        q = QueryFactory.newQuery(ObjA.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(2, result.size());

        // check existence of objects
        crit = new Criteria();
        crit.addLike("name", name+"%");
        q = QueryFactory.newQuery(RefObject.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(5, result.size());

        // test n-level depth
        //*****************************************
        crit = new Criteria();
        crit.addEqualTo("ref.ref.name", name+"_third");
        q = QueryFactory.newQuery(ObjA.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());
        //*****************************************

        // similar but more complex query
        crit = new Criteria();
        crit.addEqualTo("name", name+"_first_2");
        Criteria critOr = new Criteria();
        critOr.addEqualTo("ref.ref.name", name+"_third");
        crit.addOrCriteria(critOr);
        q = QueryFactory.newQuery(ObjA.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(2, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            RefObject ref = (RefObject) iterator.next();
            assertTrue(ref instanceof ObjA);
            String refName = ref.getName();
            assertTrue(!(refName.indexOf(name)<0));
        }
    }

    public void testAutoUpdateDeleteSettings()
    {
        changeRepositoryAutoSetting("ref", true, false, false);
        ObjectReferenceDescriptor ord = broker.getClassDescriptor(Repository.class)
                .getObjectReferenceDescriptorByName("ref");
        assertEquals(ObjectReferenceDescriptor.CASCADE_LINK, ord.getCascadingStore());
        assertEquals(ObjectReferenceDescriptor.CASCADE_NONE, ord.getCascadingDelete());
        assertEquals(false, ord.getCascadeStore());
        assertEquals(false, ord.getCascadeDelete());

        changeRepositoryAutoSetting("ref", true, true, true);
        ord = broker.getClassDescriptor(Repository.class).getObjectReferenceDescriptorByName("ref");
        assertEquals(ObjectReferenceDescriptor.CASCADE_OBJECT, ord.getCascadingStore());
        assertEquals(ObjectReferenceDescriptor.CASCADE_OBJECT, ord.getCascadingDelete());
        assertEquals(true, ord.getCascadeStore());
        assertEquals(true, ord.getCascadeDelete());
    }

    /**
     * not really a reference test, here we check handling of objects
     * with multiple PK fields. Such an object was used in following
     * reference tests.
     */
    public void testHandlingOfMultiplePKFields() throws Exception
    {
        String timestamp = "testLookupWithMultiplePK_" + System.currentTimeMillis();
        String regionName = "baden_" + timestamp;
        String countryName = "germany_" + timestamp;
        /*
        Wine has a 1:1 reference with Region, we set the reference object in
        Wine class. We don't set the FK fields in Wine, this should be done by OJB
        automatic
        */
        Region region = new Region(regionName, countryName, "original");

        broker.beginTransaction();
        broker.store(region);
        broker.commitTransaction();

        Identity oid = new Identity(region, broker);
        broker.clearCache();
        Region loadedRegion = (Region) broker.getObjectByIdentity(oid);

        assertNotNull(loadedRegion);
        assertEquals(region.getName(), loadedRegion.getName());

        loadedRegion.setDescription("update_1");
        broker.beginTransaction();
        broker.store(loadedRegion);
        broker.commitTransaction();
        broker.clearCache();
        loadedRegion = (Region) broker.getObjectByIdentity(oid);
        assertNotNull(loadedRegion);
        assertEquals("update_1", loadedRegion.getDescription());

        loadedRegion.setDescription("update_2");
        broker.beginTransaction();
        broker.store(loadedRegion);
        broker.commitTransaction();
        broker.clearCache();
        loadedRegion = (Region) broker.getObjectByIdentity(oid);
        assertNotNull(loadedRegion);
        assertEquals("update_2", loadedRegion.getDescription());

        Criteria crit = new Criteria();
        crit.addLike("name", regionName);
        Query q = QueryFactory.newQuery(Region.class, crit);
        Collection result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());
    }

    public void testStoreWithMultiplePK_1() throws Exception
    {
        String timestamp = "testStoreWithMultiplePK_1_" + System.currentTimeMillis();
        String regionName = "baden_1" + timestamp;
        String countryName = "germany_1" + timestamp;
        Region region = new Region(regionName, countryName, "brrr");
        Wine wine = new Wine(timestamp, "silvaner", "2003", regionName, countryName);

        broker.beginTransaction();
        broker.store(region);
        broker.commitTransaction();

        /*
        class Wine has a 1:1 reference with Region, we set set the FK in Wine but don't
        set the Region reference object in Wine. But retriveAllReferences materialize
        the reference object before store.
        */
        broker.beginTransaction();
        broker.retrieveAllReferences(wine);
        broker.store(wine);
        broker.commitTransaction();

        Identity oid = new Identity(wine, broker);
        broker.clearCache();
        Wine loadedWine = (Wine) broker.getObjectByIdentity(oid);
        assertNotNull(loadedWine);
        assertEquals(wine.getGrape(), loadedWine.getGrape());
        assertNotNull(loadedWine.getRegion());
        assertEquals(wine.getRegion().getCountry(), loadedWine.getRegion().getCountry());
    }

    public void testStoreWithMultiplePK_2() throws Exception
    {
        String timestamp = "testStoreWithMultiplePK_2_" + System.currentTimeMillis();
        String regionName = "baden_2" + timestamp;
        String countryName = "germany_2" + timestamp;
        /*
        Wine has a 1:1 reference with Region, we set the reference object in
        Wine class. We don't set the FK fields in Wine, this should be done by OJB
        automatic
        */
        Region region = new Region(regionName, countryName, "brrr");
        Wine wine = new Wine(timestamp, "silvaner", "2003", null, null);
        wine.setRegion(region);

        broker.beginTransaction();
        broker.store(region);
        broker.commitTransaction();

        broker.beginTransaction();
        broker.store(wine);
        broker.commitTransaction();

        Identity oid = new Identity(wine, broker);
        broker.clearCache();
        Wine loadedWine = (Wine) broker.getObjectByIdentity(oid);
        assertNotNull(loadedWine);
        assertEquals(wine.getGrape(), loadedWine.getGrape());
        assertNotNull(loadedWine.getRegion());
        assertEquals(wine.getRegion().getCountry(), loadedWine.getRegion().getCountry());
    }

    public void testDeleteWithMultiplePK()
    {
        String timestamp = "testDeleteWithMultiplePK_" + System.currentTimeMillis();
        String regionName = "baden_2" + timestamp;
        String countryName = "germany_2" + timestamp;

        /*
        Wine has a 1:1 reference with Region, we set the reference object in
        Wine class. We don't set the FK fields in Wine, this should be done by OJB
        automatic
        */
        Region region = new Region(regionName, countryName, "brrr");
        Wine wine = new Wine(timestamp, "silvaner", "2003", null, null);
        wine.setRegion(region);

        broker.beginTransaction();
        broker.store(region);
        broker.commitTransaction();

        broker.beginTransaction();
        broker.store(wine);
        broker.commitTransaction();

        Identity oid = new Identity(wine, broker);
        Identity oidRegion = new Identity(region, broker);
        broker.clearCache();
        Wine loadedWine = (Wine) broker.getObjectByIdentity(oid);
        assertNotNull(loadedWine);
        assertEquals(wine.getGrape(), loadedWine.getGrape());
        assertNotNull(loadedWine.getRegion());
        assertEquals(wine.getRegion().getCountry(), loadedWine.getRegion().getCountry());

        broker.beginTransaction();
        broker.delete(wine);
        broker.commitTransaction();

        loadedWine = (Wine) broker.getObjectByIdentity(oid);
        assertNull(loadedWine);
        Region loadedregion = (Region) broker.getObjectByIdentity(oidRegion);
        assertNotNull(loadedregion);

        broker.clearCache();
        loadedWine = (Wine) broker.getObjectByIdentity(oid);
        assertNull(loadedWine);
        loadedregion = (Region) broker.getObjectByIdentity(oidRegion);
        assertNotNull(loadedregion);
    }

    public void testStoreWithMultiplePK_3() throws Exception
    {
        String timestamp = "testStoreWithMultiplePK_3_" + System.currentTimeMillis();
        String regionName = "baden_3" + timestamp;
        String countryName = "germany_3" + timestamp;
        /*
        Wine has a 1:1 reference with Region, we set set the FK fields
        of an existing Region object in Wine
        but don't set the Region reference object itself in Wine object
        */
        Region region = new Region(regionName, countryName, "brrr");
        Wine wine = new Wine(timestamp, "silvaner", "2003", regionName, countryName);
        wine.setRegion(region);

        broker.beginTransaction();
        broker.store(region);
        broker.commitTransaction();

        broker.beginTransaction();
        broker.store(wine);
        broker.commitTransaction();

        Identity oid = new Identity(wine, broker);
        broker.clearCache();
        Wine loadedWine = (Wine) broker.getObjectByIdentity(oid);
        assertNotNull(loadedWine);
        assertEquals(wine.getGrape(), loadedWine.getGrape());
        assertNotNull(loadedWine.getRegion());
        assertEquals(wine.getRegion().getCountry(), loadedWine.getRegion().getCountry());
    }

    public void testStoreReferencesMappedToSameTable()
    {
        String referenceNamePrefix = "testStoreReferencesMappedToSameTable" + System.currentTimeMillis();
        Repository[] repository = prepareRepository(referenceNamePrefix);

        broker.beginTransaction();
        broker.store(repository[0]);
        broker.store(repository[1]);
        broker.store(repository[2]);
        broker.commitTransaction();

        broker.clearCache();
        Identity oid = new Identity(repository[0], broker);

        Repository rep = (Repository) broker.getObjectByIdentity(oid);
        assertNotNull(rep.getRef());
        assertNotNull(rep.getRefA());
        assertNotNull(rep.getRefB());
        // lookup reference name, set in prepareRepository method
        assertEquals(rep.getRefB().getRefNameB(), REF_TEST_STRING);
    }

    public void testGetReferencesByIdentityMappedToSameTable()
    {
        String referenceNamePrefix = "testGetReferencesByIdentityMappedToSameTable" + System.currentTimeMillis();
        Repository[] repository = prepareRepository(referenceNamePrefix);

        broker.beginTransaction();
        broker.store(repository[0]);
        broker.store(repository[1]);
        broker.store(repository[2]);
        broker.commitTransaction();

        assertNotNull(repository[0].getRef());
        assertNotNull(repository[0].getRefA());
        assertNotNull(repository[0].getRefB());

        Identity oid_ref = new Identity(repository[0].getRef(), broker);
        Identity oid_refA = new Identity(repository[0].getRefA(), broker);
        Identity oid_refB = new Identity(repository[0].getRefB(), broker);

        broker.clearCache();
        Object result;
        result = broker.getObjectByIdentity(oid_ref);
        assertTrue(result instanceof Reference);
        result = broker.getObjectByIdentity(oid_refA);
        assertTrue(result instanceof ReferenceA);
        result = broker.getObjectByIdentity(oid_refB);
        assertTrue(result instanceof ReferenceB);

        broker.clearCache();
        Identity repOID = new Identity(repository[0], broker);
        Repository repositoryObj = (Repository) broker.getObjectByIdentity(repOID);
        assertNotNull(repositoryObj);
        ReferenceBIF refB = repositoryObj.getRefB();
        assertNotNull(refB);
        assertEquals(refB.getRefNameB(), REF_TEST_STRING);
    }

    public void testQueryReferencesMappedToSameTable()
    {
        String referenceNamePrefix = "testQueryReferencesMappedToSameTable" + System.currentTimeMillis();
        Repository[] repository = prepareRepository(referenceNamePrefix);

        broker.beginTransaction();
        broker.store(repository[0]);
        broker.store(repository[1]);
        broker.store(repository[2]);
        broker.commitTransaction();

        broker.clearCache();
        Criteria criteria = new Criteria();
        criteria.addLike("name", referenceNamePrefix + "%");
        Query query = QueryFactory.newQuery(ReferenceIF.class, criteria);
        Collection result = broker.getCollectionByQuery(query);

        assertEquals("Wrong number of References", 9, result.size());
        int ref_count = 0;
        int refA_count = 0;
        int refB_count = 0;
        Iterator it = result.iterator();
        Object obj;
        while(it.hasNext())
        {
            obj = it.next();
            if(obj instanceof ReferenceA)
                refA_count++;
            else if(obj instanceof ReferenceB)
                refB_count++;
            else if(obj instanceof Reference) ref_count++;
        }
        assertEquals("Wrong number of RefernceA", 3, refA_count);
        assertEquals("Wrong number of RefernceB", 3, refB_count);
        assertEquals("Wrong number of Refernce", 3, ref_count);

        result = broker.getCollectionByQuery(query);
        it = result.iterator();
        while(it.hasNext())
        {
            obj = it.next();
            if(obj instanceof ReferenceA)
            {
                assertNotNull(((ReferenceA) obj).getRefNameA());
                assertNotNull(((ReferenceA) obj).getName());
            }
            else if(obj instanceof ReferenceB)
            {
                assertNotNull(((ReferenceB) obj).getRefNameB());
                assertNotNull(((ReferenceB) obj).getName());
            }
            else if(obj instanceof Reference)
            {
                assertNotNull(((Reference) obj).getName());
            }
        }
    }

    public void testDeleteReferencesMappedToSameTable()
    {
        String referenceNamePrefix = "testDeleteReferencesMappedToSameTable" + System.currentTimeMillis();
        Repository[] repository = prepareRepository(referenceNamePrefix);

        broker.beginTransaction();
        broker.store(repository[0]);
        broker.store(repository[1]);
        broker.store(repository[2]);
        broker.commitTransaction();

        Criteria criteria = new Criteria();
        criteria.addLike("name", referenceNamePrefix + "%");
        Query query = QueryFactory.newQuery(ReferenceIF.class, criteria);
        Collection result = broker.getCollectionByQuery(query);

        assertEquals("Wrong number of References", 9, result.size());

        broker.beginTransaction();
        broker.delete(repository[0]);
        broker.delete(repository[1]);
        broker.delete(repository[2]);
        broker.commitTransaction();

        result = broker.getCollectionByQuery(query);
        assertEquals("Wrong number of References", 0, result.size());
    }

    public void testDeleteReferencesMappedToSameTable_2()
    {
        String referenceNamePrefix = "testDeleteReferencesMappedToSameTable_2" + System.currentTimeMillis();
        changeRepositoryAutoSetting("ref", true, true, false);
        changeRepositoryAutoSetting("refA", true, true, false);
        changeRepositoryAutoSetting("refB", true, true, false);

        Repository[] repository = prepareRepository(referenceNamePrefix);

        broker.beginTransaction();
        broker.store(repository[0]);
        broker.store(repository[1]);
        broker.store(repository[2]);
        broker.commitTransaction();

        Criteria criteria = new Criteria();
        criteria.addLike("name", referenceNamePrefix + "%");
        Query query = QueryFactory.newQuery(ReferenceIF.class, criteria);
        Collection result = broker.getCollectionByQuery(query);

        assertEquals("Wrong number of References", 9, result.size());

        broker.beginTransaction();
        broker.delete(repository[0]);
        broker.delete(repository[1]);
        broker.delete(repository[2]);
        broker.commitTransaction();

        result = broker.getCollectionByQuery(query);
        assertEquals("Wrong number of References", 9, result.size());
    }

    public void testDeleteReferencesMappedToSameTable_3()
    {
        String referenceNamePrefix = "testDeleteReferencesMappedToSameTable_3" + System.currentTimeMillis();
        changeRepositoryAutoSetting("ref", true, true, false);
        changeRepositoryAutoSetting("refA", true, true, true);
        changeRepositoryAutoSetting("refB", true, true, false);

        Repository[] repository = prepareRepository(referenceNamePrefix);

        broker.beginTransaction();
        broker.store(repository[0]);
        broker.store(repository[1]);
        broker.store(repository[2]);
        broker.commitTransaction();

        Criteria criteria = new Criteria();
        criteria.addLike("name", referenceNamePrefix + "%");
        Query query = QueryFactory.newQuery(ReferenceIF.class, criteria);
        Collection result = broker.getCollectionByQuery(query);

        assertEquals("Wrong number of References", 9, result.size());

        broker.beginTransaction();
        broker.delete(repository[0]);
        broker.delete(repository[1]);
        broker.delete(repository[2]);
        broker.commitTransaction();

        result = broker.getCollectionByQuery(query);
        assertEquals("Wrong number of References", 6, result.size());
    }

    private void changeRepositoryAutoSetting(String attributeName, boolean retrieve, int update, int delete)
    {
        ClassDescriptor cld = broker.getClassDescriptor(Repository.class);
        ObjectReferenceDescriptor ord = cld.getObjectReferenceDescriptorByName(attributeName);
        ord.setCascadeRetrieve(retrieve);
        ord.setCascadingStore(update);
        ord.setCascadingDelete(delete);
    }

    private void changeRepositoryAutoSetting(String attributeName, boolean retrieve, boolean update, boolean delete)
    {
        ClassDescriptor cld = broker.getClassDescriptor(Repository.class);
        ObjectReferenceDescriptor ord = cld.getObjectReferenceDescriptorByName(attributeName);
        ord.setCascadeRetrieve(retrieve);
        ord.setCascadeStore(update);
        ord.setCascadeDelete(delete);
    }

    /**
     * This test does the same as the {@link #testRepositoryFKStore},
     * but the used mapping data differ.
     * {@link RepositoryFK} defines all the reference fields as
     * primary key in field-descriptors. Further on the used
     * database table declares the reference fields as PK too.
     * Based on a user post:
     * > The following fails to be stored by PersistenceBroker:
     >
     > I have a class ACL which has two primary keys: objectId and userFK, and
     > userFK is also a foreign key tied to a reference of type User. If I do
     this:
     >
     > persistentBroker.beginTransaction();
     > ACL acl = new ACL();
     > acl.setObjectId( 100 );
     > acl.setUser( currentUser );
     > persistentBroker.store(acl);
     > persistentBroker.commitTransaction();
     >
     > Acl will not be saved. The reason seems to be because in the storeToDb()
     > method of the PersistentBroker, there first comes an assertion of the
     > PrimaryKeys and afterwards comes the assignment of all the foreign keys.
     In
     > the scenario above the assertion of the primary keys will fail, because
     the
     > userFK has not been assigned yet, so we have an incomplete set of primary
     > keys. This does work with the ODMG layer, probably because of a different
     > sequence of events during the storing of the object.
     >
     > I wonder if there should be a check whether a primary key is shared by the
     > foreign key and allow that assignment before the assertion of the primary
     > keys is performed. Any ideas?
     >
     > Cheers,
     > --Bill.
     */
    public void testRepositoryFKStore()
    {
        String referenceNamePrefix = "testReferenceStore" + System.currentTimeMillis();
        RepositoryFK[] repository = prepareRepositoryFK(referenceNamePrefix);

        broker.beginTransaction();
        broker.store(repository[0]);
        broker.store(repository[1]);
        broker.store(repository[2]);
        broker.commitTransaction();

        Identity oid = new Identity(repository[0], broker);
        RepositoryFK repFK = (RepositoryFK) broker.getObjectByIdentity(oid);

        assertNotNull("We should found a RepositoryFK object, but doesn't.", repFK);
        assertNotNull(repFK.getRef());
        assertNotNull(repFK.getRefA());
        assertNotNull(repFK.getRefB());
    }

    /**
     * this test case use an abstract class as reference
     * @throws Exception
     */
    public void testAbstractReferenceStore() throws Exception
    {
        String name = "testAbstractReferenceStore_" + System.currentTimeMillis();
        /*
        create some animals
        */
        Bird bird_1 = new Bird();
        bird_1.setName(name);
        bird_1.setWingspan(new Double(2.33));
        Bird bird_2 = new Bird();
        bird_2.setName(name);
        bird_2.setWingspan(new Double(0.99));

        Mammal mammal_1 = new Mammal();
        mammal_1.setName(name);
        mammal_1.setHeight(new Double(1.88));
        Mammal mammal_2 = new Mammal();
        mammal_2.setName(name);
        mammal_2.setHeight(new Double(19.13));

        Fish fish_1 = new Fish();
        fish_1.setName(name);
        fish_1.setLength(new Double(0.033));
        Fish fish_2 = new Fish();
        fish_2.setName(name);
        fish_2.setLength(new Double(37.89));

        Repository rep_1 = new Repository();
        rep_1.setAnimal(mammal_1);
        Repository rep_2 = new Repository();
        rep_2.setAnimal(bird_1);
        Repository rep_3 = new Repository();
        rep_3.setAnimal(fish_1);

        /*
        store Repository instances and dummy animals
        */
        broker.beginTransaction();
        // store some dummy objects
        broker.store(bird_2);
        broker.store(mammal_2);
        broker.store(fish_2);
        // now store the main objects
        broker.store(rep_1);
        broker.store(rep_2);
        broker.store(rep_3);
        broker.commitTransaction();

        Identity oid_mammal = new Identity(mammal_1, broker);
        Identity oid_bird = new Identity(bird_1, broker);
        Identity oid_fish = new Identity(fish_1, broker);
        Identity oid_rep_1 = new Identity(rep_1, broker);
        Identity oid_rep_2 = new Identity(rep_2, broker);
        Identity oid_rep_3 = new Identity(rep_3, broker);

        broker.clearCache();
        // check the references
        Mammal lookup_mammal = (Mammal) broker.getObjectByIdentity(oid_mammal);
        Bird lookup_bird = (Bird) broker.getObjectByIdentity(oid_bird);
        Fish lookup_fish = (Fish) broker.getObjectByIdentity(oid_fish);
        assertEquals(mammal_1, lookup_mammal);
        assertEquals(bird_1, lookup_bird);
        assertEquals(fish_1, lookup_fish);

        broker.clearCache();
        // check the main objects
        Repository lookup_rep_1 = (Repository) broker.getObjectByIdentity(oid_rep_1);
        Repository lookup_rep_2 = (Repository) broker.getObjectByIdentity(oid_rep_2);
        Repository lookup_rep_3 = (Repository) broker.getObjectByIdentity(oid_rep_3);

        assertNotNull(lookup_rep_1.getAnimal());
        assertTrue("Expected instance of Mammal, found " + lookup_rep_1.getAnimal(),
                lookup_rep_1.getAnimal() instanceof Mammal);
        assertEquals(mammal_1, lookup_rep_1.getAnimal());

        assertNotNull(lookup_rep_2.getAnimal());
        assertTrue("Expected instance of Bird, found " + lookup_rep_2.getAnimal(),
                lookup_rep_2.getAnimal() instanceof Bird);
        assertEquals(bird_1, lookup_rep_2.getAnimal());

        assertNotNull(lookup_rep_3.getAnimal());
        assertTrue("Expected instance of Fish, found " + lookup_rep_3.getAnimal(),
                lookup_rep_3.getAnimal() instanceof Fish);
        assertEquals(fish_1, lookup_rep_3.getAnimal());
    }

    public void testAbstractReferenceQuery() throws Exception
    {
        String name = "testAbstractReferenceQuery_" + System.currentTimeMillis();
        /*
        create some animals
        */
        Bird bird_1 = new Bird();
        bird_1.setName(name);
        bird_1.setWingspan(new Double(2.33));
        Bird bird_2 = new Bird();
        bird_2.setName(name);
        bird_2.setWingspan(new Double(0.99));

        Mammal mammal_1 = new Mammal();
        mammal_1.setName(name);
        mammal_1.setHeight(new Double(1.88));
        Mammal mammal_2 = new Mammal();
        mammal_2.setName(name);
        mammal_2.setHeight(new Double(19.13));

        Fish fish_1 = new Fish();
        fish_1.setName(name);
        fish_1.setLength(new Double(0.033));
        Fish fish_2 = new Fish();
        fish_2.setName(name);
        fish_2.setLength(new Double(37.89));

        Repository rep_1 = new Repository();
        rep_1.setAnimal(mammal_1);
        Repository rep_2 = new Repository();
        rep_2.setAnimal(bird_1);
        Repository rep_3 = new Repository();
        rep_3.setAnimal(fish_1);

        /*
        store Repository instances and dummy animals
        */
        broker.beginTransaction();
        // store some dummy objects
        broker.store(bird_2);
        broker.store(mammal_2);
        broker.store(fish_2);
        // now store the main objects
        broker.store(rep_1);
        broker.store(rep_2);
        broker.store(rep_3);
        broker.commitTransaction();

        Identity oid_rep_1 = new Identity(rep_1, broker);
        Identity oid_rep_2 = new Identity(rep_2, broker);
        Identity oid_rep_3 = new Identity(rep_3, broker);

        broker.clearCache();
        // check the main objects
        Repository lookup_rep_1 = (Repository) broker.getObjectByIdentity(oid_rep_1);
        Repository lookup_rep_2 = (Repository) broker.getObjectByIdentity(oid_rep_2);
        Repository lookup_rep_3 = (Repository) broker.getObjectByIdentity(oid_rep_3);

        assertNotNull(lookup_rep_1.getAnimal());
        assertTrue("Expected instance of Mammal, found " + lookup_rep_1.getAnimal(),
                lookup_rep_1.getAnimal() instanceof Mammal);
        assertEquals(mammal_1, lookup_rep_1.getAnimal());

        assertNotNull(lookup_rep_2.getAnimal());
        assertTrue("Expected instance of Bird, found " + lookup_rep_2.getAnimal(),
                lookup_rep_2.getAnimal() instanceof Bird);
        assertEquals(bird_1, lookup_rep_2.getAnimal());

        assertNotNull(lookup_rep_3.getAnimal());
        assertTrue("Expected instance of Fish, found " + lookup_rep_3.getAnimal(),
                lookup_rep_3.getAnimal() instanceof Fish);
        assertEquals(fish_1, lookup_rep_3.getAnimal());

        broker.clearCache();
        // query the references
        Criteria crit = new Criteria();
        crit.addEqualTo("name", name);
        Query query = QueryFactory.newQuery(Animal.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertNotNull(result);
        int[] mammalBirdFish = new int[3];
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            if(o instanceof Mammal) ++mammalBirdFish[0];
            if(o instanceof Bird) ++mammalBirdFish[1];
            if(o instanceof Fish) ++mammalBirdFish[2];
        }
        assertEquals(2, mammalBirdFish[0]);
        assertEquals(2, mammalBirdFish[1]);
        assertEquals(2, mammalBirdFish[2]);
    }

    /**
     * this test case use an abstract class as reference
     * @throws Exception
     */
    public void testAbstractReferenceDelete() throws Exception
    {
        String name = "testAbstractReferenceDelete_" + System.currentTimeMillis();
        /*
        create some animals
        */
        Bird bird_1 = new Bird();
        bird_1.setName(name);
        bird_1.setWingspan(new Double(2.33));
        Bird bird_2 = new Bird();
        bird_2.setName(name);
        bird_2.setWingspan(new Double(0.99));

        Mammal mammal_1 = new Mammal();
        mammal_1.setName(name);
        mammal_1.setHeight(new Double(1.88));
        Mammal mammal_2 = new Mammal();
        mammal_2.setName(name);
        mammal_2.setHeight(new Double(19.13));

        Fish fish_1 = new Fish();
        fish_1.setName(name);
        fish_1.setLength(new Double(0.033));
        Fish fish_2 = new Fish();
        fish_2.setName(name);
        fish_2.setLength(new Double(37.89));

        Repository rep_1 = new Repository();
        rep_1.setAnimal(mammal_1);
        Repository rep_2 = new Repository();
        rep_2.setAnimal(bird_1);
        Repository rep_3 = new Repository();
        rep_3.setAnimal(fish_1);

        /*
        store Repository instances and dummy animals
        */
        broker.beginTransaction();
        // store some dummy objects
        broker.store(bird_2);
        broker.store(mammal_2);
        broker.store(fish_2);
        // now store the main objects
        broker.store(rep_1);
        broker.store(rep_2);
        broker.store(rep_3);
        broker.commitTransaction();

        Identity oid_rep_1 = new Identity(rep_1, broker);
        Identity oid_rep_2 = new Identity(rep_2, broker);
        Identity oid_rep_3 = new Identity(rep_3, broker);

        broker.clearCache();
        // check the main objects
        Repository lookup_rep_1 = (Repository) broker.getObjectByIdentity(oid_rep_1);
        Repository lookup_rep_2 = (Repository) broker.getObjectByIdentity(oid_rep_2);
        Repository lookup_rep_3 = (Repository) broker.getObjectByIdentity(oid_rep_3);

        assertNotNull(lookup_rep_1.getAnimal());
        assertTrue("Expected instance of Mammal, found " + lookup_rep_1.getAnimal(),
                lookup_rep_1.getAnimal() instanceof Mammal);
        assertEquals(mammal_1, lookup_rep_1.getAnimal());

        assertNotNull(lookup_rep_2.getAnimal());
        assertTrue("Expected instance of Bird, found " + lookup_rep_2.getAnimal(),
                lookup_rep_2.getAnimal() instanceof Bird);
        assertEquals(bird_1, lookup_rep_2.getAnimal());

        assertNotNull(lookup_rep_3.getAnimal());
        assertTrue("Expected instance of Fish, found " + lookup_rep_3.getAnimal(),
                lookup_rep_3.getAnimal() instanceof Fish);
        assertEquals(fish_1, lookup_rep_3.getAnimal());

        broker.clearCache();

        broker.beginTransaction();
        broker.delete(rep_1);
        broker.delete(rep_2);
        broker.delete(rep_3);
        broker.commitTransaction();

        lookup_rep_1 = (Repository) broker.getObjectByIdentity(oid_rep_1);
        lookup_rep_2 = (Repository) broker.getObjectByIdentity(oid_rep_2);
        lookup_rep_3 = (Repository) broker.getObjectByIdentity(oid_rep_3);
        assertNull(lookup_rep_1);
        assertNull(lookup_rep_2);
        assertNull(lookup_rep_3);

    }

    private Repository[] prepareRepository(String referenceNamePrefix)
    {
        Reference[] ref = new Reference[]{
            new Reference(referenceNamePrefix + "ref_1"),
            new Reference(referenceNamePrefix + "ref_2"),
            new Reference(referenceNamePrefix + "ref_3")};
        ReferenceA[] refA = new ReferenceA[]{
            new ReferenceA(referenceNamePrefix + "refA_1", "a1"),
            new ReferenceA(referenceNamePrefix + "refA_2", "a2"),
            new ReferenceA(referenceNamePrefix + "refA_3", "a3")};
        ReferenceB[] refB = new ReferenceB[]{
            new ReferenceB(referenceNamePrefix + "refB_1", REF_TEST_STRING),
            new ReferenceB(referenceNamePrefix + "refB_2", "b2"),
            new ReferenceB(referenceNamePrefix + "refB_3", "b3")};

        Repository repository = new Repository();
        repository.setRef(ref[0]);
        repository.setRefA(refA[0]);
        repository.setRefB(refB[0]);

        Repository repository2 = new Repository();
        repository2.setRef(ref[1]);
        repository2.setRefA(refA[1]);
        repository2.setRefB(refB[1]);

        Repository repository3 = new Repository();
        repository3.setRef(ref[2]);
        repository3.setRefA(refA[2]);
        repository3.setRefB(refB[2]);

        return new Repository[]{repository, repository2, repository3};
    }

    private RepositoryFK[] prepareRepositoryFK(String referenceNamePrefix)
    {
        Reference[] ref = new Reference[]{
            new Reference(referenceNamePrefix + "ref_1"),
            new Reference(referenceNamePrefix + "ref_2"),
            new Reference(referenceNamePrefix + "ref_3")};
        ReferenceA[] refA = new ReferenceA[]{
            new ReferenceA(referenceNamePrefix + "refA_1", "a1"),
            new ReferenceA(referenceNamePrefix + "refA_2", "a2"),
            new ReferenceA(referenceNamePrefix + "refA_3", "a3")};
        ReferenceB[] refB = new ReferenceB[]{
            new ReferenceB(referenceNamePrefix + "refB_1", REF_TEST_STRING),
            new ReferenceB(referenceNamePrefix + "refB_2", "b2"),
            new ReferenceB(referenceNamePrefix + "refB_3", "b3")};

        RepositoryFK repository = new RepositoryFK();
        repository.setRef(ref[0]);
        repository.setRefA(refA[0]);
        repository.setRefB(refB[0]);

        RepositoryFK repository2 = new RepositoryFK();
        repository2.setRef(ref[1]);
        repository2.setRefA(refA[1]);
        repository2.setRefB(refB[1]);

        RepositoryFK repository3 = new RepositoryFK();
        repository3.setRef(ref[2]);
        repository3.setRefA(refA[2]);
        repository3.setRefB(refB[2]);

        return new RepositoryFK[]{repository, repository2, repository3};
    }

    public void testMassOperations()
    {
        broker.beginTransaction();
        for (int i = 1; i < 100; i++)
        {

            ProductGroup pg = new ProductGroup();
            pg.setGroupName("1-1 test productgroup_" + i);
            broker.store(pg);

            Article article = Article.createInstance();
            article.setArticleName("1-1 test article_" + i);
            article.setProductGroupId(pg.getGroupId());

            broker.retrieveReference(article, "productGroup");
            broker.store(article);
        }
        broker.commitTransaction();
    }




//***************************************************************************
// Inner classes used by the test case
//***************************************************************************

    public static class Repository implements Serializable
    {
        private Integer repId;

        private Integer refId;
        private Integer refAId;
        private Integer refBId;

        private ReferenceIF ref;
        private ReferenceAIF refA;
        private ReferenceBIF refB;

        private Animal animal;
        private Integer animalId;

        public Repository()
        {
        }

        public Integer getRefId()
        {
            return refId;
        }

        public void setRefId(Integer refId)
        {
            this.refId = refId;
        }

        public Integer getRefAId()
        {
            return refAId;
        }

        public void setRefAId(Integer refAId)
        {
            this.refAId = refAId;
        }

        public Integer getRefBId()
        {
            return refBId;
        }

        public void setRefBId(Integer refBId)
        {
            this.refBId = refBId;
        }

        public Animal getAnimal()
        {
            return animal;
        }

        public void setAnimal(Animal animal)
        {
            this.animal = animal;
        }

        public Integer getAnimalId()
        {
            return animalId;
        }

        public void setAnimalId(Integer animalId)
        {
            this.animalId = animalId;
        }

        public ReferenceIF getRef()
        {
            return ref;
        }

        public void setRef(ReferenceIF ref)
        {
            this.ref = ref;
        }

        public ReferenceAIF getRefA()
        {
            return refA;
        }

        public void setRefA(ReferenceAIF refA)
        {
            this.refA = refA;
        }

        public ReferenceBIF getRefB()
        {
            return refB;
        }

        public void setRefB(ReferenceBIF refB)
        {
            this.refB = refB;
        }

        public Integer getRepId()
        {
            return repId;
        }

        public void setRepId(Integer repId)
        {
            this.repId = repId;
        }
    }

    //***************************************************************
    // classes mapped to one table
    //***************************************************************

    public static class RepositoryFK extends Repository
    {
    }

    public static interface ReferenceIF extends Serializable
    {
        Integer getRefId();

        void setRefId(Integer refId);

        String getName();

        void setName(String name);
    }

    public static interface ReferenceAIF extends Serializable
    {
        String getRefNameA();

        void setRefNameA(String name);
    }

    public static interface ReferenceBIF extends Serializable
    {
        String getRefNameB();

        void setRefNameB(String name);
    }

    public static class Reference implements ReferenceIF
    {
        protected String ojbConcreteClass;
        private Integer refId;
        private String name;

        public Reference()
        {
            this(null);
        }

        public Reference(String name)
        {
            this.name = name;
            ojbConcreteClass = this.getClass().getName();
        }

        public String getOjbConcreteClass()
        {
            return ojbConcreteClass;
        }

        public void setOjbConcreteClass(String ojbConcreteClass)
        {
            this.ojbConcreteClass = ojbConcreteClass;
        }

        public Integer getRefId()
        {
            return refId;
        }

        public void setRefId(Integer refId)
        {
            this.refId = refId;
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

    public static class ReferenceA extends Reference implements ReferenceAIF
    {
        private String refNameA;

        public ReferenceA()
        {
            super();
        }

        public ReferenceA(String name, String refNameA)
        {
            super(name);
            this.refNameA = refNameA;
        }

        public String getRefNameA()
        {
            return refNameA;
        }

        public void setRefNameA(String refName)
        {
            this.refNameA = refName;
        }
    }

    public static class ReferenceB extends Reference implements ReferenceBIF
    {
        private String refNameB;

        public ReferenceB()
        {
            super();
        }

        public ReferenceB(String name, String refNameB)
        {
            super(name);
            this.refNameB = refNameB;
        }

        public String getRefNameB()
        {
            return refNameB;
        }

        public void setRefNameB(String refName)
        {
            this.refNameB = refName;
        }
    }

    //***************************************************************
    // classes mapped to multiple tables
    //***************************************************************

    public static abstract class Animal
    {
        private Integer id;
        private String name;

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public boolean equals(Object obj)
        {
            if(!(obj instanceof Animal)) return false;
            Animal animal = (Animal) obj;
            boolean result;
            result = name == null ? (animal.getName() == null) : name.equals(animal.getName());
            result = result && (id == null ? animal.getId() == null : id.equals(animal.getId()));
            return result;
        }
    }

    public static class Mammal extends Animal
    {
        private Double height;
        private String ojbConcreteClass;

        public Mammal()
        {
            ojbConcreteClass = Mammal.class.getName();
        }

        public Double getHeight()
        {
            return height;
        }

        public void setHeight(Double height)
        {
            this.height = height;
        }

        public String getOjbConcreteClass()
        {
            return ojbConcreteClass;
        }

        public void setOjbConcreteClass(String ojbConcreteClass)
        {
            this.ojbConcreteClass = ojbConcreteClass;
        }

        public boolean equals(Object obj)
        {
            if(!(obj instanceof Mammal)) return false;
            Mammal m = (Mammal) obj;
            boolean result = super.equals(obj);
            result = result && (height == null ? m.getHeight() == null : height.equals(m.getHeight()));
            return result;
        }
    }

    public static class Bird extends Animal
    {
        private Double wingspan;
        private String ojbConcreteClass;

        public Bird()
        {
            ojbConcreteClass = Bird.class.getName();
        }

        public boolean equals(Object obj)
        {
            if(!(obj instanceof Bird)) return false;
            Bird m = (Bird) obj;
            boolean result = super.equals(obj);
            result = result && (wingspan == null ? m.getWingspan() == null : wingspan.equals(m.getWingspan()));
            return result;
        }

        public Double getWingspan()
        {
            return wingspan;
        }

        public void setWingspan(Double wingspan)
        {
            this.wingspan = wingspan;
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

    public static class Fish extends Animal
    {
        private Double length;
        private String ojbConcreteClass;

        public Fish()
        {
            ojbConcreteClass = Fish.class.getName();
        }

        public boolean equals(Object obj)
        {
            if(!(obj instanceof Fish)) return false;
            Fish m = (Fish) obj;
            boolean result = super.equals(obj);
            result = result && (length == null ? m.getLength() == null : length.equals(m.getLength()));
            return result;
        }

        public String getOjbConcreteClass()
        {
            return ojbConcreteClass;
        }

        public void setOjbConcreteClass(String ojbConcreteClass)
        {
            this.ojbConcreteClass = ojbConcreteClass;
        }

        public Double getLength()
        {
            return length;
        }

        public void setLength(Double length)
        {
            this.length = length;
        }
    }


    //***************************************************************
    // classes for test with multiple, non-autoincrement PK
    //***************************************************************
    public static class Wine
    {
        private String id;
        private String grape;
        private String year;
        private String regionName;
        private String regionCountry;
        private Region region;

        public Wine()
        {
        }

        public Wine(String id, String grape, String year, String regionName, String regionCountry)
        {
            this.id = id;
            this.grape = grape;
            this.year = year;
            this.regionName = regionName;
            this.regionCountry = regionCountry;
        }

        public Region getRegion()
        {
            return region;
        }

        public void setRegion(Region region)
        {
            this.region = region;
        }

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public String getGrape()
        {
            return grape;
        }

        public void setGrape(String grape)
        {
            this.grape = grape;
        }

        public String getYear()
        {
            return year;
        }

        public void setYear(String year)
        {
            this.year = year;
        }

        public String getRegionName()
        {
            return regionName;
        }

        public void setRegionName(String regionName)
        {
            this.regionName = regionName;
        }

        public String getRegionCountry()
        {
            return regionCountry;
        }

        public void setRegionCountry(String regionCountry)
        {
            this.regionCountry = regionCountry;
        }

        public String toString()
        {
            return new ToStringBuilder(this)
                    .append("id", id)
                    .append("grape", grape)
                    .append("regionCountry", regionCountry)
                    .append("regionName", regionName)
                    .append("year", year)
                    .append("region", region)
                    .toString();
        }
    }

    public static class Region
    {
        private String name;
        private String country;
        private String description;

        public Region()
        {
        }

        public Region(String name, String country, String description)
        {
            this.name = name;
            this.country = country;
            this.description = description;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getCountry()
        {
            return country;
        }

        public void setCountry(String country)
        {
            this.country = country;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public String toString()
        {
            return new ToStringBuilder(this)
                    .append("country", country)
                    .append("name", name)
                    .append("description", description)
                    .toString();
        }
    }

    public static interface RefObject
    {
        Integer getId();
        void setId(Integer id);
        String getName();
        void setName(String name);
        RefObject getRef();
        void setRef(RefObject ref);
        Integer getFkColRef();
        void setFkColRef(Integer id);
    }

    public static class ObjA implements RefObject
    {
        Integer id;
        String name;
        RefObject ref;
        Integer fkColRef;

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public Integer getFkColRef()
        {
            return fkColRef;
        }

        public void setFkColRef(Integer fkColRef)
        {
            this.fkColRef = fkColRef;
        }

        public RefObject getRef()
        {
            return ref;
        }

        public void setRef(RefObject ref)
        {
            this.ref = ref;
        }
    }

    public static class ObjB extends ObjA
    {

    }

    public static class ObjC extends ObjA
    {
        String nameC;
        List references;

        public String getNameC()
        {
            return nameC;
        }

        public void setNameC(String nameC)
        {
            this.nameC = nameC;
        }

        public List getReferences()
        {
            return references;
        }

        public void setReferences(List references)
        {
            this.references = references;
        }
    }
}
