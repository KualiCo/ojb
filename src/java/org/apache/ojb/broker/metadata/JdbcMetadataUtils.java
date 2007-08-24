package org.apache.ojb.broker.metadata;

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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;

import javax.sql.DataSource;
import org.apache.commons.beanutils.PropertyUtils;

/**
 * This class provides some utility functions to OJB for working with JDBC metadata.
 * 
 * @author <a href="mailto:tomdz@apache.org">Thomas Dudziak</a>
 */
public class JdbcMetadataUtils
{
    /** The name of the property returned by the {@link #splitConnectionUrl(String)} method
        that contains the protocol */ 
    public static final String PROPERTY_PROTOCOL    = "protocol";
    /** The name of the property returned by the {@link #splitConnectionUrl(String)} method
        that contains the sub protocol */ 
    public static final String PROPERTY_SUBPROTOCOL = "subprotocol";
    /** The name of the property returned by the {@link #splitConnectionUrl(String)} method
        that contains the database alias (the actual database url) */ 
    public static final String PROPERTY_DBALIAS     = "dbAlias";

    /** Identifier for the DB2 platform */
    public static final String PLATFORM_DB2         = "Db2";
    /** Identifier for the Firebird platform */
    public static final String PLATFORM_FIREBIRD    = "Firebird";
    /** Identifier for the Hsqldb platform */
    public static final String PLATFORM_HSQLDB      = "Hsqldb";
    /** Identifier for the Informix platform */
    public static final String PLATFORM_INFORMIX    = "Informix";
    /** Identifier for the MaxDB platform */
    public static final String PLATFORM_MAXDB       = "MaxDB";
    /** Identifier for the McKoi platform */
    public static final String PLATFORM_MCKOI       = "McKoi";
    /** Identifier for the MsAccess platform */
    public static final String PLATFORM_MSACCESS    = "MsAccess";
    /** Identifier for the Microsoft SQL Server platform */
    public static final String PLATFORM_MSSQLSERVER = "MsSQLServer";
    /** Identifier for the MySQL platform */
    public static final String PLATFORM_MYSQL       = "MySQL";
    /** Identifier for the generic Oracle platform */
    public static final String PLATFORM_ORACLE      = "Oracle";
    /** Identifier for the Oracle9i platform */
    public static final String PLATFORM_ORACLE9I    = "Oracle9i";
    /** Identifier for the PostgresSQL platform */
    public static final String PLATFORM_POSTGRESQL  = "PostgreSQL";
    /** Identifier for the generic Sybase platform */
    public static final String PLATFORM_SYBASE      = "Sybase";
    /** Identifier for the Sybase ASA platform */
    public static final String PLATFORM_SYBASEASA   = "SybaseASA";
    /** Identifier for the Sybase ASE platform */
    public static final String PLATFORM_SYBASEASE   = "SybaseASE";
    /** Identifier for the Oracle9i for WebLogic platform */
    public static final String PLATFORM_WLORACLE9I  = "WLOracle9i";

