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

/**
 * This interface defines methods that are required for an object
 * to support the nested "attribute" tag in the repository file.
 * @author		Ron Gallagher
 */
public interface AttributeContainer extends Serializable
{
	/**
     * Store the specified attribute and it's value.
     * @param attributeName the name of the attribute to retrieve
     * @param attributeValue the attribute's value
     */
    public void addAttribute(String attributeName, String attributeValue);

    /**
     * Get the value of an attribute
     * @param attributeName the attribute to retrieve
     * @param defaultValue the value to return if the attribute is not present
     * @return the attribute value
     */
    public String getAttribute(String attributeName, String defaultValue);
    /**
     * Get the value of an attribute
     * @param attributeName the attribute to retrieve
     * @return the attribute value
     */
    public String getAttribute(String attributeName);
}

