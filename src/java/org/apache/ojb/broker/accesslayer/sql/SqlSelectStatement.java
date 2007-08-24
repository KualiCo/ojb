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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.ref.WeakReference;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.JdbcType;
import org.apache.ojb.broker.platforms.Platform;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.ReportQuery;
import org.apache.ojb.broker.query.ReportQueryByCriteria;
import org.apache.ojb.broker.util.SqlHelper;
import org.apache.ojb.broker.util.logging.Logger;

/**
 * Model a SELECT Statement
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: SqlSelectStatement.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class SqlSelectStatement extends SqlQueryStatement implements SelectStatement
{
    private WeakReference fieldsForSelect;

    /**
     * Constructor for SqlSelectStatement.
     * 
     * @param pf
     * @param cld
     * @param query
     * @param logger
     */
    public SqlSelectStatement(Platform pf, ClassDescriptor cld, Query query, Logger logger)
    {
        super(pf, cld, query, logger);
    }

    /**
     * Constructor for SqlSelectStatement.
     *
     * @param parent
     * @param pf
     * @param cld
     * @param query
     * @param logger
     */
    public SqlSelectStatement(SqlQueryStatement parent, Platform pf, ClassDescriptor cld, Query query, Logger logger)
    {
        super(parent, pf, cld, query, logger);
    }

    /**
     * Append a Column with alias: A0 name -> A0.name
     * @param anAlias the TableAlias
     * @param field
     * @param buf
     */
    protected void appendColumn(TableAlias anAlias, FieldDescriptor field, StringBuffer buf)
    {
        buf.append(anAlias.alias);
        buf.append(".");
        buf.append(field.getColumnName());
    }

    /**
     * Appends to the statement a comma separated list of column names.
     *
     * DO NOT use this if order of columns is important. The row readers build reflectively and look up
     * column names to find values, so this is safe. In the case of update, you CANNOT use this as the
     * order of columns is important.
     *
     * @return list of column names for the set of all unique columns for multiple classes mapped to the
     * same table.
     */
    protected List appendListOfColumnsForSelect(StringBuffer buf)
    {
        FieldDescriptor[] fieldDescriptors = getFieldsForSelect();
        ArrayList columnList = new ArrayList();
        TableAlias searchAlias = getSearchTable();
        
        for (int i = 0; i < fieldDescriptors.length; i++)
        {
            FieldDescriptor field = fieldDescriptors[i];
            TableAlias alias = getTableAliasForClassDescriptor(field.getClassDescriptor());
            if (alias == null)
            {
                alias = searchAlias;
            }
            if (i > 0)
            {
                buf.append(",");
            }
            appendColumn(alias, field, buf);
            columnList.add(field.getAttributeName());
        }
        
        appendClazzColumnForSelect(buf);
        return columnList;
    }
 
    /**
     * Get MultiJoined ClassDescriptors
     * @param cld
     */
    private ClassDescriptor[] getMultiJoinedClassDescriptors(ClassDescriptor cld)
    {
        DescriptorRepository repository = cld.getRepository();
        Class[] multiJoinedClasses = repository.getSubClassesMultipleJoinedTables(cld, true);
        ClassDescriptor[] result = new ClassDescriptor[multiJoinedClasses.length];

        for (int i = 0 ; i < multiJoinedClasses.length; i++)
        {
            result[i] = repository.getDescriptorFor(multiJoinedClasses[i]);
         }

        return result;
    }

    /**
     * Create the OJB_CLAZZ pseudo column based on CASE WHEN.
     * This column defines the Class to be instantiated.
     * @param buf
     */
    private void appendClazzColumnForSelect(StringBuffer buf)
    {
        ClassDescriptor cld = getSearchClassDescriptor();
        ClassDescriptor[] clds = getMultiJoinedClassDescriptors(cld);

        if (clds.length == 0)
        {
            return;
        }
        
        buf.append(",CASE");

        for (int i = clds.length; i > 0; i--)
        {
            buf.append(" WHEN ");

            ClassDescriptor subCld = clds[i - 1];
            FieldDescriptor[] fieldDescriptors = subCld.getPkFields();

            TableAlias alias = getTableAliasForClassDescriptor(subCld);
            for (int j = 0; j < fieldDescriptors.length; j++)
            {
                FieldDescriptor field = fieldDescriptors[j];
                if (j > 0)
                {
                    buf.append(" AND ");
                }
                appendColumn(alias, field, buf);
                buf.append(" IS NOT NULL");
            }
            buf.append(" THEN '").append(subCld.getClassNameOfObject()).append("'");
        }
        buf.append(" ELSE '").append(cld.getClassNameOfObject()).append("'");
        buf.append(" END AS " + SqlHelper.OJB_CLASS_COLUMN);
    }
    
    /**
     * Return the Fields to be selected.
     *
     * @return the Fields to be selected
     */
    protected FieldDescriptor[] getFieldsForSelect()
    {
        if (fieldsForSelect == null || fieldsForSelect.get() == null)
        {
            fieldsForSelect = new WeakReference(buildFieldsForSelect(getSearchClassDescriptor()));
        }
        return (FieldDescriptor[]) fieldsForSelect.get();
    }

    /**
     * Return the Fields to be selected.
     *
     * @param cld the ClassDescriptor
     * @return the Fields to be selected
     */
    protected FieldDescriptor[] buildFieldsForSelect(ClassDescriptor cld)
    {
        DescriptorRepository repository = cld.getRepository();
        Set fields = new ListOrderedSet();   // keep the order of the fields
        
        // add Standard Fields
        // MBAIRD: if the object being queried on has multiple classes mapped to the table,
        // then we will get all the fields that are a unique set across all those classes so if we need to
        // we can materialize an extent
        FieldDescriptor fds[] = repository.getFieldDescriptorsForMultiMappedTable(cld);
        for (int i = 0; i < fds.length; i++)
        {
            fields.add(fds[i]);
        }

        // add inherited Fields. This is important when querying for a class having a super-reference
        fds = cld.getFieldDescriptor(true);
        for (int i = 0; i < fds.length; i++)
        {
            fields.add(fds[i]);
        }

        // add Fields of joined subclasses
        Class[] multiJoinedClasses = repository.getSubClassesMultipleJoinedTables(cld, true);
        for (int c = 0; c < multiJoinedClasses.length; c++)
        {
            ClassDescriptor subCld = repository.getDescriptorFor(multiJoinedClasses[c]);
            fds = subCld.getFieldDescriptions();
            for (int i = 0; i < fds.length; i++)
            {
                fields.add(fds[i]);
            }
        }

        FieldDescriptor[] result = new FieldDescriptor[fields.size()];
        fields.toArray(result);
        return result;
    }

    /**
     * Appends to the statement a comma separated list of column names.
     *
     * @param columns defines the columns to be selected (for reports)
     * @return list of column names
     */
    protected List appendListOfColumns(String[] columns, StringBuffer buf)
    {
        ArrayList columnList = new ArrayList();

        for (int i = 0; i < columns.length; i++)
        {
            if (i > 0)
            {
                buf.append(",");
            }
            appendColName(columns[i], false, null, buf);
            columnList.add(columns[i]);
        }
        return columnList;
    }

    /**
     * @see org.apache.ojb.broker.accesslayer.sql.SqlQueryStatement#buildStatement()
     */
    protected String buildStatement()
    {
        StringBuffer stmt = new StringBuffer(1024);
        Query query = getQuery();
        boolean first = true;
        List orderByFields = null;
        String[] attributes = null;
        String[] joinAttributes = null;
        Iterator it = getJoinTreeToCriteria().entrySet().iterator();
        List columnList = new ArrayList();

        if (query instanceof ReportQuery)
        {
            attributes = ((ReportQuery) query).getAttributes();
            joinAttributes = ((ReportQuery) query).getJoinAttributes();
        }

        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry) it.next();
            Criteria whereCrit = (Criteria) entry.getValue();
            Criteria havingCrit = query.getHavingCriteria();
            StringBuffer where = new StringBuffer();
            StringBuffer having = new StringBuffer();
            List groupByFields;

            // Set correct tree of joins for the current criteria
            setRoot((TableAlias) entry.getKey());

            if (whereCrit != null && whereCrit.isEmpty())
            {
                whereCrit = null;
            }

            if (havingCrit != null && havingCrit.isEmpty())
            {
                havingCrit = null;
            }

            if (first)
            {
                first = false;
            }
            else
            {
                stmt.append(" UNION ");
            }

            stmt.append("SELECT ");
            if (query.isDistinct())
            {
                stmt.append("DISTINCT ");
            }

            if (attributes == null || attributes.length == 0)
            {
                /**
                 * MBAIRD: use the appendListofColumnsForSelect, as it finds
                 * the union of select items for all object mapped to the same table. This
                 * will allow us to load objects with unique mapping fields that are mapped
                 * to the same table.
                 */
                columnList.addAll(appendListOfColumnsForSelect(stmt));
            }
            else
            {
                columnList.addAll(appendListOfColumns(attributes, stmt));
            }

            // BRJ:
            // joinColumns are only used to force the building of a join;
            // they are not appended to the select-clause !
            // these columns are used in COUNT-ReportQueries and
            // are taken from the query the COUNT is based on 
            if (joinAttributes != null && joinAttributes.length > 0)
            {
                for (int i = 0; i < joinAttributes.length; i++)
                {
					getAttributeInfo(joinAttributes[i], false, null, getQuery().getPathClasses());
                }
            }

            groupByFields = query.getGroupBy();
            ensureColumns(groupByFields, columnList);
            
            orderByFields = query.getOrderBy();
            columnList = ensureColumns(orderByFields, columnList, stmt);
