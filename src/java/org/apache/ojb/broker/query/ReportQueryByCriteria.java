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
 
import java.util.Map;

/**
 * Query for Reports. 
 * Supports selection of a subset of attributes.
 * 
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: ReportQueryByCriteria.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class ReportQueryByCriteria extends QueryByCriteria implements ReportQuery
{
	// define the attributes (columns) to be selected for reports
	private String[] m_attributes = null;

    // define the Jdbc-Types of the columns to be selected for reports
    private int[] m_jdbcTypes = null;

    // define the additional attributes (columns) to be used for the join
    private String[] m_joinAttributes = null;
    
    // attribute -> FieldDescriptor
    private Map m_attrToFld = null;

	/**
	 * Constructor for ReportQueryByCriteria.
	 * @param targetClass
	 * @param attributes[]
	 * @param criteria
	 * @param distinct
	 */
	public ReportQueryByCriteria(Class targetClass, String[] attributes, Criteria criteria, boolean distinct)
	{
		super(targetClass, criteria, distinct);
		setAttributes(attributes);
	}

	/**
	 * Constructor for ReportQueryByCriteria.
	 * @param targetClass
	 * @param attributes[]
	 * @param criteria
	 */
	public ReportQueryByCriteria(Class targetClass, String[] attributes, Criteria criteria)
	{
		this(targetClass, attributes, criteria, false);
	}

	/**
	 * Constructor for ReportQueryByCriteria.
	 * @param targetClass
	 * @param criteria
	 */
	public ReportQueryByCriteria(Class targetClass, Criteria criteria)
	{
		this(targetClass, null, criteria, false);
	}

	/**
	 * Constructor for ReportQueryByCriteria.
	 * @param targetClass
	 * @param criteria
	 * @param distinct
	 */
	public ReportQueryByCriteria(Class targetClass, Criteria criteria, boolean distinct)
	{
		this(targetClass, null, criteria, distinct);
	}

	/**
	 * Gets the columns.
	 * @return Returns a String[]
     * @deprecated use getAttributes()
	 */
	public String[] getColumns()
	{
		return getAttributes();
	}

	/**
	 * Sets the columns.
	 * @param columns The columns to set
     * @deprecated use setAttributes()
	 */
	public void setColumns(String[] columns)
	{
		setAttributes(columns);
	}

    /**
     * Gets the attributes to be selected.</br>
     * Attributes are translated into db-columns
     * @return the attributes to be selected
     */
    public String[] getAttributes()
    {
        return m_attributes;
    }

    /**
     * Sets the attributes to be selected.</br>
     * Attributes are translated into db-columns
     * @param attributes The attributes to set
     */
    public void setAttributes(String[] attributes)
    {
        m_attributes = attributes;
    }
    
    /**
     * @return Returns the jdbcTypes.
     */
    public int[] getJdbcTypes()
    {
        return m_jdbcTypes;
    }

    /**
     * @param jdbcTypes The jdbcTypes to set.
     */
    public void setJdbcTypes(int[] jdbcTypes)
    {
        this.m_jdbcTypes = jdbcTypes;
    }

    /**
     * @return Returns the joinAttributes.
     */
    public String[] getJoinAttributes()
    {
        return m_joinAttributes;
    }

    /**
     * @param joinAttributes The joinAttributes to set.
     */
    public void setJoinAttributes(String[] joinAttributes)
    {
        m_joinAttributes = joinAttributes;
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        String[] cols = getAttributes();
        StringBuffer buf = new StringBuffer("ReportQuery from ");
        buf.append(getSearchClass() + " ");
        if (cols != null)
        {
            for (int i = 0; i < cols.length; i++)
            {
                buf.append(cols[i] + " ");
            }
        }
        if (getCriteria() != null && !getCriteria().isEmpty())
        {
            buf.append(" where " + getCriteria());
        }

        return buf.toString();
    }

    public Map getAttributeFieldDescriptors()
    {
        return m_attrToFld;
    }

    public void setAttributeFieldDescriptors(Map attrToFld)
    {
        m_attrToFld = attrToFld;
    }


    
}
