package org.apache.ojb.broker;

import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.util.collections.ManageableVector;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.junit.PBTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Tests rollback and (simple) commit behaviour.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 */
public class PBRollbackTest extends PBTestCase
{
    public PBRollbackTest(String s)
    {
        super(s);
    }

    public void testEmptyTxDemarcation_1()
    {
        try
        {
            broker.beginTransaction();

            broker.commitTransaction();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("'Empty' transaction demarcation sequence fails");
        }
    }

    public void testEmptyTxDemarcation_2()
    {
        try
        {
            broker.beginTransaction();

            broker.abortTransaction();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("'Empty' transaction demarcation sequence fails");
        }
    }

    public void testUserCommitClearCache() throws Exception
    {
        Collection projects = getNewProjects(10);
        storeObjects(broker, projects);
        Criteria c = new Criteria();
        Query q = new QueryByCriteria(Project.class, c);
        int beforeCommit = broker.getCount(q);

        broker.beginTransaction();
        broker.clearCache();
        storeObjects(broker, getNewProjects(10));
        //while transaction we should see all stored objects
        int whileTransaction = broker.getCount(q);
        ManageableCollection result = broker.getCollectionByQuery(ManageableVector.class, q);
        int whileTransactionMC = 0;
        Iterator it = result.ojbIterator();
        while (it.hasNext())
        {
            it.next();
            ++whileTransactionMC;
        }
        broker.commitTransaction();
        //explicit clear cache
        broker.clearCache();

        c = new Criteria();
        q = new QueryByCriteria(Project.class, c);
        int afterCommit = broker.getCount(q);

        assertEquals(beforeCommit + 10, afterCommit);
        assertEquals(beforeCommit + 10, whileTransaction);
        assertEquals(beforeCommit + 10, whileTransactionMC);
    }

    public void testUserCommit() throws Exception
    {
        Collection projects = getNewProjects(10);
        storeObjects(broker, projects);
        Criteria c = new Criteria();
        Query q = new QueryByCriteria(Project.class, c);
        int beforeCommit = broker.getCount(q);

        broker.beginTransaction();
        storeObjects(broker, getNewProjects(10));
        int whileTransaction = broker.getCount(q);
        ManageableCollection result = broker.getCollectionByQuery(ManageableVector.class, q);
        int whileTransactionMC = 0;
        Iterator it = result.ojbIterator();
        while (it.hasNext())
        {
            it.next();
            ++whileTransactionMC;
        }
        broker.commitTransaction();


        c = new Criteria();
        q = new QueryByCriteria(Project.class, c);
        broker.beginTransaction();
        int afterCommit = broker.getCount(q);
        broker.commitTransaction();

        assertEquals(beforeCommit + 10, afterCommit);
        assertEquals(beforeCommit + 10, whileTransaction);
        assertEquals(beforeCommit + 10, whileTransactionMC);
    }

    public void testUserRollbackClearCache() throws Exception
    {
        Collection projects = getNewProjects(10);
        storeObjects(broker, projects);
        Criteria c = new Criteria();
        Query q = new QueryByCriteria(Project.class, c);
        broker.beginTransaction();
        int beforeRollback = broker.getCount(q);
        broker.commitTransaction();

        broker.beginTransaction();
        storeObjects(broker, getNewProjects(10));
        broker.clearCache();

        int whileTransaction = broker.getCount(q);
        ManageableCollection result = broker.getCollectionByQuery(ManageableVector.class, q);
        int whileTransactionMC = 0;
        Iterator it = result.ojbIterator();
        while (it.hasNext())
        {
            it.next();
            ++whileTransactionMC;
        }

        broker.abortTransaction();
        //explicit clear cache
        broker.clearCache();

        c = new Criteria();
        q = new QueryByCriteria(Project.class, c);
        int afterRollback = broker.getCount(q);

        assertEquals(beforeRollback, afterRollback);
        assertEquals(beforeRollback + 10, whileTransaction);
        assertEquals(beforeRollback + 10, whileTransactionMC);
    }

