package org.apache.ojb.broker.prevayler;

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


import java.io.Serializable;
import java.util.Hashtable;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.prevayler.implementation.AbstractPrevalentSystem;

/**
 * This class represents the persistent store OJB works against.
 * It is implement as an PrevalentSystem.
 * All Commands executed this DB are tracked by Prevayler and written to disk
 * as command-logs.
 * If the system is halted, crashes or rebooted for what reason so ever, Prevayler
 * will establish the state of the Database from the command-logs written to disk.
 * @author Thomas Mahler
 *
 */
public class Database extends AbstractPrevalentSystem
{
	
	private final Hashtable table = new Hashtable();
	
	private transient PersistenceBroker broker;
	
	
	public void store(Object obj)
	{
		Identity oid = new Identity(obj,broker);
		this.getTable().put(oid.toString(), obj);
	}
	
	public void remove(Object obj)
	{
		Identity oid = new Identity(obj,broker);
		this.getTable().remove(oid.toString());
	}
	
	public Serializable lookupObjectByIdentity(Identity oid)
	{
		return (Serializable) this.getTable().get(oid.toString());	
	}

    /**
     * Returns the table.
     * @return Hashtable
     */
    public Hashtable getTable()
    {
        return table;
    }

    /**
     * Returns the broker.
     * @return PersistenceBroker
     */
    public PersistenceBroker getBroker()
    {
        return broker;
    }

    /**
     * Sets the broker.
     * @param broker The broker to set
     */
    public void setBroker(PersistenceBroker broker)
    {
        this.broker = broker;
    }

}
