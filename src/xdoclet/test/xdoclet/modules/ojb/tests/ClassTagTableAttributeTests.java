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
 * Tests for the ojb.class tag with the table attribute.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ClassTagTableAttributeTests extends OjbTestBase
{
    public ClassTagTableAttributeTests(String name)
    {
        super(name);
    }

    // Test of table: empty value
    public void testTable1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class table=\"\" */\n"+
            "public class A {}\n");

        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
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

    // Test of table: normal use, no sub- or base classes
    public void testTable2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class table=\"TABLE_A\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"TABLE_A\"\n"+
            ">\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"TABLE_A\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of table: normal use with remote base- and subclass
    public void testTable3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "public class B extends A {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class table=\"C_TABLE\" */\n"+
            "public class C extends B {}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "public class D extends C {}\n");
        addClass(
            "test.E",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class E extends D {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.C\"/>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C_TABLE\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.E\"/>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.E\"\n"+
            "    table=\"E\"\n"+
            ">\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "    </table>\n"+
            "    <table name=\"C_TABLE\">\n"+
            "    </table>\n"+
            "    <table name=\"E\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of table: multi-mapped table with two unrelated classes
    public void testTable4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class table=\"TABLE\" */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int aid;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class table=\"TABLE\" */\n"+
            "public class B {\n"+
            "  /** @ojb.field */\n"+
            "  private int bid;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"TABLE\"\n"+
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
            "    table=\"TABLE\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"bid\"\n"+
            "        column=\"bid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"TABLE\">\n"+
            "        <column name=\"aid\"\n"+
            "                javaName=\"aid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"bid\"\n"+
            "                javaName=\"bid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of table: multi-mapped table with three related classes
    public void testTable5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class table=\"TABLE\" */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int aid;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class table=\"TABLE\" */\n"+
            "public class B extends A {\n"+
            "  /** @ojb.field */\n"+
            "  private int bid;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "public class C extends A {\n"+
            "  /** @ojb.field */\n"+
            "  private int cid;\n"+
            "}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class table=\"TABLE\" */\n"+
            "public class D extends C {\n"+
            "  /** @ojb.field */\n"+
            "  private int did;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"TABLE\"\n"+
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
            "    table=\"TABLE\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"aid\"\n"+
            "        column=\"aid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"bid\"\n"+
            "        column=\"bid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.D\"\n"+
            "    table=\"TABLE\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"aid\"\n"+
            "        column=\"aid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"cid\"\n"+
            "        column=\"cid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"did\"\n"+
            "        column=\"did\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"TABLE\">\n"+
            "        <column name=\"aid\"\n"+
            "                javaName=\"aid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"bid\"\n"+
            "                javaName=\"bid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"cid\"\n"+
            "                javaName=\"cid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"did\"\n"+
            "                javaName=\"did\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

}
