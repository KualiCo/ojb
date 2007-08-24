package org.apache.ojb.broker.util.sequence;

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

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;

import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.accesslayer.StatementManagerIF;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * This SequenceManager implementation uses database
 * sequence key generation (e.g supported by
 * Oracle, SAP DB, PostgreSQL, ...).
 * This class is responsible for creating new unique ID's.
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
 *          in field-descriptor.
 *          <br/>
 *          If set 'false' OJB throws an exception
 *          if none sequence name was found in field-descriptor, ditto
 *          OJB does NOT try to create a database sequence entry when
 *          for given sequence name no database sequence could be found.
 *    </td>
 * </tr>
 * </table>
 *
 *
 * <br/>
 * <p>
 * <b>Limitations:</b>
 * <ul>
 *	<li>none</li>
 * </ul>
 * </p>
 * <br/>
 * <br/>
 *
 * @author Edson Carlos Ericksson Richter
 * @author Rajeev Kaul
 * @author Thomas Mahler
 * @author Armin Waibel
 * @version $Id: SequenceManagerNextValImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class SequenceManagerNextValImpl extends AbstractSequenceManager
{
    private Logger log = LoggerFactory.getLogger(SequenceManagerNextValImpl.class);

    /**
     *
     */
    public SequenceManagerNextValImpl(PersistenceBroker broker)
    {
        super(broker);
    }

    /**
     * returns a unique int value for class clazz and field fieldName.
     * the returned number is unique accross all tables in the extent of clazz.
     */
    protected int getUniqueId(FieldDescriptor field) throws SequenceManagerException
    {
    	return (int) getUniqueLong(field);
    }


    /**
     * returns a unique long value for class clazz and field fieldName.
     * the returned number is unique accross all tables in the extent of clazz.
     */
    protected long getUniqueLong(FieldDescriptor field) throws SequenceManagerException
    {
        long result;
        // lookup sequence name
        String sequenceName = calculateSequenceName(field);
        try
        {
            result = buildNextSequence(field.getClassDescriptor(), sequenceName);
        }
        catch (Throwable e)
        {
            // maybe the sequence was not created
            try
            {
                log.info("Create DB sequence key '"+sequenceName+"'");
                createSequence(field.getClassDescriptor(), sequenceName);
            }
            catch (Exception e1)
            {
                throw new SequenceManagerException(
                        SystemUtils.LINE_SEPARATOR +
                        "Could not grab next id, failed with " + SystemUtils.LINE_SEPARATOR +
                        e.getMessage() + SystemUtils.LINE_SEPARATOR +
                        "Creation of new sequence failed with " +
                        SystemUtils.LINE_SEPARATOR + e1.getMessage() + SystemUtils.LINE_SEPARATOR
                        , e1);
            }
            try
            {
                result = buildNextSequence(field.getClassDescriptor(), sequenceName);
            }
            catch (Throwable e1)
            {
                throw new SequenceManagerException("Could not grab next id, sequence seems to exist", e);
            }
        }
        return result;
    }

    protected long buildNextSequence(ClassDescriptor cld, String sequenceName) throws Exception
    {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        long result = -1;
        StatementManagerIF stmtMan = getBrokerForClass().serviceStatementManager();
        try
        {
            stmt = stmtMan.getPreparedStatement(cld, getPlatform().nextSequenceQuery(sequenceName) ,Query.NOT_SCROLLABLE, 1, false);
            rs = stmt.executeQuery();
            rs.next();
            result = rs.getLong(1);
        }
        finally
        {
            stmtMan.closeResources(stmt, rs);
        }
        return result;
    }

    protected void createSequence(ClassDescriptor cld, String sequenceName) throws Exception
    {
        Statement stmt = null;
        StatementManagerIF stmtMan = getBrokerForClass().serviceStatementManager();
// arminw: never try to remove existing sequences, because this may lead in unexpected behaviour
// if the reason for the create call isn't a missing sequence (e.g. network problems)  
//        try
//        {
//            stmt = stmtMan.getGenericStatement(cld, Query.NOT_SCROLLABLE);
//            stmt.execute(getPlatform().dropSequenceQuery(sequenceName));
//        }
//        catch (Exception ignore)
//        {
//            // ignore it
//        }
//        finally
//        {
//            try
//            {
//                stmtMan.closeResources(stmt, null);
//            }
//            catch (Exception ignore)
//            {
//                // ignore it
//            }
//        }

        try
        {
            stmt = stmtMan.getGenericStatement(cld, Query.NOT_SCROLLABLE);
            stmt.execute(getPlatform().createSequenceQuery(sequenceName, getConfigurationProperties()));
        }
        finally
        {
            try
            {
                stmtMan.closeResources(stmt, null);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
