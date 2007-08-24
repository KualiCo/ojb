package org.apache.ojb.broker;

import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

/**
 * Tests for extent aware path expressions
 * 
 * @author <a href="leandro@ibnetwork.com.br">Leandro Rodrigo Saad Cruz</a>
 * 
 * $Id: ExtentAwarePathExpressionsTest.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class ExtentAwarePathExpressionsTest extends PBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {ExtentAwarePathExpressionsTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public ExtentAwarePathExpressionsTest(String testName)
    {
        super(testName);
    }

    public void testWithoutHintClass1() throws Exception
    {
        Criteria criteria = new Criteria();
        criteria.addEqualTo("qualifiers.name", "Cars");
        QueryByCriteria query = new QueryByCriteria(News.class, criteria);

        broker.clearCache();
        List content = (List) broker.getCollectionByQuery(query);
        assertEquals(1, content.size());
    }

    public void testWithoutHintClass2() throws Exception
    {
        Criteria criteria = new Criteria();
        criteria.addLike("qualifiers.name", "%ers%");
        QueryByCriteria query = new QueryByCriteria(BaseContentImpl.class, criteria, true);

        broker.clearCache();
        List content = (List) broker.getCollectionByQuery(query);
        assertEquals(2, content.size());
    }

    public void testNotNullPathElement()
    {
        Criteria criteria = new Criteria();
        criteria.addNotNull("qualifiers.name");
        QueryByCriteria query = new QueryByCriteria(BaseContentImpl.class, criteria, true);
        List content = (List) broker.getCollectionByQuery(query);
        assertEquals(4,content.size());
    }

    public void testSetPathClass()
    {
        Criteria criteria = new Criteria();
        criteria.addNotNull("qualifiers.name");
        QueryByCriteria query = new QueryByCriteria(BaseContentImpl.class, criteria, true);
        query.setPathClass("qualifiers",Category.class);
        List content = (List) broker.getCollectionByQuery(query);
        assertEquals(2,content.size());
        for (Iterator iter = content.iterator(); iter.hasNext();)
        {
            BaseContentImpl element = (BaseContentImpl) iter.next();
            assertTrue(element.getId() <=3 && element.getId() >=2);
        }

    }

    public void testSetPathClassInCriteria()
    {
        Criteria criteria = new Criteria();

        criteria.addNotNull("qualifiers.name");
        criteria.setPathClass("qualifiers", Category.class);

        QueryByCriteria query   = new QueryByCriteria(BaseContentImpl.class, criteria, true);
        List            content = (List)broker.getCollectionByQuery(query);

        assertEquals(2,content.size());
        for (Iterator iter = content.iterator(); iter.hasNext();)
        {
            BaseContentImpl element = (BaseContentImpl) iter.next();

            assertTrue(element.getId() <=3 && element.getId() >=2);
        }

    }

    public void testAddPathClass()
    {
        Criteria criteria = new Criteria();
        criteria.addNotNull("qualifiers.name");
        QueryByCriteria query = new QueryByCriteria(BaseContentImpl.class, criteria, true);
        query.addPathClass("qualifiers",Qualifier.class);
        query.addPathClass("qualifiers",Area.class);
        List content = (List) broker.getCollectionByQuery(query);
        assertEquals(1,content.size());
        assertEquals(10,((Paper)content.get(0)).getId());
    }

    public void testAddPathClassInCriteria()
    {
        Criteria criteria = new Criteria();

        criteria.addNotNull("qualifiers.name");
        criteria.addPathClass("qualifiers", Qualifier.class);
        criteria.addPathClass("qualifiers", Area.class);

        QueryByCriteria query   = new QueryByCriteria(BaseContentImpl.class, criteria, true);
        List            content = (List)broker.getCollectionByQuery(query);

        assertEquals(1, content.size());
        assertEquals(10, ((Paper)content.get(0)).getId());
    }

    /**
     * The order of criteria elements influences the use of parenthesis, 
     *  If we add the Like element before the NotNull one we get the wrong query 
     * Right clause : WHERE WHERE (A2.NAME IS NOT NULL  OR A2E1.NAME IS NOT NULL ) AND A0.HEADLINE LIKE  'Bra%
     * Wrong clause : WHERE (A0.HEADLINE LIKE  'Bra%' ) AND A2.NAME IS NOT NULL  OR A2E1.NAME IS NOT NULL 
     */
    public void testAddPathClasses()
    {
        Criteria criteria = new Criteria();

        criteria.addLike("headline","Bra%");
        criteria.addNotNull("qualifiers.name");
        QueryByCriteria query = new QueryByCriteria(BaseContentImpl.class, criteria, true);
        query.addPathClass("qualifiers",Qualifier.class);
        query.addPathClass("qualifiers",Topic.class);
        query.addPathClass("qualifiers",Category.class);

        List content = (List) broker.getCollectionByQuery(query);
        assertEquals(1,content.size());
        assertEquals(3,((News)content.get(0)).getId());
    }

    public void testAddPathClassesInCriteria()
    {
        Criteria criteria = new Criteria();

        criteria.addLike("headline", "Bra%");
        criteria.addNotNull("qualifiers.name");
        criteria.addPathClass("qualifiers", Qualifier.class);
        criteria.addPathClass("qualifiers", Topic.class);
        criteria.addPathClass("qualifiers", Category.class);

        QueryByCriteria query   = new QueryByCriteria(BaseContentImpl.class, criteria, true);
        List            content = (List)broker.getCollectionByQuery(query);

        assertEquals(1, content.size());
        assertEquals(3, ((News)content.get(0)).getId());
    }

    // Test for OJB-50
    public void testComplexCriteriaWithPathClasses_1()
    {
        Criteria criteria         = new Criteria();
        Criteria categoryCriteria = new Criteria();
        Criteria topicCriteria    = new Criteria();

        topicCriteria.addEqualTo("qualifiers.importance", "important");
        topicCriteria.addPathClass("qualifiers", Topic.class);
        criteria.addOrCriteria(topicCriteria);
        categoryCriteria.addNotNull("qualifiers.description");
        categoryCriteria.addPathClass("qualifiers", Category.class);
        criteria.addOrCriteria(categoryCriteria);

        QueryByCriteria query   = QueryFactory.newQuery(BaseContentImpl.class, criteria, true);
        List            content = (List)broker.getCollectionByQuery(query);

        assertEquals(1, content.size());
        assertEquals(2, ((News)content.get(0)).getId());
    }

    // Test for OJB-50
    public void testComplexCriteriaWithPathClasses_2()
    {
        Criteria criteria         = new Criteria();
        Criteria categoryCriteria = new Criteria();
        Criteria topicCriteria    = new Criteria();

        categoryCriteria.addLike("qualifiers.description", "The%");
        categoryCriteria.addPathClass("qualifiers", Category.class);
        categoryCriteria.addPathClass("qualifiers", TopicExt.class);

        topicCriteria.addEqualTo("qualifiers.importance", "important");
        topicCriteria.addPathClass("qualifiers", Topic.class);
        topicCriteria.addPathClass("qualifiers", TopicExt.class);

        criteria.addOrCriteria(categoryCriteria);
        criteria.addOrCriteria(topicCriteria);

        QueryByCriteria query   = QueryFactory.newQuery(BaseContentImpl.class, criteria, true);
        List            content = (List) broker.getCollectionByQuery(query);

        assertEquals(1, content.size());
        assertEquals(2, ((News)content.get(0)).getId());
        assertNotNull(((News)content.get(0)).getQualifiers());
        assertEquals(3, ((News)content.get(0)).getQualifiers().size());
    }

    // Test for OJB-50
    public void testComplexCriteriaWithPathClasses_3()
    {
        Criteria criteria         = new Criteria();
        Criteria criteriaTopicExtAndCategory         = new Criteria();
        Criteria categoryCriteria = new Criteria();
        Criteria topicCriteria    = new Criteria();
        Criteria topicExtCriteria    = new Criteria();

        categoryCriteria.addLike("qualifiers.description", "The buyer");
        categoryCriteria.addPathClass("qualifiers", Category.class);

        topicExtCriteria.addLike("qualifiers.description", "The buyer");
        topicExtCriteria.addPathClass("qualifiers", TopicExt.class);

        criteriaTopicExtAndCategory.addOrCriteria(categoryCriteria);
        criteriaTopicExtAndCategory.addOrCriteria(topicExtCriteria);

        topicCriteria.addEqualTo("qualifiers.importance", "important");
        topicCriteria.addPathClass("qualifiers", Topic.class);
        topicCriteria.addPathClass("qualifiers", TopicExt.class);

        criteria.addAndCriteria(criteriaTopicExtAndCategory);
        criteria.addAndCriteria(topicCriteria);

        QueryByCriteria query   = QueryFactory.newQuery(BaseContentImpl.class, criteria, true);
        List            content = (List) broker.getCollectionByQuery(query);

        assertEquals(1, content.size());
        assertEquals(2, ((News)content.get(0)).getId());
        assertNotNull(((News)content.get(0)).getQualifiers());
        assertEquals(3, ((News)content.get(0)).getQualifiers().size());
    }

    // Test for OJB-62
    public void testAddClassPathOnQuery_1()
    {
        Criteria criteria = new Criteria();
        criteria.addLike("qualifiers.importance", "impor%");
        QueryByCriteria query = QueryFactory.newQuery(BaseContentImpl.class, criteria, true);
        query.addPathClass("qualifiers", Topic.class);
        List content = (List) broker.getCollectionByQuery(query);

        assertEquals(1, content.size());
        assertEquals(2, ((News)content.get(0)).getId());
        assertNotNull(((News)content.get(0)).getQualifiers());
        assertEquals(3, ((News)content.get(0)).getQualifiers().size());
    }

    // Test for OJB-62
    public void testAddClassPathOnQuery_2()
    {
        Criteria criteria = new Criteria();
        criteria.addLike("qualifiers.importance", "NO_MATCH%");
        QueryByCriteria query = QueryFactory.newQuery(BaseContentImpl.class, criteria, true);
        query.addPathClass("qualifiers", Topic.class);
        List content = (List) broker.getCollectionByQuery(query);

        assertEquals(0, content.size());
    }

    // Test multiple path class on query object
    public void testMultipleClassPath_1a()
    {
        Criteria criteria = new Criteria();
        criteria.addLike("headline", "SAL%");
        criteria.addEqualTo("qualifiers.importance", "unimportant");
        criteria.addEqualTo("qualifiers.name", "Sellers");
        QueryByCriteria query = QueryFactory.newQuery(BaseContentImpl.class, criteria, true);
        query.addPathClass("qualifiers", Qualifier.class);
        query.addPathClass("qualifiers", Topic.class);
        query.addPathClass("qualifiers", TopicExt.class);
        List content = (List) broker.getCollectionByQuery(query);

        assertEquals(1, content.size());
        assertEquals(11, ((Paper)content.get(0)).getId());
        assertNotNull(((Paper)content.get(0)).getQualifiers());
        assertEquals(2, ((Paper)content.get(0)).getQualifiers().size());
    }

    // Test multiple path class on query object
    public void testMultipleClassPath_1b()
    {
        Criteria criteria = new Criteria();
        criteria.addLike("headline", "SAL%");
        criteria.addEqualTo("qualifiers.importance", "unimportant");
        criteria.addEqualTo("qualifiers.name", "Sellers");
        QueryByCriteria query = QueryFactory.newQuery(BaseContentImpl.class, criteria, true);
        //query.addPathClass("qualifiers", Qualifier.class);
        query.addPathClass("qualifiers", Topic.class);
        query.addPathClass("qualifiers", TopicExt.class);
        List content = (List) broker.getCollectionByQuery(query);

        assertEquals(1, content.size());
        assertEquals(11, ((Paper)content.get(0)).getId());
        assertNotNull(((Paper)content.get(0)).getQualifiers());
        assertEquals(2, ((Paper)content.get(0)).getQualifiers().size());
    }

    // Test multiple path class on criteria object
    public void testMultipleClassPath_2a()
    {
        Criteria criteria = new Criteria();
        criteria.addLike("headline", "SAL%");
        criteria.addEqualTo("qualifiers.importance", "unimportant");
        criteria.addEqualTo("qualifiers.name", "Sellers");
        criteria.addPathClass("qualifiers", Qualifier.class);
        criteria.addPathClass("qualifiers", TopicExt.class);
        criteria.addPathClass("qualifiers", Topic.class);
        QueryByCriteria query = QueryFactory.newQuery(BaseContentImpl.class, criteria, true);
        List content = (List) broker.getCollectionByQuery(query);

        assertEquals(1, content.size());
        assertEquals(11, ((Paper)content.get(0)).getId());
        assertNotNull(((Paper)content.get(0)).getQualifiers());
        assertEquals(2, ((Paper)content.get(0)).getQualifiers().size());
    }

    // Test multiple path class on query object
    public void testMultipleClassPath_2b()
    {
        Criteria criteria = new Criteria();
        criteria.addLike("headline", "SAL%");
        criteria.addEqualTo("qualifiers.importance", "unimportant");
        criteria.addEqualTo("qualifiers.name", "Sellers");
        //criteria.addPathClass("qualifiers", Qualifier.class);
        criteria.addPathClass("qualifiers", TopicExt.class);
        criteria.addPathClass("qualifiers", Topic.class);
        QueryByCriteria query = QueryFactory.newQuery(BaseContentImpl.class, criteria, true);
        List content = (List) broker.getCollectionByQuery(query);

        assertEquals(1, content.size());
        assertEquals(11, ((Paper)content.get(0)).getId());
        assertNotNull(((Paper)content.get(0)).getQualifiers());
        assertEquals(2, ((Paper)content.get(0)).getQualifiers().size());
    }
}
