package org.apache.ojb.broker.metadata.torque;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.ojb.broker.metadata.IndexDescriptor;
import org.apache.ojb.broker.metadata.torque.TorqueIndexGenerator;

import java.util.Vector;

public class TorqueIndexGeneratorTest extends TestCase {

    private TorqueIndexGenerator indexGenerator;
    private static final String EXPECTED_INDICES =
            "        <unique name=\"JUnit name1\">\n" +
            "            <unique-column name=\"JUnit column1\"/>\n" +
            "            <unique-column name=\"JUnit column2\"/>\n" +
            "        </unique>\n" +
            "        <index name=\"JUnit name2\">\n" +
            "            <index-column name=\"JUnit column3\"/>\n" +
            "        </index>\n";

    public TorqueIndexGeneratorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        this.indexGenerator = new TorqueIndexGenerator();
    }

    public void tearDown() throws Exception {
        this.indexGenerator = null;
    }

    public void testGenerateIndices() {
        StringBuffer buffer = new StringBuffer();
        Vector indexDescriptors = new Vector();

        IndexDescriptor indexUnique = new IndexDescriptor();
        indexUnique.setName("JUnit name1");
        indexUnique.setUnique(true);
        Vector columns = new Vector();
        columns.add("JUnit column1");
        columns.add("JUnit column2");
        indexUnique.setIndexColumns(columns);
        indexDescriptors.add(indexUnique);

        IndexDescriptor indexNotUnique = new IndexDescriptor();
        indexNotUnique.setName("JUnit name2");
        indexNotUnique.setUnique(false);
        columns = new Vector();
        columns.add("JUnit column3");
        indexNotUnique.setIndexColumns(columns);
        indexDescriptors.add(indexNotUnique);

        this.indexGenerator.generateIndices(indexDescriptors, buffer);


        assertEquals("The damn thing doesn't work.", EXPECTED_INDICES, buffer.toString());
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TorqueIndexGeneratorTest.class);
    }

}
