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

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import org.apache.ojb.broker.Article;
import org.apache.ojb.broker.Contract;
import org.apache.ojb.broker.Effectiveness;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PBFactoryException;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.ProductGroup;
import org.apache.ojb.broker.RelatedToContract;
import org.apache.ojb.broker.Version;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.odmg.shared.TestClassA;
import org.apache.ojb.odmg.shared.TestClassB;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.core.TransactionException;
import org.apache.ojb.otm.lock.LockType;
import org.apache.ojb.otm.lock.LockingException;
import org.apache.ojb.junit.OJBTestCase;

/**
 * User: Matthew Baird
 * Date: Jun 21, 2003
 * Time: 3:59:08 PM
 * @version $Id: SwizzleTests.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class SwizzleTests extends OJBTestCase
{
    private static Class CLASS = SwizzleTests.class;
    private TestKit _kit;
    private OTMConnection _conn;
    private static final int COUNT = 1;
    private static final long TIME = System.currentTimeMillis();

    public void setUp() throws Exception
    {
        super.setUp();
        ojbChangeReferenceSetting(TestClassA.class, "b", true, true, true, false);
		ojbChangeReferenceSetting(TestClassB.class, "a", true, true, true, false);
		ojbChangeReferenceSetting(ProductGroup.class, "allArticlesInGroup", true, true, true, false);
		ojbChangeReferenceSetting(Article.class, "productGroup", true, true, true, false);
        _kit = TestKit.getTestInstance();
        _conn = _kit.acquireConnection(PersistenceBrokerFactory.getDefaultKey());
    }

    public void tearDown() throws Exception
    {
        _conn.close();
        _conn = null;
        super.tearDown();
    }

    public static void main(String[] args)
    {
        String[] arr = {CLASS.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testSwizzle() throws TransactionException, LockingException, PBFactoryException, PersistenceBrokerException
    {
        deleteAllData();
        createTestData();
        /**
        * first get the contract object.
        */
        PersistenceBrokerFactory.defaultPersistenceBroker().clearCache();
        Transaction tx = _kit.getTransaction(_conn);
        tx.begin();
        Criteria crit = new Criteria();
        crit.addEqualTo("pk", "C" + TIME);
        Query q = QueryFactory.newQuery(Contract.class, crit);
        Iterator it = _conn.getIteratorByQuery(q, LockType.WRITE_LOCK);
        Object retval = null;
        RelatedToContract r2c = new RelatedToContract();
        r2c.setPk("R2C" + TIME);
        r2c.setRelatedValue1("matt");
        r2c.setRelatedValue2(34);
        r2c.setRelatedValue3(new Timestamp(TIME));
        _conn.makePersistent(r2c);
        while (it.hasNext())
        {
            retval = it.next();
            ((Contract) retval).setRelatedToContract(r2c);
        }
        tx.commit();
        r2c = null;
        tx = _kit.getTransaction(_conn);
        tx.begin();
        crit = new Criteria();
        crit.addEqualTo("pk", "E" + TIME);
        q = QueryFactory.newQuery(Effectiveness.class, crit);
        it = _conn.getIteratorByQuery(q);
        retval = null;
        while (it.hasNext())
        {
            retval = it.next();
        }
        tx.commit();
        assertTrue("contract object should have a RelatedToContract instance attached", ((Effectiveness) retval).getVersion().getContract().getRelatedToContract() != null);
    }

	 public void testSwizzle3() throws TransactionException, LockingException, PBFactoryException, PersistenceBrokerException
    {
        clearTestData();
        TestClassA a = generateTestData();
        Transaction tx = _kit.getTransaction(_conn);
        tx.begin();
        _conn.makePersistent(a.getB());
        _conn.makePersistent(a);
		TestClassB b = a.getB();
        tx.commit();
        /**
        * clear to start test
        */
        _conn.invalidateAll();
        tx = _kit.getTransaction(_conn);
        tx.begin();
        /**
		 * load B
		 */
		Identity oidb = _conn.getIdentity(b);
        TestClassB b1 = (TestClassB) _conn.getObjectByIdentity(oidb);
        assertTrue(b1 != null);
		/**
		 * load A
 		 */
		Identity oida = _conn.getIdentity(a);
		TestClassA a1 = (TestClassA) _conn.getObjectByIdentity(oida);

		/**
		 * B, as navigated from A, should be the same as B gotten directly.
		 */
		assertTrue(a1.getB().equals(b1));
        tx.commit();

		/**
		 * clear
		 */
        clearTestData();
    }

    private void createTestData() throws TransactionException, LockingException
    {
        for (int i = 0; i < COUNT; i++)
        {
            Transaction tx = _kit.getTransaction(_conn);
            tx.begin();
            Contract contract = new Contract();
            contract.setPk("C" + TIME);
            contract.setContractValue1("contractvalue1");
            contract.setContractValue2(1);
            contract.setContractValue3("contractvalue3");
            contract.setContractValue4(new Timestamp(TIME));
            _conn.makePersistent(contract);
            tx.commit();
            tx = _kit.getTransaction(_conn);
            tx.begin();
            Version version = new Version();
            version.setPk("V" + TIME);
            version.setVersionValue1("versionvalue1");
            version.setVersionValue2(1);
            version.setVersionValue3(new Timestamp(TIME));
            version.setContract(contract);
            _conn.makePersistent(version);
            tx.commit();
            tx = _kit.getTransaction(_conn);
            tx.begin();
            Effectiveness eff = new Effectiveness();
            eff.setPk("E" + TIME);
            eff.setEffValue1("effvalue1");
            eff.setEffValue2(1);
            eff.setEffValue3(new Timestamp(TIME));
            eff.setVersion(version);
            _conn.makePersistent(eff);
            tx.commit();
        }
    }

    public void deleteAllData() throws LockingException
    {
        Criteria crit = new Criteria();
        Query q;
        Iterator iter;
        /**
        * delete effectiveness first
        */
        Transaction tx = _kit.getTransaction(_conn);
        tx.begin();
        q = QueryFactory.newQuery(Effectiveness.class, crit);
        iter = _conn.getIteratorByQuery(q);
        while (iter.hasNext())
        {
            _conn.deletePersistent(iter.next());
        }
        tx.commit();
        /**
        * then version
        */
        tx = _kit.getTransaction(_conn);
        tx.begin();
        q = QueryFactory.newQuery(Version.class, crit);
        iter = _conn.getIteratorByQuery(q);
        while (iter.hasNext())
        {
            _conn.deletePersistent(iter.next());
        }
        tx.commit();
        /**
        * the contract
        */
        tx = _kit.getTransaction(_conn);
        tx.begin();
        q = QueryFactory.newQuery(Contract.class, crit);
        iter = _conn.getIteratorByQuery(q);
        while (iter.hasNext())
        {
            _conn.deletePersistent(iter.next());
        }
        tx.commit();
    }

    public void testSwizzle2() throws TransactionException, LockingException, PBFactoryException, PersistenceBrokerException
    {
        clearTestData();
        TestClassA a = generateTestData();
        Transaction tx = _kit.getTransaction(_conn);
        tx.begin();
        _conn.makePersistent(a.getB());
        _conn.makePersistent(a);
        tx.commit();
        /**
        * clear to start test
        */
        _conn.invalidateAll();
        /**
        * get A to make it and the related B in cache
        */
        tx = _kit.getTransaction(_conn);
        tx.begin();
        Identity oid = _conn.getIdentity(a);
        TestClassA a1 = (TestClassA) _conn.getObjectByIdentity(oid);
        assertTrue(a1.getB() != null);
        assertTrue(a1.getB().getValue1().equals("hi there"));
        /**
        * everything is good, update b
        */
        tx.commit();

        /**
        * now get B and update it, do NOT get it by traversing A
        */
        tx = _kit.getTransaction(_conn);
        tx.begin();
        Identity boid = _conn.getIdentity(a.getB());
        TestClassB b1 = (TestClassB) _conn.getObjectByIdentity(boid);
        assertTrue(b1 != null);
        assertTrue(b1.getValue1().equals("hi there"));
        /**
        * everything is good, update b
        */
        _conn.lockForWrite(b1);
        b1.setValue1("goodbye there");
        tx.commit();
        /**
        * make sure b was updated
        */
        tx = _kit.getTransaction(_conn);
        tx.begin();
        boid = _conn.getIdentity(a.getB());
        b1 = (TestClassB) _conn.getObjectByIdentity(boid);
        assertTrue(b1 != null);
        assertTrue(b1.getValue1().equals("goodbye there"));
        tx.commit();

        /**
        * now get A again and make sure the related B is updated to reflect
        * the new value.
        */
        tx = _kit.getTransaction(_conn);
        tx.begin();
        TestClassA a2 = (TestClassA) _conn.getObjectByIdentity(oid);
        assertTrue(a2.getB() != null);
        assertTrue(a2.getB().getValue1().equals("goodbye there"));
        tx.commit();
        clearTestData();
    }

    public void testSwizzleNto1() throws Exception
    {
        clearTestData();
        TestClassA a = generateTestData();
        TestClassB b2 = generateAnotherB();
        Transaction tx = _kit.getTransaction(_conn);
        tx.begin();
        _conn.makePersistent(a.getB());
        _conn.makePersistent(a);
        tx.commit();
        /**
         * change B
         */
        tx = _kit.getTransaction(_conn);
        tx.begin();
        Identity oid = _conn.getIdentity(a);
        TestClassA a1 = (TestClassA) _conn.getObjectByIdentity(oid);
        _conn.makePersistent(b2);
        a1.setB(b2);
        tx.commit();

        tx = _kit.getTransaction(_conn);
        tx.begin();
        a = (TestClassA) _conn.getObjectByIdentity(oid);
        assertTrue(a.getB() != null);
        assertTrue(a.getB().getValue1().equals("value2"));
        a.setB(null);
        tx.commit();

        tx = _kit.getTransaction(_conn);
        tx.begin();
        a = (TestClassA) _conn.getObjectByIdentity(oid);
        assertTrue(a.getB() == null);
        tx.commit();
    }

    public void testSwizzle1toN() throws Exception
    {
        if (ojbSkipKnownIssueProblem("OTM-layer has caching issues"))
        {
            return;
        }
        clearTestData();
        Transaction tx = _kit.getTransaction(_conn);
        tx.begin();
        ProductGroup pg = new ProductGroup();
        pg.setId(new Integer(77777));
        pg.setName("1");
        _conn.makePersistent(pg);
        Article article = Article.createInstance();
        article.setArticleId(new Integer(77777));
        article.setStock(333);
        pg.add(article);
        article.setProductGroup(pg);
        _conn.makePersistent(article);
        Identity pgOid = _conn.getIdentity(pg);
        tx.commit();

        tx = _kit.getTransaction(_conn);
        tx.begin();
        pg = (ProductGroup) _conn.getObjectByIdentity(pgOid);
        pg.getAllArticlesInGroup().clear();
        tx.commit();

        tx = _kit.getTransaction(_conn);
        tx.begin();
        pg = (ProductGroup) _conn.getObjectByIdentity(pgOid);
        assertEquals("should be equal", 0, pg.getAllArticlesInGroup().size());
        tx.commit();

        tx = _kit.getTransaction(_conn);
        tx.begin();
        pg = (ProductGroup) _conn.getObjectByIdentity(pgOid);
        pg.getAllArticlesInGroup().add(article);
        tx.commit();

        tx = _kit.getTransaction(_conn);
        tx.begin();
        pg = (ProductGroup) _conn.getObjectByIdentity(pgOid);
        assertEquals("should be equal", 1, pg.getAllArticlesInGroup().size());
        tx.commit();
        clearTestData();
    }

	 public void testSwizzle4() throws TransactionException, LockingException, PBFactoryException, PersistenceBrokerException
    {
        clearTestData();
        TestClassA a = generateTestData();
		TestClassB b = a.getB();
        Transaction tx = _kit.getTransaction(_conn);

        tx.begin();
        _conn.makePersistent(b);
        _conn.makePersistent(a);
        b.setA(a);
        tx.commit();
        /**
        * clear to start test
        */
        _conn.invalidateAll();
        tx = _kit.getTransaction(_conn);
        tx.begin();
        /**
		 * load B
		 */
		Identity oidb = _conn.getIdentity(b);
        TestClassB b1 = (TestClassB) _conn.getObjectByIdentity(oidb);
		/**
		 * load A
 		 */
		Identity oida = _conn.getIdentity(a);
		TestClassA a1 = (TestClassA) _conn.getObjectByIdentity(oida);
		assertTrue(a1 != null);
		assertTrue(a1.getB().equals(b1));
		assertTrue(b1.getA().equals(a1));
        /**
		 * update B
		 */
        a.setValue1("a");
        _conn.makePersistent(a);

		/**
		 * B, as navigated from A, should be the same as B gotten directly.
		 */
		assertTrue(a1.getValue1().equals(a.getValue1()));
        tx.commit();

		/**
		 * clear
		 */
        clearTestData();
    }

    /**
     * Cache data must be independent of any objects available to used,
     * otherwise modification of user objects outside transaction will
     * damage cache data
     */
    public void testCacheIndependence() throws Throwable {
        Transaction tx = null;
        Collection addresses = this.getAddresses();
        deleteAddresses(addresses);
        Identity oid;
        Address address = new Address("oldCountry", "oldCity", "oldStreet");

        try {
            tx = _kit.getTransaction(_conn);
            tx.begin();
            _conn.makePersistent(address);
            oid = _conn.getIdentity(address);
            tx.commit();

            address.setStreet("newStreet");

            tx = _kit.getTransaction(_conn);
            tx.begin();
            address = (Address) _conn.getObjectByIdentity(oid);
            assertEquals("Cache was damaged.", "oldStreet", address.getStreet());
            tx.commit();

            address.setStreet("newStreet");

            tx = _kit.getTransaction(_conn);
            tx.begin();
            address = (Address) _conn.getObjectByIdentity(oid, LockType.WRITE_LOCK);
            assertEquals("Cache was damaged.", "oldStreet", address.getStreet());
            tx.commit();

            address.setStreet("newStreet");

            tx = _kit.getTransaction(_conn);
            tx.begin();
            address = (Address) _conn.getObjectByIdentity(oid);
            assertEquals("Cache was damaged.", "oldStreet", address.getStreet());
            tx.commit();
        } catch (Throwable e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        }
    }

    public void testSomethingSimple() throws Throwable {
        Collection addresses = this.getAddresses();
		addresses = deleteAddresses(addresses);

        addresses.add(new Address("oldCountry", "oldCity", "oldStreet"));

        addresses = this.updateAddresses(addresses);

        Iterator iter = addresses.iterator();
        while (iter.hasNext()) {
            Address address = (Address)iter.next();
            address.setStreet("newStreet");
        }
        addresses = this.updateAddresses(addresses);
        addresses = this.getAddresses();
        assertEquals("Collection of addresses must be 1. ", 1, addresses.size());
        iter = addresses.iterator();
        while (iter.hasNext()) {
            Address address = (Address)iter.next();
            assertEquals("New street not set.", "newStreet",
                                                address.getStreet());
        }
    }

    private Collection getAddresses() throws Throwable {
        Transaction tx = null;
        Collection addresses;
        try {
            tx = _kit.getTransaction(_conn);
            tx.begin();
            _conn.invalidateAll();
            Query q = QueryFactory.newQuery(Address.class, (Criteria)null);
            addresses = _conn.getCollectionByQuery(q);
            tx.commit();
        } catch (Throwable e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        }
        return addresses;
    }

    private Collection updateAddresses(Collection newAddresses)
            throws Throwable {
        Transaction tx = null;
        Collection oldAddresses;
        try {
            tx = _kit.getTransaction(_conn);
            tx.begin();

            Query q = QueryFactory.newQuery(Address.class, (Criteria)null);
            oldAddresses = _conn.getCollectionByQuery(q);

            Iterator oldAddressesIterator = oldAddresses.iterator();
            while (oldAddressesIterator.hasNext()) {
                Address oldAddress = (Address)oldAddressesIterator.next();
                if (!newAddresses.contains(oldAddress)) {
                    _conn.deletePersistent(oldAddress);
                }
            }

            Iterator newAddressesIterator = newAddresses.iterator();
            while (newAddressesIterator.hasNext()) {
                Address newAddress = (Address)newAddressesIterator.next();
                _conn.makePersistent(newAddress);
            }
            tx.commit();
        } catch (Throwable e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        }
        return newAddresses;
    }

    private Collection deleteAddresses(Collection oldAddresses)
            throws Throwable {
        Transaction tx = null;
        try {
            tx = _kit.getTransaction(_conn);
            tx.begin();

            Iterator oldAddressesIterator = oldAddresses.iterator();
            while (oldAddressesIterator.hasNext()) {
                Address oldAddress = (Address)oldAddressesIterator.next();
                _conn.deletePersistent(oldAddress);
                oldAddressesIterator.remove();
            }
            tx.commit();
        } catch (Throwable e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        }
        return oldAddresses;
    }

    private void clearTestData() throws LockingException
    {
        TestClassA a = generateTestData();
        TestClassB b2 = generateAnotherB();
        Transaction tx = _kit.getTransaction(_conn);
        tx.begin();
        Identity oid = _conn.getIdentity(a);
        Identity oidb = _conn.getIdentity(a.getB());
        Identity oidb2 = _conn.getIdentity(b2);
        TestClassA a1 = (TestClassA) _conn.getObjectByIdentity(oid);
        if (a1 != null)
        {
            _conn.deletePersistent(a1);
        }
        TestClassB b1 = (TestClassB) _conn.getObjectByIdentity(oidb);
        if (b1 != null)
        {
            _conn.deletePersistent(b1);
        }
        b2 = (TestClassB) _conn.getObjectByIdentity(oidb2);
        if (b2 != null)
        {
            _conn.deletePersistent(b2);
        }

        Article article = Article.createInstance();
        article.setArticleId(new Integer(77777));
        ProductGroup pg = new ProductGroup();
        pg.setId(new Integer(77777));
        Identity oidArt = _conn.getIdentity(article);
        Identity oidPG = _conn.getIdentity(pg);
        article = (Article) _conn.getObjectByIdentity(oidArt);
        if (article != null)
        {
            _conn.deletePersistent(article);
        }
        pg = (ProductGroup) _conn.getObjectByIdentity(oidPG);
        if (pg != null)
        {
            _conn.deletePersistent(pg);
        }
        tx.commit();
    }

    private TestClassA generateTestData()
    {
        TestClassA tca = new TestClassA();
        tca.setOid("someoid");
        tca.setValue1("abc");
        tca.setValue2("123");
        tca.setValue3(5);
        TestClassB tcb = new TestClassB();
        tcb.setOid("boid");
        tcb.setValue1("hi there");
        tca.setB(tcb);
        return tca;
    }

    private TestClassB generateAnotherB()
    {
        TestClassB tcb = new TestClassB();
        tcb.setOid("boid2");
        tcb.setValue1("value2");
        return tcb;
    }
}
