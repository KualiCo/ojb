package org.apache.ojb.broker.metadata;

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
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.accesslayer.QueryCustomizer;


/**
 * mapping Description for member fields that are Collections
 * <br>
 * Note: Be careful when use references of this class or caching instances of this class,
 * because instances could become invalid (see {@link MetadataManager}).
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: CollectionDescriptor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class CollectionDescriptor extends ObjectReferenceDescriptor
{
    private static final long serialVersionUID = -8570280662286424937L;

    /**
     * Represents the type of the collection, if set to null,
     * a java.util.Vector will be used.
     * If set to a valid collection type it will be used to build typed collections.
     */
    private Class collectionClass = null;
    /**
     * the Collection of orderby Fields
     */
    private Collection m_orderby = new ArrayList();
    /**
     * For m:n related Classes this is the indirection table.
     */
    private String indirectionTable = null;
    private Vector fksToItemClass = null;
    private Vector fksToThisClass = null;
    private String[] fksToItemClassAry;
    private String[] fksToThisClassAry;
    private QueryCustomizer m_queryCustomizer;
    private Boolean m_hasProxyItems;

    public CollectionDescriptor(ClassDescriptor descriptor)
    {
        super(descriptor);
    }

    public String[] getFksToThisClass()
    {
        if (fksToThisClassAry == null)
        {
            fksToThisClassAry = (String[]) fksToThisClass.toArray(
                    new String[fksToThisClass.size()]);
        }
        return fksToThisClassAry;
    }

    public void setFksToThisClass(Vector fksToThisClass)
    {
        this.fksToThisClass = fksToThisClass;
        fksToThisClassAry = null;
    }

    /**
     * add a FK column pointing to This Class
     */
    public void addFkToThisClass(String column)
    {
        if (fksToThisClass == null)
        {
            fksToThisClass = new Vector();
        }
        fksToThisClass.add(column);
        fksToThisClassAry = null;
    }

    /**
     * add a FK column pointing to the item Class
     */
    public void addFkToItemClass(String column)
    {
        if (fksToItemClass == null)
        {
            fksToItemClass = new Vector();
        }
        fksToItemClass.add(column);
        fksToItemClassAry = null;
    }

    /**
     * returns the type of the collection.
     * @return java.lang.Class
     */
    public Class getCollectionClass()
    {
        return collectionClass;
    }

    /**
     * set the type of the collection
     * @param c the collection type
     */
    public void setCollectionClass(Class c)
    {
        collectionClass = c;
    }

    /**
     * Retrieve the classname of the collection.
     */
    public String getCollectionClassName()
    {
        return collectionClass != null ? collectionClass.getName() : null;
    }

    public String getIndirectionTable()
    {
        return indirectionTable;
    }

    public void setIndirectionTable(String indirectionTable)
    {
        this.indirectionTable = indirectionTable;
    }

    public String[] getFksToItemClass()
    {
        if (fksToItemClassAry == null)
        {
            fksToItemClassAry = (String[]) fksToItemClass.toArray(
                    new String[fksToItemClass.size()]);
        }
        return fksToItemClassAry;
    }

    public void setFksToItemClass(Vector fksToItemClass)
    {
        this.fksToItemClass = fksToItemClass;
        fksToItemClassAry = null;
    }

    public boolean isMtoNRelation()
    {
        return (indirectionTable != null);
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
     * Returns the orderby Collection of Fields.
     * @return Collection
     */
    public Collection getOrderBy()
    {
        return m_orderby;
    }

    protected int getCascadeDeleteValue(String cascade)
    {
        if(cascade.equalsIgnoreCase("false") && isMtoNRelation())
        {
            /*
            "old" implementation does always delete entries in indirection table for
            m:n relations. For 1:n relations referenced objects are not touched.
            */
            return CASCADE_LINK;
        }
        return super.getCascadeDeleteValue(cascade);
    }

    /*
     * @see XmlCapable#toXML()
     */
    public String toXML()
    {
        RepositoryTags tags = RepositoryTags.getInstance();
        String eol = SystemUtils.LINE_SEPARATOR;

        // write opening tag
        String result = "      " + tags.getOpeningTagNonClosingById(COLLECTION_DESCRIPTOR) + eol;

        // write attributes
        // name
        result       += "        " + tags.getAttribute(FIELD_NAME,this.getAttributeName()) + eol;

        // collection class is optional
        if (getCollectionClassName() != null)
        {
            result       += "        " + tags.getAttribute(COLLECTION_CLASS,this.getCollectionClassName()) + eol;
        }

        // element-class-ref
         result       += "        " + tags.getAttribute(ITEMS_CLASS,this.getItemClassName()) + eol;

        // indirection-table is optional
        if (isMtoNRelation())
        {
             result += "        " + tags.getAttribute(INDIRECTION_TABLE,getIndirectionTable()) + eol;
        }

        // proxyReference is optional, disabled by default
        if (isLazy())
        {
            result       += "        " + tags.getAttribute(PROXY_REFERENCE,"true") + eol;
            result       += "        " + tags.getAttribute(PROXY_PREFETCHING_LIMIT, "" + this.getProxyPrefetchingLimit()) + eol;
        }

        //reference refresh is optional, disabled by default
        if (isRefresh())
        {
             result       += "        " + tags.getAttribute(REFRESH,"true") + eol;
        }

        //auto retrieve
        result += "        " + tags.getAttribute(AUTO_RETRIEVE, "" + getCascadeRetrieve()) + eol;

        //auto update
        result += "        " + tags.getAttribute(AUTO_UPDATE, getCascadeAsString(getCascadingStore())) + eol;

        //auto delete
        result += "        " + tags.getAttribute(AUTO_DELETE, getCascadeAsString(getCascadingDelete())) + eol;

        //otm-dependent is optional, disabled by default
        if (getOtmDependent())
        {
            result += "        " + tags.getAttribute(OTM_DEPENDENT, "true") + eol;
        }

        // close opening tag
        result       += "      >" + eol;

        // write elements
         // inverse fk elements
        for (int i=0;i<getForeignKeyFields().size();i++)
        {
        Object obj = getForeignKeyFields().get(i);
        if (obj instanceof Integer)
        {
                String fkId = obj.toString();
            result += "        " + tags.getOpeningTagNonClosingById(INVERSE_FK) + " ";
                result += tags.getAttribute(FIELD_ID_REF, fkId) + "/>" + eol;
        }
        else
        {
                String fk = (String) obj;
            result += "        " + tags.getOpeningTagNonClosingById(INVERSE_FK) + " ";
                result += tags.getAttribute(FIELD_REF, fk) + "/>" + eol;
        }
        }

        // write optional M:N elements
        // m:n relationship settings, optional
        if (isMtoNRelation())
        {
            // foreign keys to this class
             for (int i=0;i<getFksToThisClass().length;i++)
             {
                String fkId = getFksToThisClass()[i];
                result += "        " + tags.getOpeningTagNonClosingById(FK_POINTING_TO_THIS_CLASS) + " ";
                result += tags.getAttribute(COLUMN_NAME, fkId) + "/>" + eol;
             }

            // foreign keys to item class
             for (int i=0;i<getFksToItemClass().length;i++)
             {
                String fkId = getFksToItemClass()[i];
                result += "        " + tags.getOpeningTagNonClosingById(FK_POINTING_TO_ITEMS_CLASS) + " ";
                result += tags.getAttribute(COLUMN_NAME, fkId) + "/>" + eol;
             }
        }

        // closing tag
        result       += "      " + tags.getClosingTagById(COLLECTION_DESCRIPTOR) + eol;
        return result;
    }

	/**
	 * @return QueryCustomizer
	 */
	public QueryCustomizer getQueryCustomizer()
	{
		return m_queryCustomizer;
	}

	/**
	 * Sets the queryCustomizer.
	 * @param queryCustomizer The queryCustomizer to set
	 */
	public void setQueryCustomizer(QueryCustomizer queryCustomizer)
	{
		m_queryCustomizer = queryCustomizer;
	}

    public boolean hasProxyItems() throws PersistenceBrokerException
    {
        if (m_hasProxyItems == null)
        {
            DescriptorRepository repo = getClassDescriptor().getRepository();
            ClassDescriptor cld = repo.getDescriptorFor(getItemClass());
            if (cld.getProxyClass() != null)
            {
                m_hasProxyItems = Boolean.TRUE;
            }
            else
            {
                Collection extents = cld.getExtentClasses();
                m_hasProxyItems = Boolean.FALSE;
                for (Iterator it = extents.iterator(); it.hasNext(); )
                {
                    Class ext = (Class) it.next();
                    ClassDescriptor cldExt = repo.getDescriptorFor(ext);
                    if (cldExt.getProxyClass() != null)
                    {
                        m_hasProxyItems = Boolean.TRUE;
                        break;
                    }
                }
            }
        }

        return (m_hasProxyItems.booleanValue());
    }
}
