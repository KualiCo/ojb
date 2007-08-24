package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.SerializationUtils;
import org.apache.ojb.broker.ObjectRepository.F1;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

/**
 * This TestClass tests the anonymous fields feature.
 */
public class AnonymousFieldsTest extends PBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {AnonymousFieldsTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testHandlingOfMultipleAnonymousFieldPerObject()
    {
        String prefix = "testHandlingOfMultipleAnonymousFieldPerObject_" + System.currentTimeMillis() + "_";

        ObjectRepository.ComponentIF parent = new ObjectRepository.Component();
        parent.setName(prefix + "main_component");

        ObjectRepository.ComponentIF compSub1 = new ObjectRepository.Component();
        compSub1.setName(prefix + "sub_1");

        ObjectRepository.ComponentIF compSub2 = new ObjectRepository.Component();
        compSub2.setName(prefix + "sub_2");

        ObjectRepository.ComponentIF compSub3 = new ObjectRepository.Component();
        compSub2.setName(prefix + "sub_3");

        ObjectRepository.Group group = new ObjectRepository.Group();
        group.setName(prefix + "test_group");

        compSub1.setParentComponent(parent);
        compSub2.setParentComponent(parent);
        compSub3.setParentComponent(parent);
        ArrayList list = new ArrayList();
        list.add(compSub1);
        list.add(compSub2);
        list.add(compSub3);
        parent.setChildComponents(list);
        parent.setGroup(group);

        broker.beginTransaction();
        broker.store(parent);
        broker.commitTransaction();

        broker.clearCache();
        Query query = QueryFactory.newQueryByIdentity(parent);
        parent = (ObjectRepository.ComponentIF) broker.getObjectByQuery(query);

        Query groupQuery = QueryFactory.newQueryByIdentity(group);
        ObjectRepository.Group lookedUpGroup = (ObjectRepository.Group) broker.getObjectByQuery(groupQuery);

        assertNotNull(parent);
        assertNotNull(parent.getGroup());
        assertNotNull(parent.getChildComponents());
        assertNotNull(parent.getName());
        assertNotNull(lookedUpGroup);

        assertEquals(3, parent.getChildComponents().size());
        assertEquals(group.getName(), (parent.getGroup().getName()));

        parent.setName(prefix + "updated_comp_name");
        parent.setGroup(null);

        broker.beginTransaction();
        broker.store(parent);
        broker.commitTransaction();

        broker.clearCache();
        query = QueryFactory.newQueryByIdentity(parent);
        parent = (ObjectRepository.ComponentIF) broker.getObjectByQuery(query);

        assertNotNull(parent);
        assertNull(parent.getGroup());
        assertNotNull(parent.getChildComponents());
        assertNotNull(parent.getName());

        assertEquals(3, parent.getChildComponents().size());
        assertEquals(prefix + "updated_comp_name", parent.getName());

        broker.beginTransaction();
        broker.delete(parent);
        broker.commitTransaction();

        parent = (ObjectRepository.ComponentIF) broker.getObjectByQuery(query);

        assertNull(parent);
        groupQuery = QueryFactory.newQueryByIdentity(group);
        lookedUpGroup = (ObjectRepository.Group) broker.getObjectByQuery(groupQuery);
        assertNotNull(lookedUpGroup);
    }

