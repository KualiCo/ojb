package org.odmg;

/**
 * This exception is thrown when a call has been made that modifies
 * a database that is open in read-only mode.
 * @author	David Jordan (as Java Editor of the Object Data Management Group)
 * @version ODMG 3.0
 * @see org.odmg.Database
 * @see org.odmg.ODMGRuntimeException
 */

public class DatabaseIsReadOnlyException extends ODMGRuntimeException
{
    /**
     * Construct an instance of the exception without a message.
     */
    public DatabaseIsReadOnlyException()
    {
        super();
    }

    /**
     * Construct an instance of the exception with a descriptive message.
     * @param	msg	A message indicating why the exception occurred.
     */
    public DatabaseIsReadOnlyException(String msg)
    {
        super(msg);
    }
}
