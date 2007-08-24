package org.apache.ojb.odmg;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.accesslayer.conversions.ConversionException;
import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;
import org.apache.ojb.junit.ODMGTestCase;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * class FieldConversion_ForeigenKeyTest,
 * check the field conversion behaviour.
 *
 * @author <a href="mailto:om@ppi.de">Oliver Matz</a>
 * @version $Id: FieldConversionTest_4.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class FieldConversionTest_4 extends ODMGTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {FieldConversionTest_4.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testSelfReferingParent() throws Exception
    {
        String strQuery = "select allNodes from " + Node.class.getName();
        long id = System.currentTimeMillis();
        Node node = new Node(id, null, true);
        node.setParent(node);

        List result;
        int before;
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        try
        {
            tx.begin();

            OQLQuery query = odmg.newOQLQuery();
            query.create(strQuery);
            result = (List) query.execute();
            before = result.size();

            database.makePersistent(node);
            tx.commit();

            tx.begin();
            tx.getBroker().clearCache();
            query = odmg.newOQLQuery();
            query.create(strQuery);
            result = (List) query.execute();
            tx.commit();
        }
        finally
        {
          if (tx != null && tx.isOpen())
          {
              tx.abort();
          }
        }
        int after = result.size();
        assertFalse(after == 0);
        assertEquals(before + 1, after);

        tx.begin();
        database.deletePersistent(node);
        tx.commit();

        OQLQuery query = odmg.newOQLQuery();
        query.create(strQuery);
        result = (List) query.execute();
        after = result.size();
        assertEquals(before, after);
    }

    public void testMakePersistentNode() throws Exception
    {
        String strQuery = "select allNodes from " + Node.class.getName();
        long id = System.currentTimeMillis();
        Node node = new Node(id, null, true);
        Node child = new Node(id + 1, node, false);


        List result;
        int before;
        Transaction tx = odmg.newTransaction();
        try
        {
            tx.begin();

            OQLQuery query = odmg.newOQLQuery();
            query.create(strQuery);
            result = (List) query.execute();
            before = result.size();

            database.makePersistent(child);

            tx.commit();

            tx.begin();
            query = odmg.newOQLQuery();
            query.create(strQuery);
            result = (List) query.execute();
            tx.commit();
        }
        finally
        {
            if (tx != null && tx.isOpen())
            {
                tx.abort();
            }
        }

        int after = result.size();

        assertFalse(after == 0);
        assertEquals(before + 2, after);

    }

    public void testLockNode() throws Exception
    {
        String strQuery = "select allNodes from " + Node.class.getName();
        long id = System.currentTimeMillis();
        Node node = new Node(id, null, true);
        Node child = new Node(id + 1, node, false);

        Transaction tx = odmg.newTransaction();
        List result;
        int before;

        try
        {
            tx.begin();
            OQLQuery query = odmg.newOQLQuery();
            query.create(strQuery);
            result = (List) query.execute();
            before = result.size();

            tx.lock(child, Transaction.WRITE);
            tx.commit();

            tx.begin();
            query = odmg.newOQLQuery();
            query.create(strQuery);
            result = (List) query.execute();
            tx.commit();
        }
        finally
        {
            if (tx != null && tx.isOpen())
            {
                tx.abort();
            }
        }

        int after = result.size();

        assertFalse(after == 0);
        assertEquals(before + 2, after);
    }


    //****************************************************************************
    // inner class
    //****************************************************************************
    public static class LongToBigDecimalConversion implements FieldConversion
    {
        public LongToBigDecimalConversion()
        {
        }

        public Object javaToSql(Object source) throws ConversionException
        {
            Object ret;
            if (source == null)
            {
                ret = null;
            }
            else if (source instanceof Long)
            {
                ret = new BigDecimal(((Long) source).doubleValue());
            }
            else
            {
                throw new ConversionException(
                        "java-->sql, expected type was"+Long.class.getClass()+
                        ", found type "+source.getClass());
            }
            return ret;
        }

        public Object sqlToJava(Object source) throws ConversionException
        {
            Object ret;
            if (source == null)
            {
                ret = null;
            }
            else if (source instanceof BigDecimal)
            {
                ret = new Long(((BigDecimal) source).longValue());
            }
            else
            {
                throw new ConversionException(
                        "sql-->java, expected type was"+BigDecimal.class.getClass()+
                        ", found type "+source.getClass());
            }
            return ret;
        }
    }


    //****************************************************************************
    // inner class
    //****************************************************************************
    public static class Node implements Serializable
    {
        private long uid;  // primary key
        private long refId;
        private boolean nodeState;
        Node parent;

        public Node()
        {
        }

        public Node(long uid, Node parent, boolean nodeState)
        {
            this.uid = uid;
            this.parent = parent;
            this.nodeState = nodeState;
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this,ToStringStyle.MULTI_LINE_STYLE);
        }

        public long getUid()
        {
            return uid;
        }

        public void setUid(long uid)
        {
            this.uid = uid;
        }

        public boolean isState()
        {
            return nodeState;
        }

        public void setState(boolean state)
        {
            this.nodeState = state;
        }

        public long getRefId()
        {
            return refId;
        }

        public void setRefId(long refId)
        {
            this.refId = refId;
        }

        public Node getParent()
        {
            return parent;
        }

        public void setParent(Node parent)
        {
            this.parent = parent;
        }
    }
}
