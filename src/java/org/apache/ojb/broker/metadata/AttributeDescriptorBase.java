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

import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldFactory;

import java.io.Serializable;

/**
 * Is the base class for all other attribute descriptors.
 * It holds basic the mapping information for a specific attribute.
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: AttributeDescriptorBase.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class AttributeDescriptorBase extends DescriptorBase implements Serializable
{
	private static final long serialVersionUID = -818671542770428043L;

    protected PersistentField m_PersistentField = null;
    protected ClassDescriptor m_ClassDescriptor = null;

    /**
     * Constructor declaration
     */
    public AttributeDescriptorBase(ClassDescriptor descriptor)
    {
        this.m_ClassDescriptor = descriptor;
    }

    /**
     * @throws MetadataException if an error occours when setting the PersistenteField
     */
    public void setPersistentField(Class c, String fieldname)
    {
        m_PersistentField = PersistentFieldFactory.createPersistentField(c, fieldname);
    }

	public void setPersistentField(PersistentField pf)
	{
		m_PersistentField = pf;
	}

    /**
     *
     */
    public PersistentField getPersistentField()
    {
        return m_PersistentField;
    }

    /**
     * @return the name of the Attribute
     */
    public String getAttributeName()
    {
        return getPersistentField().getName();
    }

    /**
     * Gets the classDescriptor.
     * @return Returns a ClassDescriptor
     */
    public ClassDescriptor getClassDescriptor()
    {
        return m_ClassDescriptor;
    }

    /**
     * Sets the classDescriptor.
     * @param classDescriptor The classDescriptor to set
     */
    public void setClassDescriptor(ClassDescriptor classDescriptor)
    {
        m_ClassDescriptor = classDescriptor;
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(m_PersistentField);
        buf.append(", field_belongs_to " + m_ClassDescriptor.getClassNameOfObject());
        buf.append(", "+super.toString());
        return buf.toString();
    }
}
