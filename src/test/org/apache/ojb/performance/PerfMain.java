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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.lang.reflect.Constructor;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 * The OJB stress/performance test - a simple performance test application to
 * run O/R mapper in a simulated single/multi-threaded environment.
 *
 * <p>
 * <b>You have two possibilities to run this test:</b>
 * </p>
 * <p>
 * - use the OJB build script and call
 * <br/>
 * <code>ant perf-test</code>
 * </p>
 * <p>
 * - or for standalone use perform the test class by yourself
 * <br/>
 * <code>
 * java -classpath CLASSPATH org.apache.ojb.performance.PerfMain
 * </code>
 * <br/>
 * <code>
 * [comma separated list of PerfTest implementation classes, no blanks!]
 * </code>
 * <br/>
 * <code>
 * [number of test loops, default '5']
 * </code>
 * <br/>
 * <code>
 * [number of threads, default '10']
 * </code>
 * <br/>
 * <code>
 * [number of insert/fetch/delete loops per thread, default '100']
 * </code>
 * <br/>
 * <code>
 * [boolean - run in stress mode if set true, run in performance mode if set false, default 'false']
 * </code>
 * <br/>
 * <code>
 * [boolean - if 'true' all log messages will be print, else only a test summary, default 'true']
 * </code>
 * </p>
 * <p>
 * For example:
 * </p>
 * <code>java -classpath CLASSPATH my.MyPerfTest,myMyPerfTest2 3 10 200 false true</code>
 *
 * @version $Id: PerfMain.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class PerfMain
{
    static final int TEST_INSERT = 0;
    static final int TEST_FETCH = 1;
    static final int TEST_FETCH_2 = 2;
    static final int TEST_BY_IDENTITY = 3;
    static final int TEST_UPDATE = 4;
    static final int TEST_DELETE = 5;

    static final short TIME_TOTAL = 0;
    static final short TIME_INSERT = 1;
    static final short TIME_FETCH = 2;
    static final short TIME_FETCH_2 = 3;
    static final short TIME_BY_IDENTITY = 4;
    static final short TIME_UPDATE = 5;
    static final short TIME_DELETE = 6;

    /**
     * The factor for get by Identity calls, e.g. 4 means handling 100 objects
     * result in 100/4 getByIdentity calls.
     */
    protected static final int BY_IDENTITY_FACTOR = 4;
    protected static final String EOL = SystemUtils.LINE_SEPARATOR;

    /** iterations per thread */
    private static int iterationsPerThread = 100;
    /** number of concurrent threads */
    private static int concurrentThreads = 10;
    /** if false we use performance optimized delete/insert method */
    private static boolean useStressMode = false;
    /** number of test loops */
    private static int testLoops = 5;
    /** if false only a test summary will be print*/
    private static boolean logAll = true;

    private HashMap resultMap;
    private HashMap exceptionMap;
    private static Printer printer;


    public static void main(String[] args)
    {
        PerfMain main = new PerfMain();
        try
        {
            long peroid = System.currentTimeMillis();
            // start the test
            main.startPerfTest(args);
            // print the test results
            main.printResult();
            peroid = System.currentTimeMillis() - peroid;
            if(logAll) printer().println();
            if(logAll) printer().println("PerfTest takes " + peroid / 1000 + " [sec]");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public PerfMain()
    {
        this.resultMap = new HashMap();
        this.exceptionMap = new HashMap();
    }

    public static Printer printer()
    {
        if(printer == null)
        {
            printer = new Printer();
        }
        return printer;
    }

    /** Call this to begin the performance test. */
    public void startPerfTest(String[] args) throws Exception
    {
        ArrayList testList = null;
        try
        {
            // comma separated list of the PerfTest implementation classes
            if (args.length > 0)
            {
                StringTokenizer tok = new StringTokenizer(args[0], ",");
                testList = new ArrayList();
                while (tok.hasMoreTokens())
                {
                    testList.add(tok.nextToken().trim());
                }
            }
            else
            {
                throw new IllegalArgumentException("No test handles found!");
            }
            // number of test loops
            if (args.length > 1)
            {
                testLoops = args.length > 1 ? Integer.parseInt(args[1]) : 1;
            }
            // number of threads
            if (args.length > 2)
            {
                concurrentThreads = Integer.parseInt(args[2]);
            }
            // number of insert/fetch/delete loops per thread
            if (args.length > 3)
            {
                iterationsPerThread = Integer.parseInt(args[3]);
            }
            // run in stress mode
            if (args.length > 4)
            {
                useStressMode = Boolean.valueOf(args[4]).booleanValue();
            }
            // log mode
            if (args.length > 5)
            {
                logAll = Boolean.valueOf(args[5]).booleanValue();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.err.println();
            System.err.println("Usage of PerfMain:" +
                    "java -classpath CLASSPATH org.apache.ojb.performance.PerfMain" +
                    " [comma separated list of PerfTest implementation classes]" +
                    " [number of test loops]" +
                    " [number of threads]" +
                    " [number of insert/fetch/delete loops per thread]" +
                    " [boolean - run in stress mode]" +
                    " [boolean - if 'true' detailed log messages will be print]");
            System.err.println();
            System.err.println("Example: java -classpath" +
                    " CLASSPATH org.apache.ojb.performance.PerfMain org.MyPerfTest 3 10 500 false");
        }

        if(logAll) printer().println("                                                    " +
                EOL + "Start OJB performance-test framework - running " + testLoops + " loops" +
                EOL + "-------------------------------------------------------");

        PerfRunner test;
        for (int i = 0; i < testLoops; i++)
        {
            Runtime rt = Runtime.getRuntime();
            long freeMem;
            if(logAll) printer().println(" Loop " + (i + 1));

            if(i%2 == 0)
            {
                for(int j = 0; j < testList.size(); j++)
                {
                    String perfTest = (String) testList.get(j);
                    Class testHandle = Class.forName(perfTest);
                    test = new PerfRunner(testHandle);
                    test.registerPerfMain(this);

                    rt.gc();
                    Thread.sleep(300);
                    rt.freeMemory();
                    rt.gc();
                    Thread.sleep(100);
                    freeMem = rt.freeMemory();
                    test.performTest();
                    freeMem = (freeMem - rt.freeMemory()) / 1024;
                    if(logAll) printer().println(" allocated memory=" + freeMem + "kb");
                    // rt.gc();
                }
            }
            else
            {
                for(int j = (testList.size() - 1); j >= 0; j--)
                {
                    String perfTest = (String) testList.get(j);
                    Class testHandle = Class.forName(perfTest);
                    test = new PerfRunner(testHandle);
                    test.registerPerfMain(this);

                    rt.gc();
                    Thread.sleep(300);
                    rt.freeMemory();
                    rt.gc();
                    Thread.sleep(100);
                    freeMem = rt.freeMemory();
                    test.performTest();
                    freeMem = (freeMem - rt.freeMemory()) / 1024;
                    if(logAll) printer().println(" allocated memory: " + freeMem + " kb");
                    // rt.gc();
                }
            }
        }
    }

    public void printResult()
    {
        printer().println();
        if (!getExceptionMap().isEmpty())
        {
            StringBuffer buf = new StringBuffer();
            buf.append(EOL).append("Failures occured, test not valid:").append(EOL);
            Iterator it = getExceptionMap().entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry entry = (Map.Entry) it.next();
                buf.append("Failure cause by ").append(entry.getKey());
                if(entry.getValue() != null)
                {
                    Throwable ex = ExceptionUtils.getRootCause((Exception) entry.getValue());
                    if(ex == null) ex = (Exception) entry.getValue();
                    buf.append(EOL).append("Exception was: ").append(EOL).append(ExceptionUtils.getStackTrace(ex));
                }
                buf.append(EOL);
            }
            printer().println(buf.toString());
        }
        printer().println(buildTestSummary(prepareTestResults()));
    }

    private PerfResult[] prepareTestResults()
    {
        List tmp = new ArrayList(resultMap.values());
        Collections.sort(tmp, new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                PerfResult r1 = (PerfResult) o1;
                PerfResult r2 = (PerfResult) o2;
                return new Long(r1.getTotalTime()).compareTo(new Long(r2.getTotalTime()));
            }
        });

        PerfResult[] results = (PerfResult[]) tmp.toArray(new PerfResult[tmp.size()]);
        long[][] calibration = new long[6][results.length];
        for(int k = 0; k < 6; k++)
        {
            for(int i = 0; i < results.length; i++)
            {
                PerfResult result = results[i];
                if(k==TEST_INSERT) calibration[TEST_INSERT][i] = result.getInsertPeriod();
                if(k==TEST_FETCH) calibration[TEST_FETCH][i] = result.getFetchPeriod();
                if(k==TEST_FETCH_2) calibration[TEST_FETCH_2][i] = result.getFetchSecondPeriod();
                if(k==TEST_BY_IDENTITY) calibration[TEST_BY_IDENTITY][i] = result.getByIdentityPeriod();
                if(k==TEST_UPDATE) calibration[TEST_UPDATE][i] = result.getUpdatePeriod();
                if(k==TEST_DELETE) calibration[TEST_DELETE][i] = result.getDeletePeriod();
            }
        }

        for(int k = 0; k < 6; k++)
        {
            if(k==TEST_INSERT)
            {
                long[] resultArray = calibration[TEST_INSERT];
                long minimum = NumberUtils.min(resultArray);
                for(int i = 0; i < results.length; i++)
                {
                    results[i].setInsertMinimun(minimum);
                }
            }
            if(k==TEST_FETCH)
            {
                long[] resultArray = calibration[TEST_FETCH];
                long minimum = NumberUtils.min(resultArray);
                for(int i = 0; i < results.length; i++)
                {
                    results[i].setFetchMinimun(minimum);
                }
            }
            if(k==TEST_FETCH_2)
            {
                long[] resultArray = calibration[TEST_FETCH_2];
                long minimum = NumberUtils.min(resultArray);
                for(int i = 0; i < results.length; i++)
                {
                    results[i].setFetchSecondMinimun(minimum);
                }
            }
            if(k==TEST_BY_IDENTITY)
            {
                long[] resultArray = calibration[TEST_BY_IDENTITY];
                long minimum = NumberUtils.min(resultArray);
                for(int i = 0; i < results.length; i++)
                {
                    results[i].setByIdentityMinimun(minimum);
                }
            }
            if(k==TEST_UPDATE)
            {
                long[] resultArray = calibration[TEST_UPDATE];
                long minimum = NumberUtils.min(resultArray);
                for(int i = 0; i < results.length; i++)
                {
                    results[i].setUpdateMinimun(minimum);
                }
            }
            if(k==TEST_DELETE)
            {
                long[] resultArray = calibration[TEST_DELETE];
                long minimum = NumberUtils.min(resultArray);
                for(int i = 0; i < results.length; i++)
                {
                    results[i].setDeleteMinimun(minimum);
                }
            }
        }

        return results;
    }

    private String buildTestSummary(PerfResult[] results)
    {
        int columnLength = 12;
        int columnNumbers = 8;
        final String indent = "  ";

        StringBuffer buf = new StringBuffer();
        // table header
        buf.append(EOL);
        for (int i = 0; i < columnNumbers; i++)
        {
            buf.append(alignToLength(columnLength, "="));
        }
        buf.append(EOL);
        buf.append(alignToLength(columnLength, " "));
        String headline = "OJB PERFORMANCE TEST SUMMARY, " + new Date();
        buf.append(alignLeft(headline, columnLength));
        buf.append(EOL);
        for (int i = 0; i < columnNumbers; i++)
        {
            buf.append(alignToLength(columnLength, "-"));
        }
        buf.append(EOL);
        buf.append(indent).append(PerfMain.getConcurrentThreads());
        buf.append(" concurrent threads, handle ");
        buf.append(PerfMain.getIterationsPerThread());
        buf.append(" objects per thread");
        buf.append(EOL).append(indent).append(iterationsPerThread).append(" INSERT operations per test instance");
        buf.append(EOL).append(indent + "FETCH collection of ").append(iterationsPerThread).append(" objects per test instance");
        buf.append(EOL).append(indent + "Repeat FETCH collection of ").append(iterationsPerThread).append(" objects per test instance");
        int byIdCalls = (iterationsPerThread / PerfMain.BY_IDENTITY_FACTOR);
        buf.append(EOL).append(indent).append((byIdCalls == 0 ? 1: byIdCalls)).append(" get by Identity calls  per test instance");
        buf.append(EOL).append(indent).append(iterationsPerThread).append(" UPDATE operations per test instance");
        buf.append(EOL).append(indent).append(iterationsPerThread).append(" DELETE operations per test instance");
        buf.append(EOL).append(indent);
        buf.append("- ").append(!(isUseStressMode()) ? "performance mode" : "stress mode");
        buf.append(" - results per test instance (average)");
        buf.append(EOL);
        for (int i = 0; i < columnNumbers; i++)
        {
            buf.append(alignToLength(columnLength, "="));
        }
        buf.append(EOL);
        buf.append(alignLeft("API", columnLength));
        //buf.append(alignLeft("Period", columnLength, " "));
        //buf.append(alignLeft("Total", columnLength, " "));
        buf.append(alignLeft("Total", columnLength));

        buf.append(alignLeft("Insert", columnLength));
        //buf.append(alignToLength(columnLength, " "));
        buf.append(alignLeft("Fetch", columnLength));
        //buf.append(alignToLength(columnLength, " "));
        buf.append(alignLeft("Fetch 2", columnLength));
        //buf.append(alignToLength(columnLength, " "));
        buf.append(alignLeft("by Id", columnLength));
        //buf.append(alignToLength(columnLength, " "));
        buf.append(alignLeft("Update", columnLength));
        //buf.append(alignToLength(columnLength, " "));
        buf.append(alignLeft("Delete", columnLength));
        //buf.append(alignToLength(columnLength, " "));

        buf.append(EOL);
        buf.append(alignToLength(columnLength, " "));
        //buf.append(alignLeft("[sec]", columnLength, " "));
        //buf.append(alignLeft("[sec]", columnLength, " "));
        buf.append(alignLeft("[%]", columnLength));

        buf.append(alignLeft("[msec]", columnLength));
        //buf.append(alignToLength(columnLength, " "));
        buf.append(alignLeft("[msec]", columnLength));
        //buf.append(alignToLength(columnLength, " "));
        buf.append(alignLeft("[msec]", columnLength));
        //buf.append(alignToLength(columnLength, " "));
        buf.append(alignLeft("[msec]", columnLength));
        //buf.append(alignToLength(columnLength, " "));
        buf.append(alignLeft("[msec]", columnLength));
        //buf.append(alignToLength(columnLength, " "));
        buf.append(alignLeft("[msec]", columnLength));
        //buf.append(alignToLength(columnLength, " "));
        buf.append(EOL);
        for (int i = 0; i < columnNumbers; i++)
        {
            buf.append(alignToLength(columnLength, "-"));
        }

        // fill table
        int counter = 0;
        double calibrationMark = 0;
        for(int i = 0; i < results.length; i++)
        {
            PerfResult result = results[i];
            buf.append(EOL);
            if(counter == 0)
            {
                // the first one is the fastest
                calibrationMark = result.getTotalTime();
            }
            //double period = (double) result.getTestPeriod() / 1000;
            //double total = (double) Math.round((double) result.getTotalTime()) / 1000;
            long percent = Math.round((result.getTotalTime() / calibrationMark) * 100);

            buf.append(alignLeft(result.getTestName(), columnLength));
            //buf.append(alignLeft(""+period, columnLength, " "));
            //buf.append(alignLeft(""+total, columnLength, " "));
            buf.append(alignLeft(""+percent, columnLength));

            buf.append(alignLeft(result.getInsertResult()+result.getInsertResultPercent(), columnLength));
            //buf.append(alignLeft(result.getInsertResultPercent(), columnLength, " "));
            buf.append(alignLeft(result.getFetchResult()+result.getFetchResultPercent(), columnLength));
            //buf.append(alignLeft(result.getFetchResultPercent(), columnLength, " "));
            buf.append(alignLeft(result.getFetchSecondResult()+result.getFetchSecondResultPercent(), columnLength));
            //buf.append(alignLeft(result.getFetchSecondResultPercent(), columnLength, " "));
            buf.append(alignLeft(result.getByIdentityResult()+result.getByIdentityResultPercent(), columnLength));
            //buf.append(alignLeft(result.getByIdentityResultPercent(), columnLength, " "));
            buf.append(alignLeft(result.getUpdateResult()+result.getUpdateResultPercent(), columnLength));
            //buf.append(alignLeft(result.getUpdateResultPercent(), columnLength, " "));
            buf.append(alignLeft(result.getDeleteResult()+result.getDeleteResultPercent(), columnLength));
            //buf.append(alignLeft(result.getDeleteResultPercent(), columnLength, " "));
            counter++;
        }
        buf.append(EOL);
        for (int i = 0; i < columnNumbers; i++)
        {
            buf.append(alignToLength(columnLength, "="));
        }
//        if(failures.size() > 0)
//        {
//            buf.append(EOL + "Failures detected:" + EOL);
//            for(int i = 0; i < failures.size(); i++)
//            {
//                PerfResult perfResult = (PerfResult) failures.get(i);
//                buf.append("name=" + perfResult.getTestName() + ", isValid=" +perfResult.isValid() + EOL);
//            }
//        }
        return buf.toString();
    }

//    private String alignRight(String target, int length)
//    {
//        return alignToLength(target, length, " ", true);
//    }

    private String alignLeft(String target, int length)
    {
        return alignToLength(target, length, " ", false);
    }

    private String alignToLength(int length, String fillCharacter)
    {
        return alignToLength("", length, fillCharacter, false);
    }

    private String alignToLength(String target, int length, String fillCharacter, boolean right)
    {
        if (target.length() > length) return target;

        int count = length - target.length();
        String blanks = "";
        for (int i = 0; i < count; i++)
        {
            blanks += fillCharacter;
        }
        return right ? blanks + target : target + blanks;
    }

    /**
     * testTimes[0] startTime/test length
     * testTimes[1] inserting times
     * testTimes[2] fetching times
     * testTimes[3] fetching repeat times
     * testTimes[4] get by Identity times
     * testTimes[5] updating times
     * testTimes[6] deleting times
     */
    public synchronized void addPeriodResult(String testName, long[] resultArr)
    {
        PerfResult result = (PerfResult) resultMap.get(testName);
        if (result == null)
        {
            result = new PerfResult();
            result.setTestName(testName);
            result.setStressMode(isUseStressMode());
            result.setIterationsPerThread(getIterationsPerThread());
            result.setNumberOfThreads(getConcurrentThreads());
            result.setTestLoops(getTestLoops());
            resultMap.put(testName, result);

        }
        result.addTestPeriod(resultArr[TIME_TOTAL]);
        result.addInsertPeriod(resultArr[TIME_INSERT]);
        result.addFetchPeriod(resultArr[TIME_FETCH]);
        result.addFetchSecondPeriod(resultArr[TIME_FETCH_2]);
        result.addByIdentityPeriod(resultArr[TIME_BY_IDENTITY]);
        result.addUpdatePeriod(resultArr[TIME_UPDATE]);
        result.addDeletePeriod(resultArr[TIME_DELETE]);

        if(logAll)
        {
            StringBuffer buf = new StringBuffer();
            buf.append(" Test '").append(result.getTestName()).append("' [ms]")
                .append(": testPeriod=").append(resultArr[0]/getConcurrentThreads())
                .append(" insert=").append(resultArr[1]/getConcurrentThreads())
                .append(" read=").append(resultArr[2]/getConcurrentThreads())
                .append(" read2=").append(resultArr[3]/getConcurrentThreads())
                .append(" byIdentity=").append(resultArr[4]/getConcurrentThreads())
                .append(" update=").append(resultArr[5]/getConcurrentThreads())
                .append(" delete=").append(resultArr[6]/getConcurrentThreads());
            printer().print(buf.toString());
        }
        else
        {
            printer().print(".");
        }
    }

    public synchronized void addConsistentResult(String testName, int objectsBefore, int objectsAfter)
    {
        ConsistentEntry ce = new ConsistentEntry(objectsBefore, objectsAfter);
        PerfResult result = (PerfResult) resultMap.get(testName);
        if(objectsBefore != objectsAfter)
        {
            try
            {
                throw new Exception("Wrong object count, before=" + objectsBefore + ", after=" + objectsAfter);
            }
            catch(Exception e)
            {
                registerException(testName, e);
            }
        }
        result.addConsistentEntry(ce);
    }

    public synchronized void registerException(String causer, Exception e)
    {
        exceptionMap.put(causer, e);
    }

    public Map getExceptionMap()
    {
        return exceptionMap;
    }

    public Collection getResultList()
    {
        return resultMap.values();
    }

    public static int getIterationsPerThread()
    {
        return iterationsPerThread;
    }

    public static int getConcurrentThreads()
    {
        return concurrentThreads;
    }

    public static boolean isUseStressMode()
    {
        return useStressMode;
    }

    public static int getTestLoops()
    {
        return testLoops;
    }


    Object newInstance(Class target, Class argType, Object argInstance)
    {
        try
        {
            Class[] types = new Class[]{argType};
            Object[] args = new Object[]{argInstance};
            Constructor con = target.getConstructor(types);
            return con.newInstance(args);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("Can't create instance for class "
                    + target + "using constructor argument " + argType + ", message is " + e.getMessage());
        }
    }


    //================================================================
    // inner class
    //================================================================
    static class PerfResult
    {
        private String testName;
        private long testPeriod;
        private boolean stressMode;

        private int testLoops;
        private int numberOfThreads;
        private int iterationsPerThread;

        private long insertPeriod;
        private long insertMinimun;
        private long fetchPeriod;
        private long fetchMinimun;
        private long fetchSecondPeriod;
        private long fetchSecondMinimun;
        private long byIdentityPeriod;
        private long byIdentityMinimun;
        private long updatePeriod;
        private long updateMinimun;
        private long deletePeriod;
        private long deleteMinimun;

        private boolean valid;

        private List consistentList;

        public PerfResult()
        {
            setValid(true);
            this.consistentList = new ArrayList();
        }

        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            buf.append(EOL).append("[").append(this.getClass().getName());
            buf.append(EOL).append("testName=").append(testName);
            buf.append(EOL).append("testPeriod=").append(testPeriod);
            buf.append(EOL).append("testLoops=").append(testLoops);
            buf.append(EOL).append("numberOfThreads=").append(numberOfThreads);
            buf.append(EOL).append("iterationsPerThread=").append(iterationsPerThread);
            buf.append(EOL).append("isValid=").append(isValid());
            buf.append(EOL).append("insertPeriod=").append(getInsertPeriod());
            buf.append(EOL).append("fetchPeriod=").append(getFetchPeriod());
            buf.append(EOL).append("fetchSecondPeriod=").append(getFetchSecondPeriod());
            buf.append(EOL).append("byIdentity=").append(getByIdentityPeriod());
            buf.append(EOL).append("deletePeriod=").append(getDeletePeriod());
            buf.append(EOL).append("consistentList: ").append(consistentList);
            buf.append("]");
            return buf.toString();
        }

        public void addConsistentEntry(ConsistentEntry entry)
        {
            this.consistentList.add(entry);
            valid = valid && entry.isPassed();
        }

        public boolean isStressMode()
        {
            return stressMode;
        }

        public void setStressMode(boolean stressMode)
        {
            this.stressMode = stressMode;
        }

        public boolean isValid()
        {
            return valid;
        }

        public void setValid(boolean valid)
        {
            this.valid = valid;
        }

        public String getTestName()
        {
            return testName;
        }

        public void setTestName(String testName)
        {
            this.testName = testName;
        }

        public long getTestPeriod()
        {
            return testPeriod;
        }

        public synchronized void addTestPeriod(long aTestPeriod)
        {
            this.testPeriod += aTestPeriod;
        }

        public int getTestLoops()
        {
            return testLoops;
        }

        public void setTestLoops(int testLoops)
        {
            this.testLoops = testLoops;
        }

        public int getNumberOfThreads()
        {
            return numberOfThreads;
        }

        public void setNumberOfThreads(int numberOfThreads)
        {
            this.numberOfThreads = numberOfThreads;
        }

        public int getIterationsPerThread()
        {
            return iterationsPerThread;
        }

        public void setIterationsPerThread(int numberOfObjects)
        {
            this.iterationsPerThread = numberOfObjects;
        }

        public long getTotalTime()
        {
            long result = ((insertPeriod + fetchPeriod + updatePeriod + deletePeriod) / getTestLoops()) / getNumberOfThreads();
            return result > 0 ? result : 1;
        }

        public long getInsertPeriod()
        {
            return (insertPeriod / getTestLoops()) / getNumberOfThreads();
        }

        public synchronized void addInsertPeriod(long anInsertPeriod)
        {
            this.insertPeriod += anInsertPeriod;
        }

        public long getFetchPeriod()
        {
            return (fetchPeriod / getTestLoops()) / getNumberOfThreads();
        }

        public synchronized void addFetchPeriod(long aFetchPeriod)
        {
            this.fetchPeriod += aFetchPeriod;
        }

        public long getFetchSecondPeriod()
        {
            return (fetchSecondPeriod / getTestLoops()) / getNumberOfThreads();
        }

        public synchronized void addFetchSecondPeriod(long secondPeriod)
        {
            this.fetchSecondPeriod += secondPeriod;
        }

        public long getByIdentityPeriod()
        {
            return (byIdentityPeriod / getTestLoops()) / getNumberOfThreads();
        }

        public synchronized void addByIdentityPeriod(long byIdentityPeriod)
        {
            this.byIdentityPeriod += byIdentityPeriod;
        }

        public long getUpdatePeriod()
        {
            return (updatePeriod / getTestLoops()) / getNumberOfThreads();
        }

        public synchronized void addUpdatePeriod(long aUpdatePeriod)
        {
            this.updatePeriod += aUpdatePeriod;
        }

        public long getDeletePeriod()
        {
            return (deletePeriod / getTestLoops()) / getNumberOfThreads();
        }

        public synchronized void addDeletePeriod(long aDeletePeriod)
        {
            this.deletePeriod += aDeletePeriod;
        }



        public void setInsertMinimun(long insertMinimun)
        {
            this.insertMinimun = insertMinimun > 1 ? insertMinimun : 1;
        }

        public void setFetchMinimun(long fetchMinimun)
        {
            this.fetchMinimun = fetchMinimun > 1 ? fetchMinimun : 1;
        }

        public void setFetchSecondMinimun(long fetchSecondMinimun)
        {
            this.fetchSecondMinimun = fetchSecondMinimun > 1 ? fetchSecondMinimun : 1;
        }

        public void setByIdentityMinimun(long byIdentityMinimun)
        {
            this.byIdentityMinimun = byIdentityMinimun > 1 ? byIdentityMinimun : 1;
        }

        public void setUpdateMinimun(long updateMinimun)
        {
            this.updateMinimun = updateMinimun > 1 ? updateMinimun : 1;
        }

        public void setDeleteMinimun(long deleteMinimun)
        {
            this.deleteMinimun = deleteMinimun > 1 ? deleteMinimun : 1;
        }



        public String getInsertResult()
        {
            long result = getInsertPeriod();
            return "" + result;
        }

        public String getFetchResult()
        {
            long result = getFetchPeriod();
            return "" + result;
        }

        public String getFetchSecondResult()
        {
            long result = getFetchSecondPeriod();
            return "" + result;
        }

        public String getByIdentityResult()
        {
            long result = getByIdentityPeriod();
            return "" + result;
        }

        public String getUpdateResult()
        {
            long result = getUpdatePeriod();
            return "" + result;
        }

        public String getDeleteResult()
        {
            long result = getDeletePeriod();
            return "" + result;
        }




        public String getInsertResultPercent()
        {
            long result = getInsertPeriod();
            return "(" + (int) ((result * 100)/insertMinimun) + "%)";
        }

        public String getFetchResultPercent()
        {
            long result = getFetchPeriod();
            return "(" + (int) ((result * 100)/fetchMinimun) + "%)";
        }

        public String getFetchSecondResultPercent()
        {
            long result = getFetchSecondPeriod();
            return "(" + (int) ((result * 100)/fetchSecondMinimun) + "%)";
        }

        public String getByIdentityResultPercent()
        {
            long result = getByIdentityPeriod();
            return  "(" + (int) ((result * 100)/byIdentityMinimun) + "%)";
        }

        public String getUpdateResultPercent()
        {
            long result = getUpdatePeriod();
            return "(" + (int) ((result * 100)/updateMinimun) + "%)";
        }

        public String getDeleteResultPercent()
        {
            long result = getDeletePeriod();
            return "(" + (int) ((result * 100)/deleteMinimun) + "%)";
        }
    }

    //================================================================
    // inner class
    //================================================================
    static class ConsistentEntry
    {
        private int objectsBefore;
        private int objectsAfter;

        public ConsistentEntry(int objectsBefore, int objectsAfter)
        {
            this.objectsBefore = objectsBefore;
            this.objectsAfter = objectsAfter;
        }

        public int getObjectsBefore()
        {
            return objectsBefore;
        }

        public int getObjectsAfter()
        {
            return objectsAfter;
        }

        public boolean isPassed()
        {
            return objectsBefore == objectsAfter;
        }

        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            buf.append("[").append(this.getClass().getName())
                    .append(": objectsBefore=")
                    .append(getObjectsBefore())
                    .append(" objectsAfter=")
                    .append(objectsAfter)
                    .append(" isPassed=")
                    .append(isPassed());
            return buf.toString();
        }
    }

    static class Printer
    {
        PrintStream console;
        PrintStream file;

        public Printer()
        {
            console = System.out;
            try
            {
                file = new PrintStream(new FileOutputStream(new File("OJB-Performance-Result.txt")));
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }

        void print(String str)
        {
            console.print(str);
            if(file != null) file.print(str);
        }

        void print(String str, boolean consoleOnly)
        {
            console.print(str);
            if(file != null && !consoleOnly) file.print(str);
        }

        void println(String str)
        {
            console.println(str);
            if(file != null) file.println(str);
        }

        void println()
        {
            print("");
        }
    }
}
