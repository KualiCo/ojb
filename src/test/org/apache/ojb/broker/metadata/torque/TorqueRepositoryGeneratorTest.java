package org.apache.ojb.broker.metadata.torque;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.RepositoryPersistor;
import org.apache.ojb.broker.metadata.torque.TorqueRepositoryGenerator;
import org.apache.ojb.broker.TestHelper;

import java.io.File;
import java.io.FileReader;

public class TorqueRepositoryGeneratorTest extends TestCase {

    private static final String INPUT_FILE = TestHelper.DEF_REPOSITORY;
    private static final String OUTPUT_FILE = "test-project-schema.xml";
    private static final String EXPECTED_OUTPUT_FILE = "expected-project-schema.xml";
    private TorqueRepositoryGenerator torqueRepositoryGenerator;
    private DescriptorRepository repository;

    public TorqueRepositoryGeneratorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        RepositoryPersistor repositoryPersistor = new RepositoryPersistor();
        this.repository = repositoryPersistor.readDescriptorRepository(INPUT_FILE);
        this.torqueRepositoryGenerator = new TorqueRepositoryGenerator(INPUT_FILE, false);
    }

    public void tearDown() throws Exception {
        this.torqueRepositoryGenerator = null;
        File outputFile = new File(OUTPUT_FILE);
        outputFile.delete();
    }

    public void testGenerateTorqueRepository() throws Exception {
        this.torqueRepositoryGenerator.generateTorqueRepository(OUTPUT_FILE, "test", "testIdx");
        File outputFile = new File(OUTPUT_FILE);
        File expectedFile = new File(EXPECTED_OUTPUT_FILE);
        FileReader outputFileReader = new FileReader(outputFile);
        FileReader expectedFileReader = new FileReader(expectedFile);
        assertEquals("The generated test-project-schema.xml is a different length than expected.", expectedFile.length(), outputFile.length());

        while (outputFileReader.ready()) {
            assertEquals("The generated test-project-schema.xml was not identical to the expected-project-schema.xml.", outputFileReader.read(), expectedFileReader.read());
        }
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TorqueRepositoryGeneratorTest.class);
    }

}
