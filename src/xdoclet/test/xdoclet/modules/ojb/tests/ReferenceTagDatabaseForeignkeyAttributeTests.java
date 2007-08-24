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
 * Tests for the ojb.reference tag with the database-foreignkey attribute
 *
 * @author <a href="mailto:tomdz@apache.org">Thomas Dudziak</a>
 */
public class ReferenceTagDatabaseForeignkeyAttributeTests extends OjbTestBase
{
    public ReferenceTagDatabaseForeignkeyAttributeTests(String name)
    {
        super(name);
    }

    // Test: foreignkey attribute specified, primary key of referenced type is anonymous
    public void testForeignkey1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "  /** @ojb.reference foreignkey=\"attrKey\"\n"+
            "     *               database-foreignkey=\"false\"\n"+
            "    */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.field name=\"id\"\n"+
            "  *            jdbc-type=\"INTEGER\"\n"+
            "  *            primarykey=\"true\"\n"+            "  */\n"+
            "public class B {}\n");

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
            "        access=\"anonymous\"\n"+
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

    // Test: two foreignkey attributes specified whose types match the ordered primarykeys of the referenced type
    public void testForeignkey2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attrKeyA;\n"+
            "  /** @ojb.field */\n"+
            "  private String attrKeyB;\n"+
            "  /** @ojb.reference foreignkey=\"attrKeyB,attrKeyA\"\n"+
            "   *                 database-foreignkey=\"false\"\n"+
            "   */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "  /** @ojb.field primarykey=\"true\"\n"+
            "    *            id=\"2\"\n"+
            "    */\n"+
            "  private int idA;\n"+
            "  /** @ojb.field primarykey=\"true\"\n"+
            "    *            id=\"1\"\n"+
            "    */\n"+
            "  private String idB;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKeyA\"\n"+
            "        column=\"attrKeyA\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKeyB\"\n"+
            "        column=\"attrKeyB\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKeyB\"/>\n"+
            "        <foreignkey field-ref=\"attrKeyA\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"idB\"\n"+
            "        column=\"idB\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        primarykey=\"true\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"idA\"\n"+
            "        column=\"idA\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKeyA\"\n"+
            "                javaName=\"attrKeyA\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrKeyB\"\n"+
            "                javaName=\"attrKeyB\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"idB\"\n"+
            "                javaName=\"idB\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "        <column name=\"idA\"\n"+
            "                javaName=\"idA\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: two foreignkey attributes specified and referenced type is an interface with a subclass
    public void testForeignkey3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attrKeyA;\n"+
            "  /** @ojb.field */\n"+
            "  private String attrKeyB;\n"+
            "  /** @ojb.reference foreignkey=\"attrKeyA,attrKeyB\"\n"+
            "   *                 database-foreignkey=\"true\"\n"+
            "   */\n"+
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
            "public class C implements B {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int idA;\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private String idB;\n"+
            "}\n");
        
        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKeyA\"\n"+
            "        column=\"attrKeyA\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKeyB\"\n"+
            "        column=\"attrKeyB\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKeyA\"/>\n"+
            "        <foreignkey field-ref=\"attrKeyB\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.C\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"idA\"\n"+
            "        column=\"idA\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"idB\"\n"+
            "        column=\"idB\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        primarykey=\"true\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"idA\"\n"+
            "        column=\"idA\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"idB\"\n"+
            "        column=\"idB\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        primarykey=\"true\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKeyA\"\n"+
            "                javaName=\"attrKeyA\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrKeyB\"\n"+
            "                javaName=\"attrKeyB\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"C\">\n"+
            "            <reference local=\"attrKeyA\" foreign=\"idA\"/>\n"+
            "            <reference local=\"attrKeyB\" foreign=\"idB\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"idA\"\n"+
            "                javaName=\"idA\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "        <column name=\"idB\"\n"+
            "                javaName=\"idB\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
