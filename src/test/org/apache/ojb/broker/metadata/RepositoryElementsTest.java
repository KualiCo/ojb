package org.apache.ojb.broker.metadata;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * @author Virender Dogra
 * @version Apr 16, 2003 6:14:56 PM
 *
 */
public class RepositoryElementsTest extends TestCase
{

    /**
     * Constructor for RespositoryElementsTest.
     * @param name
     */
    public RepositoryElementsTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(RepositoryElementsTest.suite());
    }

    public static Test suite()
    {
        // Reflection is used here to add all
        // the testXXX() methods to the suite.
        TestSuite suite = new TestSuite(RepositoryElementsTest.class);
        return suite;

    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Method testForDuplicateElements.
     *
     * This test is to check that there are no constants with the same values. This
     * method collates the required data and then calls another resuable method to do the
     * final checking
     */

    public void testForDuplicateElements() throws Exception
    {

        Class c = RepositoryElements.class;
        Field[] fields = c.getDeclaredFields();

        //  make a string array of all the field values
        String[] fieldvalues = new String[fields.length];

        for (int i = 0; i < fields.length; i++)
        {
            try
            {
                fieldvalues[i] = c.getDeclaredField(fields[i].getName()).get(fields[i]).toString();
            }
            catch (IllegalAccessException e)
            {
                System.out.println(e);
                throw e;
            }
            catch (NoSuchFieldException e)
            {
                System.out.println("No such field " + fields[i] + " " + e);
                throw e;
            }
        }

        Arrays.sort(fieldvalues);

        try
        {
            checkForDuplicateConstant(fieldvalues);
            assertTrue(true);
        }
        catch (DuplicateRepositoryElementsFound e)
        {
            // Constants with similar values found
            fail(
                    e.getMessage()
                    + "\n All the constants values in string sort order that i read are -> \n"
                    + fieldValuesToString(fieldvalues));

        }

    }

    private String fieldValuesToString(String[] fieldvalues)
    {
        StringBuffer result = new StringBuffer(100);

        for (int i = 0; i < fieldvalues.length; i++)
        {
            result.append(fieldvalues[i].toString()).append(',');
        }

        return result.substring(0, ((result.length()) - 1));
    }

    /**
     * Method checkForDuplicateConstant.: This method checks for duplicate constant
     * values
     * @param fieldvalues
     * @throws DuplicateRepositoryElementsFound
     */
    private void checkForDuplicateConstant(String[] fieldvalues)
            throws DuplicateRepositoryElementsFound
    {
        for (int i = 1; i < fieldvalues.length; i++)
        {
            if (fieldvalues[i - 1].equals(fieldvalues[i]))
            {
                throw new DuplicateRepositoryElementsFound(fieldvalues[i]);
            }
        }
    }

}


/**
 * This exception is for the case when constants with
 * similar values are found in the RepositoryElements class
 */

class DuplicateRepositoryElementsFound extends Exception
{


    /* (non-Javadoc)
     * @see java.lang.Throwable#Throwable(java.lang.String)
     */
    public DuplicateRepositoryElementsFound(String errorMsg)
    {
        super("Duplicate value of " + errorMsg + " found");
    }
}
