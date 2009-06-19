package org.apache.ojb.broker.accesslayer;

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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.OptimisticLockException;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerSQLException;
import org.apache.ojb.broker.accesslayer.sql.SelectStatement;
import org.apache.ojb.broker.core.ValueContainer;
import org.apache.ojb.broker.metadata.ArgumentDescriptor;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.JdbcType;
import org.apache.ojb.broker.metadata.ProcedureDescriptor;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.platforms.Platform;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.util.ExceptionHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.sequence.SequenceManagerException;

/**
 * JdbcAccess is responsible for establishing performing
 * SQL Queries against remote Databases.
 * It hides all knowledge about JDBC from the BrokerImpl
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler</a>
 * @version $Id: JdbcAccessImpl.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class JdbcAccessImpl implements JdbcAccess
{
    /**
     * The logger used.
     */
    protected Logger logger;

    /**
     * The broker in use.
     */
    protected PersistenceBroker broker;

    /**
     * constructor is private, use getInstance to get
     * the singleton instance of this class
     */
    public JdbcAccessImpl(PersistenceBroker broker)
    {
        this.broker = broker;
        logger = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Helper Platform accessor method
     *
     * @return Platform for the current broker connection manager.
     */
    private Platform getPlatform()
    {
        return this.broker.serviceConnectionManager().getSupportedPlatform();
    }

    /**
     * performs a DELETE operation against RDBMS.
     * @param cld ClassDescriptor providing mapping information.
     * @param obj The object to be deleted.
     */
    public void executeDelete(ClassDescriptor cld, Object obj) throws PersistenceBrokerException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("executeDelete: " + obj);
        }

        final StatementManagerIF sm = broker.serviceStatementManager();
        PreparedStatement stmt = null;
        try
        {
            stmt = sm.getDeleteStatement(cld);
            if (stmt == null)
            {
                logger.error("getDeleteStatement returned a null statement");
                throw new PersistenceBrokerException("JdbcAccessImpl: getDeleteStatement returned a null statement");
            }

            sm.bindDelete(stmt, cld, obj);
            if (logger.isDebugEnabled())
                logger.debug("executeDelete: " + stmt);

            // @todo: clearify semantics
            // thma: the following check is not secure. The object could be deleted *or* changed.
            // if it was deleted it makes no sense to throw an OL exception.
            // does is make sense to throw an OL exception if the object was changed?
            if (stmt.executeUpdate() == 0 && cld.isLocking()) //BRJ
            {
                /**
                 * Kuali Foundation modification -- 6/19/2009
                 */
            	String objToString = "";
            	try {
            		objToString = obj.toString();
            	} catch (Exception ex) {}
                throw new OptimisticLockException("Object has been modified or deleted by someone else: " + objToString, obj);
                /**
                 * End of Kuali Foundation modification
                 */
            }

            // Harvest any return values.
            harvestReturnValues(cld.getDeleteProcedure(), obj, stmt);
        }
        catch (OptimisticLockException e)
        {
            // Don't log as error
            if (logger.isDebugEnabled())
                logger.debug("OptimisticLockException during the execution of delete: "
                        + e.getMessage(), e);
            throw e;
        }
        catch (PersistenceBrokerException e)
        {
            logger.error("PersistenceBrokerException during the execution of delete: "
                    + e.getMessage(), e);
            throw e;
        }
        catch (SQLException e)
        {
            final String sql = broker.serviceSqlGenerator().getPreparedDeleteStatement(cld).getStatement();
            throw ExceptionHelper.generateException(e, sql, cld, logger, obj);
        }
        finally
        {
            sm.closeResources(stmt, null);
        }
    }

    /**
     * Performs a DELETE operation based on the given {@link Query} against RDBMS.
     * @param query the query string.
     * @param cld ClassDescriptor providing JDBC information.
     */
    public void executeDelete(Query query, ClassDescriptor cld) throws PersistenceBrokerException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("executeDelete (by Query): " + query);
        }
        final StatementManagerIF sm = broker.serviceStatementManager();
        PreparedStatement stmt = null;
        final String sql = this.broker.serviceSqlGenerator().getPreparedDeleteStatement(query, cld).getStatement();
        try
        {
            stmt = sm.getPreparedStatement(cld, sql,
                    false, StatementManagerIF.FETCH_SIZE_NOT_APPLICABLE, cld.getDeleteProcedure()!=null);

            sm.bindStatement(stmt, query, cld, 1);
            if (logger.isDebugEnabled())
                logger.debug("executeDelete (by Query): " + stmt);

            stmt.executeUpdate();
        }
        catch (SQLException e)
        {
            throw ExceptionHelper.generateException(e, sql, cld, null, logger);
        }
        finally
        {
            sm.closeResources(stmt, null);
        }
    }

    /**
     * performs an INSERT operation against RDBMS.
     * @param obj The Object to be inserted as a row of the underlying table.
     * @param cld ClassDescriptor providing mapping information.
     */
    public void executeInsert(ClassDescriptor cld, Object obj) throws PersistenceBrokerException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("executeInsert: " + obj);
        }
        final StatementManagerIF sm = broker.serviceStatementManager();
        PreparedStatement stmt = null;
        try
        {
            stmt = sm.getInsertStatement(cld);
            if (stmt == null)
            {
                logger.error("getInsertStatement returned a null statement");
                throw new PersistenceBrokerException("getInsertStatement returned a null statement");
            }
            // before bind values perform autoincrement sequence columns
            assignAutoincrementSequences(cld, obj);
            sm.bindInsert(stmt, cld, obj);
            if (logger.isDebugEnabled())
                logger.debug("executeInsert: " + stmt);
            stmt.executeUpdate();
            // after insert read and assign identity columns
            assignAutoincrementIdentityColumns(cld, obj);

            // Harvest any return values.
            harvestReturnValues(cld.getInsertProcedure(), obj, stmt);
        }
        catch (PersistenceBrokerException e)
        {
            logger.error("PersistenceBrokerException during the execution of the insert: " + e.getMessage(), e);
            throw e;
        }
        catch(SequenceManagerException e)
        {
            throw new PersistenceBrokerException("Error while try to assign identity value", e);
        }
        catch (SQLException e)
        {
            final String sql = broker.serviceSqlGenerator().getPreparedInsertStatement(cld).getStatement();
            throw ExceptionHelper.generateException(e, sql, cld, logger, obj);
        }
        finally
        {
            sm.closeResources(stmt, null);
        }
    }

    /**
     * performs a SELECT operation against RDBMS.
     * @param query the query string.
     * @param cld ClassDescriptor providing JDBC information.
     */
    public ResultSetAndStatement executeQuery(Query query, ClassDescriptor cld) throws PersistenceBrokerException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("executeQuery: " + query);
        }
        /*
		 * MBAIRD: we should create a scrollable resultset if the start at
		 * index or end at index is set
		 */
        boolean scrollable = ((query.getStartAtIndex() > Query.NO_START_AT_INDEX) || (query.getEndAtIndex() > Query.NO_END_AT_INDEX));
        /*
		 * OR if the prefetching of relationships is being used.
		 */
        if (query != null && query.getPrefetchedRelationships() != null && !query.getPrefetchedRelationships().isEmpty())
        {
            scrollable = true;
        }
        final StatementManagerIF sm = broker.serviceStatementManager();
        final SelectStatement sql = broker.serviceSqlGenerator().getPreparedSelectStatement(query, cld);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            final int queryFetchSize = query.getFetchSize();
            final boolean isStoredProcedure = isStoredProcedure(sql.getStatement());
            stmt = sm.getPreparedStatement(cld, sql.getStatement() ,
                    scrollable, queryFetchSize, isStoredProcedure);
            if (isStoredProcedure)
            {
                // Query implemented as a stored procedure, which must return a result set.
                // Query sytax is: { ?= call PROCEDURE_NAME(?,...,?)}
                getPlatform().registerOutResultSet((CallableStatement) stmt, 1);
                sm.bindStatement(stmt, query, cld, 2);

                if (logger.isDebugEnabled())
                    logger.debug("executeQuery: " + stmt);

                stmt.execute();
                rs = (ResultSet) ((CallableStatement) stmt).getObject(1);
            }
            else
            {
                sm.bindStatement(stmt, query, cld, 1);

                if (logger.isDebugEnabled())
                    logger.debug("executeQuery: " + stmt);

                rs = stmt.executeQuery();
            }

            return new ResultSetAndStatement(sm, stmt, rs, sql);
        }
        catch (PersistenceBrokerException e)
        {
            // release resources on exception
            sm.closeResources(stmt, rs);
            logger.error("PersistenceBrokerException during the execution of the query: " + e.getMessage(), e);
            throw e;
        }
        catch (SQLException e)
        {
            // release resources on exception
            sm.closeResources(stmt, rs);
            throw ExceptionHelper.generateException(e, sql.getStatement(), null, logger, null);
        }
    }

    public ResultSetAndStatement executeSQL(
        String sqlStatement,
        ClassDescriptor cld,
        boolean scrollable)
        throws PersistenceBrokerException
    {
        return executeSQL(sqlStatement, cld, null, scrollable);
    }

    /**
     * performs a SQL SELECT statement against RDBMS.
     * @param sql the query string.
     * @param cld ClassDescriptor providing meta-information.
     */
    public ResultSetAndStatement executeSQL(
        final String sql,
        ClassDescriptor cld,
        ValueContainer[] values,
        boolean scrollable)
        throws PersistenceBrokerException
    {
        if (logger.isDebugEnabled()) logger.debug("executeSQL: " + sql);
        final boolean isStoredprocedure = isStoredProcedure(sql);
        final StatementManagerIF sm = broker.serviceStatementManager();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            stmt = sm.getPreparedStatement(cld, sql,
                    scrollable, StatementManagerIF.FETCH_SIZE_NOT_EXPLICITLY_SET, isStoredprocedure);
            if (isStoredprocedure)
            {
                // Query implemented as a stored procedure, which must return a result set.
                // Query sytax is: { ?= call PROCEDURE_NAME(?,...,?)}
                getPlatform().registerOutResultSet((CallableStatement) stmt, 1);
                sm.bindValues(stmt, values, 2);
                stmt.execute();
                rs = (ResultSet) ((CallableStatement) stmt).getObject(1);
            }
            else
            {
                sm.bindValues(stmt, values, 1);
                rs = stmt.executeQuery();
            }

            // as we return the resultset for further operations, we cannot release the statement yet.
            // that has to be done by the JdbcAccess-clients (i.e. RsIterator, ProxyRsIterator and PkEnumeration.)
            return new ResultSetAndStatement(sm, stmt, rs, new SelectStatement()
            {
                public Query getQueryInstance()
                {
                    return null;
                }

                public int getColumnIndex(FieldDescriptor fld)
                {
                    return JdbcType.MIN_INT;
                }

                public String getStatement()
                {
                    return sql;
                }
            });
        }
        catch (PersistenceBrokerException e)
        {
            // release resources on exception
            sm.closeResources(stmt, rs);
            logger.error("PersistenceBrokerException during the execution of the SQL query: " + e.getMessage(), e);
            throw e;
        }
        catch (SQLException e)
        {
            // release resources on exception
            sm.closeResources(stmt, rs);
            throw ExceptionHelper.generateException(e, sql, cld, values, logger, null);
        }
    }

    public int executeUpdateSQL(String sqlStatement, ClassDescriptor cld)
        throws PersistenceBrokerException
    {
        return executeUpdateSQL(sqlStatement, cld, null, null);
    }

    /**
     * performs a SQL UPDTE, INSERT or DELETE statement against RDBMS.
     * @param sqlStatement the query string.
     * @param cld ClassDescriptor providing meta-information.
     * @return int returncode
     */
    public int executeUpdateSQL(
        String sqlStatement,
        ClassDescriptor cld,
        ValueContainer[] values1,
        ValueContainer[] values2)
        throws PersistenceBrokerException
    {
        if (logger.isDebugEnabled())
            logger.debug("executeUpdateSQL: " + sqlStatement);

        int result;
        int index;
        PreparedStatement stmt = null;
        final StatementManagerIF sm = broker.serviceStatementManager();
        try
        {
            stmt = sm.getPreparedStatement(cld, sqlStatement,
                    Query.NOT_SCROLLABLE, StatementManagerIF.FETCH_SIZE_NOT_APPLICABLE, isStoredProcedure(sqlStatement));
            index = sm.bindValues(stmt, values1, 1);
            sm.bindValues(stmt, values2, index);
            result = stmt.executeUpdate();
        }
        catch (PersistenceBrokerException e)
        {
            logger.error("PersistenceBrokerException during the execution of the Update SQL query: " + e.getMessage(), e);
            throw e;
        }
        catch (SQLException e)
        {
            ValueContainer[] tmp = addValues(values1, values2);
            throw ExceptionHelper.generateException(e, sqlStatement, cld, tmp, logger, null);
        }
        finally
        {
            sm.closeResources(stmt, null);
        }
        return result;
    }

    /** Helper method, returns the addition of both arrays (add source to target array) */
    private ValueContainer[] addValues(ValueContainer[] target, ValueContainer[] source)
    {
        ValueContainer[] newArray;
        if(source != null && source.length > 0)
        {
            if(target != null)
            {
                newArray = new ValueContainer[target.length + source.length];
                System.arraycopy(target, 0, newArray, 0, target.length);
                System.arraycopy(source, 0, newArray, target.length, source.length);
            }
            else
            {
                newArray = source;
            }
        }
        else
        {
            newArray = target;
        }
        return newArray;
    }

    /**
     * performs an UPDATE operation against RDBMS.
     * @param obj The Object to be updated in the underlying table.
     * @param cld ClassDescriptor providing mapping information.
     */
    public void executeUpdate(ClassDescriptor cld, Object obj) throws PersistenceBrokerException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("executeUpdate: " + obj);
        }

        // obj with nothing but key fields is not updated
        if (cld.getNonPkRwFields().length == 0)
        {
            return;
        }

        final StatementManagerIF sm = broker.serviceStatementManager();
        PreparedStatement stmt = null;
        // BRJ: preserve current locking values
        // locking values will be restored in case of exception
        ValueContainer[] oldLockingValues;
        oldLockingValues = cld.getCurrentLockingValues(obj);
        try
        {
            stmt = sm.getUpdateStatement(cld);
            if (stmt == null)
            {
                logger.error("getUpdateStatement returned a null statement");
                throw new PersistenceBrokerException("getUpdateStatement returned a null statement");
            }

            sm.bindUpdate(stmt, cld, obj);
            if (logger.isDebugEnabled())
                logger.debug("executeUpdate: " + stmt);

            if ((stmt.executeUpdate() == 0) && cld.isLocking()) //BRJ
            {
                /**
                 * Kuali Foundation modification -- 6/19/2009
                 */
            	String objToString = "";
            	try {
            		objToString = obj.toString();
            	} catch (Exception ex) {}
                throw new OptimisticLockException("Object has been modified by someone else: " + objToString, obj);
                /**
                 * End of Kuali Foundation modification
                 */
            }

            // Harvest any return values.
            harvestReturnValues(cld.getUpdateProcedure(), obj, stmt);
        }
        catch (OptimisticLockException e)
        {
            // Don't log as error
            if (logger.isDebugEnabled())
                logger.debug(
                    "OptimisticLockException during the execution of update: " + e.getMessage(),
                    e);
            throw e;
        }
        catch (PersistenceBrokerException e)
        {
            // BRJ: restore old locking values
            setLockingValues(cld, obj, oldLockingValues);

            logger.error(
                "PersistenceBrokerException during the execution of the update: " + e.getMessage(),
                e);
            throw e;
        }
        catch (SQLException e)
        {
            final String sql = broker.serviceSqlGenerator().getPreparedUpdateStatement(cld).getStatement();
            throw ExceptionHelper.generateException(e, sql, cld, logger, obj);
        }
        finally
        {
            sm.closeResources(stmt, null);
        }
    }

    /**
     * performs a primary key lookup operation against RDBMS and materializes
     * an object from the resulting row. Only skalar attributes are filled from
     * the row, references are not resolved.
     * @param oid contains the primary key info.
     * @param cld ClassDescriptor providing mapping information.
     * @return the materialized object, null if no matching row was found or if
     * any error occured.
     */
    public Object materializeObject(ClassDescriptor cld, Identity oid)
        throws PersistenceBrokerException
    {
        final StatementManagerIF sm = broker.serviceStatementManager();
        final SelectStatement sql = broker.serviceSqlGenerator().getPreparedSelectByPkStatement(cld);
        Object result = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            stmt = sm.getSelectByPKStatement(cld);
            if (stmt == null)
            {
                logger.error("getSelectByPKStatement returned a null statement");
                throw new PersistenceBrokerException("getSelectByPKStatement returned a null statement");
            }
            /*
            arminw: currently a select by PK could never be a stored procedure,
            thus we can always set 'false'. Is this correct??
            */
            sm.bindSelect(stmt, oid, cld, false);
            rs = stmt.executeQuery();
            // data available read object, else return null
            ResultSetAndStatement rs_stmt = new ResultSetAndStatement(broker.serviceStatementManager(), stmt, rs, sql);
            if (rs.next())
            {
                Map row = new HashMap();
                cld.getRowReader().readObjectArrayFrom(rs_stmt, row);
                result = cld.getRowReader().readObjectFrom(row);
            }
            // close resources
            rs_stmt.close();
        }
        catch (PersistenceBrokerException e)
        {
            // release resources on exception
            sm.closeResources(stmt, rs);
            logger.error("PersistenceBrokerException during the execution of materializeObject: " + e.getMessage(), e);
            throw e;
        }
        catch (SQLException e)
        {
            // release resources on exception
            sm.closeResources(stmt, rs);
            throw ExceptionHelper.generateException(e, sql.getStatement(), cld, logger, null);
        }
        return result;
    }

    /**
     * Set the locking values
     * @param cld
     * @param obj
     * @param oldLockingValues
     */
    private void setLockingValues(ClassDescriptor cld, Object obj, ValueContainer[] oldLockingValues)
    {
        FieldDescriptor fields[] = cld.getLockingFields();

        for (int i=0; i<fields.length; i++)
        {
            PersistentField field = fields[i].getPersistentField();
            Object lockVal = oldLockingValues[i].getValue();

            field.set(obj, lockVal);
        }
    }

    /**
     * Harvest any values that may have been returned during the execution
     * of a procedure.
     *
     * @param proc the procedure descriptor that provides info about the procedure
     *      that was invoked.
     * @param obj the object that was persisted
     * @param stmt the statement that was used to persist the object.
     *
     * @throws PersistenceBrokerSQLException if a problem occurs.
     */
    private void harvestReturnValues(
        ProcedureDescriptor proc,
        Object obj,
        PreparedStatement stmt)
        throws PersistenceBrokerSQLException
    {
        // If the procedure descriptor is null or has no return values or
        // if the statement is not a callable statment, then we're done.
        if ((proc == null) || (!proc.hasReturnValues()))
        {
            return;
        }

        // Set up the callable statement
        CallableStatement callable = (CallableStatement) stmt;

        // This is the index that we'll use to harvest the return value(s).
        int index = 0;

        // If the proc has a return value, then try to harvest it.
        if (proc.hasReturnValue())
        {

            // Increment the index
            index++;

            // Harvest the value.
            this.harvestReturnValue(obj, callable, proc.getReturnValueFieldRef(), index);
        }

        // Check each argument.  If it's returned by the procedure,
        // then harvest the value.
        Iterator iter = proc.getArguments().iterator();
        while (iter.hasNext())
        {
            index++;
            ArgumentDescriptor arg = (ArgumentDescriptor) iter.next();
            if (arg.getIsReturnedByProcedure())
            {
                this.harvestReturnValue(obj, callable, arg.getFieldRef(), index);
            }
        }
    }

    /**
     * Harvest a single value that was returned by a callable statement.
     *
     * @param obj the object that will receive the value that is harvested.
     * @param callable the CallableStatement that contains the value to harvest
     * @param fmd the FieldDescriptor that identifies the field where the
     *      harvested value will be stord.
     * @param index the parameter index.
     *
     * @throws PersistenceBrokerSQLException if a problem occurs.
     */
    private void harvestReturnValue(
        Object obj,
        CallableStatement callable,
        FieldDescriptor fmd,
        int index)
        throws PersistenceBrokerSQLException
    {

        try
        {
            // If we have a field descriptor, then we can harvest
            // the return value.
            if ((callable != null) && (fmd != null) && (obj != null))
            {
                // Get the value and convert it to it's appropriate
                // java type.
                Object value = fmd.getJdbcType().getObjectFromColumn(callable, index);

                // Set the value of the persistent field.
                fmd.getPersistentField().set(obj, fmd.getFieldConversion().sqlToJava(value));

            }
        }
        catch (SQLException e)
        {
            String msg = "SQLException during the execution of harvestReturnValue"
                + " class="
                + obj.getClass().getName()
                + ","
                + " field="
                + fmd.getAttributeName()
                + " : "
                + e.getMessage();
            logger.error(msg,e);
            throw new PersistenceBrokerSQLException(msg, e);
        }
    }

    /**
     * Check if the specified sql-string is a stored procedure
     * or not.
     * @param sql The sql query to check
     * @return <em>True</em> if the query is a stored procedure, else <em>false</em> is returned.
     */
    protected boolean isStoredProcedure(String sql)
    {
        /*
        Stored procedures start with
        {?= call <procedure-name>[<arg1>,<arg2>, ...]}
        or
        {call <procedure-name>[<arg1>,<arg2>, ...]}
        but also statements with white space like
        { ?= call <procedure-name>[<arg1>,<arg2>, ...]}
        are possible.
        */
        int k = 0, i = 0;
        char c;
        while(k < 3 && i < sql.length())
        {
            c = sql.charAt(i);
            if(c != ' ')
            {
                switch (k)
                {
                    case 0:
                        if(c != '{') return false;
                        break;
                    case 1:
                        if(c != '?' && c != 'c') return false;
                        break;
                    case 2:
                        if(c != '=' && c != 'a') return false;
                        break;
                }
                k++;
            }
            i++;
        }
        return true;
    }

    protected void assignAutoincrementSequences(ClassDescriptor cld, Object target) throws SequenceManagerException
    {
        // TODO: refactor auto-increment handling, auto-increment should only be supported by PK fields?
        // FieldDescriptor[] fields = cld.getPkFields();
        FieldDescriptor[] fields = cld.getFieldDescriptor(false);
        FieldDescriptor field;
        for(int i = 0; i < fields.length; i++)
        {
            field = fields[i];
            if(field.isAutoIncrement() && !field.isAccessReadOnly())
            {
                Object value = field.getPersistentField().get(target);
                if(broker.serviceBrokerHelper().representsNull(field, value))
                {
                    Object id = broker.serviceSequenceManager().getUniqueValue(field);
                    field.getPersistentField().set(target, id);
                }
            }
        }
    }

    protected void assignAutoincrementIdentityColumns(ClassDescriptor cld, Object target) throws SequenceManagerException
    {
        // if database Identity Columns are used, query the id from database
        // other SequenceManager implementations will ignore this call
        if(cld.useIdentityColumnField()) broker.serviceSequenceManager().afterStore(this, cld, target);
    }
}
