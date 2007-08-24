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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;

/**
 * Describes a Field containing a reference to another class. Provides handling for foreign keys etc.
 * <br>
 * Note: Be careful when use references of this class or caching instances of this class,
 * because instances could become invalid (see {@link MetadataManager}).
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 *
 */
public class ObjectReferenceDescriptor extends AttributeDescriptorBase implements XmlCapable
{
    private static final long serialVersionUID = 5561562217150972131L;

    public static final int CASCADE_NONE = 17;
    public static final int CASCADE_LINK = 19;
    public static final int CASCADE_OBJECT = 23;

    private Class m_ClassOfItems = null;
    private Vector m_ForeignKeyFields = new Vector();
    private boolean m_CascadeRetrieve = true;
    private int m_CascadeStore = CASCADE_NONE;
    private int m_CascadeDelete = CASCADE_NONE;
    private int m_ProxyPrefetchingLimit = 50;

    private Class m_ProxyOfItems = null;
    private boolean m_LookedUpProxy = false;
    private boolean m_OtmDependent = false;

    /**
     * holds the foreign-key field descriptor array for a specified class
     */
    private Hashtable fkFieldMap = new Hashtable();
    /**
     * define loading strategy of the resulting object
     */
    private boolean lazy = false;
    /**
     * if true relationship is refreshed when owner is found in cache
     */
    private boolean refresh = false;

    /**
     *
     */
    public ObjectReferenceDescriptor(ClassDescriptor descriptor)
    {
        super(descriptor);
    }

    /**
     *
     */
    public Class getItemProxyClass() throws PersistenceBrokerException
    {
        if (!m_LookedUpProxy)
        {
            m_ProxyOfItems = getClassDescriptor().getRepository().
                                getDescriptorFor(m_ClassOfItems).getProxyClass();
            m_LookedUpProxy = true;
        }
        return m_ProxyOfItems;
    }

    /**
     *
     */
	public FieldDescriptor[] getForeignKeyFieldDescriptors(ClassDescriptor cld)
	{
		FieldDescriptor[] foreignKeyFieldDescriptors;
		if ((foreignKeyFieldDescriptors = (FieldDescriptor[]) fkFieldMap.get(cld)) == null)
		{
			// 1. collect vector of indices of Fk-Fields
			Vector v = getForeignKeyFields();
			// 2. get FieldDescriptor for each index from Class-descriptor
			// 2A. In a many-to-many relationship foreignkeyfields vector will be null.
			if (v != null)
			{
				Vector ret;
				if (cld.isInterface())
				{
					//exchange interface class descriptor with first concrete
					//class
					Vector extents = cld.getExtentClasses();
					Class firstConcreteClass = (Class) extents.get(0);
					cld = getClassDescriptor().getRepository().getDescriptorFor(firstConcreteClass);
				}
				ret = new Vector();

				Iterator iter = v.iterator();
				while (iter.hasNext())
				{
					Object fk = iter.next();
					FieldDescriptor fkfd = null;
					/*
                    OJB-55
                    it's possible that the FK field is declared in the super classes of this object,
                    so we can search for a valid field in super class-descriptor
                    */
                    ClassDescriptor tmp = cld;
                    while(tmp != null)
                    {
                        if (fk instanceof Integer)
                        {
                            Integer index = (Integer) fk;
                            fkfd = cld.getFieldDescriptorByIndex(index.intValue());
                        }
                        else
                        {
                            fkfd = tmp.getFieldDescriptorByName((String) fk);
                        }
                        if(fkfd != null)
                        {
                            break;
                        }
                        else
                        {
                            tmp = tmp.getSuperClassDescriptor();
                        }
                    }

                    if (fkfd == null)
					{
                        throw new OJBRuntimeException("Incorrect or not found field reference name '"
                                + fk + "' in descriptor " + this + " for class-descriptor '"
                                + (cld != null ? cld.getClassNameOfObject() + "'" : "'null'"));
					}
					ret.add(fkfd);
				}
				foreignKeyFieldDescriptors = (FieldDescriptor[]) ret.toArray(new FieldDescriptor[ret.size()]);
				fkFieldMap.put(cld, foreignKeyFieldDescriptors);
			}
		}
		return foreignKeyFieldDescriptors;
	}