    /**
     * test handling of serialized objects (simulate
     * handling of objects across different JVM, e.g. AppServer)
     */
    public void testSerializedObjectsDelete()
    {
        String prefix = "testSerializedObjectsDelete_" + System.currentTimeMillis() + "_";

        ObjectRepository.ComponentIF comp = new ObjectRepository.Component();
        comp.setName(prefix + "main_component");

        ObjectRepository.ComponentIF compSub1 = new ObjectRepository.Component();
        compSub1.setName(prefix + "sub_1");

        ObjectRepository.ComponentIF compSub2 = new ObjectRepository.Component();
        compSub2.setName(prefix + "sub_2");

        ObjectRepository.ComponentIF compSub3 = new ObjectRepository.Component();
        compSub2.setName(prefix + "sub_3");

        ObjectRepository.Group group = new ObjectRepository.Group();
        group.setName(prefix + "test_group");

        compSub1.setParentComponent(comp);
        compSub2.setParentComponent(comp);
        compSub3.setParentComponent(comp);
        ArrayList list = new ArrayList();
        list.add(compSub1);
        list.add(compSub2);
        list.add(compSub3);
        comp.setChildComponents(list);
        comp.setGroup(group);

        broker.beginTransaction();
        broker.store(comp);
        broker.commitTransaction();

        broker.clearCache();
        Query query = QueryFactory.newQueryByIdentity(comp);
        comp = (ObjectRepository.ComponentIF) broker.getObjectByQuery(query);

        Query groupQuery = QueryFactory.newQueryByIdentity(group);
        ObjectRepository.Group lookedUpGroup = (ObjectRepository.Group) broker.getObjectByQuery(groupQuery);

        assertNotNull(comp);
        assertNotNull(comp.getGroup());
        assertNotNull(comp.getChildComponents());
        assertNotNull(comp.getName());
        assertNotNull(lookedUpGroup);

        assertEquals(3, comp.getChildComponents().size());
        assertEquals(group.getName(), (comp.getGroup().getName()));

        comp.setName(prefix + "updated_comp_name");
        comp.setGroup(null);

        comp = (ObjectRepository.ComponentIF) SerializationUtils.deserialize(SerializationUtils.serialize(comp));

        broker.beginTransaction();
        broker.store(comp);
        broker.commitTransaction();

        broker.clearCache();
        query = QueryFactory.newQueryByIdentity(comp);
        comp = (ObjectRepository.ComponentIF) broker.getObjectByQuery(query);

        assertNotNull(comp);
        assertNull(comp.getGroup());
        assertNotNull(comp.getChildComponents());
        assertNotNull(comp.getName());

        assertEquals(3, comp.getChildComponents().size());
        assertEquals(prefix + "updated_comp_name", comp.getName());

        //*****************************************
        // now we generate a deep copy
        comp = (ObjectRepository.ComponentIF) serializeDeserializeObject(comp);
        broker.beginTransaction();
        broker.delete(comp);
        broker.commitTransaction();
        //*****************************************

        comp = (ObjectRepository.ComponentIF) broker.getObjectByQuery(query);

        assertNull(comp);
        groupQuery = QueryFactory.newQueryByIdentity(group);
        lookedUpGroup = (ObjectRepository.Group) broker.getObjectByQuery(groupQuery);
        assertNotNull(lookedUpGroup);
    }

