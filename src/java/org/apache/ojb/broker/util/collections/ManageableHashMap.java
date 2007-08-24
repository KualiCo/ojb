package org.apache.ojb.broker.util.collections;

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
import java.util.Iterator;

import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.MetadataException;
import org.apache.ojb.broker.metadata.MetadataManager;


/**
 * Creates a Map where the primary key is the map key, and the object
 * is the map value.
 * <br/>
 * <strong>Note:</strong> This implementation is limited in use, only objects with
 * single primary key field are allowed (composed PK's are illegal).
 */
public class ManageableHashMap extends HashMap implements ManageableCollection
{
	public void ojbAdd(Object anObject)
	{
		if (anObject != null)
		{
			ClassDescriptor cd = MetadataManager.getInstance().getRepository().getDescriptorFor(anObject.getClass());
            FieldDescriptor[] fields = cd.getPkFields();
            if(fields.length > 1 || fields.length == 0)
            {
                throw new MetadataException("ManageableHashMap can only be used for persistence capable objects with" +
                        " exactly one primiary key field defined in metadata, for " + anObject.getClass() + " the" +
                        " PK field count is " + fields.length);
            }
            else
            {
                Object key = fields[0].getPersistentField().get(anObject);
			    put(key,anObject);
            }
		}
	}

	public void ojbAddAll(ManageableCollection otherCollection)
	{
		Iterator it = otherCollection.ojbIterator();
		while (it.hasNext())
		{
			ojbAdd(it.next());
		}
	}

	public Iterator ojbIterator()
	{
		return values().iterator();
	}

	public void afterStore(PersistenceBroker broker) throws PersistenceBrokerException
	{
        //do nothing
	}
}
