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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.util.sequence.SequenceManagerException;

/**
 * Helper class which provide all supported {@link JdbcType} classes
 * (based on the {@link java.sql.Types}) as inner classes.
 *
 * @see JdbcType
 * @version $Id: JdbcTypesHelper.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class JdbcTypesHelper
{
    private static Map jdbcObjectTypesFromType = new HashMap();
    private static Map jdbcObjectTypesFromName = new HashMap();

    /**
     * Hold out all JdbcType in a static maps
     */
    static
    {
        setJdbcType("array", Types.ARRAY, new T_Array());
        setJdbcType("bigint", Types.BIGINT, new T_BigInt());
        setJdbcType("binary", Types.BINARY, new T_Binary());
        setJdbcType("bit", Types.BIT, new T_Bit());
        setJdbcType("blob", Types.BLOB, new T_Blob());
        setJdbcType("char", Types.CHAR, new T_Char());
        setJdbcType("clob", Types.CLOB, new T_Clob());
        setJdbcType("date", Types.DATE, new T_Date());
        setJdbcType("decimal", Types.DECIMAL, new T_Decimal());
        setJdbcType("double", Types.DOUBLE, new T_Double());
        setJdbcType("float", Types.FLOAT, new T_Float());
        setJdbcType("integer", Types.INTEGER, new T_Integer());
        setJdbcType("longvarbinary", Types.LONGVARBINARY, new T_LongVarBinary());
        setJdbcType("longvarchar", Types.LONGVARCHAR, new T_LongVarChar());
        setJdbcType("numeric", Types.NUMERIC, new T_Numeric());
        setJdbcType("real", Types.REAL, new T_Real());
        setJdbcType("ref", Types.REF, new T_Ref());
        setJdbcType("smallint", Types.SMALLINT, new T_SmallInt());
        setJdbcType("struct", Types.STRUCT, new T_Struct());
        setJdbcType("time", Types.TIME, new T_Time());
        setJdbcType("timestamp", Types.TIMESTAMP, new T_Timestamp());
        setJdbcType("tinyint", Types.TINYINT, new T_TinyInt());
        setJdbcType("varbinary", Types.VARBINARY, new T_VarBinary());
        setJdbcType("varchar", Types.VARCHAR, new T_Varchar());
        
//#ifdef JDBC30
        setJdbcType("boolean", Types.BOOLEAN, new T_Boolean());
        setJdbcType("datalink", Types.DATALINK, new T_Datalink());
//#endif
        
    }

    public JdbcTypesHelper()
    {
        // default constructor
    }

    /**
     * Set the {@link JdbcType} by name and index.
     * @param typeName Name of the type
     * @param typeIndex index of the type
     * @param type the type
     */
    public static void setJdbcType(String typeName, int typeIndex, JdbcType type)
    {
        setJdbcTypeByName(typeName, type);
        setJdbcTypeByTypesIndex(typeIndex, type);
    }

    /**
     * Return the {@link JdbcType} for the given jdbc {@link java.sql.Types type}.
     */
    public static JdbcType getJdbcTypeByTypesIndex(Integer type)
    {
        return (JdbcType) jdbcObjectTypesFromType.get(type);
    }

    /**
     * Set the {@link JdbcType} by index.
     * @param typeIndex index of the type
     * @param type the type
     */
    public static void setJdbcTypeByTypesIndex(int typeIndex, JdbcType type)
    {
        jdbcObjectTypesFromType.put(new Integer(typeIndex), type);
    }

    /**
     * Lookup the {@link JdbcType} by name. If name was not found an exception
     * is thrown.
     */
    public static JdbcType getJdbcTypeByName(String typeName)
    {
        JdbcType result = null;
        result = (JdbcType) jdbcObjectTypesFromName.get(typeName.toLowerCase());
        if (result == null)
        {
            throw new OJBRuntimeException("The type " + typeName + " can not be handled by OJB." +
                    " Please specify only types as defined by java.sql.Types.");
        }
        return result;
    }
    
    /**
     * Set the {@link JdbcType} by name.
     * @param typeName Name of the type
     * @param type the type
     */
    public static void setJdbcTypeByName(String typeName, JdbcType type)
    {
        jdbcObjectTypesFromName.put(typeName, type);
    }

    /**
     * Try to automatically assign a jdbc type for the given
     * java type name. This method is used if e.g. in metadata a
     * column type was not set.
     *
     * @see FieldDescriptor#getJdbcType
     */
    public static JdbcType getJdbcTypeByReflection(String fieldType)
    {
        JdbcType result;
        
        if (fieldType.equalsIgnoreCase(Character.class.getName()) || fieldType.equalsIgnoreCase("char"))
            result = getJdbcTypeByName("char");
        else if (fieldType.equalsIgnoreCase(Short.class.getName()) || fieldType.equalsIgnoreCase("short"))
            result = getJdbcTypeByName("smallint");
        else if (fieldType.equalsIgnoreCase(Integer.class.getName()) || fieldType.equalsIgnoreCase("int"))
            result = getJdbcTypeByName("integer");
        else if (fieldType.equalsIgnoreCase(Long.class.getName()) || fieldType.equalsIgnoreCase("long"))
            result = getJdbcTypeByName("bigint");
        else if (fieldType.equalsIgnoreCase(Byte.class.getName()) || fieldType.equalsIgnoreCase("byte"))
            result = getJdbcTypeByName("tinyint");
        else if (fieldType.equalsIgnoreCase(Float.class.getName()) || fieldType.equalsIgnoreCase("float"))
            result = getJdbcTypeByName("real");
        else if (fieldType.equalsIgnoreCase(Double.class.getName()) || fieldType.equalsIgnoreCase("double"))
            result = getJdbcTypeByName("float");
        else if (fieldType.equalsIgnoreCase(String.class.getName()))
            result = getJdbcTypeByName("varchar");
        /*
        TODO: arminw: useful? This only will work in conjunction with  a FieldConversion
        */
        else if (fieldType.equalsIgnoreCase(java.util.Date.class.getName()))
            result = getJdbcTypeByName("date");
        else if (fieldType.equalsIgnoreCase(Date.class.getName()))
            result = getJdbcTypeByName("date");
        else if (fieldType.equalsIgnoreCase(Time.class.getName()))
            result = getJdbcTypeByName("time");
        else if (fieldType.equalsIgnoreCase(Timestamp.class.getName()))
            result = getJdbcTypeByName("timestamp");
        else if (fieldType.equalsIgnoreCase(BigDecimal.class.getName()))
            result = getJdbcTypeByName("decimal");
        else if (fieldType.equalsIgnoreCase(Ref.class.getName()))
            result = getJdbcTypeByName("ref");
        else if (fieldType.equalsIgnoreCase(Struct.class.getName()))
            result = getJdbcTypeByName("struct");
        else if (fieldType.equalsIgnoreCase(Boolean.class.getName()) || fieldType.equalsIgnoreCase("boolean"))
            result = getJdbcTypeByName("bit");
//#ifdef JDBC30
        else if (fieldType.equalsIgnoreCase(URL.class.getName()))
            result = getJdbcTypeByName("datalink");
//#endif
        else
            throw new OJBRuntimeException("The type " + fieldType + " can not be handled by OJB automatically."
                    + " Please specify a type as defined by java.sql.Types in your field-descriptor");
        return result;
    }


    /**
     * Returns an java object read from the specified ResultSet column.
     */
    public static Object getObjectFromColumn(ResultSet rs, Integer jdbcType, int columnId)
            throws SQLException
    {
        return getObjectFromColumn(rs, null, jdbcType, null, columnId);
    }

    /**
     * Returns an java object for the given jdbcType by extract from the given
     * CallableStatement or ResultSet.
     * NOTE: Exactly one of the arguments of type CallableStatement or ResultSet
     * have to be non-null.
     * If the 'columnId' argument is equals {@link JdbcType#MIN_INT}, then the given 'columnName'
     * argument is used to lookup column. Else the given 'columnId' is used as column index.
     */
    private static Object getObjectFromColumn(ResultSet rs, CallableStatement stmt, Integer jdbcType, String columnName, int columnId)
            throws SQLException
    {
        return getJdbcTypeByTypesIndex(jdbcType).getObjectFromColumn(rs, stmt, columnName, columnId);
    }

    /**
     * Returns a string representation of the given {@link java.sql.Types} value.
     */
    public static String getSqlTypeAsString(int jdbcType)
    {
        String statusName = "*can't find String representation for sql type '" + jdbcType + "'*";
        try
        {
            Field[] fields = Types.class.getDeclaredFields();
            for (int i = 0; i < fields.length; i++)
            {
                if (fields[i].getInt(null) == jdbcType)
                {
                    statusName = fields[i].getName();
                    break;
                }
            }
        }
        catch (Exception ignore)
        {
            // ignore it
        }
        return statusName;
    }


    //======================================================================================
    // inner classes implementing JdbcType interface
    //======================================================================================

    public abstract static class BaseType implements JdbcType
    {
        private FieldType fieldType;

        protected BaseType()
        {
            fieldType = FieldTypeClasses.newFieldType(this);
        }

        abstract Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException;

        abstract Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException;

        abstract Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException;
        /*
        only supported by jdk >= 1.4x, maybe useful in further versions
        */
        // abstract Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException;

        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            boolean result = false;
            if (obj instanceof JdbcType)
            {
                result = this.getType() == ((JdbcType) obj).getType();
            }
            return result;
        }

        public int hashCode()
        {
            return getType();
        }

        public FieldType getFieldType()
        {
            return fieldType;
        }

        public Object getObjectFromColumn(CallableStatement stmt, int columnId) throws SQLException
        {
            return getObjectFromColumn(null, stmt, null, columnId);
        }

        public Object getObjectFromColumn(ResultSet rs, String columnName) throws SQLException
        {
            return getObjectFromColumn(rs, null, columnName, MIN_INT);
        }

        public Object getObjectFromColumn(final ResultSet rs, final CallableStatement stmt,
                                          final String columnName, int columnIndex) throws SQLException
        {
            if (stmt != null)
            {
//                return columnIndex == MIN_INT
//                        ? readValueFromStatement(stmt, columnName) : readValueFromStatement(stmt, columnIndex);
                if (columnIndex == MIN_INT)
                {
                    throw new UnsupportedOperationException("Not implemented yet");
                }
                else
                {
                    return readValueFromStatement(stmt, columnIndex);
                }
            }
            else
            {
                return columnIndex == MIN_INT
                        ? readValueFromResultSet(rs, columnName) : readValueFromResultSet(rs, columnIndex);
            }
        }

        public String toString()
        {
            return new ToStringBuilder(this)
                    .append("jdbcType", getType())
                    .append("jdbcTypeString", getSqlTypeAsString(getType()))
                    .append("associatedFieldType", getFieldType())
                    .toString();
        }

