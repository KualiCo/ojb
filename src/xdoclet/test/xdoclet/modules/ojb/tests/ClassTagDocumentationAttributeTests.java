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
 * Tests for the ojb.class tag with the documentation attribute.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ClassTagDocumentationAttributeTests extends OjbTestBase
{
    public ClassTagDocumentationAttributeTests(String name)
    {
        super(name);
    }

    // Test of documentation attribute: no value
    public void testDocumentation1()
    {
        addClass(
            "A",
            "/** @ojb.class documentation=\"\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"A\"\n"+
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

    // Test of documentation attribute: with value
    public void testDocumentation2()
    {
        addClass(
            "A",
            "/** @ojb.class documentation=\"some documentation\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <documentation>some documentation</documentation>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\"\n"+
            "           description=\"some documentation\"\n"+
            "    >\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of documentation attribute: with value, not inherited in subclass
    public void testDocumentation3()
    {
        addClass(
            "A",
            "/** @ojb.class documentation=\"some documentation\" */\n"+
            "public class A {}\n");
        addClass(
            "B",
            "/** @ojb.class */\n"+
            "public class B extends A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <documentation>some documentation</documentation>\n"+
            "    <extent-class class-ref=\"B\"/>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\"\n"+
            "           description=\"some documentation\"\n"+
            "    >\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: a more complex structure 
    public void testDocumentation4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class documentation=\"Class A\" */\n"+
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private Integer id;\n"+
            "  /** @ojb.collection element-class-ref=\"test.B\"\n"+
            "    *                 foreignkey=\"aid\"\n"+
            "    */\n"+
            "  private java.util.List bs;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class documentation=\"Class B\" */\n"+
            "public class B {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private Integer aid;\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private Integer cid;\n"+
            "  /** @ojb.reference foreignkey=\"cid\" */\n"+
            "  private C c;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class documentation=\"Class C\" */\n"+
            "public class C {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private Integer id;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <documentation>Class A</documentation>\n"+
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
            "        <inverse-foreignkey field-ref=\"aid\"/>\n"+
            "    </collection-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <documentation>Class B</documentation>\n"+
            "    <field-descriptor\n"+
            "        name=\"aid\"\n"+
            "        column=\"aid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"cid\"\n"+
            "        column=\"cid\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"c\"\n"+
            "        class-ref=\"test.C\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"cid\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <documentation>Class C</documentation>\n"+
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
            "    <table name=\"A\"\n"+
            "           description=\"Class A\"\n"+
            "    >\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"B\"\n"+
            "           description=\"Class B\"\n"+
            "    >\n"+
            "        <column name=\"aid\"\n"+
            "                javaName=\"aid\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "        <column name=\"cid\"\n"+
            "                javaName=\"cid\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"A\">\n"+
            "            <reference local=\"aid\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "        <foreign-key foreignTable=\"C\">\n"+
            "            <reference local=\"cid\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"C\"\n"+
            "           description=\"Class C\"\n"+
            "    >\n"+
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
}
