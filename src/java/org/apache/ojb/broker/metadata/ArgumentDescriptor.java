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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;
import java.io.Serializable;

/**
 * An ArgumentDescriptor contains information that defines a single argument
 * that is passed to a procedure/function.
 * <br>
 * Note: Be careful when use ArgumentDescriptor variables or caching
 * ArgumentDescriptor instances, because instances could become invalid
 * during runtime (see {@link MetadataManager}).
 *
 * @author <a href="mailto:rongallagher@bellsouth.net">Ron Gallagher<a>
 * @version $Id: ArgumentDescriptor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public final class ArgumentDescriptor extends DescriptorBase implements XmlCapable, Serializable
{
	private static final long serialVersionUID = 5205304260023247711L;

    private static final int SOURCE_NULL = 0;
    private static final int SOURCE_FIELD = 1;
    private static final int SOURCE_VALUE = 2;
    private int fieldSource = SOURCE_NULL;
    private String constantValue = null;
    private String fieldRefName = null;
    private boolean returnedByProcedure = false;

    //---------------------------------------------------------------
    /**
     * The procedure descriptor that this object is related to.
     */
    private ProcedureDescriptor procedureDescriptor;

    //---------------------------------------------------------------
    /**
     * Constructor declaration.  By default, this object will be configured
     * so that the value returned by {@link #fieldSource} will be null.  To
     * change this, call either version of the setValue method.
     *
     * @see #setValue()
     * @see #setValue(String)
     * @see #setValue(String,boolean)
     */
    public ArgumentDescriptor(ProcedureDescriptor procedureDescriptor)
    {
        this.procedureDescriptor = procedureDescriptor;
        this.setValue();
    }

    /**
     * Sets up this object to represent a null value.
     */
    public void setValue()
    {
        this.fieldSource = SOURCE_NULL;
        this.fieldRefName = null;
        this.returnedByProcedure = false;
        this.constantValue = null;
    }

    /**
     * Sets up this object to represent a value that is derived from a field
     * in the corresponding class-descriptor.
     * <p>
     * If the value of <code>fieldRefName</code> is blank or refers to an
     * invalid field reference, then the value of the corresponding argument
     * will be set to null. In this case, {@link #getIsReturnedByProcedure}
     * will be set to <code>false</code>, regardless of the value of the
     * <code>returnedByProcedure</code> argument.
     *
     * @param fieldRefName the name of the field reference that provides the
     *          value of this argument.
     * @param returnedByProcedure indicates that the value of the argument
     *          is returned by the procedure that is invoked.
     */
    public void setValue(String fieldRefName, boolean returnedByProcedure)
    {
        this.fieldSource = SOURCE_FIELD;
        this.fieldRefName = fieldRefName;
        this.returnedByProcedure = returnedByProcedure;
        this.constantValue = null;

        // If the field reference is not valid, then disregard the value
        // of the returnedByProcedure argument.
        if (this.getFieldRef() == null)
        {
            this.returnedByProcedure = false;
        }

        // If the field reference is not valid, then disregard the value
        // of the returnedByProcedure argument.
        if (this.getFieldRef() == null)
        {
            this.returnedByProcedure = false;
        }
    }

    /**
     * Sets up this object to represent an argument that will be set to a
     * constant value.
     *
     * @param constantValue the constant value.
     */
    public void setValue(String constantValue)
    {
        this.fieldSource = SOURCE_VALUE;
        this.fieldRefName = null;
        this.returnedByProcedure = false;
        this.constantValue = constantValue;
    }

    public boolean getIsReturnedByProcedure()
    {
        return this.returnedByProcedure;
    }

    public Object getValue(Object objekt)
    {
        switch (this.fieldSource)
        {
            case SOURCE_FIELD :
                if (objekt == null)
                {
                    return null;
                }
                else
                {
                    FieldDescriptor fd = this.getFieldRef();
                    Object value = null;
                    FieldConversion conversion = null;
                    if (fd != null)
                    {
                        conversion = fd.getFieldConversion();
                        value = fd.getPersistentField().get(objekt);
                        if (conversion != null)
                        {
                            value = conversion.javaToSql(value);
                        }
                    }
                    return value;
                }
            case SOURCE_VALUE :
                return this.constantValue;
            case SOURCE_NULL :
                return null;
            default :
                return null;
        }
    }

    public void saveValue(Object objekt, Object value)
    {
        if ((this.fieldSource == SOURCE_FIELD) && (this.returnedByProcedure))
        {
            FieldDescriptor fd = this.getFieldRef();
            FieldConversion conversion = null;
            if (fd != null)
            {
                conversion = fd.getFieldConversion();
                if (conversion == null)
                {
                    fd.getPersistentField().set(objekt, value);
                }
                else
                {
                    fd.getPersistentField().set(objekt, conversion.sqlToJava(value));
                }
            }
        }
    }

    //---------------------------------------------------------------
    /**
     * Retrieve the field descriptor that this argument is related to.
     * <p>
     * This reference can only be set via the {@link #setValue(String,boolean)}
     * method.
     * @return The current value
     */
    public final FieldDescriptor getFieldRef()
    {
        if (this.fieldSource == SOURCE_FIELD)
        {
            return this.getProcedureDescriptor().getClassDescriptor().getFieldDescriptorByName(
                this.fieldRefName);
        }
        else
        {
            return null;
        }
    }

    /**
     * Retrieve the jdbc type for the field descriptor that is related
     * to this argument.
     */
    public final int getJdbcType()
    {
        switch (this.fieldSource)
        {
            case SOURCE_FIELD :
                return this.getFieldRef().getJdbcType().getType();
            case SOURCE_NULL :
                return java.sql.Types.NULL;
            case SOURCE_VALUE :
                return java.sql.Types.VARCHAR;
            default :
                return java.sql.Types.NULL;
        }
    }

    //---------------------------------------------------------------
    /**
     * Retrieve the procedure descriptor that this object is related to.
     *
     * @return The current value
     */
    public final ProcedureDescriptor getProcedureDescriptor()
    {
        return this.procedureDescriptor;
    }

    /*
     * @see XmlCapable#toXML()
     */
    public String toXML()
    {
        String eol = System.getProperty("line.separator");
        RepositoryTags tags = RepositoryTags.getInstance();

        // The result
        String result = "     ";

        switch (this.fieldSource)
        {
            case SOURCE_FIELD :
                result += " " + tags.getOpeningTagNonClosingById(RUNTIME_ARGUMENT);
                result += " " + tags.getAttribute(FIELD_REF, this.fieldRefName);
                result += " " + tags.getAttribute(RETURN, String.valueOf(this.returnedByProcedure));
                result += "/>";
                break;
            case SOURCE_VALUE :
                result += " " + tags.getOpeningTagNonClosingById(CONSTANT_ARGUMENT);
                result += " " + tags.getAttribute(VALUE, this.constantValue);
                result += "/>";
                break;
            case SOURCE_NULL :
                result += " " + tags.getOpeningTagNonClosingById(RUNTIME_ARGUMENT);
                result += "/>";
                break;
            default :
                break;
        }

        // Return the result.
        return (result + eol);
    }

    //---------------------------------------------------------------
    /**
     * Provide a string representation of this object
     *
     * @return a string representation of this object
     */
    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        switch (this.fieldSource)
        {
            case SOURCE_FIELD :
                {
                    buf.append("fieldRefName", this.fieldRefName);
                    buf.append("returnedByProcedure", this.returnedByProcedure);
                    break;
                }
            case SOURCE_NULL :
                {
                    break;
                }
            case SOURCE_VALUE :
                {
                    buf.append("constantValue", this.constantValue);
                    break;
                }
        }
        return buf.toString();
    }
}
