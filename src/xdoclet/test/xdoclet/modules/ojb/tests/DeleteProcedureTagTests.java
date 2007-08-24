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
 * Tests for the ojb.delete-procedure tag.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class DeleteProcedureTagTests extends OjbTestBase
{
    public DeleteProcedureTagTests(String name)
    {
        super(name);
    }

    // Test: no arguments
    public void testNoAttributes()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <delete-procedure\n"+
            "        name=\"delete-proc\"" +
            "    >\n"+
            "    </delete-procedure>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: without a name
    public void testNoName()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure\n"+
            "  */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: with valid return-field-ref
    public void testValidFieldRef()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n" +
            "  *                       return-field-ref=\"attr\"\n"+
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
            "        name=\"delete-proc\"" +
            "        return-field-ref=\"attr\"" +
            "    >\n"+
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

    // Test: with valid return-field-ref pointing to inherited field
    public void testValidInheritedFieldRef()
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
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n" +
            "  *                       return-field-ref=\"attr\"\n"+
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
            "    </field-descriptor>\n"+
            "    <delete-procedure\n"+
            "        name=\"delete-proc\"\n" +
            "        return-field-ref=\"attr\"\n" +
            "    >\n"+
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
            "    <table name=\"B\">\n"+
            "        <column name=\"attr\"\n"+
            "                javaName=\"attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: with valid return-field-ref pointing to nested field
    public void testValidNestedFieldRef()
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
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n" +
            "  *                       return-field-ref=\"attr::attr\"\n"+
            "  */\n"+
            "public class B {\n"+
            "  /** @ojb.nested */\n"+
            "  private A attr;\n"+
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
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"attr::attr\"\n"+
            "        column=\"attr_attr\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <delete-procedure\n"+
            "        name=\"delete-proc\"\n" +
            "        return-field-ref=\"attr::attr\"\n" +
            "    >\n"+
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
            "    <table name=\"B\">\n"+
            "        <column name=\"attr_attr\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: with invalid return-field-ref (no such field)
    public void testUnknownFieldRef()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n" +
            "  *                       return-field-ref=\"id\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: with invalid return-field-ref (field not persistent)
    public void testNonpersistentFieldRef()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n" +
            "  *                       return-field-ref=\"attr\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  private int attr;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: with one runtime-argument with field-ref
    public void testOneRuntimeArgument()
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

    // Test: with one constant-argument with value
    public void testOneConstantArgument()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n"+
            "  *                       arguments=\"arg1\"\n"+
            "  * @ojb.constant-argument name=\"arg1\"\n"+
            "  *                       value=\"0\"\n"+
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
            "        name=\"delete-proc\"\n"+
            "    >\n"+
            "        <constant-argument\n"+
            "            value=\"0\"\n"+
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

    // Test: with multiple mixed arguments
    public void testMultipleArguments()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.constant-argument name=\"arg2\"\n"+
            "  *                       value=\"0\"\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n"+
            "  *                       arguments=\"arg3,arg1,arg2\"\n"+
            "  * @ojb.runtime-argument name=\"arg1\"\n"+
            "  *                       field-ref=\"attr\"\n"+
            "  * @ojb.constant-argument name=\"arg3\"\n"+
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
            "    <delete-procedure\n"+
            "        name=\"delete-proc\"\n"+
            "    >\n"+
            "        <constant-argument\n"+
            "            value=\"abc\"\n"+
            "        >\n"+
            "        </constant-argument>\n"+
            "        <runtime-argument\n"+
            "            field-ref=\"attr\"\n"+
            "        >\n"+
            "        </runtime-argument>\n"+
            "        <constant-argument\n"+
            "            value=\"0\"\n"+
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

    // Test: with undefined argument
    public void testUndefinedArgument1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n"+
            "  *                       arguments=\"arg1\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: with multiple arguments, one undefined
    public void testUndefinedArgument2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n"+
            "  *                       arguments=\"arg3,arg1,arg2\"\n"+
            "  * @ojb.runtime-argument name=\"arg1\"\n"+
            "  *                       field-ref=\"attr\"\n"+
            "  * @ojb.constant-argument name=\"arg3\"\n"+
            "  *                       value=\"abc\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: with include-pk-only='true'
    public void testIncludePkOnly1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n"+
            "  *                       arguments=\"arg3,arg1,arg2\"\n"+
            "  *                       include-pk-only=\"true\"\n"+
            "  * @ojb.runtime-argument name=\"arg1\"\n"+
            "  *                       field-ref=\"attr\"\n"+
            "  * @ojb.constant-argument name=\"arg2\"\n"+
            "  *                       value=\"0\"\n"+
            "  * @ojb.constant-argument name=\"arg3\"\n"+
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
            "    <delete-procedure\n"+
            "        name=\"delete-proc\"\n"+
            "        include-pk-only=\"true\"\n"+
            "    >\n"+
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

    // Test: with include-pk-only with invalid value
    public void testIncludePkOnly2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.delete-procedure name=\"delete-proc\"\n"+
            "  *                       arguments=\"arg3,arg1,arg2\"\n"+
            "  *                       include-pk-only=\"no\"\n"+
            "  * @ojb.runtime-argument name=\"arg1\"\n"+
            "  *                       field-ref=\"attr\"\n"+
            "  * @ojb.constant-argument name=\"arg2\"\n"+
            "  *                       value=\"0\"\n"+
            "  * @ojb.constant-argument name=\"arg3\"\n"+
            "  *                       value=\"abc\"\n"+
            "  */\n"+
            "public class A {\n"+
            "  /** @ojb.field */\n"+
            "  private int attr;\n"+
            "}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
