package org.apache.ojb.broker;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ojb.junit.PBTestCase;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * This TestClass tests OJB facilities to work with persistence
 * aware instances.
 */
public class PBListenerTest extends PBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {PBListenerTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public PBListenerTest(String name)
    {
        super(name);
    }

    /**
     * Test for OJB-68
     * tests the callbacks beforeStore() and afterStore().
     */
    public void testStoreAndQuery() throws Exception
    {
        String name = "testStoreAndQuery_" + System.currentTimeMillis();

        PBAwareObject pba_1 = new PBAwareObject(name);
        pba_1.setRefObject(new RefObj(name));
        pba_1.addCollObject(new CollObj(name));
        pba_1.addCollObject(new CollObj(name));
        pba_1.addCollObject(new CollObj(name));

        PBAwareObject pba_2 = new PBAwareObject(name);
        pba_2.setRefObject(new RefObj(name));
        pba_2.addCollObject(new CollObj(name));
        pba_2.addCollObject(new CollObj(name));
        pba_2.addCollObject(new CollObj(name));

        PBAwareObject pba_3 = new PBAwareObject(name);
        pba_3.setRefObject(new RefObj(name));
        pba_3.addCollObject(new CollObj(name));
        pba_3.addCollObject(new CollObj(name));
        pba_3.addCollObject(new CollObj(name));

        broker.beginTransaction();
        broker.store(pba_1);
        broker.store(pba_2);
        broker.store(pba_3);
        broker.commitTransaction();

        Identity oid_1 = broker.serviceIdentity().buildIdentity(pba_1);
        Identity oid_2 = broker.serviceIdentity().buildIdentity(pba_2);

        broker.clearCache();

        PBAwareObject pba_1_new = (PBAwareObject) broker.getObjectByIdentity(oid_1);
        PBAwareObject pba_2_new = (PBAwareObject) broker.getObjectByIdentity(oid_2);

        assertNotNull(pba_1_new);
        assertNotNull(pba_2_new);
        assertNotNull(pba_1_new.getRefObject());
        assertNotNull(pba_2_new.getRefObject());
        assertEquals(3, pba_1_new.getCollObjects().size());
        assertEquals(3, pba_2_new.getCollObjects().size());
        assertTrue(pba_1_new.isAfterLookupRefObjectPopulated());
        assertTrue(pba_1_new.isAfterLookupCollObjectsPopulated());
        assertTrue(pba_2_new.isAfterLookupRefObjectPopulated());
        assertTrue(pba_2_new.isAfterLookupCollObjectsPopulated());

        broker.clearCache();

        Criteria criteria = new Criteria();
        criteria.addEqualTo( "name", name);
        QueryByCriteria query = QueryFactory.newQuery(PBAwareObject.class, criteria);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(3, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            PBAwareObject pba =  (PBAwareObject) iterator.next();
            assertNotNull(pba);
            assertNotNull(pba.getRefObject());
            assertEquals(3, pba.getCollObjects().size());
            assertTrue(pba.isAfterLookupRefObjectPopulated());
            assertTrue(pba.isAfterLookupCollObjectsPopulated());
        }
    }

    /**
     * tests the callbacks beforeStore() and afterStore().
     */
    public void testStoreCallbacks() throws Exception
    {
        PBAwareObject obj = new PBAwareObject();
        assertEquals(false, obj.getCalledBeforeUpdate());
        assertEquals(false, obj.getCalledAfterUpdate());
        assertEquals(false, obj.getCalledBeforeStore());
        assertEquals(false, obj.getCalledAfterStore());
        broker.beginTransaction();
        broker.store(obj);
        assertEquals(true, obj.getCalledBeforeStore());
        assertEquals(true, obj.getCalledAfterStore());
        assertEquals(false, obj.getCalledBeforeUpdate());
        assertEquals(false, obj.getCalledAfterUpdate());
        broker.commitTransaction();
    }

    /**
     * tests the callbacks beforeStore() and afterStore().
     */
    public void testUpdateCallbacks() throws Exception
    {
        PBAwareObject obj = new PBAwareObject();
        broker.beginTransaction();
        broker.store(obj);
        broker.commitTransaction();
        Identity oid = new Identity(obj, broker);

        PBAwareObject lookedUp = (PBAwareObject) broker.getObjectByIdentity(oid);
        lookedUp.setName("testUpdateCallbacks");
        assertEquals(false, obj.getCalledBeforeUpdate());
        assertEquals(false, obj.getCalledAfterUpdate());
        broker.beginTransaction();
        broker.store(lookedUp);
        broker.commitTransaction();
        assertEquals(true, lookedUp.getCalledBeforeUpdate());
        assertEquals(true, lookedUp.getCalledAfterUpdate());
    }

