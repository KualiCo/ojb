package org.apache.ojb.broker;

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

import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.junit.PBTestCase;

/**
 * This class tests how OJB handles querying against an empty
 * table in DB.
 *
 * @version $Id: EmptyTableTest.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class EmptyTableTest extends PBTestCase
{
    public EmptyTableTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        final String[] arr = {EmptyTableTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        QueryByCriteria query   = QueryFactory.newQuery(TestObject.class, new Criteria());
        broker.deleteByQuery(query);
        broker.clearCache();
    }

    public void testObjectByQuery_1() throws Exception
    {
        String dummy = "nothing_to_find";
        Criteria criteria         = new Criteria();
        criteria.addEqualTo("name", dummy);
        QueryByCriteria query   = QueryFactory.newQuery(TestObject.class, criteria);
        Object result = broker.getObjectByQuery(query);
        assertNull("We don't expect a result object", result);
    }

    public void testObjectByExample_1() throws Exception
    {
        String dummy = "nothing_to_find";
        TestObject t = new TestObject();
        t.setId(new Integer(1234));
        t.setName(dummy);
        Query query   = QueryFactory.newQuery(t);
        Object result = broker.getObjectByQuery(query);
        assertNull("We don't expect a result object", result);
    }

    public void testCollectionByQuery_1() throws Exception
    {
        String dummy = "nothing_to_find";
        Criteria criteria         = new Criteria();
        criteria.addEqualTo("name", dummy);
        Query query   = QueryFactory.newQuery(TestObject.class, criteria);
        Collection result = broker.getCollectionByQuery(query);
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            iterator.next();
            fail("We don't expect any result objects");
        }
    }

    public void testCollectionByQuery_2() throws Exception
    {
        String dummy = "nothing_to_find";
        Criteria criteria         = new Criteria();
        criteria.addEqualTo("name", dummy);
        Query query   = QueryFactory.newQuery(TestObject.class, criteria);
        // doesn't make sense to use this option here - only for test
        query.setStartAtIndex(1);
        query.setEndAtIndex(20);
        Collection result = broker.getCollectionByQuery(query);
        for(Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            iterator.next();
            fail("We don't expect any result objects");
        }
    }

    public void testIteratorByQuery_1() throws Exception
    {
        String dummy = "nothing_to_find";
        Criteria criteria         = new Criteria();
        criteria.addEqualTo("name", dummy);
        Query query   = QueryFactory.newQuery(TestObject.class, criteria);
        Iterator result = broker.getIteratorByQuery(query);
        while(result.hasNext())
        {
            result.next();
            fail("We don't expect any result objects");
        }
    }

    public void testIteratorByQuery_2() throws Exception
    {
        String dummy = "nothing_to_find";
        Criteria criteria         = new Criteria();
        criteria.addEqualTo("name", dummy);
        Query query   = QueryFactory.newQuery(TestObject.class, criteria);
        query.setStartAtIndex(1);
        query.setEndAtIndex(20);
        Iterator result = broker.getIteratorByQuery(query);
        while(result.hasNext())
        {
            result.next();
            fail("We don't expect any result objects");
        }
    }




    //===================================================================
    // inner class
    //===================================================================

    public static class TestObject
    {
        private Integer id;
        private String name;

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
}
