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

import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.io.Serializable;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ojb.broker.OJBRuntimeException;

/**
 * Encapsulates all {@link FieldType} as inner classes.
 *
 * @version $Id: FieldTypeClasses.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
class FieldTypeClasses
{
    private FieldTypeClasses()
    {
    }

    /**
     * Returns a {@link FieldType} instance for the given sql type
     * (see {@link java.sql.Types}) as specified in JDBC 3.0 specification
     * (see JDBC 3.0 specification <em>Appendix B, Data Type Conversion Tables</em>).
     *
     * @param jdbcType Specify the type to look for.
     * @return A new specific {@link FieldType} instance.
     */
    static FieldType newFieldType(JdbcType jdbcType)
    {
        FieldType result = null;
        switch (jdbcType.getType())
        {
            case Types.ARRAY:
                result = new ArrayFieldType();
                break;
            case Types.BIGINT:
                result = new LongFieldType();
                break;
            case Types.BINARY:
                result = new ByteArrayFieldType();
                break;
            case Types.BIT:
                result = new BooleanFieldType();
                break;
            case Types.BLOB:
                result = new BlobFieldType();
                break;
            case Types.CHAR:
                result = new StringFieldType();
                break;
            case Types.CLOB:
                result = new ClobFieldType();
                break;
            case Types.DATE:
                result = new DateFieldType();
                break;
            case Types.DECIMAL:
                result = new BigDecimalFieldType();
                break;
// Not needed, user have to use the underlying sql datatype in OJB mapping files
//            case Types.DISTINCT:
//                result = new DistinctFieldType();
//                break;
            case Types.DOUBLE:
                result = new DoubleFieldType();
                break;
            case Types.FLOAT:
                result = new FloatFieldType();
                break;
            case Types.INTEGER:
                result = new IntegerFieldType();
                break;
            case Types.JAVA_OBJECT:
                result = new JavaObjectFieldType();
                break;
            case Types.LONGVARBINARY:
                result = new ByteArrayFieldType();
                break;
            case Types.LONGVARCHAR:
                result = new StringFieldType();
                break;
            case Types.NUMERIC:
                result = new BigDecimalFieldType();
                break;
            case Types.REAL:
                result = new FloatFieldType();
                break;
            case Types.REF:
                result = new RefFieldType();
                break;
            case Types.SMALLINT:
                result = new ShortFieldType();
                break;
            case Types.STRUCT:
                result = new StructFieldType();
                break;
            case Types.TIME:
                result = new TimeFieldType();
                break;
            case Types.TIMESTAMP:
                result = new TimestampFieldType();
                break;
            case Types.TINYINT:
                result = new ByteFieldType();
                break;
            case Types.VARBINARY:
                result = new ByteArrayFieldType();
                break;
            case Types.VARCHAR:
                result = new StringFieldType();
                break;
            case Types.OTHER:
                result = new JavaObjectFieldType();
                break;
//
//            case Types.NULL:
//                result = new NullFieldType();
//                break;

//#ifdef JDBC30
            case Types.BOOLEAN:
                result = new BooleanFieldType();
                break;
            case Types.DATALINK:
                result = new URLFieldType();
                break;
//#endif
            default:
                throw new OJBRuntimeException("Unkown or not supported field type specified, specified jdbc type was '"
                        + jdbcType + "', as string: " + JdbcTypesHelper.getSqlTypeAsString(jdbcType.getType()));
        }
        // make sure that the sql type was set
        result.setSqlType(jdbcType);
        return result;
    }

    /**
     * Base class for all fields.
     */
    abstract static class BaseFieldType implements FieldType
    {
        int sqlType;

        public void setSqlType(JdbcType jdbcType)
        {
            sqlType = jdbcType.getType();
        }

        public int getSqlType()
        {
            return sqlType;
        }

        /**
         * Helper method to copy an object if possible.
         *
         * @param toCopy The object to copy.
         * @return The copy of the object or <em>null</em> clone is not supported.
         */
        Object copyIfCloneable(Object toCopy)
        {
            Object result = null;
            if(toCopy instanceof Cloneable)
            {
                try
                {
                    Method m = toCopy.getClass().getMethod("clone", ArrayUtils.EMPTY_CLASS_ARRAY);
                    /*
                    arminw:
                    By definition the overrided object.clone() method has to be public
                    so we don't need to make it accessible
                    */
                    //m.setAccessible(true);
                    result = m.invoke(toCopy, null);
                }
                catch(Exception e)
                {
                    throw new OJBRuntimeException("Can't invoke method 'clone' on object: " + toCopy, e);
                }
            }
            return result;
        }

        /**
         * Helper method to copy an object if possible.
         *
         * @param toCopy The object to copy.
         * @return The copy of the object or <em>null</em> if serialization is not supported.
         */
        Object copyIfSerializeable(Object toCopy)
        {
            Object result = null;
            if(toCopy instanceof Serializable)
            {
                result = SerializationUtils.clone((Serializable) toCopy);
            }
            return result;
        }

        public String toString()
        {
            return new ToStringBuilder(this)
                    .append("sqlType", sqlType)
                    .append("sqlTypeAsString", JdbcTypesHelper.getSqlTypeAsString(sqlType))
                    .append("isMutable", isMutable()).toString();
        }
    }

    /**
     * Base class for all <em>immutable</em> types, like Number fields, Strings, ...
     */
    abstract static class ImmutableFieldType extends BaseFieldType
    {
        public boolean isMutable()
        {
            return false;
        }

        public Object copy(Object source)
        {
            return source;
        }

        public boolean equals(Object firstValue, Object secondValue)
        {
            return ObjectUtils.equals(firstValue, secondValue);
        }
    }

    /**
     * Base class for all <em>mutable</em> fields.
     */
    abstract static class MutableFieldType extends BaseFieldType
    {
        public boolean isMutable()
        {
            return true;
        }

        public boolean equals(Object firstValue, Object secondValue)
        {
            return ObjectUtils.equals(firstValue, secondValue);
        }
    }

    /**
     * Clob fields are logical pointer to DB, so for OJB it's immutable
     * @see BlobFieldType
     */
    public static class ClobFieldType extends ImmutableFieldType
    {
    }

    /**
     * Blob fields are logical pointer to DB, so for OJB it's immutable.
     * Snip of JDBC specification:
     * "An application does not deal directly with the LOCATOR(blob) and
     * LOCATOR(clob) types that are defined in SQL. By default, a JDBC
     * driver should implement the Blob and Clob interfaces using the
     * appropriate locator type. Also by default, Blob and Clob objects
     * remain valid only during the transaction in which they are created."
     */
    public static class BlobFieldType extends ImmutableFieldType
    {
    }

    /**
     * Array fields are logical pointer to DB, so for OJB it's immutable.
     * Snip of JDBC specification:
     * "The Array object returned to an application by the ResultSet.getArray and
     * CallableStatement.getArray methods is a logical pointer to the SQL ARRAY
     * value in the database; it does not contain the contents of the SQL ARRAY value."
     */
    public static class ArrayFieldType extends ImmutableFieldType
    {
    }

    /**
     * Ref fields are logical pointer to DB, so for OJB it's immutable.
     * Snip of JDBC specification:
     * "An SQL REF value is a pointer; therefore, a Ref object, which is the mapping of a
     * REF value, is likewise a pointer and does not contain the data of the structured type
     * instance to which it refers."
     */
    public static class RefFieldType extends ImmutableFieldType
    {
    }

    /**
     * When using SQL UDT's it's possible that the jdbc-driver returns
     * full materialized java objects defined by the user.
     */
    public static class StructFieldType extends MutableFieldType
    {
        // TODO: does this make sense?? or Struct instances always Locator objects?
        public Object copy(Object fieldValue)
        {
            if(fieldValue == null) return null;

            Object copy = copyIfCloneable(fieldValue);
            if(copy == null)
            {
                copy = copyIfSerializeable(fieldValue);
            }
            return copy == null ? fieldValue : copy;
        }
    }

    /**
     * If a user-defined object is used, we can check if object is
     * {@link Cloneable} or {@link Serializable} to copy the object.
     * If not possible return the specified object instance.
     */
    public static class JavaObjectFieldType extends MutableFieldType
    {
        public Object copy(Object fieldValue)
        {
            if(fieldValue == null) return null;

            Object copy = copyIfCloneable(fieldValue);
            if(copy == null)
            {
                copy = copyIfSerializeable(fieldValue);
            }
            return copy == null ? fieldValue : copy;
        }
    }

    public static class ByteArrayFieldType extends MutableFieldType
    {
        public Object copy(Object fieldValue)
        {
            byte[] result = null;
            if(fieldValue != null)
            {
                byte[] source = (byte[]) fieldValue;
                int length = source.length;
                result = new byte[length];
                System.arraycopy(fieldValue, 0, result, 0, length);
            }
            return result;
        }

        public boolean equals(Object firstValue, Object secondValue)
        {
            return Arrays.equals((byte[]) firstValue, (byte[]) secondValue);
        }
    }

    public static class DateFieldType extends MutableFieldType
    {
        public Object copy(Object fieldValue)
        {
            Date source = (Date) fieldValue;
            return source != null ? new Date(source.getTime()) : null;
        }
    }

    public static class TimeFieldType extends MutableFieldType
    {
        public Object copy(Object fieldValue)
        {
            Time source = (Time) fieldValue;
            return source != null ? new Time(source.getTime()) : null;
        }
    }

    public static class TimestampFieldType extends MutableFieldType
    {
        public Object copy(Object fieldValue)
        {
            Timestamp result = null;
            if(fieldValue != null)
            {
                Timestamp source = (Timestamp) fieldValue;
                result = (Timestamp) source.clone();
            }
            return result;
        }
    }

    public static class StringFieldType extends ImmutableFieldType
    {
    }

    public static class BigDecimalFieldType extends ImmutableFieldType
    {
    }

    public static class BooleanFieldType extends ImmutableFieldType
    {
    }

    public static class ByteFieldType extends ImmutableFieldType
    {
    }

    public static class ShortFieldType extends ImmutableFieldType
    {

    }

    public static class IntegerFieldType extends ImmutableFieldType
    {

    }

    public static class LongFieldType extends ImmutableFieldType
    {

    }

    public static class FloatFieldType extends ImmutableFieldType
    {

    }

    public static class DoubleFieldType extends ImmutableFieldType
    {

    }

    public static class URLFieldType extends ImmutableFieldType
    {

    }
}
