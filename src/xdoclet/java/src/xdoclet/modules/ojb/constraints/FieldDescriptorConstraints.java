package xdoclet.modules.ojb.constraints;

import java.util.HashMap;

import xdoclet.modules.ojb.LogHelper;
import xdoclet.modules.ojb.model.FieldDescriptorDef;
import xdoclet.modules.ojb.model.PropertyHelper;

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
 * Checks constraints for field descriptors. Note that constraints may modify the field descriptor.
 *
 * @author <a href="mailto:tomdz@apache.org">Thomas Dudziak</a>
 */
public class FieldDescriptorConstraints extends FeatureDescriptorConstraints
{
    /** The interface that conversion classes must implement */
    private final static String CONVERSION_INTERFACE = "org.apache.ojb.broker.accesslayer.conversions.FieldConversion";
    /** The allowed jdbc types */
    private HashMap _jdbcTypes = new HashMap();

    /**
     * Creates a new field descriptor constraints object.
     */
    public FieldDescriptorConstraints()
    {
        _jdbcTypes.put("BIT", null);
        _jdbcTypes.put("TINYINT", null);
        _jdbcTypes.put("SMALLINT", null);
        _jdbcTypes.put("INTEGER", null);
        _jdbcTypes.put("BIGINT", null);
        _jdbcTypes.put("DOUBLE", null);
        _jdbcTypes.put("FLOAT", null);
        _jdbcTypes.put("REAL", null);
        _jdbcTypes.put("NUMERIC", null);
        _jdbcTypes.put("DECIMAL", null);
        _jdbcTypes.put("CHAR", null);
        _jdbcTypes.put("VARCHAR", null);
        _jdbcTypes.put("LONGVARCHAR", null);
        _jdbcTypes.put("DATE", null);
        _jdbcTypes.put("TIME", null);
        _jdbcTypes.put("TIMESTAMP", null);
        _jdbcTypes.put("BINARY", null);
        _jdbcTypes.put("VARBINARY", null);
        _jdbcTypes.put("LONGVARBINARY", null);
        _jdbcTypes.put("CLOB", null);
        _jdbcTypes.put("BLOB", null);
        _jdbcTypes.put("STRUCT", null);
        _jdbcTypes.put("ARRAY", null);
        _jdbcTypes.put("REF", null);
        _jdbcTypes.put("BOOLEAN", null);
        _jdbcTypes.put("DATALINK", null);
    }

    /**
     * Checks the given field descriptor.
     * 
     * @param fieldDef   The field descriptor
     * @param checkLevel The amount of checks to perform
     * @exception ConstraintException If a constraint has been violated
     */
    public void check(FieldDescriptorDef fieldDef, String checkLevel) throws ConstraintException
    {
        ensureColumn(fieldDef, checkLevel);
        ensureJdbcType(fieldDef, checkLevel);
        ensureConversion(fieldDef, checkLevel);
        ensureLength(fieldDef, checkLevel);
        ensurePrecisionAndScale(fieldDef, checkLevel);
        checkLocking(fieldDef, checkLevel);
        checkSequenceName(fieldDef, checkLevel);
        checkId(fieldDef, checkLevel);
        if (fieldDef.isAnonymous())
        {
            checkAnonymous(fieldDef, checkLevel);
        }
        else
        {
            checkReadonlyAccessForNativePKs(fieldDef, checkLevel);
        }
    }

