package org.apache.ojb.broker.metadata;

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
import java.util.*;

/**
 * base class for all Descriptors. It is used to implement the AttributeContainer
 * interface which provides mechanics for user defined attributes.
 * @author Thomas Mahler
 */
class DescriptorBase implements AttributeContainer, Serializable
{
	static final long serialVersionUID = 713914612744155925L;
    /** holds user defined attributes */
    private Map attributeMap = null;

    /**
     * Constructor for DescriptorBase.
     */
    public DescriptorBase()
    {
    }

    /**
     * @see org.apache.ojb.broker.metadata.AttributeContainer#addAttribute(String, String)
     */
    public void addAttribute(String attributeName, String attributeValue)
    {
        // Don't allow null attribute names.
        if (attributeName == null)
        {
            return;
        }
        // Set up the attribute list
        if (attributeMap == null)
        {
            attributeMap = new HashMap();
        }
        // Add the entry.
        attributeMap.put(attributeName, attributeValue);
    }

    /**
     * @see org.apache.ojb.broker.metadata.AttributeContainer#getAttribute(String, String)
     */
    public String getAttribute(String attributeName, String defaultValue)
    {
        String result = defaultValue;
        if (attributeMap != null)
        {
            result = (String) attributeMap.get(attributeName);
            if (result == null)
            {
                result = defaultValue;
            }
        }
        return result;
    }

    /**
     * @see org.apache.ojb.broker.metadata.AttributeContainer#getAttribute(String)
     */
    public String getAttribute(String attributeName)
    {
        return this.getAttribute(attributeName, null);
    }

    /**
     * Returns the attribute map (name, value) of this descriptor. Note that the
     * returned map is not modifiable.
     * 
     * @return The attributes
     */
    public Map getAttributes()
    {
        return Collections.unmodifiableMap(attributeMap);
    }

    /**
     * Returns an array of the names of all atributes of this descriptor.
     * 
     * @return The list of attribute names (will not be <code>null</code>)
     */
    public String[] getAttributeNames()
    {
        Set      keys   = (attributeMap == null ? new HashSet() : attributeMap.keySet());
        String[] result = new String[keys.size()];

        keys.toArray(result);
        return result;
    }
    
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("custom attributes [");
        buf.append(attributeMap);
        buf.append("]");
        return buf.toString();
    }
}
