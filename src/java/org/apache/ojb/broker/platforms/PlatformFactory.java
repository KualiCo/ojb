package org.apache.ojb.broker.platforms;

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

import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.ClassHelper;

import java.util.HashMap;

/**
 * this factory class is responsible to create Platform objects that
 * define RDBMS platform specific behaviour.
 * @version 	1.0
 * @author		Thomas Mahler
 */
public class PlatformFactory
{
    private static HashMap platforms = new HashMap();

    /**
     * returns the Platform matching to the JdbcConnectionDescriptor jcd.
     * The method jcd.getDbms(...) is used to determine the Name of the
     * platform.
     * BRJ : cache platforms
     * @param jcd the JdbcConnectionDescriptor defining the platform
     */
    public static Platform getPlatformFor(JdbcConnectionDescriptor jcd)
    {
        String dbms = jcd.getDbms();
        Platform result = null;
        String platformName = null;

        result = (Platform) getPlatforms().get(dbms);
        if (result == null)
        {
            try
            {
                platformName = getClassnameFor(dbms);
                Class platformClass = ClassHelper.getClass(platformName);
                result = (Platform) platformClass.newInstance();

            }
            catch (Throwable t)
            {
                LoggerFactory.getDefaultLogger().warn(
                        "[PlatformFactory] problems with platform " + platformName, t);
                LoggerFactory.getDefaultLogger().warn(
                        "[PlatformFactory] OJB will use PlatformDefaultImpl instead");

                result = new PlatformDefaultImpl();
            }
            getPlatforms().put(dbms, result); // cache the Platform
        }
        return result;
    }

    /**
     * compute the name of the concrete Class representing the Platform
     * specified by <code>platform</code>
     * @param platform the name of the platform as specified in the repository
     */
    private static String getClassnameFor(String platform)
    {
        String pf = "Default";
        if (platform != null)
        {
            pf = platform;
        }
        return "org.apache.ojb.broker.platforms.Platform" + pf.substring(0, 1).toUpperCase() + pf.substring(1) + "Impl";
    }

    /**
     * Gets the platforms.
     * @return Returns a HashMap
     */
    private static HashMap getPlatforms()
    {
        return platforms;
    }
}
