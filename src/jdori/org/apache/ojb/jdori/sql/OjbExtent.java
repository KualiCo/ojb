package org.apache.ojb.jdori.sql;
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

import javax.jdo.PersistenceManager;
import javax.jdo.spi.PersistenceCapable;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;

import com.sun.jdori.FieldManager;
import com.sun.jdori.PersistenceManagerInternal;
import com.sun.jdori.StateManagerInternal;
import com.sun.jdori.model.jdo.JDOClass;

/**
 * @see javax.jdo.Extent
 * @author Thomas Mahler
 */
public class OjbExtent implements javax.jdo.Extent
{
    private Collection extentCollection;
    private Class clazz;
    private PersistenceBroker broker;
    private PersistenceManagerInternal pmi;

    /**
     * Constructor for OjbExtent.
     */
    public OjbExtent(Class pClazz, PersistenceBroker pBroker, PersistenceManagerInternal pPmi)
    {
    	clazz = pClazz;
    	broker = pBroker;
    	pmi = pPmi;
        Criteria selectExtent = null;
        Query q = QueryFactory.newQuery(clazz, selectExtent);
        
        // the PB loads plain java objects
        Collection pojoInstances = broker.getCollectionByQuery(q);
        // To bring these instances under JDO management, 
        // each instance must be provided with its own StateManager
        extentCollection = provideStateManagers(pojoInstances);
    }

    /**
     * @see javax.jdo.Extent#iterator()
     */
    public Iterator iterator()
    {
        return extentCollection.iterator();
    }

    /**
     * @see javax.jdo.Extent#hasSubclasses()
     */
    public boolean hasSubclasses()
    {
        ClassDescriptor cld = broker.getClassDescriptor(clazz);
        return cld.isExtent();
    }

    /**
     * @see javax.jdo.Extent#getCandidateClass()
     */
    public Class getCandidateClass()
    {
        return clazz;
    }

    /**
     * @see javax.jdo.Extent#getPersistenceManager()
     */
    public PersistenceManager getPersistenceManager()
    {
    	return pmi.getCurrentWrapper();
    }

    /**
     * @see javax.jdo.Extent#closeAll()
     */
    public void closeAll()
    {
        // noop
    }

    /**
     * @see javax.jdo.Extent#close(Iterator)
     */
    public void close(Iterator pIterator)
    {
        // noop
    }
    
    /**
     * This methods enhances the objects loaded by a broker query
     * with a JDO StateManager an brings them under JDO control.
     * @param pojos the OJB pojos as obtained by the broker
     * @return the collection of JDO PersistenceCapable instances
     */
    protected Collection provideStateManagers(Collection pojos)
    {
    	PersistenceCapable pc;
    	int [] fieldNums;
    	Iterator iter = pojos.iterator();
    	Collection result = new ArrayList();
    	
    	while (iter.hasNext())
    	{
    		// obtain a StateManager
    		pc = (PersistenceCapable) iter.next();
    		Identity oid = new Identity(pc, broker);
    		StateManagerInternal smi = pmi.getStateManager(oid, pc.getClass()); 
    		
    		// fetch attributes into StateManager
			JDOClass jdoClass = Helper.getJDOClass(pc.getClass());
			fieldNums = jdoClass.getManagedFieldNumbers();

			FieldManager fm = new OjbFieldManager(pc, broker);
			smi.replaceFields(fieldNums, fm);
			smi.retrieve();
			
			// get JDO PersistencecCapable instance from SM and add it to result collection
			Object instance = smi.getObject();
			result.add(instance);
    	}
    	return result;   
	}

}
