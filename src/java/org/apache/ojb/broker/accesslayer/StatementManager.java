package org.apache.ojb.broker.accesslayer;

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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerSQLException;
import org.apache.ojb.broker.core.ValueContainer;
import org.apache.ojb.broker.metadata.ArgumentDescriptor;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.ProcedureDescriptor;
import org.apache.ojb.broker.platforms.Platform;
import org.apache.ojb.broker.platforms.PlatformException;
import org.apache.ojb.broker.platforms.PlatformFactory;
import org.apache.ojb.broker.query.BetweenCriteria;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.ExistsCriteria;
import org.apache.ojb.broker.query.FieldCriteria;
import org.apache.ojb.broker.query.InCriteria;
import org.apache.ojb.broker.query.NullCriteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.SelectionCriteria;
import org.apache.ojb.broker.query.SqlCriteria;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * manages JDBC Connection and Statement resources.
 *
 * @author Thomas Mahler
 * @author <a href="mailto:rburt3@mchsi.com">Randall Burt</a>
 * @version $Id: StatementManager.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class StatementManager implements StatementManagerIF
{
    private Logger m_log = LoggerFactory.getLogger(StatementManager.class);

    /** the associated broker */
    private final PersistenceBroker m_broker;
    private Platform m_platform;
    /**
     * Used when OJB run in JBoss
     * TODO: Find a better solution to handle OJB within JBoss
     * --> the JCA implementation should solve this problem
     *
     * arminw:
     * Seems with JBoss 3.2.2 or higher the problem is gone, so we
     * can deprecate this attribute sooner or later
     */
    private boolean m_eagerRelease;
    private ConnectionManagerIF m_conMan;

    public StatementManager(final PersistenceBroker pBroker)
    {
        this.m_broker = pBroker;
        this.m_conMan = m_broker.serviceConnectionManager();
        m_eagerRelease = m_conMan.getConnectionDescriptor().getEagerRelease();
        m_platform = PlatformFactory.getPlatformFor(m_conMan.getConnectionDescriptor());
    }

    public void closeResources(Statement stmt, ResultSet rs)
    {
        if (m_log.isDebugEnabled())
            m_log.debug("closeResources was called");
        try
        {
            m_platform.beforeStatementClose(stmt, rs);
            //close statement on wrapped statement class, or real statement
            if (stmt != null)
            {
                //log.info("## close: "+stmt);
                stmt.close();

                /*
                *********************************************
                special stuff for OJB within JBoss
                ********************************************
                */
                if (m_eagerRelease)
                {
                    m_conMan.releaseConnection();
                }

            }
            m_platform.afterStatementClose(stmt, rs);
        }
        catch (PlatformException e)
        {
            m_log.error("Platform dependent operation failed", e);
        }
        catch (SQLException ignored)
        {
            if (m_log.isDebugEnabled())
                m_log.debug("Statement closing failed", ignored);
        }
    }

    /**
     * binds the Identities Primary key values to the statement
     */
    public void bindDelete(PreparedStatement stmt, Identity oid, ClassDescriptor cld) throws SQLException
    {
        Object[] pkValues = oid.getPrimaryKeyValues();
        FieldDescriptor[] pkFields = cld.getPkFields();
        int i = 0;
        try
        {
            for (; i < pkValues.length; i++)
            {
                setObjectForStatement(stmt, i + 1, pkValues[i], pkFields[i].getJdbcType().getType());
            }
        }
        catch (SQLException e)
        {
            m_log.error("bindDelete failed for: " + oid.toString() + ", while set value '" +
                    pkValues[i] + "' for column " + pkFields[i].getColumnName());
            throw e;
        }
    }

    /**
     * binds the objects primary key and locking values to the statement, BRJ
     */
    public void bindDelete(PreparedStatement stmt, ClassDescriptor cld, Object obj) throws SQLException
    {
        if (cld.getDeleteProcedure() != null)
        {
            this.bindProcedure(stmt, cld, obj, cld.getDeleteProcedure());
        }
        else
        {
            int index = 1;
            ValueContainer[] values, currentLockingValues;

            currentLockingValues = cld.getCurrentLockingValues(obj);
            // parameters for WHERE-clause pk
            values = getKeyValues(m_broker, cld, obj);
            for (int i = 0; i < values.length; i++)
            {
                setObjectForStatement(stmt, index, values[i].getValue(), values[i].getJdbcType().getType());
                index++;
            }

            // parameters for WHERE-clause locking
            values = currentLockingValues;
            for (int i = 0; i < values.length; i++)
            {
                setObjectForStatement(stmt, index, values[i].getValue(), values[i].getJdbcType().getType());
                index++;
            }
        }
    }

    /**
     * bind attribute and value
     * @param stmt
     * @param index
     * @param attributeOrQuery
     * @param value
     * @param cld
     * @return
     * @throws SQLException
     */
    private int bindStatementValue(PreparedStatement stmt, int index, Object attributeOrQuery, Object value, ClassDescriptor cld)
            throws SQLException
    {
        FieldDescriptor fld = null;
        // if value is a subQuery bind it
        if (value instanceof Query)
        {
            Query subQuery = (Query) value;
            return bindStatement(stmt, subQuery, cld.getRepository().getDescriptorFor(subQuery.getSearchClass()), index);
        }

        // if attribute is a subQuery bind it
        if (attributeOrQuery instanceof Query)
        {
            Query subQuery = (Query) attributeOrQuery;
            bindStatement(stmt, subQuery, cld.getRepository().getDescriptorFor(subQuery.getSearchClass()), index);
        }
        else
        {
            fld = cld.getFieldDescriptorForPath((String) attributeOrQuery);
        }

        if (fld != null)
        {
            // BRJ: use field conversions and platform
            if (value != null)
            {
                m_platform.setObjectForStatement(stmt, index, fld.getFieldConversion().javaToSql(value), fld.getJdbcType().getType());
            }
            else
            {
                m_platform.setNullForStatement(stmt, index, fld.getJdbcType().getType());
            }
        }
        else
        {
            if (value != null)
            {
                stmt.setObject(index, value);
            }
            else
            {
                stmt.setNull(index, Types.NULL);
            }
        }

        return ++index; // increment before return
    }

    /**
     * bind SelectionCriteria
     * @param stmt the PreparedStatement
     * @param index the position of the parameter to bind
     * @param crit the Criteria containing the parameter
     * @param cld the ClassDescriptor
     * @return next index for PreparedStatement
     */
    private int bindStatement(PreparedStatement stmt, int index, SelectionCriteria crit, ClassDescriptor cld) throws SQLException
    {
        return bindStatementValue(stmt, index, crit.getAttribute(), crit.getValue(), cld);
    }

    /**
     * bind NullCriteria
     * @param stmt the PreparedStatement
     * @param index the position of the parameter to bind
     * @param crit the Criteria containing the parameter
     * @return next index for PreparedStatement
     */
    private int bindStatement(PreparedStatement stmt, int index, NullCriteria crit)
    {
        return index;
    }

    /**
     * bind FieldCriteria
     * @param stmt , the PreparedStatement
     * @param index , the position of the parameter to bind
     * @param crit , the Criteria containing the parameter
     * @return next index for PreparedStatement
     */
    private int bindStatement(PreparedStatement stmt, int index, FieldCriteria crit)
    {
        return index;
    }

    /**
     * bind SqlCriteria
     * @param stmt the PreparedStatement
     * @param index the position of the parameter to bind
     * @param crit the Criteria containing the parameter
     * @return next index for PreparedStatement
     */
    private int bindStatement(PreparedStatement stmt, int index, SqlCriteria crit)
    {
        return index;
    }

    /**
     * bind BetweenCriteria
     * @param stmt the PreparedStatement
     * @param index the position of the parameter to bind
     * @param crit the Criteria containing the parameter
     * @param cld the ClassDescriptor
     * @return next index for PreparedStatement
     */
    private int bindStatement(PreparedStatement stmt, int index, BetweenCriteria crit, ClassDescriptor cld) throws SQLException
    {
        index = bindStatementValue(stmt, index, crit.getAttribute(), crit.getValue(), cld);

        return bindStatementValue(stmt, index, crit.getAttribute(), crit.getValue2(), cld);
    }

    /**
     * bind InCriteria
     * @param stmt the PreparedStatement
     * @param index the position of the parameter to bind
     * @param crit the Criteria containing the parameter
     * @param cld the ClassDescriptor
     * @return next index for PreparedStatement
     */
    private int bindStatement(PreparedStatement stmt, int index, InCriteria crit, ClassDescriptor cld) throws SQLException
    {
        if (crit.getValue() instanceof Collection)
        {
            Collection values = (Collection) crit.getValue();
            Iterator iter = values.iterator();

            while (iter.hasNext())
            {
                index = bindStatementValue(stmt, index, crit.getAttribute(), iter.next(), cld);
            }
        }
        else
        {
            index = bindStatementValue(stmt, index, crit.getAttribute(), crit.getValue(), cld);
        }
        return index;
    }

    /**
     * bind ExistsCriteria
     * @param stmt the PreparedStatement
     * @param index the position of the parameter to bind
     * @param crit the Criteria containing the parameter
     * @param cld the ClassDescriptor
     * @return next index for PreparedStatement
     */
    private int bindStatement(PreparedStatement stmt, int index, ExistsCriteria crit, ClassDescriptor cld) throws SQLException
    {
        Query subQuery = (Query) crit.getValue();

        // if query has criteria, bind them
        if (subQuery.getCriteria() != null && !subQuery.getCriteria().isEmpty())
        {
            return bindStatement(stmt, subQuery.getCriteria(), cld.getRepository().getDescriptorFor(subQuery.getSearchClass()), index);

            // otherwise, just ignore it
        }
        else
        {
            return index;
        }
    }

    /**
     * bind a Query based Select Statement
     */
    public int bindStatement(PreparedStatement stmt, Query query, ClassDescriptor cld, int param) throws SQLException
    {
        int result;

        result = bindStatement(stmt, query.getCriteria(), cld, param);
        result = bindStatement(stmt, query.getHavingCriteria(), cld, result);

        return result;
    }

    /**
     * bind a Query based Select Statement
     */
    protected int bindStatement(PreparedStatement stmt, Criteria crit, ClassDescriptor cld, int param) throws SQLException
    {
        if (crit != null)
        {
            Enumeration e = crit.getElements();

            while (e.hasMoreElements())
            {
                Object o = e.nextElement();
                if (o instanceof Criteria)
                {
                    Criteria pc = (Criteria) o;
                    param = bindStatement(stmt, pc, cld, param);
                }
                else
                {
                    SelectionCriteria c = (SelectionCriteria) o;
                    // BRJ : bind once for the criterion's main class
                    param = bindSelectionCriteria(stmt, param, c, cld);

                    // BRJ : and once for each extent
                    for (int i = 0; i < c.getNumberOfExtentsToBind(); i++)
                    {
                        param = bindSelectionCriteria(stmt, param, c, cld);
                    }
                }
            }
        }
        return param;
    }

    /**
     * bind SelectionCriteria
     * @param stmt the PreparedStatement
     * @param index the position of the parameter to bind
     * @param crit the Criteria containing the parameter
     * @param cld the ClassDescriptor
     * @return next index for PreparedStatement
     */
    private int bindSelectionCriteria(PreparedStatement stmt, int index, SelectionCriteria crit, ClassDescriptor cld) throws SQLException
    {
        if (crit instanceof NullCriteria)
            index = bindStatement(stmt, index, (NullCriteria) crit);
        else if (crit instanceof BetweenCriteria)
            index = bindStatement(stmt, index, (BetweenCriteria) crit, cld);
        else if (crit instanceof InCriteria)
            index = bindStatement(stmt, index, (InCriteria) crit, cld);
        else if (crit instanceof SqlCriteria)
            index = bindStatement(stmt, index, (SqlCriteria) crit);
        else if (crit instanceof FieldCriteria)
            index = bindStatement(stmt, index, (FieldCriteria) crit);
        else if (crit instanceof ExistsCriteria)
            index = bindStatement(stmt, index, (ExistsCriteria) crit, cld);
        else
            index = bindStatement(stmt, index, crit, cld);

        return index;
    }

    /**
     * binds the values of the object obj to the statements parameters
     */
    public void bindInsert(PreparedStatement stmt, ClassDescriptor cld, Object obj) throws java.sql.SQLException
    {
        ValueContainer[] values;
        cld.updateLockingValues(obj); // BRJ : provide useful defaults for locking fields

        if (cld.getInsertProcedure() != null)
        {
            this.bindProcedure(stmt, cld, obj, cld.getInsertProcedure());
        }
        else
        {
            values = getAllValues(cld, obj);
            for (int i = 0; i < values.length; i++)
            {
                setObjectForStatement(stmt, i + 1, values[i].getValue(), values[i].getJdbcType().getType());
            }
        }
    }

    /**
     * Binds the Identities Primary key values to the statement.
     */
    public void bindSelect(PreparedStatement stmt, Identity oid, ClassDescriptor cld, boolean callableStmt) throws SQLException
    {
        ValueContainer[] values = null;
        int i = 0;
        int j = 0;

        if (cld == null)
        {
            cld = m_broker.getClassDescriptor(oid.getObjectsRealClass());
        }
        try
        {
            if(callableStmt)
            {
                // First argument is the result set
                m_platform.registerOutResultSet((CallableStatement) stmt, 1);
                j++;
            }

            values = getKeyValues(m_broker, cld, oid);
            for (/*void*/; i < values.length; i++, j++)
            {
                setObjectForStatement(stmt, j + 1, values[i].getValue(), values[i].getJdbcType().getType());
            }
        }
        catch (SQLException e)
        {
            m_log.error("bindSelect failed for: " + oid.toString() + ", PK: " + i + ", value: " + values[i]);
            throw e;
        }
    }

    /**
     * binds the values of the object obj to the statements parameters
     */
    public void bindUpdate(PreparedStatement stmt, ClassDescriptor cld, Object obj) throws java.sql.SQLException
    {
        if (cld.getUpdateProcedure() != null)
        {
            this.bindProcedure(stmt, cld, obj, cld.getUpdateProcedure());
        }
        else
        {
            int index = 1;
            ValueContainer[] values, valuesSnapshot;
            // first take a snapshot of current locking values
            valuesSnapshot = cld.getCurrentLockingValues(obj);
            cld.updateLockingValues(obj); // BRJ
            values = getNonKeyValues(m_broker, cld, obj);

            // parameters for SET-clause
            for (int i = 0; i < values.length; i++)
            {
                setObjectForStatement(stmt, index, values[i].getValue(), values[i].getJdbcType().getType());
                index++;
            }
            // parameters for WHERE-clause pk
            values = getKeyValues(m_broker, cld, obj);
            for (int i = 0; i < values.length; i++)
            {
                setObjectForStatement(stmt, index, values[i].getValue(), values[i].getJdbcType().getType());
                index++;
            }
            // parameters for WHERE-clause locking
            // take old locking values
            values = valuesSnapshot;
            for (int i = 0; i < values.length; i++)
            {
                setObjectForStatement(stmt, index, values[i].getValue(), values[i].getJdbcType().getType());
                index++;
            }
        }
    }

    /**
     * binds the given array of values (if not null) starting from the given
     * parameter index
     * @return the next parameter index
     */
    public int bindValues(PreparedStatement stmt, ValueContainer[] values, int index) throws SQLException
    {
        if (values != null)
        {
            for (int i = 0; i < values.length; i++)
            {
                setObjectForStatement(stmt, index, values[i].getValue(), values[i].getJdbcType().getType());
                index++;
            }
        }
        return index;
    }

    /**
     * return a prepared DELETE Statement fitting for the given ClassDescriptor
     */
    public PreparedStatement getDeleteStatement(ClassDescriptor cld) throws PersistenceBrokerSQLException, PersistenceBrokerException
    {
        try
        {
            return cld.getStatementsForClass(m_conMan).getDeleteStmt(m_conMan.getConnection());
        }
        catch (SQLException e)
        {
            throw new PersistenceBrokerSQLException("Could not build statement ask for", e);
        }
        catch (LookupException e)
        {
            throw new PersistenceBrokerException("Used ConnectionManager instance could not obtain a connection", e);
        }
    }

    /**
     * return a generic Statement for the given ClassDescriptor.
     * Never use this method for UPDATE/INSERT/DELETE if you want to use the batch mode.
     */
    public Statement getGenericStatement(ClassDescriptor cds, boolean scrollable) throws PersistenceBrokerException
    {
        try
        {
            return cds.getStatementsForClass(m_conMan).getGenericStmt(m_conMan.getConnection(), scrollable);
        }
        catch (LookupException e)
        {
            throw new PersistenceBrokerException("Used ConnectionManager instance could not obtain a connection", e);
        }
    }

    /**
     * return a prepared Insert Statement fitting for the given ClassDescriptor
     */
    public PreparedStatement getInsertStatement(ClassDescriptor cds) throws PersistenceBrokerSQLException, PersistenceBrokerException
    {
        try
        {
            return cds.getStatementsForClass(m_conMan).getInsertStmt(m_conMan.getConnection());
        }
        catch (SQLException e)
        {
            throw new PersistenceBrokerSQLException("Could not build statement ask for", e);
        }
        catch (LookupException e)
        {
            throw new PersistenceBrokerException("Used ConnectionManager instance could not obtain a connection", e);
        }
    }

    /**
     * return a generic Statement for the given ClassDescriptor
     */
    public PreparedStatement getPreparedStatement(ClassDescriptor cds, String sql,
                                                  boolean scrollable, int explicitFetchSizeHint, boolean callableStmt)
            throws PersistenceBrokerException
    {
        try
        {
            return cds.getStatementsForClass(m_conMan).getPreparedStmt(m_conMan.getConnection(), sql, scrollable, explicitFetchSizeHint, callableStmt);
        }
        catch (LookupException e)
        {
            throw new PersistenceBrokerException("Used ConnectionManager instance could not obtain a connection", e);
        }
    }

    /**
     * return a prepared Select Statement for the given ClassDescriptor
     */
    public PreparedStatement getSelectByPKStatement(ClassDescriptor cds) throws PersistenceBrokerSQLException, PersistenceBrokerException
    {
        try
        {
            return cds.getStatementsForClass(m_conMan).getSelectByPKStmt(m_conMan.getConnection());
        }
        catch (SQLException e)
        {
            throw new PersistenceBrokerSQLException("Could not build statement ask for", e);
        }
        catch (LookupException e)
        {
            throw new PersistenceBrokerException("Used ConnectionManager instance could not obtain a connection", e);
        }
    }

    /**
     * return a prepared Update Statement fitting to the given ClassDescriptor
     */
    public PreparedStatement getUpdateStatement(ClassDescriptor cds) throws PersistenceBrokerSQLException, PersistenceBrokerException
    {
        try
        {
            return cds.getStatementsForClass(m_conMan).getUpdateStmt(m_conMan.getConnection());
        }
        catch (SQLException e)
        {
            throw new PersistenceBrokerSQLException("Could not build statement ask for", e);
        }
        catch (LookupException e)
        {
            throw new PersistenceBrokerException("Used ConnectionManager instance could not obtain a connection", e);
        }
    }

    /**
     * returns an array containing values for all the Objects attribute
     * @throws PersistenceBrokerException if there is an erros accessing obj field values
     */
    protected ValueContainer[] getAllValues(ClassDescriptor cld, Object obj) throws PersistenceBrokerException
    {
        return m_broker.serviceBrokerHelper().getAllRwValues(cld, obj);
    }

    /**
     * returns an Array with an Objects PK VALUES
     * @throws PersistenceBrokerException if there is an erros accessing o field values
     */
    protected ValueContainer[] getKeyValues(PersistenceBroker broker, ClassDescriptor cld, Object obj) throws PersistenceBrokerException
    {
        return broker.serviceBrokerHelper().getKeyValues(cld, obj);
    }

    /**
     * returns an Array with an Identities PK VALUES
     * @throws PersistenceBrokerException if there is an erros accessing o field values
     */
    protected ValueContainer[] getKeyValues(PersistenceBroker broker, ClassDescriptor cld, Identity oid) throws PersistenceBrokerException
    {
        return broker.serviceBrokerHelper().getKeyValues(cld, oid);
    }

    /**
     * returns an Array with an Objects NON-PK VALUES
     * @throws PersistenceBrokerException if there is an erros accessing o field values
     */
    protected ValueContainer[] getNonKeyValues(PersistenceBroker broker, ClassDescriptor cld, Object obj) throws PersistenceBrokerException
    {
        return broker.serviceBrokerHelper().getNonKeyRwValues(cld, obj);
    }

    /**
     * Bind a prepared statment that represents a call to a procedure or
     * user-defined function.
     *
     * @param stmt the statement to bind.
     * @param cld the class descriptor of the object that triggered the
     *        invocation of the procedure or user-defined function.
     * @param obj the object that triggered the invocation of the procedure
     *        or user-defined function.
     * @param proc the procedure descriptor that provides information about
     *        the arguments that shoudl be passed to the procedure or
     *        user-defined function
     */
    private void bindProcedure(PreparedStatement stmt, ClassDescriptor cld, Object obj, ProcedureDescriptor proc)
            throws SQLException
    {
        int valueSub = 0;

        // Figure out if we are using a callable statement.  If we are, then we
        // will need to register one or more output parameters.
        CallableStatement callable = null;
        try
        {
            callable = (CallableStatement) stmt;
        }
        catch(Exception e)
        {
            m_log.error("Error while bind values for class '" + (cld != null ? cld.getClassNameOfObject() : null)
                    + "', using stored procedure: "+ proc, e);
            if(e instanceof SQLException)
            {
                throw (SQLException) e;
            }
            else
            {
                throw new PersistenceBrokerException("Unexpected error while bind values for class '"
                        + (cld != null ? cld.getClassNameOfObject() : null) + "', using stored procedure: "+ proc);
            }
        }

        // If we have a return value, then register it.
        if ((proc.hasReturnValue()) && (callable != null))
        {
            int jdbcType = proc.getReturnValueFieldRef().getJdbcType().getType();
            m_platform.setNullForStatement(stmt, valueSub + 1, jdbcType);
            callable.registerOutParameter(valueSub + 1, jdbcType);
            valueSub++;
        }

        // Process all of the arguments.
        Iterator iterator = proc.getArguments().iterator();
        while (iterator.hasNext())
        {
            ArgumentDescriptor arg = (ArgumentDescriptor) iterator.next();
            Object val = arg.getValue(obj);
            int jdbcType = arg.getJdbcType();
            setObjectForStatement(stmt, valueSub + 1, val, jdbcType);
            if ((arg.getIsReturnedByProcedure()) && (callable != null))
            {
                callable.registerOutParameter(valueSub + 1, jdbcType);
            }
            valueSub++;
        }
    }

    /**
     * Sets object for statement at specific index, adhering to platform- and null-rules.
     * @param stmt the statement
     * @param index the current parameter index
     * @param value the value to set
     * @param sqlType the JDBC SQL-type of the value
     * @throws SQLException on platform error
     */
    private void setObjectForStatement(PreparedStatement stmt, int index, Object value, int sqlType)
            throws SQLException
    {
        if (value == null)
        {
            m_platform.setNullForStatement(stmt, index, sqlType);
        }
        else
        {
            m_platform.setObjectForStatement(stmt, index, value, sqlType);
        }
    }

}
