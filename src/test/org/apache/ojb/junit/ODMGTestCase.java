package org.apache.ojb.junit;

import org.apache.ojb.broker.TestHelper;
import org.apache.ojb.odmg.ImplementationExt;
import org.apache.ojb.odmg.OJB;
import org.odmg.Database;
import org.odmg.Transaction;

/**
 *
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: ODMGTestCase.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class ODMGTestCase extends OJBTestCase
{
    public ImplementationExt odmg;
    public Database database;

    public ODMGTestCase()
    {
    }

    public ODMGTestCase(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        odmg = OJB.getInstance();
        database = odmg.newDatabase();
        database.open(TestHelper.DEF_DATABASE_NAME, Database.OPEN_READ_WRITE);
    }

    protected void tearDown() throws Exception
    {
        try
        {
            Transaction currentTx = odmg.currentTransaction();
            if(currentTx != null && currentTx.isOpen())
            {
                currentTx.abort();
            }
        }
        catch(Exception e)
        {
        }

        super.tearDown();
    }
}
