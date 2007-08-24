package org.apache.ojb.broker;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.accesslayer.conversions.ConversionException;
import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author J. Russell Smyth
 * @version $Id: FieldConversionTest_2.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class FieldConversionTest_2 extends PBTestCase
{
    public FieldConversionTest_2(String testName)
    {
        super(testName);
    }

    public static void main(String[] args)
    {
        String[] arr = {FieldConversionTest_2.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testConvertedReferenceLookup()
    {
        Collection coll = null;
        Criteria c = null;
        Query q = QueryFactory.newQuery(ConversionReferrer.class, c);
        coll = broker.getCollectionByQuery(q);
        assertTrue("There should be more than 0 matching items", coll.size() > 0);

        Iterator i = coll.iterator();
        while (i.hasNext())
        {
            ConversionReferrer cref = (ConversionReferrer) i.next();
            assertTrue("PK value should not be converted, id should be < 1000, found " + cref, cref.getPk1() < 1000);
            assertTrue("Reference id should be > 1000, found " + cref, cref.getRef1() > 1000);
            assertTrue("Reference should be non-null, found " + cref, cref.getReferred() != null);
            assertEquals("Reference does not select correct item", cref.getRef1(), cref.getReferred().getPk1());
        }
    }

    public void testMultipleConverted()
    {
        String error = "Indicate that the field conversion was not/or multiple times called for a value, expected > 100 - found ";
        Collection coll = null;
        Criteria c = null;
        Query q = QueryFactory.newQuery(ConversionReferrer.class, c);
        coll = broker.getCollectionByQuery(q);
        assertTrue("There should be more than 0 matching items", coll.size() > 0);

        Iterator i = coll.iterator();
        while (i.hasNext())
        {
            ConversionReferrer cref = (ConversionReferrer) i.next();
            assertTrue("PK value should not be converted, id should be < 1000, found " + cref, cref.getPk1() < 1000);
            assertTrue("Reference should be non-null, found " + cref, cref.getReferred() != null);
            assertEquals("Reference selected incorrect item", cref.getRef1(), cref.getReferred().getPk1());

            /*
            The used conversion does the following
            val = Integer.MAX_VALUE - val;
            for both conversion directions.
            The result was e.g.
            sqlToJava: 10 --> 2147483637
            javaToSql: 2147483637 --> 10

            */

            int value = 0;
            value = cref.getRef1();
            assertTrue(error + cref, value > 1000);

            value = cref.getTestId();
            assertTrue(error + cref, value > 1000);

            value = cref.getReferred().getPk1();
            assertTrue(error + cref, value > 1000);

            value = cref.getReferred().getTestId();
            assertTrue(error + cref, value > 1000);
        }
    }

    public void testConvertedReferenceInsert()
    {
        String error = "Maybe field conversion was not called or multiple times";
        int no = 110;
        int noRef = Integer.MAX_VALUE - 109;
        int noTest = Integer.MAX_VALUE - 108;
        int noTestRef = Integer.MAX_VALUE - 107;

        ConversionReferrer cref = new ConversionReferrer();
        cref.setPk1(no);
        cref.setTestId(noTest);

        ConversionReferred crefed = new ConversionReferred();
        crefed.setPk1(noRef);
        crefed.setTestId(noTestRef);
        // set reference
        cref.setReferred(crefed);

        broker.beginTransaction();
        broker.store(crefed);
        broker.store(cref);
        broker.commitTransaction();

        broker.clearCache();

        // save id for recapturing object
        Identity id = new Identity(cref, broker);
        broker.clearCache();

        ConversionReferrer referrer = (ConversionReferrer) broker.getObjectByIdentity(id);
        assertNotNull(cref.getReferred());
        assertNotNull("We should found a reference, found " + referrer, referrer.getReferred());
        assertEquals("Stored reference ID should match refed object pk", referrer.getRef1(), crefed.getPk1());
        assertEquals(error, cref.getPk1(), referrer.getPk1());
        assertEquals(error, cref.getTestId(), referrer.getTestId());
        assertEquals(error, cref.getReferred().getPk1(), referrer.getReferred().getPk1());

        assertEquals(error, cref.getReferred().getTestId(), referrer.getReferred().getTestId());

        broker.beginTransaction();
        // delete objects
        broker.delete(crefed);
        broker.delete(cref);
        broker.commitTransaction();
    }


    //****************************************************************************
    // inner class
    //****************************************************************************
    /**
     * A conversion class for unit testing. The conversion is nonsensical - java
     * field is difference of Integer.MAX_VALUE and db value.
     * @author  drfish
     */
    public static class TestInt2IntConverter implements FieldConversion
    {

        /** Creates a new instance of FromMaxInt2IntConversion */
        public TestInt2IntConverter()
        {
        }

        /** convert a Java object to its SQL pendant, used for insert & update */
        public Object javaToSql(Object source) throws ConversionException
        {

            int val = ((Integer) source).intValue();
            val = Integer.MAX_VALUE - val;
            return new Integer(val);
        }

        /** convert a SQL value to a Java Object, used for SELECT */
        public Object sqlToJava(Object source) throws ConversionException
        {
            int val = ((Integer) source).intValue();
            val = Integer.MAX_VALUE - val;
            return new Integer(val);
        }
    }


    //****************************************************************************
    // inner class
    //****************************************************************************
    public static class ConversionReferrer implements Serializable
    {

        private int pk1;
        private int testId;
        private int ref1;
        private ConversionReferred referred;

        /** Creates a new instance of ConversionParent */
        public ConversionReferrer()
        {
        }

        public String toString()
        {
            ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
            buf.append("pk1", pk1);
            buf.append("ref1", ref1);
            buf.append("testId", testId);
            buf.append("referred", referred);
            return buf.toString();
        }

        public int getTestId()
        {
            return testId;
        }

        public void setTestId(int testId)
        {
            this.testId = testId;
        }

        /** Getter for property pk1.
         * @return Value of property pk1.
         *
         */
        public int getPk1()
        {
            return pk1;
        }

        /** Setter for property pk1.
         * @param pk1 New value of property pk1.
         *
         */
        public void setPk1(int pk1)
        {
            this.pk1 = pk1;
        }

        /** Getter for property ref1.
         * @return Value of property ref1.
         *
         */
        public int getRef1()
        {
            return ref1;
        }

        /** Setter for property ref1.
         * @param ref1 New value of property ref1.
         *
         */
        public void setRef1(int ref1)
        {
            this.ref1 = ref1;
        }

        /** Getter for property referred.
         * @return Value of property referred.
         *
         */
        public ConversionReferred getReferred()
        {
            return referred;
        }

        /** Setter for property referred.
         * @param referred New value of property referred.
         *
         */
        public void setReferred(ConversionReferred referred)
        {
            this.referred = referred;
        }
    }

    //****************************************************************************
    // inner class
    //****************************************************************************
    public static class ConversionReferred implements Serializable
    {

        private int pk1;
        private int testId;

        /** Creates a new instance of ConversionReferred */
        public ConversionReferred()
        {
        }

        public String toString()
        {
            ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
            buf.append("pk1", pk1);
            buf.append("testId", testId);
            return buf.toString();
        }

        public int getTestId()
        {
            return testId;
        }

        public void setTestId(int testId)
        {
            this.testId = testId;
        }

        /** Getter for property pk1.
         * @return Value of property pk1.
         *
         */
        public int getPk1()
        {
            return pk1;
        }

        /** Setter for property pk1.
         * @param pk1 New value of property pk1.
         *
         */
        public void setPk1(int pk1)
        {
            this.pk1 = pk1;
        }
    }
}