    public void testUserRollback() throws Exception
    {
        Collection projects = getNewProjects(10);
        storeObjects(broker, projects);
        Criteria c = new Criteria();
        Query q = new QueryByCriteria(Project.class, c);
        broker.beginTransaction();
        int beforeRollback = broker.getCount(q);
        broker.commitTransaction();

        broker.beginTransaction();
        storeObjects(broker, getNewProjects(10));

        int whileTransaction = broker.getCount(q);
        ManageableCollection result = broker.getCollectionByQuery(ManageableVector.class, q);
        int whileTransactionMC = 0;
        Iterator it = result.ojbIterator();
        while (it.hasNext())
        {
            it.next();
            ++whileTransactionMC;
        }

        broker.abortTransaction();

        c = new Criteria();
        q = new QueryByCriteria(Project.class, c);
        int afterRollback = broker.getCount(q);

        assertEquals(beforeRollback, afterRollback);
        assertEquals(beforeRollback + 10, whileTransaction);
        assertEquals(beforeRollback + 10, whileTransactionMC);
    }

    public void testRollbackCausedByNotExistingObject() throws Exception
    {
        Collection projects = getNewProjects(10);
        Query q;
        int beforeRollback;
        try
        {
            broker.beginTransaction();
            storeObjects(broker, projects);
            broker.commitTransaction();

            Criteria c = new Criteria();
            q = new QueryByCriteria(Project.class, c);
            beforeRollback = broker.getCount(q);
        }
        finally
        {
            if(broker != null && !broker.isClosed()) broker.close();
        }

        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        try
        {
            broker.beginTransaction();
            storeObjects(broker, getNewProjects(10));
            //should fail
            broker.store(new Dummy());

            fail("Test should throw a exception in place");
            broker.commitTransaction();
        }
        catch (PersistenceBrokerException e)
        {
            assertTrue(true);
            // e.printStackTrace();
            broker.abortTransaction();
        }

        int afterRollback = broker.getCount(q);

        assertEquals("Object count does not match after rollback", beforeRollback, afterRollback);
    }

    public void testRollbackCausedBySQLException() throws Exception
    {
        // first we change metadata settings
        ojbChangeReferenceSetting(
                Project.class,
                "persons",
                true,
                ObjectReferenceDescriptor.CASCADE_OBJECT,
                ObjectReferenceDescriptor.CASCADE_OBJECT,
                false);
        ArrayList projects = getNewProjects(5);
        Query q;
        int beforeRollback;

        broker.beginTransaction();
        storeObjects(broker, projects);
        broker.commitTransaction();

        Criteria c = new Criteria();
        q = new QueryByCriteria(Project.class, c);
        beforeRollback = broker.getCount(q);

        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        try
        {
            broker.beginTransaction();
            projects = getNewProjects(5);
            Project badProject = (Project) projects.get(0);
            badProject.setTitle("Bad project!");
            // set wrong kind of object to force exception
            badProject.setPersons(projects);

            System.err.println("!! The following SQLException is part of the Test !!");
            storeObjects(broker, projects);

            fail("Test should throw a exception in place");
            broker.commitTransaction();
        }
        catch (PersistenceBrokerException e)
        {
            assertTrue(true);
            // e.printStackTrace();
            broker.abortTransaction();
        }

        int afterRollback = broker.getCount(q);

        assertEquals("Object count does not match after rollback", beforeRollback, afterRollback);
    }

    protected void storeObjects(PersistenceBroker broker, Collection objects)
    {
        boolean needsCommit = false;
        if(!broker.isInTransaction())
        {
            broker.beginTransaction();
            needsCommit = true;
        }
        for (Iterator it = objects.iterator(); it.hasNext();)
        {
            broker.store(it.next());
        }
        if(needsCommit)
        {
            broker.commitTransaction();
        }
    }

    private static int counter;

    protected ArrayList getNewProjects(int count)
    {
        ArrayList list = new ArrayList();
        for (int i = 0; i < count; i++)
        {
            list.add(newProject());
        }
        return list;
    }

    protected Project newProject()
    {
        Project p = new Project();
        ++counter;
        p.setDescription("Test project " + counter);
        p.setTitle("Test " + counter);
        return p;
    }

    public static void main(String[] args)
    {
        String[] arr = {PBRollbackTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    class Dummy
    {

    }
}
