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
 * Tests for the ojb.class tag with the determine-extends attribute.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ClassTagDetermineExtendsAttributeTests extends OjbTestBase
{
    public ClassTagDetermineExtendsAttributeTests(String name)
    {
        super(name);
    }

    // Test of determine-extents attribute: empty value
    public void testDetermineExtents1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class determine-extents=\"\" */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of determine-extents attribute: invalid value
    public void testDetermineExtents2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class determine-extents=\"no\" */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of determine-extents attribute: with 'true' value, one local and one remote subclass
    public void testDetermineExtents3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class determine-extents=\"true\" */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int aid;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B extends A {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "public class C extends A {}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class D extends C {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
            "    <extent-class class-ref=\"test.D\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"aid\"\n"+
            "        column=\"aid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"aid\"\n"+
            "        column=\"aid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.D\"\n"+
            "    table=\"D\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"aid\"\n"+
            "        column=\"aid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"aid\"\n"+
            "                javaName=\"aid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"aid\"\n"+
            "                javaName=\"aid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"D\">\n"+
            "        <column name=\"aid\"\n"+
            "                javaName=\"aid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of determine-extents attribute: with 'true' value, remote base and sub types
    public void testDetermineExtents4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class determine-extents=\"true\" */\n"+
            "public interface A {}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "public interface B extends A {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class determine-extents=\"true\" */\n"+
            "public class C {\n"+
            "  /** @ojb.field */\n"+
            "  private int cid;\n"+
            "}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "public class D extends C implements B {}\n");
        addClass(
            "test.E",
            "package test;\n"+
            "/** @ojb.class determine-extents=\"true\" */\n"+
            "public class E extends D {}\n");
        addClass(
            "test.F",
            "package test;\n"+
            "public class F extends E {}\n");
        addClass(
            "test.G",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class G extends F {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.E\"/>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.E\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"cid\"\n"+
            "        column=\"cid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.E\"\n"+
            "    table=\"E\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.G\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"cid\"\n"+
            "        column=\"cid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.G\"\n"+
            "    table=\"G\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"cid\"\n"+
            "        column=\"cid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"cid\"\n"+
            "                javaName=\"cid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"E\">\n"+
            "        <column name=\"cid\"\n"+
            "                javaName=\"cid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"G\">\n"+
            "        <column name=\"cid\"\n"+
            "                javaName=\"cid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of determine-extents attribute: with 'false' value, one local and one remote subclass
    public void testDetermineExtents5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class determine-extents=\"false\" */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int aid;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B extends A {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "public class C extends A {}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class D extends C {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"aid\"\n"+
            "        column=\"aid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"aid\"\n"+
            "        column=\"aid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.D\"\n"+
            "    table=\"D\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"aid\"\n"+
            "        column=\"aid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"aid\"\n"+
            "                javaName=\"aid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"aid\"\n"+
            "                javaName=\"aid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"D\">\n"+
            "        <column name=\"aid\"\n"+
            "                javaName=\"aid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of determine-extents attribute: with 'false' value, remote base and sub types
    public void testDetermineExtents6()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class determine-extents=\"true\" */\n"+
            "public interface A {}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "public interface B extends A {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class determine-extents=\"false\" */\n"+
            "public class C {\n"+
            "  /** @ojb.field */\n"+
            "  private int cid;\n"+
            "}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "public class D extends C implements B {}\n");
        addClass(
            "test.E",
            "package test;\n"+
            "/** @ojb.class determine-extents=\"false\" */\n"+
            "public class E extends D {}\n");
        addClass(
            "test.F",
            "package test;\n"+
            "public class F extends E {}\n");
        addClass(
            "test.G",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class G extends F {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.E\"/>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"cid\"\n"+
            "        column=\"cid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.E\"\n"+
            "    table=\"E\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"cid\"\n"+
            "        column=\"cid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.G\"\n"+
            "    table=\"G\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"cid\"\n"+
            "        column=\"cid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"cid\"\n"+
            "                javaName=\"cid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"E\">\n"+
            "        <column name=\"cid\"\n"+
            "                javaName=\"cid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"G\">\n"+
            "        <column name=\"cid\"\n"+
            "                javaName=\"cid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
