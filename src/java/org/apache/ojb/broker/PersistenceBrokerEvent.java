package org.apache.ojb.broker;

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

import org.apache.ojb.broker.util.event.OjbEvent;

/**
 * Base class for persistence broker events.
 */
public class PersistenceBrokerEvent extends OjbEvent
{
    /**
     * Creates a new event instance.
     * 
     * @param broker The broker
     */
    public PersistenceBrokerEvent(PersistenceBroker broker)
    {
        super(broker);
    }

    /**
     * Convenience method for {@link #getSource}.
     * 
     * @return The broker where the event originated
     */
    public PersistenceBroker getTriggeringBroker()
    {
        return (PersistenceBroker)getSource();
    }
}
