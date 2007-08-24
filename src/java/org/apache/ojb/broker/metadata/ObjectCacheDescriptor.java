package org.apache.ojb.broker.metadata;

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
import java.util.Properties;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.cache.ObjectCacheEmptyImpl;
import org.apache.ojb.broker.util.XmlHelper;

/**
 * Encapsulates a {@link org.apache.ojb.broker.cache.ObjectCache} implementation class
 * and its proprietary configuration properties.
 * <br/>
 * All ObjectCache implementation specific configuration
 * attributes are represented by key/value pairs in a
 * <code>Properties</code> object and could be reached via
 * {@link #getConfigurationProperties} or {@link #getAttribute(String key)}.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ObjectCacheDescriptor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class ObjectCacheDescriptor implements Serializable, XmlCapable, AttributeContainer
{
	private static final long serialVersionUID = 2583853027407750053L;
    private static final Class DEF_OBJECT_CACHE = ObjectCacheEmptyImpl.class;
    private Class objectCache;
    private Properties configurationProperties;

    public ObjectCacheDescriptor()
    {
        this.configurationProperties = new Properties();
        this.objectCache = DEF_OBJECT_CACHE;
    }

    public ObjectCacheDescriptor(Class objectCacheClass)
    {
        this();
        this.objectCache = objectCacheClass;
    }

    public Class getObjectCache()
    {
        return objectCache;
    }

    public void setObjectCache(Class objectCache)
    {
        this.objectCache = objectCache;
    }

    public void addAttribute(String attributeName, String attributeValue)
    {
        configurationProperties.setProperty(attributeName, attributeValue);
    }

    public String getAttribute(String key)
    {
        return getAttribute(key, null);
    }

    public String getAttribute(String attributeName, String defaultValue)
    {
        String result = configurationProperties.getProperty(attributeName);
        if(result == null) result = defaultValue;
        return result;
    }

    public Properties getConfigurationProperties()
    {
        return configurationProperties;
    }

    public void setConfigurationProperties(Properties configurationProperties)
    {
        this.configurationProperties = configurationProperties;
    }

    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE);
        buf.append("ObjectCache", getObjectCache()).
        append("Properties", getConfigurationProperties());
        return buf.toString();
    }

    public String toXML()
    {
        RepositoryTags tags = RepositoryTags.getInstance();
        String eol = SystemUtils.LINE_SEPARATOR;
        StringBuffer buf = new StringBuffer(1024);
        //opening tag + attributes
        buf.append("      ");
        buf.append(tags.getOpeningTagNonClosingById(OBJECT_CACHE));
        buf.append(eol);
        buf.append("         ");
        buf.append(tags.getAttribute(CLASS_NAME, "" + getObjectCache() != null ? getObjectCache().getName() : ""));
        buf.append("      >");
        buf.append(eol);
        buf.append("         <!-- ");
        buf.append(eol);
        buf.append("         Add proprietary ObjectCache implementation properties here, using custom attributes");
        buf.append(eol);
        buf.append("         e.g. <attribute attribute-name=\"timeout\" attribute-value=\"2000\"/>");
        buf.append(eol);
        buf.append("         -->");
        buf.append(eol);
        XmlHelper.appendSerializedAttributes(buf, "         ", getConfigurationProperties());
        buf.append("      ");
        buf.append(tags.getClosingTagById(OBJECT_CACHE));
        buf.append(eol);

        return buf.toString();
    }

}