    public void testSerializedObjectsUpdate()
    {
        String prefix = "testSerializedObjectsUpdate_" + System.currentTimeMillis() + "_";

        ObjectRepository.ComponentIF parent = new ObjectRepository.Component();
        parent.setName(prefix + "main_component");

        ObjectRepository.ComponentIF compSub1 = new ObjectRepository.Component();
        compSub1.setName(prefix + "sub_1");

        ObjectRepository.ComponentIF compSub2 = new ObjectRepository.Component();
        compSub2.setName(prefix + "sub_2");

        ObjectRepository.ComponentIF compSub3 = new ObjectRepository.Component();
        compSub2.setName(prefix + "sub_3");

        ObjectRepository.Group group = new ObjectRepository.Group();
        group.setName(prefix + "test_group");

        compSub1.setParentComponent(parent);
        compSub2.setParentComponent(parent);
        compSub3.setParentComponent(parent);
        ArrayList list = new ArrayList();
        list.add(compSub1);
        list.add(compSub2);
        list.add(compSub3);
        parent.setChildComponents(list);
        parent.setGroup(group);

        broker.beginTransaction();
        broker.store(parent);
        broker.commitTransaction();

        broker.clearCache();
        Query query = QueryFactory.newQueryByIdentity(parent);
        parent = (ObjectRepository.ComponentIF) broker.getObjectByQuery(query);

        Query groupQuery = QueryFactory.newQueryByIdentity(group);
        ObjectRepository.Group lookedUpGroup = (ObjectRepository.Group) broker.getObjectByQuery(groupQuery);

        assertNotNull(parent);
        assertNotNull(parent.getGroup());
        assertNotNull(parent.getChildComponents());
        assertNotNull(parent.getName());
        assertNotNull(lookedUpGroup);

        assertEquals(3, parent.getChildComponents().size());
        assertEquals(group.getName(), (parent.getGroup().getName()));

        parent.setName(prefix + "updated_comp_name");
        parent.setGroup(null);

        //**************************************************
        // now we generate a deep copy
        parent = (ObjectRepository.ComponentIF) serializeDeserializeObject(parent);
        broker.beginTransaction();
        broker.store(parent);
        broker.commitTransaction();
        //**************************************************

        query = QueryFactory.newQueryByIdentity(parent);
        parent = (ObjectRepository.ComponentIF) broker.getObjectByQuery(query);

        assertNotNull(parent);
        assertNull(parent.getGroup());
        assertNotNull(parent.getChildComponents());
        assertNotNull(parent.getName());

        assertEquals(3, parent.getChildComponents().size());
        assertEquals(prefix + "updated_comp_name", parent.getName());

        // same with cleared cache
        broker.clearCache();
        query = QueryFactory.newQueryByIdentity(parent);
        parent = (ObjectRepository.ComponentIF) broker.getObjectByQuery(query);

        assertNotNull(parent);
        assertNull(parent.getGroup());
        assertNotNull(parent.getChildComponents());
        assertNotNull(parent.getName());

        assertEquals(3, parent.getChildComponents().size());
        assertEquals(prefix + "updated_comp_name", parent.getName());

        broker.beginTransaction();
        broker.delete(parent);
        broker.commitTransaction();

        parent = (ObjectRepository.ComponentIF) broker.getObjectByQuery(query);

        assertNull(parent);
        groupQuery = QueryFactory.newQueryByIdentity(group);
        lookedUpGroup = (ObjectRepository.Group) broker.getObjectByQuery(groupQuery);
        assertNotNull(lookedUpGroup);
    }

