package xdoclet.modules.ojb.constraints;

import java.util.HashMap;

/* Copyright 2004-2005 The Apache Software Foundation
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

/**
 * Helper class for jdbc-type related things.
 * 
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class JdbcTypeHelper
{
    public final static String JDBC_DEFAULT_TYPE           = "LONGVARBINARY";
    public final static String JDBC_DEFAULT_TYPE_FOR_ARRAY = "LONGVARBINARY";
    public final static String JDBC_DEFAULT_CONVERSION     = "org.apache.ojb.broker.accesslayer.conversions.Object2ByteArrFieldConversion";

    /**
     * Contains the default java->jdbc mappings
     */
    private static HashMap _jdbcMappings = new HashMap();
    /**
     * Contains conversions for the default java->jdbc mappings
     */
    private static HashMap _jdbcConversions = new HashMap();
    /**
     * Contains default lengths for jdbc types
     */
    private static HashMap _jdbcLengths = new HashMap();
    /**
     * Contains default precisions for jdbc types
     */
    private static HashMap _jdbcPrecisions = new HashMap();
    /**
     * Contains default scales for jdbc types
     */
    private static HashMap _jdbcScales = new HashMap();

    static
    {
        // mappings
        _jdbcMappings.put("boolean",                         "BIT");
        _jdbcMappings.put("byte",                            "TINYINT");
        _jdbcMappings.put("short",                           "SMALLINT");
        _jdbcMappings.put("int",                             "INTEGER");
        _jdbcMappings.put("long",                            "BIGINT");
        _jdbcMappings.put("char",                            "CHAR");
        _jdbcMappings.put("float",                           "REAL");
        _jdbcMappings.put("double",                          "FLOAT");
        _jdbcMappings.put("java.lang.Boolean",               "BIT");
        _jdbcMappings.put("java.lang.Byte",                  "TINYINT");
        _jdbcMappings.put("java.lang.Short",                 "SMALLINT");
        _jdbcMappings.put("java.lang.Integer",               "INTEGER");
        _jdbcMappings.put("java.lang.Long",                  "BIGINT");
        _jdbcMappings.put("java.lang.Character",             "CHAR");
        _jdbcMappings.put("java.lang.Float",                 "REAL");
        _jdbcMappings.put("java.lang.Double",                "FLOAT");
        _jdbcMappings.put("java.lang.String",                "VARCHAR");
        _jdbcMappings.put("java.util.Date",                  "DATE");
        _jdbcMappings.put("java.sql.Blob",                   "BLOB");
        _jdbcMappings.put("java.sql.Clob",                   "CLOB");
        _jdbcMappings.put("java.sql.Date",                   "DATE");
        _jdbcMappings.put("java.sql.Time",                   "TIME");
        _jdbcMappings.put("java.sql.Timestamp",              "TIMESTAMP");
        _jdbcMappings.put("java.math.BigDecimal",            "DECIMAL");
        _jdbcMappings.put("org.apache.ojb.broker.util.GUID", "VARCHAR");

        // conversions
        _jdbcConversions.put("org.apache.ojb.broker.util.GUID",
                             "org.apache.ojb.broker.accesslayer.conversions.GUID2StringFieldConversion");

        // lengths
        _jdbcLengths.put("CHAR",    "1");
        _jdbcLengths.put("VARCHAR", "254");

        // precisions
        _jdbcPrecisions.put("DECIMAL", "20");
        _jdbcPrecisions.put("NUMERIC", "20");

        // scales
        _jdbcScales.put("DECIMAL", "0");
        _jdbcScales.put("NUMERIC", "0");
    }

    /**
     * Returns the default jdbc type for the given java type.
     * 
     * @param javaType The qualified java type
     * @return The default jdbc type
     */
    public static String getDefaultJdbcTypeFor(String javaType)
    {
        return _jdbcMappings.containsKey(javaType) ? (String)_jdbcMappings.get(javaType) : JDBC_DEFAULT_TYPE;
    }

    /**
     * Returns the default conversion for the given java type.
     * 
     * @param javaType The qualified java type
     * @return The default conversion or <code>null</code> if there is no default conversion for the type
     */
    public static String getDefaultConversionFor(String javaType)
    {
        return _jdbcConversions.containsKey(javaType) ? (String)_jdbcConversions.get(javaType) : null;
    }

    /**
     * Returns the default length for the given jdbc type.
     * 
     * @param jdbcType The jdbc type
     * @return The default length or <code>null</code> if there is none defined for this jdbc type
     */
    public static String getDefaultLengthFor(String jdbcType)
    {
        return (String)_jdbcLengths.get(jdbcType);
    }

    /**
     * Returns the default precision for the given jdbc type.
     * 
     * @param jdbcType The jdbc type
     * @return The default precision or <code>null</code> if there is none defined for this jdbc type
     */
    public static String getDefaultPrecisionFor(String jdbcType)
    {
        return (String)_jdbcPrecisions.get(jdbcType);
    }

    /**
     * Returns the default scale for the given jdbc type.
     * 
     * @param jdbcType The jdbc type
     * @return The default scale or <code>null</code> if there is none defined for this jdbc type
     */
    public static String getDefaultScaleFor(String jdbcType)
    {
        return (String)_jdbcScales.get(jdbcType);
    }
}
