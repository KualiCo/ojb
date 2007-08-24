package org.apache.ojb.broker.metadata;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.NestedFieldsTest;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldAutoProxyImpl;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldDirectImpl;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldIntrospectorImpl;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldPrivilegedImpl;
import org.apache.ojb.broker.util.ClassHelper;
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
 * This is a developer test and NOT part of the test suite.
 * This test help to test the performance of the different
 * {@link org.apache.ojb.broker.metadata.fieldaccess.PersistentField}
 * implementations.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: PersistentFieldPerfTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class PersistentFieldPerfTest extends OJBTestCase
{
    String EOL = SystemUtils.LINE_SEPARATOR;
    Class testClass = NestedFieldsTest.NestedMain.class;
    String fieldName = "name";
    String fieldNameNested = "nestedDetail::nestedDetailDetail::realDetailName";
    int numberOfOperations = 30000;
    int repeat = 5;

    public static void main(String[] args)
    {
        String[] arr = {PersistentFieldPerfTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    Class[] persistentFieldClasses = new Class[]{
        PersistentFieldDirectImpl.class
        , PersistentFieldIntrospectorImpl.class
        , PersistentFieldPrivilegedImpl.class
        , PersistentFieldAutoProxyImpl.class};


    private PersistentField newInstance(Class pfClass, Class testClass, String fieldName) throws Exception
    {
        Class[] types = new Class[]{Class.class, String.class};
        Object[] args = new Object[]{testClass, fieldName};
        return (PersistentField) ClassHelper.newInstance(pfClass, types, args);
    }

    public void testFieldPerformance() throws Exception
    {
        System.out.println();
        System.out.println("=========================================");
        System.out.println("Field performance, set/get " + numberOfOperations + " times a field");
        System.out.println("----------------------------------------");
        for (int i = 0; i < persistentFieldClasses.length; i++)
        {
            Class persistentFieldClass = persistentFieldClasses[i];
            PersistentField p = newInstance(persistentFieldClass, testClass, fieldName);
            buildTestFor(p, false);
        }
        System.out.println("----------------------------------------");
        for (int i = 0; i < persistentFieldClasses.length; i++)
        {
            Class persistentFieldClass = persistentFieldClasses[i];
            PersistentField p = newInstance(persistentFieldClass, testClass, fieldName);
            buildTestFor(p, false);
        }
        System.out.println("----------------------------------------");
    }

    public void testNestedFieldPerformance() throws Exception
    {
        System.out.println();
        System.out.println("=========================================");
        System.out.println("Nested Field performance, set/get " + numberOfOperations + " times a nested field");
        System.out.println("----------------------------------------");
        for (int i = 0; i < persistentFieldClasses.length; i++)
        {
            Class persistentFieldClass = persistentFieldClasses[i];
            PersistentField p = newInstance(persistentFieldClass, testClass, fieldNameNested);
            buildTestFor(p, true);
        }
        System.out.println("----------------------------------------");
        for (int i = 0; i < persistentFieldClasses.length; i++)
        {
            Class persistentFieldClass = persistentFieldClasses[i];
            PersistentField p = newInstance(persistentFieldClass, testClass, fieldNameNested);
            buildTestFor(p, true);
        }
        System.out.println("----------------------------------------");
    }

    private void buildTestFor(PersistentField pf, boolean nested) throws Exception
    {
        long getter = 0;
        long setter = 0;
        for (int i = 0; i < repeat; i++)
        {
            System.gc();
            Thread.sleep(100);
            getter += nested ? getterPerformanceNestedFor(pf) : getterPerformanceFor(pf);
        }
        for (int i = 0; i < repeat; i++)
        {
            System.gc();
            Thread.sleep(100);
            setter += nested ? setterPerformanceNestedFor(pf) : setterPerformanceFor(pf);
        }
        printResult(pf, getter, setter, nested);
    }

    private void printResult(PersistentField pf, long getterPeriod, long setterPeriod, boolean nested)
    {

        System.out.println(ClassUtils.getShortClassName(pf.getClass())
                + (nested ? ": nestedGetter=" : ": getter=") + getterPeriod
                + (nested ? " nestedSetter=" : " setter=") + setterPeriod);
    }

    private long getterPerformanceFor(PersistentField pf)
    {
        String testString = "a test name";
        NestedFieldsTest.NestedMain testObject = new NestedFieldsTest.NestedMain();
        testObject.setName(testString);
        // validate
        assertEquals(testString, pf.get(testObject));

        long period = System.currentTimeMillis();
        for (int i = 0; i < numberOfOperations; i++)
        {
            pf.get(testObject);
        }
        return System.currentTimeMillis() - period;
    }

    private long setterPerformanceFor(PersistentField pf)
    {
        String testString = "a test name";
        NestedFieldsTest.NestedMain testObject = new NestedFieldsTest.NestedMain();
        // validate
        pf.set(testObject, testString);
        assertEquals(testString, testObject.getName());
        long period = System.currentTimeMillis();
        for (int i = 0; i < numberOfOperations; i++)
        {
            pf.set(testObject, testString);
        }
        return System.currentTimeMillis() - period;
    }

    private long getterPerformanceNestedFor(PersistentField pf)
    {
        String testString = "a test name";
        NestedFieldsTest.NestedMain testObject = new NestedFieldsTest.NestedMain();
        NestedFieldsTest.NestedDetail d1 = new NestedFieldsTest.NestedDetail();
        NestedFieldsTest.NestedDetailDetail d2 = new NestedFieldsTest.NestedDetailDetail();
        d2.setRealDetailName(testString);
        d1.setNestedDetailDetail(d2);
        testObject.setNestedDetail(d1);
        // validate
        assertEquals(testString, pf.get(testObject));

        long period = System.currentTimeMillis();
        for (int i = 0; i < numberOfOperations; i++)
        {
            pf.get(testObject);
        }
        return System.currentTimeMillis() - period;
    }

    private long setterPerformanceNestedFor(PersistentField pf)
    {
        String testString = "a test name";
        NestedFieldsTest.NestedMain testObject = new NestedFieldsTest.NestedMain();
        // validate
        pf.set(testObject, testString);
        assertNotNull(testObject.getNestedDetail());
        assertNotNull(testObject.getNestedDetail().getNestedDetailDetail());
        assertEquals(testString, testObject.getNestedDetail().getNestedDetailDetail().getRealDetailName());
        assertEquals(testString, testObject.getNestedDetail().getNestedDetailDetail().getRealDetailName());
        long period = System.currentTimeMillis();
        for (int i = 0; i < numberOfOperations; i++)
        {
            pf.set(testObject, testString);
        }
        return System.currentTimeMillis() - period;
    }
}