    /** The standard DB2 jdbc driver */
    public static final String DRIVER_DB2                     = "COM.ibm.db2.jdbc.app.DB2Driver";
    /** The i-net DB2 jdbc driver */
    public static final String DRIVER_DB2_INET                = "com.inet.drda.DRDADriver";
    /** The standard Firebird jdbc driver */
    public static final String DRIVER_FIREBIRD                = "org.firebirdsql.jdbc.FBDriver";
    /** The standard Hsqldb jdbc driver */
    public static final String DRIVER_HSQLDB                  = "org.hsqldb.jdbcDriver";
    /** The i-net pooled jdbc driver for SQLServer and Sybase */
    public static final String DRIVER_INET_POOLED             = "com.inet.pool.PoolDriver";
    /** The standard Informix jdbc driver */
    public static final String DRIVER_INFORMIX                = "com.informix.jdbc.IfxDriver";
    /** The jTDS jdbc driver for SQLServer and Sybase */
    public static final String DRIVER_JTDS                    = "net.sourceforge.jtds.jdbc.Driver";
    /** The standard MaxDB jdbc driver */
    public static final String DRIVER_MAXDB                   = "com.sap.dbtech.jdbc.DriverSapDB";
    /** The standard McKoi jdbc driver */
    public static final String DRIVER_MCKOI                   = "com.mckoi.JDBCDriver";
    /** The standard SQLServer jdbc driver */
    public static final String DRIVER_MSSQLSERVER             = "com.microsoft.jdbc.sqlserver.SQLServerDriver";
    /** The i-net SQLServer jdbc driver */
    public static final String DRIVER_MSSQLSERVER_INET        = "com.inet.tds.TdsDriver";
    /** The JNetDirect SQLServer jdbc driver */
    public static final String DRIVER_MSSQLSERVER_JSQLCONNECT = "com.jnetdirect.jsql.JSQLDriver";
    /** The standard MySQL jdbc driver */
    public static final String DRIVER_MYSQL                   = "com.mysql.jdbc.Driver";
    /** The old MySQL jdbc driver */
    public static final String DRIVER_MYSQL_OLD               = "org.gjt.mm.mysql.Driver";
    /** The standard Oracle jdbc driver */
    public static final String DRIVER_ORACLE                  = "oracle.jdbc.driver.OracleDriver";
    /** The i-net Oracle jdbc driver */
    public static final String DRIVER_ORACLE_INET             = "com.inet.ora.OraDriver";
    /** The standard PostgreSQL jdbc driver */
    public static final String DRIVER_POSTGRESQL              = "org.postgresql.Driver";
    /** The standard Sapdb jdbc driver */
    public static final String DRIVER_SAPDB                   = DRIVER_MAXDB;
    /** The standard Sybase jdbc driver */
    public static final String DRIVER_SYBASE                  = "com.sybase.jdbc2.jdbc.SybDriver";
    /** The old Sybase jdbc driver */
    public static final String DRIVER_SYBASE_OLD              = "com.sybase.jdbc.SybDriver";
    /** The i-net Sybase jdbc driver */
    public static final String DRIVER_SYBASE_INET             = "com.inet.syb.SybDriver";

