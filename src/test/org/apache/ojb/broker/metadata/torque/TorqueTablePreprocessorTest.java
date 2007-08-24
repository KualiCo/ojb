package org.apache.ojb.broker.metadata.torque;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.RepositoryPersistor;
import org.apache.ojb.broker.metadata.torque.TableDescriptor;
import org.apache.ojb.broker.metadata.torque.TorqueTablePreprocessor;
import org.apache.ojb.broker.TestHelper;

import java.util.HashMap;
import java.util.Vector;

public class TorqueTablePreprocessorTest extends TestCase {

    private static final String EXAMPLE_FILE = TestHelper.DEF_REPOSITORY;
    private TorqueTablePreprocessor torqueTablePreprocessor;

    public TorqueTablePreprocessorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        RepositoryPersistor repositoryPersistor = new RepositoryPersistor();
        DescriptorRepository descriptorRepository = repositoryPersistor.readDescriptorRepository(EXAMPLE_FILE);
        this.torqueTablePreprocessor = new TorqueTablePreprocessor(descriptorRepository);
    }

    public void tearDown() throws Exception {
        this.torqueTablePreprocessor = null;
    }

    public void testBuildStandardTables() {
        StringBuffer buffer = new StringBuffer();
        this.torqueTablePreprocessor.buildStandardTables();
        HashMap standardTables = this.torqueTablePreprocessor.getStandardTables();
        assertNotNull(standardTables);
        TableDescriptor tableDescriptor = (TableDescriptor) standardTables.get("Artikel");
        assertEquals("Artikel", tableDescriptor.getName());
        assertTrue(tableDescriptor.getIndices().isEmpty());
        Vector columns = tableDescriptor.getColumns();
        assertNotNull(columns);
        assertEquals(10, columns.size());
        Vector references = tableDescriptor.getReferences();
        assertNotNull(references);
        assertEquals(2, references.size());
        ObjectReferenceDescriptor ord = ((ObjectReferenceDescriptor) references.get(0));

        assertTrue(org.apache.ojb.odmg.shared.ProductGroup.class == ord.getItemClass() || org.apache.ojb.broker.ProductGroup.class == ord.getItemClass());
        ord = ((ObjectReferenceDescriptor) references.get(1));
        assertTrue(org.apache.ojb.odmg.shared.ProductGroup.class == ord.getItemClass() || org.apache.ojb.broker.ProductGroup.class == ord.getItemClass());
    }


    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TorqueTablePreprocessorTest.class);
    }

}
