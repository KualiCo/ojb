package org.apache.ojb.odmg;

import org.apache.ojb.broker.Contract;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.shared.DetailFKinPK;
import org.apache.ojb.odmg.shared.DetailFKnoPK;
import org.apache.ojb.odmg.shared.Master;
import org.odmg.Implementation;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.List;

/** Demo Application that shows basic concepts for Applications using the OJB ODMG
 * implementation as an transactional object server.
 */
public class RITest extends ODMGTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {RITest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public RITest(String name)
    {
        super(name);
    }

    /**
     * Test referential integrity with the ODMG-Layer, the foreign key
     * is not a part of the primary key of the detail.
     */
    public void testStoreFKnoPK() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Transaction tx = odmg.newTransaction();
        tx.begin();
        Master master_1 = populatedMasterFKnoPK(tx, 5, timestamp);
        Master master_2 = populatedMasterFKnoPK(tx, 5, timestamp);

        database.makePersistent(master_1);
        database.makePersistent(master_2);
        tx.commit();

        // Check stored objects
        OQLQuery query = odmg.newOQLQuery();
        query.create("select masters from " + Master.class.getName() + " where masterText like $1");
        query.bind("%" + timestamp);
        List allMasters = (List) query.execute();
        assertEquals("We should found master objects", 2, allMasters.size());
        Master lookup_1 = (Master) allMasters.get(0);

