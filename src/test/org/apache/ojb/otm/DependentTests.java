package org.apache.ojb.otm;

import java.util.Iterator;
import junit.framework.TestCase;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.lock.LockingException;

public class DependentTests extends TestCase
{
    private static Class CLASS = DependentTests.class;
    private TestKit _kit;
    private OTMConnection _conn;

    public void setUp() throws LockingException
    {
        _kit = TestKit.getTestInstance();
        _conn = _kit.acquireConnection(PersistenceBrokerFactory.getDefaultKey());
    }

    public void tearDown() throws LockingException
    {
        _conn.close();
        _conn = null;
    }

    public static void main(String[] args)
    {
        String[] arr = {CLASS.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testDependent() throws Exception
    {
        Person person = new Person("Ostap", "Bender");
        Address address1 = new Address("Ukraine", "Odessa", "Deribasovskaya");
        Address address2 = new Address("Ukraine", "Odessa", "Malaya Arnautskaya");
        Address address3 = new Address("Brasil", "Rio de Janeiro", "Rua Professor Azevedo Marques");
        Criteria emptyCriteria = new Criteria();
        Query q;
        Iterator it;

        Transaction tx = _kit.getTransaction(_conn);
        tx.begin();
        // prepare tables for the test - empty them
        q = QueryFactory.newQuery(AddressDesc.class, emptyCriteria);
        for (it = _conn.getIteratorByQuery(q); it.hasNext(); ) {
            _conn.deletePersistent(it.next());
        }
        q = QueryFactory.newQuery(Person.class, emptyCriteria);
        for (it = _conn.getIteratorByQuery(q); it.hasNext(); ) {
            _conn.deletePersistent(it.next());
        }
        q = QueryFactory.newQuery(Address.class, emptyCriteria);
        for (it = _conn.getIteratorByQuery(q); it.hasNext(); ) {
            _conn.deletePersistent(it.next());
        }
        tx.commit();

        person.setMainAddress(address1);
        person.addOtherAddress("work", address2);
        person.addOtherAddress("dream", address3);

        tx = _kit.getTransaction(_conn);
        tx.begin();
        // Cascade create
        _conn.makePersistent(person);
        tx.commit();

        Identity oid = _conn.getIdentity(person);

        _conn.invalidateAll();
        tx = _kit.getTransaction(_conn);
        tx.begin();
        person = (Person) _conn.getObjectByIdentity(oid);
        assertTrue("person exists", (person != null));
        assertTrue("main Address exists", (person.getMainAddress() != null));
        assertEquals("main Address is correct", address1.getStreet(), person.getMainAddress().getStreet());
        assertEquals("two other Addresses", 2, person.getOtherAddresses().size());
        AddressDesc desc1 = (AddressDesc) person.getOtherAddresses().get(0);
        assertEquals("1st other Address has correct description", "work", desc1.getDesc());
        assertEquals("1st other Address is correct", address2.getStreet(), desc1.getAddress().getStreet());
        AddressDesc desc2 = (AddressDesc) person.getOtherAddresses().get(1);
        assertEquals("2nd other Address has correct description", "dream", desc2.getDesc());
        assertEquals("2nd other Address is correct", address3.getStreet(), desc2.getAddress().getStreet());

        // Delete dependent
        person.setMainAddress(null);
        person.getOtherAddresses().remove(1);
        tx.commit();

        _conn.invalidateAll();
        tx = _kit.getTransaction(_conn);
        tx.begin();
        person = (Person) _conn.getObjectByIdentity(oid);
        assertTrue("main Address doesn't exist", (person.getMainAddress() == null));
        assertEquals("one other Address", 1, person.getOtherAddresses().size());
        desc2 = (AddressDesc) person.getOtherAddresses().get(0);
        assertEquals("the other Address has correct description", "work", desc1.getDesc());
        assertEquals("the other Address is correct", address2.getStreet(), desc1.getAddress().getStreet());

        // Create dependent
        person.setMainAddress(address1);
        person.addOtherAddress("dream", address3);
        tx.commit();

        _conn.invalidateAll();
        tx = _kit.getTransaction(_conn);
        tx.begin();
        person = (Person) _conn.getObjectByIdentity(oid);
        assertTrue("main Address exists", (person.getMainAddress() != null));
        assertEquals("main Address is correct", address1.getStreet(), person.getMainAddress().getStreet());
        assertEquals("two other Addresses", 2, person.getOtherAddresses().size());
        desc1 = (AddressDesc) person.getOtherAddresses().get(0);
        assertEquals("1st other Address has correct description", "work", desc1.getDesc());
        assertEquals("1st other Address is correct", address2.getStreet(), desc1.getAddress().getStreet());
        desc2 = (AddressDesc) person.getOtherAddresses().get(1);
        assertEquals("2nd other Address has correct description", "dream", desc2.getDesc());
        assertEquals("2nd other Address is correct", address3.getStreet(), desc2.getAddress().getStreet());

        // Cascade delete
        _conn.deletePersistent(person);
        tx.commit();

        _conn.invalidateAll();
        tx = _kit.getTransaction(_conn);
        tx.begin();
        person = (Person) _conn.getObjectByIdentity(oid);
        assertTrue("person doesn't exist", (person == null));
        q = QueryFactory.newQuery(AddressDesc.class, emptyCriteria);
        it = _conn.getIteratorByQuery(q);
        assertTrue("address descriptions don't exist", !it.hasNext());
        q = QueryFactory.newQuery(Address.class, emptyCriteria);
        it = _conn.getIteratorByQuery(q);
        assertTrue("addresses don't exist", !it.hasNext());
        tx.commit();
    }

    public void testDependent2() throws Exception
    {
        AbstractPerson person = new LegalPerson();
        Debitor debitor = new Debitor();
        Address2 address = new Address2();
        Criteria emptyCriteria = new Criteria();
        Query q;
        Iterator it;

        Transaction tx = _kit.getTransaction(_conn);
        tx.begin();
        // prepare tables for the test - empty them
        q = QueryFactory.newQuery(Debitor.class, emptyCriteria);
        for (it = _conn.getIteratorByQuery(q); it.hasNext(); ) {
            _conn.deletePersistent(it.next());
        }
        q = QueryFactory.newQuery(AbstractPerson.class, emptyCriteria);
        for (it = _conn.getIteratorByQuery(q); it.hasNext(); ) {
            _conn.deletePersistent(it.next());
        }
        q = QueryFactory.newQuery(Address2.class, emptyCriteria);
        for (it = _conn.getIteratorByQuery(q); it.hasNext(); ) {
            _conn.deletePersistent(it.next());
        }
        tx.commit();

        person.getAddresses().add(address);
        debitor.setAbstractPerson(person);

        tx = _kit.getTransaction(_conn);
        tx.begin();
        // Cascade create
        _conn.makePersistent(debitor);
        tx.commit();

        Identity debitorOid = _conn.getIdentity(debitor);
        Identity personOid = _conn.getIdentity(person);
        int addrId = address.getId();

        _conn.invalidateAll();
        tx = _kit.getTransaction(_conn);
        tx.begin();
        debitor = (Debitor) _conn.getObjectByIdentity(debitorOid);
        assertNotNull("debitor does not exist", debitor);
        person = debitor.getAbstractPerson();
        assertNotNull("person does not exist", person);
        assertTrue("person has not the expected type", (person instanceof LegalPerson));
        assertEquals("address does not exist", 1, person.getAddresses().size());
        address = (Address2) person.getAddresses().iterator().next();
        assertEquals("addressid is not correct", addrId, address.getId());

        // Delete dependent
        person.getAddresses().clear();
        tx.commit();

        _conn.invalidateAll();
        tx = _kit.getTransaction(_conn);
        tx.begin();
        debitor = (Debitor) _conn.getObjectByIdentity(debitorOid);
        person = (LegalPerson) debitor.getAbstractPerson();
        assertEquals("address was not deleted", person.getAddresses().size(), 0);

        // Create dependent
        person.getAddresses().add(address);
        tx.commit();

        _conn.invalidateAll();
        tx = _kit.getTransaction(_conn);
        tx.begin();
        debitor = (Debitor) _conn.getObjectByIdentity(debitorOid);
        person = (LegalPerson) debitor.getAbstractPerson();
        assertEquals("address does not exist", person.getAddresses().size(), 1);
        tx.commit();

        // Change dependent reference, should delete old dependant
        person = new NaturalPerson();
        person.setName("before");
        debitor.setAbstractPerson(person);

        _conn.invalidateAll();
        tx = _kit.getTransaction(_conn);
        tx.begin();
        _conn.makePersistent(debitor);
        tx.commit();

        _conn.invalidateAll();
        tx = _kit.getTransaction(_conn);
        tx.begin();
        assertTrue("old person has not been deleted", (_conn.getObjectByIdentity(personOid) == null));
        q = QueryFactory.newQuery(Address2.class, emptyCriteria);
        it = _conn.getIteratorByQuery(q);
        assertTrue("old address has not been deleted", !it.hasNext());
        person = debitor.getAbstractPerson();
        assertTrue("new person has unexpected type", (person instanceof NaturalPerson));
        assertTrue("person does not have correct name", "before".equals(person.getName()));
        tx.commit();

        person.setName("after");
        assertTrue("name of person was not saved", "after".equals(debitor.getAbstractPerson().getName()));

        _conn.invalidateAll();
        tx = _kit.getTransaction(_conn);
        tx.begin();
        _conn.makePersistent(debitor);
        tx.commit();
        assertTrue("name of person was not saved: " + debitor.getAbstractPerson().getName(),
                   "after".equals(debitor.getAbstractPerson().getName()));

        _conn.invalidateAll();
        tx = _kit.getTransaction(_conn);
        tx.begin();
        debitor = (Debitor) _conn.getObjectByIdentity(debitorOid);
        person = debitor.getAbstractPerson();
        assertTrue("name of person was not saved: " + debitor.getAbstractPerson().getName(),
                   "after".equals(debitor.getAbstractPerson().getName()));
        // Cascade delete
        _conn.deletePersistent(debitor);
        tx.commit();

        _conn.invalidateAll();
        tx = _kit.getTransaction(_conn);
        tx.begin();
        debitor = (Debitor) _conn.getObjectByIdentity(debitorOid);
        assertNull("debitor still exists", debitor);
        q = QueryFactory.newQuery(AbstractPerson.class, emptyCriteria);
        it = _conn.getIteratorByQuery(q);
        assertTrue("persons still exist", !it.hasNext());
        tx.commit();
    }
}
