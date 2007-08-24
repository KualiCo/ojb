package org.apache.ojb.broker.core;

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
 * Interface for the service implementation of PBF used in jboss
 * or other application server.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PBFactoryIF.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public interface PBFactoryIF
{
    public static final String PBFACTORY_JNDI_NAME = "java:/ojb/PBAPI";

    public PersistenceBrokerFactoryIF getInstance();
}
