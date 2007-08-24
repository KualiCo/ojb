package org.apache.ojb.broker.util;

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

import java.sql.SQLException;
import java.sql.BatchUpdateException;
import java.sql.SQLWarning;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.ojb.broker.KeyConstraintViolatedException;
import org.apache.ojb.broker.PersistenceBrokerSQLException;
import org.apache.ojb.broker.core.ValueContainer;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.JdbcTypesHelper;
import org.apache.ojb.broker.util.logging.Logger;

/**
 * A helper class which endorse dealing with exceptions.
 *
 * @version $Id: ExceptionHelper.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
abstract public class ExceptionHelper
{
    /**
     * Method which support the conversion of {@link java.sql.SQLException} to
     * OJB's runtime exception (with additional message details).
     *
     * @param message The error message to use, if <em>null</em> a standard message is used.
     * @param ex The exception to convert (mandatory).
     * @param sql The used sql-statement or <em>null</em>.
     * @param logger The {@link org.apache.ojb.broker.util.logging.Logger} to log an detailed message
     * to the specified {@link org.apache.ojb.broker.util.logging.Logger} or <em>null</em> to skip logging message.
     * @return A new created {@link org.apache.ojb.broker.PersistenceBrokerSQLException} based on the specified
     *         arguments.
     */
    public static PersistenceBrokerSQLException generateException(String message, SQLException ex,  String sql, Logger logger)
    {
        return generateException(message, ex, sql, null, null, logger, null);
    }

    /**
     * Method which support the conversion of {@link java.sql.SQLException} to
     * OJB's runtime exception (with additional message details).
     *
     * @param ex The exception to convert (mandatory).
     * @param sql The used sql-statement or <em>null</em>.
     * @param cld The {@link org.apache.ojb.broker.metadata.ClassDescriptor} of the target object or <em>null</em>.
     * @param logger The {@link org.apache.ojb.broker.util.logging.Logger} to log an detailed message
     * to the specified {@link org.apache.ojb.broker.util.logging.Logger} or <em>null</em> to skip logging message.
     * @param obj The target object or <em>null</em>.
     * @return A new created {@link org.apache.ojb.broker.PersistenceBrokerSQLException} based on the specified
     *         arguments.
     */
    public static PersistenceBrokerSQLException generateException(SQLException ex,  String sql, ClassDescriptor cld, Logger logger, Object obj)
    {
        return generateException(ex, sql, cld, null, logger, obj);
    }

    /**
     * Method which support the conversion of {@link java.sql.SQLException} to
     * OJB's runtime exception (with additional message details).
     *
     * @param ex The exception to convert (mandatory).
     * @param sql The used sql-statement or <em>null</em>.
     * @param cld The {@link org.apache.ojb.broker.metadata.ClassDescriptor} of the target object or <em>null</em>.
     * @param values The values set in prepared statement or <em>null</em>.
     * @param logger The {@link org.apache.ojb.broker.util.logging.Logger} to log an detailed message
     * to the specified {@link org.apache.ojb.broker.util.logging.Logger} or <em>null</em> to skip logging message.
     * @param obj The target object or <em>null</em>.
     * @return A new created {@link org.apache.ojb.broker.PersistenceBrokerSQLException} based on the specified
     *         arguments.
     */
    public static PersistenceBrokerSQLException generateException(SQLException ex,  String sql, ClassDescriptor cld, ValueContainer[] values, Logger logger, Object obj)
    {
        return generateException(null, ex, sql, cld, values, logger, obj);
    }

    /**
     * Method which support the conversion of {@link java.sql.SQLException} to
     * OJB's runtime exception (with additional message details).
     *
     * @param message The error message to use, if <em>null</em> a standard message is used.
     * @param ex The exception to convert (mandatory).
     * @param sql The used sql-statement or <em>null</em>.
     * @param cld The {@link org.apache.ojb.broker.metadata.ClassDescriptor} of the target object or <em>null</em>.
     * @param values The values set in prepared statement or <em>null</em>.
     * @param logger The {@link org.apache.ojb.broker.util.logging.Logger} to log an detailed message
     * to the specified {@link org.apache.ojb.broker.util.logging.Logger} or <em>null</em> to skip logging message.
     * @param obj The target object or <em>null</em>.
     * @return A new created {@link org.apache.ojb.broker.PersistenceBrokerSQLException} based on the specified
     *         arguments.
     */
    public static PersistenceBrokerSQLException generateException(String message, SQLException ex,  String sql, ClassDescriptor cld, ValueContainer[] values, Logger logger, Object obj)
    {
        /*
        X/OPEN codes within class 23:
        23000	INTEGRITY CONSTRAINT VIOLATION
        23001	RESTRICT VIOLATION
        23502	NOT NULL VIOLATION
        23503	FOREIGN KEY VIOLATION
        23505	UNIQUE VIOLATION
        23514	CHECK VIOLATION
        */
        String eol = SystemUtils.LINE_SEPARATOR;
        StringBuffer msg = new StringBuffer(eol);
        eol += "* ";

        if(ex instanceof BatchUpdateException)
        {
            BatchUpdateException tmp = (BatchUpdateException) ex;
            if(message != null)
            {
                msg.append("* ").append(message);
            }
            else
            {
                msg.append("* BatchUpdateException during execution of sql-statement:");
            }
            msg.append(eol).append("Batch update count is '").append(tmp.getUpdateCounts()).append("'");
        }
        else if(ex instanceof SQLWarning)
        {
            if(message != null)
            {
                msg.append("* ").append(message);
            }
            else
            {
                msg.append("* SQLWarning during execution of sql-statement:");
            }
        }
        else
        {
            if(message != null)
            {
                msg.append("* ").append(message);
            }
            else
            {
                msg.append("* SQLException during execution of sql-statement:");
            }
        }

        if(sql != null)
        {
            msg.append(eol).append("sql statement was '").append(sql).append("'");
        }
        String stateCode = null;
        if(ex != null)
        {
            msg.append(eol).append("Exception message is [").append(ex.getMessage()).append("]");
            msg.append(eol).append("Vendor error code [").append(ex.getErrorCode()).append("]");
            msg.append(eol).append("SQL state code [");

            stateCode = ex.getSQLState();
            if("23000".equalsIgnoreCase(stateCode)) msg.append(stateCode).append("=INTEGRITY CONSTRAINT VIOLATION");
            else if("23001".equalsIgnoreCase(stateCode)) msg.append(stateCode).append("=RESTRICT VIOLATION");
            else if("23502".equalsIgnoreCase(stateCode)) msg.append(stateCode).append("=NOT NULL VIOLATION");
            else if("23503".equalsIgnoreCase(stateCode)) msg.append(stateCode).append("=FOREIGN KEY VIOLATION");
            else if("23505".equalsIgnoreCase(stateCode)) msg.append(stateCode).append("=UNIQUE VIOLATION");
            else if("23514".equalsIgnoreCase(stateCode)) msg.append(stateCode).append("=CHECK VIOLATION");
            else msg.append(stateCode);
            msg.append("]");
        }

        if(cld != null)
        {
            msg.append(eol).append("Target class is '")
                    .append(cld.getClassNameOfObject())
                    .append("'");
            FieldDescriptor[] fields = cld.getPkFields();
            msg.append(eol).append("PK of the target object is [");
            for(int i = 0; i < fields.length; i++)
            {
                try
                {
                    if(i > 0) msg.append(", ");
                    msg.append(fields[i].getPersistentField().getName());
                    if(obj != null)
                    {
                        msg.append("=");
                        msg.append(fields[i].getPersistentField().get(obj));
                    }
                }
                catch(Exception ignore)
                {
                    msg.append(" PK field build FAILED! ");
                }
            }
            msg.append("]");
        }
        if(values != null)
        {
            msg.append(eol).append(values.length).append(" values performed in statement: ").append(eol);
            for(int i = 0; i < values.length; i++)
            {
                ValueContainer value = values[i];
                msg.append("[");
                msg.append("jdbcType=").append(JdbcTypesHelper.getSqlTypeAsString(value.getJdbcType().getType()));
                msg.append(", value=").append(value.getValue());
                msg.append("]");
            }
        }
        if(obj != null)
        {
            msg.append(eol).append("Source object: ");
            try
            {
                msg.append(obj.toString());
            }
            catch(Exception e)
            {
                msg.append(obj.getClass());
            }
        }

        // message string for PB exception
        String shortMsg = msg.toString();

        if(ex != null)
        {
            // add causing stack trace
            Throwable rootCause = ExceptionUtils.getRootCause(ex);
            if(rootCause == null) rootCause = ex;
            msg.append(eol).append("The root stack trace is --> ");
            String rootStack = ExceptionUtils.getStackTrace(rootCause);
            msg.append(eol).append(rootStack);
        }
        msg.append(SystemUtils.LINE_SEPARATOR).append("**");

        // log error message
        if(logger != null) logger.error(msg.toString());

        // throw a specific type of runtime exception for a key constraint.
        if("23000".equals(stateCode) || "23505".equals(stateCode))
        {
            throw new KeyConstraintViolatedException(shortMsg, ex);
        }
        else
        {
            throw new PersistenceBrokerSQLException(shortMsg, ex);
        }
    }
}
