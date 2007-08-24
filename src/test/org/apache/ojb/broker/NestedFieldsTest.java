package org.apache.ojb.broker;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;


/**
 * This TestCase checks the NestedField support
 * @author Thomas Mahler
 */
public class NestedFieldsTest extends PBTestCase
{
    /**
     * launches the TestCase.
     */
    public static void main(String[] args)
    {

        String[] arr = {NestedFieldsTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testStoreReadNestedField()
    {
        long timestamp = System.currentTimeMillis();
        String ddName = "second_level_detail_" + timestamp;
        String ddDescription = "a real detail description" + timestamp;
        String entryName = "nested_entry_store_" + timestamp;

        NestedDetailDetail dd = new NestedDetailDetail(ddName, ddDescription);
        NestedDetail d = new NestedDetail(dd);

        ArrayList entryList = new ArrayList();
        entryList.add(new NestedEntry(entryName));
        entryList.add(new NestedEntry(entryName));
        entryList.add(new NestedEntry(entryName));

        d.setNestedEntryCollection(entryList);
        NestedMain nested = new NestedMain("main_object_" + timestamp, d);

        broker.beginTransaction();
        broker.store(nested);
        broker.commitTransaction();

        Identity oid = new Identity(nested, broker);

        assertNotNull(nested.getNestedDetail());
        assertNotNull(nested.getNestedDetail().getNestedDetailDetail());
        assertNotNull(nested.getNestedDetail().getNestedDetailDetail().getRealDetailName());
        dd = nested.getNestedDetail().getNestedDetailDetail();
        assertEquals(ddName, dd.getRealDetailName());
        assertEquals(ddDescription, dd.getRealDetailDescription());
        assertEquals(3, nested.getNestedDetail().getNestedEntryCollection().size());

        // retrieve copy of nested object
        // using cached version
        NestedMain nestedCopy = (NestedMain) broker.getObjectByIdentity(oid);
        assertNotNull(nestedCopy.getNestedDetail());
        assertNotNull(nestedCopy.getNestedDetail().getNestedDetailDetail());
        assertNotNull(nestedCopy.getNestedDetail().getNestedDetailDetail().getRealDetailName());
        dd = nested.getNestedDetail().getNestedDetailDetail();
        assertEquals(ddName, dd.getRealDetailName());
        assertEquals(ddDescription, dd.getRealDetailDescription());
        assertNotNull(nestedCopy.getNestedDetail().getNestedEntryCollection());
        assertEquals(3, nestedCopy.getNestedDetail().getNestedEntryCollection().size());

        // clear cache and retrieve copy of nested object
        broker.clearCache();
        nestedCopy = (NestedMain) broker.getObjectByIdentity(oid);
        assertNotNull(nestedCopy.getNestedDetail());
        assertNotNull(nestedCopy.getNestedDetail().getNestedDetailDetail());
        assertNotNull(nestedCopy.getNestedDetail().getNestedDetailDetail().getRealDetailName());
        dd = nested.getNestedDetail().getNestedDetailDetail();
        assertEquals(ddName, dd.getRealDetailName());
        assertEquals(ddDescription, dd.getRealDetailDescription());
        assertNotNull(nestedCopy.getNestedDetail().getNestedEntryCollection());
        assertEquals(3, nestedCopy.getNestedDetail().getNestedEntryCollection().size());
    }

    /**
     * Not all nested fields were populated (some are 'null').
     */
    public void testStoreReadNestedFieldWithNullFields()
    {
        long timestamp = System.currentTimeMillis();
        String entryName = "nested_entry_" + timestamp;

        NestedDetail d = new NestedDetail(null);

        ArrayList entryList = new ArrayList();
        entryList.add(new NestedEntry(entryName));
        entryList.add(new NestedEntry(entryName));
        entryList.add(new NestedEntry(entryName));

        d.setNestedEntryCollection(entryList);
        NestedMain nested = new NestedMain("main_object_" + timestamp, d);

        broker.beginTransaction();
        broker.store(nested);
        broker.commitTransaction();

        Identity oid = new Identity(nested, broker);

        assertNotNull(nested.getNestedDetail());
        assertNull(nested.getNestedDetail().getNestedDetailDetail());
        assertEquals(3, nested.getNestedDetail().getNestedEntryCollection().size());

        // retrieve copy of nested object
        // using cached version
        NestedMain nestedCopy = (NestedMain) broker.getObjectByIdentity(oid);
        assertNotNull(nestedCopy.getNestedDetail());
        assertNull(nestedCopy.getNestedDetail().getNestedDetailDetail());
        assertNotNull(nestedCopy.getNestedDetail().getNestedEntryCollection());
        assertEquals(3, nestedCopy.getNestedDetail().getNestedEntryCollection().size());

        // clear cache and retrieve copy of nested object
        broker.clearCache();
        nestedCopy = (NestedMain) broker.getObjectByIdentity(oid);
        assertNotNull(nestedCopy.getNestedDetail());
        assertNull(nestedCopy.getNestedDetail().getNestedDetailDetail());
        assertNotNull(nestedCopy.getNestedDetail().getNestedEntryCollection());
        assertEquals(3, nestedCopy.getNestedDetail().getNestedEntryCollection().size());
    }

    public void testUpdateNestedField()
    {
        long timestamp = System.currentTimeMillis();
        String ddName = "second_level_detail_" + timestamp;
        String ddDescription = "a real detail description" + timestamp;
        String entryName = "nested_entry_upd_" + timestamp;

        NestedDetailDetail dd = new NestedDetailDetail(ddName, ddDescription);
        NestedDetail d = new NestedDetail(dd);

        ArrayList entryList = new ArrayList();
        entryList.add(new NestedEntry(entryName));
        entryList.add(new NestedEntry(entryName));
        entryList.add(new NestedEntry(entryName));

        d.setNestedEntryCollection(entryList);

        NestedMain nested = new NestedMain("main_object_" + timestamp, d);

        broker.beginTransaction();
        broker.store(nested);
        broker.commitTransaction();

        Identity oid = new Identity(nested, broker);

        // clear cache and retrieve copy of nested object
        broker.clearCache();
        nested = (NestedMain) broker.getObjectByIdentity(oid);

        /*
        till now we do the same as in the test above
        now change nested field and store
        */
        nested.getNestedDetail().getNestedDetailDetail().setRealDetailName("update_name_"+timestamp);
        nested.getNestedDetail().getNestedEntryCollection().add(new NestedEntry(entryName));
        broker.beginTransaction();
        broker.store(nested);
        broker.commitTransaction();

        // clear cache and retrieve copy of nested object
        broker.clearCache();
        nested = (NestedMain) broker.getObjectByIdentity(oid);
        assertEquals("update_name_"+timestamp, nested.getNestedDetail().getNestedDetailDetail().getRealDetailName());
        assertNotNull(nested.getNestedDetail().getNestedEntryCollection());
        assertEquals(4, nested.getNestedDetail().getNestedEntryCollection().size());
    }

    public void testDeleteNestedField()
    {
        long timestamp = System.currentTimeMillis();
        String ddName = "second_level_detail_" + timestamp;
        String ddDescription = "a real detail description" + timestamp;
        String entryName = "nested_entry_del_" + timestamp;

        NestedDetailDetail dd = new NestedDetailDetail(ddName, ddDescription);
        NestedDetail d = new NestedDetail(dd);

        ArrayList entryList = new ArrayList();
        entryList.add(new NestedEntry(entryName));
        entryList.add(new NestedEntry(entryName));
        entryList.add(new NestedEntry(entryName));

        d.setNestedEntryCollection(entryList);

        NestedMain nested = new NestedMain("main_object_" + timestamp, d);
        Identity oid = new Identity(nested, broker);

        broker.beginTransaction();
        broker.store(nested);
        broker.commitTransaction();

        // clear cache and retrieve copy of nested object
        broker.clearCache();
        nested = (NestedMain) broker.getObjectByIdentity(oid);

        broker.beginTransaction();
        broker.delete(nested);
        broker.commitTransaction();

        nested = (NestedMain) broker.getObjectByIdentity(oid);
        assertNull("Object was not deleted", nested);

        Criteria crit = new Criteria();
        crit.addEqualTo("name", entryName);
        Query query = QueryFactory.newQuery(NestedEntry.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertEquals(0, result.size());
    }



    //************************************************************************
    // inner classes - used test classes
    //************************************************************************

    public static class NestedMain
    {
        private Long objId;
        private String name;
        private NestedDetail nestedDetail;

        public NestedMain()
        {
        }

        public NestedMain(String name, NestedDetail detail)
        {
            this.name = name;
            this.nestedDetail = detail;
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

        public NestedDetail getNestedDetail()
        {
            return nestedDetail;
        }

        public void setNestedDetail(NestedDetail nestedDetail)
        {
            this.nestedDetail = nestedDetail;
        }

        public String toString()
        {
            ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE);
            buf.append("objId", objId).
                    append("name", name).
                    append("detail", nestedDetail);
            return buf.toString();
        }
    }

    public static class NestedDetail
    {
        private NestedDetailDetail nestedDetailDetail;
        private Collection nestedEntryCollection;

        public NestedDetail()
        {
        }

        public Collection getNestedEntryCollection()
        {
            return nestedEntryCollection;
        }

        public void setNestedEntryCollection(Collection nestedEntryCollection)
        {
            this.nestedEntryCollection = nestedEntryCollection;
        }

        public NestedDetail(NestedDetailDetail detail)
        {
            this.nestedDetailDetail = detail;
        }

        public NestedDetailDetail getNestedDetailDetail()
        {
            return nestedDetailDetail;
        }

        public void setNestedDetailDetail(NestedDetailDetail nestedDetailDetail)
        {
            this.nestedDetailDetail = nestedDetailDetail;
        }

        public String toString()
        {
            ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE);
            buf.append("detail", nestedDetailDetail);
            return buf.toString();
        }
    }

    public static class NestedDetailDetail
    {
        private String realDetailName;
        private String realDetailDescription;

        public NestedDetailDetail()
        {
        }

        public NestedDetailDetail(String realDetailName, String realDetailDescription)
        {
            this.realDetailName = realDetailName;
            this.realDetailDescription = realDetailDescription;
        }

        public String getRealDetailName()
        {
            return realDetailName;
        }

        public void setRealDetailName(String realDetailName)
        {
            this.realDetailName = realDetailName;
        }

        public String getRealDetailDescription()
        {
            return realDetailDescription;
        }

        public void setRealDetailDescription(String realDetailDescription)
        {
            this.realDetailDescription = realDetailDescription;
        }

        public String toString()
        {
            ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE);
            buf.append("realDetailName", realDetailName).
                    append("realDetailDescription", realDetailDescription);
            return buf.toString();
        }
    }

    public static class NestedEntry
    {
        private Integer id;
        private Long fkId;
        private String name;

        public NestedEntry()
        {
        }

        public NestedEntry(String name)
        {
            this.name = name;
        }

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public Long getFkId()
        {
            return fkId;
        }

        public void setFkId(Long fkId)
        {
            this.fkId = fkId;
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