    /**
     * tests the callbacks beforeDelete() and afterDelete().
     */
    public void testDeleteCallbacks() throws Exception
    {
        PBAwareObject obj = new PBAwareObject();
        broker.beginTransaction();
        broker.store(obj);
        broker.commitTransaction();

        assertEquals(false, obj.getCalledBeforeDelete());
        assertEquals(false, obj.getCalledAfterDelete());
        broker.beginTransaction();
        broker.delete(obj);
        assertEquals(true, obj.getCalledBeforeDelete());
        assertEquals(true, obj.getCalledAfterDelete());
        broker.commitTransaction();
    }

    /**
     * tests the callback afterLookup()
     */
    public void testLookupCallback() throws Exception
    {
        PBAwareObject obj = new PBAwareObject();
        Identity oid = new Identity(obj, broker);
        broker.beginTransaction();
        broker.store(obj);
        broker.commitTransaction();
        assertEquals(false, obj.getCalledAfterLookup());

        PBAwareObject lookedUp = (PBAwareObject) broker.getObjectByIdentity(oid);

        assertEquals(true, lookedUp.getCalledAfterLookup());
    }

    public void testLifeCycleListener()
    {
        PBAwareObject obj = new PBAwareObject();
        obj.setName("testLifeCycleListener");
        Identity oid = new Identity(obj, broker);

        // now we add the listener
        PBLifeCycleListenerObject listener = new PBLifeCycleListenerObject();
        broker.addListener(listener);

        broker.beginTransaction();
        broker.store(obj);
        assertEquals("insert listener call failed", 6, listener.evaluateTest());
        broker.commitTransaction();

        broker.clearCache();
        PBAwareObject lookedUp = (PBAwareObject) broker.getObjectByIdentity(oid);
        assertEquals("lookup listener call failed", 30, listener.evaluateTest());
        lookedUp.setName("testLifeCycleListener_updated");
        broker.beginTransaction();
        broker.store(lookedUp);
        assertEquals("update listener call failed", 2310, listener.evaluateTest());
        broker.commitTransaction();

        broker.beginTransaction();
        broker.delete(obj);
        assertEquals("delete listener call failed", 510510, listener.evaluateTest());
        broker.commitTransaction();
    }

    public void testPBStateListener()
    {
        // This test need its own broker instance
        PersistenceBroker pb = PersistenceBrokerFactory.defaultPersistenceBroker();
        PBStateListenerObject listener;
        PBStateListenerObject listener_2 = null;
        try
        {
            listener = new PBStateListenerObject();
            pb.addListener(listener);

            // we could not check after open method
            listener.afterOpen(new PBStateEvent(pb, PBStateEvent.Type.AFTER_OPEN));
            assertEquals("afterOpen listener call failed", 2, listener.evaluateTest());

            pb.beginTransaction();
            assertEquals("beforeBegin/afterBegin listener call failed", 30, listener.evaluateTest());

            pb.commitTransaction();
            assertEquals("beforeCommit/afterCommit listener call failed", 2310, listener.evaluateTest());


            listener_2 = new PBStateListenerObject();
            pb.addListener(listener_2);

            // we could not check after open method
            listener_2.afterOpen(new PBStateEvent(pb, PBStateEvent.Type.AFTER_OPEN));
            assertEquals("afterOpen listener call failed", 2, listener_2.evaluateTest());

            pb.beginTransaction();
            assertEquals("beforeBegin/afterBegin listener call failed", 30, listener_2.evaluateTest());

            pb.abortTransaction();
            assertEquals("beforeRollback/afterRollback listener call failed", 6630, listener_2.evaluateTest());
        }
        finally
        {
            pb.close();
            if(listener_2 != null) assertEquals("beforeClose listener call failed", 125970, listener_2.evaluateTest());
            else fail("Something wrong with test");
        }
    }



    //************************************************************************
    // test classes
    //************************************************************************
    public static class PBLifeCycleListenerObject implements PBLifeCycleListener
    {
        int beforeInsert = 1, afterInsert = 1, beforeUpdate = 1, afterUpdate = 1,
        beforeDelete = 1, afterDelete = 1, afterLookup = 1;

        public void beforeInsert(PBLifeCycleEvent event) throws PersistenceBrokerException
        {
            if(!(event.getTarget() instanceof PBAwareObject)) return;
            beforeInsert*=2;
        }

