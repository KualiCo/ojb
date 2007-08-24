package org.apache.ojb.broker.core.proxy;

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

import org.apache.ojb.broker.Identity;


/**
 * This is a callback interface that allows interaction with the
 * Materialization process of the IndirectionHandler
 * The ODMG TransactionImpl implements this interface to
 * provide a delayed registration mechanism for proxies.
 * @author Thomas Mahler
 * @version $Id: MaterializationListener.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public interface MaterializationListener
{
    /**
     *  this callback is invoked before an Object is materialized
     *  within an IndirectionHandler.
     *  @param handler the invoking handler
     *  @param oid the identity of the object to be materialized
     */
    public abstract void beforeMaterialization(IndirectionHandler handler, Identity oid);

    /**
     *  this callback is invoked after an Object is materialized
     *  within an IndirectionHandler.
     *  @param handler the invoking handler
     *  @param materializedObject the materialized Object
     */
    public abstract void afterMaterialization(IndirectionHandler handler, Object materializedObject);
}
