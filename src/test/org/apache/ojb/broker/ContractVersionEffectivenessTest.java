/**
 * Author: Matthew Baird
 * mattbaird@yahoo.com
 */
package org.apache.ojb.broker;

import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.junit.PBTestCase;

import java.sql.Timestamp;
import java.util.Iterator;

/**
 * This TestClass tests OJB ability to handle Contract Version Effectiveness patterns.
 */
public class ContractVersionEffectivenessTest extends PBTestCase
{
    private int COUNT = 10;
    public static void main(String[] args)
    {
        String[] arr = {ContractVersionEffectivenessTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**
     * Insert the method's description here.
     * Creation date: (24.12.2000 00:33:40)
     */
    public ContractVersionEffectivenessTest(String name)

    {
        super(name);
    }

    /**
     * Insert the method's description here.
     * Creation date: (06.12.2000 21:58:53)
     */
    public void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * Insert the method's description here.
     * Creation date: (06.12.2000 21:59:14)
     */
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    private void createTestData()
    {
        broker.beginTransaction();
        for (int i = 0; i < COUNT; i++)
        {
            Contract contract = new Contract();
            contract.setPk("C"+System.currentTimeMillis());
            contract.setContractValue1("contractvalue1");
            contract.setContractValue2(1);
            contract.setContractValue3("contractvalue3");
            contract.setContractValue4(new Timestamp(System.currentTimeMillis()));
            broker.store(contract);

            Version version = new Version();
            version.setPk("V"+System.currentTimeMillis());
            version.setVersionValue1("versionvalue1");
            version.setVersionValue2(1);
            version.setVersionValue3(new Timestamp(System.currentTimeMillis()));
            version.setContract(contract);
            broker.store(version);

            Effectiveness eff = new Effectiveness();
            eff.setPk("E"+System.currentTimeMillis());
            eff.setEffValue1("effvalue1");
            eff.setEffValue2(1);
            eff.setEffValue3(new Timestamp(System.currentTimeMillis()));
            eff.setVersion(version);
            broker.store(eff);
        }
        broker.commitTransaction();
    }

    public void testCreateContractVersionEffectiveness()
    {
        createTestData();
    }

    public void testAutoRetrieveFalse()
    {
        ojbChangeReferenceSetting(Contract.class, "relatedToContract", false,
                ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        String name = "testAutoRetrieveFalse_" + System.currentTimeMillis();
        Contract contract = new Contract();
        contract.setPk("C"+System.currentTimeMillis());
        contract.setContractValue1(name + "_Contract");
        contract.setContractValue2(1);
        contract.setContractValue3("contractvalue3");
        contract.setContractValue4(new Timestamp(System.currentTimeMillis()));

        RelatedToContract rc = new RelatedToContract();
        rc.setPk("R_" + System.currentTimeMillis());
        rc.setRelatedValue1(name + "_RelatedToContract");

        contract.setRelatedToContract(rc);
        broker.beginTransaction();
        // auto-update is true
        broker.store(contract);
        broker.commitTransaction();

        broker.clearCache();
        Identity oid = broker.serviceIdentity().buildIdentity(contract);
        Contract newC = (Contract) broker.getObjectByIdentity(oid);
        assertNotNull(newC);
        // auto-retrieve is false
        assertNull(newC.getRelatedToContract());
        broker.retrieveAllReferences(newC);
        // now the field should be populated
        assertNotNull(newC.getRelatedToContract());
    }

    public void testUpdateContractVersionEffectiveness()
    {
        createTestData();

        Criteria crit = new Criteria();
        Query q;
        Iterator iter;
        /**
         * update effectiveness first
         */
        q = QueryFactory.newQuery(Effectiveness.class, crit);
        iter = broker.getIteratorByQuery(q);
        Effectiveness eff = null;
        broker.beginTransaction();
        while (iter.hasNext())
        {
            eff = (Effectiveness)iter.next();
            eff.setEffValue1("effValueUpdated");
            broker.store(eff);
        }
        broker.commitTransaction();
        /**
         * then version
         */
        Version version = null;
        q = QueryFactory.newQuery(Version.class, crit);
        iter = broker.getIteratorByQuery(q);
        broker.beginTransaction();
        while (iter.hasNext())
        {
            version = (Version) iter.next();
            version.setVersionValue1("verValueUpdated");
            broker.store(version);
        }
        broker.commitTransaction();
        /**
         * the contract
         */
        Contract contract = null;
        q = QueryFactory.newQuery(Contract.class, crit);
        iter = broker.getIteratorByQuery(q);
        broker.beginTransaction();
        while (iter.hasNext())
        {
            contract = (Contract) iter.next();
            contract.setContractValue1("contractValueUpdated");
            broker.store(contract);
        }
        broker.commitTransaction();
    }
    public void testDeleteContractVersionEffectiveness()
    {
        createTestData();

        Criteria crit = new Criteria();
        Query q;
        Iterator iter;
        /**
         * delete effectiveness first
         */
        q = QueryFactory.newQuery(Effectiveness.class, crit);
        iter = broker.getIteratorByQuery(q);
        broker.beginTransaction();
        while (iter.hasNext())
        {
            broker.delete(iter.next());
        }
        broker.commitTransaction();
        /**
         * then version
         */
        q = QueryFactory.newQuery(Version.class, crit);
        iter = broker.getIteratorByQuery(q);
        broker.beginTransaction();
        while (iter.hasNext())
        {
            broker.delete(iter.next());
        }
        broker.commitTransaction();
        /**
         * the contract
         */
        q = QueryFactory.newQuery(Contract.class, crit);
        iter = broker.getIteratorByQuery(q);
        broker.beginTransaction();
        while (iter.hasNext())
        {
            broker.delete(iter.next());
        }
        broker.commitTransaction();
    }

}
