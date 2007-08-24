package org.apache.ojb.broker.metadata;

import junit.framework.TestCase;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PBFactoryException;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.junit.PBTestCase;

/**
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ReadonlyTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class ReadonlyTest extends PBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {ReadonlyTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void setUp() throws Exception
    {
        super.setUp();
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testReadonly() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        TestObject obj = new TestObject(null, "testReadonly_"+timestamp, "should not persisted", new Long(timestamp));

        broker.beginTransaction();
        broker.store(obj);
        broker.commitTransaction();
        broker.clearCache();

        broker.beginTransaction();
        Identity oid = broker.serviceIdentity().buildIdentity(obj);
        TestObject ret_obj = (TestObject) broker.getObjectByIdentity(oid);
        broker.commitTransaction();

        assertNotNull(ret_obj);
        assertNotNull(ret_obj.getName());
        assertNull("Field should not be populated", ret_obj.getReadonlyLong());
        assertNull("Field should not be populated", ret_obj.getReadonlyString());
    }

    public void testReadonlyAll() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        TestObject obj = new TestObject(null, "testReadonlyAll_"+timestamp, "should not persisted", new Long(timestamp));
        ClassDescriptor cld = broker.getClassDescriptor(TestObject.class);
        FieldDescriptor fld_id = cld.getFieldDescriptorByName("objId");
        FieldDescriptor fld_name = cld.getFieldDescriptorByName("name");

        try
        {
            broker.beginTransaction();
            broker.store(obj);
            broker.commitTransaction();
            broker.clearCache();

            broker.beginTransaction();
            Identity oid = broker.serviceIdentity().buildIdentity(obj);
            TestObject ret_obj = (TestObject) broker.getObjectByIdentity(oid);
            broker.commitTransaction();

            fld_id.setAccess("readonly");
            fld_name.setAccess("readonly");

            assertNotNull(ret_obj);
            assertNotNull(ret_obj.getName());
            assertNull("Field should not be populated", ret_obj.getReadonlyLong());
            assertNull("Field should not be populated", ret_obj.getReadonlyString());

            broker.beginTransaction();
            oid = broker.serviceIdentity().buildIdentity(obj);
            ret_obj = (TestObject) broker.getObjectByIdentity(oid);
            broker.store(ret_obj);
            broker.commitTransaction();

            assertNotNull(ret_obj);
            assertNotNull(ret_obj.getName());
            assertNull("Field should not be populated", ret_obj.getReadonlyLong());
            assertNull("Field should not be populated", ret_obj.getReadonlyString());

            broker.beginTransaction();
            oid = broker.serviceIdentity().buildIdentity(obj);
            ret_obj = (TestObject) broker.getObjectByIdentity(oid);
            // nevertheless we can remove the whole object
            broker.delete(ret_obj);
            broker.commitTransaction();

            oid = broker.serviceIdentity().buildIdentity(obj);
            ret_obj = (TestObject) broker.getObjectByIdentity(oid);

            assertNull(ret_obj);
        }
        finally
        {
            if(fld_id != null) fld_id.setAccess("readwrite");
            if(fld_name != null) fld_name.setAccess("readwrite");
        }
    }

    public static class TestObject
    {
        private Long objId;
        private String name;
        private String readonlyString;
        private Long readonlyLong;

        public TestObject()
        {
        }

        public TestObject(Long objId, String name, String readonlyString, Long readonlyLong)
        {
            this.objId = objId;
            this.name = name;
            this.readonlyString = readonlyString;
            this.readonlyLong = readonlyLong;
        }

        public Long getObjId()
        {
            return objId;
        }

        public void setObjId(Long objId)
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

        public String getReadonlyString()
        {
            return readonlyString;
        }

        public void setReadonlyString(String readonlyString)
        {
            this.readonlyString = readonlyString;
        }

        public Long getReadonlyLong()
        {
            return readonlyLong;
        }

        public void setReadonlyLong(Long readonlyLong)
        {
            this.readonlyLong = readonlyLong;
        }
    }
}
