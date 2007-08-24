/*
 * BatchModeTest.java
 * JUnit based test
 *
 * Created on February 15, 2003, 12:47 AM
 */

package org.apache.ojb.broker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.ojb.broker.accesslayer.ConnectionManagerIF;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

/**
 * @author Oleg Nitz
 */
public class BatchModeTest extends PBTestCase
{
    private static long timestamp = System.currentTimeMillis();

    public BatchModeTest(String testName)
    {
        super(testName);
    }

    private long getNewId()
    {
        return timestamp++;
    }

    public static void main(String[] args)
    {
        String[] arr = {BatchModeTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        broker.serviceConnectionManager().setBatchMode(true);
        // lookup connection to enable batch mode
        broker.serviceConnectionManager().getConnection();
    }

    boolean batchModeDisabled()
    {
        if(broker.serviceConnectionManager().isBatchMode())
        {
            return false;
        }
        else
        {
            System.out.println("[" + BatchModeTest.class.getName() + "] Skip test, batch mode was not enabled or supported");
            return true;
        }
    }


    /**
     * A common (no specific batch) test
     */
    public void testDelete()
    {
        if(batchModeDisabled()) return;

        long pk = getNewId();
        String nameMain = "testDelete_Main_" + pk;
        String nameSub = "testDelete_Sub_" + pk;

        MainObject main1 = new MainObject(pk, nameMain);
        SubObject sub = new SubObject(nameSub);
        main1.add(sub);
        main1.add(new SubObject(nameSub));

        broker.beginTransaction();
        broker.store(main1);
        Identity oid_main = broker.serviceIdentity().buildIdentity(main1);
        Identity oid_sub = broker.serviceIdentity().buildIdentity(sub);
        broker.delete(main1);
        broker.commitTransaction();


        MainObject newMain = (MainObject) broker.getObjectByIdentity(oid_main);
        assertNull(newMain);
        SubObject newSub = (SubObject) broker.getObjectByIdentity(oid_sub);
        assertNull(newSub);

        Criteria crit = new Criteria();
        crit.addLike("name", nameSub);
        Query q = QueryFactory.newQuery(SubObject.class, crit);
        Collection result = broker.getCollectionByQuery(q);
        assertEquals(0, result.size());
    }

    /**
     * A common (no specific batch) test
     */
    public void testEquals() throws Exception
    {
        if(batchModeDisabled()) return;

        long pk = getNewId();
        String nameMain = "testEquals_Main_" + pk;
        String nameSub = "testEquals_Sub_" + pk;

        MainObject main1 = new MainObject(pk, nameMain);
        main1.add(new SubObject(nameSub));
        MainObject main2 = new MainObject(pk, nameMain);
        main2.add(new SubObject(nameSub));

        broker.beginTransaction();
        broker.store(main1);
        // delete object before add new instance with same PK
        broker.delete(main1);
        broker.store(main2);
        broker.commitTransaction();

        // new PB instance
        super.tearDown();
        super.setUp();

        MainObject main3 = new MainObject(pk, nameMain);
        main3.add(new SubObject(nameSub));

        Identity oid = broker.serviceIdentity().buildIdentity(main1);
        broker.clearCache();
        MainObject main4 = (MainObject) broker.getObjectByIdentity(oid);

        assertEquals(main3, main4);
    }

    public void testDeleteInsert()
    {
        if(batchModeDisabled()) return;

        long pk = getNewId();
        String nameMain = "testDeleteInsert_Main_" + pk;
        String nameSub = "testDeleteInsert_Sub_" + pk;

        MainObject main1 = new MainObject(pk, nameMain);
        main1.add(new SubObject("normal_" + nameSub));
        broker.beginTransaction();
        broker.store(main1);
        broker.commitTransaction();

        // enable batch mode before start tx
        broker.serviceConnectionManager().setBatchMode(true);
        Identity oid = broker.serviceIdentity().buildIdentity(main1);
        broker.beginTransaction();
        broker.delete(main1);
        MainObject main2 = new MainObject(pk, nameMain);
        main2.add(new SubObject("updated_" + nameSub));
        broker.store(main2);

        broker.commitTransaction();

        broker.clearCache();
        MainObject newMain = (MainObject) broker.getObjectByIdentity(oid);
        assertNotNull(newMain);
        assertNotNull(newMain.getSubObjects());
        assertEquals(1, newMain.getSubObjects().size());
    }

    public void testBatchStatementsOrder()
    {
        if(batchModeDisabled()) return;

        String name = "testBatchStatementsOrder_" + System.currentTimeMillis();
        ConnectionManagerIF conMan = broker.serviceConnectionManager();
        // try to enable batch mode
        conMan.setBatchMode(true);
        broker.beginTransaction();

        ProductGroup pg1 = new ProductGroup();
        pg1.setName("ProductGroup#1_" + name);
        broker.store(pg1);

        conMan.executeBatch();

        Article a1 = new Article();
        a1.setArticleName(name);
        a1.setProductGroup(pg1);
        pg1.add(a1);
        broker.store(pg1);
        broker.store(a1);

        ProductGroup pg2 = new ProductGroup();
        pg2.setName("ProductGroup #2_" + name);
        broker.store(pg2);

        Article a2 = new Article();
        a2.setArticleName(name);
        a2.setProductGroup(pg2);
        pg2.add(a2);
        broker.store(a2);

        ProductGroup pg3 = new ProductGroup();
        pg3.setName("ProductGroup #3_" + name);
        broker.store(pg3);

        Article a3 = new Article();
        a3.setArticleName(name);
        a3.setProductGroup(pg3);
        pg3.add(a3);
        broker.store(a3);

        conMan.executeBatch();

        broker.delete(a1);

        conMan.executeBatch();

        broker.delete(pg1);
        broker.delete(a2);
        broker.delete(pg2);
        broker.delete(a3);
        broker.delete(pg3);
        broker.commitTransaction();


        broker.beginTransaction();
        pg3.getAllArticles().clear();
        broker.store(pg3);
        broker.delete(pg3);
        broker.store(pg3);
        broker.delete(pg3);
        conMan.executeBatch();
        broker.commitTransaction();
    }

    /**
     * collection-descriptor without inverse reference-descriptor
     */
    public void testBatchStatementsOrder2()
    {
        if(batchModeDisabled()) return;

        ConnectionManagerIF conMan = broker.serviceConnectionManager();
        broker.beginTransaction();

        Zoo zoo1 = new Zoo();
        zoo1.setName("BatchModeTest Zoo #1");
        broker.store(zoo1);

        conMan.executeBatch();

        Mammal m1 = new Mammal();
        m1.setName("BatchModeTest Mammal #1");
        m1.setAge(5);
        m1.setNumLegs(4);
        m1.setZooId(zoo1.getZooId());
        zoo1.getAnimals().add(m1);
        broker.store(m1);

        Zoo zoo2 = new Zoo();
        zoo2.setName("BatchModeTest Zoo #2");
        broker.store(zoo2);

        Mammal m2 = new Mammal();
        m2.setName("BatchModeTest Mammal #2");
        m2.setAge(5);
        m2.setNumLegs(4);
        m2.setZooId(zoo2.getZooId());
        zoo2.getAnimals().add(m2);
        broker.store(m2);

        Zoo zoo3 = new Zoo();
        zoo3.setName("BatchModeTest Zoo #3");
        broker.store(zoo3);

        Mammal m3 = new Mammal();
        m3.setName("BatchModeTest Mammal #3");
        m3.setAge(5);
        m3.setNumLegs(4);
        m3.setZooId(zoo3.getZooId());
        zoo3.getAnimals().add(m3);
        broker.store(m3);

        conMan.executeBatch();

        broker.delete(m1);

        conMan.executeBatch();

        broker.delete(zoo1);
        broker.delete(m2);
        broker.delete(zoo2);
        broker.delete(m3);
        broker.delete(zoo3);

        conMan.executeBatch();
        broker.commitTransaction();
    }

    public void testMassInsertDelete()
    {
        if(batchModeDisabled()) return;

        String name = "testMassInsert_" + System.currentTimeMillis();

        broker.serviceConnectionManager().setBatchMode(true);
        broker.beginTransaction();
        for(int i = 200 - 1; i >= 0; i--)
        {
            Person p = new Person();
            p.setFirstname("a mass test_" + i);
            p.setLastname(name);
            broker.store(p);
        }
        broker.commitTransaction();

        Criteria crit = new Criteria();
        crit.addLike("lastname", name);
        Query q = QueryFactory.newQuery(Person.class, crit);
        Collection result = broker.getCollectionByQuery(q);
        assertEquals(200, result.size());

        broker.beginTransaction();
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            broker.delete(iterator.next());
        }
        broker.commitTransaction();

        crit = new Criteria();
        crit.addLike("lastname", name);
        q = QueryFactory.newQuery(Person.class, crit);
        result = broker.getCollectionByQuery(q);
        assertEquals(0, result.size());
    }

