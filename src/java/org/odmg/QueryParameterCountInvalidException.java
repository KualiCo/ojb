package org.odmg;

/**
 * This exception is thrown when the number of bound parameters for a query
 * does not match the number of placeholders.
 * @author	David Jordan (as Java Editor of the Object Data Management Group)
 * @version ODMG 3.0
 */

public class QueryParameterCountInvalidException extends QueryException
{
    /**
     * Construct an instance of the exception.
     */
    public QueryParameterCountInvalidException()
    {
        super();
    }

    /**
     * Construct an instance of the exception with a message.
     * @param	msg	A message indicating why the exception has been thrown.
     */
    public QueryParameterCountInvalidException(String msg)
    {
        super(msg);
    }
}
