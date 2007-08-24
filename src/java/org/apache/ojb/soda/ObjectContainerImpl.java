package org.apache.ojb.soda;

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

import org.odbms.ObjectContainer;
import org.odbms.Query;
import org.apache.ojb.broker.PersistenceBroker;
/**
 * @author Thomas Mahler
 * @version $Id: ObjectContainerImpl.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class ObjectContainerImpl implements ObjectContainer
{
    private PersistenceBroker broker;

    private ObjectContainerImpl(PersistenceBroker broker)
    {
        this.broker = broker;
    }

    public static ObjectContainer getInstance(PersistenceBroker broker)
	{
	    return new ObjectContainerImpl(broker);
	}
    /*
     * @see ObjectContainer#query()
     */
    public Query query()
    {
        return new QueryImpl(broker);
    }
}
