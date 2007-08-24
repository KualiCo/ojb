package org.apache.ojb.odmg;

import java.util.List;

import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.shared.TestClassA;
import org.apache.ojb.odmg.shared.TestClassAWithBProxy;
import org.apache.ojb.odmg.shared.TestClassB;
import org.apache.ojb.odmg.shared.TestClassBProxy;
import org.apache.ojb.odmg.shared.TestClassBProxyI;
import org.odmg.ODMGException;
import org.odmg.OQLQuery;
import org.odmg.Transaction;


public class OneToOneTest extends ODMGTestCase
{
	TestClassA m_a;
	TestClassB m_b;

    public static void main(String[] args)
    {
        String[] arr = {OneToOneTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**
     * Constructor for OneToOneTest.
     * @param arg0
     */
    public OneToOneTest(String arg0)
    {
        super(arg0);
    }

    protected void setUp() throws Exception
    {
    	super.setUp();

    	m_a = new TestClassA();
    	m_b = new TestClassB();

    	// init a
    	m_a.setValue1("A.One");
    	m_a.setValue2("B.Two");
    	m_a.setValue3(3);
    	m_a.setB(m_b);

    	// init b
    	m_b.setValue1("B.One");
    	m_b.setA(m_a);
    }

    public void testSave()
    {
        Transaction tx = odmg.newTransaction();
        tx.begin();
        database.makePersistent(m_a);
        database.makePersistent(m_b);
        tx.commit();

        assertTrue(m_a.getOid() != null);
        assertTrue(m_b.getOid() != null);
    }

    /**
     * This method tests the correct assignment of a foreign
     * key field in case that the referenced object is a
     * dynamic proxy
     */
    public void testFKAssignForProxy()
    {
        try
        {
            Transaction tx = odmg.newTransaction();
            tx.begin();
            //create a TestClassBProxy and persist it
            //so that when loading it again we will
            //get a dynamic proxy object
            TestClassBProxy b = new TestClassBProxy();
            database.makePersistent(b);
            tx.commit();

            //reload the object created in the previous step
            tx = odmg.newTransaction();
            tx.begin();
            OQLQuery query = odmg.newOQLQuery();
            query.create("select bproxies from " + TestClassBProxy.class.getName() +
            " where oid = $1");
            query.bind(b.getOid());
            List bList = (List) query.execute();
            assertEquals(1, bList.size());

            TestClassBProxyI bI = (TestClassBProxyI) bList.get(0);

            //bI should now be a dynamic proxy
            assertTrue(ProxyHelper.isProxy(bI));

            TestClassAWithBProxy a = new TestClassAWithBProxy();
            a.setBProxy(bI);
            tx.lock(a, Transaction.WRITE);
            tx.commit();

            //on commit the foreign key in "a" should have been set to
            //bOid
            String aBOid = a.getBoid();

            assertEquals("foreign key should match", b.getOid(), aBOid);

        } catch(ODMGException ex) {
          fail("ODMGException" + ex);
        }
    }
}
