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
 * Tests for the ojb.class tag with the initialization-method attribute.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ClassTagInitializationMethodAttributeTests extends OjbTestBase
{
    public ClassTagInitializationMethodAttributeTests(String name)
    {
        super(name);
    }

    // Test: empty value
    public void testInitializationMethod1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class initialization-method=\"\" */\n"+
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

    // Test: normal use
    public void testInitializationMethod2()
    {
        // note that we only recreate parts of the test class here
        // (only the actual class is used in the method check)
        addClass(
            TestClass.class.getName(),
            "package "+TestClass.class.getPackage().getName()+";\n"+
            "/** @ojb.class initialization-method=\"initMethod1\" */\n"+
            "public class "+TestClass.getShortName()+" {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\""+TestClass.class.getName()+"\"\n"+
            "    table=\""+TestClass.getShortName()+"\"\n"+
            "    initialization-method=\"initMethod1\"\n"+
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

    // Test: method does not exist
    public void testInitializationMethod3()
    {
        // note that we only recreate parts of the test class here
        // (only the actual class is used in the method check)
        addClass(
            TestClass.class.getName(),
            "package "+TestClass.class.getPackage().getName()+";\n"+
            "/** @ojb.class initialization-method=\"initMethod\" */\n"+
            "public class "+TestClass.getShortName()+" {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: a method of that name exists but has parameters
    public void testInitializationMethod4()
    {
        // note that we only recreate parts of the test class here
        // (only the actual class is used in the method check)
        addClass(
            TestClass.class.getName(),
            "package "+TestClass.class.getPackage().getName()+";\n"+
            "/** @ojb.class initialization-method=\"initMethod2\" */\n"+
            "public class "+TestClass.getShortName()+" {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: method exists but is static
    public void testInitializationMethod5()
    {
        // note that we only recreate parts of the test class here
        // (only the actual class is used in the method check)
        addClass(
            TestClass.class.getName(),
            "package "+TestClass.class.getPackage().getName()+";\n"+
            "/** @ojb.class initialization-method=\"initMethod3\" */\n"+
            "public class "+TestClass.getShortName()+" {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: unknown method but strict is set to false
    public void testInitializationMethod6()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class initialization-method=\"someMethod\" */\n"+
            "public class A {}\n");

        HashMap taskProps          = new HashMap();
        HashMap torqueSubTaskProps = new HashMap();
        
        taskProps.put(PROPERTY_CHECKS, "basic");
        torqueSubTaskProps.put(PROPERTY_DATABASENAME, "ojbtest");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            "    initialization-method=\"someMethod\"\n"+
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