        public void afterInsert(PBLifeCycleEvent event) throws PersistenceBrokerException
        {
            if(!(event.getTarget() instanceof PBAwareObject)) return;
            afterInsert*=3;
        }

        public void afterLookup(PBLifeCycleEvent event) throws PersistenceBrokerException
        {
            if(!(event.getTarget() instanceof PBAwareObject)) return;
            afterLookup*=5;
        }

        public void beforeUpdate(PBLifeCycleEvent event) throws PersistenceBrokerException
        {
            if(!(event.getTarget() instanceof PBAwareObject)) return;
            beforeUpdate*=7;
        }

        public void afterUpdate(PBLifeCycleEvent event) throws PersistenceBrokerException
        {
            if(!(event.getTarget() instanceof PBAwareObject)) return;
            afterUpdate*=11;
        }

        public void beforeDelete(PBLifeCycleEvent event) throws PersistenceBrokerException
        {
            if(!(event.getTarget() instanceof PBAwareObject)) return;
            beforeDelete*=13;
        }

        public void afterDelete(PBLifeCycleEvent event) throws PersistenceBrokerException
        {
            if(!(event.getTarget() instanceof PBAwareObject)) return;
            afterDelete*=17;
        }

        public int evaluateTest()
        {
            return beforeInsert * afterInsert * beforeUpdate * afterUpdate * beforeDelete *
                    afterDelete * afterLookup;
        }
    }



    public static class PBStateListenerObject implements PBStateListener
    {
        int afterOpen = 1, beforeBegin = 1, afterBegin = 1, beforeCommit = 1, afterCommit = 1,
        beforeRollback = 1, afterRollback = 1, beforeClose = 1;

        public void afterOpen(PBStateEvent event)
        {
            afterOpen*=2;
        }

        public void beforeBegin(PBStateEvent event)
        {
            beforeBegin*=3;
        }

        public void afterBegin(PBStateEvent event)
        {
            afterBegin*=5;
        }

        public void beforeCommit(PBStateEvent event)
        {
            beforeCommit*=7;
        }

        public void afterCommit(PBStateEvent event)
        {
            afterCommit*=11;
        }

        public void beforeRollback(PBStateEvent event)
        {
            beforeRollback*=13;
        }

        public void afterRollback(PBStateEvent event)
        {
            afterRollback*=17;
        }

        public void beforeClose(PBStateEvent event)
        {
            beforeClose*=19;
        }

        public int evaluateTest()
        {
            return afterOpen * beforeBegin * afterBegin * beforeCommit * afterCommit *
                    beforeRollback * afterRollback * beforeClose;
        }
    }



    /**
     * persistence capable class
     */
    public static class PBAwareObject implements PersistenceBrokerAware, Serializable
    {
        private int id;
        private String name;
        private RefObj refObject;
        private List collObjects;

        private boolean calledBeforeInsert = false;
        private boolean calledAfterInsert = false;
        private boolean calledBeforeDelete = false;
        private boolean calledAfterDelete = false;
        private boolean calledAfterLookup = false;
        private boolean calledAfterUpdate = false;
        private boolean calledBeforeUpdate = false;
        private boolean afterLookupRefObjectPopulated = false;
        private boolean afterLookupCollObjectsPopulated = false;

        public PBAwareObject()
        {
        }

        public PBAwareObject(String name)
        {
            this.name = name;
        }

        public void beforeUpdate(PersistenceBroker broker) throws PersistenceBrokerException
        {
            calledBeforeUpdate = true;
        }

        public void afterUpdate(PersistenceBroker broker) throws PersistenceBrokerException
        {
            calledAfterUpdate = true;
        }
        public void beforeInsert(PersistenceBroker broker) throws PersistenceBrokerException
        {
            //System.out.println("beforeStore()");
            calledBeforeInsert = true;
        }
        public void afterInsert(PersistenceBroker broker) throws PersistenceBrokerException
        {
            //System.out.println("afterStore()");
            if (calledBeforeInsert)
            {
                calledAfterInsert = true;
            }
        }
        public void beforeDelete(PersistenceBroker broker) throws PersistenceBrokerException
        {
            //System.out.println("beforeDelete()");
            calledBeforeDelete = true;
        }
        public void afterDelete(PersistenceBroker broker) throws PersistenceBrokerException
        {
            //System.out.println("afterDelete()");
            if (calledBeforeDelete)
            {
                calledAfterDelete = true;
            }
        }
        public void afterLookup(PersistenceBroker broker) throws PersistenceBrokerException
        {
            //System.out.println("afterLookup()");
            calledAfterLookup = true;
            if(refObject != null) afterLookupRefObjectPopulated = true;
            if(collObjects != null) afterLookupCollObjectsPopulated = true;
//            System.out.println("## " + refObject);
//            System.out.println("## " + collObjects);
//            if(refObject == null)
//            {
//                try{throw new Exception();}catch(Exception e)
//                {
//                e.printStackTrace();
//                }
//            }
        }


