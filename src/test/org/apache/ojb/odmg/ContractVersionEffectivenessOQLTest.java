/*
 * Created by IntelliJ IDEA.
 * User: Matt
 * Date: May 19, 2002
 * Time: 3:43:36 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.apache.ojb.odmg;

import java.sql.Timestamp;

import org.apache.ojb.broker.Contract;
import org.apache.ojb.broker.Effectiveness;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.RelatedToContract;
import org.apache.ojb.broker.Version;
import org.apache.ojb.junit.ODMGTestCase;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

public class ContractVersionEffectivenessOQLTest extends ODMGTestCase
{

    private static Class CLASS = ContractVersionEffectivenessOQLTest.class;
    private int COUNT = 10;

	/**
	 * Insert the method's description here.
	 * Creation date: (23.12.2000 18:30:38)
	 * @param args java.lang.String[]
	 */
	public static void main(String[] args)
	{
		String[] arr = { CLASS.getName()};
		junit.textui.TestRunner.main(arr);
	}

    public ContractVersionEffectivenessOQLTest(String name)
    {
        super(name);
    }

    private void createData(Database db, Implementation odmg)
            throws Exception
    {
//        Implementation odmg = OJB.getInstance();
        Transaction tx = odmg.newTransaction();
        for (int i = 0; i < COUNT; i++)
        {
            tx.begin();
            Contract contract = new Contract();
            contract.setPk("C" + i + System.currentTimeMillis());
            contract.setContractValue1("contractvalue1");
            contract.setContractValue2(1);
            contract.setContractValue3("contractvalue3");
            contract.setContractValue4(new Timestamp(System.currentTimeMillis()));
            db.makePersistent(contract);

            Version version = new Version();
            version.setPk("V" + i + System.currentTimeMillis());
            version.setVersionValue1("versionvalue1");
            version.setVersionValue2(1);
            version.setVersionValue3(new Timestamp(System.currentTimeMillis()));
            version.setContract(contract);
            db.makePersistent(version);

            Effectiveness eff = new Effectiveness();
            eff.setPk("E" + i + System.currentTimeMillis());
            eff.setEffValue1("effvalue1");
            eff.setEffValue2(1);
            eff.setEffValue3(new Timestamp(System.currentTimeMillis()));
            eff.setVersion(version);
            /**
             * will create all
             */
            db.makePersistent(eff);
            tx.commit();
        }

    }

    public void testCreate() throws Exception
    {
        createData(database, odmg);
    }

    public void testComplexOQL()
            throws Exception
    {
        /**
         * 1. create the objects with specific values we'll search on later.
         */
        Transaction tx = odmg.newTransaction();
        tx.begin();

        Contract contract = new Contract();
        contract.setPk("C" + System.currentTimeMillis());
        contract.setContractValue1("version.contract.contractValue1.testComplexOQL");
        contract.setContractValue2(1);
        contract.setContractValue3("contractvalue3");
        contract.setContractValue4(new Timestamp(System.currentTimeMillis()));
        database.makePersistent(contract);

        RelatedToContract rtc = new RelatedToContract();
        rtc.setPk("R" + System.currentTimeMillis());
        rtc.setRelatedValue1("test");
        rtc.setRelatedValue2(5);
        rtc.setRelatedValue3(new Timestamp(System.currentTimeMillis()));
        contract.setRelatedToContract(rtc);
        database.makePersistent(rtc);

        Version version = new Version();
        version.setPk("V" + System.currentTimeMillis());
        version.setVersionValue1("versionvalue1");
        version.setVersionValue2(1);
        version.setVersionValue3(new Timestamp(System.currentTimeMillis()));
        version.setContract(contract);
        database.makePersistent(version);

        Effectiveness eff = new Effectiveness();
        eff.setPk("E" + System.currentTimeMillis());
        eff.setEffValue1("effValue1.testComplexOQL");
        eff.setEffValue2(20);
        eff.setEffValue3(new Timestamp(System.currentTimeMillis()));
        eff.setVersion(version);
        database.makePersistent(eff);

        tx.commit();
        /**
         * 2. define the complex OQL query to find the object we created
         */
        String oql = "select s from " + org.apache.ojb.broker.Effectiveness.class.getName() + " where " +
                " version.contract.contractValue1=$1 and effValue1 = $2 and " +
                " (effValue3 > $3 or is_undefined(effValue3)) and " +
                " effValue2 <= $4 and (effValue3<$5 or is_undefined(effValue3)) and " +
                " version.contract.relatedToContract.relatedValue2=$6";

        OQLQuery query = odmg.newOQLQuery();
        query.create(oql);
        query.bind("version.contract.contractValue1.testComplexOQL"); //version.contract.contractValue1=$1
        query.bind("effValue1.testComplexOQL"); // effValue1 = $2
        query.bind(new Timestamp(System.currentTimeMillis() - 5000)); // a while ago (effValue3 > $3)
        query.bind(new Integer(20)); // effValue2 <= $4
        query.bind(new Timestamp(System.currentTimeMillis() + 5000)); // a while from now (effValue3<$5)
        query.bind(new Integer(5)); // version.contract.relatedToContract.relatedValue2=$6

        ManageableCollection all = (ManageableCollection) query.execute();
        java.util.Iterator it = all.ojbIterator();
        /**
         * make sure we got
         */
        int i = 0;
        while (it.hasNext())
        {
            it.next();
            i++;
        }
        if (i != 1)
        {
            fail("Should have found just one object, instead found " + i);
        }
    }

	 public void testComplexOQL2()
            throws Exception
    {
        /**
         * 1. create the objects with specific values we'll search on later.
         */
        Transaction tx = odmg.newTransaction();
        tx.begin();

        Contract contract = new Contract();
        contract.setPk("C" + System.currentTimeMillis());
        contract.setContractValue1("version.contract.contractValue1.testComplexOQL");
        contract.setContractValue2(1);
        contract.setContractValue3("contractvalue3");
        contract.setContractValue4(new Timestamp(System.currentTimeMillis()));
        database.makePersistent(contract);

        Version version = new Version();
        version.setPk("V" + System.currentTimeMillis());
        version.setVersionValue1("versionvalue1");
        version.setVersionValue2(1);
        version.setVersionValue3(new Timestamp(System.currentTimeMillis()));
        version.setContract(contract);
        database.makePersistent(version);

        Effectiveness eff = new Effectiveness();
        eff.setPk("E" + System.currentTimeMillis());
        eff.setEffValue1("effValue1.testComplexOQL");
        eff.setEffValue2(20);
        eff.setEffValue3(new Timestamp(System.currentTimeMillis()));
        eff.setVersion(version);
        database.makePersistent(eff);

        tx.commit();
        /**
         * 2. define the complex OQL query to find the object we created
         */
        String oql = "select s from " + org.apache.ojb.broker.Effectiveness.class.getName() + " where " +
                " version.contract.contractValue1=$1 and effValue1 = $2 and " +
                " (effValue3 > $3 or is_undefined(effValue3)) and " +
                " effValue2 <= $4 and (effValue3<$5 or is_undefined(effValue3)) and " +
                " is_undefined(version.contract.relatedToContract.pk)";

        OQLQuery query = odmg.newOQLQuery();
        query.create(oql);
        query.bind("version.contract.contractValue1.testComplexOQL"); //version.contract.contractValue1=$1
        query.bind("effValue1.testComplexOQL"); // effValue1 = $2
        query.bind(new Timestamp(System.currentTimeMillis() - 5000)); // a while ago (effValue3 > $3)
        query.bind(new Integer(20)); // effValue2 <= $4
        query.bind(new Timestamp(System.currentTimeMillis() + 5000)); // a while from now (effValue3<$5)

        ManageableCollection all = (ManageableCollection) query.execute();
        java.util.Iterator it = all.ojbIterator();
        /**
         * make sure we got
         */
        int i = 0;
        while (it.hasNext())
        {
            it.next();
            i++;
        }
        if (i != 1)
        {
            fail("Should have found just one object, instead found " + i);
        }
    }

    public void testGetWithVersionCriteria() throws Exception
    {
        createData(database, odmg);
        OQLQuery query = odmg.newOQLQuery();
        int i = 0;
        query.create("select effectiveness from " + Effectiveness.class.getName() + " where version.versionValue1=$1");
        query.bind("versionvalue1");
        ManageableCollection all = (ManageableCollection) query.execute();
        java.util.Iterator it = all.ojbIterator();
        Effectiveness temp = null;
        while (it.hasNext())
        {
            temp = (Effectiveness) it.next();
            if (!temp.getVersion().getVersionValue1().equals("versionvalue1"))
            {
                fail("Should find only effectiveness objects where version.versionValue1='versionvalue1', found one with value " + temp.getVersion().getVersionValue1());
            }
            i++;
        }
        if (i < COUNT)
            fail("Should have found at least " + COUNT + " where version.versionValue1='versionvalue1' items, only found " + i);
    }

	public void testGetEmbeddedObject() throws Exception
    {
        createData(database, odmg);
        OQLQuery query = odmg.newOQLQuery();
        query.create("select effectiveness.version from " + Effectiveness.class.getName() + " where is_defined(effectiveness.version.versionValue1)");
        ManageableCollection all = (ManageableCollection) query.execute();
        java.util.Iterator it = all.ojbIterator();
        while (it.hasNext())
        {
            assertTrue("Selected item is Version", (it.next() instanceof Version));
        }

        query.create("select effectiveness.version.contract from " + Effectiveness.class.getName() + " where is_defined(effectiveness.version.versionValue1)");
        all = (ManageableCollection) query.execute();
        it = all.ojbIterator();
        while (it.hasNext())
        {
            assertTrue("Selected item is Contract", (it.next() instanceof Contract));
        }
    }


    public void testGetWithContractCriteria() throws Exception
    {
        createData(database, odmg);
        OQLQuery query = odmg.newOQLQuery();
        int i = 0;
        query.create("select effectiveness from " + Effectiveness.class.getName() + " where version.contract.contractValue1=$1");
        query.bind("contractvalue1");
        ManageableCollection all = (ManageableCollection) query.execute();
        java.util.Iterator it = all.ojbIterator();
        Effectiveness temp = null;
        while (it.hasNext())
        {
            temp = (Effectiveness) it.next();
            if (!temp.getVersion().getContract().getContractValue1().equals("contractvalue1"))
            {
                fail("Should find only effectiveness objects where contract.contractValue1='contractvalue1', found one with value " + temp.getVersion().getContract().getContractValue1());
            }
            i++;
        }
        if (i < COUNT)
            fail("Should have found at least " + COUNT + " where version.contract.contractValue1='contractvalue1' items, only found " + i);
    }

    public void testGet() throws Exception
    {
        createData(database, odmg);
        OQLQuery query = odmg.newOQLQuery();
        int i = 0;
        query.create("select effectiveness from " + Effectiveness.class.getName());
        ManageableCollection all = (ManageableCollection) query.execute();
        java.util.Iterator it = all.ojbIterator();
        while (it.hasNext())
        {
            it.next();
            i++;
        }
        if (i < COUNT)
            fail("Should have found at least " + COUNT + " items, only found " + i);
    }

    public void testDelete() throws Exception
    {
        /**
         * create some data for us to delete.
         */
        createData(database, odmg);

        // 3. Get a list of some articles
        Transaction tx = odmg.newTransaction();

        OQLQuery query = odmg.newOQLQuery();
        ManageableCollection all = null;
        java.util.Iterator it = null;
        int i = 0;
        query.create("select effectiveness from " + org.apache.ojb.broker.Effectiveness.class.getName());

        /**
         * try doing this as part of one transaction, ODMG should figure out
         * which order to delete in.
         */
        all = (ManageableCollection) query.execute();
        // Iterator over the restricted articles objects
        it = all.ojbIterator();
        Effectiveness eff = null;
        Version ver = null;
        Contract contract = null;
        while (it.hasNext())
        {
            eff = (Effectiveness) it.next();
            ver = eff.getVersion();
            contract = ver.getContract();

            tx.begin();
            database.deletePersistent(eff);
            tx.commit();

            tx.begin();
            database.deletePersistent(ver);
            tx.commit();

            tx.begin();
            database.deletePersistent(contract);
            tx.commit();
            // keep the count
            i++;
        }
        if (i < COUNT)
            fail("Should have found at least " + COUNT + " items to delete, only found " + i);
        /**
         * run query again, should get 0 results.
         */
        query.create("select contracts from " + org.apache.ojb.broker.Contract.class.getName());
        ManageableCollection allContracts = (ManageableCollection) query.execute();
        allContracts = (ManageableCollection) query.execute();
        it = allContracts.ojbIterator();
        if (it.hasNext())
        {
            fail("all contracts should have been removed, we found one.");
        }
    }

    /**
     * this test needs to either be invalidated as a test case, or ODMG has to be fixed.
     * @throws Exception
     */
    public void XtestNotYetWorkingDelete() throws Exception
    {
        /**
         * create some data for us to delete.
         */
        createData(database, odmg);

        // 3. Get a list of some articles
        Transaction tx = odmg.newTransaction();

        OQLQuery query = odmg.newOQLQuery();
        ManageableCollection all = null;
        java.util.Iterator it = null;
        int i = 0;
        query.create("select effectiveness from " + org.apache.ojb.broker.Effectiveness.class.getName());

        /**
         * try doing this as part of one transaction, ODMG should figure out
         * which order to delete in.
         */
        all = (ManageableCollection) query.execute();
        // Iterator over the restricted articles objects
        it = all.ojbIterator();
        Effectiveness eff = null;
        Version ver = null;
        Contract contract = null;
        /**
         * should mark all these objects for delete then on commit
         * ODMG should make sure they get deleted in proper order
         */
        tx.begin();
        while (it.hasNext())
        {
            eff = (Effectiveness) it.next();
            ver = eff.getVersion();
            contract = ver.getContract();
            /**
             * should mean that version and effectivedate are cascade deleted.
             */
            database.deletePersistent(contract);
            i++;
        }
        /**
         * commit all changes.
         */
        tx.commit();
        if (i < COUNT)
            fail("Should have found at least " + COUNT + " effectiveness to delete, only found " + i);
        /**
         * run query again, should get 0 results.
         */
        query.create("select contracts from " + org.apache.ojb.broker.Contract.class.getName());
        ManageableCollection allContracts = (ManageableCollection) query.execute();
        allContracts = (ManageableCollection) query.execute();
        it = allContracts.ojbIterator();
        if (it.hasNext())
        {
            fail("all contracts should have been removed, we found one.");
        }
    }

    /**
     * test getting all (make sure basic operation is still functional)
     */
    public void testQuery() throws Exception
    {
        createData(database, odmg);
        // 3. Get a list of some articles
        Transaction tx = odmg.newTransaction();
        tx.begin();

        OQLQuery query = odmg.newOQLQuery();
        String sql = "select effectiveness from " + Effectiveness.class.getName();
        query.create(sql);

        ManageableCollection allEffectiveness = (ManageableCollection) query.execute();

        // Iterator over the restricted articles objects
        java.util.Iterator it = allEffectiveness.ojbIterator();
        int i = 0;
        while (it.hasNext())
        {
            Effectiveness value = (Effectiveness) it.next();
            /**
             * check pk value of related contract item.
             */
            if (value.getVersion().getContract().getPk() == null)
                fail("Contract PK should not be null");
            i++;
        }
        if (i < COUNT)
            fail("Should have found at least " + COUNT + " items, only found: " + i);
        tx.commit();
    }

    /**
     * test changing a versions fk reference to it's contract.
     * The old bug in ODMG wouldn't trigger an update if an object reference changed.
     */
    public void testContractReassignment() throws Exception
    {
        Transaction tx = odmg.newTransaction();
        Contract contract = new Contract();
        contract.setPk("contract1");
        contract.setContractValue1("contract1value1");
        contract.setContractValue2(1);
        contract.setContractValue3("contract1value3");
        contract.setContractValue4(new Timestamp(System.currentTimeMillis()));

        Version version = new Version();
        version.setPk("version1");
        version.setVersionValue1("version1value1");
        version.setVersionValue2(1);
        version.setVersionValue3(new Timestamp(System.currentTimeMillis()));
        version.setContract(contract);

        Effectiveness eff = new Effectiveness();
        eff.setPk("eff1");
        eff.setEffValue1("eff1value1");
        eff.setEffValue2(1);
        eff.setEffValue3(new Timestamp(System.currentTimeMillis()));
        eff.setVersion(version);

        Contract contract2 = new Contract();
        contract2.setPk("contract2");
        contract2.setContractValue1("contract2value1");
        contract2.setContractValue2(1);
        contract2.setContractValue3("contractvalue3");
        contract2.setContractValue4(new Timestamp(System.currentTimeMillis()));

        Version version2 = new Version();
        version2.setPk("version2");
        version2.setVersionValue1("version2value1");
        version2.setVersionValue2(1);
        version2.setVersionValue3(new Timestamp(System.currentTimeMillis()));
        version2.setContract(contract2);

        Effectiveness eff2 = new Effectiveness();
        eff2.setPk("eff2");
        eff2.setEffValue1("eff2value1");
        eff2.setEffValue2(1);
        eff2.setEffValue3(new Timestamp(System.currentTimeMillis()));
        eff2.setVersion(version2);

        /**
         * make them persistent
         */
        tx.begin();
        database.makePersistent(eff2);
        database.makePersistent(eff);
        tx.commit();

        /**
         * do the reassignment
         */
        tx.begin();
        tx.lock(version, Transaction.WRITE);
        tx.lock(version2, Transaction.WRITE);
        version.setContract(contract2);
        version2.setContract(contract);
        tx.commit();

        /**
         * query and check values
         */
        OQLQuery query = odmg.newOQLQuery();
        String sql = "select version from " + org.apache.ojb.broker.Version.class.getName() + " where pk=$1";
        query.create(sql);
        query.bind("version1");
        tx.begin();
        ManageableCollection results = (ManageableCollection) query.execute();
        // Iterator over the restricted articles objects
        java.util.Iterator it = results.ojbIterator();
        Version ver1 = null;
        while (it.hasNext())
        {
            ver1 = (Version) it.next();
            if (!ver1.getContract().getPk().equals(contract2.getPk()))
            {
                fail(ver1.getPk() + " should have pointed to contract2 instead it pointed to: " + ver1.getContract().getPk());
            }
        }
        tx.commit();

        OQLQuery query2 = odmg.newOQLQuery();
        String sql2 = "select version from " + org.apache.ojb.broker.Version.class.getName() + " where pk=$1";
        query2.create(sql2);
        query2.bind("version2");
        tx.begin();
        results = (ManageableCollection) query2.execute();
        // Iterator over the restricted articles objects
        java.util.Iterator it2 = results.ojbIterator();
        Version ver2 = null;
        while (it2.hasNext())
        {
            ver2 = (Version) it2.next();
            if (!ver2.getContract().getPk().equals(contract.getPk()))
            {
                fail(ver2.getPk() + " should have pointed to contract instead it pointed to: " + ver2.getContract().getPk());
            }
        }
        tx.commit();

        /**
         * clean up
         */
        tx.begin();
        database.deletePersistent(eff2);
        database.deletePersistent(eff);
        tx.commit();
        tx.begin();
        database.deletePersistent(version2);
        database.deletePersistent(version);
        tx.commit();
        tx.begin();
        database.deletePersistent(contract2);
        database.deletePersistent(contract);
        tx.commit();
    }
}
