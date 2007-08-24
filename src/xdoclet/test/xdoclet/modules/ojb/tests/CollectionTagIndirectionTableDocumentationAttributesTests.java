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
 * Tests for the ojb.collection tag with the indirection-table and its documentation attributes.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class CollectionTagIndirectionTableDocumentationAttributesTests extends OjbTestBase
{
    public CollectionTagIndirectionTableDocumentationAttributesTests(String name)
    {
        super(name);
    }

    // Test: indirection-table with two classes (both with collection: one array, other pre-defined ojb collection class)
    //       with integer primary keys, one of them anonymous
    public void testIndirectionTable1()
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
            "    *                 indirection-table-documentation=\"The indirection table between A and B\"\n"+
            "    */\n"+
            "  private B[] bs;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.field name=\"id\"\n"+
            "  *            jdbc-type=\"INTEGER\"\n"+
            "  *            primarykey=\"true\"\n"+
            "  */\n"+
            "public class B {\n"+
            "  /** @ojb.collection element-class-ref=\"test.A\"\n"+
            "    *                 foreignkey=\"BID\"\n"+
            "    *                 indirection-table=\"A_B\"\n"+
            "    *                 indirection-table-documentation=\"The indirection table between A and B\"\n"+
            "    */\n"+
            "  private org.apache.ojb.odmg.collections.DListImpl as;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
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
            "        indirection-table=\"A_B\"\n"+
            "    >\n"+
            "        <fk-pointing-to-this-class column=\"AID\"/>\n"+
            "        <fk-pointing-to-element-class column=\"BID\"/>\n"+
            "    </collection-descriptor>\n"+
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
            "        access=\"anonymous\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <collection-descriptor\n"+
            "        name=\"as\"\n"+
            "        collection-class=\"org.apache.ojb.odmg.collections.DListImpl\"\n"+
            "        element-class-ref=\"test.A\"\n"+
            "        indirection-table=\"A_B\"\n"+
            "    >\n"+
            "        <fk-pointing-to-this-class column=\"BID\"/>\n"+
            "        <fk-pointing-to-element-class column=\"AID\"/>\n"+
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
            "    <table name=\"A_B\"\n" +
            "           description=\"The indirection table between A and B\"\n"+
            "    >\n"+
            "        <column name=\"AID\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <column name=\"BID\"\n"+
            "                type=\"INTEGER\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"A\">\n"+
            "            <reference local=\"AID\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"BID\" foreign=\"id\"/>\n"+
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
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: indirection-table with two classes (both with collection) with non-integer jdbc types that are also different
    public void testIndirectionTable2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private org.apache.ojb.broker.util.GUID id;\n"+
            "  /** @ojb.collection element-class-ref=\"test.B\"\n"+
            "    *                 foreignkey=\"AID\"\n"+
            "    *                 indirection-table=\"A_B\"\n"+
            "    *                 foreignkey-documentation=\"References A\"\n"+
            "    */\n"+
            "  private java.util.List bs;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private java.util.Date id;\n"+
            "  /** @ojb.collection element-class-ref=\"test.A\"\n"+
            "    *                 foreignkey=\"BID\"\n"+
            "    *                 indirection-table=\"A_B\"\n"+
            "    *                 indirection-table-documentation=\"The indirection table between A and B\"\n"+
            "    *                 foreignkey-documentation=\"References B\"\n"+
            "    */\n"+
            "  private "+TestCollectionClass.class.getName()+" as;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        primarykey=\"true\"\n"+
            "        conversion=\"org.apache.ojb.broker.accesslayer.conversions.GUID2StringFieldConversion\"\n"+
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <collection-descriptor\n"+
            "        name=\"bs\"\n"+
            "        element-class-ref=\"test.B\"\n"+
            "        indirection-table=\"A_B\"\n"+
            "    >\n"+
            "        <fk-pointing-to-this-class column=\"AID\"/>\n"+
            "        <fk-pointing-to-element-class column=\"BID\"/>\n"+
            "    </collection-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"DATE\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <collection-descriptor\n"+
            "        name=\"as\"\n"+
            "        collection-class=\""+TestCollectionClass.class.getName()+"\"\n"+
            "        element-class-ref=\"test.A\"\n"+
            "        indirection-table=\"A_B\"\n"+
            "    >\n"+
            "        <fk-pointing-to-this-class column=\"BID\"/>\n"+
            "        <fk-pointing-to-element-class column=\"AID\"/>\n"+
            "    </collection-descriptor>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"A_B\"\n" +
            "           description=\"The indirection table between A and B\"\n"+
            "    >\n"+
            "        <column name=\"AID\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "                description=\"References A\"\n"+
            "        />\n"+
            "        <column name=\"BID\"\n"+
            "                type=\"DATE\"\n"+
            "                description=\"References B\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"A\">\n"+
            "            <reference local=\"AID\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"BID\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"DATE\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: indirection-table with two classes (only one with collection), remote-foreignkey used with one foreignkey
    public void testIndirectionTable3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class */\n" +
            "public class A {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id;\n"+
            "  /** @ojb.collection element-class-ref=\"test.B\"\n"+
            "    *                 foreignkey=\"AID\"\n"+
            "    *                 remote-foreignkey=\"BID\"\n"+
            "    *                 indirection-table=\"A_B\"\n"+
            "    *                 indirection-table-documentation=\"The indirection table between A and B\"\n"+
            "    *                 foreignkey-documentation=\"References A\"\n"+
            "    *                 remote-foreignkey-documentation=\"References B\"\n"+
            "    */\n"+
            "  private java.util.List bs;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private String id;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
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
            "        indirection-table=\"A_B\"\n"+
            "    >\n"+
            "        <fk-pointing-to-this-class column=\"AID\"/>\n"+
            "        <fk-pointing-to-element-class column=\"BID\"/>\n"+
            "    </collection-descriptor>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "    <field-descriptor\n"+
            "        name=\"id\"\n"+
            "        column=\"id\"\n"+
            "        jdbc-type=\"VARCHAR\"\n"+
            "        primarykey=\"true\"\n"+
            "        length=\"254\"\n"+
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
            "    <table name=\"A_B\"\n" +
            "           description=\"The indirection table between A and B\"\n" +
            "    >\n"+
            "        <column name=\"AID\"\n"+
            "                type=\"INTEGER\"\n"+
            "                description=\"References A\"\n"+
            "        />\n"+
            "        <column name=\"BID\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "                description=\"References B\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"A\">\n"+
            "            <reference local=\"AID\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"BID\" foreign=\"id\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"id\"\n"+
            "                javaName=\"id\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: indirection-table with two classes (only one with collection), remote-foreignkey used with multiple foreignkeys
    public void testIndirectionTable4()
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
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id3;\n"+
            "  /** @ojb.collection element-class-ref=\"test.B\"\n"+
            "    *                 foreignkey=\"AID1,AID2,AID3\"\n"+
            "    *                 remote-foreignkey=\"BID1,BID2\"\n"+
            "    *                 indirection-table=\"A_B\"\n"+
            "    *                 foreignkey-documentation=\"References A#id1, References A#id2, References A#id3\"\n"+
            "    *                 remote-foreignkey-documentation=\"References B#id1, References B#id2\"\n"+
            "    */\n"+
            "  private java.util.List bs;\n"+
            "}");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B {\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private String id1;\n"+
            "  /** @ojb.field primarykey=\"true\" */\n"+
            "  private int id2;\n"+
            "}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
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
            "    <field-descriptor\n"+
            "        name=\"id3\"\n"+
            "        column=\"id3\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <collection-descriptor\n"+
            "        name=\"bs\"\n"+
            "        element-class-ref=\"test.B\"\n"+
            "        indirection-table=\"A_B\"\n"+
            "    >\n"+
            "        <fk-pointing-to-this-class column=\"AID1\"/>\n"+
            "        <fk-pointing-to-this-class column=\"AID2\"/>\n"+
            "        <fk-pointing-to-this-class column=\"AID3\"/>\n"+
            "        <fk-pointing-to-element-class column=\"BID1\"/>\n"+
            "        <fk-pointing-to-element-class column=\"BID2\"/>\n"+
            "    </collection-descriptor>\n"+
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
            "        length=\"254\"\n"+
            "    >\n"+
            "    </field-descriptor>\n"+
            "    <field-descriptor\n"+
            "        name=\"id2\"\n"+
            "        column=\"id2\"\n"+
            "        jdbc-type=\"INTEGER\"\n"+
            "        primarykey=\"true\"\n"+
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
            "        <column name=\"id3\"\n"+
            "                javaName=\"id3\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "    <table name=\"A_B\">\n"+
            "        <column name=\"AID1\"\n"+
            "                type=\"INTEGER\"\n"+
            "                description=\"References A#id1\"\n"+
            "        />\n"+
            "        <column name=\"AID2\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "                description=\"References A#id2\"\n"+
            "        />\n"+
            "        <column name=\"AID3\"\n"+
            "                type=\"INTEGER\"\n"+
            "                description=\"References A#id3\"\n"+
            "        />\n"+
            "        <column name=\"BID1\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                size=\"254\"\n"+
            "                description=\"References B#id1\"\n"+
            "        />\n"+
            "        <column name=\"BID2\"\n"+
            "                type=\"INTEGER\"\n"+
            "                description=\"References B#id2\"\n"+
            "        />\n"+
            "        <foreign-key foreignTable=\"A\">\n"+
            "            <reference local=\"AID1\" foreign=\"id1\"/>\n"+
            "            <reference local=\"AID2\" foreign=\"id2\"/>\n"+
            "            <reference local=\"AID3\" foreign=\"id3\"/>\n"+
            "        </foreign-key>\n"+
            "        <foreign-key foreignTable=\"B\">\n"+
            "            <reference local=\"BID1\" foreign=\"id1\"/>\n"+
            "            <reference local=\"BID2\" foreign=\"id2\"/>\n"+
            "        </foreign-key>\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "        <column name=\"id1\"\n"+
            "                javaName=\"id1\"\n"+
            "                type=\"VARCHAR\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "                size=\"254\"\n"+
            "        />\n"+
            "        <column name=\"id2\"\n"+
            "                javaName=\"id2\"\n"+
            "                type=\"INTEGER\"\n"+
            "                primaryKey=\"true\"\n"+
            "                required=\"true\"\n"+
            "        />\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
