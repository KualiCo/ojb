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
 * Tests for the ojb.runtime-argument and ojb.constant-argument tags.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ProcedureArgumentTagTests extends OjbTestBase
{
    public ProcedureArgumentTagTests(String name)
    {
        super(name);
    }

    // Test: insert with one runtime-argument without field-ref
    public void testOneRuntimeArgument1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.insert-procedure name=\"insert-proc\"\n"+
            "  *                       arguments=\"arg1\"\n"+
            "  * @ojb.runtime-argument name=\"arg1\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <insert-procedure\n"+
            "        name=\"insert-proc\""+
            "    >\n"+
            "        <runtime-argument\n"+
            "        >\n"+
            "        </runtime-argument>\n"+
            "    </insert-procedure>\n"+
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

    // Test: delete with one runtime-argument with valid field-ref
    public void testOneRuntimeArgument2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n"+
            "  *                       arguments=\"arg1\"\n"+
            "  * @ojb.runtime-argument name=\"arg1\"\n"+
            "  *                       field-ref=\"attr\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <delete-procedure\n"+
            "        name=\"delete-proc\""+
            "    >\n"+
            "        <runtime-argument\n"+
            "            field-ref=\"attr\"\n"+
            "        >\n"+
            "        </runtime-argument>\n"+
            "    </delete-procedure>\n"+
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

    // Test: update with one runtime-argument with valid field-ref, with return='true'
    public void testOneRuntimeArgument3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.update-procedure name=\"update-proc\"\n"+
            "  *                       arguments=\"arg1\"\n"+
            "  * @ojb.runtime-argument name=\"arg1\"\n"+
            "  *                       field-ref=\"attr\"\n"+
            "  *                       return=\"true\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <update-procedure\n"+
            "        name=\"update-proc\""+
            "    >\n"+
            "        <runtime-argument\n"+
            "            field-ref=\"attr\"\n"+
            "            return=\"true\"\n"+
            "        >\n"+
            "        </runtime-argument>\n"+
            "    </update-procedure>\n"+
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

    // Test: insert with multiple runtime-argument with valid field-ref
    public void testMultipleRuntimeArguments1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.insert-procedure name=\"insert-proc\"\n"+
            "  *                       arguments=\"arg1,arg2\"\n"+
            "  * @ojb.runtime-argument name=\"arg1\"\n"+
            "  *                       field-ref=\"attr2\"\n"+
            "  * @ojb.runtime-argument name=\"arg2\"\n"+
            "  *                       field-ref=\"attr1\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr1;\n"+
            "  /** @ojb.field */\n"+
            "  private int attr2;\n"+
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
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <insert-procedure\n"+
            "        name=\"insert-proc\"\n"+
            "    >\n"+
            "        <runtime-argument\n"+
            "            field-ref=\"attr2\"\n"+
            "        >\n"+
            "        </runtime-argument>\n"+
            "        <runtime-argument\n"+
            "            field-ref=\"attr1\"\n"+
            "        >\n"+
            "        </runtime-argument>\n"+
            "    </insert-procedure>\n"+
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
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: delete with multiple runtime-argument with valid field-ref, with return='true'
    public void testMultipleRuntimeArguments2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n"+
            "  *                       arguments=\"arg1,arg2,arg1\"\n"+
            "  * @ojb.runtime-argument name=\"arg1\"\n"+
            "  *                       field-ref=\"attr2\"\n"+
            "  * @ojb.runtime-argument name=\"arg2\"\n"+
            "  *                       field-ref=\"attr1\"\n"+
            "  *                       return=\"true\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr1;\n"+
            "  /** @ojb.field */\n"+
            "  private int attr2;\n"+
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
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <delete-procedure\n"+
            "        name=\"delete-proc\"\n"+
            "    >\n"+
            "        <runtime-argument\n"+
            "            field-ref=\"attr2\"\n"+
            "        >\n"+
            "        </runtime-argument>\n"+
            "        <runtime-argument\n"+
            "            field-ref=\"attr1\"\n"+
            "            return=\"true\"\n"+
            "        >\n"+
            "        </runtime-argument>\n"+
            "        <runtime-argument\n"+
            "            field-ref=\"attr2\"\n"+
            "        >\n"+
            "        </runtime-argument>\n"+
            "    </delete-procedure>\n"+
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
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: update with one runtime-argument with invalid field-ref (no such field)
    public void testInvalidRuntimeArgument1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.update-procedure name=\"update-proc\"\n"+
            "  *                       arguments=\"arg1\"\n"+
            "  * @ojb.runtime-argument name=\"arg1\"\n"+
            "  *                       field-ref=\"attr1\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: insert with multiple runtime-argument, one invalid field-ref (field not persistent)
    public void testInvalidRuntimeArguments2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.insert-procedure name=\"insert-proc\"\n"+
            "  *                       arguments=\"arg1,arg2\"\n"+
            "  * @ojb.runtime-argument name=\"arg1\"\n"+
            "  *                       field-ref=\"attr2\"\n"+
            "  * @ojb.runtime-argument name=\"arg2\"\n"+
            "  *                       field-ref=\"attr1\"\n"+
            "  *                       return=\"true\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  private int attr1;\n"+
            "  /** @ojb.field */\n"+
            "  private int attr2;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: delete with one runtime-argument with field-ref with documentation
    public void testRuntimeArgumentDocumentation()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n"+
            "  *                       arguments=\"arg1\"\n"+
            "  * @ojb.runtime-argument name=\"arg1\"\n"+
            "  *                       field-ref=\"attr\"\n"+
            "  *                       documentation=\"Some important argument\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <delete-procedure\n"+
            "        name=\"delete-proc\""+
            "    >\n"+
            "        <runtime-argument\n"+
            "            field-ref=\"attr\"\n"+
            "        >\n"+
            "            <documentation>Some important argument</documentation>\n"+
            "        </runtime-argument>\n"+
            "    </delete-procedure>\n"+
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

    // Test: update with one runtime-argument with field-ref with attributes
    public void testRuntimeArgumentAttributes()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.update-procedure name=\"update-proc\"\n"+
            "  *                       arguments=\"arg1\"\n"+
            "  * @ojb.runtime-argument name=\"arg1\"\n"+
            "  *                       field-ref=\"attr\"\n"+
            "  *                       attributes=\"a=b,c,d=e\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <update-procedure\n"+
            "        name=\"update-proc\""+
            "    >\n"+
            "        <runtime-argument\n"+
            "            field-ref=\"attr\"\n"+
            "        >\n"+
            "            <attribute attribute-name=\"a\" attribute-value=\"b\"/>\n"+
            "            <attribute attribute-name=\"c\" attribute-value=\"\"/>\n"+
            "            <attribute attribute-name=\"d\" attribute-value=\"e\"/>\n"+
            "        </runtime-argument>\n"+
            "    </update-procedure>\n"+
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

    // Test: insert with one constant-argument without value
    public void testOneConstantArgument1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.insert-procedure name=\"insert-proc\"\n"+
            "  *                       arguments=\"arg1\"\n"+
            "  * @ojb.constant-argument name=\"arg1\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <insert-procedure\n"+
            "        name=\"insert-proc\""+
            "    >\n"+
            "        <constant-argument\n"+
            "            value=\"\"\n"+
            "        >\n"+
            "        </constant-argument>\n"+
            "    </insert-procedure>\n"+
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

    // Test: delete with one constant-argument with value
    public void testOneConstantArgument2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n"+
            "  *                       arguments=\"arg1\"\n"+
            "  * @ojb.constant-argument name=\"arg1\"\n"+
            "  *                        value=\"some value\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <delete-procedure\n"+
            "        name=\"delete-proc\""+
            "    >\n"+
            "        <constant-argument\n"+
            "            value=\"some value\"\n"+
            "        >\n"+
            "        </constant-argument>\n"+
            "    </delete-procedure>\n"+
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

    // Test: update with multiple constant-argument with value
    public void testMultipleConstantArguments1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.update-procedure name=\"update-proc\"\n"+
            "  *                       arguments=\"arg1,arg2,arg1\"\n"+
            "  * @ojb.constant-argument name=\"arg1\"\n"+
            "  *                       value=\"0\"\n"+
            "  * @ojb.constant-argument name=\"arg2\"\n"+
            "  *                       value=\"abc\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <update-procedure\n"+
            "        name=\"update-proc\"\n"+
            "    >\n"+
            "        <constant-argument\n"+
            "            value=\"0\"\n"+
            "        >\n"+
            "        </constant-argument>\n"+
            "        <constant-argument\n"+
            "            value=\"abc\"\n"+
            "        >\n"+
            "        </constant-argument>\n"+
            "        <constant-argument\n"+
            "            value=\"0\"\n"+
            "        >\n"+
            "        </constant-argument>\n"+
            "    </update-procedure>\n"+
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

    // Test: insert with multiple constant-argument, one without value
    public void testMultipleConstantArguments2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.insert-procedure name=\"insert-proc\"\n"+
            "  *                       arguments=\"arg1,arg2\"\n"+
            "  * @ojb.constant-argument name=\"arg1\"\n"+
            "  * @ojb.constant-argument name=\"arg2\"\n"+
            "  *                       value=\"abc\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <insert-procedure\n"+
            "        name=\"insert-proc\"\n"+
            "    >\n"+
            "        <constant-argument\n"+
            "            value=\"\"\n"+
            "        >\n"+
            "        </constant-argument>\n"+
            "        <constant-argument\n"+
            "            value=\"abc\"\n"+
            "        >\n"+
            "        </constant-argument>\n"+
            "    </insert-procedure>\n"+
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

    // Test: delete with one constant-argument with documentation
    public void testConstantArgumentDocumentation()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n"+
            "  *                       arguments=\"arg1\"\n"+
            "  * @ojb.constant-argument name=\"arg1\"\n"+
            "  *                        value=\"some value\"\n"+
            "  *                        documentation=\"Some important argument\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <delete-procedure\n"+
            "        name=\"delete-proc\""+
            "    >\n"+
            "        <constant-argument\n"+
            "            value=\"some value\"\n"+
            "        >\n"+
            "            <documentation>Some important argument</documentation>\n"+
            "        </constant-argument>\n"+
            "    </delete-procedure>\n"+
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

    // Test: update with one constant-argument with attributes
    public void testConstantArgumentAttributes()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.update-procedure name=\"update-proc\"\n"+
            "  *                       arguments=\"arg1\"\n"+
            "  * @ojb.constant-argument name=\"arg1\"\n"+
            "  *                        value=\"some value\"\n"+
            "  *                        attributes=\"a=\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr\"\n"+
            "        column=\"attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <update-procedure\n"+
            "        name=\"update-proc\""+
            "    >\n"+
            "        <constant-argument\n"+
            "            value=\"some value\"\n"+
            "        >\n"+
            "            <attribute attribute-name=\"a\" attribute-value=\"\"/>\n"+
            "        </constant-argument>\n"+
            "    </update-procedure>\n"+
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
}
