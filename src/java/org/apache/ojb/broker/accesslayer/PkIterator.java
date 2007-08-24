package org.apache.ojb.broker.accesslayer;

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
import java.util.Enumeration;
import java.util.Iterator;

/**
 * @version $Id: PkIterator.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class PkIterator implements Iterator, Serializable
{
	static final long serialVersionUID = 1906110293623601724L;
    private Enumeration internalEnum;

    public PkIterator(Enumeration en)
    {
        internalEnum = en;
    }

    public boolean hasNext()
    {
        return internalEnum.hasMoreElements();
    }

    public Object next()
    {
        return internalEnum.nextElement();
    }

    public void remove()
    {
        //do nothing
    }
}
