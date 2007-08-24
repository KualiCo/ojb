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

import java.util.Collection;

import org.apache.ojb.broker.metadata.ClassDescriptor;
 
/**
 * Interface for Relationship Prefetchers.
 * Each Prefetcher handles a single Relationship (1:1 or 1:n)
 * 
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: RelationshipPrefetcher.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public interface RelationshipPrefetcher
{
    /**
     * Returns the ClassDescriptor of the item Class
     * @return ClassDescriptor
     */
    public ClassDescriptor getItemClassDescriptor();

    /**
	 * Prepare the Relationship for prefetch (ie: disable auto-retrieve)
	 */ 
	public void prepareRelationshipSettings();

	/**
	 * Prefetch the Relationship for the owners (the 1 side of a 1:n)
	 */ 
	public void prefetchRelationship(Collection owners);

	/**
	 * Restore the Relationship settings(ie: enable auto-retrieve)
	 */ 
	public void restoreRelationshipSettings();

    /**
     * The limit of objects loaded by one SQL query
     */
    public int getLimit();
}
