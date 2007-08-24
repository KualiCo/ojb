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
 * Tests for the ojb.modify-inherited tag with the autoincrement attribute.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ModifyInheritedTagAutoIncrementAttributeTests extends OjbTestBase
{
    public ModifyInheritedTagAutoIncrementAttributeTests(String name)
    {
        super(name);
    }

    // Test: modifying the autoincrement attribute of a field in a direct base class
    public void testAutoincrement1()
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
            "  *                       autoincrement=\"ojb\"\n"+
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
            "        autoincrement=\"true\"\n"+
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
            "        <column name=\"attr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the autoincrement attribute of a reference
    public void testAutoincrement2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int bid;\n"+
            "  /** @ojb.reference foreignkey=\"bid\" */\n"+
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
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"b\"\n"+
            "  *                       autoincrement=\"ojb\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the autoincrement attribute of a collection
    public void testAutoincrement3()
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
            "  *                       autoincrement=\"database\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the autoincrement attribute of an anonymous field in a direct base class
    public void testAutoincrement4()
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
            "  *                       autoincrement=\"database\"\n"+
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
            "        autoincrement=\"true\"\n"+
            "        access=\"anonymous\"\n"+
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
            "        <column name=\"attr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "                autoIncrement=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: invalid value
    public void testAutoincrement5()
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
            "  *                       autoincrement=\"false\"\n"+
            "  */\n"+
            "public class B extends A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying value when sequence-name is used
    public void testAutoincrement6()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field autoincrement=\"ojb\"\n"+
            "    *            sequence-name=\"SomeSequence\"\n"+
            "    */\n"+
            "  private int attr;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       autoincrement=\"database\"\n"+
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
            "        autoincrement=\"true\"\n"+
            "        sequence-name=\"SomeSequence\"\n"+
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
            "        autoincrement=\"true\"\n"+
            "        sequence-name=\"SomeSequence\"\n"+
            "        access=\"readonly\"\n"+
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
            "        <column name=\"attr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "                autoIncrement=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
