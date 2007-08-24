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

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * The <code>PBStateEvent</code> encapsulates information about
 * the life-cycle/transaction demarcation of the used {@link org.apache.ojb.broker.PersistenceBroker}
 * instance.
 *
 * @author Armin Waibel
 * @version $Id: PBStateEvent.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public final class PBStateEvent extends PersistenceBrokerEvent
{
    /** Denotes an event that happens before the broker will be closed. */
    public static final int KEY_BEFORE_CLOSE = 1;
    /** Denotes an event that happens before a transaction will be started. */
    public static final int KEY_BEFORE_BEGIN = 2;
    /** Denotes an event that happens before a transaction will be comitted. */
    public static final int KEY_BEFORE_COMMIT = 3;
    /** Denotes an event that happens before a transaction will be rolled back. */
    public static final int KEY_BEFORE_ROLLBACK = 4;
    /** Denotes an event that happens after a transaction was started. */
    public static final int KEY_AFTER_BEGIN = 5;
    /** Denotes an event that happens after a transaction was comitted. */
    public static final int KEY_AFTER_COMMIT = 6;
    /** Denotes an event that happens after a broker was opened. */
    public static final int KEY_AFTER_OPEN = 7;
    /** Denotes an event that happens after a transaction was rolled back. */
    public static final int KEY_AFTER_ROLLBACK = 8;

    private Type eventType;

    /**
     * Creates a new event instance.
     * 
     * @param broker    The broker
     * @param eventType The type of the event
     */
    public PBStateEvent(PersistenceBroker broker, Type eventType)
    {
        super(broker);
        this.eventType = eventType;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this);
        buf.append("type", eventType.toString()).
                append("source object", getSource());
        return buf.toString();
    }

    /**
     * Returns the event type.
     * 
     * @return The event type
     */
    public Type getEventType()
    {
        return eventType;
    }

    /**
     * Enum-like class for the event types.
     */
    public static class Type
    {
        /** Denotes an event that happens before a transaction will be started. */
        public static final Type BEFORE_BEGIN = new Type(KEY_BEFORE_BEGIN);
        /** Denotes an event that happens after a transaction was started. */
        public static final Type AFTER_BEGIN = new Type(KEY_AFTER_BEGIN);
        /** Denotes an event that happens before a transaction will be comitted. */
        public static final Type BEFORE_COMMIT = new Type(KEY_BEFORE_COMMIT);
        /** Denotes an event that happens after a transaction was comitted. */
        public static final Type AFTER_COMMIT = new Type(KEY_AFTER_COMMIT);
        /** Denotes an event that happens before a transaction will be rolled back. */
        public static final Type BEFORE_ROLLBACK = new Type(KEY_BEFORE_ROLLBACK);
        /** Denotes an event that happens after a transaction was rolled back. */
        public static final Type AFTER_ROLLBACK = new Type(KEY_AFTER_ROLLBACK);
        /** Denotes an event that happens after a broker was opened. */
        public static final Type AFTER_OPEN = new Type(KEY_AFTER_OPEN);
        /** Denotes an event that happens before the broker will be closed. */
        public static final Type BEFORE_CLOSE = new Type(KEY_BEFORE_CLOSE);

        private int type;

        /**
         * Creates a new instance.
         * 
         * @param type The type value
         */
        protected Type(int type)
        {
            this.type = type;
        }

        /**
         * {@inheritDoc}
         */
        public final boolean equals(Object obj)
        {
            if (obj == this)
            {
                return true;
            }
            if (!(obj instanceof PBStateEvent))
            {
                return false;
            }

            return type == ((Type) obj).type;
        }

        /**
         * {@inheritDoc}
         */
        public final int hashCode()
        {
            return type;
        }
        
        /**
         * Returns the type id.
         * 
         * @return The type id
         */
        public final int typeId()
        {
            return type;
        }

        /**
         * {@inheritDoc}
         */
        public String toString()
        {
            return this.getClass().getName() + " [type= " + typeAsName(type) + "]";
        }

        private String typeAsName(int aType)
        {
            if (aType == KEY_AFTER_BEGIN)
            {
                return "AFTER_BEGIN";
            }
            else if (aType == KEY_AFTER_COMMIT)
            {
                return "AFTER_COMMIT";
            }
            else if (aType == KEY_AFTER_OPEN)
            {
                return "AFTER_OPEN";
            }
            else if (aType == KEY_AFTER_ROLLBACK)
            {
                return "AFTER_ROLLBACK";
            }
            else if (aType == KEY_BEFORE_BEGIN)
            {
                return "BEFORE_BEGIN";
            }
            else if (aType == KEY_BEFORE_CLOSE)
            {
                return "BEFORE_CLOSE";
            }
            else if (aType == KEY_BEFORE_COMMIT)
            {
                return "BEFORE_COMMIT";
            }
            else if (aType == KEY_BEFORE_ROLLBACK)
            {
                return "BEFORE_ROLLBACK";
            }
            else
            {
                throw new OJBRuntimeException("Could not find type " + aType);
            }
        }
    }
}
