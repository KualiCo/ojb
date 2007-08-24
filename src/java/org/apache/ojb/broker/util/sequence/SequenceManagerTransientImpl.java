package org.apache.ojb.broker.util.sequence;

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

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.metadata.FieldDescriptor;

/**
 * For internal use only!
 * This class is used to create transient primary key values for transient 
 * {@link org.apache.ojb.broker.Identity} objects.
 *
 * @version $Id: SequenceManagerTransientImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class SequenceManagerTransientImpl extends AbstractSequenceManager
{
    /*
     Use keyword 'volatile' to make decrement of a long value an
     atomic operation
     */
    private static volatile long tempKey = 1000;

    public SequenceManagerTransientImpl(PersistenceBroker broker)
    {
        super(broker);
    }

    protected long getUniqueLong(FieldDescriptor field) throws SequenceManagerException
    {
        /*
        arminw:
        We need unique 'dummy keys' for new objects before storing.
        Variable 'tempKey' is declared volatile, thus increment should be atomic
        */
        return ++tempKey;
    }
    
	/*
	 * abyrne: KULRNE-4545 when the maintenance framework triggers a refresh
	 * of references on a new bank account, and ojb goes to retrieve the bank, it uses this class
	 * to initialize any null keys on bank account with a unique value.  however, after setting
	 * the bank account to a number > 1000, it calls the sqlToJava method on the field converter.
	 * bank account number is an encrypted field, and the field converter complains because the value passed in
	 * is not encrypted.  so, we removing the field conversion call, which should not impact uniqueness
	 * or anything else since the unique long did not come from the db in this case
	 */
    /**
     * Returns a unique object for the given field attribute.
     * The returned value takes in account the jdbc-type
     * and the FieldConversion.sql2java() conversion defined for <code>field</code>.
     * The returned object is unique accross all tables in the extent
     * of class the field belongs to.
     */
    public Object getUniqueValue(FieldDescriptor field) throws SequenceManagerException
    {
        return field.getJdbcType().sequenceKeyConversion(new Long(getUniqueLong(field)));
        // perform a sql to java conversion here, so that clients do
        // not see any db specific values
        // abyrne commented out: result = field.getFieldConversion().sqlToJava(result);
        // abyrne commented out: return result;
    }
}
