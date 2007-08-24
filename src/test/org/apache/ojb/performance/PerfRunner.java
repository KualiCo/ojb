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


/**
 * Derivate this class to implement a test instance for the performance test.
 *
 * @version $Id: PerfRunner.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
class PerfRunner
{
    private final String PREFIX_LOG = "[" + this.getClass().getName() + "] ";

    /**
     * testTimes[0] startTime/test length
     * testTimes[1] inserting times
     * testTimes[2] fetching times
     * testTimes[3] fetching repeat times
     * testTimes[4] get by Identity times
     * testTimes[5] updating times
     * testTimes[6] deleting times
     */
    private long[] testTimes;
    private ThreadGroup threadGroup;
    private PerfMain perfMain;
    private long perfTestId;
    private boolean checked;
    /**
     * The threads that are executing.
     */
    private Thread threads[] = null;
    private Class testClass;
    private PerfTest test;


    public PerfRunner(Class perfTestClass)
    {
        this.perfTestId = System.currentTimeMillis();
        this.checked = false;
        this.testClass = perfTestClass;
        // create a tmp test instance
        this.test = createTest();
        this.threadGroup = new ThreadGroup(testName() + "_Group");
    }

    private PerfTest createTest()
    {
        try
        {
            PerfTest result = (PerfTest) testClass.newInstance();
            result.setPerfRunner(this);
            return result;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("Can't create test instance: " + e.getMessage());
        }
    }

    /**
     * Returns the name of the test
     */
    public String testName()
    {
        return test.testName();
    }

    private void checkApi() throws Exception
    {
        String name = testName() + "_Pre_Test_Object";
        PerfArticle article = test.getPreparedPerfArticle(name);
        PerfArticle[] arr = new PerfArticle[]{article};
        test.insertNewArticles(arr);
        test.readArticlesByCursor(name);
        test.updateArticles(arr);
        test.deleteArticles(arr);
        checked = true;
    }

    /**
     * Interrupt the running threads.
     */
    protected void interruptThreads()
    {
        if (threads != null)
        {
            for (int i = 0; i < threads.length; i++)
            {
                threads[i].interrupt();
            }
        }
        PerfMain.printer().println("## Test failed! ##");
        PerfMain.printer().println("## Test failed! ##");
    }

    /**
     * Run the threads.
     */
    protected void runTestHandles(final PerfTest[] runnables)
    {
        if (runnables == null)
        {
            throw new IllegalArgumentException("runnables is null");
        }
        threads = new Thread[runnables.length];
        for (int i = 0; i < threads.length; i++)
        {
            threads[i] = new Thread(threadGroup, runnables[i]);
        }
        for (int i = 0; i < threads.length; i++)
        {
            threads[i].start();
        }
        try
        {
            for (int i = 0; i < threads.length; i++)
            {
                threads[i].join();
            }
        }
        catch (InterruptedException ignore)
        {
            PerfMain.printer().println(PREFIX_LOG + "Thread join interrupted.");
        }

        // should always be skipped, because we use 'thread.join'
        while(threadGroup.activeCount() > 0)
        {
            PerfMain.printer().println("## active threads: " + threadGroup.activeCount());
        }

        threads = null;
    }

    public void performTest()
    {
        try
        {
            // prepare tmp used test
            test.init();

            if (!checked)
            {
                checkApi();
                //PerfMain.printer().println("# PerfTest: " + testName() + " # ");
            }

            int objectCount;
            int objectCountAfter;

            testTimes = new long[7];

            objectCount = test.articleCount();

            // now we start the test threads
            PerfTest[] perfHandles = new PerfTest[PerfMain.getConcurrentThreads()];
            for (int i = 0; i < PerfMain.getConcurrentThreads(); i++)
            {
                perfHandles[i] = createTest();
            }
            runTestHandles(perfHandles);
            
            // end of test threads
            objectCountAfter = test.articleCount();
            perfMain.addPeriodResult(testName(), testTimes);
            perfMain.addConsistentResult(testName(), objectCount, objectCountAfter);

            // tear down tmp used test
            test.tearDown();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            perfMain.registerException(PREFIX_LOG, e);
        }
    }

    public void registerException(String causer, Exception e)
    {
        perfMain.registerException(causer, e);
    }

    public synchronized void addTime(short position, long time)
    {
        testTimes[position] += time;
    }

    public void registerPerfMain(PerfMain aPerfMain)
    {
        this.perfMain = aPerfMain;
    }

    public ThreadGroup getThreadGroup()
    {
        return threadGroup;
    }

    public long getPerfTestId()
    {
        return perfTestId;
    }
}
