/*
 * (C) 2003 ppi Media
 * User: om
 */

package org.apache.ojb.broker.sqlcount;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.cloneable.CloneableGroup;
import org.apache.ojb.broker.util.configuration.impl.OjbConfiguration;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;

/**
 * @author <a href="mailto:om@ppi.de">Oliver Matz</a>
 * @version $Id: TwoLevelSimpleTest.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class TwoLevelSimpleTest extends AbstractCountTest
{
  private Class old_ObjectCache;
  private String[] old_CacheFilter;

  private OjbConfiguration getConfig()
  {
      return (OjbConfiguration) OjbConfigurator.getInstance().getConfigurationFor(null);
  }
  /**
   * switch cache to {@link org.apache.ojb.broker.cache.ObjectCacheTwoLevelImpl}.
   * @throws Exception
   */
  protected void setUp() throws Exception
  {
    //ObjectCacheFactory.getInstance().setClassToServe(ObjectCacheTwoLevelImpl.class);
    //ObjectCacheFactory.getInstance().setClassToServe(org.apache.ojb.broker.cache.ObjectCachePerBrokerImpl.class);
    super.setUp();
    //old_CacheFilter = getConfig().getCacheFilters();
    //old_ObjectCache = ObjectCacheFactory.getInstance().getClassToServe();
  }

  /**
   * undo Cache change.
   * @throws Exception
   */
  protected void tearDown() throws Exception
  {
    //getConfig().setCacheFilters(old_CacheFilter);
    //ObjectCacheFactory.getInstance().setClassToServe(old_ObjectCache);
    super.tearDown();
  }

  /**
   * retrieve one CdArticle twice.
   */
  public void testAccessArticleTwice()
  {
    PersistenceBroker pb0, pb1;
    pb0 = PersistenceBrokerFactory.defaultPersistenceBroker();
    pb1 = PersistenceBrokerFactory.defaultPersistenceBroker();
    assertNotSame(pb0, pb1);

    resetStmtCount();
    pb0.clearCache();
    pb0.beginTransaction();
    Identity id = new Identity(null, CloneableGroup.class, new Object[] {new Integer(1)});
    logger.info(id.toString());
    assertNull(id.getObjectsRealClass());
    Object group0 = pb0.getObjectByIdentity(id);
    assertNotNull(group0);
    assertEquals(CloneableGroup.class, id.getObjectsRealClass());
    assertStmtCount("access one group", 1);
    pb0.commitTransaction();

    resetStmtCount();
    pb1.beginTransaction();
    Object group1 = pb1.getObjectByIdentity(id);
    assertStmtCount("access one group again", 0); // lookup again, 2nd level hit, no SQL access.
    assertNotSame(group0, group1);
    pb1.commitTransaction();
  }
}
