package org.apache.ojb.broker.accesslayer;

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

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.metadata.AttributeContainer;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;

/**
 * Interface for QueryCustomizer in CollectionDescriptor
 * <pre>
 * ...
 *   <query-customizer
 *     class="org.apache.ojb.broker.accesslayer.QueryCustomizerDefaultImpl">
 *   <attribute
 *     attribute-name="attr1"
 *     attribute-value="value1"
 *   />
 * ...
 * </pre>
 *
 * If the customized Query is null execution of PB retrieveCollection is skipped
 * and an empty Collection is placed in the relationship attribute.
 * 
 * @see org.apache.ojb.broker.PersistenceBroker#getCollectionByQuery
 *
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: QueryCustomizer.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public interface QueryCustomizer extends AttributeContainer
{
    /**
     * Return a new Query based on the original Query, the originator object and
     * the additional Attributes.
     *
     * @param anObject the originator object
     * @param aBroker the PersistenceBroker
     * @param aCod the CollectionDescriptor
     * @param aQuery the original 1:n-Query
     * @return Query the customized 1:n-Query, return null to skip execution of the query
     */
    public Query customizeQuery(Object anObject, PersistenceBroker aBroker, CollectionDescriptor aCod, QueryByCriteria aQuery);
}
