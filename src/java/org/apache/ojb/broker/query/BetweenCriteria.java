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
 * SelectionCriteria for 'between x and y'
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: BetweenCriteria.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class BetweenCriteria extends ValueCriteria
{
    private Object value2;
	
	BetweenCriteria(Object anAttribute, Object aValue1, Object aValue2, String aClause, String anAlias)
	{
		super(anAttribute, aValue1, aClause, anAlias);
		setValue2(aValue2);
	}

	// PAW
	BetweenCriteria(Object anAttribute, Object aValue1, Object aValue2, String aClause, UserAlias aUserAlias)
	{
		super(anAttribute, aValue1, aClause, aUserAlias);
		setValue2(aValue2);
	}

    /**
     * sets the value of the criteria to newValue. 
     * Used by the ODMG OQLQuery.bind() operation
     * BRJ: bind get's called twice so we need to know which value to set
     */
    public void bind(Object newValue)
    {
    	if (getValue() == null)
    	{
    		setValue(newValue);
    	}	
    	else
    	{
    		setValue2(newValue);
    		setBound(true); 		
    	}
    }


	/**
	 * Gets the value2.
	 * @return Returns a Object
	 */
	public Object getValue2()
	{
		return value2;
	}

	/**
	 * Sets the value2.
	 * @param value2 The value2 to set
	 */
	protected void setValue2(Object value2)
	{
		this.value2 = value2;
	}

	/**
	 * @see org.apache.ojb.broker.query.SelectionCriteria#isBindable()
	 */
	protected boolean isBindable()
	{
		return (getValue() == null && getValue2() == null);
	}

	// PAW
	/**
	 * String representation
	 */
	public String toString()
	{
		return super.toString() + " AND " + value2;
	}
}

