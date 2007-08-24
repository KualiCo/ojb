package org.apache.ojb.broker;

import java.sql.Statement;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.math.NumberRange;
import org.apache.ojb.broker.accesslayer.OJBIterator;
import org.apache.ojb.broker.core.DelegatingPersistenceBroker;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryByIdentity;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.query.ReportQueryByCriteria;
import org.apache.ojb.broker.util.ObjectModification;
import org.apache.ojb.junit.PBTestCase;

/**
 * Junit test driver for elematary PB tests.
 */
public class PersistenceBrokerTest extends PBTestCase
{
    /**
     * BrokerTests constructor comment.
     * @param name java.lang.String
     */
    public PersistenceBrokerTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        String[] arr = {PersistenceBrokerTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    private boolean checkIdentityEquality(PersistenceBroker p1, PersistenceBroker p2)
    {
        return ((DelegatingPersistenceBroker) p1).getInnermostDelegate()
                == ((DelegatingPersistenceBroker) p2).getInnermostDelegate();
    }

    protected Article createArticle(ProductGroup group, String name)
    {
        Article a = new Article();
        a.setArticleName(name);
        a.setIsSelloutArticle(true);
        a.setMinimumStock(100);
        a.setOrderedUnits(17);
        a.setPrice(0.45);
        if(group != null)
        {
            a.setProductGroup(group);
            group.add(a);
        }
        a.setStock(234);
        a.setSupplierId(4);
        a.setUnit("bottle");
        return a;
    }

    protected CdArticle createCdArticle(ProductGroup group, String name)
    {
        CdArticle a = new CdArticle();
        a.setArticleName(name);
        a.setIsSelloutArticle(true);
        a.setMinimumStock(100);
        a.setOrderedUnits(17);
        a.setPrice(9.95);
        a.setProductGroup(group);
        a.setStock(234);
        a.setSupplierId(4);
        a.setUnit("cd");
        return a;
    }

    protected void deleteArticle(Integer id) throws PersistenceBrokerException
    {
        Article a = new Article();
        a.setArticleId(id);
        deleteArticle(a);
    }

    protected void deleteArticle(Article articleToDelete) throws PersistenceBrokerException
    {
        boolean needsCommit = false;
        if(!broker.isInTransaction())
        {
            broker.beginTransaction();
            needsCommit = true;
        }
        broker.delete(articleToDelete);
        if(needsCommit)
        {
            broker.commitTransaction();
        }
    }

    protected Article readArticleByExample(Integer id) throws PersistenceBrokerException
    {

        Article example = new Article();
        example.setArticleId(id);
        return (Article) broker.getObjectByQuery(QueryFactory.newQuery(example));

    }

    protected Article readArticleByIdentity(Article article) throws PersistenceBrokerException
    {
        return (Article) broker.getObjectByIdentity(broker.serviceIdentity().buildIdentity(article));
    }

    protected Article readArticleByIdentity(Integer id) throws PersistenceBrokerException
    {
        return (Article) broker.getObjectByIdentity(broker.serviceIdentity().buildIdentity(Article.class, id));
    }

    protected void storeArticle(Article anArticle) throws PersistenceBrokerException
    {
        boolean needsCommit = false;
        if(!broker.isInTransaction())
        {
            broker.beginTransaction();
            needsCommit = true;
        }
        broker.store(anArticle);
        if(needsCommit)
        {
            broker.commitTransaction();
        }
    }

     public void testReadUncommitedDataWithinSamePB() throws Exception
    {
        String name = "testReadUncommitedDataWithinSamePB" + System.currentTimeMillis();
        ObjectRepository.Component comp = new ObjectRepository.Component();
        comp.setName(name);

        broker.beginTransaction();
        // store data
        broker.store(comp, ObjectModification.INSERT);
        Query query = new QueryByCriteria(ObjectRepository.Component.class, null);
        // now we try to read the uncommitted data
        Collection all = broker.getCollectionByQuery(query);
        Iterator iter = all.iterator();
        ObjectRepository.Component temp;
        boolean result = false;
        while (iter.hasNext())
        {
            temp = (ObjectRepository.Component) iter.next();
            // System.out.println(temp.getName());
            if(name.equals(temp.getName()))
            {
                result = true;
                break;
            }
        }
        broker.commitTransaction();
        assertTrue("Can't read uncommitted data within same PB instance", result);
    }

    /**
     * PK fields with primitive data types interpret '0' value as
     * 'null' by default. But if we don't use primitive data types and read
     * an object with 0 as PK value and store such an object without
     * changes, nothing should happen.
     */
    public void testNull_0_Complex() throws Exception
    {
        Class objClass = ObjectRepository.E.class;
        ClassDescriptor cld = broker.getClassDescriptor(objClass);
        Integer someOtherValue = new Integer(1111111111);
        String insert = "INSERT INTO TABLE_E VALUES(0,"+someOtherValue.intValue()+")";
        String delete = "DELETE FROM TABLE_E WHERE ID=0";
        Statement stmt;
        try
        {
            broker.beginTransaction();
            // cleanup
            stmt = broker.serviceStatementManager().getGenericStatement(cld, false);
            stmt.executeUpdate(delete);
            broker.serviceStatementManager().closeResources(stmt, null);
            broker.commitTransaction();

            broker.beginTransaction();
            // prepare test
            stmt = broker.serviceStatementManager().getGenericStatement(cld, false);
            // insert object with 0 as PK
            stmt.executeUpdate(insert);
            broker.serviceStatementManager().closeResources(stmt, null);
            broker.commitTransaction();

            // find all objects with 'someSubValue' 111111111
            Criteria crit = new Criteria();
            crit.addEqualTo( "someSuperValue", someOtherValue);
            Query queryAllSubs = new QueryByCriteria(objClass, crit );
            Collection resultBefore = broker.getCollectionByQuery( queryAllSubs );
            int matchesBefore = resultBefore.size();

            // materialize object with 0 PK
            Criteria c = new Criteria();
            c.addEqualTo( "id", new Integer(0));
            Query q = new QueryByCriteria(objClass, c );
            ObjectRepository.E obj = (ObjectRepository.E) broker.getObjectByQuery( q );
            // store the unchanged read object
            broker.beginTransaction();
            broker.store( obj );
            broker.commitTransaction();

            Collection resultAfter = broker.getCollectionByQuery( queryAllSubs );
            int matchesAfter = resultAfter.size();

            assertEquals("We don't store new objects, thus we expect same numbers", matchesBefore, matchesAfter);
        }
        finally
        {
            broker.beginTransaction();
            // cleanup
            stmt = broker.serviceStatementManager().getGenericStatement(cld, false);
            stmt.executeUpdate(delete);
            broker.serviceStatementManager().closeResources(stmt, null);
            broker.commitTransaction();
        }
    }

    /**
     * Object with autoincrement 'true' and a NON primitive
     * data type for the PK field. It should be allowed to set an
     * new object with PK 0, because PK field is not primitive
     */
    public void testNull_0_Complex_2() throws Exception
    {
        Class objClass = ObjectRepository.E.class;
        ClassDescriptor cld = broker.getClassDescriptor(objClass);
        Integer someOtherValue = new Integer(1111111111);
        String delete = "DELETE FROM TABLE_E WHERE ID=0";
        Statement stmt;
        try
        {
            broker.beginTransaction();
            stmt = broker.serviceStatementManager().getGenericStatement(cld, false);
            stmt.executeUpdate(delete);
            broker.serviceStatementManager().closeResources(stmt, null);
            broker.commitTransaction();
            broker.clearCache();

            // find all objects with 'someSubValue' 111111111
            Criteria crit = new Criteria();
            crit.addEqualTo( "someSuperValue", someOtherValue);
            Query queryAllSubs = new QueryByCriteria(objClass, crit );
            Collection resultBefore = broker.getCollectionByQuery( queryAllSubs );
            int matchesBefore = resultBefore.size();

            ObjectRepository.E obj = new ObjectRepository.E();
            obj.setId(new Integer(0));
            obj.setSomeSuperValue(someOtherValue.intValue());
            broker.beginTransaction();
            broker.store( obj );
            broker.commitTransaction();

            broker.clearCache();
            Collection resultAfter = broker.getCollectionByQuery( queryAllSubs );
            int matchesAfter = resultAfter.size();
            assertEquals("We store new object, but was not written to DB", matchesBefore + 1, matchesAfter);
            // lookup object with 0 PK
            Criteria c = new Criteria();
            c.addEqualTo( "id", new Integer(0));
            Query q = new QueryByCriteria(objClass, c );
            obj = (ObjectRepository.E) broker.getObjectByQuery( q );
            assertEquals("We should found object with id 0 for PK field", new Integer(0), obj.getId());
        }
        finally
        {
            broker.beginTransaction();
            // cleanup
            stmt = broker.serviceStatementManager().getGenericStatement(cld, false);
            stmt.executeUpdate(delete);
            broker.serviceStatementManager().closeResources(stmt, null);
            broker.commitTransaction();
        }
    }

    public void testPBF() throws Exception
    {
        // we don't need this
        broker.close();

        PersistenceBroker pb_1 = PersistenceBrokerFactory.defaultPersistenceBroker();
        pb_1.getObjectByQuery(QueryFactory.newQuery(Person.class, (Criteria) null));
        PersistenceBroker pb_2 = PersistenceBrokerFactory.defaultPersistenceBroker();
        pb_2.getObjectByQuery(QueryFactory.newQuery(Person.class, (Criteria) null));
        PersistenceBroker pb_3 = PersistenceBrokerFactory.defaultPersistenceBroker();
        pb_3.getObjectByQuery(QueryFactory.newQuery(Person.class, (Criteria) null));
        pb_1.close();
        pb_2.close();
        pb_3.close();
        PersistenceBrokerFactory.releaseAllInstances();
        PersistenceBroker pbNew = PersistenceBrokerFactory.defaultPersistenceBroker();
        if(pbNew instanceof DelegatingPersistenceBroker)
        {
            if(checkIdentityEquality(pbNew, pb_1)
                    || checkIdentityEquality(pbNew, pb_2)
                    || checkIdentityEquality(pbNew, pb_3))
            {
                fail("Reuse of released PB instance");
            }
        }
        assertFalse(pbNew.isClosed());
        assertFalse(pbNew.isInTransaction());
        pbNew.close();
    }

    /**
     * test the the PB delete() method.
     */
    public void testDelete() throws Exception
    {
        String name = "testDelete_" + System.currentTimeMillis();
        Article a = createArticle(null, name);
        storeArticle(a);
        broker.clearCache();
        Article b = readArticleByIdentity(a);
        assertEquals(
                "after inserting an object it should be equal to its re-read pendant",
                a.getArticleName(),
                b.getArticleName());
        deleteArticle(b);
        b = readArticleByIdentity(a);
        assertNull("should be null after deletion", b);
        b = readArticleByExample(a.getArticleId());
        assertNull("should be null after deletion", b);
    }

    public void testPBisClosed()
    {
        PersistenceBroker pb = PersistenceBrokerFactory.defaultPersistenceBroker();

        assertFalse(pb.isClosed());
        pb.beginTransaction();
        assertTrue(pb.isInTransaction());
        pb.commitTransaction();
        assertFalse(pb.isInTransaction());

        pb.beginTransaction();
        assertTrue(pb.isInTransaction());
        pb.abortTransaction();
        assertFalse(pb.isInTransaction());

        pb.close();
        assertTrue(pb.isClosed());
        assertFalse(pb.isInTransaction());
        try
        {
            pb.beginTransaction();
            fail("We expect an exception, but was not thrown");
        }
        catch (Exception e)
        {
            assertTrue(true);
        }
    }

    public void testLocalTransactionDemarcation()
    {
        PersistenceBroker pb = PersistenceBrokerFactory.defaultPersistenceBroker();

        try
        {
            pb.beginTransaction();
            pb.commitTransaction();
            pb.close();

            pb = PersistenceBrokerFactory.defaultPersistenceBroker();
            pb.beginTransaction();
            pb.abortTransaction();
            pb.abortTransaction();
            pb.close();

            pb = PersistenceBrokerFactory.defaultPersistenceBroker();
            pb.beginTransaction();
            pb.commitTransaction();
            pb.abortTransaction();
            pb.close();

            pb = PersistenceBrokerFactory.defaultPersistenceBroker();
            try
            {
                pb.commitTransaction();
                fail("Commit tx without begin shouldn't be possible");
            }
            catch(TransactionNotInProgressException e)
            {
                assertTrue(true);
            }

            try
            {
                pb.beginTransaction();
                Query q = QueryFactory.newQuery(Article.class, "Select * from NOT_EXIST");
                pb.getObjectByQuery(q);
                pb.commitTransaction();
                fail("Query should fail");
            }
            catch(PersistenceBrokerException e)
            {
                pb.abortTransaction();
                assertTrue(true);
            }
            finally
            {
                pb.close();
            }
        }
        finally
        {
            if(pb != null) pb.close();
        }
    }

    /**
     * test the the PB deleteByQuery() method.
     */
    public void testDeleteByQuery() throws Exception
    {
        String name = "Funny_testDelete_" + System.currentTimeMillis();
        ProductGroup pg;
        pg = new ProductGroup();
        pg.setGroupName(name);

        broker.beginTransaction();
        broker.store(pg);
        broker.commitTransaction();

        Article a = createArticle(pg, name);
        Article b = createArticle(pg, name);
        CdArticle c = createCdArticle(pg, name);

        storeArticle(a);
        storeArticle(b);
        storeArticle(c);

        broker.clearCache();

        Criteria crit = new Criteria();
        crit.addEqualTo("productGroupId", pg.getId());
        crit.addLike("articleName", "%Funny%");
        Query q = new QueryByCriteria(Article.class, crit);

        // 1. check for matching items
        broker.clearCache();
        Collection col = broker.getCollectionByQuery(q);
        assertEquals("There should be 3 matching items", 3, col.size());

        // 2. perform delete by query
        broker.deleteByQuery(q);

        // 3. recheck for matching elements
        col = broker.getCollectionByQuery(q);
        assertEquals("there should be no more matching items", 0, col.size());
    }


    /**
     * performs a test of the inheritance mapping to one table.
     */
    public void testMappingToOneTableWithAbstractBaseClass()
    {
        // first delete all ABs from database
        Collection abs = null;
        Criteria c = null;
        Query q = QueryFactory.newQuery(ObjectRepository.AB.class, c);
        abs = broker.getCollectionByQuery(q);
        broker.beginTransaction();
        if (abs != null)
        {
            Iterator iter = abs.iterator();
            while (iter.hasNext())
            {
                broker.delete(iter.next());
            }
        }
        broker.commitTransaction();

        // Insert 2 A, 1 B and 1 B1
        ObjectRepository.A a1 = new ObjectRepository.A();
        a1.setSomeAField("a A_Field value");
        ObjectRepository.A a2 = new ObjectRepository.A();
        a1.setSomeAField("another A_Field value");

        ObjectRepository.B b1 = new ObjectRepository.B();
        b1.setSomeBField("a B_Field value");
        ObjectRepository.B1 b2 = new ObjectRepository.B1();

        broker.beginTransaction();
        broker.store(a1);
        broker.store(a2);
        broker.store(b1);
        broker.store(b2);
        broker.commitTransaction();

        ObjectRepository.AB ab = null;

        // test retrieval by Identity
        Criteria crit = new Criteria();
        crit.addEqualTo("id", new Integer(a1.getId()));
        q = QueryFactory.newQuery(ObjectRepository.AB.class, crit);
        ab = (ObjectRepository.AB) broker.getObjectByQuery(q);

        assertEquals(ObjectRepository.A.class.getName(), ab.getOjbConcreteClass());
        assertEquals(ObjectRepository.A.class, ab.getClass());

        crit = new Criteria();
        crit.addEqualTo("id", new Integer(b1.getId()));
        q = QueryFactory.newQuery(ObjectRepository.AB.class, crit);
        ab = (ObjectRepository.AB) broker.getObjectByQuery(q);

        assertEquals(ObjectRepository.B.class.getName(), ab.getOjbConcreteClass());
        assertEquals(ObjectRepository.B.class, ab.getClass());

        // test retrieval of collections
        abs = null;
        Criteria selectAll = null;
        q = QueryFactory.newQuery(ObjectRepository.AB.class, selectAll);
        abs = broker.getCollectionByQuery(q);
        assertEquals("collection size", 4, abs.size());
        assertEquals("counted size", 4, broker.getCount(q));

        q = QueryFactory.newQuery(ObjectRepository.A.class, selectAll);
        abs = broker.getCollectionByQuery(q);
        assertEquals("collection size", 2, abs.size());
        assertEquals("counted size", 2, broker.getCount(q));

        q = QueryFactory.newQuery(ObjectRepository.B.class, selectAll);
        abs = broker.getCollectionByQuery(q);
        assertEquals("collection size", 2, abs.size());
        assertEquals("counted size", 2, broker.getCount(q));
    }

    /**
     * performs a test of an extent with one concrete class that uses
     * ojbConcreteClass identifier.
     */
    public void testExtentWithOneConcreteClassWithOjbConcreteClass() throws Exception
    {
        // first delete all ObjectRepository.ABs from database
        Collection as = null;
        Criteria c = null;
        Query q = QueryFactory.newQuery(ObjectRepository.AB.class, c);
        as = broker.getCollectionByQuery(q);
        broker.beginTransaction();
        if (as != null)
        {
            Iterator iter = as.iterator();
            while (iter.hasNext())
            {
                broker.delete(iter.next());
            }
        }
        broker.commitTransaction();

        // Insert 2 ObjectRepository.A
        ObjectRepository.A a1 = new ObjectRepository.A();
        ObjectRepository.A a2 = new ObjectRepository.A();

        broker.beginTransaction();
        broker.store(a1);
        broker.store(a2);
        broker.commitTransaction();

        Criteria selectAll = null;

        q = QueryFactory.newQuery(ObjectRepository.AAlone.class, selectAll);
        as = broker.getCollectionByQuery(q);
        assertEquals("collection size", 2, as.size());
        assertEquals("counted size", 2, broker.getCount(q));
    }

    /**
     * performs a test of the inheritance mapping to one table.
     */
    public void testMappingToOneTable() throws Exception
    {
        // first delete all Cs from database
        Collection cs = null;
        Criteria crit = null;
        Query q = QueryFactory.newQuery(ObjectRepository.C.class, crit);
        cs = broker.getCollectionByQuery(q);
        broker.beginTransaction();
        if (cs != null)
        {
            Iterator iter = cs.iterator();
            while (iter.hasNext())
            {
                broker.delete(iter.next());
            }
        }
        broker.commitTransaction();

        ObjectRepository.C c1 = new ObjectRepository.C();
        ObjectRepository.C c2 = new ObjectRepository.C();
        ObjectRepository.D d1 = new ObjectRepository.D();

        broker.beginTransaction();
        broker.store(c1);
        broker.store(c2);
        broker.store(d1);
        broker.commitTransaction();

        ObjectRepository.C candidate = null;

        // test retrieval by Identity
        crit = new Criteria();
        crit.addEqualTo("id", new Integer(c1.getId()));
        q = QueryFactory.newQuery(ObjectRepository.C.class, crit);
        candidate = (ObjectRepository.C) broker.getObjectByQuery(q);

        assertEquals(ObjectRepository.C.class.getName(), candidate.getOjbConcreteClass());
        assertEquals(ObjectRepository.C.class, candidate.getClass());

        crit = new Criteria();
        crit.addEqualTo("id", new Integer(d1.getId()));
        q = QueryFactory.newQuery(ObjectRepository.C.class, crit);
        candidate = (ObjectRepository.C) broker.getObjectByQuery(q);
        assertEquals(ObjectRepository.D.class.getName(), candidate.getOjbConcreteClass());
        assertEquals(ObjectRepository.D.class, candidate.getClass());

        crit = new Criteria();
        crit.addEqualTo("id", new Integer(d1.getId()));
        q = QueryFactory.newQuery(ObjectRepository.D.class, crit);
        candidate = (ObjectRepository.D) broker.getObjectByQuery(q);
        assertEquals(ObjectRepository.D.class.getName(), candidate.getOjbConcreteClass());
        assertEquals(ObjectRepository.D.class, candidate.getClass());

        // test retrieval of collections
        cs = null;
        Criteria selectAll = null;
        q = QueryFactory.newQuery(ObjectRepository.C.class, selectAll);
        cs = broker.getCollectionByQuery(q);
        assertEquals("collection size", 3, cs.size());
        assertEquals("counted size", 3, broker.getCount(q));

        q = QueryFactory.newQuery(ObjectRepository.D.class, selectAll);
        cs = broker.getCollectionByQuery(q);
        assertEquals("collection size", 1, cs.size());
        assertEquals("counted size", 1, broker.getCount(q));
    }

    /**
     * performs a test to check if metadata can be read
     */
    public void testGetDescriptor() throws Exception
    {
        ClassDescriptor cld = broker.getClassDescriptor(Article.class);
        assertNotNull("classdescriptor should not be null", cld);
    }

    /**
     * tests the FieldConversion facility
     */
    public void testGuidFieldConversion()
    {
        GuidTestEntity gte = new GuidTestEntity();
        broker.beginTransaction();
        broker.store(gte);
        broker.commitTransaction();
        broker.clearCache();

        GuidTestEntity gte1 = (GuidTestEntity) broker.getObjectByIdentity(new Identity(gte, broker));

        assertEquals(gte, gte1);
    }

    /**
     * tests the RowReader mechanism
     */
    public void testRowReader()
    {
        String name = "testRowReader_" + System.currentTimeMillis();
        // a little hack, both classes use the same table, so it's possible
        // to insert Article but read ArticleWithStockDetail
        Article a = createArticle(null, name);
        storeArticle(a);
        Criteria crit = new Criteria();
        crit.addEqualTo("articleId", a.getArticleId());
        Query q = QueryFactory.newQuery(ArticleWithStockDetail.class, crit);

        broker.clearCache();
        ArticleWithStockDetail b = (ArticleWithStockDetail) broker.getObjectByQuery(q);
        StockDetail detail = b.getDetail();
        assertNotNull("detail should be loaded by RowReader !", detail);
        assertEquals(a.getMinimumStock(), detail.getMinimumStock());
        assertEquals(a.getOrderedUnits(), detail.getOrderedUnits());
    }

    public void testEscaping() throws Exception
    {
        String name = "testEscaping_" + System.currentTimeMillis();
        Article a = createArticle(null, name);
        Article b = readArticleByIdentity(a);
        assertNull("should be null after deletion", b);
        a.setArticleName("Single quote 'article_" +name);
        storeArticle(a);
        broker.clearCache();
        b = readArticleByIdentity(a);
        assertEquals(
                "after inserting an object it should be equal to its re-read pendant",
                a.getArticleName(),
                b.getArticleName());

        Collection col;
        Iterator iter;
        String aName = a.getArticleName();
        Criteria criteria = new Criteria();
        criteria.addEqualTo("articleName", aName);
        Query query = QueryFactory.newQuery(InterfaceArticle.class, criteria);
        col = broker.getCollectionByQuery(query);
        iter = col.iterator();
        assertTrue("should have one element", iter.hasNext());
        assertEquals("should be equal", aName, ((InterfaceArticle) iter.next()).getArticleName());
        assertFalse(iter.hasNext());

        a.setArticleName("2 Single quotes 'article'_" + name);
        storeArticle(a);
        broker.clearCache();
        b = readArticleByIdentity(a);
        assertEquals(
                "after inserting an object it should be equal to its re-read pendant",
                a.getArticleName(),
                b.getArticleName());
        aName = a.getArticleName();
        criteria = new Criteria();
        criteria.addEqualTo("articleName", aName);
        query = QueryFactory.newQuery(Article.class, criteria);
        col = broker.getCollectionByQuery(query);
        iter = col.iterator();
        assertTrue("should have one element", iter.hasNext());
        assertEquals("should be equal", aName, ((InterfaceArticle) iter.next()).getArticleName());
        assertFalse(iter.hasNext());

        a.setArticleName("double quote \"article_" + name);
        storeArticle(a);
        broker.clearCache();
        b = readArticleByIdentity(a);
        assertEquals(
                "after inserting an object it should be equal to its re-read pendant",
                a.getArticleName(),
                b.getArticleName());
        aName = a.getArticleName();
        criteria = new Criteria();
        criteria.addEqualTo("articleName", aName);
        query = QueryFactory.newQuery(Article.class, criteria);
        col = broker.getCollectionByQuery(query);
        iter = col.iterator();
        assertTrue("should have one element", iter.hasNext());
        assertEquals("should be equal", aName, ((InterfaceArticle) iter.next()).getArticleName());
        //
        a.setArticleName("2 double quotes \"article\"_"+name);
        storeArticle(a);
        broker.clearCache();
        b = readArticleByIdentity(a);
        assertEquals(
                "after inserting an object it should be equal to its re-read pendant",
                a.getArticleName(),
                b.getArticleName());
        aName = a.getArticleName();
        criteria = new Criteria();
        criteria.addEqualTo("articleName", aName);
        query = QueryFactory.newQuery(Article.class, criteria);
        col = broker.getCollectionByQuery(query);
        iter = col.iterator();
        assertTrue("should have one element", iter.hasNext());
        assertEquals("should be equal", aName, ((InterfaceArticle) iter.next()).getArticleName());
        //
        a.setArticleName("a comma thing ,article,_" + name);
        storeArticle(a);
        broker.clearCache();
        b = readArticleByIdentity(a);
        assertEquals(
                "after inserting an object it should be equal to its re-read pendant",
                a.getArticleName(),
                b.getArticleName());
        aName = a.getArticleName();
        criteria = new Criteria();
        criteria.addEqualTo("articleName", aName);
        query = QueryFactory.newQuery(Article.class, criteria);
        col = broker.getCollectionByQuery(query);
        iter = col.iterator();
        assertTrue("should have one element", iter.hasNext());
        assertEquals("should be equal", aName, ((InterfaceArticle) iter.next()).getArticleName());
    }

    public void testGetByExampleAndGetByIdentity() throws Exception
    {
        String name = "testGetByExampleAndGetByIdentity_" + System.currentTimeMillis();
        Article a = createArticle(null, name);
        storeArticle(a);
        broker.clearCache();
        Article b = readArticleByIdentity(a);
        assertEquals(
                "after inserting an object it should be equal to its re-read pendant",
                a.getArticleName(),
                b.getArticleName());
        broker.clearCache();
        Article c = readArticleByExample(a.getArticleId());
        assertEquals(
                "after inserting an object it should be equal to its re-read pendant",
                a.getArticleName(),
                c.getArticleName());
    }

    /**
     * Insert the method's description here.
     * Creation date: (06.12.2000 21:51:22)
     */
    public void testGetCollectionByQuery() throws Exception
    {
        String name = "testGetCollectionByQuery_" + System.currentTimeMillis();

        Criteria criteria = new Criteria();
        criteria.addEqualTo("articleName", name);
        Query query = QueryFactory.newQuery(Article.class, criteria);
        Collection col = broker.getCollectionByQuery(query);
        assertEquals("size of collection should be zero", 0, col.size());
        //2. insert 3 matching items
        Article a1 = createArticle(null, name);
        broker.beginTransaction();
        broker.store(a1);
        Article a2 = createArticle(null, name);
        broker.store(a2);
        Article a3 = createArticle(null, name);
        broker.store(a3);
        broker.commitTransaction();

        // 3. check if all items are found
        broker.clearCache();
        col = broker.getCollectionByQuery(query);
        assertEquals("size of collection should be three", 3, col.size());

        assertEquals("size of count should be three", 3, broker.getCount(query));

        Iterator iter = col.iterator();
        while (iter.hasNext())
        {
            assertEquals("should be same value", name, ((InterfaceArticle) iter.next()).getArticleName());
        }
    }

    public void testGetCollectionByQueryWithStartAndEnd() throws Exception
    {
        String name = "testGetCollectionByQueryWithStartAndEnd_" + System.currentTimeMillis();
        Criteria criteria = new Criteria();
        criteria.addEqualTo("articleName", name);
        //            criteria.addEqualTo("isSelloutArticle", new Boolean(true));
        Query query = QueryFactory.newQuery(Article.class, criteria);
        Collection col = broker.getCollectionByQuery(query);
        assertEquals("size of collection should be zero", 0, col.size());
        //2. insert 5 matching items
        broker.beginTransaction();
        Article a1 = createArticle(null, name);
        broker.store(a1);
        Article a2 = createArticle(null, name);
        broker.store(a2);
        Article a3 = createArticle(null, name);
        broker.store(a3);
        Article a4 = createArticle(null, name);
        broker.store(a4);
        Article a5 = createArticle(null, name);
        broker.store(a5);
        broker.commitTransaction();

        broker.clearCache();
        // 3. set query start and end
        query.setStartAtIndex(2);
        query.setEndAtIndex(5);

        // 4. check if all items are found
        col = broker.getCollectionByQuery(query);
        assertEquals("size of collection should be four", 4, col.size());

        NumberRange range = new NumberRange(a1.getArticleId(), a5.getArticleId());
        Iterator iter = col.iterator();
        while (iter.hasNext())
        {
            InterfaceArticle testIa = (InterfaceArticle) iter.next();
            assertEquals("should be same value", name, testIa.getArticleName());
            Integer id = testIa.getArticleId();
            assertTrue("Id should be a number of the generated articles", range.containsInteger(id));
        }

        // read one item only
        // 1. set query start equals end
        query.setStartAtIndex(4);
        query.setEndAtIndex(4);

        // 2. check if only one item is found
        OJBIterator ojbIter = (OJBIterator)broker.getIteratorByQuery(query);
        assertEquals("size of iterator should be one", 1, ojbIter.size());
        InterfaceArticle test4 = (InterfaceArticle) ojbIter.next();
        ojbIter.releaseDbResources();
        assertTrue("Id should be a number of the generated articles", range.containsInteger(test4.getArticleId()));
    }

    public void testSorting() throws Exception
    {
        String name = "testSorting_" + System.currentTimeMillis();
        Criteria criteria = new Criteria();
        criteria.addEqualTo("articleName", name);
        QueryByCriteria query = QueryFactory.newQuery(Article.class, criteria);
        query.addOrderByDescending("articleId");
        Collection col = broker.getCollectionByQuery(query);
        assertEquals("size of collection should be zero", 0, col.size());

        //2. insert 3 matching items
        broker.beginTransaction();
        Article a1 = createArticle(null, name);
        broker.store(a1);
        Article a2 = createArticle(null, name);
        broker.store(a2);
        Article a3 = createArticle(null, name);
        broker.store(a3);
        broker.commitTransaction();
        // 3. check if all items are found
        col = broker.getCollectionByQuery(query);
        assertEquals("size of collection should be three", 3, col.size());
        Iterator iter = col.iterator();

        assertEquals("should be same value", a3.getArticleId(), ((InterfaceArticle) iter.next()).getArticleId());
        assertEquals("should be same value", a2.getArticleId(), ((InterfaceArticle) iter.next()).getArticleId());
        assertEquals("should be same value", a1.getArticleId(), ((InterfaceArticle) iter.next()).getArticleId());
    }

    /**
     * testing the sorted collections feature.)
     */
    public void testSortedCollectionAttribute()
    {
        String name = "testSortedCollectionAttribute_" + System.currentTimeMillis();
        ProductGroup samplePG = new ProductGroup();
        Article a1_ = createArticle(samplePG, name);
        Article a2_ = createArticle(samplePG, name);
        Article a3_ = createArticle(samplePG, name);
        // auto insert of referenced Article is enabled
        // and aX_ was added to PG
        broker.beginTransaction();
        broker.store(samplePG);
        broker.commitTransaction();
        broker.clearCache();

        InterfaceProductGroup pg = (InterfaceProductGroup) broker.getObjectByQuery(new QueryByIdentity(samplePG));
        List list = pg.getAllArticles();
        assertNotNull(list);
        assertEquals(3, list.size());
        NumberRange range = new NumberRange(a1_.getArticleId(), a3_.getArticleId());
        InterfaceArticle a1 = null;
        InterfaceArticle a2 = null;
        for(int i = 0; i < list.size(); i++)
        {
            a2 = a1;
            a1 =  (InterfaceArticle) list.get(i);
            if(i>0) assertTrue(a1.getArticleId().intValue() < a2.getArticleId().intValue());
            assertTrue(range.containsInteger(a1.getArticleId()));
        }
    }

    /**
     * Test the AutoIncrement facility
     */
    public void testAutoIncrement() throws Exception
    {
        // create new items for a class with autoincrement PK
        ProductGroup pg1 = new ProductGroup();
        // Identity id1 = new Identity(pg1, broker);
        ProductGroup pg2 = new ProductGroup();
        // Identity id2 = new Identity(pg2, broker);
        pg1.setName("AutoIncGroup1");
        pg2.setName("AutoIncGroup2");
        broker.beginTransaction();
        broker.store(pg1);
        broker.store(pg2);
        broker.commitTransaction();
        assertEquals("should have assigned to Integers with diff 1", 1, pg2.getId().intValue() - pg1.getId().intValue());
    }

    /**
     * do a count by report query
     */
    public void testCountByReportQuery() throws Exception
    {
        // 7 articles, 2 books, 3 cds
        Criteria criteria = new Criteria();
        criteria.addEqualTo("productGroupId", new Integer(5));
        ReportQueryByCriteria query = QueryFactory.newReportQuery(Article.class, criteria);
        query.setAttributes(new String[]{"count(*)"});
        Iterator iter = broker.getReportQueryIteratorByQuery(query);
        Object[] row;
        int count = 0;

        while (iter.hasNext())
        {
            row = (Object[]) iter.next();
            count += ((Number) row[0]).intValue();
        }
        assertEquals("Iterator should produce 12 items", 12, count);

        // get count
        count = broker.getCount(query);
        assertEquals("Count should be 12", 12, count);
    }

    public void testMultiKeyCount() throws Exception
    {
        Criteria criteria = new Criteria();
        QueryByCriteria query1 = QueryFactory.newQuery(Role.class, criteria);
        QueryByCriteria query2 = QueryFactory.newQuery(Role.class, criteria,true);
        
        int count1 = broker.getCount(query1);
        int count2 = broker.getCount(query2);
        
        assertEquals("count and count distinct must match", count1, count2);
    }
    
    /**
     * extent aware iterator
     */
    public void testExtentAwareIteratorByQuery() throws Exception
    {
        // 7 articles, 2 books, 3 cds
        Criteria criteria = new Criteria();
        criteria.addEqualTo("productGroupId", new Integer(5));
        Query query = QueryFactory.newQuery(Article.class, criteria);
        ReportQueryByCriteria reportQuery;
        Iterator iter = broker.getIteratorByQuery(query);
        Collection result = new Vector();
        InterfaceArticle article;
        int count;

        while (iter.hasNext())
        {
            article = (InterfaceArticle) iter.next();
            result.add(article);
        }
        assertEquals("Iterator should produce 12 items", 12, result.size());

        // get count
        count = broker.getCount(query);
        assertEquals("Count should be 12", 12, count);

        reportQuery = QueryFactory.newReportQuery(Article.class, criteria);
        reportQuery.setAttributes(new String[]{"count(*)"});
        iter = broker.getReportQueryIteratorByQuery(reportQuery);

        while (iter.hasNext())
        {
            result.add(iter.next());
        }
    }

    public void testGetIteratorByQuery() throws Exception
    {
        String name = "testGetIteratorByQuery_" + System.currentTimeMillis();
        Criteria criteria = new Criteria();
        criteria.addEqualTo("articleName", name);
        Query query = QueryFactory.newQuery(Article.class, criteria);
        Iterator iter = broker.getIteratorByQuery(query);
        assertTrue("size of Iterator should be zero", !iter.hasNext());
        //2. insert 3 matching items
        broker.beginTransaction();
        Article a1 = createArticle(null, name);
        broker.store(a1);
        Article a2 = createArticle(null, name);
        broker.store(a2);
        Article a3 = createArticle(null, name);
        broker.store(a3);
        broker.commitTransaction();

        // 3. check if all items are found
        iter = broker.getIteratorByQuery(query);
        int count = 0;
        while (iter.hasNext())
        {
            count++;
            assertEquals("should be same value", name, ((InterfaceArticle) iter.next()).getArticleName());
        }
        assertEquals("Iterator should produce 3 items", 3, count);
    }

    /**
     * Testing the getIteratorBySQL functionality
     */
    public void testGetIteratorBySQL() throws Exception
    {
        String name = "testGetIteratorBySQL_" + System.currentTimeMillis();
        // prepare test
        ProductGroup pg = new ProductGroup();
        pg.setGroupName(name);
        Article a1_ = createArticle(pg, name);
        Article a2_ = createArticle(pg, name);
        Article a3_ = createArticle(pg, name);
        // auto insert of referenced Article is enabled
        // and aX_ was added to PG
        broker.beginTransaction();
        broker.store(pg);
        broker.commitTransaction();
        broker.clearCache();

        Criteria criteria = new Criteria();
        criteria.addEqualTo("productGroupId", pg.getId());
        Query query = QueryFactory.newQuery(Article.class, criteria);
        Iterator iter1 = broker.getIteratorByQuery(query);
        String sql =
                "SELECT A.Artikel_Nr FROM Artikel A, Kategorien PG"
                + " WHERE A.Kategorie_Nr = PG.Kategorie_Nr"
                + " AND PG.Kategorie_Nr = " + pg.getId();

        Query q2 = QueryFactory.newQuery(Article.class, sql);
        Iterator iter2 = broker.getIteratorByQuery(q2);
        while (iter1.hasNext())
        {
            InterfaceArticle a1 = (InterfaceArticle) iter1.next();
            InterfaceArticle a2 = (InterfaceArticle) iter2.next();
            assertEquals("iterators should return equal objects", a1.getArticleId(), a2.getArticleId());
        }
        assertTrue("iter2 should not contain more items than iter1", !iter2.hasNext());
    }

    /**
     * Testing the getReportQueryIteratorBySQL functionality
     */
    public void testGetReportQueryIteratorBySQL()
    {
        String sql =
                "SELECT * FROM Artikel A, Kategorien PG"
                + " WHERE A.Kategorie_Nr = PG.Kategorie_Nr"
                + " AND PG.Kategorie_Nr = 2";
        Query q = QueryFactory.newQuery(Article.class, sql);

        Iterator iter = broker.getReportQueryIteratorByQuery(q);

        while (iter.hasNext())
        {
            Object[] arr = (Object[]) iter.next();
            for (int i = 0; i < arr.length; i++)
            {
                //System.out.print(arr[i] + ", ");
            }
            //System.out.println();
        }
    }

    public void testGetMultipleIteratorsByQuery() throws Exception
    {
        String name = "testGetIteratorBySQL_" + System.currentTimeMillis();
        // 1. ensure there are 0 items matching the query
        Criteria criteria = new Criteria();
        criteria.addEqualTo("articleName", name);
        Query query = QueryFactory.newQuery(Article.class, criteria);
        Iterator iter = broker.getIteratorByQuery(query);
        assertTrue("size of Iterator should be zero", !iter.hasNext());
        //2. insert 3 matching items
        broker.beginTransaction();
        Article a1 = createArticle(null, name);
        broker.store(a1);
        Article a2 = createArticle(null, name);
        broker.store(a2);
        Article a3 = createArticle(null, name);
        broker.store(a3);
        broker.commitTransaction();

        // 3. return multiple iterators
        Iterator i1 = broker.getIteratorByQuery(query);
        Iterator i2 = broker.getIteratorByQuery(query);
        Iterator i3 = broker.getIteratorByQuery(query);
        // 4. TestThreadsNLocks the iterators
        for (int i = 0; i < 3; i++)
        {
            assertTrue("should have more elements", i3.hasNext());
            assertTrue("should have more elements", i1.hasNext());
            assertTrue("should have more elements", i2.hasNext());
            assertEquals("should be same value", name, ((InterfaceArticle) i2.next()).getArticleName());
            assertEquals("should be same value", name, ((InterfaceArticle) i1.next()).getArticleName());
            assertEquals("should be same value", name, ((InterfaceArticle) i3.next()).getArticleName());
        }
    }

    public void testGetObjectByQuery() throws Exception
    {
        String name = "testGetIteratorBySQL_" + System.currentTimeMillis();
        // ensure article is persistent
        Article a = createArticle(null, name);
        storeArticle(a);

        // build query-by-example and execute
        Query query = QueryFactory.newQuery(a);
        Article b = (Article) broker.getObjectByQuery(query);
        assertEquals(
                "after inserting an object it should be equal to its re-read pendant",
                a.getArticleName(),
                b.getArticleName());

        Article c = readArticleByExample(a.getArticleId());
        assertEquals(
                "after inserting an object it should be equal to its re-read pendant",
                a.getArticleName(),
                c.getArticleName());

        // now TestThreadsNLocks a criteria query
        Criteria crit = new Criteria();
        crit.addEqualTo("articleId", a.getArticleId());
        Query q = QueryFactory.newQuery(Article.class, crit);
        InterfaceArticle d = (InterfaceArticle) broker.getObjectByQuery(q);
        assertEquals(
                "after inserting an object it should be equal to its re-read pendant",
                a.getArticleName(),
                d.getArticleName());
    }

    public void testGetPKEnumerationByConstraints() throws Exception
    {
        String name = "testGetPKEnumerationByConstraints_" + System.currentTimeMillis();
        // 1. ensure there are 0 items matching the query
        Criteria criteria = new Criteria();
        criteria.addEqualTo("articleName", name);
        Query query = QueryFactory.newQuery(Article.class, criteria);
        Enumeration en = ((PersistenceBrokerInternal)broker).getPKEnumerationByQuery(ArticlePrimaryKey.class, query);
        assertTrue("size of collection should be zero", !en.hasMoreElements());

        //2. insert 3 matching items
        broker.beginTransaction();
        Article a1 = createArticle(null, name);
        broker.store(a1);
        Article a2 = createArticle(null, name);
        broker.store(a2);
        Article a3 = createArticle(null, name);
        broker.store(a3);
        broker.commitTransaction();
        // 3. check if all items are found
        en = ((PersistenceBrokerInternal)broker).getPKEnumerationByQuery(ArticlePrimaryKey.class, query);
        int count = 0;
        while (en.hasMoreElements())
        {
            count++;
            Article tmp = readArticleByIdentity(new Integer(((ArticlePrimaryKey) en.nextElement()).id));
            assertEquals("should be same value", name, tmp.getArticleName());
        }
        assertEquals("Iterator should produce 3 items", 3, count);
    }

    public void testInsert() throws Exception
    {
        String name = "testInsert_" + System.currentTimeMillis();
        Article a = createArticle(null, name);
        Article b = readArticleByIdentity(a.getArticleId());
        assertNull("should be null after deletion", b);
        storeArticle(a);
        b = readArticleByIdentity(a);
        assertEquals(
                "after inserting an object it should be equal to its re-read pendant",
                a.getArticleName(),
                b.getArticleName());
        // check if object is not only stored in cache but also in db
        b = null;
        broker.clearCache();
        b = readArticleByIdentity(a.getArticleId());
        assertEquals(
                "after inserting and flushing the cache an object should still be equal to its re-read pendant",
                a.getArticleName(),
                b.getArticleName());
    }

    public void testUpdate() throws Exception
    {
        String name = "testUpdate_" + System.currentTimeMillis();
        Article a = createArticle(null, name);
        storeArticle(a);
        Article b = readArticleByIdentity(a);
        assertEquals(
                "after inserting an object it should be equal to its re-read pendant",
                a.getArticleName(),
                b.getArticleName());
        String newname = "TESTUPDATE_"+name;
        b.setArticleName(newname);
        storeArticle(b);
        b = readArticleByIdentity(a.getArticleId());
        assertEquals("should be equal after update", newname, b.getArticleName());
        // ensure that object is really stored in DB and not only in ObjectCache
        broker.clearCache();
        b = readArticleByIdentity(a.getArticleId());
        assertEquals("should be equal after update and db lookup", newname, b.getArticleName());
    }

    public void testUpdateWithModification() throws Exception
    {
        String name = "testUpdateWithModification_" + System.currentTimeMillis();
        assertFalse("should not be marked for update yet", ObjectModification.INSERT.needsUpdate());
        assertFalse("should not be marked for insert", ObjectModification.UPDATE.needsInsert());
        Article a = createArticle(null, name);

        broker.beginTransaction();
        broker.store(a, ObjectModification.INSERT);
        broker.commitTransaction();

        Article b = readArticleByIdentity(a.getArticleId());
        assertEquals(
                "after inserting an object it should be equal to its re-read pendant",
                a.getArticleName(),
                b.getArticleName());
        String newname = "TESTUPDATE_" + name;
        b.setArticleName(newname);
        broker.beginTransaction();
        broker.store(b, ObjectModification.UPDATE);
        broker.commitTransaction();

        b = null;
        b = readArticleByIdentity(a.getArticleId());
        assertEquals("should be equal after update", newname, b.getArticleName());
    }

    /**
     * test if reference to Proxy is updated
     * @throws Exception
     */
    public void testUpdateReferencedProxy() throws Exception
    {
        String name = "testUpdateReferencedProxy_" + System.currentTimeMillis();
        ProductGroup pg = new ProductGroup();
        pg.setGroupName(name);
        Article b = createArticle(pg, name);
        broker.beginTransaction();
        broker.store(pg);
        // not needed (auto insert of reference), anyway... we do it
        broker.store(b);
        broker.commitTransaction();

        broker.clearCache();
        b = readArticleByIdentity(b.getArticleId());
        InterfaceProductGroup pgb = b.getProductGroup();
        assertEquals("should be equal after update", pg.getId(), pgb.getId());
    }

    public void testChangeFieldsWhileStoringObject()
    {
        long timestamp = System.currentTimeMillis();

        broker.beginTransaction();
        Person p = new Person();
        p.setFirstname("no_1_" + timestamp);
        p.setLastname("no_1_" + timestamp);
        broker.store(p);
        // change fields
        p.setFirstname("no_2_" + timestamp);
        p.setLastname("no_2_" + timestamp);
        // store changed object again
        broker.store(p);
        broker.commitTransaction();

        Identity id = new Identity(p, broker);
        Person result = (Person) broker.getObjectByIdentity(id);
        assertNotNull(result);
        assertEquals("no_2_" + timestamp, result.getFirstname());
        assertEquals("no_2_" + timestamp, result.getLastname());

        /*
        same with cleared cache
        */
        timestamp = System.currentTimeMillis() + 1;
        broker.beginTransaction();
        p = new Person();
        p.setFirstname("no_3_" + timestamp);
        p.setLastname("no_3_" + timestamp);
        broker.store(p);
        broker.clearCache();
        p.setFirstname("no_4_" + timestamp);
        p.setLastname("no_4_" + timestamp);
        broker.store(p);
        broker.commitTransaction();

        broker.clearCache();
        id = new Identity(p, broker);
        broker.clearCache();
        result = (Person) broker.getObjectByIdentity(id);
        assertNotNull(result);
        assertEquals("no_4_" + timestamp, result.getFirstname());
        assertEquals("no_4_" + timestamp, result.getLastname());
    }

    public void testDoubleStore()
    {
        long timestamp = System.currentTimeMillis();

        Person person = new Person();
        person.setFirstname("testDoubleStore_" + timestamp);
        person.setLastname("time_" + timestamp);

        broker.beginTransaction();
        // Identity used to assign PK of object
        Identity oid = new Identity(person, broker);
        Person serializedPerson = (Person) SerializationUtils.clone(person);
        broker.store(person);
        broker.store(person);
        broker.store(serializedPerson);
        broker.commitTransaction();

        Criteria crit = new Criteria();
        crit.addLike("firstName", "testDoubleStore_" + timestamp);
        Query query = QueryFactory.newQuery(Person.class, crit);
        Collection result = broker.getCollectionByQuery(query);

        assertEquals("Expect to find exact 1 object for "+oid, 1, result.size());
    }

    public void testDoubleDelete()
    {
        long timestamp = System.currentTimeMillis();

        Person person = new Person();
        person.setFirstname("testDoubleDelete_" + timestamp);
        person.setLastname("time_" + timestamp);

        broker.beginTransaction();
        // Identity oid = new Identity(person, broker);
        Person serializedPerson = (Person) SerializationUtils.clone(person);
        broker.store(person);
        broker.commitTransaction();

        Criteria crit = new Criteria();
        crit.addLike("firstName", "testDoubleDelete_" + timestamp);
        Query query = QueryFactory.newQuery(Person.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals("Expect to find exact 1 object", 1, result.size());

        broker.beginTransaction();
        broker.delete(person);
        broker.delete(serializedPerson);
        broker.delete(person);
        broker.commitTransaction();

        broker.beginTransaction();
        broker.delete(serializedPerson);
        broker.commitTransaction();

        result = broker.getCollectionByQuery(query);
        assertEquals("Expect to find none objects", 0, result.size());
    }

    /**
     * Test if only one query is executed for each extent.<br>
     * If the same query is run multiple times the result will contain duplicates
     */
    public void testDuplicateExtentQueries()
    {
        Collection result;
        Set set = new HashSet();
        Criteria crit = new Criteria();
        crit.addGreaterThan("articleId",new Integer(70));
        QueryByCriteria qry = new QueryByCriteria(InterfaceArticle.class, crit);

        broker.clearCache();
        result = broker.getCollectionByQuery(qry);
        set.addAll(result);

        assertEquals("Both sizes must be equal", set.size(), result.size());
    }

    /**
     * Size returned by Iterator must be same as size of Collection
     */
    public void testIteratorSize()
    {
        OJBIterator ojbIter;
        Criteria crit;
        QueryByCriteria query;
        int collSize;
        int iterSize;

        crit = new Criteria();
        query = QueryFactory.newQuery(Article.class, crit);

        collSize = broker.getCollectionByQuery(query).size();

        ojbIter = (OJBIterator)broker.getIteratorByQuery(query);
        iterSize = ojbIter.size();

        assertEquals("collSize == iterSize", collSize , iterSize);
        ojbIter.releaseDbResources();
    }

    public void testPaging()
    {
        OJBIterator ojbIter;
        Criteria crit;
        QueryByCriteria query;

        // All Articles index in range
        crit = new Criteria();
        query = QueryFactory.newQuery(Article.class, crit);
        int fullSize = broker.getCollectionByQuery(query).size();

        query.setStartAtIndex(10);
        query.setEndAtIndex(14);
        ojbIter = (OJBIterator)broker.getIteratorByQuery(query);
        assertEquals("index 10 - 14 expecting 5 rows", 5,ojbIter.size());
        ojbIter.releaseDbResources();
    }

    public void testPagingPosition()
    {
        String name = "testPagingPosition_" + System.currentTimeMillis();
        broker.beginTransaction();
        for(int i=1; i<21; i++)
        {
            Article a = createArticle(null, name);
            a.setStock(i);
            broker.store(a);
        }
        broker.commitTransaction();

        OJBIterator ojbIter;
        Criteria crit;
        QueryByCriteria query;
        Collection fullColl, pagedColl;
        InterfaceArticle article;
        
        // All Articles index in range
        crit = new Criteria();
        crit.addEqualTo("articleName", name);
        query = QueryFactory.newQuery(Article.class, crit);
        query.addOrderByAscending("stock");
        fullColl = broker.getCollectionByQuery(query);
        assertEquals(20, fullColl.size());

        // limited query
        query.setStartAtIndex(10);
        query.setEndAtIndex(14);
        pagedColl = broker.getCollectionByQuery(query);
                      
        ojbIter = (OJBIterator)broker.getIteratorByQuery(query);
        
        assertEquals("collection- and iterator-size must match", pagedColl.size(), ojbIter.size());
        assertEquals("index 10 - 14 expecting 5 rows", 5,ojbIter.size());
        
        ojbIter.absolute(2);
        article = (InterfaceArticle)ojbIter.next();
        assertEquals("Article stock=12", article.getStock(), 12);
        
        ojbIter.relative(-1);
        article = (InterfaceArticle)ojbIter.next();
        assertEquals("Article id=12", article.getStock(), 12);

        ojbIter.relative(-1);
        article = (InterfaceArticle)ojbIter.next();
        assertEquals("Article id=12", article.getStock(), 12);
        
        // last
        ojbIter.absolute(12);
        article = (InterfaceArticle)ojbIter.next();
        assertEquals("Article id=15", article.getStock(), 15);

        // first
        ojbIter.absolute(-12);
        article = (InterfaceArticle)ojbIter.next();
        assertEquals("Article id=10", article.getStock(), 10);
        
        ojbIter.releaseDbResources();
    }
    
    public void testPagingIndicesOutOfRange()
    {
        OJBIterator ojbIter;
        Criteria crit;
        QueryByCriteria query;
        int fullSize;

        // All Articles index out of  range
        crit = new Criteria();
        query = QueryFactory.newQuery(Article.class, crit);
        fullSize = broker.getCollectionByQuery(query).size();

        query.setStartAtIndex(fullSize + 5);
        query.setEndAtIndex(fullSize + 14);
        ojbIter = (OJBIterator)broker.getIteratorByQuery(query);
        assertEquals("indices out of range expecting 0 rows", 0,ojbIter.size());
        ojbIter.releaseDbResources();
    }

    public void testPagingEndIndexOutOfRange()
    {
        OJBIterator ojbIter;
        Criteria crit;
        QueryByCriteria query;
        int fullSize;

        // All Articles index out of  range
        crit = new Criteria();
        query = QueryFactory.newQuery(Article.class, crit);
        fullSize = broker.getCollectionByQuery(query).size();

        query.setStartAtIndex(fullSize - 9);
        query.setEndAtIndex(fullSize + 9);
        ojbIter = (OJBIterator)broker.getIteratorByQuery(query);
        assertEquals("end index out of range expecting 10 rows", 10,ojbIter.size());
        ojbIter.releaseDbResources();
    }

    public void testPagingEmptyIterator()
    {
        OJBIterator ojbIter;
        Criteria crit;
        QueryByCriteria query;

        // looking for inexistent Article
        crit = new Criteria();
        crit.addEqualTo("articleId",new Integer(-777));
        query = QueryFactory.newQuery(Article.class, crit);
        int fullSize = broker.getCollectionByQuery(query).size();

        query.setStartAtIndex(10);
        query.setEndAtIndex(14);
        ojbIter = (OJBIterator)broker.getIteratorByQuery(query);
        assertEquals("index 10 - 14 expecting 0 rows for empty iterator", 0,ojbIter.size());
        ojbIter.releaseDbResources();
    }
}
