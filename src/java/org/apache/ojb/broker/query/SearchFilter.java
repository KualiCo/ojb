package org.apache.ojb.broker.query;

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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Class to build selection criteria for searches
 * Search Filter Class (Abstract)
 * This class builds a search filter tree, specifing how names and
 * values are to be compared when searching a database.
 * It just builds internal structures, and needs to be extended
 * to return a search filter string or other object that can be
 * used by the database to perform the actual search.
 */
public abstract class SearchFilter
{
    // Define the operators
    // Binary operators have bit 8 set
    // Logical operators have bit 9 set
    public static final int AND = 0x200;              // Logical operator
    public static final int OR = 0x201;               // Logical operator
    public static final int NOT = 0x2;                // Unary operator
    public static final int IN = 0x3;
    public static final int NOT_IN = 0x4;
    public static final int LIKE = 0x109;             // Binary operator
    public static final int EQUAL = 0x10A;            // Binary operator
    public static final int NOT_EQUAL = 0x10B;        // Binary operator
    public static final int LESS_THAN = 0x10C;        // Binary operator
    public static final int GREATER_THAN = 0x10D;     // Binary operator
    public static final int GREATER_EQUAL = 0x10E;    // Binary operator
    public static final int LESS_EQUAL = 0x10F;       // Binary operator
    // Define a mask for the binary and logical operators
    // private static final int					BINARY_OPER_MASK = 0x100;
    // private static final int					LOGICAL_OPER_MASK = 0x200;
    protected static final int BINARY_OPER_MASK = 0x100;
    protected static final int LOGICAL_OPER_MASK = 0x200;
    // Define the current search filter
    protected SearchBase m_filter = null;

    /**
     * Create an empty search filter.
     *
     */
    // -----------------------------------------------------------
    public SearchFilter()
    {
    }

    /**
     * Change the search filter to one that specifies an element to
     * match or not match one of a list of values.
     * The old search filter is deleted.
     *
     * @param ElementName is the name of the element to be matched
     * @param values is a vector of possible matches
     * @param oper is the IN or NOT_IN operator to indicate how to matche
     */
    public void matchList(String ElementName, Vector values, int oper)
    {
        // Delete the old search filter
        m_filter = null;
        // If not NOT_IN, assume IN
        // (Since ints are passed by value, it is OK to change it)
        if (oper != NOT_IN)
        {
            oper = IN;
            // Convert the vector of match strings to an array of strings
        }
        String[] value_string_array = new String[values.size()];
        values.copyInto(value_string_array);
        // Create a leaf node for this list and store it as the filter
        m_filter = new SearchBaseLeaf(ElementName, oper, value_string_array);
    }

    /**
     * Change the search filter to one that specifies an element to not
     * match one of a list of values.
     * The old search filter is deleted.
     *
     * @param ElementName is the name of the element to be matched
     * @param values is an array of possible matches
     * @param oper is the IN or NOT_IN operator to indicate how to matche
     */
    public void matchList(String ElementName, String[] values, int oper)
    {
        // Delete the old search filter
        m_filter = null;
        // If not NOT_IN, assume IN
        // (Since ints are passed by value, it is OK to change it)
        if (oper != NOT_IN)
        {
            oper = IN;
            // Create a leaf node for this list and store it as the filter
        }
        m_filter = new SearchBaseLeaf(ElementName, oper, values);
    }

    /**
     * Change the search filter to one that specifies an element to not
     * match one of a list of integer values.
     * The old search filter is deleted.
     *
     * @param ElementName is the name of the element to be matched
     * @param values is an array of possible integer matches
     * @param oper is the IN or NOT_IN operator to indicate how to matche
     */
    public void matchList(String ElementName, int[] values, int oper)
    {
        // Delete the old search filter
        m_filter = null;
        // If not NOT_IN, assume IN
        // (Since ints are passed by value, it is OK to change it)
        if (oper != NOT_IN)
        {
            oper = IN;
            // Create a leaf node for this list and store it as the filter
        }
        m_filter = new SearchBaseLeafInt(ElementName, oper, values);
    }

