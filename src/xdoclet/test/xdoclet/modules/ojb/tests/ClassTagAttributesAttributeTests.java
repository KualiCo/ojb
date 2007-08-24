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
 * Tests for the ojb.class tag with the attributes attribute.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ClassTagAttributesAttributeTests extends OjbTestBase
{
    public ClassTagAttributesAttributeTests(String name)
    {
        super(name);
    }

    // Test of attributes attribute: no value
    public void testAttributes1()
    {
        addClass(
            "A",
            "/** @ojb.class attributes=\"\" */\n"+
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

    // Test of attributes attribute: with one name-value pair
    public void testAttributes2()
    {
        addClass(
            "A",
            "/** @ojb.class attributes=\"name=value\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <attribute attribute-name=\"name\" attribute-value=\"value\"/>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of attributes attribute: with empty attribute
    public void testAttributes3()
    {
        addClass(
            "A",
            "/** @ojb.class attributes=\"name=\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <attribute attribute-name=\"name\" attribute-value=\"\"/>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of attributes attribute: with three name-value pairs, one without value 
    public void testAttributes4()
    {
        addClass(
            "A",
            "/** @ojb.class attributes=\"name1=value1,name2=,name3=value3\" */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <attribute attribute-name=\"name1\" attribute-value=\"value1\"/>\n"+
            "    <attribute attribute-name=\"name2\" attribute-value=\"\"/>\n"+
            "    <attribute attribute-name=\"name3\" attribute-value=\"value3\"/>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of attributes attribute: with value, not inherited in subclass
    public void testAttributes5()
    {
        addClass(
            "A",
            "/** @ojb.class attributes=\"name=value\" */\n"+
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
            "    <extent-class class-ref=\"B\"/>\n"+
            "    <attribute attribute-name=\"name\" attribute-value=\"value\"/>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"B\"\n"+
            "    table=\"B\"\n"+
            ">\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "    </table>\n"+
            "    <table name=\"B\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

}
