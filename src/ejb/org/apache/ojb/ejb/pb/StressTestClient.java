package org.apache.ojb.ejb.pb;

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

import junit.framework.TestCase;
import org.apache.commons.lang.SystemUtils;

/**
 *
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: StressTestClient.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class StressTestClient extends TestCase
{
    public StressTestClient(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        String[] arr = {StressTestClient.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testStressPB() throws Exception
    {
        int loops = 2;
        int concurrentThreads = 10;
        int objectsPerThread = 200;
        String eol = SystemUtils.LINE_SEPARATOR;
        for (int i = 0; i < loops; i++)
        {
            System.out.println(eol + "##  perform loop " + (i + 1) + "   ##");
            StressTest.performTest(new int[]{concurrentThreads, objectsPerThread});
        }
    }
}
