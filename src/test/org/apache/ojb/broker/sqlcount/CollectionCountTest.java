/*
 * (C) 2003 ppi Media
 * User: om
 */

package org.apache.ojb.broker.sqlcount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.Article;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.InterfaceArticle;
import org.apache.ojb.broker.InterfaceProductGroup;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.Person;
import org.apache.ojb.broker.ProductGroup;
import org.apache.ojb.broker.Project;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.util.ObjectModification;

/**
 * @author <a href="mailto:om@ppi.de">Oliver Matz</a>
 * @version $Id: CollectionCountTest.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class CollectionCountTest extends AbstractCountTest
{
  protected PersistenceBroker myPB;

  protected void setUp() throws Exception
  {
    super.setUp();
    myPB = PersistenceBrokerFactory.defaultPersistenceBroker();
  }

  protected void tearDown() throws Exception
  {
    if ((myPB != null) && !myPB.isClosed())
    {
      myPB.close();
    }
    super.tearDown();
  }

  /**
   * retrieve all product groups.
   */
  public void testAllProductGroups()
  {
    resetStmtCount();
    myPB.clearCache();
    myPB.beginTransaction();
    Query qry = new QueryByCriteria(ProductGroup.class, null);
    assertStmtCount("preparation", 0);
    Iterator iter = myPB.getIteratorByQuery(qry);
    assertStmtCount("getIteratorByQuery", 1);
    while (iter.hasNext())
    {
      resetStmtCount();
      InterfaceProductGroup next = (InterfaceProductGroup) iter.next();
      assertStmtCount("next", 0);
      List articles = next.getAllArticles();
      // SELECT ... FROM Kategorien
      // SELECT ... FROM Artikel
      // SELECT ... FROM BOOKS
      // SELECT ... FROM CDS
      assertStmtCount("getAllArticles", 4);
    }
    myPB.commitTransaction();
  }

  /**
   * retrieve Product group number 5 and its 12 articles
   */
  public void testProductGroup5()
  {
    resetStmtCount();
    myPB.clearCache();
    myPB.beginTransaction();
    ProductGroup pg =
            (ProductGroup)myPB.getObjectByIdentity(new Identity(null, ProductGroup.class, new Object[] {new Integer(5)}));
    assertStmtCount("getObjectByIdentity", 4);
    resetStmtCount();
    List articles = pg.getAllArticles();
    assertEquals(12, articles.size());
    assertStmtCount("getAllArticles", 0);
    resetStmtCount();
    for (Iterator articleIterator = articles.iterator(); articleIterator.hasNext(); )
    {
      InterfaceArticle article = (InterfaceArticle)articleIterator.next();
      logger.info("articleId " + article.getArticleId());
      // SELECT ... FROM Artikel WHERE Artikel_Nr = ...
    }
    assertStmtCount("collect ids: ", 1);  // batch retrieval!
    resetStmtCount();
    String str = pg.toString();
    // SELECT ... FROM Kategorien WHERE Kategorie_Nr = '5'
    // SELECT ... FROM Artikel A0 WHERE Kategorie_Nr =  '5'
    // SELECT ... FROM BOOKS A0   WHERE Kategorie_Nr =  '5'
    // SELECT ... FROM CDS A0     WHERE Kategorie_Nr =  '5'
    assertStmtCount("toString", 4);
    logger.info(str);
    myPB.commitTransaction();
  }

  /*
  * insert a person with an empty project collection.
  * note: the <em>first</em> Person and Project require extra lookups
  * in the table OJB_HL_SEQ.
  */
  public void testPersonEmptyProjectsInsert()
  {
    resetStmtCount();
    myPB.clearCache();
    myPB.beginTransaction();
    Person pers = new Person();
    myPB.store(pers);
    //SELECT A0.VERSION,A0.GRAB_SIZE,A0.MAX_KEY,A0.FIELDNAME,A0.TABLENAME FROM OJB_HL_SEQ A0 WHERE (A0.TABLENAME LIKE  'SEQ_PERSON' ) AND A0.FIELDNAME LIKE  'ID'
    //SELECT VERSION,GRAB_SIZE,MAX_KEY,FIELDNAME,TABLENAME FROM OJB_HL_SEQ WHERE TABLENAME = 'SEQ_PERSON'  AND FIELDNAME = 'ID'
    //UPDATE OJB_HL_SEQ SET MAX_KEY='150',GRAB_SIZE='20',VERSION='7' WHERE TABLENAME = 'SEQ_PERSON'  AND FIELDNAME = 'ID'  AND VERSION = '6'
    // commit|
    //SELECT LASTNAME,FIRSTNAME,ID FROM PERSON WHERE ID = '131'
    //INSERT INTO PERSON (ID,FIRSTNAME,LASTNAME) VALUES ('131','','')
    logStmtCount("Storing first person");  // 6. oma: why so many? double lookup in OJB_HL_SEQ !
    resetStmtCount();
    pers = new Person();
    myPB.store(pers, ObjectModification.INSERT);
    myPB.commitTransaction();
    // INSERT INTO PERSON (ID,FIRSTNAME,LASTNAME) VALUES ('172','','')
    // commit
    assertStmtCount("insert second Person with empty collection.", 2);
  }

  /**
   * insert a person with a project collection with one fresh element.
   * note: the <em>first</em> Person and Project require extra lookups
   * in the table OJB_HL_SEQ.
   */
  public void testPersonSingleProjectInsert()
  {
    resetStmtCount();
    myPB.clearCache();
    myPB.beginTransaction();
    Person pers = new Person();
    pers.setFirstname("testPersonSingleProjectInsert(1)");
    Project proj = new Project();
    proj.setTitle("testPersonSingleProjectInsert(1)");
    myPB.store(pers);
    myPB.store(proj);
    logStmtCount("Storing first person and first project");
    // 12. oma: why so many? double lookup in OJB_HL_SEQ !
    resetStmtCount();
    pers = new Person();
    Project proj2 = new Project();
    proj2.setTitle("proj2");
    Collection projects = Arrays.asList(new Project[] {proj2});
    pers.setProjects(projects);
    myPB.store(pers, ObjectModification.INSERT);
    myPB.commitTransaction();
    // INSERT INTO PERSON (ID,FIRSTNAME,LASTNAME) VALUES ('292','','')
    // SELECT TITLE,DESCRIPTION,ID FROM PROJECT WHERE ID = '88'
    // INSERT INTO PROJECT (ID,TITLE,DESCRIPTION) VALUES ('88','proj2','')
    // SELECT PROJECT_ID FROM PERSON_PROJECT WHERE PERSON_ID='292'      // BRJ: check mn-implementor
    // INSERT INTO PERSON_PROJECT (PERSON_ID,PROJECT_ID) VALUES ('292','88')
    // commit|
    assertStmtCount("insert second Person, singleton collection.", 6);
  }


  public void testPrefetched()
  {
      ClassDescriptor cldProductGroup = myPB.getClassDescriptor(ProductGroup.class);
      ClassDescriptor cldArticle = myPB.getClassDescriptor(Article.class);
      Class productGroupProxy = cldProductGroup.getProxyClass();
      Class articleProxy = cldArticle.getProxyClass();

      //
      // use ProductGroup and Articles with disabled Proxy
      //
      cldProductGroup.setProxyClass(null);
      cldProductGroup.setProxyClassName(null);
      cldArticle.setProxyClass(null);
      cldArticle.setProxyClassName(null);

      resetStmtCount();
      myPB.clearCache();


      myPB.beginTransaction();

      Criteria crit = new Criteria();
      crit.addLessOrEqualThan("groupId", new Integer(5));
      QueryByCriteria q = QueryFactory.newQuery(ProductGroup.class, crit);
      q.addOrderByDescending("groupId");
      q.addPrefetchedRelationship("allArticlesInGroup");

      Collection results = myPB.getCollectionByQuery(q);
      assertEquals("Number of ProductGroups", 5, results.size());
      Collection articles = new ArrayList();
      for (Iterator it = results.iterator();it.hasNext();)
      {
          ProductGroup p = (ProductGroup) it.next();
          articles.addAll(p.getAllArticles());
      }
      assertEquals("Total number of Articles", 59, articles.size());

      //SELECT A0.KategorieName,A0.Kategorie_Nr,A0.Beschreibung FROM Kategorien A0 WHERE A0.Kategorie_Nr <=  '5' 
      //SELECT ... FROM CDS A0 WHERE A0.Kategorie_Nr IN ( '1' , '4' , '2' , '5' , '3' ) 
      //SELECT ... FROM Artikel A0 WHERE A0.Kategorie_Nr IN ( '1' , '4' , '2' , '5' , '3' ) 
      //SELECT ... FROM BOOKS A0 WHERE A0.Kategorie_Nr IN ( '1' , '4' , '2' , '5' , '3' ) 
      assertStmtCount("Read Prefetched.", 4);
      myPB.commitTransaction();

      //
      // Reset original Proxy settings
      //
      cldProductGroup.setProxyClass(productGroupProxy);
      cldProductGroup.setProxyClassName(productGroupProxy.getName());
      cldArticle.setProxyClass(articleProxy);
      cldArticle.setProxyClassName(articleProxy.getName());

  }

  public void testNonPrefetched()
  {
      ClassDescriptor cldProductGroup = myPB.getClassDescriptor(ProductGroup.class);
      ClassDescriptor cldArticle = myPB.getClassDescriptor(Article.class);
      Class productGroupProxy = cldProductGroup.getProxyClass();
      Class articleProxy = cldArticle.getProxyClass();

      //
      // use ProductGroup and Articles with disabled Proxy
      //
      cldProductGroup.setProxyClass(null);
      cldProductGroup.setProxyClassName(null);
      cldArticle.setProxyClass(null);
      cldArticle.setProxyClassName(null);

      resetStmtCount();
      myPB.clearCache();

      myPB.beginTransaction();

      Criteria crit = new Criteria();
      crit.addLessOrEqualThan("groupId", new Integer(5));
      QueryByCriteria q = QueryFactory.newQuery(ProductGroup.class, crit);
      q.addOrderByDescending("groupId");

      Collection results = myPB.getCollectionByQuery(q);
      assertEquals("Number of ProductGroups", 5, results.size());
      Collection articles = new ArrayList();
      for (Iterator it = results.iterator();it.hasNext();)
      {
          ProductGroup p = (ProductGroup) it.next();
          articles.addAll(p.getAllArticles());
      }
      assertEquals("Total number of Articles", 59, articles.size());

      //SELECT A0.KategorieName,A0.Kategorie_Nr,A0.Beschreibung FROM Kategorien A0 WHERE A0.Kategorie_Nr <=  '5' 
      //SELECT ... FROM CDS A0 WHERE A0.Kategorie_Nr =  '5' 
      //SELECT ... FROM Artikel A0 WHERE A0.Kategorie_Nr =  '5'  
      //SELECT ... FROM BOOKS A0 WHERE A0.Kategorie_Nr =  '5'  
      //SELECT ... FROM CDS A0 WHERE A0.Kategorie_Nr =  '4' 
      //SELECT ... FROM Artikel A0 WHERE A0.Kategorie_Nr =  '4'  
      //SELECT ... FROM BOOKS A0 WHERE A0.Kategorie_Nr =  '4'
      //...
      //SELECT ... FROM CDS A0 WHERE A0.Kategorie_Nr =  '1' 
      //SELECT ... FROM Artikel A0 WHERE A0.Kategorie_Nr =  '1'  
      //SELECT ... FROM BOOKS A0 WHERE A0.Kategorie_Nr =  '1'  
      assertStmtCount("Read Non-Prefetched.", 16);
      myPB.commitTransaction();

      //
      // Reset original Proxy settings
      //
      cldProductGroup.setProxyClass(productGroupProxy);
      cldProductGroup.setProxyClassName(productGroupProxy.getName());
      cldArticle.setProxyClass(articleProxy);
      cldArticle.setProxyClassName(articleProxy.getName());

  }

}
