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
 * Tests for the ojb.nested tag.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class NestedTagSimpleTests extends OjbTestBase
{
    public NestedTagSimpleTests(String name)
    {
        super(name);
    }

    // Basic test: no attributes, nested object of non-persistent type with single field of type int
    public void testSimple1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.nested */\n"+
            "  private B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "public class B {\n"+
            "  /** @ojb.field */\n"+
            "  private int id;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr::id\"\n"+
            "        column=\"attr_id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attr_id\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Basic test: no attributes, nested object of persistent type with reference to other type
    public void testSimple2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.nested */\n"+
            "  private B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "  /** @ojb.field */\n"+
            "  private int aid;\n"+
            "  /** @ojb.reference foreignkey=\"aid\" */\n"+
            "  private A aobj;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr::aid\"\n"+
            "        column=\"attr_aid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
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
            "        name=\"attr::aobj\"\n"+
            "        class-ref=\"test.A\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attr::aid\"/>\n"+
            "    </reference-descriptor>\n"+
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
            "        name=\"aobj\"\n"+
            "        class-ref=\"test.A\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"aid\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attr_aid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"A\">\n"+
            "            <reference local=\"attr_aid\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
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

    // Basic test: no attributes, nested object of non-persistent type with collection
    public void testSimple3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.nested */\n"+
            "  private B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "public class B {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.collection foreignkey=\"bid\" */\n"+
            "  private C[] cobjs;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C {\n"+
            "  /** @ojb.field */\n"+
            "  private int bid;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr::id\"\n"+
            "        column=\"attr_id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <collection-descriptor\n"+
            "        name=\"attr::cobjs\"\n"+
            "        element-class-ref=\"test.C\"\n"+
            "    >\n"+
            "        <inverse-foreignkey field-ref=\"bid\"/>\n"+
            "    </collection-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"bid\"\n"+
            "        column=\"bid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attr_id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"bid\"\n"+
            "                javaName=\"bid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"A\">\n"+
            "            <reference local=\"bid\" foreign=\"attr_id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Basic test: deep nested object of persistent type with reference to other type
    public void testDeepNesting()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.nested */\n"+
            "  private B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.nested */\n"+
            "  private C attr;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C {\n"+
            "  /** @ojb.field */\n"+
            "  private int bid;\n"+
            "  /** @ojb.reference foreignkey=\"bid\" */\n"+
            "  private B bobj;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr::attr::bid\"\n"+
            "        column=\"attr_attr_bid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr::id\"\n"+
            "        column=\"attr_id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr::attr::bobj\"\n"+
            "        class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attr::attr::bid\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr::bid\"\n"+
            "        column=\"attr_bid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
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
            "        name=\"attr::bobj\"\n"+
            "        class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attr::bid\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"bid\"\n"+
            "        column=\"bid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"bobj\"\n"+
            "        class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"bid\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attr_attr_bid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attr_id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"attr_attr_bid\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"attr_bid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"attr_bid\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"bid\"\n"+
            "                javaName=\"bid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"bid\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
