package org.apache.ojb.odmg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryByIdentity;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.shared.ODMGGourmet;
import org.apache.ojb.odmg.shared.ODMGZoo;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.LockNotGrantedException;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * Do some rollback tests and check behavior within transactions.
 * CAUTION: This tests works only against the default repository.
 */
public class ODMGRollbackTest extends ODMGTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {ODMGRollbackTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public ODMGRollbackTest(String s)
    {
        super(s);
    }

    public void testDatabaseClose() throws Exception
    {
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        try
        {
            tx.begin();
            database.close();
            fail("We should not able to close database instance while running tx");
        }
        catch (Exception e)
        {
        }
        finally
        {
            tx.abort();
        }
    }

    public void testDeleteAll() throws Exception
    {
        deleteAll(odmg, ODMGZoo.class);
        deleteAll(odmg, ODMGGourmet.class);
    }

    public void testTransactionFlush() throws Exception
    {
        String name = "testTransactionFlush_" + System.currentTimeMillis();
        TransactionExt tx = (TransactionExt) odmg.newTransaction();

        tx.begin();
        PersistenceBroker broker = tx.getBroker();
        ODMGZoo obj = new ODMGZoo();
        tx.lock(obj, Transaction.WRITE);
        obj.setName(name);

        tx.flush();

        Criteria crit = new Criteria();
        crit.addEqualTo("name", obj.getName());
        QueryByCriteria query = QueryFactory.newQuery(ODMGZoo.class, crit);
        // we flushed all objects, thus we should find object in DB/cache
        Iterator it = broker.getIteratorByQuery(query);
        assertTrue(it.hasNext());
        ODMGZoo other = (ODMGZoo) it.next();
        assertNotNull(other);
        assertEquals(obj.getZooId(), other.getZooId());
        assertEquals(obj.getName(), other.getName());
        assertFalse(it.hasNext());
        // now we abort tx, so all flushed objects shouldn't be found
        // in further queries
        tx.abort();

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        broker = tx.getBroker();
        QueryByIdentity query2 = new QueryByIdentity(obj);
        Object result = broker.getObjectByQuery(query2);
        tx.commit();

        assertNull("We should not find objects from aborted tx", result);
    }

    public void testTransactionFlush_2() throws Exception
    {
        String name = "testTransactionFlush_2_" + System.currentTimeMillis();
        TransactionExt tx = (TransactionExt) odmg.newTransaction();

        tx.begin();
        PersistenceBroker broker = tx.getBroker();

        ODMGZoo obj = new ODMGZoo();
        obj.setName(name);
        tx.lock(obj, Transaction.WRITE);
        tx.flush();
        // System.err.println("First flush call, insert new object");

        // PB to query
        Criteria crit = new Criteria();
        crit.addEqualTo("name", obj.getName());
        QueryByCriteria query = QueryFactory.newQuery(ODMGZoo.class, crit);
        // we flushed all objects, thus we should found object in DB/cache
        Iterator it = broker.getIteratorByQuery(query);
        assertTrue(it.hasNext());
        ODMGZoo other = (ODMGZoo) it.next();
        assertNotNull(other);
        assertEquals(obj.getZooId(), other.getZooId());
        assertEquals(obj.getName(), other.getName());
        assertFalse(it.hasNext());

        /*** Charles : Start ***/
        // Let's flush, change the name and flush again
        tx.flush();
        // System.err.println("Second flush call, nothing to do");
        obj.setName("updated_" + name);
        tx.flush();
        // System.err.println("Third flush call, update");
        OQLQuery q = odmg.newOQLQuery();
        q.create("select zoos from " + ODMGZoo.class.getName() + " where name like $1");
        q.bind("updated_" + name);

        //Redo the query - we should find the object again
        it = ((Collection) q.execute()).iterator();
        assertTrue(it.hasNext());
        other = (ODMGZoo) it.next();
        assertNotNull(other);
        assertEquals(obj.getZooId(), other.getZooId());
        assertEquals(obj.getName(), other.getName());
        assertFalse(it.hasNext());
        /*** Charles : End ***/

        // now we abort tx, so all flushed objects shouldn't be found
        // in further queries
        tx.abort();

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        broker = tx.getBroker();
        QueryByIdentity query2 = new QueryByIdentity(obj);
        Object result = broker.getObjectByQuery(query2);
        tx.commit();

        assertNull("We should not find objects from aborted tx", result);
    }

