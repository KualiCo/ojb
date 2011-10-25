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

import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.sequence.SequenceManagerHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Properties;

/**
 * This class is a concrete implementation of <code>Platform</code>. Provides an implementation
 * that works around some issues with Oracle in general and Oracle's Thin driver in particular.
 *
 * <p/>
 * Many of the database sequence specific properties can be specified using
 * <em>custom attributes</em> within the <em>sequence-manager</em> element.
 * <br/>
 * The database sequence specific properties are generally speaking, see database user guide
 * for detailed description.
 *
 * <p>
 * Implementation configuration properties:
 * </p>
 *
 * <table cellspacing="2" cellpadding="2" border="3" frame="box">
 * <tr>
 *     <td><strong>Property Key</strong></td>
 *     <td><strong>Property Values</strong></td>
 * </tr>
 * <tr>
 *     <td>sequenceStart</td>
 *     <td>
 *          DEPRECATED. Database sequence specific property.<br/>
 *          Specifies the first sequence number to be
 *          generated. Allowed: <em>1</em> or greater.
 *    </td>
 * </tr>
 * <tr>
 *     <td>seq.start</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          Specifies the first sequence number to be
 *          generated. Allowed: <em>1</em> or greater.
 *    </td>
 * </tr>
 * <tr>
 *     <td>seq.incrementBy</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          Specifies the interval between sequence numbers.
 *          This value can be any positive or negative
 *          integer, but it cannot be 0.
 *    </td>
 * </tr>
 * <tr>
 *     <td>seq.maxValue</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          Set max value for sequence numbers.
 *    </td>
 * </tr>
 * <tr>
 *     <td>seq.minValue</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          Set min value for sequence numbers.
 *    </td>
 * </tr>
 * <tr>
 *     <td>seq.cycle</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          If <em>true</em>, specifies that the sequence continues to generate
 *          values after reaching either its maximum or minimum value.
 *          <br/>
 *          If <em>false</em>, specifies that the sequence cannot generate more values after
 *          reaching its maximum or minimum value.
 *    </td>
 * </tr>
 * <tr>
 *     <td>seq.cache</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          Specifies how many values of the sequence Oracle
 *          preallocates and keeps in memory for faster access.
 *          Allowed values: <em>2</em> or greater. If set <em>0</em>,
 *          an explicite <em>nocache</em> expression will be set.
 *    </td>
 * </tr>
 * <tr>
 *     <td>seq.order</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          If set <em>true</em>, guarantees that sequence numbers
 *          are generated in order of request.
 *          <br/>
 *          If <em>false</em>, a <em>no order</em> expression will be set.
 *    </td>
 * </tr>
 * </table>
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler <a>
 * @version $Id: PlatformOracleImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */

public class PlatformOracleImpl extends PlatformDefaultImpl
{
    protected static final String THIN_URL_PREFIX = "jdbc:oracle:thin";
    // Oracle:thin handles direct BLOB insert <= 4000 and update <= 2000
    protected static final int THIN_BLOB_MAX_SIZE = 2000;
    // Oracle:thin handles direct CLOB insert and update <= 4000
    protected static final int THIN_CLOB_MAX_SIZE = 4000;

    /**
     * Field value of <code>oracle.jdbc.OracleTypes.CURSOR</code>.
     * @see #initOracleReflectedVars
     */
    protected static int ORACLE_JDBC_TYPE_CURSOR = -10;

    private Logger logger = LoggerFactory.getLogger(PlatformOracleImpl.class);

    /**
     * Default constructor.
     */
    public PlatformOracleImpl()
    {
        initOracleReflectedVars();
    }

