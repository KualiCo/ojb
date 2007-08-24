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
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * The <code>PBLifeCycleEvent</code> encapsulates information about
 * the life-cycle of a persistent object.
 * <br/>
 * NOTE:
 * <br/>
 * Because of performance reasons OJB intern reuse instances of this class
 * by reset target object.
 *
 * @author Armin Waibel
 * @version $Id: PBLifeCycleEvent.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public final class PBLifeCycleEvent extends PersistenceBrokerEvent
{
    /** Denotes an event that happens before the insertion of an object. */
    public static final int TYPE_BEFORE_INSERT = 1;
    /** Denotes an event that happens before the deletion of an object. */
    public static final int TYPE_BEFORE_DELETE = 2;
    /** Denotes an event that happens before the update of an object. */
    public static final int TYPE_BEFORE_UPDATE = 3;
    /** Denotes an event that happens after the update of an object. */
    public static final int TYPE_AFTER_UPDATE = 4;
    /** Denotes an event that happens after the deletion of an object. */
    public static final int TYPE_AFTER_DELETE = 5;
    /** Denotes an event that happens after the lookup of an object. */
    public static final int TYPE_AFTER_LOOKUP = 6;
    /** Denotes an event that happens after the insertion of an object. */
    public static final int TYPE_AFTER_INSERT = 7;

    private Type eventType;
    private Object target;

    /**
     * Creates a new event instance.
     * 
     * @param broker    The broker
     * @param target    The object which caused the event
     * @param eventType The type of the event
     */
    public PBLifeCycleEvent(PersistenceBroker broker, Object target, Type eventType)
    {
        super(broker);
        this.target = target;
        this.eventType = eventType;
    }

    /**
     * Creates a new event instance.
     * 
     * @param broker The broker
     * @param type   The type of the event
     */
    public PBLifeCycleEvent(PersistenceBroker broker, Type type)
    {
        super(broker);
        this.eventType = type;
    }

    /**
     * Returns the target object as an instance of {@link PersistenceBrokerAware} if possible.
     * 
     * @return The {@link PersistenceBrokerAware} instance if there is a target and it implements
     *         this interface
     */
    public PersistenceBrokerAware getPersitenceBrokerAware()
    {
        if ((target != null) && (target instanceof PersistenceBrokerAware))
        {
            return (PersistenceBrokerAware)target;
        }
        else
        {
            return null;
        }
    }

    /**
     * Set the object that caused the event.
     * 
     * @param obj The object
     */
    public void setTarget(Object obj)
    {
        this.target = obj;
    }

    /**
     * Returns the object that caused the event.
     * 
     * @return The object
     */
    public Object getTarget()
    {
        return target;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        buf.append("target object", target).
                append("source object", getSource()).
                append("eventType", eventType.toString());
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
        /** Denotes an event that happens before the insertion of an object. */
        public static final Type BEFORE_INSERT = new Type(TYPE_BEFORE_INSERT);
        /** Denotes an event that happens before the update of an object. */
        public static final Type BEFORE_UPDATE = new Type(TYPE_BEFORE_UPDATE);
        /** Denotes an event that happens after the insertion of an object. */
        public static final Type AFTER_INSERT = new Type(TYPE_AFTER_INSERT);
        /** Denotes an event that happens after the update of an object. */
        public static final Type AFTER_UPDATE = new Type(TYPE_AFTER_UPDATE);
        /** Denotes an event that happens before the deletion of an object. */
        public static final Type BEFORE_DELETE = new Type(TYPE_BEFORE_DELETE);
        /** Denotes an event that happens after the deletion of an object. */
        public static final Type AFTER_DELETE = new Type(TYPE_AFTER_DELETE);
        /** Denotes an event that happens after the lookup of an object. */
        public static final Type AFTER_LOOKUP = new Type(TYPE_AFTER_LOOKUP);

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

        private static String typeAsName(int type)
        {
            if (type == TYPE_AFTER_DELETE)
            {
                return "AFTER_DELETE";
            }
            else if (type == TYPE_AFTER_LOOKUP)
            {
                return "AFTER_LOOKUP";
            }
            else if (type == TYPE_AFTER_INSERT)
            {
                return "AFTER_INSERT";
            }
            else if (type == TYPE_AFTER_UPDATE)
            {
                return "AFTER_UPDATE";
            }
            else if (type == TYPE_BEFORE_DELETE)
            {
                return "BEFORE_DELETE";
            }
            else if (type == TYPE_BEFORE_INSERT)
            {
                return "BEFORE_INSERT";
            }
            else if (type == TYPE_BEFORE_UPDATE)
            {
                return "BEFORE_UPDATE";
            }
            else
            {
                throw new OJBRuntimeException("Could not find type with typeId " + type);
            }
        }
    }
}
