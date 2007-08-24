package org.apache.ojb.broker.sequence;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.TestHelper;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.SequenceDescriptor;
import org.apache.ojb.broker.platforms.Platform;
import org.apache.ojb.broker.platforms.PlatformHsqldbImpl;
import org.apache.ojb.broker.platforms.PlatformMySQLImpl;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.util.sequence.SequenceManagerNativeImpl;
import org.apache.ojb.junit.PBTestCase;
import org.apache.ojb.odmg.OJB;
import org.apache.ojb.odmg.TransactionExt;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.Transaction;

/**
 * Test case for {@link SequenceManagerNativeImpl}. These test check
 * support for native identity columns. Test case only works for
 * Hsql and Mysql.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: NativeIdentifierTest.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class NativeIdentifierTest extends PBTestCase
{
    // Statements for MainObject table
    private static final String DROP = "DROP TABLE NATIVE_MAIN_OBJECT";
    private static final String CREATE_MYSQL =
            "CREATE TABLE NATIVE_MAIN_OBJECT(NATIVE_ID int(11) NOT NULL PRIMARY KEY" +
            " auto_increment,REF_ID int(11),NAME VARCHAR(250))";
    private static final String CREATE_HSQL =
            "CREATE TABLE NATIVE_MAIN_OBJECT(NATIVE_ID IDENTITY NOT NULL PRIMARY KEY," +
            " REF_ID int,NAME VARCHAR(250))";

    private static final String INSERT_DUMMY = "INSERT INTO NATIVE_MAIN_OBJECT (NAME) VALUES ('Dummy_1')";

    private static final String ADD_CONSTRAINT =
        "ALTER TABLE NATIVE_MAIN_OBJECT" +
        " ADD CONSTRAINT MAIN_REF_FK" +
        " FOREIGN KEY (REF_ID) REFERENCES NATIVE_REFERENCE_OBJECT (NATIVE_ID)";
    private static final String DROP_CONSTRAINT_HSQL =
        "ALTER TABLE NATIVE_MAIN_OBJECT" +
        " DROP CONSTRAINT MAIN_REF_FK";
    private static final String DROP_CONSTRAINT_MYSQL =
        "ALTER TABLE NATIVE_MAIN_OBJECT" +
        " DROP FOREIGN KEY MAIN_REF_FK";


    // Statements for NATIVE_REF_TEST table
    private static final String CREATE_REF_MYSQL =
            "CREATE TABLE NATIVE_REFERENCE_OBJECT (NATIVE_ID int(11) NOT NULL PRIMARY KEY auto_increment," +
            " NAME VARCHAR(250), OJB_CONCRETE_CLASS VARCHAR(250), FK_ID int, REF_ID int(11), SINGLE_REF_FK BIGINT" +
    		" , FOREIGN KEY (FK_ID) REFERENCES NATIVE_MAIN_OBJECT (NATIVE_ID) )";
    private static final String CREATE_REF_HSQL =
            "CREATE TABLE NATIVE_REFERENCE_OBJECT (NATIVE_ID IDENTITY NOT NULL PRIMARY KEY," +
            " NAME VARCHAR(250), OJB_CONCRETE_CLASS VARCHAR(250), FK_ID int, REF_ID int, SINGLE_REF_FK BIGINT" +
            " , FOREIGN KEY (FK_ID) REFERENCES NATIVE_MAIN_OBJECT (NATIVE_ID) )";
    private static final String DROP_REF = "DROP TABLE NATIVE_REFERENCE_OBJECT";
    private static final String INSERT_DUMMY_REF = "INSERT INTO NATIVE_REFERENCE_OBJECT (NAME) VALUES ('Dummy_2')";

    private Platform platform;
    private Class oldSequenceManager;

    public NativeIdentifierTest(String s)
    {
        super(s);
    }

    public static void main(String[] args)
    {
        String[] arr = {NativeIdentifierTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    private boolean skipTest() throws Exception
    {
        return !((platform instanceof PlatformMySQLImpl) || (platform instanceof PlatformHsqldbImpl));
    }

    public void setUp() throws Exception
    {
        super.setUp();

        platform = broker.serviceConnectionManager().getSupportedPlatform();
        if (skipTest()) return;

        Connection con;
        Statement stmt;

        PersistenceBroker pb = PersistenceBrokerFactory.defaultPersistenceBroker();
        try
        {
            con = pb.serviceConnectionManager().getConnection();
            stmt = con.createStatement();
            try
            {
                if(platform instanceof PlatformMySQLImpl)
                {
                    stmt.execute(DROP_CONSTRAINT_MYSQL);
                }
                else if(platform instanceof PlatformHsqldbImpl)
                {
                    stmt.execute(DROP_CONSTRAINT_HSQL);
                }
            }
            catch (SQLException e)
            {
            }
            stmt.close();

            stmt = con.createStatement();
            try
            {
                stmt.execute(DROP_REF);
            }
            catch (SQLException e)
            {
            }
            stmt.close();

            stmt = con.createStatement();
            try
            {
                stmt.execute(DROP);
            }
            catch (SQLException e)
            {
            }
            stmt.close();
        }
        finally
        {
            if (pb != null) pb.close();
        }


        try
        {
            con = broker.serviceConnectionManager().getConnection();
            if(platform instanceof PlatformMySQLImpl)
            {
                stmt = con.createStatement();
                stmt.execute(CREATE_MYSQL);
                stmt.close();
            }
            if(platform instanceof PlatformHsqldbImpl)
            {
                stmt = con.createStatement();
                stmt.execute(CREATE_HSQL);
                stmt.close();
            }

            stmt = con.createStatement();
            stmt.execute(INSERT_DUMMY);
            stmt.close();

            if(platform instanceof PlatformMySQLImpl)
            {
                stmt = con.createStatement();
                stmt.execute(CREATE_REF_MYSQL);
                stmt.close();
            }
            if(platform instanceof PlatformHsqldbImpl)
            {
                stmt = con.createStatement();
                stmt.execute(CREATE_REF_HSQL);
                stmt.close();
            }

            stmt = con.createStatement();
            stmt.execute(ADD_CONSTRAINT);
            stmt.close();

            stmt = con.createStatement();
            stmt.execute(INSERT_DUMMY_REF);
            stmt.close();

            SequenceDescriptor sd = MetadataManager.getInstance().connectionRepository().
                    getDescriptor(broker.getPBKey()).getSequenceDescriptor();
            oldSequenceManager = sd.getSequenceManagerClass();
            sd.setSequenceManagerClass(SequenceManagerNativeImpl.class);
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            if (broker != null) broker.close();
        }

        PersistenceBrokerFactory.releaseAllInstances();
        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        SequenceDescriptor sd = MetadataManager.getInstance().connectionRepository().
                getDescriptor(broker.getPBKey()).getSequenceDescriptor();
        assertEquals(SequenceManagerNativeImpl.class, sd.getSequenceManagerClass());
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
        if (skipTest()) return;

        Connection con;
        Statement stmt;
        PersistenceBroker pb = PersistenceBrokerFactory.defaultPersistenceBroker();
        try
        {
            con = pb.serviceConnectionManager().getConnection();

            stmt = con.createStatement();
            if(platform instanceof PlatformMySQLImpl)
            {
                stmt.execute(DROP_CONSTRAINT_MYSQL);
            }
            else if(platform instanceof PlatformHsqldbImpl)
            {
                stmt.execute(DROP_CONSTRAINT_HSQL);
            }
            stmt.close();

            stmt = con.createStatement();
            stmt.execute(DROP_REF);
            stmt.close();

            stmt = con.createStatement();
            stmt.execute(DROP);
            stmt.close();

            SequenceDescriptor sd = MetadataManager.getInstance().connectionRepository().
                    getDescriptor(pb.getPBKey()).getSequenceDescriptor();
            sd.setSequenceManagerClass(oldSequenceManager);
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            if (pb != null)
            {
                pb.clearCache();
                pb.close();
            }
        }

        PersistenceBrokerFactory.releaseAllInstances();
        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        SequenceDescriptor sd = MetadataManager.getInstance().connectionRepository().
                getDescriptor(broker.getPBKey()).getSequenceDescriptor();
        assertEquals(oldSequenceManager, sd.getSequenceManagerClass());
        broker.close();
    }

    public void testSimpleInsert_1() throws Exception
    {
        // prepare for PB-api test
        ojbChangeReferenceSetting(MainObject.class, "singleReference", true, true, true, false);
        ojbChangeReferenceSetting(MainObject.class, "allReferences", true, true, true, false);
        ojbChangeReferenceSetting(CollectionReference.class, "singleReference", true, true, true, false);
        ojbChangeReferenceSetting(SingleReference.class, "mainObject", true, true, true, false);
        doTtestSimpleInsert();
    }

    public void testSimpleInsert_2() throws Exception
    {
        // prepare for PB-api test
        ojbChangeReferenceSetting(MainObject.class, "singleReference", true, true, true, true);
        ojbChangeReferenceSetting(MainObject.class, "allReferences", true, true, true, true);
        ojbChangeReferenceSetting(CollectionReference.class, "singleReference", true, true, true, true);
        ojbChangeReferenceSetting(SingleReference.class, "mainObject", true, true, true, true);
        doTtestSimpleInsert();
    }

    public void doTtestSimpleInsert() throws Exception
    {
        if (skipTest()) return;

        long timestamp = System.currentTimeMillis();
        String name = "testSimpleInsert_" + timestamp;

        MainObject obj_1 = new MainObject(null, name);
        MainObject obj_2 = new MainObject(null, name);
        MainObject obj_3 = new MainObject(null, name);

        broker.beginTransaction();
        broker.store(obj_1);
//        System.out.println("obj_1: "+obj_1);
        broker.store(obj_2);
//        System.out.println("obj_2: "+obj_2);
        broker.store(obj_3);
//        System.out.println("obj_3: "+obj_3);
        broker.commitTransaction();

        Criteria crit = new Criteria();
        crit.addEqualTo("name", name);
        QueryByCriteria query = QueryFactory.newQuery(MainObject.class, crit);
        int result = broker.getCount(query);
        assertEquals("Not all objects created", 3, result);
        assertNotNull(obj_1.getIdentifier());
        assertTrue(obj_1.getIdentifier().longValue() > 0);
        assertTrue(obj_3.getIdentifier().longValue() > 0);
    }

    public void testSimpleInsertODMG_1() throws Exception
    {
        int none = ObjectReferenceDescriptor.CASCADE_NONE;
        ojbChangeReferenceSetting(MainObject.class, "singleReference", true, none, none, false);
        ojbChangeReferenceSetting(MainObject.class, "allReferences", true, none, none, false);
        ojbChangeReferenceSetting(CollectionReference.class, "singleReference", true, none, none, false);
        ojbChangeReferenceSetting(SingleReference.class, "mainObject", true, none, none, false);
    }

    public void testSimpleInsertODMG_2() throws Exception
    {
        int none = ObjectReferenceDescriptor.CASCADE_NONE;
        ojbChangeReferenceSetting(MainObject.class, "singleReference", true, none, none, true);
        ojbChangeReferenceSetting(MainObject.class, "allReferences", true, none, none, true);
        ojbChangeReferenceSetting(CollectionReference.class, "singleReference", true, none, none, true);
        ojbChangeReferenceSetting(SingleReference.class, "mainObject", true, none, none, true);
    }
    public void doTestSimpleInsertODMG() throws Exception
    {
        if (skipTest()) return;

        long timestamp = System.currentTimeMillis();
        String name = "testSimpleInsert_" + timestamp;

        MainObject obj_1 = new MainObject(null, name);
        MainObject obj_2 = new MainObject(null, name);
        MainObject obj_3 = new MainObject(null, name);

        Implementation odmg = OJB.getInstance();
        Database db = odmg.newDatabase();
        db.open(TestHelper.DEF_DATABASE_NAME, Database.OPEN_READ_WRITE);

        Transaction tx = odmg.newTransaction();
        tx.begin();
        tx.lock(obj_1, Transaction.WRITE);
        tx.lock(obj_2, Transaction.WRITE);
        tx.lock(obj_3, Transaction.WRITE);
        tx.commit();

        Criteria crit = new Criteria();
        crit.addEqualTo("name", name);
        QueryByCriteria query = QueryFactory.newQuery(MainObject.class, crit);
        int result = broker.getCount(query);
        assertEquals("Not all objects created", 3, result);
        assertNotNull(obj_1.getIdentifier());
        assertTrue(obj_1.getIdentifier().longValue() > 0);
        assertTrue(obj_3.getIdentifier().longValue() > 0);
    }

    public void testReferenceInsertUpdate_1() throws Exception
    {
        // prepare for PB-api test
        ojbChangeReferenceSetting(MainObject.class, "singleReference", true, true, true, false);
        ojbChangeReferenceSetting(MainObject.class, "allReferences", true, true, true, false);
        ojbChangeReferenceSetting(CollectionReference.class, "singleReference", true, true, true, false);
        ojbChangeReferenceSetting(SingleReference.class, "mainObject", true, true, true, false);
        doTestReferenceInsertUpdate();
    }

    public void testReferenceInsertUpdate_2() throws Exception
    {
        // prepare for PB-api test
        ojbChangeReferenceSetting(MainObject.class, "singleReference", true, true, true, true);
        ojbChangeReferenceSetting(MainObject.class, "allReferences", true, true, true, true);
        ojbChangeReferenceSetting(CollectionReference.class, "singleReference", true, true, true, true);
        ojbChangeReferenceSetting(SingleReference.class, "mainObject", true, true, true, true);
        doTestReferenceInsertUpdate();
    }

    public void doTestReferenceInsertUpdate() throws Exception
    {
        if (skipTest()) return;
        long timestamp = System.currentTimeMillis();
        String name = "testReferenceInsert_main_" + timestamp;
        String nameRef = "testReferenceInsert_reference_" + timestamp;
        String nameSingleRef = "testReferenceInsert_single_reference_" + timestamp;

        MainObject obj_1 = new MainObject(null, name);
        MainObject obj_2 = new MainObject(null, name);

        SingleReference s_ref_1 = new SingleReference(nameSingleRef);
        SingleReference s_ref_2 = new SingleReference(nameSingleRef);

        CollectionReference ref_1 = new CollectionReference(null, nameRef);
        CollectionReference ref_2 = new CollectionReference(null, nameRef);
        CollectionReference ref_3 = new CollectionReference(null, nameRef);
        CollectionReference ref_4 = new CollectionReference(null, nameRef);
        ref_1.setSingleReference(s_ref_1);
        ref_4.setSingleReference(s_ref_2);

        SingleReference s_ref_3 = new SingleReference(nameSingleRef);
        SingleReference s_ref_4 = new SingleReference(nameSingleRef);

        obj_1.addReference(ref_1);
        obj_1.addReference(ref_2);
        obj_1.addReference(ref_3);
        obj_1.addReference(ref_4);

        obj_1.setSingleReference(s_ref_3);
        s_ref_3.setMainObject(obj_1);
        obj_2.setSingleReference(s_ref_4);
        s_ref_3.setMainObject(obj_1);

        broker.beginTransaction();
        // first store a reference
        broker.store(ref_1);
//        System.out.println("ref_1: "+ref_1);
        // then store main object with other references
        broker.store(obj_1);
//        System.out.println("obj_1: "+obj_1);
        // store second object without references
        broker.store(obj_2);
//        System.out.println("obj_2: "+obj_2);
        broker.commitTransaction();

        // try to find both objects
        Criteria crit = new Criteria();
        crit.addEqualTo("name", name);
        QueryByCriteria query = QueryFactory.newQuery(MainObject.class, crit);
        int result = broker.getCount(query);
        assertEquals("Wrong object count", 2, result);

        // pk have to set and have to be different
        assertNotNull(obj_1.getIdentifier());
        assertNotNull(obj_2.getIdentifier());
        assertNotSame(obj_1.getIdentifier(), obj_2.getIdentifier());
        assertTrue(obj_1.getIdentifier().longValue() > 0);
        assertTrue(obj_2.getIdentifier().longValue() > 0);
        assertTrue(s_ref_3.getId().longValue() > 0);
        assertTrue(ref_3.getRefIdentifier().longValue() > 0);

        // get Identity objects
        Identity oid_1 = new Identity(obj_1, broker);
        Identity oid_2 = new Identity(obj_2, broker);
        // get identifier (PK) values
        Long id_1 = obj_1.getIdentifier();
        Long id_2 = obj_2.getIdentifier();

        broker.clearCache();

        // get object with references
        obj_1 = (MainObject) broker.getObjectByIdentity(oid_1);
        assertNotNull(obj_1);
        List references = obj_1.getAllReferences();
        assertNotNull(references);
        assertEquals("4 references expected for object: "+obj_1, 4, references.size());
        Iterator it = references.iterator();
        while (it.hasNext())
        {
            CollectionReference ref = (CollectionReference) it.next();
            assertEquals("Main object fk expected", obj_1.getIdentifier(), ref.fkIdentifier);
            assertTrue("We expect a positive value, identity columns have to start > 0",
                    (ref.getRefIdentifier().longValue() > 0));
        }
        assertNotNull(obj_1.getSingleReference());
        obj_2 = (MainObject) broker.getObjectByIdentity(oid_2);
        assertTrue(obj_1.getIdentifier().longValue() > 0);
        assertTrue(obj_2.getIdentifier().longValue() > 0);
        assertNotNull(obj_2.getSingleReference());
        assertTrue(obj_2.getSingleReference().getId().longValue() > 0);
        assertTrue(obj_1.getSingleReference().getId().longValue() > 0);
        assertNotSame(obj_1.getSingleReference(), obj_2.getSingleReference());
        broker.clearCache();

        // get references only
        Criteria crit_2 = new Criteria();
        crit_2.addEqualTo("refName", nameRef);
        QueryByCriteria query_2 = QueryFactory.newQuery(CollectionReference.class, crit_2);
        int result_2 = broker.getCount(query_2);
        assertEquals("Not all objects created", 4, result_2);
        assertNotNull(ref_3.getRefIdentifier());

        broker.clearCache();

        // get second object
        MainObject retObj = (MainObject) broker.getObjectByIdentity(oid_2);
        List refList = retObj.getAllReferences();
        assertNotNull(refList);
        assertEquals("object do not have references", 0, refList.size());

        // add new reference to object
        CollectionReference ref_5 = new CollectionReference(null, nameRef);
        CollectionReference ref_6 = new CollectionReference(null, nameRef);
        obj_1.addReference(ref_5);
        obj_2.addReference(ref_6);
        broker.beginTransaction();
        broker.store(obj_1);
        broker.store(obj_2);
        broker.commitTransaction();
        assertNotNull(ref_5.getRefIdentifier());
        assertNotNull(ref_6.getRefIdentifier());
        assertEquals(id_1, obj_1.getIdentifier());
        assertEquals(id_2, obj_2.getIdentifier());

        obj_1 = (MainObject) broker.getObjectByIdentity(oid_1);
        assertNotNull(obj_1);
        references = obj_1.getAllReferences();
        assertNotNull(references);
        assertEquals("5 references expected for object: "+obj_1, 5, references.size());

        obj_2 = (MainObject) broker.getObjectByIdentity(oid_2);
        assertNotNull(obj_2);
        references = obj_2.getAllReferences();
        assertNotNull(references);
        assertEquals("1 references expected for object: "+obj_2, 1, references.size());

        assertEquals(id_1, obj_1.getIdentifier());
        assertEquals(id_2, obj_2.getIdentifier());

        // now update main objects
        obj_1.setName(name+"_update");
        obj_2.setName(name+"_update");
        broker.beginTransaction();
        broker.store(obj_1);
        broker.store(obj_2);
        broker.commitTransaction();

        obj_1 = (MainObject) broker.getObjectByIdentity(oid_1);
        obj_2 = (MainObject) broker.getObjectByIdentity(oid_2);

        assertNotNull(obj_1);
        assertNotNull(obj_2);
        assertEquals(obj_1.getName(), name+"_update");
        assertEquals(obj_2.getName(), name+"_update");
        assertEquals(id_1, obj_1.getIdentifier());
        assertEquals(id_2, obj_2.getIdentifier());

        // now update reference
        obj_2 = (MainObject) broker.getObjectByIdentity(oid_2);
        assertNotNull(obj_2);
        references = obj_2.getAllReferences();
        CollectionReference ref = (CollectionReference) references.get(0);
        ref.setRefName(nameRef+"_update");
        broker.beginTransaction();
        broker.store(obj_2);
        broker.commitTransaction();
        obj_2 = (MainObject) broker.getObjectByIdentity(oid_2);
        assertNotNull(obj_2);
        references = obj_2.getAllReferences();
        ref = (CollectionReference) references.get(0);
        assertEquals(nameRef+"_update", ref.getRefName());
        assertEquals(id_1, obj_1.getIdentifier());
        assertEquals(id_2, obj_2.getIdentifier());
    }

    /**
     * critical test case, because single broker instance (PB-api) is concurrent used
     * with the ODMG-api, take care of caches
     */
    public void testReferenceInsertUpdateODMG_1() throws Exception
    {
        if (skipTest()) return;

        // prepare metadata for odmg-api
        int none = ObjectReferenceDescriptor.CASCADE_NONE;
        ojbChangeReferenceSetting(MainObject.class, "singleReference", true, none, none, false);
        ojbChangeReferenceSetting(MainObject.class, "allReferences", true, none, none, false);
        ojbChangeReferenceSetting(CollectionReference.class, "singleReference", true, none, none, false);
        ojbChangeReferenceSetting(SingleReference.class, "mainObject", true, none, none, false);

        long timestamp = System.currentTimeMillis();
        String name = "testReferenceInsert_main_" + timestamp;
        String nameRef = "testReferenceInsert_reference_" + timestamp;
        String nameSingleRef = "testReferenceInsert_single_reference_" + timestamp;

        MainObject obj_2 = new MainObject(null, name);
        SingleReference s_ref_4 = new SingleReference(nameSingleRef);
        obj_2.setSingleReference(s_ref_4);

        Implementation odmg = OJB.getInstance();
        Database db = odmg.newDatabase();
        db.open(TestHelper.DEF_DATABASE_NAME, Database.OPEN_READ_WRITE);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        db.makePersistent(s_ref_4);
        db.makePersistent(obj_2);
        tx.commit();

        tx.begin();

        // try to find object
        Criteria crit = new Criteria();
        crit.addEqualTo("name", name);
        QueryByCriteria query = QueryFactory.newQuery(MainObject.class, crit);

        int result = tx.getBroker().getCount(query);
        assertEquals("Wrong object count", 1, result);
        // pk have to set and have to be different
        assertNotNull(obj_2.getIdentifier());
        assertTrue(obj_2.getIdentifier().longValue() > 0);
        // no collection reference set
        List references = obj_2.getAllReferences();
        assertTrue(references == null || references.size() == 0);
        // get Identity objects
        Identity oid_2 = tx.getBroker().serviceIdentity().buildIdentity(obj_2);
        // get identifier (PK) values
        Long id_2 = obj_2.getIdentifier();

        tx.getBroker().clearCache();
        obj_2 = (MainObject) tx.getBroker().getObjectByIdentity(oid_2);

        assertTrue(obj_2.getIdentifier().longValue() > 0);
        assertNotNull(obj_2.getSingleReference());
        assertTrue(obj_2.getSingleReference().getId().longValue() > 0);
        // no collection reference set
        references = obj_2.getAllReferences();
        assertTrue(references == null || references.size() == 0);

        tx.getBroker().clearCache();
        // get references only
        Criteria crit_2 = new Criteria();
        crit_2.addEqualTo("refName", nameRef);
        QueryByCriteria query_2 = QueryFactory.newQuery(CollectionReference.class, crit_2);
        int result_2 = tx.getBroker().getCount(query_2);

        assertEquals(0, result_2);

        tx.getBroker().clearCache();
        // get object
        MainObject retObj = (MainObject) tx.getBroker().getObjectByIdentity(oid_2);

        List refList = retObj.getAllReferences();
        assertNotNull(refList);
        assertEquals("object do not have references", 0, refList.size());
        tx.commit();

        // add new reference to object
        CollectionReference ref_6 = new CollectionReference(null, "###_new_" + nameRef);
        tx.begin();
        tx.lock(obj_2, Transaction.WRITE);
        obj_2.addReference(ref_6);
        tx.commit();

        references = obj_2.getAllReferences();
        assertNotNull(references);
        assertEquals("1 references expected for object: "+obj_2, 1, references.size());


        assertNotNull(ref_6.getRefIdentifier());
        // check FK setting
        Long fk = ref_6.getFkIdentifier();
        assertNotNull(fk);
        assertEquals(obj_2.getIdentifier(), fk);
        assertEquals(id_2, obj_2.getIdentifier());
        references = obj_2.getAllReferences();
        assertNotNull(references);
        assertEquals("1 references expected for object: "+obj_2, 1, references.size());
        assertNotNull(references);

        tx.begin();
        obj_2 = (MainObject) tx.getBroker().getObjectByIdentity(oid_2);
        // we don't change the main object, only add an reference, so the
        // cached version of the object isn't up to date
        tx.getBroker().retrieveAllReferences(obj_2);
        tx.commit();

        assertNotNull(obj_2);
        references = obj_2.getAllReferences();
        assertNotNull(references);
        assertEquals("Reference expected for object", 1, references.size());

        assertEquals(id_2, obj_2.getIdentifier());

        // now update main objects
        tx.begin();
        tx.lock(obj_2, Transaction.WRITE);
        obj_2.setName(name+"_update");
        tx.commit();

        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        obj_2 = (MainObject) broker.getObjectByIdentity(oid_2);
        broker.close();

        assertNotNull(obj_2);
        assertEquals(obj_2.getName(), name+"_update");
        assertEquals(id_2, obj_2.getIdentifier());

        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        obj_2 = (MainObject) broker.getObjectByIdentity(oid_2);
        broker.close();

        // now update reference
        assertNotNull(obj_2);
        tx.begin();
        tx.lock(obj_2, Transaction.WRITE);
        references = obj_2.getAllReferences();
        CollectionReference ref = (CollectionReference) references.get(0);
        tx.lock(ref, Transaction.WRITE);
        ref.setRefName(nameRef+"_update");
        tx.commit();

        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        obj_2 = (MainObject) broker.getObjectByIdentity(oid_2);
        assertNotNull(obj_2);
        references = obj_2.getAllReferences();
        ref = (CollectionReference) references.get(0);
        assertEquals(nameRef+"_update", ref.getRefName());
        assertEquals(id_2, obj_2.getIdentifier());
    }

    /**
     * critical test case, because single broker instance (PB-api) is concurrent used
     * with the ODMG-api, take care of caches
     */
    public void testReferenceInsertUpdateODMG_2() throws Exception
    {
        if (skipTest()) return;

        // prepare metadata for odmg-api
        int none = ObjectReferenceDescriptor.CASCADE_NONE;
        ojbChangeReferenceSetting(MainObject.class, "singleReference", true, none, none, false);
        ojbChangeReferenceSetting(MainObject.class, "allReferences", true, none, none, false);
        ojbChangeReferenceSetting(CollectionReference.class, "singleReference", true, none, none, false);
        ojbChangeReferenceSetting(SingleReference.class, "mainObject", true, none, none, false);

        long timestamp = System.currentTimeMillis();
        String name = "testReferenceInsert_main_" + timestamp;
        String nameRef = "testReferenceInsert_reference_" + timestamp;
        String nameSingleRef = "testReferenceInsert_single_reference_" + timestamp;

        MainObject obj_1 = new MainObject(null, name);
        MainObject obj_2 = new MainObject(null, name);

        SingleReference s_ref_1 = new SingleReference(nameSingleRef);
        SingleReference s_ref_2 = new SingleReference(nameSingleRef);

        CollectionReference ref_1 = new CollectionReference(null, nameRef);
        CollectionReference ref_2 = new CollectionReference(null, nameRef);
        CollectionReference ref_3 = new CollectionReference(null, nameRef);
        CollectionReference ref_4 = new CollectionReference(null, nameRef);
        ref_1.setSingleReference(s_ref_1);
        ref_4.setSingleReference(s_ref_2);

        SingleReference s_ref_3 = new SingleReference(nameSingleRef);
        SingleReference s_ref_4 = new SingleReference(nameSingleRef);

        obj_1.addReference(ref_1);
        obj_1.addReference(ref_2);
        obj_1.addReference(ref_3);
        obj_1.addReference(ref_4);

        obj_1.setSingleReference(s_ref_3);
        obj_2.setSingleReference(s_ref_4);

        Implementation odmg = OJB.getInstance();
        Database db = odmg.newDatabase();
        db.open(TestHelper.DEF_DATABASE_NAME, Database.OPEN_READ_WRITE);

        Transaction tx = odmg.newTransaction();
        tx.begin();
        db.makePersistent(s_ref_1);
        db.makePersistent(s_ref_2);
        db.makePersistent(s_ref_3);
        db.makePersistent(s_ref_4);

        db.makePersistent(obj_1);
        db.makePersistent(obj_2);
        tx.commit();

        // try to find both objects
        Criteria crit = new Criteria();
        crit.addEqualTo("name", name);
        QueryByCriteria query = QueryFactory.newQuery(MainObject.class, crit);
        int result = broker.getCount(query);
        assertEquals("Wrong object count", 2, result);

        // pk have to set and have to be different
        assertNotNull(obj_1.getIdentifier());
        assertNotNull(obj_2.getIdentifier());
        assertNotSame(obj_1.getIdentifier(), obj_2.getIdentifier());
        assertTrue(obj_1.getIdentifier().longValue() > 0);
        assertTrue(obj_2.getIdentifier().longValue() > 0);
        assertTrue(s_ref_3.getId().longValue() > 0);
        assertTrue(ref_3.getRefIdentifier().longValue() > 0);

        // no collection reference set
        List references = obj_2.getAllReferences();
        assertTrue(references == null || references.size() == 0);
        // check anonymous FK setting
        Long fk = (Long) broker.getClassDescriptor(MainObject.class)
                .getFieldDescriptorByName("refFK")
                .getPersistentField().get(obj_1);
        assertTrue("The assigned FK should be > 0 after store of main object, but was " + fk.longValue(), fk.longValue() > 0);

        // get Identity objects
        Identity oid_1 = new Identity(obj_1, broker);
        Identity oid_2 = new Identity(obj_2, broker);
        // get identifier (PK) values
        Long id_1 = obj_1.getIdentifier();
        Long id_2 = obj_2.getIdentifier();

        broker.clearCache();

        // get object with references
        obj_1 = (MainObject) broker.getObjectByIdentity(oid_1);
        assertNotNull(obj_1);
        references = obj_1.getAllReferences();
        assertNotNull(references);
        assertEquals("4 references expected for object: "+obj_1, 4, references.size());
        Iterator it = references.iterator();
        while (it.hasNext())
        {
            CollectionReference ref = (CollectionReference) it.next();
            assertEquals("Main object fk expected", obj_1.getIdentifier(), ref.fkIdentifier);
            assertTrue("We expect a positive value, identity columns have to start > 0",
                    (ref.getRefIdentifier().longValue() > 0));
        }
        assertNotNull(obj_1.getSingleReference());
        obj_2 = (MainObject) broker.getObjectByIdentity(oid_2);
        assertTrue(obj_1.getIdentifier().longValue() > 0);
        assertTrue(obj_2.getIdentifier().longValue() > 0);
        assertNotNull(obj_2.getSingleReference());
        assertTrue(obj_2.getSingleReference().getId().longValue() > 0);
        assertTrue(obj_1.getSingleReference().getId().longValue() > 0);
        assertNotSame(obj_1.getSingleReference(), obj_2.getSingleReference());
        // no collection reference set
        references = obj_2.getAllReferences();
        assertTrue(references == null || references.size() == 0);
        broker.clearCache();

        // get references only
        Criteria crit_2 = new Criteria();
        crit_2.addEqualTo("refName", nameRef);
        QueryByCriteria query_2 = QueryFactory.newQuery(CollectionReference.class, crit_2);
        int result_2 = broker.getCount(query_2);
        assertEquals("Not all objects created", 4, result_2);
        assertNotNull(ref_3.getRefIdentifier());

        broker.clearCache();

        // get second object
        MainObject retObj = (MainObject) broker.getObjectByIdentity(oid_2);
        List refList = retObj.getAllReferences();
        assertNotNull(refList);
        assertEquals("object do not have references", 0, refList.size());

        // add new reference to object
        CollectionReference ref_5 = new CollectionReference(null, "##new ref 1_" + nameRef);
        CollectionReference ref_6 = new CollectionReference(null, "##new ref 2_" + nameRef);
        tx.begin();
        tx.lock(obj_1, Transaction.WRITE);
        tx.lock(obj_2, Transaction.WRITE);
        obj_1.addReference(ref_5);
        obj_2.addReference(ref_6);
        references = obj_2.getAllReferences();
        assertNotNull(references);
        assertEquals("1 references expected for object: "+obj_2, 1, references.size());
        tx.commit();

        assertNotNull(ref_5.getRefIdentifier());
        assertNotNull(ref_6.getRefIdentifier());
        // check FK setting
        fk = ref_5.getFkIdentifier();
        assertNotNull(fk);
        assertEquals(obj_1.getIdentifier(), fk);
        fk = ref_6.getFkIdentifier();
        assertNotNull(fk);
        assertEquals(obj_2.getIdentifier(), fk);
        assertEquals(id_1, obj_1.getIdentifier());
        assertEquals(id_2, obj_2.getIdentifier());
        references = obj_2.getAllReferences();
        assertNotNull(references);
        assertEquals("1 references expected for object: "+obj_2, 1, references.size());

        // refresh used broker instance to avoid problems with session cache (when used)
        broker.close();
        broker = PersistenceBrokerFactory.defaultPersistenceBroker();

        obj_1 = (MainObject) broker.getObjectByIdentity(oid_1);
        assertNotNull(obj_1);
        references = obj_1.getAllReferences();
        assertNotNull(references);
        assertEquals("5 references expected for object: "+obj_1, 5, references.size());

        // we don't change the main object, only add an reference, so the
        // cached version of the object isn't up to date. So we have to retrieve
        // all referenced objects to make it work with all cache implementations
        // or evict the whole cache instead
        // broker.clearCache();
        obj_2 = (MainObject) broker.getObjectByIdentity(oid_2);
        broker.retrieveAllReferences(obj_2);
        assertNotNull(obj_2);
        references = obj_2.getAllReferences();
        assertNotNull(references);
        assertEquals("1 references expected for object: "+obj_2, 1, references.size());

        assertEquals(id_1, obj_1.getIdentifier());
        assertEquals(id_2, obj_2.getIdentifier());

        // now update main objects
        tx.begin();
        tx.lock(obj_1, Transaction.WRITE);
        tx.lock(obj_2, Transaction.WRITE);
        obj_1.setName(name+"_update");
        obj_2.setName(name+"_update");
        tx.commit();

        obj_1 = (MainObject) broker.getObjectByIdentity(oid_1);
        obj_2 = (MainObject) broker.getObjectByIdentity(oid_2);

        assertNotNull(obj_1);
        assertNotNull(obj_2);
        assertEquals(obj_1.getName(), name+"_update");
        assertEquals(obj_2.getName(), name+"_update");
        assertEquals(id_1, obj_1.getIdentifier());
        assertEquals(id_2, obj_2.getIdentifier());

        // now update reference
        obj_2 = (MainObject) broker.getObjectByIdentity(oid_2);
        assertNotNull(obj_2);
        tx.begin();
        tx.lock(obj_2, Transaction.WRITE);
        references = obj_2.getAllReferences();
        CollectionReference ref = (CollectionReference) references.get(0);
        tx.lock(ref, Transaction.WRITE);
        ref.setRefName(nameRef+"_update");
        tx.commit();

        obj_2 = (MainObject) broker.getObjectByIdentity(oid_2);
        assertNotNull(obj_2);
        references = obj_2.getAllReferences();
        ref = (CollectionReference) references.get(0);
        assertEquals(nameRef+"_update", ref.getRefName());
        assertEquals(id_1, obj_1.getIdentifier());
        assertEquals(id_2, obj_2.getIdentifier());
    }

    public void testDelete_1() throws Exception
    {
        // prepare for PB-api test
        ojbChangeReferenceSetting(MainObject.class, "singleReference", true, true, true, false);
        ojbChangeReferenceSetting(MainObject.class, "allReferences", true, true, true, false);
        ojbChangeReferenceSetting(CollectionReference.class, "singleReference", true, true, true, false);
        ojbChangeReferenceSetting(SingleReference.class, "mainObject", true, true, true, false);
        doTestDelete();
    }

    public void testDelete_2() throws Exception
    {
        // prepare for PB-api test
        ojbChangeReferenceSetting(MainObject.class, "singleReference", true, true, true, true);
        ojbChangeReferenceSetting(MainObject.class, "allReferences", true, true, true, true);
        ojbChangeReferenceSetting(CollectionReference.class, "singleReference", true, true, true, true);
        ojbChangeReferenceSetting(SingleReference.class, "mainObject", true, true, true, true);
        doTestDelete();
    }

    public void doTestDelete() throws Exception
    {
        if (skipTest()) return;

        long timestamp = System.currentTimeMillis();
        String name = "testDelete_main_" + timestamp;
        String nameRef = "testDelete_reference_" + timestamp;

        MainObject obj_1 = new MainObject(null, name);

        CollectionReference ref_1 = new CollectionReference(null, nameRef);
        CollectionReference ref_2 = new CollectionReference(null, nameRef);

        obj_1.addReference(ref_1);
        obj_1.addReference(ref_2);
        broker.beginTransaction();
        broker.store(obj_1);
        broker.commitTransaction();
        Identity oid_1 = new Identity(obj_1, broker);

        MainObject result = (MainObject) broker.getObjectByIdentity(oid_1);
        assertNotNull(result);
        assertNotNull(result.getAllReferences());
        assertEquals(2, result.getAllReferences().size());
        Long fk = ((CollectionReference) result.getAllReferences().get(0)).getFkIdentifier();
        assertNotNull(result.getIdentifier());
        assertEquals(result.getIdentifier(), fk);

        broker.beginTransaction();
        broker.delete(obj_1);
        broker.commitTransaction();

        result = (MainObject) broker.getObjectByIdentity(oid_1);
        assertNull(result);
        Criteria crit_2 = new Criteria();
        crit_2.addEqualTo("refName", nameRef);
        QueryByCriteria query_2 = QueryFactory.newQuery(CollectionReference.class, crit_2);
        int result_2 = broker.getCount(query_2);
        assertEquals(0, result_2);
    }

    public void testDeleteTwo_1() throws Exception
    {
        // prepare for PB-api test
        ojbChangeReferenceSetting(MainObject.class, "singleReference", true, true, true, false);
        ojbChangeReferenceSetting(MainObject.class, "allReferences", true, true, true, false);
        ojbChangeReferenceSetting(CollectionReference.class, "singleReference", true, true, true, false);
        ojbChangeReferenceSetting(SingleReference.class, "mainObject", true, true, true, false);
        doTestDeleteTwo();
    }

    public void testDeleteTwo_2() throws Exception
    {
        // prepare for PB-api test
        ojbChangeReferenceSetting(MainObject.class, "singleReference", true, true, true, true);
        ojbChangeReferenceSetting(MainObject.class, "allReferences", true, true, true, true);
        ojbChangeReferenceSetting(CollectionReference.class, "singleReference", true, true, true, true);
        ojbChangeReferenceSetting(SingleReference.class, "mainObject", true, true, true, true);
        doTestDeleteTwo();
    }

    public void doTestDeleteTwo() throws Exception
    {
        if (skipTest()) return;
        long timestamp = System.currentTimeMillis();
        String name = "testDeleteTwo_main_" + timestamp;
        String nameRef = "testDeleteTwo_reference_" + timestamp;

        MainObject obj_1 = new MainObject(null, name);

        CollectionReference ref_1 = new CollectionReference(null, nameRef);
        CollectionReference ref_2 = new CollectionReference(null, nameRef);

        obj_1.addReference(ref_1);
        obj_1.addReference(ref_2);

        // chaotic operations
        broker.beginTransaction();
        // System.out.println("1. "+obj_1);
        broker.store(obj_1);
        // System.out.println("2. "+obj_1);
        broker.delete(obj_1);
        // System.out.println("3. "+obj_1);
        broker.store(obj_1);
        // System.out.println("4. "+obj_1);
        broker.delete(obj_1);
        // System.out.println("5. "+obj_1);
        broker.store(obj_1);
        // System.out.println("6. "+obj_1);
        broker.delete(obj_1);
        // System.out.println("7. "+obj_1);
        broker.store(obj_1);
        // System.out.println("8. "+obj_1);
        broker.commitTransaction();
        Identity oid_1 = new Identity(obj_1, broker);

        MainObject result = (MainObject) broker.getObjectByIdentity(oid_1);
        assertNotNull(result);
        assertNotNull(result.getAllReferences());
        assertEquals(2, result.getAllReferences().size());
        Long fk = ((CollectionReference) result.getAllReferences().get(0)).getFkIdentifier();
        assertNotNull(result.getIdentifier());
        assertEquals(result.getIdentifier(), fk);

        // we should find exactly one object
        Criteria c = new Criteria();
        c.addEqualTo("name", name);
        QueryByCriteria q = QueryFactory.newQuery(MainObject.class, c);
        Collection col = broker.getCollectionByQuery(q);
        assertNotNull(col);
        assertEquals(1, col.size());

        broker.beginTransaction();
        broker.delete(obj_1);
        broker.commitTransaction();

        result = (MainObject) broker.getObjectByIdentity(oid_1);
        assertNull(result);
        Criteria crit_2 = new Criteria();
        crit_2.addEqualTo("refName", nameRef);
        QueryByCriteria query_2 = QueryFactory.newQuery(CollectionReference.class, crit_2);
        int result_2 = broker.getCount(query_2);
        assertEquals(0, result_2);
    }

