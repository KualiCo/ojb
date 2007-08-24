package org.odmg;

/**
 * This exception is thrown when the type of a query parameter
 * is not compatible with the expected type.
 * @author	David Jordan (as Java Editor of the Object Data Management Group)
 * @version ODMG 3.0
 */

public class QueryParameterTypeInvalidException extends QueryException
{
    /**
     * Construct an instance of the exception.
     */
    public QueryParameterTypeInvalidException()
    {
        super();
    }

    /**
     * Construct an instance of the exception with a message.
     *	@param	msg	The message explaining details of the exception.
     */
    public QueryParameterTypeInvalidException(String msg)
    {
        super(msg);
    }
}
