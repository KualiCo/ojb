package org.apache.ojb.broker.cache;

import java.io.Serializable;
import java.util.Properties;

import org.apache.ojb.broker.Article;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.InterfaceArticle;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.metadata.ConnectionRepository;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.metadata.MetadataTest;
import org.apache.ojb.broker.query.QueryByIdentity;
import org.apache.ojb.broker.sequence.Repository;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.GUID;
import org.apache.ojb.junit.OJBTestCase;

/**
 * Do some basic tests using ObjectCache implementations.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @author <a href="mailto:thma@apache.org">Thomas Mahler</a>
 * @version $Id: ObjectCacheTest.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class ObjectCacheTest extends OJBTestCase
{
    static final String EXCLUDE_PACKAGE = "org.apache.ojb.broker.sequence";
    static final String EXCLUDE_PACKAGE_NOT_EXIST = "org.apache.ojb.broker.sequence.xyz";

    Class[] objectCacheImpls = new Class[]{
        // ObjectCacheEmptyImpl.class,
        ObjectCacheDefaultImpl.class,
        ObjectCacheTwoLevelImpl.class,
        ObjectCachePerBrokerImpl.class,
        ObjectCacheJCSImpl.class,
        ObjectCacheJCSPerClassImpl.class,
        ObjectCachePerClassImpl.class
    };

    Class old_ObjectCache;
    String[] old_CacheFilter;

    public ObjectCacheTest(String s)
    {
        super(s);
    }

    public static void main(String[] args)
    {
        String[] arr = {ObjectCacheTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Test the JCS cache implementation. In JCS config file the following
     * properties are set:
     * <br/>
     * jcs.region.org.apache.ojb.broker.cache.ObjectCacheTest$CacheObject.cacheattributes.MaxObjects=3
     * jcs.region.org.apache.ojb.broker.cache.ObjectCacheTest$CacheObject.cacheattributes.MaxMemoryIdleTimeSeconds=2
     * jcs.region.org.apache.ojb.broker.cache.ObjectCacheTest$CacheObject.cacheattributes.UseMemoryShrinker=true
     * jcs.region.org.apache.ojb.broker.cache.ObjectCacheTest$CacheObject.cacheattributes.ShrinkerIntervalSeconds=1
     */
    public void testJCSPerClassObjectCacheImplementation() throws Exception
    {
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        try
        {
            ObjectCache cache = new ObjectCacheJCSPerClassImpl(broker, null);

            CacheObject obj_1 = new CacheObject(null, "testJCSPerClassObjectCacheImplementation_1");
            Identity oid_1 = new Identity(obj_1, broker);
            CacheObject obj_2 = new CacheObject(null, "testJCSPerClassObjectCacheImplementation_2");
            Identity oid_2 = new Identity(obj_2, broker);
            CacheObject obj_3 = new CacheObject(null, "testJCSPerClassObjectCacheImplementation_2");
            Identity oid_3 = new Identity(obj_3, broker);

            cache.cache(oid_1, obj_1);
            cache.cache(oid_2, obj_2);

            // two objects should be found
            assertNotNull(cache.lookup(oid_1));
            assertNotNull(cache.lookup(oid_2));
            cache.cache(oid_3, obj_3);
            // we only allow two objects in cache region
            boolean bool = cache.lookup(oid_1) != null;
            bool = bool && cache.lookup(oid_2) != null;
            bool = bool && cache.lookup(oid_3) != null;
            assertFalse("We should not found all cached objects", bool);
            // idle time is 2 sec
            Thread.sleep(4000);
            assertNull(cache.lookup(oid_1));
            assertNull(cache.lookup(oid_2));
            assertNull(cache.lookup(oid_3));

        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
        finally
        {
            if(broker != null) broker.close();
        }
    }

    public void testObjectCacheDefaultImplTimeout() throws Exception
    {
        TestObjectDefaultCache obj = new TestObjectDefaultCache();
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        try
        {
            broker.beginTransaction();
            broker.store(obj);
            broker.commitTransaction();

            Identity oid = new Identity(obj, broker);
            obj = (TestObjectDefaultCache) broker.serviceObjectCache().lookup(oid);
            assertNotNull(obj);

            Thread.sleep(5000);
            obj = (TestObjectDefaultCache) broker.serviceObjectCache().lookup(oid);
            assertNull(obj);
        }
        finally
        {
            if(broker != null) broker.close();
        }
    }

    public void testObjectCacheDefaultImpl() throws Exception
    {
        String name = "testObjectCacheDefaultImpl_"+System.currentTimeMillis();
        TestObjectDefaultCache obj = new TestObjectDefaultCache();
        obj.setName(name);
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        try
        {
            broker.beginTransaction();
            broker.store(obj);
            broker.commitTransaction();

            Identity oid = new Identity(obj, broker);
            obj = (TestObjectDefaultCache) broker.serviceObjectCache().lookup(oid);
            assertNotNull(obj);
            assertEquals(name, obj.getName());

            // modify name
            String new_name = "modified_"+name;
            obj.setName(new_name);
            obj = (TestObjectDefaultCache) broker.getObjectByIdentity(oid);
            assertNotNull(obj);
            assertEquals("current version of cache should return the modified object", new_name, obj.getName());

            broker.removeFromCache(oid);
            obj = (TestObjectDefaultCache) broker.serviceObjectCache().lookup(oid);
            assertNull("Should be removed from cache", obj);
            obj = (TestObjectDefaultCache) broker.getObjectByIdentity(oid);
            assertNotNull(obj);
            assertEquals("Should return the unmodified object", name, obj.getName());
        }
        finally
        {
            if(broker != null) broker.close();
        }
    }

    /**
     * This test check the 'cacheExcludes' property and try to exclude a whole package from
     * caching.
     * @throws Exception
     */
    public void testCacheFilterFunctions() throws Exception
    {
        PersistenceBrokerFactory.releaseAllInstances();
        String old = null;
        try
        {
            MetadataManager mm = MetadataManager.getInstance();
            JdbcConnectionDescriptor jcd = mm.connectionRepository().getDescriptor(mm.getDefaultPBKey());
            if(jcd.getObjectCacheDescriptor().getObjectCache().equals(ObjectCacheEmptyImpl.class))
            {
                ojbSkipTestMessage("Doesn't work with " + ObjectCacheEmptyImpl.class + " as default cache.");
                return;
            }
            old = jcd.getAttribute(CacheDistributor.CACHE_EXCLUDES_STRING);
            jcd.addAttribute(CacheDistributor.CACHE_EXCLUDES_STRING, "org.apache.ojb.broker.sequence");

            PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            try
            {
                ObjectCache cache = broker.serviceObjectCache();
                CacheObject obj = new CacheObject(null, "CacheObject persistent obj");
                Identity oid = new Identity(obj, broker);


                Repository.SMKey filterOutPackageObject = new Repository.SMKey();
                filterOutPackageObject.setName("ObjectCacheTest: package filter");
                Identity filterOutPackageOid = new Identity(filterOutPackageObject, broker);

                Object result = null;
                cache.clear();
                result = cache.lookup(oid);
                assertNull(result);
                result = cache.lookup(filterOutPackageOid);
                assertNull(result);

                // cache it
                cache.cache(oid, obj);
                cache.cache(filterOutPackageOid, filterOutPackageObject);

                // lookup things
                result = cache.lookup(oid);
                assertNotNull(result);
                assertEquals(obj, result);
                result = cache.lookup(filterOutPackageOid);
                assertNull(result);
            }
            finally
            {
                jcd.addAttribute(CacheDistributor.CACHE_EXCLUDES_STRING, old);
                if (broker != null) broker.close();
            }
        }
        finally
        {
            PersistenceBrokerFactory.releaseAllInstances();
            PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            broker.close();
        }
    }

    /**
     * Check base caching functions of some cache implementations.
     *
     * @throws Exception
     */
    public void testSimpleObjectCacheFunctions() throws Exception
    {
        for (int i = 0; i < objectCacheImpls.length; i++)
        {
            PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            try
            {
                ObjectCache cache = (ObjectCache) ClassHelper.newInstance(
                            objectCacheImpls[i],
                            new Class[] {PersistenceBroker.class,Properties.class},
                            new Object[] {broker, null});
                checkBaseFunctions(broker, cache);
            }
            finally
            {
                if (broker != null) broker.close();
            }
        }
    }


    /**
     * Checks the base functions of the current ObjectCache implementation.
     *
     * @throws Exception
     */
    private void checkBaseFunctions(PersistenceBroker broker, ObjectCache cache) throws Exception
    {
        CacheObject obj = new CacheObject(null, "ObjectCache test");
        Identity oid = new Identity(obj, broker);
        CacheObject obj2 = new CacheObject(null, "ObjectCache test 2");
        Identity oid2 = new Identity(obj2, broker);
        cache.clear();
        Object result = cache.lookup(oid);
        assertNull(result);

        cache.cache(oid, obj);
        cache.cache(oid2, obj2);
        result = cache.lookup(oid);
        assertNotNull(result);
        assertEquals(obj, result);
        assertNotSame(obj2, result);

        cache.remove(oid);
        result = cache.lookup(oid);
        Object result2 = cache.lookup(oid2);
        assertNull(result);
        assertNotNull(result2);

        cache.clear();
        result = cache.lookup(oid);
        assertNull(result);
        result = cache.lookup(oid2);
        assertNull(result);
        // cache.clear();
    }

    /**
     * Test per class ObjectCache declaration. 'TestObjectEmptyCache'
     * class metadata declare an 'empty ObjectCache' implementation
     * as cache, CacheObject use the default ObjectCache implementation.
     * Thus we should found 'CacheObject' instance in cache, but NOT found
     * 'TestObjectEmptyCache' instance.
     */
    public void testPerClassCache() throws Exception
    {
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        JdbcConnectionDescriptor jcd = broker.serviceConnectionManager().getConnectionDescriptor();
        if(jcd.getObjectCacheDescriptor().getObjectCache().equals(ObjectCacheEmptyImpl.class))
        {
            ojbSkipTestMessage("Doesn't work with " + ObjectCacheEmptyImpl.class + " as default cache.");
            return;
        }
        String name = "testPerClassCache_" + System.currentTimeMillis();

        TestObjectEmptyCache obj = new TestObjectEmptyCache();
        obj.setName(name);
        CacheObject dummy = new CacheObject();
        dummy.setName(name);

        try
        {
            broker.beginTransaction();
            broker.store(obj);
            broker.store(dummy);
            broker.commitTransaction();

            Identity obj_oid = new Identity(obj, broker);
            Identity dummy_oid = new Identity(dummy, broker);
            ObjectCache cache = broker.serviceObjectCache();
            Object ret_obj = cache.lookup(obj_oid);
            Object ret_dummy = cache.lookup(dummy_oid);
            assertNotNull(ret_dummy);
            assertNull(ret_obj);
        }
        finally
        {
            if (broker != null && broker.isInTransaction()) broker.abortTransaction();
            if (broker != null) broker.close();
        }
    }

    /**
     * Read a specific jdbc-connction-descriptor at runtime, merge it with current
     * ConnectionRepository, lookup a specific PersistenceBroker instance, get ObjectCache.
     * This should be ObjectCacheEmptyImpl, because this is declared at jdbc-connection-descriptor
     * level.
     */
    public void testPerDatabaseCache()
    {
        ConnectionRepository cr = MetadataManager.getInstance()
                .readConnectionRepository(MetadataTest.TEST_REPOSITORY);
        MetadataManager.getInstance().mergeConnectionRepository(cr);

        PersistenceBroker pb = PersistenceBrokerFactory.createPersistenceBroker(new PBKey("runtime_2"));
        try
        {
            ObjectCache oc = pb.serviceObjectCache();
            CacheObject testObj = new CacheObject(null, "testPerDatabaseCache");
            Identity oid = new Identity(testObj, pb);
            oc.cache(oid, testObj);
            Object result = oc.lookup(oid);
            assertNull("We should not found this object in cache", result);
        }
        finally
        {
            if (pb != null && !pb.isClosed()) pb.close();
            MetadataManager.getInstance().connectionRepository().removeDescriptor(cr.getAllDescriptor().get(0));
        }
    }

    /**
     * This test checks if the caches of two different brokers are properly isolated.
     * changes made to an object in tx1 should not be visible in tx2 !
     * TODO: once we work without global cache only (e.g. intern temporary cache), this test should pass!
     */
    public void YYYtestCacheIsolation() throws Exception
    {
        Object[] pk = new Object[]{new Long(42)};
        Identity oid = new Identity(Article.class, InterfaceArticle.class, pk);

        GUID guid = new GUID();

        PersistenceBroker broker1 = PersistenceBrokerFactory.defaultPersistenceBroker();
        broker1.beginTransaction();

        Article a1 = (Article) broker1.getObjectByQuery(new QueryByIdentity(oid));
        String originalName = a1.getArticleName();
        a1.setArticleName(guid.toString());

// start a second transaction
        PersistenceBroker broker2 = PersistenceBrokerFactory.defaultPersistenceBroker();
        broker2.beginTransaction();

        Article a2 = (Article) broker2.getObjectByQuery(new QueryByIdentity(oid));

        assertEquals(guid.toString(), a1.getArticleName());
        assertEquals(originalName, a2.getArticleName());
        assertNotSame(a1, a2);


        broker1.commitTransaction();
        broker1.close();

        broker2.commitTransaction();
        broker2.close();
    }



    // **********************************************************************
    // inner class
    // **********************************************************************
    public static class CacheObject implements Serializable
    {
        private Integer objId;
        private String name;

        public CacheObject(Integer objId, String name)
        {
            this.objId = objId;
            this.name = name;
        }

        public CacheObject()
        {
        }

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

    /**
     * in class-descriptor ObjectCacheEmptyImpl class is declared
     * as cache implementation.
     */
    public static class TestObjectEmptyCache
    {
        private Integer id;
        private String name;

        public TestObjectEmptyCache()
        {
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

    public static class TestObjectDefaultCache extends TestObjectEmptyCache
    {
    }
}
