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

import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;

/**
 * Abstract Relationship Prefetchers.
 * Each Prefetcher handles a single Relationship (1:1 or 1:n)
 *
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: RelationshipPrefetcherImpl.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public abstract class RelationshipPrefetcherImpl extends BasePrefetcher
{
    private ObjectReferenceDescriptor objectReferenceDescriptor;
    private boolean cascadeRetrieve;

    /**
     * Constructor for RelationshipPrefetcherImpl.
     */
    public RelationshipPrefetcherImpl(PersistenceBrokerImpl aBroker, ObjectReferenceDescriptor anOrd)
    {
        super(aBroker, anOrd.getItemClass());
        objectReferenceDescriptor = anOrd;
    }

    /**
     * @see org.apache.ojb.broker.accesslayer.RelationshipPrefetcher#prepareRelationshipSettings()
     */
    public void prepareRelationshipSettings()
    {
        setCascadeRetrieve(getObjectReferenceDescriptor().getCascadeRetrieve());
        
        // BRJ: do not modify reference-descriptor
        // getObjectReferenceDescriptor().setCascadeRetrieve(false);
    }

    /**
     * Returns the ClassDescriptor of the owner Class
     * @return ClassDescriptor
     */
    protected ClassDescriptor getOwnerClassDescriptor()
    {
        return getObjectReferenceDescriptor().getClassDescriptor();
    }

    /**
     * @see org.apache.ojb.broker.accesslayer.RelationshipPrefetcher#restoreRelationshipSettings()
     */
    public void restoreRelationshipSettings()
    {
        // BRJ: do not modify reference-descriptor
        // getObjectReferenceDescriptor().setCascadeRetrieve(isCascadeRetrieve());
    }

    /**
     * Returns the objectReferenceDescriptor.
     * @return ObjectReferenceDescriptor
     */
    protected ObjectReferenceDescriptor getObjectReferenceDescriptor()
    {
        return objectReferenceDescriptor;
    }

    /**
     * Sets the objectReferenceDescriptor.
     * @param objectReferenceDescriptor The objectReferenceDescriptor to set
     */
    protected void setObjectReferenceDescriptor(ObjectReferenceDescriptor objectReferenceDescriptor)
    {
        this.objectReferenceDescriptor = objectReferenceDescriptor;
    }

    /**
     * Returns the cascadeRetrieve.
     * @return boolean
     */
    protected boolean isCascadeRetrieve()
    {
        return cascadeRetrieve;
    }

    /**
     * Sets the cascadeRetrieve.
     * @param cascadeRetrieve The cascadeRetrieve to set
     */
    protected void setCascadeRetrieve(boolean cascadeRetrieve)
    {
        this.cascadeRetrieve = cascadeRetrieve;
    }
}
