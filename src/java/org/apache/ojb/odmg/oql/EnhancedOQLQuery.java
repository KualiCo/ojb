package org.apache.ojb.odmg.oql;

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

import org.odmg.OQLQuery;
import org.odmg.QueryInvalidException;

/**
 * Offers useful none odmg-standard methods of the odmg {@link org.odmg.OQLQuery} interface.
 * <p>
 * Note: All listed methods are <strong>not</strong> part of the standard ODMG-api -
 * they are special (proprietary) OJB extensions.
 * </p>
 *
 * @version $Id: EnhancedOQLQuery.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public interface EnhancedOQLQuery extends OQLQuery
{
	/**
     * An extension of the {@link org.odmg.OQLQuery#create(String)} method, which
     * additionally allow to specify an <em>start-</em> and <em>end-Index</em> for
     * the query.
     *
     * @param queryString An oql query string.
     * @param startAtIndex The start index.
     * @param endAtIndex The end index.
     * @throws QueryInvalidException The type of the parameter does
     * not correspond with the type of the parameter in the query.
     */
    void create(String queryString, int startAtIndex, int endAtIndex) throws QueryInvalidException;
	
    /**
     * Deprecated method.
     * @deprecated
     */
    int fullSize();
}
