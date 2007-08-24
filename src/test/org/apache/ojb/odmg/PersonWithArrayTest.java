/**
 * (C) 2003 ppi Media
 * User: om
 */

package org.apache.ojb.odmg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.shared.Person;
import org.apache.ojb.odmg.shared.PersonImpl;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.collections.ListUtils;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * class PersonWithArrayTest
 *
 * @author <a href="mailto:om@ppi.de">Oliver Matz</a>
 * @version $Id: PersonWithArrayTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class PersonWithArrayTest extends ODMGTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {PersonWithArrayTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /*
     * lock only the father, let OJB do the rest
     * delete father then children
     */
    public void testStoreDeleteThreePersons_1() throws Exception
    {
        String postfix = "_" + System.currentTimeMillis();
        String firstnameFather = "Father" + postfix;
        String firstnameChild_1 = "Child_One" + postfix;
        String firstnameChild_2 = "Child_Two" + postfix;
        String lastname = "testStoreThreePersons_1_" + postfix;

        Person father = createPerson(firstnameFather, lastname, null, null);
        Person child_1 = createPerson(firstnameChild_1, lastname, null, null);
        Person child_2 = createPerson(firstnameChild_2, lastname, null, null);

        Person[] children = new Person[]{child_1, child_2};
        father.setChildren(children);
        child_1.setFather(father);
        child_2.setFather(father);

        /*
         * lock only the father, let OJB do the rest
         */
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.lock(father, Transaction.WRITE);
        tx.commit();

        tx.begin();
        // make sure all objects are retrieved freshly in subsequent transactions
        ((TransactionImpl) tx).getBroker().clearCache();
        OQLQuery qry = odmg.newOQLQuery();
        qry.create("select a from " + PersonImpl.class.getName() + " where firstname=$1");
        qry.bind(firstnameFather);
        Collection result = (Collection) qry.execute();

        assertEquals("Exactly one element in result set", 1, result.size());
        Person returnedFather = (Person) result.iterator().next();
        // should retrieve new instance
        assertTrue("not same", returnedFather != father);
        Person[] returnedChildren = returnedFather.getChildren();
        assertNotNull(returnedChildren);
        assertEquals(2, returnedChildren.length);
        Person child = returnedChildren[0];
        Person lookupFather = child.getFather();
        assertNotNull(lookupFather);
        assertEquals(returnedFather.getFirstname(), lookupFather.getFirstname());
        // unfortunately, PersonImpl does not have a suitable equals method.
        assertEquals(
                "children's names are equal",
                Arrays.asList(getFirstNames(returnedChildren)),
                Arrays.asList(getFirstNames(children)));
        tx.commit();

        /*
         delete father then children
         fk-constraints?
         */
        tx.begin();
        database.deletePersistent(returnedFather);
        database.deletePersistent(returnedFather.getChildren()[0]);
        database.deletePersistent(returnedFather.getChildren()[1]);

        tx.commit();

        qry = odmg.newOQLQuery();
        qry.create("select a from " + PersonImpl.class.getName() + " where firstname=$1");
        qry.bind(firstnameFather);
        result = (Collection) qry.execute();
        assertEquals(0, result.size());

        qry = odmg.newOQLQuery();
        qry.create("select a from " + PersonImpl.class.getName() + " where firstname=$1");
        qry.bind(firstnameChild_1);
        result = (Collection) qry.execute();
        // System.out.println("child: "+ new ArrayList(result));
        assertEquals(0, result.size());
    }

    /*
     lock father then all childs
     delete children then father
     */
    public void testStoreDeleteThreePersons_2() throws Exception
    {
        String postfix = "_" + System.currentTimeMillis();
        String firstnameFather = "Father" + postfix;
        String firstnameChild_1 = "Child_One" + postfix;
        String firstnameChild_2 = "Child_Two" + postfix;
        String lastname = "testStoreThreePersons_2_" + postfix;

        Person father = createPerson(firstnameFather, lastname, null, null);
        Person child_1 = createPerson(firstnameChild_1, lastname, null, null);
        Person child_2 = createPerson(firstnameChild_2, lastname, null, null);

        Person[] children = new Person[]{child_1, child_2};
        father.setChildren(children);
        child_1.setFather(father);
        child_2.setFather(father);

        /*
         lock father then all childs
         */
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.lock(father, Transaction.WRITE);
        tx.lock(child_2, Transaction.WRITE);
        tx.lock(child_1, Transaction.WRITE);

        tx.commit();
        assertEquals(2, father.getChildren().length);

        tx.begin();
        // make sure all objects are retrieved freshly in subsequent transactions
        ((TransactionImpl) tx).getBroker().clearCache();
        OQLQuery qry = odmg.newOQLQuery();
        qry.create("select a from " + PersonImpl.class.getName() + " where firstname=$1");
        qry.bind(firstnameFather);
        Collection result = (Collection) qry.execute();

        assertEquals("Exactly one element in result set", 1, result.size());
        Person returnedFather = (Person) result.iterator().next();
        // should retrieve new instance
        assertTrue("not same", returnedFather != father);
        Person[] returnedChildren = returnedFather.getChildren();
        assertNotNull(returnedChildren);
        // check original instance again
        assertEquals(2, father.getChildren().length);
        assertEquals(2, returnedChildren.length);
        Person child = returnedChildren[0];
        Person lookupFather = child.getFather();
        assertNotNull(lookupFather);
        assertEquals(returnedFather.getFirstname(), lookupFather.getFirstname());
        // unfortunately, PersonImpl does not have a suitable equals method.
        assertEquals(
                "children's names are equal",
                Arrays.asList(getFirstNames(returnedChildren)),
                Arrays.asList(getFirstNames(children)));
        // System.out.println(Arrays.asList(getFirstNames(returnedChildren)));
        tx.commit();

        /*
         delete father only and disable cascading delete
         */
        tx.begin();
        /*
        by default cascading delete is enabled for 1:n relations, but we want to
        delete the father without deleting the dependent childs, so change runtime
        behavior of cascading delete
        */
        tx.setCascadingDelete(PersonImpl.class, "children", false);
        database.deletePersistent(returnedFather);
        tx.commit();

        qry = odmg.newOQLQuery();
        qry.create("select a from " + PersonImpl.class.getName() + " where firstname=$1");
        qry.bind(firstnameFather);
        result = (Collection) qry.execute();
        assertEquals("Exactly one element in result set", 0, result.size());

        qry = odmg.newOQLQuery();
        qry.create("select a from " + PersonImpl.class.getName() + " where lastname=$1");
        qry.bind(lastname);
        result = (Collection) qry.execute();
        assertEquals("Expected the two children objects of deleted main object", 2, result.size());
    }

    /**
     * Seems the locking order of objects is mandatory in this
     * case. This test fails
     */
    public void testStoreDeleteThreePersons_3() throws Exception
    {
        String postfix = "_" + System.currentTimeMillis();
        String firstnameFather = "Father" + postfix;
        String firstnameChild_1 = "Child_One" + postfix;
        String firstnameChild_2 = "Child_Two" + postfix;
        String lastname = "testStoreThreePersons_3" + postfix;

        Person father = createPerson(firstnameFather, lastname, null, null);
        Person child_1 = createPerson(firstnameChild_1, lastname, null, null);
        Person child_2 = createPerson(firstnameChild_2, lastname, null, null);

        Person[] children = new Person[]{child_1, child_2};
        father.setChildren(children);
        child_1.setFather(father);
        child_2.setFather(father);

        /*
         lock childs first, then lock father
         TODO: Does not pass - why? A defined lock
         order necessary?
         if this doesn't make sense remove the test
         */
        Transaction tx = odmg.newTransaction();
        tx.begin();
        tx.lock(child_1, Transaction.WRITE);
        tx.lock(child_2, Transaction.WRITE);
        tx.lock(father, Transaction.WRITE);
        tx.commit();

        tx = odmg.newTransaction();
        tx.begin();
        // make sure all objects are retrieved freshly in subsequent transactions
        ((TransactionImpl) tx).getBroker().clearCache();

        OQLQuery qry = odmg.newOQLQuery();
        qry.create("select a from " + PersonImpl.class.getName() + " where lastname=$1");
        qry.bind(lastname);
        Collection result = (Collection) qry.execute();
        assertEquals(3, new ArrayList(result).size());


        qry = odmg.newOQLQuery();
        qry.create("select a from " + PersonImpl.class.getName() + " where firstname=$1");
        qry.bind(firstnameFather);
        result = (Collection) qry.execute();
        tx.commit();
        assertEquals("Exactly one element in result set", 1, result.size());

        tx.begin();
        Person returnedFather = (Person) result.iterator().next();
        // should retrieve new instance, cause we clear the cache
        assertTrue("not same instance expected", returnedFather != father);
        Person[] returnedChildren = returnedFather.getChildren();
        assertNotNull(returnedChildren);
        assertEquals(2, returnedChildren.length);
        Person child = returnedChildren[0];
        Person lookupFather = child.getFather();
        assertNotNull(lookupFather);
        assertEquals(returnedFather.getFirstname(), lookupFather.getFirstname());
        // unfortunately, PersonImpl does not have a suitable equals method.
// comment out, because of child object order problem (it's not a bug, it's bad test writing)
//        assertEquals(
//                "children's names are equal",
//                Arrays.asList(getFirstNames(returnedChildren)),
//                Arrays.asList(getFirstNames(children)));
        // we expect the same names in both array, thus intersection result have to be '2'
        List list = ListUtils.intersection(Arrays.asList(getFirstNames(returnedChildren)), Arrays.asList(getFirstNames(children)));
        assertEquals(2, list.size());
        // System.out.println(Arrays.asList(getFirstNames(returnedChildren)));
        tx.commit();

        /*
         delete father then children
         fk-constraints?
         Delete calls in wrong order
         */
        tx.begin();
        database.deletePersistent(returnedFather);
        database.deletePersistent(returnedFather.getChildren()[0]);
        database.deletePersistent(returnedFather.getChildren()[1]);
        tx.commit();

        qry = odmg.newOQLQuery();
        qry.create("select a from " + PersonImpl.class.getName() + " where firstname=$1");
        qry.bind(firstnameFather);
        result = (Collection) qry.execute();
        assertEquals("Exactly one element in result set", 0, result.size());

        qry = odmg.newOQLQuery();
        qry.create("select a from " + PersonImpl.class.getName() + " where firstname=$1");
        qry.bind(firstnameChild_1);
        result = (Collection) qry.execute();
        // System.out.println("child: "+result.iterator().next());
        assertEquals("Exactly one element in result set", 0, result.size());
    }

    private Person createPerson(String firstname, String lastname, Person father, Person mother)
    {
        Person p = new PersonImpl();
        p.setFirstname(firstname);
        p.setLastname(lastname);
        p.setFather(father);
        p.setMother(mother);
        // p.setChildren(null);
        return p;
    }

    private static String[] getFirstNames(Person[] persons)
    {
        int length = persons == null ? 0 : persons.length;
        String[] ret = new String[length];
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = persons[i].getFirstname();
        }
        return ret;
    }
}
