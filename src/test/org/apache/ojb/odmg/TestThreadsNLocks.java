package org.apache.ojb.odmg;

import java.util.Collection;

import org.apache.ojb.broker.TestHelper;
import org.apache.ojb.odmg.shared.Article;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.OQLQuery;
import org.odmg.Transaction;


/**
 * put your documentation comment here
 */
public class TestThreadsNLocks extends Thread
{

    private static String databaseName;
    private static Implementation odmg;
    private static Database db;

	static
	{
        databaseName = TestHelper.DEF_DATABASE_NAME;
	}

    /**
     * put your documentation comment here
     * @param args
     */
    public static void main(String[] args)
    {
        try
        {

            // get odmg facade instance
            odmg = OJB.getInstance();
            db = odmg.newDatabase();
            //open database

            db.open(databaseName, Database.OPEN_READ_WRITE);


            TestThreadsNLocks test = new TestThreadsNLocks();
            test.start();
            TestThreadsNLocks test2 = new TestThreadsNLocks();
            test2.start();
        }
        catch (Exception except)
        {
            System.out.println(except);
            except.printStackTrace(System.out);
        }
    }

    /**
     * put your documentation comment here
     * @param     PrintWriter writer
     */
    public TestThreadsNLocks() throws Exception
    {

    }

    /**
     * put your documentation comment here
     */
    public void run()
    {

        //System.out.println("The list of available products:");
        try
        {
            // 1. open a transaction
            Transaction tx = odmg.newTransaction();
            tx.begin();
            // 2. get an OQLQuery object from the ODMG facade
            OQLQuery query = odmg.newOQLQuery();
            // 3. set the OQL select statement
            query.create("select all from " + Article.class.getName());
            // 4. perform the query and store the result in a persistent Collection
            Collection allArticles = (Collection) query.execute();
            // 5. now iterate over the result to print each product
            java.util.Iterator iter = allArticles.iterator();
            Article a = null;
            while (iter.hasNext())
            {
                a = (Article) iter.next();

                if (tx.tryLock(a, Transaction.WRITE))
                {

                    //db.makePersistent(a);
                    //System.out.println(super.getName() + " ---- Lock erhalten & warten"+ a);
                    Thread.sleep(1000);
                    a.setArticleName(super.getName() + a.getArticleId());
                }
                else
                {
                    //System.out.println(super.getName() + " ---- Konnte lock nicht bekommen" + a);
                }

            }

            tx.commit();
            db.close();
            //System.out.println("DB-close");
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            // db.close();
        }
    }
}





