package org.apache.ojb.otm;

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

import org.apache.ojb.broker.*;
import org.apache.ojb.odmg.shared.TestClassA;
import org.apache.ojb.odmg.shared.TestClassB;
import org.apache.ojb.otm.copy.MetadataObjectCopyStrategy;
import org.apache.ojb.otm.copy.ObjectCopyStrategy;
import org.apache.ojb.otm.copy.ReflectiveObjectCopyStrategy;
import org.apache.ojb.otm.copy.SerializeObjectCopyStrategy;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.lock.LockingException;
import org.apache.ojb.junit.OJBTestCase;

/**
 * Created by IntelliJ IDEA.
 * User: matthew.baird
 * Date: Jul 7, 2003
 * Time: 2:10:49 PM
 */
public class CopyTest extends OJBTestCase
{
	private static Class CLASS = CopyTest.class;
	private PersistenceBroker m_pb;
	private static final int ITERATIONS = 10000;
	private ObjectCopyStrategy m_mdcs = new MetadataObjectCopyStrategy();
	private ObjectCopyStrategy m_scs = new SerializeObjectCopyStrategy();
	private ObjectCopyStrategy m_rcs = new ReflectiveObjectCopyStrategy();
	private TestKit _kit;
	private OTMConnection _conn;
	private Zoo m_zoo;
	private TestClassA m_tca;
	private BidirectionalAssociationObjectA m_baoa;

	public CopyTest(String name)
	{
		super(name);
	}

	public void setUp() throws Exception
	{
		super.setUp();
        ojbChangeReferenceSetting(TestClassA.class, "b", true, true, true, false);
		ojbChangeReferenceSetting(TestClassB.class, "a", true, true, true, false);
        m_pb = PersistenceBrokerFactory.defaultPersistenceBroker();
		_kit = TestKit.getTestInstance();
		_conn = _kit.acquireConnection(PersistenceBrokerFactory.getDefaultKey());
	}

	public void tearDown() throws Exception
	{
		m_pb.close();
		_conn.close();
		_conn = null;
        super.tearDown();
    }

	public static void main(String[] args)
	{
		String[] arr = {CLASS.getName()};
		junit.textui.TestRunner.main(arr);
	}

	public void testMetadataCopy() throws LockingException
	{
		TestClassA tca = generateTestData();
		TestClassB tcb = tca.getB();
		internalTest(m_mdcs, tca, tcb);
	}

	public void testSerializedCopy() throws LockingException
	{
		TestClassA tca = generateTestData();
		TestClassB tcb = tca.getB();
		internalTest(m_scs, tca, tcb);
	}

	public void testReflectiveCopy() throws LockingException
	{
		TestClassA tca = generateTestData();
		TestClassB tcb = tca.getB();
		internalTest(m_rcs, tca, tcb);
	}

	private void internalTest(ObjectCopyStrategy strategy, TestClassA a, TestClassB b)
	{
		TestClassA copy = (TestClassA) strategy.copy(a, m_pb);
		assertTrue(a != copy);
		assertTrue(copy.getOid().equals("someoid"));
		assertTrue(copy.getValue1().equals("abc"));
		assertTrue(copy.getValue2().equals("123"));
		assertTrue(copy.getValue3() == 5);
		assertTrue(copy.getB() != b);
		assertTrue(copy.getB().getOid().equals("boid"));
		assertTrue(copy.getB().getValue1().equals("hi there"));
	}

	public void testMetadataCopy2() throws LockingException
	{
		Zoo zoo = generateZoo();
		internalTest2(m_mdcs, zoo);
	}

	public void testSerializeCopy2() throws LockingException
	{
		Zoo zoo = generateZoo();
		internalTest2(m_scs, zoo);
	}

	public void testReflectiveCopy2() throws LockingException
	{
		Zoo zoo = generateZoo();
		internalTest2(m_rcs, zoo);
	}

	/**
	 * tests for recursion handling
	 */
	public void testMetadataCopy3() throws LockingException
	{
		BidirectionalAssociationObjectA a = generateBidirectional();
		internalTest3(m_mdcs, a);
	}

	public void testSerializeCopy3() throws LockingException
	{
		BidirectionalAssociationObjectA a = generateBidirectional();
		internalTest3(m_scs, a);
	}

	public void testReflectiveCopy3() throws LockingException
	{
		BidirectionalAssociationObjectA a = generateBidirectional();
		internalTest3(m_rcs, a);
	}

