package org.apache.ojb.odmg.link;

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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;

/**
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: LinkEntry.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public abstract class LinkEntry
{
    final boolean isUnlink;
    final Object source;
    final Object referenceToLink;
    final ObjectReferenceDescriptor ord;

    protected LinkEntry(Object source, ObjectReferenceDescriptor ord, Object referenceToLink, boolean unlink)
    {
        this.source = source;
        this.ord = ord;
        this.referenceToLink = referenceToLink;
        isUnlink = unlink;
    }

    public abstract void execute(final PersistenceBroker broker);

    public String toString()
    {
        return new ToStringBuilder(this)
                .append("source", source)
                .append("descriptor", ord)
                .append("referenceToLink", referenceToLink)
                .append("isUnlink", isUnlink)
                .toString();
    }
}