    /**
     * Tests behavior within transactions. If i store 5 odmgZoos within a transaction
     * and after that within the same transaction i do query 'select all odmgZoos'
     * the number of odmgZoos returned should be oldNumber+5 when using checkpoint.
     * thma:
     * this testcase seems to fail for some strange problems with the testbed data
     * the thrown error is unrelated to the things covered in the testcase.
     * arminw: should be fixed
     */
    public void testResultsWhileTransactionWithCheckpoint() throws Exception
    {
        // if(ojbSkipKnownIssueProblem()) return;

        int odmgZoosBefore = getDBObjectCountWithNewPB(ODMGZoo.class);
        int projectsBefore = getDBObjectCountWithNewPB(ODMGGourmet.class);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        List zoos = new ArrayList(getNewODMGZoos("testResultsWhileTransactionWithCheckpoint", 5));
        List projects = new ArrayList(getNewProjects("testResultsWhileTransactionWithCheckpoint", 3));
        //store some objects
        storeObjects(tx, zoos);
        storeObjects(tx, projects);
        // checkpoint, should bring objects to DB but shouldn't commit
        tx.checkpoint();

        //Do a queries within a transaction
        int odmgZoosWhile = getDBObjectCountViaOqlQuery(odmg, ODMGZoo.class);
        int projectsWhile = getDBObjectCountViaOqlQuery(odmg, ODMGGourmet.class);
        int odmgZoosWhilePB = 0;
        int projectsWhilePB = 0;

        odmgZoosWhilePB = getDBObjectCount(tx.getBroker(), ODMGZoo.class);
        projectsWhilePB = getDBObjectCount(tx.getBroker(), ODMGGourmet.class);

        //store more
        List zoos2 = new ArrayList(getNewODMGZoos("testResultsWhileTransactionWithCheckpoint", 5));
        List projects2 = new ArrayList(getNewProjects("testResultsWhileTransactionWithCheckpoint", 2));
        storeObjects(tx, zoos2);
        storeObjects(tx, projects2);

        zoos.addAll(zoos2);
        projects.addAll(projects2);
        // checkpoint, should bring objects to DB but shouldn't commit
        tx.checkpoint();

        // after checkpoint another tx should NOT be able to lock these objects
        TransactionImpl tx2 = (TransactionImpl) odmg.newTransaction();
        tx2.begin();
        try
        {
            Iterator it = zoos.iterator();
            while (it.hasNext())
            {
                Object o =  it.next();
                tx2.lock(o, Transaction.WRITE);
            }

            it = projects.iterator();
            while (it.hasNext())
            {
                Object o =  it.next();
                tx2.lock(o, Transaction.WRITE);
            }
            fail("After checkpoint all locks should be still exist for objects");
        }
        catch(LockNotGrantedException e)
        {
            // expected
            assertTrue(true);
        }
        finally
        {
            tx2.abort();
        }
        // reassign tx
        tx.join();

        //more queries
        int odmgZoosWhile2 = getDBObjectCountViaOqlQuery(odmg, ODMGZoo.class);
        int projectsWhile2 = getDBObjectCountViaOqlQuery(odmg, ODMGGourmet.class);
        int odmgZoosWhilePB2 = 0;
        int projectsWhilePB2 = 0;

        odmgZoosWhilePB2 = getDBObjectCount(tx.getBroker(), ODMGZoo.class);
        projectsWhilePB2 = getDBObjectCount(tx.getBroker(), ODMGGourmet.class);

        tx.commit();
        int odmgZoosAfter = getDBObjectCountWithNewPB(ODMGZoo.class);
        int projectsAfter = getDBObjectCountWithNewPB(ODMGGourmet.class);
        int odmgZoosAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGZoo.class);
        int projectsAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGGourmet.class);

        assertEquals("Wrong number of odmgZoos found after commit", (odmgZoosBefore + 10), odmgZoosAfter);
        assertEquals("Wrong number of projects found after commit", (projectsBefore + 5), projectsAfter);
        assertEquals("Wrong number of odmgZoos found after commit", (odmgZoosBefore + 10), odmgZoosAfterOQL);
        assertEquals("Wrong number of projects found after commit", (projectsBefore + 5), projectsAfterOQL);

        /*
        Here we test if we can see our changes while the transaction runs. IMO it must be
        possible to see all changes made in a transaction.
        */

        assertEquals("Wrong number of odmgZoos found while transaction", (odmgZoosBefore + 5), odmgZoosWhilePB);
        assertEquals("Wrong number of projects found while transaction", (projectsBefore + 3), projectsWhilePB);
        assertEquals("Wrong number of odmgZoos found while transaction", (odmgZoosBefore + 10), odmgZoosWhilePB2);
        assertEquals("Wrong number of projects found while transaction", (projectsBefore + 5), projectsWhilePB2);
        assertEquals("Wrong number of odmgZoos found while transaction", (odmgZoosBefore + 5), odmgZoosWhile);
        assertEquals("Wrong number of projects found while transaction", (projectsBefore + 3), projectsWhile);
        assertEquals("Wrong number of odmgZoos found while transaction", (odmgZoosBefore + 10), odmgZoosWhile2);
        assertEquals("Wrong number of projects found while transaction", (projectsBefore + 5), projectsWhile2);

        // after tx another tx should be able to lock these objects
        tx2 = (TransactionImpl) odmg.newTransaction();
        tx2.begin();
        try
        {
            Iterator it = zoos.iterator();
            while (it.hasNext())
            {
                Object o =  it.next();
                tx2.lock(o, Transaction.WRITE);
            }

            it = projects.iterator();
            while (it.hasNext())
            {
                Object o =  it.next();
                tx2.lock(o, Transaction.WRITE);
            }
        }
        finally
        {
            tx2.abort();
        }
    }

    /**
     * Tests object count after a commited transaction
     * thma:
     * this testcase seems to fail for some strange problems with the testbed data
     * the thrown error is unrelated to the things covered in the testcase.
     * arminw: should be fixed
     */
    public void testResultsAfterTransaction() throws Exception
    {
        // if(ojbSkipKnownIssueProblem()) return;

        int odmgZoosBefore = getDBObjectCountWithNewPB(ODMGZoo.class);
        int projectsBefore = getDBObjectCountWithNewPB(ODMGGourmet.class);

        Transaction tx = odmg.newTransaction();
        tx.begin();
        //store
        persistentStoreObjects(database, getNewODMGZoos("testResultsAfterTransaction", 5));
        persistentStoreObjects(database, getNewProjects("testResultsAfterTransaction", 3));
        //store more
        storeObjects(tx, getNewODMGZoos("testResultsAfterTransaction", 5));
        storeObjects(tx, getNewProjects("testResultsAfterTransaction", 2));
        tx.commit();

        int odmgZoosAfter = getDBObjectCountWithNewPB(ODMGZoo.class);
        int projectsAfter = getDBObjectCountWithNewPB(ODMGGourmet.class);
        int odmgZoosAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGZoo.class);
        int projectsAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGGourmet.class);

        assertEquals("Wrong number of odmgZoos found", (odmgZoosBefore + 10), odmgZoosAfter);
        assertEquals("Wrong number of projects found", (projectsBefore + 5), projectsAfter);
        assertEquals("Wrong number of odmgZoos found", (odmgZoosBefore + 10), odmgZoosAfterOQL);
        assertEquals("Wrong number of projects found", (projectsBefore + 5), projectsAfterOQL);


        //we do twice
        odmgZoosBefore = getDBObjectCountWithNewPB(ODMGZoo.class);
        projectsBefore = getDBObjectCountWithNewPB(ODMGGourmet.class);

        //tx should be reusable
        tx.begin();
        //store
        persistentStoreObjects(database, getNewODMGZoos("testResultsAfterTransaction", 5));
        persistentStoreObjects(database, getNewProjects("testResultsAfterTransaction", 3));
        //store more
        storeObjects(tx, getNewODMGZoos("testResultsAfterTransaction", 5));
        storeObjects(tx, getNewProjects("testResultsAfterTransaction", 2));
        tx.commit();

        odmgZoosAfter = getDBObjectCountWithNewPB(ODMGZoo.class);
        projectsAfter = getDBObjectCountWithNewPB(ODMGGourmet.class);
        odmgZoosAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGZoo.class);
        projectsAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGGourmet.class);

        assertEquals("Wrong number of odmgZoos found", (odmgZoosBefore + 10), odmgZoosAfter);
        assertEquals("Wrong number of projects found", (projectsBefore + 5), projectsAfter);
        assertEquals("Wrong number of odmgZoos found", (odmgZoosBefore + 10), odmgZoosAfterOQL);
        assertEquals("Wrong number of projects found", (projectsBefore + 5), projectsAfterOQL);

    }

    /**
     * Tests object count after a commited transaction
     */
    public void testResultsAfterTransactionWithClearedCache() throws Exception
    {
        int odmgZoosBefore = getDBObjectCountWithNewPB(ODMGZoo.class);
        int projectsBefore = getDBObjectCountWithNewPB(ODMGGourmet.class);

        Transaction tx = odmg.newTransaction();
        tx.begin();
        //store
        persistentStoreObjects(database, getNewODMGZoos("testResultsAfterTransactionWithClearedCache", 5));
        persistentStoreObjects(database, getNewProjects("testResultsAfterTransactionWithClearedCache", 3));
        //store more
        storeObjects(tx, getNewODMGZoos("testResultsAfterTransactionWithClearedCache", 5));
        storeObjects(tx, getNewProjects("testResultsAfterTransactionWithClearedCache", 2));
        tx.commit();

        //###### hack we clear cache of PB ########
        PersistenceBroker tmp = PersistenceBrokerFactory.defaultPersistenceBroker();
        tmp.clearCache();
        tmp.close();

        int odmgZoosAfter = getDBObjectCountWithNewPB(ODMGZoo.class);
        int projectsAfter = getDBObjectCountWithNewPB(ODMGGourmet.class);
        int odmgZoosAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGZoo.class);
        int projectsAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGGourmet.class);

        assertEquals("Wrong number of odmgZoos found", (odmgZoosBefore + 10), odmgZoosAfter);
        assertEquals("Wrong number of projects found", (projectsBefore + 5), projectsAfter);
        assertEquals("Wrong number of odmgZoos found", (odmgZoosBefore + 10), odmgZoosAfterOQL);
        assertEquals("Wrong number of projects found", (projectsBefore + 5), projectsAfterOQL);


        //we do twice
        odmgZoosBefore = getDBObjectCountWithNewPB(ODMGZoo.class);
        projectsBefore = getDBObjectCountWithNewPB(ODMGGourmet.class);

        //tx should be reusable
        tx.begin();
        //store
        persistentStoreObjects(database, getNewODMGZoos("testResultsAfterTransactionWithClearedCache", 5));
        persistentStoreObjects(database, getNewProjects("testResultsAfterTransactionWithClearedCache", 3));
        //store more
        storeObjects(tx, getNewODMGZoos("testResultsAfterTransactionWithClearedCache", 5));
        storeObjects(tx, getNewProjects("testResultsAfterTransactionWithClearedCache", 2));
        tx.commit();

        //###### hack we clear cache of PB ########
        tmp = PersistenceBrokerFactory.defaultPersistenceBroker();
        tmp.clearCache();
        tmp.close();

        odmgZoosAfter = getDBObjectCountWithNewPB(ODMGZoo.class);
        projectsAfter = getDBObjectCountWithNewPB(ODMGGourmet.class);
        odmgZoosAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGZoo.class);
        projectsAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGGourmet.class);

        assertEquals("Wrong number of odmgZoos found", (odmgZoosBefore + 10), odmgZoosAfter);
        assertEquals("Wrong number of projects found", (projectsBefore + 5), projectsAfter);
        assertEquals("Wrong number of odmgZoos found", (odmgZoosBefore + 10), odmgZoosAfterOQL);
        assertEquals("Wrong number of projects found", (projectsBefore + 5), projectsAfterOQL);
    }

    /**
     * Test the rollback behaviour. If i store 10 odmgZoos within a transaction and after
     * that store the transaction was aborted, the number of odmgZoos in the DB should be unchanged.
     */
    public void testUserRollback() throws Exception
    {
        int odmgZoosBefore = getDBObjectCountWithNewPB(ODMGZoo.class);
        int projectsBefore = getDBObjectCountWithNewPB(ODMGGourmet.class);

        Transaction tx = odmg.newTransaction();
        tx.begin();
        storeObjects(tx, getNewODMGZoos("testUserRollback", 10));
        storeObjects(tx, getNewProjects("testUserRollback", 10));
        //we abort tx
        tx.abort();

        int odmgZoosAfter = getDBObjectCountWithNewPB(ODMGZoo.class);
        int projectsAfter = getDBObjectCountWithNewPB(ODMGGourmet.class);
        int odmgZoosAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGZoo.class);
        int projectsAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGGourmet.class);

        assertEquals("Wrong number of odmgZoos found", odmgZoosBefore, odmgZoosAfter);
        assertEquals("Wrong number of projects found", projectsBefore, projectsAfter);
        assertEquals("Wrong number of odmgZoos found", (odmgZoosBefore), odmgZoosAfterOQL);
        assertEquals("Wrong number of projects found", (projectsBefore), projectsAfterOQL);

        //We do this twice
        odmgZoosBefore = getDBObjectCountWithNewPB(ODMGZoo.class);
        projectsBefore = getDBObjectCountWithNewPB(ODMGGourmet.class);

        tx.begin();
        storeObjects(tx, getNewODMGZoos("testUserRollback", 10));
        storeObjects(tx, getNewProjects("testUserRollback", 10));
        //we abort tx
        tx.abort();

        odmgZoosAfter = getDBObjectCountWithNewPB(ODMGZoo.class);
        projectsAfter = getDBObjectCountWithNewPB(ODMGGourmet.class);
        odmgZoosAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGZoo.class);
        projectsAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGGourmet.class);

        assertEquals("Wrong number of odmgZoos found", odmgZoosBefore, odmgZoosAfter);
        assertEquals("Wrong number of projects found", projectsBefore, projectsAfter);
        assertEquals("Wrong number of odmgZoos found", (odmgZoosBefore), odmgZoosAfterOQL);
        assertEquals("Wrong number of projects found", (projectsBefore), projectsAfterOQL);
    }

    /**
     * Test the rollback behaviour. If i store 10 odmgZoos within a transaction and do a checkpoint call.
     * After that the transaction was aborted, the number of odmgZoos in the DB should be unchanged.
     */
    public void testUserRollbackWithCheckpoint() throws Exception
    {
        int odmgZoosBefore = getDBObjectCountWithNewPB(ODMGZoo.class);
        int projectsBefore = getDBObjectCountWithNewPB(ODMGGourmet.class);

        Transaction tx = odmg.newTransaction();
        tx.begin();
        storeObjects(tx, getNewODMGZoos("testUserRollbackWithCheckpoint", 10));
        // now we store objects to DB
        tx.checkpoint();
        storeObjects(tx, getNewProjects("testUserRollbackWithCheckpoint", 10));
        //we abort tx, all actions after the last checkpoint call should be rollback
        tx.abort();

        int odmgZoosAfter = getDBObjectCountWithNewPB(ODMGZoo.class);
        int projectsAfter = getDBObjectCountWithNewPB(ODMGGourmet.class);
        int odmgZoosAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGZoo.class);
        int projectsAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGGourmet.class);

        assertEquals("Wrong number of odmgZoos found", odmgZoosBefore + 10, odmgZoosAfter);
        assertEquals("Wrong number of projects found", projectsBefore, projectsAfter);
        assertEquals("Wrong number of odmgZoos found", odmgZoosBefore + 10, odmgZoosAfterOQL);
        assertEquals("Wrong number of projects found", projectsBefore, projectsAfterOQL);

        //***********************
        // do the procedure again
        odmgZoosBefore = getDBObjectCountWithNewPB(ODMGZoo.class);
        projectsBefore = getDBObjectCountWithNewPB(ODMGGourmet.class);
        // we reuse current tx
        tx.begin();
        storeObjects(tx, getNewODMGZoos("testUserRollbackWithCheckpoint", 10));
        // now we store objects to DB
        tx.checkpoint();
        storeObjects(tx, getNewProjects("testUserRollbackWithCheckpoint", 10));
        //we abort tx, all actions after the last checkpoint call should be rollback
        tx.abort();

        odmgZoosAfter = getDBObjectCountWithNewPB(ODMGZoo.class);
        projectsAfter = getDBObjectCountWithNewPB(ODMGGourmet.class);
        odmgZoosAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGZoo.class);
        projectsAfterOQL = getDBObjectCountViaOqlQueryUseNewTransaction(odmg, ODMGGourmet.class);

        assertEquals("Wrong number of odmgZoos found", odmgZoosBefore + 10, odmgZoosAfter);
        assertEquals("Wrong number of projects found", projectsBefore, projectsAfter);
        assertEquals("Wrong number of odmgZoos found", odmgZoosBefore + 10, odmgZoosAfterOQL);
        assertEquals("Wrong number of projects found", projectsBefore, projectsAfterOQL);
    }




    private void storeObjects(Transaction tx, Collection objects)
    {
        for (Iterator iterator = objects.iterator(); iterator.hasNext();)
        {
            tx.lock(iterator.next(), Transaction.WRITE);
        }
    }

    private void persistentStoreObjects(Database database, Collection objects)
    {
        for (Iterator iterator = objects.iterator(); iterator.hasNext();)
        {
            database.makePersistent(iterator.next());
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

    protected ODMGGourmet newProject(String name)
    {
        ODMGGourmet p = new ODMGGourmet();
        ++counter;
        if(name == null)
        {
            p.setName("Test " + counter);
        }
        else
        {
            p.setName(name + "_" + counter);
        }
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

    protected ODMGZoo newODMGZoo(String name)
    {
        ODMGZoo odmgZoo = new ODMGZoo();
        ++counter;
        if(name == null)
        {
            odmgZoo.setName("animal_" + counter);
        }
        else
        {
            odmgZoo.setName(name + "_" + counter);
        }
        return odmgZoo;
    }

    protected int getDBObjectCountWithNewPB(Class target) throws Exception
    {
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
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

    protected int getDBObjectCountViaOqlQueryUseNewTransaction(Implementation odmg, Class target) throws Exception
    {
        Transaction tx = odmg.newTransaction();
        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select allODMGGourmets from " + target.getName());
        List list = (List) query.execute();
        tx.commit();
        return list.size();
    }

    protected int getDBObjectCountViaOqlQuery(Implementation odmg, Class target) throws Exception
    {
        OQLQuery query = odmg.newOQLQuery();
        query.create("select allObjects from " + target.getName());
        List list = (List) query.execute();
        return list.size();
    }

    protected void deleteAll(Implementation odmg, Class target) throws Exception
    {
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.setCascadingDelete(target, true);
        OQLQuery query = odmg.newOQLQuery();
        query.create("select allObjects from " + target.getName());
        List list = (List) query.execute();
        for (int i = 0; i < list.size(); i++)
        {
            Object obj = list.get(i);
            database.deletePersistent(obj);
        }
        tx.commit();
    }

    // user described test case
    public void testDuplicateLocking() throws Exception
    {
        RollbackObjectOne ro = null;

        Transaction tx = odmg.newTransaction();
        tx.begin();
        ro = new RollbackObjectOne();
        ro.setName("test_step_1");
        tx.lock(ro, Transaction.WRITE);
        tx.lock(ro, Transaction.WRITE);
        tx.commit();

        tx.begin();
        tx.lock(ro, Transaction.WRITE);
        tx.lock(ro, Transaction.WRITE);
        ro.setName("test_step_2");
        tx.commit();

        tx.begin();
        tx.lock(ro, Transaction.WRITE);
        ro.setName("test_step_3");
        tx.lock(ro, Transaction.WRITE);
        tx.commit();
    }

    public void testCheckCacheAfterRollback() throws Exception
    {
        RollbackObjectOne ro = null;
        RollbackObjectOne ro_2 = null;
        String name = "testCheckCacheAfterRollback_"+System.currentTimeMillis();

        Transaction tx = odmg.newTransaction();
        tx.begin();
        ro = new RollbackObjectOne();
        ro.setName(name);
        tx.lock(ro, Transaction.WRITE);

        ro_2 = new RollbackObjectOne();
        ro_2.setName(name);
        tx.lock(ro_2, Transaction.WRITE);
        tx.commit();

        tx = odmg.newTransaction();
        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select all from " + RollbackObjectOne.class.getName() + " where name like $1");
        query.bind(name);
        List list = (List) query.execute();
        tx.commit();
        assertEquals(2,list.size());

        tx = odmg.newTransaction();
        tx.begin();
        tx.lock(ro, Transaction.WRITE);
        ro = new RollbackObjectOne();
        ro.setDescription(name);

        tx.lock(ro_2, Transaction.WRITE);
        ro_2 = new RollbackObjectOne();
        ro_2.setDescription(name);

        tx.abort();

        tx = odmg.newTransaction();
        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select all from " + RollbackObjectOne.class.getName() + " where name like $1");
        query.bind(name);
        list = (List) query.execute();
        tx.commit();
        assertEquals(2,list.size());
        assertNull(((RollbackObjectOne)list.get(0)).getDescription());
        assertNull(((RollbackObjectOne)list.get(1)).getDescription());

        // after tx another tx should be able to lock these objects
        TransactionImpl tx2 = (TransactionImpl) odmg.newTransaction();
        tx2.begin();
        try
        {
            Iterator it = list.iterator();
            while (it.hasNext())
            {
                Object o =  it.next();
                tx2.lock(o, Transaction.WRITE);
            }
        }
        finally
        {
            tx2.abort();
        }
    }

    /**
     * test empty usage of methods
     */
    public void testEmpty() throws Exception
    {
        // get new tx instance each time
        Transaction tx = odmg.newTransaction();
        tx.begin();
        tx.abort();

        tx = odmg.newTransaction();
        tx.begin();
        tx.checkpoint();
        tx.checkpoint();
        tx.abort();

        tx = odmg.newTransaction();
        tx.begin();
        tx.checkpoint();
        tx.checkpoint();
        tx.commit();

        tx = odmg.newTransaction();
        tx.begin();
        tx.commit();

        // with same tx instance
        tx = odmg.newTransaction();
        tx.begin();
        tx.abort();

        tx.begin();
        tx.checkpoint();
        tx.checkpoint();
        tx.abort();

        tx.begin();
        tx.checkpoint();
        tx.checkpoint();
        tx.commit();

        tx.begin();
        tx.commit();
    }

    public void testDoubleAbortTxCall() throws Exception
    {
        try
        {
            Transaction tx = odmg.newTransaction();
            tx.begin();
            tx.abort();
            tx.abort();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fail("We allow to do multiple tx.abort calls, but this test fails with: " + e.getMessage());
        }
    }

    public void testInternalCausedRollback() throws Exception
    {
        Transaction tx = odmg.newTransaction();
        String name = "testCheckCacheAfterRollback_"+System.currentTimeMillis();
        try
        {
            tx.begin();

            RollbackObjectOne ro = new RollbackObjectOne();
            ro.setName(name);
            tx.lock(ro, Transaction.WRITE);
            // this should fail
            tx.lock(new Exception(), Transaction.WRITE);

            tx.commit();
            fail("A exception was expected");
        }
        catch(Exception e)
        {
            if(tx != null && tx.isOpen()) tx.abort();
        }
    }


    //**************************************************************
    // test classes
    //**************************************************************
    public static class RollbackObjectOne
    {
        Integer objId;
        String name;
        String description;

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

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }

    public static class RollbackObjectTwo
    {
        Integer objId;
        String name;
        String description;

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

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }
}