//      // not used in code, but maybe useful in further versions
//        public Object getObjectFromColumn(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return getObjectFromColumn(null, stmt, columnName, MIN_INT);
//        }
//
//        public Object getObjectFromColumn(ResultSet rs, int columnId) throws SQLException
//        {
//            return getObjectFromColumn(rs, null, null, columnId);
//        }

    }


    public static final class T_Char extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return identifier.toString();
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getString(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getString(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return rs.getString(columnName);
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return rs.getString(columnIndex);
        }

        public int getType()
        {
            return Types.CHAR;
        }
    }

    public static final class T_Varchar extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return identifier.toString();
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getString(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getString(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return rs.getString(columnName);
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return rs.getString(columnIndex);
        }

        public int getType()
        {
            return Types.VARCHAR;
        }
    }

    public static final class T_LongVarChar extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return identifier.toString();
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getString(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getString(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return rs.getString(columnName);
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return rs.getString(columnIndex);
        }

        public int getType()
        {
            return Types.LONGVARCHAR;
        }
    }

    public static final class T_Numeric extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return new BigDecimal(identifier.longValue());
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getBigDecimal(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getBigDecimal(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return rs.getBigDecimal(columnName);
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return rs.getBigDecimal(columnIndex);
        }

        public int getType()
        {
            return Types.NUMERIC;
        }
    }

    public static final class T_Decimal extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return new BigDecimal(identifier.longValue());
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getBigDecimal(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getBigDecimal(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return rs.getBigDecimal(columnName);
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return rs.getBigDecimal(columnIndex);
        }

        public int getType()
        {
            return Types.DECIMAL;
        }
    }

    public static final class T_Bit extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier) throws SequenceManagerException
        {
            throw new SequenceManagerException("Not supported sequence key type 'BIT'");
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            boolean temp = stmt.getBoolean(columnName);
//            return (stmt.wasNull() ? null : new Boolean(temp));
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            boolean temp = stmt.getBoolean(columnIndex);
            return (stmt.wasNull() ? null : BooleanUtils.toBooleanObject(temp));
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            boolean temp = rs.getBoolean(columnName);
            return (rs.wasNull() ? null : BooleanUtils.toBooleanObject(temp));
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            boolean temp = rs.getBoolean(columnIndex);
            return (rs.wasNull() ? null : BooleanUtils.toBooleanObject(temp));
        }

        public int getType()
        {
            return Types.BIT;
        }
    }

