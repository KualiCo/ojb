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
 * OJB implements the mapping conventions for JDBC as specified by the JDBC 3.0 specification and
 * this class representing the jdbc java types of the sql types mappings - e.g.
 * VARCHAR --> String, NUMERIC --> BigDecimal
 * (see JDBC 3.0 specification <em>Appendix B, Data Type Conversion Tables</em>).
 * <p/>
 * We differ two types of fields, <em>immutable</em> (like Integer, Long, String, ...) and <em>mutable</em>
 * (like Date, byte[], most SQL3 datatypes, ...).
 *
 * @version $Id: FieldType.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public interface FieldType extends Serializable
{
    /**
     * Returns a copy of the specified persistent class field (e.g. Long, Integer,...).
     * <br/>
     * NOTE: The specified field value 
     * @param fieldValue The field to copy.
     * @return A copy of the field or the same instance if copying is not possible. Depends on
     * the implementation.
     */
    public Object copy(Object fieldValue);

    /**
     * Returns <em>true</em> if the field value hasn't changed.
     * @param firstValue A field value object.
     * @param secondValue A field value object.
     * @return <em>true</em> if the field value hasn't changed.
     */
    public boolean equals(Object firstValue, Object secondValue);

    /**
     * Returns the sql {@link java.sql.Types} of this field.
     */
    public int getSqlType();

    /**
     * Dets the associated sql field type of this field.
     * @param jdbcType The associated {@link org.apache.ojb.broker.metadata.JdbcType}.
     */
    public void setSqlType(JdbcType jdbcType);

    /**
     * Returns <em>true</em> if the field type is mutable, e.g. a jdbc BLOB field or
     * jdbc TIMESTAMP field.
     * @return
     */
    public boolean isMutable();
}
