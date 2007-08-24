package org.apache.ojb.broker;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.metadata.ConnectionRepository;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.sql.Connection;
import java.util.Collection;

/**
 * Test to check support for multiple DB.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 */
public class MultipleDBTest extends TestCase
{
    private static final String REFERENCE_NAME = "Test reference";

    public MultipleDBTest(String s)
    {
        super(s);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        MetadataManager mm = MetadataManager.getInstance();
        JdbcConnectionDescriptor jcd = mm.connectionRepository().getDescriptor(TestHelper.FAR_AWAY_KEY);
        if(jcd == null)
        {
            ConnectionRepository cr = mm.readConnectionRepository(TestHelper.FAR_AWAY_CONNECTION_REPOSITORY);
            mm.connectionRepository().addDescriptor(cr.getDescriptor(TestHelper.FAR_AWAY_KEY));
        }
    }

    protected void tearDown() throws Exception
    {
        MetadataManager mm = MetadataManager.getInstance();
        JdbcConnectionDescriptor jcd = mm.connectionRepository().getDescriptor(TestHelper.FAR_AWAY_KEY);
        mm.connectionRepository().removeDescriptor(jcd);
        super.tearDown();
    }

    public static void main(String[] args)
    {
        String[] arr = {MultipleDBTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**
     * This test show how it is possible to materialize an object
     * with a collection of objects retrieved from a different DB
     * NOTE: This is not a recommended design, but it works. 
     */
    public void testMaterializeFromDifferentDB()
    {
        String name = "testMaterializeFromDifferentDB" + System.currentTimeMillis();
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        PersistenceBroker brokerFarAway = PersistenceBrokerFactory.createPersistenceBroker(TestHelper.FAR_AWAY_KEY);

        MultipleObject obj = new MultipleObject();
        obj.setName(name);
        broker.beginTransaction();
        broker.store(obj);
        broker.commitTransaction();

        Identity oid = new Identity(obj, broker);

        MultipleObjectRef ref_1 = new MultipleObjectRef();
        MultipleObjectRef ref_2 = new MultipleObjectRef();
        ref_1.setName(name);
        ref_1.setRefId(obj.getId());
        ref_2.setName(name);
        ref_2.setRefId(obj.getId());

        brokerFarAway.beginTransaction();
        brokerFarAway.store(ref_1);
        brokerFarAway.store(ref_2);
        brokerFarAway.commitTransaction();

        broker.clearCache();
        brokerFarAway.clearCache();

        MultipleObject newObj = (MultipleObject)broker.getObjectByIdentity(oid);
        brokerFarAway.retrieveAllReferences(newObj);

        assertNotNull(newObj.getReferences());
        assertEquals(2, newObj.getReferences().size());
        // System.out.println("## " + newObj);
    }

    /**
     * test PB instance lookup using different
     * PBKey constructors + databases
     */
    public void testLookupByPBKey()
    {
        PBKey pb_1a = new PBKey(TestHelper.DEF_JCD_ALIAS);
        PBKey pb_1b = new PBKey(TestHelper.DEF_JCD_ALIAS, null, null);
        PBKey pb_1c = new PBKey(TestHelper.DEF_JCD_ALIAS, TestHelper.DEF_USER, TestHelper.DEF_PASSWORD);

        PBKey pb_2a = new PBKey(TestHelper.FAR_AWAY_JCD_ALIAS);
        PBKey pb_2b = new PBKey(TestHelper.FAR_AWAY_JCD_ALIAS, null, null);

        PersistenceBroker b1a = PersistenceBrokerFactory.createPersistenceBroker(pb_1a);
        PersistenceBroker b1b = PersistenceBrokerFactory.createPersistenceBroker(pb_1b);
        PersistenceBroker b1c = PersistenceBrokerFactory.createPersistenceBroker(pb_1c);
        PersistenceBroker b2a = PersistenceBrokerFactory.createPersistenceBroker(pb_2a);
        PersistenceBroker b2b = PersistenceBrokerFactory.createPersistenceBroker(pb_2b);

        assertNotNull(b1a);
        assertNotNull(b1b);
        assertNotNull(b1c);
        assertNotNull(b2a);
        assertNotNull(b2b);

        if(b1a != null) b1a.close();
        if(b1b != null) b1b.close();
        if(b1c != null) b1c.close();
        if(b2a != null) b2a.close();
        if(b2b != null) b2b.close();
    }

    public void testPBLookupConnection() throws Exception
    {
        PBKey key = new PBKey(TestHelper.FAR_AWAY_JCD_ALIAS);
        PersistenceBroker broker = PersistenceBrokerFactory.createPersistenceBroker(key);
        // get connection to check lookup
        Connection con = broker.serviceConnectionManager().getConnection();
        con.isClosed();
        assertNotNull(broker);
        assertEquals(key.getAlias(), broker.getPBKey().getAlias());
        broker.close();

        key = new PBKey(TestHelper.DEF_JCD_ALIAS);
        broker = PersistenceBrokerFactory.createPersistenceBroker(key);
        // get connection to check lookup
        con = broker.serviceConnectionManager().getConnection();
        con.isClosed();
        assertNotNull(broker);
        assertEquals(key.getAlias(), broker.getPBKey().getAlias());
        broker.close();
    }

    public void testPBCreation() throws Exception
    {
        PersistenceBroker defPB = null;
        PersistenceBroker secPB = null;

        try
        {
            defPB = PersistenceBrokerFactory.defaultPersistenceBroker();
            PBKey secKey = TestHelper.FAR_AWAY_KEY;
            secPB = PersistenceBrokerFactory.createPersistenceBroker(secKey);

            Assert.assertNotNull("Cannot lookup default PB", defPB);
            Assert.assertNotNull("Cannot lookup PB for PBKey: " + secKey, secPB);
            Assert.assertEquals("Different repository files for second db",
                    secPB.getPBKey().getAlias(), secKey.getAlias());
        }
        finally
        {
            if (defPB != null) defPB.close();
            if (secPB != null) secPB.close();
        }

    }

    /**
     * Insert/delete the same object with given id in two different DB
     */
    public void testInsertDeleteNoAutoSequence() throws Exception
    {
        Article article = createArticleWithId(Integer.MAX_VALUE - 1001);
        PBKey secKey = TestHelper.FAR_AWAY_KEY;

        FarAwayClass fa = createFarAwayObjectWithId(Integer.MAX_VALUE - 1002);
        PersistenceBroker secPB = PersistenceBrokerFactory.createPersistenceBroker(secKey);
        secPB.beginTransaction();
        secPB.store(fa);
        secPB.commitTransaction();
        secPB.close();

        PersistenceBroker defPB = PersistenceBrokerFactory.defaultPersistenceBroker();
        defPB.beginTransaction();
        defPB.store(article);
        defPB.commitTransaction();
        defPB.close();



        secPB = PersistenceBrokerFactory.createPersistenceBroker(secKey);
        secPB.clearCache();
        Object[] pks = {new Integer(fa.getId())};
        Identity oid = new Identity(FarAwayClass.class, FarAwayClass.class, pks);
        FarAwayClass fa2 = (FarAwayClass) secPB.getObjectByIdentity(oid);
        Assert.assertNotNull("Lookup for article in second DB failed", fa2);
        Assert.assertEquals(fa.toString(), fa2.toString());
        secPB.close();

        defPB = PersistenceBrokerFactory.defaultPersistenceBroker();
        defPB.clearCache();
        Identity oid2 = defPB.serviceIdentity().buildIdentity(Article.class, article.getArticleId());
        Article article2 = (Article) defPB.getObjectByIdentity(oid2);
        Assert.assertNotNull("Lookup for article in default DB failed", article2);
        defPB.close();

        secPB = PersistenceBrokerFactory.createPersistenceBroker(secKey);
        secPB.beginTransaction();
        secPB.delete(fa);
        secPB.commitTransaction();
        secPB.close();

        defPB = PersistenceBrokerFactory.defaultPersistenceBroker();
        defPB.beginTransaction();
        defPB.delete(article);
        defPB.commitTransaction();
        defPB.close();
    }

    /**
     * Insert/delete objects in two different DB, use auto-generated keys
     */
    public void testInsertDeleteAutoSequenceClearCache() throws Exception
    {
        Article article = createArticle();
        PBKey secKey = TestHelper.FAR_AWAY_KEY;

        FarAwayClass fa = createFarAwayObject();
        PersistenceBroker farAwayPB = PersistenceBrokerFactory.createPersistenceBroker(secKey);
        farAwayPB.clearCache();
        farAwayPB.beginTransaction();
        farAwayPB.store(fa);
        farAwayPB.commitTransaction();
        farAwayPB.close();

        PersistenceBroker defaultPB = PersistenceBrokerFactory.defaultPersistenceBroker();
        defaultPB.clearCache();
        defaultPB.beginTransaction();
        defaultPB.store(article);
        defaultPB.commitTransaction();
        defaultPB.close();

        farAwayPB = PersistenceBrokerFactory.createPersistenceBroker(secKey);
        farAwayPB.clearCache();
        Object[] pks = {new Integer(fa.getId())};
        Identity oid = new Identity(FarAwayClass.class, FarAwayClass.class, pks);
        FarAwayClass fa2 = (FarAwayClass) farAwayPB.getObjectByIdentity(oid);
        Assert.assertNotNull("Lookup for article in second DB failed", fa2);
        Assert.assertEquals(fa.toString(), fa2.toString());
        farAwayPB.close();

        defaultPB = PersistenceBrokerFactory.defaultPersistenceBroker();
        defaultPB.clearCache();
        Identity oid2 = defaultPB.serviceIdentity().buildIdentity(Article.class, article.getArticleId());
        Article article2 = (Article) defaultPB.getObjectByIdentity(oid2);
        Assert.assertNotNull("Lookup for article in default DB failed", article2);
        defaultPB.close();

        farAwayPB = PersistenceBrokerFactory.createPersistenceBroker(secKey);
        farAwayPB.clearCache();
        farAwayPB.beginTransaction();
        farAwayPB.delete(fa);
        farAwayPB.commitTransaction();
        farAwayPB.close();

        defaultPB = PersistenceBrokerFactory.defaultPersistenceBroker();
        defaultPB.clearCache();
        defaultPB.beginTransaction();
        defaultPB.delete(article);
        defaultPB.commitTransaction();
        defaultPB.close();
    }

    /**
     * Insert/delete objects in two different DB, use auto-generated keys
     */
    public void testInsertDeleteAutoSequence() throws Exception
    {
        Article article = createArticle();
        PBKey secKey = TestHelper.FAR_AWAY_KEY;

        PersistenceBroker defaultPB = PersistenceBrokerFactory.defaultPersistenceBroker();
        defaultPB.beginTransaction();
        defaultPB.store(article);
        defaultPB.commitTransaction();
        defaultPB.close();

        FarAwayClass fa = createFarAwayObject();
        PersistenceBroker farAwayPB = PersistenceBrokerFactory.createPersistenceBroker(secKey);
        farAwayPB.beginTransaction();
        farAwayPB.store(fa);
        farAwayPB.commitTransaction();
        farAwayPB.close();

        farAwayPB = PersistenceBrokerFactory.createPersistenceBroker(secKey);
        Object[] pks = {new Integer(fa.getId())};
        Identity oid = new Identity(FarAwayClass.class, FarAwayClass.class, pks);
        FarAwayClass fa2 = (FarAwayClass) farAwayPB.getObjectByIdentity(oid);
        Assert.assertNotNull("Lookup for article in second DB failed", fa2);
        Assert.assertEquals(fa.toString(), fa2.toString());
        farAwayPB.close();

        defaultPB = PersistenceBrokerFactory.defaultPersistenceBroker();
        Identity oid2 = defaultPB.serviceIdentity().buildIdentity(Article.class, article.getArticleId());
        Article article2 = (Article) defaultPB.getObjectByIdentity(oid2);
        Assert.assertNotNull("Lookup for article in default DB failed", article2);
        defaultPB.close();

        farAwayPB = PersistenceBrokerFactory.createPersistenceBroker(secKey);
        farAwayPB.beginTransaction();
        farAwayPB.delete(fa);
        farAwayPB.commitTransaction();
        farAwayPB.close();

        defaultPB = PersistenceBrokerFactory.defaultPersistenceBroker();
        defaultPB.beginTransaction();
        defaultPB.delete(article);
        defaultPB.commitTransaction();
        defaultPB.close();
    }

    /**
     * tests if references work on second database
     */
    public void testWithReference() throws Exception
    {
        PBKey secKey = TestHelper.FAR_AWAY_KEY;
        FarAwayClass f1 = doReferenceMatchingStore(secKey);
        FarAwayClass f2 = doReferenceMatchingStore(secKey);

        PersistenceBroker broker = null;
        try
        {
            broker = PersistenceBrokerFactory.createPersistenceBroker(secKey);
            broker.clearCache();
            Identity oid = new Identity(f1,broker);
            FarAwayClass fac = (FarAwayClass) broker.getObjectByIdentity(oid);
            FarAwayReferenceIF ref = fac.getReference();
            assertNotNull(ref);
            assertEquals(REFERENCE_NAME, ref.getName());
        }
        finally
        {
            if(broker != null) broker.close();
        }

        doReferenceMatchingDelete(secKey, f1);
        doReferenceMatchingDelete(secKey, f2);
    }

    private FarAwayClass doReferenceMatchingStore(PBKey key) throws Exception
    {
        FarAwayClass fa = createFarAwayObject();
        FarAwayReferenceIF ref = new FarAwayReference();
        ref.setName(REFERENCE_NAME);

        PersistenceBroker farAwayPB = PersistenceBrokerFactory.createPersistenceBroker(key);
        farAwayPB.beginTransaction();
        farAwayPB.store(ref);
        fa.setReference(ref);
        farAwayPB.store(fa);
        farAwayPB.commitTransaction();
        farAwayPB.close();

        farAwayPB = PersistenceBrokerFactory.createPersistenceBroker(key);
        Criteria criteria = new Criteria();
        criteria.addEqualTo("id", new Integer(fa.getId()));
        FarAwayClass result = (FarAwayClass)farAwayPB.getObjectByQuery(
                new QueryByCriteria(FarAwayClass.class, criteria));
        farAwayPB.close();

        int refId = result.getReference().getId();
        assertEquals(ref.getId(), refId);
        return result;
    }

    private void doReferenceMatchingDelete(PBKey key, FarAwayClass farAwayClass) throws Exception
    {
        Integer refId = farAwayClass.getReferenceId();
        Integer mainId = new Integer(farAwayClass.getId());
        PersistenceBroker broker = PersistenceBrokerFactory.createPersistenceBroker(key);
        try
        {
            Criteria criteria = new Criteria();
            criteria.addEqualTo("id", mainId);
            FarAwayClass result = (FarAwayClass) broker.getObjectByQuery(
                    new QueryByCriteria(FarAwayClass.class, criteria));
            assertNotNull("Object not found", result);

            Criteria criteriaRef = new Criteria();
            criteriaRef.addEqualTo("id", refId);
            FarAwayReferenceIF resultRef = (FarAwayReferenceIF) broker.getObjectByQuery(
                    new QueryByCriteria(FarAwayReference.class, criteriaRef));
            assertNotNull("Object not found", result);

            broker.beginTransaction();
            broker.delete(farAwayClass);
            broker.commitTransaction();

            criteria = new Criteria();
            criteria.addEqualTo("id", mainId);
            result = (FarAwayClass) broker.getObjectByQuery(
                    new QueryByCriteria(FarAwayClass.class, criteria));
            assertNull("Object was not deleted", result);

            criteriaRef = new Criteria();
            criteriaRef.addEqualTo("id", refId);
            resultRef = (FarAwayReferenceIF) broker.getObjectByQuery(
                    new QueryByCriteria(FarAwayReference.class, criteriaRef));
            assertNull("Reference object was not deleted", resultRef);
        }
        finally
        {
            if(broker != null) broker.close();
        }
    }


    /**
     * factory method that createa an PerformanceArticle
     * @return the created PerformanceArticle object
     * @param id the primary key value for the new object
     */
    private Article createArticleWithId(int id)
    {
        Article ret = createArticle();
        ret.setArticleId(new Integer(id));
        return ret;
    }

    private FarAwayClass createFarAwayObjectWithId(int id)
    {
        FarAwayClass fa = createFarAwayObject();
        fa.setId(id);
        return fa;
    }

    private FarAwayClass createFarAwayObject()
    {
        FarAwayClass fa = new FarAwayClass();
        fa.setName("away from " + counter);
        fa.setDescription("so far away from " + counter);
        return fa;
    }

    private static int counter;

    /**
     * factory method that createa an PerformanceArticle
     * @return the created PerformanceArticle object
     */
    private Article createArticle()
    {
        Article a = new Article();
        a.setArticleName("New Performance Article " + (++counter));
        a.setMinimumStock(100);
        a.setOrderedUnits(17);
        a.setPrice(0.45);
        a.setStock(234);
        a.setSupplierId(4);
        a.setUnit("bottle");
        return a;
    }

    //***********************************************************************
    // inner classes used by test case
    //***********************************************************************
    public static class MultipleObject
    {
        Integer id;
        String name;
        Collection references;

        public MultipleObject()
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

        public Collection getReferences()
        {
            return references;
        }

        public void setReferences(Collection references)
        {
            this.references = references;
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    public static class MultipleObjectRef
    {
        Integer id;
        Integer refId;
        String name;

        public MultipleObjectRef()
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

        public Integer getRefId()
        {
            return refId;
        }

        public void setRefId(Integer refId)
        {
            this.refId = refId;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE);
        }
    }
}