    /**
     * Change the search filter to one that specifies an element to not
     * match one single value.
     * The old search filter is deleted.
     *
     * @param ElementName is the name of the element to be matched
     * @param value is the value to not be matched
     * @param oper is the IN or NOT_IN operator to indicate how to matche
     */
    public void matchValue(String ElementName, String value, int oper)
    {
        // Delete the old search filter
        m_filter = null;
        // If not NOT_IN, assume IN
        // (Since ints are passed by value, it is OK to change it)
        if (oper != NOT_IN)
        {
            oper = IN;
            // Create a String array in which to hold the one name,
            // and put that name in the array
        }
        String[] ValueArray = new String[1];
        ValueArray[0] = value;
        // Create a leaf node for this list and store it as the filter
        m_filter = new SearchBaseLeaf(ElementName, oper, ValueArray);
    }

    /**
     * -----------------------------------------------------------
     * @param ElementName
     * @param value
     * @param oper
     */
    public void matchValue(String ElementName, int value, int oper)
    {
        // Delete the old search filter
        m_filter = null;
        // If not NOT_IN, assume IN
        // (Since ints are passed by value, it is OK to change it)
        if (oper != NOT_IN)
        {
            oper = IN;
            // Create a leaf node for this list and store it as the filter
        }
        m_filter = new SearchBaseLeafInt(ElementName, oper, new int[]
        {
            value
        });
    }

    /**
     * Change the search filter to one that compares an element name to a value.
     * The old search filter is deleted.
     *
     * @param ElementName is the name of the element to be tested
     * @param value is the value to be compared against
     * @param oper is the binary comparison operator to be used
     * @exception DBException
     */
    public void compareFilter(String ElementName, String value, int oper) throws DBException
    {
        // Delete the old search filter
        m_filter = null;
        // If this is not a binary operator, throw an exception
        if ((oper & BINARY_OPER_MASK) == 0)
        {
            throw new DBException();
            // Create a SearchBaseLeafComparison node and store it as the filter
        }
        m_filter = new SearchBaseLeafComparison(ElementName, oper, value);
    }

    /**
     * Change the search filter to one that specifies a set of elements and their values
     * that must match, and the operator to use to combine the elements.
     * Each key is compared for an equal match to the value, and all
     * comparisons are combined by the specified logical operator (OR or AND).
     * The old search filter is deleted.
     *
     * @param elements is a hashtable holding key-value pairs
     * @param combine_op is the logical operator to be used to combine the comparisons
     * @param compare_op is the binary operator to be used for the comparisons
     * @exception DBException
     */
    public void matchSet(Hashtable elements, int combine_op, int compare_op) throws DBException
    {
        // Delete the old search filter
        m_filter = null;
        // If combine_op is not a logical operator, throw an exception
        if ((combine_op & LOGICAL_OPER_MASK) == 0)
        {
            throw new DBException();
            // If compare_op is not a binary operator, throw an exception
        }
        if ((compare_op & BINARY_OPER_MASK) == 0)
        {
            throw new DBException();
            // Create a vector that will hold the comparison nodes for all elements in the hashtable
        }
        Vector compareVector = new Vector();
        // For each of the elements in the hashtable, create a comparison node for the match
        for (Enumeration e = elements.keys(); e.hasMoreElements();)
        {
            // Get the element name from the enumerator
            // and its value
            String elementName = (String) e.nextElement();
            String elementValue = (String) elements.get(elementName);
            // Create a comparison node for this list and store it as the filter
            SearchBaseLeafComparison comparenode = new SearchBaseLeafComparison(elementName,
                    compare_op, elementValue);
            // Add this leaf node to the vector
            compareVector.addElement(comparenode);
        }
        // Now return a node that holds this set of leaf nodes
        m_filter = new SearchBaseNode(combine_op, compareVector);
    }

    /**
     * Change the search filter to one that specifies a set of elements and their values
     * that must match, and the operator to use to combine the elements.
     * Each element name is compared for an equal match to the value, and all
     * comparisons are combined by the specified logical operator (OR or AND).
     * The old search filter is deleted.
     *
     * @param ElementNames is an array of names of elements to be tested
     * @param ElementValues is an array of values for the corresponding element
     * @param op is the logical operator to be used to combine the comparisons
     * @exception DBException
     */
    public void matchSet(String[] ElementNames, String[] ElementValues, int op) throws DBException
    {
        // Delete the old search filter
        m_filter = null;
        // If this is not a logical operator, throw an exception
        if ((op & LOGICAL_OPER_MASK) == 0)
        {
            throw new DBException();
            // Create a vector that will hold the leaf nodes for all elements in the hashtable
        }
        Vector leafVector = new Vector();
        // For each of the elements in the array, create a leaf node for the match
        int numnames = ElementNames.length;
        for (int i = 0; i < numnames; i++)
        {
            // Create a leaf node for this list and store it as the filter
            SearchBaseLeaf leafnode = new SearchBaseLeaf(ElementNames[i], IN, ElementValues[i]);
            // Add this leaf node to the vector
            leafVector.addElement(leafnode);
        }
        // Now return a node that holds this set of leaf nodes
        m_filter = new SearchBaseNode(op, leafVector);
    }

