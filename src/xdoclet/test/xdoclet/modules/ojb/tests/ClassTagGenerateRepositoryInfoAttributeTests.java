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
 * Tests for the ojb.class tag with the generate-repository-info attribute.
 *
 * @author <a href="mailto:tomdz@apache.org">Thomas Dudziak (tomdz@apache.org)</a>
 */
public class ClassTagGenerateRepositoryInfoAttributeTests extends OjbTestBase
{
    public ClassTagGenerateRepositoryInfoAttributeTests(String name)
    {
        super(name);
    }

    // Test: empty value
    public void testGenerateRepositoryInfo1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class generate-repository-info=\"\" */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Testinfo: invalid value
    public void testGenerateRepositoryInfo2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class generate-repository-info=\"yes\" */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: 'true' value
    public void testGenerateRepositoryInfo3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class generate-repository-info=\"true\" */\n"+
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

    // Test: 'false' value with remote base- and subclass and a reference
    public void testGenerateRepositoryInfo4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "public class B extends A {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class generate-repository-info=\"false\" */\n"+
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
            ">\n"+
            "    <extent-class class-ref=\"test.E\"/>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.E\"\n"+
            "    table=\"E\"\n"+
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
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"E\">\n"+
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

    // Test: interface w/o table but with persistent subclasses, is used as the reference target
    public void testGenerateRepositoryInfo5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class generate-table-info=\"false\" */\n"+
            "public interface A {}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B implements A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C implements A {\n"+
            "  /** @ojb.field jdbc-type=\"INTEGER\"\n"+
            "    *            primarykey=\"true\"\n"+
            "    */\n"+
            "  private short id;\n"+
            "}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class D {\n"+
            "  /** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "  /** @ojb.reference foreignkey=\"attrKey\" */\n"+
            "  private A attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
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
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.D\"\n"+
            "    table=\"D\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
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
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: abstract baseclass w/o table is used as the collection element class (1:n)
    public void testGenerateRepositoryInfo6()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class generate-repository-info=\"false\" */\n"+
            "public abstract class A {}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B extends A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.field */\n"+
            "  private int eid;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "public class C extends A {\n"+
            "  /** @ojb.field */\n"+
            "  private int eid;\n"+
            "}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class D extends C {\n" +
            "  /** @ojb.field primarykey=\"true\"\n"+
            "    *            length=\"50\"\n"+
            "    *            id=\"0\"\n"+
            "    */\n"+
            "  private String id;\n"+
            "}\n");
        addClass(
            "test.E",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class E {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.collection element-class-ref=\"test.A\"\n"+
            "    *                 foreignkey=\"eid\"\n"+
            "    */\n"+
            "  private java.util.List as;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
            "    <extent-class class-ref=\"test.D\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"eid\"\n"+
            "        column=\"eid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
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
            "    <field-descriptor\n"+
            "        name=\"eid\"\n"+
            "        column=\"eid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
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
            "        jdbc-type=\"VARCHAR\"\n"+
            "        primarykey=\"true\"\n"+
            "        length=\"50\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"eid\"\n"+
            "        column=\"eid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.E\"\n"+
            "    table=\"E\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <collection-descriptor\n"+
            "        name=\"as\"\n"+
            "        element-class-ref=\"test.A\"\n"+
            "    >\n"+
            "        <inverse-foreignkey field-ref=\"eid\"/>\n"+
            "    </collection-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "        <column name=\"eid\"\n"+
            "                javaName=\"eid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"E\">\n"+
            "            <reference local=\"eid\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"D\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "                size=\"50\"\n"+
            "        />\n"+
            "        <column name=\"eid\"\n"+
            "                javaName=\"eid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"E\">\n"+
            "            <reference local=\"eid\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"E\">\n"+
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

    // Test: baseclass w/o table is used as the collection element class (m:n)
    public void testGenerateRepositoryInfo8()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class generate-repository-info=\"false\" */\n"+
            "public interface A {}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B implements A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.collection element-class-ref=\"test.D\"\n"+
            "    *                 indirection-table=\"A_D\"\n"+
            "    *                 foreignkey=\"AID\"\n"+
            "    */\n"+
            "  private java.util.List ds;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C implements A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.collection element-class-ref=\"test.D\"\n"+
            "    *                 indirection-table=\"A_D\"\n"+
            "    *                 foreignkey=\"AID\"\n"+
            "    */\n"+
            "  private java.util.List ds;\n"+
            "}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class D {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.collection element-class-ref=\"test.A\"\n"+
            "    *                 indirection-table=\"A_D\"\n"+
            "    *                 foreignkey=\"DID\"\n"+
            "    */\n"+
            "  private java.util.List as;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
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
            "        name=\"ds\"\n"+
            "        element-class-ref=\"test.D\"\n"+
            "        indirection-table=\"A_D\"\n"+
            "    >\n"+
            "        <fk-pointing-to-this-class column=\"AID\"/>\n"+
            "        <fk-pointing-to-element-class column=\"DID\"/>\n"+
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
            "        indirection-table=\"A_D\"\n"+
            "    >\n"+
            "        <fk-pointing-to-this-class column=\"AID\"/>\n"+
            "        <fk-pointing-to-element-class column=\"DID\"/>\n"+
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
            "    <collection-descriptor\n"+
            "        name=\"as\"\n"+
            "        element-class-ref=\"test.A\"\n"+
            "        indirection-table=\"A_D\"\n"+
            "    >\n"+
            "        <fk-pointing-to-this-class column=\"DID\"/>\n"+
            "        <fk-pointing-to-element-class column=\"AID\"/>\n"+
            "    </collection-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A_D\">\n"+
            "        <column name=\"AID\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"DID\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"D\">\n"+
            "            <reference local=\"DID\" foreign=\"id\"/>\n"+
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

    // Test: baseclass w/o table is used as the collection element class (m:n)
    public void testGenerateRepositoryInfo9()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class generate-repository-info=\"false\" */\n"+
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\"\n"+
            "   *             length=\"50\"\n"+
            "   */\n"+
            "  private String id1;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B extends A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id2;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C extends A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id2;\n"+
            "}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class D {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.collection element-class-ref=\"test.A\"\n"+
            "    *                 indirection-table=\"A_D\"\n"+
            "    *                 foreignkey=\"DID\"\n"+
            "    *                 remote-foreignkey=\"AID1,AID2\"\n"+
            "    */\n"+
            "  private java.util.List as;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
            "    <extent-class class-ref=\"test.C\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"id1\"\n"+
            "        column=\"id1\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        primarykey=\"true\"\n"+
            "        length=\"50\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"id2\"\n"+
            "        column=\"id2\"\n"+
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
            "        name=\"id1\"\n"+
            "        column=\"id1\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        primarykey=\"true\"\n"+
            "        length=\"50\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"id2\"\n"+
            "        column=\"id2\"\n"+
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
            "        name=\"id1\"\n"+
            "        column=\"id1\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        primarykey=\"true\"\n"+
            "        length=\"50\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"id2\"\n"+
            "        column=\"id2\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
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
            "    <collection-descriptor\n"+
            "        name=\"as\"\n"+
            "        element-class-ref=\"test.A\"\n"+
            "        indirection-table=\"A_D\"\n"+
            "    >\n"+
            "        <fk-pointing-to-this-class column=\"DID\"/>\n"+
            "        <fk-pointing-to-element-class column=\"AID1\"/>\n"+
            "        <fk-pointing-to-element-class column=\"AID2\"/>\n"+
            "    </collection-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A_D\">\n"+
            "        <column name=\"DID\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"AID1\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"50\"\n"+
            "        />\n"+
            "        <column name=\"AID2\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"D\">\n"+
            "            <reference local=\"DID\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"id1\"\n"+
            "                javaName=\"id1\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "                size=\"50\"\n"+
            "        />\n"+
            "        <column name=\"id2\"\n"+
            "                javaName=\"id2\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"id1\"\n"+
            "                javaName=\"id1\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "                size=\"50\"\n"+
            "        />\n"+
            "        <column name=\"id2\"\n"+
            "                javaName=\"id2\"\n"+
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
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: abstract baseclass w/o table is used as the collection element class (1:n)
    //       but the subclasses differ in fks-parts
    public void testGenerateRepositoryInfo7()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class generate-repository-info=\"false\" */\n"+
            "public abstract class A {\n"+
            "  /** @ojb.field length=\"50\" */\n"+
            "  private String did1;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B extends A {\n"+
            "  /** @ojb.field */\n"+
            "  private int did2;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C extends A {\n"+
            "  /** @ojb.field */\n"+
            "  private float did2;\n"+
            "}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class D {\n"+
            "  /** @ojb.field primarykey=\"true\"\n"+
            "    *            length=\"50\"\n"+
            "    */\n"+
            "  private String id1;\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id2;\n"+
            "  /** @ojb.collection element-class-ref=\"test.A\"\n"+
            "    *                 foreignkey=\"did1,did2\"\n"+
            "    */\n"+
            "  private java.util.List as;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: baseinterface w/o table is used as the reference target but the fk jdbc-types are different
    public void testGenerateRepositoryInfo10()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class generate-table-info=\"false\" */\n"+
            "public interface A {}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B implements A {\n"+
            "  /** @ojb.field */\n"+
            "  private int id;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class C implements A {\n"+
            "  /** @ojb.field */\n"+
            "  private float id;\n"+
            "}\n");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class D {\n"+
            "  /** @ojb.field */\n"+
            "  private int aid;\n"+
            "  /** @ojb.reference foreignkey=\"aid\"\n"+
            "    */\n"+
            "  private A a;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
