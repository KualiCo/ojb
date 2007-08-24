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
 * Tests for the ojb.modify-inherited tag with the id attribute
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ModifyInheritedTagIdAttributeTests extends OjbTestBase
{
    public ModifyInheritedTagIdAttributeTests(String name)
    {
        super(name);
    }

    // Test: removing id attribute of inherited field
    public void testId1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field id=\"3\" */\n"+
            "  private int attrA;\n"+
            "/** @ojb.field id=\"1\" */\n"+
            "  private int attrB;\n"+
            "/** @ojb.field id=\"2\" */\n"+
            "  private int attrC;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attrC\"\n"+
            "  *                       id=\"\"\n"+
            "  */\n"+
            "public class B extends A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrB\"\n"+
            "        column=\"attrB\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrC\"\n"+
            "        column=\"attrC\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrA\"\n"+
            "        column=\"attrA\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrB\"\n"+
            "        column=\"attrB\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrA\"\n"+
            "        column=\"attrA\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrC\"\n"+
            "        column=\"attrC\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrB\"\n"+
            "                javaName=\"attrB\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrC\"\n"+
            "                javaName=\"attrC\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrA\"\n"+
            "                javaName=\"attrA\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"attrB\"\n"+
            "                javaName=\"attrB\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrA\"\n"+
            "                javaName=\"attrA\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrC\"\n"+
            "                javaName=\"attrC\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: adding id attribute to remotely inherited field
    public void testId2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.field name=\"attrC\"\n"+
            "  *            jdbc-type=\"INTEGER\"\n"+
            "  */\n"+
            "public class A {\n"+
            "/** @ojb.field id=\"3\" */\n"+
            "  private int attrA;\n"+
            "/** @ojb.field id=\"1\" */\n"+
            "  private int attrB;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B extends A {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attrC\"\n"+
            "  *                       id=\"2\"\n"+
            "  */\n"+
            "public class C extends B {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrB\"\n"+
            "        column=\"attrB\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrA\"\n"+
            "        column=\"attrA\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrC\"\n"+
            "        column=\"attrC\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        access=\"anonymous\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.C\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrB\"\n"+
            "        column=\"attrB\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrA\"\n"+
            "        column=\"attrA\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrC\"\n"+
            "        column=\"attrC\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        access=\"anonymous\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrB\"\n"+
            "        column=\"attrB\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrC\"\n"+
            "        column=\"attrC\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        access=\"anonymous\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrA\"\n"+
            "        column=\"attrA\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrB\"\n"+
            "                javaName=\"attrB\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrA\"\n"+
            "                javaName=\"attrA\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrC\"\n"+
            "                javaName=\"attrC\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"attrB\"\n"+
            "                javaName=\"attrB\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrA\"\n"+
            "                javaName=\"attrA\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrC\"\n"+
            "                javaName=\"attrC\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"attrB\"\n"+
            "                javaName=\"attrB\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrC\"\n"+
            "                javaName=\"attrC\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrA\"\n"+
            "                javaName=\"attrA\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: invalid value
    public void testId3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field id=\"3\" */\n"+
            "  private int attrA;\n"+
            "/** @ojb.field id=\"1\" */\n"+
            "  private int attrB;\n"+
            "/** @ojb.field id=\"2\" */\n"+
            "  private int attrC;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attrB\"\n"+
            "  *                       id=\"a\"\n"+
            "  */\n"+
            "public class B extends A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: changing id attribute of inherited field
    public void testId4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field id=\"3\" */\n"+
            "  private int attrA;\n"+
            "/** @ojb.field id=\"1\" */\n"+
            "  private int attrB;\n"+
            "/** @ojb.field id=\"2\" */\n"+
            "  private int attrC;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attrB\"\n"+
            "  *                       id=\"4\"\n"+
            "  */\n"+
            "public class B extends A {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C extends B {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrB\"\n"+
            "        column=\"attrB\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrC\"\n"+
            "        column=\"attrC\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrA\"\n"+
            "        column=\"attrA\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.C\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrC\"\n"+
            "        column=\"attrC\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrA\"\n"+
            "        column=\"attrA\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrB\"\n"+
            "        column=\"attrB\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrC\"\n"+
            "        column=\"attrC\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrA\"\n"+
            "        column=\"attrA\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrB\"\n"+
            "        column=\"attrB\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrB\"\n"+
            "                javaName=\"attrB\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrC\"\n"+
            "                javaName=\"attrC\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrA\"\n"+
            "                javaName=\"attrA\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"attrC\"\n"+
            "                javaName=\"attrC\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrA\"\n"+
            "                javaName=\"attrA\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrB\"\n"+
            "                javaName=\"attrB\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"attrC\"\n"+
            "                javaName=\"attrC\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrA\"\n"+
            "                javaName=\"attrA\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrB\"\n"+
            "                javaName=\"attrB\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
