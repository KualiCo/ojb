package org.apache.ojb.odmg;

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


/**
 * This interface defines the configurable setting of the ODMG
 * layer.
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: OdmgConfiguration.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */

public interface OdmgConfiguration
{
	/**
	 * If true acquiring a write-lock on a given object x implies write 
	 * locks on all objects associated to x. 
	 * If false implicit read-locks are acquired.
	 */
    public boolean lockAssociationAsWrites();
    
    /**
     * This class is used to hold results of OQL queries.
     * By default a DListImpl is used.
     */
    public Class getOqlCollectionClass();
    
    /**
     * defines if implicit lock acquisition is to be used.
     * If set to true OJB implicitely locks objects to ODMG
     * transactions after performing OQL queries.
     * If implicit locking is used locking objects is recursive, that is
     * associated objects are also locked.
     * If ImplicitLocking is set to false, no locks are obtained in OQL
     * queries and there is also no recursive locking.
     */
    public boolean useImplicitLocking();

}
