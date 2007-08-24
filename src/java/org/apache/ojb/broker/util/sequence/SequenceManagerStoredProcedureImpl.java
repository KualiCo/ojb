package org.apache.ojb.broker.util.sequence;

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

import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.platforms.PlatformException;
import org.apache.ojb.broker.accesslayer.LookupException;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;

/**
 * This solution will give those seeking an oracle-style
 * sequence generator a final answer (Identity columns really suck).
 * <br/>
 * The <code>SequenceManagerStoredProcedureImpl</code> implementation enabled database
 * sequence key generation for all databases (e.g. MSSQL, MySQL, DB2, ...)
 * with a <b>JDBC 2.0</b> compliant driver.
 * <br/>
 * First add a new table <code>OJB_NEXTVAL_SEQ</code> to
 * your database.
 * <pre>
 * CREATE TABLE OJB_NEXTVAL_SEQ
 * (
 *     SEQ_NAME    VARCHAR(150) NOT NULL,
 *     MAX_KEY     BIGINT,
 *     CONSTRAINT SYS_PK_OJB_NEXTVAL_SEQ PRIMARY KEY(SEQ_NAME)
 * )
 * </pre>
 * You will also need the stored procedure OJB_NEXTVAL
 * will will take care of giving you a guaranteed unique
 * sequence number, in multi server environments.
 * <br/>
 * <pre>
 * CREATE PROCEDURE ojb_nextval_proc @SEQ_NAME varchar(100)
 *              AS
 *		declare @MAX_KEY BIGINT
 *              -- return an error if sequence does not exist
 *              -- so we will know if someone truncates the table
 *              set @MAX_KEY = 0
 *
 *              UPDATE OJB_NEXTVAL_SEQ
 *              SET    @MAX_KEY = MAX_KEY = MAX_KEY + 1
 *              WHERE  SEQ_NAME = @SEQ_NAME
 *
 *		if @MAX_KEY = 0
 *			select 1/0
 *		else
 *			select @MAX_KEY
 *
 *              RETURN @MAX_KEY
 * </pre>
 * <br/>
 * It is possible to define a <code>sequence-name</code>
 * field-descriptor attribute in the repository file. If
 * such an attribute was not found, the implementation build
 * an extent aware sequence name by its own.
 * <br/>
 * Keep in mind when define a sequence name, that you are responsible
 * to be aware of extents, that is: if you ask for an uid for an
 * interface with several
 * implementor classes, or a baseclass with several subclasses the returned
 * uid have to be unique accross all tables representing objects of the
 * extent in question. Thus you have to use the same <code>sequence-name</code>
 * for all extents.
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
 *     <td>autoNaming</td>
 *     <td>
 *          Default was 'true'. If set 'true' OJB try to build a
 *          sequence name automatic if none found in field-descriptor
 *          and set this generated name as <code>sequence-name</code>
 *          in field-descriptor. If set 'false' OJB throws an exception
 *          if none sequence name was found in field-descriptor.
 *    </td>
 * </tr>
 * </table>
 *
 * <p>
 * <b>Limitations:</b>
 * <ul>
 *	<li>do not use when other application use the native key generation ditto</li>
 * </ul>
 * </p>
 * <br/>
 * <br/>
 *
 * @author Ryan Vanderwerf
 * @author Edson Carlos Ericksson Richter
 * @author Rajeev Kaul
 * @author Thomas Mahler
 * @author Armin Waibel
 * @version $Id: SequenceManagerStoredProcedureImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class SequenceManagerStoredProcedureImpl extends AbstractSequenceManager
{
    private Logger log = LoggerFactory.getLogger(SequenceManagerStoredProcedureImpl.class);
    protected static final String PROCEDURE_NAME = "ojb_nextval_proc";
    protected static final String SEQ_NAME_STRING = "SEQ_NAME";
    protected static final String SEQ_ID_STRING = "MAX_KEY";
    protected static final String SEQ_TABLE_NAME = "OJB_NEXTVAL_SEQ";

    /**
     * Constructor
     * @param broker
     */
    public SequenceManagerStoredProcedureImpl(PersistenceBroker broker)
    {
        super(broker);
    }

    /**
     * Insert syntax for our special table
     * @param sequenceName
     * @param maxKey
     * @return sequence insert statement
     */
    protected String sp_createSequenceQuery(String sequenceName, long maxKey)
    {
        return "insert into " + SEQ_TABLE_NAME + " ("
                + SEQ_NAME_STRING + "," + SEQ_ID_STRING +
                ") values ('" + sequenceName + "'," + maxKey + ")";
    }

    /**
     * Gets the actual key - will create a new row with the max key of table if it
     * does not exist.
     * @param field
     * @return
     * @throws SequenceManagerException
     */
    protected long getUniqueLong(FieldDescriptor field) throws SequenceManagerException
    {
        boolean needsCommit = false;
        long result = 0;
        /*
        arminw:
        use the associated broker instance, check if broker was in tx or
        we need to commit used connection.
        */
        PersistenceBroker targetBroker = getBrokerForClass();
        if(!targetBroker.isInTransaction())
        {
            targetBroker.beginTransaction();
            needsCommit = true;
        }
        try
        {
            // lookup sequence name
            String sequenceName = calculateSequenceName(field);
            try
            {
                result = buildNextSequence(targetBroker, field.getClassDescriptor(), sequenceName);
                /*
                if 0 was returned we assume that the stored procedure
                did not work properly.
                */
                if (result == 0)
                {
                    throw new SequenceManagerException("No incremented value retrieved");
                }
            }
            catch (Exception e)
            {
                // maybe the sequence was not created
                log.info("Could not grab next key, message was " + e.getMessage() +
                        " - try to write a new sequence entry to database");
                try
                {
                    // on create, make sure to get the max key for the table first
                    long maxKey = SequenceManagerHelper.getMaxForExtent(targetBroker, field);
                    createSequence(targetBroker, field, sequenceName, maxKey);
                }
                catch (Exception e1)
                {
                    String eol = SystemUtils.LINE_SEPARATOR;
                    throw new SequenceManagerException(eol + "Could not grab next id, failed with " + eol +
                            e.getMessage() + eol + "Creation of new sequence failed with " +
                            eol + e1.getMessage() + eol, e1);
                }
                try
                {
                    result = buildNextSequence(targetBroker, field.getClassDescriptor(), sequenceName);
                }
                catch (Exception e1)
                {
                    throw new SequenceManagerException("Could not grab next id although a sequence seems to exist", e);
                }
            }
        }
        finally
        {
            if(targetBroker != null && needsCommit)
            {
                targetBroker.commitTransaction();
            }
        }
        return result;
    }

    /**
     * Calls the stored procedure stored procedure throws an
     * error if it doesn't exist.
     * @param broker
     * @param cld
     * @param sequenceName
     * @return
     * @throws LookupException
     * @throws SQLException
     */
    protected long buildNextSequence(PersistenceBroker broker, ClassDescriptor cld, String sequenceName)
            throws LookupException, SQLException, PlatformException
    {
        CallableStatement cs = null;
        try
        {
            Connection con = broker.serviceConnectionManager().getConnection();
            cs = getPlatform().prepareNextValProcedureStatement(con, PROCEDURE_NAME, sequenceName);
            cs.executeUpdate();
            return cs.getLong(1);
        }
        finally
        {
            try
            {
                if (cs != null)
                    cs.close();
            }
            catch (SQLException ignore)
            {
                // ignore it
            }
        }
    }

    /**
     * Creates new row in table
     * @param broker
     * @param field
     * @param sequenceName
     * @param maxKey
     * @throws Exception
     */
    protected void createSequence(PersistenceBroker broker, FieldDescriptor field,
                                  String sequenceName, long maxKey) throws Exception
    {
        Statement stmt = null;
        try
        {
            stmt = broker.serviceStatementManager().getGenericStatement(field.getClassDescriptor(), Query.NOT_SCROLLABLE);
            stmt.execute(sp_createSequenceQuery(sequenceName, maxKey));
        }
        catch (Exception e)
        {
            log.error(e);
            throw new SequenceManagerException("Could not create new row in "+SEQ_TABLE_NAME+" table - TABLENAME=" +
                    sequenceName + " field=" + field.getColumnName(), e);
        }
        finally
        {
            try
            {
                if (stmt != null) stmt.close();
            }
            catch (SQLException sqle)
            {
                if(log.isDebugEnabled())
                    log.debug("Threw SQLException while in createSequence and closing stmt", sqle);
                // ignore it
            }
        }
    }
}
