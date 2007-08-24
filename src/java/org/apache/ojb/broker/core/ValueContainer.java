package org.apache.ojb.broker.core;

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

import org.apache.ojb.broker.metadata.JdbcType;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public final class ValueContainer implements Serializable
{
    private static final long serialVersionUID = 3689069556052340793L;
  
    private final JdbcType m_jdbcType;
    private final Object m_value;
    private int hc;

    public ValueContainer(Object value, JdbcType jdbcType)
    {
        this.m_jdbcType = jdbcType;
        this.m_value = value;
    }

    public JdbcType getJdbcType()
    {
        return m_jdbcType;
    }

    public Object getValue()
    {
        return m_value;
    }

    public boolean equals(Object obj)
    {
        if(obj == this) return true;
        boolean result = false;
        if(obj instanceof ValueContainer)
        {
            final ValueContainer container = (ValueContainer) obj;
            // if jdbcType was null, we can't compare
            result = this.m_jdbcType != null ? this.m_jdbcType.equals(container.getJdbcType()) : false;
            if(result)
            {
                result = new EqualsBuilder().append(this.m_value, container.getValue()).isEquals();
            }
        }
        return result;
    }

    public int hashCode()
    {
//        int hash =  m_value != null ? m_value.hashCode() : 0;
//        hash += m_jdbcType != null ? m_jdbcType.hashCode() : 0;
//        return hash;
        if(hc == 0) hc = new HashCodeBuilder().append(m_jdbcType).append(m_value).toHashCode();
        return hc;
    }

    public String toString()
    {
        return this.getClass().getName() + "[jdbcType: "
        + m_jdbcType
        + ", value: " + m_value + "]";
    }

}
