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
 * Tests for the ojb.modify-inherited tag.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ModifyInheritedTagSimpleTests extends OjbTestBase
{
    public ModifyInheritedTagSimpleTests(String name)
    {
        super(name);
    }

    // Test: overriding a base class field (direct base class, no sub classes)
    public void testSimple1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       column=\"battr\"\n"+
            "  */\n"+
            "public class B extends A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"battr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"battr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: overriding a base class reference (indirect base class, no sub classes)
    public void testSimple2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int did;\n"+
            "  /** @ojb.reference foreignkey=\"did\" */\n"+
            "  private D dobj;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "public class B extends A {}");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"dobj\"\n"+
            "  *                       auto-update=\"true\"\n"+
            "  */\n"+
            "public class C extends B {}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class D {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.C\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"did\"\n"+
            "        column=\"did\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"dobj\"\n"+
            "        class-ref=\"test.D\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"did\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"did\"\n"+
            "        column=\"did\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"dobj\"\n"+
            "        class-ref=\"test.D\"\n"+
            "        auto-update=\"true\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"did\"/>\n"+
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
            "        <column name=\"did\"\n"+
            "                javaName=\"did\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"D\">\n"+
            "            <reference local=\"did\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"did\"\n"+
            "                javaName=\"did\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"D\">\n"+
            "            <reference local=\"did\" foreign=\"id\"/>\n"+
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

    // Test: overriding a base class collection (direct base class, one sub class)
    public void testSimple3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.collection element-class-ref=\"test.D\"\n"+
            "    *                 foreignkey=\"aid\"\n"+            "    *                 database-foreignkey=\"false\"\n"+
            "    */\n"+
            "  private java.util.List ds;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"ds\"\n"+
            "  *                       orderby=\"id=DESC\"\n"+
            "  */\n"+
            "public class B extends A {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class C extends B {}");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class D {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.field */\n"+
            "  private int aid;\n"+
            "}");

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
            "    <collection-descriptor\n"+
            "        name=\"ds\"\n"+
            "        element-class-ref=\"test.D\"\n"+
            "    >\n"+
            "        <inverse-foreignkey field-ref=\"aid\"/>\n"+
            "    </collection-descriptor>\n"+
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
            "    <collection-descriptor\n"+
            "        name=\"ds\"\n"+
            "        element-class-ref=\"test.D\"\n"+
            "    >\n"+
            "        <orderby name=\"id\" sort=\"DESC\"/>\n"+
            "        <inverse-foreignkey field-ref=\"aid\"/>\n"+
            "    </collection-descriptor>\n"+
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
            "    <collection-descriptor\n"+
            "        name=\"ds\"\n"+
            "        element-class-ref=\"test.D\"\n"+
            "    >\n"+
            "        <orderby name=\"id\" sort=\"DESC\"/>\n"+
            "        <inverse-foreignkey field-ref=\"aid\"/>\n"+
            "    </collection-descriptor>\n"+
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
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: undefined entity
    public void testSimple4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int id;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       column=\"battr\"\n"+
            "  */\n"+
            "public class B extends A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: multiple modifications in the same class
    public void testSimple5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       column=\"battr\"\n"+
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       column=\"cattr\"\n"+
            "  */\n"+
            "public class B extends A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"battr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"battr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: multiple modifications across several classes
    public void testSimple6()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       column=\"battr\"\n"+
            "  */\n"+
            "public class B extends A {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C extends B {}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       column=\"dattr\"\n"+
            "  */\n"+
            "public class D extends C {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
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
            "        name=\"attr\"\n"+
            "        column=\"battr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.D\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"battr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.D\"\n"+
            "    table=\"D\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"dattr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"battr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"battr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"D\">\n"+
            "        <column name=\"dattr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modification in a non-persistent class with sub classes
    public void testSimple7()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       column=\"battr\"\n"+
            "  */\n"+
            "public class B extends A {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C extends B {}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "public class D extends B {}\n");
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
            "    <extent-class class-ref=\"test.E\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"battr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.E\"\n"+
            "    table=\"E\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"battr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"battr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"E\">\n"+
            "        <column name=\"battr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying unknown attribute
    public void testSimple8()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int did;\n"+
            "  /** @ojb.reference foreignkey=\"did\" */\n"+
            "  private D dobj;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"dobj\"\n"+
            "  *                       unknown=\"true\"\n"+
            "  */\n"+
            "public class B extends A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
