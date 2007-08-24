package org.apache.ojb.odmg;


import java.util.List;

import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.shared.Person;
import org.apache.ojb.odmg.shared.PersonImpl;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/** Demo Application that shows basic concepts for Applications using the OJB ODMG
 * implementation as an transactional object server.
 */
public class ProxyTest extends ODMGTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {ProxyTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**TestThreadsNLocks state transition of modification states*/
    public void testLoading()
    {
        try
        {
            Person mum = new PersonImpl();
            mum.setFirstname("Macy");
            mum.setLastname("Gray");

            Person dad = new PersonImpl();
            dad.setFirstname("Paul");
            dad.setLastname("Gray");

            Person kevin = new PersonImpl();
            kevin.setFirstname("Kevin");
            kevin.setLastname("Gray");
            kevin.setMother(mum);
            kevin.setFather(dad);

            Transaction tx = odmg.newTransaction();
            tx.begin();
            tx.lock(kevin, Transaction.WRITE);
            tx.commit();

            tx = odmg.newTransaction();
            tx.begin();
            ((HasBroker) tx).getBroker().clearCache();
            OQLQuery qry = odmg.newOQLQuery();
            qry.create("select a from " + PersonImpl.class.getName() + " where firstname=$1");
            qry.bind("Kevin");

            List result = (List) qry.execute();
            Person boy = (Person) result.get(0);
            assertEquals(boy.getFirstname(), kevin.getFirstname());
            assertEquals(boy.getFather().getFirstname(), dad.getFirstname());
            assertEquals(boy.getMother().getFirstname(), mum.getFirstname());

            tx.commit();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            fail(t.getMessage());
        }
    }
}
