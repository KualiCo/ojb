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
 * Tests for the ojb.modify-inherited tag with the documentation attribute.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ModifyInheritedTagDocumentationAttributeTests extends OjbTestBase
{
    public ModifyInheritedTagDocumentationAttributeTests(String name)
    {
        super(name);
    }

    // Test: modifying the documentation attribute of a field in a direct base class
    public void testDocumentation1()
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
            "  *                       documentation=\"Some documentation\"\n"+
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
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "        <documentation>Some documentation</documentation>\n"+
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
            "        <column name=\"attr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "                description=\"Some documentation\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the documentation attribute of a reference in an indirect base class
    public void testDocumentation2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int bid;\n"+
            "  /** @ojb.reference foreignkey=\"bid\"\n"+
            "    *                documentation=\"Some documentation\"\n"+            "    */\n"+
            "  private B b;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class B {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}");
        addClass(
            "test.C",
            "package test;\n"+
            "public class C extends A {}");
        addClass(
            "test.D",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class D extends C {}");
        addClass(
            "test.E",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"b\"\n"+
            "  *                       documentation=\"Some other documentation\"\n"+
            "  */\n"+
            "public class E extends D {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.D\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"bid\"\n"+
            "        column=\"bid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"b\"\n"+
            "        class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <documentation>Some documentation</documentation>\n"+
            "        <foreignkey field-ref=\"bid\"/>\n"+
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
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.D\"\n"+
            "    table=\"D\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.E\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"bid\"\n"+
            "        column=\"bid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"b\"\n"+
            "        class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <documentation>Some documentation</documentation>\n"+
            "        <foreignkey field-ref=\"bid\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.E\"\n"+
            "    table=\"E\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"bid\"\n"+
            "        column=\"bid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"b\"\n"+
            "        class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <documentation>Some other documentation</documentation>\n"+
            "        <foreignkey field-ref=\"bid\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"bid\"\n"+
            "                javaName=\"bid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"bid\" foreign=\"id\"/>\n"+
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
            "    <table name=\"D\">\n"+
            "        <column name=\"bid\"\n"+
            "                javaName=\"bid\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"bid\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"E\">\n"+
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

    // Test: modifying the documentation attribute of a collection in an direct base class
    public void testDocumentation3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.collection element-class-ref=\"test.B\"\n"+
            "    *                 foreignkey=\"aid\"\n"+
            "    *                 documentation=\"Some documentation\"\n"+
            "    *                 database-foreignkey=\"false\"\n"+
            "    */\n"+
            "  private java.util.Collection bs;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class B {\n"+
            "  /** @ojb.field */\n"+
            "  private int aid;\n"+
            "}");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"bs\"\n"+
            "  *                       documentation=\"Some other documentation\"\n"+
            "  */\n"+
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
            "    <extent-class class-ref=\"test.C\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <collection-descriptor\n"+
            "        name=\"bs\"\n"+
            "        element-class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <documentation>Some documentation</documentation>\n"+
            "        <inverse-foreignkey field-ref=\"aid\"/>\n"+
            "    </collection-descriptor>\n"+
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
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.D\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <collection-descriptor\n"+
            "        name=\"bs\"\n"+
            "        element-class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <documentation>Some other documentation</documentation>\n"+
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
            "    <collection-descriptor\n"+
            "        name=\"bs\"\n"+
            "        element-class-ref=\"test.B\"\n"+
            "    >\n"+
            "        <documentation>Some other documentation</documentation>\n"+
            "        <inverse-foreignkey field-ref=\"aid\"/>\n"+
            "    </collection-descriptor>\n"+
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

    // Test: modifying the documentation attribute of an anonymous field in a direct base class
    public void testDocumentation4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.field name=\"attr\"\n"+
            "  *            jdbc-type=\"INTEGER\"\n"+
            "  */\n" +
            "public class A {}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       documentation=\"Some documentation\"\n"+
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
            "        access=\"anonymous\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        access=\"anonymous\"\n"+
            "    >\n"+
            "        <documentation>Some documentation</documentation>\n"+
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
            "        <column name=\"attr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "                description=\"Some documentation\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: unsetting the documentation attribute of a field in a direct base class
    public void testDocumentation5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field documentation=\"Some documentation\" */\n"+
            "  private int attr;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       documentation=\"\"\n"+
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
            "        <documentation>Some documentation</documentation>\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
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
            "                description=\"Some documentation\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"attr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