    public void testBatchModeDeclaration() throws Exception
    {
        if(batchModeDisabled()) return;

        String name = "testBatchModeDeclaration_" + System.currentTimeMillis();

        broker.serviceConnectionManager().setBatchMode(true);
        broker.beginTransaction();
        Person p = new Person();
        p.setFirstname("a mass test");
        p.setLastname(name);
        broker.store(p);
        broker.commitTransaction();

        // new PB instance
        tearDown();
        setUp();

        broker.beginTransaction();
        broker.serviceConnectionManager().setBatchMode(true);
        p = new Person();
        p.setFirstname("a mass test");
        p.setLastname(name);
        broker.store(p);
        broker.commitTransaction();

        // new PB instance
        tearDown();
        setUp();
        broker.serviceConnectionManager().setBatchMode(true);
        broker.serviceConnectionManager().getConnection();
        broker.beginTransaction();
        broker.commitTransaction();

        // new PB instance
        tearDown();
        setUp();
        broker.serviceConnectionManager().setBatchMode(true);
        broker.serviceConnectionManager().getConnection();
        broker.beginTransaction();
        broker.abortTransaction();

        // new PB instance
        tearDown();
        setUp();
        broker.beginTransaction();
        broker.serviceConnectionManager().setBatchMode(true);
        broker.serviceConnectionManager().getConnection();
        broker.commitTransaction();

        // new PB instance
        tearDown();
        setUp();
        broker.beginTransaction();
        broker.serviceConnectionManager().setBatchMode(true);
        broker.serviceConnectionManager().getConnection();
        broker.abortTransaction();
    }