//#ifdef JDBC30
    public static final class T_Boolean extends BaseType
    {
        public Object sequenceKeyConversion(final Long identifier) throws SequenceManagerException
        {
            throw new SequenceManagerException("Not supported sequence key type 'BOOLEAN'");
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            boolean temp = stmt.getBoolean(columnName);
//            return (stmt.wasNull() ? null : BooleanUtils.toBooleanObject(temp));
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            boolean temp = stmt.getBoolean(columnIndex);
            return (stmt.wasNull() ? null : BooleanUtils.toBooleanObject(temp));
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            boolean temp = rs.getBoolean(columnName);
            return (rs.wasNull() ? null : BooleanUtils.toBooleanObject(temp));
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            boolean temp = rs.getBoolean(columnIndex);
            return (rs.wasNull() ? null : BooleanUtils.toBooleanObject(temp));
        }

        public int getType()
        {
            return Types.BOOLEAN;
        }
    }
//#endif

    public static final class T_TinyInt extends BaseType
    {
        public Object sequenceKeyConversion(final Long identifier)
        {
            return new Byte(identifier.byteValue());
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            byte temp = stmt.getByte(columnName);
//            return (stmt.wasNull() ? null : new Byte(temp));
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            byte temp = stmt.getByte(columnIndex);
            return (stmt.wasNull() ? null : new Byte(temp));
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            byte temp = rs.getByte(columnName);
            return (rs.wasNull() ? null : new Byte(temp));
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            byte temp = rs.getByte(columnIndex);
            return (rs.wasNull() ? null : new Byte(temp));
        }

        public int getType()
        {
            return Types.TINYINT;
        }
    }

    public static final class T_SmallInt extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return new Short(identifier.shortValue());
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            short temp = stmt.getShort(columnName);
//            return (stmt.wasNull() ? null : new Short(temp));
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            short temp = stmt.getShort(columnIndex);
            return (stmt.wasNull() ? null : new Short(temp));
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            short temp = rs.getShort(columnName);
            return (rs.wasNull() ? null : new Short(temp));
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            short temp = rs.getShort(columnIndex);
            return (rs.wasNull() ? null : new Short(temp));
        }

        public int getType()
        {
            return Types.SMALLINT;
        }
    }

    public static final class T_Integer extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return new Integer(identifier.intValue());
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            int temp = stmt.getInt(columnName);
//            return (stmt.wasNull() ? null : new Integer(temp));
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            int temp = stmt.getInt(columnIndex);
            return (stmt.wasNull() ? null : new Integer(temp));
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            int temp = rs.getInt(columnName);
            return (rs.wasNull() ? null : new Integer(temp));
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            int temp = rs.getInt(columnIndex);
            return (rs.wasNull() ? null : new Integer(temp));
        }

        public int getType()
        {
            return Types.INTEGER;
        }
    }

    public static final class T_BigInt extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return identifier;
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            long temp = stmt.getLong(columnName);
//            return (stmt.wasNull() ? null : new Long(temp));
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            long temp = stmt.getLong(columnIndex);
            return (stmt.wasNull() ? null : new Long(temp));
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            long temp = rs.getLong(columnName);
            return (rs.wasNull() ? null : new Long(temp));
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            long temp = rs.getLong(columnIndex);
            return (rs.wasNull() ? null : new Long(temp));
        }

        public int getType()
        {
            return Types.BIGINT;
        }
    }

    public static final class T_Real extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return new Float(identifier.floatValue());
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            float temp = stmt.getFloat(columnName);
//            return (stmt.wasNull() ? null : new Float(temp));
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            float temp = stmt.getFloat(columnIndex);
            return (stmt.wasNull() ? null : new Float(temp));
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            float temp = rs.getFloat(columnName);
            return (rs.wasNull() ? null : new Float(temp));
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            float temp = rs.getFloat(columnIndex);
            return (rs.wasNull() ? null : new Float(temp));
        }

        public int getType()
        {
            return Types.REAL;
        }
    }

    public static final class T_Float extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return new Double(identifier.doubleValue());
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            double temp = stmt.getDouble(columnName);
//            return (stmt.wasNull() ? null : new Double(temp));
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            double temp = stmt.getDouble(columnIndex);
            return (stmt.wasNull() ? null : new Double(temp));
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            double temp = rs.getDouble(columnName);
            return (rs.wasNull() ? null : new Double(temp));
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            double temp = rs.getDouble(columnIndex);
            return (rs.wasNull() ? null : new Double(temp));
        }

        public int getType()
        {
            return Types.FLOAT;
        }
    }

    public static final class T_Double extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return new Double(identifier.doubleValue());
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            double temp = stmt.getDouble(columnName);
//            return (stmt.wasNull() ? null : new Double(temp));
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            double temp = stmt.getDouble(columnIndex);
            return (stmt.wasNull() ? null : new Double(temp));
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            double temp = rs.getDouble(columnName);
            return (rs.wasNull() ? null : new Double(temp));
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            double temp = rs.getDouble(columnIndex);
            return (rs.wasNull() ? null : new Double(temp));
        }

        public int getType()
        {
            return Types.DOUBLE;
        }
    }

    public static final class T_Binary extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return identifier.toString().getBytes();
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getBytes(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getBytes(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return rs.getBytes(columnName);
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return rs.getBytes(columnIndex);
        }

        public int getType()
        {
            return Types.BINARY;
        }
    }

    public static final class T_VarBinary extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return identifier.toString().getBytes();
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getBytes(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getBytes(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return rs.getBytes(columnName);
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return rs.getBytes(columnIndex);
        }

        public int getType()
        {
            return Types.VARBINARY;
        }
    }

    public static final class T_LongVarBinary extends BaseType
    {
        protected static final int BUFSZ = 2048;

        /**
         * Retrieve LONGVARBINARY InputStream data and pack into a byte array.
         *
         * @param is the input stream to be retrieved
         * @return a string containing the clob data
         * @throws java.sql.SQLException if conversion fails or the clob cannot be read
         */
        protected static byte[] retrieveStreamDataFromRs(InputStream is) throws SQLException
        {
            if (is == null)
            {
                return null;
            }
            byte[] bytes = null;
            ByteArrayOutputStream bos = null;
            try
            {
                bos = new ByteArrayOutputStream();
                int numRead;
                byte[] buf = new byte[BUFSZ];
                while ((numRead = is.read(buf, 0, buf.length)) > 0)
                {
                    bos.write(buf, 0, numRead);
                }
                bytes = bos.toByteArray();
            }
            catch (IOException e)
            {
                throw new SQLException("I/O exception retrieving LONGVARBINARY: " + e.getLocalizedMessage());
            }
            finally
            {
                if (bos != null)
                {
                    try
                    {
                        bos.close();
                    }
                    catch (Exception ignored)
                    {
                        //ignore
                    }
                }
            }
            return bytes;
        }

        public Object sequenceKeyConversion(Long identifier)
        {
            return identifier.toString().getBytes();
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getBytes(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getBytes(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return retrieveStreamDataFromRs(rs.getBinaryStream(columnName));
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return retrieveStreamDataFromRs(rs.getBinaryStream(columnIndex));
        }

        public int getType()
        {
            return Types.LONGVARBINARY;
        }
    }

    public static final class T_Date extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return new Date(identifier.longValue());
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getDate(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getDate(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return rs.getDate(columnName);
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return rs.getDate(columnIndex);
        }

        public int getType()
        {
            return Types.DATE;
        }
    }

    public static final class T_Time extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return new Time(identifier.longValue());
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getTime(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getTime(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return rs.getTime(columnName);
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return rs.getTime(columnIndex);
        }

        public int getType()
        {
            return Types.TIME;
        }
    }

    public static final class T_Timestamp extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier)
        {
            return new Timestamp(identifier.longValue());
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getTimestamp(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getTimestamp(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return rs.getTimestamp(columnName);
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return rs.getTimestamp(columnIndex);
        }

        public int getType()
        {
            return Types.TIMESTAMP;
        }
    }

    public static final class T_Clob extends BaseType
    {
        protected static final int BUFSZ = 32768;

        /**
         * Convert CLOB to String. Safe for very large objects.
         *
         * @param aClob clob with character data
         * @return a string containing the clob data
         * @throws java.sql.SQLException if conversion fails or the clob cannot be read
         */
        protected static String safeClobToString(Clob aClob) throws SQLException
        {
            long length = aClob.length();
            if (length == 0)
            {
                return "";
            }
            StringBuffer sb = new StringBuffer();
            char[] buf = new char[BUFSZ];
            java.io.Reader stream = aClob.getCharacterStream();
            try
            {
                int numRead;
                while ((numRead = stream.read(buf)) != -1)
                {
                    sb.append(buf, 0, numRead);
                }
                stream.close();
            }
            catch (IOException e)
            {
                throw new SQLException(e.getLocalizedMessage());
            }
            return sb.toString();
        }

        public Object sequenceKeyConversion(Long identifier) throws SequenceManagerException
        {
            throw new SequenceManagerException("Not supported sequence key type 'CLOB'");
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            Clob aClob = stmt.getClob(columnName);
//            return (stmt.wasNull() ? null : aClob.getSubString(1L, (int) aClob.length()));
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            Clob aClob = stmt.getClob(columnIndex);
            return (stmt.wasNull() ? null : safeClobToString(aClob));
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            Clob aClob = rs.getClob(columnName);
            return (rs.wasNull() ? null : safeClobToString(aClob));
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            Clob aClob = rs.getClob(columnIndex);
            return (rs.wasNull() ? null : safeClobToString(aClob));
        }

        public int getType()
        {
            return Types.CLOB;
        }
    }

    public static final class T_Blob extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier) throws SequenceManagerException
        {
            throw new SequenceManagerException("Not supported sequence key type 'BLOB'");
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            Blob aBlob = stmt.getBlob(columnName);
//            return (stmt.wasNull() ? null : aBlob.getBytes(1L, (int) aBlob.length()));
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            Blob aBlob = stmt.getBlob(columnIndex);
            return (stmt.wasNull() ? null : aBlob.getBytes(1L, (int) aBlob.length()));
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            Blob aBlob = rs.getBlob(columnName);
            return (rs.wasNull() ? null : aBlob.getBytes(1L, (int) aBlob.length()));
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            Blob aBlob = rs.getBlob(columnIndex);
            return (rs.wasNull() ? null : aBlob.getBytes(1L, (int) aBlob.length()));
        }

        public int getType()
        {
            return Types.BLOB;
        }
    }

    public static final class T_Array extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier) throws SequenceManagerException
        {
            throw new SequenceManagerException("Not supported sequence key type 'ARRAY'");
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getArray(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getArray(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return rs.getArray(columnName);
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return rs.getArray(columnIndex);
        }

        public int getType()
        {
            return Types.ARRAY;
        }
    }

    public static final class T_Struct extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier) throws SequenceManagerException
        {
            throw new SequenceManagerException("Not supported sequence key type 'STRUCT'");
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getObject(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getObject(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return rs.getObject(columnName);
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return rs.getObject(columnIndex);
        }

        public int getType()
        {
            return Types.STRUCT;
        }
    }

    public static final class T_Ref extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier) throws SequenceManagerException
        {
            throw new SequenceManagerException("Not supported sequence key type 'REF'");
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getRef(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getRef(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return rs.getRef(columnName);
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return rs.getRef(columnIndex);
        }

        public int getType()
        {
            return Types.REF;
        }
    }

//#ifdef JDBC30
    public static final class T_Datalink extends BaseType
    {
        public Object sequenceKeyConversion(Long identifier) throws SequenceManagerException
        {
            throw new SequenceManagerException("Not supported sequence key type 'DATALINK'");
        }

//        Object readValueFromStatement(CallableStatement stmt, String columnName) throws SQLException
//        {
//            return stmt.getURL(columnName);
//        }

        Object readValueFromStatement(CallableStatement stmt, int columnIndex) throws SQLException
        {
            return stmt.getURL(columnIndex);
        }

        Object readValueFromResultSet(ResultSet rs, String columnName) throws SQLException
        {
            return rs.getURL(columnName);
        }

        Object readValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException
        {
            return rs.getURL(columnIndex);
        }

        public int getType()
        {
            return Types.DATALINK;
        }
    }
//#endif
}