    /**
     * Returns an Object array of all FK field values of the specified object.
     * If the specified object is an unmaterialized Proxy, it will be materialized
     * to read the FK values.
     *
     * @throws MetadataException if an error occours while accessing ForeingKey values on obj
     */
    public Object[] getForeignKeyValues(Object obj, ClassDescriptor mif)
            throws PersistenceBrokerException
    {
        FieldDescriptor[] fks = getForeignKeyFieldDescriptors(mif);
        // materialize object only if FK fields are declared
        if(fks.length > 0) obj = ProxyHelper.getRealObject(obj);
        Object[] result = new Object[fks.length];
        for (int i = 0; i < result.length; i++)
        {
            FieldDescriptor fmd = fks[i];
            PersistentField f = fmd.getPersistentField();

            // BRJ: do NOT convert.
            // conversion is done when binding the sql-statement
            //
            // FieldConversion fc = fmd.getFieldConversion();
            // Object val = fc.javaToSql(f.get(obj));

            result[i] = f.get(obj);
        }
        return result;
    }

    /**
     *
     */
    public Class getItemClass()
    {
        return m_ClassOfItems;
    }

    /**
     * @return the fully qualified name of the item class for this descriptor.
     */
    public String getItemClassName()
    {
        return this.m_ClassOfItems != null ? this.m_ClassOfItems.getName() : null;
    }

    /**
     * sets the item class
     * @param c the items class object
     */
    public void setItemClass(Class c)
    {
        m_ClassOfItems = c;
    }

    /**
     *
     */
    public Vector getForeignKeyFields()
    {
        return m_ForeignKeyFields;
    }

    /**
     *
     */
    public void setForeignKeyFields(Vector vec)
    {
        m_ForeignKeyFields = vec;
    }

    /**
     * add a foreign key field ID
     */
    public void addForeignKeyField(int newId)
    {
        if (m_ForeignKeyFields == null)
        {
            m_ForeignKeyFields = new Vector();
        }
        m_ForeignKeyFields.add(new Integer(newId));
    }

    /**
     * add a foreign key field
     */
    public void addForeignKeyField(String newField)
    {
        if (m_ForeignKeyFields == null)
        {
            m_ForeignKeyFields = new Vector();
        }
        m_ForeignKeyFields.add(newField);
    }

    /**
     * Gets the refresh.
     * @return Returns a boolean
     */
    public boolean isRefresh()
    {
        return refresh;
    }

    /**
     * Sets the refresh.
     * @param refresh The refresh to set
     */
    public void setRefresh(boolean refresh)
    {
        this.refresh = refresh;
    }

    /**
     * Gets the lazy.
     * @return Returns a boolean
     */
    public boolean isLazy()
    {
        return lazy;
    }

    /**
     * Sets the lazy.
     * @param lazy The lazy to set
     */
    public void setLazy(boolean lazy)
    {
        this.lazy = lazy;
    }

    /**
     *
     */
    public boolean getCascadeRetrieve()
    {
        return m_CascadeRetrieve;
    }

    /**
     *
     */
    public void setCascadeRetrieve(boolean b)
    {
        m_CascadeRetrieve = b;
    }

    /**
     *
     */
    public int getCascadingStore()
    {
        return m_CascadeStore;
    }

    /**
     *
     */
    public void setCascadingStore(int cascade)
    {
        m_CascadeStore = cascade;
    }

    public void setCascadingStore(String value)
    {
        setCascadingStore(getCascadeStoreValue(value));
    }

    /**
     * @deprecated use {@link #getCascadingStore} instead.
     */
    public boolean getCascadeStore()
    {
        return getCascadingStore() == CASCADE_OBJECT;
    }

    /**
     * @deprecated use {@link #setCascadingStore(int)} instead.
     */
    public void setCascadeStore(boolean cascade)
    {
        if(cascade)
        {
            setCascadingStore(getCascadeStoreValue("true"));
        }
        else
        {
            setCascadingStore(getCascadeStoreValue("false"));
        }
    }

    /**
     *
     */
    public int getCascadingDelete()
    {
        return m_CascadeDelete;
    }

    /**
     *
     */
    public void setCascadingDelete(int cascade)
    {
        m_CascadeDelete = cascade;
    }

    public void setCascadingDelete(String value)
    {
        setCascadingDelete(getCascadeDeleteValue(value));
    }

