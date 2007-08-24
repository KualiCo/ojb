package org.apache.ojb.broker.accesslayer.conversions;

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

import java.io.Serializable;


/**
 * The class <em>FieldConversion</em> declares a protocol for type and value
 * conversions between persistent classes attributes and counterpart objects supported by the
 * JDBC specification, e.g. <em>String</em> is supported by JDBC, so only an <em>empty</em> field
 * conversion is needed. But if the persistent class attribute is of type <code>int[]</code> a
 * field conversion to a supported field type is needed - e.g. <code>int[] ---> String</code>.
 * <p/>
 * The default implementation {@link FieldConversionDefaultImpl} does not modify its input.
 * OJB users can use predefined implementation and can also 
 * build their own conversions that perform arbitrary mappings.
 * The mapping has to defined in the OJB mapping configuration file - more see documentation.
 * 
 * @author Thomas Mahler
 * @version $Id: FieldConversion.java,v 1.1 2007-08-24 22:17:31 ewestfal Exp $
 */
public interface FieldConversion extends Serializable
{
	/**
     * Convert an object of the persistent class to a counterpart object
     * supported by the JDBC specification.
     */
    public Object javaToSql(Object source) throws ConversionException;

    /**
     * Convert a JDBC object to a persistent class value.
     */
    public Object sqlToJava(Object source) throws ConversionException;

}
