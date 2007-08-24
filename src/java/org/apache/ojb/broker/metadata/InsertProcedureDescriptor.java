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
import java.io.Serializable;
import java.util.Iterator;

/**
 * An InsertProcedureDescriptor contains information that is related to the
 * procedure/function that is used to handle the insertion of new records.
 * <br>
 * Note: Be careful when use InsertProcedureDescriptor variables or caching
 * InsertProcedureDescriptor instances, because instances could become invalid
 * during runtime (see {@link MetadataManager}).
 *
 * @author <a href="mailto:rongallagher@bellsouth.net">Ron Gallagher<a>
 * @version $Id: InsertProcedureDescriptor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class InsertProcedureDescriptor
    extends ProcedureDescriptor
    implements Serializable, XmlCapable
{
	private static final long serialVersionUID = -3808311052971075269L;
    //---------------------------------------------------------------
    /**
     * The value that indicates if the argument list for this procedure
     * includes all field-descriptors from the related class-descriptor.
     */
    private boolean includeAllFields;

    //---------------------------------------------------------------
    /**
     * Constructor declaration
     */
    public InsertProcedureDescriptor(
        ClassDescriptor classDescriptor,
        String name,
        boolean includeAllFields)
    {
        super(classDescriptor, name);
        if (includeAllFields)
        {
            this.addArguments(this.getClassDescriptor().getFieldDescriptions());
        }
        this.includeAllFields = includeAllFields;
    }

    //---------------------------------------------------------------
    /**
     * Retrieve the value that indicates if the argument list for this
     * procedure includes all field-descriptors from the related
     * class-descriptor.
     *
     * @return The current value
     */
    public boolean getIncludeAllFields()
    {
        return this.includeAllFields;
    }

    //---------------------------------------------------------------
    /**
     * Add an argument
     * <p>
     * The argument will be added only if this procedure is not configured
     * to {@link #getIncludeAllFields() include all arguments}.
     */
    public final void addArgument(ArgumentDescriptor argument)
    {
        if (!this.getIncludeAllFields())
        {
            super.addArgument(argument);
        }
    }

    /*
     * @see XmlCapable#toXML()
     */
    public String toXML()
    {
        RepositoryTags tags = RepositoryTags.getInstance();
        String eol = System.getProperty( "line.separator" );

        // The result
        StringBuffer result = new StringBuffer( 1024 );
        result.append( eol );
        result.append( "   " );

        // Opening tag and attributes
        result.append( " " );
        result.append( tags.getOpeningTagNonClosingById( INSERT_PROCEDURE ) );
        result.append( " " );
        result.append( tags.getAttribute( NAME, this.getName() ) );
        if( this.hasReturnValue() )
        {
            result.append( " " );
            result.append( tags.getAttribute( RETURN_FIELD_REF, this.getReturnValueFieldRefName() ) );
        }
        result.append( " " );
        result.append( tags.getAttribute( INCLUDE_ALL_FIELDS, String.valueOf( this.getIncludeAllFields() ) ) );
        result.append( ">" );
        result.append( eol );

        // Write all arguments only if we're not including all fields.
        if( !this.getIncludeAllFields() )
        {
            Iterator args = this.getArguments().iterator();
            while( args.hasNext() )
            {
                result.append( ( ( ArgumentDescriptor ) args.next() ).toXML() );
            }
        }

        // Closing tag
        result.append( "    " );
        result.append( tags.getClosingTagById( INSERT_PROCEDURE ) );
        result.append( eol );
        return result.toString();
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
        buf.append("name", this.getName());
        buf.append("includeAllFields", this.getIncludeAllFields());
        if (this.hasReturnValue())
        {
            buf.append("returnFieldRefName", this.getReturnValueFieldRefName());
        }
        return buf.toString();
    }
}
