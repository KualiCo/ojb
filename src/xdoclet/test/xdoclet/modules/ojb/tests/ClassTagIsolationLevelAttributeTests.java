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

/**
 * Tests for the ojb.class tag with the isolation-level attribute.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ClassTagIsolationLevelAttributeTests extends OjbTestBase
{
    public ClassTagIsolationLevelAttributeTests(String name)
    {
        super(name);
    }

    // Test of isolation-level attribute: with empty value
    public void testIsolationLevel1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class isolation-level=\"\" */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of isolation-level attribute: with invalid value
    public void testIsolationLevel2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class isolation-level=\"some\" */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of isolation-level attribute: with 'read-uncommitted' value
    public void testIsolationLevel3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class isolation-level=\"read-uncommitted\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    isolation-level=\"read-uncommitted\"\n"+
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

    // Test of isolation-level attribute: with 'read-committed' value
    public void testIsolationLevel4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class isolation-level=\"read-committed\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    isolation-level=\"read-committed\"\n"+
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

    // Test of isolation-level attribute: with 'repeatable-read' value
    public void testIsolationLevel5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class isolation-level=\"repeatable-read\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    isolation-level=\"repeatable-read\"\n"+
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

    // Test of isolation-level attribute: with 'serializable' value
    public void testIsolationLevel6()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class isolation-level=\"serializable\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    isolation-level=\"serializable\"\n"+
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

    // Test of isolation-level attribute: with 'optimistic' value
    public void testIsolationLevel7()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class isolation-level=\"optimistic\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    isolation-level=\"optimistic\"\n"+
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

}
