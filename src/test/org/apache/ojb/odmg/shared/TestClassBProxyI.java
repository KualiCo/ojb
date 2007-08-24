package org.apache.ojb.odmg.shared;

import java.io.Serializable;

/**
 * @author <a href="mailto:user@domain.com">John Doe</a>
 * The interface to allow TestClassBProxy to work
 * as a dynamic proxy
 *	
 */

public interface TestClassBProxyI extends Serializable
{
	
    /**
    * Gets the value1.
    * @return returns value1
    */
    String getValue1();
}
