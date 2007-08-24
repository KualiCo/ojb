package org.apache.ojb.broker.util;

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

import java.io.Serializable;

/**
 *
 * The Interface ObjectModification represents information about
 * modifications of persistence capable objects.
 * Allows clients of the PersistenceBroker (e.g. a TransactionServer)
 * to interact with the Broker in order to generate optimized SQL Statements.
 *
 * @author Thomas Mahler
 * @version $Id: ObjectModification.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public interface ObjectModification extends Serializable
{
	static final long serialVersionUID = -3208237880606252967L;

    /**
     * Default implementation of this interface usable for INSERT.
     */
    public static final ObjectModification INSERT = new ObjectModification()
    {
        public boolean needsInsert()
        {
            return true;
        }

        public boolean needsUpdate()
        {
            return false;
        }
    };

    /**
     * Default implementation of this interface usable for UPDATE.
     */
    public static final ObjectModification UPDATE = new ObjectModification()
    {
        public boolean needsInsert()
        {
            return false;
        }

        public boolean needsUpdate()
        {
            return true;
        }
    };

    /**
     * Returns true if the underlying Object needs an INSERT statement.
     * else Returns false.
     */
    public boolean needsInsert();

    /**
     * Returns true if the underlying Object needs an UPDATE statement.
     * else Returns false.
     */
    public boolean needsUpdate();
}
