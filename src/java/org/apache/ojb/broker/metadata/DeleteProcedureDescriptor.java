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
 * An DeleteProcedureDescriptor contains information that is related to the
 * procedure/function that is used to handle the deleting of existing records.
 * <br>
 * Note: Be careful when use DeleteProcedureDescriptor variables or caching
 * DeleteProcedureDescriptor instances, because instances could become invalid
 * during runtime (see {@link MetadataManager}).
 *
 * @author <a href="mailto:rongallagher@bellsouth.net">Ron Gallagher<a>
 * @version $Id: DeleteProcedureDescriptor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class DeleteProcedureDescriptor extends ProcedureDescriptor
    implements Serializable, XmlCapable
{
    private static final long serialVersionUID = -1265854095889157172L;
    //---------------------------------------------------------------
    /**
     * The value that indicates if the argument list for this procedure
     * includes only the field-descriptors from the related class-descriptor
     * that are identified as being part of the primary key.
     */
    private boolean includePkFieldsOnly;

    //---------------------------------------------------------------
    
    /**
     * Constructor declaration
     */
    public DeleteProcedureDescriptor(ClassDescriptor classDescriptor, 
            String name, boolean includePkFieldsOnly)
    {
        super(classDescriptor, name);
        if (includePkFieldsOnly)
        {
            addArguments(getClassDescriptor().getPkFields());
            addArguments(getClassDescriptor().getLockingFields());
        }
        this.includePkFieldsOnly = includePkFieldsOnly;
    }

    //---------------------------------------------------------------
    /**
     * Retrieve the value that indicates if the argument list for this
     * procedure includes only the field-descriptors from the related
     * class-descriptor that are identified as being part of the primary
     * key.
     *
     * @return The current value
     */
    public boolean getIncludePkFieldsOnly()
    {
        return this.includePkFieldsOnly;
    }

    //---------------------------------------------------------------
    /**
     * Add an argument
     * <p>
     * The argument will be added only if this procedure is not configured
     * to {@link #getIncludePkFieldsOnly() include just the
     * primary key fields}.
     */
    public final void addArgument(ArgumentDescriptor argument)
    {
        if (!this.getIncludePkFieldsOnly())
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
        result.append( tags.getOpeningTagNonClosingById( DELETE_PROCEDURE ) );
        result.append( " " );
        result.append( tags.getAttribute( NAME, this.getName() ) );
        if( this.hasReturnValue() )
        {
            result.append( " " );
            result.append( tags.getAttribute( RETURN_FIELD_REF, this.getReturnValueFieldRefName() ) );
        }
        result.append( " " );
        result.append( tags.getAttribute( INCLUDE_PK_FIELDS_ONLY, String.valueOf( this.getIncludePkFieldsOnly() ) ) );
        result.append( ">" );
        result.append( eol );

        // Write all arguments only if we're not including all fields.
        if( !this.getIncludePkFieldsOnly() )
        {
            Iterator args = this.getArguments().iterator();
            while( args.hasNext() )
            {
                result.append( ( ( ArgumentDescriptor ) args.next() ).toXML() );
            }
        }

        // Closing tag
        result.append( "    " );
        result.append( tags.getClosingTagById( DELETE_PROCEDURE ) );
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
        buf.append("includePkFieldsOnly", this.getIncludePkFieldsOnly());
        if (this.hasReturnValue())
        {
            buf.append("returnFieldRefName", this.getReturnValueFieldRefName());
        }
        return buf.toString();
    }
}
