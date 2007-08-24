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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.FieldHelper;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * represents a search by criteria.
 * "find all articles where article.price > 100"
 * could be represented as:
 *
 * Criteria crit = new Criteria();
 * crit.addGreaterThan("price", new Double(100));
 * Query qry = new QueryByCriteria(Article.class, crit);
 *
 * The PersistenceBroker can retrieve Objects by Queries as follows:
 *
 * PersistenceBroker broker = PersistenceBrokerFactory.createPersistenceBroker();
 * Collection col = broker.getCollectionByQuery(qry);
 *
 * Creation date: (24.01.2001 21:45:46)
 * @author Thomas Mahler
 * @version $Id: QueryByCriteria.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class QueryByCriteria extends AbstractQueryImpl
{
    private Criteria m_criteria;
    private boolean m_distinct = false;
    private Map m_pathClasses;
    private Criteria m_havingCriteria;
	private String m_objectProjectionAttribute;

    // holding FieldHelper for orderBy and groupBy
    private List m_orderby = null;
    private List m_groupby = null;

    // list of names of prefetchable relationships
    private List m_prefetchedRelationships = null;

    private Collection m_pathOuterJoins = null;

    /**
     * handy criteria that can be used to select all instances of
     * a class.
     */
    public static final Criteria CRITERIA_SELECT_ALL = null;

    /**
     * Build a Query for class targetClass with criteria.
     * Criteriy may be null (will result in a query returning ALL objects from a table)
     */
    public QueryByCriteria(Class targetClass, Criteria whereCriteria, Criteria havingCriteria, boolean distinct)
    {
        super (targetClass);

        setCriteria(whereCriteria);
        setHavingCriteria(havingCriteria);

        m_distinct = distinct;
        m_pathClasses = new HashMap();
        m_groupby = new ArrayList();
        m_orderby = new ArrayList();
        m_prefetchedRelationships = new ArrayList();
        m_pathOuterJoins = new HashSet();
    }

    /**
     * Build a Query for class targetClass with criteria.
     * Criteriy may be null (will result in a query returning ALL objects from a table)
     */
    public QueryByCriteria(Class targetClass, Criteria whereCriteria, Criteria havingCriteria)
    {
        this(targetClass, whereCriteria, havingCriteria, false);
    }


    /**
     * Build a Query for class targetClass with criteria.
     * Criteriy may be null (will result in a query returning ALL objects from a table)
     */
    public QueryByCriteria(Class targetClass, Criteria criteria)
    {
        this(targetClass, criteria, false);
    }

    /**
     * Build a Query for class targetClass with criteria.
     * Criteriy may be null (will result in a query returning ALL objects from a table)
     */
    public QueryByCriteria(Class targetClass, Criteria criteria, boolean distinct)
    {
        this(targetClass, criteria, null, distinct);
    }

    /**
     * Build a Query based on anObject <br>
     * all non null values are used as EqualToCriteria
     */
    public QueryByCriteria(Object anObject, boolean distinct)
    {
        this(anObject.getClass(), buildCriteria(anObject), distinct);
    }

    /**
     * Build a Query based on anObject <br>
     * all non null values are used as EqualToCriteria
     */
    public QueryByCriteria(Object anObject)
    {
        this(anObject.getClass(), buildCriteria(anObject));
    }

    /**
     * Build a Query based on a Class Object. This
     * Query will return all instances of the given class.
     * @param aClassToSearchFrom the class to search from
     */
    public QueryByCriteria(Class aClassToSearchFrom)
    {
        this(aClassToSearchFrom, CRITERIA_SELECT_ALL);
    }

    /**
     * Build Criteria based on example object<br>
     * all non null values are used as EqualToCriteria
     */
    private static Criteria buildCriteria(Object anExample)
    {
        Criteria criteria = new Criteria();
        ClassDescriptor cld = MetadataManager.getInstance().getRepository().getDescriptorFor(anExample.getClass());
        FieldDescriptor[] fds = cld.getFieldDescriptions();
        PersistentField f;
        Object value;

        for (int i = 0; i < fds.length; i++)
        {
            try
            {
                f = fds[i].getPersistentField();
                value = f.get(anExample);
                if (value != null)
                {
                    criteria.addEqualTo(f.getName(), value);
                }
            }
            catch (Throwable ex)
            {
                LoggerFactory.getDefaultLogger().error(ex);
            }
        }

        return criteria;
    }

    /**
     * Add a hint Class for a path. Used for relationships to extents.<br>
     * SqlStatment will use these hint classes when resolving the path.
     * Without these hints SqlStatment will use the base class the
     * relationship points to ie: Article instead of CdArticle.
     *
     * @param aPath the path segment ie: allArticlesInGroup
     * @param aClass the Class ie: CdArticle
     * @see org.apache.ojb.broker.QueryTest#testInversePathExpression()
     */
    public void addPathClass(String aPath, Class aClass)
    {
        List pathClasses = (List) m_pathClasses.get(aPath);
        if(pathClasses == null)
        {
            setPathClass(aPath, aClass);
        }
        else
        {
            pathClasses.add(aClass);
        }
    }

    /**
     * Set the Class for a path. Used for relationships to extents.<br>
     * SqlStatment will use this class when resolving the path.
     * Without this hint SqlStatment will use the base class the
     * relationship points to ie: Article instead of CdArticle.
     * Using this method is the same as adding just one hint
     *
     * @param aPath the path segment ie: allArticlesInGroup
     * @param aClass the Class ie: CdArticle
     * @see org.apache.ojb.broker.QueryTest#testInversePathExpression()
     * @see #addPathClass
     */
    public void setPathClass(String aPath, Class aClass)
    {
        List pathClasses = new ArrayList();
        pathClasses.add(aClass);
        m_pathClasses.put(aPath, pathClasses);
    }

    /**
     * Get the a List of Class objects used as hints for a path
     *
     * @param aPath the path segment ie: allArticlesInGroup
     * @return a List o Class objects to be used in SqlStatment
     * @see #addPathClass
     * @see org.apache.ojb.broker.QueryTest#testInversePathExpression()
     */
    public List getClassesForPath(String aPath)
    {
        return (List)m_pathClasses.get(aPath);
    }

    /**
     * Answer true if outer join for path should be used.
     * @param aPath the path to query the outer join setting for
     * @return true for outer join
     */
    public boolean isPathOuterJoin(String aPath)
    {
        return getOuterJoinPaths().contains(aPath);
    }

    /**
     * Force outer join for the last segment of the path.
     * ie. path = 'a.b.c' the outer join will be applied only to the relationship from B to C.
     * if multiple segments need an outer join, setPathOuterJoin needs to be called for each segement.
     * @param aPath force outer join to the last segment of this path
     */
    public void setPathOuterJoin(String aPath)
    {
        getOuterJoinPaths().add(aPath);
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.query.Query#getCriteria()
     */
    public Criteria getCriteria()
    {
        return m_criteria;
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.query.Query#getHavingCriteria()
     */
    public Criteria getHavingCriteria()
    {
        return m_havingCriteria;
    }

    /**
     * Insert the method's description here.
     * Creation date: (07.02.2001 22:01:55)
     * @return java.lang.String
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer("QueryByCriteria from ");
        buf.append(getSearchClass()).append(" ");
        if (getCriteria() != null && !getCriteria().isEmpty())
        {
            buf.append(" where ").append(getCriteria());
        }
        return buf.toString();
    }

    /**
     * Gets the distinct.
     * @return Returns a boolean
     */
    public boolean isDistinct()
    {
        return m_distinct;
    }

    /**
     * Sets the distinct.
     * @param distinct The distinct to set
     */
    public void setDistinct(boolean distinct)
    {
        this.m_distinct = distinct;
    }

    /**
     * Gets the pathClasses.
     * A Map containing hints about what Class to be used for what path segment
     * @return Returns a Map
     */
    public Map getPathClasses()
    {
        return m_pathClasses;
    }

	/**
	 * Sets the criteria.
	 * @param criteria The criteria to set
	 */
	public void setCriteria(Criteria criteria)
	{
		m_criteria = criteria;
        if (m_criteria != null)
        {
            m_criteria.setQuery(this);
        }
	}

	/**
	 * Sets the havingCriteria.
	 * @param havingCriteria The havingCriteria to set
	 */
	public void setHavingCriteria(Criteria havingCriteria)
	{
		m_havingCriteria = havingCriteria;
        if (m_havingCriteria != null)
        {
            m_havingCriteria.setQuery(this);
        }
	}

    /**
     * Adds a groupby fieldName for ReportQueries.
     * @param fieldName The groupby to set
     */
    public void addGroupBy(String fieldName)
    {
        if (fieldName != null)
        {
            m_groupby.add(new FieldHelper(fieldName, false));
        }
    }

    /**
     * Adds a field for groupby
     * @param aField
     */
    public void addGroupBy(FieldHelper aField)
    {
        if (aField != null)
        {
            m_groupby.add(aField);
        }
    }

    /**
     * Adds an array of groupby fieldNames for ReportQueries.
     * @param fieldNames The groupby to set
     */
    public void addGroupBy(String[] fieldNames)
    {
        for (int i = 0; i < fieldNames.length; i++)
        {
            addGroupBy(fieldNames[i]);
        }
    }

	/**
	 * @see org.apache.ojb.broker.query.Query#getGroupBy()
	 */
	public List getGroupBy()
	{
        // BRJ:
        // combine data from query and criteria
        // TODO: to be removed when Criteria#addGroupBy is removed
        ArrayList temp = new ArrayList();
        temp.addAll(m_groupby);

        if (getCriteria() != null)
        {
            temp.addAll(getCriteria().getGroupby());
        }

        return temp;
	}

    /**
     * Adds a field for orderBy
     * @param  fieldName    The field name to be used
     * @param  sortAscending    true for ASCENDING, false for DESCENDING
     */
    public void addOrderBy(String fieldName, boolean sortAscending)
    {
        if (fieldName != null)
        {
            m_orderby.add(new FieldHelper(fieldName, sortAscending));
        }
    }

    /**
     * Adds a field for orderBy, order is ASCENDING
     * @param  fieldName    The field name to be used
     * @deprecated use #addOrderByAscending(String fieldName)
     */
    public void addOrderBy(String fieldName)
    {
        addOrderBy(fieldName, true);
    }

    /**
     * Adds a field for orderBy
     * @param aField
     */
    public void addOrderBy(FieldHelper aField)
    {
        if (aField != null)
        {
            m_orderby.add(aField);
        }
    }

    /**
     * Adds a field for orderBy ASCENDING
     * @param  fieldName    The field name to be used
     */
    public void addOrderByAscending(String fieldName)
    {
        addOrderBy(fieldName, true);
    }

    /**
     * Adds a field for orderBy DESCENDING
     * @param  fieldName    The field name to be used
     */
    public void addOrderByDescending(String fieldName)
    {
        addOrderBy(fieldName, false);
    }

	/**
	 * @see org.apache.ojb.broker.query.Query#getOrderBy()
	 */
	public List getOrderBy()
	{
        // BRJ:
        // combine data from query and criteria
        // TODO: to be removed when Criteria#addOrderBy is removed
        ArrayList temp = new ArrayList();
        temp.addAll(m_orderby);

        if (getCriteria() != null)
        {
            temp.addAll(getCriteria().getOrderby());
        }

        return temp;
	}

    /**
     * add the name of aRelationship for prefetched read
     */
    public void addPrefetchedRelationship(String aName)
    {
        m_prefetchedRelationships.add(aName);
    }

	/* (non-Javadoc)
	 * @see org.apache.ojb.broker.query.Query#getPrefetchedRelationships()
	 */
	public List getPrefetchedRelationships()
	{
        // BRJ:
        // combine data from query and criteria
        // TODO: to be removed when Criteria#addPrefetchedRelationship is removed
        ArrayList temp = new ArrayList();
        temp.addAll(m_prefetchedRelationships);

        if (getCriteria() != null)
        {
            temp.addAll(getCriteria().getPrefetchedRelationships());
        }

        return temp;
    }

	/**
     * Get a Collection containing all Paths having an Outer-Joins-Setting
     * @return a Collection containing the Paths (Strings)
	 */
    public Collection getOuterJoinPaths()
    {
        return m_pathOuterJoins;
    }

    public String getObjectProjectionAttribute()
    {
        return m_objectProjectionAttribute;
    }

    /**
     * Use this method to query some related class by object references,
     * for example query.setObjectProjectionAttribute("ref1.ref2.ref3");
     */
    public void setObjectProjectionAttribute(String objectProjectionAttribute)
    {
        ClassDescriptor baseCld = MetadataManager.getInstance().getRepository().getDescriptorFor(m_baseClass);
        ArrayList descs = baseCld.getAttributeDescriptorsForPath(objectProjectionAttribute);
        int pathLen = descs.size();

        if ((pathLen > 0) && (descs.get(pathLen - 1) instanceof ObjectReferenceDescriptor))
        {
            ObjectReferenceDescriptor ord =
                    ((ObjectReferenceDescriptor) descs.get(pathLen - 1));
            setObjectProjectionAttribute(objectProjectionAttribute,
                                         ord.getItemClass());
        }
    }

    public void setObjectProjectionAttribute(String objectProjectionAttribute,
                                             Class objectProjectionClass)
    {
        m_objectProjectionAttribute = objectProjectionAttribute;
        m_searchClass = objectProjectionClass;
	}
}