    /**
     * Combine other search filters with this one, using the specific operator.
     *
     * @param new_filters is a vector of SearchFilter classes to be combined
     * @param op is the logical operator to be used to combine the filters
     * @exception DBException
     */
    public void combine(Vector new_filters, int op) throws DBException
    {
        // If this is not a logical operator, throw an exception
        if ((op & LOGICAL_OPER_MASK) == 0)
        {
            throw new DBException();
            // Create a new vector consisting of just the filters
            // from the SearchFilter classes in new_filters
        }
        Vector filters = new Vector();
        // Now add in all the nodes of the new filters
        for (Enumeration e = new_filters.elements(); e.hasMoreElements();)
        {
            // Get the search filter from the vector
            SearchFilter f = (SearchFilter) e.nextElement();
            filters.addElement(f.getFilter());
        }
        // Create a node for this list and return it
        m_filter = new SearchBaseNode(op, m_filter, filters);
    }

    /**
     * Combine one other search filters with this one, using the specific operator.
     *
     * @param new_filter is the SearchFilter class to be combined
     * @param op is the logical operator to be used to combine the filters
     * @exception DBException
     */
    public void combine(SearchFilter new_filter, int op) throws DBException
    {
        // If this is not a logical operator, throw an exception
        if ((op & LOGICAL_OPER_MASK) == 0)
        {
            throw new DBException();
            // Create a new vector consisting of just the filters
            // from the SearchFilter classes in new_filters
        }
        Vector filters = new Vector();
        filters.addElement(new_filter.getFilter());
        // Create a node for this list and return it
        m_filter = new SearchBaseNode(op, m_filter, filters);
    }

    /**
     * Static method to convert a binary operator into a string.
     *
     * @param oper is the binary comparison operator to be converted
     */
    protected static String ConvertBinaryOperator(int oper)
    {
        // Convert the operator into the proper string
        String oper_string;
        switch (oper)
        {
            default:
            case EQUAL:
                oper_string = "=";
                break;
            case LIKE:
                oper_string = "LIKE";
                break;
            case NOT_EQUAL:
                oper_string = "!=";
                break;
            case LESS_THAN:
                oper_string = "<";
                break;
            case GREATER_THAN:
                oper_string = ">";
                break;
            case GREATER_EQUAL:
                oper_string = ">=";
                break;
            case LESS_EQUAL:
                oper_string = "<=";
                break;
        }
        return oper_string;
    }

    /**
     * Get the actual filter out of the class.
     * This is only needed when combining filters together
     * and one instantiation has to get the filter from another.
     * However, I do not know how to protect it from outside use.
     * @return
     *
     */
    protected SearchBase getFilter()
    {
        return m_filter;
    }

    /**
     * Converts this search filter into a search string for use by a database.
     *
     */

    /**
     * -----------------------------------------------------------
     * @return
     */
    public abstract String toString();

    /**
     * SearchBase is the base node class for the parse tree
     * This class holds the binary operator
     *
     */
    // ===============================================================
    protected class SearchBase
    {
        // Define the operator to be used for this group
        public int oper;

        /**
         * Constructor.
         * This is protected so only the subclasses can instantiate it
         *
         */
        // ------------------------------------------------------------
        protected SearchBase()
        {
        }

    }

    /**
     * SearchBaseLeafComparison holds a leaf of the search tree
     * This class holds an element name, a binary operator, and a value to be compared
     */
    // ===============================================================
    protected class SearchBaseLeafComparison extends SearchBase
    {
        // Define the element name
        public String elementName;
        // Only operators allowed are binary operators
        // Define the comparison value
        public String value;

