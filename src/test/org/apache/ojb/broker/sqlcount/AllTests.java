package org.apache.ojb.broker.sqlcount;

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

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * The facade to all TestCases in this package.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: AllTests.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class AllTests
{
    /**
     * runs the suite in a junit.textui.TestRunner.
     */
    public static void main(String[] args)
    {
        String[] arr = {AllTests.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /** build a TestSuite from all the TestCases in this package*/
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(CollectionCountTest.class));
        suite.addTest(new TestSuite(SimpleCountTest.class));
        suite.addTest(new TestSuite(EmptyCacheCountTest.class));
        return suite;
    }
}
