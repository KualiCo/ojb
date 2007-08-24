package org.apache.ojb.broker.accesslayer;

/* Copyright 2003-2005 The Apache Software Foundation
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

import java.util.HashMap;
import java.util.Map;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;

/**
 * Default Implementation of QueryCustomizer.
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: QueryCustomizerDefaultImpl.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class QueryCustomizerDefaultImpl implements QueryCustomizer
{

    private Map m_attributeList = null;

	/**
	 * Default Constructor
	 */
	public QueryCustomizerDefaultImpl()
	{
		super();
	}

    /**
     * Default implementation returns unmodified original Query
     *
     * @see org.apache.ojb.broker.accesslayer.QueryCustomizer#customizeQuery
     */
    public Query customizeQuery(Object anObject, PersistenceBroker aBroker, CollectionDescriptor aCod, QueryByCriteria aQuery)
    {
        return aQuery;
    }


    /**
     * @see org.apache.ojb.broker.metadata.AttributeContainer#addAttribute(String, String)
     */
   public void addAttribute(String attributeName, String attributeValue)
   {
        if (attributeName==null)
        {
            return;
        }
        if (m_attributeList==null)
        {
            m_attributeList=new HashMap();
        }
       m_attributeList.put(attributeName,attributeValue);
    }

	/* (non-Javadoc)
	 * @see org.apache.ojb.broker.metadata.AttributeContainer#getAttribute(java.lang.String, java.lang.String)
	 */
	public String getAttribute(String attributeName, String defaultValue)
	{
        String result = defaultValue;
        if (m_attributeList!=null)
        {
            result = (String)m_attributeList.get(attributeName);
            if (result==null)
            {
                result = defaultValue;
            }
        }
        return result;
	}

	/* (non-Javadoc)
	 * @see org.apache.ojb.broker.metadata.AttributeContainer#getAttribute(java.lang.String)
	 */
	public String getAttribute(String attributeName)
	{
        return this.getAttribute(attributeName,null);
	}

}
