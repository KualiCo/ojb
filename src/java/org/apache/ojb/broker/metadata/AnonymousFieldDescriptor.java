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

import org.apache.ojb.broker.metadata.fieldaccess.AnonymousPersistentField;

public final class AnonymousFieldDescriptor extends FieldDescriptor
{
    private static final long serialVersionUID = -2179877752386923963L;

    public AnonymousFieldDescriptor(ClassDescriptor cld, int id)
    {
        super(cld, id);
    }

    public void setPersistentField(Class c, String fieldName)
    {
        m_PersistentField = new AnonymousPersistentField(fieldName);
    }
}