    /**
     * @deprecated use {@link #getCascadingDelete} instead.
     */
    public boolean getCascadeDelete()
    {
        return getCascadingDelete() == CASCADE_OBJECT;
    }

    /**
     * @deprecated use {@link #setCascadingDelete(int)}
     */
    public void setCascadeDelete(boolean cascade)
    {
        if(cascade)
        {
            setCascadingDelete(getCascadeDeleteValue("true"));
        }
        else
        {
            setCascadingDelete(getCascadeDeleteValue("false"));
        }
    }

    protected int getCascadeStoreValue(String cascade)
    {
        if(cascade.equalsIgnoreCase(RepositoryTags.CASCADE_NONE_STR))
        {
            return CASCADE_NONE;
        }
        else if(cascade.equalsIgnoreCase(RepositoryTags.CASCADE_LINK_STR))
        {
            return CASCADE_LINK;
        }
        else if(cascade.equalsIgnoreCase(RepositoryTags.CASCADE_OBJECT_STR))
        {
            return CASCADE_OBJECT;
        }
        else if(cascade.equalsIgnoreCase("true"))
        {
            return CASCADE_OBJECT;
        }
        else if(cascade.equalsIgnoreCase("false"))
        {
            /*
            in old implementation the FK values of an 1:1 relation are always
            set. Thus we choose 'link' instead of 'none'
            The CollectionDescriptor should override this behaviour.
            */
            return CASCADE_LINK;
        }
        else
        {
            throw new OJBRuntimeException("Invalid value! Given value was '" + cascade
                    + "', expected values are: " + RepositoryTags.CASCADE_NONE_STR + ", "
                    + RepositoryTags.CASCADE_LINK_STR + ", " + RepositoryTags.CASCADE_OBJECT_STR
                    + " ('false' and 'true' are deprecated but still valid)");
        }
    }

    protected int getCascadeDeleteValue(String cascade)
    {
        if(cascade.equalsIgnoreCase(RepositoryTags.CASCADE_NONE_STR))
        {
            return CASCADE_NONE;
        }
        else if(cascade.equalsIgnoreCase(RepositoryTags.CASCADE_LINK_STR))
        {
            return CASCADE_LINK;
        }
        else if(cascade.equalsIgnoreCase(RepositoryTags.CASCADE_OBJECT_STR))
        {
            return CASCADE_OBJECT;
        }
        else if(cascade.equalsIgnoreCase("true"))
        {
            return CASCADE_OBJECT;
        }
        else if(cascade.equalsIgnoreCase("false"))
        {
            return CASCADE_NONE;
        }
        else
        {
            throw new OJBRuntimeException("Invalid value! Given value was '" + cascade
                    + "', expected values are: " + RepositoryTags.CASCADE_NONE_STR + ", "
                    + RepositoryTags.CASCADE_LINK_STR + ", " + RepositoryTags.CASCADE_OBJECT_STR
                    + " ('false' and 'true' are deprecated but still valid)");
        }
    }

    public String getCascadeAsString(int cascade)
    {
        String result = null;
        switch(cascade)
        {
            case CASCADE_NONE:
                result = RepositoryTags.CASCADE_NONE_STR;
                break;
            case CASCADE_LINK:
                result = RepositoryTags.CASCADE_LINK_STR;
                break;
            case CASCADE_OBJECT:
                result = RepositoryTags.CASCADE_OBJECT_STR;
                break;
        }
        return result;
    }

    public int getProxyPrefetchingLimit()
    {
        return m_ProxyPrefetchingLimit;
    }

    public void setProxyPrefetchingLimit(int proxyPrefetchingLimit)
    {
        m_ProxyPrefetchingLimit = proxyPrefetchingLimit;
    }

    /**
     *
     */
    public boolean getOtmDependent()
    {
        return m_OtmDependent;
    }

    /**
     *
     */
    public void setOtmDependent(boolean b)
    {
        m_OtmDependent = b;
    }

    /**
     * Returns <code>true</code> if this descriptor was used to
     * describe a reference to a super class of an object.
     *
     * @return always <code>false</code> for this instance.
     */
    public boolean isSuperReferenceDescriptor()
    {
        return false;
    }