    public void testSerializedObjectsRefresh()
    {
        String prefix = "testSerializedObjectsRefresh_" + System.currentTimeMillis() + "_";
        ObjectRepository.ComponentIF parent = new ObjectRepository.Component();
        parent.setName(prefix + "main_component");

        ObjectRepository.ComponentIF compSub1 = new ObjectRepository.Component();
        compSub1.setName(prefix + "sub_1");

        ObjectRepository.ComponentIF compSub2 = new ObjectRepository.Component();
        compSub2.setName(prefix + "sub_2");

        ObjectRepository.ComponentIF compSub3 = new ObjectRepository.Component();
        compSub2.setName(prefix + "sub_3");

        ObjectRepository.Group group = new ObjectRepository.Group();
        group.setName(prefix + "test_group");

        compSub1.setParentComponent(parent);
        compSub2.setParentComponent(parent);
        compSub3.setParentComponent(parent);
        ArrayList list = new ArrayList();
        list.add(compSub1);
        list.add(compSub2);
        list.add(compSub3);
        parent.setChildComponents(list);
        parent.setGroup(group);

        broker.beginTransaction();
        broker.store(parent);
        broker.commitTransaction();

        broker.clearCache();
        Query query = QueryFactory.newQueryByIdentity(parent);
        parent = (ObjectRepository.ComponentIF) broker.getObjectByQuery(query);

        Query groupQuery = QueryFactory.newQueryByIdentity(group);
        ObjectRepository.Group lookedUpGroup = (ObjectRepository.Group) broker.getObjectByQuery(groupQuery);

        assertNotNull(parent);
        assertNotNull(parent.getGroup());
        assertNotNull(parent.getChildComponents());
        assertNotNull(parent.getName());
        assertEquals(3, parent.getChildComponents().size());
        assertEquals(group.getName(), (parent.getGroup().getName()));
        ObjectRepository.ComponentIF aChild = (ObjectRepository.ComponentIF) parent.getChildComponents().iterator().next();
        assertNotNull(aChild);
        assertNotNull(aChild.getParentComponent());
        assertEquals(parent, aChild.getParentComponent());
        assertNotNull(lookedUpGroup);

        //*************************************
        assertNotNull(parent);
        assertNotNull(parent.getGroup());
        parent = (ObjectRepository.ComponentIF) serializeDeserializeObject(parent);
        broker.retrieveAllReferences(parent);
        assertNotNull(parent);
        /*
        Now we have a problem! After serialization we can't find the anonymous keys
        for parent object, because object identity has changed!!
        This is now fixed in class QueryReferenceBroker#getReferencedObjectIdentity
        */
        assertNotNull(parent.getGroup());
        //*************************************
        assertNotNull(parent.getChildComponents());
        assertNotNull(parent.getName());
        assertEquals(3, parent.getChildComponents().size());
        aChild = (ObjectRepository.ComponentIF) parent.getChildComponents().iterator().next();
        assertNotNull(aChild);
        assertNotNull(aChild.getParentComponent());
        assertEquals(parent, aChild.getParentComponent());

        broker.beginTransaction();
        broker.store(parent);
        broker.commitTransaction();

        // now nothing should happen, because we don't make any changes
        broker.clearCache();
        query = QueryFactory.newQueryByIdentity(parent);
        parent = (ObjectRepository.ComponentIF) broker.getObjectByQuery(query);
        groupQuery = QueryFactory.newQueryByIdentity(group);
        lookedUpGroup = (ObjectRepository.Group) broker.getObjectByQuery(groupQuery);
        assertNotNull(parent);
        assertNotNull(parent.getGroup());
        assertNotNull(parent.getChildComponents());
        assertNotNull(parent.getName());
        assertEquals(3, parent.getChildComponents().size());
        assertEquals(group.getName(), (parent.getGroup().getName()));
        aChild = (ObjectRepository.ComponentIF) parent.getChildComponents().iterator().next();
        assertNotNull(aChild);
        assertNotNull(aChild.getParentComponent());
        assertEquals(parent, aChild.getParentComponent());
        assertNotNull(lookedUpGroup);
    }

