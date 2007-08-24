package org.apache.ojb.broker.sqlcount;

import junit.framework.TestCase;
import com.p6spy.engine.common.P6SpyProperties;
import com.p6spy.engine.spy.P6SpyDriver;
import org.apache.ojb.p6spy.CountLogger;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBrokerFactory;

import java.io.File;

/**
 * provides methods to count the number statements.
 *
 * @author <a href="mailto:om@ppi.de">Oliver Matz</a>
 * @version $Id: AbstractCountTest.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public abstract class AbstractCountTest extends TestCase
{
  private int stmtCount;
  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  private static final File SPY_PROPS_FILE = new File("testsuite-spy.properties");

  /**
   * sets the spy.properties file name.
   */
  protected void setUp() throws Exception
  {
    if (!SPY_PROPS_FILE.exists())
      fail("Missing file: " + SPY_PROPS_FILE.getAbsolutePath());
    P6SpyProperties.setSpyProperties(SPY_PROPS_FILE.getName());
    checkP6spyEnabled(PersistenceBrokerFactory.getDefaultKey());
  }

  /**
   * start count SQL statements
   */
  protected final void resetStmtCount()
  {
    stmtCount = CountLogger.getSQLStatementCount();
  }

  /**
   * assert that the number of statements issued since the last call of {@link #resetStmtCount()}.
   * is between two specified numbers.
   *
   * @param msg short description of the actions since the last call of {@link #resetStmtCount()}.
   */
  protected final void assertStmtCount(String msg, int minExpected, int maxExpected)
  {
    int stmtNum = CountLogger.getSQLStatementCount() - stmtCount;
    if (stmtNum > maxExpected)
      fail(msg + ": more SQL statements than expected. Expected: " + maxExpected + ", was: " + stmtNum);
    else if (minExpected > 0 && stmtNum == 0)
      fail("No SQL statements, maybe CountLogger not enabled?");
    else if (stmtNum < minExpected)
      fail(msg + ": less SQL statements than expected (Performance improvement? Please correct test limit)."
               +  " Expected: " + minExpected + ", was: " + stmtNum);
    else
    {
      logStmtCount(msg, stmtNum);
    }
  }

  /**
   * assert that the number of statements issued since the last call of {@link #resetStmtCount()}.
   * is equal to a specified number.
   *
   * @param msg short description of the actions since the last call of {@link #resetStmtCount()}.
   */
  protected final void assertStmtCount(String msg, int expected)
  {
    assertStmtCount(msg, expected, expected);
  }

  private void logStmtCount(String msg, int num)
  {
    logger.info(msg + ": " + num);
  }

  protected final void logStmtCount(String msg)
  {
    logStmtCount(msg, CountLogger.getSQLStatementCount() - stmtCount);
  }

  /**
   * fail ifF the specified PersistenceBroker does not use P6Spy.
   */
  protected final void checkP6spyEnabled(PBKey pbKey)
  {
    JdbcConnectionDescriptor conDesc
            = MetadataManager.getInstance().connectionRepository().getDescriptor(pbKey);
    if (!P6SpyDriver.class.getName().equals(conDesc.getDriver()))
    {
      fail("this test works only with p6spy.\n" +
           "Please set 'driver=" + P6SpyDriver.class.getName() + "' in file repository_database.xml" +
           " or use ant build property '-DuseP6Spy=true'");
    }
  }
}
