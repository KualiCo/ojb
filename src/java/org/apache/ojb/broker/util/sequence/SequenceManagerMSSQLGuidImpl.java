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
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.metadata.JdbcTypesHelper;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.accesslayer.ResultSetAndStatement;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.JdbcType;
import org.apache.ojb.broker.query.Query;

import java.sql.SQLException;


/**
 * An Implementation Class that will retrieve a valid new value
 * for a PK field that is of type 'uniqueidentifier'. Since values
 * for these types are generated through a 'newid()' call to
 * MSSQL Server, this class is only valid for MSSQL Server 7.0 and up.
 * <br/>
 * This SequenceManager can be used for any classes that have their PK
 * defined as a 'uniqueidetifier'
 *
 * @author <a href="mailto:aclute825@hotmail.com">Andrew Clute</a>
 * @version $Id: SequenceManagerMSSQLGuidImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class SequenceManagerMSSQLGuidImpl extends AbstractSequenceManager
{
    private static final JdbcType JDBC_TYPE_VARCHAR = JdbcTypesHelper.getJdbcTypeByName("varchar");
    /**
     * Constructor used by
     * {@link org.apache.ojb.broker.util.sequence.SequenceManagerFactory}
     *
     * @param broker  PB instance to perform the
     * id generation.
     */
    public SequenceManagerMSSQLGuidImpl(PersistenceBroker broker)
    {
        super(broker);
    }

    public Object getUniqueValue(FieldDescriptor field) throws SequenceManagerException
    {
        // only works for VARCHAR fields
        if(!field.getJdbcType().equals(JDBC_TYPE_VARCHAR))
        {
            throw new SequenceManagerException("This implementation only works with fields defined" +
                    " as VARCHAR, but given field was " + (field != null ? field.getJdbcType() : null));
        }
        Object result = getUniqueString(field);
        // perform a sql to java conversion here, so that clients do
        // not see any db specific values
        result = field.getFieldConversion().sqlToJava(result);
        return result;
    }

    /**
     * returns a unique String for given field.
     * the returned uid is unique accross all tables.
     */
    protected String getUniqueString(FieldDescriptor field) throws SequenceManagerException
    {
        ResultSetAndStatement rsStmt = null;
        String returnValue = null;
        try
        {
            rsStmt = getBrokerForClass().serviceJdbcAccess().executeSQL(
                    "select newid()", field.getClassDescriptor(), Query.NOT_SCROLLABLE);
            if (rsStmt.m_rs.next())
            {
                returnValue = rsStmt.m_rs.getString(1);
            }
            else
            {
                LoggerFactory.getDefaultLogger().error(this.getClass()
                        + ": Can't lookup new oid for field " + field);
            }
        }
        catch (PersistenceBrokerException e)
        {
            throw new SequenceManagerException(e);
        }
        catch (SQLException e)
        {
            throw new SequenceManagerException(e);
        }

        finally
        {
            // close the used resources
            if (rsStmt != null) rsStmt.close();
        }
        return returnValue;
    }

    /**
     * Returns a new unique int for the given Class and fieldname.
     */
    protected int getUniqueId(FieldDescriptor field) throws SequenceManagerException
    {
        throw new SequenceManagerException(
                SystemUtils.LINE_SEPARATOR +
                "Failure attempting to retrieve a Guid for a field that is an int -- field should be returned as a VARCHAR");
    }

    /**
     * Returns a new unique int for the given Class and fieldname.
     */
    protected long getUniqueLong(FieldDescriptor field) throws SequenceManagerException
    {
        throw new SequenceManagerException(
                SystemUtils.LINE_SEPARATOR +
                "Failure attempting to retrieve a Guid for a field that is a long -- field should be returned as a VARCHAR");
    }

    // TODO: never used? remove?
//    /**
//     * returns a unique Object for class clazz and field fieldName.
//     * the returned Object is unique accross all tables in the extent of clazz.
//     */
//    protected Object getUniqueObject(FieldDescriptor field) throws SequenceManagerException
//    {
//        return getUniqueString(field);
//    }
}