    public void testSerializedObjectsRefreshWithProxy()
    {
        String prefix = "testSerializedObjectsRefreshWithProxy_" + System.currentTimeMillis() + "_";

        ClassDescriptor cld = broker.getClassDescriptor(ObjectRepository.Component.class);
        ObjectReferenceDescriptor ord = cld.getObjectReferenceDescriptorByName("parentComponent");
        boolean oldState = ord.isLazy();
        try
        {
            ord.setLazy(true);
            ObjectRepository.ComponentIF parent = new ObjectRepository.Component();
            parent.setName(prefix + "main_component");

            ObjectRepository.ComponentIF compSub1 = new ObjectRepository.Component();
            compSub1.setName(prefix + "sub_1");

            ObjectRepository.ComponentIF compSub2 = new ObjectRepository.Component();
            compSub2.setName(prefix + "sub_2");

            ObjectRepository.ComponentIF compSub3 = new ObjectRepository.Component();
            compSub2.setName(prefix + "sub_3");

            ObjectRepository.Group group = new ObjectRepository.Group();
            group.setName(prefix + "test_group");

            compSub1.setParentComponent(parent);
            compSub2.setParentComponent(parent);
            compSub3.setParentComponent(parent);
            ArrayList list = new ArrayList();
            list.add(compSub1);
            list.add(compSub2);
            list.add(compSub3);
            parent.setChildComponents(list);
            parent.setGroup(group);

            broker.beginTransaction();
            broker.store(parent);
            broker.commitTransaction();

            broker.clearCache();
            Query query = QueryFactory.newQueryByIdentity(parent);
            parent = (ObjectRepository.ComponentIF) broker.getObjectByQuery(query);

            Query groupQuery = QueryFactory.newQueryByIdentity(group);
            ObjectRepository.Group lookedUpGroup = (ObjectRepository.Group) broker.getObjectByQuery(groupQuery);

            assertNotNull(parent);
            assertNotNull(parent.getGroup());
            assertNotNull(parent.getChildComponents());
            assertNotNull(parent.getName());
            assertEquals(3, parent.getChildComponents().size());
            assertEquals(group.getName(), (parent.getGroup().getName()));
            ObjectRepository.ComponentIF aChild = (ObjectRepository.ComponentIF) parent.getChildComponents().iterator().next();
            assertNotNull(aChild);
            assertNotNull(aChild.getParentComponent());
            assertEquals(parent, aChild.getParentComponent());
            assertNotNull(lookedUpGroup);

            //*************************************
            assertNotNull(parent);
            assertNotNull(parent.getGroup());
            parent = (ObjectRepository.ComponentIF) serializeDeserializeObject(parent);
            broker.retrieveAllReferences(parent);
            assertNotNull(parent);
            /*
            Now we have a problem! After serialization we can't find the anonymous keys
            for parent object, because object identity has changed!!
            This is now fixed in class QueryReferenceBroker#getReferencedObjectIdentity
            */
            assertNotNull(parent.getGroup());
            //*************************************
            assertNotNull(parent.getChildComponents());
            assertNotNull(parent.getName());
            assertEquals(3, parent.getChildComponents().size());
            aChild = (ObjectRepository.ComponentIF) parent.getChildComponents().iterator().next();
            assertNotNull(aChild);
            assertNotNull(aChild.getParentComponent());
            assertEquals(parent, aChild.getParentComponent());

            broker.beginTransaction();
            broker.store(parent);
            broker.commitTransaction();

            // now nothing should happen, because we don't make any changes
            broker.clearCache();
            query = QueryFactory.newQueryByIdentity(parent);
            parent = (ObjectRepository.ComponentIF) broker.getObjectByQuery(query);
            groupQuery = QueryFactory.newQueryByIdentity(group);
            lookedUpGroup = (ObjectRepository.Group) broker.getObjectByQuery(groupQuery);
            assertNotNull(parent);
            assertNotNull(parent.getGroup());
            assertNotNull(parent.getChildComponents());
            assertNotNull(parent.getName());
            assertEquals(3, parent.getChildComponents().size());
            assertEquals(group.getName(), (parent.getGroup().getName()));
            aChild = (ObjectRepository.ComponentIF) parent.getChildComponents().iterator().next();
            assertNotNull(aChild);
            assertNotNull(aChild.getParentComponent());
            assertEquals(parent, aChild.getParentComponent());
            assertNotNull(lookedUpGroup);
        }
        finally
        {
            ord.setLazy(oldState);
        }
    }

    /**
     * write an entry using vertical inheritance and try to read it again. E-F
     */
    public void testVerticalInheritanceStoreAndLoad() throws Exception
    {
        // produce some test data
        ObjectRepository.F entry = new ObjectRepository.F();
        entry.setSomeSuperValue(31415926);
        entry.setSomeValue(123456);
        broker.beginTransaction();
        broker.store(entry);
        broker.commitTransaction();

        Identity oid = new Identity(entry, broker);

        // clear cache and retrieve a copy from the DB
        broker.clearCache();
        ObjectRepository.F copy = (ObjectRepository.F) broker.getObjectByIdentity(oid);

        // check equality
        assertEquals(entry.getSomeValue(), copy.getSomeValue());
        assertEquals(entry.getSomeSuperValue(), copy.getSomeSuperValue());
    }

