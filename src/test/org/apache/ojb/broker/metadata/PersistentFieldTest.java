package org.apache.ojb.broker.metadata;

import java.util.Collection;

import org.apache.commons.beanutils.BasicDynaClass;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldAutoProxyImpl;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldDirectImpl;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldDynaBeanImpl;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldIntrospectorImpl;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldPrivilegedImpl;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.configuration.impl.OjbConfiguration;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;
import org.apache.ojb.junit.OJBTestCase;

/* Copyright 2002-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Test to check the capability of the {@link org.apache.ojb.broker.metadata.fieldaccess.PersistentField}
 * implementations.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: PersistentFieldTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class PersistentFieldTest extends OJBTestCase
{
    private Class oldPFClass;

    Class[] persistentFieldClasses = new Class[]{
        PersistentFieldDirectImpl.class
        , PersistentFieldIntrospectorImpl.class
        , PersistentFieldPrivilegedImpl.class
        , PersistentFieldAutoProxyImpl.class
        , PersistentFieldDynaBeanImpl.class};

    public static void main(String[] args)
    {
        String[] arr = {PersistentFieldTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        oldPFClass = ((OjbConfiguration) OjbConfigurator.getInstance()
                .getConfigurationFor(null)).getPersistentFieldClass();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        ((OjbConfiguration) OjbConfigurator.getInstance()
                .getConfigurationFor(null)).setPersistentFieldClass(oldPFClass);
    }

    private void runFieldTestsFor(Class targetClass, boolean supportJavaBeanNames) throws Exception
    {
        ((OjbConfiguration) OjbConfigurator.getInstance().getConfigurationFor(null)).setPersistentFieldClass(targetClass);

        PersistentField pfNM_Name = newInstance(targetClass, NestedMain.class, NESTED_MAIN_NAME);
        PersistentField pfNDD_RD = newInstance(targetClass, NestedMain.class, NESTED_DETAIL_DETAIL_REAL_DETAIL);
        PersistentField pfNDD_RDD = newInstance(targetClass, NestedMain.class, NESTED_DETAIL_DETAIL_REAL_DESCRIPTION);
        PersistentField pfND_MJB = null;
        PersistentField pfNE_Name = null;
        if (supportJavaBeanNames)
        {
            pfND_MJB = newInstance(targetClass, NestedMain.class, NESTED_DETAIL_MORE_JAVA_BEAN);
            pfNE_Name = newInstance(targetClass, NestedMain.class, NESTED_ENTRY_NAME);
        }

        // test getter
        NestedMain nm = createNestedObject();
        Object result = pfNM_Name.get(nm);
        assertEquals(NESTED_MAIN_NAME_VALUE, result);
        result = pfNDD_RD.get(nm);
        assertEquals(NESTED_DETAIL_DETAIL_REAL_DETAIL_VALUE, result);
        result = pfNDD_RDD.get(nm);
        assertEquals(NESTED_DETAIL_DETAIL_REAL_DESCRIPTION_VALUE, result);

        if (supportJavaBeanNames)
        {
            result = pfND_MJB.get(nm);
            assertEquals(NESTED_DETAIL_MORE_JAVA_BEAN_VALUE, result);
            result = pfNE_Name.get(nm);
            assertEquals(NESTED_ENTRY_NAME_VALUE, result);
        }

        NestedMain newNM = new NestedMain();
        // test setter
        pfNM_Name.set(newNM, NESTED_MAIN_NAME_VALUE);
        pfNDD_RD.set(newNM, NESTED_DETAIL_DETAIL_REAL_DETAIL_VALUE);
        result = pfNDD_RDD.get(newNM);
        assertEquals(NESTED_DETAIL_DETAIL_REAL_DESCRIPTION_VALUE, result);

        result = pfNM_Name.get(newNM);
        assertEquals(NESTED_MAIN_NAME_VALUE, result);
        result = pfNDD_RD.get(newNM);
        assertEquals(NESTED_DETAIL_DETAIL_REAL_DETAIL_VALUE, result);

        if (supportJavaBeanNames)
        {
            pfND_MJB.set(newNM, NESTED_DETAIL_MORE_JAVA_BEAN_VALUE);
            pfNE_Name.set(newNM, NESTED_ENTRY_NAME_VALUE);
            result = pfND_MJB.get(newNM);
            assertEquals(NESTED_DETAIL_MORE_JAVA_BEAN_VALUE, result);
            result = pfNE_Name.get(newNM);
            assertEquals(NESTED_ENTRY_NAME_VALUE, result);
        }

        // serialize fields and test again
        pfNM_Name = (PersistentField) SerializationUtils.deserialize(SerializationUtils.serialize(pfNM_Name));
        pfNDD_RD = (PersistentField) SerializationUtils.deserialize(SerializationUtils.serialize(pfNDD_RD));
        pfNDD_RDD = (PersistentField) SerializationUtils.deserialize(SerializationUtils.serialize(pfNDD_RDD));
        if (supportJavaBeanNames)
        {
            pfND_MJB = (PersistentField) SerializationUtils.deserialize(SerializationUtils.serialize(pfND_MJB));
            pfNE_Name = (PersistentField) SerializationUtils.deserialize(SerializationUtils.serialize(pfNE_Name));
        }

        // test getter
        nm = createNestedObject();
        result = pfNM_Name.get(nm);
        assertEquals(NESTED_MAIN_NAME_VALUE, result);
        result = pfNDD_RD.get(nm);
        assertEquals(NESTED_DETAIL_DETAIL_REAL_DETAIL_VALUE, result);
        result = pfNDD_RDD.get(nm);
        assertEquals(NESTED_DETAIL_DETAIL_REAL_DESCRIPTION_VALUE, result);

        if (supportJavaBeanNames)
        {
            result = pfND_MJB.get(nm);
            assertEquals(NESTED_DETAIL_MORE_JAVA_BEAN_VALUE, result);
            result = pfNE_Name.get(nm);
            assertEquals(NESTED_ENTRY_NAME_VALUE, result);
        }

        newNM = new NestedMain();
        // test setter
        pfNM_Name.set(newNM, NESTED_MAIN_NAME_VALUE);
        pfNDD_RD.set(newNM, NESTED_DETAIL_DETAIL_REAL_DETAIL_VALUE);
        result = pfNDD_RDD.get(newNM);
        assertEquals(NESTED_DETAIL_DETAIL_REAL_DESCRIPTION_VALUE, result);

        result = pfNM_Name.get(newNM);
        assertEquals(NESTED_MAIN_NAME_VALUE, result);
        result = pfNDD_RD.get(newNM);
        assertEquals(NESTED_DETAIL_DETAIL_REAL_DETAIL_VALUE, result);

        if (supportJavaBeanNames)
        {
            pfND_MJB.set(newNM, NESTED_DETAIL_MORE_JAVA_BEAN_VALUE);
            pfNE_Name.set(newNM, NESTED_ENTRY_NAME_VALUE);
            result = pfND_MJB.get(newNM);
            assertEquals(NESTED_DETAIL_MORE_JAVA_BEAN_VALUE, result);
            result = pfNE_Name.get(newNM);
            assertEquals(NESTED_ENTRY_NAME_VALUE, result);
        }
    }

    private void checkBoundaryConditions(Class targetClass) throws Exception
    {
        checkBoundaryConditions(targetClass, true);
    }

    private void checkBoundaryConditions(Class targetClass, boolean withNested) throws Exception
    {
        PersistentField pf = newInstance(targetClass, NestedMain.class, NESTED_MAIN_NAME);
        pf.get(null);
        pf.set(null, null);
        pf = newInstance(targetClass, NestedMain.class, NESTED_MAIN_NAME);
        pf.get(null);
        pf.set(null, "kkddk");
        if(withNested)
        {
            PersistentField pf_2 = newInstance(targetClass, NestedMain.class, NESTED_DETAIL_DETAIL_REAL_DETAIL);
            pf_2.get(null);
            pf_2.set(null, null);
            pf_2 = newInstance(targetClass, NestedMain.class, NESTED_DETAIL_DETAIL_REAL_DETAIL);
            pf_2.get(null);
            pf_2.set(null, "gkfgfg");
        }
    }

    public void testDirectAccess() throws Exception
    {
        runFieldTestsFor(PersistentFieldDirectImpl.class, false);
        checkBoundaryConditions(PersistentFieldDirectImpl.class);
    }

    public void testPrivileged() throws Exception
    {
        runFieldTestsFor(PersistentFieldPrivilegedImpl.class, false);
        checkBoundaryConditions(PersistentFieldPrivilegedImpl.class);
    }

    public void testIntrospector() throws Exception
    {
        runFieldTestsFor(PersistentFieldIntrospectorImpl.class, true);
        checkBoundaryConditions(PersistentFieldIntrospectorImpl.class);
    }

    public void testAutoProxy() throws Exception
    {
        runFieldTestsFor(PersistentFieldAutoProxyImpl.class, true);
        checkBoundaryConditions(PersistentFieldAutoProxyImpl.class);
    }

    public void testDynaBean() throws Exception
    {
        checkBoundaryConditions(PersistentFieldDynaBeanImpl.class, false);

        DynaClass dynaClass = createDynaClass();
        DynaBean bean = dynaClass.newInstance();
        bean.set("name", "testDynaBean");

        PersistentField pf = new PersistentFieldDynaBeanImpl(String.class, "name");
        String result = (String) pf.get(bean);
        assertNotNull(result);
        assertEquals("testDynaBean", result);

        pf.set(bean, "XXXX");
        result = (String) pf.get(bean);
        assertNotNull(result);
        assertEquals("XXXX", result);
    }

    public void testAutoProxyWithDyna() throws Exception
    {
        DynaClass dynaClass = createDynaClass();
        DynaBean bean = dynaClass.newInstance();
        bean.set("name", "testDynaBean");

        PersistentField pf = new PersistentFieldAutoProxyImpl(String.class, "name");
        String result = (String) pf.get(bean);
        assertNotNull(result);
        assertEquals("testDynaBean", result);

        pf.set(bean, "XXXX");
        result = (String) pf.get(bean);
        assertNotNull(result);
        assertEquals("XXXX", result);
    }



    //************************************************************************
    // helper methods
    //************************************************************************
    private NestedMain createNestedObject()
    {
        NestedEntry ne = new NestedEntry(NESTED_ENTRY_NAME_VALUE);
        NestedDetailDetail ndd = new NestedDetailDetail(NESTED_DETAIL_DETAIL_REAL_DETAIL_VALUE, null);
        NestedDetail nd = new NestedDetail(ndd);
        nd.setMoreJavaBeans(NESTED_DETAIL_MORE_JAVA_BEAN_VALUE);
        NestedMain main = new NestedMain(NESTED_MAIN_NAME_VALUE, nd);
        main.setNestedEntry(ne);
        main.setJavaBeansField(NESTED_MAIN_JAVA_BEAN_VALUE);

        return main;
    }

    private PersistentField newInstance(Class pfClass, Class testClass, String fieldName) throws Exception
    {
        Class[] types = new Class[]{Class.class, String.class};
        Object[] args = new Object[]{testClass, fieldName};
        return (PersistentField) ClassHelper.newInstance(pfClass, types, args);
    }

    protected DynaClass createDynaClass()
    {
        DynaClass dynaClass = new BasicDynaClass
                ("TestDynaClass", null,
                        new DynaProperty[]{
                            new DynaProperty("name", String.class),
                        });
        return (dynaClass);
    }


    //************************************************************************
    // inner classes - used test classes
    //************************************************************************

    static String NESTED_MAIN_NAME = "name";
    static String NESTED_MAIN_NAME_VALUE = "name_value";

    static String NESTED_MAIN_JAVA_BEAN = "javaBeansField";
    static String NESTED_MAIN_JAVA_BEAN_VALUE = "javaBeansField_value";

    static String NESTED_MAIN_NESTED_ENTRY = "nestedEntry::name";
    static String NESTED_MAIN_NESTED_ENTRY_VALUE = "nestedEntry_value";

    static String NESTED_DETAIL_MORE_JAVA_BEAN = "nestedDetail::moreJavaBeans";
    static String NESTED_DETAIL_MORE_JAVA_BEAN_VALUE = "moreJavaBeans_value";

    static String NESTED_DETAIL_DETAIL_REAL_DETAIL = "nestedDetail::nestedDetailDetail::realDetailName";
    static String NESTED_DETAIL_DETAIL_REAL_DETAIL_VALUE = "realDetailName_value";

    static String NESTED_DETAIL_DETAIL_REAL_DESCRIPTION = "nestedDetail::nestedDetailDetail::realDetailDescription";
    static String NESTED_DETAIL_DETAIL_REAL_DESCRIPTION_VALUE = null;

    static String NESTED_ENTRY_NAME = "nestedEntry::name";
    static String NESTED_ENTRY_NAME_VALUE = "nestedEntryName_value";


    public static class NestedMain
    {
        private Long objId;
        private String name;
        private NestedDetail nestedDetail;
        // getter/setter don't have to match field name
        private String javaBeansFieldXXX;
        // getter/setter don't have to match field name
        private NestedEntry nestedEntryXXX;

        public NestedMain()
        {
        }

        public NestedMain(String name, NestedDetail detail)
        {
            this.name = name;
            this.nestedDetail = detail;
        }

        public NestedEntry getNestedEntry()
        {
            return nestedEntryXXX;
        }

        public void setNestedEntry(NestedEntry nestedEntry)
        {
            this.nestedEntryXXX = nestedEntry;
        }

        public String getJavaBeansField()
        {
            return javaBeansFieldXXX;
        }

        public void setJavaBeansField(String javaBeansField)
        {
            this.javaBeansFieldXXX = javaBeansField;
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
        // getter/setter don't have to match field name
        private String moreJavaBeansXXX;

        public NestedDetail()
        {
        }

        public String getMoreJavaBeans()
        {
            return moreJavaBeansXXX;
        }

        public void setMoreJavaBeans(String moreJavaBeans)
        {
            this.moreJavaBeansXXX = moreJavaBeans;
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
