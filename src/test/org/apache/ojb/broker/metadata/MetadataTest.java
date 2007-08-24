package org.apache.ojb.broker.metadata;

import java.util.Iterator;

import junit.framework.TestCase;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.ObjectRepository;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.TestHelper;
import org.apache.ojb.broker.cache.ObjectCacheDefaultImpl;
import org.apache.ojb.broker.cache.ObjectCacheEmptyImpl;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.sequence.Repository;
import org.apache.ojb.broker.util.sequence.SequenceManagerHighLowImpl;
import org.apache.ojb.odmg.OJB;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * This TestClass tests the RepositoryPersitors facilities for
 * reading and writing a valid repository.
 */
public class MetadataTest extends TestCase
{
    public static final String TEST_CLASS_DESCRIPTOR = "Test_ClassDescriptor.xml";
    public static final String TEST_CONNECTION_DESCRIPTOR = "Test_ConnectionDescriptor.xml";
    public static final String TEST_REPOSITORY = "Test_Repository.xml";

    /**
     * A persistent object class
     */
    private static final Class TEST_CLASS = Repository.SMKey.class;

    public static void main(String[] args)
    {
        String[] arr = {MetadataTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public MetadataTest(String name)
    {
        super(name);
    }

    public void setUp()
    {
    }

    public void tearDown()
    {
        try
        {
            MetadataManager.getInstance().setEnablePerThreadChanges(false);
        }
        catch (Exception e)
        {
        }
    }

    public void testFindFirstConcreteClassDescriptor()
    {
        DescriptorRepository dr = MetadataManager.getInstance().getRepository();
        ClassDescriptor cld = dr.getDescriptorFor(Repository.SMInterface.class);
        ClassDescriptor firstConcrete = dr.findFirstConcreteClass(cld);
        assertFalse(firstConcrete.isInterface());
        assertFalse(firstConcrete.isAbstract());
        firstConcrete = dr.findFirstConcreteClass(cld);
        firstConcrete = dr.findFirstConcreteClass(cld);
    }

    public void testDescriptorRepository_1()
    {
        MetadataManager mm = MetadataManager.getInstance();
        DescriptorRepository dr = mm.copyOfGlobalRepository();
        // get an class/interface with extents
        ClassDescriptor cld = dr.getDescriptorFor(Repository.SMMax.class);

        int extentSize = cld.getExtentClasses().size();
        Class topLevelInterface = dr.getTopLevelClass(Repository.SMMax.class);
        Class topLevelExtent = dr.getTopLevelClass(Repository.SMMaxA.class);
        assertEquals(Repository.SMMax.class, topLevelInterface);
        assertEquals(Repository.SMMax.class, topLevelExtent);
        assertEquals(2, extentSize);

        dr.removeExtent(Repository.SMMaxA.class.getName());
        int extentSizeNew = cld.getExtentClasses().size();
        Class topLevelInterfaceNew = dr.getTopLevelClass(Repository.SMMax.class);
        Class topLevelExtentNew = dr.getTopLevelClass(Repository.SMMaxA.class);
        assertEquals(Repository.SMMax.class, topLevelInterfaceNew);
        assertEquals(Repository.SMMaxA.class, topLevelExtentNew);
        assertEquals(1, extentSizeNew);
    }

    public void testDescriptorRepository_2()
    {
        MetadataManager mm = MetadataManager.getInstance();
        DescriptorRepository dr = mm.copyOfGlobalRepository();
        // get an class/interface with extents
        ClassDescriptor cld = dr.getDescriptorFor(Repository.SMMax.class);
        int allSubClasses = dr.getAllConcreteSubclassDescriptors(cld).size();
        int allExtents = cld.getExtentClasses().size();
        int allExtentNames = cld.getExtentClassNames().size();
        assertEquals(allExtents, allExtentNames);

        dr.remove(Repository.SMMaxA.class);
        // after removing SMMaxA, SMMax interface lost 4 concrete extents (sub-classes)
        // be carefully in changing SMM*** metadata, could make fail this test
        int allSubClassesNew = dr.getAllConcreteSubclassDescriptors(cld).size();
        int allExtentsNew = cld.getExtentClasses().size();
        int allExtentNamesNew = cld.getExtentClassNames().size();
        assertEquals(allExtentsNew, allExtentNamesNew);
        assertEquals(allSubClasses - 4, allSubClassesNew);
    }

    public void testClassDescriptor_1()
    {
        MetadataManager mm = MetadataManager.getInstance();
        DescriptorRepository dr = mm.copyOfGlobalRepository();
        ClassDescriptor cld = dr.getDescriptorFor(ObjectRepository.Component.class);

        FieldDescriptor[] a = cld.getAutoIncrementFields();
        assertEquals("autoincrement field should be found", 1, a.length);
        FieldDescriptor target = cld.getFieldDescriptorByName("id");
        cld.removeFieldDescriptor(target);
        a = cld.getAutoIncrementFields();
        assertEquals("autoincrement PK should be deleted", 0, a.length);
        assertNull(cld.getFieldDescriptorByName("id"));

        cld.addFieldDescriptor(target);
        a = cld.getAutoIncrementFields();
        assertEquals("autoincrement field should be found", 1, a.length);
        assertNotNull(cld.getFieldDescriptorByName("id"));

    }

    public void testLoadingProfiles() throws Exception
    {
        PersistenceBroker broker = null;
        MetadataManager mm = MetadataManager.getInstance();
        try
        {
            mm.setEnablePerThreadChanges(true);
            DescriptorRepository dr_1 = mm.readDescriptorRepository(TEST_CLASS_DESCRIPTOR);
            // add some profiles
            mm.addProfile("global", mm.copyOfGlobalRepository());
            mm.addProfile("test", dr_1);

            // now load a specific profile
            mm.loadProfile("test");
            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            CldTestObject obj = new CldTestObject();
            obj.setName("testLoadingProfiles");
            try
            {
                broker.beginTransaction();
                broker.store(obj);
                broker.commitTransaction();
                try
                {
                    // try to find persistent object, only available in global
                    // repository profile
                    Class clazz = broker.getClassDescriptor(TEST_CLASS).getClassOfObject();
                    assertNull("We should not found this class-descriptor in profile", clazz);
                }
                catch (PersistenceBrokerException e)
                {
                    assertTrue(true);
                }
            }
            finally
            {
                broker.close();
            }

            //***************************************
            mm.removeProfile("test");
            try
            {
                mm.loadProfile("test");
                fail("Loading of profile should fail, but doesn't");
            }
            catch (Exception e)
            {
                // we expect exception
            }
            // now we load copy of global DescriptorRepository
            mm.loadProfile("global");
            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            Class clazz = broker.getClassDescriptor(TEST_CLASS).getClassOfObject();

            ObjectRepository.Component compChild = new ObjectRepository.Component();
            compChild.setName("MetadataTest_child");

            ObjectRepository.Component compParent = new ObjectRepository.Component();
            compParent.setName("MetadataTest_parent");
            compChild.setParentComponent(compParent);

            broker.beginTransaction();
            broker.store(compChild);
            broker.commitTransaction();

            Identity oid = new Identity(compChild, broker);
            broker.clearCache();
            compChild = (ObjectRepository.Component) broker.getObjectByIdentity(oid);

            assertNotNull(compChild);
            assertNotNull(compChild.getParentComponent());

            broker.close();
            assertEquals(TEST_CLASS, clazz);
            mm.removeAllProfiles();
            try
            {
                mm.loadProfile("global");
                fail("Loading of profile should fail, but doesn't");
            }
            catch (Exception e)
            {
            }

            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            clazz = broker.getClassDescriptor(TEST_CLASS).getClassOfObject();
            broker.close();
            assertEquals(TEST_CLASS, clazz);
        }
        finally
        {
            mm.setEnablePerThreadChanges(false);
            if(broker != null) broker.close();
        }


    }

    public void testRuntimeMergeConnectionDescriptor() throws Exception
    {

        MetadataManager mm = MetadataManager.getInstance();
        ConnectionRepository cr = mm.readConnectionRepository(TEST_CONNECTION_DESCRIPTOR);
        mm.mergeConnectionRepository(cr);

        ConnectionRepository mergedCR = mm.connectionRepository();
        JdbcConnectionDescriptor jcd = mergedCR.getDescriptor(new PBKey("runtime"));
        assertNotNull("Runtime merge of ConnectionRepository failed", jcd);
    }

    public void testRuntimeMergeDescriptorRepository() throws Exception
    {
        MetadataManager mm = MetadataManager.getInstance();
        DescriptorRepository dr = mm.readDescriptorRepository(TEST_CLASS_DESCRIPTOR);
        mm.mergeDescriptorRepository(dr);

        DescriptorRepository mergedDR = mm.getRepository();
        ClassDescriptor cld = mergedDR.getDescriptorFor(MetadataTest.CldTestObject.class);
        assertNotNull("Runtime merge of DescriptorRepository failed", cld);
    }

    /**
     * test to check PB create with PBKey
     */
    public void testLookupPB1()
    {
        PBKey key1 = new PBKey(TestHelper.DEF_JCD_ALIAS);
        PBKey key2 = new PBKey(TestHelper.DEF_JCD_ALIAS, TestHelper.DEF_USER, TestHelper.DEF_PASSWORD);
        PBKey key3 = new PBKey(TestHelper.FAR_AWAY_JCD_ALIAS);
        Query query = QueryFactory.newQuery(TEST_CLASS, new Criteria());

        PersistenceBroker broker = PersistenceBrokerFactory.createPersistenceBroker(key1);
        broker.getCount(query);
        broker.close();

        broker = PersistenceBrokerFactory.createPersistenceBroker(key2);
        broker.getCount(query);
        broker.close();

        broker = PersistenceBrokerFactory.createPersistenceBroker(key3);
        broker.getCount(query);
        broker.close();
    }

    /**
     * test to check PB create with PBKey
     */
    public void testLookupPB2()
    {
        PBKey key1 = new PBKey(TestHelper.DEF_JCD_ALIAS, "!!TestCase: This should fail!!", "nothing");
        Query query = QueryFactory.newQuery(TEST_CLASS, new Criteria());
        PersistenceBroker broker = PersistenceBrokerFactory.createPersistenceBroker(key1);
        // hsql is not very strict in user handling
        try
        {
            broker.getCount(query);
            fail("We excect a exception, because we pass a PBKey with user and password that doesn't exist");
        }
        catch (Exception e)
        {
            assertTrue(true);
        }
        broker.close();
    }

    /**
     * test to check database open
     */
    public void testLookupDatabase() throws Exception
    {
        String queryStr = "select allArticle from " + TEST_CLASS.getName();
        Implementation odmg = OJB.getInstance();
        Transaction tx;
        Database db = odmg.newDatabase();
        db.open(TestHelper.DEF_JCD_ALIAS, Database.OPEN_READ_WRITE);
        db.close();
        db = odmg.newDatabase();
        db.open(TestHelper.DEF_JCD_ALIAS + "#" +
                TestHelper.DEF_USER + "#" +
                TestHelper.DEF_PASSWORD, Database.OPEN_READ_WRITE);
        tx = odmg.newTransaction();
        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create(queryStr);
        query.execute();
        tx.commit();
        db.close();

        db = odmg.newDatabase();
        db.open(TestHelper.DEF_JCD_ALIAS, Database.OPEN_READ_WRITE);
        tx = odmg.newTransaction();
        tx.begin();
        OQLQuery query2 = odmg.newOQLQuery();
        query2.create(queryStr);
        query2.execute();
        tx.commit();
        db.close();
    }

    public void testTimeToCopyRepository()
    {
        DescriptorRepository dr = null;
        int loop = 5;
        long period = System.currentTimeMillis();
        for (int i = 0; i < loop; i++)
        {
            dr = MetadataManager.getInstance().copyOfGlobalRepository();
        }
        period = System.currentTimeMillis() - period;
        int descriptors = 0;
        Iterator it = dr.iterator();
        while (it.hasNext())
        {
            it.next();
            ++descriptors;
        }
        System.out.println("# Time to create a copy of " + descriptors + " class-descriptors: " + period / loop + " ms #");
    }

    public void testObjectCacheDeclarations()
    {
        DescriptorRepository dr = MetadataManager.getInstance()
                                    .readDescriptorRepository(TEST_REPOSITORY);
        ConnectionRepository cr = MetadataManager.getInstance().readConnectionRepository(TEST_REPOSITORY);

        ObjectCacheDescriptor ocd;
        ocd = ((JdbcConnectionDescriptor)cr.getAllDescriptor().get(0)).getObjectCacheDescriptor();
        assertNotNull(ocd);
        assertNotNull(ocd.getObjectCache());
        assertEquals(ObjectCacheEmptyImpl.class, ocd.getObjectCache());
        assertNotNull(ocd.getAttribute("attr_con"));
        assertNull("Wrong custom attribute found", ocd.getAttribute("attr_class"));
        assertEquals("555", ocd.getAttribute("attr_con"));

        ocd = dr.getDescriptorFor(CacheObject.class).getObjectCacheDescriptor();
        assertNotNull(ocd);
        assertNotNull(ocd.getObjectCache());
        assertEquals(ObjectCacheDefaultImpl.class, ocd.getObjectCache());
        assertNotNull(ocd.getAttribute("attr_class"));
        assertNull("Wrong custom attribute found", ocd.getAttribute("attr_con"));
        assertEquals("444", ocd.getAttribute("attr_class"));
    }

    public void testReadConnectionDescriptor()
    {
        JdbcConnectionDescriptor jcd = MetadataManager.getInstance().connectionRepository().
                getDescriptor(new PBKey("testConnection", "a user", "a password"));
        /* descriptor snip

        <jdbc-connection-descriptor
        jcd-alias="testConnection"
        default-connection="false"
        platform="Oracle"
        jdbc-level="1.0"
        driver="a driver"
        protocol="a protocol"
        subprotocol="a subprotocol"
        dbalias="myDbalias"
        username="a user"
        password="a password"
        eager-release="true"
        batch-mode="true"
        useAutoCommit="0"
        ignoreAutoCommitExceptions="true"
    >

        <object-cache class="org.apache.ojb.broker.cache.ObjectCacheEmptyImpl">
            <attribute attribute-name="cacheKey1" attribute-value="cacheValue1"/>
            <attribute attribute-name="cacheKey2" attribute-value="cacheValue2"/>
        </object-cache>

        <connection-pool
            maxActive="1"
            maxIdle="2"
            maxWait="3"
            minEvictableIdleTimeMillis="4"
            numTestsPerEvictionRun="5"
            testOnBorrow="true"
            testOnReturn="true"
            testWhileIdle="true"
            timeBetweenEvictionRunsMillis="6"
            whenExhaustedAction="2"
            validationQuery="a query"
            logAbandoned="true"
            removeAbandoned="true"
            removeAbandonedTimeout="8"
        />

        <sequence-manager className="org.apache.ojb.broker.util.sequence.SequenceManagerHighLowImpl">
            <attribute attribute-name="key1" attribute-value="value1"/>
            <attribute attribute-name="key2" attribute-value="value2"/>
        </sequence-manager>
    </jdbc-connection-descriptor>
        */
        // don't set it to true!!! This may break everything (2 default connections)
        assertEquals(false, jcd.isDefaultConnection());

        assertNotNull(jcd.getDbms());
        assertEquals("Oracle", jcd.getDbms());
        assertEquals(1.0d, jcd.getJdbcLevel(), 0.1);

        assertNotNull(jcd.getDriver());
        assertEquals("a driver", jcd.getDriver());

        assertNotNull(jcd.getProtocol());
        assertEquals("a protocol", jcd.getProtocol());

        assertNotNull(jcd.getSubProtocol());
        assertEquals("a subprotocol", jcd.getSubProtocol());

        assertNotNull(jcd.getDbAlias());
        assertEquals("myDbalias", jcd.getDbAlias());

        assertNotNull(jcd.getUserName());
        assertEquals("a user", jcd.getUserName());

        assertNotNull(jcd.getPassWord());
        assertEquals("a password", jcd.getPassWord());

        assertEquals(true, jcd.getEagerRelease());

        assertEquals(true, jcd.getBatchMode());

        assertEquals(0, jcd.getUseAutoCommit());
        assertEquals(true, jcd.isIgnoreAutoCommitExceptions());

        ObjectCacheDescriptor ocd = jcd.getObjectCacheDescriptor();
        assertNotNull(ocd);
        assertNotNull(ocd.getObjectCache());
        assertEquals(ocd.getObjectCache(), ObjectCacheEmptyImpl.class);
        assertNotNull(ocd.getAttribute("cacheKey1"));
        assertNotNull(ocd.getAttribute("cacheKey2"));
        assertEquals("cacheValue1", ocd.getAttribute("cacheKey1"));
        assertEquals("cacheValue2", ocd.getAttribute("cacheKey2"));

        ConnectionPoolDescriptor cpd = jcd.getConnectionPoolDescriptor();
        assertEquals(1, cpd.getMaxActive());
        assertEquals(2, cpd.getMaxIdle());
        assertEquals(3, cpd.getMaxWait());
        assertEquals(4, cpd.getMinEvictableIdleTimeMillis());
        assertEquals(5, cpd.getNumTestsPerEvictionRun());
        assertEquals(true, cpd.isTestOnBorrow());
        assertEquals(true, cpd.isTestOnReturn());
        assertEquals(true, cpd.isTestWhileIdle());
        assertEquals(6, cpd.getTimeBetweenEvictionRunsMillis());
        assertEquals(2, cpd.getWhenExhaustedAction());

        assertNotNull(cpd.getValidationQuery());
        assertEquals("a query", cpd.getValidationQuery());

        assertEquals(true, cpd.isLogAbandoned());
        assertEquals(true, cpd.isRemoveAbandoned());
        assertEquals(8, cpd.getRemoveAbandonedTimeout());

        SequenceDescriptor seq = jcd.getSequenceDescriptor();
        assertEquals(SequenceManagerHighLowImpl.class.getName(), seq.getSequenceManagerClass().getName());

        assertNotNull(seq.getAttribute("key1"));
        assertEquals("value1", seq.getAttribute("key1"));

        assertNotNull(seq.getAttribute("key2"));
        assertEquals("value2", seq.getAttribute("key2"));
    }


    // ======================================================================
    // inner test class
    // ======================================================================
    public static class CldTestObject
    {
        int id;
        String name;

        public CldTestObject()
        {
        }

        public CldTestObject(int id, String name)
        {
            this.id = id;
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

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    public static class CacheObject
    {
        Integer objId;
        String name;

        public Integer getObjId()
        {
            return objId;
        }

        public void setObjId(Integer objId)
        {
            this.objId = objId;
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
}