    /** The subprotocol used by the standard DB2 driver */
    public static final String SUBPROTOCOL_DB2                       = "db2";
    /** The subprotocol used by the i-net DB2 driver */
    public static final String SUBPROTOCOL_DB2_INET                  = "inetdb2";
    /** The subprotocol used by the standard Firebird driver */
    public static final String SUBPROTOCOL_FIREBIRD                  = "firebirdsql";
    /** The subprotocol used by the standard Hsqldb driver */
    public static final String SUBPROTOCOL_HSQLDB                    = "hsqldb";
    /** The subprotocol used by the standard Informix driver */
    public static final String SUBPROTOCOL_INFORMIX                  = "informix-sqli";
    /** The subprotocol used by the standard MaxDB driver */
    public static final String SUBPROTOCOL_MAXDB                     = "sapdb";
    /** The subprotocol used by the standard McKoi driver */
    public static final String SUBPROTOCOL_MCKOI                     = "mckoi";
    /** The subprotocol used by the standard SQLServer driver */
    public static final String SUBPROTOCOL_MSSQLSERVER               = "microsoft:sqlserver";
    /** A subprotocol used by the i-net SQLServer driver */
    public static final String SUBPROTOCOL_MSSQLSERVER_INET          = "inetdae";
    /** A subprotocol used by the i-net SQLServer driver */
    public static final String SUBPROTOCOL_MSSQLSERVER6_INET         = "inetdae6";
    /** A subprotocol used by the i-net SQLServer driver */
    public static final String SUBPROTOCOL_MSSQLSERVER7_INET         = "inetdae7";
    /** A subprotocol used by the i-net SQLServer driver */
    public static final String SUBPROTOCOL_MSSQLSERVER7A_INET        = "inetdae7a";
    /** A subprotocol used by the pooled i-net SQLServer driver */
    public static final String SUBPROTOCOL_MSSQLSERVER_INET_POOLED   = "inetpool:inetdae";
    /** A subprotocol used by the pooled i-net SQLServer driver */
    public static final String SUBPROTOCOL_MSSQLSERVER6_INET_POOLED  = "inetpool:inetdae6";
    /** A subprotocol used by the pooled i-net SQLServer driver */
    public static final String SUBPROTOCOL_MSSQLSERVER7_INET_POOLED  = "inetpool:inetdae7";
    /** A subprotocol used by the pooled i-net SQLServer driver */
    public static final String SUBPROTOCOL_MSSQLSERVER7A_INET_POOLED = "inetpool:inetdae7a";
    /** The subprotocol used by the JNetDirect SQLServer driver */
    public static final String SUBPROTOCOL_MSSQLSERVER_JSQLCONNECT   = "JSQLConnect";
    /** The subprotocol used by the jTDS SQLServer driver */
    public static final String SUBPROTOCOL_MSSQLSERVER_JTDS          = "jtds:sqlserver";
    /** The subprotocol used by the standard MySQL driver */
    public static final String SUBPROTOCOL_MYSQL                     = "mysql";
    /** The subprotocol used by the standard Oracle driver */
    public static final String SUBPROTOCOL_ORACLE                    = "oracle";
    /** The subprotocol used by the i-net Oracle driver */
    public static final String SUBPROTOCOL_ORACLE_INET               = "inetora";
    /** The subprotocol used by the standard PostgreSQL driver */
    public static final String SUBPROTOCOL_POSTGRESQL                = "postgresql";
    /** The subprotocol used by the standard Sapdb driver */
    public static final String SUBPROTOCOL_SAPDB                     = SUBPROTOCOL_MAXDB;
    /** The subprotocol used by the standard Sybase driver */
    public static final String SUBPROTOCOL_SYBASE                    = "sybase:Tds";
    /** The subprotocol used by the i-net Sybase driver */
    public static final String SUBPROTOCOL_SYBASE_INET               = "inetsyb";
    /** The subprotocol used by the pooled i-net Sybase driver */
    public static final String SUBPROTOCOL_SYBASE_INET_POOLED        = "inetpool:inetsyb";
    /** The subprotocol used by the jTDS Sybase driver */
    public static final String SUBPROTOCOL_SYBASE_JTDS               = "jtds:sybase";
    
    
    /** Maps the sub-protocl part of a jdbc connection url to a OJB platform name */
    private HashMap jdbcSubProtocolToPlatform = new HashMap();
    /** Maps the jdbc driver name to a OJB platform name */
    private HashMap jdbcDriverToPlatform      = new HashMap();

