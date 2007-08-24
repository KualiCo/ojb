package org.apache.ojb.broker;

import junit.framework.TestCase;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.accesslayer.conversions.ConversionException;
import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;

import java.io.Serializable;

/**
 * Test case to check the field conversion.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: FieldConversionTest.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class FieldConversionTest extends TestCase
{
    private PersistenceBroker broker;

    public static void main(String[] args)
    {
        String[] arr = {FieldConversionTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**
     * Insert the method's description here.
     * Creation date: (06.12.2000 21:58:53)
     */
    public void setUp() throws PBFactoryException
    {
        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
    }

    /**
     * Insert the method's description here.
     * Creation date: (06.12.2000 21:59:14)
     */
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

    public void testConversion()
    {
        int id = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        ConversionVO vo = new ConversionVO(
                null, new ConversionId(new Integer(id)), null, new ConversionId(new Integer(id + 1)));
        broker.beginTransaction();
        broker.store(vo);
        broker.commitTransaction();

        Identity identity = new Identity(vo, broker);
        broker.clearCache();
        ConversionVO find_vo = (ConversionVO) broker.getObjectByIdentity(identity);
    }

    public static class FieldConversionConversionIdToInteger implements FieldConversion
    {
        public Object javaToSql(Object source) throws ConversionException
        {
            if (!(source instanceof ConversionId))
            {
                throw new ConversionException(
                        "Wrong java field type when java-->sql, expected " +
                        ConversionId.class.getClass() + ", found "
                        + source.getClass());
            }
            return ((ConversionId) source).getConversionId();
        }

        public Object sqlToJava(Object source) throws ConversionException
        {
            if (!(source instanceof Integer))
            {
                throw new ConversionException(
                        "Wrong java field type when java-->sql, expected java.lang.Integer, found "
                        + source.getClass());
            }
            return new ConversionId((Integer) source);
        }
    }

    public static class FieldConversionLongToInteger implements FieldConversion
    {
        public Object javaToSql(Object source) throws ConversionException
        {
            if(source == null) return null;
            if (!(source instanceof Long))
            {
                throw new ConversionException(
                        "Wrong java field type when java-->sql, expected java.lang.Long, found "
                        + source.getClass());
            }
            return new Integer(((Long) source).intValue());
        }

        public Object sqlToJava(Object source) throws ConversionException
        {
            if(source == null) return null;
            if (!(source instanceof Integer))
            {
                throw new ConversionException(
                        "Wrong java field type when java-->sql, expected java.lang.Integer, found "
                        + source.getClass());
            }
            return new Long(((Integer) source).longValue());
        }
    }

    public static class ConversionId implements Serializable
    {
        private Integer conversionId;

        public ConversionId(Integer conversionId)
        {
            this.conversionId = conversionId;
        }

        public Integer getConversionId()
        {
            return conversionId;
        }

        public void setConversionId(Integer conversionId)
        {
            this.conversionId = conversionId;
        }

        public String toString()
        {
            return new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE).
                    append("conversionId", conversionId).toString();
        }
    }

    public static class ConversionVO implements Serializable
    {
        Long pkWithAutoIncrement;
        ConversionId pkWithoutAutoIncrement;
        Long normalWithAutoIncrement;
        ConversionId normalWithoutAutoIncrement;

        public ConversionVO()
        {
        }

        public ConversionVO(Long pkWithAutoIncrement, ConversionId pkWithoutAutoIncrement, Long normalWithAutoIncrement, ConversionId normalWithoutAutoIncrement)
        {
            this.pkWithAutoIncrement = pkWithAutoIncrement;
            this.pkWithoutAutoIncrement = pkWithoutAutoIncrement;
            this.normalWithAutoIncrement = normalWithAutoIncrement;
            this.normalWithoutAutoIncrement = normalWithoutAutoIncrement;
        }

        public String toString()
        {
            return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).
                    append("pkWithAutoIncrement", pkWithAutoIncrement).
                    append("pkWithoutAutoIncrement", pkWithoutAutoIncrement).
                    append("normalWithAutoIncrement", normalWithAutoIncrement).
                    append("normalWithoutAutoIncrement", normalWithoutAutoIncrement).toString();
        }

        public Long getPkWithAutoIncrement()
        {
            return pkWithAutoIncrement;
        }

        public void setPkWithAutoIncrement(Long pkWithAutoIncrement)
        {
            this.pkWithAutoIncrement = pkWithAutoIncrement;
        }

        public ConversionId getPkWithoutAutoIncrement()
        {
            return pkWithoutAutoIncrement;
        }

        public void setPkWithoutAutoIncrement(ConversionId pkWithoutAutoIncrement)
        {
            this.pkWithoutAutoIncrement = pkWithoutAutoIncrement;
        }

        public Long getNormalWithAutoIncrement()
        {
            return normalWithAutoIncrement;
        }

        public void setNormalWithAutoIncrement(Long normalWithAutoIncrement)
        {
            this.normalWithAutoIncrement = normalWithAutoIncrement;
        }

        public ConversionId getNormalWithoutAutoIncrement()
        {
            return normalWithoutAutoIncrement;
        }

        public void setNormalWithoutAutoIncrement(ConversionId normalWithoutAutoIncrement)
        {
            this.normalWithoutAutoIncrement = normalWithoutAutoIncrement;
        }
    }
}
