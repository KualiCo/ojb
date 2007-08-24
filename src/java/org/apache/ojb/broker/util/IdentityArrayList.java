package org.apache.ojb.broker.util;

/* Copyright 2004-2006 The Apache Software Foundation
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;

/**
 * Object identity based {@link java.util.List}, use <tt>"=="</tt> instead of
 * <tt>element_1.equals(element_2)</tt> to compare objects.
 *
 * @version $Id: IdentityArrayList.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class IdentityArrayList extends ArrayList
{

    public IdentityArrayList()
    {
    }

    public IdentityArrayList(int initialCapacity)
    {
        super(initialCapacity);
    }

    public IdentityArrayList(Collection c)
    {
        super(c);
    }

    public boolean contains(Object elem)
    {
        return indexOf(elem) >= 0;
    }

    public int indexOf(Object elem)
    {
        for(int i = 0; i < size(); i++)
            if(elem == get(i))
                return i;
        return -1;
    }

    public int lastIndexOf(Object elem)
    {
        for(int i = size() - 1; i >= 0; i--)
            if(elem == get(i))
                return i;
        return -1;
    }

    public boolean remove(Object o)
    {
        Iterator e = iterator();
        if(o == null)
        {
            while(e.hasNext())
            {
                if(e.next() == null)
                {
                    e.remove();
                    return true;
                }
            }
        }
        else
        {
            while(e.hasNext())
            {
                if(o == e.next())
                {
                    e.remove();
                    return true;
                }
            }
        }
        return false;
    }
}
