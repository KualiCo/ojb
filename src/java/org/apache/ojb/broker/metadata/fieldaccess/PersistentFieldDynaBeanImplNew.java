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
 * A {@link PersistentField} implementation accesses a property
 * from a {@link org.apache.commons.beanutils.DynaBean}.
 * Note that because of the way that PersistentField works,
 * at run time the type of the field could actually be different, since
 * it depends on the DynaClass of the DynaBean that is given at runtime.
 * <p>
 * This implementation does not support nested fields.
 * </p>
 *
 * @deprecated replaced by {@link PersistentFieldDynaBeanImpl}.
 * @version $Id: PersistentFieldDynaBeanImplNew.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PersistentFieldDynaBeanImplNew extends PersistentFieldDynaBeanImpl
{
}
