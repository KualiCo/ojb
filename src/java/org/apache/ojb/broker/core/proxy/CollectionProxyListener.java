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

/**
 * This is a callback interface that allows interaction with the
 * loading process of the CollectionProxy
 * The OTM layer implements this interface to
 * provide a delayed registration mechanism for ColelctionProxies.
 * @author Oleg Nitz
 * @version $Id: CollectionProxyListener.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public interface CollectionProxyListener
{
    /**
     *  this callback is invoked before a CollectionProxy is loaded
     *  @param colProxy the CollectionProxy
     */
    public void beforeLoading(CollectionProxyDefaultImpl colProxy);

    /**
     *  this callback is invoked after a CollectionProxy is loaded
     *  @param colProxy the CollectionProxy
     */
    public void afterLoading(CollectionProxyDefaultImpl colProxy);

}
