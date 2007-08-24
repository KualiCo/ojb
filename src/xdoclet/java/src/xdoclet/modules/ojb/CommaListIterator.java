package xdoclet.modules.ojb;

/* Copyright 2004-2005 The Apache Software Foundation
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

import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * Small helper class for dealing with comma-separated string lists.
 * 
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class CommaListIterator implements Iterator
{
    /** The tokenizer for the comma-separated list */
    private StringTokenizer _list;
    /** The current token */
    private String          _current = "";

    /**
     * Creates a new string iterator for the given comma-separated list.
     *  
     * @param list The list
     */
    public CommaListIterator(String list)
    {
        _list = new StringTokenizer(list == null ? "" : list, ",");
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext()
    {
        while (_list.hasMoreTokens() && (_current.length() == 0))
        {
            _current = _list.nextToken();
        }
        return (_current.length() > 0);
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next()
    {
        return getNext();
    }

    /**
     * Returns the next string from the list.
     * 
     * @return The next string
     */
    public String getNext()
    {
        String result = _current;

        _current = "";
        return result.trim();
    }

    /**
     * Determines whether one of the remaining elements is the given element. If it is found
     * then the iterator is moved after the element, otherwise the iterator is after the end
     * of the list.
     *  
     * @param element The element to search for
     * @return <code>true</code> if the element has been found
     */
    public boolean contains(String element)
    {
        while (hasNext())
        {
            if (element.equals(getNext()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares this iterator with the other given one. Note that this consumes
     * the iterators, so you should not use them afterwards.
     * 
     * @param obj The other object
     * @return If the other object is a comma list iterator and it delivers the same values
     *         as this iterator 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        if (!(obj instanceof CommaListIterator))
        {
            return false;
        }

        CommaListIterator otherIt = (CommaListIterator)obj;

        while (hasNext() || otherIt.hasNext())
        {
            if (!hasNext() || !otherIt.hasNext())
            {
                return false;
            }
            if (!getNext().equals(otherIt.getNext()))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares the two comma-separated lists.
     * 
     * @param list1 The first list
     * @param list2 The second list
     * @return <code>true</code> if the lists are equal
     */
    public static boolean sameLists(String list1, String list2)
    {
        return new CommaListIterator(list1).equals(new CommaListIterator(list2));
    }
}
