package org.apache.ojb.broker.accesslayer;

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

import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;

/**
 * Factory for Relationship Prefetchers
 * 
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: RelationshipPrefetcherFactory.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class RelationshipPrefetcherFactory
{
    private PersistenceBrokerImpl broker;

    public RelationshipPrefetcherFactory(final PersistenceBrokerImpl broker)
    {
        this.broker = broker;
    }

    /**
     * create either a CollectionPrefetcher or a ReferencePrefetcher
     */ 
    public RelationshipPrefetcher createRelationshipPrefetcher(ObjectReferenceDescriptor ord)
    {
        if (ord instanceof CollectionDescriptor)
        {
            CollectionDescriptor cds = (CollectionDescriptor)ord;
            if (cds.isMtoNRelation())
            {
                return new MtoNCollectionPrefetcher(broker, cds);
            }
            else
            {
                return new CollectionPrefetcher(broker, cds);
            }
        }
        else
        {    
            return new ReferencePrefetcher(broker, ord);
        }
    }   
    
    /**
     * create either a CollectionPrefetcher or a ReferencePrefetcher
     */ 
    public RelationshipPrefetcher createRelationshipPrefetcher(ClassDescriptor anOwnerCld, String aRelationshipName)
    {
        ObjectReferenceDescriptor ord;
        
        ord = anOwnerCld.getCollectionDescriptorByName(aRelationshipName);
        if (ord == null)
        {
            ord = anOwnerCld.getObjectReferenceDescriptorByName(aRelationshipName);
            if (ord == null)
            {
                throw new PersistenceBrokerException("Relationship named '" + aRelationshipName
                        + "' not found in owner class " + (anOwnerCld != null ? anOwnerCld.getClassNameOfObject() : null));
            }
        }
        return createRelationshipPrefetcher(ord);
    }   
}
