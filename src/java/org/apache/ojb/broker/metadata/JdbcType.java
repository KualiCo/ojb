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

import java.sql.ResultSet;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.io.Serializable;

import org.apache.ojb.broker.util.sequence.SequenceManagerException;

/**
 * Represents a jdbc sql type object defined by the JDBC 3.0 specification to handle
 * data conversion (see JDBC 3.0 specification <em>Appendix B, Data Type Conversion Tables</em>).
 *
 * @see FieldType
 * @version $Id: JdbcType.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public interface JdbcType extends Serializable
{
	/**
     * Intern used flag.
     */
    public static final int MIN_INT = Integer.MIN_VALUE;

    /**
     * Returns an java object for this jdbc type by extract from the given
     * CallableStatement or ResultSet.
     * <br/>
     * NOTE: For internal use only!!
     * <br/>
     * Exactly one of the arguments of type CallableStatement or ResultSet
     * have to be non-null. If the 'columnId' argument is equals {@link #MIN_INT}, then the given 'columnName'
     * argument is used to lookup column. Else the given 'columnId' is used as column index.
     */
    public Object getObjectFromColumn(ResultSet rs, CallableStatement stmt, String columnName, int columnId)
            throws SQLException;

    /**
     * Convenience method for {@link #getObjectFromColumn(ResultSet, CallableStatement, String, int)}
     */
    public Object getObjectFromColumn(CallableStatement stmt, int columnId) throws SQLException;

    /**
     * Convenience method for {@link #getObjectFromColumn(ResultSet, CallableStatement, String, int)}
     */
    public Object getObjectFromColumn(ResultSet rs, String columnName) throws SQLException;

    /**
     * Convert the given {@link java.lang.Long} value to
     * a java object representation of this jdbc type.
     */
    public Object sequenceKeyConversion(Long identifier) throws SequenceManagerException;

    /**
     * Returns the representing {@link java.sql.Types sql type}.
     */
    public int getType();

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object obj);

    /**
     * Returns the associated {@link FieldType} (java field type mapped to this sql type).
     */
    public FieldType getFieldType();

//      // not used in code, but maybe useful in further versions
//    /**
//     * Convenience method for {@link #getObjectFromColumn(ResultSet, CallableStatement, String, int)}
//     */
//    Object getObjectFromColumn(CallableStatement stmt, String columnName) throws SQLException;
//    /**
//     * Convenience method for {@link #getObjectFromColumn(ResultSet, CallableStatement, String, int)}
//     */
//    Object getObjectFromColumn(ResultSet rs, int columnId) throws SQLException;
}
