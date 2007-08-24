package org.apache.ojb.broker;

import java.util.Collection;

import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

/**
 * Test case for checking the management of references, handling of PK used as FK too.
 * In this test the PK of {@link org.apache.ojb.broker.PrimaryKeyForeignKeyTest.Person}
 * is a FK to {@link org.apache.ojb.broker.PrimaryKeyForeignKeyTest.PersonDetail} too.
 *
 * Person has a reference to PersonDetail, the PK of Person is the FK to PersonDetail too!!
 * PersonDetail PK is not autoincremented we have to set the PK of Person
 *
 * NOTE: Don't change metadata in production environments in such a way. All made changes are global
 * changes and visible to all threads.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PrimaryKeyForeignKeyTest.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class PrimaryKeyForeignKeyTest extends PBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {PrimaryKeyForeignKeyTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void tearDown() throws Exception
    {
        try
        {
            if(broker != null)
            {
                changeRepositoryAutoSetting(Person.class, "detail", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_NONE, false);
                changeRepositoryAutoSetting(PersonDetail.class, "person", false, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_NONE, false);
                changeRepositoryAutoSetting(PersonDetail.class, "gender", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_NONE, false);
            }
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testStoreLookup()
    {
        changeRepositoryAutoSetting(Person.class, "detail", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_NONE, false);
        changeRepositoryAutoSetting(PersonDetail.class, "person", false, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_NONE, false);
        changeRepositoryAutoSetting(PersonDetail.class, "gender", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_NONE, false);


        String postfix = "_" + System.currentTimeMillis();

        broker.beginTransaction();
        Person jeff = new Person("jeff" + postfix, null);
        // store Person first to assign PK, needed to set PK in PersonDetail
        broker.store(jeff);
        GenderIF gender = new Gender("male"+postfix);
        PersonDetailIF jeffDetail = new PersonDetail(jeff.getId(), "profile"+postfix, jeff, gender);
        broker.store(gender);
        broker.serviceBrokerHelper().link(jeffDetail, true);
        broker.store(jeffDetail);
        // now we can assign reference
        jeff.setDetail(jeffDetail);
        broker.store(jeff);
        broker.commitTransaction();

        broker.clearCache();

        Criteria crit = new Criteria();
        crit.addEqualTo("name", "jeff" + postfix);
        Query query = QueryFactory.newQuery(Person.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(1, result.size());
        Person jeffNew = (Person) result.iterator().next();
        assertEquals("jeff" + postfix, jeffNew.getName());
        assertNotNull(jeffNew.getDetail());
        assertFalse(ProxyHelper.isProxy(jeffNew.getDetail()));
        PersonDetailIF jeffNewDetail = jeffNew.getDetail();
        assertNotNull(jeffNewDetail.getGender());
        assertEquals("male"+postfix, jeffNewDetail.getGender().getType());

        broker.beginTransaction();
        broker.delete(jeffNew);
        GenderIF g = jeffNewDetail.getGender();
        broker.delete(jeffNewDetail);
        broker.delete(g);
        broker.commitTransaction();
    }

    public void testStoreLookupProxy()
    {
        changeRepositoryAutoSetting(Person.class, "detail", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_NONE, true);
        changeRepositoryAutoSetting(PersonDetail.class, "person", false, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_NONE, true);
        changeRepositoryAutoSetting(PersonDetail.class, "gender", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_NONE, true);


        String postfix = "_" + System.currentTimeMillis();

        broker.beginTransaction();
        Person jeff = new Person("jeff" + postfix, null);
        // store Person first to assign PK, needed to set PK in PersonDetail
        broker.store(jeff);
        GenderIF gender = new Gender("male"+postfix);
        PersonDetailIF jeffDetail = new PersonDetail(jeff.getId(), "profile"+postfix, jeff, gender);
        broker.store(gender);
        broker.serviceBrokerHelper().link(jeffDetail, true);
        broker.store(jeffDetail);
        // now we can assign reference
        jeff.setDetail(jeffDetail);
        broker.store(jeff);
        broker.commitTransaction();

        broker.clearCache();

        Criteria crit = new Criteria();
        crit.addEqualTo("name", "jeff" + postfix);
        Query query = QueryFactory.newQuery(Person.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(1, result.size());
        Person jeffNew = (Person) result.iterator().next();
        assertEquals("jeff" + postfix, jeffNew.getName());
        assertNotNull(jeffNew.getDetail());
        assertTrue(ProxyHelper.isProxy(jeffNew.getDetail()));
        PersonDetailIF jeffNewDetail = jeffNew.getDetail();
        assertNotNull(jeffNewDetail.getGender());
        assertTrue(ProxyHelper.isProxy(jeffNewDetail.getGender()));
        assertEquals("male"+postfix, jeffNewDetail.getGender().getType());

        broker.beginTransaction();
        broker.delete(jeffNew);
        GenderIF g = jeffNewDetail.getGender();
        broker.delete(jeffNewDetail);
        broker.delete(g);
        broker.commitTransaction();
    }

    public void testStoreLookup_2()
    {
        changeRepositoryAutoSetting(Person.class, "detail", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        changeRepositoryAutoSetting(PersonDetail.class, "person", false, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        changeRepositoryAutoSetting(PersonDetail.class, "gender", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, false);


        String postfix = "_" + System.currentTimeMillis();

        broker.beginTransaction();
        Person jeff = new Person("jeff" + postfix, null);
        // store Person first to assign PK, needed to set PK in PersonDetail
        broker.store(jeff);
        GenderIF gender = new Gender("male"+postfix);
        PersonDetailIF jeffDetail = new PersonDetail(jeff.getId(), "profile"+postfix, jeff, gender);
        broker.store(jeffDetail);
        // now we can assign reference
        jeff.setDetail(jeffDetail);
        broker.store(jeff);
        broker.commitTransaction();

        broker.clearCache();

        Criteria crit = new Criteria();
        crit.addEqualTo("name", "jeff" + postfix);
        Query query = QueryFactory.newQuery(Person.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(1, result.size());
        Person jeffNew = (Person) result.iterator().next();
        assertEquals("jeff" + postfix, jeffNew.getName());
        assertNotNull(jeffNew.getDetail());
        assertFalse(ProxyHelper.isProxy(jeffNew.getDetail()));
        PersonDetailIF jeffNewDetail = jeffNew.getDetail();
        assertNotNull(jeffNewDetail.getGender());
        assertEquals("male"+postfix, jeffNewDetail.getGender().getType());

        broker.beginTransaction();
        broker.delete(jeffNew);
        GenderIF g = jeffNewDetail.getGender();
        broker.delete(jeffNewDetail);
        broker.delete(g);
        broker.commitTransaction();
    }

    public void testStoreLookupProxy_2()
    {
        changeRepositoryAutoSetting(Person.class, "detail", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, true);
        changeRepositoryAutoSetting(PersonDetail.class, "person", false, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, true);
        changeRepositoryAutoSetting(PersonDetail.class, "gender", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, true);


        String postfix = "_" + System.currentTimeMillis();

        broker.beginTransaction();
        Person jeff = new Person("jeff" + postfix, null);
        // store Person first to assign PK, needed to set PK in PersonDetail
        broker.store(jeff);
        GenderIF gender = new Gender("male"+postfix);
        PersonDetailIF jeffDetail = new PersonDetail(jeff.getId(), "profile"+postfix, jeff, gender);
        broker.store(jeffDetail);
        // now we can assign reference
        jeff.setDetail(jeffDetail);
        broker.store(jeff);
        broker.commitTransaction();

        broker.clearCache();

        Criteria crit = new Criteria();
        crit.addEqualTo("name", "jeff" + postfix);
        Query query = QueryFactory.newQuery(Person.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(1, result.size());
        Person jeffNew = (Person) result.iterator().next();
        assertEquals("jeff" + postfix, jeffNew.getName());
        assertNotNull(jeffNew.getDetail());
        assertTrue(ProxyHelper.isProxy(jeffNew.getDetail()));
        PersonDetailIF jeffNewDetail = jeffNew.getDetail();
        assertNotNull(jeffNewDetail.getGender());
        assertTrue(ProxyHelper.isProxy(jeffNewDetail.getGender()));
        assertEquals("male"+postfix, jeffNewDetail.getGender().getType());

        broker.beginTransaction();
        broker.delete(jeffNew);
        GenderIF g = jeffNewDetail.getGender();
        broker.delete(jeffNewDetail);
        broker.delete(g);
        broker.commitTransaction();
    }

    private void changeRepositoryAutoSetting(Class clazz, String attributeName, boolean retrieve, int update, int delete, boolean proxy)
    {
        ClassDescriptor cld = broker.getClassDescriptor(clazz);
        ObjectReferenceDescriptor ord = cld.getObjectReferenceDescriptorByName(attributeName);
        ord.setCascadeRetrieve(retrieve);
        ord.setCascadingStore(update);
        ord.setCascadingDelete(delete);
        ord.setLazy(proxy);
    }

    public static class Person
    {
        Integer id;
        String name;
        PersonDetailIF detail;

        public Person()
        {
        }

        public Person(String name, PersonDetailIF detail)
        {
            this.name = name;
            this.detail = detail;
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

        public PersonDetailIF getDetail()
        {
            return detail;
        }

        public void setDetail(PersonDetailIF detail)
        {
            this.detail = detail;
        }
    }

    public static interface PersonDetailIF
    {
        public Integer getId();
        public void setId(Integer id);
        public String getProfile();
        public void setProfile(String profile);
        public Person getPerson();
        public void setPerson(Person person);
        public GenderIF getGender();
        public void setGender(GenderIF person);
    }

    public static class PersonDetail implements PersonDetailIF
    {
        Integer id;
        String profile;
        Person person;
        GenderIF gender;

        public PersonDetail()
        {
        }

        public PersonDetail(Integer id, String profile, Person person, GenderIF gender)
        {
            this.id = id;
            this.profile = profile;
            this.person = person;
            this.gender = gender;
        }

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getProfile()
        {
            return profile;
        }

        public void setProfile(String profile)
        {
            this.profile = profile;
        }

        public Person getPerson()
        {
            return person;
        }

        public void setPerson(Person person)
        {
            this.person = person;
        }

        public GenderIF getGender()
        {
            return gender;
        }

        public void setGender(GenderIF gender)
        {
            this.gender = gender;
        }
    }

    public static interface GenderIF
    {
        public String getType();
        public void setType(String type);
    }

    public static class Gender implements GenderIF
    {
        String type;

        public Gender()
        {
        }

        public Gender(String type)
        {
            this.type = type;
        }

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }
    }
}
