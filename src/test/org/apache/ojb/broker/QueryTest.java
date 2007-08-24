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

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.math.BigDecimal;

import org.apache.commons.lang.time.StopWatch;
import org.apache.ojb.broker.accesslayer.sql.SqlGenerator;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.platforms.PlatformHsqldbImpl;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.LikeCriteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.query.ReportQueryByCriteria;
import org.apache.ojb.junit.PBTestCase;

/**
 * Testing the query API.
 * @version $Id: QueryTest.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class QueryTest extends PBTestCase
{

    // Product groups above high water mark are modified by other testcases,
    // setting this low makes it easier to re-run this test
    private static final Integer PGROUP_ID_HI_WATERMARK =
            new Integer(4000);
    
    /**
     * BrokerTests constructor comment.
     */
    public QueryTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        String[] arr = {QueryTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testQueryZero()
    {
        String name = "testQueryZero_" + System.currentTimeMillis();
        ObjectRepository.Group group = new ObjectRepository.Group();
        group.setId(new Integer(0));
        group.setName(name);

        // prepare test
        broker.beginTransaction();
        Query q = QueryFactory.newQuery(group);
        broker.deleteByQuery(q);
        broker.commitTransaction();
        broker.clearCache();

        QueryByCriteria query = new QueryByCriteria(group);
        ObjectRepository.Group newGroup = (ObjectRepository.Group) broker.getObjectByQuery(query);
        assertNull(newGroup);

        broker.beginTransaction();
        broker.store(group);
        broker.commitTransaction();

        ObjectRepository.Group groupTemp = new ObjectRepository.Group();
        groupTemp.setId(new Integer(0));
        Query queryNew = QueryFactory.newQuery(groupTemp);
        newGroup = (ObjectRepository.Group) broker.getObjectByQuery(queryNew);
        assertNotNull(newGroup);
        assertEquals(new Integer(0), newGroup.getId());

        newGroup = (ObjectRepository.Group) broker.getObjectByQuery(queryNew);
        assertNotNull(newGroup);
        assertEquals(new Integer(0), newGroup.getId());

        broker.clearCache();
        newGroup = (ObjectRepository.Group) broker.getObjectByQuery(queryNew);
        assertNotNull(newGroup);
        assertEquals(new Integer(0), newGroup.getId());
    }

    /**
     * Criteria containing other Criteria only
     */
    public void testCriteria()
    {
        String name = "testCriteria_" + System.currentTimeMillis();
        Person p1 = new Person();
        p1.setFirstname("tomm");
        p1.setLastname(name);

        Person p2 = new Person();
        p2.setFirstname("tom");
        p2.setLastname(name);

        Person p3 = new Person();
        p3.setFirstname("xtom");
        p3.setLastname(name);

        broker.beginTransaction();
        broker.store(p1);
        broker.store(p2);
        broker.store(p3);
        broker.commitTransaction();

        Criteria crit1 = new Criteria();
        Criteria crit2 = new Criteria();
        Criteria crit3 = new Criteria();

        crit2.addEqualTo("lastname", name);
        crit2.setNegative(false);

        crit3.addEqualTo("firstname", "tom");
        crit3.setNegative(true);

        crit1.addAndCriteria(crit2);
        crit1.addAndCriteria(crit3);

        Query q = QueryFactory.newQuery(Person.class, crit1);
        Collection results = broker.getCollectionByQuery(q);

        // all persons except tom
        assertEquals(2, results.size());
    }

    /**
     * test EqualTo Criteria
     */
    public void testEqualCriteria()
    {
        Criteria crit = new Criteria();
        crit.addEqualTo("firstname", "tom");
        Query q = QueryFactory.newQuery(Person.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertTrue(results.size() > 0);
    }

    /**
     * test sql Criteria
     */
    public void testSqlCriteria()
    {
        Criteria crit;
        Query q;
        Collection results;
        
        // sql only
        crit = new Criteria();
        crit.addSql("upper(firstname) = 'TOM' and id = 1");
        q = QueryFactory.newQuery(Person.class, crit);

        results = broker.getCollectionByQuery(q);
        int size1 = results.size();
        
        // sql plus attribute 
        crit = new Criteria();
        crit.addSql("upper(firstname) = 'TOM'");
        crit.addEqualTo("id", new Integer(1));
        q = QueryFactory.newQuery(Person.class, crit);

        results = broker.getCollectionByQuery(q);
        int size2 = results.size();
        
        // attribute plus sql 
        crit = new Criteria();
        crit.addEqualTo("upper(firstname)", "TOM");
        crit.addSql("id = 1");
        q = QueryFactory.newQuery(Person.class, crit);

        results = broker.getCollectionByQuery(q);
        int size3 = results.size();

        assertTrue(size2 == size1);
        assertTrue(size3 == size2);
    }


    /**
     * test OrderBy and Count
     */
    public void testOrderByCount()
    {
        Criteria crit = new Criteria();
        crit.addEqualTo("firstname", "tom");
        QueryByCriteria q = QueryFactory.newQuery(Person.class, crit);
        q.addOrderByAscending("firstname");

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        
        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);
    }

    /**
     * test OrderBy joined column
     */
    public void testOrderByJoined()
    {
        String name = "testOrderByJoined" + System.currentTimeMillis();
        Person p = new Person();
        p.setFirstname("tom");
        p.setLastname(name);

        Project p1 = new Project();
        p1.setTitle(name);
        ArrayList list_1 = new ArrayList();
        list_1.add(p);
        p1.setPersons(list_1);

        Project p2 = new Project();
        p2.setTitle(name);
        ArrayList list_2 = new ArrayList();
        list_2.add(p);
        p2.setPersons(list_2);

        ArrayList list_projects = new ArrayList();
        list_projects.add(p1);
        list_projects.add(p2);
        p.setProjects(list_projects);

        Project p3 = new Project();
        p3.setTitle(name);
        ArrayList list_3 = new ArrayList();
        // empty list
        p3.setPersons(list_3);

        broker.beginTransaction();
        broker.store(p1);
        //broker.store(p2);
        //broker.store(p3);
        broker.commitTransaction();

        Criteria crit = new Criteria();
        crit.addLike("title", name);
        QueryByCriteria q = QueryFactory.newQuery(Project.class, crit);
        q.addOrderByAscending("title");
        q.addOrderByAscending("persons.lastname");
        q.addOrderByAscending("persons.firstname");

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(2, results.size());

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);
    }

    /**
     * test Subquery get all product groups without articles
     * <p/>
     * test may fail if db does not support sub queries
     */
    public void testSubQuery2()
    {
        Collection results = null;
        String stamp = "" + System.currentTimeMillis();
        int loops = 10;
        // create ProductGroups without article
        broker.beginTransaction();
        for(int i = 0; i < loops; i++)
        {
            ProductGroup pg = new ProductGroup();
            pg.setGroupName("test group " + stamp);
            pg.setDescription("build by QueryTest#testSubQuery2");
            broker.store(pg);
        }
        broker.commitTransaction();

        ReportQueryByCriteria subQuery;
        Criteria subCrit = new Criteria();
        Criteria crit = new Criteria();

        subQuery = QueryFactory.newReportQuery(Article.class, subCrit);
        subQuery.setAttributes(new String[]{"productGroupId"});
        subQuery.setDistinct(true);

        crit.addEqualTo("groupName", "test group " + stamp);
        crit.addNotIn("groupId", subQuery);
        Query q = QueryFactory.newQuery(ProductGroup.class, crit);

        results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals("Result of the query with sub-query does not match", loops, results.size());
    }

    /**
     * test Subquery get all articles with price > avg(price) PROBLEM:
     * avg(price) is NOT extent aware !!
     * <p/>
     * test may fail if db does not support sub queries
     */
    public void testSubQuery1()
    {

        ReportQueryByCriteria subQuery;
        Criteria subCrit = new Criteria();
        Criteria crit = new Criteria();

        subCrit.addLike("articleName", "A%");
        subQuery = QueryFactory.newReportQuery(Article.class, subCrit);
        subQuery.setAttributes(new String[]{"avg(price)"});

        crit.addGreaterOrEqualThan("price", subQuery);
        Query q = QueryFactory.newQuery(Article.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertTrue(results.size() > 0);

    }

    /**
     * test Subquery get all product groups with more than 10 articles, uses
     * attribute as value ! see testSubQuery4 for a better way
     * <p/>
     * test may fail if db does not support sub queries
     */
    public void testSubQuery3()
    {

        ReportQueryByCriteria subQuery;
        Criteria subCrit = new Criteria();
        Criteria crit = new Criteria();

        subCrit.addEqualToField("productGroupId", Criteria.PARENT_QUERY_PREFIX + "groupId");
        subQuery = QueryFactory.newReportQuery(Article.class, subCrit);
        subQuery.setAttributes(new String[]{"count(productGroupId)"});

        crit.addLessThan("10", subQuery); // MORE than 10 articles, uses
        // attribute as value !
        crit.addLessThan("groupId", PGROUP_ID_HI_WATERMARK);
        Query q = QueryFactory.newQuery(ProductGroup.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(4, results.size());
    }

    /**
     * test Subquery get all product groups with more than 10 articles
     * <p/>
     * test may fail if db does not support sub queries
     */
    public void testSubQuery4()
    {

        ReportQueryByCriteria subQuery;
        Criteria subCrit = new Criteria();
        Criteria crit = new Criteria();

        subCrit.addEqualToField("productGroupId", Criteria.PARENT_QUERY_PREFIX + "groupId");
        subQuery = QueryFactory.newReportQuery(Article.class, subCrit);
        subQuery.setAttributes(new String[]{"count(productGroupId)"});

        // mkalen: if using String("10") instead of Integer below,
        // PostgreSQL will return 7 (sic!) groups
        crit.addGreaterThan(subQuery, new Integer(10)); // MORE than 10 articles
        crit.addLessThan("groupId", PGROUP_ID_HI_WATERMARK);
        Query q = QueryFactory.newQuery(ProductGroup.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(4, results.size());
    }

    /**
     * test Like Criteria
     */
    public void testLikeCriteria()
    {

        Criteria crit = new Criteria();
        crit.addLike("firstname", "%o%");
        Query q = QueryFactory.newQuery(Person.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertTrue(results.size() > 0);

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);
    }

    /**
     * test escaped Like Criteria
     */
    public void testLikeEscapedCriteria1()
    {
        Criteria crit = new Criteria();
        crit.addLike("firstname", "h%\\%");
        Query q = QueryFactory.newQuery(Person.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(1, results.size());

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);
    }

    /**
     * test escaped Like Criteria with escape character
     */
    public void testLikeEscapedCriteria2()
    {
        LikeCriteria.setEscapeCharacter('|');

        Criteria crit = new Criteria();
        crit.addLike("firstname", "h%|%");
        Query q = QueryFactory.newQuery(Person.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(1, results.size());

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);
    }

    /**
     * test escaped Like Criteria
     */
    public void testLikeEscapedCriteria3()
    {
        String name = "testLikeEscapedCriteria3()_" + System.currentTimeMillis();
        Person p = new Person();
        p.setFirstname("123%45");
        p.setLastname(name);
        broker.beginTransaction();
        broker.store(p);
        broker.commitTransaction();

        LikeCriteria.setEscapeCharacter('/');
        Criteria crit = new Criteria();
        crit.addEqualTo("lastname", name);
        crit.addLike("firstname", "%/%45");
        Query q = QueryFactory.newQuery(Person.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    /**
     * test Null Criteria
     */
    public void testNullCriteria()
    {
        String name = "testNullCriteria_" + System.currentTimeMillis();
        Person p = new Person();
        p.setLastname(name);
        broker.beginTransaction();
        broker.store(p);
        broker.commitTransaction();

        Criteria crit = new Criteria();
        crit.addIsNull("firstname");
        Criteria crit2 = new Criteria();
        crit2.addLike("lastname", name);
        crit.addAndCriteria(crit2);
        
        Query q = QueryFactory.newQuery(Person.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    /**
     * Add an empty criteria
     */
    public void testEmptyORed() throws Exception
    {
        Collection result;
        Criteria crit1 = new Criteria();
        crit1.addEqualTo("articleName", "Hamlet");
        crit1.addEqualTo("productGroup.description", "Strange Books...");

        Criteria crit2 = new Criteria();

        crit1.addOrCriteria(crit2);
        QueryByCriteria q = QueryFactory.newQuery(Article.class, crit1);

        result = broker.getCollectionByQuery(q);
        assertNotNull(result);

        int count = broker.getCount(q);
        assertEquals(count, result.size());
    }

    /**
     * Add an empty criteria
     */
    public void testEmptyANDed() throws Exception
    {
        Collection result;
        Criteria crit1 = new Criteria();
        crit1.addEqualTo("articleName", "Hamlet");
        crit1.addEqualTo("productGroup.description", "Strange Books...");

        Criteria crit2 = new Criteria();

        crit1.addAndCriteria(crit2);
        QueryByCriteria q = QueryFactory.newQuery(Article.class, crit1);

        result = broker.getCollectionByQuery(q);
        assertNotNull(result);

        int count = broker.getCount(q);
        assertEquals(count, result.size());
    }

    /**
     * test Between Criteria
     */
    public void testBetweenCriteria()
    {

        Criteria crit = new Criteria();
        crit.addBetween("id", new Integer(1), new Integer(5));
        Query q = QueryFactory.newQuery(Person.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertTrue(results.size() == 5);

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);
    }

    /**
     * test In Criteria
     */
    public void testInCriteria()
    {

        Criteria crit = new Criteria();
        Collection ids = new Vector();
        ids.add(new Integer(1));
        ids.add(new Integer(3));
        ids.add(new Integer(5));

        crit.addIn("id", ids);
        Query q = QueryFactory.newQuery(Person.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertTrue(results.size() == 3);

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);
    }

    /**
     * Single Path Expression
     */
    public void testPathExpressions()
    {

        Criteria crit = new Criteria();
        crit.addEqualTo("productGroup.groupName", "Liquors");
        Query q = QueryFactory.newQuery(Article.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertTrue(results.size() > 0);

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);
    }

    /**
     * Multi Path Expression over decomposed m:n
     */
    public void testPathExpressionsMtoNDecomposed()
    {

        Criteria crit = new Criteria();
        crit.addEqualTo("roles.project.title", "HSQLDB");
        // use decomposed m:n
        Query q = QueryFactory.newQuery(Person.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);

    }

    /**
     * Multi Path Expression over nondecomposed m:n
     */
    public void testPathExpressionsMtoN()
    {

        Criteria crit = new Criteria();
        crit.addEqualTo("projects.title", "HSQLDB"); // direct m:n
        Query q = QueryFactory.newQuery(Person.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);

    }

    /**
     * Multi Path Expression over nondecomposed m:n new test case for the 'not
     * unique alias' problem with m:n relationship
     */
    public void testPathExpressionsMtoN2()
    {
        Criteria crit = new Criteria();
        crit.addEqualTo("projects.roles.roleName", "developer");
        crit.addLike("projects.persons.lastname", "%b%");
        Query q = QueryFactory.newQuery(Person.class, crit);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertTrue(results.size() > 0);
    }

    /**
     * Distinct Query
     */
    public void testDistinct()
    {

        Criteria crit = new Criteria();
        crit.addLike("allArticlesInGroup.articleName", "C%");
        QueryByCriteria q = QueryFactory.newQuery(ProductGroup.class, crit, true);
        q.addOrderByAscending("groupId");

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(5, results.size());

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);
    }

    /**
     * Distinct Query
     */
    public void testDistinctMultiPk()
    {

        Criteria crit = new Criteria();
        crit.addEqualTo("project_id", new Integer(1));
        QueryByCriteria q = QueryFactory.newQuery(Role.class, crit, true);

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(3, results.size());

        // compare with count
        int count = broker.getCount(q);

        assertEquals(results.size(), count); // FAILS
    }

    /**
     * Simple ReportQuery returning rows with 3 columns of Person
     */
    public void testReportQuery()
    {
        String name = "testReportQuery_" + System.currentTimeMillis();
        Person p1 = new Person();
        p1.setFirstname("Robert");
        p1.setLastname(name);
        Person p2 = new Person();
        p2.setFirstname("Tom");
        p2.setLastname(name);
        Person p3 = new Person();
        p3.setFirstname("Roger");
        p3.setLastname(name);
        broker.beginTransaction();
        broker.store(p1);
        broker.store(p2);
        broker.store(p3);
        broker.commitTransaction();


        Criteria crit = new Criteria();
        Collection results = new Vector();
        crit.addLike("firstname", "%o%");
        Criteria crit2 = new Criteria();
        crit2.addLike("lastname", name);
        crit.addAndCriteria(crit2);
        ReportQueryByCriteria q = QueryFactory.newReportQuery(Person.class, crit);
        q.setAttributes(new String[]{"id", "firstname", "count(*)"});
        q.addGroupBy(new String[]{"id", "firstname"});

        Iterator iter = broker.getReportQueryIteratorByQuery(q);

        assertNotNull(iter);
        while(iter.hasNext())
        {
            Object[] row = (Object[]) iter.next();
            assertEquals(3, row.length);

            results.add(row);
        }

        assertEquals(3, results.size());

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);
    }

    /**
     * Simple ReportQuery returning rows with max(id) of Person grouped by not selected columns
     */
    public void testReportQueryGroupByNonSelectColumns()
    {
        String name = "testReportQueryGroupByNonSelectColumns_" + System.currentTimeMillis();
        Person p1 = new Person();
        p1.setFirstname("Robert");
        p1.setLastname(name);
        Person p2 = new Person();
        p2.setFirstname("Tom");
        p2.setLastname(name);
        Person p3 = new Person();
        p3.setFirstname("Roger");
        p3.setLastname(name);
        broker.beginTransaction();
        broker.store(p1);
        broker.store(p2);
        broker.store(p3);
        broker.commitTransaction();

        Criteria crit = new Criteria();
        Collection results = new Vector();
        crit.addLike("firstname", "%o%");
        Criteria crit2 = new Criteria();
        crit2.addLike("lastname", name);
        crit.addAndCriteria(crit2);
        ReportQueryByCriteria q = QueryFactory.newReportQuery(Person.class, crit);
        q.setAttributes(new String[]{"max(id)"});
        q.addGroupBy(new String[]{"lastname", "firstname"});

        Iterator iter = broker.getReportQueryIteratorByQuery(q);

        assertNotNull(iter);
        while(iter.hasNext())
        {
            Object[] row = (Object[]) iter.next();
            assertEquals(1, row.length);

            results.add(row);
        }

        assertEquals(3, results.size());

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);
    }

    /**
     * Simple ReportQuery returning rows with 3 columns of Person
     * Type of column data defined by sqltypes
     *
     * @see java.sql.Types
     */
    public void testReportQueryWithJdbcTypes()
    {
        String name = "testReportQuery_" + System.currentTimeMillis();
        Person p1 = new Person();
        p1.setFirstname("Robert");
        p1.setLastname(name);
        Person p2 = new Person();
        p2.setFirstname("Tom");
        p2.setLastname(name);
        Person p3 = new Person();
        p3.setFirstname("Roger");
        p3.setLastname(name);
        broker.beginTransaction();
        broker.store(p1);
        broker.store(p2);
        broker.store(p3);
        broker.commitTransaction();

        int types[] = new int[]{Types.DECIMAL, Types.VARCHAR, Types.BIGINT};
        Criteria crit = new Criteria();
        Collection results = new Vector();
        crit.addLike("firstname", "%o%");
        Criteria crit2 = new Criteria();
        crit2.addLike("lastname", name);
        crit.addAndCriteria(crit2);

        ReportQueryByCriteria q = QueryFactory.newReportQuery(Person.class, crit);
        q.setAttributes(new String[]{"id", "firstname", "count(*)"});
        q.addGroupBy(new String[]{"id", "firstname"});
        q.setJdbcTypes(types);

        Iterator iter = broker.getReportQueryIteratorByQuery(q);

        assertNotNull(iter);
        while(iter.hasNext())
        {
            Object[] row = (Object[]) iter.next();
            assertEquals(3, row.length);

//            assertEquals(row[0].getClass(), BigDecimal.class);
//            assertEquals(row[1].getClass(), String.class);
//            assertEquals(row[2].getClass(), Long.class);

            results.add(row);
        }

        assertEquals(3, results.size());

    }

    /**
     * Simple ReportQuery returning rows with 3 columns of Person
     * needs SQL paging
     */
    public void testReportQueryWithStartAndEnd()
    {
        Criteria crit = new Criteria();
        Collection results = new Vector();
        crit.addLike("firstname", "%o%");
        ReportQueryByCriteria q = QueryFactory.newReportQuery(Person.class, crit);
        q.setAttributes(new String[]{"id", "firstname", "count(*)"});
        q.addGroupBy(new String[]{"id", "firstname"});

        q.setStartAtIndex(3);
        q.setEndAtIndex(5);
        Iterator iter = broker.getReportQueryIteratorByQuery(q);
        assertNotNull(iter);
        while(iter.hasNext())
        {
            results.add(iter.next());
        }
        assertTrue(results.size() == 3);

        results.clear();
        q.setStartAtIndex(1);
        q.setEndAtIndex(5);
        iter = broker.getReportQueryIteratorByQuery(q);
        assertNotNull(iter);
        while(iter.hasNext())
        {
            results.add(iter.next());
        }
        assertTrue(results.size() == 5);
    }

    /**
     * Simple ReportQuery returning rows with 2 columns of Person
     */
    public void testReportQueryExtent()
    {

        Criteria crit = new Criteria();
        Collection results = new Vector();
        crit.addLike("articleName", "%o%");
        ReportQueryByCriteria q = QueryFactory.newReportQuery(Article.class, crit);
        q.setAttributes(new String[]{"articleId", "articleName", "price"});

        Iterator iter = broker.getReportQueryIteratorByQuery(q);

        assertNotNull(iter);
        while(iter.hasNext())
        {
            results.add(iter.next());
        }
        assertTrue(results.size() > 0);

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);
    }

    /**
     * Concrete Class in SubQuery
     */
    public void testSubQueryAgainstConcreteClass()
    {
        String name = "testSubQueryAgainstConcreteClass_" + System.currentTimeMillis();
        Article article = new BookArticle();
        article.setArticleName(name);
        ProductGroup group = new ProductGroup();
        group.setGroupName(name);
        article.setProductGroup(group);

        broker.beginTransaction();
        broker.store(group);
        broker.store(article);
        broker.commitTransaction();

        Criteria critMain = new Criteria();
        Criteria critSub = new Criteria();

        critSub.addEqualTo("articleName", name);
        ReportQueryByCriteria querySub = QueryFactory.newReportQuery(BookArticle.class, critSub);
        querySub.setAttributes(new String[]{"productGroupId"});

        ReportQueryByCriteria queryMain = QueryFactory.newReportQuery(AbstractProductGroup.class, critMain);
        queryMain.setAttributes(new String[]{"groupId", "groupName"});
        critMain.addIn("groupId", querySub);

        Iterator iter = broker.getReportQueryIteratorByQuery(queryMain);
        int result = 0;
        assertNotNull(iter);
        while(iter.hasNext())
        {
            iter.next();
            ++result;
        }
        assertEquals(1, result);

        // compare with count
        int count = broker.getCount(queryMain);
        assertEquals(result, count);
    }

    /**
     * Class with extents in SubQuery.
     * SubQueries are NOT extent aware ! so we have to use two queries.
     */
    public void testSubQueryAgainstExtents()
    {
        String name = "testSubQueryAgainstExtents_" + System.currentTimeMillis();
        Article article = new BookArticle();
        article.setArticleName(name);
        ProductGroup group = new ProductGroup();
        group.setGroupName(name);
        article.setProductGroup(group);

        broker.beginTransaction();
        broker.store(group);
        broker.store(article);
        broker.commitTransaction();

        Criteria critMain = new Criteria();
        Criteria critSub = new Criteria();

        critSub.addEqualTo("articleName", name);
        ReportQueryByCriteria querySub = QueryFactory.newReportQuery(Article.class, critSub);
        querySub.setAttributes(new String[]{"productGroupId"});
        Iterator subIter = broker.getReportQueryIteratorByQuery(querySub);
        Collection subIds = new ArrayList();
        while(subIter.hasNext())
        {
            Object[] id = (Object[]) subIter.next();
            subIds.add(id[0]);
        }

        ReportQueryByCriteria queryMain = QueryFactory.newReportQuery(AbstractProductGroup.class, critMain);
        queryMain.setAttributes(new String[]{"groupId", "groupName"});
        critMain.addIn("groupId", subIds);

        Iterator iter = broker.getReportQueryIteratorByQuery(queryMain);
        int result = 0;
        assertNotNull(iter);
        while(iter.hasNext())
        {
            iter.next();
            ++result;
        }
        assertEquals(1, result);
    
        // compare with count
        int count = broker.getCount(queryMain);
        assertEquals(result, count);
    }

    /**
     * ReportQuery with pathExpression in columns
     */
    public void testReportQueryPathExpression()
    {
        Criteria crit = new Criteria();
        Collection results = new Vector();
        crit.addLike("articleName", "C%");
        ReportQueryByCriteria q = QueryFactory.newReportQuery(Article.class, crit);
        q.setAttributes(new String[]{"productGroup.groupName", "articleId", "articleName", "price"});

        Iterator iter = broker.getReportQueryIteratorByQuery(q);

        assertNotNull(iter);
        while(iter.hasNext())
        {
            results.add(iter.next());
        }
        assertEquals(9, results.size());

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);

    }

    /**
     * ReportQuery returning rows with some "Liquor" data ordered by price
     */
    public void testReportQueryOrderBy()
    {

        Criteria crit = new Criteria();
        Collection results = new Vector();
        crit.addEqualTo("productGroup.groupName", "Liquors");
        ReportQueryByCriteria q = QueryFactory.newReportQuery(Article.class, crit);
        q.setAttributes(new String[]{"articleId", "articleName", "price"});
        q.addOrderByAscending("price");

        Iterator iter = broker.getReportQueryIteratorByQuery(q);
        assertNotNull(iter);
        while(iter.hasNext())
        {
            results.add(iter.next());
        }
        assertTrue(results.size() > 0);

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);

    }

    /**
     * ReportQuery returning rows with some "Liquor" data ordered by productGroup.groupId
     */
    public void testReportQueryOrderByNonSelectColumn()
    {

        Criteria crit = new Criteria();
        Collection results = new Vector();
        crit.addEqualTo("productGroup.groupName", "Liquors");
        ReportQueryByCriteria q = QueryFactory.newReportQuery(Article.class, crit);
        q.setAttributes(new String[]{"articleId", "articleName", "price"});
        q.addOrderByAscending("productGroup.groupId");

        Iterator iter = broker.getReportQueryIteratorByQuery(q);
        assertNotNull(iter);
        while(iter.hasNext())
        {
            results.add(iter.next());
        }
        assertTrue(results.size() > 0);

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);

    }

    /**
     * ReportQuery returning rows with summed stock and price per article group
     * The selected columns point to a class having extents.<br>
     * The query produces a wrong sql, selecting only rows of the top-class
     */
    public void testReportQueryGroupByExtents2()
    {
        Criteria crit = new Criteria();
        Collection results = new Vector();
        ReportQueryByCriteria q = QueryFactory.newReportQuery(ProductGroup.class, crit);
        q.setAttributes(new String[]{"groupName", "sum(allArticlesInGroup.stock)", "sum(allArticlesInGroup.price)"});
        q.addGroupBy("groupName");

        Iterator iter = broker.getReportQueryIteratorByQuery(q);
        
//        SELECT A0.KategorieName,sum(A1.Lagerbestand),sum(A1.Einzelpreis)
//        FROM Kategorien A0
//        LEFT OUTER JOIN artikel A1 ON A0.Kategorie_Nr=A1.Kategorie_Nr
//        LEFT OUTER JOIN books A1E2 ON A0.Kategorie_Nr=A1E2.Kategorie_Nr
//        LEFT OUTER JOIN cds A1E1 ON A0.Kategorie_Nr=A1E1.Kategorie_Nr
//        GROUP BY A0.KategorieName
        
        assertNotNull(iter);
        while(iter.hasNext())
        {
            results.add(iter.next());
        }
        assertTrue(results.size() > 0);

        // TODO: resolve
        if (ojbSkipKnownIssueProblem("broker.getCount() vs .getReportQueryIteratorByQuery().size()"))
        {
            return;
        }

        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);   // FAILS !
    }

    /**
     * ReportQuery returning rows with summed stock and price per article group
     * The selected class has the extents.<br>
     * The query returns summed values for each row, so there may be multiple rows
     * for one productgroup because one query for each extent is executed.
     */
    public void testReportQueryGroupByExtents1()
    {
        Criteria crit = new Criteria();
        Collection results = new Vector();
        ReportQueryByCriteria q = QueryFactory.newReportQuery(Article.class, crit);
        q.setAttributes(new String[]{"productGroup.groupName", "sum(stock)", "sum(price)"});
        q.addGroupBy("productGroup.groupName");

        Iterator iter = broker.getReportQueryIteratorByQuery(q);
        
//        SELECT  A1.KategorieName,sum(A0.Lagerbestand),sum(A0.Einzelpreis) 
//        FROM artikel A0 
//        INNER JOIN Kategorien A1 ON A0.Kategorie_Nr=A1.Kategorie_Nr
//        GROUP BY A1.KategorieName
//
//        SELECT  A1.KategorieName,sum(A0.Lagerbestand),sum(A0.Einzelpreis) 
//        FROM cds A0 
//        INNER JOIN Kategorien A1 ON A0.Kategorie_Nr=A1.Kategorie_Nr
//        GROUP BY A1.KategorieName
//
//        SELECT  A1.KategorieName,sum(A0.Lagerbestand),sum(A0.Einzelpreis) 
//        FROM books A0 
//        INNER JOIN Kategorien A1 ON A0.Kategorie_Nr=A1.Kategorie_Nr
//        GROUP BY A1.KategorieName

        assertNotNull(iter);
        while(iter.hasNext())
        {
            results.add(iter.next());
        }
        assertTrue(results.size() > 0);

        // TODO: resolve
        if (ojbSkipKnownIssueProblem("broker.getCount() vs .getReportQueryIteratorByQuery().size()"))
        {
            return;
        }
        
        // compare with count
        int count = broker.getCount(q);
        assertEquals(results.size(), count);   // FAILS !
    }

    /**
     * Read a CD and then read the ProductGroup for the CD
     */
    public void testInversePathExpression()
    {
        QueryByCriteria query;
        Criteria crit;
        CdArticle cd;
        ProductGroupProxy pg, cdPg;

        crit = new Criteria();
        crit.addEqualTo("articleId", new Integer(200));
        query = new QueryByCriteria(CdArticle.class, crit);
        cd = (CdArticle) broker.getObjectByQuery(query);
        cdPg = (ProductGroupProxy) cd.getProductGroup();

        crit = new Criteria();
        crit.addEqualTo("allArticlesInGroup.articleId", cd.getArticleId());
        query = new QueryByCriteria(ProductGroup.class, crit);
        query.setPathClass("allArticlesInGroup", CdArticle.class);
        pg = (ProductGroupProxy) broker.getObjectByQuery(query);

        // this test can only succeed in singlevm mode:
//        if (!BrokerHelper.isRunningInServerMode())
//        {
//            assertNotNull(pg);
//            assertNotNull(cdPg);
//            assertEquals("ProductGroups should be identical", pg.getRealSubject().toString(), cdPg.getRealSubject().toString());
//        }

        assertNotNull(pg);
        assertNotNull(cdPg);
        assertEquals("ProductGroups should be identical", pg.getRealSubject().toString(), cdPg.getRealSubject().toString());
    }

    /**
     * prefetch Articles for ProductGroupsWithArray, Does not yet work with
     * Arrays
     */
    public void testPrefetchedArraySingleKey()
    {
        ClassDescriptor cldArticle = broker.getClassDescriptor(Article.class);
        Class articleProxy = cldArticle.getProxyClass();

        //
        // use ProductGroup and Articles with disabled Proxy
        //
        broker.clearCache();
        cldArticle.setProxyClass(null);

        Criteria crit = new Criteria();
        crit.addLessOrEqualThan("groupId", new Integer(5));
        QueryByCriteria q = QueryFactory.newQuery(ProductGroupWithArray.class, crit);
        q.addOrderByDescending("groupId");
        q.addPrefetchedRelationship("allArticlesInGroup");

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        ProductGroupWithArray pg = (ProductGroupWithArray) results.toArray()[0];
        int articleSize = pg.getAllArticles().length;
        assertTrue(articleSize != 0);
        String articleString = Arrays.asList(pg.getAllArticles()).toString();

        //
        // use ProductGroupWithArray and Articles with original Proxy settings
        //
        broker.clearCache();
        cldArticle.setProxyClass(articleProxy);


        crit = new Criteria();
        crit.addEqualTo("groupId", new Integer(5));
        q = QueryFactory.newQuery(ProductGroupWithArray.class, crit);
        results = broker.getCollectionByQuery(q);
        ProductGroupWithArray pg2 = (ProductGroupWithArray) results.toArray()[0];
        InterfaceArticle[] articles = pg2.getAllArticles();
        assertNotNull("Array of articles should not be null!", articles);
        assertTrue("Array should contain more than 0 entries!", articles.length != 0);

        // force materialization
        for(int i = 0; i < articles.length; i++)
        {
            articles[i].getArticleName();
        }
        int articleSize2 = articles.length;
        String articleString2 = Arrays.asList(articles).toString();

        //
        // compare prefetched and 'normal' data
        //
        assertEquals("Check size", articleSize, articleSize2);
        assertEquals("Check content", articleString, articleString2);

    }

    /**
     * orderby for prefetch Articles of ProductGroups
     */
    public void testPrefetchedCollectionOrderBy()
    {
        ClassDescriptor cldProductGroup = broker.getClassDescriptor(ProductGroup.class);
        ClassDescriptor cldArticle = broker.getClassDescriptor(Article.class);
        Class productGroupProxy = cldProductGroup.getProxyClass();
        Class articleProxy = cldArticle.getProxyClass();
        CollectionDescriptor cds = cldProductGroup.getCollectionDescriptorByName("allArticlesInGroup");

        //
        // use ProductGroup and Articles with disabled Proxy
        //
        cldProductGroup.setProxyClass(null);
        cldProductGroup.setProxyClassName(null);
        cldArticle.setProxyClass(null);
        cldArticle.setProxyClassName(null);
        broker.getDescriptorRepository().setClassDescriptor(cldProductGroup);
        broker.getDescriptorRepository().setClassDescriptor(cldArticle);
        
        //
        // orderby articleId, ASC
        //
        broker.clearCache();
        cds.getOrderBy().clear();
        cds.addOrderBy("articleId", true);
         
        Criteria crit = new Criteria();
        crit.addLessOrEqualThan("groupId", new Integer(5));
        QueryByCriteria q = QueryFactory.newQuery(ProductGroup.class, crit);
        q.addOrderByDescending("groupId");
        q.addPrefetchedRelationship("allArticlesInGroup");

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertTrue(results.size() == 5);
        InterfaceProductGroup pg = (InterfaceProductGroup) results.toArray()[1];
        assertNotNull(pg.getAllArticles());
        Object articles[] = pg.getAllArticles().toArray();
        int articleSize = articles.length;
        assertTrue(articleSize == 10);
        Article a1 = (Article) articles[0];
        Article a2 = (Article) articles[9];
        assertTrue(a1.getArticleId().intValue() < a2.getArticleId().intValue());

        //
        // orderby articleId, DESC
        //
        broker.clearCache();
        cds.getOrderBy().clear();
        cds.addOrderBy("articleId", false);

        results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertTrue(results.size() == 5);
        pg = (InterfaceProductGroup) results.toArray()[1];
        assertNotNull(pg.getAllArticles());
        articles = pg.getAllArticles().toArray();
        articleSize = articles.length;
        assertTrue(articleSize == 10);
        Article b1 = (Article) articles[0];
        Article b2 = (Article) articles[9];
        assertTrue(b1.getArticleId().intValue() > b2.getArticleId().intValue());

        assertEquals(a1.getArticleId(), b2.getArticleId());
        assertEquals(a2.getArticleId(), b1.getArticleId());

        //
        // use ProductGroup and Articles with original Proxy settings
        //
        cldProductGroup.setProxyClass(productGroupProxy);
        cldProductGroup.setProxyClassName(productGroupProxy.getName());
        cldArticle.setProxyClass(articleProxy);
        cldArticle.setProxyClassName(articleProxy.getName());
        broker.getDescriptorRepository().setClassDescriptor(cldProductGroup);
        broker.getDescriptorRepository().setClassDescriptor(cldArticle);
   }

    /**
     * prefetch Articles for ProductGroups
     */
    public void testPrefetchedCollectionSingleKey()
    {
        ClassDescriptor cldProductGroup = broker.getClassDescriptor(ProductGroup.class);
        ClassDescriptor cldArticle = broker.getClassDescriptor(Article.class);
        Class productGroupProxy = cldProductGroup.getProxyClass();
        Class articleProxy = cldArticle.getProxyClass();

        //
        // use ProductGroup and Articles with disabled Proxy
        //
        broker.clearCache();
        cldProductGroup.setProxyClass(null);
        cldProductGroup.setProxyClassName(null);
        cldArticle.setProxyClass(null);
        cldArticle.setProxyClassName(null);
        broker.getDescriptorRepository().setClassDescriptor(cldProductGroup);
        broker.getDescriptorRepository().setClassDescriptor(cldArticle);

        Criteria crit = new Criteria();
        crit.addLessOrEqualThan("groupId", new Integer(5));
        QueryByCriteria q = QueryFactory.newQuery(ProductGroup.class, crit);
        q.addOrderByDescending("groupId");
        q.addPrefetchedRelationship("allArticlesInGroup");

        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        InterfaceProductGroup pg = (InterfaceProductGroup) results.toArray()[0];
        assertNotNull(pg.getAllArticles());
        int articleSize = pg.getAllArticles().size();
        String articleString = pg.getAllArticles().toString();

        //
        // use ProductGroup and Articles with original Proxy settings
        //
        broker.clearCache();
        cldProductGroup.setProxyClass(productGroupProxy);
        cldProductGroup.setProxyClassName(productGroupProxy.getName());
        cldArticle.setProxyClass(articleProxy);
        cldArticle.setProxyClassName(articleProxy.getName());
        broker.getDescriptorRepository().setClassDescriptor(cldProductGroup);
        broker.getDescriptorRepository().setClassDescriptor(cldArticle);

        crit = new Criteria();
        crit.addEqualTo("groupId", new Integer(5));
        q = QueryFactory.newQuery(ProductGroup.class, crit);
        results = broker.getCollectionByQuery(q);
        InterfaceProductGroup pg2 = (InterfaceProductGroup) results.toArray()[0];
        // force materialization
        for(Iterator it = pg2.getAllArticles().iterator(); it.hasNext();)
        {
            ((InterfaceArticle) it.next()).getArticleName();
        }
        int articleSize2 = pg2.getAllArticles().size();
        String articleString2 = pg2.getAllArticles().toString();

        //
        // compare prefetched and 'normal' data
        //
        assertEquals("Check size", articleSize, articleSize2);
        assertEquals("Check content", articleString, articleString2);
    }

    /**
     * Test nested joins using pathExpressions
     */
    public void testNestedJoins()
    {
        ArrayList list = new java.util.ArrayList();

        Criteria crit = new Criteria();
        ReportQueryByCriteria q = QueryFactory.newReportQuery(Person.class, crit, true);

        q.setAttributes(new String[]{"roles.roleName", "roles.project.title", "firstname", });

        Iterator iter = broker.getReportQueryIteratorByQuery(q);
        while(iter.hasNext())
        {
            list.add(iter.next());
        }

        assertTrue(list.size() > 0);
    }

    /**
     * Test multiple non nested joins using pathExpressions
     */
    public void testMultipleJoins()
    {
        ArrayList list = new java.util.ArrayList();

        Criteria crit = new Criteria();
        ReportQueryByCriteria q = QueryFactory.newReportQuery(Role.class, crit, true);

        q.setAttributes(new String[]{"roleName", "project.title", "person.firstname", });

        Iterator iter = broker.getReportQueryIteratorByQuery(q);
        while(iter.hasNext())
        {
            list.add(iter.next());
        }

        assertTrue(list.size() > 0);
    }

    /**
     * Test nested joins using pathExpressions *** Fails under hsqldb because
     * of join using multiple keys ***
     */
    public void tesXNestedJoins2()
    {
        ArrayList list = new ArrayList();
        Criteria crit = new Criteria();
        ReportQueryByCriteria q = QueryFactory.newReportQuery(Task.class, crit);

        q.setAttributes(new String[]{"role.roleName", "role.project.title", "role.person.firstname", });

        Iterator iter = broker.getReportQueryIteratorByQuery(q);
        while(iter.hasNext())
        {
            list.add(iter.next());
        }
        assertTrue(list.size() > 0);
    }

    /**
     * prefetch ProductGroups for Articles
     */
    public void testPrefetchedReferencesSingleKey()
    {
        ClassDescriptor cldProductGroup = broker.getClassDescriptor(ProductGroup.class);
        ClassDescriptor cldArticle = broker.getClassDescriptor(Article.class);
        Class productGroupProxy = cldProductGroup.getProxyClass();
        Class articleProxy = cldArticle.getProxyClass();

        //
        // use ProductGroup and Articles with disabled Proxy
        //
        broker.clearCache();
        cldProductGroup.setProxyClass(null);
        cldProductGroup.setProxyClassName(null);
        cldArticle.setProxyClass(null);
        cldArticle.setProxyClassName(null);
        broker.getDescriptorRepository().setClassDescriptor(cldProductGroup);
        broker.getDescriptorRepository().setClassDescriptor(cldArticle);

        Criteria crit = new Criteria();
        crit.addNotNull("productGroupId");
        crit.addLessOrEqualThan("productGroupId", new Integer(5));
        QueryByCriteria q = QueryFactory.newQuery(Article.class, crit);
        q.addOrderByDescending("productGroupId");
        q.addPrefetchedRelationship("productGroup");

        Collection results = broker.getCollectionByQuery(q);
        Set pgs = new HashSet();
        Iterator iter = results.iterator();
        while(iter.hasNext())
        {
            InterfaceArticle a = (InterfaceArticle) iter.next();
            pgs.add(a.getProductGroup().getName());
        }

        assertTrue(pgs.size() > 0);
        String pgsString = pgs.toString();

        //
        // use ProductGroup and Articles with original Proxy settings
        //
        broker.clearCache();
        cldProductGroup.setProxyClass(productGroupProxy);
        cldProductGroup.setProxyClassName(productGroupProxy.getName());
        cldArticle.setProxyClass(articleProxy);
        cldArticle.setProxyClassName(articleProxy.getName());
        broker.getDescriptorRepository().setClassDescriptor(cldProductGroup);
        broker.getDescriptorRepository().setClassDescriptor(cldArticle);

        crit = new Criteria();
        crit.addNotNull("productGroupId");
        crit.addLessOrEqualThan("productGroupId", new Integer(5));
        q = QueryFactory.newQuery(Article.class, crit);
        q.addOrderByDescending("productGroupId");

        results = broker.getCollectionByQuery(q);
        Set pgs2 = new HashSet();
        iter = results.iterator();
        while(iter.hasNext())
        {
            InterfaceArticle a = (InterfaceArticle) iter.next();
            pgs2.add(a.getProductGroup().getName());
        }

        assertTrue(pgs2.size() > 0);
        String pgsString2 = pgs2.toString();

        //
        // compare prefetched and 'normal' data
        //
        assertEquals("Check size", pgs.size(), pgs2.size());
        assertEquals("Check content", pgsString, pgsString2);

    }

    /**
     * test PathExpression pointing to abstract class (InterfaceArticle)
     */
    public void testReportPathExpressionAbstractExtent()
    {
        // TODO: make path expressions extent aware
        if(ojbSkipKnownIssueProblem("Make path expressions extent aware")) return;

        ArrayList list = new ArrayList();
        Criteria crit = new Criteria();
        crit.addEqualTo("groupId", new Integer(5));

        ReportQueryByCriteria q = QueryFactory.newReportQuery(ProductGroupWithAbstractArticles.class, crit, true);
        q.setAttributes(new String[]{"groupId", "groupName", "allArticlesInGroup.articleId", "allArticlesInGroup.articleName"});

        Iterator iter = broker.getReportQueryIteratorByQuery(q);
        while(iter.hasNext())
        {
            list.add(iter.next());
        }

        // 7 Articles, 2 Books, 3 Cds
        //  BRJ: path expression is not yet extent aware
        assertEquals("check size", 12, list.size());
    }

    /**
     * ReportQuery returning rows with some "Liquor" data ordered by productGroup.groupId
     */
    public void testQueryOrderByNonSelectColumn()
    {

        Criteria crit = new Criteria();
        Collection results = new Vector();
        crit.addEqualTo("productGroup.groupName", "Liquors");
        QueryByCriteria q = QueryFactory.newQuery(Article.class, crit);
        q.addOrderByAscending("productGroup.groupId");

        Iterator iter = broker.getIteratorByQuery(q);
        assertNotNull(iter);
        while(iter.hasNext())
        {
            results.add(iter.next());
        }
        assertTrue(results.size() > 0);

    }

    /**
     * test PathExpression pointing to abstract class (InterfaceArticle)
     */
    public void testPathExpressionForAbstractExtent()
    {
        ArrayList list = new java.util.ArrayList();

        Criteria crit = new Criteria();
        crit.addLike("allArticlesInGroup.articleName", "Chef%");
        Criteria crit1 = new Criteria();
        crit1.addEqualTo("allArticlesInGroup.articleName", "Faust");
        crit.addOrCriteria(crit1);

        QueryByCriteria q = QueryFactory.newQuery(ProductGroupWithAbstractArticles.class, crit, true);
        q.addOrderByAscending("groupId");

        Iterator iter = broker.getIteratorByQuery(q);
        while(iter.hasNext())
        {
            list.add(iter.next());
        }

        // Groups 2, 5
        assertEquals("check size", 2, list.size());

        assertEquals("Group 2", 2, ((InterfaceProductGroup) list.get(0)).getId().intValue());
        assertEquals("Group 5", 5, ((InterfaceProductGroup) list.get(1)).getId().intValue());
    }

    /**
     * Test pathExpression and Extents
     */
    public void testReportPathExpressionForExtents1()
    {
        ArrayList list = new java.util.ArrayList();

        Criteria crit = new Criteria();
        crit.addGreaterOrEqualThan("allArticlesInGroup.articleId", new Integer(1));
        crit.addLessOrEqualThan("allArticlesInGroup.articleId", new Integer(5));

        ReportQueryByCriteria q = QueryFactory.newReportQuery(ProductGroup.class, crit, true);
        q.setAttributes(new String[]{"groupId", "groupName", "allArticlesInGroup.articleId"});

        Iterator iter = broker.getReportQueryIteratorByQuery(q);
        while(iter.hasNext())
        {
            Object row;
            Object[] columns;

            assertNotNull("Invalid ReportQueryIterator, hasNext() is true but next() is null",
                    row = iter.next());
            assertTrue("ReportQuery result row is not Object[]",
                    row instanceof Object[]);
            columns = (Object[]) row;
            list.add(columns);

            assertTrue("ReportQuery result row does not contain all expected columns",
                    columns.length == 3);

            /*
            arminw:
            think hsql returns the wrong result or interpret the query in wrong
            way (e.g. using hashcode of values instead values itself), so skip test
            evaluation for this DB
            */
            if(!broker.serviceConnectionManager().getSupportedPlatform().getClass().equals(PlatformHsqldbImpl.class))
            {
//                System.out.println("### " + ((Object[]) obj)[0]
//                        + "  " + ((Object[]) obj)[1]
//                        + "  " + ((Object[]) obj)[2]);
                Object articleId = columns[2];
                int i = -1;
                if (articleId instanceof Integer) {
                    i = ((Integer) articleId).intValue();
                } else if (articleId instanceof BigDecimal) {
                    i = ((BigDecimal) articleId).intValue();
                } else {
                    assertTrue("TODO: Your platforms resulting class for INTEGER (" +
                                articleId.getClass().getName() +
                                ") is not yet supported in testcase.", false);
                }

                assertTrue("i=" + i, i < 6 & i > 0);
            }
        }
    }

    /**
     * Test pathExpression and Extents
     */
    public void testReportPathExpressionForExtents2()
    {
        ArrayList list = new java.util.ArrayList();

        Criteria crit = new Criteria();
        crit.addLike("groupName", "%o%");
        crit.addLike("allArticlesInGroup.articleName", "%\u00f6%"); //unicode for �

        ReportQueryByCriteria q = QueryFactory.newReportQuery(ProductGroup.class, crit, true);
        q.setAttributes(new String[]{"groupId", "groupName"});

        Iterator iter = broker.getReportQueryIteratorByQuery(q);
        while(iter.hasNext())
        {
            list.add(iter.next());
        }

        // Groups: 3 Books , 1 Dairy Product
        assertEquals("check size", 2, list.size());
    }

    /**
     * ReportQuery with Expression in column need to add table alias to the field (price)
     **/
    public void testReportQueryExpressionInStatement()
    {
        // TODO: Resolve attributes of expressions
        if(ojbSkipKnownIssueProblem("Resolve attributes of expressions")) return;

        Criteria crit = new Criteria();
        ReportQueryByCriteria q = QueryFactory.newReportQuery(Article.class, crit);
        q.setAttributes(new String[]{"articleId", "price+10"});
        ClassDescriptor cd = broker.getClassDescriptor(q.getBaseClass());
        SqlGenerator sqlg = broker.serviceSqlGenerator();
        String sql = sqlg.getPreparedSelectStatement(q, cd).getStatement();
        
        assertTrue("Bad query generated. the 'price' field has not table prefix. SQL Output: " + sql, sql
                .equalsIgnoreCase("SELECT A0.Artikel_Nr,A0.Einzelpreis+10 FROM Artikel A0"));
    }
     

    /**
     * Test pathExpression and Extents
     */
    public void testPathExpressionForExtents1()
    {
        ArrayList list = new java.util.ArrayList();

        Criteria crit = new Criteria();
        crit.addEqualTo("allArticlesInGroup.productGroupId", new Integer(5));

        Query q = QueryFactory.newQuery(ProductGroup.class, crit, true);

        Iterator iter = broker.getIteratorByQuery(q);
        while(iter.hasNext())
        {
            list.add(iter.next());
        }

        // ProductGroup 5
        assertEquals("check size", 1, list.size());
    }

    /**
     * Test pathExpression and Extents
     */
    public void testPathExpressionForExtents2()
    {
        ArrayList list = new java.util.ArrayList();

        Criteria crit = new Criteria();
        crit.addLike("upper(allArticlesInGroup.articleName)", "F%");

        Query q = QueryFactory.newQuery(ProductGroup.class, crit, true);

        Iterator iter = broker.getIteratorByQuery(q);
        while(iter.hasNext())
        {
            list.add(iter.next());
        }

        // ProductGroups 4 and 5
        assertEquals("check size", 2, list.size());

    }

    /**
     * Test pathExpression and Extents musicians is only defined in CD
     */
    public void testPathExpressionForExtents3()
    {
        ArrayList list = new java.util.ArrayList();

        Criteria crit = new Criteria();
        crit.addLike("allArticlesInGroup.musicians", "%");

        QueryByCriteria q = QueryFactory.newQuery(ProductGroup.class, crit, true);
        q.setPathClass("allArticlesInGroup", CdArticle.class);

        Iterator iter = broker.getIteratorByQuery(q);
        while(iter.hasNext())
        {
            list.add(iter.next());
        }

        // ProductGroups 5
        assertEquals("check size", 1, list.size());
    }

    /**
     * Test pathExpression and Extents Abstract Base
     */
    public void testPathExpressionForExtents4()
    {
        ArrayList list = new java.util.ArrayList();

        Criteria crit = new Criteria();
        crit.addEqualTo("allArticlesInGroup.productGroupId", new Integer(5));

        Query q = QueryFactory.newQuery(AbstractProductGroup.class, crit, true);

        Iterator iter = broker.getIteratorByQuery(q);
        while(iter.hasNext())
        {
            list.add(iter.next());
        }

        // ProductGroup 5
        assertEquals("check size", 1, list.size());
    }

    /**
     * Test pathExpression and Extents using Alias
     */
    public void testPathExpressionForExtentsAlias()
    {
        ArrayList list = new java.util.ArrayList();

        Criteria crit1 = new Criteria();
        crit1.setAlias("NAMES");
        crit1.addLike("upper(allArticlesInGroup.articleName)", "F%");

        Criteria crit2 = new Criteria();
        crit2.addGreaterOrEqualThan("allArticlesInGroup.stock", new Integer(110));

        crit1.addAndCriteria(crit2);
        Query q = QueryFactory.newQuery(ProductGroup.class, crit1, true);

        Iterator iter = broker.getIteratorByQuery(q);
        while(iter.hasNext())
        {
            list.add(iter.next());
        }

        // ProductGroup 4
        assertEquals("check size", 1, list.size());
    }

    /**
     * Test ReportQuery and Alias
     */
    public void testReportQueryAlias()
    {
        ArrayList list = new java.util.ArrayList();

        Criteria crit1 = new Criteria();
        crit1.setAlias("NAMES");
        crit1.addLike("upper(allArticlesInGroup.articleName)", "F%");

        Criteria crit2 = new Criteria();
        crit2.setAlias("STOCKS");
        crit2.addGreaterOrEqualThan("allArticlesInGroup.stock", new Integer(110));

        crit1.addAndCriteria(crit2);
        ReportQueryByCriteria q = QueryFactory.newReportQuery(ProductGroup.class, crit1);
        q.setAttributes(new String[]{"groupId", "groupName", "STOCKS.allArticlesInGroup.articleName",
                                     "NAMES.allArticlesInGroup.articleName", "NAMES.allArticlesInGroup.stock"});

        // Due to AliasPrefixes ArticleName is taken from A2 and A1, Stock from A1,
        // SELECT A0.Kategorie_Nr,A0.KategorieName,A2.Artikelname,A1.Artikelname,A1.Lagerbestand FROM

        Iterator iter = broker.getReportQueryIteratorByQuery(q);
        while(iter.hasNext())
        {
            list.add(iter.next());
        }

        // ProductGroup 4 with it's Articles
        assertEquals("check size", 1, list.size());
    }

    /**
     * Run a query range test that includes one record less than the total
     * number of records that exist.
     */
    public void testQueryRangeOneLessThanTotal()
    {
        this.runQueryRangeTest(-1);
    }

    /**
     * Run a query range test that includes all of the records that exist.
     */
    public void testQueryRangeAllRecords()
    {
        this.runQueryRangeTest(0);
    }

    /**
     * Run a query range test.
     */
    public void testQueryRangeMassTest()
    {
        String name = "testQueryRangeMassTest_" + System.currentTimeMillis();
        int objCount = 2000;

        broker.beginTransaction();
        for(int i = 0; i < objCount; i++)
        {
            Gourmet a = new Gourmet();
            a.setName(name);
            broker.store(a);
        }
        broker.commitTransaction();

        Criteria crit = new Criteria();
        crit.addEqualTo("name", name);
        QueryByCriteria q = QueryFactory.newQuery(Gourmet.class, crit);
        q.setStartAtIndex(100);
        q.setEndAtIndex(109);

        StopWatch watch = new StopWatch();
        watch.start();
        Collection c = broker.getCollectionByQuery(q);
        watch.stop();
        System.out.println("# Query 10 of " + objCount + " objects take " + watch.getTime() + " ms");
        assertNotNull(c);
        List result = new ArrayList(c);
        assertEquals(10, result.size());

        crit = new Criteria();
        crit.addEqualTo("name", name);
        q = QueryFactory.newQuery(Gourmet.class, crit);
        watch.reset();
        watch.start();
        c = broker.getCollectionByQuery(q);
        watch.stop();
        System.out.println("# Query all " + objCount + " objects take " + watch.getTime() + " ms");
        assertNotNull(c);
        result = new ArrayList(c);
        assertEquals(objCount, result.size());

        broker.beginTransaction();
        for(int i = 0; i < result.size(); i++)
        {
            broker.delete(result.get(i));
        }
        broker.commitTransaction();

        c = broker.getCollectionByQuery(q);
        assertNotNull(c);
        result = new ArrayList(c);
        assertEquals(0, result.size());
    }

    /**
     * Run a query range test that includes one record more than the total
     * number of records that exist.
     */
    public void testQueryRangeOneMoreThanTotal()
    {
        this.runQueryRangeTest(+1);
    }

    /**
     * Run a query range test.
     *
     * @param delta the amount to add to the existing record count when setting
     *              the start/end index for the query that we'll use.
     */
    private void runQueryRangeTest(int delta)
    {

        // How many records are there in the table?
        Query countQuery = QueryFactory.newQuery(ProductGroup.class, null, false);

        // Get the existing record count
        int recordCount = broker.getCollectionByQuery(countQuery).toArray().length;

        // Build a query that will get the range we're looking for.
        Query listQuery = QueryFactory.newQuery(ProductGroup.class, null, false);
        listQuery.setStartAtIndex(1);
        listQuery.setEndAtIndex(recordCount + delta);

        // Get the list.
        Object[] theObjects = broker.getCollectionByQuery(listQuery).toArray();

        // Verify the record count
        if(delta <= 0)
        {
            assertEquals("record count", (recordCount + delta), theObjects.length);
        }
        else
        {
            assertEquals("record count", recordCount, theObjects.length);
        }

        // Verify the query size, fullSize is 0
        // assertEquals("Query size", recordCount, listQuery.fullSize());
    }

    public void testQueryMN_Alias1() throws Exception
    {
        Criteria crit1 = new Criteria();
        Criteria crit2 = new Criteria();
        QueryByCriteria q;
        Collection result;

        broker.clearCache();

        // get persons working for projects OJB and SODA
        crit1.addLike("projects.title", "OJB%");
        crit1.setAlias("alias1");
        crit2.addLike("projects.title", "SODA%");
        crit2.setAlias("alias2");
        crit1.addAndCriteria(crit2);

        q = QueryFactory.newQuery(Person.class, crit1, true);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertTrue(result.size() == 2); // bob, tom
    }

    public void testQueryMN_Alias2() throws Exception
    {
        Criteria crit1 = new Criteria();
        Criteria crit2 = new Criteria();
        QueryByCriteria q;
        Collection result1;
        Collection result2;

        broker.clearCache();

        // get persons working for projects OJB (alias should have no effect)
        crit1.setAlias("myAlias");
        crit1.addLike("firstname", "%o%");
        crit1.addLike("projects.title", "OJB%");

        q = QueryFactory.newQuery(Person.class, crit1, true);
        result1 = broker.getCollectionByQuery(q);
        assertNotNull(result1);
        assertTrue(result1.size() == 2); // bob, tom

        // WITHOUT ALIAS
        // get persons working for projects OJB
        crit2.addLike("firstname", "%o%");
        crit2.addLike("projects.title", "OJB%");

        q = QueryFactory.newQuery(Person.class, crit2, true);
        result2 = broker.getCollectionByQuery(q);
        assertNotNull(result2);
        assertTrue(result2.size() == 2); // bob, tom

    }

    public void testQueryMN() throws Exception
    {
        Criteria crit1 = new Criteria();
        Criteria crit2 = new Criteria();
        QueryByCriteria q;
        Collection result;

        broker.clearCache();

        // get persons working for projects OJB _or_ SODA
        crit1.addLike("projects.title", "OJB%");
        crit2.addLike("projects.title", "SODA%");
        crit1.addOrCriteria(crit2);

        q = QueryFactory.newQuery(Person.class, crit1, true);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);
        assertTrue(result.size() == 5); // bob, tom, cindy, albert ,betrand
    }

    public void testQueryCommutative12() throws Exception
    {
        Collection result;
        Criteria crit1 = new Criteria();
        crit1.addEqualTo("articleName", "Hamlet");
        crit1.addEqualTo("productGroup.description", "Strange Books...");

        Criteria crit2 = new Criteria();
        crit2.addEqualTo("stock", new Integer(32));

        Criteria crit3 = new Criteria();
        crit3.addEqualTo("stock", new Integer(42));

        crit2.addOrCriteria(crit3);
        crit1.addAndCriteria(crit2);
        QueryByCriteria qry12 = QueryFactory.newQuery(Article.class, crit1);
        qry12.setPathOuterJoin("productGroup");

        result = broker.getCollectionByQuery(qry12);
        assertNotNull(result);

        int count = broker.getCount(qry12);
        assertEquals(count, result.size());
    }


    public void testQueryCommutative21()
    {
        Collection result;
        Criteria crit1 = new Criteria();
        crit1.addEqualTo("articleName", "Hamlet");
        crit1.addEqualTo("productGroup.description", "Strange Books...");

        Criteria crit2 = new Criteria();
        crit2.addEqualTo("stock", new Integer(32));

        Criteria crit3 = new Criteria();
        crit3.addEqualTo("stock", new Integer(42));

        crit2.addOrCriteria(crit3);
        crit2.addAndCriteria(crit1);
        QueryByCriteria qry21 = QueryFactory.newQuery(Article.class, crit2);
        qry21.setPathOuterJoin("productGroup");

        result = broker.getCollectionByQuery(qry21);
        assertNotNull(result);

        int count = broker.getCount(qry21);
        assertEquals(count, result.size());
    }

    public void testOuterJoin()
    {
        Article a = new Article();
        a.articleName = "Good stuff";
        a.productGroup = null;
        broker.beginTransaction();
        broker.store(a);
        broker.commitTransaction();
        
        Criteria crit = new Criteria();
        crit.addLike("articleName", "G%");
        crit.addIsNull("productGroup.description");
        
        QueryByCriteria qry1 = QueryFactory.newQuery(Article.class, crit);
        Collection result1 = broker.getCollectionByQuery(qry1);

        QueryByCriteria qry2 = QueryFactory.newQuery(Article.class, crit);
        qry2.setPathOuterJoin("productGroup");
        Collection result2 = broker.getCollectionByQuery(qry2);
        
        assertEquals(0, result1.size());
        assertEquals(1, result2.size());
        
        broker.beginTransaction();
        broker.delete(a);
        broker.commitTransaction();
    }
    
    public void testExtentByInterface()
    {
        String name = "testExtentByInterface_" + System.currentTimeMillis();
        Zoo zoo = new Zoo();
        zoo.setName(name);
        Mammal m1 = new Mammal();
        m1.setName(name);
        Mammal m2 = new Mammal();
        m2.setName(name);
        Reptile r1 = new Reptile();
        r1.setName(name);
        broker.beginTransaction();
        broker.store(zoo);
        m1.setZooId(zoo.getZooId());
        m2.setZooId(zoo.getZooId());
        r1.setZooId(zoo.getZooId());
        broker.store(m1);
        broker.store(m2);
        broker.store(r1);
        broker.commitTransaction();

        Criteria crit = new Criteria();
        crit.addEqualTo("name", name);

        QueryByCriteria queryAnimals = QueryFactory.newQuery(InterfaceAnimal.class, crit);
        QueryByCriteria queryReptile = QueryFactory.newQuery(Reptile.class, crit);
        QueryByCriteria queryMammal = QueryFactory.newQuery(Mammal.class, crit);

        Collection resultA = broker.getCollectionByQuery(queryAnimals);
        Collection resultR = broker.getCollectionByQuery(queryReptile);
        Collection resultM = broker.getCollectionByQuery(queryMammal);

        assertEquals(3, resultA.size());
        assertEquals(1, resultR.size());
        assertEquals(2, resultM.size());

        for(Iterator iterator = resultA.iterator(); iterator.hasNext();)
        {
            InterfaceAnimal a = (InterfaceAnimal) iterator.next();
            assertEquals(name, a.getName());

        }
        for(Iterator iterator = resultR.iterator(); iterator.hasNext();)
        {
            Reptile a = (Reptile) iterator.next();
            assertEquals(name, a.getName());
        }
        for(Iterator iterator = resultM.iterator(); iterator.hasNext();)
        {
            Mammal a = (Mammal) iterator.next();
            assertEquals(name, a.getName());
        }

        Reptile reptile = (Reptile) broker.getObjectByQuery(queryReptile);
        Mammal mammal = (Mammal) broker.getObjectByQuery(queryMammal);
        assertNotNull(reptile);
        assertNotNull(mammal);
    }

}
