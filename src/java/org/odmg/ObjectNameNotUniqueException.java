package org.odmg;

/**
 * This exception is thrown when attempting to bind a name to an object
 * when the name is already bound to another object.
 * @author	David Jordan (as Java Editor of the Object Data Management Group)
 * @version ODMG 3.0
 * @see org.odmg.Database#bind
 */

public class ObjectNameNotUniqueException extends ODMGException
{
/*
	private	Object o;
  private String n;
	public ObjectNameNotUniqueException(Object obj, String name)
  {
  	super();
		o = obj;
	n = name;
  }


* Get the object that was passed to Database.bind.
* @return	The object that was being bound to a name.

	public	Object	getObject()
  {
		return o;
  }


* Get the name that is not unique.
* @return The name that is already associated with another object.

  public String getName()
  {
  	return n;
  }
*/
/*
	private	Object o;
  private String n;
	public ObjectNameNotUniqueException(Object obj, String name)
  {
  	super();
		o = obj;
	n = name;
  }


* Get the object that was passed to Database.bind.
* @return	The object that was being bound to a name.

	public	Object	getObject()
  {
		return o;
  }


* Get the name that is not unique.
* @return The name that is already associated with another object.

  public String getName()
  {
  	return n;
  }
*/
/*
	private	Object o;
  private String n;
	public ObjectNameNotUniqueException(Object obj, String name)
  {
  	super();
		o = obj;
	n = name;
  }


* Get the object that was passed to Database.bind.
* @return	The object that was being bound to a name.

	public	Object	getObject()
  {
		return o;
  }


* Get the name that is not unique.
* @return The name that is already associated with another object.

  public String getName()
  {
  	return n;
  }
*/
    /**
     * Construct an instance of the exception.
     */
    public ObjectNameNotUniqueException()
    {
        super();
    }

    /**
     * Construct an instance of the exception with a descriptive message.
     * @param	msg	A message containing a description of the exception.
     */
    public ObjectNameNotUniqueException(String msg)
    {
        super(msg);
    }
/*
	private	Object o;
  private String n;
	public ObjectNameNotUniqueException(Object obj, String name)
  {
  	super();
		o = obj;
	n = name;
  }


* Get the object that was passed to Database.bind.
* @return	The object that was being bound to a name.

	public	Object	getObject()
  {
		return o;
  }


* Get the name that is not unique.
* @return The name that is already associated with another object.

  public String getName()
  {
  	return n;
  }
*/
}
