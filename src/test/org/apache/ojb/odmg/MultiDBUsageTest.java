package org.apache.ojb.odmg;

import org.apache.ojb.broker.FarAwayClass;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.TestHelper;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.ConnectionRepository;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.odmg.shared.Project;
import org.apache.ojb.odmg.shared.ODMGZoo;
import org.apache.ojb.junit.OJBTestCase;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Do some rollback tests and check behavior within transactions.
 * CAUTION: This tests works only against the default repository.
 */
public class MultiDBUsageTest extends OJBTestCase
{
    private Implementation odmg_1;
    private Database db_1;
    private Implementation odmg_2;
    private Database db_2;

    public static void main(String[] args)
    {
        String[] arr = {MultiDBUsageTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public MultiDBUsageTest(String s)
    {
        super(s);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        MetadataManager mm = MetadataManager.getInstance();
        JdbcConnectionDescriptor jcd = mm.connectionRepository().getDescriptor(TestHelper.FAR_AWAY_KEY);
        if(jcd == null)
        {
            ConnectionRepository cr = mm.readConnectionRepository(TestHelper.FAR_AWAY_CONNECTION_REPOSITORY);
            mm.connectionRepository().addDescriptor(cr.getDescriptor(TestHelper.FAR_AWAY_KEY));
        }

        odmg_1 = OJB.getInstance();
        db_1 = odmg_1.newDatabase();
        db_1.open(TestHelper.DEF_DATABASE_NAME, Database.OPEN_READ_WRITE);

        odmg_2 = OJB.getInstance();
        db_2 = odmg_2.newDatabase();
        db_2.open(TestHelper.FAR_AWAY_DATABASE_NAME, Database.OPEN_READ_WRITE);
    }

    protected void tearDown() throws Exception
    {
        MetadataManager mm = MetadataManager.getInstance();
        JdbcConnectionDescriptor jcd = mm.connectionRepository().getDescriptor(TestHelper.FAR_AWAY_KEY);
        mm.connectionRepository().removeDescriptor(jcd);
        try
        {
            if(odmg_1.currentTransaction() != null)
            {
                odmg_1.currentTransaction().abort();
            }
            db_1.close();
            odmg_1 = null;
        }
        catch (Exception e)
        {
            // ignore
        }

        try
        {
            if(odmg_2.currentTransaction() != null)
            {
                odmg_2.currentTransaction().abort();
            }
            db_2.close();
            odmg_2 = null;
        }
        catch (Exception e)
        {
            // ignore
        }
        super.tearDown();
    }

    /**
     * Test store / delete objects to different db
     */
    public void testStore() throws Exception
    {
        String name = "testStoreDelete_" + System.currentTimeMillis();

        // little hack for the test. use PB and ODMG api to verify results
        int odmgZoosBefore = getDBObjectCountWithNewPB(((DatabaseImpl) db_1).getPBKey(), ODMGZoo.class);
        int projectsBefore = getDBObjectCountWithNewPB(((DatabaseImpl) db_1).getPBKey(), Project.class);
        int farAwaysBefore = getDBObjectCountWithNewPB(((DatabaseImpl) db_2).getPBKey(), FarAwayClass.class);

        Transaction tx_1 = odmg_1.newTransaction();
        tx_1.begin();
        //store
        storeObjects(tx_1, getNewODMGZoos(name, 5));
        storeObjects(tx_1, getNewProjects(name, 3));
        //store more
        storeObjects(tx_1, getNewODMGZoos(name, 5));
        storeObjects(tx_1, getNewProjects(name, 2));
        tx_1.commit();

        Transaction tx_2 = odmg_2.newTransaction();
        tx_2.begin();
        //store
        storeObjects(tx_2, getNewFarAways(name, 9));
        //store more
        storeObjects(tx_2, getNewFarAways(name, 11));
        tx_2.commit();

        int odmgZoosAfter = getDBObjectCountWithNewPB(((DatabaseImpl) db_1).getPBKey(), ODMGZoo.class);
        int projectsAfter = getDBObjectCountWithNewPB(((DatabaseImpl) db_1).getPBKey(), Project.class);
        int farAwaysAfter = getDBObjectCountWithNewPB(((DatabaseImpl) db_2).getPBKey(), FarAwayClass.class);
        int odmgZoosAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg_1, ODMGZoo.class);
        int projectsAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg_1, Project.class);
        int farAwaysAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg_2, FarAwayClass.class);

