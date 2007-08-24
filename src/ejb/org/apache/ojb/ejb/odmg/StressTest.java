package org.apache.ojb.ejb.odmg;

/* Copyright 2004-2005 The Apache Software Foundation
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

import javax.ejb.EJBHome;
import javax.naming.Context;
import javax.rmi.PortableRemoteObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.ejb.ArticleVO;
import org.apache.ojb.ejb.ContextHelper;

/**
 * stress test against one database using several threads
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>.
 * @version $Id: StressTest.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class StressTest
{
    private static int iterationsPerThread = 50;
    private static int concurrentThreads = 2;
    private ODMGSessionRemote bean;
    private static boolean testClientPass = true;

    /**
     * times[0] startTime/test length
     * times[1] inserting times
     * times[2] fetching times
     * times[3] deleting times
     */
    private long[] times;

    /**
     * The threads that are executing.
     */
    private Thread threads[] = null;

    public StressTest()
    {
    }

    public static void performTest(int[] args) throws Exception
    {
        if (args.length > 0)
        {
            concurrentThreads = args[0];
        }
        if (args.length > 1)
        {
            iterationsPerThread = args[1];
        }

        StressTest test = new StressTest();
        int objectCount = 0;
        int objectCountAfter = 0;
        test.init();
        objectCount = test.getArticleCount();
        test.runMultithreaded();

        System.out.println("Test-Info:   Objects in DB before ODMG test: " + objectCount);
        objectCountAfter = test.getArticleCount();
        System.out.println("Test-Info:   Objects in DB after ODMG test: " + objectCountAfter);
        System.out.println("Test-Info:   Stress test was successful? - " + (objectCount == objectCountAfter && testClientPass) + " -");
        if(!testClientPass) throw new RuntimeException("Test does not pass");
    }

    /**
     * Setting up the test fixture.
     */
    private void init() throws Exception
    {
        Context ctx = ContextHelper.getContext();
        times = new long[4];
        try
        {
            Object object = PortableRemoteObject.narrow(ctx.lookup(ODMGSessionHome.JNDI_NAME), EJBHome.class);
            bean = ((ODMGSessionHome) object).create();
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
            throw e;
        }
    }

    private int getArticleCount() throws Exception
    {
        try
        {
            return bean.getArticleCount();
        }
        catch(java.rmi.RemoteException e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    private void runMultithreaded() throws Exception
    {
        String sep = SystemUtils.LINE_SEPARATOR;

        System.out.println(sep + sep + "++ Start thread generation for ODMG api test ++");
        System.out.println("Begin with performance test, " + concurrentThreads +
                " concurrent threads, handle " + iterationsPerThread + " articles per thread");
        ODMGTestClient[] clientsODMG = new ODMGTestClient[concurrentThreads];
        for (int i = 0; i < concurrentThreads; i++)
        {
            ODMGTestClient obj = new ODMGTestClient(this);
            clientsODMG[i] = obj;
        }
        System.out.println("");
        times[0] = System.currentTimeMillis();
        runTestClients(clientsODMG);
        times[0] = (System.currentTimeMillis() - times[0]);
        System.out.println(buildTestSummary("ODMG API"));
        System.out.println("++ End of performance test ODMG api ++" + sep + sep);
    }

    /**
     * Interrupt the running threads.
     */
    synchronized void interruptThreads()
    {
        testClientPass = false;
        if (threads != null)
        {
            for (int i = 0; i < threads.length; i++)
            {
                threads[i].interrupt();
            }
        }
        System.err.println("## Test failed! ##");
        System.err.println("## Test failed! ##");
    }

    /**
     * Run the threads.
     */
    void runTestClients(final TestClient[] runnables) throws Exception
    {
        if (runnables == null)
        {
            throw new IllegalArgumentException("runnables is null");
        }
        threads = new Thread[runnables.length];
        for (int i = 0; i < threads.length; i++)
        {
            threads[i] = new Thread(runnables[i]);
        }
        for (int i = 0; i < threads.length; i++)
        {
            threads[i].start();
            threads[i].join();
        }
        threads = null;
    }

    synchronized void addTime(int position, long time)
    {
        times[position] = times[position] + time;
    }

    private String buildTestSummary(String key)
    {
        String sep = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer();
        buf.append(sep);
        buf.append("----------------------------------------------------");
        buf.append(sep);
        buf.append("TEST SUMMARY - " + key);
        buf.append(sep);
        buf.append(concurrentThreads + " concurrent threads, handle " + iterationsPerThread + " articles per thread");
        buf.append(sep);
        buf.append("Test period: " + (((double) times[0]) / 1000) + " [sec]");
        buf.append(sep);
        buf.append("Inserting period: " + times[1] + " [msec]");
        buf.append(sep);
        buf.append("Fetching period: " + times[2] + " [msec]");
        buf.append(sep);
        buf.append("Deleting period: " + times[3] + " [msec]");
        buf.append(sep);
        buf.append("----------------------------------------------------");

        return buf.toString();
    }



    //*********************************************************************************
    // inner classes
    //*********************************************************************************

    static abstract class TestClient implements Runnable
    {

    }

/**
     * ODMG-api test class
     */
    static class ODMGTestClient extends TestClient
    {
        private List articlesList;
        private ODMGSessionRemote bean;
        private String articleName;
        private StressTest test;

        public ODMGTestClient(StressTest test)
        {
            this.test = test;
            init();
            articlesList = new ArrayList();
            articleName = "ODMGTestClient_" + System.currentTimeMillis();
            for(int i = 0; i < iterationsPerThread; i++)
            {
                articlesList.add(createArticle(articleName));
            }
        }

        protected void init()
        {
            Context ctx = ContextHelper.getContext();
            try
            {
                Object object = PortableRemoteObject.narrow(ctx.lookup(ODMGSessionHome.JNDI_NAME), EJBHome.class);
                bean = ((ODMGSessionHome) object).create();
            }
            catch(Exception e)
            {
                e.printStackTrace();
                throw new OJBRuntimeException("Can't lookup bean: " + ODMGSessionHome.JNDI_NAME, e);
            }
        }

        public void run()
        {
            //log.info("Thread "+this+" run");
            try
            {
                insertNewArticles();
                readAllArticles();
                deleteArticles();
            }
            catch(Throwable e)
            {
                System.err.println("Error in client: " + e.getMessage());
                test.interruptThreads();
                throw new OJBRuntimeException("[" + ODMGTestClient.class.getName()
                        + "] Stress test client cause exception, thread was " + Thread.currentThread(), e);
            }
        }

        /**
         * factory method that createa an ArticleVO
         * @return the created ArticleVO object
         */
        private ArticleVO createArticle(String articleName)
        {
            ArticleVO a = new ArticleVO();
            a.setName(articleName);
            a.setPrice(new BigDecimal(0.45 * articleName.hashCode()));
            a.setDescription("description " + articleName.hashCode());
            return a;
        }

        protected void deleteArticles() throws Exception
        {
            long start = System.currentTimeMillis();

            bean.deleteObjects(articlesList);

            long stop = System.currentTimeMillis();
            test.addTime(3, stop - start);
        }

        protected void insertNewArticles() throws Exception
        {
            long start = System.currentTimeMillis();

            articlesList = bean.storeObjects(articlesList);

            long stop = System.currentTimeMillis();
            test.addTime(1, stop - start);
        }

        protected void readAllArticles() throws Exception
        {
            long start = System.currentTimeMillis();

            Iterator it = bean.getArticlesByName(articleName).iterator();
            while(it.hasNext()) it.next();

            long stop = System.currentTimeMillis();
            test.addTime(2, stop - start);
        }
    }
}
