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
 * Tests for the ojb.modify-inherited tag with the proxy attribute.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ModifyInheritedTagProxyAttributeTests extends OjbTestBase
{
    public ModifyInheritedTagProxyAttributeTests(String name)
    {
        super(name);
    }

    // Test: modifying the proxy attribute of a field in a direct base class
    public void testProxy1()
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
            "  *                       proxy=\"true\"\n"+
            "  */\n"+
            "public class B extends A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the proxy attribute of a reference
    public void testProxy2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference foreignkey=\"attrKey\"\n"+
            "  *                proxy=\"true\"\n"+
            "  */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       proxy=\"false\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.C\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.B\"\n"+
            "        proxy=\"true\"\n"+
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
            "    >\n"+
            "    </field-descriptor>\n"+
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
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.B\"\n"+
            "        proxy=\"false\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKey\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
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
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the proxy attribute of a collection
    public void testProxy3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "/** @ojb.collection element-class-ref=\"test.B\"\n"+
            "  *                 foreignkey=\"aid\"\n"+
            "  *                 proxy=\"false\"\n"+
            "  *                 database-foreignkey=\"false\"\n"+
            "  */\n"+
            "  private java.util.List objs;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field */\n"+
            "  private int aid;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"objs\"\n"+
            "  *                       proxy=\"true\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

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
            "        name=\"objs\"\n"+
            "        element-class-ref=\"test.B\"\n"+
            "        proxy=\"false\"\n"+
            "    >\n"+
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
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <collection-descriptor\n"+
            "        name=\"objs\"\n"+
            "        element-class-ref=\"test.B\"\n"+
            "        proxy=\"true\"\n"+
            "    >\n"+
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
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the proxy attribute of an anonymous field in a direct base class
    public void testProxy4()
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
            "  *                       proxy=\"false\"\n"+
            "  */\n"+
            "public class B extends A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: invalid value
    public void testProxy5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference foreignkey=\"attrKey\"\n"+
            "  *                proxy=\"true\"\n"+
            "  */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       proxy=\"no\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the proxy-prefetching-limit attribute of a field in a direct base class
    public void testProxyPrefetchingLimit1()
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
            "  *                       proxy-prefetching-limit=\"true\"\n"+
            "  */\n"+
            "public class B extends A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the proxy-prefetching-limit attribute of a reference
    public void testProxyPrefetchingLimit2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference foreignkey=\"attrKey\"\n"+
            "  *                proxy=\"true\"\n"+
            "  */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       proxy-prefetching-limit=\"2\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.C\"/>\n"+
            "    <field-descriptor\n"+
            "        name=\"attrKey\"\n"+
            "        column=\"attrKey\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.B\"\n"+
            "        proxy=\"true\"\n"+
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
            "    >\n"+
            "    </field-descriptor>\n"+
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
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.B\"\n"+
            "        proxy=\"true\"\n"+
            "        proxy-prefetching-limit=\"2\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKey\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
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
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the proxy and proxy-prefetching-limit attributes of a reference
    public void testProxyPrefetchingLimit3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference foreignkey=\"attrKey\" */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       proxy-prefetching-limit=\"1\"\n"+
            "  *                       proxy=\"true\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <extent-class class-ref=\"test.C\"/>\n"+
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
            "    >\n"+
            "    </field-descriptor>\n"+
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
            "    <reference-descriptor\n"+
            "        name=\"attr\"\n"+
            "        class-ref=\"test.B\"\n"+
            "        proxy=\"true\"\n"+
            "        proxy-prefetching-limit=\"1\"\n"+
            "    >\n"+
            "        <foreignkey field-ref=\"attrKey\"/>\n"+
            "    </reference-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
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
            "        <column name=\"attrKey\"\n"+
            "                javaName=\"attrKey\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the proxy and proxy-prefetching-limit attributes of a reference
    public void testProxyPrefetchingLimit4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference foreignkey=\"attrKey\"\n"+
            "  *                proxy=\"true\"\n"+
            "  */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       proxy-prefetching-limit=\"1\"\n"+
            "  *                       proxy=\"false\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

        assertEqualsOjbDescriptorFile(
                "<class-descriptor\n"+
                "    class=\"test.A\"\n"+
                "    table=\"A\"\n"+
                ">\n"+
                "    <extent-class class-ref=\"test.C\"/>\n"+
                "    <field-descriptor\n"+
                "        name=\"attrKey\"\n"+
                "        column=\"attrKey\"\n"+
                "        jdbc-type=\"INTEGER\"\n"+
                "    >\n"+
                "    </field-descriptor>\n"+
                "    <reference-descriptor\n"+
                "        name=\"attr\"\n"+
                "        class-ref=\"test.B\"\n"+
                "        proxy=\"true\"\n"+
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
                "    >\n"+
                "    </field-descriptor>\n"+
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
                "    <reference-descriptor\n"+
                "        name=\"attr\"\n"+
                "        class-ref=\"test.B\"\n"+
                "        proxy=\"false\"\n"+
                "    >\n"+
                "        <foreignkey field-ref=\"attrKey\"/>\n"+
                "    </reference-descriptor>\n"+
                "</class-descriptor>",
                runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
                "<database name=\"ojbtest\">\n"+
                "    <table name=\"A\">\n"+
                "        <column name=\"attrKey\"\n"+
                "                javaName=\"attrKey\"\n"+
                "                type=\"INTEGER\"\n"+
                "        />\n"+
                "        <foreign-key foreignTable=\"B\">\n"+
                "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
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
                "        <column name=\"attrKey\"\n"+
                "                javaName=\"attrKey\"\n"+
                "                type=\"INTEGER\"\n"+
                "        />\n"+
                "        <foreign-key foreignTable=\"B\">\n"+
                "            <reference local=\"attrKey\" foreign=\"id\"/>\n"+
                "        </foreign-key>\n"+
                "    </table>\n"+
                "</database>",
                runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the proxy-prefetching-limit attribute of a reference
    public void testProxyPrefetchingLimit5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field */\n"+
            "  private int attrKey;\n"+
            "/** @ojb.reference foreignkey=\"attrKey\"\n"+
            "  *                proxy=\"true\"\n"+
            "  */\n"+
            "  private test.B attr;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"attr\"\n"+
            "  *                       proxy-prefetching-limit=\"-1\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the proxy attribute of a collection
    public void testProxyPrefetchingLimit6()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "/** @ojb.collection element-class-ref=\"test.B\"\n"+
            "  *                 foreignkey=\"aid\"\n"+
            "  *                 proxy=\"true\"\n"+
            "  *                 database-foreignkey=\"false\"\n"+
            "  */\n"+
            "  private java.util.List objs;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field */\n"+
            "  private int aid;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"objs\"\n"+
            "  *                       proxy-prefetching-limit=\"10\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

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
            "        name=\"objs\"\n"+
            "        element-class-ref=\"test.B\"\n"+
            "        proxy=\"true\"\n"+
            "    >\n"+
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
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <collection-descriptor\n"+
            "        name=\"objs\"\n"+
            "        element-class-ref=\"test.B\"\n"+
            "        proxy=\"true\"\n"+
            "        proxy-prefetching-limit=\"10\"\n"+
            "    >\n"+
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
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the proxy and proxy-prefetching-limit attributes of a collection
    public void testProxyPrefetchingLimit7()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "/** @ojb.collection element-class-ref=\"test.B\"\n"+
            "  *                 foreignkey=\"aid\"\n"+
            "  *                 proxy=\"false\"\n"+
            "  *                 database-foreignkey=\"false\"\n"+
            "  */\n"+
            "  private java.util.List objs;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field */\n"+
            "  private int aid;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"objs\"\n"+
            "  *                       proxy-prefetching-limit=\"10\"\n"+
            "  *                       proxy=\"true\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

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
            "        name=\"objs\"\n"+
            "        element-class-ref=\"test.B\"\n"+
            "        proxy=\"false\"\n"+
            "    >\n"+
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
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <collection-descriptor\n"+
            "        name=\"objs\"\n"+
            "        element-class-ref=\"test.B\"\n"+
            "        proxy=\"true\"\n"+
            "        proxy-prefetching-limit=\"10\"\n"+
            "    >\n"+
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
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the proxy and proxy-prefetching-limit attributes of a collection
    public void testProxyPrefetchingLimit8()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "/** @ojb.collection element-class-ref=\"test.B\"\n"+
            "  *                 foreignkey=\"aid\"\n"+
            "  *                 proxy=\"true\"\n"+
            "  *                 database-foreignkey=\"false\"\n"+
            "  */\n"+
            "  private java.util.List objs;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field */\n"+
            "  private int aid;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"objs\"\n"+
            "  *                       proxy-prefetching-limit=\"10\"\n"+
            "  *                       proxy=\"false\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

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
                "        name=\"objs\"\n"+
                "        element-class-ref=\"test.B\"\n"+
                "        proxy=\"true\"\n"+
                "    >\n"+
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
                "    <field-descriptor\n"+
                "        name=\"id\"\n"+
                "        column=\"id\"\n"+
                "        jdbc-type=\"INTEGER\"\n"+
                "        primarykey=\"true\"\n"+
                "    >\n"+
                "    </field-descriptor>\n"+
                "    <collection-descriptor\n"+
                "        name=\"objs\"\n"+
                "        element-class-ref=\"test.B\"\n"+
                "        proxy=\"false\"\n"+
                "    >\n"+
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
                "</database>",
                runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the proxy-prefetching-limit attribute of a collection
    public void testProxyPrefetchingLimit9()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "/** @ojb.collection element-class-ref=\"test.B\"\n"+
            "  *                 foreignkey=\"aid\"\n"+
            "  *                 database-foreignkey=\"false\"\n"+
            "  */\n"+
            "  private java.util.List objs;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field */\n"+
            "  private int aid;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"objs\"\n"+
            "  *                       proxy-prefetching-limit=\"10\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

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
                "        name=\"objs\"\n"+
                "        element-class-ref=\"test.B\"\n"+
                "    >\n"+
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
                "    <field-descriptor\n"+
                "        name=\"id\"\n"+
                "        column=\"id\"\n"+
                "        jdbc-type=\"INTEGER\"\n"+
                "        primarykey=\"true\"\n"+
                "    >\n"+
                "    </field-descriptor>\n"+
                "    <collection-descriptor\n"+
                "        name=\"objs\"\n"+
                "        element-class-ref=\"test.B\"\n"+
                "    >\n"+
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
                "</database>",
                runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: modifying the proxy-prefetching-limit attribute of a collection
    public void testProxyPrefetchingLimit10()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class A {\n"+
            "/** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "/** @ojb.collection element-class-ref=\"test.B\"\n"+
            "  *                 foreignkey=\"aid\"\n"+
            "  *                 proxy=\"true\"\n"+
            "  */\n"+
            "  private java.util.List objs;\n"+
            "}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "/** @ojb.field */\n"+
            "  private int aid;\n"+
            "}\n");
        addClass(
            "test.C",
            "package test;\n"+
            "/** @ojb.class\n" +
            "  * @ojb.modify-inherited name=\"objs\"\n"+
            "  *                       proxy-prefetching-limit=\"a\"\n"+
            "  */\n"+
            "public class C extends A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