    /**
     * write an entry using vertical inheritance and try to read it again.
     * E-F-G
     */
    public void testVerticalInheritanceStoreAndLoad2() throws Exception
    {
        // produce some test data G
        ObjectRepository.G entry = new ObjectRepository.G();
        entry.setSomeSuperValue(31415926);
        entry.setSomeValue(123456);
        entry.setSomeSubValue(4242);
        broker.beginTransaction();
        broker.store(entry);
        broker.commitTransaction();

        Identity oid = new Identity(entry, broker);

        // clear cache and retrieve a copy from the DB
        broker.clearCache();
        ObjectRepository.G copy = (ObjectRepository.G) broker.getObjectByIdentity(oid);

        // check equality
        assertEquals(entry.getSomeValue(), copy.getSomeValue());
        assertEquals(entry.getSomeSuperValue(), copy.getSomeSuperValue());
        assertEquals(entry.getSomeSubValue(), copy.getSomeSubValue());
    }

    /**
     * write an entry using vertical inheritance and try to read it again.
     * E-F1-G1, autoincrement id in E
     */
    public void testVerticalInheritanceStoreAndLoad3() throws Exception
    {
        // produce some test data G1
        ObjectRepository.G1 entry = new ObjectRepository.G1();
        entry.setSomeSuperValue(31415926);
        entry.setSomeValue(123456);
        entry.setSomeSubValue(4242);
        broker.beginTransaction();
        broker.store(entry);
        broker.commitTransaction();

        Identity oid = new Identity(entry, broker);

        // clear cache and retrieve a copy from the DB
        broker.clearCache();
        ObjectRepository.G1 copy = (ObjectRepository.G1) broker.getObjectByIdentity(oid);

        // check equality
        assertEquals(entry.getSomeValue(), copy.getSomeValue());
        assertEquals(entry.getSomeSuperValue(), copy.getSomeSuperValue());
        assertEquals(entry.getSomeSubValue(), copy.getSomeSubValue());
    }

    /**
     * write an entry using vertical inheritance and try to read it again. E-F
     */
    public void testVerticalInheritanceUpdate() throws Exception
    {
        // produce some test data
        ObjectRepository.F entry = new ObjectRepository.F();
        entry.setSomeSuperValue(2718281);
        entry.setSomeValue(9999);
        broker.beginTransaction();
        broker.store(entry);
        broker.commitTransaction();

        Identity oid = new Identity(entry, broker);

        entry.setSomeSuperValue(2718282);
        entry.setSomeValue(10000);
        broker.beginTransaction();
        broker.store(entry);
        broker.commitTransaction();

        // clear cache and retrieve a copy from the DB
        broker.clearCache();
        ObjectRepository.F copy = (ObjectRepository.F) broker.getObjectByIdentity(oid);

        // check equality
        assertEquals(entry.getSomeValue(), copy.getSomeValue());
        assertEquals(entry.getSomeSuperValue(), copy.getSomeSuperValue());
    }

    /**
     * write an entry using vertical inheritance and try to read it again.
     * E-F-G
     */
    public void testVerticalInheritanceUpdate2() throws Exception
    {
        // produce some test data
        ObjectRepository.G entry = new ObjectRepository.G();
        entry.setSomeSuperValue(2718281);
        entry.setSomeValue(9999);
        entry.setSomeSubValue(8888);
        broker.beginTransaction();
        broker.store(entry);
        broker.commitTransaction();

        Identity oid = new Identity(entry, broker);

        entry.setSomeSuperValue(2718282);
        entry.setSomeValue(10000);
        entry.setSomeSubValue(7777);
        broker.beginTransaction();
        broker.store(entry);
        broker.commitTransaction();

        // clear cache and retrieve a copy from the DB
        broker.clearCache();
        ObjectRepository.G copy = (ObjectRepository.G) broker.getObjectByIdentity(oid);

        // check equality
        assertEquals(entry.getSomeValue(), copy.getSomeValue());
        assertEquals(entry.getSomeSuperValue(), copy.getSomeSuperValue());
        assertEquals(entry.getSomeSubValue(), copy.getSomeSubValue());
    }

