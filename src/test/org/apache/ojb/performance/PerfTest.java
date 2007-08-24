package org.apache.ojb.performance;

/* Copyright 2002-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Collection;
import java.util.Iterator;

/**
 * Derivate this class to implement a test client for the performance test.
 *
 * @version $Id: PerfTest.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public abstract class PerfTest implements Runnable
{
    private String PREFIX_LOG = "[" + this.getClass().getName() + "] ";
    private PerfRunner test;
    private String objectName;

    public PerfTest()
    {
    }

    /**
     * Returns the name of the test
     */
    public abstract String testName();

    /**
     * Returns the count of all found {@link PerfArticle}
     * in database.
     * This method is not involved in the performance test
     * methods, thus it's not mandatory to use the api-methods
     * for implementation.
     */
    public abstract int articleCount();

    /**
     * Init the test. do setup stuff here
     */
    public abstract void init() throws Exception;

    /**
     * Do clean up.
     */
    public abstract void tearDown() throws Exception;

    /**
     * Store the given articles to database. Do optimize
     * performance.
     */
    public abstract void insertNewArticles(PerfArticle[] arr) throws Exception;

    /**
     * Store the given articles to database. Implement a really
     * resource stressing way.
     */
    public abstract void insertNewArticlesStress(PerfArticle[] arr) throws Exception;

    /**
     * Read all stored articles from the database and return the
     * result as collection of <code>PerfArticles</code>.
     * Do optimize performance.
     * @param articleName article name used for all {@link PerfArticle} created
     * by this instance/thread. Use this name in your query to match all belonging articles
     */
    public abstract Collection readArticlesByCursor(String articleName) throws Exception;

    /**
     * Read all stored articles from the database and return the
     * result as collection of <code>PerfArticles</code>.
     * Do optimize performance.
     * @param articleId the primary key of a {@link PerfArticle} instance
     * @return The matching {@link PerfArticle} instance or <em>null</em> if not found.
     */
    public abstract PerfArticle getArticleByIdentity(Long articleId) throws Exception;

    /**
     * Delete all given article from the database.
     * Do optimize performance.
     */
    public abstract void deleteArticles(PerfArticle[] arr) throws Exception;

    /**
     * Delete all given article from the database in a really resource
     * sressing way.
     */
    public abstract void deleteArticlesStress(PerfArticle[] arr) throws Exception;

    /**
     * Update the given articles. Do optimize
     * performance.
     */
    public abstract void updateArticles(PerfArticle[] arr) throws Exception;

    /**
     * Update the given articles. Implement a really
     * resource stressing way.
     */
    public abstract void updateArticlesStress(PerfArticle[] arr) throws Exception;

    /**
     * Called to get a new instance class of the {@link org.apache.ojb.performance.PerfArticle}
     * interface, override this method if you need your own implementation
     * (with default constructor) of the PerfArticle-Interface.
     * <br/>
     * By default this method returns a new instance of the
     * {@link org.apache.ojb.performance.PerfArticleImpl} class.
     *
     */
    public PerfArticle newPerfArticle()
    {
        return new PerfArticleImpl();
    }

    /**
     * The returned name was used as 'articleName' for all
     * created <code>PerfArticles</code> for this thread.
     * This allows an easy build of the query statement
     * to match the created {@link PerfArticle} for this
     * instance/thread.
     */
    public String getTestObjectName()
    {
        if (objectName == null)
            objectName = testName() + "_" +
                    Thread.currentThread().toString() + "_" + test.getPerfTestId();
        return objectName;
    }

    /**
     * Factory method that creates an {@link org.apache.ojb.performance.PerfArticle}
     * using the {@link org.apache.ojb.performance.PerfArticleImpl} class,
     * override this method if you need your own implementation
     * of the PerfArticle-Interface.
     *
     * @param articleName set the 'articleName'
     * @return the created PerfArticle object
     */
    public PerfArticle getPreparedPerfArticle(String articleName)
    {
        PerfArticle a = newPerfArticle();
        a.setArticleName(articleName);
        a.setMinimumStock(100);
        a.setPrice(0.45);
        a.setProductGroupId(1);
        a.setStock(234);
        a.setSupplierId(4);
        a.setUnit("bottle");
        return a;
    }

    void setPerfRunner(PerfRunner test)
    {
        this.test = test;
    }

    /**
     * Runnable implementation method.
     */
    public void run()
    {
        PerfArticle[] m_arr = new PerfArticle[PerfMain.getIterationsPerThread()];
        for (int i = 0; i < PerfMain.getIterationsPerThread(); i++)
        {
            m_arr[i] = getPreparedPerfArticle(getTestObjectName());
        }

        try
        {
            long totalTime = 0;
            long period;
            init();

            // insert objects
            if (PerfMain.isUseStressMode())
            {
                period = System.currentTimeMillis();
                insertNewArticlesStress(m_arr);
                period = System.currentTimeMillis() - period;
                test.addTime(PerfMain.TIME_INSERT, period);
                totalTime+=period;
            }
            else
            {
                period = System.currentTimeMillis();
                insertNewArticles(m_arr);
                period = System.currentTimeMillis() - period;
                test.addTime(PerfMain.TIME_INSERT, period);
                totalTime+=period;
                // System.out.println("I=" + period);
            }
            checkInsertResult(m_arr);

            // read objects
            period = System.currentTimeMillis();
            Collection col = readArticlesByCursor(objectName);
            period = System.currentTimeMillis() - period;
            try
            {
                checkQueryResult(col, m_arr);
            }
            catch (Exception e)
            {
                test.registerException(PREFIX_LOG
                        + "(Something wrong with query result or with object insert operation) ", e);
            }
            test.addTime(PerfMain.TIME_FETCH, period);
            totalTime+=period;
            // System.out.println("R=" + period);


            // read objects 2
            period = System.currentTimeMillis();
            col = readArticlesByCursor(objectName);
            period = System.currentTimeMillis() - period;
            try
            {
                checkQueryResult(col, m_arr);
            }
            catch (Exception e)
            {
                test.registerException(PREFIX_LOG
                        + "(Something wrong with query result or with object insert operation) ", e);
            }
            test.addTime(PerfMain.TIME_FETCH_2, period);
            totalTime+=period;
            // System.out.println("R2=" + period);


            // get by Identity
            period = System.currentTimeMillis();
            PerfArticle result;
            for(int i = 0; i < m_arr.length; i++)
            {
                if(i%4==0)
                {
                    PerfArticle perfArticle = m_arr[i];
                    result = getArticleByIdentity(perfArticle.getArticleId());
                    if(result == null)
                    {
                        test.registerException("Unexpected result: Get by Identity is 'null' for "
                                + PerfArticle.class.getName() + " with primary key "
                                + perfArticle.getArticleId(), null);
                    }
                }
            }
            period = (System.currentTimeMillis() - period);
            test.addTime(PerfMain.TIME_BY_IDENTITY, period);
            totalTime+=period;
            // System.out.println("B=" + period);


            // update objects
            modifyPerfArticle(m_arr);
            if (PerfMain.isUseStressMode())
            {
                period = System.currentTimeMillis();
                updateArticlesStress(m_arr);
                period = System.currentTimeMillis() - period;
                test.addTime(PerfMain.TIME_UPDATE, period);
                totalTime+=period;
            }
            else
            {
                period = System.currentTimeMillis();
                updateArticles(m_arr);
                period = System.currentTimeMillis() - period;
                test.addTime(PerfMain.TIME_UPDATE, period);
                totalTime+=period;
                // System.out.println("U=" + period);
            }

            // delete objects
            if (PerfMain.isUseStressMode())
            {
                period = System.currentTimeMillis();
                deleteArticlesStress(m_arr);
                period = System.currentTimeMillis() - period;
                test.addTime(PerfMain.TIME_DELETE, period);
                totalTime+=period;
            }
            else
            {
                period = System.currentTimeMillis();
                deleteArticles(m_arr);
                period = System.currentTimeMillis() - period;
                test.addTime(PerfMain.TIME_DELETE, period);
                totalTime+=period;
                // System.out.println("D=" + period);
            }
            test.addTime(PerfMain.TIME_TOTAL, totalTime);
            tearDown();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            test.registerException(PREFIX_LOG + "(Unexpected behaviour) ", e);
            test.interruptThreads();
        }
    }

    private void modifyPerfArticle(PerfArticle[] m_arr)
    {
        PerfArticle article;
        String prefix = "updated_";
        for (int i = 0; i < m_arr.length; i++)
        {
            article = m_arr[i];
            article.setArticleName(prefix + article.getArticleName());
        }
    }

    private void checkQueryResult(Collection col, PerfArticle[] m_arr) throws Exception
    {
        if(col.size() > 0)
        {
            Iterator it = col.iterator();
            Object obj = it.next();
            if(!(obj instanceof PerfArticle))
            {
                throw new Exception("Wrong object type found. Expected instance of"+
                    PerfArticle.class.getName() + ", found " + obj.getClass().getName());
            }
        }
        if (col.size() != m_arr.length)
        {
            throw new Exception("Read objects: Wrong number of objects found. Expected " +
                    (m_arr.length) + ", found " + col.size());
        }
    }

    private void checkInsertResult(PerfArticle[] m_arr) throws Exception
    {
        for(int i = 0; i < m_arr.length; i++)
        {
            PerfArticle perfArticle = m_arr[i];
            if(perfArticle.getArticleId() == null)
            {
                throw new Exception("Insert objects: Object with 'null' PK found");
            }
        }
    }
}
