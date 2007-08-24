package org.apache.ojb.broker.metadata.torque;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.RepositoryPersistor;
import org.apache.ojb.broker.metadata.torque.TableDescriptor;
import org.apache.ojb.broker.metadata.torque.TorqueTableGenerator;
import org.apache.ojb.broker.TestHelper;

public class TorqueTableGeneratorTest extends TestCase {

    private static final String EXAMPLE_FILE = TestHelper.DEF_REPOSITORY;
    private static final String EMPTY_STANDARD_TABLE =
            "    <table name=\"null\" indexTablespace=\"JUnit indx\">\n" +
            "    </table>\n\n";
    private static final String PERSON_PROJECT_TABLE =
            "    <table name=\"PERSON_PROJECT\" indexTablespace=\"JUnit indx\">\n" +
            "        <column name=\"Kategorie_Nr\" required=\"false\" autoIncrement=\"false\" primaryKey=\"true\" type=\"INTEGER\"/>\n" +
            "        <column name=\"KategorieName\" required=\"false\" autoIncrement=\"false\" primaryKey=\"false\" type=\"VARCHAR\"/>\n" +
            "        <column name=\"Beschreibung\" required=\"false\" autoIncrement=\"false\" primaryKey=\"false\" type=\"VARCHAR\"/>\n" +
            "        <foreign-key foreignTable=\"PROJECT\">\n" +
            "            <reference local=\"PROJECT_ID\" foreign=\"ID\"/>\n" +
            "        </foreign-key>\n" +
            "        <foreign-key foreignTable=\"PERSON\">\n" +
            "            <reference local=\"PERSON_ID\" foreign=\"ID\"/>\n" +
            "        </foreign-key>\n" +
            "    </table>\n\n";

    private static final String MAPPING_TABLES =
            "    <table name=\"PageWords\" indexTablespace=\"JUnit indx\">\n" +
            "        <column name=\"ID\" required=\"true\" autoIncrement=\"false\" primaryKey=\"true\" type=\"INTEGER\"/>\n" +
            "        <foreign-key foreignTable=\"WORD\">\n" +
            "            <reference local=\"wordId\" foreign=\"ID\"/>\n" +
            "        </foreign-key>\n" +
            "        <foreign-key foreignTable=\"PAGE\">\n" +
            "            <reference local=\"pageId\" foreign=\"ID\"/>\n" +
            "        </foreign-key>\n" +
            "    </table>\n";

    private TorqueTableGenerator torqueTableGenerator;
    private DescriptorRepository repository;

    public TorqueTableGeneratorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        RepositoryPersistor repositoryPersistor = new RepositoryPersistor();
        this.repository = repositoryPersistor.readDescriptorRepository(EXAMPLE_FILE);
        this.torqueTableGenerator = new TorqueTableGenerator(this.repository, false);
    }

    public void tearDown() throws Exception {
        this.torqueTableGenerator = null;
    }

    public void testGenerateStandardTables() {
        StringBuffer buffer = new StringBuffer();
        TableDescriptor tableDescriptor = new TableDescriptor();
        this.torqueTableGenerator.generateStandardTable(tableDescriptor, buffer, "JUnit indx");
        assertEquals(EMPTY_STANDARD_TABLE, buffer.toString());

        buffer = new StringBuffer();
        tableDescriptor.setName("PERSON_PROJECT");
        ClassDescriptor classDescriptor = this.repository.getDescriptorFor(org.apache.ojb.odmg.shared.ProductGroup.class);
        FieldDescriptor fieldDescriptors[] = classDescriptor.getFieldDescriptions();
        for (int i = 0; i < fieldDescriptors.length; i++) {
            tableDescriptor.addColumn(fieldDescriptors[i]);
        }
        tableDescriptor.setIndices(classDescriptor.getIndexes());
        tableDescriptor.getReferences().addAll(classDescriptor.getObjectReferenceDescriptors());

        this.torqueTableGenerator.generateStandardTable(tableDescriptor, buffer, "JUnit indx");
        assertEquals(PERSON_PROJECT_TABLE, buffer.toString());
    }

    public void testGenerateMappingTables() {
        StringBuffer buffer = new StringBuffer();
        TableDescriptor tableDescriptor = new TableDescriptor();
        this.torqueTableGenerator.generateMappingTables(buffer, "JUnit indx");
        assertEquals(MAPPING_TABLES, buffer.toString());
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TorqueTableGeneratorTest.class);
    }

}