        assertEquals("Wrong number of odmgZoos found", (odmgZoosBefore + 10), odmgZoosAfter);
        assertEquals("Wrong number of projects found", (projectsBefore + 5), projectsAfter);
        assertEquals("Wrong number of odmgZoos found", (odmgZoosBefore + 10), odmgZoosAfterOQL);
        assertEquals("Wrong number of projects found", (projectsBefore + 5), projectsAfterOQL);
        assertEquals("Wrong number of farAways found", (farAwaysBefore + 20), farAwaysAfter);
        assertEquals("Wrong number of farAways found", (farAwaysBefore + 20), farAwaysAfterOQL);

        //************
        // we do twice
        //************
        // little hack for the test
        odmgZoosBefore = getDBObjectCountWithNewPB(((DatabaseImpl) db_1).getPBKey(), ODMGZoo.class);
        projectsBefore = getDBObjectCountWithNewPB(((DatabaseImpl) db_1).getPBKey(), Project.class);
        farAwaysBefore = getDBObjectCountWithNewPB(((DatabaseImpl) db_2).getPBKey(), FarAwayClass.class);

        tx_1.begin();
        //store
        storeObjects(tx_1, getNewODMGZoos(name, 5));
        storeObjects(tx_1, getNewProjects(name, 3));
        //store more
        storeObjects(tx_1, getNewODMGZoos(name, 5));
        storeObjects(tx_1, getNewProjects(name, 2));
        tx_1.commit();

        tx_2.begin();
        //store
        storeObjects(tx_2, getNewFarAways(name, 9));
        //store more
        storeObjects(tx_2, getNewFarAways(name, 11));
        tx_2.commit();