//    public void testAllInOne() throws Exception
//    {
//        // sleep thread to make timestamp based tests work
//        testSimpleInsert();
//        ojbSleep();
//        ojbSleep();
//        testSimpleInsert();
//        ojbSleep();
//        ojbSleep();
//        testReferenceInsertUpdate();
//        ojbSleep();
//        ojbSleep();
//        // testReferenceInsertUpdate_2();
//        ojbSleep();
//        ojbSleep();
//        testReferenceInsertUpdate();
//    }

//    void ojbChangeReferenceSetting(Class clazz, String referenceField, boolean autoRetrieve, int autoUpdate, int autoDelete, boolean useProxy)
//    {
//        ClassDescriptor cld = broker.getClassDescriptor(clazz);
//        ObjectReferenceDescriptor ref = cld.getCollectionDescriptorByName(referenceField);
//        if(ref == null) ref = cld.getObjectReferenceDescriptorByName(referenceField);
//        ref.setLazy(useProxy);
//        ref.setCascadeRetrieve(autoRetrieve);
//        ref.setCascadingStore(autoUpdate);
//        ref.setCascadingDelete(autoDelete);
//    }
//
//    void ojbChangeReferenceSetting(Class clazz, String referenceField, boolean autoRetrieve, boolean autoUpdate, boolean autoDelete, boolean useProxy)
//    {
//        ClassDescriptor cld = broker.getClassDescriptor(clazz);
//        ObjectReferenceDescriptor ref = cld.getCollectionDescriptorByName(referenceField);
//        if(ref == null) ref = cld.getObjectReferenceDescriptorByName(referenceField);
//        ref.setLazy(useProxy);
//        ref.setCascadeRetrieve(autoRetrieve);
//        ref.setCascadeStore(autoUpdate);
//        ref.setCascadeDelete(autoDelete);
//    }


    //========================================================================
    // inner classes, used for test
    //========================================================================

    public static interface MainObjectIF extends Serializable
    {
        public SingleReferenceIF getSingleReference();
        public void setSingleReference(SingleReferenceIF singleReference);
        public List getAllReferences();
        public void addReference(CollectionReference reference);
        public void setAllReferences(List allReferences);
        public Long getIdentifier();
        public void setIdentifier(Long identifier);
        public String getName();
        public void setName(String name);
    }

    public static class MainObject implements MainObjectIF
    {
        private Long identifier;
        private String name;
        private List allReferences;
        // we use anonymous field for FK
        private SingleReferenceIF singleReference;


        public MainObject()
        {
        }

        public MainObject(Long identifier, String name)
        {
            this.identifier = identifier;
            this.name = name;
        }

        public SingleReferenceIF getSingleReference()
        {
            return singleReference;
        }

        public void setSingleReference(SingleReferenceIF singleReference)
        {
            this.singleReference = singleReference;
        }

        public List getAllReferences()
        {
            return allReferences;
        }

        public void addReference(CollectionReference reference)
        {
            if (allReferences == null)
            {
                allReferences = new ArrayList();
            }
            allReferences.add(reference);
        }

        public void setAllReferences(List allReferences)
        {
            this.allReferences = allReferences;
        }

        public Long getIdentifier()
        {
            return identifier;
        }

        public void setIdentifier(Long identifier)
        {
            this.identifier = identifier;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return new ToStringBuilder(this).append("identifier", identifier).append("name", name)
                    .append("allReferences", allReferences != null ? allReferences.toString() : "null")
                    .append("singleReference", singleReference.getClass().toString()).toString();
        }
    }

    public static interface SingleReferenceIF extends Serializable
    {
        public MainObjectIF getMainObject();
        public void setMainObject(MainObjectIF mainObject);
        public Long getId();
        public void setId(Long id);
        public String getName();
        public void setName(String name);
    }

    public static class SingleReference implements SingleReferenceIF
    {
        Long id;
        String name;
        String ojbConcreteClass;
        MainObjectIF mainObject;

        public SingleReference()
        {
            this(null);
        }

        public SingleReference(String name)
        {
            this.name = name;
            ojbConcreteClass = SingleReference.class.getName();
            // id = new Long((long)(Math.random() * Integer.MAX_VALUE));
        }

        public MainObjectIF getMainObject()
        {
            return mainObject;
        }

        public void setMainObject(MainObjectIF mainObject)
        {
            this.mainObject = mainObject;
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

        public String toString()
        {
            return new ToStringBuilder(this).append("id", id).append("name", name)
                    .append("mainObject", mainObject != null ? mainObject.getClass().toString() : "null").toString();
        }
    }

    public static interface CollectionReferenceIF extends Serializable
    {
        public Long getRefIdentifier();
        public void setRefIdentifier(Long refIdentifier);
        public SingleReferenceIF getSingleReference();
        public void setSingleReference(SingleReferenceIF singleReference);
        public Long getFkIdentifier();
        public void setFkIdentifier(Long fkIdentifier);
        public String getRefName();
        public void setRefName(String refName);
    }

    public static class CollectionReference implements CollectionReferenceIF
    {
        private Long refIdentifier;
        private String refName;
        private Long fkIdentifier;
        String ojbConcreteClass;
        private SingleReferenceIF singleReference;

        public CollectionReference()
        {
            ojbConcreteClass = CollectionReference.class.getName();
        }

        public CollectionReference(Long refIdentifier, String refName)
        {
            this();
            this.refIdentifier = refIdentifier;
            this.refName = refName;
        }

        public Long getRefIdentifier()
        {
            return refIdentifier;
        }

        public void setRefIdentifier(Long refIdentifier)
        {
            this.refIdentifier = refIdentifier;
        }

        public SingleReferenceIF getSingleReference()
        {
            return singleReference;
        }

        public void setSingleReference(SingleReferenceIF singleReference)
        {
            this.singleReference = singleReference;
        }

        public Long getFkIdentifier()
        {
            return fkIdentifier;
        }

        public void setFkIdentifier(Long fkIdentifier)
        {
            this.fkIdentifier = fkIdentifier;
        }

        public String getRefName()
        {
            return refName;
        }

        public void setRefName(String refName)
        {
            this.refName = refName;
        }

        public String toString()
        {
            return new ToStringBuilder(this).append("id", refIdentifier).append("name", refName)
                    .append("fkIdentifier", fkIdentifier)
                    .append("singleReference", singleReference != null ? singleReference.toString() : "null")
                    .toString();
        }
    }
}
