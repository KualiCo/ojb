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
import java.util.Set;

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.util.collections.ManageableHashSet;

/**
 * A placeHolder for a whole set to support deferred loading of
 * relationships. The complete relationship is loaded on request.
 * 
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: SetProxyDefaultImpl.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public class SetProxyDefaultImpl extends CollectionProxyDefaultImpl implements Set
{

	/**
	 * Constructor for SetProxy.
	 * @param aKey
	 * @param aQuery
	 */
	public SetProxyDefaultImpl(PBKey aKey, Query aQuery)
	{
		this(aKey, ManageableHashSet.class, aQuery);
	}

	/**
	 * Constructor for SetProxy.
	 * @param aKey
	 * @param aCollClass
	 * @param aQuery
	 */
	public SetProxyDefaultImpl(PBKey aKey, Class aCollClass, Query aQuery)
	{
		super(aKey, aCollClass, aQuery);
	}

    protected Set getSetData()
    {
        return (Set)super.getData();    
    }
    
	/**
	 * @see org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl#loadData()
	 */
	protected Collection loadData() throws PersistenceBrokerException
	{
        Collection result = super.loadData();

        if (result instanceof Set)
        {
            return result;
        }    
        else
        {
            throw new PersistenceBrokerException("loaded data does not implement java.util.Set");
        }
		
	}

}
