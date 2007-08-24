package xdoclet.modules.ojb.tests;

/* Copyright 2002-2005 The Apache Software Foundation
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
 * Tests for the ojb.modify-nested tag.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ModifyNestedTagSimpleTests extends OjbTestBase
{
    public ModifyNestedTagSimpleTests(String name)
    {
        super(name);
    }

    // Test: changing column attribute of nested field
    public void testSimple1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.nested\n"+
            "   *  @ojb.modify-nested name=\"id\"\n"+
            "   *                     column=\"ID\"\n"+
            "   */\n"+
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
            "        column=\"ID\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"ID\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: change of auto-delete attribute of nested reference
    public void testSimple2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.nested\n"+
            "    * @ojb.modify-nested name=\"cobj\"\n"+
            "    *                    auto-delete=\"true\"\n"+
            "    */\n"+
            "  private B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "  /** @ojb.field */\n"+
            "  private int cid;\n"+
            "  /** @ojb.reference foreignkey=\"cid\" */\n"+
            "  private C cobj;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr::cid\"\n"+
            "        column=\"attr_cid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr::cobj\"\n"+
            "        class-ref=\"test.C\"\n"+
            "        auto-delete=\"true\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attr::cid\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"cid\"\n"+
            "        column=\"cid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"cobj\"\n"+
            "        class-ref=\"test.C\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"cid\"/>\n"+
            "    </reference-descriptor>\n"+
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
            "        <column name=\"attr_cid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"C\">\n"+
            "            <reference local=\"attr_cid\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"cid\"\n"+
            "                javaName=\"cid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"C\">\n"+
            "            <reference local=\"cid\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
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

    // Test: removing of collection-class attribute of nested collection
    public void testSimple3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.nested\n"+
            "    * @ojb.modify-nested name=\"cobjs\"\n"+
            "    *                    collection-class=\"\"\n"+
            "    */\n"+
            "  private B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.collection foreignkey=\"bid\"\n"+
            "    *                 element-class-ref=\"test.C\"\n"+
            "    *                 collection-class=\"org.apache.ojb.odmg.collections.DListImpl\"\n"+
            "    *                 database-foreignkey=\"false\"\n"+
            "    */\n"+
            "  private java.util.List cobjs;\n"+
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
            "    <collection-descriptor\n"+
            "        name=\"cobjs\"\n"+
            "        collection-class=\"org.apache.ojb.odmg.collections.DListImpl\"\n"+
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
            "    <table name=\"B\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
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
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: changing jdbc-type attribute of deeply nested field twice
    public void testSimple4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.nested\n"+
            "   *  @ojb.modify-nested name=\"attr::id\"\n"+
            "   *                     jdbc-type=\"BIT\"\n"+
            "   */\n"+
            "  private B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "  /** @ojb.nested\n"+
            "   *  @ojb.modify-nested name=\"id\"\n"+
            "   *                     jdbc-type=\"FLOAT\"\n"+
            "   */\n"+
            "  private C attr;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C {\n"+
            "  /** @ojb.field */\n"+
            "  private int id;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr::attr::id\"\n"+
            "        column=\"attr_attr_id\"\n"+
            "        jdbc-type=\"BIT\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr::id\"\n"+
            "        column=\"attr_id\"\n"+
            "        jdbc-type=\"FLOAT\"\n"+
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
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attr_attr_id\"\n"+
            "                type=\"BIT\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"attr_id\"\n"+
            "                type=\"FLOAT\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: changing column attribute of multiple nested fields
    public void testSimple5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.nested\n"+
            "   *  @ojb.modify-nested name=\"id\"\n"+
            "   *                     column=\"ID\"\n"+
            "   *  @ojb.modify-nested name=\"name\"\n"+
            "   *                     column=\"NAME\"\n"+
            "   */\n"+
            "  private B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "public class B {\n"+
            "  /** @ojb.field */\n"+
            "  private int id;\n"+
            "  /** @ojb.field length=\"24\" */\n"+
            "  private String name;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr::id\"\n"+
            "        column=\"ID\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr::name\"\n"+
            "        column=\"NAME\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"24\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"ID\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"NAME\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"24\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
