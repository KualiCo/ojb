package org.apache.ojb.broker.accesslayer;

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

import org.apache.ojb.broker.metadata.ClassDescriptor;

import java.io.Serializable;
import java.sql.ResultSet;import java.util.Map;

/**
 * @version $Id: RowReader.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public interface RowReader extends Serializable
{
	static final long serialVersionUID = -1283322922537162249L;    /**
     * materialize a single object from the values of the Map row.
     * the implementor of this class must not care for materializing
     * references or collection attributes, this is done later! 
     * @param row the Map containing the new values
     * @return a properly created instance.
     */
    public Object readObjectFrom(Map row);

    /**
     * refresh an existing instance from the values of the Map row.
     * @param instance the instance to refresh
     * @param row the Map containing the new values
     */
    public void refreshObject(Object instance, Map row);


	/**
	 * Read all fields from the current ResultRow into the Object[] row.#
	 * ConversionStrategies are applied here!
	 */
	public void readObjectArrayFrom(ResultSetAndStatement rs, Map row);

	/**
	 * Read primary key fields from the current ResultRow into the Object[] row.#
	 * ConversionStrategies are applied here!
	 */
	public void readPkValuesFrom(ResultSetAndStatement rs, Map row);

    /**
     * Set the descriptor this <i>RowReader</i> worked with.
     */
    public void setClassDescriptor(ClassDescriptor cld);

    /**
     * Returns the associated {@link org.apache.ojb.broker.metadata.ClassDescriptor}
     */
    public ClassDescriptor getClassDescriptor();
}
