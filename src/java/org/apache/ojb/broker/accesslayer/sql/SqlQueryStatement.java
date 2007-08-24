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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ojb.broker.PersistenceBrokerSQLException;
import org.apache.ojb.broker.accesslayer.JoinSyntaxTypes;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.FieldHelper;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.SuperReferenceDescriptor;
import org.apache.ojb.broker.platforms.Platform;
import org.apache.ojb.broker.query.BetweenCriteria;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.ExistsCriteria;
import org.apache.ojb.broker.query.FieldCriteria;
import org.apache.ojb.broker.query.InCriteria;
import org.apache.ojb.broker.query.LikeCriteria;
import org.apache.ojb.broker.query.MtoNQuery;
import org.apache.ojb.broker.query.NullCriteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryBySQL;
import org.apache.ojb.broker.query.SelectionCriteria;
import org.apache.ojb.broker.query.SqlCriteria;
import org.apache.ojb.broker.query.UserAlias;
import org.apache.ojb.broker.util.SqlHelper;
import org.apache.ojb.broker.util.SqlHelper.PathInfo;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Model a Statement based on Query.
 *
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: SqlQueryStatement.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public abstract class SqlQueryStatement implements SqlStatement, JoinSyntaxTypes
{
    private static final String ALIAS_SEPARATOR = ".";
    private static final String M_N_ALIAS = "M_N";
    private String sql;
    
    private SqlQueryStatement m_parentStatement;
    /** the logger */
    private Logger m_logger;
    /** the target table of the query */
    private TableAlias m_root;
    /** the search table of the query */
    private TableAlias m_search;
    /** the query */
    private QueryByCriteria m_query;
    /** the mapping of paths to TableAliases. the key is built using the path and the path class hints. */
    private HashMap m_pathToAlias = new HashMap();
    /** the mapping of ClassDescriptor to TableAliases */
    private HashMap m_cldToAlias = new HashMap();
    /** maps trees of joins to criteria */
    private HashMap m_joinTreeToCriteria = new HashMap();

    private Platform m_platform;
    private ClassDescriptor m_baseCld;
    private ClassDescriptor m_searchCld;

    private int m_aliasCount = 0;
    protected HashMap m_attrToFld = new HashMap();   //attribute -> FieldDescriptor

    /**
     * Constructor for SqlCriteriaStatement.
     *
     * @param pf the Platform
     * @param cld the ClassDescriptor
     * @param query the Query
     * @param logger the Logger
     */
    public SqlQueryStatement(Platform pf, ClassDescriptor cld, Query query, Logger logger)
    {
        this(null, pf, cld, query, logger);
    }

    /**
     * Constructor for SqlCriteriaStatement.
     *
     * @param parent the Parent Query
     * @param pf the Platform
     * @param cld the ClassDescriptor
     * @param query the Query
     * @param logger the Logger
     */
    public SqlQueryStatement(SqlQueryStatement parent, Platform pf, ClassDescriptor cld, Query query, Logger logger)
    {
        m_logger = logger != null ? logger : LoggerFactory.getLogger(SqlQueryStatement.class);
        m_parentStatement = parent;
        m_query = (QueryByCriteria) query;
        m_platform = pf;
        m_searchCld = cld;

        if ((m_query == null) || (m_query.getBaseClass() == m_query.getSearchClass()))
        {
            m_baseCld = m_searchCld;
        }
        else
        {
            m_baseCld = cld.getRepository().getDescriptorFor(query.getBaseClass());
        }

        m_root = createTableAlias(m_baseCld, null, "");
        
        // BRJ: create a special alias for the indirection table
        if (m_query instanceof MtoNQuery)
        {
            MtoNQuery mnQuery = (MtoNQuery)m_query; 
            TableAlias mnAlias = new TableAlias(mnQuery.getIndirectionTable(), M_N_ALIAS);
            setTableAliasForPath(mnQuery.getIndirectionTable(), null, mnAlias);        
        }

        if (m_searchCld == m_baseCld)
        {
            m_search = m_root;
        }
        else
        {
			m_search = getTableAlias(m_query.getObjectProjectionAttribute(), false, null, null, m_query.getPathClasses());
        }

        // Walk the super reference-descriptor
        buildSuperJoinTree(m_root, m_baseCld, "" ,false);

        buildMultiJoinTree(m_root, m_baseCld, "", true);

        // In some cases it is necessary to split the query criteria
        // and then to generate UNION of several SELECTs
        // We build the joinTreeToCriteria mapping,
        if (query != null)
        {
            splitCriteria();
        }
    }

    protected ClassDescriptor getBaseClassDescriptor()
    {
        return m_baseCld;
    }

    protected ClassDescriptor getSearchClassDescriptor()
    {
        return m_searchCld;
    }

	/**
	 * Return the TableAlias and the PathInfo for an Attribute name<br>
	 * field names in functions (ie: sum(name) ) are tried to resolve ie: name
	 * from FIELDDESCRIPTOR , UPPER(name_test) from Criteria<br>
	 * also resolve pathExpression adress.city or owner.konti.saldo
	 * @param attr
	 * @param useOuterJoins
	 * @param aUserAlias
	 * @param pathClasses
	 * @return ColumnInfo
	 */
	protected AttributeInfo getAttributeInfo(String attr, boolean useOuterJoins, UserAlias aUserAlias, Map pathClasses)
	{
		AttributeInfo result = new AttributeInfo();
		TableAlias tableAlias;
		SqlHelper.PathInfo pathInfo = SqlHelper.splitPath(attr);
		String colName = pathInfo.column;
		int sp;

		// BRJ:
		// check if we refer to an attribute in the parent query
		// this prefix is temporary !
		if (colName.startsWith(Criteria.PARENT_QUERY_PREFIX) && m_parentStatement != null)
		{
			String[] fieldNameRef = {colName.substring(Criteria.PARENT_QUERY_PREFIX.length())};
			return m_parentStatement.getAttributeInfo(fieldNameRef[0], useOuterJoins, aUserAlias, pathClasses);
		}

		sp = colName.lastIndexOf(".");
		if (sp == -1)
		{
			tableAlias = getRoot();
		}
		else
		{
			String pathName = colName.substring(0, sp);
			String[] fieldNameRef = {colName.substring(sp + 1)};

			tableAlias = getTableAlias(pathName, useOuterJoins, aUserAlias, fieldNameRef, pathClasses);
			/**
			 * if we have not found an alias by the pathName or
			 * aliasName (if given), try again because pathName
			 * may be an aliasname. it can be only and only if it is not
			 * a path, which means there may be no path separators (,)
			 * in the pathName.
			 */
			if ((tableAlias == null) && (colName.lastIndexOf(".") == -1))
			{
				/**
				 * pathName might be an alias, so check this first
				 */
				tableAlias = getTableAlias(pathName, useOuterJoins, new UserAlias(pathName, pathName, pathName), null, pathClasses);
			}

			if (tableAlias != null)
			{
				// correct column name to match the alias
				// productGroup.groupName -> groupName
				pathInfo.column = fieldNameRef[0];
			}
		}

		result.tableAlias = tableAlias;
		result.pathInfo = pathInfo;
		return result;
	}

    /**
     * Answer the column name for alias and path info<br>
     * if translate try to convert attribute name into column name otherwise use attribute name<br>
     * if a FieldDescriptor is found for the attribute name the column name is taken from
     * there prefixed with the alias (firstname -> A0.F_NAME).
     */
    protected String getColName(TableAlias aTableAlias, PathInfo aPathInfo, boolean translate)
    {
        String result = null;

        // no translation required, use attribute name
        if (!translate)
        {
            return aPathInfo.column;
        }

        // BRJ: special alias for the indirection table has no ClassDescriptor 
        if (aTableAlias.cld == null && M_N_ALIAS.equals(aTableAlias.alias))
        {
            return getIndirectionTableColName(aTableAlias, aPathInfo.path);
        }

        // translate attribute name into column name
        FieldDescriptor fld = getFieldDescriptor(aTableAlias, aPathInfo);

        if (fld != null)
        {
            m_attrToFld.put(aPathInfo.path, fld);

            // added to suport the super reference descriptor
            if (!fld.getClassDescriptor().getFullTableName().equals(aTableAlias.table) && aTableAlias.hasJoins())
            {
                Iterator itr = aTableAlias.joins.iterator();
                while (itr.hasNext())
                {
                    Join join = (Join) itr.next();
                    if (join.right.table.equals(fld.getClassDescriptor().getFullTableName()))
                    {
                        result = join.right.alias + "." + fld.getColumnName();
                        break;
                    }
                }

                if (result == null)
                {
                    result = aPathInfo.column;
                }
            }
            else
            {
                result = aTableAlias.alias + "." + fld.getColumnName();
            }
        }
        else if ("*".equals(aPathInfo.column))
        {
            result = aPathInfo.column;
        }
        else
        {
            // throw new IllegalArgumentException("No Field found for : " + aPathInfo.column);
            result = aPathInfo.column;
        }

        return result;
    }

    /**
     * Add the Column to the StringBuffer <br>
     *
     * @param aTableAlias
     * @param aPathInfo
     * @param translate flag to indicate translation of pathInfo
     * @param buf
     * @return true if appended
     */
    protected boolean appendColName(TableAlias aTableAlias, PathInfo aPathInfo, boolean translate, StringBuffer buf)
    {
        String prefix = aPathInfo.prefix;
        String suffix = aPathInfo.suffix;
        String colName = getColName(aTableAlias, aPathInfo, translate);

        if (prefix != null) // rebuild function contains (
        {
            buf.append(prefix);
        }

        buf.append(colName);

        if (suffix != null) // rebuild function
        {
            buf.append(suffix);
        }

        return true;
    }

    /**
     * Get the FieldDescriptor for the PathInfo
     *
     * @param aTableAlias
     * @param aPathInfo
     * @return FieldDescriptor
     */
    protected FieldDescriptor getFieldDescriptor(TableAlias aTableAlias, PathInfo aPathInfo)
    {
        FieldDescriptor fld = null;
        String colName = aPathInfo.column;

        if (aTableAlias != null)
        {
            fld = aTableAlias.cld.getFieldDescriptorByName(colName);
            if (fld == null)
            {
                ObjectReferenceDescriptor ord = aTableAlias.cld.getObjectReferenceDescriptorByName(colName);
                if (ord != null)
                {
                    fld = getFldFromReference(aTableAlias, ord);
                }
                else
                {
                    fld = getFldFromJoin(aTableAlias, colName);
                }
            }
        }

        return fld;
    }

    /**
     * Get FieldDescriptor from joined superclass.
     */
    private FieldDescriptor getFldFromJoin(TableAlias aTableAlias, String aColName)
    {
        FieldDescriptor fld = null;

        // Search Join Structure for attribute
        if (aTableAlias.joins != null)
        {
            Iterator itr = aTableAlias.joins.iterator();
            while (itr.hasNext())
            {
                Join join = (Join) itr.next();
                ClassDescriptor cld = join.right.cld;

                if (cld != null)
                {
                    fld = cld.getFieldDescriptorByName(aColName);
                    if (fld != null)
                    {
                        break;
                    }

                }
            }
        }
        return fld;
    }

    /**
     * Get FieldDescriptor from Reference
     */
    private FieldDescriptor getFldFromReference(TableAlias aTableAlias, ObjectReferenceDescriptor anOrd)
    {
        FieldDescriptor fld = null;

        if (aTableAlias == getRoot())
        {
            // no path expression
            FieldDescriptor[] fk = anOrd.getForeignKeyFieldDescriptors(aTableAlias.cld);
            if (fk.length > 0)
            {
                fld = fk[0];
            }
        }
        else
        {
            // attribute with path expression
            /**
             * MBAIRD
             * potentially people are referring to objects, not to the object's primary key, 
             * and then we need to take the primary key attribute of the referenced object 
             * to help them out.
             */
            ClassDescriptor cld = aTableAlias.cld.getRepository().getDescriptorFor(anOrd.getItemClass());
            if (cld != null)
            {
                fld = aTableAlias.cld.getFieldDescriptorByName(cld.getPkFields()[0].getPersistentField().getName());
            }
        }

        return fld;
    }

    /**
     * Append the appropriate ColumnName to the buffer<br>
     * if a FIELDDESCRIPTOR is found for the Criteria the colName is taken from
     * there otherwise its taken from Criteria. <br>
     * field names in functions (ie: sum(name) ) are tried to resolve
     * ie: name from FIELDDESCRIPTOR , UPPER(name_test) from Criteria<br>
     * also resolve pathExpression adress.city or owner.konti.saldo
     */
	protected boolean appendColName(String attr, boolean useOuterJoins, UserAlias aUserAlias, StringBuffer buf)
    {
		AttributeInfo attrInfo = getAttributeInfo(attr, useOuterJoins, aUserAlias, getQuery().getPathClasses());
        TableAlias tableAlias = attrInfo.tableAlias;

        return appendColName(tableAlias, attrInfo.pathInfo, (tableAlias != null), buf);
    }

    /**
     * Append the appropriate ColumnName to the buffer<br>
     * if a FIELDDESCRIPTOR is found for the Criteria the colName is taken from
     * there otherwise its taken from Criteria. <br>
     * field names in functions (ie: sum(name) ) are tried to resolve
     * ie: name from FIELDDESCRIPTOR , UPPER(name_test) from Criteria<br>
     * also resolve pathExpression adress.city or owner.konti.saldo
     */
	protected boolean appendColName(String attr, String attrAlias, boolean useOuterJoins, UserAlias aUserAlias,
            StringBuffer buf)
    {
		AttributeInfo attrInfo = getAttributeInfo(attr, useOuterJoins, aUserAlias, getQuery().getPathClasses());
        TableAlias tableAlias = attrInfo.tableAlias;
        PathInfo pi = attrInfo.pathInfo;

        if (pi.suffix != null)
        {
            pi.suffix = pi.suffix + " as " + attrAlias;
        }
        else
        {
            pi.suffix = " as " + attrAlias;
        }

        return appendColName(tableAlias, pi, true, buf);
    }

    /**
     * Builds the Join for columns if they are not found among the existingColumns.
     * @param columns the list of columns represented by Criteria.Field to ensure
     * @param existingColumns the list of column names (String) that are already appended
     */
    protected void ensureColumns(List columns, List existingColumns)
    {
        if (columns == null || columns.isEmpty())
        {
            return;
        }
        
        Iterator iter = columns.iterator();

        while (iter.hasNext())
        {
            FieldHelper cf = (FieldHelper) iter.next();
            if (!existingColumns.contains(cf.name))
            {
                getAttributeInfo(cf.name, false, null, getQuery().getPathClasses());
            }
        }
    }

    /**
     * Builds the Join for columns if they are not found among the existingColumns.
     * These <b>columns are added to the statement</b> using a column-alias "ojb_col_x", 
     * x being the number of existing columns
     * @param columns the list of columns represented by Criteria.Field to ensure
     * @param existingColumns the list of column names (String) that are already appended
     * @param buf the statement
     * @return List of existingColumns including ojb_col_x
     */
    protected List ensureColumns(List columns, List existingColumns, StringBuffer buf)
    {
        if (columns == null || columns.isEmpty())
        {
            return existingColumns;
        }

        Iterator iter = columns.iterator();
        int ojb_col = existingColumns.size() + 1;

        while (iter.hasNext())
        {
            FieldHelper cf = (FieldHelper) iter.next();
            if (!existingColumns.contains(cf.name))
            {
                existingColumns.add(cf.name);
                
                buf.append(",");
                appendColName(cf.name, "ojb_col_" + ojb_col, false, null, buf);
                ojb_col++;
            }
        }
        
        return existingColumns;
    }


    /**
     * appends a WHERE-clause to the Statement
     * @param where
     * @param crit
     * @param stmt
     */
    protected void appendWhereClause(StringBuffer where, Criteria crit, StringBuffer stmt)
    {
        if (where.length() == 0)
        {
            where = null;
        }

        if (where != null || (crit != null && !crit.isEmpty()))
        {
            stmt.append(" WHERE ");
            appendClause(where, crit, stmt);
        }
    }

    /**
     * appends a HAVING-clause to the Statement
     * @param having
     * @param crit
     * @param stmt
     */
    protected void appendHavingClause(StringBuffer having, Criteria crit, StringBuffer stmt)
    {
        if (having.length() == 0)
        {
            having = null;
        }

        if (having != null || crit != null)
        {
            stmt.append(" HAVING ");
            appendClause(having, crit, stmt);
        }
    }

    /**
     * appends a WHERE/HAVING-clause to the Statement
     * @param clause
     * @param crit
     * @param stmt
     */
    protected void appendClause(StringBuffer clause, Criteria crit, StringBuffer stmt)
    {
        /**
         * MBAIRD
         * when generating the "WHERE/HAVING" clause we need to append the criteria for multi-mapped
         * tables. We only need to do this for the root classdescriptor and not for joined tables
         * because we assume you cannot make a relation of the wrong type upon insertion. Of course,
         * you COULD mess the data up manually and this would cause a problem.
         */

        if (clause != null)
        {
            stmt.append(clause.toString());
        }
        if (crit != null)
        {
            if (clause == null)
            {
                stmt.append(asSQLStatement(crit));
            }
            else
            {
                stmt.append(" AND (");
                stmt.append(asSQLStatement(crit));
                stmt.append(")");
            }

        }
    }

    /**
     * Create SQL-String based on Criteria
     */
    private String asSQLStatement(Criteria crit)
    {
        Enumeration e = crit.getElements();
        StringBuffer statement = new StringBuffer();

        while (e.hasMoreElements())
        {
            Object o = e.nextElement();
            if (o instanceof Criteria)
            {
                Criteria pc = (Criteria) o;
                
                if (pc.isEmpty())
                {
                    continue;	//skip empty criteria
                }
                
                String addAtStart = "";
                String addAtEnd = "";

                // need to add parenthesises?
                if (pc.isEmbraced())
                {
                    addAtStart = " (";
                    addAtEnd = ")";
                }    

                switch (pc.getType())
                {
                    case (Criteria.OR) :
                        {
                            if (statement.length() > 0)
                            {
                                statement.append(" OR ");
                            }
                            statement.append(addAtStart);
                            statement.append(asSQLStatement(pc));
                            statement.append(addAtEnd);
                            break;
                        }
                    case (Criteria.AND) :
                        {
                            if (statement.length() > 0)
                            {
                                statement.insert(0, "( ");
                                statement.append(") AND ");
                            }
                            statement.append(addAtStart);
                            statement.append(asSQLStatement(pc));
                            statement.append(addAtEnd);
                            break;
                        }
                }
            }
            else
            {
                SelectionCriteria c = (SelectionCriteria) o;
                if (statement.length() > 0)
                {
                    statement.insert(0, "(");
                    statement.append(") AND ");
                }
                appendSQLClause(c, statement);
            }
        } // while

        // BRJ : negative Criteria surrounded by NOT (...)
        if (crit.isNegative())
        {
            statement.insert(0, " NOT (");
            statement.append(")");
        }
        
        return (statement.length() == 0 ? null : statement.toString());
    }

    /**
     * Answer the SQL-Clause for a BetweenCriteria
     *
     * @param alias
     * @param pathInfo
     * @param c BetweenCriteria
     * @param buf
     */
    private void appendBetweenCriteria(TableAlias alias, PathInfo pathInfo, BetweenCriteria c, StringBuffer buf)
    {
        appendColName(alias, pathInfo, c.isTranslateAttribute(), buf);
        buf.append(c.getClause());
        appendParameter(c.getValue(), buf);
        buf.append(" AND ");
        appendParameter(c.getValue2(), buf);
    }

    /**
     * Answer the SQL-Clause for an ExistsCriteria
     * @param c ExistsCriteria
     */
    private void appendExistsCriteria(ExistsCriteria c, StringBuffer buf)
    {
        Query subQuery = (Query) c.getValue();

        buf.append(c.getClause());
        appendSubQuery(subQuery, buf);
    }

    /**
     * Answer the SQL-Clause for a FieldCriteria<br>
     * The value of the FieldCriteria will be translated
     *
     * @param alias
     * @param pathInfo
     * @param c ColumnCriteria
     * @param buf
     */
    private void appendFieldCriteria(TableAlias alias, PathInfo pathInfo, FieldCriteria c, StringBuffer buf)
    {
        appendColName(alias, pathInfo, c.isTranslateAttribute(), buf);
        buf.append(c.getClause());

        if (c.isTranslateField())
        {
			appendColName((String) c.getValue(), false, c.getUserAlias(), buf);
        }
        else
        {
            buf.append(c.getValue());
        }
    }
    
    /**
     * Get the column name from the indirection table.
     * @param mnAlias 
     * @param path
     */
    private String getIndirectionTableColName(TableAlias mnAlias, String path)
    {
        int dotIdx = path.lastIndexOf(".");
        String column = path.substring(dotIdx);
        return mnAlias.alias + column;
    }

    /**
     * Answer the SQL-Clause for an InCriteria
     *
     * @param alias
     * @param pathInfo
     * @param c InCriteria
     * @param buf
     */
    private void appendInCriteria(TableAlias alias, PathInfo pathInfo, InCriteria c, StringBuffer buf)
    {
        appendColName(alias, pathInfo, c.isTranslateAttribute(), buf);
        buf.append(c.getClause());

        if (c.getValue() instanceof Collection)
        {
            Object[] values = ((Collection) c.getValue()).toArray();
            int size = ((Collection) c.getValue()).size();

            buf.append("(");
            if (size > 0)
            {
                for (int i = 0; i < size - 1; i++)
                {
                    appendParameter(values[i], buf);
                    buf.append(",");
                }
                appendParameter(values[size - 1], buf);
            }
            buf.append(")");
        }
        else
        {
            appendParameter(c.getValue(), buf);
        }
    }

    /**
     * Answer the SQL-Clause for a NullCriteria
     *
     * @param alias
     * @param pathInfo
     * @param c NullCriteria
     * @param buf
     */
    private void appendNullCriteria(TableAlias alias, PathInfo pathInfo, NullCriteria c, StringBuffer buf)
    {
        appendColName(alias, pathInfo, c.isTranslateAttribute(), buf);
        buf.append(c.getClause());
    }

    /**
     * Answer the SQL-Clause for a SqlCriteria
     *
     */
    private void appendSQLCriteria(SqlCriteria c, StringBuffer buf)
    {
        buf.append(c.getClause());
    }

    /**
     * Answer the SQL-Clause for a SelectionCriteria
     *
     * @param c
     * @param buf
     */
    private void appendSelectionCriteria(TableAlias alias, PathInfo pathInfo, SelectionCriteria c, StringBuffer buf)
    {
        appendColName(alias, pathInfo, c.isTranslateAttribute(), buf);
        buf.append(c.getClause());
        appendParameter(c.getValue(), buf);
    }

    /**
     * Answer the SQL-Clause for a LikeCriteria
     *
     * @param c
     * @param buf
     */
    private void appendLikeCriteria(TableAlias alias, PathInfo pathInfo, LikeCriteria c, StringBuffer buf)
    {
        appendColName(alias, pathInfo, c.isTranslateAttribute(), buf);
        buf.append(c.getClause());
        appendParameter(c.getValue(), buf);

        buf.append(m_platform.getEscapeClause(c));
    }

    /**
     * Answer the SQL-Clause for a SelectionCriteria
     *
     * @param alias
     * @param pathInfo
     * @param c SelectionCriteria
     * @param buf
     */
    protected void appendCriteria(TableAlias alias, PathInfo pathInfo, SelectionCriteria c, StringBuffer buf)
    {
        if (c instanceof FieldCriteria)
        {
            appendFieldCriteria(alias, pathInfo, (FieldCriteria) c, buf);
        }
        else if (c instanceof NullCriteria)
        {
            appendNullCriteria(alias, pathInfo, (NullCriteria) c, buf);
        }
        else if (c instanceof BetweenCriteria)
        {
            appendBetweenCriteria(alias, pathInfo, (BetweenCriteria) c, buf);
        }
        else if (c instanceof InCriteria)
        {
            appendInCriteria(alias, pathInfo, (InCriteria) c, buf);
        }
        else if (c instanceof SqlCriteria)
        {
            appendSQLCriteria((SqlCriteria) c, buf);
        }
        else if (c instanceof ExistsCriteria)
        {
            appendExistsCriteria((ExistsCriteria) c, buf);
        }
        else if (c instanceof LikeCriteria)
        {
            appendLikeCriteria(alias, pathInfo, (LikeCriteria) c, buf);
        }
        else
        {
            appendSelectionCriteria(alias, pathInfo, c, buf);
        }
    }

    /**
     * Answer the SQL-Clause for a SelectionCriteria
     * If the Criteria references a class with extents an OR-Clause is
     * added for each extent
     * @param c SelectionCriteria
     */
    protected void appendSQLClause(SelectionCriteria c, StringBuffer buf)
    {
        // BRJ : handle SqlCriteria
        if (c instanceof SqlCriteria)
        {
            buf.append(c.getAttribute());
            return;
        }
        
        // BRJ : criteria attribute is a query
        if (c.getAttribute() instanceof Query)
        {
            Query q = (Query) c.getAttribute();
            buf.append("(");
            buf.append(getSubQuerySQL(q));
            buf.append(")");
            buf.append(c.getClause());
            appendParameter(c.getValue(), buf);
            return;
        }

		AttributeInfo attrInfo = getAttributeInfo((String) c.getAttribute(), false, c.getUserAlias(), c.getPathClasses());
        TableAlias alias = attrInfo.tableAlias;

        if (alias != null)
        {
            boolean hasExtents = alias.hasExtents();

            if (hasExtents)
            {
                // BRJ : surround with braces if alias has extents
                buf.append("(");
                appendCriteria(alias, attrInfo.pathInfo, c, buf);

                c.setNumberOfExtentsToBind(alias.extents.size());
                Iterator iter = alias.iterateExtents();
                while (iter.hasNext())
                {
                    TableAlias tableAlias = (TableAlias) iter.next();
                    buf.append(" OR ");
                    appendCriteria(tableAlias, attrInfo.pathInfo, c, buf);
                }
                buf.append(")");
            }
            else
            {
                // no extents
                appendCriteria(alias, attrInfo.pathInfo, c, buf);
            }
        }
        else
        {
            // alias null
            appendCriteria(alias, attrInfo.pathInfo, c, buf);
        }

    }

    /**
     * Append the Parameter
     * Add the place holder ? or the SubQuery
     * @param value the value of the criteria
     */
    private void appendParameter(Object value, StringBuffer buf)
    {
        if (value instanceof Query)
        {
            appendSubQuery((Query) value, buf);
        }
        else
        {
            buf.append("?");
        }
    }

    /**
     * Append a SubQuery the SQL-Clause
     * @param subQuery the subQuery value of SelectionCriteria
     */
    private void appendSubQuery(Query subQuery, StringBuffer buf)
    {
        buf.append(" (");
        buf.append(getSubQuerySQL(subQuery));
        buf.append(") ");
    }

    /**
     * Convert subQuery to SQL
     * @param subQuery the subQuery value of SelectionCriteria
     */
    private String getSubQuerySQL(Query subQuery)
    {
        ClassDescriptor cld = getRoot().cld.getRepository().getDescriptorFor(subQuery.getSearchClass());
        String sql;

        if (subQuery instanceof QueryBySQL)
        {
            sql = ((QueryBySQL) subQuery).getSql();
        }
        else
        {
            sql = new SqlSelectStatement(this, m_platform, cld, subQuery, m_logger).getStatement();
        }

        return sql;
    }

	/**
	 * Get TableAlias by the path from the target table of the query.
	 * @param aPath the path from the target table of the query to this TableAlias.
	 * @param useOuterJoins use outer join to join this table with the previous
	 * table in the path.
	 * @param aUserAlias if specified, overrides alias in crit
	 * @param fieldRef String[1] contains the field name.
	 * In the case of related table's primary key the "related.pk" attribute
	 * must not add new join, but use the value of foreign key
	 * @param pathClasses the hints 
	 */
	private TableAlias getTableAlias(String aPath, boolean useOuterJoins, UserAlias aUserAlias, String[] fieldRef, Map pathClasses)
	{
        TableAlias curr, prev, indirect;
        String attr, attrPath = null;
        ObjectReferenceDescriptor ord;
        CollectionDescriptor cod;
        ClassDescriptor cld;
        Object[] prevKeys;
        Object[] keys;
        ArrayList descriptors;
        boolean outer = useOuterJoins;
        int pathLength;
        List hintClasses = null;       
        String pathAlias = aUserAlias == null ? null : aUserAlias.getAlias(aPath);
        
        if (pathClasses != null)
        {
            hintClasses = (List) pathClasses.get(aPath);
        }    
        
        curr = getTableAliasForPath(aPath, pathAlias, hintClasses);
        if (curr != null)
        {
            return curr;
        }

		descriptors = getRoot().cld.getAttributeDescriptorsForPath(aPath, pathClasses);
		prev = getRoot();

		if (descriptors == null || descriptors.size() == 0)
		{
			if (prev.hasJoins())
			{
				for (Iterator itr = prev.iterateJoins(); itr.hasNext();)
				{
					prev = ((Join) itr.next()).left;
					descriptors = prev.cld.getAttributeDescriptorsForPath(aPath, pathClasses);
					if (descriptors.size() > 0)
					{
						break;
					}
				}
			}
		}

		pathLength = descriptors.size();
		for (int i = 0; i < pathLength; i++)
		{
			if (!(descriptors.get(i) instanceof ObjectReferenceDescriptor))
			{
				// only use Collection- and ObjectReferenceDescriptor
				continue;
			}

			ord = (ObjectReferenceDescriptor) descriptors.get(i);
			attr = ord.getAttributeName();
			if (attrPath == null)
			{
				attrPath = attr;
			}
			else
			{
				attrPath = attrPath + "." + attr;
			}

            // use clas hints for path
            if (pathClasses != null)
            {
                hintClasses = (List) pathClasses.get(attrPath);     
            }    

			// look for outer join hint
			outer = outer || getQuery().isPathOuterJoin(attrPath);

			// look for 1:n or m:n
			if (ord instanceof CollectionDescriptor)
			{
				cod = (CollectionDescriptor) ord;
				cld = getItemClassDescriptor(cod, hintClasses);

				if (!cod.isMtoNRelation())
				{
					prevKeys = prev.cld.getPkFields();
					keys = cod.getForeignKeyFieldDescriptors(cld);
				}
				else
				{
					String mnAttrPath = attrPath + "*";
					String mnUserAlias = (aUserAlias == null ? null : aUserAlias + "*");
					indirect = getTableAliasForPath(mnAttrPath, mnUserAlias, null);
					if (indirect == null)
					{
						indirect = createTableAlias(cod.getIndirectionTable(), mnAttrPath, mnUserAlias);

						// we need two Joins for m:n
						// 1.) prev class to indirectionTable
						prevKeys = prev.cld.getPkFields();
						keys = cod.getFksToThisClass();
						addJoin(prev, prevKeys, indirect, keys, outer, attr + "*");
					}
					// 2.) indirectionTable to the current Class
					prev = indirect;
					prevKeys = cod.getFksToItemClass();
					keys = cld.getPkFields();
				}
			}
			else
			{
				// must be n:1 or 1:1
				cld = getItemClassDescriptor(ord, hintClasses);

			    // BRJ : if ord is taken from 'super' we have to change prev accordingly
				if (!prev.cld.equals(ord.getClassDescriptor()))
				{
					TableAlias ordAlias = getTableAliasForClassDescriptor(ord.getClassDescriptor());
					Join join = prev.getJoin(ordAlias);
                    if (join != null)
                    {
                        join.isOuter = join.isOuter || outer;
                    }    
				    prev = ordAlias;
				}	

				prevKeys = ord.getForeignKeyFieldDescriptors(prev.cld);
				keys = cld.getPkFields();

				// [olegnitz]
				// a special case: the last element of the path is
				// reference and the field is one of PK fields =>
				// use the correspondent foreign key field, don't add the join
				if ((fieldRef != null) && (i == (pathLength - 1)))
				{
					FieldDescriptor[] pk = cld.getPkFields();

					for (int j = 0; j < pk.length; j++)
					{
						if (pk[j].getAttributeName().equals(fieldRef[0]))
						{
							fieldRef[0] = ((FieldDescriptor) prevKeys[j]).getAttributeName();
							return prev;
						}
					}
				}
			}

			pathAlias = aUserAlias == null ? null : aUserAlias.getAlias(attrPath);
			curr = getTableAliasForPath(attrPath, pathAlias, hintClasses);

			if (curr == null)
			{
				curr = createTableAlias(cld, attrPath, pathAlias, hintClasses);

				outer = outer || (curr.cld == prev.cld) || curr.hasExtents() || useOuterJoins;
				addJoin(prev, prevKeys, curr, keys, outer, attr);

				buildSuperJoinTree(curr, cld, aPath, outer);
			}

			prev = curr;
		}

		m_logger.debug("Result of getTableAlias(): " + curr);
		return curr;
	}

    /**
     * add a join between two aliases
     * 
     * TODO BRJ : This needs refactoring, it looks kind of weird
     *
     * no extents
     * A1   -> A2
     *
     * extents on the right
     * A1   -> A2
     * A1   -> A2E0
     *
     * extents on the left : copy alias on right, extents point to copies
     * A1   -> A2
     * A1E0 -> A2C0
     *
     * extents on the left and right
     * A1   -> A2
     * A1   -> A2E0
     * A1E0 -> A2C0
     * A1E0 -> A2E0C0
     *
     * @param left
     * @param leftKeys
     * @param right
     * @param rightKeys
     * @param outer
     * @param name
     */
    private void addJoin(TableAlias left, Object[] leftKeys, TableAlias right, Object[] rightKeys, boolean outer,
            String name)
    {
        TableAlias extAlias, rightCopy;

        left.addJoin(new Join(left, leftKeys, right, rightKeys, outer, name));

        // build join between left and extents of right
        if (right.hasExtents())
        {
            for (int i = 0; i < right.extents.size(); i++)
            {
                extAlias = (TableAlias) right.extents.get(i);
                FieldDescriptor[] extKeys = getExtentFieldDescriptors(extAlias, (FieldDescriptor[]) rightKeys);

                left.addJoin(new Join(left, leftKeys, extAlias, extKeys, true, name));
            }
        }

        // we need to copy the alias on the right for each extent on the left
        if (left.hasExtents())
        {
            for (int i = 0; i < left.extents.size(); i++)
            {
                extAlias = (TableAlias) left.extents.get(i);
                FieldDescriptor[] extKeys = getExtentFieldDescriptors(extAlias, (FieldDescriptor[]) leftKeys);
                rightCopy = right.copy("C" + i);

                // copies are treated like normal extents
                right.extents.add(rightCopy);
                right.extents.addAll(rightCopy.extents);

                addJoin(extAlias, extKeys, rightCopy, rightKeys, true, name);
            }
        }
    }

    /**
     * Get the FieldDescriptors of the extent based on the FieldDescriptors of the parent.
     */
    private FieldDescriptor[] getExtentFieldDescriptors(TableAlias extAlias, FieldDescriptor[] fds)
    {
        FieldDescriptor[] result = new FieldDescriptor[fds.length];

        for (int i = 0; i < fds.length; i++)
        {
            result[i] = extAlias.cld.getFieldDescriptorByName(fds[i].getAttributeName());
        }

        return result;
    }

    private char getAliasChar()
    {
        char result = 'A';

        if (m_parentStatement != null)
        {
            result = (char) (m_parentStatement.getAliasChar() + 1);
        }

        return result;
    }

    /**
     * Create a TableAlias for path or userAlias
     * @param aCld
     * @param aPath
     * @param aUserAlias
     * @param hints a List os Class objects to be used as hints for path expressions
     * @return TableAlias
     *
     */
    private TableAlias createTableAlias(ClassDescriptor aCld, String aPath, String aUserAlias, List hints)
    {
		if (aUserAlias == null)
		{
			return createTableAlias(aCld, hints, aPath);
		}
		else
		{
			return createTableAlias(aCld, hints, aUserAlias + ALIAS_SEPARATOR + aPath);
		}
    }

    /**
     * Create new TableAlias for path
     * @param cld the class descriptor for the TableAlias
     * @param path the path from the target table of the query to this TableAlias.
     * @param hints a List of Class objects to be used on path expressions
     */
    private TableAlias createTableAlias(ClassDescriptor cld, List hints, String path)
    {
        TableAlias alias;
        boolean lookForExtents = false;

        if (!cld.getExtentClasses().isEmpty() && path.length() > 0)
        {
            lookForExtents = true;
        }

        String aliasName = String.valueOf(getAliasChar()) + m_aliasCount++; // m_pathToAlias.size();
        alias = new TableAlias(cld, aliasName, lookForExtents, hints);

        setTableAliasForPath(path, hints, alias);        
        return alias;
    }

    /**
     * Create a TableAlias for path or userAlias
     * @param aTable
     * @param aPath
     * @param aUserAlias
     * @return TableAlias
     */
    private TableAlias createTableAlias(String aTable, String aPath, String aUserAlias)
    {
		if (aUserAlias == null)
		{
			return createTableAlias(aTable, aPath);
		}
		else
		{
			return createTableAlias(aTable, aUserAlias + ALIAS_SEPARATOR + aPath);
		}
    }

    /**
     * Create new TableAlias for path
     * @param table the table name
     * @param path the path from the target table of the query to this TableAlias.
     */
    private TableAlias createTableAlias(String table, String path)
    {
        TableAlias alias;

        if (table == null)
        {
            getLogger().warn("Creating TableAlias without table for path: " + path);
        }

        String aliasName = String.valueOf(getAliasChar()) + m_aliasCount++; // + m_pathToAlias.size();
        alias = new TableAlias(table, aliasName);
        setTableAliasForPath(path, null, alias);        
        m_logger.debug("createTableAlias2: path: " + path + " tableAlias: " + alias);

        return alias;
    }

    /**
     * Answer the TableAlias for aPath
     * @param aPath
     * @param hintClasses 
     * @return TableAlias, null if none
     */
    private TableAlias getTableAliasForPath(String aPath, List hintClasses)
    {
        return (TableAlias) m_pathToAlias.get(buildAliasKey(aPath, hintClasses));
    }

    /**
     * Set the TableAlias for aPath
     * @param aPath
     * @param hintClasses 
     * @param TableAlias
     */
    private void setTableAliasForPath(String aPath, List hintClasses, TableAlias anAlias)
    {
        m_pathToAlias.put(buildAliasKey(aPath, hintClasses), anAlias);
    }
    
    /**
     * Build the key for the TableAlias based on the path and the hints
     * @param aPath
     * @param hintClasses
     * @return the key for the TableAlias
     */
    private String buildAliasKey(String aPath, List hintClasses)
    {
        if (hintClasses == null || hintClasses.isEmpty())
        {
            return aPath;
        }
        
        StringBuffer buf = new StringBuffer(aPath);
        for (Iterator iter = hintClasses.iterator(); iter.hasNext();)
        {
            Class hint = (Class) iter.next();
            buf.append(" ");
            buf.append(hint.getName());
        }
        return buf.toString();
    }

    /**
     * Answer the TableAlias for ClassDescriptor.
     */
    protected TableAlias getTableAliasForClassDescriptor(ClassDescriptor aCld)
    {
        return (TableAlias) m_cldToAlias.get(aCld);
    }

    /**
     * Set the TableAlias for ClassDescriptor
     */
    private void setTableAliasForClassDescriptor(ClassDescriptor aCld, TableAlias anAlias)
    {
        if (m_cldToAlias.get(aCld) == null)
        {
            m_cldToAlias.put(aCld, anAlias);
        }    
    }

    /**
     * Answer the TableAlias for aPath or aUserAlias
     * @param aPath
     * @param aUserAlias
     * @param hintClasses
     * @return TableAlias, null if none
     */
    private TableAlias getTableAliasForPath(String aPath, String aUserAlias, List hintClasses)
    {
        if (aUserAlias == null)
        {
            return getTableAliasForPath(aPath, hintClasses);
        }
        else
        {
			return getTableAliasForPath(aUserAlias + ALIAS_SEPARATOR + aPath, hintClasses);
        }
    }

	/**
     * Answer the ClassDescriptor for itemClass for an ObjectReferenceDescriptor
     * check optional hint. The returned Class is to highest superclass contained in the hint list. 
	 * TODO: add super ClassDescriptor
	 */
    private ClassDescriptor getItemClassDescriptor(ObjectReferenceDescriptor ord, List hintClasses)
    {   
        DescriptorRepository repo = ord.getClassDescriptor().getRepository();

        if (hintClasses == null || hintClasses.isEmpty())
        {
            return repo.getDescriptorFor(ord.getItemClass()); 
        }
        
        Class resultClass = (Class) hintClasses.get(0);
        
        for (Iterator iter = hintClasses.iterator(); iter.hasNext();)
        {
            Class clazz = (Class) iter.next();
            Class superClazz = clazz.getSuperclass();

            if (superClazz != null && resultClass.equals(superClazz.getSuperclass()))
            {
                continue; // skip if we already have a super superclass 
            }
           
            if (hintClasses.contains(superClazz))
            {
                resultClass = superClazz;   // use superclass if it's in the hints
            }
        }

        return repo.getDescriptorFor(resultClass);
    }

	/**
     * Appends the ORDER BY clause for the Query.
     * <br>
     * If the orderByField is found in the list of selected fields it's index is added. 
     * Otherwise it's name is added.
	 * @param orderByFields 
	 * @param selectedFields the names of the fields in the SELECT clause
	 * @param buf
	 */
    protected void appendOrderByClause(List orderByFields, List selectedFields, StringBuffer buf)
    {

        if (orderByFields == null || orderByFields.size() == 0)
        {
            return;
        }
        
        buf.append(" ORDER BY ");
        for (int i = 0; i < orderByFields.size(); i++)
        {
            FieldHelper cf = (FieldHelper) orderByFields.get(i);
            int colNumber = selectedFields.indexOf(cf.name);
            
            if (i > 0)
            {
                buf.append(",");
            }
            
            if (colNumber >= 0)
            {
                buf.append(colNumber + 1);                
            }
            else
            {            
                appendColName(cf.name, false, null, buf);
            }
            
            if (!cf.isAscending)
            {
                buf.append(" DESC");
            }
        }
    }

    /**
     * Appends the GROUP BY clause for the Query
	 * @param groupByFields 
	 * @param buf
     */
    protected void appendGroupByClause(List groupByFields, StringBuffer buf)
    {
        if (groupByFields == null || groupByFields.size() == 0)
        {
            return;
        }

        buf.append(" GROUP BY ");
        for (int i = 0; i < groupByFields.size(); i++)
        {
            FieldHelper cf = (FieldHelper) groupByFields.get(i);
 
            if (i > 0)
            {
                buf.append(",");
            }

            appendColName(cf.name, false, null, buf);
        }
    }

    /**
     * Appends to the statement table and all tables joined to it.
     * @param alias the table alias
     * @param where append conditions for WHERE clause here
     */
    protected void appendTableWithJoins(TableAlias alias, StringBuffer where, StringBuffer buf)
    {
        int stmtFromPos = 0;
        byte joinSyntax = getJoinSyntaxType();

        if (joinSyntax == SQL92_JOIN_SYNTAX)
        {
            stmtFromPos = buf.length(); // store position of join (by: Terry Dexter)
        }

        if (alias == getRoot())
        {
            // BRJ: also add indirection table to FROM-clause for MtoNQuery 
            if (getQuery() instanceof MtoNQuery)
            {
                MtoNQuery mnQuery = (MtoNQuery)m_query; 
                buf.append(getTableAliasForPath(mnQuery.getIndirectionTable(), null).getTableAndAlias());
                buf.append(", ");
            }           
            buf.append(alias.getTableAndAlias());
        }
        else if (joinSyntax != SQL92_NOPAREN_JOIN_SYNTAX)
        {
            buf.append(alias.getTableAndAlias());
        }

        if (!alias.hasJoins())
        {
            return;
        }

        for (Iterator it = alias.iterateJoins(); it.hasNext();)
        {
            Join join = (Join) it.next();

            if (joinSyntax == SQL92_JOIN_SYNTAX)
            {
                appendJoinSQL92(join, where, buf);
                if (it.hasNext())
                {
                    buf.insert(stmtFromPos, "(");
                    buf.append(")");
                }
            }
            else if (joinSyntax == SQL92_NOPAREN_JOIN_SYNTAX)
            {
                appendJoinSQL92NoParen(join, where, buf);
            }
            else
            {
                appendJoin(where, buf, join);
            }

        }
    }

    /**
     * Append Join for non SQL92 Syntax
     */
    private void appendJoin(StringBuffer where, StringBuffer buf, Join join)
    {
        buf.append(",");
        appendTableWithJoins(join.right, where, buf);
        if (where.length() > 0)
        {
            where.append(" AND ");
        }
        join.appendJoinEqualities(where);
    }

    /**
     * Append Join for SQL92 Syntax
     */
    private void appendJoinSQL92(Join join, StringBuffer where, StringBuffer buf)
    {
        if (join.isOuter)
        {
            buf.append(" LEFT OUTER JOIN ");
        }
        else
        {
            buf.append(" INNER JOIN ");
        }
        if (join.right.hasJoins())
        {
            buf.append("(");
            appendTableWithJoins(join.right, where, buf);
            buf.append(")");
        }
        else
        {
            appendTableWithJoins(join.right, where, buf);
        }
        buf.append(" ON ");
        join.appendJoinEqualities(buf);
    }

    /**
     * Append Join for SQL92 Syntax without parentheses
     */
    private void appendJoinSQL92NoParen(Join join, StringBuffer where, StringBuffer buf)
    {
        if (join.isOuter)
        {
            buf.append(" LEFT OUTER JOIN ");
        }
        else
        {
            buf.append(" INNER JOIN ");
        }

        buf.append(join.right.getTableAndAlias());
        buf.append(" ON ");
        join.appendJoinEqualities(buf);

        appendTableWithJoins(join.right, where, buf);
    }

    /**
     * Build the tree of joins for the given criteria
     */
    private void buildJoinTree(Criteria crit)
    {
        Enumeration e = crit.getElements();

        while (e.hasMoreElements())
        {
            Object o = e.nextElement();
            if (o instanceof Criteria)
            {
                buildJoinTree((Criteria) o);
            }
            else
            {
                SelectionCriteria c = (SelectionCriteria) o;
                
                // BRJ skip SqlCriteria
                if (c instanceof SqlCriteria)
                {
                    continue;
                }
                
                // BRJ: Outer join for OR
                boolean useOuterJoin = (crit.getType() == Criteria.OR);

                // BRJ: do not build join tree for subQuery attribute                  
                if (c.getAttribute() != null && c.getAttribute() instanceof String)
                {
					//buildJoinTreeForColumn((String) c.getAttribute(), useOuterJoin, c.getAlias(), c.getPathClasses());
					buildJoinTreeForColumn((String) c.getAttribute(), useOuterJoin, c.getUserAlias(), c.getPathClasses());
                }
                if (c instanceof FieldCriteria)
                {
                    FieldCriteria cc = (FieldCriteria) c;
					buildJoinTreeForColumn((String) cc.getValue(), useOuterJoin, c.getUserAlias(), c.getPathClasses());
                }
            }
        }
    }

	/**
	 * build the Join-Information for name
	 * functions and the last segment are removed
	 * ie: avg(accounts.amount) -> accounts
	 */
	private void buildJoinTreeForColumn(String aColName, boolean useOuterJoin, UserAlias aUserAlias, Map pathClasses)
	{
		String pathName = SqlHelper.cleanPath(aColName);
		int sepPos = pathName.lastIndexOf(".");

		if (sepPos >= 0)
		{
			getTableAlias(pathName.substring(0, sepPos), useOuterJoin, aUserAlias,
						  new String[]{pathName.substring(sepPos + 1)}, pathClasses);
		}
	}

    /**
     * build the Join-Information if a super reference exists
     *
     * @param left
     * @param cld
     * @param name
     */
    protected void buildSuperJoinTree(TableAlias left, ClassDescriptor cld, String name, boolean useOuterJoin)
    {
        ClassDescriptor superCld = cld.getSuperClassDescriptor();
        if (superCld != null)
        {
            SuperReferenceDescriptor superRef = cld.getSuperReference();
            FieldDescriptor[] leftFields = superRef.getForeignKeyFieldDescriptors(cld);
            TableAlias base_alias = getTableAliasForPath(name, null, null);
            String aliasName = String.valueOf(getAliasChar()) + m_aliasCount++;
            TableAlias right = new TableAlias(superCld, aliasName, useOuterJoin, null);

            Join join1to1 = new Join(left, leftFields, right, superCld.getPkFields(), useOuterJoin, "superClass");
            base_alias.addJoin(join1to1);

            buildSuperJoinTree(right, superCld, name, useOuterJoin);
        }
    }

    /**
     * build the Join-Information for Subclasses having a super reference to this class
     *
     * @param left
     * @param cld
     * @param name
     */
    private void buildMultiJoinTree(TableAlias left, ClassDescriptor cld, String name, boolean useOuterJoin)
    {
        DescriptorRepository repository = cld.getRepository();
        Class[] multiJoinedClasses = repository.getSubClassesMultipleJoinedTables(cld, false);

        for (int i = 0; i < multiJoinedClasses.length; i++)
        {
            ClassDescriptor subCld = repository.getDescriptorFor(multiJoinedClasses[i]);
            SuperReferenceDescriptor srd = subCld.getSuperReference();
            if (srd != null)
            {
                FieldDescriptor[] leftFields = subCld.getPkFields();
                FieldDescriptor[] rightFields = srd.getForeignKeyFieldDescriptors(subCld);
                TableAlias base_alias = getTableAliasForPath(name, null, null);

                String aliasName = String.valueOf(getAliasChar()) + m_aliasCount++;
                TableAlias right = new TableAlias(subCld, aliasName, false, null);

                Join join1to1 = new Join(left, leftFields, right, rightFields, useOuterJoin, "subClass");
                base_alias.addJoin(join1to1);

                buildMultiJoinTree(right, subCld, name, useOuterJoin);
            }
        }
    }

    /**
     * First reduce the Criteria to the normal disjunctive form, then
     * calculate the necessary tree of joined tables for each item, then group
     * items with the same tree of joined tables.
     */
    protected void splitCriteria()
    {
        Criteria whereCrit = getQuery().getCriteria();
        Criteria havingCrit = getQuery().getHavingCriteria();

        if (whereCrit == null || whereCrit.isEmpty())
        {
            getJoinTreeToCriteria().put(getRoot(), null);
        }
        else
        {
            // TODO: parameters list shold be modified when the form is reduced to DNF.
            getJoinTreeToCriteria().put(getRoot(), whereCrit);
            buildJoinTree(whereCrit);
        }

        if (havingCrit != null && !havingCrit.isEmpty())
        {
            buildJoinTree(havingCrit);
        }

    }
 
    
    /**
     * Gets the query.
     * @return Returns a Query
     */
    protected QueryByCriteria getQuery()
    {
        return m_query;
    }

    /**
     * Gets the root.
     * @return Returns a TableAlias
     */
    protected TableAlias getRoot()
    {
        return m_root;
    }

    /**
     * Sets the root.
     * @param root The root to set
     */
    protected void setRoot(TableAlias root)
    {
        this.m_root = root;
    }

    /**
     * Gets the search table of this query.
     * @return Returns a TableAlias
     */
    protected TableAlias getSearchTable()
    {
        return m_search;
    }

    /**
     * Gets the joinTreeToCriteria.
     * @return Returns a HashMap
     */
    protected HashMap getJoinTreeToCriteria()
    {
        return m_joinTreeToCriteria;
    }

    /**
     * Returns the joinSyntaxType.
     * @return byte
     */
    protected byte getJoinSyntaxType()
    {
        return m_platform.getJoinSyntaxType();
    }

    /**
     * Returns the logger.
     * @return Logger
     */
    protected Logger getLogger()
    {
        return m_logger;
    }
    
    public String getStatement()
    {
        if(sql == null)
        {
            sql = buildStatement();
        }
        return sql;
    }

    /**
     * Build the SQL String.
     * @return SQL String
     */
    protected abstract String buildStatement();
    
    
    //-----------------------------------------------------------------
    // ------------------- Inner classes ------------------------------
    //-----------------------------------------------------------------

    /**
     * This class is a helper to return TableAlias and PathInfo
     */
    static final class AttributeInfo
    {
        TableAlias tableAlias;
        PathInfo pathInfo;
    }

    /**
     * This class represents one table (possibly with alias) in the SQL query
     */
    final class TableAlias
    {
        Logger logger = LoggerFactory.getLogger(TableAlias.class);
        ClassDescriptor cld; // Is null for indirection table of M:N relation
        String table;
        final String alias;
        List extents = new ArrayList();
        List hints = new ArrayList();
        List joins;

        TableAlias(String aTable, String anAlias)
        {
            this.cld = null;
            this.table = aTable;
            this.alias = anAlias;
        }

        TableAlias(ClassDescriptor aCld, String anAlias)
        {
            this(aCld, anAlias, false, null);
        }

        TableAlias(ClassDescriptor aCld, String anAlias, boolean lookForExtents, List hints)
        {
            this.cld = aCld;
            this.table = aCld.getFullTableName();
            this.alias = anAlias;
            boolean useHintsOnExtents = false;

            // BRJ: store alias map of in enclosing class
			setTableAliasForClassDescriptor(aCld, this);
			
            //LEANDRO: use hints
            if (hints != null && hints.size() > 0)
            {
                useHintsOnExtents = true;
            }

            logger.debug("TableAlias(): using hints ? " + useHintsOnExtents);

            // BRJ : build alias for extents, only one per Table
            if (lookForExtents)
            {
                ClassDescriptor[] extCLDs = (ClassDescriptor[]) aCld.getRepository().getAllConcreteSubclassDescriptors(
                        aCld).toArray(new ClassDescriptor[0]);

                ClassDescriptor extCd;
                Class extClass;
                String extTable;
                Map extMap = new HashMap(); // only one Alias per Table
                int firstNonAbstractExtentIndex = 0;

                for (int i = 0; i < extCLDs.length; i++)
                {
                    extCd = extCLDs[i];
                    extClass = extCd.getClassOfObject();
                    if (useHintsOnExtents && (!hints.contains(extClass)))
                    {
                        //LEANDRO: don't include this class
                        logger.debug("Skipping class [" + extClass + "] from extents List");
                        firstNonAbstractExtentIndex++;
                        continue;
                    }
                    extTable = extCd.getFullTableName();

                    // BRJ : Use the first non abstract extent
                    // if the main cld is abstract
                    //logger.debug("cld abstract["+aCld.isAbstract()+"] i["+i+"] index ["+firtsNonAbstractExtentIndex+"]");
                    if (aCld.isAbstract() && i == firstNonAbstractExtentIndex)
                    {
                        this.cld = extCd;
                        this.table = extTable;
                    }
                    else
                    {
                        // Add a new extent entry only if the table of the extent
                        // does not match the table of the 'base' class.
                        if (extMap.get(extTable) == null && !extTable.equals(table))
                        {
                            extMap.put(extTable, new TableAlias(extCd, anAlias + "E" + i, false, hints));
                        }
                    }
                }
                extents.addAll(extMap.values());
            }

            if (cld == null)
            {
                throw new PersistenceBrokerSQLException("Table is NULL for alias: " + alias);
            }
        }

        ClassDescriptor getClassDescriptor()
        {
            return cld;
        }

        String getTableAndAlias()
        {
            return table + " " + alias;
        }

        boolean hasExtents()
        {
            return (!extents.isEmpty());
        }

        Iterator iterateExtents()
        {
            return extents.iterator();
        }

        /**
         * Copy the Alias and all it's extents adding a Postfix
         * Joins are not copied.
         */
        TableAlias copy(String aPostfix)
        {
            TableAlias result, temp;
            Iterator iter = iterateExtents();

            if (cld == null)
            {
                result = new TableAlias(table, alias + aPostfix);
            }
            else
            {
                result = new TableAlias(cld, alias + aPostfix);
            }

            while (iter.hasNext())
            {
                temp = (TableAlias) iter.next();
                result.extents.add(temp.copy(aPostfix));
            }

            return result;
        }

        void addJoin(Join join)
        {
            if (joins == null)
            {
                joins = new ArrayList();
            }
            joins.add(join);
        }

        Iterator iterateJoins()
        {
            return joins.iterator();
        }

        boolean hasJoins()
        {
            return (joins != null);
        }

        /**
         * Get the Join ponting to anAlias.
         */
        Join getJoin(TableAlias anAlias)
        {
            Join result = null;

            if (joins != null)
            {
                Iterator iter = joins.iterator();
                while (iter.hasNext())
                {
                    Join join = (Join) iter.next();
                    if (join.right.equals(anAlias))
                    {
                        result = join;
                        break;
                    }
                }
            }
            return result;
        }
        
        public String toString()
        {
            StringBuffer sb = new StringBuffer(1024);
            boolean first = true;

            sb.append(getTableAndAlias());
            if (joins != null)
            {
                sb.append(" [");
                for (Iterator it = joins.iterator(); it.hasNext();)
                {
                    Join join = (Join) it.next();

                    if (first)
                    {
                        first = false;
                    }
                    else
                    {
                        sb.append(", ");
                    }
                    sb.append("-(");
                    sb.append(join.name);
                    sb.append(")->");
                    sb.append(join.right);
                }
                sb.append("]");
            }
            return sb.toString();
        }

        public boolean equals(Object obj)
        {
            TableAlias t = (TableAlias) obj;

            return table.equals(t.table); // BRJ: check table only
        }

        public int hashCode()
        {
            return table.hashCode();
        }

    }

    /**
     * This class represents join between two TableAliases
     */
    final class Join
    {
        final TableAlias left;
        final String[] leftKeys;
        final TableAlias right;
        final String[] rightKeys;
        boolean isOuter;
        /** This is the name of the field corresponding to this join */
        final String name;

        /**
         * leftKeys and rightKeys should be either FieldDescriptor[] or String[]
         */
        Join(TableAlias left, Object[] leftKeys, TableAlias right, Object[] rightKeys, boolean isOuter, String name)
        {
            this.left = left;
            this.leftKeys = getColumns(leftKeys);
            this.right = right;
            this.rightKeys = getColumns(rightKeys);
            this.isOuter = isOuter;
            this.name = name;
        }

        private String[] getColumns(Object[] keys)
        {
            String[] columns = new String[keys.length];

            if (keys instanceof FieldDescriptor[])
            {
                FieldDescriptor[] kd = (FieldDescriptor[]) keys;
                for (int i = 0; i < columns.length; i++)
                {
                    columns[i] = kd[i].getColumnName();
                }
            }
            else
            {
                for (int i = 0; i < columns.length; i++)
                {
                    columns[i] = keys[i].toString();
                }
            }
            return columns;
        }

        void appendJoinEqualities(StringBuffer buf)
        {
            byte joinSyntax = getJoinSyntaxType();

            for (int i = 0; i < leftKeys.length; i++)
            {
                if (i > 0)
                {
                    buf.append(" AND ");
                }
                buf.append(left.alias);
                buf.append(".");
                buf.append(leftKeys[i]);

                if (isOuter && joinSyntax == SYBASE_JOIN_SYNTAX)
                {
                    buf.append("*=");
                }
                else
                {
                    buf.append("=");
                }

                buf.append(right.alias);
                buf.append(".");
                buf.append(rightKeys[i]);

                if (isOuter && joinSyntax == ORACLE_JOIN_SYNTAX)
                {
                    buf.append("(+)");
                }
            }
        }

        public boolean equals(Object obj)
        {
            Join j = (Join) obj;
            return name.equals(j.name) && (isOuter == j.isOuter) && right.equals(j.right);
        }

        public int hashCode()
        {
            return name.hashCode();
        }

        public String toString()
        {
            return left.alias + " -> " + right.alias;
        }
    }
}
