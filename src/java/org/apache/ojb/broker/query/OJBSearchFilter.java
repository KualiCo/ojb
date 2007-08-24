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
 * OJB Search Filter Class for ObJectRelationalBridge O/R mapping tool
 *
 * This class builds a search filter tree, specifing how names and
 * values are to be compared when searching a database.
 * This extends SearchFilter and implements the Convert method
 * that produces the search filter string for the SQL database
 *
 * @author David Forslund
 * @author koenig
 * @version $Revision: 1.1 $ $Date: 2007-08-24 22:17:36 $
 */
public class OJBSearchFilter extends SearchFilter
{
    Criteria criteria = new Criteria();

    /**
     * Constructors are not needed, as the base class has a default constructor.
     *
     */

    /**
     * Change the search filter to one that specifies an element to
     * match or not match one of a list of values.
     * The old search filter is deleted.
     *
     * @param elementName is the name of the element to be matched
     * @param values is a vector of possible matches
     * @param oper is the IN or NOT_IN operator to indicate how to matche
     */
    public void matchList(String elementName, Vector values, int oper)
    {

        // Delete the old search criteria
        criteria = new Criteria();

        if (oper != NOT_IN)
        {
            for (int i = 0; i < values.size(); i++)
            {
                Criteria tempCrit = new Criteria();

                tempCrit.addEqualTo(elementName, values.elementAt(i));
                criteria.addOrCriteria(tempCrit);
            }
        }
        else
        {
            for (int i = 0; i < values.size(); i++)
            {
                criteria.addNotEqualTo(elementName, values.elementAt(i));
            }
        }
    }

    /**
     * Change the search filter to one that specifies an element to not
     * match one of a list of values.
     * The old search filter is deleted.
     *
     * @param elementName is the name of the element to be matched
     * @param values is an array of possible matches
     * @param oper is the IN or NOT_IN operator to indicate how to matche
     */
    public void matchList(String elementName, String[] values, int oper)
    {

        // see also matchList(String elementName, Vector values, int oper)
        // Delete the old search criteria
        criteria = new Criteria();

        if (oper != NOT_IN)
        {
            for (int i = 0; i < values.length; i++)
            {
                Criteria tempCrit = new Criteria();

                tempCrit.addEqualTo(elementName, values[i]);
                criteria.addOrCriteria(tempCrit);
            }
        }
        else
        {
            for (int i = 0; i < values.length; i++)
            {
                criteria.addNotEqualTo(elementName, values[i]);
            }
        }
    }

    /**
     * Change the search filter to one that specifies an element to not
     * match one of a list of integer values.
     * The old search filter is deleted.
     *
     * @param elementName is the name of the element to be matched
     * @param values is an array of possible integer matches
     * @param oper is the IN or NOT_IN operator to indicate how to matche
     */
    public void matchList(String elementName, int[] values, int oper)
    {

        // see also matchList(String elementName, Vector values, int oper)
        // Delete the old search criteria
        criteria = new Criteria();

        if (oper != NOT_IN)
        {
            for (int i = 0; i < values.length; i++)
            {
                Criteria tempCrit = new Criteria();

                tempCrit.addEqualTo(elementName, new Integer(values[i]));
                criteria.addOrCriteria(tempCrit);
            }
        }
        else
        {
            for (int i = 0; i < values.length; i++)
            {
                criteria.addNotEqualTo(elementName, new Integer(values[i]));
            }
        }
    }

    /**
     * Change the search filter to one that specifies an element to not
     * match one single value.
     * The old search filter is deleted.
     *
     * @param elementName is the name of the element to be matched
     * @param value is the value to not be matched
     * @param oper is the IN or NOT_IN operator to indicate how to matche
     */
    public void matchValue(String elementName, String value, int oper)
    {

        // Delete the old search criteria
        criteria = new Criteria();

        if (oper != NOT_IN)
        {
            criteria.addEqualTo(elementName, value);
        }
        else
        {
            criteria.addNotEqualTo(elementName, value);
        }
    }

