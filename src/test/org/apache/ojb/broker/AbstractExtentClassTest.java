package org.apache.ojb.broker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;

/**
 *
 *
 * @author <a href="mailto:om@ppi.de">Oliver Matz</a>
 * @version $Id: AbstractExtentClassTest.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class AbstractExtentClassTest extends TestCase
{
    private PersistenceBroker broker;

    public AbstractExtentClassTest(String name)
    {
        super(name);
    }

    public static void main(String args[])
    {
        String[] arr = {AbstractExtentClassTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void setUp() throws Exception
    {
        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
    }

    public void tearDown()
    {
        try
        {
            broker.clearCache();
            broker.close();
        }
        catch (PersistenceBrokerException e)
        {
        }
    }

    /**
     * Used data model:
     * AbstractIF_X <-- AbstractIF_Y <-- ConcreteZ
     * AbstractIF_X <-- AbstractClassX <-- AbstractClassY <-- ConcreteZZ
     */
    public void testStoreRetrieveQueryUsingInterface() throws Exception
    {
        String name = "interface_test_"+System.currentTimeMillis();
        broker.clearCache();
        broker.beginTransaction();
        // create new XContainer with ConcreteZ and ConreteZZ references
        XContainer container = new XContainer();
        container.addX(new ConcreteZ(name));
        container.addX(new ConcreteZ(name));
        // we a set additional field 'zzName' in ConcreteZZ
        container.addX(new ConcreteZZ(name, "ZZ"));
        container.addX(new ConcreteZZ(name, "ZZ"));
        container.addX(new ConcreteZZ(name, "ZZ"));

        broker.store(container);
        broker.commitTransaction();
        broker.clearCache();

        Identity cont = new Identity(container, broker);
        broker.beginTransaction();
        XContainer retContainer = (XContainer) broker.getObjectByIdentity(cont);
        broker.commitTransaction();
        Collection res = retContainer.getXReferences();
        assertNotNull(res);
        assertEquals(5, res.size());
        boolean found = false;
        for (Iterator iterator = res.iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            if(o instanceof ConcreteZZ)
            {
                ConcreteZZ zz = (ConcreteZZ) o;
                assertNotNull(zz.getConcreteZZName());
                assertEquals("ZZ", zz.getConcreteZZName());
                found = true;
            }
        }
        assertTrue("No ConcreteZZ instances be returned",found);

        broker.clearCache();

        // test query base interface
        Criteria crit = new Criteria();
        crit.addLike("name", name);
        Query q = QueryFactory.newQuery(AbstractIF_X.class, crit);
        Collection results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(5, results.size());
        found = false;
        for (Iterator iterator = results.iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            if(o instanceof ConcreteZZ)
            {
                ConcreteZZ zz = (ConcreteZZ) o;
                assertNotNull(zz.getConcreteZZName());
                assertEquals("ZZ", zz.getConcreteZZName());
                found = true;
            }
        }
        assertTrue("No ConcreteZZ instances be returned",found);

        // test query abstract class
        broker.clearCache();
        crit = new Criteria();
        crit.addLike("name", name);
        q = QueryFactory.newQuery(AbstractClassX.class, crit);
        results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(3, results.size());
        for (Iterator iterator = results.iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            ConcreteZZ zz = (ConcreteZZ) o;
            assertNotNull(zz.getConcreteZZName());
            assertEquals("ZZ", zz.getConcreteZZName());
        }

        // test query abstract class
        broker.clearCache();
        crit = new Criteria();
        crit.addLike("name", name);
        q = QueryFactory.newQuery(AbstractClassY.class, crit);
        results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(3, results.size());
        for (Iterator iterator = results.iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            ConcreteZZ zz = (ConcreteZZ) o;
            assertNotNull(zz.getConcreteZZName());
            assertEquals("ZZ", zz.getConcreteZZName());
        }

        // test query extended interface
        broker.clearCache();
        crit = new Criteria();
        crit.addLike("name", name);
        q = QueryFactory.newQuery(AbstractIF_Y.class, crit);
        results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(2, results.size());

        // test query concrete class
        broker.clearCache();
        crit = new Criteria();
        crit.addLike("name", name);
        q = QueryFactory.newQuery(ConcreteZ.class, crit);
        results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(2, results.size());

        // test query concrete class
        broker.clearCache();
        crit = new Criteria();
        crit.addLike("name", name);
        q = QueryFactory.newQuery(ConcreteZZ.class, crit);
        results = broker.getCollectionByQuery(q);
        assertNotNull(results);
        assertEquals(3, results.size());
        for (Iterator iterator = results.iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            ConcreteZZ zz = (ConcreteZZ) o;
            assertNotNull(zz.getConcreteZZName());
            assertEquals("ZZ", zz.getConcreteZZName());
        }
    }



    //******************************************************************
    // inner classes / persistent objects
    //******************************************************************

    public static abstract interface AbstractIF_X
    {
        public int getId();

        public int getContainerId();

        public void setContainerId(int containerId);

        public void setName(String name);

        public String getName();
    }

    public static abstract interface AbstractIF_Y extends AbstractIF_X
    {
    }

    public static class ConcreteZ implements AbstractIF_Y
    {
        private int containerId;
        private int someValue;
        private int id;
        private String name;

        public ConcreteZ()
        {
        }

        public ConcreteZ(String name)
        {
            this.name = name;
        }

        ConcreteZ(int no)
        {
            someValue = no;
        }

        public int getContainerId()
        {
            return containerId;
        }

        public void setContainerId(int containerId)
        {
            this.containerId = containerId;
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
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

        public int getSomeValue()
        {
            return someValue;
        }

        public void setSomeValue(int someValue)
        {
            this.someValue = someValue;
        }

        public String toString()
        {
            return (new ToStringBuilder(this)).
                    append("id", id).
                    append("someValue", someValue).toString();
        }
    }

    public static class XContainer
    {
        private int id;
        private Collection myXReferences;

        public XContainer()
        {
        }

        public XContainer(int id)
        {
            this.id = id;
        }

        // make javabean conform
        public Collection getMyXReferences()
        {
            return myXReferences;
        }

        public void setMyXReferences(Collection myXReferences)
        {
            this.myXReferences = myXReferences;
        }

        public void addX(AbstractIF_X someX)
        {
            if (myXReferences == null) myXReferences = new ArrayList();
            myXReferences.add(someX);
        }

        public Collection getXReferences()
        {
            return myXReferences;
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public String toString()
        {
            return (new ToStringBuilder(this)).
                    append("id", id).
                    append("myXReferences", myXReferences).toString();
        }
    }

    public static abstract class AbstractClassX implements AbstractIF_X
    {
        private int containerId;
        private String name;

        public AbstractClassX()
        {
        }

        public AbstractClassX(String name)
        {
            this.name = name;
        }

        public int getContainerId()
        {
            return containerId;
        }

        public void setContainerId(int containerId)
        {
            this.containerId = containerId;
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

    public static abstract class AbstractClassY extends AbstractClassX
    {
        public AbstractClassY()
        {
        }

        public AbstractClassY(String name)
        {
            super(name);
        }
    }

    public static class ConcreteZZ extends AbstractClassY
    {

        private int someValue;
        private int id;
        private String concreteZZName;

        public ConcreteZZ()
        {
        }

        public ConcreteZZ(String name, String zzName)
        {
            super(name);
            this.concreteZZName = zzName;
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public String getConcreteZZName()
        {
            return concreteZZName;
        }

        public void setConcreteZZName(String concreteZZName)
        {
            this.concreteZZName = concreteZZName;
        }

        public int getSomeValue()
        {
            return someValue;
        }

        public void setSomeValue(int someValue)
        {
            this.someValue = someValue;
        }

        public String toString()
        {
            return (new ToStringBuilder(this)).
                    append("id", id).
                    append("someValue", someValue).toString();
        }
    }
}
