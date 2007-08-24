/**
 * User: oliverm
 * $Id: CountLogger.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
package org.apache.ojb.p6spy;

import com.p6spy.engine.logging.appender.P6Logger;
import com.p6spy.engine.logging.appender.FileLogger;


import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Use this class in order to log and count jdbc statements
 *
 * @author <a href="mailto:om@ppi.de">Oliver Matz</a>
 * @version $Id: CountLogger.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class CountLogger extends FileLogger implements P6Logger
{
  protected String lastEntry;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private static int countSQL;

  public CountLogger()
  {
    logger.debug("start logging");
  }

  /**
   * count the statements in case counting is enabled.
   *
   * @see com.p6spy.engine.logging.appender.FormattedLogger#logSQL
   */
  public void logSQL(int i, String s, long l, String s1, String s2, String s3)
  {  
    if (s1.equals("resultset"))
    {
        // BRJ: p6spy workaround 
        // resultset cannot be excluded using p6spy properties
        return;
    }
    
    super.logSQL(i, s, l, s1, s2, s3);
    countSQL++;
    logger.info("sql: " + s1 + "|" + s3);
  }

  /**
   * @return the number of statements issued so far.
   */
  public static int getSQLStatementCount()
  {
    return countSQL;
  }
}
