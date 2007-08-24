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
 * Models a Criteria comparing an attribute to a value
 * <br>ie: name = 'Mark' , name like 'M%'
 * 
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: ValueCriteria.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */ 
public class ValueCriteria extends SelectionCriteria
{
	static ValueCriteria buildEqualToCriteria(Object anAttribute, Object aValue, String anAlias)
	{
		return new ValueCriteria(anAttribute, aValue, EQUAL, anAlias);
	}
	
	static ValueCriteria buildEqualToCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
	{
		return new ValueCriteria(anAttribute, aValue, EQUAL, anAlias);
	}

	static ValueCriteria buildNotEqualToCriteria(Object anAttribute, Object aValue, String anAlias)
	{
		return new ValueCriteria(anAttribute, aValue, NOT_EQUAL, anAlias);
	}
	static ValueCriteria buildNotEqualToCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
	{
		return new ValueCriteria(anAttribute, aValue, NOT_EQUAL, anAlias);
	}

	static ValueCriteria buildGreaterCriteria(Object anAttribute, Object aValue, String anAlias)
	{
		return new ValueCriteria(anAttribute, aValue,GREATER, anAlias);
	}
	static ValueCriteria buildGreaterCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
	{
		return new ValueCriteria(anAttribute, aValue,GREATER, anAlias);
	}

	static ValueCriteria buildNotGreaterCriteria(Object anAttribute, Object aValue, String anAlias)
	{
		return new ValueCriteria(anAttribute, aValue, NOT_GREATER, anAlias);
	}
	static ValueCriteria buildNotGreaterCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
	{
		return new ValueCriteria(anAttribute, aValue, NOT_GREATER, anAlias);
	}

	static ValueCriteria buildLessCriteria(Object anAttribute, Object aValue, String anAlias)
	{
		return new ValueCriteria(anAttribute, aValue, LESS, anAlias);
	}
	static ValueCriteria buildLessCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
	{
		return new ValueCriteria(anAttribute, aValue, LESS, anAlias);
	}

	static ValueCriteria buildNotLessCriteria(Object anAttribute, Object aValue, String anAlias)
	{
		return new ValueCriteria(anAttribute, aValue, NOT_LESS, anAlias);
	}
	static ValueCriteria buildNotLessCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
	{
		return new ValueCriteria(anAttribute, aValue, NOT_LESS, anAlias);
	}

	static ValueCriteria buildLikeCriteria(Object anAttribute, Object aValue, String anAlias)
	{
		return new LikeCriteria(anAttribute, aValue, LIKE, anAlias);
	}
	static ValueCriteria buildLikeCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
	{
		return new LikeCriteria(anAttribute, aValue, LIKE, anAlias);
	}

	static ValueCriteria buildNotLikeCriteria(Object anAttribute, Object aValue, String anAlias)
	{
		return new ValueCriteria(anAttribute, aValue, NOT_LIKE, anAlias);
	}
	static ValueCriteria buildNotLikeCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
	{
		return new LikeCriteria(anAttribute, aValue, NOT_LIKE, anAlias);
	}

	static InCriteria buildInCriteria(Object anAttribute, Object aValue, String anAlias)
	{
		return new InCriteria(anAttribute, aValue, IN, anAlias);
	}
	static InCriteria buildInCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
	{
		return new InCriteria(anAttribute, aValue, IN, anAlias);
	}

	static InCriteria buildNotInCriteria(Object anAttribute, Object aValue, String anAlias)
	{
		return new InCriteria(anAttribute, aValue, NOT_IN, anAlias);
	}
	static InCriteria buildNotInCriteria(Object anAttribute, Object aValue, UserAlias anAlias)
	{
		return new InCriteria(anAttribute, aValue, NOT_IN, anAlias);
	}

	static NullCriteria buildNullCriteria(String anAttribute, String anAlias)
	{
		return new NullCriteria(anAttribute, IS_NULL, anAlias);
	}
	static NullCriteria buildNullCriteria(String anAttribute, UserAlias anAlias)
	{
		return new NullCriteria(anAttribute, IS_NULL, anAlias);
	}

	static NullCriteria buildNotNullCriteria(String anAttribute, String anAlias)
	{
		return new NullCriteria(anAttribute, NOT_IS_NULL, anAlias);
	}
	static NullCriteria buildNotNullCriteria(String anAttribute, UserAlias anAlias)
	{
		return new NullCriteria(anAttribute, NOT_IS_NULL, anAlias);
	}
   
	static BetweenCriteria buildBeweenCriteria(Object anAttribute, Object aValue1, Object aValue2, String anAlias)
	{
		return new BetweenCriteria(anAttribute, aValue1, aValue2, BETWEEN, anAlias);
	}
	static BetweenCriteria buildBeweenCriteria(Object anAttribute, Object aValue1, Object aValue2, UserAlias anAlias)
	{
		return new BetweenCriteria(anAttribute, aValue1, aValue2, BETWEEN, anAlias);
	}

	static BetweenCriteria buildNotBeweenCriteria(Object anAttribute, Object aValue1, Object aValue2, String anAlias)
	{
		return new BetweenCriteria(anAttribute, aValue1, aValue2, NOT_BETWEEN, anAlias);
	}
	static BetweenCriteria buildNotBeweenCriteria(Object anAttribute, Object aValue1, Object aValue2, UserAlias anAlias)
	{
		return new BetweenCriteria(anAttribute, aValue1, aValue2, NOT_BETWEEN, anAlias);
	}
    
    private String m_clause;

	/**
	 * Constructor declaration
	 *
	 * @param anAttribute  column- or fieldName
	 * @param aValue  the value to compare with
	 * @param aClause the SQL compare clause (ie LIKE, = , IS NULL)
	 * @param anAlias use alias to link anAttribute to
	 */
	ValueCriteria(Object anAttribute, Object aValue, String aClause, String anAlias)
	{
		super(anAttribute, aValue, anAlias);
		m_clause = aClause;
	}

	/**
	 * Constructor declaration
	 *
	 * @param anAttribute  column- or fieldName
	 * @param aValue  the value to compare with
	 * @param aClause the SQL compare clause (ie LIKE, = , IS NULL)
	 * @param aUserAlias userAlias to link anAttribute to
	 */
	ValueCriteria(Object anAttribute, Object aValue, String aClause, UserAlias aUserAlias)
	{
		super(anAttribute, aValue, aUserAlias);
		m_clause = aClause;
	}

    /**
     * @see org.apache.ojb.broker.query.SelectionCriteria#getClause()
     */
    public String getClause()
    {
        return m_clause;
    }

}
