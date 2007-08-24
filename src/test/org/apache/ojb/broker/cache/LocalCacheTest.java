package org.apache.ojb.broker.cache;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

/**
 * Tests both caching level (LocalCache + real OjbjectCache) using circular
 * relationship tests.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: LocalCacheTest.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class LocalCacheTest extends PBTestCase
{
//    private static final int CASCADE_NONE = ObjectReferenceDescriptor.CASCADE_NONE;
//    private static final int CASCADE_LINK = ObjectReferenceDescriptor.CASCADE_LINK;
    private static final int CASCADE_OBJECT = ObjectReferenceDescriptor.CASCADE_OBJECT;

    public static void main(String[] args)
    {
        String[] arr = {LocalCacheTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testCircularStore()
    {
        // prepare test
        ojbChangeReferenceSetting(Person.class, "father", true, CASCADE_OBJECT, CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(Person.class, "grandfather", true, CASCADE_OBJECT, CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(Person.class, "childs", true, CASCADE_OBJECT, CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(Person.class, "grandchilds", true, CASCADE_OBJECT, CASCADE_OBJECT, false);

        String postfix = "_testCircularStore_" + System.currentTimeMillis();
        Person junior = createComplexFamily(postfix);
        broker.beginTransaction();
        broker.store(junior);
        broker.commitTransaction();
        Identity oidJunior = new Identity(junior, broker);
        Identity oidSenior = new Identity(junior.getFather(), broker);
        broker.clearCache();

        Criteria crit = new Criteria();
        crit.addLike("name", "jeffChild_%" + postfix);
        Query q = QueryFactory.newQuery(Person.class, crit);

        Person newJunior = (Person) broker.getObjectByIdentity(oidJunior);
        assertNotNull(newJunior);
        assertNotNull(newJunior.getFather());
        assertNotNull(newJunior.getChilds());
        assertEquals(2, newJunior.getChilds().size());

        Person newSenior = (Person) broker.getObjectByIdentity(oidSenior);
        assertNotNull(newSenior);
        assertNotNull(newSenior.getChilds());
        assertEquals(1, newSenior.getChilds().size());
        assertNotNull(newSenior.getGrandchilds());
        assertEquals(2, newSenior.getGrandchilds().size());

        Collection result = broker.getCollectionByQuery(q);
        assertEquals(2, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Person p = (Person) iterator.next();
            assertNotNull(p.getFather());
            assertEquals("jeffJunior" + postfix, p.getFather().getName());
            assertNotNull(p.getGrandfather());
            assertEquals("jeffSenior" + postfix, p.getGrandfather().getName());
        }
    }

    /**
     * same as above but without clearing the cache
     */
    public void testCircularStore_2()
    {
        // prepare test
        ojbChangeReferenceSetting(Person.class, "father", true, CASCADE_OBJECT, CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(Person.class, "grandfather", true, CASCADE_OBJECT, CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(Person.class, "childs", true, CASCADE_OBJECT, CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(Person.class, "grandchilds", true, CASCADE_OBJECT, CASCADE_OBJECT, false);

        String postfix = "_testCircularStore_2_" + System.currentTimeMillis();
        Person junior = createComplexFamily(postfix);
        broker.beginTransaction();
        broker.store(junior);
        broker.commitTransaction();
        Identity oidJunior = new Identity(junior, broker);
        Identity oidSenior = new Identity(junior.getFather(), broker);

        Criteria crit = new Criteria();
        crit.addLike("name", "jeffChild_%" + postfix);
        Query q = QueryFactory.newQuery(Person.class, crit);

        Person newJunior = (Person) broker.getObjectByIdentity(oidJunior);
        assertNotNull(newJunior);
        assertNotNull(newJunior.getFather());
        assertNotNull(newJunior.getChilds());
        assertEquals(2, newJunior.getChilds().size());

        Person newSenior = (Person) broker.getObjectByIdentity(oidSenior);
        assertNotNull(newSenior);
        assertNotNull(newSenior.getChilds());
        assertEquals(1, newSenior.getChilds().size());
        assertNotNull(newSenior.getGrandchilds());
        assertEquals(2, newSenior.getGrandchilds().size());

        Collection result = broker.getCollectionByQuery(q);
        assertEquals(2, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Person p = (Person) iterator.next();
            assertNotNull(p.getFather());
            assertEquals("jeffJunior" + postfix, p.getFather().getName());
            assertNotNull(p.getGrandfather());
            assertEquals("jeffSenior" + postfix, p.getGrandfather().getName());
        }
    }

    public void testCircularStoreUpdate()
    {
        // prepare test
        ojbChangeReferenceSetting(Person.class, "father", true, CASCADE_OBJECT, CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(Person.class, "grandfather", true, CASCADE_OBJECT, CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(Person.class, "childs", true, CASCADE_OBJECT, CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(Person.class, "grandchilds", true, CASCADE_OBJECT, CASCADE_OBJECT, false);

        String postfix = "_testCircularStore_" + System.currentTimeMillis();
        Person junior = createComplexFamily(postfix);
        broker.beginTransaction();
        broker.store(junior);
        broker.commitTransaction();
        Identity oidJunior = new Identity(junior, broker);
        Identity oidSenior = new Identity(junior.getFather(), broker);
        broker.clearCache();

        Criteria crit = new Criteria();
        crit.addLike("name", "jeffChild_%" + postfix);
        Query q = QueryFactory.newQuery(Person.class, crit);

        Person newJunior = (Person) broker.getObjectByIdentity(oidJunior);
        assertNotNull(newJunior);
        assertNotNull(newJunior.getFather());
        assertNotNull(newJunior.getChilds());
        assertEquals(2, newJunior.getChilds().size());

        Person newSenior = (Person) broker.getObjectByIdentity(oidSenior);
        assertNotNull(newSenior);
        assertNotNull(newSenior.getChilds());
        assertEquals(1, newSenior.getChilds().size());
        assertNotNull(newSenior.getGrandchilds());
        assertEquals(2, newSenior.getGrandchilds().size());

        Collection result = broker.getCollectionByQuery(q);
        assertEquals(2, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Person p = (Person) iterator.next();
            assertNotNull(p.getFather());
            assertEquals("jeffJunior" + postfix, p.getFather().getName());
            assertNotNull(p.getGrandfather());
            assertEquals("jeffSenior" + postfix, p.getGrandfather().getName());
        }

        broker.beginTransaction();
        Person newChild = new Person("newGrandChild" + postfix, null, newSenior, null);
        newSenior.addGranschild(newChild);
        broker.store(newSenior);
        broker.commitTransaction();
        broker.clearCache();

        newSenior = (Person) broker.getObjectByIdentity(oidSenior);
        assertNotNull(newSenior);
        assertNotNull(newSenior.getChilds());
        assertEquals(1, newSenior.getChilds().size());
        assertNotNull(newSenior.getGrandchilds());
        assertEquals(3, newSenior.getGrandchilds().size());
    }

    /**
     * same as above, but without clearing the cache
     */
    public void testCircularStoreUpdate_2()
    {
        // prepare test
        ojbChangeReferenceSetting(Person.class, "father", true, CASCADE_OBJECT, CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(Person.class, "grandfather", true, CASCADE_OBJECT, CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(Person.class, "childs", true, CASCADE_OBJECT, CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(Person.class, "grandchilds", true, CASCADE_OBJECT, CASCADE_OBJECT, false);

        String postfix = "_testCircularStore_2_" + System.currentTimeMillis();
        Person junior = createComplexFamily(postfix);
        broker.beginTransaction();
        broker.store(junior);
        broker.commitTransaction();
        Identity oidJunior = new Identity(junior, broker);
        Identity oidSenior = new Identity(junior.getFather(), broker);

        Criteria crit = new Criteria();
        crit.addLike("name", "jeffChild_%" + postfix);
        Query q = QueryFactory.newQuery(Person.class, crit);

        Person newJunior = (Person) broker.getObjectByIdentity(oidJunior);
        assertNotNull(newJunior);
        assertNotNull(newJunior.getFather());
        assertNotNull(newJunior.getChilds());
        assertEquals(2, newJunior.getChilds().size());

        Person newSenior = (Person) broker.getObjectByIdentity(oidSenior);
        assertNotNull(newSenior);
        assertNotNull(newSenior.getChilds());
        assertEquals(1, newSenior.getChilds().size());
        assertNotNull(newSenior.getGrandchilds());
        assertEquals(2, newSenior.getGrandchilds().size());

        Collection result = broker.getCollectionByQuery(q);
        assertEquals(2, result.size());
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Person p = (Person) iterator.next();
            assertNotNull(p.getFather());
            assertEquals("jeffJunior" + postfix, p.getFather().getName());
            assertNotNull(p.getGrandfather());
            assertEquals("jeffSenior" + postfix, p.getGrandfather().getName());
        }

        broker.beginTransaction();
        Person newChild = new Person("newGrandChild" + postfix, null, newSenior, null);
        newSenior.addGranschild(newChild);
        broker.store(newSenior);
        broker.commitTransaction();

        newSenior = (Person) broker.getObjectByIdentity(oidSenior);
        assertNotNull(newSenior);
        assertNotNull(newSenior.getChilds());
        assertEquals(1, newSenior.getChilds().size());
        assertNotNull(newSenior.getGrandchilds());
        assertEquals(3, newSenior.getGrandchilds().size());
    }

    /**
     * Creates an circular object hierarchy
     */
    private Person createComplexFamily(String postfix)
    {
        Person jeffJunior = new Person();
        jeffJunior.setName("jeffJunior" + postfix);

        Person jeffSenior = new Person();
        jeffSenior.setName("jeffSenior" + postfix);

        Person jeffChild_1 = new Person();
        jeffChild_1.setName("jeffChild_1" + postfix);
        jeffChild_1.setFather(jeffJunior);
        jeffChild_1.setGrandfather(jeffSenior);

        Person jeffChild_2 = new Person();
        jeffChild_2.setName("jeffChild_2" + postfix);
        jeffChild_2.setFather(jeffJunior);
        jeffChild_2.setGrandfather(jeffSenior);

        jeffJunior.setFather(jeffSenior);
        jeffJunior.addChild(jeffChild_1);
        jeffJunior.addChild(jeffChild_2);

        jeffSenior.addChild(jeffJunior);
        jeffSenior.addGranschild(jeffChild_1);
        jeffSenior.addGranschild(jeffChild_2);

        return jeffJunior;
    }

    public static class Person
    {
        private Integer id;
        private String name;
        private Person father;
        private Person grandfather;
        private List childs;
        private List grandchilds;
        private Integer fkChild;
        private Integer fkGrandchild;

        public Person()
        {
        }

        public Person(String name, Person father, Person grandfather, List childs)
        {
            this.name = name;
            this.father = father;
            this.grandfather = grandfather;
            this.childs = childs;
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

        public Person getFather()
        {
            return father;
        }

        public void setFather(Person father)
        {
            this.father = father;
        }

        public Person getGrandfather()
        {
            return grandfather;
        }

        public void setGrandfather(Person grandfather)
        {
            this.grandfather = grandfather;
        }

        public List getChilds()
        {
            return childs;
        }

        public void setChilds(List childs)
        {
            this.childs = childs;
        }

        public void addChild(Person child)
        {
            if(childs == null)
            {
                childs = new ArrayList();
            }
            childs.add(child);
        }

        public List getGrandchilds()
        {
            return grandchilds;
        }

        public void setGrandchilds(List grandchilds)
        {
            this.grandchilds = grandchilds;
        }

        public void addGranschild(Person child)
        {
            if(grandchilds == null)
            {
                grandchilds = new ArrayList();
            }
            grandchilds.add(child);
        }

        public Integer getFkChild()
        {
            return fkChild;
        }

        public void setFkChild(Integer fkChild)
        {
            this.fkChild = fkChild;
        }

        public Integer getFkGrandchild()
        {
            return fkGrandchild;
        }

        public void setFkGrandchild(Integer fkGrandchild)
        {
            this.fkGrandchild = fkGrandchild;
        }
    }
}
