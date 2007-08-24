package org.apache.ojb.broker.metadata.torque;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.torque.TableDescriptor;

import java.util.Vector;

public class TableDescriptorTest extends TestCase {

    private TableDescriptor tableDescriptor;

    public TableDescriptorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        this.tableDescriptor = new TableDescriptor();
    }

    public void tearDown() throws Exception {
        this.tableDescriptor = null;
    }

    public void testAddColumn() {
        FieldDescriptor fieldDescriptor = new FieldDescriptor(null, 1);
        fieldDescriptor.setColumnName("JUnit 1");
        this.tableDescriptor.addColumn(fieldDescriptor);
        Vector columns = this.tableDescriptor.getColumns();
        assertNotNull("The vector of columns is strangely null", columns);
        assertEquals("There was strangely not 1 element in the vector", 1, columns.size());
        this.tableDescriptor.addColumn(fieldDescriptor);
        columns = this.tableDescriptor.getColumns();
        assertEquals("There was strangely not 1 element in the vector", 1, columns.size());
        FieldDescriptor newFieldDescriptor = new FieldDescriptor(null, 2);
        newFieldDescriptor.setColumnName("JUnit 2");
        this.tableDescriptor.addColumn(newFieldDescriptor);
        columns = this.tableDescriptor.getColumns();
        assertEquals("There was strangely not 1 element in the vector", 2, columns.size());
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TableDescriptorTest.class);
    }

}
