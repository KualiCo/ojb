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
 * SelectionCriteria for free form sql "REVERSE(name) like 're%'"
 * 
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: SqlCriteria.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class SqlCriteria extends SelectionCriteria
{

    /**
     * Constructor for SqlCriteria.
     * @param anSqlStatement
     */
    SqlCriteria(String anSqlStatement)
    {
        super(anSqlStatement, null, (String)null);
    }

    /*
	 * @see SelectionCriteria#getClause()
	 */
    public String getClause()
    {
        return (String)getAttribute();
    }

	/**
	 * @see SelectionCriteria#isBindable()
	 */
	protected boolean isBindable()
	{
		return false;
	}

}