    /**
     * Method prepareNextValProcedureStatement implementation 
     * is simply copied over from PlatformMsSQLServerImpl class.
     * @see org.apache.ojb.broker.platforms.Platform#prepareNextValProcedureStatement(java.sql.Connection, java.lang.String, java.lang.String)
     */
    public CallableStatement prepareNextValProcedureStatement(Connection con, String procedureName, String sequenceName)
            throws PlatformException
    {
        try
        {
            String sp = "{?= call " + procedureName + " (?)}";
            CallableStatement cs = con.prepareCall(sp);
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2, sequenceName);
            return cs;
        }
        catch (SQLException e)
        {
            throw new PlatformException(e);
        }
    }

    /**
     * In Oracle we set escape processing explizit 'true' after a statement was created.
     */
    public void afterStatementCreate(Statement stmt) throws PlatformException
    {
        try
        {
            stmt.setEscapeProcessing(true);
        }
        catch (SQLException e)
        {
            throw new PlatformException("Could not set escape processing", e);
        }
    }

    /**
     * For objects beyond 4k, weird things happen in Oracle if you try to use "setBytes", so for
     * all cases it's better to use setBinaryStream. Oracle also requires a change in the resultset
     * type of the prepared statement. MBAIRD NOTE: BLOBS may not work with Oracle database/thin
     * driver versions prior to 8.1.6.
     * 
     * @see Platform#setObjectForStatement
     */
    public void setObjectForStatement(PreparedStatement ps, int index, Object value, int sqlType)
            throws SQLException
    {
        if (((sqlType == Types.VARBINARY) || (sqlType == Types.LONGVARBINARY) || (sqlType == Types.BLOB))
                && (value instanceof byte[]))
        {
            byte buf[] = (byte[]) value;
            int length = buf.length;
           /* if (isUsingOracleThinDriver(ps.getConnection()) && length > THIN_BLOB_MAX_SIZE)
            {
                throw new SQLException(
                        "Oracle thin driver cannot update BLOB values with length>2000. (Consider using Oracle9i as OJB platform.)");
            }*/
            ByteArrayInputStream inputStream = new ByteArrayInputStream(buf);
            changePreparedStatementResultSetType(ps);
            ps.setBinaryStream(index, inputStream, length);
        }
        else if (value instanceof Double)
        {
            // workaround for the bug in Oracle thin driver
            ps.setDouble(index, ((Double) value).doubleValue());
        }
        else if (sqlType == Types.BIGINT && value instanceof Integer)
        {
            // workaround: Oracle thin driver problem when expecting long
            ps.setLong(index, ((Integer) value).intValue());
        }
        else if (sqlType == Types.INTEGER && value instanceof Long)
        {
            ps.setLong(index, ((Long) value).longValue());
        }
        else if (sqlType == Types.DATE && value instanceof String)
        {
            // special handling of like for dates (birthDate like '2000-01%')
            ps.setString(index, (String) value);
        }
        else if (sqlType == Types.CLOB && (value instanceof String || value instanceof byte[]))
        {
            Reader reader;
            int length;
            if (value instanceof String)
            {
                String stringValue = (String) value;
                length = stringValue.length();
                reader = new StringReader(stringValue);
            }
            else
            {
                byte buf[] = (byte[]) value;
                ByteArrayInputStream inputStream = new ByteArrayInputStream(buf);
                reader = new InputStreamReader(inputStream);
                length = buf.length;
            }
            /*if (isUsingOracleThinDriver(ps.getConnection()) && length > THIN_CLOB_MAX_SIZE)
            {
                throw new SQLException(
                        "Oracle thin driver cannot insert CLOB values with length>4000. (Consider using Oracle9i as OJB platform.)");
            }*/
            ps.setCharacterStream(index, reader, length);
        }
        else if ((sqlType == Types.CHAR || sqlType == Types.VARCHAR)
                 &&
                 (value instanceof String || value instanceof Character))
        {
            if (value instanceof String)
            {
                ps.setString(index, (String) value);
            }
            else // assert: value instanceof Character
            {
                ps.setString(index, value.toString());
            }
        }
        else
        {
            super.setObjectForStatement(ps, index, value, sqlType);
        }
    }

    /**
     * Attempts to modify a private member in the Oracle thin driver's resultset to allow proper
     * setting of large binary streams.
     */
    protected void changePreparedStatementResultSetType(PreparedStatement ps)
    {
        try
        {
            final Field f = ps.getClass().getSuperclass().getDeclaredField("m_userRsetType");
            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    f.setAccessible(true);
                    return null;
                }
            });
            f.setInt(ps, 1);
            f.setAccessible(false);
        }
        catch (Exception e)
        {
            logger.info("Not using classes12.zip.");
        }
    }

    /**
     * Get join syntax type for this RDBMS - one on of the constants from JoinSyntaxType interface
     */
    public byte getJoinSyntaxType()
    {
        return ORACLE_JOIN_SYNTAX;
    }

    public String createSequenceQuery(String sequenceName)
    {
        return "CREATE SEQUENCE " + sequenceName;
    }

    public String createSequenceQuery(String sequenceName, Properties prop)
    {
        /*
        CREATE SEQUENCE [schema.]sequence
            [INCREMENT BY integer]
            [START WITH integer]
            [MAXVALUE integer | NOMAXVALUE]
            [MINVALUE integer | NOMINVALUE]
            [CYCLE | NOCYCLE]
            [CACHE integer | NOCACHE]
            [ORDER | NOORDER]
        */
        StringBuffer query = new StringBuffer(createSequenceQuery(sequenceName));
        if(prop != null)
        {
            Boolean b;
            Long value;

            value = SequenceManagerHelper.getSeqIncrementBy(prop);
            if(value != null)
            {
                query.append(" INCREMENT BY ").append(value.longValue());
            }

            value = SequenceManagerHelper.getSeqStart(prop);
            if(value != null)
            {
                query.append(" START WITH ").append(value.longValue());
            }

            value = SequenceManagerHelper.getSeqMaxValue(prop);
            if(value != null)
            {
                query.append(" MAXVALUE ").append(value.longValue());
            }

            value = SequenceManagerHelper.getSeqMinValue(prop);
            if(value != null)
            {
                query.append(" MINVALUE ").append(value.longValue());
            }

            b = SequenceManagerHelper.getSeqCycleValue(prop);
            if(b != null)
            {
                if(b.booleanValue()) query.append(" CYCLE");
                else query.append(" NOCYCLE");
            }

            value = SequenceManagerHelper.getSeqCacheValue(prop);
            if(value != null)
            {
                query.append(" CACHE ").append(value.longValue());
            }

            b = SequenceManagerHelper.getSeqOrderValue(prop);
            if(b != null)
            {
                if(b.booleanValue()) query.append(" ORDER");
                else query.append(" NOORDER");
            }
        }
        return query.toString();
    }

    public String nextSequenceQuery(String sequenceName)
    {
        return "select " + sequenceName + ".nextval from dual";
    }

    public String dropSequenceQuery(String sequenceName)
    {
        return "drop sequence " + sequenceName;
    }

    /**
     * @see org.apache.ojb.broker.platforms.Platform#registerOutResultSet(java.sql.CallableStatement, int)
     */
    public void registerOutResultSet(CallableStatement stmt, int position)
            throws SQLException
    {
        stmt.registerOutParameter(position, ORACLE_JDBC_TYPE_CURSOR);
    }

    /**
     * Checks if the supplied connection is using the Oracle thin driver.
     * 
     * @param conn database connection for which to check JDBC-driver
     * @return <code>true</code> if the connection is using Oracle thin driver, <code>false</code>
     *         otherwise.
     */
    protected static boolean isUsingOracleThinDriver(Connection conn)
    {
        if (conn == null)
        {
            return false;
        }
        final DatabaseMetaData dbMetaData;
        final String dbUrl;
        try
        {
            dbMetaData = conn.getMetaData();
            dbUrl = dbMetaData.getURL();
            if (dbUrl != null && dbUrl.startsWith(THIN_URL_PREFIX))
            {
                return true;
            }
        }
        catch (Exception e)
        {
            // ignore it
        }
        return false;
    }

    /**
     * Initializes static variables needed for getting Oracle-specific JDBC types.
     */
    protected void initOracleReflectedVars()
    {
        try
        {
            // Check for Oracle-specific Types class
            final Class oracleTypes = ClassHelper.getClass("oracle.jdbc.OracleTypes", false);
            final Field cursorField = oracleTypes.getField("CURSOR");
            ORACLE_JDBC_TYPE_CURSOR = cursorField.getInt(null);
        }
        catch (ClassNotFoundException e)
        {
            log.warn("PlatformOracleImpl could not find Oracle JDBC classes");
        }
        catch (NoSuchFieldException e)
        {
            log.warn("PlatformOracleImpl could not find Oracle JDBC type fields");
        }
        catch (IllegalAccessException e)
        {
            log.warn("PlatformOracleImpl could not get Oracle JDBC type values");
        }
    }

}
