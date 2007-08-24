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

import org.apache.ojb.broker.Identity;

/**
 * Represents a search by identity.
 * <i>"find the article with id 7"</i>
 * could be represented as:<br/>
 * <br/>
 *
 * <code>
 * Article example = new Article();<br/>
 * example.setId(7);<br/>
 * Query qry = new QueryByIdentity(example);<br/>
 * </code>
 * <br/>
 * The PersistenceBroker can retrieve Objects by examples as follows:<br/>
 * <br/>
 * <code>
 * PersistenceBroker broker = PersistenceBrokerFactory.createPersistenceBroker();<br/>
 * Collection col = broker.getObjectByQuery(qry);<br/>
 * </code>
 * <br/>
 * This Class can also handle working with OJB Identity objects:
 * <i>"find the article with Identity xyz"</i> could be represnted as<br/>
 * <br/>
 * <code>
 * Article example = new Article();<br/>
 * example.setId(7);<br/>
 * Identity xyz = broker.serviceIdentity().buildIdentity(example);<br/>
 * Query qry = new QueryByIdentity(xyz);<br/>
 * Collection col = broker.getObjectByQuery(qry);<br/>
 * </code>
 * <br/>
 * But in this case a smarter solution will be<br/>
 * <br/>
 * <code>
 * Identity xyz = broker.serviceIdentity().buildIdentity(Article.class, new Integer(7));<br/>
 * Collection col = broker.getObjectByIdentity(xyz);<br/>
 * </code>
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: QueryByIdentity.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class QueryByIdentity extends AbstractQueryImpl
{
	private Object m_exampleObjectOrIdentity;

	/**
	 * QueryByIdentity can be generated from example Objects or by Identity Objects
	 */
	public QueryByIdentity(Object example_or_identity)
	{
		m_exampleObjectOrIdentity = example_or_identity;
	}

	/**
	 * Answer the example Object
	 * @return the example Object or an Identity
	 */
	public Object getExampleObject()
	{
		return m_exampleObjectOrIdentity;
	}

	/**
	 * Answer the search class.
	 * This is the class of the example object or
	 * the class represented by Identity.
	 * @return Class
	 */
	public Class getSearchClass()
	{
		Object obj = getExampleObject();

		if (obj instanceof Identity)
		{
			return ((Identity) obj).getObjectsTopLevelClass();
		}
		else
		{
			return obj.getClass();
		}
	}

}
