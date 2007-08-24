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

/**
 * Abstract superclass for Criteria using a field to compare with
 * 
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: FieldCriteria.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class FieldCriteria extends SelectionCriteria
{
	// PAW
//	static FieldCriteria buildEqualToCriteria(Object anAttribute, Object aValue, String anAlias)
	static FieldCriteria buildEqualToCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
    {
        return new FieldCriteria(anAttribute, aValue, EQUAL, anAlias);
    }

	// PAW
//	static FieldCriteria buildNotEqualToCriteria(Object anAttribute, Object aValue, String anAlias)
	static FieldCriteria buildNotEqualToCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
    {
        return new FieldCriteria(anAttribute, aValue, NOT_EQUAL, anAlias);
    }

	// PAW
//	static FieldCriteria buildGreaterCriteria(Object anAttribute, Object aValue, String anAlias)
	static FieldCriteria buildGreaterCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
    {
        return new FieldCriteria(anAttribute, aValue,GREATER, anAlias);
    }

	// PAW
//	static FieldCriteria buildNotGreaterCriteria(Object anAttribute, Object aValue, String anAlias)
	static FieldCriteria buildNotGreaterCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
    {
        return new FieldCriteria(anAttribute, aValue, NOT_GREATER, anAlias);
    }

	// PAW
//	static FieldCriteria buildLessCriteria(Object anAttribute, Object aValue, String anAlias)
	static FieldCriteria buildLessCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
    {
        return new FieldCriteria(anAttribute, aValue, LESS, anAlias);
    }

	// PAW
//	static FieldCriteria buildNotLessCriteria(Object anAttribute, Object aValue, String anAlias)
	static FieldCriteria buildNotLessCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
    {
        return new FieldCriteria(anAttribute, aValue, NOT_LESS, anAlias);
    }

	// BRJ: indicate whether field name should be translated into column name
	private boolean m_translateField = true;
    private String m_clause;

	/**
	 * Constructor declaration
	 *
	 * @param anAttribute  column- or fieldName
	 * @param aValue  the value to compare with
	 * @param negative  criteria is negated (ie NOT LIKE instead of LIKE)
	 * @param alias  use alias to link anAttribute to
	 */
	// PAW
//	FieldCriteria(Object anAttribute, Object aValue, String aClause, String alias)
	FieldCriteria(Object anAttribute, Object aValue, String aClause, UserAlias alias)
	{
		super(anAttribute, aValue, alias);
        m_clause = aClause;
	}

    /**
     * @see SelectionCriteria#isBindable()
     */
    protected boolean isBindable()
    {
        return false;
    }

	/**
	 * @return true if field name should be translated into column name
	 */
	public boolean isTranslateField()
	{
		return m_translateField;
	}

	/**
	 * @param b
	 */
	void setTranslateField(boolean b)
	{
		m_translateField = b;
	}

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.query.SelectionCriteria#getClause()
     */
    public String getClause()
    {
        return m_clause;
    }
}

