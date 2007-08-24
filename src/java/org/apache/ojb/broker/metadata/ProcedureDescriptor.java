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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * A ProcedureDescriptor contains information that is common for all types
 * of procedures/functions that are used to handle the persistence operations.
 * <br>
 * Note: Be careful when use ProcedureDescriptor variables or caching
 * ProcedureDescriptor instances, because instances could become invalid
 * during runtime (see {@link MetadataManager}).
 *
 * @author <a href="mailto:rongallagher@bellsouth.net">Ron Gallagher<a>
 * @version $Id: ProcedureDescriptor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public abstract class ProcedureDescriptor extends DescriptorBase implements Serializable
{
	private static final long serialVersionUID = -8228331122289787173L;
    //---------------------------------------------------------------
    /**
     * The the name of the procedure/function to invoke.
     */
    private String name;

    //---------------------------------------------------------------
    /**
     * The the field descriptor that will receive the return value from the procedure/function...
     */
    private FieldDescriptor returnValueFieldRef;

    //---------------------------------------------------------------
    /**
     * The class descriptor that this object is related to.
     */
    private ClassDescriptor classDescriptor;

    //---------------------------------------------------------------
    /**
     * The argument descriptor lists.
     */
    private ArrayList arguments = new ArrayList();

    //---------------------------------------------------------------
    /**
     * Constructor declaration
     */
    public ProcedureDescriptor(ClassDescriptor classDescriptor, String name)
    {
        this.classDescriptor = classDescriptor;
        this.name = name;
    }

    //---------------------------------------------------------------
    /**
     * Retrieve the the name of the procedure/function to invoke.
     *
     * @return The current value
     */
    public final String getName()
    {
        return this.name;
    }

    //---------------------------------------------------------------
    /**
     * Change the field descriptor that will receive the return value
     * from the procedure/function..
     *
     * @param fieldName the name of the field that will receive the
     * return value from the procedure/function.
     */
    public final void setReturnValueFieldRef(String fieldName)
    {
        this.returnValueFieldRef = this.getClassDescriptor().getFieldDescriptorByName(fieldName);
    }

    //---------------------------------------------------------------
    /**
     * Change the the field descriptor that will receive the return
     * value from the procedure/function...
     *
     * @param fieldDescriptor the field descriptor that will receive the
     * return value from the procedure/function.
     */
    public final void setReturnValueFieldRef(FieldDescriptor fieldDescriptor)
    {
        this.returnValueFieldRef = fieldDescriptor;
    }

    //---------------------------------------------------------------
    /**
     * Retrieve the field descriptor that will receive the return value
     * from the procedure/function...
     *
     * @return The current value
     */
    public final FieldDescriptor getReturnValueFieldRef()
    {
        return this.returnValueFieldRef;
    }

    //---------------------------------------------------------------
    /**
     * Is there a return value for this procedure?
     *
     * @return <code>true</code> if there is a return value for this
     * procedure.
     */
    public final boolean hasReturnValue()
    {
        return (this.returnValueFieldRef != null);
    }

    //---------------------------------------------------------------
    /**
     * Does this procedure return any values to the 'caller'?
     *
     * @return <code>true</code> if the procedure returns at least 1
     * value that is returned to the caller.
     */
    public final boolean hasReturnValues()
    {
        if (this.hasReturnValue())
        {
            return true;
        }
        else
        {
            // TODO: We may be able to 'pre-calculate' the results
            // of this loop by just checking arguments as they are added
            // The only problem is that the 'isReturnedbyProcedure' property
            // can be modified once the argument is added to this procedure.
            // If that occurs, then 'pre-calculated' results will be inacccurate.
            Iterator iter = this.getArguments().iterator();
            while (iter.hasNext())
            {
                ArgumentDescriptor arg = (ArgumentDescriptor) iter.next();
                if (arg.getIsReturnedByProcedure())
                {
                    return true;
                }
            }
        }
        return false;
    }

    //---------------------------------------------------------------
    /**
     * Retrieve the name of the field descriptor that will receive the
     * return value from the procedure/function...
     *
     * @return The current value
     */
    public final String getReturnValueFieldRefName()
    {
        if (this.returnValueFieldRef == null)
        {
            return null;
        }
        else
        {
            return this.returnValueFieldRef.getAttributeName();
        }
    }

    //---------------------------------------------------------------
    /**
     * Retrieve the class descriptor that this object is related to.
     *
     * @return The current value
     */
    public final ClassDescriptor getClassDescriptor()
    {
        return this.classDescriptor;
    }

    /*
     * @see XmlCapable#toXML()
     */
    public abstract String toXML();

    //---------------------------------------------------------------
    /**
     * Add an argument
     */
    protected void addArgument(ArgumentDescriptor argument)
    {
        this.arguments.add(argument);
    }

    //---------------------------------------------------------------
    /**
     * Set up arguments for each FieldDescriptor in an array.
     */
    protected void addArguments(FieldDescriptor field[])
    {
        for (int i = 0; i < field.length; i++)
        {
            ArgumentDescriptor arg = new ArgumentDescriptor(this);
            arg.setValue(field[i].getAttributeName(), false);
            this.addArgument(arg);
        }
    }

    //---------------------------------------------------------------
    /**
     * Get the argument descriptors for this procedure.
     */
    public final Collection getArguments()
    {
        return this.arguments;
    }

    //---------------------------------------------------------------
    /**
     * Retrieves the number of arguments that are passed to the
     * procedure that this descriptor represents.
     * <p>
     * Note: The value returned by this method does not reflect
     * the presence of any return value for the procedure
     */
    public final int getArgumentCount()
    {
        return this.arguments.size();
    }
}
