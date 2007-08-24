package xdoclet.modules.ojb.model;

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
 * Field descriptor for the ojb repository file.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class FieldDescriptorDef extends FeatureDescriptorDef
{
    /** Whether this is an anonymous field */
    private boolean _isAnonymous = false;

    /**
     * Creates a new field descriptor object.
     *
     * @param name  The name of the field
     */
    public FieldDescriptorDef(String name)
    {
        super(name);
    }

    /**
     * Creates copy of the given field descriptor object. Note that the copy has no owner initially.
     *
     * @param src    The original field
     * @param prefix A prefix for the name
     */
    public FieldDescriptorDef(FieldDescriptorDef src, String prefix)
    {
        super(src, prefix);
        _isAnonymous = src._isAnonymous;
    }

    /**
     * Declares this field to be anonymous.
     */
    public void setAnonymous()
    {
        _isAnonymous = true;
    }
    
    /**
     * Returns whether this field is anonymous.
     * 
     * @return <code>true</code> if it is anonymous
     */
    public boolean isAnonymous()
    {
        return _isAnonymous;
    }

    /**
     * Determines the size constraint (length or precision+scale).
     * 
     * @return The size constraint
     */
    public String getSizeConstraint()
    {
        String constraint = getProperty(PropertyHelper.OJB_PROPERTY_LENGTH);

        if ((constraint == null) || (constraint.length() == 0))
        {
            String precision = getProperty(PropertyHelper.OJB_PROPERTY_PRECISION);
            String scale     = getProperty(PropertyHelper.OJB_PROPERTY_SCALE);

            if ((precision == null) || (precision.length() == 0))
            {
                precision = getProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_PRECISION);
            }
            if ((scale == null) || (scale.length() == 0))
            {
                scale = getProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_SCALE);
            }
            if (((precision != null) && (precision.length() > 0)) ||
                ((scale     != null) && (scale.length()     > 0)))
            {
                if ((precision == null) || (precision.length() == 0))
                {
                    precision = "1";
                }
                if ((scale == null) || (scale.length() == 0))
                {
                    scale = "0";
                }
                constraint = precision + "," + scale;
            }
        }
        return constraint;
    }
}
