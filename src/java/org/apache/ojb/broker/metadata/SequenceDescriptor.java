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
import org.apache.ojb.broker.util.XmlHelper;

/**
 * Encapsulates sequence manager configuration properties managed by
 * {@link org.apache.ojb.broker.metadata.JdbcConnectionDescriptor}.
 * <br/>
 * All sequence manager implementation specific configuration
 * attributes are represented by key/value pairs in a
 * <code>Properties</code> object and could be reached via
 * {@link #getConfigurationProperties} or {@link #getAttribute(String key)}.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: SequenceDescriptor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class SequenceDescriptor implements Serializable, XmlCapable, AttributeContainer
{

	private static final long serialVersionUID = -5161713731380949398L;
    private JdbcConnectionDescriptor jcd;
    private Class sequenceManagerClass;
    private Properties configurationProperties;

    public SequenceDescriptor(JdbcConnectionDescriptor jcd)
    {
        this.jcd = jcd;
        this.configurationProperties = new Properties();
    }

    public SequenceDescriptor(JdbcConnectionDescriptor jcd, Class sequenceManagerClass)
    {
        this(jcd);
        this.sequenceManagerClass = sequenceManagerClass;
    }

    public JdbcConnectionDescriptor getJdbcConnectionDescriptor()
    {
        return jcd;
    }

    public void setJdbcConnectionDescriptor(JdbcConnectionDescriptor jcd)
    {
        this.jcd = jcd;
    }

    public Class getSequenceManagerClass()
    {
        return sequenceManagerClass;
    }

    public void setSequenceManagerClass(Class sequenceManagerClass)
    {
        this.sequenceManagerClass = sequenceManagerClass;
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
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        buf.append("   sequenceManagerClass", getSequenceManagerClass()).
        append("   Properties", getConfigurationProperties());
        return buf.toString();
    }

    public String toXML()
    {
        RepositoryTags tags = RepositoryTags.getInstance();
        String eol = SystemUtils.LINE_SEPARATOR;
        StringBuffer buf = new StringBuffer( 1024 );
        //opening tag + attributes
        buf.append( "      " );
        buf.append( tags.getOpeningTagNonClosingById( SEQUENCE_MANAGER ) );
        buf.append( eol );
        buf.append( "         " );
        buf.append( tags.getAttribute( SEQUENCE_MANAGER_CLASS, "" + getSequenceManagerClass().getName() ) );
        buf.append( "      >" );
        buf.append( eol );
        buf.append( "         <!-- " );
        buf.append( eol );
        buf.append( "         Add sequence manger properties here, using custom attributes" );
        buf.append( eol );
        buf.append( "         e.g. <attribute attribute-name=\"grabSize\" attribute-value=\"20\"/>" );
        buf.append( eol );
        buf.append( "         -->" );
        buf.append( eol );
        XmlHelper.appendSerializedAttributes(buf, "         ", getConfigurationProperties());
        buf.append( "      " );
        buf.append( tags.getClosingTagById( SEQUENCE_MANAGER ) );
        buf.append( eol );

        return buf.toString();
    }

}
