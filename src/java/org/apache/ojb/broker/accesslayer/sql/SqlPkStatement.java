package org.apache.ojb.broker.accesslayer.sql;

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

import java.lang.ref.WeakReference;

import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.util.logging.Logger;

/**
 * Model simple Statements based on ClassDescriptor and/or PrimaryKey
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: SqlPkStatement.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public abstract class SqlPkStatement implements SqlStatement
{
    // arminw: Use weak reference to allow GC of removed metadata instances
    private WeakReference m_classDescriptor;
    private Logger m_logger;

    /** Constructor for SqlPkStatement. */
    public SqlPkStatement(ClassDescriptor aCld, Logger aLogger)
    {
        super();
        m_classDescriptor = new WeakReference(aCld);
        m_logger = aLogger;
    }

    /** append table name */
    protected void appendTable(ClassDescriptor cld, StringBuffer stmt)
    {
        stmt.append(cld.getFullTableName());
    }

    /**
     * Returns the logger.
     *
     * @return Logger
     */
    protected Logger getLogger()
    {
        return m_logger;
    }

    /**
     * Returns the classDescriptor.
     *
     * @return ClassDescriptor
     */
    protected ClassDescriptor getClassDescriptor()
    {
        ClassDescriptor cld = (ClassDescriptor) m_classDescriptor.get();
        if(cld == null)
        {
            throw new OJBRuntimeException("Requested ClassDescriptor instance was already GC by JVM");
        }
        return cld;
    }

    /**
     * Generate a sql where-clause for the array of fields
     *
     * @param fields array containing all columns used in WHERE clause
     */
    protected void appendWhereClause(FieldDescriptor[] fields, StringBuffer stmt) throws PersistenceBrokerException
    {
        stmt.append(" WHERE ");

        for(int i = 0; i < fields.length; i++)
        {
            FieldDescriptor fmd = fields[i];

            stmt.append(fmd.getColumnName());
            stmt.append(" = ? ");
            if(i < fields.length - 1)
            {
                stmt.append(" AND ");
            }
        }
    }

    /**
     * Generate a where clause for a prepared Statement.
     * Only primary key and locking fields are used in this where clause
     *
     * @param cld the ClassDescriptor
     * @param useLocking true if locking fields should be included
     * @param stmt the StatementBuffer
     */
    protected void appendWhereClause(ClassDescriptor cld, boolean useLocking, StringBuffer stmt)
    {
        FieldDescriptor[] pkFields = cld.getPkFields();
        FieldDescriptor[] fields;

        fields = pkFields;
        if(useLocking)
        {
            FieldDescriptor[] lockingFields = cld.getLockingFields();
            if(lockingFields.length > 0)
            {
                fields = new FieldDescriptor[pkFields.length + lockingFields.length];
                System.arraycopy(pkFields, 0, fields, 0, pkFields.length);
                System.arraycopy(lockingFields, 0, fields, pkFields.length, lockingFields.length);
            }
        }

        appendWhereClause(fields, stmt);
    }

}
