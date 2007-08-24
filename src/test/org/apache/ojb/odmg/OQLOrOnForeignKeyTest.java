package org.apache.ojb.odmg;

/* Copyright 2002-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.shared.PersonImpl;
import org.apache.ojb.odmg.shared.TestClassA;
import org.apache.ojb.odmg.shared.TestClassB;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * @author Matthew.Baird
 *
 * Illustrates a problem with OJB SQL Generation:
 *
 * 1. OJB will generate the following SQL when items are mapped to the same table:
 *
 * SELECT A0.FATHER_ID,A0.MOTHER_ID,A0.LASTNAME,A0.FIRSTNAME,A0.ID
 * FROM FAMILY_MEMBER A0
 * INNER JOIN FAMILY_MEMBER A2 ON A0.FATHER_ID=A2.ID
 * INNER JOIN FAMILY_MEMBER A1 ON A0.MOTHER_ID=A1.ID
 * WHERE A1.ID =  ?  OR  (A2.ID =  ? )
 *
 * When it should generate:
 * SELECT A0.FATHER_ID,A0.MOTHER_ID,A0.LASTNAME,A0.FIRSTNAME,A0.ID
 * FROM FAMILY_MEMBER A0
 * WHERE A0.FATHER_ID =  ?  OR  (A0.MOTHER_ID =  ? )
 *
 * or:
 * SELECT A0.FATHER_ID,A0.MOTHER_ID,A0.LASTNAME,A0.FIRSTNAME,A0.ID
 * FROM FAMILY_MEMBER A0
 * LEFT OUTER JOIN FAMILY_MEMBER A1 ON A0.MOTHER_ID=A1.ID
 * LEFT OUTER JOIN FAMILY_MEMBER A2 ON A0.FATHER_ID=A2.ID
 * WHERE A1.ID = ?  OR  (A2.ID = ?)
 *
 */
public class OQLOrOnForeignKeyTest extends ODMGTestCase
{
	public static void main(String[] args)
    {
        String[] arr = {OQLOrOnForeignKeyTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public OQLOrOnForeignKeyTest(String name)
	{
		super(name);
	}

    private void deleteData(Class target)
			throws Exception
	{
		Transaction tx = odmg.newTransaction();
		tx.begin();
		OQLQuery query = odmg.newOQLQuery();
		query.create("select allStuff from " + target.getName());
		Collection allTargets = (Collection) query.execute();
		Iterator it = allTargets.iterator();
		while (it.hasNext())
		{
			database.deletePersistent(it.next());
		}
		tx.commit();
	}

    /**
     * test joins on same table
     *
     * @throws Exception
     */
	public void testOrReferenceOnSameTable() throws Exception
	{
		deleteData(PersonImpl.class);

		PersonImpl jimmy = new PersonImpl();
		PersonImpl joe = new PersonImpl();
		PersonImpl father = new PersonImpl();
		PersonImpl mother = new PersonImpl();
        OQLQuery query;
        List persons;

		mother.setFirstname("mom");
		father.setFirstname("dad");

		jimmy.setMother(mother);
		jimmy.setFirstname("jimmy");

		joe.setFather(father);
		joe.setFirstname("joe");

		Transaction tx = odmg.newTransaction();
		tx.begin();
		database.makePersistent(father);
		database.makePersistent(mother);
		database.makePersistent(jimmy);
		database.makePersistent(joe);
		tx.commit();

        // read using id
		tx = odmg.newTransaction();
		tx.begin();
		query = odmg.newOQLQuery();
		query.create("select person from " + PersonImpl.class.getName() +
					 " where (mother.id=$1 or father.id=$2)");
		query.bind(new Integer(mother.getId()));
		query.bind(new Integer(father.getId()));
		persons = (List) query.execute();
        assertEquals(2, persons.size());
		tx.commit();

        // read using firstname
        tx = odmg.newTransaction();
        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select person from " + PersonImpl.class.getName() +
                     " where (mother.firstname=$1 or father.firstname=$2)");
        query.bind("mom");
        query.bind("dad");
        persons = (List) query.execute();
        assertEquals(2, persons.size());
        tx.commit();
	}

	public void testOrReferenceOnDifferentTables() throws Exception
	{
        deleteData(TestClassA.class);
        deleteData(TestClassB.class);

		TestClassA a1 = new TestClassA();
		TestClassA a2 = new TestClassA();

		TestClassB b1 = new TestClassB();
		TestClassB b2 = new TestClassB();
		a1.setB(b1);
		a2.setB(b2);

		Transaction tx = odmg.newTransaction();
		tx.begin();
		database.makePersistent(a1);
		database.makePersistent(a2);
		database.makePersistent(b1);
		database.makePersistent(b2);
		tx.commit();
		tx = odmg.newTransaction();
		tx.begin();
		// get the right values
		OQLQuery query = odmg.newOQLQuery();
		query.create("select a from " + TestClassA.class.getName());
		List As = (List) query.execute();
		Iterator asIterator = As.iterator();
		TestClassA temp = null;

		temp = (TestClassA) asIterator.next();
		String bID1 = temp.getB().getOid();
		temp = (TestClassA) asIterator.next();
		String bID2 = temp.getB().getOid();

		query = odmg.newOQLQuery();
		query.create("select a from " + TestClassA.class.getName() +
					 " where (b.oid=$1 or b.oid=$2)");
		query.bind(bID1);
		query.bind(bID2);
		As = (List) query.execute();
		assertTrue(As.size() == 2);
		tx.commit();
	}
}
