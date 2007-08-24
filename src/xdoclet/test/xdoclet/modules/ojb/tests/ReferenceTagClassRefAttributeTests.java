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
 * Tests for the ojb.reference tag with the class-ref attribute
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ReferenceTagClassRefAttributeTests extends OjbTestBase
{
    public ReferenceTagClassRefAttributeTests(String name)
    {
        super(name);
    }

    // Test: no class-ref attribute specified, type of variable has ojb.class tag
    public void testClassRef1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference foreignkey=\"attrKey\" */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKey\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: no class-ref attribute specified, type of variable has ojb.class tag but no table schema
    public void testClassRef2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference foreignkey=\"attrKey\" */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class generate-table-info=\"false\" */\n"+
            "public class B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKey\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: no class-ref attribute specified, type of variable is primitive
    public void testClassRef3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference foreignkey=\"attrKey\" */\n"+
            "  private float attr;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: no class-ref attribute specified, type of variable has no ojb.class tag
    public void testClassRef4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference foreignkey=\"attrKey\" */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "public class B {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: no class-ref attribute specified, type of variable has no ojb.class tag but a sub type of it has
    public void testClassRef5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference foreignkey=\"attrKey\" */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "public class B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class  */\n"+
            "public class C extends B {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: class-ref attribute specified with same type as the variable
    public void testClassRef6()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference class-ref=\"test.B\"\n"+
            "  *                foreignkey=\"attrKey\"\n"+
            "  */\n"+
            "  private B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKey\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: class-ref attribute specified
    public void testClassRef7()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference class-ref=\"test.B\"\n"+
            "  *                foreignkey=\"attrKey\"\n"+
            "  */\n"+
            "  private Object attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKey\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: class-ref attribute specified pointing to a type without a table schema
    public void testClassRef8()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference class-ref=\"test.B\"\n"+
            "  *                foreignkey=\"attrKey\"\n"+
            "  */\n"+
            "  private Object attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class generate-table-info=\"false\" */\n"+
            "public class B {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKey\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: class-ref attribute specified pointing to a primitive type
    public void testClassRef9()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference class-ref=\"boolean\"\n"+
            "  *                foreignkey=\"attrKey\"\n"+
            "  */\n"+
            "  private Object attr;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: class-ref attribute specified pointing to a type without the ojb.class tag
    public void testClassRef10()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference class-ref=\"test.B\"\n"+
            "  *                foreignkey=\"attrKey\"\n"+
            "  */\n"+
            "  private Object attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "public class B {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: class-ref attribute specified pointing to a type without the ojb.class tag (but the type of the variable has it)
    public void testClassRef11()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference class-ref=\"test.C\"\n"+
            "  *                foreignkey=\"attrKey\"\n"+
            "  */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "public class C extends test.B {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: class-ref attribute specified pointing to a type without the ojb.class tag but a sub-type has it
    public void testClassRef12()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference class-ref=\"test.B\"\n"+
            "  *                foreignkey=\"attrKey\"\n"+
            "  */\n"+
            "  private Object attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "public class B {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C extends test.B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: class-ref attribute specified pointing to a type that is no subclass of the variable's type
    public void testClassRef13()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference class-ref=\"test.C\"\n"+
            "  *                foreignkey=\"attrKey\"\n"+
            "  */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: class-ref attribute specified pointing to a type that does not implement the specified interface (variable's type)
    public void testClassRef14()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference class-ref=\"test.C\"\n"+
            "  *                foreignkey=\"attrKey\"\n"+
            "  */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class generate-table-info=\"false\" */\n"+
            "public interface B {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: class-ref attribute specified which points to a direct subclass of the variable's type 
    public void testClassRef15()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference class-ref=\"test.C\"\n"+
            "  *                foreignkey=\"attrKey\"\n"+
            "  */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");
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
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.C\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKey\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.C\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"C\">\n"+
            "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: class-ref attribute specified which points to an indirect subtype of the variable's type 
    public void testClassRef16()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference class-ref=\"test.D\"\n"+
            "  *                foreignkey=\"attrKey\"\n"+
            "  */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "public interface B {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "public class C implements B {}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class D extends C {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.D\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKey\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.D\"\n"+
            "    table=\"D\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"D\">\n"+
            "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"D\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: no class-ref attribute specified, self-reference with anonymous foreignkey
    public void testClassRef17()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.field name=\"attrKey\"\n"+
            "  *            jdbc-type=\"INTEGER\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.reference foreignkey=\"attrKey\" */\n"+
            "  private test.A attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        access=\"anonymous\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.A\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKey\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"A\">\n"+
            "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: no class-ref attribute specified, reference to interface
    public void testClassRef18()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class table=\"A\" */\n"+
            "public interface A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  public Integer getId();\n"+
            "  /** @ojb.field length=\"254\" */\n"+
            "  public String getOjbConcreteClass();\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class table=\"A\" */\n"+
            "public abstract class B implements A {\n"+
            "  private Integer id;\n"+
            "  private String ojbConcreteClass;\n"+
            "  public Integer getId() {\n"+
            "    return id;\n"+
            "  }\n"+
            "  public String getOjbConcreteClass() {\n"+
            "    return ojbConcreteClass;\n"+
            "  }\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class table=\"A\" */\n"+
            "public class C extends B {\n"+
            "  /** @ojb.field length=\"254\" */\n"+
            "  private String value;\n"+
            "}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class D {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private Integer id;\n"+
            "  /** @ojb.field */\n"+
            "  private Integer aid;\n"+
            "  /** @ojb.reference foreignkey=\"aid\" */\n"+
            "  private test.A attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"ojbConcreteClass\"\n"+
            "        column=\"ojbConcreteClass\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.C\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"ojbConcreteClass\"\n"+
            "        column=\"ojbConcreteClass\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"ojbConcreteClass\"\n"+
            "        column=\"ojbConcreteClass\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"value\"\n"+
            "        column=\"value\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.D\"\n"+
            "    table=\"D\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"aid\"\n"+
            "        column=\"aid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.A\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"aid\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "        <column name=\"ojbConcreteClass\"\n"+
            "                javaName=\"ojbConcreteClass\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "        <column name=\"value\"\n"+
            "                javaName=\"value\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"D\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "        <column name=\"aid\"\n"+
            "                javaName=\"aid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"A\">\n"+
            "            <reference local=\"aid\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: no class-ref attribute specified, type of variable is nested class with ojb.class tag
    public void testClassRef19()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference foreignkey=\"attrKey\" */\n"+
            "  private test.B.Inner attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "public class B {\n"+
            "  /** @ojb.class */\n"+
            "  public static class Inner {\n"+
            "    /** @ojb.field primarykey=\"true\" */\n"+
            "    private int id;\n"+
            "  }\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.B$Inner\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKey\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B$Inner\"\n"+
            "    table=\"Inner\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"Inner\">\n"+
            "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"Inner\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: class-ref attribute points to a persistent inner class and uses "$"
    public void testClassRef20()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.class */\n"+
            "  public class Inner {\n"+
            "    /** @ojb.field primarykey=\"true\" */\n"+
            "    private int id;\n"+
            "  }\n"+
            "  /** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "  /** @ojb.reference foreignkey=\"attrKey\"\n"+
            "    *                class-ref=\"test.A$Inner\"\n"+
            "    */\n"+
            "  private Object attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.A$Inner\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKey\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.A$Inner\"\n"+
            "    table=\"Inner\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"Inner\">\n"+
            "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"Inner\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test for OJB-61 though without Java5 syntax; the classes would not actually compile
    // but the idea is that the variable type cannot be resolved
    public void testClassRef21()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private Integer id;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "  /** @ojb.field */\n"+
            "  private Integer aid;\n"+
            "  /** @ojb.reference foreignkey=\"aid\"\n"+
            "    *                class-ref=\"test.A\"\n"+
            "    */\n"+
            "  private T attr;\n"+
            "}\n");

        // this should pass but with a few warnings because T could not be found
        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
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
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.A\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"aid\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"aid\"\n"+
            "                javaName=\"aid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"A\">\n"+
            "            <reference local=\"aid\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
