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

import java.util.List;
import java.util.Map;

/**
 * abstract baseclass of all criteria classes, can't be instantiated.
 * 
 * This code is based on stuff from 
 * COBRA - Java Object Persistence Layer
 * Copyright (C) 1997, 1998    DB Harvey-George
 * eMail: cobra@lowrent.org

 * @author DB Harvey-George
 * @author Thomas Mahler
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: SelectionCriteria.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public abstract class SelectionCriteria implements java.io.Serializable
{
	static final long serialVersionUID = -5194901539702756536L;    protected static final String EQUAL = " = ";
    protected static final String NOT_EQUAL = " <> ";
    protected static final String GREATER = " > ";
    protected static final String NOT_GREATER = " <= ";
    protected static final String LESS = " < ";
    protected static final String NOT_LESS = " >= ";
    protected static final String LIKE = " LIKE ";
    protected static final String NOT_LIKE = " NOT LIKE ";
    protected static final String IS_NULL = " IS NULL ";
    protected static final String NOT_IS_NULL = " IS NOT NULL ";
    protected static final String BETWEEN = " BETWEEN ";
    protected static final String NOT_BETWEEN = " NOT BETWEEN ";
    protected static final String IN = " IN ";
    protected static final String NOT_IN = " NOT IN ";

	private Object m_attribute;
	private Object m_value;


	// BRJ: true if criterion is bound
	private boolean m_bound = false;

	// BRJ: the criterion must be bound for the main class and for all extents
	private int m_numberOfExtentsToBind = 0; 

	private String m_alias = null;
	private UserAlias m_userAlias = null;
	    
    // BRJ: indicate whether attribute name should be translated into column name
    private boolean m_translateAttribute = true;

    private Criteria m_criteria;
    
	/**
	 * Constructor declaration
	 *
	 * @param anAttribute  column- or fieldName or a Query
	 * @param aValue  the value to compare with
	 * @param negative  criteria is negated (ie NOT LIKE instead of LIKE)
	 * @param alias  use alias to link anAttribute to
	 */
	SelectionCriteria(Object anAttribute, Object aValue, String alias)
	{
		if (!(anAttribute instanceof String || anAttribute instanceof Query))
		{
			throw new IllegalArgumentException("An attribute must be a String or a Query !");
		}    
            
		m_attribute = anAttribute;
		m_value = aValue;
		this.m_bound = !isBindable();
		this.m_alias = alias;
		this.m_userAlias  = m_alias == null ? null : new UserAlias(m_alias, (String)getAttribute(), true);
	}

	/**
	 * Constructor declaration
	 *
	 * @param anAttribute  column- or fieldName or a Query
	 * @param aValue  the value to compare with
	 * @param aUserAlias  userAlias to link anAttribute to
	 */
	SelectionCriteria(Object anAttribute, Object aValue, UserAlias aUserAlias)
	{
		if (!(anAttribute instanceof String || anAttribute instanceof Query))
		{
			throw new IllegalArgumentException("An attribute must be a String or a Query !");
		}

		m_attribute = anAttribute;
		m_value = aValue;
		this.m_bound = !isBindable();
		this.m_userAlias = aUserAlias;
		this.m_alias = m_userAlias == null ? null : m_userAlias.getName();
	}

	/**
	 * Answer the SQL compare-clause for this criteria
	 */
	abstract public String getClause();

	/**
	 * sets the value of the criteria to newValue. Used by the ODMG OQLQuery.bind() operation
	 */
	public void bind(Object newValue)
	{
		setValue(newValue);
		setBound(true);
	}

	/**
	 * Answer the value
	 */
	public Object getValue()
	{
		return m_value;
	}

	/**
	 * Answer the attribute
	 */
	public Object getAttribute()
	{
		return m_attribute;
	}

	/**
	 * String representation
	 */
	public String toString()
	{
		return m_attribute + getClause() + m_value;
	}

	/**
	 * BRJ : Used by the ODMG OQLQuery.bind() operation
	 * @return Returns a boolean indicator
	 */
	public boolean isBound()
	{
		return m_bound;
	}

	/**
	 * Sets the bound.
	 * @param bound The bound to set
	 */
	protected void setBound(boolean bound)
	{
		this.m_bound = bound;
	}

	/**
	 * Sets the value.
	 * @param value The value to set
	 */
	protected void setValue(Object value)
	{
		this.m_value = value;
	}

	/**
	 * answer true if the selection criteria is bindable 
	 * BRJ: value null is bindable
	 */
	protected boolean isBindable()
	{
		return (getValue() == null);
	}
	/**
	 * Returns the numberOfExtentsToBind.
	 * @return int
	 */
	public int getNumberOfExtentsToBind()
	{
		return m_numberOfExtentsToBind;
	}

	/**
	 * Sets the numberOfExtentsToBind.
	 * @param numberOfExtentsToBind The numberOfExtentsToBind to set
	 */
	public void setNumberOfExtentsToBind(int numberOfExtentsToBind)
	{
		this.m_numberOfExtentsToBind = numberOfExtentsToBind;
	}

	/**
	 * @return String
	 */
	public String getAlias()
	{
		return m_alias;
	}

	/**
	 * Sets the alias. By default the entire attribute path participates in the alias
	 * @param alias The name of the alias to set
	 */
	public void setAlias(String alias)
	{
		m_alias = alias;
		String attributePath = (String)getAttribute();
		boolean allPathsAliased = true;
		m_userAlias = new UserAlias(alias, attributePath, allPathsAliased);
		
	}

	/**
	 * Sets the alias. 
	 * @param alias The alias to set
	 */
	public void setAlias(String alias, String aliasPath)
	{
		m_alias = alias;
		m_userAlias = new UserAlias(alias, (String)getAttribute(), aliasPath);
	}
	
	/**
	 * Sets the alias using a userAlias object. 
	 * @param userAlias The alias to set
	 */
	public void setAlias(UserAlias userAlias)
	{
		m_alias = userAlias.getName();
		m_userAlias = userAlias;
	}

	public UserAlias getUserAlias()
	{
		return m_userAlias;
	}
	/**
	 * @return true if attribute name should be translated into column name
	 */
	public boolean isTranslateAttribute()
	{
		return m_translateAttribute;
	}

	/**
	 * @param b
	 */
	void setTranslateAttribute(boolean b)
	{
		m_translateAttribute = b;
	}

	/**
	 * @return
	 */
	public Criteria getCriteria()
	{
		return m_criteria;
	}

	/**
	 * @param criteria
	 */
	void setCriteria(Criteria criteria)
	{
		m_criteria = criteria;
	}

    public QueryByCriteria getQuery()
    {
        if (getCriteria() != null)
        {
            return getCriteria().getQuery();
        }
        else
        {
            return null;
        }
    }
    
	/**
	 * Gets the pathClasses from the parent Criteria.
	 * A Map containing hints about what Class to be used for what path segment
	 * @return Returns a Map
	 */
	public Map getPathClasses()
	{
		return getCriteria().getPathClasses();
	}

	/**
	 * Get the a List of Class objects used as hints for a path
	 *
	 * @param aPath the path segment ie: allArticlesInGroup
	 * @return a List o Class objects to be used in SqlStatment
	 * @see org.apache.ojb.broker.QueryTest#testInversePathExpression()
	 */
	public List getClassesForPath(String aPath)
	{
		return getCriteria().getClassesForPath(aPath);
	}
}
