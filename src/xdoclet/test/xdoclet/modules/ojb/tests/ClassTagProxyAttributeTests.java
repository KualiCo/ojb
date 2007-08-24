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
 * Tests for the ojb.class tag with the proxy and proxy-prefetching-limit attributes.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ClassTagProxyAttributeTests extends OjbTestBase
{
    public ClassTagProxyAttributeTests(String name)
    {
        super(name);
    }

    // Test of proxy attribute: with no value
    public void testProxy1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class proxy=\"\" */\n"+
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

    // Test of proxy attribute: with "dynamic" value
    public void testProxy2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class proxy=\"dynamic\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    proxy=\"dynamic\"\n"+
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

    // Test of proxy attribute: with class name value
    // TODO Check that the class is really there
    public void testProxy3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class proxy=\"SomeProxy\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    proxy=\"SomeProxy\"\n"+
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

    // Test of proxy-prefetching-limit attribute with zero value
    public void testProxyPrefetchingLimit1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class proxy=\"dynamic\"\n" +
            "  *            proxy-prefetching-limit=\"0\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    proxy=\"dynamic\"\n"+
            "    proxy-prefetching-limit=\"0\"\n"+
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

    // Test of proxy-prefetching-limit attribute with some value
    public void testProxyPrefetchingLimit2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class proxy=\"dynamic\"\n" +
            "  *            proxy-prefetching-limit=\"10\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    proxy=\"dynamic\"\n"+
            "    proxy-prefetching-limit=\"10\"\n"+
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

    // Test of proxy-prefetching-limit attribute with non-numeric value
    public void testProxyPrefetchingLimit3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class proxy=\"dynamic\"\n" +
            "  *            proxy-prefetching-limit=\"abc\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of proxy-prefetching-limit attribute with illegal value
    public void testProxyPrefetchingLimit4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class proxy=\"dynamic\"\n" +
            "  *            proxy-prefetching-limit=\"-2\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