        /**
         * Constructor.
         *
         * @param ElementName is the name of the element to be tested
         * @param oper is the binary operator to be used for the comparison
         * @param value is the value to be used for the comparison
         */
        SearchBaseLeafComparison(String ElementName, int oper, String value)
        {
            this.elementName = ElementName;
            this.oper = oper;
            this.value = value;
        }

    }

    /**
     * SearchBaseLeaf holds a leaf of the search tree
     * This class holds an element name, and a vector of possible matches.
     * It searches for an element of the given name that matches at least
     * one of the strings in the array (IN), or does not match any (NOT_IN)
     *
     */
    // ===============================================================
    protected class SearchBaseLeaf extends SearchBase
    {
        // Define the element name
        public String elementName;
        // Only operators allowed are IN and NOT_IN
        // Define the vector of possible matches
        public String[] matches;

        /**
         * Constructor.
         *
         * @param ElementName is the name of the element to be tested
         * @param oper is the operator (IN or NOT_IN) to be used for the comparison
         * @param matches is an array of String values to be matched
         */
        SearchBaseLeaf(String ElementName, int oper, String[] matches)
        {
            this.elementName = ElementName;
            this.oper = oper;
            this.matches = matches;
        }

        /**
         * Constructor for only one value.
         *
         * @param ElementName is the name of the element to be tested
         * @param oper is the operator (IN or NOT_IN) to be used for the comparison
         * @param match is a string value to be matched
         */
        SearchBaseLeaf(String ElementName, int oper, String match)
        {
            this.elementName = ElementName;
            this.oper = oper;
            this.matches = new String[1];
            this.matches[0] = match;
        }

    }

    /**
     * SearchBaseLeafInt holds a leaf of the search tree with integers
     * This class holds an element name, and a vector of possible matches.
     * It searches for an element of the given name that matches at least
     * one of the integers in the array (IN), or does not match any (NOT_IN)
     */
    // ===============================================================
    protected class SearchBaseLeafInt extends SearchBase
    {
        // Define the element name
        public String elementName;
        // Only operators allowed are IN and NOT_IN
        // Define the vector of possible matches
        public int[] matches;

        /**
         * Constructor.
         *
         * @param ElementName is the name of the element to be tested
         * @param oper is the operator (IN or NOT_IN) to be used for the comparison
         * @param matches is an array of integer values to be matched
         */
        SearchBaseLeafInt(String ElementName, int oper, int[] matches)
        {
            this.elementName = ElementName;
            this.oper = oper;
            this.matches = matches;
        }

        /**
         * Constructor for only one value.
         *
         * @param ElementName is the name of the element to be tested
         * @param oper is the operator (IN or NOT_IN) to be used for the comparison
         * @param match is an int value to be matched
         */
        SearchBaseLeafInt(String ElementName, int oper, int match)
        {
            this.elementName = ElementName;
            this.oper = oper;
            this.matches = new int[1];
            this.matches[0] = match;
        }

    }

    /**
     * Define the class to represent a node of the search tree
     * This class holds an operator and a vector other nodes.
     */
    // ===============================================================
    protected class SearchBaseNode extends SearchBase
    {
        // Define the vector of other nodes
        public Vector nodes;

        /**
         * Constructor.
         * Store a list of filters and an operator for combining them.
         *
         * @param oper is the operator (IN or NOT_IN) to be used for the comparison
         * @param new_filters is a vector of filters to be combined
         */
        SearchBaseNode(int oper, Vector new_filters)
        {
            this.oper = oper;
            this.nodes = new_filters;
        }

        /**
         * Constructor for a specific filter and a vector of new ones.
         * Store a list of filters and an operator for combining them.
         *
         * @param oper is the operator (IN or NOT_IN) to be used for the comparison
         * @param filter is the first filter to be combined
         * @param new_filters is a vector of filters to be combined
         */
        SearchBaseNode(int oper, Object filter, Vector new_filters)
        {
            // Store the operator
            this.oper = oper;
            // Create a vector and add in the first filter as the initial node (if present)
            nodes = new Vector();
            if (filter != null)
            {
                nodes.addElement(filter);
                // Now add in all the nodes of the new filters
            }
            for (Enumeration e = new_filters.elements(); e.hasMoreElements();)
            {
                nodes.addElement(e.nextElement());
            }
        }

    }
}