    /**
     * write an entry using vertical inheritance and try to read it again.
     * E-F1-G1, autoincrement id in E
     */
    public void testVerticalInheritanceUpdate3() throws Exception
    {
        // produce some test data
        ObjectRepository.G1 entry = new ObjectRepository.G1();
        entry.setSomeSuperValue(2718281);
        entry.setSomeValue(9999);
        entry.setSomeSubValue(8888);
        broker.beginTransaction();
        broker.store(entry);
        broker.commitTransaction();

        Identity oid = new Identity(entry, broker);

        entry.setSomeSuperValue(2718282);
        entry.setSomeValue(10000);
        entry.setSomeSubValue(7777);
        broker.beginTransaction();
        broker.store(entry);
        broker.commitTransaction();

        // clear cache and retrieve a copy from the DB
        broker.clearCache();
        ObjectRepository.G1 copy = (ObjectRepository.G1) broker.getObjectByIdentity(oid);

        // check equality
        assertEquals(entry.getSomeValue(), copy.getSomeValue());
        assertEquals(entry.getSomeSuperValue(), copy.getSomeSuperValue());
        assertEquals(entry.getSomeSubValue(), copy.getSomeSubValue());
    }

    /**
     * Query Attribute of Super class E-F
     */
    public void testQuerySuperField_WithCache()
    {
        doTestQuerySuperField(false);
    }

    /**
     * Query Attribute of Super class E-F
     */
    public void testQuerySuperField_ClearedCache()
    {
        doTestQuerySuperField(true);
    }

    /**
     * Query Attribute of Super class E-F
     */
    public void doTestQuerySuperField(boolean clearCache)
    {
        int data1 = (int) (Math.random() * Integer.MAX_VALUE);
        int data2 = (int) (Math.random() * Integer.MAX_VALUE);

        broker.beginTransaction();

        ObjectRepository.F f1 = new ObjectRepository.F();
        f1.setSomeValue(data1);
        f1.setSomeSuperValue(data2);
        broker.store(f1);

        ObjectRepository.F f2 = new ObjectRepository.F();
        f2.setSomeValue(data1);
        f2.setSomeSuperValue(data2);
        broker.store(f2);

        ObjectRepository.F f3 = new ObjectRepository.F();
        f3.setSomeValue(data1);
        f3.setSomeSuperValue(data2);
        broker.store(f3);

        broker.commitTransaction();

        if(clearCache) broker.clearCache();

        Criteria c = new Criteria();
        c.addEqualTo("someSuperValue", new Integer(data2));
        Query q = QueryFactory.newQuery(ObjectRepository.F.class, c);
        Collection result = broker.getCollectionByQuery(q);
        assertEquals(3, result.size());
        ObjectRepository.F retF = (ObjectRepository.F) result.iterator().next();
        assertEquals(data1, retF.getSomeValue());
        assertEquals(data2, retF.getSomeSuperValue());
    }

    /**
     * Query Attribute of Super class E-F-G
     */
    public void testQuerySuperField_2_WithCache()
    {
        doTestQuerySuperField_2(false);
    }

    /**
     * Query Attribute of Super class E-F-G
     */
    public void testQuerySuperField_2_ClearedCache()
    {
        doTestQuerySuperField_2(true);
    }

    /**
     * Query Attribute of Super class E-F-G
     */
    public void doTestQuerySuperField_2(boolean clearCache)
    {
        int data1 = (int) (Math.random() * Integer.MAX_VALUE);
        int data2 = (int) (Math.random() * Integer.MAX_VALUE);
        int data3 = (int) (Math.random() * Integer.MAX_VALUE);
        broker.beginTransaction();
        ObjectRepository.G g1 = new ObjectRepository.G();
        g1.setSomeValue(data1);
        g1.setSomeSuperValue(data2);
        g1.setSomeSubValue(data3);
        broker.store(g1);

        ObjectRepository.G g2 = new ObjectRepository.G();
        g2.setSomeValue(data1);
        g2.setSomeSuperValue(data2);
        g2.setSomeSubValue(data3);
        broker.store(g2);

        ObjectRepository.G g3 = new ObjectRepository.G();
        g3.setSomeValue(data1);
        g3.setSomeSuperValue(data2);
        g3.setSomeSubValue(data3);
        broker.store(g3);
        broker.commitTransaction();

        if(clearCache) broker.clearCache();
        Criteria c = new Criteria();
        c.addEqualTo("someSuperValue", new Integer(data2));
        Query q = QueryFactory.newQuery(ObjectRepository.G.class, c);
        Collection result = broker.getCollectionByQuery(q);
        assertEquals(3, result.size());
        ObjectRepository.G ret = (ObjectRepository.G) result.iterator().next();
        assertEquals(data1, ret.getSomeValue());
        assertEquals(data2, ret.getSomeSuperValue());
        assertEquals(data3, ret.getSomeSubValue());
    }

