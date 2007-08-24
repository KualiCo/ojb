package xdoclet.modules.ojb.tests;

/* Copyright 2003-2005 The Apache Software Foundation
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
 * Test class used in some of the unit tests.
 */
public class TestClass
{
    /**
     * Returns the short name of this class.
     * 
     * @return The short name
     */
    public static String getShortName()
    {
        String longName = TestClass.class.getName();

        return longName.substring(longName.lastIndexOf('.') + 1);
    }

    /**
     * Used as an initialization method in a unit test.
     * 
     * @return Some string
     */
    private String initMethod1()
    {
        return null;
    }

    /**
     * Used as an initialization method in a unit test.
     * 
     * @param arg Some string
     */
    public void initMethod2(String arg)
    {}

    /**
     * Used as an initialization method in a unit test.
     */
    public static void initMethod3()
    {}
}
