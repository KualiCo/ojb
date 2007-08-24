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
 * SelectionCriteria for 'is null' and 'is not null'
 * 
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: NullCriteria.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class NullCriteria extends ValueCriteria
{
	NullCriteria(String anAttribute, String aClause, String anAlias)
	{
		super(anAttribute, null, aClause, anAlias);
	}

	NullCriteria(String anAttribute, String aClause, UserAlias anAlias)
	{
		super(anAttribute, null, aClause, anAlias);
	}

    public String toString()
    {
        return getAttribute() + getClause();
    }

    /**
     * @see SelectionCriteria#isBound()
     */
    public boolean isBound()
    {
        return true;
    }

    /**
     * @see SelectionCriteria#isBindable()
     */
    protected boolean isBindable()
    {
        return false;
    }

}
