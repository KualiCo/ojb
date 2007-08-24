package org.apache.ojb.broker.core.proxy;

/* Copyright 2005 The Apache Software Foundation
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
import org.apache.ojb.broker.PBKey;


public class IndirectionHandlerCGLIBImpl extends AbstractIndirectionHandler implements IndirectionHandlerCGLIB
{
    /**
     * Creates a new indirection handler for the indicated object.
     * 
     * @param persistenceConf The persistence configuration
     * @param id              The identity of the subject
     */
    public IndirectionHandlerCGLIBImpl(PBKey brokerKey, Identity id)
    {
       super(brokerKey,id);
    }
}
