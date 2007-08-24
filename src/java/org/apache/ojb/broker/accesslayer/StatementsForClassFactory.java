package org.apache.ojb.broker.accesslayer;

/* Copyright 2003-2005 The Apache Software Foundation
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

import org.apache.ojb.broker.util.factory.ConfigurableFactory;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;

/**
 * Factory for {@link org.apache.ojb.broker.accesslayer.StatementsForClassIF}
 * implementations. Developers may specify the specific implementation returned by
 * {@link #getStatementsForClass} by implementing the
 * {@link org.apache.ojb.broker.accesslayer.StatementsForClassIF}
 * interface and setting the <code>StatementsForClassClass</code> property in
 * <code>OJB.properties</code>.
 * <br/>
 * @see org.apache.ojb.broker.accesslayer.StatementManager
 * @see org.apache.ojb.broker.accesslayer.StatementsForClassImpl
 * @author <a href="mailto:rburt3@mchsi.com">Randall Burt</a>
 * @version $Id: StatementsForClassFactory.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */

public class StatementsForClassFactory extends ConfigurableFactory
{
	private static StatementsForClassFactory singleton;


	/**
	 * Get the singleton instance of this class
     * @return the singleton instance of StatementsForClassFactory
     */
    public static synchronized StatementsForClassFactory getInstance()
	{
		if (singleton == null)
		{
			singleton = new StatementsForClassFactory();
		}
		return singleton;
	}

    /*
     * @see org.apache.ojb.broker.util.factory.ConfigurableFactory#getConfigurationKey()
     */
    protected String getConfigurationKey()
    {
        return "StatementsForClassClass";
    }


    /**
     * Get an instance of {@link org.apache.ojb.broker.accesslayer.StatementsForClassIF}
     * @param cds our connection descriptor
     * @param cld the class descriptor of the persistant object
     * @return an instance of {@link org.apache.ojb.broker.accesslayer.StatementsForClassIF}
     */
    public StatementsForClassIF getStatementsForClass(JdbcConnectionDescriptor cds, ClassDescriptor cld)
    {
		return (StatementsForClassIF) this.createNewInstance(new Class[]{JdbcConnectionDescriptor.class, ClassDescriptor.class},
		                                                     new Object[]{cds, cld});
    }
}
