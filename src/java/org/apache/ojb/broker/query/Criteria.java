package org.apache.ojb.broker.query;

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
import java.util.Vector;

import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.metadata.FieldHelper;
import org.apache.ojb.broker.core.PersistenceBrokerConfiguration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;

/**
 * Persistent Criteria can be used to retrieve sets of objects based on their attributes
 * Normally each attribute is ANDed together, an OR can be performed by creating a new
 * PersistentCriteria and adding it.
 * <P>
 * Criteria are used, rather than a simple string, because they can be precompiled for
 * efficiency.
 *
 * This code is based on stuff from
 * COBRA - Java Object Persistence Layer
 * Copyright (C) 1997, 1998    DB Harvey-George
 * eMail: cobra@lowrent.org
 *
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: Criteria.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class Criteria implements java.io.Serializable
{
    static final long serialVersionUID = 7384550404778187808L;

    /** criteria is OR-ed with it's parent */
    public static final int OR = 0;
    /** criteria is AND-ed with it's parent */
    public static final int AND = 1;
    /** criteria has no parent */
    public static final int NONE = 9;

    /** prefix to identify attributes referencing enclosing query */
    public static final String PARENT_QUERY_PREFIX = "parentQuery.";

    private Vector m_criteria;
    private int m_type;
    private boolean m_embraced;
    private boolean m_negative = false;

    // holding CriteriaFields for orderBy and groupBy
    private List orderby = null;
    private List groupby = null;
    private List prefetchedRelationships = null;

	// an optional alias to be used for this criteria
	private String m_alias = null;

	// PAW
	// an aliasPath to be used for this criteria
	private String m_aliasPath = null;

	// holds the path segment(s) to which the alias applies
	private UserAlias m_userAlias = null;

	/** the max. number of parameters in a IN-statement */
    protected static final int IN_LIMIT = getSqlInLimit();

    private QueryByCriteria m_query;
    private Criteria m_parentCriteria;

    // PAW
	// hint classes for paths of this criteria
	private Map m_pathClasses;

    /**
     * Constructor declaration
     */
    public Criteria()
    {
        m_criteria = new Vector();
        groupby = new ArrayList();
        orderby = new ArrayList();
        prefetchedRelationships = new ArrayList();
        m_type = NONE;
        m_embraced = false;
        // PAW
		m_pathClasses = new HashMap();
    }

    /**
     * Constructor with a SelectionCriteria
     * @param aSelectionCriteria SelectionCriteria
     */
    public Criteria(SelectionCriteria aSelectionCriteria)
    {
        this();
        addSelectionCriteria(aSelectionCriteria);
    }

    /**
     * make a copy of the criteria
     * @param includeGroupBy if true
     * @param includeOrderBy if ture
     * @param includePrefetchedRelationships if true
     * @return a copy of the criteria
     */
    public Criteria copy(boolean includeGroupBy, boolean includeOrderBy, boolean includePrefetchedRelationships)
    {
        Criteria copy = new Criteria();

        copy.m_criteria = new Vector(this.m_criteria);
        copy.m_negative = this.m_negative;

        if (includeGroupBy)
        {
            copy.groupby = this.groupby;
        }
        if (includeOrderBy)
        {
            copy.orderby = this.orderby;
        }
        if (includePrefetchedRelationships)
        {
            copy.prefetchedRelationships = this.prefetchedRelationships;
        }

        return copy;
    }

    protected void addSelectionCriteria(SelectionCriteria selectionCrit)
    {
        selectionCrit.setCriteria(this);
        m_criteria.addElement(selectionCrit);
    }

    protected void addCriteria(Criteria crit)
    {
        crit.setParentCriteria(this);
        m_criteria.addElement(crit);
    }

    protected void addCriteria(Vector criteria)
    {
        Object crit;

        for (int i = 0; i < criteria.size(); i++)
        {
            crit = criteria.elementAt(i);
            if (crit instanceof SelectionCriteria)
            {
                addSelectionCriteria((SelectionCriteria) crit);
            }
            else if (crit instanceof Criteria)
            {
                addCriteria((Criteria) crit);
            }
        }
    }

    /**
     * Answer a List of InCriteria based on values, each InCriteria
     * contains only inLimit values
     * @param attribute
     * @param values
     * @param negative
     * @param inLimit the maximum number of values for IN (-1 for no limit)
     * @return List of InCriteria
     */
    protected List splitInCriteria(Object attribute, Collection values, boolean negative, int inLimit)
    {
        List result = new ArrayList();
        Collection inCollection = new ArrayList();

        if (values == null || values.isEmpty())
        {
            // OQL creates empty Criteria for late binding
            result.add(buildInCriteria(attribute, negative, values));
        }
        else
        {
            Iterator iter = values.iterator();

            while (iter.hasNext())
            {
                inCollection.add(iter.next());
                if (inCollection.size() == inLimit || !iter.hasNext())
                {
                    result.add(buildInCriteria(attribute, negative, inCollection));
                    inCollection = new ArrayList();
                }
            }
        }
        return result;
    }

    private InCriteria buildInCriteria(Object attribute, boolean negative, Collection values)
    {
        if (negative)
        {
        	// PAW
			// return ValueCriteria.buildNotInCriteria(attribute, values, getAlias());
			return ValueCriteria.buildNotInCriteria(attribute, values, getUserAlias(attribute));
        }
        else
        {
			// PAW
			// return ValueCriteria.buildInCriteria(attribute, values, getAlias());
			return ValueCriteria.buildInCriteria(attribute, values, getUserAlias(attribute));
        }
    }

    /**
     * Get an Enumeration with all sub criteria
     * @return Enumeration
     */
    public Enumeration getElements()
    {
        return getCriteria().elements();
    }

    /**
     * Get a Vector with all sub criteria
     * @return Vector
     */
    protected Vector getCriteria()
    {
        return m_criteria;
    }

    /**
     * Answer the type
     * @return int
     */
    public int getType()
    {
        return m_type;
    }

    /**
     * Set the type
     * @param type OR, AND, NONE
     */
    public void setType(int type)
    {
        m_type = type;
    }

    /**
     * ANDed criteria are embraced
     * @return true if embraced,
     */
    public boolean isEmbraced()
    {
        return m_embraced;
    }

    /**
     * Set embraced
     * @param embraced true if criteria is to be surrounded by braces
     */
    public void setEmbraced(boolean embraced)
    {
        m_embraced = embraced;
    }

    /**
     * Adds and equals (=) criteria,
     * customer_id = 10034
     *
     * @param  attribute   The field name to be used
     * @param  value       An object representing the value of the field
     */
    public void addEqualTo(String attribute, Object value)
    {
    	// PAW
//		addSelectionCriteria(ValueCriteria.buildEqualToCriteria(attribute, value, getAlias()));
		addSelectionCriteria(ValueCriteria.buildEqualToCriteria(attribute, value, getUserAlias(attribute)));
    }

    /**
     * Adds and equals (=) criteria,
     * CUST_ID = 10034
     * attribute will NOT be translated into column name
     *
     * @param  column   The column name to be used without translation
     * @param  value    An object representing the value of the column
     */
    public void addColumnEqualTo(String column, Object value)
    {
    	// PAW
//		SelectionCriteria c = ValueCriteria.buildEqualToCriteria(column, value, getAlias());
		SelectionCriteria c = ValueCriteria.buildEqualToCriteria(column, value, getUserAlias(column));
        c.setTranslateAttribute(false);
        addSelectionCriteria(c);
    }

    /**
     * Adds and equals (=) criteria for field comparison.
     * The field name will be translated into the appropriate columnName by SqlStatement.
     * The attribute will NOT be translated into column name
     *
     * @param  column       The column name to be used without translation
     * @param  fieldName    An object representing the value of the field
     */
    public void addColumnEqualToField(String column, Object fieldName)
    {
    	// PAW
		//SelectionCriteria c = FieldCriteria.buildEqualToCriteria(column, fieldName, getAlias());
		SelectionCriteria c = FieldCriteria.buildEqualToCriteria(column, fieldName, getUserAlias(column));
        c.setTranslateAttribute(false);
        addSelectionCriteria(c);
    }

    /**
     * Adds and equals (=) criteria for field comparison.
     * The field name will be translated into the appropriate columnName by SqlStatement.
     * <br>
     * name = boss.name
     *
     * @param  attribute   The field name to be used
     * @param  fieldName   The field name to compare with
     */
    public void addEqualToField(String attribute, String fieldName)
    {
		// PAW
		// FieldCriteria c = FieldCriteria.buildEqualToCriteria(attribute, fieldName, getAlias());
		FieldCriteria c = FieldCriteria.buildEqualToCriteria(attribute, fieldName, getUserAlias(attribute));
        addSelectionCriteria(c);
    }

    /**
     * Adds and equals (=) criteria for field comparison.
     * The field name will be translated into the appropriate columnName by SqlStatement.
     * <br>
     * name <> boss.name
     *
     * @param  attribute   The field name to be used
     * @param  fieldName   The field name to compare with
     */
    public void addNotEqualToField(String attribute, String fieldName)
    {
        // PAW
		// SelectionCriteria c = FieldCriteria.buildNotEqualToCriteria(attribute, fieldName, getAlias());
		SelectionCriteria c = FieldCriteria.buildNotEqualToCriteria(attribute, fieldName, getUserAlias(attribute));
        addSelectionCriteria(c);
    }

    /**
     * Adds and equals (<>) criteria for column comparison.
     * The column Name will NOT be translated.
     * <br>
     * name <> T_BOSS.LASTNMAE
     *
     * @param  attribute   The field name to be used
     * @param  colName     The name of the column to compare with
     */
    public void addNotEqualToColumn(String attribute, String colName)
    {
        // PAW
		// FieldCriteria c = FieldCriteria.buildNotEqualToCriteria(attribute, colName, getAlias());
		FieldCriteria c = FieldCriteria.buildNotEqualToCriteria(attribute, colName, getUserAlias(attribute));
        c.setTranslateField(false);
        addSelectionCriteria(c);
    }

    /**
     * Adds and equals (=) criteria for column comparison.
     * The column Name will NOT be translated.
     * <br>
     * name = T_BOSS.LASTNMAE
     *
     * @param  attribute   The field name to be used
     * @param  colName     The name of the column to compare with
     */
    public void addEqualToColumn(String attribute, String colName)
    {
		// FieldCriteria c = FieldCriteria.buildEqualToCriteria(attribute, colName, getAlias());
		FieldCriteria c = FieldCriteria.buildEqualToCriteria(attribute, colName, getUserAlias(attribute));
        c.setTranslateField(false);
        addSelectionCriteria(c);
    }

    /**
     * Adds GreaterOrEqual Than (>=) criteria,
     * customer_id >= 10034
     *
     * @param  attribute   The field name to be used
     * @param  value       An object representing the value of the field
     */
    public void addGreaterOrEqualThan(Object attribute, Object value)
    {
		// PAW
		// addSelectionCriteria(ValueCriteria.buildNotLessCriteria(attribute, value, getAlias()));
		addSelectionCriteria(ValueCriteria.buildNotLessCriteria(attribute, value, getUserAlias(attribute)));
    }

    /**
     * Adds GreaterOrEqual Than (>=) criteria,
     * customer_id >= person_id
     *
     * @param  attribute   The field name to be used
     * @param  value       The field name to compare with
     */
    public void addGreaterOrEqualThanField(String attribute, Object value)
    {
		// PAW
		// addSelectionCriteria(FieldCriteria.buildNotLessCriteria(attribute, value, getAlias()));
		addSelectionCriteria(FieldCriteria.buildNotLessCriteria(attribute, value, getUserAlias(attribute)));
    }

    /**
     * Adds LessOrEqual Than (<=) criteria,
     * customer_id <= 10034
     *
     * @param  attribute   The field name to be used
     * @param  value       An object representing the value of the field
     */
    public void addLessOrEqualThan(Object attribute, Object value)
    {
		// PAW
		// addSelectionCriteria(ValueCriteria.buildNotGreaterCriteria(attribute, value, getAlias()));
		addSelectionCriteria(ValueCriteria.buildNotGreaterCriteria(attribute, value, getUserAlias(attribute)));
    }

    /**
     * Adds LessOrEqual Than (<=) criteria,
     * customer_id <= person_id
     *
     * @param  attribute   The field name to be used
     * @param  value       The field name to compare with
     */
    public void addLessOrEqualThanField(String attribute, Object value)
    {
		// PAW
		// addSelectionCriteria(FieldCriteria.buildNotGreaterCriteria(attribute, value, getAlias()));
		addSelectionCriteria(FieldCriteria.buildNotGreaterCriteria(attribute, value, getUserAlias(attribute)));
    }

    /**
     * Adds Like (LIKE) criteria,
     * customer_name LIKE "m%ller"
     *
     * @see LikeCriteria
     * @param  attribute   The field name to be used
     * @param  value       An object representing the value of the field
     */
    public void addLike(Object attribute, Object value)
    {
		// PAW
		// addSelectionCriteria(ValueCriteria.buildLikeCriteria(attribute, value, getAlias()));
		addSelectionCriteria(ValueCriteria.buildLikeCriteria(attribute, value, getUserAlias(attribute)));
    }

    /**
     * Adds Like (NOT LIKE) criteria,
     * customer_id NOT LIKE 10034
     *
     * @see LikeCriteria
     * @param  attribute   The field name to be used
     * @param  value       An object representing the value of the field
     */
    public void addNotLike(String attribute, Object value)
    {
		// PAW
		// addSelectionCriteria(ValueCriteria.buildNotLikeCriteria(attribute, value, getAlias()));
		addSelectionCriteria(ValueCriteria.buildNotLikeCriteria(attribute, value, getUserAlias(attribute)));
    }

    /**
     * Adds NotEqualTo (<>) criteria,
     * customer_id <> 10034
     *
     * @param  attribute   The field name to be used
     * @param  value       An object representing the value of the field
     */
    public void addNotEqualTo(Object attribute, Object value)
    {
		// PAW
		// addSelectionCriteria(ValueCriteria.buildNotEqualToCriteria(attribute, value, getAlias()));
		addSelectionCriteria(ValueCriteria.buildNotEqualToCriteria(attribute, value, getUserAlias(attribute)));
    }

    /**
     * Adds Greater Than (>) criteria,
     * customer_id > 10034
     *
     * @param  attribute   The field name to be used
     * @param  value       An object representing the value of the field
     */
    public void addGreaterThan(Object attribute, Object value)
    {
		// PAW
		// addSelectionCriteria(ValueCriteria.buildGreaterCriteria(attribute, value, getAlias()));
		addSelectionCriteria(ValueCriteria.buildGreaterCriteria(attribute, value, getUserAlias(attribute)));
    }

    /**
     * Adds Greater Than (>) criteria,
     * customer_id > person_id
     *
     * @param  attribute   The field name to be used
     * @param  value       The field to compare with
     */
    public void addGreaterThanField(String attribute, Object value)
    {
		// PAW
		// addSelectionCriteria(FieldCriteria.buildGreaterCriteria(attribute, value, getAlias()));
		addSelectionCriteria(FieldCriteria.buildGreaterCriteria(attribute, value, getUserAlias(attribute)));
    }

    /**
     * Adds Less Than (<) criteria,
     * customer_id < 10034
     *
     * @param  attribute   The field name to be used
     * @param  value       An object representing the value of the field
     */
    public void addLessThan(Object attribute, Object value)
    {
		// PAW
		// addSelectionCriteria(ValueCriteria.buildLessCriteria(attribute, value, getAlias()));
		addSelectionCriteria(ValueCriteria.buildLessCriteria(attribute, value, getUserAlias(attribute)));
    }

    /**
     * Adds Less Than (<) criteria,
     * customer_id < person_id
     *
     * @param  attribute   The field name to be used
     * @param  value       The field to compare with
     */
    public void addLessThanField(String attribute, Object value)
    {
		// PAW
		// addSelectionCriteria(FieldCriteria.buildLessCriteria(attribute, value, getAlias()));
		addSelectionCriteria(FieldCriteria.buildLessCriteria(attribute, value, getUserAlias(attribute)));
    }

    /**
     * Adds a field for orderBy, order is ASCENDING
     * @param  fieldName The field name to be used
     * @deprecated use #addOrderByAscending(String fieldName)
     */
    public void addOrderBy(String fieldName)
    {
        addOrderBy(fieldName, true);
    }

    /**
     * Adds a field for orderBy
     * @param  fieldName the field name to be used
     * @param  sortAscending true for ASCENDING, false for DESCENDING
     * @deprecated use QueryByCriteria#addOrderBy
     */
    public void addOrderBy(String fieldName, boolean sortAscending)
    {
        if (fieldName != null)
        {
            _getOrderby().add(new FieldHelper(fieldName, sortAscending));
        }
    }

    /**
     * Adds a field for orderBy
     * @param aField the Field
     * @deprecated use QueryByCriteria#addOrderBy
     */
    public void addOrderBy(FieldHelper aField)
    {
        if (aField != null)
        {
            _getOrderby().add(aField);
        }
    }

    /**
     * Adds a field for orderBy ASCENDING
     * @param  fieldName The field name to be used
     * @deprecated use QueryByCriteria#addOrderByAscending
     */
    public void addOrderByAscending(String fieldName)
    {
        addOrderBy(fieldName, true);
    }

    /**
     * Adds a field for orderBy DESCENDING
     * @param  fieldName The field name to be used
     * @deprecated use QueryByCriteria#addOrderByDescending
     */
    public void addOrderByDescending(String fieldName)
    {
        addOrderBy(fieldName, false);
    }

    /**
     * Answer the orderBy of all Criteria and Sub Criteria
     * the elements are of class Criteria.FieldHelper
     * @return List
     */
    List getOrderby()
    {
        List result = _getOrderby();
        Iterator iter = getCriteria().iterator();
        Object crit;

        while (iter.hasNext())
        {
            crit = iter.next();
            if (crit instanceof Criteria)
            {
                result.addAll(((Criteria) crit).getOrderby());
            }
        }

        return result;
    }

    /**
     * Answer the Vector with all orderBy,
     * the elements are of class Criteria.FieldHelper
     * @return List
     */
    protected List _getOrderby()
    {
        return orderby;
    }

    /**
     * ORs two sets of criteria together:
     * <pre>
     * active = true AND balance < 0 OR active = true AND overdraft = 0
     * </pre>
     * @param pc criteria
     */
    public void addOrCriteria(Criteria pc)
    {
        if (!m_criteria.isEmpty())
        {
            pc.setEmbraced(true);
            pc.setType(OR);
            addCriteria(pc);
        }
        else
        {
            setEmbraced(false);
            pc.setType(OR);
            addCriteria(pc);
        }
    }

    /**
     * Adds is Null criteria,
     * customer_id is Null
     *
     * @param  attribute   The field name to be used
     */
    public void addIsNull(String attribute)
    {
		// PAW
		//addSelectionCriteria(ValueCriteria.buildNullCriteria(attribute, getAlias()));
		addSelectionCriteria(ValueCriteria.buildNullCriteria(attribute, getUserAlias(attribute)));
    }

    /**
     * Adds is Null criteria,
     * customer_id is Null
     * The attribute will NOT be translated into column name
     *
     * @param  column   The column name to be used without translation
     */
    public void addColumnIsNull(String column)
    {
		// PAW
		//SelectionCriteria c = ValueCriteria.buildNullCriteria(column, getAlias());
		SelectionCriteria c = ValueCriteria.buildNullCriteria(column, getUserAlias(column));
        c.setTranslateAttribute(false);
        addSelectionCriteria(c);
    }

    /**
     * Adds not Null criteria,
     * customer_id is not Null
     *
     * @param  attribute   The field name to be used
     */
    public void addNotNull(String attribute)
    {
		// PAW
		//addSelectionCriteria(ValueCriteria.buildNotNullCriteria(attribute, getAlias()));
		addSelectionCriteria(ValueCriteria.buildNotNullCriteria(attribute, getUserAlias(attribute)));
    }

    /**
     * Adds not Null criteria,
     * customer_id is not Null
     * The attribute will NOT be translated into column name
     *
     * @param  column   The column name to be used without translation
     */
    public void addColumnNotNull(String column)
    {
		// PAW
		// SelectionCriteria c = ValueCriteria.buildNotNullCriteria(column, getAlias());
		SelectionCriteria c = ValueCriteria.buildNotNullCriteria(column, getUserAlias(column));
        c.setTranslateAttribute(false);
        addSelectionCriteria(c);
    }

    /**
     * Adds BETWEEN criteria,
     * customer_id between 1 and 10
     *
     * @param  attribute   The field name to be used
     * @param  value1   The lower boundary
     * @param  value2   The upper boundary
     */
    public void addBetween(Object attribute, Object value1, Object value2)
    {
		// PAW
		// addSelectionCriteria(ValueCriteria.buildBeweenCriteria(attribute, value1, value2, getAlias()));
		addSelectionCriteria(ValueCriteria.buildBeweenCriteria(attribute, value1, value2, getUserAlias(attribute)));
    }

    /**
     * Adds NOT BETWEEN criteria,
     * customer_id not between 1 and 10
     *
     * @param  attribute   The field name to be used
     * @param  value1   The lower boundary
     * @param  value2   The upper boundary
     */
    public void addNotBetween(Object attribute, Object value1, Object value2)
    {
        // PAW
		// addSelectionCriteria(ValueCriteria.buildNotBeweenCriteria(attribute, value1, value2, getAlias()));
		addSelectionCriteria(ValueCriteria.buildNotBeweenCriteria(attribute, value1, value2, getUserAlias(attribute)));
    }

    /**
     * Adds IN criteria,
     * customer_id in(1,10,33,44)
     * large values are split into multiple InCriteria
     * IN (1,10) OR IN(33, 44)
     *
     * @param  attribute   The field name to be used
     * @param  values   The value Collection
     */
    public void addIn(String attribute, Collection values)
    {
        List list = splitInCriteria(attribute, values, false, IN_LIMIT);
        int index = 0;
        InCriteria inCrit;
        Criteria allInCritaria;

        inCrit = (InCriteria) list.get(index);
        allInCritaria = new Criteria(inCrit);

        for (index = 1; index < list.size(); index++)
        {
            inCrit = (InCriteria) list.get(index);
            allInCritaria.addOrCriteria(new Criteria(inCrit));
        }

        addAndCriteria(allInCritaria);
    }

    /**
     * Adds IN criteria,
     * customer_id in(1,10,33,44)
     * large values are split into multiple InCriteria
     * IN (1,10) OR IN(33, 44) </br>
     * The attribute will NOT be translated into column name
     *
     * @param  column   The column name to be used without translation
     * @param  values   The value Collection
     */
    public void addColumnIn(String column, Collection values)
    {
        List list = splitInCriteria(column, values, false, IN_LIMIT);
        int index = 0;
        InCriteria inCrit;
        Criteria allInCritaria;

        inCrit = (InCriteria) list.get(index);
        inCrit.setTranslateAttribute(false);
        allInCritaria = new Criteria(inCrit);

        for (index = 1; index < list.size(); index++)
        {
            inCrit = (InCriteria) list.get(index);
            inCrit.setTranslateAttribute(false);
            allInCritaria.addOrCriteria(new Criteria(inCrit));
        }

        addAndCriteria(allInCritaria);
    }

    /**
     * Adds NOT IN criteria,
     * customer_id not in(1,10,33,44)
     * large values are split into multiple InCriteria
     * NOT IN (1,10) AND NOT IN(33, 44)
     *
     * @param  attribute   The field name to be used
     * @param  values   The value Collection
     */
    public void addNotIn(String attribute, Collection values)
    {
        List list = splitInCriteria(attribute, values, true, IN_LIMIT);
        InCriteria inCrit;
        for (int index = 0; index < list.size(); index++)
        {
            inCrit = (InCriteria) list.get(index);
            addSelectionCriteria(inCrit);
        }
    }

    /**
     * IN Criteria with SubQuery
     * @param attribute The field name to be used
     * @param subQuery  The subQuery
     */
    public void addIn(Object attribute, Query subQuery)
    {
        // PAW
		// addSelectionCriteria(ValueCriteria.buildInCriteria(attribute, subQuery, getAlias()));
		addSelectionCriteria(ValueCriteria.buildInCriteria(attribute, subQuery, getUserAlias(attribute)));
    }

    /**
     * NOT IN Criteria with SubQuery
     * @param attribute The field name to be used
     * @param subQuery  The subQuery
     */
    public void addNotIn(String attribute, Query subQuery)
    {
		// PAW
		// addSelectionCriteria(ValueCriteria.buildNotInCriteria(attribute, subQuery, getAlias()));
		addSelectionCriteria(ValueCriteria.buildNotInCriteria(attribute, subQuery, getUserAlias(attribute)));
    }

    /**
     * Adds freeform SQL criteria,
     * REVERSE(name) like 're%'
     *
     * @param  anSqlStatment   The free form SQL-Statement
     */
    public void addSql(String anSqlStatment)
    {
        addSelectionCriteria(new SqlCriteria(anSqlStatment));
    }

    /**
     * ANDs two sets of criteria together:
     *
     * @param  pc criteria
     */
    public void addAndCriteria(Criteria pc)
    {
        // by combining a second criteria by 'AND' the existing criteria needs to be enclosed
        // in parenthesis
        if (!m_criteria.isEmpty())
        {
            this.setEmbraced(true);
            pc.setEmbraced(true);
            pc.setType(AND);
            addCriteria(pc);
        }
        else
        {
            setEmbraced(false);
            pc.setType(AND);
            addCriteria(pc);
        }
    }

    /**
     * Adds an exists(sub query)
     *
     * @param subQuery sub-query
     */
    public void addExists(Query subQuery)
    {
        addSelectionCriteria(new ExistsCriteria(subQuery, false));
    }

    /**
     * Adds a not exists(sub query)
     *
     * @param subQuery sub-query
     */
    public void addNotExists(Query subQuery)
    {
        addSelectionCriteria(new ExistsCriteria(subQuery, true));
    }

    /**
     * Answer true if no sub criteria available
     * @return boolean
     */
    public boolean isEmpty()
    {
        return m_criteria.isEmpty();
    }

    /**
     * Gets the groupby for ReportQueries of all Criteria and Sub Criteria
     * the elements are of class FieldHelper
     * @return List of FieldHelper
     */
    List getGroupby()
    {
        List result = _getGroupby();
        Iterator iter = getCriteria().iterator();
        Object crit;

        while (iter.hasNext())
        {
            crit = iter.next();
            if (crit instanceof Criteria)
            {
                result.addAll(((Criteria) crit).getGroupby());
            }
        }

        return result;
    }

    /**
     * Gets the groupby for ReportQueries,
     * the elements are of class Criteria.FieldHelper
     * @return List of Criteria.FieldHelper
     */
    protected List _getGroupby()
    {
        return groupby;
    }

    /**
     * Adds a groupby fieldName for ReportQueries.
     * @param fieldName The groupby to set
     * @deprecated use QueryByCriteria#addGroupBy
     */
    public void addGroupBy(String fieldName)
    {
        if (fieldName != null)
        {
            _getGroupby().add(new FieldHelper(fieldName, false));
        }
    }

    /**
     * Adds a field for groupby
     * @param aField the Field
     * @deprecated use QueryByCriteria#addGroupBy
     */
    public void addGroupBy(FieldHelper aField)
    {
        if (aField != null)
        {
            _getGroupby().add(aField);
        }
    }

    /**
     * Adds an array of groupby fieldNames for ReportQueries.
     * @param fieldNames The groupby to set
     * @deprecated use QueryByCriteria#addGroupBy
     */
    public void addGroupBy(String[] fieldNames)
    {
        for (int i = 0; i < fieldNames.length; i++)
        {
            addGroupBy(fieldNames[i]);
        }
    }

    /**
     * Returns the prefetchedRelationships.
     * @return List
     */
    List getPrefetchedRelationships()
    {
        return prefetchedRelationships;
    }

    /**
     * add the name of a Relationship for prefetch read
     * @param aName the name of the relationship
     * @deprecated use QueryByCriteria#addPrefetchedRelationship
     */
    public void addPrefetchedRelationship(String aName)
    {
        getPrefetchedRelationships().add(aName);
    }

    /**
     * read the prefetchInLimit from Config based on OJB.properties
     */
    private static int getSqlInLimit()
    {
        try
        {
            PersistenceBrokerConfiguration config = (PersistenceBrokerConfiguration) PersistenceBrokerFactory
                    .getConfigurator().getConfigurationFor(null);
            return config.getSqlInLimit();
        }
        catch (ConfigurationException e)
        {
            return 200;
        }
    }

	/**
	 * @return String
	 */
	public String getAlias()
	{
		return m_alias;
	}

	/**
	 * @return String
	 */
	// PAW
	public UserAlias getUserAlias()
	{
		return m_userAlias;
	}

	// PAW
	/**
	 * Retrieves or if necessary, creates a user alias to be used
	 * by a child criteria
	 * @param attribute The alias to set
	 */
	private UserAlias getUserAlias(Object attribute)
	{
		if (m_userAlias != null)
		{
			return m_userAlias;
		}
		if (!(attribute instanceof String))
		{
			return null;
		}
		if (m_alias == null)
		{
			return null;
		}
		if (m_aliasPath == null)
		{
			boolean allPathsAliased = true;
			return new UserAlias(m_alias, (String)attribute, allPathsAliased);
		}
		return new UserAlias(m_alias, (String)attribute, m_aliasPath);
	}


	/**
	 * Sets the alias. Empty String is regarded as null.
	 * @param alias The alias to set
	 */
	public void setAlias(String alias)
	{
		if (alias == null || alias.trim().equals(""))
		{
			m_alias = null;
		}
		else
		{
			m_alias = alias;
		}

		// propagate to SelectionCriteria,not to Criteria
		for (int i = 0; i < m_criteria.size(); i++)
		{
			if (!(m_criteria.elementAt(i) instanceof Criteria))
			{
				((SelectionCriteria) m_criteria.elementAt(i)).setAlias(m_alias);
			}
		}
	}

	// PAW
	/**
	 * Sets the alias. Empty String is regarded as null.
	 * @param alias The alias to set
	 * @param aliasPath The path segment(s) to which the alias applies
	 */
	public void setAlias(String alias, String aliasPath)
	{
		if (alias == null || alias.trim().equals(""))
		{
			m_alias = null;
		}
		else
		{
			m_alias = alias;
			m_aliasPath = aliasPath;
		}

		// propagate to SelectionCriteria,not to Criteria
		for (int i = 0; i < m_criteria.size(); i++)
		{
			if (!(m_criteria.elementAt(i) instanceof Criteria))
			{
				((SelectionCriteria) m_criteria.elementAt(i)).setAlias(m_alias, aliasPath);
			}
		}
	}

	// PAW
	/**
	 * Sets the alias using a userAlias object.
	 * @param userAlias The alias to set
	 */
	public void setAlias(UserAlias userAlias)
	{
		m_alias = userAlias.getName();

		// propagate to SelectionCriteria,not to Criteria
		for (int i = 0; i < m_criteria.size(); i++)
		{
			if (!(m_criteria.elementAt(i) instanceof Criteria))
			{
				((SelectionCriteria) m_criteria.elementAt(i)).setAlias(userAlias);
			}
		}
	}


    /**
     * @return the query containing the criteria
     */
    public QueryByCriteria getQuery()
    {
        if (getParentCriteria() != null)
        {
            return getParentCriteria().getQuery();
        }
        else
        {
            return m_query;
        }

    }

    /**
     * @param query
     */
    void setQuery(QueryByCriteria query)
    {
        m_query = query;
    }

    /**
     * @return the parent criteria
     */
    public Criteria getParentCriteria()
    {
        return m_parentCriteria;
    }

    /**
     * @param criteria
     */
    void setParentCriteria(Criteria criteria)
    {
        m_parentCriteria = criteria;
    }

    /**
     * @see Object#toString()
     */
    public String toString()
    {
        if (isNegative())
        {
            return "-" + m_criteria.toString();
        }
        else
        {
            return m_criteria.toString();
        }
    }

    /**
     * @return Returns the negative.
     */
    public boolean isNegative()
    {
        return m_negative;
    }

    /**
     * Flags the whole Criteria as negative.
     * @param negative The negative to set.
     */
    public void setNegative(boolean negative)
    {
        m_negative = negative;
    }

	// PAW
	/**
	 * Add a hint Class for a path. Used for relationships to extents.<br>
	 * SqlStatment will use these hint classes when resolving the path.
	 * Without these hints SqlStatment will use the base class the
	 * relationship points to ie: Article instead of CdArticle.
	 *
	 * @param aPath the path segment ie: allArticlesInGroup
	 * @param aClass the Class ie: CdArticle
	 * @see org.apache.ojb.broker.QueryTest#testInversePathExpression()
	 */
	public void addPathClass(String aPath, Class aClass)
	{
		List pathClasses = (List) m_pathClasses.get(aPath);
		if(pathClasses == null)
		{
			setPathClass(aPath,aClass);
		}
		else
		{
			pathClasses.add(aClass);
			//m_pathClasses.put(aPath, pathClasses);
		}
	}

	/**
	 * Set the Class for a path. Used for relationships to extents.<br>
	 * SqlStatment will use this class when resolving the path.
	 * Without this hint SqlStatment will use the base class the
	 * relationship points to ie: Article instead of CdArticle.
	 * Using this method is the same as adding just one hint
	 *
	 * @param aPath the path segment ie: allArticlesInGroup
	 * @param aClass the Class ie: CdArticle
	 * @see org.apache.ojb.broker.QueryTest#testInversePathExpression()
	 * @see #addPathClass
	 */
	public void setPathClass(String aPath, Class aClass)
	{
		List pathClasses = new ArrayList(1);
		pathClasses.add(aClass);
		m_pathClasses.put(aPath, pathClasses);
	}

	/**
	 * Get the a List of Class objects used as hints for a path
	 *
	 * @param aPath the path segment ie: allArticlesInGroup
	 * @return a List o Class objects to be used in SqlStatment
	 * @see #addPathClass
	 * @see org.apache.ojb.broker.QueryTest#testInversePathExpression()
	 */
	public List getClassesForPath(String aPath)
	{
		return (List)getPathClasses().get(aPath);
	}

	/**
	 * Gets the pathClasses.
	 * A Map containing hints about what Class to be used for what path segment
	 * If local instance not set, try parent Criteria's instance.  If this is
	 * the top-level Criteria, try the m_query's instance
	 * @return Returns a Map
	 */
	public Map getPathClasses()
	{
		if (m_pathClasses.isEmpty())
		{
			if (m_parentCriteria == null)
			{
				if (m_query == null)
				{
					return m_pathClasses;
				}
				else
				{
					return m_query.getPathClasses();
				}
			}
			else
			{
				return m_parentCriteria.getPathClasses();
			}
		}
		else
		{
			return m_pathClasses;
		}
	}



}