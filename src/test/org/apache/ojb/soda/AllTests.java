package org.apache.ojb.soda;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.ojb.broker.HsqldbShutdown;

/**
 * the facade to all TestCases in this package.
 * 
 * @author Thomas Mahler
 */
public class AllTests extends junit.framework.TestSuite
{
    /** static reference to .class.
     * Java does not provide any way to obtain the Class object from
     * static method without naming it.
     */
    private static Class CLASS = AllTests.class;

    /**
     * runs the suite in a junit.textui.TestRunner.
     */
    public static void main(String[] args)
    {
        String[] arr = {CLASS.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /** build a TestSuite from all the TestCases in this package*/
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(SodaExamples.class));

        // BRJ: ensure shutdown of hsqldb
        suite.addTestSuite(HsqldbShutdown.class);
        return suite;
    }
}
