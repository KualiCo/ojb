package org.apache.ojb.broker.metadata.torque;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.torque.TorqueFieldGenerator;

public class TorqueFieldGeneratorTest extends TestCase {

    private static final String EXPECTED_STANDARD_FIELDS =
            "        <column name=\"JUnit column 1\" required=\"false\" autoIncrement=\"false\" primaryKey=\"false\" type=\"VARCHAR\"/>\n" +
            "        <column name=\"JUnit column 2\" required=\"true\" autoIncrement=\"true\" primaryKey=\"true\" type=\"DOUBLE\"/>\n";
    private static final String EXPECTED_MAPPING_FIELDS =
            "        <column name=\"JUnit column 1\" required=\"true\" autoIncrement=\"false\" primaryKey=\"true\" type=\"VARCHAR\"/>\n" +
            "        <column name=\"JUnit column 2\" required=\"true\" autoIncrement=\"false\" primaryKey=\"true\" type=\"DOUBLE\"/>\n";
    private static final String EXPECTED_STANDARD_FIELDS_NO_AUTO =
            "        <column name=\"JUnit column 1\" required=\"false\" primaryKey=\"false\" type=\"VARCHAR\"/>\n" +
            "        <column name=\"JUnit column 2\" required=\"true\" primaryKey=\"true\" type=\"DOUBLE\"/>\n";
    private static final String EXPECTED_MAPPING_FIELDS_NO_AUTO =
            "        <column name=\"JUnit column 1\" required=\"true\" primaryKey=\"true\" type=\"VARCHAR\"/>\n" +
            "        <column name=\"JUnit column 2\" required=\"true\" primaryKey=\"true\" type=\"DOUBLE\"/>\n";

    public TorqueFieldGeneratorTest(String name) {
        super(name);
    }

    public void testGenerateFieldDescriptors() {
        TorqueFieldGenerator fieldGenerator = new TorqueFieldGenerator(false);
        StringBuffer buffer = new StringBuffer();

        FieldDescriptor[] descriptors = new FieldDescriptor[2];
        descriptors[0] = new FieldDescriptor(null, 1);
        descriptors[0].setColumnName("JUnit column 1");
        descriptors[0].setColumnType("VARCHAR");

        descriptors[1] = new FieldDescriptor(null, 2);
        descriptors[1].setColumnName("JUnit column 2");
        descriptors[1].setColumnType("DOUBLE");
        descriptors[1].setRequired(true);
        descriptors[1].setAutoIncrement(true);
        descriptors[1].setPrimaryKey(true);

        fieldGenerator.generateFieldDescriptors(descriptors, buffer);

        assertEquals("The fields were all hosed to pieces", EXPECTED_STANDARD_FIELDS, buffer.toString());

        fieldGenerator = new TorqueFieldGenerator(true);
        buffer = new StringBuffer();

        descriptors = new FieldDescriptor[2];
        descriptors[0] = new FieldDescriptor(null, 1);
        descriptors[0].setColumnName("JUnit column 1");
        descriptors[0].setColumnType("VARCHAR");

        descriptors[1] = new FieldDescriptor(null, 2);
        descriptors[1].setColumnName("JUnit column 2");
        descriptors[1].setColumnType("DOUBLE");
        descriptors[1].setRequired(true);
        descriptors[1].setAutoIncrement(true);
        descriptors[1].setPrimaryKey(true);

        fieldGenerator.generateFieldDescriptors(descriptors, buffer);

        assertEquals("The fields were all hosed to pieces", EXPECTED_STANDARD_FIELDS_NO_AUTO, buffer.toString());
    }

    public void testGenerateMappingFieldDescriptors() {
        TorqueFieldGenerator fieldGenerator = new TorqueFieldGenerator(false);
        StringBuffer buffer = new StringBuffer();

        FieldDescriptor[] descriptors = new FieldDescriptor[2];
        descriptors[0] = new FieldDescriptor(null, 1);
        descriptors[0].setColumnName("JUnit column 1");
        descriptors[0].setColumnType("VARCHAR");

        descriptors[1] = new FieldDescriptor(null, 2);
        descriptors[1].setColumnName("JUnit column 2");
        descriptors[1].setColumnType("DOUBLE");
        descriptors[1].setRequired(true);
        descriptors[1].setAutoIncrement(true);
        descriptors[1].setPrimaryKey(true);

        fieldGenerator.generateMappingFieldDescriptors(descriptors, buffer);

        assertEquals("The fields were all hosed to pieces", EXPECTED_MAPPING_FIELDS, buffer.toString());

        fieldGenerator = new TorqueFieldGenerator(true);
        buffer = new StringBuffer();

        descriptors = new FieldDescriptor[2];
        descriptors[0] = new FieldDescriptor(null,1);
        descriptors[0].setColumnName("JUnit column 1");
        descriptors[0].setColumnType("VARCHAR");

        descriptors[1] = new FieldDescriptor(null, 2);
        descriptors[1].setColumnName("JUnit column 2");
        descriptors[1].setColumnType("DOUBLE");
        descriptors[1].setRequired(true);
        descriptors[1].setAutoIncrement(true);
        descriptors[1].setPrimaryKey(true);

        fieldGenerator.generateMappingFieldDescriptors(descriptors, buffer);

        assertEquals("The fields were all hosed to pieces", EXPECTED_MAPPING_FIELDS_NO_AUTO, buffer.toString());
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TorqueFieldGeneratorTest.class);
    }

}