	private void internalTest3(ObjectCopyStrategy strategy, BidirectionalAssociationObjectA a)
	{
		BidirectionalAssociationObjectA copy = (BidirectionalAssociationObjectA) strategy.copy(a, m_pb);
		assertTrue(a != copy);
		assertTrue(copy.getPk().equals("abc123"));
		assertTrue(copy.getRelatedB().getPk().equals("xyz987"));
	}

	private void internalTest2(ObjectCopyStrategy strategy, Zoo zoo)
	{
		Zoo copy = (Zoo) strategy.copy(zoo, m_pb);
		assertTrue(zoo != copy);
		assertTrue(zoo.getAnimals().size() == copy.getAnimals().size());
	}

	private BidirectionalAssociationObjectA generateBidirectional() throws LockingException
	{
		if (m_baoa != null)
		{
			return m_baoa;
		}
		else
		{
			Transaction tx = _kit.getTransaction(_conn);
			tx.begin();
			BidirectionalAssociationObjectA a = new BidirectionalAssociationObjectA();
			a.setPk("abc123");
			Identity oid = _conn.getIdentity(a);
			a = (BidirectionalAssociationObjectA) _conn.getObjectByIdentity(oid);
			if (a == null)
			{
				a = new BidirectionalAssociationObjectA();
				a.setPk("abc123");
				_conn.makePersistent(a);
				BidirectionalAssociationObjectB b = new BidirectionalAssociationObjectB();
				b.setPk("xyz987");
				_conn.makePersistent(b);
				a.setRelatedB(b);
				b.setRelatedA(a);
			}
			tx.commit();
			m_baoa = a;
			return m_baoa;
		}
	}


	private Zoo generateZoo() throws LockingException
	{
		if (m_zoo != null)
		{
			return m_zoo;
		}
		else
		{
			Transaction tx = _kit.getTransaction(_conn);
			tx.begin();
			Zoo zoo = new Zoo();
			zoo.setZooId(1234);
			Identity oid = _conn.getIdentity(zoo);
			zoo = (Zoo) _conn.getObjectByIdentity(oid);
			if (zoo == null)
			{
				zoo = new Zoo();
				zoo.setZooId(1234);
				_conn.makePersistent(zoo);
				Mammal mammal = new Mammal();
				mammal.setName("molly");
				mammal.setNumLegs(4);
				mammal.setAge(55);
				zoo.addAnimal(mammal);
				_conn.makePersistent(mammal);
				Reptile reptile = new Reptile();
				reptile.setColor("green");
				reptile.setName("hubert");
				reptile.setAge(51);
				zoo.addAnimal(reptile);
				_conn.makePersistent(reptile);
			}
			tx.commit();
			m_zoo = zoo;
			return m_zoo;
		}
	}

	public void testPerformance() throws LockingException
	{
		long start = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++)
		{
			TestClassA tca = generateTestData();
			TestClassB tcb = tca.getB();
			TestClassA copy = (TestClassA) m_scs.copy(tca, m_pb);
		}
		long stop = System.currentTimeMillis();
		System.out.println("testSerializedCopy took: " + (stop - start));
		start = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++)
		{
			TestClassA tca = generateTestData();
			TestClassB tcb = tca.getB();
			TestClassA copy = (TestClassA) m_mdcs.copy(tca, m_pb);
		}
		stop = System.currentTimeMillis();
		System.out.println("testMetadataCopy took: " + (stop - start));
		start = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++)
		{
			TestClassA tca = generateTestData();
			TestClassB tcb = tca.getB();
			TestClassA copy = (TestClassA) m_rcs.copy(tca, m_pb);
		}
		stop = System.currentTimeMillis();
		System.out.println("testReflectiveCopy took: " + (stop - start));
	}

	private TestClassA generateTestData() throws LockingException
	{
		if (m_tca != null)
		{
			return m_tca;
		}
		else
		{
			Transaction tx = _kit.getTransaction(_conn);
			tx.begin();
			TestClassA tca = new TestClassA();
			tca.setOid("someoid");
			Identity oid = _conn.getIdentity(tca);
			tca = (TestClassA) _conn.getObjectByIdentity(oid);
			if (tca == null)
			{
				tca = new TestClassA();
				tca.setOid("someoid");
				tca.setValue1("abc");
				tca.setValue2("123");
				tca.setValue3(5);
				_conn.makePersistent(tca);
				TestClassB tcb = new TestClassB();
				tcb.setOid("boid");
				tcb.setValue1("hi there");
				_conn.makePersistent(tcb);
				tca.setB(tcb);
			}
			tx.commit();
			m_tca = tca;
			return m_tca;
		}
	}
}
