package org.apache.ojb.odmg.shared;

import org.apache.ojb.broker.Zoo;

/**
 * This class just extends the Zoo class and doesn't add any functionality.
 * It is necessary, so a different mapping can be used in the repository file
 * (i.e. no auto-update,... as in Zoo)
 * @author <a href="mailto:schneider@mendel.imp.univie.ac.at">Georg Schneider</a>
 *
 */
public class ODMGZoo extends Zoo
{

	/**
	 * Constructor for ODMGZoo.
	 */
	public ODMGZoo()
	{
		super();
	}

	/**
	 * Constructor for ODMGZoo.
	 * @param name
	 */
	public ODMGZoo(String name)
	{
		super(name);
	}

}
