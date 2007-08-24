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

import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import org.apache.commons.collections.map.ReferenceIdentityMap;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ProcedureDescriptor;
import org.apache.ojb.broker.platforms.Platform;
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
 * This Class is responsible for building sql statements
 * Objects fields and their repective values are accessed by Java reflection
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @author <a href="mailto:rgallagh@bellsouth.net">Ron Gallagher</a>
 * @author <a href="mailto:rburt3@mchsi.com">Randall Burt</a>
 * @version $Id: SqlGeneratorDefaultImpl.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class SqlGeneratorDefaultImpl implements SqlGenerator
{
    private Logger logger = LoggerFactory.getLogger(SqlGeneratorDefaultImpl.class);
    private Platform m_platform;
    /*
    arminw:
    TODO: In ClassDescriptor we need support for "field change event" listener if we allow
    to change metadata at runtime.
    Further on we have to deal with weak references to allow GC of outdated Metadata classes
    (key=cld of map have to be a weak reference and the metadata used in the SqlStatement classes too,
    because inner class SqlForClass indirectly refer the key=cld in some cases and SqlStatement
    implementation classes have references to metadata classes too).

    Field changes are not reflected in this implementation!
    */
    /** Cache for {@link SqlForClass} instances, keyed per class descriptor. */
    private Map sqlForClass = new ReferenceIdentityMap(ReferenceIdentityMap.WEAK, ReferenceIdentityMap.HARD);

    public SqlGeneratorDefaultImpl(Platform platform)
    {
        this.m_platform = platform;
    }

    /**
     * generate a prepared DELETE-Statement for the Class
     * described by cld.
     * @param cld the ClassDescriptor
     */
    public SqlStatement getPreparedDeleteStatement(ClassDescriptor cld)
    {
        SqlForClass sfc = getSqlForClass(cld);
        SqlStatement sql = sfc.getDeleteSql();
        if(sql == null)
        {
            ProcedureDescriptor pd = cld.getDeleteProcedure();

            if(pd == null)
            {
                sql = new SqlDeleteByPkStatement(cld, logger);
            }
            else
            {
                sql = new SqlProcedureStatement(pd, logger);
            }
            // set the sql string
            sfc.setDeleteSql(sql);

            if(logger.isDebugEnabled())
            {
                logger.debug("SQL:" + sql.getStatement());
            }
        }
        return sql;
    }

    /**
     * generate a prepared INSERT-Statement for the Class
     * described by cld.
     *
     * @param cld the ClassDescriptor
     */
    public SqlStatement getPreparedInsertStatement(ClassDescriptor cld)
    {
        SqlStatement sql;
        SqlForClass sfc = getSqlForClass(cld);
        sql = sfc.getInsertSql();
        if(sql == null)
        {
            ProcedureDescriptor pd = cld.getInsertProcedure();

            if(pd == null)
            {
                sql = new SqlInsertStatement(cld, logger);
            }
            else
            {
                sql = new SqlProcedureStatement(pd, logger);
            }
            // set the sql string
            sfc.setInsertSql(sql);

            if(logger.isDebugEnabled())
            {
                logger.debug("SQL:" + sql.getStatement());
            }
        }
        return sql;
    }

    /**
     * generate a prepared SELECT-Statement for the Class
     * described by cld
     * @param cld the ClassDescriptor
     */
    public SelectStatement getPreparedSelectByPkStatement(ClassDescriptor cld)
    {
        SelectStatement sql;
        SqlForClass sfc = getSqlForClass(cld);
        sql = sfc.getSelectByPKSql();
        if(sql == null)
        {
            sql = new SqlSelectByPkStatement(m_platform, cld, logger);

            // set the sql string
            sfc.setSelectByPKSql(sql);

            if(logger.isDebugEnabled())
            {
                logger.debug("SQL:" + sql.getStatement());
            }
        }
        return sql;
    }

    public SqlStatement getPreparedExistsStatement(ClassDescriptor cld)
    {
        SqlStatement sql;
        SqlForClass sfc = getSqlForClass(cld);
        sql = sfc.getSelectExists();
        if(sql == null)
        {
            // TODO: Should we support a procedure call for this too??
            sql = new SqlExistStatement(cld, logger);
            // set the sql string
            sfc.setSelectExists(sql);
            if(logger.isDebugEnabled())
            {
                logger.debug("SQL:" + sql.getStatement());
            }
        }
        return sql;
    }

    /**
     * generate a select-Statement according to query
     *
     * @param query the Query
     * @param cld the ClassDescriptor
     */
    public SelectStatement getPreparedSelectStatement(Query query, ClassDescriptor cld)
    {
        SelectStatement sql = new SqlSelectStatement(m_platform, cld, query, logger);
        if (logger.isDebugEnabled())
        {
            logger.debug("SQL:" + sql.getStatement());
        }
        return sql;
    }

    /**
     * generate a prepared UPDATE-Statement for the Class
     * described by cld
     * @param cld the ClassDescriptor
     */
    public SqlStatement getPreparedUpdateStatement(ClassDescriptor cld)
    {
        SqlForClass sfc = getSqlForClass(cld);
        SqlStatement result = sfc.getUpdateSql();
        if(result == null)
        {
            ProcedureDescriptor pd = cld.getUpdateProcedure();

            if(pd == null)
            {
                result = new SqlUpdateStatement(cld, logger);
            }
            else
            {
                result = new SqlProcedureStatement(pd, logger);
            }
            // set the sql string
            sfc.setUpdateSql(result);

            if(logger.isDebugEnabled())
            {
                logger.debug("SQL:" + result.getStatement());
            }
        }
        return result;
    }

    /**
     * generate an INSERT-Statement for M:N indirection table
     *
     * @param table
     * @param pkColumns1
     * @param pkColumns2
     */
    public String getInsertMNStatement(String table, String[] pkColumns1, String[] pkColumns2)
    {
        SqlStatement sql;
        String result;

        String[] cols = new String[pkColumns1.length + pkColumns2.length];
        System.arraycopy(pkColumns1, 0, cols, 0, pkColumns1.length);
        System.arraycopy(pkColumns2, 0, cols, pkColumns1.length, pkColumns2.length);

        sql = new SqlInsertMNStatement(table, cols, logger);
        result = sql.getStatement();

        if (logger.isDebugEnabled())
        {
            logger.debug("SQL:" + result);
        }
        return result;
    }

    /**
     * generate a SELECT-Statement for M:N indirection table
     *
     * @param table the indirection table
     * @param selectColumns selected columns
     * @param columns for where
     */
    public String getSelectMNStatement(String table, String[] selectColumns, String[] columns)
    {
        SqlStatement sql;
        String result;

        sql = new SqlSelectMNStatement(table, selectColumns, columns, logger);
        result = sql.getStatement();

        if (logger.isDebugEnabled())
        {
            logger.debug("SQL:" + result);
        }
        return result;
    }

    /**
     * generate a DELETE-Statement for M:N indirection table
     *
     * @param table
     * @param pkColumns1
     * @param pkColumns2
     */
    public String getDeleteMNStatement(String table, String[] pkColumns1, String[] pkColumns2)
    {
        SqlStatement sql;
        String result;
        String[] cols;

        if (pkColumns2 == null)
        {
            cols = pkColumns1;
        }
        else
        {
            cols = new String[pkColumns1.length + pkColumns2.length];
            System.arraycopy(pkColumns1, 0, cols, 0, pkColumns1.length);
            System.arraycopy(pkColumns2, 0, cols, pkColumns1.length, pkColumns2.length);
        }

        sql = new SqlDeleteMNStatement(table, cols, logger);
        result = sql.getStatement();

        if (logger.isDebugEnabled())
        {
            logger.debug("SQL:" + result);
        }
        return result;
    }

    /**
     * generate a select-Statement according to query
     * @param query the Query
     * @param cld the ClassDescriptor
     */
    public SelectStatement getSelectStatementDep(Query query, ClassDescriptor cld)
    {
        // TODO: Why do we need this method?
        return getPreparedSelectStatement(query, cld);
    }

    /**
     * @param crit Selection criteria
     *
     * 26/06/99 Change statement to a StringBuffer for efficiency
     */
    public String asSQLStatement(Criteria crit, ClassDescriptor cld)
    {
        Enumeration e = crit.getElements();
        StringBuffer statement = new StringBuffer();
        while (e.hasMoreElements())
        {
            Object o = e.nextElement();
            if (o instanceof Criteria)
            {
                String addAtStart;
                String addAtEnd;
                Criteria pc = (Criteria) o;
                // need to add parenthesises?
                if (pc.isEmbraced())
                {
                    addAtStart = " (";
                    addAtEnd = ") ";
                }
                else
                {
                    addAtStart = "";
                    addAtEnd = "";
                }

                switch (pc.getType())
                {
                    case (Criteria.OR) :
                        {
                            statement.append(" OR ").append(addAtStart);
                            statement.append(asSQLStatement(pc, cld));
                            statement.append(addAtEnd);
                            break;
                        }
                    case (Criteria.AND) :
                        {
                            statement.insert(0, "( ");
                            statement.append(") ");
                            statement.append(" AND ").append(addAtStart);
                            statement.append(asSQLStatement(pc, cld));
                            statement.append(addAtEnd);
                            break;
                        }
                }
            }
            else
            {
                SelectionCriteria c = (SelectionCriteria) o;
                if (statement.length() == 0)
                {
                    statement.append(asSQLClause(c, cld));
                }
                else
                {
                    statement.insert(0, "(");
                    statement.append(") ");
                    statement.append(" AND ");
                    statement.append(asSQLClause(c, cld));
                }
            }
        } // while
        if (statement.length() == 0)
        {
            return null;
        }
        return statement.toString();
    }

    /**
     * Answer the SQL-Clause for a SelectionCriteria
     *
     * @param c SelectionCriteria
     * @param cld ClassDescriptor
     */
    protected String asSQLClause(SelectionCriteria c, ClassDescriptor cld)
    {
        if (c instanceof FieldCriteria)
            return toSQLClause((FieldCriteria) c, cld);

        if (c instanceof NullCriteria)
            return toSQLClause((NullCriteria) c);

        if (c instanceof BetweenCriteria)
            return toSQLClause((BetweenCriteria) c, cld);

        if (c instanceof InCriteria)
            return toSQLClause((InCriteria) c);

        if (c instanceof SqlCriteria)
            return toSQLClause((SqlCriteria) c);

        if (c instanceof ExistsCriteria)
            return toSQLClause((ExistsCriteria) c, cld);

        return toSQLClause(c, cld);
    }

    private String toSqlClause(Object attributeOrQuery, ClassDescriptor cld)
    {
        String result;

        if (attributeOrQuery instanceof Query)
        {
            Query q = (Query) attributeOrQuery;
            result = getPreparedSelectStatement(q, cld.getRepository().getDescriptorFor(q.getSearchClass()))
                    .getStatement();
        }
        else
        {
           result = (String)attributeOrQuery;
        }

        return result;
    }

    /**
     * Answer the SQL-Clause for a NullCriteria
     *
     * @param c NullCriteria
     */
    private String toSQLClause(NullCriteria c)
    {
        String colName = (String)c.getAttribute();
        return colName + c.getClause();
    }

    /**
     * Answer the SQL-Clause for a FieldCriteria
     *
     * @param c FieldCriteria
     * @param cld ClassDescriptor
     */
    private String toSQLClause(FieldCriteria c, ClassDescriptor cld)
    {
        String colName = toSqlClause(c.getAttribute(), cld);
        return colName + c.getClause() + c.getValue();
    }

    /**
     * Answer the SQL-Clause for a BetweenCriteria
     *
     * @param c BetweenCriteria
     * @param cld ClassDescriptor
     */
    private String toSQLClause(BetweenCriteria c, ClassDescriptor cld)
    {
        String colName = toSqlClause(c.getAttribute(), cld);
        return colName + c.getClause() + " ? AND ? ";
    }

    /**
     * Answer the SQL-Clause for an InCriteria
     *
     * @param c SelectionCriteria
     */
    private String toSQLClause(InCriteria c)
    {
        StringBuffer buf = new StringBuffer();
        Collection values = (Collection) c.getValue();
        int size = values.size();

        buf.append(c.getAttribute());
        buf.append(c.getClause());
        buf.append("(");
        for (int i = 0; i < size - 1; i++)
        {
            buf.append("?,");
        }
        buf.append("?)");
        return buf.toString();
    }

    /**
     * Answer the SQL-Clause for a SelectionCriteria
     *
     * @param c SelectionCriteria
     * @param cld ClassDescriptor
     */
    private String toSQLClause(SelectionCriteria c, ClassDescriptor cld)
    {
        String colName = toSqlClause(c.getAttribute(), cld);
        return colName + c.getClause() + " ? ";
    }

    /**
     * Answer the SQL-Clause for a SqlCriteria
     *
     * @param c SqlCriteria
     */
    private String toSQLClause(SqlCriteria c)
    {
        return c.getClause();
    }

    /**
     * Answer the SQL-Clause for an ExistsCriteria
     *
     * @param c ExistsCriteria
     * @param cld ClassDescriptor
     */
    private String toSQLClause(ExistsCriteria c, ClassDescriptor cld)
    {
        StringBuffer buf = new StringBuffer();
        Query subQuery = (Query) c.getValue();

        buf.append(c.getClause());
        buf.append(" (");

        // If it's a proper call
        if (cld != null)
        {
            buf.append(
                getPreparedSelectStatement(
                    subQuery,
                    cld.getRepository().getDescriptorFor(subQuery.getSearchClass())));

            // Otherwise it's most likely a call to toString()
        }
        else
        {
            buf.append(subQuery);
        }

        buf.append(")");
        return buf.toString();
    }

    /**
     * generate a prepared DELETE-Statement according to query
     * @param query the Query
     * @param cld the ClassDescriptor
     */
    public SqlStatement getPreparedDeleteStatement(Query query, ClassDescriptor cld)
    {
        return new SqlDeleteByQuery(m_platform, cld, query, logger);
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.accesslayer.sql.SqlGenerator#getPlatform()
     */
    public Platform getPlatform()
    {
        return m_platform;
    }

    /**
     * Returns the {@link SqlForClass} instance for
     * the given class descriptor.
     *
     * @param cld The class descriptor.
     * @return The {@link SqlForClass}.
     */
    protected SqlForClass getSqlForClass(ClassDescriptor cld)
    {
        SqlForClass result = (SqlForClass) sqlForClass.get(cld);
        if(result == null)
        {
            result = newInstanceSqlForClass();
            sqlForClass.put(cld, result);
        }
        return result;
    }

    /**
     * User who want to extend this implementation can override this method to use
     * their own (extended) version of
     * {@link org.apache.ojb.broker.accesslayer.sql.SqlGeneratorDefaultImpl.SqlForClass}.
     *
     * @return A new instance.
     */
    protected SqlForClass newInstanceSqlForClass()
    {
        return new SqlForClass();
    }

    //===================================================================
    // inner class
    //===================================================================

    /**
     * This class serves as a cache for sql-Statements
     * used for persistence operations.
     */
    public static class SqlForClass
    {
        private SqlStatement deleteSql;
        private SqlStatement insertSql;
        private SqlStatement updateSql;
        private SelectStatement selectByPKSql;
        private SqlStatement selectExists;

        public SqlStatement getDeleteSql()
        {
            return deleteSql;
        }

        public void setDeleteSql(SqlStatement deleteSql)
        {
            this.deleteSql = deleteSql;
        }

        public SqlStatement getInsertSql()
        {
            return insertSql;
        }

        public void setInsertSql(SqlStatement insertSql)
        {
            this.insertSql = insertSql;
        }

        public SqlStatement getUpdateSql()
        {
            return updateSql;
        }

        public void setUpdateSql(SqlStatement updateSql)
        {
            this.updateSql = updateSql;
        }

        public SelectStatement getSelectByPKSql()
        {
            return selectByPKSql;
        }

        public void setSelectByPKSql(SelectStatement selectByPKSql)
        {
            this.selectByPKSql = selectByPKSql;
        }

        public SqlStatement getSelectExists()
        {
            return selectExists;
        }

        public void setSelectExists(SqlStatement selectExists)
        {
            this.selectExists = selectExists;
        }
    }

}
