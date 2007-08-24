package org.apache.ojb.broker.metadata.fieldaccess;

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


/**
 * A {@link PersistentField} implementation using
 * JavaBeans compliant calls only to access persistent attributes.
 * No Reflection is needed. But for each attribute xxx there must be
 * public getXxx() and setXxx() methods. In metadata the field name must be
 * the bean compliant 'xxx'.
 *
 * @deprecated replaced by {@link PersistentFieldIntrospectorImpl}.
 * @version $Id: PersistentFieldIntrospectorImplNew.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PersistentFieldIntrospectorImplNew extends PersistentFieldIntrospectorImpl
{
}
