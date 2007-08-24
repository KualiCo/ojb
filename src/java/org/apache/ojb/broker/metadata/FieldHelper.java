package org.apache.ojb.broker.metadata;

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
 * HelperClass for Fields, used for orderBy, groupBy
 *
 * @author  <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: FieldHelper.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class FieldHelper implements Serializable
{
	private static final long serialVersionUID = -297186561855340166L;
    public String name;
    public boolean isAscending;

    public FieldHelper(String fieldName, boolean orderAscending)
    {
        name = fieldName;
        isAscending = orderAscending;
    }
}

