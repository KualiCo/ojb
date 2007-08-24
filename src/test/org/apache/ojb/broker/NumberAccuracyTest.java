package org.apache.ojb.broker;

import java.math.BigDecimal;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.junit.PBTestCase;

/**
 * Test case to check the accuracy of {@link BigDecimal} values stored in DB.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: NumberAccuracyTest.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class NumberAccuracyTest extends PBTestCase
{
    public static void main(String[] args)
    {
        junit.textui.TestRunner.main(new String[] {NumberAccuracyTest.class.getName()});
    }

    public NumberAccuracyTest()
    {
    }

    public NumberAccuracyTest(String name)
    {
        super(name);
    }

    public void testBigDecimal()
    {
        BigDecimal scaleTwo = new BigDecimal("17.34554");
        BigDecimal scaleFour = new BigDecimal("67.345567");
        // round half up values
        scaleTwo = scaleTwo.setScale(2, BigDecimal.ROUND_HALF_UP);
        scaleFour = scaleFour.setScale(4, BigDecimal.ROUND_HALF_UP);

        NumberObject no = new NumberObject();
        no.setScaleTwo(scaleTwo);
        no.setScaleFour(scaleFour);

//        System.out.println("# " + no.getScaleTwoDouble());
//        System.out.println("# " + no.getScaleFourDouble());
        broker.beginTransaction();
        broker.store(no);
        broker.commitTransaction();

        Identity oidNo = new Identity(no, broker);
        broker.clearCache();
        NumberObject newNo = (NumberObject) broker.getObjectByIdentity(oidNo);
//        System.out.println("# " + newNo.getScaleTwoDouble());
//        System.out.println("# " + newNo.getScaleFourDouble());
        assertEquals(17.35d ,newNo.getScaleTwoDouble(), 0.001);
        assertEquals(67.3456 ,newNo.getScaleFourDouble(), 0.00001);
        assertTrue(0 == scaleTwo.compareTo(newNo.getScaleTwo()));
        assertTrue(0 == scaleFour.compareTo(newNo.getScaleFour()));


        BigDecimal newScaleTwo = newNo.getScaleTwo().multiply(new BigDecimal("10"));
        newNo.setScaleTwo(newScaleTwo);
        BigDecimal newScaleFour = newNo.getScaleFour().multiply(new BigDecimal("10"));
        newNo.setScaleFour(newScaleFour);
        broker.beginTransaction();
        broker.store(newNo);
        broker.commitTransaction();

        broker.clearCache();
        NumberObject newNo_2 = (NumberObject) broker.getObjectByIdentity(oidNo);
//        System.out.println("# " + newNo.getScaleTwoDouble());
//        System.out.println("# " + newNo.getScaleFourDouble());
        assertEquals(173.5d ,newNo_2.getScaleTwoDouble(), 0.1);
        assertEquals(673.455 ,newNo_2.getScaleFourDouble(), 0.001);
        assertTrue(0 == newScaleTwo.compareTo(newNo_2.getScaleTwo()));
        assertTrue(0 == newScaleFour.compareTo(newNo_2.getScaleFour()));
    }

    public static class NumberObject
    {
        private Integer id;
        private BigDecimal scaleTwo;
        private BigDecimal scaleFour;

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public BigDecimal getScaleTwo()
        {
            return scaleTwo;
        }

        public void setScaleTwo(BigDecimal scaleTwo)
        {
            this.scaleTwo = scaleTwo;
        }

        public double getScaleTwoDouble()
        {
            return scaleTwo.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        }

        public BigDecimal getScaleFour()
        {
            return scaleFour;
        }

        public void setScaleFour(BigDecimal scalefour)
        {
            this.scaleFour = scalefour;
        }

        public double getScaleFourDouble()
        {
            return scaleFour.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }
}
