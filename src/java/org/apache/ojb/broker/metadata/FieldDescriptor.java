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
import java.util.Comparator;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.metadata.fieldaccess.AnonymousPersistentField;
import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;
import org.apache.ojb.broker.accesslayer.conversions.FieldConversionDefaultImpl;
import org.apache.ojb.broker.util.ClassHelper;

/**
 * A FieldDescriptor holds the mapping information for a specific member-variable
 * of a persistent object.
 * <br>
 * Note: Be careful when use references of this class or caching instances of this class,
 * because instances could become invalid (see {@link MetadataManager}).
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: FieldDescriptor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class FieldDescriptor extends AttributeDescriptorBase implements XmlCapable, Serializable
{
	private static final long serialVersionUID = 7865777758296851949L;

    public static final String ACCESS_ANONYMOUS = RepositoryElements.TAG_ACCESS_ANONYMOUS;
    public static final String ACCESS_READONLY = RepositoryElements.TAG_ACCESS_READONLY;
    public static final String ACCESS_READWRITE = RepositoryElements.TAG_ACCESS_READWRITE;

    private int m_ColNo;
    private String m_ColumnName;
    private String m_ColumnType;
    private boolean m_IsKeyField = false;
    private boolean indexed = false;
    private boolean m_autoIncrement = false;
    private String m_sequenceName;
    private JdbcType m_jdbcType;

    private int length = 0;
    private int precision = 0;
    private int scale = 0;
    private boolean required = false;
    private boolean scaleSpecified = false;
    private boolean precisionSpecified = false;
    private boolean lengthSpecified = false;
    private FieldConversion fieldConversion = null;
    // true if field is used for optimistic locking BRJ
    private boolean m_locking = false;
    // if locking is true and updateLock is true then
    // on save lock columns will be updated.
    // if false then it is the responsibility of the
    // dbms to update all lock columns eg using triggers
    private boolean updateLock = true;
    private String m_access;

    /**
     * returns a comparator that allows to sort a Vector of FieldMappingDecriptors
     * according to their m_Order entries.
     */
    public static Comparator getComparator()
    {
        return new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                FieldDescriptor fmd1 = (FieldDescriptor) o1;
                FieldDescriptor fmd2 = (FieldDescriptor) o2;
                if (fmd1.getColNo() < fmd2.getColNo())
                {
                    return -1;
                }
                else if (fmd1.getColNo() > fmd2.getColNo())
                {
                    return 1;
                }
                else
                {
                    return 0;
                }
            }
        };
    }

    /**
     * Constructor declaration
     *
     * @param cld The parent {@link ClassDescriptor}
     * @param id A field id - unique against all other fields in the {@link ClassDescriptor}
     */
    public FieldDescriptor(ClassDescriptor cld, int id)
    {
        super(cld);
        m_ColNo = id;
    }

    /**
     *
     */
    public String getColumnName()
    {
        return m_ColumnName;
    }

    /**
     * Answer the qualified ColumnName<br>
     * ie: myTab.name
     *
     * @return
     */
    public String getFullColumnName()			// BRJ
    {
        return getClassDescriptor().getFullTableName() + "." + getColumnName();
    }

    public void setColumnName(String str)
    {
        m_ColumnName = str;
    }

    public String getColumnType()
    {
        return m_ColumnType;
    }

    public void setColumnType(String str)
    {
        m_ColumnType = str;
        m_jdbcType = lookupJdbcType();
    }

    /**
     * Returns the corresponding database {@link JdbcType}) of this field,
     * defined by the JDBC 3.0 specification, e.g. <em>VARCHAR</em>, <em>VARBINARY</em> ...
     * <p/>
     * The complement class is {@link FieldType}) which manage the java field
     * type, e.g. a <em>String</em>, <em>byte[]</em> ...
     *
     * Returns the mapped jdbc type of this field (see complement {@link FieldType}), defined by
     * the JDBC specification.
     * @return The jdbc database type of this field.
     */
    public JdbcType getJdbcType()
    {
        // check if jdbcType is assigned
        if(m_jdbcType == null)
        {
            m_jdbcType = lookupJdbcType();
        }
        return m_jdbcType;
    }

    /**
     * determines the JDBC type (represented as an int value as specified
     * by java.sql.Types) of a FIELDDESCRIPTOR.
     *
     * @return int the int value representing the Type according to
     * java.sql.Types.
     */
    private JdbcType lookupJdbcType()
    {
        JdbcType result = null;
        String columnType = getColumnType();
        // if sql type was not set in metadata we use reflection
        // to determine sql type by reflection
        if (columnType == null)
        {
            try
            {
                result = JdbcTypesHelper.getJdbcTypeByReflection(m_PersistentField.getType().getName());
            }
            catch(Exception e)
            {
                String eol = SystemUtils.LINE_SEPARATOR;
                throw new OJBRuntimeException("Can't automatically assign a jdbc field-type for field: "
                        + eol + this.toXML() + eol + "in class: " + eol + getClassDescriptor(), e);
            }
        }
        else
        {
            try
            {
                result = JdbcTypesHelper.getJdbcTypeByName(columnType);
            }
            catch(Exception e)
            {
                String eol = SystemUtils.LINE_SEPARATOR;
                throw new OJBRuntimeException("Can't assign the specified jdbc field-type '"+columnType+"' for field: "
                        + eol + this.toXML() + eol + "in class: " + eol + getClassDescriptor(), e);
            }
        }
        return result;
    }

    /**
     * Returns a string representation of this class.
     */
    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE);
        buf.append("columnName", m_ColumnName);
        buf.append("columnType", m_ColumnType);
        buf.append("isPrimaryKey", m_IsKeyField);
        buf.append("isLocking", m_locking);
        buf.append("isAutoincrement", m_autoIncrement);
        buf.append("access", m_access);
        buf.append("sequenceName", m_sequenceName);
        buf.append("jdbcType", m_jdbcType);
        buf.append("super_class_fields ", "=> " + super.toString());
        buf.append(SystemUtils.LINE_SEPARATOR);
        return buf.toString();
    }

    /**
     * Gets the fieldConversion.
     * @return Returns a FieldConversion
     */
    public FieldConversion getFieldConversion()
    {
        // if no conversion is specified use the default conversion
        if (fieldConversion == null)
        {
            fieldConversion = new FieldConversionDefaultImpl();
        }
        return fieldConversion;
    }

    /**
     * Sets the fieldConversion.
     * @param fieldConversion The fieldConversion to set
     * @deprecated use setFieldConversionClassName instead
     */
    public void setFieldConversion(FieldConversion fieldConversion)
    {
        this.fieldConversion = fieldConversion;
    }

    /**
     * Sets the fieldConversion.
     * @param fieldConversionClassName The fieldConversion to set
     */
    public void setFieldConversionClassName(String fieldConversionClassName)
    {
        try
        {
            this.fieldConversion = (FieldConversion) ClassHelper.newInstance(fieldConversionClassName);
        }
        catch (Exception e)
        {
            throw new MetadataException(
                    "Could not instantiate FieldConversion class using default constructor", e);
        }
    }

    public boolean isIndexed()
    {
        return indexed;
    }

    public void setIndexed(boolean indexed)
    {
        this.indexed = indexed;
    }

    public boolean isAutoIncrement()
    {
        return m_autoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement)
    {
        m_autoIncrement = autoIncrement;
    }

    public String getSequenceName()
    {
        return m_sequenceName;
    }

    public void setSequenceName(String sequenceName)
    {
        m_sequenceName = sequenceName;
    }

    public boolean isPrimaryKey()
    {
        return m_IsKeyField;
    }

    public void setPrimaryKey(boolean b)
    {
        m_IsKeyField = b;
    }

    public int getColNo()
    {
        return m_ColNo;
    }

    /**
     * Gets the locking.
     * @return Returns a boolean
     */
    public boolean isLocking()
    {
        return m_locking;
    }

    /**
     * Sets the locking.
     * @param locking The locking to set
     */
    public void setLocking(boolean locking)
    {
        m_locking = locking;
    }

    /**
     * Gets the updateLock
     * updateLock controls whether the lock fields should be
     * updated by OJB when a row is saved
     * If false then the dbms needs to update the lock fields.
     * The default is true
     * @return Returns a boolean
     */
    public boolean isUpdateLock()
    {
        return updateLock;
    }

    /**
     * Sets the updateLock
     * updateLock controls whether the lock fields should be
     * updated by OJB when a row is saved.
     * If false then the dbms needs to update the lock fields.
     * The default is true
     * @param updateLock The updateLock to set
     */
    public void setUpdateLock(boolean updateLock)
    {
        this.updateLock = updateLock;
    }

    public void setLength(int length)
    {
        this.length = length;
    }

    public int getLength()
    {
        return this.length;
    }

    public void setPrecision(int precision)
    {
        this.precision = precision;
    }

    public int getPrecision()
    {
        return this.precision;
    }

    public void setScale(int scale)
    {
        this.scale = scale;
    }

    public int getScale()
    {
        return this.scale;
    }

    public boolean isRequired()
    {
        return required;
    }

    public void setRequired(boolean required)
    {
        this.required = required;
    }

    public boolean isScaleSpecified()
    {
        return scaleSpecified;
    }

    public void setScaleSpecified(boolean scaleSpecified)
    {
        this.scaleSpecified = scaleSpecified;
    }

    public boolean isPrecisionSpecified()
    {
        return precisionSpecified;
    }

    public void setPrecisionSpecified(boolean precisionSpecified)
    {
        this.precisionSpecified = precisionSpecified;
    }

    public boolean isLengthSpecified()
    {
        return lengthSpecified;
    }

    public void setLengthSpecified(boolean lengthSpecified)
    {
        this.lengthSpecified = lengthSpecified;
    }

    public String getAccess()
    {
        return m_access;
    }

    public void setAccess(String access)
    {
        if (access == null)
        {
            access = ACCESS_READWRITE;
        }

        if (ACCESS_ANONYMOUS.equals(access) ||
                ACCESS_READONLY.equals(access) ||
                ACCESS_READWRITE.equals(access))
        {
            m_access = access;
        }
        else
        {
            throw new OJBRuntimeException("Try to set unkown field 'access' value: " + access);
        }
    }

    public boolean isAccessReadOnly()
    {
        return ACCESS_READONLY.equals(getAccess());
    }

    /**
     * Returns <em>true</em> if this field is declared as anonymous field.
     */
    public boolean isAnonymous()
    {
        return AnonymousPersistentField.class.isAssignableFrom(getPersistentField().getClass()) ? true : false;
    }

    /*
     * @see XmlCapable#toXML()
     */
    public String toXML()
    {
        RepositoryTags tags = RepositoryTags.getInstance();
        String eol = SystemUtils.LINE_SEPARATOR;

        //opening tag + attributes
        StringBuffer result = new StringBuffer( 1024 );
        result.append( "      " );
        result.append( tags.getOpeningTagNonClosingById( FIELD_DESCRIPTOR ) );
        result.append( " " );
        result.append( eol );

        //        // id
        //        String id = new Integer(getColNo()).toString();
        //        result += /*"        " +*/ tags.getAttribute(ID, id) + eol;

        // name
        result.append( "        " );
        result.append( tags.getAttribute( FIELD_NAME, this.getAttributeName() ) );
        result.append( eol );

        // table not yet implemented

        // column
        result.append( "        " );
        result.append( tags.getAttribute( COLUMN_NAME, this.getColumnName() ) );
        result.append( eol );

        // jdbc-type
        result.append( "        " );
        result.append( tags.getAttribute( JDBC_TYPE, this.getColumnType() ) );
        result.append( eol );

        // primarykey
        if( this.isPrimaryKey() )
        {
            result.append( "        " );
            result.append( tags.getAttribute( PRIMARY_KEY, "true" ) );
            result.append( eol );
        }

        // nullable
        if( this.isRequired() )
        {
            result.append( "        " );
            result.append( tags.getAttribute( NULLABLE, "false" ) );
            result.append( eol );
        }

        // indexed not yet implemented

        // autoincrement
        if( this.isAutoIncrement() )
        {
            result.append( "        " );
            result.append( tags.getAttribute( AUTO_INCREMENT, "true" ) );
            result.append( eol );
        }

        // locking
        if( this.isLocking() )
        {
            result.append( "        " );
            result.append( tags.getAttribute( LOCKING, "true" ) );
            result.append( eol );
        }

        // updateLock
        // default is true so only write if false
        if( !this.isUpdateLock() )
        {
            result.append( "        " );
            result.append( tags.getAttribute( UPDATE_LOCK, "false" ) );
            result.append( eol );
        }

        // default-fetch not yet implemented

        // conversion
        if( this.getFieldConversion().getClass() != FieldConversionDefaultImpl.class )
        {
            result.append( "        " );
            result.append( tags.getAttribute( FIELD_CONVERSION, getFieldConversion().getClass().getName() ) );
            result.append( eol );
        }

        // length
        if( this.isLengthSpecified() )
        {
            result.append( "        " );
            result.append( tags.getAttribute( LENGTH, "" + getLength() ) );
            result.append( eol );
        }

        // precision
        if( this.isPrecisionSpecified() )
        {
            result.append( "        " );
            result.append( tags.getAttribute( PRECISION, "" + getPrecision() ) );
            result.append( eol );
        }

        // scale
        if( this.isScaleSpecified() )
        {
            result.append( "        " );
            result.append( tags.getAttribute( SCALE, "" + getScale() ) );
            result.append( eol );
        }

        // access
        result.append( "        " );
        result.append( tags.getAttribute( ACCESS, this.getAccess() ) );
        result.append( eol );

        result.append( "      />" );
        result.append( eol );
        return result.toString();
    }

    public Object clone()
    {
        return SerializationUtils.deserialize(SerializationUtils.serialize(this));
    }
}