        public boolean getCalledAfterUpdate()
        {
            return calledAfterUpdate;
        }

        public void setCalledAfterUpdate(boolean calledAfterUpdate)
        {
            this.calledAfterUpdate = calledAfterUpdate;
        }

        public boolean getCalledBeforeUpdate()
        {
            return calledBeforeUpdate;
        }

        public void setCalledBeforeUpdate(boolean calledBeforeUpdate)
        {
            this.calledBeforeUpdate = calledBeforeUpdate;
        }

        public boolean getCalledAfterDelete()
        {
            return calledAfterDelete;
        }

        public void setCalledAfterDelete(boolean calledAfterDelete)
        {
            this.calledAfterDelete = calledAfterDelete;
        }

        public boolean getCalledAfterLookup()
        {
            return calledAfterLookup;
        }

        public void setCalledAfterLookup(boolean calledAfterLookup)
        {
            this.calledAfterLookup = calledAfterLookup;
        }

        public boolean getCalledAfterStore()
        {
            return calledAfterInsert;
        }

        public void setCalledAfterStore(boolean calledAfterStore)
        {
            this.calledAfterInsert = calledAfterStore;
        }

        public boolean getCalledBeforeDelete()
        {
            return calledBeforeDelete;
        }

        public void setCalledBeforeDelete(boolean calledBeforeDelete)
        {
            this.calledBeforeDelete = calledBeforeDelete;
        }

        public boolean getCalledBeforeStore()
        {
            return calledBeforeInsert;
        }

        public void setCalledBeforeStore(boolean calledBeforeStore)
        {
            this.calledBeforeInsert = calledBeforeStore;
        }

        public boolean isCalledBeforeInsert()
        {
            return calledBeforeInsert;
        }

        public void setCalledBeforeInsert(boolean calledBeforeInsert)
        {
            this.calledBeforeInsert = calledBeforeInsert;
        }

        public boolean isCalledAfterInsert()
        {
            return calledAfterInsert;
        }

        public void setCalledAfterInsert(boolean calledAfterInsert)
        {
            this.calledAfterInsert = calledAfterInsert;
        }

        public boolean isAfterLookupRefObjectPopulated()
        {
            return afterLookupRefObjectPopulated;
        }

        public void setAfterLookupRefObjectPopulated(boolean afterLookupRefObjectPopulated)
        {
            this.afterLookupRefObjectPopulated = afterLookupRefObjectPopulated;
        }

        public boolean isAfterLookupCollObjectsPopulated()
        {
            return afterLookupCollObjectsPopulated;
        }

        public void setAfterLookupCollObjectsPopulated(boolean afterLookupCollObjectsPopulated)
        {
            this.afterLookupCollObjectsPopulated = afterLookupCollObjectsPopulated;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public RefObj getRefObject()
        {
            return refObject;
        }

        public void setRefObject(RefObj refObj)
        {
            this.refObject = refObj;
        }

        public List getCollObjects()
        {
            return collObjects;
        }

        public void addCollObject(CollObj obj)
        {
            if(collObjects == null)
            {
                collObjects = new ArrayList();
            }
            collObjects.add(obj);
        }

        public void setCollObjects(List collObjects)
        {
            this.collObjects = collObjects;
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    public static class RefObj implements Serializable
    {
        private Integer id;
        private String name;

        public RefObj()
        {
        }

        public RefObj(Integer id, String name)
        {
            this.id = id;
            this.name = name;
        }

        public RefObj(String name)
        {
            this.name = name;
        }

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
    }

    public static class CollObj extends RefObj
    {
        private Integer fkPBAwareObject;

        public CollObj()
        {
        }

        public CollObj(Integer id, String name)
        {
            super(id, name);
        }

        public CollObj(String name)
        {
            super(name);
        }

        public Integer getFkPBAwareObject()
        {
            return fkPBAwareObject;
        }

        public void setFkPBAwareObject(Integer fkPBAwareObject)
        {
            this.fkPBAwareObject = fkPBAwareObject;
        }
    }

}
