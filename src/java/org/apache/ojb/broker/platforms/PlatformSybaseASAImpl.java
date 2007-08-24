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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class is a concrete implementation of <code>Platform</code>.
 * Provides an implementation for Sybase ASA.
 * <br/>
 * This class extends {@link PlatformSybaseImpl} and defines specific
 * behavior for the Sybase ASA platform.
 *
 * <br/>NOTE: Different than the Sybase ASE platform
 * <br/> Modified by Nicus for Sybase ASA
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird<a>
 * @version $Id: PlatformSybaseASAImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PlatformSybaseASAImpl extends PlatformSybaseImpl
{
    /**
     * Sybase Adaptive Server Enterprise (ASE) support timestamp to a precision of 1/300th of second.
     * Adaptive Server Anywhere (ASA) support timestamp to a precision of 1/1000000tho of second.
     * Sybase JDBC driver (JConnect) retrieving timestamp always rounds to 1/300th sec., causing rounding
     * problems with ASA when retrieving Timestamp fields.
     * This work around was suggested by Sybase Support. Unfortunately it works only with ASA.
     * <br/>
     * author Lorenzo Nicora
     */
    public void initializeJdbcConnection(JdbcConnectionDescriptor jcd, Connection conn) throws PlatformException
    {
        // Do origial init
        super.initializeJdbcConnection(jcd, conn);
        // Execute a statement setting the tempory option
        try
        {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("set temporary option RETURN_DATE_TIME_AS_STRING = On");
        }
        catch (SQLException e)
        {
            throw new PlatformException(e);
        }
    }

}
