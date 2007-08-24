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
 * Tests for the ojb.object-cache tag.
 * TODO Add tests for strict=true
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ObjectCacheTagTests extends OjbTestBase
{
    public ObjectCacheTagTests(String name)
    {
        super(name);
    }

    // Test: class attribute with empty value
    public void testSimple1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.object-cache class=\"\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: OJB-supplied cache class
    public void testSimple2()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.object-cache class=\"org.apache.ojb.broker.cache.ObjectCachePerBrokerImpl\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <object-cache class=\"org.apache.ojb.broker.cache.ObjectCachePerBrokerImpl\">\n"+
            "    </object-cache>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: user-defined cache class
    public void testSimple3()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.object-cache class=\""+TestObjectCache.class.getName()+"\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <object-cache class=\""+TestObjectCache.class.getName()+"\">\n"+
            "    </object-cache>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: user-defined cache class which does not implement the ObjectCache interface
    public void testSimple4()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.object-cache class=\"java.lang.Map\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertNull(runOjbXDoclet(OJB_DEST_FILE));
        assertNull(runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test: inheritance
    public void testSimple5()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.object-cache class=\"org.apache.ojb.broker.cache.ObjectCachePerBrokerImpl\"\n"+
            "  */\n"+
            "public class A {}\n");
        addClass(
            "test.B",
            "package test;\n"+
            "/** @ojb.class */\n"+
            "public class B extends A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <object-cache class=\"org.apache.ojb.broker.cache.ObjectCachePerBrokerImpl\">\n"+
            "    </object-cache>\n"+
            "    <extent-class class-ref=\"test.B\"/>\n"+
            "</class-descriptor>\n"+
            "<class-descriptor\n"+
            "    class=\"test.B\"\n"+
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

    // Test of attributes attribute: no value
    public void testAttributes1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.object-cache class=\""+TestObjectCache.class.getName()+"\"\n"+
            "  *                   attributes=\"\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <object-cache class=\""+TestObjectCache.class.getName()+"\">\n"+
            "    </object-cache>\n"+
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
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.object-cache class=\""+TestObjectCache.class.getName()+"\"\n"+
            "  *                   attributes=\"name=value\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <object-cache class=\""+TestObjectCache.class.getName()+"\">\n"+
            "        <attribute attribute-name=\"name\" attribute-value=\"value\"/>\n"+
            "    </object-cache>\n"+
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
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.object-cache class=\""+TestObjectCache.class.getName()+"\"\n"+
            "  *                   attributes=\"name=\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <object-cache class=\""+TestObjectCache.class.getName()+"\">\n"+
            "        <attribute attribute-name=\"name\" attribute-value=\"\"/>\n"+
            "    </object-cache>\n"+
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
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.object-cache class=\""+TestObjectCache.class.getName()+"\"\n"+
            "  *                   attributes=\"name1=value1,name2=,name3=value3\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <object-cache class=\""+TestObjectCache.class.getName()+"\">\n"+
            "        <attribute attribute-name=\"name1\" attribute-value=\"value1\"/>\n"+
            "        <attribute attribute-name=\"name2\" attribute-value=\"\"/>\n"+
            "        <attribute attribute-name=\"name3\" attribute-value=\"value3\"/>\n"+
            "    </object-cache>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }

    // Test of documentation attribute: no value
    public void testDocumentation1()
    {
        addClass(
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.object-cache class=\""+TestObjectCache.class.getName()+"\"\n"+
            "  *                   documentation=\"\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <object-cache class=\""+TestObjectCache.class.getName()+"\">\n"+
            "    </object-cache>\n"+
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
            "test.A",
            "package test;\n"+
            "/** @ojb.class\n"+
            "  * @ojb.object-cache class=\""+TestObjectCache.class.getName()+"\"\n"+
            "  *                   documentation=\"some documentation\"\n"+
            "  */\n"+
            "public class A {}\n");

        assertEqualsOjbDescriptorFile(
            "<class-descriptor\n"+
            "    class=\"test.A\"\n"+
            "    table=\"A\"\n"+
            ">\n"+
            "    <object-cache class=\""+TestObjectCache.class.getName()+"\">\n"+
            "        <documentation>some documentation</documentation>\n"+
            "    </object-cache>\n"+
            "</class-descriptor>",
            runOjbXDoclet(OJB_DEST_FILE));
        assertEqualsTorqueSchemaFile(
            "<database name=\"ojbtest\">\n"+
            "    <table name=\"A\">\n"+
            "    </table>\n"+
            "</database>",
            runTorqueXDoclet(TORQUE_DEST_FILE, "ojbtest"));
    }
}