/*
arminw:
TODO: this feature doesn't work, so remove this in future
*/
            /**
             * treeder: going to map superclass tables here,
             * not sure about the columns, just using all columns for now
             */
            ClassDescriptor cld = getBaseClassDescriptor();
            ClassDescriptor cldSuper = null;
            if (cld.getSuperClass() != null)
            {
                // then we have a super class so join tables
                cldSuper = cld.getRepository().getDescriptorFor(cld.getSuperClass());
                appendSuperClassColumns(cldSuper, stmt);
            }

            stmt.append(" FROM ");
            appendTableWithJoins(getRoot(), where, stmt);

            if (cld.getSuperClass() != null)
            {
                appendSuperClassJoin(cld, cldSuper, stmt, where);
            }
            
            appendWhereClause(where, whereCrit, stmt);
            appendGroupByClause(groupByFields, stmt);
            appendHavingClause(having, havingCrit, stmt);
        }

        appendOrderByClause(orderByFields, columnList, stmt);

        if (query instanceof ReportQueryByCriteria)
        {
             ((ReportQueryByCriteria) query).setAttributeFieldDescriptors(m_attrToFld);
        }

        return stmt.toString();
    }

/*
arminw:
TODO: this feature doesn't work, so remove this in future
*/
    private void appendSuperClassJoin(ClassDescriptor cld, ClassDescriptor cldSuper, StringBuffer stmt, StringBuffer where)
    {
        stmt.append(",");
        appendTable(cldSuper, stmt);

        if (where != null)
        {
            if (where.length() > 0)
            {
                where.append(" AND ");
            }

            // get reference field in super class
            // TODO: do not use the superclassfield anymore, just assume that the id is the same in both tables - @see PBroker.storeToDb
            int superFieldRef = cld.getSuperClassFieldRef();
            FieldDescriptor refField = cld.getFieldDescriptorByIndex(superFieldRef);

            appendTable(cldSuper, where);
            where.append(".");
            appendField(cldSuper.getAutoIncrementFields()[0], where);
            where.append(" = ");
            appendTable(cld, where);
            where.append(".");
            appendField(refField, where);
        }
    }

    private void appendSuperClassColumns(ClassDescriptor cldSuper, StringBuffer buf)
    {
        FieldDescriptor[] fields = cldSuper.getFieldDescriptions();
        for (int i = 0; i < fields.length; i++)
        {
            FieldDescriptor field = fields[i];
            if (i > 0)
            {
                buf.append(",");
            }
            buf.append(cldSuper.getFullTableName());
            buf.append(".");
            buf.append(field.getColumnName());
        }
    }

    /**
     * Append table name. Quote if necessary.
     */
    protected void appendTable(ClassDescriptor cld, StringBuffer buf)
    {
        buf.append(cld.getFullTableName());
    }

    /**
     * Append column name. Quote if necessary.
     */
    protected void appendField(FieldDescriptor fld, StringBuffer buf)
    {
        buf.append(fld.getColumnName());
    }

    public Query getQueryInstance()
    {
        return getQuery();
    }

    public int getColumnIndex(FieldDescriptor fld)
    {
        int index = JdbcType.MIN_INT;
        FieldDescriptor[] fields = getFieldsForSelect();
        if (fields != null)
        {
            for (int i = 0; i < fields.length; i++)
            {
                if (fields[i].equals(fld))
                {
                    index = i + 1;  // starts at 1
                    break;
                }
            }
        }
        return index;
    }
}
