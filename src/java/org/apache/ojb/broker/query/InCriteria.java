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

import java.util.Collection;

/**
 * SelectionCriteria for 'in (a,b,c..)'
 * 
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: InCriteria.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class InCriteria extends ValueCriteria
{
	InCriteria(Object anAttribute, Object aValue, String aClause, String anAlias)
	{
		super(anAttribute, aValue, aClause, anAlias);
	}
 
	InCriteria(Object anAttribute, Object aValue, String aClause, UserAlias anAlias)
	{
		super(anAttribute, aValue, aClause, anAlias);
	}
 
	/**
	 * @see org.apache.ojb.broker.query.SelectionCriteria#isBindable()
	 * BRJ: empty Collection is bindable
	 */
	protected boolean isBindable()
	{
		if (getValue() instanceof Collection)
		{
			Collection coll = (Collection)getValue();
			return coll.isEmpty();
		}	
		else
		{
			return true;
		}	
	}

}