    /**
     * -----------------------------------------------------------
     * @param elementName
     * @param value
     * @param oper
     */
    public void matchValue(String elementName, int value, int oper)
    {

        // Delete the old search criteria
        criteria = new Criteria();

        if (oper != NOT_IN)
        {
            criteria.addEqualTo(elementName, new Integer(value));
        }
        else
        {
            criteria.addNotEqualTo(elementName, new Integer(value));
        }
    }

    /**
     * Change the search filter to one that compares an element name to a value.
     * The old search filter is deleted.
     *
     * @param elementName is the name of the element to be tested
     * @param value is the value to be compared against
     * @param oper is the binary comparison operator to be used
     * @exception DBException
     */
    public void compareFilter(String elementName, String value,
                              int oper) throws DBException
    {

        // Delete the old search criteria
        criteria = new Criteria();

        // If this is not a binary operator, throw an exception
        if ((oper & BINARY_OPER_MASK) == 0)
        {
            throw new DBException();
        }

        switch (oper)
        {

            case LIKE:
                {
                    criteria.addLike(elementName, value);

                    break;
                }

            case EQUAL:
                {
                    criteria.addEqualTo(elementName, value);

                    break;
                }

            case NOT_EQUAL:
                {
                    criteria.addNotEqualTo(elementName, value);

                    break;
                }

            case LESS_THAN:
                {
                    criteria.addLessThan(elementName, value);

                    break;
                }

            case GREATER_THAN:
                {
                    criteria.addGreaterThan(elementName, value);

                    break;
                }

            case GREATER_EQUAL:
                {
                    criteria.addGreaterOrEqualThan(elementName, value);

                    break;
                }

            case LESS_EQUAL:
                {
                    criteria.addLessOrEqualThan(elementName, value);

                    break;
                }

            default:
                {
                    throw new DBException("Unsupported binary operation in OJBSearchFilter!");
                }
        }
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
    public void matchSet(Hashtable elements, int combine_op,
                         int compare_op) throws DBException
    {

        // Delete the old search criteria
        criteria = new Criteria();

        // If compare_op is not a binary operator, throw an exception
        if ((compare_op & BINARY_OPER_MASK) == 0)
        {
            throw new DBException();
        }

        if (combine_op == AND)
        {
            // combine all value pairs by an AND

            // For each of the elements in the hashtable, create a comparison node for the match
            for (Enumeration e = elements.keys(); e.hasMoreElements();)
            {

                // Get the element name from the enumerator
                // and its value
                String elementName = (String) e.nextElement();
                String elementValue = (String) elements.get(elementName);

                switch (compare_op)
                {

                    case LIKE:
                        {
                            criteria.addLike(elementName, elementValue);

                            break;
                        }

                    case EQUAL:
                        {
                            criteria.addEqualTo(elementName, elementValue);

                            break;
                        }

                    case NOT_EQUAL:
                        {
                            criteria.addNotEqualTo(elementName, elementValue);

                            break;
                        }

                    case LESS_THAN:
                        {
                            criteria.addLessThan(elementName, elementValue);

                            break;
                        }

                    case GREATER_THAN:
                        {
                            criteria.addGreaterThan(elementName, elementValue);

                            break;
                        }

                    case GREATER_EQUAL:
                        {
                            criteria.addGreaterOrEqualThan(elementName, elementValue);

                            break;
                        }

                    case LESS_EQUAL:
                        {
                            criteria.addLessOrEqualThan(elementName, elementValue);

                            break;
                        }

                    default:
                        {
                            throw new DBException("Unsupported binary operation in OJBSearchFilter!");
                        }
                }						   // end of switch
            }							   // end of for
        }
        else if (combine_op == OR)
        {
            // combine all value pairs by an OR
            // For each of the elements in the hashtable, create a comparison node for the match
            for (Enumeration e = elements.keys(); e.hasMoreElements();)
            {

                // Get the element name from the enumerator
                // and its value
                String elementName = (String) e.nextElement();
                String elementValue = (String) elements.get(elementName);

                switch (compare_op)
                {

                    case LIKE:
                        {
                            Criteria tempCrit = new Criteria();

                            tempCrit.addLike(elementName, elementValue);
                            criteria.addOrCriteria(tempCrit);

                            break;
                        }

                    case EQUAL:
                        {
                            Criteria tempCrit = new Criteria();

                            tempCrit.addEqualTo(elementName, elementValue);
                            criteria.addOrCriteria(tempCrit);

                            break;
                        }

                    case NOT_EQUAL:
                        {
                            Criteria tempCrit = new Criteria();

                            tempCrit.addNotEqualTo(elementName, elementValue);
                            criteria.addOrCriteria(tempCrit);

                            break;
                        }

                    case LESS_THAN:
                        {
                            Criteria tempCrit = new Criteria();

                            tempCrit.addLessThan(elementName, elementValue);
                            criteria.addOrCriteria(tempCrit);

                            break;
                        }

                    case GREATER_THAN:
                        {
                            Criteria tempCrit = new Criteria();

                            tempCrit.addGreaterThan(elementName, elementValue);
                            criteria.addOrCriteria(tempCrit);

                            break;
                        }

                    case GREATER_EQUAL:
                        {
                            Criteria tempCrit = new Criteria();

                            tempCrit.addGreaterOrEqualThan(elementName, elementValue);
                            criteria.addOrCriteria(tempCrit);

                            break;
                        }

                    case LESS_EQUAL:
                        {
                            Criteria tempCrit = new Criteria();

                            tempCrit.addLessOrEqualThan(elementName, elementValue);
                            criteria.addOrCriteria(tempCrit);

                            break;
                        }

                    default:
                        {
                            throw new DBException("Unsupported binary operation in OJBSearchFilter!");
                        }
                }					   // end of switch
            }						   // end of for

        }
        else
        {

            // combine_op is not a logical operator, throw an exception
            throw new DBException();
        }
    }

    /**
     * Change the search filter to one that specifies a set of elements and their values
     * that must match, and the operator to use to combine the elements.
     * Each element name is compared for an equal match to the value, and all
     * comparisons are combined by the specified logical operator (OR or AND).
     * The old search filter is deleted.
     *
     * @param elementNames is an array of names of elements to be tested
     * @param elementValues is an array of values for the corresponding element
     * @param op is the logical operator to be used to combine the comparisons
     * @exception DBException
     */
    public void matchSet(String[] elementNames, String[] elementValues,
                         int op) throws DBException
    {

        // Delete the old search criteria
        criteria = new Criteria();

        if (op == OR)
        {
            // For each of the elements in the array, create a leaf node for the match
            for (int i = 0; i < elementNames.length; i++)
            {
                Criteria tempCrit = new Criteria();

                tempCrit.addEqualTo(elementNames[i], elementValues[i]);
                criteria.addOrCriteria(tempCrit);
            }
        }
        else if (op == AND)
        {
            for (int i = 0; i < elementNames.length; i++)
            {
                criteria.addEqualTo(elementNames[i], elementValues[i]);
            }
        }
        else
        {
            throw new DBException();
        }
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
        for (Enumeration elems = new_filters.elements(); elems.hasMoreElements();)
        {
            SearchFilter filter = (SearchFilter) elems.nextElement();

            combine(filter, op);
        }
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

        // cast down to OJBSearchFilter
        OJBSearchFilter ojbFilter = (OJBSearchFilter) new_filter;

        switch (op)
        {

            case OR:
                {
                    criteria.addOrCriteria(ojbFilter.getCriteria());
                    break;
                }

            case AND:
                {
                    criteria.addAndCriteria(ojbFilter.getCriteria());
                    break;
                }

            default:
                {
                    throw new DBException();
                }
        }
    }

    /**
     * Converts this search filter into a search string
     * Note:
     * ObJectRelationalBridge can't parse a SQL string yet, the functionality
     * is therefor not implemented!
     */

    /**
     * -----------------------------------------------------------
     * @return
     */
    public String toString()
    {

        // return "";
        return criteria.toString();
    }

    /**
     * Returns the search critera
     *
     */

    /**
     * -----------------------------------------------------------
     * @return
     */
    protected Criteria getCriteria()
    {

        // return the search criteria
        return criteria;
    }

}
