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
 * Tests for the ojb.modify-inherited tag with the foreignkey attribute
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ModifyInheritedTagForeignkeyAttributeTests extends OjbTestBase
{
    public ModifyInheritedTagForeignkeyAttributeTests(String name)
    {
        super(name);
    }

    // Test: removing foreignkey attribute of inherited collection
    public void testForeignkey1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.collection element-class-ref=\"test.C\"\n"+
            "    *                 foreignkey=\"aid\"\n"+
            "    */\n"+
            "  private java.util.List attr;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       foreignkey=\"\"\n"+
            "  */\n" +
            "public class B extends A {}");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C {\n"+
            "  /** @ojb.field */\n"+
            "  private int aid;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: changing foreignkey attribute of inherited reference to multiple foreignkeys
    public void testForeignkey2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "  /** @ojb.reference foreignkey=\"attrKey\" */\n"+
            "  private test.D attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       foreignkey=\"attrKey1,attrKey2\"\n"+
            "  */\n"+
            "public class B extends A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attrKey1;\n"+
            "  /** @ojb.field */\n"+
            "  private int attrKey2;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C extends B {}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class D {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: changing foreignkey attribute of inherited reference
    public void testForeignkey3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "  /** @ojb.reference foreignkey=\"attrKey\" */\n"+
            "  private test.D attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       foreignkey=\"attrKeyB\"\n"+
            "  */\n"+
            "public class B extends A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attrKeyB;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C extends B {}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class D {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
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
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.C\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKeyB\"\n"+
            "        column=\"attrKeyB\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.D\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKeyB\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKeyB\"\n"+
            "        column=\"attrKeyB\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.D\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKeyB\"/>\n"+
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
            "    <table name=\"B\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrKeyB\"\n"+
            "                javaName=\"attrKeyB\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"D\">\n"+
            "            <reference local=\"attrKeyB\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attrKeyB\"\n"+
            "                javaName=\"attrKeyB\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"D\">\n"+
            "            <reference local=\"attrKeyB\" foreign=\"id\"/>\n"+
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

    // Test: changing foreignkey attribute of inherited collection to unknown field
    public void testForeignkey4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.collection element-class-ref=\"test.C\"\n"+
            "    *                 foreignkey=\"aid\"\n"+
            "    */\n"+
            "  private java.util.List attr;\n"+
            "}");
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       foreignkey=\"id\"\n"+
            "  */\n" +
            "public class B extends A {}");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "  /** @ojb.field */\n"+
            "  private int aid;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: changing foreignkey attribute of inherited reference to non-persistent field
    public void testForeignkey5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "  /** @ojb.reference foreignkey=\"attrKey\" */\n"+
            "  private test.D attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       foreignkey=\"attrKeyB\"\n"+
            "  */\n"+
            "public class B extends A {\n"+
            "  private int attrKeyB;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C extends B {}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class D {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: changing foreignkey attribute of inherited reference to field with wrong jdbc type
    public void testForeignkey6()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "  /** @ojb.reference foreignkey=\"attrKey\" */\n"+
            "  private test.D attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       foreignkey=\"attrKeyB\"\n"+
            "  */\n"+
            "public class B extends A {\n"+
            "  /** @ojb.field jdbc-type=\"DECIMAL\" */\n"+
            "  private int attrKeyB;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C extends B {}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class D {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: removing foreignkey attributes (with multiple foreignkeys) of inherited reference  
    public void testForeignkey7()
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
            "  /** @ojb.reference foreignkey=\"attrKeyA,attrKeyB\" */\n"+
            "  private test.C attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       foreignkey=\"\"\n"+
            "  */\n"+
            "public class B extends A {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int idA;\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private String idB;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: changing foreignkey attributes (with multiple foreignkeys) of inherited reference to only one foreignkey  
    public void testForeignkey8()
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
            "  /** @ojb.reference foreignkey=\"attrKeyA,attrKeyB\" */\n"+
            "  private test.C attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       foreignkey=\"attrKey\"\n"+
            "  */\n"+
            "public class B extends A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int idA;\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private String idB;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: changing foreignkey attribute (with multiple foreignkeys) of remotely inherited collection
    public void testForeignkey9()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id1;\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private String id2;\n"+
            "  /** @ojb.collection element-class-ref=\"test.D\"\n"+
            "    *                 foreignkey=\"aid1,aid2\"\n"+
            "    *                 database-foreignkey=\"false\"\n"+
            "    */\n"+
            "  private java.util.List attr;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class B extends A {}");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       foreignkey=\"aid4,aid3\"\n"+
            "  */\n" +
            "public class C extends B {}");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.field name=\"aid3\"\n"+
            "  *            jdbc-type=\"VARCHAR\"\n"+
            "  */\n"+
            "public class D {\n"+
            "  /** @ojb.field */\n"+
            "  private int aid1;\n"+
            "  /** @ojb.field */\n"+
            "  private int aid4;\n"+
            "  /** @ojb.field */\n"+
            "  private String aid2;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"id1\"\n"+
            "        column=\"id1\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"id2\"\n"+
            "        column=\"id2\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        primarykey=\"true\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <collection-descriptor\n"+
            "        name=\"attr\"\n"+
            "        element-class-ref=\"test.D\"\n"+
            "    >\n"+
            "        <inverse-foreignkey field-ref=\"aid1\"/>\n"+
            "        <inverse-foreignkey field-ref=\"aid2\"/>\n"+
            "    </collection-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.C\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"id1\"\n"+
            "        column=\"id1\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"id2\"\n"+
            "        column=\"id2\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        primarykey=\"true\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <collection-descriptor\n"+
            "        name=\"attr\"\n"+
            "        element-class-ref=\"test.D\"\n"+
            "    >\n"+
            "        <inverse-foreignkey field-ref=\"aid1\"/>\n"+
            "        <inverse-foreignkey field-ref=\"aid2\"/>\n"+
            "    </collection-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id1\"\n"+
            "        column=\"id1\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"id2\"\n"+
            "        column=\"id2\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        primarykey=\"true\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <collection-descriptor\n"+
            "        name=\"attr\"\n"+
            "        element-class-ref=\"test.D\"\n"+
            "    >\n"+
            "        <inverse-foreignkey field-ref=\"aid4\"/>\n"+
            "        <inverse-foreignkey field-ref=\"aid3\"/>\n"+
            "    </collection-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.D\"\n"+
            "    table=\"D\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"aid3\"\n"+
            "        column=\"aid3\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "        access=\"anonymous\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"aid1\"\n"+
            "        column=\"aid1\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"aid4\"\n"+
            "        column=\"aid4\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"aid2\"\n"+
            "        column=\"aid2\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"id1\"\n"+
            "                javaName=\"id1\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "        <column name=\"id2\"\n"+
            "                javaName=\"id2\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"id1\"\n"+
            "                javaName=\"id1\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "        <column name=\"id2\"\n"+
            "                javaName=\"id2\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"id1\"\n"+
            "                javaName=\"id1\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "        <column name=\"id2\"\n"+
            "                javaName=\"id2\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"D\">\n"+
            "        <column name=\"aid3\"\n"+
            "                javaName=\"aid3\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "        <column name=\"aid1\"\n"+
            "                javaName=\"aid1\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"aid4\"\n"+
            "                javaName=\"aid4\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"aid2\"\n"+
            "                javaName=\"aid2\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: changing foreignkey attributes (with multiple foreignkeys) of inherited reference where one new foreignkey is undefined  
    public void testForeignkey10()
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
            "  /** @ojb.reference foreignkey=\"attrKeyA,attrKeyB\" */\n"+
            "  private test.C attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       foreignkey=\"attrKey1,attrKey2\"\n"+
            "  */\n"+
            "public class B extends A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attrKey1;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int idA;\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private String idB;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: changing foreignkey attribute (with multiple foreignkeys) of inherited collection where one foreignkey field is not persistent
    public void testForeignkey11()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id1;\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private String id2;\n"+
            "  /** @ojb.collection element-class-ref=\"test.C\"\n"+
            "    *                 foreignkey=\"aid1,aid2\"\n"+
            "    */\n"+
            "  private java.util.List attr;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       foreignkey=\"aid3,aid4\"\n"+
            "  */\n" +
            "public class B extends A {}");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C {\n"+
            "  /** @ojb.field */\n"+
            "  private int aid1;\n"+
            "  /** @ojb.field */\n"+
            "  private int aid3;\n"+
            "  /** @ojb.field */\n"+
            "  private String aid2;\n"+
            "  private String aid4;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: changing foreignkey attribute (with multiple foreignkeys) of inherited collection where the jdbc-type of one foreignkey field doesn't match
    public void testForeignkey12()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id1;\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private String id2;\n"+
            "  /** @ojb.collection element-class-ref=\"test.C\"\n"+
            "    *                 foreignkey=\"aid1,aid2\"\n"+
            "    */\n"+
            "  private java.util.List attr;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       foreignkey=\"aid3,aid4\"\n"+
            "  */\n" +
            "public class B extends A {}");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C {\n"+
            "  /** @ojb.field */\n"+
            "  private int aid1;\n"+
            "  /** @ojb.field */\n"+
            "  private int aid3;\n"+
            "  /** @ojb.field */\n"+
            "  private String aid2;\n"+
            "  /** @ojb.field */\n"+
            "  private int aid4;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: change of foreignkey when used in combination with indirection-table
    public void testForeignkey13()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.collection foreignkey=\"AID\"\n"+
            "    *                 indirection-table=\"A_B\"\n"+
            "    */\n"+
            "  private B[] bs;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.collection element-class-ref=\"test.A\"\n"+
            "    *                 foreignkey=\"BID\"\n"+
            "    *                 indirection-table=\"A_B\"\n"+
            "    */\n"+
            "  private java.util.List as;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.modify-inherited name=\"bs\"\n"+
            "  *                       foreignkey=\"CID\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
