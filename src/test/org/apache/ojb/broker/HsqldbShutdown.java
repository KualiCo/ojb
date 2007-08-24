package org.apache.ojb.broker;

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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.ojb.broker.platforms.Platform;
import org.apache.ojb.broker.platforms.PlatformHsqldbImpl;
import org.apache.ojb.junit.PBTestCase;

/**
 * Performs a shutdown of hsqldb.
 *
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: HsqldbShutdown.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class HsqldbShutdown extends PBTestCase
{
    public void testHsqldbShutdown()
    {
        Platform platform = broker.serviceConnectionManager().getSupportedPlatform();

        if(platform instanceof PlatformHsqldbImpl)
        {
            Connection con = null;
            Statement stmt = null;
            
            try
            {
                con = broker.serviceConnectionManager().getConnection();
                stmt = con.createStatement();
                stmt.execute("shutdown");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    stmt.close();
                    con.close();
                }
                catch (SQLException e1)
                {
                    e1.printStackTrace();
                }
            }
        }

    }
}
