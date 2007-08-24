package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.util.ObjectModification;
import org.apache.ojb.junit.PBTestCase;

/**
 * These tests check inheritance using multiple tables via 1:1 reference and "super" keyword in
 * reference descriptor. The test objects use a composite PK.
 * One autoincrement PK field - Integer. One non-autoincrement PK field with manually set PK- Long.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: InheritanceMultipleTableTest.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class InheritanceMultipleTableTest extends PBTestCase
{
    public static void main(String[] args)
    {
        junit.textui.TestRunner.main(new String[]{InheritanceMultipleTableTest.class.getName()});
    }

    public void testLookupByIdentity()
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testLookupByIdentity_" + timestamp;
        Employee em1 = new Employee(id_2, "employee_" + name);
        Executive ex1 = new Executive(id_2, "executive_" + name, "department_1", null);
        Executive ex2 = new Executive(id_2, "executive_" + name, "department_2", null);
        ArrayList list = new ArrayList();
        list.add(ex1);
        list.add(ex2);
        Manager m1 = new Manager(id_2, "manager_" + name);
        m1.setExecutives(list);

        broker.beginTransaction();
        broker.store(em1);
        broker.store(m1);
        broker.commitTransaction();

        Identity m1_oid = broker.serviceIdentity().buildIdentity(m1);
        Identity ex1_oid = broker.serviceIdentity().buildIdentity(ex1);
        Identity em1_oid = broker.serviceIdentity().buildIdentity(em1);

        broker.clearCache();

        Employee newEm1 = (Employee) broker.getObjectByIdentity(em1_oid);
        Executive newEx1 = (Executive) broker.getObjectByIdentity(ex1_oid);
        Manager newM1 = (Manager) broker.getObjectByIdentity(m1_oid);

        assertNotNull(newEm1);
        assertNotNull(newEx1);
        assertNotNull(newM1);

        assertEquals(em1.getId(), newEm1.getId());
        assertEquals(em1.getId_2(), newEm1.getId_2());
        assertEquals(2, newM1.getExecutives().size());

        assertEquals(m1.getId(), newM1.getId());
        assertEquals(m1.getId_2(), newM1.getId_2());
        assertEquals(2, newM1.getExecutives().size());

        assertEquals(ex1.getId(), newEx1.getId());
        assertEquals(ex1.getId_2(), newEx1.getId_2());
        assertEquals(ex1.getDepartment(), newEx1.getDepartment());
    }

    public void testLookupByQuery()
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testLookupByIdentity_" + timestamp;
        Employee em1 = new Employee(id_2, "employee_" + name);
        Executive ex1 = new Executive(id_2, "executive_" + name, "department_1", null);
        Executive ex2 = new Executive(id_2, "executive_" + name, "department_2", null);
        ArrayList list = new ArrayList();
        list.add(ex1);
        list.add(ex2);
        Manager m1 = new Manager(id_2, "manager_" + name);
        m1.setExecutives(list);

        broker.beginTransaction();
        broker.store(em1);
        broker.store(m1);
        broker.commitTransaction();

        Criteria crit = new Criteria();
        crit.addEqualTo("name", "employee_" + name);
        Query q = QueryFactory.newQuery(Employee.class, crit);
        Employee newEm1 = (Employee) broker.getObjectByQuery(q);

        crit = new Criteria();
        crit.addEqualTo("name", "executive_" + name);
        q = QueryFactory.newQuery(Employee.class, crit);
        Executive newEx1 = (Executive) broker.getObjectByQuery(q);

        crit = new Criteria();
        crit.addEqualTo("name", "manager_" + name);
        q = QueryFactory.newQuery(Employee.class, crit);
        Manager newM1 = (Manager) broker.getObjectByQuery(q);
        
        broker.clearCache();

        assertNotNull(newEm1);
        assertNotNull(newEx1);
        assertNotNull(newM1);
        assertEquals(2, newM1.getExecutives().size());
        assertEquals(em1.getId(), newEm1.getId());
        assertEquals(em1.getId_2(), newEm1.getId_2());

        assertEquals(m1.getId(), newM1.getId());
        assertEquals(m1.getId_2(), newM1.getId_2());

        assertEquals(ex1.getId(), newEx1.getId());
        assertEquals(ex1.getId_2(), newEx1.getId_2());
    }

    public void testQueryInheritancedObjects()
    {
        if(ojbSkipKnownIssueProblem("Classes mapped to multiple joined tables will always be instantiated " +
                " with the class type of the query, instead of the real type"))
        {
            return;
        }

        long timestamp = System.currentTimeMillis();
        String name = "testQueryInheritancedObjects_" + timestamp;
        // store company with Employee/Executive/Manager
        Company company = prepareTestDataWithCompany(name);
        Long id_2 = company.getId();
        
        // add Shareholder too
        Shareholder shareholder = new Shareholder(id_2, name);
        shareholder.setShare(77);
        shareholder.setDepartment("none");
        AddressIF ad = new Address(name);
        shareholder.setAddress(ad);

        broker.beginTransaction();
        broker.store(shareholder);
        broker.commitTransaction();

        broker.clearCache();
        // now we expect 7 objects when query for all Employee (this is the base class)
        Criteria crit = new Criteria();
        crit.addEqualTo("id_2", id_2);
        crit.addLike("name", "%" + name + "%");
        Query query = QueryFactory.newQuery(Employee.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(7, result.size());
        int employeeCount = 0;
        int executiveCount = 0;
        int managerCount = 0;
        int shareholderCount = 0;
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Object obj =  iterator.next();
            if(obj instanceof Employee) ++employeeCount;
            if(obj instanceof Executive) ++executiveCount;
            if(obj instanceof Manager) ++managerCount;
            if(obj instanceof Shareholder) ++shareholderCount;
        }
        assertEquals(7, employeeCount);
        assertEquals(6, executiveCount);
        assertEquals(4, managerCount);
        assertEquals(1, shareholderCount);

        broker.clearCache();
        // now we expect 4 objects when query for all Manager
        crit = new Criteria();
        crit.addEqualTo("id_2", id_2);
        crit.addLike("name", "%" + name + "%");
        query = QueryFactory.newQuery(Manager.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals(4, result.size());
        employeeCount = 0;
        executiveCount = 0;
        managerCount = 0;
        shareholderCount = 0;
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Object obj =  iterator.next();
            if(obj instanceof Employee) ++employeeCount;
            if(obj instanceof Executive) ++executiveCount;
            if(obj instanceof Manager) ++managerCount;
            if(obj instanceof Shareholder) ++shareholderCount;
        }
        assertEquals(4, employeeCount);
        assertEquals(4, executiveCount);
        assertEquals(4, managerCount);
        assertEquals(1, shareholderCount);

        broker.clearCache();
        // now we expect 1 objects when query for all Shareholder
        crit = new Criteria();
        crit.addEqualTo("id_2", id_2);
        crit.addLike("name", "%" + name + "%");
        query = QueryFactory.newQuery(Shareholder.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals(1, result.size());
        employeeCount = 0;
        executiveCount = 0;
        managerCount = 0;
        shareholderCount = 0;
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Object obj =  iterator.next();
            if(obj instanceof Employee) ++employeeCount;
            if(obj instanceof Executive) ++executiveCount;
            if(obj instanceof Manager) ++managerCount;
            if(obj instanceof Shareholder) ++shareholderCount;
        }
        assertEquals(1, employeeCount);
        assertEquals(1, executiveCount);
        assertEquals(1, managerCount);
        assertEquals(1, shareholderCount);
    }

    public void testQueryInheritancedObjectsById()
    {
        long timestamp = System.currentTimeMillis();
        String name = "testQueryInheritancedObjectsByPk_" + timestamp;
        Long id_2 = new Long(timestamp);

        List insertedObjs = prepareForQueryTests(id_2, name); 

        // add Shareholder 
        Shareholder shareholder = new Shareholder(id_2, name);
        shareholder.setShare(77);
        shareholder.setDepartment("none");
        AddressIF ad = new Address(name);
        shareholder.setAddress(ad);

        broker.beginTransaction();
        broker.store(shareholder);
        broker.commitTransaction();
           
        broker.clearCache();

        Employee emp1;
        Identity ident;
        Employee retrievedEmp;

        // retrieve Manager by pk
        emp1 = (Employee) insertedObjs.get(0);
        ident = broker.serviceIdentity().buildIdentity(emp1);
        retrievedEmp = (Employee) broker.getObjectByIdentity(ident);
        assertNotNull(retrievedEmp);
        assertSame(Manager.class, retrievedEmp.getClass());
        assertEquals(emp1, retrievedEmp);

        // retrieve Executive by pk
        emp1 = (Employee) insertedObjs.get(3);
        ident = broker.serviceIdentity().buildIdentity(emp1);
        retrievedEmp = (Employee) broker.getObjectByIdentity(ident);
        assertNotNull(retrievedEmp);
        assertSame(Executive.class, retrievedEmp.getClass());
        assertEquals(emp1, retrievedEmp);
        
        // retrieve Employee by pk
        emp1 = (Employee) insertedObjs.get(5);
        ident = broker.serviceIdentity().buildIdentity(emp1);
        retrievedEmp = (Employee) broker.getObjectByIdentity(ident);
        assertNotNull(retrievedEmp);
        assertSame(Employee.class, retrievedEmp.getClass());
        assertEquals(emp1, retrievedEmp);

        // retrieve Shareholder by pk
        emp1 = shareholder;
        ident = broker.serviceIdentity().buildIdentity(emp1);
        retrievedEmp = (Employee) broker.getObjectByIdentity(ident);
        assertNotNull(retrievedEmp);
        assertSame(Shareholder.class, retrievedEmp.getClass());
        assertEquals(emp1, retrievedEmp);
    }

    public void testJavaInheritance()
    {
        ojbSkipKnownIssueProblem("Declared inheritance (without java inheritance)" +
                "of classes is currently not supported and will be difficult to implement");

        String name = "testWithoutJavaInheritance_tmp" + System.currentTimeMillis();
        Animal animal = new Animal(name, 55);
        Food f1 = new Food(name + "fruit1");
        Food f2 = new Food(name + "fruit2");
        animal.addFood(f1);
        animal.addFood(f2);
        // animal.setParent(animal);

        broker.beginTransaction();
        broker.store(animal);
        broker.commitTransaction();
        Identity oid = broker.serviceIdentity().buildIdentity(animal);

        broker.clearCache();
        Animal newAnimal = (Animal) broker.getObjectByIdentity(oid);
        assertTrue(animal.equals(newAnimal));

        Criteria crit = new Criteria();
        crit.addEqualTo("name", name);
        Query q = QueryFactory.newQuery(Animal.class, crit);
        Collection result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertEquals(1, result.size());
        newAnimal = (Animal) result.iterator().next();
        assertTrue(animal.equals(newAnimal));
    }

    public void testInheritancedObjectsInCollectionReferences()
    {
        if(ojbSkipKnownIssueProblem("References of classes (1:1, 1:n) mapped to multiple joined tables only" +
                " return base class type instances"))
        {
            return;
        }

        long timestamp = System.currentTimeMillis();
        String name = "testInheritancedObjectsInCollectionReferences_" + timestamp;
        Company company = prepareTestDataWithCompany(name);
        Long id_2 = company.getId();

        broker.clearCache();
        Criteria crit = new Criteria();
        crit.addEqualTo("id", id_2);
        Query query = QueryFactory.newQuery(Company.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(1, result.size());
        Company newCompany = (Company) result.iterator().next();
        List newEmployees = newCompany.getEmployees();
        assertNotNull(newEmployees);
        assertEquals(company.getEmployees().size(), newEmployees.size());
        
        List newExecutives = newCompany.getExecutives();
        assertNotNull(newExecutives);
        assertEquals(company.getExecutives().size(), newExecutives.size());

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
        assertEquals(5, countExecutive);
        assertEquals(3, countManager);
    }

    public void testInheritedReferences() throws Exception
    {
        // TODO: fix this bug
        if(ojbSkipKnownIssueProblem("[OJB-84] Will be fixed in next version")) return;

        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testInheritedReferences_" + timestamp;
        Shareholder s1 = new Shareholder(id_2, name + "_shareholder");
        s1.setShare(23);
        Shareholder s2 = new Shareholder(id_2, name + "_shareholder");
        s2.setShare(24);
        List sh = new ArrayList();
        sh.add(s1);
        sh.add(s2);
        Consortium consortium = new Consortium();
        consortium.setName(name);
        consortium.setShareholders(sh);

        Identity oidCon;
        Identity oidSH;
        broker.beginTransaction();
        broker.store(consortium);
        broker.commitTransaction();
        oidCon = broker.serviceIdentity().buildIdentity(consortium);
        oidSH = broker.serviceIdentity().buildIdentity(s1);
        broker.clearCache();
        Consortium con = (Consortium) broker.getObjectByIdentity(oidCon);
        assertNotNull(con);
        assertNotNull(con.getShareholders());
        assertEquals(2, con.getShareholders().size());

        broker.clearCache();
        Shareholder s1_new = (Shareholder) broker.getObjectByIdentity(oidSH);
        assertNotNull(s1_new.getConsortiumKey());

        broker.clearCache();
        Criteria crit = new Criteria();
        crit.addEqualTo("name", consortium.getName());
        crit.addEqualTo("shareholders.share", new Integer(24));
        crit.addEqualTo("shareholders.name", name + "_shareholder");
        Query q = QueryFactory.newQuery(Consortium.class, crit);
        Collection result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());
        assertEquals(consortium, result.iterator().next());
    }

    public void testQuery()
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQuery_" + timestamp;
        String s_name = name + "_Shareholder_3";

        Shareholder shareholder = new Shareholder(id_2, s_name);
        shareholder.setName(name);
        shareholder.setShare(77);
        shareholder.setDepartment("none");
        AddressIF ad = new Address(name);
        shareholder.setAddress(ad);

        broker.beginTransaction();
        broker.store(shareholder);
        broker.commitTransaction();

        Identity oid_shareholder = broker.serviceIdentity().buildIdentity(shareholder);
        broker.clearCache();

        Shareholder new_shareholder = (Shareholder) broker.getObjectByIdentity(oid_shareholder);
        assertNotNull(new_shareholder);
        assertEquals(shareholder, new_shareholder);

        Criteria c = new Criteria();
        c.addEqualTo("name", shareholder.getName());
        c.addEqualTo("share", new Integer(shareholder.getShare()));
        c.addEqualTo("department", shareholder.getDepartment());
        c.addEqualTo("address.street", shareholder.getAddress().getStreet());
        Query q = QueryFactory.newQuery(Shareholder.class, c);
        Collection result = broker.getCollectionByQuery(q);
        assertEquals(1, result.size());
        assertEquals(shareholder, result.iterator().next());
    }

    public void testStoreDelete_2()
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testStoreDelete_" + timestamp;
        String s_name = name + "_Shareholder_3";

        Shareholder shareholder = new Shareholder(id_2, s_name);
        shareholder.setShare(77);
        shareholder.setDepartment("none");
        AddressIF ad = new Address(name);
        shareholder.setAddress(ad);

        broker.beginTransaction();
        broker.store(shareholder);
        broker.commitTransaction();

        Identity oid_shareholder = broker.serviceIdentity().buildIdentity(shareholder);
        broker.clearCache();

        Shareholder new_shareholder = (Shareholder) broker.getObjectByIdentity(oid_shareholder);
        assertNotNull(new_shareholder);

        assertEquals(s_name, new_shareholder.getName());
        assertNotNull(new_shareholder.getAddress());
        assertEquals(name, new_shareholder.getAddress().getStreet());
        assertEquals(77, new_shareholder.getShare());

        shareholder.getAddress().setStreet(name + "_updated");
        shareholder.setShare(1313);
        shareholder.setName(name + "_updated");

        // use serialized version of object
        shareholder = (Shareholder) SerializationUtils.clone(shareholder);
        broker.beginTransaction();
        broker.store(shareholder);
        broker.commitTransaction();

        oid_shareholder = broker.serviceIdentity().buildIdentity(shareholder);
        broker.clearCache();

        new_shareholder = (Shareholder) broker.getObjectByIdentity(oid_shareholder);
        assertNotNull(new_shareholder);

        assertEquals(1313, new_shareholder.getShare());
        assertEquals(name + "_updated", new_shareholder.getName());
        assertNotNull(new_shareholder.getAddress());
        assertEquals(name + "_updated", new_shareholder.getAddress().getStreet());

        broker.beginTransaction();
        broker.delete(shareholder);
        broker.commitTransaction();

        new_shareholder = (Shareholder) broker.getObjectByIdentity(oid_shareholder);
        assertNull(new_shareholder);
    }

    public void testStoreDelete()
    {

        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testInheritancedObjectsInCollectionReferences_" + timestamp;
        String m_name = name + "_manager_3";

        Manager m = new Manager(id_2, m_name);
        m.setDepartment("none");
        AddressIF ad = new Address(name);
        m.setAddress(ad);

        String ex_name = name + "_executive";
        Executive ex = new Executive(id_2, ex_name, "department_1", null);

        String em_name = name + "_employee";
        Employee em = new Employee(id_2, em_name);

        broker.beginTransaction();
        broker.store(em);
        broker.store(ex);
        broker.store(m);
        broker.commitTransaction();

        Identity oid_em = broker.serviceIdentity().buildIdentity(em);
        Identity oid_ex = broker.serviceIdentity().buildIdentity(ex);
        Identity oid_m = broker.serviceIdentity().buildIdentity(m);
        broker.clearCache();

        Employee new_em = (Employee) broker.getObjectByIdentity(oid_em);
        Executive new_ex = (Executive) broker.getObjectByIdentity(oid_ex);
        Manager new_m = (Manager) broker.getObjectByIdentity(oid_m);

        assertNotNull(new_em);
        assertNotNull(new_ex);
        assertNotNull(new_m);

        assertEquals(em_name, new_em.getName());
        assertEquals(ex_name, new_ex.getName());
        assertEquals(m_name, new_m.getName());
        assertNotNull(new_m.getAddress());
        assertEquals(name, new_m.getAddress().getStreet());

        broker.beginTransaction();
        broker.delete(m);
        broker.delete(ex);
        broker.delete(em);
        broker.commitTransaction();

        new_em = (Employee) broker.getObjectByIdentity(oid_em);
        new_ex = (Executive) broker.getObjectByIdentity(oid_ex);
        new_m = (Manager) broker.getObjectByIdentity(oid_m);

        assertNull(new_em);
        assertNull(new_ex);
        assertNull(new_m);
    }

    public void testStoreUpdateQuerySerialized_2()
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testStoreUpdateQuerySerialized_" + timestamp;
        Manager m_1 = new Manager(id_2, name + "_manager_1");
        Manager m_2 = new Manager(id_2, name + "_manager_2");
        Manager m_3 = new Manager(id_2, name + "_manager_3");
        m_3.setDepartment("none");

        Executive ex_1 = new Executive(id_2, name + "_executive", "department_1", null);
        Executive ex_2 = new Executive(id_2, name + "_executive", "department_1", null);

        Employee em = new Employee(id_2, name + "_employee");

        broker.beginTransaction();
        broker.store(em);
        broker.store(m_1);
        broker.store(m_3);
        broker.store(ex_1);
        broker.store(m_2);
        broker.store(ex_2);
        broker.commitTransaction();

        broker.clearCache();
        Criteria crit = new Criteria();
        crit.addLike("name", name + "%");
        crit.addLike("department", "none");
        Query query = QueryFactory.newQuery(Manager.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(1, result.size());

        crit = new Criteria();
        crit.addLike("name", name + "%");
        query = QueryFactory.newQuery(Employee.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals(6, result.size());

        crit = new Criteria();
        crit.addLike("name", name + "%");
        query = QueryFactory.newQuery(Executive.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals(5, result.size());

        crit = new Criteria();
        crit.addLike("name", name + "%");
        query = QueryFactory.newQuery(Manager.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals(3, result.size());

        em = (Employee) SerializationUtils.clone(em);
        m_1 = (Manager) SerializationUtils.clone(m_1);
        m_2 = (Manager) SerializationUtils.clone(m_2);
        m_3 = (Manager) SerializationUtils.clone(m_3);
        ex_1 = (Executive) SerializationUtils.clone(ex_1);
        ex_2 = (Executive) SerializationUtils.clone(ex_2);

        em.setName(em.getName() + "_updated");
        m_1.setName(m_1.getName() + "_updated");
        m_1.setDepartment("_updated_Dep");
        m_2.setName(m_2.getName() + "_updated");
        m_3.setName(m_3.getName() + "_updated");
        ex_1.setName(ex_1.getName() + "_updated");
        ex_2.setName(ex_2.getName() + "_updated");

        broker.clearCache();
        broker.beginTransaction();
        //========================================
        // update fields
        broker.store(em, ObjectModification.UPDATE);
        broker.store(m_1, ObjectModification.UPDATE);
        broker.store(m_3, ObjectModification.UPDATE);
        broker.store(ex_1, ObjectModification.UPDATE);
        broker.store(m_2, ObjectModification.UPDATE);
        broker.store(ex_2, ObjectModification.UPDATE);
        //========================================
        broker.commitTransaction();

        /*
        after de/serialization and update we expect the same row count in
        each table
        */
        broker.clearCache();

        crit = new Criteria();
        crit.addLike("name", name + "%");
        crit.addLike("department", "_updated_Dep");
        query = QueryFactory.newQuery(Manager.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals("Expect the same number of objects as before update", 1, result.size());
        Manager newMan = (Manager) result.iterator().next();
        assertEquals(m_1.getName(), newMan.getName());
        assertEquals(m_1.getDepartment(), newMan.getDepartment());

        crit = new Criteria();
        crit.addLike("name", name + "%");
        crit.addLike("department", "none");
        query = QueryFactory.newQuery(Manager.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals("Expect the same number of objects as before update", 1, result.size());

        crit = new Criteria();
        crit.addLike("name", name + "%");
        query = QueryFactory.newQuery(Employee.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals("Expect the same number of objects as before update", 6, result.size());

        crit = new Criteria();
        crit.addLike("name", name + "%");
        query = QueryFactory.newQuery(Executive.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals("Expect the same number of objects as before update", 5, result.size());

        crit = new Criteria();
        crit.addLike("name", name + "%");
        query = QueryFactory.newQuery(Manager.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals("Expect the same number of objects as before update", 3, result.size());
    }

    public void testObjectExistence()
    {
        Manager target_1 = new Manager(new Long(1), "testObjectExistence");
        Manager target_2 = new Manager(new Long(System.currentTimeMillis()), "testObjectExistence");

        Identity oid_1 = broker.serviceIdentity().buildIdentity(target_1);
        Identity oid_2 = broker.serviceIdentity().buildIdentity(target_2);

        ClassDescriptor cld = broker.getClassDescriptor(Manager.class);

        boolean b_1 = broker.serviceBrokerHelper().doesExist(cld, oid_1, target_1);
        boolean b_2 = broker.serviceBrokerHelper().doesExist(cld, oid_2, target_2);
        assertFalse(b_1);
        assertFalse(b_2);
    }

    public void testStoreUpdateQuerySerialized()
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testStoreUpdateQuerySerialized_" + timestamp;
        Manager m_1 = new Manager(id_2, name + "_manager_1");
        Manager m_2 = new Manager(id_2, name + "_manager_2");
        Manager m_3 = new Manager(id_2, name + "_manager_3");
        m_3.setDepartment("none");

        Executive ex_1 = new Executive(id_2, name + "_executive", "department_1", null);
        Executive ex_2 = new Executive(id_2, name + "_executive", "department_1", null);

        Employee em = new Employee(id_2, name + "_employee");

        broker.beginTransaction();
        broker.store(em);
        broker.store(m_1);
        broker.store(m_3);
        broker.store(ex_1);
        broker.store(m_2);
        broker.store(ex_2);
        broker.commitTransaction();

        broker.clearCache();
        Criteria crit = new Criteria();
        crit.addLike("name", name + "%");
        crit.addLike("department", "none");
        Query query = QueryFactory.newQuery(Manager.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(1, result.size());

        crit = new Criteria();
        crit.addLike("name", name + "%");
        query = QueryFactory.newQuery(Employee.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals(6, result.size());

        crit = new Criteria();
        crit.addLike("name", name + "%");
        query = QueryFactory.newQuery(Executive.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals(5, result.size());

        crit = new Criteria();
        crit.addLike("name", name + "%");
        query = QueryFactory.newQuery(Manager.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals(3, result.size());

        em = (Employee) SerializationUtils.clone(em);
        m_1 = (Manager) SerializationUtils.clone(m_1);
        m_2 = (Manager) SerializationUtils.clone(m_2);
        m_3 = (Manager) SerializationUtils.clone(m_3);
        ex_1 = (Executive) SerializationUtils.clone(ex_1);
        ex_2 = (Executive) SerializationUtils.clone(ex_2);

        em.setName(em.getName() + "_updated");
        m_1.setName(m_1.getName() + "_updated");
        m_2.setName(m_2.getName() + "_updated");
        m_3.setName(m_3.getName() + "_updated");
        ex_1.setName(ex_1.getName() + "_updated");
        ex_2.setName(ex_2.getName() + "_updated");

        broker.clearCache();
        broker.beginTransaction();
        broker.store(em);
        broker.store(m_1);
        broker.store(m_3);
        broker.store(ex_1);
        broker.store(m_2);
        broker.store(ex_2);
        broker.commitTransaction();

        /*
        after de/serialization and update we expect the same row count in
        each table
        */
        broker.clearCache();
        crit = new Criteria();
        crit.addLike("name", name + "%");
        crit.addLike("department", "none");
        query = QueryFactory.newQuery(Manager.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals("Expect the same number of objects as before update", 1, result.size());

        crit = new Criteria();
        crit.addLike("name", name + "%");
        query = QueryFactory.newQuery(Employee.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals("Expect the same number of objects as before update", 6, result.size());

        crit = new Criteria();
        crit.addLike("name", name + "%");
        query = QueryFactory.newQuery(Executive.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals("Expect the same number of objects as before update", 5, result.size());

        crit = new Criteria();
        crit.addLike("name", name + "%");
        query = QueryFactory.newQuery(Manager.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals("Expect the same number of objects as before update", 3, result.size());
    }

    private List prepareForQueryTests(Long id_2, String name)
    {
        List result = new ArrayList();
        
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

        result.add(m_1);
        result.add(m_2);
        result.add(m_3);
        result.add(ex_1);
        result.add(ex_2);
        result.add(em);

        broker.beginTransaction();
        broker.store(m_1);
        broker.store(m_2);
        broker.store(m_3);
        broker.store(ex_1);
        broker.store(ex_2);
        broker.store(em);
        broker.commitTransaction();
        
        return result;
    }

	private Company prepareTestDataWithCompany(String name)
    {
        Long id_2 = null;
		Manager m_1 = new Manager(id_2, name + "_manager_1");
        Manager m_2 = new Manager(id_2, name + "_manager_2");
        Manager m_3 = new Manager(id_2, name + "_manager_3");
        m_3.setDepartment("none");
        Executive ex_1 = new Executive(id_2, name + "_executive", "department_1", null);
        Executive ex_2 = new Executive(id_2, name + "_executive", "department_1", null);
        Employee em = new Employee(id_2, name + "_employee");

        ArrayList employees = new ArrayList();
        employees.add(m_1);
        employees.add(m_2);
        employees.add(m_3);
        employees.add(ex_1);
        employees.add(ex_2);
        employees.add(em);

        ArrayList executives = new ArrayList();
        executives.add(m_1);
        executives.add(m_2);
        executives.add(m_3);
        executives.add(ex_1);
        executives.add(ex_2);
        
        Company company = new Company(null, name, employees, executives);
        broker.beginTransaction();
        broker.store(company);
        broker.commitTransaction();
		return company;
	}
    
    public void testQuery_InheritedObjects()
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQuery_InheritedObjects" + timestamp;
        prepareForQueryTests(id_2, name);
        broker.clearCache();

        Criteria crit = new Criteria();
        crit.addLike("name", name + "%");
        Query query = QueryFactory.newQuery(Employee.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(6, result.size());
        for (Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Employee obj = (Employee) iterator.next();
            assertNotNull(obj.getName());
        }

        broker.clearCache();
        crit = new Criteria();
        crit.addLike("name", name + "%");
        query = QueryFactory.newQuery(Executive.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals(5, result.size());
        for (Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Executive obj = (Executive) iterator.next();
            assertNotNull(obj.getName());
        }

        broker.clearCache();
        crit = new Criteria();
        crit.addLike("name", name + "%");
        query = QueryFactory.newQuery(Manager.class, crit);
        result = broker.getCollectionByQuery(query);
        assertEquals(3, result.size());
        for (Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Manager obj = (Manager) iterator.next();
            assertNotNull(obj.getName());
        }
    }

    public void testQuery_InheritedField()
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQuery_InheritedField" + timestamp;
        prepareForQueryTests(id_2, name);
        broker.clearCache();

        broker.clearCache();
        Criteria crit = new Criteria();
        crit.addLike("name", name + "%");
        crit.addLike("department", "none");
        Query query = QueryFactory.newQuery(Manager.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(1, result.size());
    }

    public void testQuery_Reference()
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQuery_Reference" + timestamp;
        prepareForQueryTests(id_2, name);
        broker.clearCache();

        Criteria crit = new Criteria();
        crit.addLike("name", name + "%");
        crit.addLike("address.street", "%valley");
        Query query = QueryFactory.newQuery(Employee.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(1, result.size());
        Employee emp = (Employee) result.iterator().next();
        assertNotNull(emp.getAddress());
        assertEquals("cockroaches valley", emp.getAddress().getStreet());
    }

    public void testQuery_InheritedReference_1()
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQuery_InheritedReference_1" + timestamp;
        prepareForQueryTests(id_2, name);
        broker.clearCache();

        Criteria crit = new Criteria();
        crit.addLike("name", name + "%");
        crit.addEqualTo("address.street", "snob allee");
        Query query = QueryFactory.newQuery(Manager.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(1, result.size());
        Manager retManager = (Manager) result.iterator().next();
        assertNotNull(retManager);
        assertEquals(name + "_manager_1", retManager.getName());
        assertNotNull(retManager.getAddress());
        assertEquals("snob allee", retManager.getAddress().getStreet());
    }

    public void testQuery_InheritedReference_2()
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQuery_InheritedReference_2" + timestamp;
        prepareForQueryTests(id_2, name);
        broker.clearCache();

        Criteria crit = new Criteria();
        crit.addLike("name", name + "%");
        crit.addEqualTo("address.street", "snob allee");
        Query query = QueryFactory.newQuery(Executive.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(1, result.size());

        Executive retManager = (Executive) result.iterator().next();
        assertNotNull(retManager);
        assertEquals(name + "_manager_1", retManager.getName());
    }

    public void testQuery_InheritedReference_3()
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testQuery_InheritedReference_3" + timestamp;
        prepareForQueryTests(id_2, name);
        broker.clearCache();

        Criteria crit = new Criteria();
        crit.addLike("name", name + "%");
        crit.addEqualTo("address.street", "snob allee");
        Query query = QueryFactory.newQuery(Employee.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(1, result.size());
    }

    public void testQuery_ReferenceOuterJoin()
    {
        long timestamp = System.currentTimeMillis();
        String name = "testQuery_ReferenceOuterJoin_" + timestamp;
        prepareTestDataWithCompany(name);
        //Long id_2 = company.getId();
        
        // Store a dummy company
        Company dummyComp = new Company(null, name + "_dummy", Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        broker.beginTransaction();
        broker.store(dummyComp);
        broker.commitTransaction();
        
        broker.clearCache();

        Criteria crit = new Criteria();
        crit.addLike("name", name + "%");
        QueryByCriteria query = QueryFactory.newQuery(Company.class, crit, true);
        Collection result = broker.getCollectionByQuery(query);
        // retrieve both companies
        assertEquals(2, result.size());

        crit = new Criteria();
        crit.addLike("name", name + "%");

        Criteria nameCrit1 = new Criteria();
        nameCrit1.addLike("executives.name", name + "%");
        Criteria nameCrit2 = new Criteria();
        nameCrit2.addIsNull("executives.name");
        nameCrit1.addOrCriteria(nameCrit2);
        crit.addAndCriteria(nameCrit1);

        query = QueryFactory.newQuery(Company.class, crit, true);
        query.addOrderByAscending("id");
        query.setPathOuterJoin("executives");
        result = broker.getCollectionByQuery(query);
        // should retrieve both companies
        assertEquals(2, result.size());
     }
    
    public void testInsertQuery()
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

        broker.beginTransaction();
        broker.store(em1);
        broker.store(m1);
        broker.commitTransaction();

        Identity m1_oid = broker.serviceIdentity().buildIdentity(m1);
        Identity ex1_oid = broker.serviceIdentity().buildIdentity(ex1);
        Identity em1_oid = broker.serviceIdentity().buildIdentity(em1);

        broker.clearCache();

        Employee newEm1 = (Employee) broker.getObjectByIdentity(em1_oid);
        Executive newEx1 = (Executive) broker.getObjectByIdentity(ex1_oid);
        Manager newM1 = (Manager) broker.getObjectByIdentity(m1_oid);

        assertEquals(em1, newEm1);
        assertEquals(ex1, newEx1);
        assertEquals(m1, newM1);
        assertEquals(name, newEx1.getName());
        assertEquals(name, newM1.getName());

        assertEquals(2, newM1.getExecutives().size());

        Criteria crit = new Criteria();
        crit.addEqualTo("name", name);
        Query queryEmployee = QueryFactory.newQuery(Employee.class, crit);
        Query queryExecutive = QueryFactory.newQuery(Executive.class, crit);
        Query queryManager = QueryFactory.newQuery(Manager.class, crit);

        Collection result = broker.getCollectionByQuery(queryEmployee);
        assertEquals(4, result.size());

        result = broker.getCollectionByQuery(queryExecutive);
        assertEquals(3, result.size());

        result = broker.getCollectionByQuery(queryManager);
        assertEquals(1, result.size());
    }

    public void testUpdate()
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testUpdate_" + timestamp;
        Employee em1 = new Employee(id_2, "employee_" + name);
        Executive ex1 = new Executive(id_2, "executive_" + name, "department_1", null);
        Executive ex2 = new Executive(id_2, "executive_" + name, "department_2", null);
        ArrayList list = new ArrayList();
        list.add(ex1);
        list.add(ex2);
        Manager m1 = new Manager(id_2, "manager_" + name);
        m1.setExecutives(list);

        broker.beginTransaction();
        broker.store(em1);
        broker.store(m1);
        broker.commitTransaction();

        Identity m1_oid = broker.serviceIdentity().buildIdentity(m1);
        Identity ex1_oid = broker.serviceIdentity().buildIdentity(ex1);
        Identity em1_oid = broker.serviceIdentity().buildIdentity(em1);

        broker.clearCache();

        Employee newEm1 = (Employee) broker.getObjectByIdentity(em1_oid);
        Executive newEx1 = (Executive) broker.getObjectByIdentity(ex1_oid);
        Manager newM1 = (Manager) broker.getObjectByIdentity(m1_oid);

        assertEquals(2, newM1.getExecutives().size());

        newEm1.setName("**updated_" + name);
        newM1.setName("**updated_" + name);
        ((Executive) newM1.getExecutives().get(0)).setName("**updated_" + name);

        broker.beginTransaction();
        broker.store(newEm1);
        broker.store(newM1);
        broker.store(newEx1);
        broker.commitTransaction();

        broker.clearCache();

        em1 = (Employee) broker.getObjectByIdentity(em1_oid);
        ex1 = (Executive) broker.getObjectByIdentity(ex1_oid);
        m1 = (Manager) broker.getObjectByIdentity(m1_oid);

        assertEquals(newEm1, em1);
        assertEquals(newEx1, ex1);
        assertEquals(newM1, m1);

        assertEquals(2, m1.getExecutives().size());
    }

    public void testDelete()
    {
        long timestamp = System.currentTimeMillis();
        Long id_2 = new Long(timestamp);
        String name = "testDelete_" + timestamp;
        Employee em1 = new Employee(id_2, "employee_" + name);
        Executive ex1 = new Executive(id_2, "executive_" + name, "department_1", null);
        Executive ex2 = new Executive(id_2, "executive_" + name, "department_2", null);
        ArrayList list = new ArrayList();
        list.add(ex1);
        list.add(ex2);
        Manager m1 = new Manager(id_2, "manager_" + name);
        m1.setExecutives(list);

        broker.beginTransaction();
        broker.store(em1);
        broker.store(m1);
        broker.commitTransaction();

        Identity m1_oid = broker.serviceIdentity().buildIdentity(m1);
        Identity ex1_oid = broker.serviceIdentity().buildIdentity(ex1);
        Identity em1_oid = broker.serviceIdentity().buildIdentity(em1);

        broker.clearCache();

        Employee newEm1 = (Employee) broker.getObjectByIdentity(em1_oid);
        Executive newEx1 = (Executive) broker.getObjectByIdentity(ex1_oid);
        Manager newM1 = (Manager) broker.getObjectByIdentity(m1_oid);

        assertNotNull(newEm1);
        assertNotNull(newEx1);
        assertNotNull(newM1);
        assertEquals(2, newM1.getExecutives().size());

        broker.beginTransaction();
        broker.delete(newEm1);
        broker.delete(newEx1);
        broker.delete(newM1);
        broker.commitTransaction();

        newEm1 = (Employee) broker.getObjectByIdentity(em1_oid);
        newEx1 = (Executive) broker.getObjectByIdentity(ex1_oid);
        newM1 = (Manager) broker.getObjectByIdentity(m1_oid);

        assertNull(newEm1);
        assertNull(newEx1);
        assertNull(newM1);
    }

    /**
     * Check backward compatibility with 'old' super-reference handling using explicite
     * anonymous field for FK to super class/table.
     */
    public void testInheritanceViaAnonymousField()
    {
        ObjectRepository.G obj_1 = new ObjectRepository.G();
        obj_1.setSomeValue(1);
        obj_1.setSomeSuperValue(2);
        obj_1.setSomeSubValue(3);

        broker.beginTransaction();
        broker.store(obj_1);
        broker.commitTransaction();

        Identity oid = broker.serviceIdentity().buildIdentity(obj_1);
        broker.clearCache();

        ObjectRepository.G obj_2 = (ObjectRepository.G) broker.getObjectByIdentity(oid);
        assertEquals(obj_1.getId(), obj_2.getId());
        assertEquals(obj_1.getSomeSubValue(), obj_2.getSomeSubValue());
        assertEquals(obj_1.getSomeSuperValue(), obj_2.getSomeSuperValue());
        assertEquals(obj_1.getSomeValue(), obj_2.getSomeValue());

        broker.beginTransaction();
        obj_1.setSomeValue(11);
        obj_1.setSomeSuperValue(22);
        obj_1.setSomeSubValue(33);
        broker.store(obj_1);
        broker.commitTransaction();

        broker.clearCache();

        ObjectRepository.G obj_3 = (ObjectRepository.G) broker.getObjectByIdentity(oid);
        assertEquals(obj_1.getId(), obj_3.getId());
        assertEquals(obj_1.getSomeSubValue(), obj_3.getSomeSubValue());
        assertEquals(obj_1.getSomeSuperValue(), obj_3.getSomeSuperValue());
        assertEquals(obj_1.getSomeValue(), obj_3.getSomeValue());

        assertEquals(obj_2.getId(), obj_3.getId());
        assertFalse(obj_2.getSomeSubValue() == obj_3.getSomeSubValue());
        assertFalse(obj_2.getSomeSuperValue() == obj_3.getSomeSuperValue());
        assertFalse(obj_2.getSomeValue() == obj_3.getSomeValue());
    }

//    /**
//     * TODO: Should we support some kind of "declarative inheritance"? This test
//     * try to use this kind of inheritance as class Dog expects some fields from a
//     * "declarated super class" (no java inheritance, only declared in metadata).
//     * In class {@link org.apache.ojb.broker.metadata.SuperReferenceDescriptor} the
//     * support is comment out (but only works for simple objects without references).
//     */
//    public void YYYtestWithoutJavaInheritance_1()
//    {
//        if (ojbSkipKnownIssueProblem("Declarative Inheritance not supported"))
//        {
//            return;
//        }
//
//        String name = "testWithoutJavaInheritance_1" + System.currentTimeMillis();
//        Dog dog = new Dog(name, 35, 4);
//        broker.beginTransaction();
//        broker.store(dog);
//        broker.commitTransaction();
//
//        broker.clearCache();
//        Criteria crit = new Criteria();
//        crit.addEqualTo("name", name);
//        Query q = QueryFactory.newQuery(Dog.class, crit);
//        Collection result = broker.getCollectionByQuery(q);
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        Dog newDog = (Dog) result.iterator().next();
//        assertTrue(dog.equals(newDog));
//
//        broker.beginTransaction();
//        newDog.setWeight(1000);
//        newDog.setLegs(10);
//        broker.store(newDog);
//        broker.commitTransaction();
//
//        broker.clearCache();
//        result = broker.getCollectionByQuery(q);
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        Dog newDog2 = (Dog) result.iterator().next();
//        assertTrue(newDog.equals(newDog2));
//
//        broker.beginTransaction();
//        broker.delete(dog);
//        broker.commitTransaction();
//    }
//
//    /**
//     * TODO: Should we support some kind of "declarative inheritance"? This test
//     * try to use this kind of inheritance as class Dog expects some fields from a
//     * "declarated super class" (no java inheritance, only declared in metadata).
//     * In class {@link org.apache.ojb.broker.metadata.SuperReferenceDescriptor} the
//     * support is comment out (but only works for simple objects without references).
//     */
//    public void YYYtestWithoutJavaInheritance_2()
//    {
//        if (ojbSkipKnownIssueProblem("Declarative Inheritance not supported"))
//        {
//            return;
//        }
//
//        String name = "testWithoutJavaInheritance_2" + System.currentTimeMillis();
//        Dog dog = new Dog(name, 35, 4);
//        Animal parent = new Animal(name + "_parent", 55);
//        Food f1 = new Food(name + "fruit1");
//        Food f2 = new Food(name + "fruit2");
//        dog.addFood(f1);
//        dog.addFood(f2);
//        dog.setParent(parent);
//
//        broker.beginTransaction();
//        broker.store(dog);
//        broker.commitTransaction();
//
//        broker.clearCache();
//        Criteria crit = new Criteria();
//        crit.addEqualTo("name", name);
//        Query q = QueryFactory.newQuery(Dog.class, crit);
//        Collection result = broker.getCollectionByQuery(q);
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        Dog newDog = (Dog) result.iterator().next();
//        assertEquals(dog, newDog);
//
//        broker.beginTransaction();
//        newDog.setWeight(1000);
//        newDog.setLegs(10);
//        newDog.addFood(new Food(name + "_new"));
//        broker.store(newDog);
//        broker.commitTransaction();
//
//        broker.clearCache();
//        result = broker.getCollectionByQuery(q);
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        Dog newDog2 = (Dog) result.iterator().next();
//        assertTrue(newDog.equals(newDog2));
//
//        broker.beginTransaction();
//        broker.delete(dog);
//        broker.commitTransaction();
//    }

    //************************************************************
    // inner classes used for test
    //************************************************************
    public static class Shareholder extends Manager
    {
        private int share;

        public Shareholder()
        {
        }

        public Shareholder(Long id_2, String name)
        {
            super(id_2, name);
        }

        public int getShare()
        {
            return share;
        }

        public void setShare(int share)
        {
            this.share = share;
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof Shareholder))
            {
                return false;
            }
            Shareholder s = (Shareholder) obj;
            return new EqualsBuilder().append(getShare(), s.getShare()).isEquals() && super.equals(obj);
        }
    }


    public static class Manager extends Executive
    {
        private List executives;
        private Integer consortiumKey;

        public Manager()
        {
        }

        public Manager(Long id_2, String name)
        {
            super(id_2, name, null, null);
        }

        public List getExecutives()
        {
            return executives;
        }

        public void setExecutives(List executives)
        {
            this.executives = executives;
        }

        public Integer getConsortiumKey()
        {
            return consortiumKey;
        }

        public void setConsortiumKey(Integer consortiumKey)
        {
            this.consortiumKey = consortiumKey;
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof Manager))
            {
                return false;
            }
            Manager m = (Manager) obj;
            return new EqualsBuilder().append(getConsortiumKey(), m.getConsortiumKey()).isEquals() && super.equals(obj);
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
            return new EqualsBuilder().append(getDepartment(), ex.getDepartment()).isEquals() && super.equals(obj);
        }
    }

    public static class Employee implements Serializable
    {
        private Integer id;
        private Long id_2;
        private String name;
        private AddressIF address;

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
                    .append(getAddress(), em.getAddress())
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

        public Address()
        {
        }

        public Address(String street)
        {
            this.street = street;
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

        public boolean equals(Object obj)
        {
            if (!(obj instanceof Address))
            {
                return false;
            }
            Address adr = (Address) obj;
            return new EqualsBuilder()
                    .append(getId(), adr.getId())
                    .append(getStreet(), adr.getStreet())
                    .isEquals();
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE, false, Address.class);
        }
    }

    public static interface AddressIF extends Serializable
    {
        public Integer getId();

        public void setId(Integer id);

        public String getStreet();

        public void setStreet(String street);
    }

    public static class Company
    {
        private Long id;
        private String name;
        private List  employees;
        private List  executives;

        public Company()
        {
        }

        public Company(Long id, String name, List employees,List executives)
        {
            this.id = id;
            this.name = name;
            this.employees = employees;
            this.executives = executives;
        }

        public Long getId()
        {
            return id;
        }

        public void setId(Long id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public List getEmployees()
        {
            return employees;
        }

        public void setEmployees(List employees)
        {
            this.employees = employees;
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

    public static class Consortium
    {
        private Integer id;
        private String name;
        private List shareholders;

        public Consortium()
        {
        }

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public List getShareholders()
        {
            return shareholders;
        }

        public void setShareholders(List shareholders)
        {
            this.shareholders = shareholders;
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof Consortium))
            {
                return false;
            }
            Consortium c = (Consortium) obj;
            return new EqualsBuilder()
                    .append(getId(), c.getId())
                    // todo: this could be problematic, if so remove it
                    .append(getShareholders(), c.getShareholders())
                    .append(getName(), c.getName()).isEquals();
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE, false, Consortium.class);
        }
    }

    public static class Entity
    {
        private Integer id;
        private String name;

        public Entity()
        {
        }

        public Entity(String name)
        {
            this.name = name;
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof Entity))
            {
                return false;
            }
            Entity other = (Entity) obj;
            return new EqualsBuilder()
                    .append(getId(), other.getId())
                    .append(getName(), other.getName())
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

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    public static class Animal
    {
        private Integer id;
        private int weight;
        private String name;
        private Animal parent;
        private List foods = new ArrayList();

        public Animal()
        {
        }

        public Animal(String name, int weight)
        {
            this.name = name;
            this.weight = weight;
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof Animal))
            {
                return false;
            }
            Animal other = (Animal) obj;
            return new EqualsBuilder()
                    .append(getId(), other.getId())
                    .append(getName(), other.getName())
                    .append(getWeight(), other.getWeight())
                    .append(getParent(), other.getParent())
                    .append(getFoods(), other.getFoods())
                    .isEquals();
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE, false, Entity.class);
        }

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public int getWeight()
        {
            return weight;
        }

        public void setWeight(int weight)
        {
            this.weight = weight;
        }

        public Animal getParent()
        {
            return parent;
        }

        public void setParent(Animal parent)
        {
            this.parent = parent;
        }

        public void addFood(Food food)
        {
            foods.add(food);
        }

        public List getFoods()
        {
            return foods;
        }

        public void setFoods(List foods)
        {
            this.foods = foods;
        }
    }

    public static class Dog
    {
        private Integer id;
        private int legs;

        // these fields should be mapped to a super table
        private String name;
        private int weight;
        private Animal parent;
        private List foods = new ArrayList();

        public Dog()
        {
        }

        public Dog(String name, int weight, int legs)
        {
            this.name = name;
            this.weight = weight;
            this.legs = legs;
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof Dog))
            {
                return false;
            }
            Dog other = (Dog) obj;
            return new EqualsBuilder()
                    .append(getId(), other.getId())
                    .append(getName(), other.getName())
                    .append(getLegs(), other.getLegs())
                    .append(getWeight(), other.getWeight())
                    .append(getParent(), other.getParent())
                    .append((getFoods() != null ? new Integer(getFoods().size()) : null),
                            ((other.getFoods() != null ? new Integer(other.getFoods().size()) : null)))
                    .isEquals();
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE, false, Dog.class);
        }

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public int getWeight()
        {
            return weight;
        }

        public void setWeight(int weight)
        {
            this.weight = weight;
        }

        public int getLegs()
        {
            return legs;
        }

        public void setLegs(int legs)
        {
            this.legs = legs;
        }

        public Animal getParent()
        {
            return parent;
        }

        public void setParent(Animal parent)
        {
            this.parent = parent;
        }

        public void addFood(Food food)
        {
            this.foods.add(food);
        }

        public List getFoods()
        {
            return foods;
        }

        public void setFoods(List foods)
        {
            this.foods = foods;
        }
    }

    public static class Food extends Entity
    {
        private Integer fkAnimal;

        public Food()
        {
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof Food))
            {
                return false;
            }
            Food other = (Food) obj;
            return new EqualsBuilder()
                    .append(getFkAnimal(), other.getFkAnimal())
                    .isEquals() && super.equals(obj);
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE, false, Food.class);
        }

        public Food(String name)
        {
            super(name);
        }

        public Integer getFkAnimal()
        {
            return fkAnimal;
        }

        public void setFkAnimal(Integer fkAnimal)
        {
            this.fkAnimal = fkAnimal;
        }
    }
}