        odmgZoosAfter = getDBObjectCountWithNewPB(((DatabaseImpl) db_1).getPBKey(), ODMGZoo.class);
        projectsAfter = getDBObjectCountWithNewPB(((DatabaseImpl) db_1).getPBKey(), Project.class);
        farAwaysAfter = getDBObjectCountWithNewPB(((DatabaseImpl) db_2).getPBKey(), FarAwayClass.class);
        odmgZoosAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg_1, ODMGZoo.class);
        projectsAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg_1, Project.class);
        farAwaysAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg_2, FarAwayClass.class);

        assertEquals("Wrong number of odmgZoos found", (odmgZoosBefore + 10), odmgZoosAfter);
        assertEquals("Wrong number of projects found", (projectsBefore + 5), projectsAfter);
        assertEquals("Wrong number of odmgZoos found", (odmgZoosBefore + 10), odmgZoosAfterOQL);
        assertEquals("Wrong number of projects found", (projectsBefore + 5), projectsAfterOQL);
        assertEquals("Wrong number of farAways found", (farAwaysBefore + 20), farAwaysAfter);
        assertEquals("Wrong number of farAways found", (farAwaysBefore + 20), farAwaysAfterOQL);

        List result;
        tx_1.begin();
        OQLQuery query = odmg_1.newOQLQuery();
        query.create("select projects from " + Project.class.getName() + " where title like $1");
        query.bind(name);
        result = (List) query.execute();
        deleteObjects(db_1, result);
        tx_1.commit();

        tx_1.begin();
        query = odmg_1.newOQLQuery();
        query.create("select projects from " + ODMGZoo.class.getName() + " where name like $1");
        query.bind(name);
        result = (List) query.execute();
        deleteObjects(db_1, result);
        tx_1.commit();

        tx_2.begin();
        query = odmg_2.newOQLQuery();
        query.create("select projects from " + FarAwayClass.class.getName() + " where name like $1");
        query.bind(name);
        result = (List) query.execute();
        deleteObjects(db_2, result);
        tx_2.commit();


        tx_1.begin();
        query = odmg_1.newOQLQuery();
        query.create("select projects from " + Project.class.getName() + " where title like $1");
        query.bind(name);
        result = (List) query.execute();
        tx_1.commit();
        assertEquals(0, result.size());

        tx_1.begin();
        query = odmg_1.newOQLQuery();
        query.create("select projects from " + ODMGZoo.class.getName() + " where name like $1");
        query.bind(name);
        result = (List) query.execute();
        tx_1.commit();
        assertEquals(0, result.size());

        tx_2.begin();
        query = odmg_2.newOQLQuery();
        query.create("select projects from " + FarAwayClass.class.getName() + " where name like $1");
        query.bind(name);
        result = (List) query.execute();
        tx_2.commit();
        assertEquals(0, result.size());
    }


    private void storeObjects(Transaction tx, Collection objects)
    {
        for (Iterator iterator = objects.iterator(); iterator.hasNext();)
        {
            tx.lock(iterator.next(), Transaction.WRITE);
        }
    }

    private void deleteObjects(Database db, Collection objects)
    {
        for (Iterator iterator = objects.iterator(); iterator.hasNext();)
        {
            db.deletePersistent(iterator.next());
        }
    }

    private static int counter;

    protected Collection getNewProjects(String name, int count)
    {
        ArrayList list = new ArrayList();
        for (int i = 0; i < count; i++)
        {
            list.add(newProject(name));
        }
        return list;
    }

    protected Project newProject(String name)
    {
        Project p = new Project();
        ++counter;
        p.setDescription("Test project " + counter);
        p.setTitle(name);
        return p;
    }

    protected Collection getNewODMGZoos(String name, int count)
    {
        ArrayList list = new ArrayList();
        for (int i = 0; i < count; i++)
        {
            list.add(newODMGZoo(name));
        }
        return list;
    }

    protected Collection getNewFarAways(String name, int count)
    {
        ArrayList list = new ArrayList();
        for (int i = 0; i < count; i++)
        {
            list.add(newFarAway(name));
        }
        return list;
    }

    protected ODMGZoo newODMGZoo(String name)
    {
        ODMGZoo odmgZoo = new ODMGZoo();
        ++counter;
        odmgZoo.setName(name);
        return odmgZoo;
    }

    private FarAwayClass newFarAway(String name)
    {
        FarAwayClass fa = new FarAwayClass();
        counter++;
        fa.setName(name);
        fa.setDescription("so far away from " + counter);
        return fa;
    }

    protected int getDBObjectCountWithNewPB(PBKey pbKey, Class target) throws Exception
    {
        PersistenceBroker broker = PersistenceBrokerFactory.createPersistenceBroker(pbKey);
        Criteria c = new Criteria();
        Query q = new QueryByCriteria(target, c);
        int count = broker.getCount(q);
        broker.close();
        return count;
    }

    protected int getDBObjectCount(PersistenceBroker broker, Class target) throws Exception
    {
        Criteria c = new Criteria();
        Query q = new QueryByCriteria(target, c);
        int count = broker.getCount(q);
        return count;
    }

    protected int getDBObjectCountViaOqlQueryUseNewTransaction(Implementation ojb, Class target) throws Exception
    {
        Transaction tx = ojb.newTransaction();
        tx.begin();
        OQLQuery query = ojb.newOQLQuery();
        query.create("select allProjects from " + target.getName());
        List list = (List) query.execute();
        tx.commit();
        return list.size();
    }

    protected int getDBObjectCountViaOqlQuery(Implementation ojb, Class target) throws Exception
    {
        OQLQuery query = ojb.newOQLQuery();
        query.create("select allObjects from " + target.getName());
        List list = (List) query.execute();
        return list.size();
    }

    protected List getAllObjects(Implementation ojb, Class target) throws Exception
    {
        OQLQuery query = ojb.newOQLQuery();
        query.create("select allObjects from " + target.getName());
        List list = (List) query.execute();
        return list;
    }
}