    /**
     * Creates a new <code>JdbcMetadataUtils</code> object.
     */
    public JdbcMetadataUtils()
    {
        // Note that currently Sapdb and MaxDB have equal subprotocols and
        // drivers so we have no means to distinguish them
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_DB2,                       PLATFORM_DB2);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_DB2_INET,                  PLATFORM_DB2);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_FIREBIRD,                  PLATFORM_FIREBIRD);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_HSQLDB,                    PLATFORM_HSQLDB);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_INFORMIX,                  PLATFORM_INFORMIX);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_MAXDB,                     PLATFORM_MAXDB);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_MSSQLSERVER,               PLATFORM_MSSQLSERVER);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_MSSQLSERVER_INET,          PLATFORM_MSSQLSERVER);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_MSSQLSERVER6_INET,         PLATFORM_MSSQLSERVER);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_MSSQLSERVER7_INET,         PLATFORM_MSSQLSERVER);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_MSSQLSERVER7A_INET,        PLATFORM_MSSQLSERVER);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_MSSQLSERVER_INET_POOLED,   PLATFORM_MSSQLSERVER);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_MSSQLSERVER6_INET_POOLED,  PLATFORM_MSSQLSERVER);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_MSSQLSERVER7_INET_POOLED,  PLATFORM_MSSQLSERVER);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_MSSQLSERVER7A_INET_POOLED, PLATFORM_MSSQLSERVER);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_MSSQLSERVER_JTDS,          PLATFORM_MSSQLSERVER);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_MYSQL,                     PLATFORM_MYSQL);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_ORACLE,                    PLATFORM_ORACLE);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_ORACLE_INET,               PLATFORM_ORACLE);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_POSTGRESQL,                PLATFORM_POSTGRESQL);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_SYBASE,                    PLATFORM_SYBASE);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_SYBASE_INET,               PLATFORM_SYBASE);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_SYBASE_INET_POOLED,        PLATFORM_SYBASE);
        jdbcSubProtocolToPlatform.put(SUBPROTOCOL_SYBASE_JTDS,               PLATFORM_SYBASE);

        jdbcDriverToPlatform.put(DRIVER_DB2,                     PLATFORM_DB2);
        jdbcDriverToPlatform.put(DRIVER_DB2_INET,                PLATFORM_DB2);
        jdbcDriverToPlatform.put(DRIVER_FIREBIRD,                PLATFORM_FIREBIRD);
        jdbcDriverToPlatform.put(DRIVER_HSQLDB,                  PLATFORM_HSQLDB);
        jdbcDriverToPlatform.put(DRIVER_INFORMIX,                PLATFORM_INFORMIX);
        jdbcDriverToPlatform.put(DRIVER_MAXDB,                   PLATFORM_MAXDB);
        jdbcDriverToPlatform.put(DRIVER_MCKOI,                   PLATFORM_MCKOI);
        jdbcDriverToPlatform.put(DRIVER_MSSQLSERVER,             PLATFORM_MSSQLSERVER);
        jdbcDriverToPlatform.put(DRIVER_MSSQLSERVER_INET,        PLATFORM_MSSQLSERVER);
        jdbcDriverToPlatform.put(DRIVER_MSSQLSERVER_JSQLCONNECT, PLATFORM_MSSQLSERVER);
        jdbcDriverToPlatform.put(DRIVER_MYSQL,                   PLATFORM_MYSQL);
        jdbcDriverToPlatform.put(DRIVER_MYSQL_OLD,               PLATFORM_MYSQL);
        jdbcDriverToPlatform.put(DRIVER_ORACLE,                  PLATFORM_ORACLE);
        jdbcDriverToPlatform.put(DRIVER_ORACLE_INET,             PLATFORM_ORACLE);
        jdbcDriverToPlatform.put(DRIVER_POSTGRESQL,              PLATFORM_POSTGRESQL);
        jdbcDriverToPlatform.put(DRIVER_SYBASE,                  PLATFORM_SYBASE);
        jdbcDriverToPlatform.put(DRIVER_SYBASE_OLD,              PLATFORM_SYBASE);
        jdbcDriverToPlatform.put(DRIVER_SYBASE_INET,             PLATFORM_SYBASE);
    }

    /**
     * Fills parameters of the given {@link JdbcConnectionDescriptor} with metadata
     * extracted from the given datasource.
     * 
     * @param jcd        The jdbc connection descriptor to fill
     * @param dataSource The data source
     * @param username   The username required to establish a connection via the data source
     *                   Can be empty if the data source does not require it or if one
     *                   is specified in the jdbc connection descriptor
     * @param password   The username required to establish a connection via the data source
     *                   Can be empty if the data source or username does not require it or if one
     *                   is specified in the jdbc connection descriptor
     */
    public void fillJCDFromDataSource(JdbcConnectionDescriptor jcd, DataSource dataSource, String username, String password) throws MetadataException
    {
        String           realUsername = (jcd.getUserName() != null ? jcd.getUserName() : username);
        String           realPassword = (jcd.getPassWord() != null ? jcd.getPassWord() : password);
        Connection       connection   = null;
        DatabaseMetaData metadata     = null;

        try
        {
            // we have to open a connection to be able to retrieve metadata
            if (realUsername != null)
            {
                connection = dataSource.getConnection(realUsername, realPassword);
            }
            else
            {
                connection = dataSource.getConnection();
            }

            metadata = connection.getMetaData();
        }
        catch (Throwable t)
        {
            if (connection != null)
            {
                try
                {
                    connection.close();
                }
                catch (SQLException ex)
                {}
            }
            throw new MetadataException("Could not get the metadata from the given datasource", t);
        }

        try
        {
            HashMap urlComponents = parseConnectionUrl(metadata.getURL());
    
            if (urlComponents.containsKey(PROPERTY_DBALIAS))
            {
                jcd.setProtocol((String)urlComponents.get(PROPERTY_PROTOCOL));
                jcd.setSubProtocol((String)urlComponents.get(PROPERTY_SUBPROTOCOL));
                jcd.setDbAlias((String)urlComponents.get(PROPERTY_DBALIAS));
                if (jdbcSubProtocolToPlatform.containsKey(jcd.getSubProtocol()))
                {
                    // TODO: We might be able to use this: metadata.getDatabaseProductName();
                    jcd.setDbms((String)jdbcSubProtocolToPlatform.get(jcd.getSubProtocol()));
                }
            }
        }
        catch (Throwable t)
        {
            try
            {
                connection.close();
            }
            catch (SQLException ex)
            {}
            throw new MetadataException("Could not get the metadata from the given datasource", t);
        }
        try
        {
            // this will only work with JDK >= 1.4 and only with some jdbc drivers
            Integer majorVersion = (Integer)PropertyUtils.getProperty(metadata, "JDBCMajorVersion");
            Integer minorVersion = (Integer)PropertyUtils.getProperty(metadata, "JDBCMinorVersion");

            jcd.setJdbcLevel(Double.parseDouble(majorVersion.toString()+"."+minorVersion.toString()));
        }
        catch (Throwable t)
        {
            // otherwise we're assuming JDBC 2.0 compliance
            jcd.setJdbcLevel(2.0);
        }
        try
        {
            connection.close();
        }
        catch (SQLException ex)
        {}
    }
    
    /**
     * Splits the given jdbc connection url into its components and puts them into
     * a hash map using the <code>PROPERTY_</code> constants.
     * 
     * @param jdbcConnectionUrl The connection url
     * @return The properties
     */
    public HashMap parseConnectionUrl(String jdbcConnectionUrl)
    {
        HashMap result = new HashMap();

        if (jdbcConnectionUrl == null)
        {
            return result;
        }

        int pos = jdbcConnectionUrl.indexOf(':');
        int lastPos;

        result.put(PROPERTY_PROTOCOL, jdbcConnectionUrl.substring(0, pos));

        lastPos = pos;
        pos     = jdbcConnectionUrl.indexOf(':', lastPos + 1);

        String subProtocol = jdbcConnectionUrl.substring(lastPos + 1, pos);

        // there are a few jdbc drivers that have a subprotocol containing one or more ':'
        if ("inetpool".equals(subProtocol))
        {
            // Possible forms are:
            //   inetpool:<subprotocol>
            //   inetpool:jdbc:<subprotocol>   (where we'll remove the 'jdbc' part)
            
            int tmpPos = jdbcConnectionUrl.indexOf(':', pos + 1);

            if ("inetpool:jdbc".equals(jdbcConnectionUrl.substring(lastPos + 1, tmpPos)))
            {
                pos    = tmpPos;
                tmpPos = jdbcConnectionUrl.indexOf(':', pos + 1);
            }
            subProtocol += ":" + jdbcConnectionUrl.substring(pos + 1, tmpPos);
        }
        else if ("jtds".equals(subProtocol) ||
                 "microsoft".equals(subProtocol) ||
                 "sybase".equals(subProtocol))
        {
            pos         = jdbcConnectionUrl.indexOf(':', pos + 1);
            subProtocol = ":" + jdbcConnectionUrl.substring(lastPos + 1, pos);
        }

        result.put(PROPERTY_SUBPROTOCOL, subProtocol);
        result.put(PROPERTY_DBALIAS, jdbcConnectionUrl.substring(pos + 1));

        return result;
    }

    /**
     * Derives the OJB platform to use for a database that is connected via a url using the specified
     * subprotocol, and where the specified jdbc driver is used.
     * 
     * @param jdbcSubProtocol The JDBC subprotocol used to connect to the database
     * @param jdbcDriver      The JDBC driver used to connect to the database
     * @return The platform identifier or <code>null</code> if no platform could be found
     */
    public String findPlatformFor(String jdbcSubProtocol, String jdbcDriver)
    {
        String platform = (String)jdbcSubProtocolToPlatform.get(jdbcSubProtocol);

        if (platform == null)
        {
            platform = (String)jdbcDriverToPlatform.get(jdbcDriver);
        }
        return platform;
    }
}
