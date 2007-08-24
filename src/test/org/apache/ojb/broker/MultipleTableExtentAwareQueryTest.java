package org.apache.ojb.broker;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;

/**
 * Test query against extent classes and abstract base class.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: MultipleTableExtentAwareQueryTest.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class MultipleTableExtentAwareQueryTest extends TestCase
{
    PersistenceBroker broker;

    public MultipleTableExtentAwareQueryTest(String s)
    {
        super(s);
    }

    public void setUp()
    {
        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
    }

    protected void tearDown() throws Exception
    {
        if(broker != null && !broker.isClosed()) broker.close();
    }

    public static void main(String[] args)
    {
        String[] arr = {MultipleTableExtentAwareQueryTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testQueryForBaseClass()
    {
        int objCount = 5;
        String name = "testQueryForBaseClass_"+System.currentTimeMillis();
        broker.beginTransaction();
        for (int i = objCount-1; i >= 0; i--)
        {
            ExtentA a = new ExtentA();
            a.setName(name);
            ExtentB b = new ExtentB();
            b.setName(name);
            broker.store(a);
            broker.store(b);
        }
        broker.commitTransaction();

        Criteria crit = new Criteria();
        crit.addEqualTo("name", name);
        QueryByCriteria query = QueryFactory.newQuery(BaseClass.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertNotNull(result);
        assertEquals("Expect all objects extending 'BaseClass'", 2*objCount, result.size());

        int count = broker.getCount(query);
        assertEquals("Expect all objects extending 'BaseClass'", 2*objCount, count);
    }

    public void testQueryForExtentsOfAbstractClass()
    {
        int objCount = 5;
        String name = "testQueryForExtentsOfAbstractClass_"+System.currentTimeMillis();
        broker.beginTransaction();
        for (int i = objCount-1; i >= 0; i--)
        {
            ExtentA a = new ExtentA();
            a.setName(name);
            ExtentB b = new ExtentB();
            b.setName(name);
            broker.store(a);
            broker.store(b);
        }
        broker.commitTransaction();

        Criteria crit = new Criteria();
        crit.addEqualTo("name", name);
        QueryByCriteria query = QueryFactory.newQuery(ExtentA.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertNotNull(result);
        assertEquals("Wrong number of objects, expect only classes of type 'ExtentA'", objCount, result.size());
        assertTrue("Expect only classes of type 'ExtentA'", result.iterator().next() instanceof ExtentA);

        int count = broker.getCount(query);
        assertNotNull(result);
        assertEquals("Wrong number of objects, expect only classes of type 'ExtentA'", objCount, count);
    }

    public void testQueryForExtentsOfRealClass()
    {
        int objCount = 5;
        String name = "testQueryForExtentsOfRealClass_"+System.currentTimeMillis();
        broker.beginTransaction();
        for (int i = objCount-1; i >= 0; i--)
        {
            ExtentB b = new ExtentB();
            b.setName(name);
            ExtentC c = new ExtentC();
            c.setName(name);
            ExtentD d = new ExtentD();
            d.setName(name);
            broker.store(b);
            broker.store(c);
            broker.store(d);
        }
        broker.commitTransaction();

        Criteria crit = new Criteria();
        crit.addEqualTo("name", name);
        QueryByCriteria query = QueryFactory.newQuery(ExtentD.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertNotNull(result);
        assertEquals("Wrong number of objects, expect only classes of type 'ExtentD'", objCount, result.size());
        assertTrue("Expect only classes of type 'ExtentD'", result.iterator().next() instanceof ExtentD);

        query = QueryFactory.newQuery(ExtentC.class, crit);
        result = broker.getCollectionByQuery(query);
        assertNotNull(result);
        assertEquals("Wrong number of objects, expect only classes of type ExtentC/ExtentD",
                objCount*2, result.size());

        int counterB = 0;
        for (Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Object obj = (ExtentC) iterator.next();
            if(obj instanceof ExtentB) ++counterB;
        }

        query = QueryFactory.newQuery(ExtentB.class, crit);
        result = broker.getCollectionByQuery(query);
        assertNotNull(result);
        assertEquals("Wrong number of objects, expect only classes of type ExtentB/ExtentC/ExtentD",
                objCount*3, result.size());
        counterB = 0;
        int counterC = 0;
        for (Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            Object obj = iterator.next();
            if(obj instanceof ExtentB) ++counterB;
            if(obj instanceof ExtentC) ++counterC;
        }
        assertEquals(15, counterB);
        assertEquals(10, counterC);
        int count = broker.getCount(query);
        assertEquals("Wrong number of objects, expect only classes of type ExtentB/ExtentC/ExtentD",
                objCount*3, count);
    }



    //********************************************************
    // inner classes test objects
    //********************************************************
    public static class ExtentA extends BaseClass
    {
    }

    public static class ExtentB extends BaseClass
    {
    }

    public static class ExtentC extends ExtentB
    {
    }

    public static class ExtentD extends ExtentC
    {
    }

    public abstract static class BaseClass
    {
        private int objId;
        private String name;

        public int getObjId()
        {
            return objId;
        }

        public void setObjId(int objId)
        {
            this.objId = objId;
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
