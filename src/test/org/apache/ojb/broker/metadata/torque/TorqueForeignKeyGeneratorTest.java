package org.apache.ojb.broker.metadata.torque;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.RepositoryPersistor;
import org.apache.ojb.broker.metadata.torque.TableDescriptor;
import org.apache.ojb.broker.metadata.torque.TorqueForeignKeyGenerator;
import org.apache.ojb.broker.TestHelper;

import java.util.HashMap;
import java.util.Vector;

public class TorqueForeignKeyGeneratorTest extends TestCase {

    private static final String EXAMPLE_FILE = TestHelper.DEF_REPOSITORY;
    private static final String OJB_DLIST_ENTRY_FOREIGN_KEY =
            "        <foreign-key foreignTable=\"OJB_DLIST\">\n" +
            "            <reference local=\"DLIST_ID\" foreign=\"ID\"/>\n" +
            "        </foreign-key>\n";
    private static final String PERSON_PROJECT_FK_1 =
            "        <foreign-key foreignTable=\"PERSON\">\n" +
            "            <reference local=\"PERSON_ID\" foreign=\"ID\"/>\n" +
            "        </foreign-key>\n";

    private static final String PERSON_PROJECT_FK_2 =
            "        <foreign-key foreignTable=\"PROJECT\">\n" +
            "            <reference local=\"PROJECT_ID\" foreign=\"ID\"/>\n" +
            "        </foreign-key>\n";
    private static final String ARTIKEL_FK =
            "        <foreign-key foreignTable=\"Kategorien\">\n" +
            "            <reference local=\"Kategorie_Nr\" foreign=\"Kategorie_Nr\"/>\n" +
            "        </foreign-key>\n";
    private static final String PAGEWORD_FK_1 =
            "        <foreign-key foreignTable=\"WORD\">\n" +
            "            <reference local=\"wordId\" foreign=\"ID\"/>\n" +
            "        </foreign-key>\n";
    private static final String PAGEWORD_FK_2 =
            "        <foreign-key foreignTable=\"PAGE\">\n" +
            "            <reference local=\"pageId\" foreign=\"ID\"/>\n" +
            "        </foreign-key>\n";

    private TorqueForeignKeyGenerator foreignKeyGenerator;

    public TorqueForeignKeyGeneratorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        RepositoryPersistor repositoryPersistor = new RepositoryPersistor();
        DescriptorRepository descriptorRepository = repositoryPersistor.readDescriptorRepository(EXAMPLE_FILE);
        this.foreignKeyGenerator = new TorqueForeignKeyGenerator(descriptorRepository);
    }

    public void tearDown() throws Exception {
        this.foreignKeyGenerator = null;
    }

    public void testBuildConstraintsMap() {
        StringBuffer buffer = new StringBuffer();
        this.foreignKeyGenerator.buildConstraintsMap();
        Vector foreignKeys = this.foreignKeyGenerator.getForeignKeysForTable("OJB_DLIST_ENTRIES");
        assertNotNull(foreignKeys);
        assertEquals(1, foreignKeys.size());
        assertEquals(OJB_DLIST_ENTRY_FOREIGN_KEY, foreignKeys.get(0));

        foreignKeys = this.foreignKeyGenerator.getForeignKeysForTable("PERSON_PROJECT");
        assertEquals(2, foreignKeys.size());
        assertTrue(foreignKeys.contains(PERSON_PROJECT_FK_1));
        assertTrue(foreignKeys.contains(PERSON_PROJECT_FK_2));

        foreignKeys = this.foreignKeyGenerator.getForeignKeysForTable("Artikel");
        assertEquals(1, foreignKeys.size());
        assertEquals(ARTIKEL_FK, foreignKeys.get(0));

        HashMap mappingTables = this.foreignKeyGenerator.getMappingTables();
        assertEquals(1, mappingTables.size());
        TableDescriptor tableDescriptor = (TableDescriptor) mappingTables.values().iterator().next();
        assertEquals("PageWords", tableDescriptor.getName());
        foreignKeys = this.foreignKeyGenerator.getForeignKeysForTable("PageWords");
        assertNotNull(foreignKeys);
        assertEquals(2, foreignKeys.size());
        assertTrue(foreignKeys.contains(PAGEWORD_FK_1));
        assertTrue(foreignKeys.contains(PAGEWORD_FK_2));
    }


    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TorqueForeignKeyGeneratorTest.class);
    }

}
