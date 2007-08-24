package xdoclet.modules.ojb.model;

/* Copyright 2004-2005 The Apache Software Foundation
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

import java.util.*;

/**
 * Base type for ojb repository file defs.
 *
 * @author    <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 * @created   April 13, 2003
 */
public abstract class DefBase
{
    /** The owner (parent def) */
    private DefBase _owner;
    /** The name */
    private String  _name;
    /** The properties of the class */
    private Properties _properties = new Properties();

    /**
     * Initializes the base def object.
     *
     * @param name  The name
     */
    public DefBase(String name)
    {
        _name = name;
    }

    /**
     * Initializes the base def object to be a copy of the given base def object (except for the owner).
     *
     * @param src    The original base def object
     * @param prefix A prefix for the name
     */
    public DefBase(DefBase src, String prefix)
    {
        _name = (prefix != null ? prefix + src._name : src._name);

        String key;

        for (Iterator it = src._properties.keySet().iterator(); it.hasNext();)
        {
            key = (String)it.next();
            setProperty(key, src._properties.getProperty(key));
        }
    }

    /**
     * Returns the owner of this def.
     * 
     * @return The owner
     */
    public DefBase getOwner()
    {
        return _owner;
    }

    /**
     * Sets the owner of this def.
     * 
     * @param owner The owner
     */
    public void setOwner(DefBase owner)
    {
        _owner = owner;
    }

    /**
     * Returns the name of the def object.
     *
     * @return   The name
     */
    public String getName()
    {
        return _name;
    }

    /**
     * Returns the value of the specified property.
     *
     * @param name  The name of the property
     * @return      The value
     */
    public String getProperty(String name)
    {
        return _properties.getProperty(name);
    }

    /**
     * Returns the boolean value of the specified property.
     *
     * @param name         The name of the property
     * @param defaultValue The value to use if the property is not set or not a boolean
     * @return The value
     */
    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        return PropertyHelper.toBoolean(_properties.getProperty(name), defaultValue);
    }

    /**
     * Returns the property names.
     * 
     * @return The names
     */
    public Iterator getPropertyNames()
    {
        return _properties.keySet().iterator();
    }

    /**
     * Sets a property.
     *
     * @param name   The property name
     * @param value  The property value
     */
    public void setProperty(String name, String value)
    {
        if ((value == null) || (value.length() == 0))
        {
            _properties.remove(name);
        }
        else
        {
            _properties.setProperty(name, value);
        }
    }

    /**
     * Determines whether a properties exists.
     *
     * @param name  The property name
     * @return      <code>true</code> if the property exists
     */
    public boolean hasProperty(String name)
    {
        return _properties.containsKey(name);
    }

    /**
     * Applies the modifications contained in the given properties object.
     * Properties are removed by having a <code>null</code> entry in the mods object.
     * Properties that are new in mods object, are added to this def object.
     * 
     * @param mods The modifications
     */
    public void applyModifications(Properties mods)
    {
        String key;
        String value;

        for (Iterator it = mods.keySet().iterator(); it.hasNext();)
        {
            key   = (String)it.next();
            value = mods.getProperty(key);
            setProperty(key, value);
        }
    }

    public String toString()
    {
        return getName();
    }
}
