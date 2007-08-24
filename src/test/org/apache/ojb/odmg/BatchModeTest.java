/*
 * BatchModeTest.java
 * JUnit based test
 *
 * Created on February 15, 2003, 12:47 AM
 */

package org.apache.ojb.odmg;

import junit.framework.TestSuite;
import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.shared.Article;
import org.apache.ojb.odmg.shared.ProductGroup;
import org.odmg.Transaction;

/**
 * @author Oleg Nitz
 */
public class BatchModeTest extends ODMGTestCase
{
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(new TestSuite(BatchModeTest.class));
    }

    public void testBatchStatementsOrder()
    {
        // arminw: fixed
        // if(ojbSkipKnownIssueProblem()) return;

        Transaction tx = odmg.newTransaction();
        tx.begin();

        ProductGroup pg1 = new ProductGroup();
        pg1.setName("BatchModeTest ProductGroup #1");
        database.makePersistent(pg1);

        tx.checkpoint();

        Article a1 = Article.createInstance();
        a1.setArticleName("BatchModeTest Article #1");
        a1.setProductGroup(pg1);
        pg1.addArticle(a1);
        database.makePersistent(a1);

        ProductGroup pg2 = new ProductGroup();
        pg2.setName("BatchModeTest ProductGroup #2");
        database.makePersistent(pg2);

        Article a2 = Article.createInstance();
        a2.setArticleName("BatchModeTest Article #2");
        a2.setProductGroup(pg2);
        pg2.addArticle(a2);

        tx.checkpoint();

        database.deletePersistent(a1);

        tx.checkpoint();

        database.deletePersistent(pg1);
        database.deletePersistent(a2);
        database.deletePersistent(pg2);

        tx.checkpoint();
        tx.commit();
    }
}
