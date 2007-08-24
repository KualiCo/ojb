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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.accesslayer.JdbcAccess;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;


/**
 * Sequence manager implementation using native database <tt>Identity columns</tt>
 * (like MySQL, MSSQL, ...). For proper work some specific metadata settings
 * needed:
 * <ul>
 * <li>field representing the identity column need attribute <code>autoincrement</code> 'true'</li>
 * <li>field representing the identity column need attribute <code>access</code> set 'readonly'</li>
 * <li>field representing the identity column need attribute <code>primarykey</code> set 'true'</li>
 * <li>only possible to declare one identity field per class</li>
 * </ul>
 * <p/>
 * <b>Note:</b>
 * Make sure that the DB generated identity columns represent values &gt 0, because negative values
 * intern used by this implementation and 0 could cause problems with primitive FK fields.
 * </p>
 * <p/>
 * Implementation configuration properties:
 * <table cellspacing="2" cellpadding="2" border="3" frame="box">
 * <tr>
 * <td><strong>Property Key</strong></td>
 * <td><strong>Property Values</strong></td>
 * </tr>
 * <tr>
 * <td>no properties to set</td>
 * <td>
 * <p/>
 * </td>
 * </tr>
 * </table>
 * </p>
 * <p/>
 * <p/>
 * <b>Limitations:</b>
 * <ul>
 * <li>Native key generation is not 'extent aware'
 * when extent classes span several tables! Please
 * see more in shipped docs 'extents and polymorphism'
 * or sequence manager docs.
 * </li>
 * <li>
 * Only positive identity values are allowed (see above).
 * </li>
 * </ul>
 * </p>
 * <br/>
 * <br/>
 *
 * @author <a href="mailto:travis@spaceprogram.com">Travis Reeder</a>
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: SequenceManagerNativeImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class SequenceManagerNativeImpl extends AbstractSequenceManager
{
    private Log log = LogFactory.getLog(SequenceManagerNativeImpl.class);

    /*
     TODO:
     1. Find a better solution (if possible) for this problem
     We need this dummy field to return a negative long value
     on getUniqueLong(...) call. If we return always the same
     value, the resulting Identity object was found on cache.

     2. Problem is that generated oid (by Identity column)
     must not begin with 0.

     Use keyword 'volatile' to make decrement of a long value an
     atomic operation
     */
    private static volatile long tempKey = -1;

    public SequenceManagerNativeImpl(PersistenceBroker broker)
    {
        super(broker);
    }

    public void afterStore(JdbcAccess dbAccess, ClassDescriptor cld, Object obj) throws SequenceManagerException
    {
        FieldDescriptor identityField = extractIdentityColumnField(cld);
        if(identityField != null)
        {
            ifNotReadOnlyFail(identityField);
            long newId = getLastInsert(cld, identityField);
            setFieldValue(obj, identityField, new Long(newId));
        }
    }

    /**
     * Gets the identity column descriptor for the given class
     * or return <code>null</code> if none defined.
     *
     * @param cld The class descriptor
     * @return The class's identity column or <code>null</code> if it does not have one
     */
    private FieldDescriptor extractIdentityColumnField(ClassDescriptor cld)
    {
        FieldDescriptor[] pkFields = cld.getPkFields();
        for(int i = 0; i < pkFields.length; i++)
        {
            // to find the identity column we search for a autoincrement
            // read-only field
            if(pkFields[i].isAutoIncrement() && pkFields[i].isAccessReadOnly())
            {
                return pkFields[i];
            }
        }
        return null;
    }

    private void ifNotReadOnlyFail(FieldDescriptor field) throws SequenceManagerException
    {
        // is field declared as read-only?
        if(!field.isAccessReadOnly())
        {
            throw new SequenceManagerException("Can't find Identity column: Identity columns/fields need to be declared as" +
                    " 'autoincrement' with 'readonly' access in field-descriptor");
        }
    }

    private long getLastInsert(ClassDescriptor cld, FieldDescriptor field) throws SequenceManagerException
    {
        long newId = 0;
        Statement stmt = null;
        if(field != null)
        { // an autoinc column exists
            try
            {
                stmt = getBrokerForClass().serviceConnectionManager().getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(lastInsertSelect(cld.getFullTableName()));
                if(!rs.next())
                {
                    throw new SequenceManagerException("Could not find native identifier");
                }
                newId = rs.getLong(1);
                rs.close();
                if(log.isDebugEnabled()) log.debug("After store - newid=" + newId);
            }
            catch(Exception e)
            {
                throw new SequenceManagerException(e);
            }
            finally
            {
                try
                {
                    if(stmt != null) stmt.close();
                }
                catch(SQLException e)
                {
                    if(log.isDebugEnabled())
                        log.debug("Threw SQLException while in getLastInsert and closing stmt", e);
                    // ignore it
                }
            }
        }
        else
        {
            throw new SequenceManagerException("No autoincrement field declared, please check repository for " + cld);
        }
        return newId;
    }

    /*
     * query for the last insert id.
     */
    protected String lastInsertSelect(String tableName)
    {
        return getBrokerForClass().serviceConnectionManager().
                getSupportedPlatform().getLastInsertIdentityQuery(tableName);
    }

    private void setFieldValue(Object obj, FieldDescriptor field, Long identifier) throws SequenceManagerException
    {
        Object result = field.getJdbcType().sequenceKeyConversion(identifier);
        result = field.getFieldConversion().sqlToJava(result);
        PersistentField pf = field.getPersistentField();
        pf.set(obj, result);
    }

    /**
     * returns a negative value
     */
    protected long getUniqueLong(FieldDescriptor field) throws SequenceManagerException
    {
        /*
        arminw:
        workaround for locking problems of new objects
        We need unique 'dummy keys' for new objects before storing.
        Variable 'tempKey' is declared volatile, thus decrement should be atomic
        */
        return --tempKey;
    }
}