    public void testMultipleJoinedInheritanceAndExtents()
    {
        ObjectRepository.F1 entry = new ObjectRepository.F1();
        entry.setSomeSuperValue(1);
        entry.setSomeValue(2);
        broker.beginTransaction();
        broker.store(entry);
        broker.commitTransaction();

        Integer id = entry.getId();
        broker.clearCache();
        entry = (F1) findById(ObjectRepository.F1.class, id.intValue());
        assertEquals(id, entry.getId());
        assertEquals(1, entry.getSomeSuperValue());
        assertEquals(2, entry.getSomeValue());
    }

    public void testMultipleJoinedInheritanceAndExtentsWithCache()
    {
        ObjectRepository.F1 entry = new ObjectRepository.F1();
        entry.setSomeSuperValue(1);
        entry.setSomeValue(2);
        broker.beginTransaction();
        broker.store(entry);
        broker.commitTransaction();

        Integer id = entry.getId();
        entry = (F1) findById(ObjectRepository.F1.class, id.intValue());
        assertEquals(id, entry.getId());
        assertEquals(1, entry.getSomeSuperValue());
        assertEquals(2, entry.getSomeValue());
    }

    public void testMultipleJoinedInheritanceAndExtents_2()
    {
        ObjectRepository.F1 entry = new ObjectRepository.F1();
        entry.setSomeSuperValue(1);
        entry.setSomeValue(2);
        broker.beginTransaction();
        broker.store(entry);
        broker.commitTransaction();

        Integer id = entry.getId();
        broker.clearCache();
        entry = (F1) findById(ObjectRepository.E.class, id.intValue());
        assertEquals(id, entry.getId());
        assertEquals(1, entry.getSomeSuperValue());
        assertEquals(2, entry.getSomeValue());
    }

    public void testMultipleJoinedInheritanceAndExtents_2_WithCache()
    {
        ObjectRepository.F1 entry = new ObjectRepository.F1();
        entry.setSomeSuperValue(1);
        entry.setSomeValue(2);
        broker.beginTransaction();
        broker.store(entry);
        broker.commitTransaction();

        Integer id = entry.getId();
        entry = (F1) findById(ObjectRepository.E.class, id.intValue());
        assertEquals(id, entry.getId());
        assertEquals(1, entry.getSomeSuperValue());
        assertEquals(2, entry.getSomeValue());
    }


    // --------------------------------------------------------------------------------------
    // Utiltity methods.
    private Object serializeDeserializeObject(Serializable obj)
    {
        return (ObjectRepository.ComponentIF) SerializationUtils.deserialize(SerializationUtils.serialize(obj));
    }

    private Object findById(Class type, int id)
    {
        Collection result = broker.getCollectionByQuery(createQueryById(type, id));
        if(result == null || result.size() == 0)
        {
            return null;
        }
        if(result.size() > 1)
        {
            throw new RuntimeException("Unexpected unique id constraint violation ");
        }

        return result.iterator().next();

    }

    private Query createQueryById(Class type, int id)
    {
        Criteria byIdCriteria = new Criteria();
        byIdCriteria.addEqualTo("id", new Integer(id));
        return QueryFactory.newQuery(type, byIdCriteria);
    }
}
