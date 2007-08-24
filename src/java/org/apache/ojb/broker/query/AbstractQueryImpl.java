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

import java.io.Serializable;
import java.util.List;

/**
 * Abstract implemenation of Query interface
 *
 * @author ???
 * @version $Id: AbstractQueryImpl.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public abstract class AbstractQueryImpl implements Query, Serializable
{
	static final long serialVersionUID = -6265085604410295816L;
    
    private int m_startAtIndex = Query.NO_START_AT_INDEX;
    private int m_endAtIndex = Query.NO_END_AT_INDEX;
    private int m_fullSize = 0;
    private int fetchSize;
    protected Class m_searchClass;
    protected Class m_baseClass;
    private boolean m_withExtents = true;

    public AbstractQueryImpl()
    {
    }

    public AbstractQueryImpl(Class aSearchClass)
    {
        m_searchClass = aSearchClass;
        m_baseClass = aSearchClass;
    }

    public int getStartAtIndex()
    {
        return m_startAtIndex;
    }

    public void setStartAtIndex(int startAtIndex)
    {
        m_startAtIndex = startAtIndex;
    }

    public int getEndAtIndex()
    {
        return m_endAtIndex;
    }
    
    public void setEndAtIndex(int endAtIndex)
    {
        m_endAtIndex = endAtIndex;
    }
    
    public void fullSize(int size)
    {
        m_fullSize = size;
    }
    
    public int fullSize()
    {
        return m_fullSize;
    }
    
    public void setWithExtents(boolean withExtents)
    {
        m_withExtents = withExtents;
    }
    
    public boolean getWithExtents()
    {
        return m_withExtents;
    }

    /*
     * @see Query#getSearchClass()
     */
    public Class getSearchClass()
    {
        return m_searchClass;
    }

    /*
     * @see Query#getBaseClass()
     */
    public Class getBaseClass()
    {
        return m_baseClass;
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.query.Query#getGroupBy()
     */
    public List getGroupBy()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.query.Query#getOrderBy()
     */
    public List getOrderBy()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.query.Query#getPrefetchedRelationships()
     */
    public List getPrefetchedRelationships()
    {
        return null;
    }
    


	/* (non-Javadoc)
	 * @see org.apache.ojb.broker.query.Query#getCriteria()
	 */
	public Criteria getCriteria()
	{
		return null;
	}

	/* (non-Javadoc)
	 * @see org.apache.ojb.broker.query.Query#getExampleObject()
	 */
	public Object getExampleObject()
	{
		return null;
	}

	/* (non-Javadoc)
	 * @see org.apache.ojb.broker.query.Query#getHavingCriteria()
	 */
	public Criteria getHavingCriteria()
	{
		return null;
	}

	/* (non-Javadoc)
	 * @see org.apache.ojb.broker.query.Query#isDistinct()
	 */
	public boolean isDistinct()
	{
		return false;
	}

    public boolean usePaging()
    {
        return getEndAtIndex() > NO_END_AT_INDEX
            || getStartAtIndex() > NO_START_AT_INDEX;
    }

    public void setFetchSize(int fetchSize)
    {
        this.fetchSize = fetchSize;
    }

    public int getFetchSize()
    {
        return fetchSize;
    }

}
