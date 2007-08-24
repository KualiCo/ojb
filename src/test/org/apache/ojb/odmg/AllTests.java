package org.apache.ojb.odmg;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.ojb.broker.HsqldbShutdown;

/**
 * the facade to all TestCases in this package.
 * @author Thomas Mahler
 */
public class AllTests extends junit.framework.TestSuite
{
    /**
     * runs the suite in a junit.textui.TestRunner.
     */
    public static void main(String[] args)
    {
        String[] arr = {AllTests.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /** build a TestSuite from all the TestCases in this package*/
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(OdmgExamples.class);
        suite.addTestSuite(NamedRootsTest.class);
        suite.addTestSuite(DListTest.class);
        suite.addTestSuite(DMapTest.class);
        suite.addTestSuite(DSetTest.class);
        suite.addTestSuite(LockingTest.class);
        suite.addTestSuite(LockingMultithreadedTest.class);
        suite.addTestSuite(ProxyTest.class);
        suite.addTestSuite(RITest.class);
        suite.addTestSuite(ScrollableQueryResultsTest.class);
        suite.addTestSuite(ProjectionAttributeTest.class);
        suite.addTestSuite(ContractVersionEffectivenessOQLTest.class);
        suite.addTestSuite(OQLTest.class);
        suite.addTestSuite(ODMGRollbackTest.class);
        suite.addTestSuite(MultiDBUsageTest.class);
        suite.addTestSuite(UserTestCases.class);
        suite.addTestSuite(BidirectionalAssociationTest.class);
        suite.addTestSuite(ManyToManyTest.class);
        suite.addTestSuite(OneToOneTest.class);
        suite.addTestSuite(OneToManyTest.class);
        suite.addTestSuite(OQLOrOnForeignKeyTest.class);
        suite.addTestSuite(FieldConversionTest_4.class);
        suite.addTestSuite(BatchModeTest.class);
        suite.addTestSuite(CollectionsTest.class);
        suite.addTestSuite(PersonWithArrayTest.class);
        suite.addTestSuite(M2NTest.class);
        suite.addTestSuite(ObjectImageTest.class);
        suite.addTestSuite(InheritanceMultipleTableTest.class);
        suite.addTestSuite(CircularTest.class);

        // BRJ: ensure shutdown of hsqldb
        suite.addTestSuite(HsqldbShutdown.class);
        return suite;
    }
}
