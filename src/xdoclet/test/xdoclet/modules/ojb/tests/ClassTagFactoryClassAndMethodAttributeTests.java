package xdoclet.modules.ojb.tests;

/* Copyright 2003-2005 The Apache Software Foundation
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

import java.util.HashMap;

/**
 * Tests for the ojb.class tag with the factory-class and factory-method attributes.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ClassTagFactoryClassAndMethodAttributeTests extends OjbTestBase
{
    public ClassTagFactoryClassAndMethodAttributeTests(String name)
    {
        super(name);
    }
    
    // Test: factory-class with empty value
    public void testFactoryClassAndMethod1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class factory-class=\"\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: factory-class with empty value
    public void testFactoryClassAndMethod2()
    {
        // note that we only recreate parts of the test class here
        // (only the actual class is used in the method check)
        addClass(
            TestClass.class.getName(),
            "package "+TestClass.class.getPackage().getName()+";\n"+
            "/** @ojb.class factory-class=\"\"\n"+
            "  *            factory-method=\"create"+TestClass.getShortName()+"\"\n"+
            "  */\n"+
            "public class "+TestClass.getShortName()+" {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: factory-method with empty value
    public void testFactoryClassAndMethod3()
    {
        // note that we only recreate parts of the test class here
        // (only the actual class is used in the method check)
        addClass(
            TestClass.class.getName(),
            "package "+TestClass.class.getPackage().getName()+";\n"+
            "/** @ojb.class factory-class=\""+TestFactoryClass.class.getName()+"\"\n"+
            "  *            factory-method=\"\"\n"+
            "  */\n"+
            "public class "+TestClass.getShortName()+" {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: both with value
    public void testFactoryClassAndMethod4()
    {
        // note that we only recreate parts of the test class here
        // (only the actual class is used in the method check)
        addClass(
            TestClass.class.getName(),
            "package "+TestClass.class.getPackage().getName()+";\n"+
            "/** @ojb.class factory-class=\""+TestFactoryClass.class.getName()+"\"\n"+
            "  *            factory-method=\"create"+TestClass.getShortName()+"\"\n"+
            "  */\n"+
            "public class "+TestClass.getShortName()+" {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\""+TestClass.class.getName()+"\"\n"+
            "    table=\""+TestClass.getShortName()+"\"\n"+
            "    factory-class=\""+TestFactoryClass.class.getName()+"\"\n"+
            "    factory-method=\"create"+TestClass.getShortName()+"\"\n"+
            ">\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\""+TestClass.getShortName()+"\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: unknown class
    public void testFactoryClassAndMethod5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class factory-class=\"test.SomeClass\"\n"+
            "  *            factory-method=\"someMethod\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: unknown method
    public void testFactoryClassAndMethod6()
    {
        // note that we only recreate parts of the test class here
        // (only the actual class is used in the method check)
        addClass(
            TestClass.class.getName(),
            "package "+TestClass.class.getPackage().getName()+";\n"+
            "/** @ojb.class factory-class=\""+TestFactoryClass.class.getName()+"\"\n"+
            "  *            factory-method=\"make"+TestClass.getShortName()+"\"\n"+
            "  */\n"+
            "public class "+TestClass.getShortName()+" {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: method exists but has parameters
    public void testFactoryClassAndMethod7()
    {
        // note that we only recreate parts of the test class here
        // (only the actual class is used in the method check)
        addClass(
            TestClass.class.getName(),
            "package "+TestClass.class.getPackage().getName()+";\n"+
            "/** @ojb.class factory-class=\""+TestFactoryClass.class.getName()+"\"\n"+
            "  *            factory-method=\"createNew"+TestClass.getShortName()+"\"\n"+
            "  */\n"+
            "public class "+TestClass.getShortName()+" {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: method exists but is not static
    public void testFactoryClassAndMethod8()
    {
        // note that we only recreate parts of the test class here
        // (only the actual class is used in the method check)
        addClass(
            TestClass.class.getName(),
            "package "+TestClass.class.getPackage().getName()+";\n"+
            "/** @ojb.class factory-class=\""+TestFactoryClass.class.getName()+"\"\n"+
            "  *            factory-method=\"makeNew"+TestClass.getShortName()+"\"\n"+
            "  */\n"+
            "public class "+TestClass.getShortName()+" {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: unknown factory-class but strict is set to false
    public void testFactoryClassAndMethod9()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class factory-class=\"SomeClass\"\n"+
            "  *            factory-method=\"someMethod\"\n"+
            "  */\n"+
            "public class A {}\n");

        HashMap taskProps          = new HashMap();
        HashMap torqueSubTaskProps = new HashMap();
        
        taskProps.put(PROPERTY_CHECKS, "basic");
        torqueSubTaskProps.put(PROPERTY_DATABASENAME, "ojbtest");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            "    factory-class=\"SomeClass\"\n"+
            "    factory-method=\"someMethod\"\n"+
            ">\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE, taskProps, null));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, taskProps, torqueSubTaskProps));
    }
}
