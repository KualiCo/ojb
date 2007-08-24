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

import org.apache.commons.lang.SystemUtils;

import java.util.Properties;
import java.util.Enumeration;

/**
 * Simple helper class with static methods for common XML-handling tasks.
 *
 * @version CVS $Id: XmlHelper.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 * @since OJB 1.0.4
 */
public class XmlHelper
{

    /** End-of-line string used in serialized XML. */
    public static final String XML_EOL = SystemUtils.LINE_SEPARATOR;

    /**
     * Returns an XML-string with serialized configuration attributes.
     * Used when serializing {@link org.apache.ojb.broker.metadata.AttributeContainer} attributes.
     * @param prefix the line prefix (ie indent) or null for no prefix
     * @param attributeProperties the properties object holding attributes to be serialized
     * (null-safe)
     * @return XML-string with serialized configuration attributes (never null)
     */
    public static String getSerializedAttributes(final String prefix,
                                                 final Properties attributeProperties)
    {
        final StringBuffer buf = new StringBuffer();
        appendSerializedAttributes(buf, prefix, attributeProperties);
        return buf.toString();
    }

    /**
     * Appends an XML-string with serialized configuration attributes to the specified buffer.
     * Used when serializing {@link org.apache.ojb.broker.metadata.AttributeContainer} attributes.
     * @param buf the string buffer to append to
     * @param prefix the line prefix (ie indent) or null for no prefix
     * @param attributeProperties the properties object holding attributes to be serialized
     * (null-safe)
     */
    public static void appendSerializedAttributes(final StringBuffer buf,
                                                  final String prefix,
                                                  final Properties attributeProperties)
    {
        if (attributeProperties != null)
        {
            final Enumeration keys = attributeProperties.keys();
            while (keys.hasMoreElements())
            {
                final String key = (String) keys.nextElement();
                final String value = attributeProperties.getProperty( key );
                if (prefix != null)
                {
                    buf.append(prefix);
                }
                buf.append("<attribute attribute-name=\"").append(key);
                buf.append("\" attribute-value=\"" ).append(value);
                buf.append("\"/>");
                buf.append(XML_EOL);
            }
        }
    }

}
