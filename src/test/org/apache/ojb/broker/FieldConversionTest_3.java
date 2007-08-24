package org.apache.ojb.broker;

import org.apache.ojb.broker.accesslayer.conversions.ConversionException;
import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Test using field conversions for PK values.
 *
 * @author <a href="mailto:om@ppi.de">Oliver Matz</a>
 * @version $Id: FieldConversionTest_3.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class FieldConversionTest_3 extends PBTestCase
{

    public static void main(String[] args)
    {
        String[] arr = {FieldConversionTest_3.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void tearDown()
    {
        try
        {
            broker.clearCache();
            super.tearDown();
        }
        catch (Exception e)
        {
            //ignored
        }
    }

    /**
     * store nested classes needed field conversion of a primary key field.
     */
    public void testStoreNestedNodes() throws Exception
    {
        //String strQuery = "select allNodes from " + Node.class.getName();
        long id = System.currentTimeMillis();
        Node node = new Node(id, null, true);
        Node child = new Node(id + 1, node, false);

        int before;

        broker.beginTransaction();
        before = broker.getCount(QueryFactory.newQuery(Node.class, (Criteria) null));

        broker.store(child);

        broker.commitTransaction();

        broker.beginTransaction();
        int after = broker.getCount(QueryFactory.newQuery(Node.class, (Criteria) null));
        broker.commitTransaction();

        assertFalse(after == 0);
        assertEquals(before + 2, after);
    }

    /**
     * store class needed field conversion of a primary key field.
     */
    public void testStoreNode() throws Exception
    {
        //String strQuery = "select allNodes from " + Node.class.getName();
        long id = System.currentTimeMillis();
        Node node = new Node(id, null, false);

        int before;

        broker.beginTransaction();
        before = broker.getCount(QueryFactory.newQuery(Node.class, (Criteria) null));

        broker.store(node);

        broker.commitTransaction();

        broker.beginTransaction();
        int after = broker.getCount(QueryFactory.newQuery(Node.class, (Criteria) null));
        broker.commitTransaction();

        assertFalse(after == 0);
        assertEquals(before + 1, after);
    }

    /**
     * Assert that StatementManager handles NULL-values correct when binding deletions.
     */
    public void testDeleteNode() throws Exception
    {
        NodeWoAutoInc node = new NodeWoAutoInc(0);

        try
        {
            broker.beginTransaction();
            // mkalen: Try to issue delete with numeric field=NULL after field conversion,
            // which will make eg Oracle JDBC throw SQLException if not using stmt.setNull()
            broker.delete(node);
            broker.commitTransaction();
        }
        finally
        {
            if (broker.isInTransaction())
            {
                try
                {
                    broker.abortTransaction();
                }
                catch (Throwable ignore)
                {
                    //ignore
                }
            }
        }
    }


    //****************************************************************************
    // inner class
    //****************************************************************************
    public static class LongToBigDecimalConversion implements FieldConversion
    {
        private static final Long NULL_BIG_DECIMAL = null;
        private static final Long ZERO = new Long(0);

        public LongToBigDecimalConversion()
        {
        }

        public Object javaToSql(Object source) throws ConversionException
        {
            if(source == null) return null;
            Object ret;
            if (source instanceof Long)
            {
                if (ZERO.equals(source))
                {
				    ret = NULL_BIG_DECIMAL;
                }
                else
                {
                    ret = new BigDecimal(((Long) source).doubleValue());
                }
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
            if(source == null) return null;
            Object ret;
            if (source instanceof BigDecimal)
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

        public long getUid()
        {
            return uid;
        }

        public void setUid(long uid)
        {
            this.uid = uid;
        }

        public boolean isNodeState()
        {
            return nodeState;
        }

        public void setNodeState(boolean nodeState)
        {
            this.nodeState = nodeState;
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

    public static class NodeWoAutoInc implements Serializable
    {
        private long uid;  // primary key, no auto increment

        public NodeWoAutoInc()
        {
        }

        public NodeWoAutoInc(long uid)
        {
            this.uid = uid;
        }

        public long getUid()
        {
            return uid;
        }

        public void setUid(long uid)
        {
            this.uid = uid;
        }
    }

}
