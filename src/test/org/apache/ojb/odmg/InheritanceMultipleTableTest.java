package org.apache.ojb.odmg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.*;
import org.apache.ojb.junit.ODMGTestCase;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * Test inheritance using multiple tables.
 * <p/>
 * Inner test classes:<br/>
 * AddressIF<--Address<br/>
 * Employee<--Executive<--Manager
 *<p/>
 * m:n relation between Employee and Address
 * 1:1 relation from Employee to Address
 * 1:n relation from Employee to Address
 * 1:1 relation from Executive to Manager
 * 1:n relation from Manager to Executive
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: InheritanceMultipleTableTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class InheritanceMultipleTableTest extends ODMGTestCase
{
    public static void main(String[] args)
    {
        junit.textui.TestRunner.main(new String[]{InheritanceMultipleTableTest.class.getName()});
    }

    public void testQueryUsingReference_1() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQueryUsingReference_1" + timestamp;

        Manager m_1 = new Manager(id_2, name + "_manager_1");
        m_1.setDepartment("m_1");
        Address a_1 = new Address("snob allee");
        m_1.setAddress(a_1);


        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(m_1);
        tx.commit();

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Manager.class.getName() + " where name like $1 and address.street like $2");
        query.bind(name + "%");
        query.bind("snob allee");
        Collection result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        Manager retManager = (Manager) result.iterator().next();
        assertNotNull(retManager);
        assertEquals(name + "_manager_1", retManager.getName());
        assertNotNull(retManager.getAddress());
        assertEquals("snob allee", retManager.getAddress().getStreet());

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.lock(m_1, Transaction.WRITE);
        m_1.setName(m_1.getName() + "_updated");
        tx.commit();
    }

    public void testQueryUsingReference_2() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQueryUsingReference_2" + timestamp;

        Manager manager = new Manager(id_2, name + "_manager_1");
        manager.setDepartment("manager");
        Address addressManager = new Address("snob allee 1");
        Address addressManagerOld = new Address("snob allee 2");
        Address address3 = new Address("snob allee 3");
        Address address4 = new Address("snob allee 4");
        manager.setAddress(addressManager);
        manager.addOldAddress(addressManagerOld);
        manager.addCarrel(address3);
        manager.addCarrel(address4);


        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(manager);
        tx.commit();

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Manager.class.getName() + " where name like $1 and address.street like $2");
        query.bind(name + "%");
        query.bind("snob allee 1");
        Collection result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        Manager retManager = (Manager) result.iterator().next();
        assertNotNull(retManager);
        assertEquals(name + "_manager_1", retManager.getName());
        assertNotNull(retManager.getAddress());
        assertEquals("snob allee 1", retManager.getAddress().getStreet());

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.lock(manager, Transaction.WRITE);
        manager.setName(manager.getName() + "_updated");
        manager.getAddress().setStreet("updated_street");
        tx.commit();

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select objects from " + Manager.class.getName() + " where name like $1 and address.street like $2");
        query.bind(name + "%");
        query.bind("updated_street");
        result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        retManager = (Manager) result.iterator().next();
        assertNotNull(retManager);
        assertEquals(name + "_manager_1_updated", retManager.getName());
        assertNotNull(retManager.getAddress());
        assertEquals("updated_street", retManager.getAddress().getStreet());
    }

    public void testQuery_3() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testInsert" + timestamp;
        Manager m_3 = new Manager(id_2, name + "_manager_3");
        m_3.setDepartment("none");

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(m_3);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Manager.class.getName() + " where name like $1");
        query.bind(name + "%");
        List newManagers = new ArrayList((Collection) query.execute());
        tx.commit();
        assertEquals(1, newManagers.size());

        Manager new_m = (Manager) newManagers.get(0);
        assertNotNull(new_m.getId());
        assertNotNull(new_m.getId_2());
        assertEquals(m_3.getName(), new_m.getName());
        assertEquals(m_3.getDepartment(), new_m.getDepartment());

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.deletePersistent(m_3);
        tx.commit();
    }

    public void testQuery_2() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testInsert" + timestamp;
        Manager m_1 = new Manager(id_2, name + "_manager_1");
        Manager m_2 = new Manager(id_2, name + "_manager_2");
        Manager m_3 = new Manager(id_2, name + "_manager_3");
        m_3.setDepartment("none");

        Executive ex_1 = new Executive(id_2, name + "_executive", "department_1", null);
        Executive ex_2 = new Executive(id_2, name + "_executive", "department_1", null);

        Employee em = new Employee(id_2, name + "_employee");

        List executives = new ArrayList();
        executives.add(ex_1);
        executives.add(ex_2);
        m_3.setExecutives(executives);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(m_1);
        database.makePersistent(m_2);
        database.makePersistent(m_3);
        database.makePersistent(ex_1);
        database.makePersistent(ex_2);
        database.makePersistent(em);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        tx.commit();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Employee.class.getName() + " where name like $1");
        query.bind(name + "%");
        List newEmployees = new ArrayList((Collection) query.execute());
        assertEquals(6, newEmployees.size());
        int countEmployee = 0;
        int countExecutive = 0;
        int countManager = 0;
        for(int i = 0; i < newEmployees.size(); i++)
        {
            Object o =  newEmployees.get(i);
            if(o instanceof Employee)
            {
                ++countEmployee;
            }
            if(o instanceof Executive)
            {
                ++countExecutive;
            }
            if(o instanceof Manager)
            {
                ++countManager;
            }
        }
        assertEquals(6, countEmployee);
        /*
        bug:
        expect that the real classes will be populated
        currently this does not happen, only objects of
        type Employee will be returned.
        */
        assertEquals(5, countExecutive);
        assertEquals(3, countManager);
    }

    public void testQuery_1() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testInsert" + timestamp;
        Manager m_1 = new Manager(id_2, name + "_manager_1");
        Manager m_2 = new Manager(id_2, name + "_manager_2");
        Manager m_3 = new Manager(id_2, name + "_manager_3");
        m_3.setDepartment("none");

        Executive ex_1 = new Executive(id_2, name + "_executive", "department_1", null);
        Executive ex_2 = new Executive(id_2, name + "_executive", "department_1", null);

        Employee em = new Employee(id_2, name + "_employee");

        List executives = new ArrayList();
        executives.add(ex_1);
        executives.add(ex_2);
        m_3.setExecutives(executives);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(m_1);
        database.makePersistent(m_2);
        database.makePersistent(m_3);
        database.makePersistent(ex_1);
        database.makePersistent(ex_2);
        database.makePersistent(em);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        tx.commit();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Manager.class.getName() + " where name like $1 and department like $2");
        query.bind(name + "%");
        query.bind("none");
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Object o =  iterator.next();
            assertTrue(o instanceof Manager);
            Manager temp = (Manager) o;
            assertNotNull(temp.getExecutives());
            assertEquals(2, temp.getExecutives().size());
        }

        query = odmg.newOQLQuery();
        query.create("select objects from " + Employee.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(6, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Object o =  iterator.next();
            assertTrue(o instanceof Employee);
        }

        query = odmg.newOQLQuery();
        query.create("select objects from " + Executive.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(5, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Object o =  iterator.next();
            assertTrue(o instanceof Executive);
        }

        query = odmg.newOQLQuery();
        query.create("select objects from " + Manager.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(3, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Object o =  iterator.next();
            assertTrue(o instanceof Manager);
        }
    }

    public void testQueryWithSerializedObjects() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testInsert" + timestamp;
        Manager m_1 = new Manager(id_2, name + "_manager_1");
        Manager m_2 = new Manager(id_2, name + "_manager_2");
        Manager m_3 = new Manager(id_2, name + "_manager_3");
        m_3.setDepartment("none");

        Executive ex_1 = new Executive(id_2, name + "_executive", "department_1", null);
        Executive ex_2 = new Executive(id_2, name + "_executive", "department_1", null);

        Employee em = new Employee(id_2, name + "_employee");

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(m_1);
        database.makePersistent(m_2);
        database.makePersistent(m_3);
        database.makePersistent(ex_1);
        database.makePersistent(ex_2);
        database.makePersistent(em);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        tx.commit();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Manager.class.getName() + " where name like $1 and department like $2");
        query.bind(name + "%");
        query.bind("none");
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + Employee.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(6, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + Executive.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(5, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + Manager.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(3, result.size());

        em = (Employee) SerializationUtils.deserialize(SerializationUtils.serialize(em));
        m_1 = (Manager) SerializationUtils.deserialize(SerializationUtils.serialize(m_1));
        m_2 = (Manager) SerializationUtils.deserialize(SerializationUtils.serialize(m_2));
        m_3 = (Manager) SerializationUtils.deserialize(SerializationUtils.serialize(m_3));
        ex_1 = (Executive) SerializationUtils.deserialize(SerializationUtils.serialize(ex_1));
        ex_2 = (Executive) SerializationUtils.deserialize(SerializationUtils.serialize(ex_2));

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();
        tx.lock(em, Transaction.WRITE);
        tx.lock(m_1, Transaction.WRITE);
        tx.lock(m_2, Transaction.WRITE);
        tx.lock(m_3, Transaction.WRITE);
        tx.lock(ex_1, Transaction.WRITE);
        tx.lock(ex_2, Transaction.WRITE);

        em.setName(em.getName() + "_updated");
        m_1.setName(m_1.getName() + "_updated");
        m_2.setName(m_2.getName() + "_updated");
        m_3.setName(m_3.getName() + "_updated");
        ex_1.setName(ex_1.getName() + "_updated");
        ex_2.setName(ex_2.getName() + "_updated");

        tx.commit();

        query = odmg.newOQLQuery();
        query.create("select objects from " + Manager.class.getName() + " where name like $1 and department like $2");
        query.bind(name + "%");
        query.bind("none");
        result = (Collection) query.execute();
        assertEquals("Expect the same number of objects as before update", 1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + Employee.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals("Expect the same number of objects as before update", 6, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + Executive.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals("Expect the same number of objects as before update", 5, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + Manager.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals("Expect the same number of objects as before update", 3, result.size());
    }

    private void prepareForQueryTests(Long id_2, String name)
    {
        Manager m_1 = new Manager(id_2, name + "_manager_1");
        Manager m_2 = new Manager(id_2, name + "_manager_2");
        Manager m_3 = new Manager(id_2, name + "_manager_3");
        m_3.setDepartment("none");
        Address a_1 = new Address("snob allee");
        m_1.setAddress(a_1);

        Executive ex_1 = new Executive(id_2, name + "_executive", "department_1", null);
        Executive ex_2 = new Executive(id_2, name + "_executive", "department_1", null);

        Employee em = new Employee(id_2, name + "_employee");
        Address a_2 = new Address("cockroaches valley");
        em.setAddress(a_2);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(m_1);
        database.makePersistent(m_2);
        database.makePersistent(m_3);
        database.makePersistent(ex_1);
        database.makePersistent(ex_2);
        database.makePersistent(em);
        tx.commit();
    }

    public void testQuery_InheritedObjects() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQuery_InheritedObjects" + timestamp;
        prepareForQueryTests(id_2, name);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Employee.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        assertEquals(6, result.size());
        for (Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Employee obj = (Employee) iterator.next();
            assertNotNull(obj.getName());
        }

        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select objects from " + Executive.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(5, result.size());
        for (Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Executive obj = (Executive) iterator.next();
            assertNotNull(obj.getName());
        }

        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select objects from " + Manager.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(3, result.size());
        for (Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Manager obj = (Manager) iterator.next();
            assertNotNull(obj.getName());
        }

        tx.commit();
    }

    public void testQuery_InheritedField() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQuery_InheritedField" + timestamp;
        prepareForQueryTests(id_2, name);
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Manager.class.getName() + " where name like $1 and department like $2");
        query.bind(name + "%");
        query.bind("none");
        Collection result = (Collection) query.execute();
        tx.commit();
        assertEquals(1, result.size());
    }

    public void testQuery_Reference() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQuery_Reference" + timestamp;
        prepareForQueryTests(id_2, name);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Employee.class.getName() + " where name like $1 and address.street like $2");
        query.bind(name + "%");
        query.bind("%valley");
        Collection result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        Employee emp = (Employee) result.iterator().next();
        assertNotNull(emp.getAddress());
        assertEquals("cockroaches valley", emp.getAddress().getStreet());
    }

    public void testQuery_InheritedReference_1() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQuery_InheritedReference_1" + timestamp;
        prepareForQueryTests(id_2, name);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Manager.class.getName() + " where name like $1 and address.street like $2");
        query.bind(name + "%");
        query.bind("snob allee");
        Collection result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        Manager retManager = (Manager) result.iterator().next();
        assertNotNull(retManager);
        assertEquals(name + "_manager_1", retManager.getName());
        assertNotNull(retManager.getAddress());
        assertEquals("snob allee", retManager.getAddress().getStreet());
    }

    public void testQuery_InheritedReference_2() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQuery_InheritedReference_2" + timestamp;
        prepareForQueryTests(id_2, name);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Executive.class.getName() + " where name like $1 and address.street like $2");
        query.bind(name + "%");
        query.bind("snob allee");
        Collection result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        Executive retManager = (Executive) result.iterator().next();
        assertNotNull(retManager);
        assertEquals(name + "_manager_1", retManager.getName());
    }

    public void testQuery_InheritedReference_3() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQuery_InheritedReference_3" + timestamp;
        prepareForQueryTests(id_2, name);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Employee.class.getName() + " where name like $1 and address.street like $2");
        query.bind(name + "%");
        query.bind("snob allee");
        Collection result = (Collection) query.execute();
        tx.commit();
        assertEquals(1, result.size());
        Employee emp = (Employee) result.iterator().next();
        assertNotNull(emp);
        assertEquals(name + "_manager_1", emp.getName());
    }

    public void testInsertQuery() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testInsert" + timestamp;

        Employee em1 = new Employee(id_2, name);
        Executive ex1 = new Executive(id_2, name, "department_1", null);
        Executive ex2 = new Executive(id_2, name, "department_2", null);
        ArrayList list = new ArrayList();
        list.add(ex1);
        list.add(ex2);
        Manager m1 = new Manager(id_2, name);
        m1.setExecutives(list);
        ex1.setManager(m1);
        ex2.setManager(m1);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(em1);
        database.makePersistent(m1);
        tx.commit();

        tx.begin();
        Identity m1_oid = tx.getBroker().serviceIdentity().buildIdentity(m1);
        Identity ex1_oid = tx.getBroker().serviceIdentity().buildIdentity(ex1);
        Identity em1_oid = tx.getBroker().serviceIdentity().buildIdentity(em1);

        tx.getBroker().clearCache();

        Employee newEm1 = (Employee) tx.getBroker().getObjectByIdentity(em1_oid);
        Executive newEx1 = (Executive) tx.getBroker().getObjectByIdentity(ex1_oid);
        Manager newM1 = (Manager) tx.getBroker().getObjectByIdentity(m1_oid);

        assertEquals(em1, newEm1);
        assertEquals(ex1, newEx1);
        assertEquals(m1, newM1);
        assertEquals(name, newEx1.getName());
        assertEquals(name, newM1.getName());

        assertEquals(2, newM1.getExecutives().size());

        OQLQuery queryEmployee = odmg.newOQLQuery();
        queryEmployee.create("select objects from " + Employee.class.getName() + " where name like $1");
        queryEmployee.bind(name);

        OQLQuery queryExecutive = odmg.newOQLQuery();
        queryExecutive.create("select objects from " + Executive.class.getName() + " where name like $1");
        queryExecutive.bind(name);

        OQLQuery queryManager = odmg.newOQLQuery();
        queryManager.create("select objects from " + Manager.class.getName() + " where name like $1");
        queryManager.bind(name);

        Collection result = (Collection) queryEmployee.execute();
        assertEquals(4, result.size());

        result = (Collection) queryExecutive.execute();
        assertEquals(3, result.size());

        result = (Collection) queryManager.execute();
        assertEquals(1, result.size());
    }

    public void testUpdate() throws Exception
    {
        // not all changes are written to DB
        // arminw: fixed
        // if (ojbSkipKnownIssueProblem()) return;

        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testUpdate" + timestamp;
        Employee em1 = new Employee(id_2, "employee_" + name);
        Executive ex1 = new Executive(id_2, "executive_" + name, "department_1", null);
        Executive ex2 = new Executive(id_2, "executive_" + name, "department_2", null);
        ArrayList list = new ArrayList();
        list.add(ex1);
        list.add(ex2);
        Manager m1 = new Manager(id_2, "manager_" + name);
        m1.setExecutives(list);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(em1);
        database.makePersistent(m1);
        tx.commit();

        tx.begin();
        Identity m1_oid = tx.getBroker().serviceIdentity().buildIdentity(m1);
        Identity ex1_oid = tx.getBroker().serviceIdentity().buildIdentity(ex1);
        Identity em1_oid = tx.getBroker().serviceIdentity().buildIdentity(em1);

        tx.getBroker().clearCache();

        Employee newEm1 = (Employee) tx.getBroker().getObjectByIdentity(em1_oid);
        Executive newEx1 = (Executive) tx.getBroker().getObjectByIdentity(ex1_oid);
        Manager newM1 = (Manager) tx.getBroker().getObjectByIdentity(m1_oid);
        tx.commit();

        assertEquals(2, newM1.getExecutives().size());

        tx.begin();
        tx.lock(newEm1, Transaction.WRITE);
        tx.lock(newEx1, Transaction.WRITE);
        tx.lock(newM1, Transaction.WRITE);
        newEm1.setName("**updated_employee_" + name);
        newM1.setName("**updated_manager1_" + name);
        newM1.setDepartment("**new");
        ((Executive) newM1.getExecutives().get(0)).setName("**updated_executive1_" + name);
        ((Executive) newM1.getExecutives().get(1)).setName("**updated_executive2_" + name);
        tx.commit();

        //*************************************
        tx.begin();
        tx.getBroker().clearCache();
        em1 = (Employee) tx.getBroker().getObjectByIdentity(em1_oid);
        ex1 = (Executive) tx.getBroker().getObjectByIdentity(ex1_oid);
        m1 = (Manager) tx.getBroker().getObjectByIdentity(m1_oid);
        tx.commit();
        //*************************************

        assertEquals(newEm1, em1);
        assertEquals(newM1, m1);
        assertEquals(2, m1.getExecutives().size());
    }

    public void testDelete()
    {
        // not all objects will be deleted
        // arminw: fixed
        // if (ojbSkipKnownIssueProblem()) return;

        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testUpdate" + timestamp;
        Employee em1 = new Employee(id_2, "employee_" + name);
        Executive ex1 = new Executive(id_2, "executive_" + name, "department_1", null);
        Executive ex2 = new Executive(id_2, "executive_" + name, "department_2", null);
        ArrayList list = new ArrayList();
        list.add(ex1);
        list.add(ex2);
        Manager m1 = new Manager(id_2, "manager_" + name);
        m1.setExecutives(list);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(em1);
        database.makePersistent(m1);
        tx.commit();

        tx.begin();
        Identity m1_oid = tx.getBroker().serviceIdentity().buildIdentity(m1);
        Identity ex1_oid = tx.getBroker().serviceIdentity().buildIdentity(ex1);
        Identity em1_oid = tx.getBroker().serviceIdentity().buildIdentity(em1);

        tx.getBroker().clearCache();

        Employee newEm1 = (Employee) tx.getBroker().getObjectByIdentity(em1_oid);
        Executive newEx1 = (Executive) tx.getBroker().getObjectByIdentity(ex1_oid);
        Manager newM1 = (Manager) tx.getBroker().getObjectByIdentity(m1_oid);
        tx.commit();

        assertNotNull(newEm1);
        assertNotNull(newEx1);
        assertNotNull(newM1);
        assertEquals(2, newM1.getExecutives().size());

        //*************************************
        tx.begin();
        database.deletePersistent(newEm1);
        database.deletePersistent(newEx1);
        database.deletePersistent(newM1);
        tx.commit();
        //*************************************

        tx.begin();
        newEm1 = (Employee) tx.getBroker().getObjectByIdentity(em1_oid);
        newEx1 = (Executive) tx.getBroker().getObjectByIdentity(ex1_oid);
        newM1 = (Manager) tx.getBroker().getObjectByIdentity(m1_oid);
        tx.commit();

        assertNull(newEm1);
        assertNull(newEx1);
        assertNull(newM1);
    }

    //************************************************************
    // inner classes used for test
    //************************************************************
    public static class Manager extends Executive
    {
        private List executives = new ArrayList();

        public Manager()
        {
        }

        public Manager(Long id_2, String name)
        {
            super(id_2, name, null, null);
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof Manager))
            {
                return false;
            }
            Manager other = (Manager) obj;
            return new EqualsBuilder().append(getExecutives(), other.getExecutives())
                    .isEquals() && super.equals(obj);
        }

        public List getExecutives()
        {
            return executives;
        }

        public void setExecutives(List executives)
        {
            this.executives = executives;
        }
    }

    public static class Executive extends Employee
    {
        private String department;
        private Manager manager;

        public Executive()
        {
        }

        public Executive(Long id_2, String name, String department, Manager manager)
        {
            super(id_2, name);
            this.department = department;
            this.manager = manager;
        }

        public String getDepartment()
        {
            return department;
        }

        public void setDepartment(String department)
        {
            this.department = department;
        }

        public Manager getManager()
        {
            return manager;
        }

        public void setManager(Manager manager)
        {
            this.manager = manager;
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof Executive))
            {
                return false;
            }
            Executive ex = (Executive) obj;
            return new EqualsBuilder()
                    // avoid endless loop with Manager 1:n relation
                    .append((getManager() != null ? getManager().getId(): null), (ex.getManager() != null ? ex.getManager().getId() : null))
                    .append((getManager() != null ? getManager().getId_2(): null), (ex.getManager() != null ? ex.getManager().getId_2() : null))
                    .append(getDepartment(), ex.getDepartment())
                    .isEquals() && super.equals(obj);
        }
    }

    public static class Employee implements Serializable
    {
        private Integer id;
        private Long id_2;
        private String name;
        private AddressIF address;
        private List oldAddresses = new ArrayList();
        private List carrels = new ArrayList();

        public Employee()
        {
        }

        public Employee(Long id_2, String name)
        {
            this.id_2 = id_2;
            this.name = name;
        }

        public Integer getId()
        {
            return id;
        }

        public Long getId_2()
        {
            return id_2;
        }

        public void setId_2(Long id_2)
        {
            this.id_2 = id_2;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public void addOldAddress(AddressIF address)
        {
            if(oldAddresses == null)
            {
                oldAddresses = new ArrayList();
            }
            oldAddresses.add(address);
        }


        public AddressIF getAddress()
        {
            return address;
        }

        public void setAddress(AddressIF address)
        {
            this.address = address;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public List getOldAddresses()
        {
            return oldAddresses;
        }

        public void setOldAddresses(List oldAddresses)
        {
            this.oldAddresses = oldAddresses;
        }

        public void addCarrel(Address address)
        {
            if(carrels == null)
            {
                carrels = new ArrayList();
            }
            carrels.add(address);
        }

        public List getCarrels()
        {
            return carrels;
        }

        public void setCarrels(List carrels)
        {
            this.carrels = carrels;
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof Employee))
            {
                return false;
            }
            Employee em = (Employee) obj;
            return new EqualsBuilder()
                    .append(getId(), em.getId())
                    .append(getId_2(), em.getId_2())
                    .append(getName(), em.getName())
                    // avoid endless loop with Address
                    .append((getAddress() != null ? getAddress().getId() : null), (em.getAddress() != null ? em.getAddress().getId() : null))
                    .append(getOldAddresses(), em.getOldAddresses())
                    .append(getCarrels(), em.getCarrels())
                    .isEquals();
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE, false, Employee.class);
        }
    }

    public static class Address implements AddressIF
    {
        private Integer id;
        private String street;
        private List employees = new ArrayList();
        private Integer fkEmployee1;
        private Long fkEmployee2;

        public Address()
        {
        }

        public Address(String street)
        {
            this.street = street;
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof AddressIF))
            {
                return false;
            }
            AddressIF other = (AddressIF) obj;
            return new EqualsBuilder()
                    .append(getId(), other.getId())
                    .append(getStreet(), other.getStreet())
                    .append(getEmployees(), other.getEmployees())
                    .append(getFkEmployee1(), other.getFkEmployee1())
                    .append(getFkEmployee2(), other.getFkEmployee2())
                    .isEquals();
        }

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getStreet()
        {
            return street;
        }

        public void setStreet(String street)
        {
            this.street = street;
        }

        public Integer getFkEmployee1()
        {
            return fkEmployee1;
        }

        public void setFkEmployee1(Integer fkEmployee1)
        {
            this.fkEmployee1 = fkEmployee1;
        }

        public Long getFkEmployee2()
        {
            return fkEmployee2;
        }

        public void setFkEmployee2(Long fkEmployee2)
        {
            this.fkEmployee2 = fkEmployee2;
        }

        public void addEmployee(Employee emp)
        {
            if(employees == null)
            {
                employees = new ArrayList();
            }
            employees.add(emp);
        }

        public List getEmployees()
        {
            return employees;
        }

        public void setEmployees(List employees)
        {
            this.employees = employees;
        }
    }

    public static interface AddressIF
    {
        public Integer getId();
        public void setId(Integer id);
        public String getStreet();
        public void setStreet(String street);

        public List getEmployees();
        public void setEmployees(List employees);
        public Integer getFkEmployee1();
        public void setFkEmployee1(Integer id);
        public Long getFkEmployee2();
        public void setFkEmployee2(Long id);
    }
}
