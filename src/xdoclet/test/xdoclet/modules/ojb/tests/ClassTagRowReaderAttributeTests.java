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
 * Tests for the ojb.class tag with the row-reader attribute.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ClassTagRowReaderAttributeTests extends OjbTestBase
{
    public ClassTagRowReaderAttributeTests(String name)
    {
        super(name);
    }

    // Test: empty value
    public void testRowReader1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class row-reader=\"\" */\n"+
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

    // Test: pre-defined class
    public void testRowReader2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class row-reader=\"org.apache.ojb.broker.accesslayer.RowReaderDefaultImpl\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            "    row-reader=\"org.apache.ojb.broker.accesslayer.RowReaderDefaultImpl\"\n"+
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

    // Test: user-defined class
    public void testRowReader3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class row-reader=\""+TestRowReader.class.getName()+"\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            "    row-reader=\""+TestRowReader.class.getName()+"\"\n"+
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

    // Test: unknown class
    public void testRowReader4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class row-reader=\"test.RowReader\" */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: invalid class
    public void testRowReader5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class row-reader=\"java.lang.String\" */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: unknown class but strict is set to false
    public void testRowReader6()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class row-reader=\"SomeClass\" */\n"+
            "public class A {}\n");

        HashMap taskProps          = new HashMap();
        HashMap torqueSubTaskProps = new HashMap();
        
        taskProps.put(PROPERTY_CHECKS, "basic");
        torqueSubTaskProps.put(PROPERTY_DATABASENAME, "ojbtest");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            "    row-reader=\"SomeClass\"\n"+
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