    /**
     * Constraint that ensures that the field has a column property. If none is specified, then
     * the name of the field is used.
     * 
     * @param fieldDef   The field descriptor
     * @param checkLevel The current check level (this constraint is checked in all levels)
     */
    private void ensureColumn(FieldDescriptorDef fieldDef, String checkLevel)
    {
        if (!fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_COLUMN))
        {
            String javaname = fieldDef.getName();

            if (fieldDef.isNested())
            {    
                int pos = javaname.indexOf("::");

                // we convert nested names ('_' for '::')
                if (pos > 0)
                {
                    StringBuffer newJavaname = new StringBuffer(javaname.substring(0, pos));
                    int          lastPos     = pos + 2;

                    do
                    {
                        pos = javaname.indexOf("::", lastPos);
                        newJavaname.append("_");
                        if (pos > 0)
                        {    
                            newJavaname.append(javaname.substring(lastPos, pos));
                            lastPos = pos + 2;
                        }
                        else
                        {
                            newJavaname.append(javaname.substring(lastPos));
                        }
                    }
                    while (pos > 0);
                    javaname = newJavaname.toString();
                }
            }
            fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_COLUMN, javaname);
        }
    }

    /**
     * Constraint that ensures that the field has a jdbc type. If none is specified, then
     * the default type is used (which has been determined when the field descriptor was added)
     * and - if necessary - the default conversion is set.
     * 
     * @param fieldDef   The field descriptor
     * @param checkLevel The current check level (this constraint is checked in all levels)
     * @exception ConstraintException If the constraint has been violated
     */
    private void ensureJdbcType(FieldDescriptorDef fieldDef, String checkLevel) throws ConstraintException
    {
        if (!fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE))
        {
            if (!fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_JDBC_TYPE))
            {
                throw new ConstraintException("No jdbc-type specified for the field "+fieldDef.getName()+" in class "+fieldDef.getOwner().getName());
            }

            fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE, fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_JDBC_TYPE));
            if (!fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_CONVERSION) && fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_CONVERSION))
            {
                fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_CONVERSION, fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_CONVERSION));
            }
        }
        else
        {
            // we could let XDoclet check the type for field declarations but not for modifications (as we could
            // not specify the empty string anymore)
            String jdbcType = fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE);

            if (!_jdbcTypes.containsKey(jdbcType))
            {
                throw new ConstraintException("The field "+fieldDef.getName()+" in class "+fieldDef.getOwner().getName()+" specifies the invalid jdbc type "+jdbcType);
            }
        }
    }

    /**
     * Constraint that ensures that the field has a conversion if the java type requires it. Also checks the conversion class.
     * 
     * @param fieldDef   The field descriptor
     * @param checkLevel The current check level (this constraint is checked in basic (partly) and strict)
     * @exception ConstraintException If the conversion class is invalid
     */
    private void ensureConversion(FieldDescriptorDef fieldDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        // we issue a warning if we encounter a field with a java.util.Date java type without a conversion
        if ("java.util.Date".equals(fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_JAVA_TYPE)) &&
            !fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_CONVERSION))
        {
            LogHelper.warn(true,
                           FieldDescriptorConstraints.class,
                           "ensureConversion",
                           "The field "+fieldDef.getName()+" in class "+fieldDef.getOwner().getName()+
                               " of type java.util.Date is directly mapped to jdbc-type "+
                               fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE)+
                               ". However, most JDBC drivers can't handle java.util.Date directly so you might want to "+
                               " use a conversion for converting it to a JDBC datatype like TIMESTAMP.");
        }

        String conversionClass = fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_CONVERSION);

        if (((conversionClass == null) || (conversionClass.length() == 0)) &&
            fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_CONVERSION) &&
            fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_JDBC_TYPE).equals(fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE)))
        {
            conversionClass = fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_CONVERSION);
            fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_CONVERSION, conversionClass);
        }
        // now checking
        if (CHECKLEVEL_STRICT.equals(checkLevel) && (conversionClass != null) && (conversionClass.length() > 0))
        {
            InheritanceHelper helper = new InheritanceHelper();

            try
            {
                if (!helper.isSameOrSubTypeOf(conversionClass, CONVERSION_INTERFACE))
                {
                    throw new ConstraintException("The conversion class specified for field "+fieldDef.getName()+" in class "+fieldDef.getOwner().getName()+" does not implement the necessary interface "+CONVERSION_INTERFACE);
                }
            }
            catch (ClassNotFoundException ex)
            {
                throw new ConstraintException("The class "+ex.getMessage()+" hasn't been found on the classpath while checking the conversion class specified for field "+fieldDef.getName()+" in class "+fieldDef.getOwner().getName());
            }
        }
}

    /**
     * Constraint that ensures that the field has a length if the jdbc type requires it.
     * 
     * @param fieldDef   The field descriptor
     * @param checkLevel The current check level (this constraint is checked in all levels)
     */
    private void ensureLength(FieldDescriptorDef fieldDef, String checkLevel)
    {
        if (!fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_LENGTH))
        {
            String defaultLength = JdbcTypeHelper.getDefaultLengthFor(fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE));

            if (defaultLength != null)
            {
                LogHelper.warn(true,
                               FieldDescriptorConstraints.class,
                               "ensureLength",
                               "The field "+fieldDef.getName()+" in class "+fieldDef.getOwner().getName()+" has no length setting though its jdbc type requires it (in most databases); using default length of "+defaultLength);
                fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_LENGTH, defaultLength);
            }
        }
    }

    /**
     * Constraint that ensures that the field has precision and scale settings if the jdbc type requires it.
     * 
     * @param fieldDef   The field descriptor
     * @param checkLevel The current check level (this constraint is checked in all levels)
     */
    private void ensurePrecisionAndScale(FieldDescriptorDef fieldDef, String checkLevel)
    {
        fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_PRECISION, null);
        fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_SCALE, null);
        if (!fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_PRECISION))
        {
            String defaultPrecision = JdbcTypeHelper.getDefaultPrecisionFor(fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE));

            if (defaultPrecision != null)
            {
                LogHelper.warn(true,
                               FieldDescriptorConstraints.class,
                               "ensureLength",
                               "The field "+fieldDef.getName()+" in class "+fieldDef.getOwner().getName()+" has no precision setting though its jdbc type requires it (in most databases); using default precision of "+defaultPrecision);
                fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_PRECISION, defaultPrecision);
            }
            else if (fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_SCALE))
            {
                fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_PRECISION, "1");
            }
        }
        if (!fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_SCALE))
         {
            String defaultScale = JdbcTypeHelper.getDefaultScaleFor(fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE));

            if (defaultScale != null)
            {
                LogHelper.warn(true,
                               FieldDescriptorConstraints.class,
                               "ensureLength",
                               "The field "+fieldDef.getName()+" in class "+fieldDef.getOwner().getName()+" has no scale setting though its jdbc type requires it (in most databases); using default scale of "+defaultScale);
                fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_SCALE, defaultScale);
            }
            else if (fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_PRECISION) || fieldDef.hasProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_PRECISION))
            {
                fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_SCALE, "0");
            }
        }
    }

    /**
     * Checks that locking and update-lock are only used for fields of TIMESTAMP or INTEGER type.
     * 
     * @param fieldDef The field descriptor
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     * @exception ConstraintException If the constraint has been violated
     */
    private void checkLocking(FieldDescriptorDef fieldDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        String jdbcType = fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_JDBC_TYPE);

        if (!"TIMESTAMP".equals(jdbcType) && !"INTEGER".equals(jdbcType))
        {
            if (fieldDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_LOCKING, false))
            {
                throw new ConstraintException("The field "+fieldDef.getName()+" in class "+fieldDef.getOwner().getName()+" has locking set to true though it is not of TIMESTAMP or INTEGER type");
            }
            if (fieldDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_UPDATE_LOCK, false))
            {
                throw new ConstraintException("The field "+fieldDef.getName()+" in class "+fieldDef.getOwner().getName()+" has update-lock set to true though it is not of TIMESTAMP or INTEGER type");
            }
        }
    }

    /**
     * Checks that sequence-name is only used with autoincrement='ojb'
     * 
     * @param fieldDef The field descriptor
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     * @exception ConstraintException If the constraint has been violated
     */
    private void checkSequenceName(FieldDescriptorDef fieldDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        String autoIncr = fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_AUTOINCREMENT);
        String seqName  = fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_SEQUENCE_NAME);

        if ((seqName != null) && (seqName.length() > 0))
        {
            if (!"ojb".equals(autoIncr) && !"database".equals(autoIncr))
            {
                throw new ConstraintException("The field "+fieldDef.getName()+" in class "+fieldDef.getOwner().getName()+" has sequence-name set though it's autoincrement value is not set to 'ojb'");
            }
        }
    }

    /**
     * Checks the id value.
     * 
     * @param fieldDef The field descriptor
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     * @exception ConstraintException If the constraint has been violated
     */
    private void checkId(FieldDescriptorDef fieldDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        String id = fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_ID);

        if ((id != null) && (id.length() > 0))
        {
            try
            {
                Integer.parseInt(id);
            }
            catch (NumberFormatException ex)
            {
                throw new ConstraintException("The id attribute of field "+fieldDef.getName()+" in class "+fieldDef.getOwner().getName()+" is not a valid number");
            }
        }
    }

    /**
     * Checks that native primarykey fields have readonly access, and warns if not.
     * 
     * @param fieldDef The field descriptor
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     */
    private void checkReadonlyAccessForNativePKs(FieldDescriptorDef fieldDef, String checkLevel)
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        String access  = fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_ACCESS);
        String autoInc = fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_AUTOINCREMENT);

        if ("database".equals(autoInc) && !"readonly".equals(access))
        {
            LogHelper.warn(true,
                           FieldDescriptorConstraints.class,
                           "checkAccess",
                           "The field "+fieldDef.getName()+" in class "+fieldDef.getOwner().getName()+" is set to database auto-increment. Therefore the field's access is set to 'readonly'.");
            fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_ACCESS, "readonly");
        }
    }

    /**
     * Checks anonymous fields.
     * 
     * @param fieldDef The field descriptor
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     * @exception ConstraintException If the constraint has been violated
     */
    private void checkAnonymous(FieldDescriptorDef fieldDef, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }

        String access = fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_ACCESS);

        if (!"anonymous".equals(access))
        {
            throw new ConstraintException("The access property of the field "+fieldDef.getName()+" defined in class "+fieldDef.getOwner().getName()+" cannot be changed");
        }

        if ((fieldDef.getName() == null) || (fieldDef.getName().length() == 0))
        {
            throw new ConstraintException("An anonymous field defined in class "+fieldDef.getOwner().getName()+" has no name");
        }
    }
}
