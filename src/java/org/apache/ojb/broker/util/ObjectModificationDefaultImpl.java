package org.apache.ojb.broker.util;

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


/**
 * Insert the type's description here.
 *
 * @deprecated Please use {@link ObjectModification#UPDATE} and {@link ObjectModification#INSERT} 
 * @author Thomas Mahler
 * @version $Id: ObjectModificationDefaultImpl.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class ObjectModificationDefaultImpl implements ObjectModification
{
    private boolean needsInsert = false;
    private boolean needsUpdate = false;

    /**
     * ObjectModificationImpl constructor comment.
     */
    public ObjectModificationDefaultImpl()
    {
        super();
    }

    /**
     * ObjectModificationImpl constructor comment.
     */
    public ObjectModificationDefaultImpl(boolean pNeedsInsert, boolean pNeedsUpdate)
    {
        needsInsert = pNeedsInsert;
        needsUpdate = pNeedsUpdate;
    }


	public static final ObjectModificationDefaultImpl INSERT = new ObjectModificationDefaultImpl(true, false);
	public static final ObjectModificationDefaultImpl UPDATE = new ObjectModificationDefaultImpl(false, true);


    /**
     * returns true if the underlying Object needs an INSERT statement. Returns false else.
     */
    public boolean needsInsert()
    {
        return needsInsert;
    }

    /**
     * returns true if the underlying Object needs an UPDATE statement.
     *
     * Else Returns false.
     */
    public boolean needsUpdate()
    {
        return needsUpdate;
    }

    /**
     * Method declaration
     *
     * @param newValue
     */
    public void setNeedsInsert(boolean newValue)
    {
        needsInsert = newValue;
    }

    /**
     * Method declaration
     *
     * @param newValue
     */
    public void setNeedsUpdate(boolean newValue)
    {
        needsUpdate = newValue;
    }

    /**
     * Method declaration
     */
    public void markModified()
    {
        needsUpdate = true;
    }
}
