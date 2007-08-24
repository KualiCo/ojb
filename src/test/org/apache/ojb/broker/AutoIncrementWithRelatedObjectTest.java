package org.apache.ojb.broker;

import org.apache.ojb.junit.PBTestCase;
/**
 * @author MBaird mattbaird@yahoo.com
 * Equivalent to the ODMG test of the same name, but redone using PB api.
 *
 * works for HSQLDB
 * fails for MSSQL :(
 * If you have results for other databases, please enter them here.
 */
public class AutoIncrementWithRelatedObjectTest extends PBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {AutoIncrementWithRelatedObjectTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**
     * since java defaults the value of an int to 0, there might be a problem storing an
     * object that uses int's as foreign keys. If this doesn't work, it means we can't use int's
     * as foreign keys :(
     */
    public void testCreateWithoutRelatedObject()
    {
        Table_1Object table1Ojb = new Table_1Object();
        broker.beginTransaction();
        broker.store(table1Ojb);
        broker.commitTransaction();
    }

    /**
     * do the create with a related object to prove it works.
     */
    public void testCreateWithRelatedObject()
    {
        Table_1Object table1Obj = new Table_1Object();
        Table_2Object table2Obj = new Table_2Object();
        table1Obj.setTable2Object(table2Obj);
        broker.beginTransaction();
        broker.store(table2Obj);
        broker.store(table1Obj);
        broker.commitTransaction();
    }

    public AutoIncrementWithRelatedObjectTest(String name)
    {
        super(name);
    }
}
