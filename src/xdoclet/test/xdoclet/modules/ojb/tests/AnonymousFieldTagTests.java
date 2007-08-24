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
 * Tests for the ojb.field tag placed in the class javadoc comment.
 *
 * @author <a href="mailto:tomdz@apache.org">Thomas Dudziak</a>
 */
public class AnonymousFieldTagTests extends OjbTestBase
{
    public AnonymousFieldTagTests(String name)
    {
        super(name);
    }

    /**
     * Test: no sub- or baseclass
     */
    public void testSimple1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.field name=\"attr\"\n"+
            "  *            jdbc-type=\"INTEGER\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
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
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    /**
     * Test: in non-persistent class with a persistent subclass
     */
    public void testSimple2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.field name=\"attr\"\n"+
            "  *            jdbc-type=\"INTEGER\""+
            "  */\n"+
            "public class A {}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B extends A {}\n");

        assertEqualsOjbDescriptorFile(
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
            "    </field-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"attr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    /**
     * Test: persistent inherited field has same name
     */
    public void testSimple3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.field name=\"attr\"\n"+
            "  *            jdbc-type=\"INTEGER\""+
            "  */\n"+
            "public class B extends A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    /**
     * Test: persistent collection in subclass has same name
     */
    public void testSimple4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.field name=\"attr\"\n"+
            "  *            jdbc-type=\"INTEGER\""+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class B extends A {\n"+
            "  /** @ojb.collection element-class-ref=\"test.C\"\n"+
            "    *                 foreignkey=\"aid\"\n"+
            "    */\n"+
            "  private java.util.List attr;\n"+
            "}");
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

    /**
     * Test: two anonymous fields with the same name
     */
    public void testSimple5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.field name=\"attr\"\n"+
            "  *            jdbc-type=\"INTEGER\""+
            "  * @ojb.field name=\"attr\"\n"+
            "  *            jdbc-type=\"VARCHAR\""+
            "  */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
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
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    /**
     * Test: anonymous field in a remote base class with the same name
     */
    public void testSimple6()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.field name=\"attr\"\n"+
            "  *            jdbc-type=\"INTEGER\""+
            "  */\n"+
            "public class A {}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "public class B extends A {}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.field name=\"attr\"\n"+
            "  *            jdbc-type=\"VARCHAR\""+
            "  */\n"+
            "public class C extends B {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    /**
     * Test: missing name attribute
     */
    public void testSimple7()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.field column=\"attr\"\n"+
            "  *            jdbc-type=\"INTEGER\""+
            "  */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    /**
     * Test: empty name attribute
     */
    public void testSimple8()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.field name=\"\"\n"+
            "  *            jdbc-type=\"INTEGER\""+
            "  */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    /**
     * Test: there exists a persistent field with same name
     */
    public void testSimple9()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.field name=\"attr\"\n"+
            "  *            jdbc-type=\"INTEGER\""+
            "  */\n"+
            "public class A {\n"+
            " /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
