package org.apache.ojb.broker.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.ClassUtils;
import org.apache.ojb.broker.*;
import org.apache.ojb.broker.accesslayer.OJBIterator;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.sequence.Repository;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.junit.JUnitExtensions;

/**
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: MetadataMultithreadedTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class MetadataMultithreadedTest extends JUnitExtensions.MultiThreadedTestCase
{
    // we change table name in test for target class
    private String newTestObjectString = "SM_TAB_MAX_AA";
    private Class targetTestClass = Repository.SMMaxA.class;
    int loops = 7;
    int threads = 4;
    // need min 80% free memory after test campared with
    // beginning, else test fails
    int minimalFreeMemAfterTest = 80;

    private String oldTestObjectString;
    DescriptorRepository defaultRepository;

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public MetadataMultithreadedTest(String s)
    {
        super(s);
    }

    public static void main(String[] args)
    {
        String[] arr = {MetadataMultithreadedTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    private long getTotalMemory()
    {
        long result = Long.MAX_VALUE;
        // TODO: find a solution for this problem, or uncomment if we cancel 1.2 support
        // result = Runtime.getRuntime().maxMemory(); // not available in JDK 1.2
        return result;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        MetadataManager mm = MetadataManager.getInstance();
        // enable the per thread changes of metadata
        mm.setEnablePerThreadChanges(true);
        defaultRepository = mm.copyOfGlobalRepository();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        MetadataManager.getInstance().setEnablePerThreadChanges(false);
    }

    private String getTestObjectString()
    {
        return oldTestObjectString;
    }

    public void testProxiedLoading() throws Exception
    {
        PersistenceBroker broker = null;
        try
        {
            MetadataManager mm = MetadataManager.getInstance();
            // Store the current repository mappings under a profile key
            DescriptorRepository repository = mm.getRepository();
            String profileKey = "TestMappings";
            mm.addProfile(profileKey, repository);

            // "Destroy" this thread's mappings
            mm.setDescriptor(defaultRepository);

            ProductGroupWithCollectionProxy pgTemplate = new ProductGroupWithCollectionProxy();
            pgTemplate.setGroupId(new Integer(6));
            Query query = QueryFactory.newQueryByExample(pgTemplate);

            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            Collection groups;
            Iterator groupIter;
            ProductGroupWithCollectionProxy pg;

            assertNotNull(groupIter = (OJBIterator) broker.getIteratorByQuery(query));
            assertTrue(groupIter.hasNext());

            // We have not named any OJB profiles, so using dynamic proxies at this stage is not
            // supported
            Throwable expectedThrowable = null;
            try {
                System.err.println("------ The following exception is part of the tests...");
                groupIter.next();
            } catch (Throwable t) {
                expectedThrowable = t;
                System.err.println("------");
            }
            assertNotNull("Should get metadata exception from proxy", expectedThrowable);
            ((OJBIterator) groupIter).releaseDbResources();

            // Load the repository profile and re-try loading.
            broker.clearCache();
            mm.loadProfile(profileKey);
            assertNotNull(groups = broker.getCollectionByQuery(query));
            assertEquals(1, groups.size());
            assertNotNull(groupIter = groups.iterator());
            assertTrue(groupIter.hasNext());
            assertNotNull(pg = (ProductGroupWithCollectionProxy) groupIter.next());
            assertFalse(groupIter.hasNext());
            assertEquals(pgTemplate.getGroupId(), pg.getGroupId());
            Collection articles;
            assertNotNull(articles = pg.getAllArticlesInGroup());
            assertEquals(6, articles.size());

            TestCaseRunnable tct [] = new TestCaseRunnable[]{new LazyLoading(articles)};
            runTestCaseRunnables(tct);
        }
        finally
        {
            if (broker != null) broker.close();
        }

    }

    /**
     * Regression test for loading CollectionProxy data in a thread where the profile
     * is swapped, and the thread-local broker is closed, between CollectionProxy construction
     * and retrieveCollections-call.
     * @throws Exception on unexpected failure
     */
    public void testCollectionProxySwapProfiles() throws Exception
    {
        PersistenceBroker broker = null;
        try
        {
            MetadataManager mm = MetadataManager.getInstance();

            // Store the current repository mappings under a profile key and load it
            DescriptorRepository repository = mm.getRepository();
            String profileKey = "TestMappings";
            mm.addProfile(profileKey, repository);
            mm.loadProfile(profileKey);

            // Load object and proxy
            ProductGroupWithCollectionProxy pgTemplate = new ProductGroupWithCollectionProxy();
            pgTemplate.setGroupId(new Integer(6));
            Query query = QueryFactory.newQueryByExample(pgTemplate);
            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            ProductGroupWithCollectionProxy productGroup;
            assertNotNull(productGroup =
                    (ProductGroupWithCollectionProxy) broker.getObjectByQuery(query));

            // Close broker to make sure proxy needs a new internal one
            broker.close();

            // Swap profile (to a completely empty one)
            final String emptyKey = "EMPTY";
            DescriptorRepository emptyDr = new DescriptorRepository();
            mm.addProfile(emptyKey, emptyDr);
            mm.loadProfile(emptyKey);

            List collectionProxy = productGroup.getAllArticlesInGroup();
            assertNotNull(collectionProxy);

            // Load proxy data, will throw ClassNotPersistenceCapableException
            // if not reactivating profile with new thread-local broker
            assertNotNull(collectionProxy.get(0));
        }
        finally
        {
            if (broker != null) broker.close();
        }
    }

    public void testRuntimeMetadataChanges() throws Exception
    {
        PersistenceBroker broker = null;
        try
        {
            MetadataManager.getInstance().setDescriptor(defaultRepository);

            ClassDescriptor cld;
            long memoryUseBeforeTest;
            long memoryUseAfterTest;
            try
            {
                // prepare for test
                long period = System.currentTimeMillis();
                broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                cld = broker.getClassDescriptor(targetTestClass);

                // we manipulate the schema name of the class
                // thus we note the original value
                oldTestObjectString = cld.getFullTableName();
                broker.close();

                // cleanup JVM
                Runtime.getRuntime().gc();
                Thread.sleep(200);
                Runtime.getRuntime().gc();

                // start test
                long memory = Runtime.getRuntime().freeMemory();
                long totalMemory = getTotalMemory();

                int count = 0;
                for (int k = 0; k < loops; k++)
                {
                    TestCaseRunnable tct [] = new TestCaseRunnable[threads];
                    for (int i = 0; i < threads; i++)
                    {
                        if (i % 2 == 0)
                            tct[i] = new ThreadedUsingBroker(loops);
                        else
                            tct[i] = new GlobalUsingBroker(loops);
                    }
                    // run test classes
                    runTestCaseRunnables(tct);
                    ++count;
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Free/total Memory after loop " + count + ":          "
                                     + convertToMB(Runtime.getRuntime().freeMemory())
                                     + "/" + convertToMB(getTotalMemory()) + "MB");
                    }
                }
                period = System.currentTimeMillis() - period;
                if (logger.isDebugEnabled())
                {
                    logger.debug(ClassUtils.getShortClassName(MetadataMultithreadedTest.class) + " take: "
                                 + period + " ms for " + loops + " loops, creating each with " + threads + " threads");
                    logger.debug("Free/total Memory before test:          "
                                 + convertToMB(memory) + "/" + convertToMB(totalMemory) + "MB");
                }
                Runtime.getRuntime().gc();
                Thread.sleep(200);
                Runtime.getRuntime().gc();
                Runtime.getRuntime().gc();

                memoryUseBeforeTest = convertToMB(memory);
                memoryUseAfterTest = convertToMB(Runtime.getRuntime().freeMemory());
                if (logger.isDebugEnabled())
                {
                    logger.debug("Free/total Memory after test and gc:   "
                                 + memoryUseAfterTest
                                 + "/" + convertToMB(getTotalMemory()) + "MB");
                    logger.debug("Do cleanup now ...");
                }
            }
            finally
            {
                MetadataManager.getInstance().setEnablePerThreadChanges(false);
            }
            // get new PB instance
            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            cld = broker.getClassDescriptor(targetTestClass);
            String name = cld.getFullTableName();
            assertEquals(oldTestObjectString, name);
            assertFalse(MetadataManager.getInstance().isEnablePerThreadChanges());
            double d = ((double) memoryUseAfterTest) / ((double) memoryUseBeforeTest);
            int result = (int) (d * 100);
            if (result < minimalFreeMemAfterTest)
            {
                fail("** When using a offical version of OJB, ignore this failure! **" +
                        " Memory usage after this test differs more than "+(100 - minimalFreeMemAfterTest)
                        +"% from beginning, this may indicate" +
                        " a memory leak (GC can't free unused metadata objects), but this could also be a result" +
                        " of your JVM settings. Please re-run test.");
            }
        }
        finally
        {
            if (broker != null) broker.close();
        }
    }

    private long convertToMB(long byteValue)
    {
        return (byteValue / 1024) / 1024;
    }

    // ======================================================================
    // inner test class
    // ======================================================================
    class ThreadedUsingBroker extends JUnitExtensions.MultiThreadedTestCase.TestCaseRunnable
    {
        int loops;
        String title = "ThreadedUsingBroker_" + System.currentTimeMillis();

        public ThreadedUsingBroker()
        {
        }

        public ThreadedUsingBroker(int loops)
        {
            this.loops = loops;
        }

        public void runTestCase() throws Exception
        {
            MetadataManager mm = MetadataManager.getInstance();
            DescriptorRepository dr = mm.copyOfGlobalRepository();
            ClassDescriptor cld = dr.getDescriptorFor(targetTestClass);
            // we change a class descriptor value
            cld.setTableName(newTestObjectString);
            // set the changed repository for this thread
            mm.setDescriptor(dr);

            int k = 0;
            while (k < loops)
            {
                PersistenceBroker broker = null;
                try
                {
                    broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                    cld = broker.getClassDescriptor(targetTestClass);
                    String name = cld.getFullTableName();
                    assertEquals(newTestObjectString, name);
                    assertTrue(MetadataManager.getInstance().isEnablePerThreadChanges());
                }
                finally
                {
                    if (broker != null) broker.close();
                }

                try
                {
                    broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                    // check made changes
                    cld = broker.getClassDescriptor(targetTestClass);
                    String name = cld.getFullTableName();
                    assertEquals(newTestObjectString, name);
                    assertTrue(MetadataManager.getInstance().isEnablePerThreadChanges());

                    // query a test object
                    Query query = new QueryByCriteria(Person.class, null, true);
                    broker.getCollectionByQuery(query);
                    // store target object
                    /*
                    store some complex objects to check if references to
                    metadata classes are cached
                    */
                    Project project = new Project();
                    project.setTitle(title);

                    Person p1 = new Person();
                    p1.setFirstname(title);
                    List l1 = new ArrayList();
                    l1.add(project);
                    p1.setProjects(l1);

                    Person p2 = new Person();
                    p2.setFirstname(title);
                    List l2 = new ArrayList();
                    l2.add(project);
                    p2.setProjects(l2);

                    Role r1 = new Role();
                    r1.setPerson(p1);
                    r1.setRoleName(title);
                    r1.setProject(project);
                    List roles1 = new ArrayList();
                    roles1.add(r1);

                    Role r2 = new Role();
                    r2.setPerson(p2);
                    r2.setRoleName(title);
                    r2.setProject(project);
                    List roles2 = new ArrayList();
                    roles2.add(r2);

                    p1.setRoles(roles1);
                    p2.setRoles(roles2);

                    Object obj = ClassHelper.newInstance(targetTestClass);

                    broker.beginTransaction();
                    broker.store(obj);
                    broker.store(p1);
                    broker.store(p2);
                    broker.commitTransaction();
                    // delete target object
                    broker.beginTransaction();
                    broker.delete(obj);
                    //broker.delete(p1);
                    //broker.delete(p2);
                    broker.commitTransaction();
                }
                finally
                {
                    if (broker != null) broker.close();
                }

                k++;
                try
                {
                    Thread.sleep(5);
                }
                catch (InterruptedException e)
                {
                }
            }

        }
    }

    // ======================================================================
    // inner test class
    // ======================================================================
    class GlobalUsingBroker extends JUnitExtensions.MultiThreadedTestCase.TestCaseRunnable
    {
        int loops;

        public GlobalUsingBroker(int loops)
        {
            this.loops = loops;
        }

        public void runTestCase()
        {
            PersistenceBroker broker = null;
            int k = 0;
            try
            {
                while (k < loops)
                {
                    try
                    {
                        MetadataManager.getInstance().setDescriptor(defaultRepository);
                        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                        ClassDescriptor cld = broker.getClassDescriptor(targetTestClass);
                        assertTrue(MetadataManager.getInstance().isEnablePerThreadChanges());
                        String name = cld.getFullTableName();
                        // this PB instance use unchanged global metadata repository
                        assertEquals(getTestObjectString(), name);
                    }
                    finally
                    {
                        if (broker != null) broker.close();
                    }
                    try
                    {
                        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                        ClassDescriptor cld = broker.getClassDescriptor(targetTestClass);
                        assertTrue(MetadataManager.getInstance().isEnablePerThreadChanges());
                        String name = cld.getFullTableName();
                        // this PB instance use unchanged global metadata repository
                        assertEquals(getTestObjectString(), name);
                        // System.out.println("Default: found "+name);

                        // query a test object
                        Query query = new QueryByCriteria(Person.class, null, true);
                        broker.getCollectionByQuery(query);
                        // store target object
                        Object obj = ClassHelper.newInstance(targetTestClass);
                        broker.beginTransaction();
                        broker.store(obj);
                        broker.commitTransaction();
                        // delete target object
                        broker.beginTransaction();
                        broker.delete(obj);
                        broker.commitTransaction();
                    }
                    finally
                    {
                        if (broker != null) broker.close();
                    }

                    k++;
                    try
                    {
                        Thread.sleep(5);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new OJBRuntimeException(e);
            }
        }
    }

    /**
     * Inner test class for lazy materialization of CollectionProxy in different thread.
     */
    protected class LazyLoading extends JUnitExtensions.MultiThreadedTestCase.TestCaseRunnable
    {
        private Collection articles;

        public LazyLoading(Collection articles)
        {
            assertNotNull(this.articles = articles);
        }

        public void runTestCase() throws Throwable
        {
            // Explicitly clear descriptor repository in this thread (similar to loading
            // profile with unrelated class-mappings).
            DescriptorRepository dr = new DescriptorRepository();
            MetadataManager.getInstance().setDescriptor(dr);
            Article article;
            int numArticles = 0;
            for (Iterator iterator = articles.iterator(); iterator.hasNext();)
            {
                assertNotNull(article = (Article) iterator.next());
                assertNotNull(article.getArticleId());
                assertFalse(new Integer(0).equals(article.getArticleId()));
                numArticles++;
            }
            assertEquals(6, numArticles);
        }
    }

}
