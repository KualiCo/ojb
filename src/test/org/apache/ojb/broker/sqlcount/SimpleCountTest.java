package org.apache.ojb.broker.sqlcount;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.Person;

import java.util.Collection;

/**
 * @author <a href="mailto:om@ppi.de">Oliver Matz</a>
 * @version $Id: SimpleCountTest.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class SimpleCountTest
        extends org.apache.ojb.broker.sqlcount.AbstractCountTest
{
  private Identity aId;

  protected PersistenceBroker myPB;

  protected void setUp() throws Exception
  {
    super.setUp();
    resetStmtCount();
    myPB = PersistenceBrokerFactory.defaultPersistenceBroker();
    myPB.beginTransaction();
    Person a = new Person();
    a.setFirstname("A");
    myPB.store(a);
    aId = new Identity(a, myPB);
    myPB.commitTransaction();
    logStmtCount("Wrote test data");
  }

  /**
   * very simple test: retrieve a person from the database
   */
  public void testRetrievePerson()
  {
    resetStmtCount();
    myPB.clearCache();
    logger.info("begin txn");
    myPB.beginTransaction();
    logger.info("retrieving person");
    Person a = (Person)myPB.getObjectByIdentity(aId);
    // SELECT ... FROM PERSON WHERE ID = ..
    // SELECT ... FROM PERSON_PROJECT WHERE PERSON_ID = ..
    // SELECT ... FROM PROJECT, PERSON_PROJECT WHERE ...
    logger.info("comitting txn");
    // COMMIT
    myPB.commitTransaction();
    assertStmtCount("retrieve Person by Identity", 4);
  }

  public void testRetrievePersonTwice()
  {
    resetStmtCount();
    myPB.clearCache();
    logger.info("begin txn");
    myPB.beginTransaction();
    Person a = (Person)myPB.getObjectByIdentity(aId);   // see above
    assertStmtCount("retrieve Person by Identity", 3);
    resetStmtCount();
    Person b = (Person)myPB.getObjectByIdentity(aId);   // should use cache
    assertSame(a, b);
    assertStmtCount("retrieve Person 2nd time", 0);
    myPB.commitTransaction();
  }

  public void testRetrieveEmptyProjects()
  {
    resetStmtCount();
    myPB.clearCache();
    logger.info("begin txn");
    myPB.beginTransaction();
    Person a = (Person)myPB.getObjectByIdentity(aId);   // see above
    assertStmtCount("retrieve Person by Identity", 3);
    resetStmtCount();
    logger.info("accessing projects");
    Collection c = a.getProjects();
    assertEquals(0, c.size());
    assertStmtCount("accessing non-proxy collection", 0);
    Collection d = a.getRoles();
    assertStmtCount("accessing proxy-collection", 0);
    assertEquals(0, d.size());
    myPB.commitTransaction();
  }
}
