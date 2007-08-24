/*
 * (C) 2003 ppi Media
 * User: om
 */

package org.apache.ojb.broker.sqlcount;

import org.apache.ojb.broker.CdArticle;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.InterfaceArticle;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.cache.ObjectCacheEmptyImpl;
import org.apache.ojb.broker.util.configuration.impl.OjbConfiguration;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;

/**
 * @author <a href="mailto:om@ppi.de">Oliver Matz</a>
 * @version $Id: EmptyCacheCountTest.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class EmptyCacheCountTest extends AbstractCountTest
{
  protected PersistenceBroker myPB;

  private Class old_ObjectCache;
  private String[] old_CacheFilter;

  private OjbConfiguration getConfig()
  {
      return (OjbConfiguration) OjbConfigurator.getInstance().getConfigurationFor(null);
  }

  /**
   * switch cache to {@link ObjectCacheEmptyImpl}.
   * @throws Exception
   */
  protected void setUp() throws Exception
  {
    //ObjectCacheFactory.getInstance().setClassToServe(ObjectCacheEmptyImpl.class);
    super.setUp();
    myPB = PersistenceBrokerFactory.defaultPersistenceBroker();
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
    resetStmtCount();
    myPB.clearCache();
    myPB.beginTransaction();
    Identity id = new Identity(null, InterfaceArticle.class, new Object[] {new Integer(200)});
    logger.info(id.toString());
    assertNull(id.getObjectsRealClass());
    myPB.getObjectByIdentity(id);
    assertEquals(CdArticle.class, id.getObjectsRealClass());
    assertStmtCount("access one cd", 1, 3); // 3 tables: Artikel, BOOKS, CDS
    resetStmtCount();
    myPB.getObjectByIdentity(id);
    assertStmtCount("access one cd again", 1); // lookup again, but exploit objectsRealClass
    myPB.commitTransaction();
  }
}
