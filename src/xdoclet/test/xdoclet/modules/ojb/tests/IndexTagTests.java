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
 * Tests for the ojb.index tag.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class IndexTagTests extends OjbTestBase
{
    public IndexTagTests(String name)
    {
        super(name);
    }

    // Test: use of indexed-attribute and ojb.index in an inheritance hierarchy
    public void testSimple1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.index name=\"idx\"\n"+
            "  *            fields=\"attr3,attr2\"\n"+
            "  */\n"+
            "public class A {\n"+
            "/** @ojb.field indexed=\"true\" */\n"+
            "  private int attr1;\n"+
            "/** @ojb.field */\n"+
            "  private String attr2;\n"+
            "/** @ojb.field */\n"+
            "  private java.util.Date attr3;\n"+
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
            "  * @ojb.index name=\"idx\"\n"+
            "  *            fields=\"attr1,attr2\"\n"+
            "  * @ojb.modify-inherited name=\"attr1\"\n"+
            "  *                       indexed=\"false\"\n"+
            "  */\n"+
            "public class C extends B {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr1\"\n"+
            "        column=\"attr1\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        indexed=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr2\"\n"+
            "        column=\"attr2\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr3\"\n"+
            "        column=\"attr3\"\n"+
            "        jdbc-type=\"DATE\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <index-descriptor\n" +
            "        name=\"idx\"\n" +
            "    >\n"+
            "        <index-column name=\"attr3\"/>\n"+
            "        <index-column name=\"attr2\"/>\n"+
            "    </index-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.C\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr1\"\n"+
            "        column=\"attr1\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        indexed=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr2\"\n"+
            "        column=\"attr2\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr3\"\n"+
            "        column=\"attr3\"\n"+
            "        jdbc-type=\"DATE\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.C\"\n"+
            "    table=\"C\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr1\"\n"+
            "        column=\"attr1\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        indexed=\"false\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr2\"\n"+
            "        column=\"attr2\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr3\"\n"+
            "        column=\"attr3\"\n"+
            "        jdbc-type=\"DATE\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <index-descriptor\n" +
            "        name=\"idx\"\n" +
            "    >\n"+
            "        <index-column name=\"attr1\"/>\n"+
            "        <index-column name=\"attr2\"/>\n"+
            "    </index-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attr1\"\n"+
            "                javaName=\"attr1\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attr2\"\n"+
            "                javaName=\"attr2\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "        <column name=\"attr3\"\n"+
            "                javaName=\"attr3\"\n"+
            "                type=\"DATE\"\n"+
            "        />\n"+
            "        <index>\n"+
            "            <index-column name=\"attr1\"/>\n"+
            "        </index>\n"+
            "        <index name=\"idx\">\n"+
            "            <index-column name=\"attr3\"/>\n"+
            "            <index-column name=\"attr2\"/>\n"+
            "        </index>\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"attr1\"\n"+
            "                javaName=\"attr1\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attr2\"\n"+
            "                javaName=\"attr2\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "        <column name=\"attr3\"\n"+
            "                javaName=\"attr3\"\n"+
            "                type=\"DATE\"\n"+
            "        />\n"+
            "        <index>\n"+
            "            <index-column name=\"attr1\"/>\n"+
            "        </index>\n"+
            "    </table>\n"+
            "    <table name=\"C\">\n"+
            "        <column name=\"attr1\"\n"+
            "                javaName=\"attr1\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attr2\"\n"+
            "                javaName=\"attr2\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "        <column name=\"attr3\"\n"+
            "                javaName=\"attr3\"\n"+
            "                type=\"DATE\"\n"+
            "        />\n"+
            "        <index name=\"idx\">\n"+
            "            <index-column name=\"attr1\"/>\n"+
            "            <index-column name=\"attr2\"/>\n"+
            "        </index>\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: use of ojb.index without a name
    public void testSimple2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.index fields=\"attr3,attr2\"\n"+
            "  */\n"+
            "public class A {\n"+
            "/** @ojb.field indexed=\"true\" */\n"+
            "  private int attr1;\n"+
            "/** @ojb.field */\n"+
            "  private String attr2;\n"+
            "/** @ojb.field */\n"+
            "  private java.util.Date attr3;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: unique ojb.index and default index on a field with a column attribute
    public void testSimple3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.index name=\"idx\"\n"+
            "  *            fields=\"attr1,attr2\"\n"+
            "  *            unique=\"true\"\n"+
            "  */\n"+
            "public class A {\n"+
            "/** @ojb.field column=\"ATTR\"\n"+
            "  *            indexed=\"true\"\n"+
            "  */\n"+
            "  private int attr1;\n"+
            "/** @ojb.field */\n"+
            "  private String attr2;\n"+
            "/** @ojb.field */\n"+
            "  private java.util.Date attr3;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr1\"\n"+
            "        column=\"ATTR\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        indexed=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr2\"\n"+
            "        column=\"attr2\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr3\"\n"+
            "        column=\"attr3\"\n"+
            "        jdbc-type=\"DATE\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <index-descriptor\n" +
            "        name=\"idx\"\n" +
            "        unique=\"true\"\n" +
            "    >\n"+
            "        <index-column name=\"ATTR\"/>\n"+
            "        <index-column name=\"attr2\"/>\n"+
            "    </index-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"ATTR\"\n"+
            "                javaName=\"attr1\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attr2\"\n"+
            "                javaName=\"attr2\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "        <column name=\"attr3\"\n"+
            "                javaName=\"attr3\"\n"+
            "                type=\"DATE\"\n"+
            "        />\n"+
            "        <index>\n"+
            "            <index-column name=\"ATTR\"/>\n"+
            "        </index>\n"+
            "        <unique name=\"idx\">\n"+
            "            <unique-column name=\"ATTR\"/>\n"+
            "            <unique-column name=\"attr2\"/>\n"+
            "        </unique>\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: two ojb.index' with the same name, one is unique, one is not
    public void testSimple4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.index name=\"idx\"\n"+
            "  *            fields=\"attr1,attr2\"\n"+
            "  *            unique=\"true\"\n"+
            "  * @ojb.index name=\"idx\"\n"+
            "  *            fields=\"attr1,attr3\"\n"+
            "  *            unique=\"false\"\n"+
            "  */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attr1;\n"+
            "/** @ojb.field */\n"+
            "  private String attr2;\n"+
            "/** @ojb.field */\n"+
            "  private java.util.Date attr3;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr1\"\n"+
            "        column=\"attr1\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr2\"\n"+
            "        column=\"attr2\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr3\"\n"+
            "        column=\"attr3\"\n"+
            "        jdbc-type=\"DATE\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <index-descriptor\n" +
            "        name=\"idx\"\n" +
            "        unique=\"true\"\n" +
            "    >\n"+
            "        <index-column name=\"attr1\"/>\n"+
            "        <index-column name=\"attr2\"/>\n"+
            "    </index-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attr1\"\n"+
            "                javaName=\"attr1\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attr2\"\n"+
            "                javaName=\"attr2\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "        <column name=\"attr3\"\n"+
            "                javaName=\"attr3\"\n"+
            "                type=\"DATE\"\n"+
            "        />\n"+
            "        <unique name=\"idx\">\n"+
            "            <unique-column name=\"attr1\"/>\n"+
            "            <unique-column name=\"attr2\"/>\n"+
            "        </unique>\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: documentation attribute with empty value
    public void testDocumentation1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.index name=\"idx\"\n"+
            "  *            fields=\"attr3,attr1\"\n"+
            "  *            documentation=\"\"\n"+
            "  *            unique=\"true\"\n"+
            "  */\n"+
            "public class A {\n"+
            "/** @ojb.field column=\"ATTR\" */\n"+
            "  private int attr1;\n"+
            "/** @ojb.field */\n"+
            "  private String attr2;\n"+
            "/** @ojb.field */\n"+
            "  private java.util.Date attr3;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr1\"\n"+
            "        column=\"ATTR\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr2\"\n"+
            "        column=\"attr2\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr3\"\n"+
            "        column=\"attr3\"\n"+
            "        jdbc-type=\"DATE\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <index-descriptor\n" +
            "        name=\"idx\"\n" +
            "        unique=\"true\"\n" +
            "    >\n"+
            "        <index-column name=\"attr3\"/>\n"+
            "        <index-column name=\"ATTR\"/>\n"+
            "    </index-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"ATTR\"\n"+
            "                javaName=\"attr1\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attr2\"\n"+
            "                javaName=\"attr2\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "        <column name=\"attr3\"\n"+
            "                javaName=\"attr3\"\n"+
            "                type=\"DATE\"\n"+
            "        />\n"+
            "        <unique name=\"idx\">\n"+
            "            <unique-column name=\"attr3\"/>\n"+
            "            <unique-column name=\"ATTR\"/>\n"+
            "        </unique>\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: documentation attribute with empty value
    public void testDocumentation2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.index name=\"idx\"\n"+
            "  *            documentation=\"Some documentation\"\n"+
            "  *            fields=\"attr3,attr1\"\n"+
            "  */\n"+
            "public class A {\n"+
            "/** @ojb.field column=\"ATTR\" */\n"+
            "  private int attr1;\n"+
            "/** @ojb.field */\n"+
            "  private String attr2;\n"+
            "/** @ojb.field */\n"+
            "  private java.util.Date attr3;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr1\"\n"+
            "        column=\"ATTR\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr2\"\n"+
            "        column=\"attr2\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"attr3\"\n"+
            "        column=\"attr3\"\n"+
            "        jdbc-type=\"DATE\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <index-descriptor\n" +
            "        name=\"idx\"\n" +
            "    >\n"+
            "        <documentation>Some documentation</documentation>\n"+
            "        <index-column name=\"attr3\"/>\n"+
            "        <index-column name=\"ATTR\"/>\n"+
            "    </index-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"ATTR\"\n"+
            "                javaName=\"attr1\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"attr2\"\n"+
            "                javaName=\"attr2\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "        <column name=\"attr3\"\n"+
            "                javaName=\"attr3\"\n"+
            "                type=\"DATE\"\n"+
            "        />\n"+
            "        <index name=\"idx\">\n"+
            "            <index-column name=\"attr3\"/>\n"+
            "            <index-column name=\"ATTR\"/>\n"+
            "        </index>\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