    /**
     * A common (no specific batch) test
     */
    public void testStoreDeleteStore()
    {
        if(batchModeDisabled()) return;

        long pk = getNewId();
        String nameMain = "testDelete_Main_" + pk;
        String nameSub = "testDelete_Sub_" + pk;

        MainObject main1 = new MainObject(pk, nameMain);
        main1.add(new SubObject(nameSub));
        main1.add(new SubObject(nameSub));

        broker.beginTransaction();
        broker.store(main1);
        broker.delete(main1);
        broker.store(main1);
        broker.delete(main1);
        broker.store(main1);
        broker.commitTransaction();

        Identity oid = broker.serviceIdentity().buildIdentity(main1);
        broker.clearCache();
        MainObject newMain = (MainObject) broker.getObjectByIdentity(oid);
        assertNotNull(newMain);
        assertEquals(2, newMain.getSubObjects().size());
        broker.clearCache();
        Criteria crit = new Criteria();
        crit.addLike("name", nameSub);
        Query q = QueryFactory.newQuery(SubObject.class, crit);
        Collection result = broker.getCollectionByQuery(q);
        assertEquals(2, result.size());
    }


    //==========================================================
    // inner classes used for testing
    //==========================================================
    public static class MainObject
    {
        private Long id;
        private String name;
        private Collection subObjects;

        public MainObject()
        {
        }

        public MainObject(long id, String name)
        {
            setId(new Long(id));
            setName(name);
        }

        public Long getId()
        {
            return id;
        }

        public void setId(Long id)
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

        public Collection getSubObjects()
        {
            return subObjects;
        }

        public void setSubObjects(Collection subObjects)
        {
            this.subObjects = subObjects;
        }

        public void add(SubObject obj)
        {
            if(subObjects == null)
            {
                subObjects = new ArrayList();
            }
            subObjects.add(obj);
        }

        public boolean equals(Object other)
        {
            if(other instanceof MainObject)
            {
                MainObject main = (MainObject) other;
                return ((name == null) ? main.name == null : name.equals(main.name))
                        && ((subObjects == null || subObjects.isEmpty())
                        ? (main.subObjects == null || main.subObjects.isEmpty())
                        : subObjects.equals(main.subObjects));
            }
            else
            {
                return false;
            }
        }
    }

    public static class SubObject
    {
        private Long id;
        private String name;
        private Long mainId;

        public SubObject()
        {
        }

        public SubObject(String name)
        {
            setName(name);
        }

        public Long getId()
        {
            return id;
        }

        public void setId(Long id)
        {
            this.id = id;
        }

        public Long getMainId()
        {
            return mainId;
        }

        public void setMainId(Long mainId)
        {
            this.mainId = mainId;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public boolean equals(Object other)
        {
            if(other instanceof SubObject)
            {
                SubObject sub = (SubObject) other;
                return (name == null) ? sub.name == null : name.equals(sub.name);
            }
            else
            {
                return false;
            }
        }

    }
}
