package org.apache.ojb.broker.core.proxy;

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

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.util.collections.RemovalAwareCollection;

/**
 * A placeHolder for a whole list to support deferred loading of
 * relationships. The complete relationship is loaded on request.
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: ListProxyDefaultImpl.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public class ListProxyDefaultImpl extends CollectionProxyDefaultImpl implements List
{

    /**
     * Constructor for ListProxy.
     * @param aKey
     * @param aQuery
     */
    public ListProxyDefaultImpl(PBKey aKey, Query aQuery)
    {
        this(aKey, RemovalAwareCollection.class, aQuery);
    }

    /**
     * Constructor for ListProxy.
     * @param aKey
     * @param aCollClass
     * @param aQuery
     */
    public ListProxyDefaultImpl(PBKey aKey, Class aCollClass, Query aQuery)
    {
        super(aKey, aCollClass, aQuery);
    }

    /**
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int index, Collection c)
    {
        return getListData().addAll(index, c);
    }

    /**
     * @see java.util.List#get(int)
     */
    public Object get(int index)
    {
        return getListData().get(index);
    }

    /**
     * @see java.util.List#set(int, java.lang.Object)
     */
    public Object set(int index, Object element)
    {
        return getListData().set(index, element);
    }

    /**
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int index, Object element)
    {
        getListData().add(index, element);
    }

    /**
     * @see java.util.List#remove(int)
     */
    public Object remove(int index)
    {
        return getListData().remove(index);
    }

    /**
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object o)
    {
        return getListData().indexOf(o);
    }

    /**
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object o)
    {
        return getListData().lastIndexOf(o);
    }

    /**
     * @see java.util.List#listIterator()
     */
    public ListIterator listIterator()
    {
        return getListData().listIterator();
    }

    /**
     * @see java.util.List#listIterator(int)
     */
    public ListIterator listIterator(int index)
    {
        return getListData().listIterator(index);
    }

    /**
     * @see java.util.List#subList(int, int)
     */
    public List subList(int fromIndex, int toIndex)
    {
        return getListData().subList(fromIndex, toIndex);
    }

    protected List getListData()
    {
        return (List) super.getData();
    }

    /**
     * @see org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl#loadData()
     */
    protected Collection loadData() throws PersistenceBrokerException
    {
        Collection result = super.loadData();

        if (result instanceof List)
        {
            return result;
        }
        else
        {
            throw new PersistenceBrokerException("loaded data does not implement java.util.List");
        }

    }

}
