package org.apache.ojb.odmg;

import java.util.Collection;
import java.util.Iterator;

import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.shared.Article;
import org.apache.ojb.odmg.shared.Person;
import org.apache.ojb.odmg.shared.PersonImpl;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

public class ProjectionAttributeTest extends ODMGTestCase
{
    private int COUNT = 10;

    public static void main(String[] args)
    {
        String[] arr = {ProjectionAttributeTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public ProjectionAttributeTest(String name)
    {
        super(name);
    }

    private void createData(String id) throws Exception
    {
        Transaction tx = odmg.newTransaction();
        tx.begin();
        for (int i = 0; i < COUNT; i++)
        {
            Person aPerson = new PersonImpl();
            aPerson.setFirstname("firstname" + id +"_" + i);
            aPerson.setLastname("lastname" + id +"_" + i);
            database.makePersistent(aPerson);
        }
        tx.commit();
    }

    /**
     * test getting all (make sure basic operation is still functional)
     */
    public void testGetProjectionAttribute() throws Exception
    {
        String id = "_" + System.currentTimeMillis();
        createData(id);

        // 3. Get a list of some articles
        Transaction tx = odmg.newTransaction();
        tx.begin();

        OQLQuery query = odmg.newOQLQuery();
        String sql = "select aPerson.firstname, aPerson.lastname from " + Person.class.getName() + " where firstname like $1";
        query.create(sql);
        query.bind("%" + id + "%");

        Collection result = (Collection) query.execute();

        // Iterator over the restricted articles objects
        Iterator it = result.iterator();
        int i = 0;
        while (it.hasNext())
        {
            Object[] res = (Object[]) it.next();
            String firstname = (String) res[0];
            String lastname = (String) res[1];
            assertTrue(firstname.startsWith("firstname"));
            assertTrue(lastname.startsWith("lastname"));
            i++;
        }
        if (i < COUNT)
            fail("Should have found at least " + COUNT + " items");

        OQLQuery query1 = odmg.newOQLQuery();
        query1.create("select distinct anArticle.productGroup.groupId from " + Article.class.getName());
        Collection result1 = (Collection) query1.execute();
        for (it = result1.iterator(); it.hasNext(); )
        {
            it.next();
        }
        tx.commit();
    }

}
