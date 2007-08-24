package org.apache.ojb.broker.sequence;

import org.apache.commons.lang.SerializationUtils;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.metadata.SequenceDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.util.sequence.SequenceManager;
import org.apache.ojb.broker.util.sequence.SequenceManagerException;
import org.apache.ojb.broker.util.sequence.SequenceManagerFactory;
import org.apache.ojb.broker.util.sequence.SequenceManagerHelper;
import org.apache.ojb.broker.util.sequence.SequenceManagerHighLowImpl;
import org.apache.ojb.broker.util.sequence.SequenceManagerNextValImpl;
import org.apache.ojb.broker.util.sequence.SequenceManagerSeqHiLoImpl;
import org.apache.ojb.broker.util.sequence.SequenceManagerStoredProcedureImpl;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.TestHelper;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.ObjectRepository;
import org.apache.ojb.broker.BookArticle;
import org.apache.ojb.broker.Article;
import org.apache.ojb.broker.CdArticle;
import org.apache.ojb.junit.OJBTestCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * Tests to verify SequenceManager implementations - All sequence
 * manager implementations have to pass these tests without failures.
 * <br>
 * Note: For the multi-threaded tests, the keys will be generated once for all tests.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: SequenceManagerTest.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class SequenceManagerTest extends OJBTestCase
{
    private static final String TEST_SEQUENCE_NAME = "TEST_SEQUENCE";
    /**
     * Max PK value for {@link Repository.SMMax} test class prepared
     * in database.
     */
    private static final int SMMAX_MAX_PK_VALUE = 131;
    /**
     * Error message.
     */
    private static final String SMMAX_FAIL_MESSAGE = "Expected " + SMMAX_MAX_PK_VALUE +
            ", something goes wrong when try to identify max PK id in the prepared database tables" +
            " - Check the ...SMMAX... database tables for id " + SMMAX_MAX_PK_VALUE +
            ", if id was found in one of the tables, test fails";

    private static final String DEF_FAIL_MESSAGE = "Found different max PK, expected the same";

    /*
    attributes for the multi-threaded key generation
    used in method generateKeys(). See test cases
    testSequenceGeneration
    testForLostKeys
    */
    private int loops = 1000;
    private int instances = 10;
    private Class targetClass = Repository.SMSameTableA.class;
    // end

    private int numberOfKeys = 200;

    private PersistenceBroker[] brokers;
    private ThreadGroup threadGroup;
    private static ArrayList generatedKeys;
    private static int keyCount;

    public SequenceManagerTest(String s)
    {
        super(s);
    }

    public static void main(String[] args)
    {
        String[] arr = {SequenceManagerTest.class.getName()};
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
     * Test support for classes with multiple autoincrement
     * fields - e.g. see repository for {@link Repository.SMKey}
     */
    public void testMultipleAutoincrement()
    {
        String MESSAGE = "Autoincrement field was not incremented: ";
        String name = "my test key " + System.currentTimeMillis();
        Repository.SMKey key = new Repository.SMKey();
        key.setName(name);
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        broker.beginTransaction();
        broker.store(key);
        broker.commitTransaction();
        assertEquals("Value was not store: " + key, name, key.getName());
        assertNotNull(MESSAGE + key, key.getIntegerKey());
        assertTrue(MESSAGE + key, (key.getIntKey() != 0));
        assertNotNull(MESSAGE + key, key.getLongKey());
        assertNotNull(MESSAGE + key, key.getStringKey());
//        System.out.println("## SMKey: \n"+key);

        Criteria cr = new Criteria();
        cr.addEqualTo("name", name);
        Query query = new QueryByCriteria(Repository.SMKey.class, cr);
        key = (Repository.SMKey) broker.getObjectByQuery(query);

        assertEquals("Value was not store: ", name, key.getName());
        assertNotNull(MESSAGE + key, key.getIntegerKey());
        assertTrue(MESSAGE + key, (key.getIntKey() != 0));
        assertNotNull(MESSAGE + key, key.getLongKey());
        assertNotNull(MESSAGE + key, key.getStringKey());
//        System.out.println("## SMKey: \n"+key);

        broker.close();
    }

    /**
     * Test the use of the 'sequence-name' field descriptor
     * attribute.
     */
    public void testSequenceNameAttribute() throws Exception
    {
        // sequence name used in the repository
        String fieldName = "stringKey";
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        FieldDescriptor field = broker.getClassDescriptor(Repository.SMKey.class).getFieldDescriptorByName(fieldName);
        String result = SequenceManagerHelper.buildSequenceName(broker, field, true);

        assertEquals(TEST_SEQUENCE_NAME, result);
        broker.close();
    }

    public void testAutoNaming() throws Exception
    {
        String jcdAlias = "testAutoNaming";
        PBKey tempKey = new PBKey(jcdAlias, TestHelper.DEF_KEY.getUser(), TestHelper.DEF_KEY.getPassword());
        MetadataManager mm = MetadataManager.getInstance();
        PersistenceBroker broker = null;
        try
        {
            JdbcConnectionDescriptor jcd = mm.connectionRepository().getDescriptor(TestHelper.DEF_KEY);
            jcd = (JdbcConnectionDescriptor) SerializationUtils.clone(jcd);
            // modify jcd copy
            jcd.setJcdAlias(jcdAlias);
            SequenceDescriptor sd = jcd.getSequenceDescriptor();
            assertNotNull("Can not find sequence-descriptor - check test", sd);
            // don't use autoNaming
            sd.addAttribute("autoNaming", "false");
            // add new connection descriptor to global base
            mm.connectionRepository().addDescriptor(jcd);

            // allow per thread changes of persistent object data
            mm.setEnablePerThreadChanges(true);
            DescriptorRepository dr = mm.copyOfGlobalRepository();
            ClassDescriptor cld = dr.getDescriptorFor(SMAutoNaming.class);
            FieldDescriptor field = cld.getAutoIncrementFields()[0];

            // set sequence name for persistent object to null
            field.setSequenceName(null);
            mm.setDescriptor(dr);

            broker = PersistenceBrokerFactory.createPersistenceBroker(tempKey);
            try
            {
                /*
                persistent object descriptor doesn't has a sequence name
                and autoNaming is false --> expect an exception
                */
                SMAutoNaming obj = new SMAutoNaming("testAutoNaming_1");
                sd = broker.serviceConnectionManager().getConnectionDescriptor().getSequenceDescriptor();
                assertTrue("false".equals(sd.getAttribute("autoNaming")));

                broker.beginTransaction();
                broker.store(obj);
                broker.commitTransaction();
                fail("If sequence manager implementation supports 'autoNaming' feature,"
                        +" this test should cause an exception (else ignore this failure).");
            }
            catch (PersistenceBrokerException e)
            {
                assertTrue(true);
                broker.abortTransaction();
            }

            try
            {
                /* attribute 'auto-naming' is still false,
                but now we set a sequence name for autoincrement field
                --> should pass
                */
                field.setSequenceName("AA_testAutoNaming_user_set");
                SMAutoNaming obj = new SMAutoNaming("testAutoNaming_2");
                broker.beginTransaction();
                broker.store(obj);
                broker.commitTransaction();
            }
            catch (PersistenceBrokerException e)
            {
                e.printStackTrace();
                broker.close();
                throw e;
            }

            try
            {
                // let OJB re-initialize sequence-manager
                broker.close();
                PersistenceBrokerFactory.releaseAllInstances();
                /*
                remove sequence name of autoincrement field
                but enable automatic sequence name generation
                --> should pass
                */
                field.setSequenceName(null);
                sd.addAttribute("autoNaming", "true");
                broker = PersistenceBrokerFactory.createPersistenceBroker(tempKey);
                SMAutoNaming obj = new SMAutoNaming("testAutoNaming_3");
                broker.beginTransaction();
                broker.store(obj);
                broker.commitTransaction();
            }
            catch (PersistenceBrokerException e)
            {
                e.printStackTrace();
                fail("Sequence key generation failed");
            }

        }
        finally
        {
            // cleanup
            if (broker != null) broker.close();
            mm.setEnablePerThreadChanges(false);
        }
    }

    /**
     * This test only works, when using
     * {@link org.apache.ojb.broker.util.sequence.SequenceManagerNextValImpl}
     * for sequence generation.
     */
    public void testDatabaseSequenceGeneration() throws Exception
    {
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        SequenceManager sm = SequenceManagerFactory.getSequenceManager(broker);
        if (!(sm instanceof SequenceManagerNextValImpl))
        {
            System.out.println("This test only works for SeqMan implementations using "
                    + SequenceManagerNextValImpl.class + " Skip test case.");
            broker.close();
            return;
        }
        int count = 0;
        FieldDescriptor idFld = broker.getClassDescriptor(
                Repository.SMDatabaseSequence.class).getAutoIncrementFields()[0];
        for (int i = 0; i < 10; i++)
        {
            Integer val = (Integer) sm.getUniqueValue(idFld);
            count += val.intValue();
            System.err.println("count " + count);
        }
        assertFalse("No keys generated", count == 0);
        broker.close();
    }

    /**
     * Test the max id search used in the standard sequence manager
     * implementations.
     */
    public void testMaxKeySearch1()
    {
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        FieldDescriptor field = null;

        // find max from classes using different tables
        // FieldDescriptor field = broker.getClassDescriptor(Repository.SMMax.class).getAutoIncrementFields()[0];
        // long result1 = SequenceManagerHelper.getMaxForExtent(broker, field);
        field = broker.getClassDescriptor(Repository.SMMaxA.class).getAutoIncrementFields()[0];
        long result2 = SequenceManagerHelper.getMaxForExtent(broker, field);
        field = broker.getClassDescriptor(Repository.SMMaxAA.class).getAutoIncrementFields()[0];
        long result3 = SequenceManagerHelper.getMaxForExtent(broker, field);
        field = broker.getClassDescriptor(Repository.SMMaxAB.class).getAutoIncrementFields()[0];
        long result4 = SequenceManagerHelper.getMaxForExtent(broker, field);
        field = broker.getClassDescriptor(Repository.SMMaxAAA.class).getAutoIncrementFields()[0];
        long result5 = SequenceManagerHelper.getMaxForExtent(broker, field);

        // assertEquals(SMMAX_FAIL_MESSAGE, SMMAX_MAX_PK_VALUE, result1);
        assertEquals(SMMAX_FAIL_MESSAGE, SMMAX_MAX_PK_VALUE, result2);
        assertEquals(SMMAX_FAIL_MESSAGE, SMMAX_MAX_PK_VALUE, result3);
        assertEquals(SMMAX_FAIL_MESSAGE, SMMAX_MAX_PK_VALUE, result4);
        assertEquals(SMMAX_FAIL_MESSAGE, SMMAX_MAX_PK_VALUE, result5);

        broker.close();
    }

    /**
     * Test the max id search used in the standard sequence manager
     * implementations.
     */
    public void testMaxKeySearch2()
    {
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        // find max from classes using the same table
        broker.beginTransaction();
        broker.store(new ObjectRepository.A());
        broker.store(new ObjectRepository.B());
        broker.store(new ObjectRepository.B1());
        broker.store(new ObjectRepository.C());
        broker.store(new ObjectRepository.D());
        broker.commitTransaction();
        long[] result = new long[5];
        FieldDescriptor field = broker.getClassDescriptor(ObjectRepository.A.class).getAutoIncrementFields()[0];
        result[0] = SequenceManagerHelper.getMaxForExtent(broker, field);
        field = broker.getClassDescriptor(ObjectRepository.B.class).getAutoIncrementFields()[0];
        result[1] = SequenceManagerHelper.getMaxForExtent(broker, field);
        field = broker.getClassDescriptor(ObjectRepository.C.class).getAutoIncrementFields()[0];
        result[2] = SequenceManagerHelper.getMaxForExtent(broker, field);
        field = broker.getClassDescriptor(ObjectRepository.D.class).getAutoIncrementFields()[0];
        result[3] = SequenceManagerHelper.getMaxForExtent(broker, field);
        field = broker.getClassDescriptor(ObjectRepository.B1.class).getAutoIncrementFields()[0];
        result[4] = SequenceManagerHelper.getMaxForExtent(broker, field);
        broker.close();

        for (int i = 0; i < result.length; i++)
        {
            for (int k = 0; k < result.length; k++)
            {
                if (!(result[i] == result[k]))
                {
                    fail(DEF_FAIL_MESSAGE);
                }
            }
        }
    }

    /**
     * Test the max id search used in the standard sequence manager
     * implementations.
     */
    public void testMaxKeySearch3()
    {
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        long[] result = new long[3];
        FieldDescriptor field = broker.getClassDescriptor(Article.class).getAutoIncrementFields()[0];
        result[0] = SequenceManagerHelper.getMaxForExtent(broker, field);
        // field = broker.getClassDescriptor(AbstractArticle.class).getAutoIncrementFields()[0];
        // result[1] = SequenceManagerHelper.getMaxForExtent(broker, field);
        field = broker.getClassDescriptor(BookArticle.class).getAutoIncrementFields()[0];
        result[1] = SequenceManagerHelper.getMaxForExtent(broker, field);
        // field = broker.getClassDescriptor(AbstractCdArticle.class).getAutoIncrementFields()[0];
        // result[2] = SequenceManagerHelper.getMaxForExtent(broker, field);
        field = broker.getClassDescriptor(CdArticle.class).getAutoIncrementFields()[0];
        result[2] = SequenceManagerHelper.getMaxForExtent(broker, field);
        broker.close();

        for (int i = 0; i < result.length; i++)
        {
            for (int k = 0; k < result.length; k++)
            {
                if (!(result[i] == result[k]))
                {
                    fail(DEF_FAIL_MESSAGE);
                }
            }
        }
    }

    /**
     * Tests if the generated id's are unique across extents.
     */
    public void testUniqueAcrossExtendsWithDifferentTables1() throws Exception
    {
        Class classOne = Repository.SMInterfaceExtendAAA.class;
        Class classTwo = Repository.SMInterfaceExtendBB.class;
        doKeyAnalysing(classOne, classTwo);
    }

    /**
     * Tests if the generated id's are unique across extents.
     */
    public void testUniqueAcrossExtendsWithDifferentTables2() throws Exception
    {
        Class classOne = Repository.SMInterfaceExtendAA.class;
        Class classTwo = Repository.SMInterfaceExtendB.class;
        doKeyAnalysing(classOne, classTwo);
    }

    /**
     * Tests if the generated id's are unique across extents.
     */
    public void testUniqueAcrossExtendsWithDifferentTables3() throws Exception
    {
        Class classOne = Repository.SMInterfaceExtendA.class;
        Class classTwo = Repository.SMInterfaceExtendAB.class;
        doKeyAnalysing(classOne, classTwo);
    }

    /**
     * Tests if the generated id's are unique across extents.
     */
    public void testUniqueAcrossExtendsWithSameTable1() throws Exception
    {
        Class classOne = Repository.SMSameTableAA.class;
        Class classTwo = Repository.SMSameTableBB.class;
        doKeyAnalysing(classOne, classTwo);
    }

    /**
     * Tests if the generated id's are unique across extents.
     */
    public void testUniqueAcrossExtendsWithSameTable3() throws Exception
    {
        Class classOne = Repository.SMSameTableA.class;
        Class classTwo = Repository.SMSameTableB.class;
        doKeyAnalysing(classOne, classTwo);
    }

    /**
     * Tests if the generated id's are unique across extents.
     */
    public void testUniqueAcrossExtendsWithSameTable4() throws Exception
    {
        Class classOne = ObjectRepository.A.class;
        Class classTwo = ObjectRepository.B.class;
        doKeyAnalysing(classOne, classTwo);
    }

    /**
     * Tests if the generated id's are unique across extents.
     */
    public void testUniqueAcrossExtendsWithSameTable5() throws Exception
    {
        Class classOne = ObjectRepository.B1.class;
        Class classTwo = ObjectRepository.C.class;
        doKeyAnalysing(classOne, classTwo);
    }

    private void doKeyAnalysing(Class classOne, Class classTwo) throws SequenceManagerException
    {
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        FieldDescriptor fieldOne = broker.getClassDescriptor(classOne).getAutoIncrementFields()[0];
        FieldDescriptor fieldTwo = broker.getClassDescriptor(classOne).getAutoIncrementFields()[0];

        List listOne = createKeyList(broker, fieldOne, numberOfKeys);
        List listTwo = createKeyList(broker, fieldTwo, numberOfKeys);
        for (int i = 0; i < listOne.size(); i++)
        {
            if (listTwo.contains(listOne.get(i)))
            {
                fail("\nFound double generated key " + listOne.get(i) +
                        " when generate keys for \n" + classOne + " with autoincrement field " + fieldOne +
                        " and \n" + classTwo + " with autoincrement field " + fieldTwo);
            }
        }
        broker.close();
    }

    private List createKeyList(PersistenceBroker broker, FieldDescriptor field, int number)
            throws SequenceManagerException
    {
        SequenceManager sm = SequenceManagerFactory.getSequenceManager(broker);
        List resultList = new ArrayList();
        int result;
        for (int i = 0; i < number; i++)
        {
            Integer val = (Integer) sm.getUniqueValue(field);
            result = val.intValue();
            resultList.add(new Integer(result));
        }
        return resultList;
    }

    /**
     * test case written by a user
     */
    public void testGetUniqueIdWithOneBroker() throws Exception
    {
        PersistenceBroker pb = PersistenceBrokerFactory.defaultPersistenceBroker();
        FieldDescriptor field = pb.getClassDescriptor(targetClass).getAutoIncrementFields()[0];
        Integer val = (Integer) pb.serviceSequenceManager().getUniqueValue(field);
        int id1 = val.intValue();
        val = (Integer) pb.serviceSequenceManager().getUniqueValue(field);
        int id2 = val.intValue();
        assertTrue(id1 != id2);
        assertTrue(id2 > id1);
        assertTrue("If the sequence manger implementation does not support continuous key generation" +
                " per PB instance, you could ignore this failure", (id2 - id1) == 1);
    }

    /**
     * Tests the generation of unique sequence numbers
     * in multi-threaded environment.
     */
    public void testSequenceGeneration()
    {
        long time = System.currentTimeMillis();
        generateKeys();
        time = System.currentTimeMillis() - time;
        System.out.println(this.getClass().getName() + ": " + time + " (ms) time for key generating");
        analyseUniqueness(generatedKeys);
    }

    /**
     * Tests to detect the lost of sequence numbers
     * in multi-threaded environments.
     */
    public void testForLostKeys()
    {
        generateKeys();
        TreeSet set = new TreeSet((List) generatedKeys.clone());
        if (set.isEmpty()) fail("No generated keys found");
        int result = ((Integer) set.last()).intValue() - ((Integer) set.first()).intValue() + 1;
        assertEquals("Sequence manager lost sequence numbers, this could be a failure or could be" +
                " the volitional behaviour of the sequence manager" +
                " - retry test case, check test case, check sequence manager implementation.", keyCount, result);
    }

    /**
     * Test for unique **continuous** key generation
     * across different PB instances.
     *
     * test case was written by a user - thanks.
     * this test was *commented out* by default, because
     * not all sequence manager implementations generate continuous keys
     * across different PB instances.
     */
    public void YYYtest_getUniqueIdWithTwoBrokers() throws Exception
    {
        PersistenceBroker pb = PersistenceBrokerFactory.defaultPersistenceBroker();
        PersistenceBroker pb2 = PersistenceBrokerFactory.defaultPersistenceBroker();
        FieldDescriptor field = pb.getClassDescriptor(targetClass).getAutoIncrementFields()[0];

        Integer val = (Integer) pb.serviceSequenceManager().getUniqueValue(field);
        int id1 = val.intValue();

        val = (Integer) pb2.serviceSequenceManager().getUniqueValue(field);
        int id2 = val.intValue();

        assertTrue(id1 != id2);
        assertTrue(id2 > id1);
        assertTrue((id2 - id1) == 1);

        val = (Integer) pb2.serviceSequenceManager().getUniqueValue(field);
        id1 = val.intValue();

        val = (Integer) pb.serviceSequenceManager().getUniqueValue(field);
        id2 = val.intValue();

        assertTrue(id1 != id2);
        assertTrue(id2 > id1);
        assertTrue((id2 - id1) == 1);

        val = (Integer) pb.serviceSequenceManager().getUniqueValue(field);
        id1 = val.intValue();

        val = (Integer) pb2.serviceSequenceManager().getUniqueValue(field);
        id2 = val.intValue();


        assertTrue(id1 != id2);
        assertTrue(id2 > id1);
        assertTrue((id2 - id1) == 1);
    }

    /**
     * Test case for internal use while developing!
     * Was commented out by default!
     */
    public void YYYtestSequenceManagerStoredProcedureImpl() throws Exception
    {
        JdbcConnectionDescriptor jcd = MetadataManager.getInstance().connectionRepository().
                getDescriptor(PersistenceBrokerFactory.getDefaultKey());
        SequenceDescriptor old_sd = (SequenceDescriptor) SerializationUtils.clone(jcd.getSequenceDescriptor());
        PersistenceBroker broker;
        try
        {
            jcd.setSequenceDescriptor(new SequenceDescriptor(jcd, SequenceManagerStoredProcedureImpl.class));
            PersistenceBrokerFactory.releaseAllInstances();
            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            SequenceManager sm = broker.serviceSequenceManager();
            if (!(sm instanceof SequenceManagerStoredProcedureImpl))
            {
                fail("testSM_StoredProcedure: Expected sequence manager implemenation was " +
                        SequenceManagerStoredProcedureImpl.class.getName());
                return;
            }
            // now we start the tests
            FieldDescriptor field = broker.getClassDescriptor(targetClass).getAutoIncrementFields()[0];
            sm.getUniqueValue(field);

            generatedKeys.clear();
// comment in
//            testSequenceGeneration();
//            testMultipleAutoincrement();
//            testSequenceNameAttribute();
            broker.close();
        }
        finally
        {
            if (old_sd != null)
            {

                PersistenceBrokerFactory.releaseAllInstances();
                jcd.setSequenceDescriptor(old_sd);
            }
        }
    }

    private void generateKeys()
    {
        // we generate the keys only once
        if (generatedKeys != null && generatedKeys.size() > 1) return;

        prepareKeyGeneration();
        
        System.out.println(
                this.getClass().getName() + ":\n" + instances + " threads generating " +
                loops + " keys per thread,\nusing target class " + targetClass);
        keyCount = 0;
        for (int i = 0; i < instances; i++)
        {
            SequenceManagerHandle handle = new SequenceManagerHandle(
                    brokers[i], targetClass, loops);
            new Thread(threadGroup, handle).start();
        }
        while (threadGroup.activeCount() > 0)
        {
            try
            {
                Thread.sleep(300);
                //System.out.print(".");
            }
            catch (InterruptedException e)
            {
            }
        }

        cleanupKeyGeneration();

        System.out.println("Generated keys: " + (generatedKeys != null ? "" + generatedKeys.size() : "no keys generated"));
    }

    private void cleanupKeyGeneration()
    {
        if (brokers != null)
        {
            for (int i = 0; i < instances; i++)
            {
                brokers[i].close();
            }
        }
        threadGroup = null;
        brokers = null;
    }

    private void prepareKeyGeneration()
    {
        if (generatedKeys == null) generatedKeys = new ArrayList();
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        SequenceManager sm = broker.serviceSequenceManager();
        int seqGrabSize = 0;
        // we need the SM grab size
        if (sm instanceof SequenceManagerSeqHiLoImpl || sm instanceof SequenceManagerHighLowImpl)
        {
            SequenceDescriptor sd = broker.serviceConnectionManager().getConnectionDescriptor().getSequenceDescriptor();
            String strSize = sd.getAttribute(SequenceManagerHighLowImpl.PROPERTY_GRAB_SIZE);
            if (strSize != null)
            {
                seqGrabSize = new Integer(strSize).intValue();
            }
        }
        broker.close();

        // the grab size have to be a factor of the loops number
        // to pass the 'testForLostKeys' test because we
        if (loops < seqGrabSize) loops = seqGrabSize;
        if (seqGrabSize != 0) loops = (loops / seqGrabSize) * seqGrabSize;

        brokers = new PersistenceBroker[instances];
        for (int i = 0; i < instances; i++)
        {
            brokers[i] = PersistenceBrokerFactory.defaultPersistenceBroker();
        }
        threadGroup = new ThreadGroup("sequenceManagerTG");
    }

    private void analyseUniqueness(ArrayList results)
    {
        System.out.println(this.getClass().getName() + ": Analyse generated keys");
        TreeSet set = new TreeSet();
        //only to test the test
        //set.add(new Integer(41001));
        Iterator it = ((List) results.clone()).iterator();
        Integer key;
        while (it.hasNext())
        {
            key = (Integer) it.next();
            if (set.contains(key))
            {
                fail("Found double generated key: " + key +
                        ". Check the used SequenceManager implementation");
            }
            set.add(key);
        }
        System.out.println(this.getClass().getName() + ": Last generated key was " +
                ((set.size() > 0) ? set.last() : " no generated keys found"));
        set.clear();
    }

    protected static synchronized void addResultList(List resultList)
    {
        System.out.println(" add " + resultList.size() + "generated Keys");
        if (resultList == null) return;
        generatedKeys.addAll(resultList);
    }

    protected static synchronized void countKey()
    {
        ++keyCount;
    }


    public void testObjectsFromAbstractBaseClass1() throws Exception
    {
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        try
        {
            SequenceManager sm = broker.serviceSequenceManager();
            FieldDescriptor fld_1 = broker.getClassDescriptor(SMObjectOne.class).getAutoIncrementFields()[0];
            FieldDescriptor fld_2 = broker.getClassDescriptor(SMObjectTwo.class).getAutoIncrementFields()[0];

            Object result_1 = sm.getUniqueValue(fld_1);
            Object result_2 = sm.getUniqueValue(fld_2);

            assertNotNull(result_1);
            assertNotNull(result_2);
            assertTrue(result_1 instanceof Integer);
            assertTrue(result_2 instanceof Integer);

            result_1 = sm.getUniqueValue(fld_1);
            result_2 = sm.getUniqueValue(fld_2);

            assertNotNull(result_1);
            assertNotNull(result_2);
            assertTrue(result_1 instanceof Integer);
            assertTrue(result_2 instanceof Integer);

            assertFalse("Should not have same ids", result_2.equals(result_1));
        }
        finally
        {
            if (broker != null) broker.close();
        }
    }

    public void testObjectsFromAbstractBaseClass2() throws Exception
    {
        long stamp = System.currentTimeMillis();
        String objectName_One = "testObjectsFromAbstractBaseClass2_objOne_" + stamp;
        String objectName_Two = "testObjectsFromAbstractBaseClass2_objTwo_" + stamp;

        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();

        Repository.SMSameTableBB dummy1 = new Repository.SMSameTableBB();
        Repository.SMInterfaceExtendA dummy2 = new Repository.SMInterfaceExtendA();

        SMObjectOne smOne_1 = new SMObjectOne(objectName_One);
        SMObjectOne smOne_2 = new SMObjectOne(objectName_One);

        SMObjectTwo smTwo_2 = new SMObjectTwo(objectName_Two);
        SMObjectTwo smTwo_1 = new SMObjectTwo(objectName_Two);
        try
        {
            broker.beginTransaction();

            broker.store(dummy1);
            broker.store(dummy2);

            broker.store(smOne_1);
            broker.store(smOne_2);
// broker.clearCache();
            broker.store(smTwo_2);
            broker.store(smTwo_1);

            broker.commitTransaction();

            // now check if store was successful
            broker.clearCache();

            Criteria cr = new Criteria();
            cr.addEqualTo("name", objectName_One);
            Query query = new QueryByCriteria(SMObjectOne.class, cr);
            Collection result = broker.getCollectionByQuery(query);

            broker.clearCache();

            Criteria cr_2 = new Criteria();
            cr_2.addEqualTo("name", objectName_Two);
            Query query_2 = new QueryByCriteria(SMObjectTwo.class, cr_2);
            Collection result_2 = broker.getCollectionByQuery(query_2);

            assertEquals("We have to found 2 SMObjectOne objects", 2, result.size());
            assertEquals("We have to found 2 SMObjectTwo objects", 2, result_2.size());
        }
        finally
        {
            if (broker != null) broker.close();
        }
    }

    public void testMassStoreOfObjects()
    {
        int outerLoops = 10;
        int innerLoops = 30;
        String name = "Name_" + System.currentTimeMillis();

        Repository.SMKey key = null;
        for (int i = outerLoops - 1; i >= 0; i--)
        {
            PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            try
            {
                broker.beginTransaction();
                for (int j = innerLoops - 1; j >= 0; j--)
                {
                    key = new Repository.SMKey();
                    key.setName(name);
                    broker.store(key);
                }
                broker.commitTransaction();
            }
            finally
            {
                if(broker != null) broker.close();
            }
        }
    }


    // ******************************************************************************
    // inner class
    // ******************************************************************************
    public static class AbstractSMObject implements Serializable
    {
        private Integer objectId;

        public Integer getObjectId()
        {
            return objectId;
        }

        public void setObjectId(Integer objectId)
        {
            this.objectId = objectId;
        }
    }

    public static class SMObjectOne extends AbstractSMObject
    {
        private String name;

        public SMObjectOne()
        {
        }

        public SMObjectOne(String name)
        {
            this.name = name;
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

    public static class SMObjectTwo extends AbstractSMObject
    {
        private String name;

        public SMObjectTwo()
        {
        }

        public SMObjectTwo(String name)
        {
            this.name = name;
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

    public static class SMAutoNaming extends AbstractSMObject
    {
        private String name;

        public SMAutoNaming()
        {
        }

        public SMAutoNaming(String name)
        {
            this.name = name;
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