    /**
     * Returns <em>true</em> if a foreign key constraint to the referenced object is
     * declared, else <em>false</em> is returned.
     */
    public boolean hasConstraint()
    {
        /*
        arminw: Currently we don't have a ForeignKey descriptor object and
        a official xml-element to support FK settings. As a workaround I introduce
        a custom-attribute to handle FK settings in collection-/reference-decriptor
        */
        String result = getAttribute("constraint");
        return result != null && result.equalsIgnoreCase("true");
    }

    /**
     * Set a foreign key constraint flag for this reference - see {@link #hasConstraint()}
     * @param constraint If set <em>true</em>, signals a foreign key constraint in database. 
     */
    public void setConstraint(boolean constraint)
    {
        addAttribute("constraint", constraint ? "true" : "false");
    }

    public String toString()
    {
        return new ToStringBuilder(this)
                .append("cascade_retrieve", getCascadeRetrieve())
                .append("cascade_store", getCascadeAsString(m_CascadeStore))
                .append("cascade_delete", getCascadeAsString(m_CascadeDelete))
                .append("is_lazy", lazy)
                .append("class_of_Items", m_ClassOfItems)
                .toString();
    }

    /*
     * @see XmlCapable#toXML()
     */
    public String toXML()
    {
        RepositoryTags tags = RepositoryTags.getInstance();
        String eol = System.getProperty( "line.separator" );

        // opening tag
        StringBuffer result = new StringBuffer( 1024 );
        result.append( "      " );
        result.append( tags.getOpeningTagNonClosingById( REFERENCE_DESCRIPTOR ) );
        result.append( eol );

        // attributes
        // name
        String name = this.getAttributeName();
        if( name == null )
        {
            name = RepositoryElements.TAG_SUPER;
        }
        result.append( "        " );
        result.append( tags.getAttribute( FIELD_NAME, name ) );
        result.append( eol );

        // class-ref
        result.append( "        " );
        result.append( tags.getAttribute( REFERENCED_CLASS, this.getItemClassName() ) );
        result.append( eol );

        // proxyReference is optional
        if( isLazy() )
        {
            result.append( "        " );
            result.append( tags.getAttribute( PROXY_REFERENCE, "true" ) );
            result.append( eol );
            result.append( "        " );
            result.append( tags.getAttribute( PROXY_PREFETCHING_LIMIT, "" + this.getProxyPrefetchingLimit() ) );
            result.append( eol );
        }

        //reference refresh is optional, disabled by default
        if( isRefresh() )
        {
            result.append( "        " );
            result.append( tags.getAttribute( REFRESH, "true" ) );
            result.append( eol );
        }

        //auto retrieve
        result.append( "        " );
        result.append( tags.getAttribute( AUTO_RETRIEVE, "" + getCascadeRetrieve() ) );
        result.append( eol );

        //auto update
        result.append( "        " );
        result.append( tags.getAttribute( AUTO_UPDATE, getCascadeAsString( getCascadingStore() ) ) );
        result.append( eol );

        //auto delete
        result.append( "        " );
        result.append( tags.getAttribute( AUTO_DELETE, getCascadeAsString( getCascadingDelete() ) ) );
        result.append( eol );

        //otm-dependent is optional, disabled by default
        if( getOtmDependent() )
        {
            result.append( "        " );
            result.append( tags.getAttribute( OTM_DEPENDENT, "true" ) );
            result.append( eol );
        }

        // close opening tag
        result.append( "      >" );
        result.append( eol );

        // elements
        // write foreignkey elements
        for( int i = 0; i < getForeignKeyFields().size(); i++ )
        {
            Object obj = getForeignKeyFields().get( i );
            if( obj instanceof Integer )
            {
                String fkId = obj.toString();
                result.append( "        " );
                result.append( tags.getOpeningTagNonClosingById( FOREIGN_KEY ) );
                result.append( " " );
                result.append( tags.getAttribute( FIELD_ID_REF, fkId ) );
                result.append( "/>" );
                result.append( eol );
            }
            else
            {
                String fk = ( String ) obj;
                result.append( "        " );
                result.append( tags.getOpeningTagNonClosingById( FOREIGN_KEY ) );
                result.append( " " );
                result.append( tags.getAttribute( FIELD_REF, fk ) );
                result.append( "/>" );
                result.append( eol );
            }
        }

        // closing tag
        result.append( "      " );
        result.append( tags.getClosingTagById( REFERENCE_DESCRIPTOR ) );
        result.append( eol );
        return result.toString();
    }
}
