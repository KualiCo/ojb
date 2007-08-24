package org.apache.ojb.odmg.collections;

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

/**
 * Encapsulates an DSet entry object.
 *
 * @version $Id: DSetEntry.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class DSetEntry extends DListEntry implements Serializable
{
    private static final long serialVersionUID = 6334656303221694908L;
    
    /**
     * DSetEntry constructor comment.
     */
    public DSetEntry()
    {
        super();
    }

    /**
     * DSetEntry constructor comment.
     * @param theDSet org.apache.ojb.server.collections.DListImpl
     * @param theObject java.lang.Object
     */
    public DSetEntry(DSetImpl theDSet, Object theObject)
    {
        this.position = theDSet.size();
        this.dlistId = theDSet.getId();
        this.realSubject = theObject;
        this.pbKey = getPBKey();
    }
}