        Collection col_in = lookup_1.getCollDetailFKinPK();
        Collection col_no = lookup_1.getCollDetailFKnoPK();
        assertEquals("Should found none " + DetailFKinPK.class.getName() + " objects", 0, col_in.size());
        assertEquals("Should found " + DetailFKnoPK.class.getName() + " objects", 5, col_no.size());
    }

    /**
     * Test referential integrity with the ODMG-Layer, the foreign key
     * is part of the primary key of the detail table in this case
     */
    public void testStoreFKinPK() throws Exception
    {
        final long timestamp = System.currentTimeMillis();
        Transaction tx = odmg.newTransaction();
        tx.begin();

        Master master_1 = populatedMasterFKinPK(tx, 5, timestamp);
        Master master_2 = populatedMasterFKinPK(tx, 5, timestamp);

        database.makePersistent(master_1);
        database.makePersistent(master_2);
        tx.commit();

        // Check stored objects
        OQLQuery query = odmg.newOQLQuery();
        query.create("select masters from " + Master.class.getName() + " where masterText like $1");
        query.bind("%" + timestamp);
        List allMasters = (List) query.execute();
        assertEquals("We should found master objects", 2, allMasters.size());
        Master lookup_1 = (Master) allMasters.get(0);
        Collection col_in = lookup_1.getCollDetailFKinPK();
        Collection col_no = lookup_1.getCollDetailFKnoPK();
        assertEquals("Should found none " + DetailFKnoPK.class.getName() + " objects", 0, col_no.size());
        assertEquals("Should found " + DetailFKinPK.class.getName() + " objects", 5, col_in.size());
    }

    private Master populatedMasterFKnoPK(Transaction tx, int countDetailObjects, long timestamp)
            throws Exception
    {
        Master master = new Master();
        master.masterText = "Master_timestamp_" + timestamp;
        master.collDetailFKnoPK = new Vector();
        new Identity(master, ((HasBroker) tx).getBroker());
        for (int i = 0; i < countDetailObjects; i++)
        {
            DetailFKnoPK aDetail = new DetailFKnoPK();
            aDetail.detailText = "DetailFK*no*PK count " + i + ", associate master " + master.masterId + " timestamp_" + timestamp;
            aDetail.master = master;
            master.collDetailFKnoPK.add(aDetail);
        }
        return master;
    }

    private Master populatedMasterFKinPK(Transaction tx, int countDetailObjects, long timestamp)
            throws Exception
    {
        Master master = new Master();
        master.masterText = "Master_timestamp_" + timestamp;
        master.collDetailFKinPK = new Vector();
        new Identity(master, ((HasBroker) tx).getBroker());
        for (int i = 0; i < countDetailObjects; i++)
        {
            DetailFKinPK aDetail = new DetailFKinPK();
            aDetail.detailText = "DetailFKinPK count " + i + ", associate master " + master.masterId + " timestamp_" + timestamp;
            aDetail.master = master;
            master.collDetailFKinPK.add(aDetail);
        }
        return master;
    }

    public void testDelete() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        // 2. Get a list of all Masters
        Transaction tx = odmg.newTransaction();
        tx.begin();
        // 1. Insert some objects into the database, some of them with
        // details in DetailFKinPK, some of them in DetailFKnoPK
        Master master_1 = populatedMasterFKinPK(tx, 7, timestamp);
        Master master_2 = populatedMasterFKinPK(tx, 6, timestamp);
        Master master_3 = populatedMasterFKnoPK(tx, 7, timestamp);
        Master master_4 = populatedMasterFKnoPK(tx, 6, timestamp);

        tx.lock(master_1, Transaction.WRITE);
        tx.lock(master_2, Transaction.WRITE);
        tx.lock(master_3, Transaction.WRITE);
        tx.lock(master_4, Transaction.WRITE);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select masters from " + Master.class.getName() + " where masterText like $1");
        query.bind("%" + timestamp);
        List allMasters = (List) query.execute();

        // Iterator over all Master objects
        Iterator it = allMasters.iterator();
        int counter = 0;
        while (it.hasNext())
        {
            ++counter;
            Master aMaster = (Master) it.next();
            Iterator it2 = aMaster.collDetailFKinPK.iterator();
            while (it2.hasNext())
                database.deletePersistent(it2.next());
            it2 = aMaster.collDetailFKnoPK.iterator();
            while (it2.hasNext())
                database.deletePersistent(it2.next());
            database.deletePersistent(aMaster);
        }
        tx.commit();
        assertEquals("Wrong count of Master objects found", 4, counter);

        query = odmg.newOQLQuery();
        query.create("select masters from " + Master.class.getName() + " where masterText like $1");
        query.bind("%" + timestamp);
        allMasters = (List) query.execute();
        assertEquals("Delete of Master objects failed", 0, allMasters.size());
    }

    public void testInsertAfterDelete() throws Exception
    {
        final String contractPk = "The Contract_" + System.currentTimeMillis();
        Contract obj1 = new Contract();
        Contract obj2 = new Contract();

        obj1.setPk(contractPk);
        obj1.setContractValue2(1);

        obj2.setPk(contractPk);
        obj2.setContractValue2(2);

        // 1. Insert object
        Transaction tx = odmg.newTransaction();
        tx.begin();
        /*
        arminw:
        seems to have problems when within a tx a object
        was stored/deleted.
        Without obj1 test pass
        TODO: fix this
        */
        database.makePersistent(obj1);
        database.deletePersistent(obj2);
        database.makePersistent(obj1);
        /*
         thma: I checked this, and don't see a problem here.
        obj1 and obj2 have the same Identity. Thus the
        calls database.makePersistent(obj1); and database.makePersistent(obj2);
        will only register one instance to the transaction.
        The second call does not add a second instance, but just marks the
        existing instance as dirty a second time.
        So it's no wonder why after deletePersistent(obj1); no contract is found.
        Works as designed.
        The Lesson to learn: never let business objects have the same primary key values!
         * */
        tx.commit();
        Collection result = getContract(contractPk, odmg);
        assertEquals("We should found exact one contract", 1, result.size());
        Contract newObj = (Contract) result.iterator().next();
        assertNotNull("Object not found", newObj);
        assertEquals(1, newObj.getContractValue2());

        // 2. Delete, then insert object with the same identity
        tx.begin();
        database.deletePersistent(newObj);
        database.makePersistent(obj2);
        tx.commit();
        assertEquals(2, obj2.getContractValue2());

        result = getContract(contractPk, odmg);
        assertEquals("We should found exact one contract", 1, result.size());
        newObj = (Contract) result.iterator().next();
        assertNotNull("Object not found", newObj);
        assertEquals(2, newObj.getContractValue2());

        // 3. Delete
        tx.begin();
        database.deletePersistent(obj1);
        tx.commit();

        result = getContract(contractPk, odmg);
        assertEquals(0, result.size());
    }

    private Collection getContract(String pk, Implementation odmg)
            throws Exception
    {
        OQLQuery query = odmg.newOQLQuery();
        query.create("select c from " + Contract.class.getName() + " where pk like $1");
        query.bind(pk);
        return (Collection) query.execute();
    }
}
